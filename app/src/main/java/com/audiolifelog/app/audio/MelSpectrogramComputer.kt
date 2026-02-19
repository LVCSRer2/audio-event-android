package com.audiolifelog.app.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin mel spectrogram computation matching EfficientAT's AugmentMelSTFT preprocessing.
 *
 * Key differences from standard librosa-style mel:
 * - Pre-emphasis filter: [−0.97, 1]
 * - Power spectrogram (magnitude²)
 * - Kaldi-style mel filterbank with fmax = sr/2 − 1000
 * - Normalization: (log(x + 1e-5) + 4.5) / 5.0
 */
@Singleton
class MelSpectrogramComputer @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 32000
        const val N_FFT = 1024
        const val HOP_LENGTH = 320
        const val WIN_LENGTH = 800
        const val N_MELS = 128
        private const val F_MIN = 0f
        private const val F_MAX = 15000f // sr/2 - 1000
    }

    private val hannWindow: FloatArray = createHannWindow(WIN_LENGTH)
    private val melFilterbank: Array<FloatArray> = createMelFilterbank(
        nMels = N_MELS,
        nFft = N_FFT,
        sampleRate = SAMPLE_RATE,
        fMin = F_MIN,
        fMax = F_MAX
    )

    /**
     * Compute the mel spectrogram for the given audio samples.
     *
     * @param samples Raw audio samples in [-1.0, 1.0] range at 32 kHz.
     * @return Mel spectrogram of shape [N_MELS x timeFrames], normalized.
     */
    fun compute(samples: FloatArray): Array<FloatArray> {
        // Step 1: Pre-emphasis filter: y[n] = x[n] - 0.97 * x[n-1]
        val preemphasized = FloatArray(samples.size - 1)
        for (i in 1 until samples.size) {
            preemphasized[i - 1] = samples[i] - 0.97f * samples[i - 1]
        }

        // Step 2: Center-padded STFT
        val padLength = N_FFT / 2
        val padded = FloatArray(preemphasized.size + 2 * padLength)
        // Reflect padding at edges
        for (i in 0 until padLength) {
            padded[i] = preemphasized[padLength - i]
        }
        System.arraycopy(preemphasized, 0, padded, padLength, preemphasized.size)
        for (i in 0 until padLength) {
            val srcIdx = preemphasized.size - 2 - i
            if (srcIdx >= 0) padded[padLength + preemphasized.size + i] = preemphasized[srcIdx]
        }

        val numFrames = 1 + (padded.size - N_FFT) / HOP_LENGTH
        if (numFrames <= 0) {
            return Array(N_MELS) { floatArrayOf() }
        }

        val melSpectrogram = Array(N_MELS) { FloatArray(numFrames) }

        val fftReal = FloatArray(N_FFT)
        val fftImag = FloatArray(N_FFT)
        val powerSpec = FloatArray(N_FFT / 2 + 1)

        for (frame in 0 until numFrames) {
            val offset = frame * HOP_LENGTH

            fftReal.fill(0f)
            fftImag.fill(0f)

            // Apply Hann window
            for (i in 0 until N_FFT) {
                val sampleIdx = offset + i
                fftReal[i] = if (sampleIdx < padded.size && i < WIN_LENGTH) {
                    padded[sampleIdx] * hannWindow[i]
                } else if (i < WIN_LENGTH && sampleIdx < padded.size) {
                    padded[sampleIdx] * hannWindow[i]
                } else {
                    0f
                }
            }

            // Radix-2 FFT
            fft(fftReal, fftImag, N_FFT)

            // Power spectrum (magnitude²)
            for (k in 0 until powerSpec.size) {
                powerSpec[k] = fftReal[k] * fftReal[k] + fftImag[k] * fftImag[k]
            }

            // Apply mel filterbank, log, and normalize
            for (mel in 0 until N_MELS) {
                var sum = 0f
                val filter = melFilterbank[mel]
                for (k in filter.indices) {
                    sum += filter[k] * powerSpec[k]
                }
                // EfficientAT normalization: (log(x + 1e-5) + 4.5) / 5.0
                melSpectrogram[mel][frame] = (ln(max(1e-5f, sum)) + 4.5f) / 5.0f
            }
        }

        return melSpectrogram
    }

    private fun createHannWindow(length: Int): FloatArray {
        // periodic=False (symmetric Hann window)
        return FloatArray(length) { n ->
            (0.5 * (1.0 - cos(2.0 * PI * n / (length - 1)))).toFloat()
        }
    }

    private fun hzToMel(hz: Float): Float {
        return 2595f * log10(1f + hz / 700f)
    }

    private fun melToHz(mel: Float): Float {
        return 700f * (10f.pow(mel / 2595f) - 1f)
    }

    private fun createMelFilterbank(nMels: Int, nFft: Int, sampleRate: Int, fMin: Float, fMax: Float): Array<FloatArray> {
        val numBins = nFft / 2 + 1
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)

        val melPoints = FloatArray(nMels + 2) { i ->
            melMin + i * (melMax - melMin) / (nMels + 1)
        }

        val hzPoints = FloatArray(melPoints.size) { melToHz(melPoints[it]) }

        val binPoints = FloatArray(hzPoints.size) { i ->
            (nFft + 1).toFloat() * hzPoints[i] / sampleRate
        }

        val filterbank = Array(nMels) { FloatArray(numBins) }

        for (m in 0 until nMels) {
            val startBin = binPoints[m]
            val centerBin = binPoints[m + 1]
            val endBin = binPoints[m + 2]

            for (k in 0 until numBins) {
                val kf = k.toFloat()
                filterbank[m][k] = when {
                    kf < startBin -> 0f
                    kf <= centerBin && centerBin != startBin -> (kf - startBin) / (centerBin - startBin)
                    kf <= endBin && endBin != centerBin -> (endBin - kf) / (endBin - centerBin)
                    else -> 0f
                }
            }
        }

        return filterbank
    }

    private fun fft(real: FloatArray, imag: FloatArray, n: Int) {
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]; real[i] = real[j]; real[j] = temp
                temp = imag[i]; imag[i] = imag[j]; imag[j] = temp
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        var step = 2
        while (step <= n) {
            val halfStep = step / 2
            val angleStep = -2.0 * PI / step

            for (i in 0 until n step step) {
                for (k in 0 until halfStep) {
                    val angle = angleStep * k
                    val twiddleReal = cos(angle).toFloat()
                    val twiddleImag = sin(angle).toFloat()

                    val evenIdx = i + k
                    val oddIdx = i + k + halfStep

                    val tReal = twiddleReal * real[oddIdx] - twiddleImag * imag[oddIdx]
                    val tImag = twiddleReal * imag[oddIdx] + twiddleImag * real[oddIdx]

                    real[oddIdx] = real[evenIdx] - tReal
                    imag[oddIdx] = imag[evenIdx] - tImag
                    real[evenIdx] = real[evenIdx] + tReal
                    imag[evenIdx] = imag[evenIdx] + tImag
                }
            }
            step *= 2
        }
    }
}
