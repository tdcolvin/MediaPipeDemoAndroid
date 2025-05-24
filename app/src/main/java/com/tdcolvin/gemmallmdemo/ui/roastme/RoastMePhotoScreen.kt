package com.tdcolvin.gemmallmdemo.ui.roastme

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcolvin.gemmallmdemo.ui.components.CameraPreview

@Composable
fun RoastMePhotoScreen(
    modifier: Modifier = Modifier,
    setPhoto: (Bitmap) -> Unit,
    viewModel: RoastMePhotoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.image) {
        uiState.image?.let { setPhoto(it) }
    }

    RoastMePhotoScreenContent(
        modifier = modifier,
        onCapturedImage = viewModel::setCapturedImage
    )
}

@Composable
private fun RoastMePhotoScreenContent(
    modifier: Modifier = Modifier,
    onCapturedImage: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current

    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }

    Column(modifier = modifier) {
        CameraPreview(
            modifier = Modifier.fillMaxWidth().weight(1f),
            lensFacing = CameraSelector.LENS_FACING_FRONT,
            imageCaptureUseCase = imageCaptureUseCase
        )
        Button(onClick = {
            imageCaptureUseCase.takePicture(
                context.mainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        super.onCaptureSuccess(image)
                        onCapturedImage(image)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("MainActivity", "Image capture failed", exception)
                    }
                }
            )
        }) {
            Text("Capture")
        }
    }
}