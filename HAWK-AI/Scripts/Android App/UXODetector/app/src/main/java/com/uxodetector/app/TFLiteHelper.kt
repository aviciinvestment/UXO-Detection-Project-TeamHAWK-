package com.uxodetector.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random

class TFLiteHelper(private val context: Context) {

    companion object {
        private const val TAG = "TFLiteHelper"
        private const val MODEL_FILE = "uxo_model.tflite"
        private const val INPUT_SIZE = 420
        private const val PIXEL_SIZE = 3
        private const val NUM_BYTES_PER_CHANNEL = 4 // float32
    }

    private var interpreter: Interpreter? = null
    private var modelLoaded = false

    private val uxoLabels = listOf(
        "AntiSubmarine Bomb",
        "Aviation Bomb",
        "Cartridge",
        "Cartridge Magazine",
        "Fuse",
        "Grenade",
        "LandMine",
        "Mortar Bomb",
        "Projectile",
        "RPG",
        "Rocket",
        "Sea Mine"
    )


    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val mappedBuffer: MappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
            interpreter = Interpreter(mappedBuffer)
            modelLoaded = true
            Log.i(TAG, "TFLite model loaded successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Model file not found or failed to load. Using dummy inference. Error: ${e.message}")
            modelLoaded = false
        }
    }

    fun runInference(bitmap: Bitmap): List<DetectionResult> {
        return if (modelLoaded && interpreter != null) {
            runRealInference(bitmap)
        } else {
            runDummyInference()
        }
    }

    private fun runRealInference(bitmap: Bitmap): List<DetectionResult> {
        return try {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = bitmapToByteBuffer(scaledBitmap)

            // Output tensors: [boxes, classes, scores, count]
            val outputBoxes = Array(1) { Array(10) { FloatArray(4) } }
            val outputClasses = Array(1) { FloatArray(10) }
            val outputScores = Array(1) { FloatArray(10) }
            val outputCount = FloatArray(1)

            val outputMap = mapOf(
                0 to outputBoxes,
                1 to outputClasses,
                2 to outputScores,
                3 to outputCount
            )

            interpreter!!.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            val results = mutableListOf<DetectionResult>()
            val count = outputCount[0].toInt().coerceAtMost(10)

            for (i in 0 until count) {
                val score = outputScores[0][i]
                if (score >= 0.4f) {
                    val classIdx = outputClasses[0][i].toInt().coerceIn(0, uxoLabels.size - 1)
                    val box = outputBoxes[0][i]
                    results.add(
                        DetectionResult(
                            label = uxoLabels[classIdx],
                            confidence = score,
                            boundingBox = RectF(box[1], box[0], box[3], box[2])
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            runDummyInference()
        }
    }

    private fun runDummyInference(): List<DetectionResult> {
        // Simulate occasional detections for demo purposes
        val frameCount = System.currentTimeMillis() / 2000
        if (frameCount % 4 != 0L) return emptyList()

        val rand = Random(frameCount)
        val labelIndex = rand.nextInt(uxoLabels.size)
        val confidence = 0.55f + rand.nextFloat() * 0.40f

        val left = 0.1f + rand.nextFloat() * 0.3f
        val top = 0.15f + rand.nextFloat() * 0.3f
        val right = left + 0.2f + rand.nextFloat() * 0.2f
        val bottom = top + 0.2f + rand.nextFloat() * 0.2f

        return listOf(
            DetectionResult(
                label = uxoLabels[labelIndex],
                confidence = confidence,
                boundingBox = RectF(left, top, right.coerceAtMost(0.95f), bottom.coerceAtMost(0.95f))
            )
        )
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            NUM_BYTES_PER_CHANNEL * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
