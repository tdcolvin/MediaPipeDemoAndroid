package com.tdcolvin.gemmallmdemo.ui.roastme

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoastMePhotoUiState(
    val image: Bitmap? = null
)

class RoastMePhotoViewModel: ViewModel() {
    val uiState = MutableStateFlow(RoastMePhotoUiState())

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

            uiState.update { it.copy(image = bmp) }
        }
    }

}