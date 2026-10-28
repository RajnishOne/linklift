package com.rjnsdev.linklift.app.util

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import java.io.IOException

/**
 * A [DataSource] wrapper that automatically chunks open-ended or large HTTP range requests
 * into consecutive bounded byte ranges (e.g., 5MB chunks).
 *
 * This is required for streaming providers like YouTube / googlevideo.com CDN nodes, which reject
 * unbounded HTTP range requests (`Range: bytes=0-`) or requests without a Range header with HTTP 403 Forbidden,
 * and require bounded requests (`Range: bytes=start-end`).
 *
 * Rather than returning EOF when a single chunk ends (which causes ExoPlayer to assume the whole file
 * has finished downloading and starve the buffer after a few seconds), this source transparently
 * opens the next chunk and feeds consecutive bytes to the player.
 */
class ChunkedHttpDataSource(
    private val upstreamFactory: DataSource.Factory,
    private val chunkSize: Long = DEFAULT_CHUNK_SIZE,
) : BaseDataSource(/* isNetwork = */ true) {

    companion object {
        const val DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024L // 5MB
        private const val TAG = "ChunkedHttpDataSource"
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val chunkSize: Long = DEFAULT_CHUNK_SIZE,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return ChunkedHttpDataSource(upstreamFactory, chunkSize)
        }
    }

    private var currentDataSpec: DataSpec? = null
    private var currentChunkSource: DataSource? = null
    private var currentPosition: Long = 0L
    private var totalContentLength: Long = -1L
    private var bytesRemainingInRequestedRange: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingInCurrentChunk: Long = 0L
    private var isOpened: Boolean = false
    private var isGoogleVideo: Boolean = false
    private var requestNumber: Int = 0

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        currentDataSpec = dataSpec
        currentPosition = dataSpec.position
        requestNumber = 0
        val host = dataSpec.uri.host.orEmpty()
        isGoogleVideo = host.contains("googlevideo.com")

        if (!isGoogleVideo) {
            // For standard hosts that support full streaming, delegate directly to a single source
            val source = upstreamFactory.createDataSource()
            currentChunkSource = source
            val length = source.open(dataSpec)
            isOpened = true
            transferStarted(dataSpec)
            return length
        }

        // Parse content length from clen query parameter if present
        val clen = dataSpec.uri.getQueryParameter("clen")?.toLongOrNull()
        totalContentLength = if (clen != null && clen > 0L) clen else -1L

        bytesRemainingInRequestedRange = dataSpec.length

        if (totalContentLength > 0L && currentPosition >= totalContentLength) {
            isOpened = true
            transferStarted(dataSpec)
            return 0L
        }
        if (bytesRemainingInRequestedRange == 0L) {
            isOpened = true
            transferStarted(dataSpec)
            return 0L
        }

        val opened = openNextChunk()
        if (!opened) {
            isOpened = true
            transferStarted(dataSpec)
            return 0L
        }

        isOpened = true
        transferStarted(dataSpec)

        return if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (totalContentLength > 0L && totalContentLength >= currentPosition) {
            totalContentLength - currentPosition
        } else {
            C.LENGTH_UNSET.toLong()
        }
    }

    private fun openNextChunk(): Boolean {
        closeCurrentChunk()
        val spec = currentDataSpec ?: return false

        if (totalContentLength > 0L && currentPosition >= totalContentLength) {
            return false
        }
        if (bytesRemainingInRequestedRange == 0L) {
            return false
        }

        requestNumber++
        val chunkStart = currentPosition
        val chunkEnd = if (totalContentLength > 0L) {
            minOf(chunkStart + chunkSize - 1L, totalContentLength - 1L)
        } else {
            chunkStart + chunkSize - 1L
        }

        if (chunkEnd < chunkStart) {
            return false
        }

        val chunkLength = chunkEnd - chunkStart + 1L
        val chunkSpec = if (isGoogleVideo) {
            // For Google Video CDN streams, YouTube expects URL query params:
            // &range=start-end&rn=N
            // and no Range header (position=0, length=unset) to avoid HTTP 416 conflicts
            // and avoid the unsequenced stream 403 cutoff.
            val rawUri = spec.uri.toString()
            val cleanBase = rawUri
                .replace(Regex("&range=[^&]*"), "")
                .replace(Regex("&rn=[^&]*"), "")
            val delimiter = if (cleanBase.contains("?")) "&" else "?"
            val newUri = Uri.parse("$cleanBase${delimiter}range=$chunkStart-$chunkEnd&rn=$requestNumber")

            spec.buildUpon()
                .setUri(newUri)
                .setPosition(0L)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()
        } else {
            spec.buildUpon()
                .setPosition(chunkStart)
                .setLength(chunkLength)
                .build()
        }

        val source = upstreamFactory.createDataSource()
        try {
            val resolvedLength = source.open(chunkSpec)

            if (totalContentLength <= 0L) {
                val clen = chunkSpec.uri.getQueryParameter("clen")?.toLongOrNull()
                if (clen != null && clen > 0L) {
                    totalContentLength = clen
                } else {
                    val cr = source.responseHeaders["Content-Range"]?.firstOrNull()
                    val totalFromCr = cr?.substringAfterLast('/')?.trim()?.toLongOrNull()
                    if (totalFromCr != null && totalFromCr > 0L) {
                        totalContentLength = totalFromCr
                    }
                }
            }

            bytesRemainingInCurrentChunk = if (resolvedLength != C.LENGTH_UNSET.toLong()) {
                resolvedLength
            } else {
                chunkLength
            }
            Log.i(TAG, "Chunk #$requestNumber opened: range $chunkStart-$chunkEnd ($bytesRemainingInCurrentChunk bytes)")
            currentChunkSource = source
            return bytesRemainingInCurrentChunk > 0L
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            source.close()
            if (e.responseCode == 416) {
                // Range Not Satisfiable -> reached end of file
                return false
            }
            if (isGoogleVideo && e.responseCode == 403 && currentPosition > 0L) {
                // If Google Video returns 403 mid-stream after initial playback, treat it as EOF
                // to allow buffered content to play cleanly without triggering a fatal player error.
                Log.w(TAG, "Google Video stream ended or restricted at byte $currentPosition: HTTP 403")
                return false
            }
            Log.e(TAG, "Chunk request failed: HTTP ${e.responseCode} for range $chunkStart-$chunkEnd", e)
            throw e
        } catch (e: Exception) {
            source.close()
            Log.e(TAG, "Error opening chunk for range $chunkStart-$chunkEnd", e)
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (!isOpened) return C.RESULT_END_OF_INPUT

        if (!isGoogleVideo) {
            val source = currentChunkSource ?: return C.RESULT_END_OF_INPUT
            val bytesRead = source.read(buffer, offset, length)
            if (bytesRead > 0) {
                bytesTransferred(bytesRead)
            }
            return bytesRead
        }

        if (bytesRemainingInRequestedRange == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        while (true) {
            val source = currentChunkSource
            if (source != null && bytesRemainingInCurrentChunk > 0L) {
                val toReadLong = if (bytesRemainingInRequestedRange != C.LENGTH_UNSET.toLong()) {
                    minOf(length.toLong(), bytesRemainingInCurrentChunk, bytesRemainingInRequestedRange)
                } else {
                    minOf(length.toLong(), bytesRemainingInCurrentChunk)
                }
                val toRead = toReadLong.toInt()
                if (toRead > 0) {
                    val bytesRead = source.read(buffer, offset, toRead)
                    if (bytesRead > 0) {
                        currentPosition += bytesRead
                        bytesRemainingInCurrentChunk -= bytesRead
                        if (bytesRemainingInRequestedRange != C.LENGTH_UNSET.toLong()) {
                            bytesRemainingInRequestedRange -= bytesRead
                        }
                        bytesTransferred(bytesRead)
                        return bytesRead
                    } else if (bytesRead == C.RESULT_END_OF_INPUT) {
                        closeCurrentChunk()
                    }
                }
            }

            // Current chunk finished
            if (bytesRemainingInRequestedRange == 0L) {
                return C.RESULT_END_OF_INPUT
            }
            if (totalContentLength > 0L && currentPosition >= totalContentLength) {
                return C.RESULT_END_OF_INPUT
            }

            Log.i(TAG, "Current chunk exhausted at $currentPosition. Opening next chunk...")
            val opened = try {
                openNextChunk()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open next chunk at position $currentPosition: ${e.message}")
                return C.RESULT_END_OF_INPUT
            }
            if (!opened) {
                Log.i(TAG, "No more chunks available at $currentPosition. Returning EOF.")
                return C.RESULT_END_OF_INPUT
            }
        }
    }

    override fun getUri(): Uri? {
        return currentChunkSource?.uri ?: currentDataSpec?.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        return currentChunkSource?.responseHeaders ?: emptyMap()
    }

    override fun close() {
        closeCurrentChunk()
        if (isOpened) {
            isOpened = false
            transferEnded()
        }
        currentDataSpec = null
    }

    private fun closeCurrentChunk() {
        try {
            currentChunkSource?.close()
        } catch (ignored: Exception) {
        } finally {
            currentChunkSource = null
            bytesRemainingInCurrentChunk = 0L
        }
    }
}
