package com.tdcolvin.gemmallmdemo.ui.paintme

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcolvin.gemmallmdemo.ui.components.CameraPreview

@Composable
fun PaintMePhotoScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    viewModel: PaintMePhotoViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    PaintMePhotoScreenContent(
        modifier = modifier,
        stylizedImage = uiState.value.stylizedImage,
        onClose = onClose,
        onRetry = viewModel::removeCapturedImage,
        onCapturedImage = viewModel::setCapturedImage
    )
}

@Composable
private fun PaintMePhotoScreenContent(
    modifier: Modifier = Modifier,
    stylizedImage: Bitmap?,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onCapturedImage: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current

    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }

    Column(modifier = modifier) {
        if (stylizedImage == null) {
            CameraPreview(
                modifier = Modifier.fillMaxWidth().weight(1f),
                lensFacing = CameraSelector.LENS_FACING_FRONT,
                imageCaptureUseCase = imageCaptureUseCase
            )
        }
        else {
            Image(
                modifier = Modifier.fillMaxWidth().weight(1f),
                bitmap = stylizedImage.asImageBitmap(),
                contentDescription = null
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (stylizedImage == null) {
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
            else {
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }

            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}