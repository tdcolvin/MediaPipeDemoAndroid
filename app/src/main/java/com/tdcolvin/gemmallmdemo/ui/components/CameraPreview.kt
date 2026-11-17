package com.tdcolvin.gemmallmdemo.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

//Suppressions needed to manually shut down the cameraProvider when the composable exits the composition
@SuppressLint("RestrictedApi", "VisibleForTests")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    imageCaptureUseCase: ImageCapture? = null,
    imageAnalysisUseCase: ImageAnalysis? = null
) {

    val previewUseCase = remember { androidx.camera.core.Preview.Builder().build() }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }



    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraProvider?.shutdownAsync()
        }
    }

    val localContext = LocalContext.current
    val localLifecycleOwner = LocalLifecycleOwner.current

    fun rebindCameraProvider() {
        val useCases = listOfNotNull(
            previewUseCase,
            imageCaptureUseCase,
            imageAnalysisUseCase
        )

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        Log.i("camerarebind", "camerarebind, prov=$cameraProvider")
        cameraProvider?.bindToLifecycle(
            localLifecycleOwner,
            cameraSelector,
            *useCases.toTypedArray()
        )
    }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(localContext)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            rebindCameraProvider()
        }, ContextCompat.getMainExecutor(localContext))
    }

    AndroidView(modifier = modifier,
        factory = { context ->
            PreviewView(context).also {
                previewUseCase.setSurfaceProvider(it.surfaceProvider)
                rebindCameraProvider()
            }
        }
    )
}