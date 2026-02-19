package com.audiolifelog.app.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

@Singleton
class OnnxClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Closeable {

    companion object {
        private const val TAG = "OnnxClassifier"
        private const val MODEL_ASSET_PATH = "mn05_as.onnx"
        private const val MIN_CONFIDENCE_DEFAULT = 0.15f
    }

    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    private var modelAvailable = false

    init {
        try {
            val modelBytes = context.assets.open(MODEL_ASSET_PATH).use { it.readBytes() }
            val sessionOptions = OrtSession.SessionOptions().apply {
                try {
                    addNnapi()
                } catch (_: Exception) {
                    // NNAPI not available; fall back to CPU
                }
            }
            ortSession = ortEnvironment.createSession(modelBytes, sessionOptions)
            modelAvailable = true
            Log.i(TAG, "ONNX model loaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "ONNX model not found or failed to load: ${e.message}. Classifier will return empty results.")
            modelAvailable = false
        }
    }

    fun isModelLoaded(): Boolean = modelAvailable

    fun classify(
        melSpectrogram: Array<FloatArray>,
        minConfidence: Float = MIN_CONFIDENCE_DEFAULT
    ): List<ClassificationResult> {
        val session = ortSession ?: return emptyList()

        val nMels = melSpectrogram.size
        val timeFrames = if (nMels > 0) melSpectrogram[0].size else 0
        if (nMels == 0 || timeFrames == 0) return emptyList()

        val totalElements = nMels * timeFrames
        val flatData = FloatBuffer.allocate(totalElements)
        for (mel in 0 until nMels) {
            flatData.put(melSpectrogram[mel])
        }
        flatData.rewind()

        val inputShape = longArrayOf(1L, 1L, nMels.toLong(), timeFrames.toLong())
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, flatData, inputShape)

        val inputName = session.inputNames.first()
        val results = session.run(mapOf(inputName to inputTensor))

        // Model may have multiple outputs. Find the one with 527 classes (logits).
        var logitsOutput: Any? = null
        for (i in 0 until results.size()) {
            val value = results[i].value
            val size = when (value) {
                is FloatArray -> value.size
                is Array<*> -> (value as? Array<FloatArray>)?.getOrNull(0)?.size ?: 0
                else -> 0
            }
            Log.d(TAG, "Output[$i] type: ${value?.javaClass?.name}, size: $size")
            if (size == 527) {
                logitsOutput = value
                break
            }
        }
        if (logitsOutput == null) {
            // Fallback: use first output
            logitsOutput = results[0].value
        }
        val rawValue = logitsOutput
        Log.d(TAG, "Using output with type: ${rawValue?.javaClass?.name}, outputs count: ${results.size()}")

        val logits: FloatArray = when (rawValue) {
            is Array<*> -> {
                @Suppress("UNCHECKED_CAST")
                try {
                    (rawValue as Array<FloatArray>)[0]
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cast Array output: ${e.message}")
                    inputTensor.close()
                    results.close()
                    return emptyList()
                }
            }
            is FloatArray -> rawValue
            else -> {
                Log.w(TAG, "Unexpected output type: ${rawValue?.javaClass?.name}")
                inputTensor.close()
                results.close()
                return emptyList()
            }
        }

        inputTensor.close()
        results.close()

        Log.d(TAG, "Logits size: ${logits.size}, top5 raw: ${logits.sortedDescending().take(5)}")

        val labels = AudioSetLabels.load(context)

        val classificationResults = mutableListOf<ClassificationResult>()
        for (i in logits.indices) {
            val confidence = sigmoid(logits[i])
            if (confidence >= minConfidence) {
                val label = if (i < labels.size) labels[i] else "Unknown_$i"
                classificationResults.add(ClassificationResult(label, confidence))
            }
        }

        classificationResults.sortByDescending { it.confidence }
        if (classificationResults.isNotEmpty()) {
            Log.i(TAG, "Detected ${classificationResults.size} events, top: ${classificationResults.first().label} (${classificationResults.first().confidence})")
        }
        return classificationResults
    }

    private fun sigmoid(x: Float): Float {
        return (1.0f / (1.0f + exp(-x)))
    }

    override fun close() {
        ortSession?.close()
    }
}
