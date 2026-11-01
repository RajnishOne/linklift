package com.rjnsdev.linklift.app.util

import java.io.Closeable

class Mp3Encoder(
    inSampleRate: Int,
    inChannels: Int,
    outSampleRate: Int = inSampleRate,
    outBitrateKbps: Int = 192,
    quality: Int = 2,
    title: String? = null,
    artist: String? = null,
) : Closeable {

    companion object {
        @Volatile
        private var isLoaded: Boolean = false

        init {
            try {
                System.loadLibrary("linklift-mp3")
                isLoaded = true
            } catch (t: Throwable) {
                isLoaded = false
            }
        }

        fun isNativeAvailable(): Boolean = isLoaded
    }

    private var handle: Long = 0L

    init {
        check(isLoaded) { "Native library linklift-mp3 is not loaded" }
        handle = nativeInit(
            inSampleRate = inSampleRate,
            inChannels = inChannels,
            outSampleRate = outSampleRate,
            outBitrateKbps = outBitrateKbps,
            quality = quality,
            title = title,
            artist = artist,
        )
        check(handle != 0L) { "Failed to initialize LAME MP3 encoder" }
    }

    fun encodeInterleaved(
        pcmSamples: ShortArray,
        numSamplesPerChannel: Int,
        mp3Buffer: ByteArray,
        mp3Offset: Int = 0,
        mp3Capacity: Int = mp3Buffer.size - mp3Offset,
    ): Int {
        check(handle != 0L) { "Encoder is closed" }
        return nativeEncodeInterleaved(
            handle = handle,
            pcmSamples = pcmSamples,
            numSamplesPerChannel = numSamplesPerChannel,
            mp3Buffer = mp3Buffer,
            mp3Offset = mp3Offset,
            mp3Capacity = mp3Capacity,
        )
    }

    fun flush(
        mp3Buffer: ByteArray,
        mp3Offset: Int = 0,
        mp3Capacity: Int = mp3Buffer.size - mp3Offset,
    ): Int {
        check(handle != 0L) { "Encoder is closed" }
        return nativeFlush(
            handle = handle,
            mp3Buffer = mp3Buffer,
            mp3Offset = mp3Offset,
            mp3Capacity = mp3Capacity,
        )
    }

    override fun close() {
        if (handle != 0L) {
            val toClose = handle
            handle = 0L
            nativeClose(toClose)
        }
    }

    private external fun nativeInit(
        inSampleRate: Int,
        inChannels: Int,
        outSampleRate: Int,
        outBitrateKbps: Int,
        quality: Int,
        title: String?,
        artist: String?,
    ): Long

    private external fun nativeEncodeInterleaved(
        handle: Long,
        pcmSamples: ShortArray,
        numSamplesPerChannel: Int,
        mp3Buffer: ByteArray,
        mp3Offset: Int,
        mp3Capacity: Int,
    ): Int

    private external fun nativeFlush(
        handle: Long,
        mp3Buffer: ByteArray,
        mp3Offset: Int,
        mp3Capacity: Int,
    ): Int

    private external fun nativeClose(handle: Long)
}
