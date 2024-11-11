package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.dicoding.asclepius.R
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifierHelper(
    private val confidence: Float = 0.5f,
    private val results: Int = 5,
    private val file: String = "cancer_classification.tflite",
    val context: Context
) {
    private var classifier: ImageClassifier? = null

    init {
        initializeClassifier()
    }

    private fun initializeClassifier() {
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(confidence)
            .setMaxResults(results)
            .setBaseOptions(BaseOptions.builder().setNumThreads(4).build())
            .build()

        try {
            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                file,
                options
            )
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.image_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun classifyImage(uri: Uri, onResult: (String, Float) -> Unit) {
        val bitmap = getBitmapFromUri(uri)
        val tensorImage = preprocessImage(bitmap)

        val results = classifier?.classify(tensorImage)
        if (!results.isNullOrEmpty()) {
            val topCategory = results[0].categories[0]
            onResult(topCategory.label, topCategory.score)
        } else {
            onResult("No results", 0f)
        }
    }

    @Suppress("DEPRECATION")
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val processor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .build()

        return processor.process(TensorImage.fromBitmap(bitmap))
    }
}
