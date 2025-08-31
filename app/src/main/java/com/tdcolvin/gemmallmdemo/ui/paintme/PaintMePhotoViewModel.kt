package com.tdcolvin.gemmallmdemo.ui.paintme

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facestylizer.FaceStylizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import androidx.core.graphics.createBitmap

data class PaintMePhotoUiState(
    val stylizedImage: Bitmap? = null
)

class PaintMePhotoViewModel(application: Application): AndroidViewModel(application) {
    val uiState = MutableStateFlow(PaintMePhotoUiState())

    fun setCapturedImage(image: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val maxDim = 1000

            val (newWidth, newHeight) = if (image.width > image.height) {
                Pair(maxDim, image.height * maxDim / image.width)
            } else {
                Pair(image.width * maxDim / image.height, maxDim)
            }

            val scaleAndRotate = Matrix().apply {
                postScale(newWidth.toFloat() / image.width, newHeight.toFloat() / image.height)
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }

            val bmp = Bitmap.createBitmap(image.toBitmap(), 0, 0, image.width, image.height, scaleAndRotate, true)

            image.close()

            withContext(Dispatchers.IO) {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_stylizer_oil_painting.task")
                    .build()

                val faceStylizerOptions = FaceStylizer.FaceStylizerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .build()

                val faceStylizer = FaceStylizer.createFromOptions(getApplication(), faceStylizerOptions)

                val stylizeResult = faceStylizer.stylize(BitmapImageBuilder(bmp).build())
                val stylizedImage = stylizeResult.stylizedImage().getOrNull() ?: return@withContext

                val byteBuffer = ByteBufferExtractor.extract(stylizedImage)
                val width = stylizedImage.width
                val height = stylizedImage.height

                val bitmap = createBitmap(width, height)
                bitmap.copyPixelsFromBuffer(byteBuffer)
                uiState.update { it.copy(stylizedImage = bitmap) }
            }
        }
    }

    fun removeCapturedImage() {
        uiState.update { it.copy(stylizedImage = null) }
    }
}