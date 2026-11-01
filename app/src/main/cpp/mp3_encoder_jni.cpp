#include <jni.h>
#include <string>
#include <android/log.h>
#include "lame/include/lame.h"

#define TAG "Mp3EncoderJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_rjnsdev_linklift_app_util_Mp3Encoder_nativeInit(
        JNIEnv *env,
        jobject /* this */,
        jint inSampleRate,
        jint inChannels,
        jint outSampleRate,
        jint outBitrateKbps,
        jint quality,
        jstring title,
        jstring artist
) {
    lame_global_flags *flags = lame_init();
    if (!flags) {
        LOGE("Failed to allocate lame_global_flags");
        return 0;
    }

    lame_set_in_samplerate(flags, inSampleRate);
    lame_set_num_channels(flags, inChannels);
    lame_set_out_samplerate(flags, outSampleRate > 0 ? outSampleRate : inSampleRate);
    lame_set_brate(flags, outBitrateKbps > 0 ? outBitrateKbps : 192);
    lame_set_quality(flags, quality >= 0 && quality <= 9 ? quality : 2); // 2 = high quality, very fast

    if (inChannels == 1) {
        lame_set_mode(flags, MONO);
    } else {
        lame_set_mode(flags, JOINT_STEREO);
    }

    id3tag_init(flags);
    id3tag_add_v2(flags);

    if (title != nullptr) {
        const char *titleStr = env->GetStringUTFChars(title, nullptr);
        if (titleStr) {
            id3tag_set_title(flags, titleStr);
            env->ReleaseStringUTFChars(title, titleStr);
        }
    }

    if (artist != nullptr) {
        const char *artistStr = env->GetStringUTFChars(artist, nullptr);
        if (artistStr) {
            id3tag_set_artist(flags, artistStr);
            env->ReleaseStringUTFChars(artist, artistStr);
        }
    }

    int result = lame_init_params(flags);
    if (result < 0) {
        LOGE("lame_init_params failed with code: %d", result);
        lame_close(flags);
        return 0;
    }

    return reinterpret_cast<jlong>(flags);
}

JNIEXPORT jint JNICALL
Java_com_rjnsdev_linklift_app_util_Mp3Encoder_nativeEncodeInterleaved(
        JNIEnv *env,
        jobject /* this */,
        jlong handle,
        jshortArray pcmSamples,
        jint numSamplesPerChannel,
        jbyteArray mp3Buffer,
        jint mp3Offset,
        jint mp3Capacity
) {
    auto *flags = reinterpret_cast<lame_global_flags *>(handle);
    if (!flags) return -1;

    jshort *pcm = env->GetShortArrayElements(pcmSamples, nullptr);
    if (!pcm) return -1;

    jbyte *mp3 = env->GetByteArrayElements(mp3Buffer, nullptr);
    if (!mp3) {
        env->ReleaseShortArrayElements(pcmSamples, pcm, JNI_ABORT);
        return -1;
    }

    int bytesEncoded = lame_encode_buffer_interleaved(
            flags,
            pcm,
            numSamplesPerChannel,
            reinterpret_cast<unsigned char *>(mp3 + mp3Offset),
            mp3Capacity
    );

    env->ReleaseByteArrayElements(mp3Buffer, mp3, 0);
    env->ReleaseShortArrayElements(pcmSamples, pcm, JNI_ABORT);

    return bytesEncoded;
}

JNIEXPORT jint JNICALL
Java_com_rjnsdev_linklift_app_util_Mp3Encoder_nativeFlush(
        JNIEnv *env,
        jobject /* this */,
        jlong handle,
        jbyteArray mp3Buffer,
        jint mp3Offset,
        jint mp3Capacity
) {
    auto *flags = reinterpret_cast<lame_global_flags *>(handle);
    if (!flags) return -1;

    jbyte *mp3 = env->GetByteArrayElements(mp3Buffer, nullptr);
    if (!mp3) return -1;

    int bytesFlushed = lame_encode_flush(
            flags,
            reinterpret_cast<unsigned char *>(mp3 + mp3Offset),
            mp3Capacity
    );

    env->ReleaseByteArrayElements(mp3Buffer, mp3, 0);

    return bytesFlushed;
}

JNIEXPORT void JNICALL
Java_com_rjnsdev_linklift_app_util_Mp3Encoder_nativeClose(
        JNIEnv */* env */,
        jobject /* this */,
        jlong handle
) {
    auto *flags = reinterpret_cast<lame_global_flags *>(handle);
    if (flags) {
        lame_close(flags);
    }
}

} // extern "C"
