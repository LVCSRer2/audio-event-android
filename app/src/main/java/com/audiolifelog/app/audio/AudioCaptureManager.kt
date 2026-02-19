package com.audiolifelog.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Manages audio capture via [AudioRecord] at 16 kHz mono PCM 16-bit.
 *
 * Provides silence-gated window reading so that the downstream pipeline
 * can skip classification when the environment is quiet, saving battery.
 */
@Singleton
class AudioCaptureManager @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 32000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** Number of samples for ~1 second of audio at 32 kHz. */
        const val WINDOW_SAMPLES = 32000
    }

    private var audioRecord: AudioRecord? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    /**
     * Start audio capture. Requires RECORD_AUDIO permission to be granted.
     *
     * @throws SecurityException if RECORD_AUDIO permission is not granted.
     * @throws IllegalStateException if AudioRecord cannot be initialised.
     */
    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, WINDOW_SAMPLES * 2) // 2 bytes per SHORT sample

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        check(record.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialise"
        }

        record.startRecording()
        audioRecord = record
        isRecording = true
    }

    /**
     * Stop audio capture (can be restarted with [start]).
     */
    fun stop() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
    }

    /**
     * Release the [AudioRecord] entirely. After this call, [start] will
     * create a new instance.
     */
    fun release() {
        stop()
        audioRecord?.release()
        audioRecord = null
    }

    /**
     * Read a window of audio samples and return it only if it exceeds the
     * given RMS threshold (silence gate).
     *
     * @param rmsThreshold Minimum RMS amplitude to consider the window as
     *                     non-silent. Values are in the normalised [-1, 1] range.
     *                     A typical starting point is 0.01f.
     * @return A [FloatArray] of [WINDOW_SAMPLES] samples in [-1.0, 1.0],
     *         or null if not recording or the window is below the threshold.
     */
    fun readWindowIfActive(rmsThreshold: Float): FloatArray? {
        val record = audioRecord ?: return null
        if (!isRecording) return null

        val shortBuffer = ShortArray(WINDOW_SAMPLES)
        val samplesRead = record.read(shortBuffer, 0, WINDOW_SAMPLES)
        if (samplesRead < WINDOW_SAMPLES) return null

        // Convert Short PCM to Float [-1.0, 1.0]
        val floatBuffer = FloatArray(samplesRead)
        var sumSquared = 0.0
        for (i in 0 until samplesRead) {
            val sample = shortBuffer[i].toFloat() / Short.MAX_VALUE.toFloat()
            floatBuffer[i] = sample
            sumSquared += sample * sample
        }

        val rms = sqrt(sumSquared / samplesRead).toFloat()
        return if (rms >= rmsThreshold) floatBuffer else null
    }
}
