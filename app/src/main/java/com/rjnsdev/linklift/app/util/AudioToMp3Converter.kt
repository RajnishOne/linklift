package com.rjnsdev.linklift.app.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

object AudioToMp3Converter {
    private const val TAG = "AudioToMp3Converter"
    private const val TIMEOUT_US = 10_000L

    suspend fun convert(
        inputFile: File,
        outputFile: File,
        title: String? = null,
        artist: String? = null,
        bitrateKbps: Int = 192,
        onProgress: ((Float) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        require(inputFile.exists()) { "Input file does not exist: ${inputFile.absolutePath}" }
        if (outputFile.exists()) outputFile.delete()

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: Mp3Encoder? = null
        var outputStream: FileOutputStream? = null

        try {
            extractor = MediaExtractor().apply {
                setDataSource(inputFile.absolutePath)
            }

            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                throw IllegalStateException("No audio track found in file: ${inputFile.name}")
            }

            extractor.selectTrack(audioTrackIndex)

            val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            var channels = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(trackFormat, null, null, 0)
                start()
            }

            fun createEncoder(sr: Int, ch: Int): Mp3Encoder {
                val effectiveChannels = ch.coerceIn(1, 2)
                return Mp3Encoder(
                    inSampleRate = sr,
                    inChannels = effectiveChannels,
                    outSampleRate = sr,
                    outBitrateKbps = bitrateKbps,
                    quality = 2,
                    title = title,
                    artist = artist,
                )
            }

            encoder = createEncoder(sampleRate, channels)
            outputStream = FileOutputStream(outputFile)

            val bufferInfo = MediaCodec.BufferInfo()
            val mp3Buffer = ByteArray(65536)
            var shortBuffer = ShortArray(16384)
            var stereoDownmixBuffer = ShortArray(16384)

            var isExtractorEOS = false
            var isDecoderEOS = false

            while (!isDecoderEOS) {
                if (isCancelled?.invoke() == true) {
                    throw CancellationException("MP3 conversion cancelled")
                }

                if (!isExtractorEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                isExtractorEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(
                                    inputBufferIndex,
                                    0,
                                    sampleSize,
                                    sampleTime,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputBufferIndex >= 0 -> {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isDecoderEOS = true
                        }

                        if (bufferInfo.size > 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                                val numShorts = bufferInfo.size / 2
                                if (shortBuffer.size < numShorts) {
                                    shortBuffer = ShortArray(numShorts)
                                }
                                outputBuffer.asShortBuffer().get(shortBuffer, 0, numShorts)

                                val (pcmToEncode, framesToEncode) = if (channels > 2) {
                                    val numFrames = numShorts / channels
                                    if (stereoDownmixBuffer.size < numFrames * 2) {
                                        stereoDownmixBuffer = ShortArray(numFrames * 2)
                                    }
                                    for (f in 0 until numFrames) {
                                        val base = f * channels
                                        val l = shortBuffer[base].toInt()
                                        val r = shortBuffer[base + 1].toInt()
                                        val c = shortBuffer[base + 2].toInt() / 2
                                        stereoDownmixBuffer[f * 2] = (l + c).coerceIn(-32768, 32767).toShort()
                                        stereoDownmixBuffer[f * 2 + 1] = (r + c).coerceIn(-32768, 32767).toShort()
                                    }
                                    stereoDownmixBuffer to numFrames
                                } else {
                                    val frames = if (channels == 1) numShorts else numShorts / 2
                                    shortBuffer to frames
                                }

                                val activeEncoder = encoder ?: throw IllegalStateException("Encoder is not initialized")
                                val encodedBytes = activeEncoder.encodeInterleaved(
                                    pcmSamples = pcmToEncode,
                                    numSamplesPerChannel = framesToEncode,
                                    mp3Buffer = mp3Buffer,
                                    mp3Offset = 0,
                                    mp3Capacity = mp3Buffer.size,
                                )
                                if (encodedBytes > 0) {
                                    outputStream.write(mp3Buffer, 0, encodedBytes)
                                }

                                if (durationUs > 0) {
                                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
                                    onProgress?.invoke(progress)
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = decoder.outputFormat
                        val newSampleRate = if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        } else sampleRate
                        val newChannels = if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        } else channels

                        if (newSampleRate != sampleRate || newChannels != channels) {
                            sampleRate = newSampleRate
                            channels = newChannels
                            encoder?.close()
                            encoder = createEncoder(sampleRate, channels)
                        }
                    }
                }
            }

            val flushedBytes = encoder?.flush(mp3Buffer, 0, mp3Buffer.size) ?: 0
            if (flushedBytes > 0) {
                outputStream.write(mp3Buffer, 0, flushedBytes)
            }
            outputStream.flush()
            onProgress?.invoke(1f)
            true
        } catch (cancellation: CancellationException) {
            outputFile.delete()
            throw cancellation
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to convert audio to MP3: ${e.message}", e)
            outputFile.delete()
            throw e
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor?.release() }
            runCatching { encoder?.close() }
            runCatching { outputStream?.close() }
        }
    }
}
