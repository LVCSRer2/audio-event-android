package com.audiolifelog.app.audio

import android.util.Log
import com.audiolifelog.app.data.db.entity.AudioEventEntity
import com.audiolifelog.app.data.repository.AudioEventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full audio classification pipeline:
 *
 *   [AudioCaptureManager] -> [MelSpectrogramComputer] -> [OnnxClassifier]
 *
 * Detected events are accumulated in a batch and periodically flushed to
 * [AudioEventRepository]. A [SharedFlow] of [ClassificationResult] is
 * exposed so that the UI layer can observe live detections.
 */
@Singleton
class AudioClassificationPipeline @Inject constructor(
    private val captureManager: AudioCaptureManager,
    private val spectrogramComputer: MelSpectrogramComputer,
    private val classifier: OnnxClassifier,
    private val repository: AudioEventRepository
) {

    companion object {
        private const val TAG = "AudioPipeline"

        /** RMS threshold below which a window is considered silent. */
        private const val RMS_THRESHOLD = 0.01f

        /** How often to flush accumulated events to the database (ms). */
        private const val FLUSH_INTERVAL_MS = 2_000L

        /** Small delay between capture reads to avoid busy-looping (ms). */
        private const val READ_INTERVAL_MS = 50L
    }

    private val _classificationFlow = MutableSharedFlow<ClassificationResult>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Observe live classification results from the pipeline. */
    val classificationFlow: SharedFlow<ClassificationResult> = _classificationFlow.asSharedFlow()

    private var processingJob: Job? = null
    private var flushJob: Job? = null

    private val eventBatch = mutableListOf<AudioEventEntity>()
    private val batchLock = Any()

    /**
     * Start the classification loop.
     *
     * @param scope A [CoroutineScope] whose lifetime controls the pipeline.
     *              Typically the service or ViewModel scope.
     */
    fun start(scope: CoroutineScope) {
        if (processingJob?.isActive == true) return

        captureManager.start()

        processingJob = scope.launch {
            Log.i(TAG, "Pipeline started")
            while (isActive) {
                try {
                    processOneWindow()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing window", e)
                }
                delay(READ_INTERVAL_MS)
            }
        }

        flushJob = scope.launch {
            while (isActive) {
                delay(FLUSH_INTERVAL_MS)
                flushBatch()
            }
        }
    }

    /**
     * Stop the classification loop and flush remaining events.
     */
    fun stop() {
        processingJob?.cancel()
        processingJob = null
        flushJob?.cancel()
        flushJob = null
        captureManager.stop()
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private suspend fun processOneWindow() {
        val samples = captureManager.readWindowIfActive(RMS_THRESHOLD) ?: return

        val melSpectrogram = spectrogramComputer.compute(samples)
        val results = classifier.classify(melSpectrogram)

        if (results.isEmpty()) return

        // 1-best: confidence가 가장 높은 결과만 사용
        val best = results.first()

        val now = System.currentTimeMillis()
        val durationMs = (AudioCaptureManager.WINDOW_SAMPLES * 1000L) / AudioCaptureManager.SAMPLE_RATE

        _classificationFlow.tryEmit(best)

        val entity = AudioEventEntity(
            timestamp = now,
            eventLabel = best.label,
            confidence = best.confidence,
            durationMs = durationMs
        )
        synchronized(batchLock) {
            eventBatch.add(entity)
        }
    }

    private suspend fun flushBatch() {
        val toFlush: List<AudioEventEntity>
        synchronized(batchLock) {
            if (eventBatch.isEmpty()) return
            toFlush = eventBatch.toList()
            eventBatch.clear()
        }

        try {
            repository.insertAll(toFlush)
            Log.d(TAG, "Flushed ${toFlush.size} events to repository")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush events", e)
            // Re-add events so they are not lost
            synchronized(batchLock) {
                eventBatch.addAll(0, toFlush)
            }
        }
    }
}
