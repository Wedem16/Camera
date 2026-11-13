package com.example.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.model.FlashSetting
import com.example.model.HardwareCapabilities
import com.example.model.ProCameraSettings
import com.example.model.VideoFpsSetting
import com.example.model.VideoResolutionSetting
import com.example.model.WhiteBalancePreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CameraHardwareCtrl"

@OptIn(ExperimentalCamera2Interop::class)
class CameraHardwareController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var boundPreviewView: PreviewView? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        flashSetting: FlashSetting,
        videoResolution: VideoResolutionSetting,
        videoFps: VideoFpsSetting,
        videoStabilizationEnabled: Boolean,
        proSettings: ProCameraSettings,
        frameAnalyzer: ImageAnalysis.Analyzer,
        onHardwareInspected: (HardwareCapabilities) -> Unit,
        onZoomChanged: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        boundPreviewView = previewView
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                provider.unbindAll()

                val hasBack = runCatching {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                }.getOrDefault(true)
                val hasFront = runCatching {
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                }.getOrDefault(false)

                val resolvedLens = when {
                    lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
                    hasBack -> CameraSelector.LENS_FACING_BACK
                    hasFront -> CameraSelector.LENS_FACING_FRONT
                    else -> CameraSelector.LENS_FACING_BACK
                }

                val selector = CameraSelector.Builder()
                    .requireLensFacing(resolvedLens)
                    .build()

                // 1. Preview UseCase
                val previewBuilder = Preview.Builder()
                Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(videoFps.fps, videoFps.fps)
                )
                val preview = previewBuilder.build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 2. ImageCapture UseCase
                val captureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                val imgCapture = ImageCapture.Builder()
                    .setCaptureMode(captureMode)
                    .setFlashMode(mapFlashMode(flashSetting))
                    .build()
                imageCapture = imgCapture

                // 3. ImageAnalysis UseCase (for 4Hz AI Scene Optimization & Histogram)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, frameAnalyzer)
                    }

                // 4. VideoCapture UseCase
                val preferredQuality = if (videoResolution == VideoResolutionSetting.UHD_4K) {
                    Quality.UHD
                } else {
                    Quality.FHD
                }
                val qualitySelector = QualitySelector.from(
                    preferredQuality,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                val vidCapture = VideoCapture.withOutput(recorder)
                videoCapture = vidCapture

                // Bind UseCases (if all 4 exceed hardware stream limits on legacy devices, fall back to 3)
                val camera = try {
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imgCapture,
                        vidCapture,
                        imageAnalysis
                    )
                } catch (streamEx: IllegalArgumentException) {
                    Log.w(TAG, "4-stream bind exceeded device surface limit, binding core 3 streams", streamEx)
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imgCapture,
                        vidCapture
                    )
                }

                boundCamera = camera

                // Inspect real hardware capabilities
                val capabilities = inspectCapabilities(camera, hasBack, hasFront)
                onHardwareInspected(capabilities)

                // Observe zoom changes
                camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    if (zoomState != null) {
                        onZoomChanged(zoomState.zoomRatio)
                    }
                }

                // Apply initial Pro / Stabilization settings
                applyCamera2Parameters(proSettings, videoStabilizationEnabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera hardware", e)
                onError(e.localizedMessage ?: "Camera hardware initialization failed.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun inspectCapabilities(
        camera: Camera,
        hasRear: Boolean,
        hasFront: Boolean
    ): HardwareCapabilities {
        val info = camera.cameraInfo
        val zoomState = info.zoomState.value
        val minZoom = zoomState?.minZoomRatio ?: 1.0f
        val maxZoom = (zoomState?.maxZoomRatio ?: 8.0f).coerceAtLeast(2.0f)

        val videoCaps = runCatching { Recorder.getVideoCapabilities(info) }.getOrNull()
        val supportedQualities = videoCaps?.getSupportedQualities(androidx.camera.core.DynamicRange.SDR).orEmpty()
        val supports4K = supportedQualities.contains(Quality.UHD)

        val camera2Info = runCatching { Camera2CameraInfo.from(info) }.getOrNull()
        val hwLevel = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        ) ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED

        val hwLevelLabel = when (hwLevel) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            else -> "LEGACY"
        }

        val supportsManualSensor = hwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
            hwLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3

        val minFocusDist = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
        ) ?: 10.0f

        val stabModes = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
        )
        val supportsStab = stabModes?.any {
            it == CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
        } ?: true

        val fpsRanges = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
        )
        val supports60Fps = fpsRanges?.any { it.upper >= 60 } ?: false

        val expState = info.exposureState
        val expRange = expState.exposureCompensationRange
        val expStep = expState.exposureCompensationStep.toFloat().let { if (it <= 0f) 0.25f else it }

        val presets = buildList {
            add(0.5f)
            add(1.0f)
            add(2.0f)
            add(5.0f)
            add(10.0f)
        }

        return HardwareCapabilities(
            hasRearCamera = hasRear,
            hasFrontCamera = hasFront,
            hasFlashUnit = info.hasFlashUnit(),
            minZoomRatio = minZoom,
            maxZoomRatio = maxZoom,
            zoomPresets = presets,
            supportsUltraWideOptical = minZoom < 0.9f,
            supports4K = supports4K,
            supports60Fps = supports60Fps,
            supportsVideoStabilization = supportsStab,
            supportsManualSensor = supportsManualSensor,
            minExposureIndex = expRange.lower,
            maxExposureIndex = expRange.upper,
            exposureStepEv = expStep,
            minFocusDistanceDiopters = minFocusDist.coerceAtLeast(1.0f),
            hardwareLevelLabel = hwLevelLabel
        )
    }

    fun setZoomRatio(requestedRatio: Float) {
        val camera = boundCamera ?: return
        val zoomState = camera.cameraInfo.zoomState.value
        val minZ = zoomState?.minZoomRatio ?: 1.0f
        val maxZ = zoomState?.maxZoomRatio ?: 10.0f
        val clamped = requestedRatio.coerceIn(minZ, maxZ)
        camera.cameraControl.setZoomRatio(clamped)
    }

    fun setFlashSetting(setting: FlashSetting) {
        imageCapture?.flashMode = mapFlashMode(setting)
    }

    fun setTorchEnabled(enabled: Boolean) {
        val camera = boundCamera ?: return
        if (camera.cameraInfo.hasFlashUnit()) {
            camera.cameraControl.enableTorch(enabled)
        }
    }

    fun triggerTapToFocus(x: Float, y: Float) {
        val camera = boundCamera ?: return
        val previewView = boundPreviewView ?: return
        try {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()
            camera.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w(TAG, "Tap to focus failed: ${e.message}")
        }
    }

    fun setExposureCompensation(index: Int) {
        val camera = boundCamera ?: return
        val range = camera.cameraInfo.exposureState.exposureCompensationRange
        val clamped = index.coerceIn(range.lower, range.upper)
        camera.cameraControl.setExposureCompensationIndex(clamped)
    }

    fun applyCamera2Parameters(
        proSettings: ProCameraSettings,
        videoStabilizationEnabled: Boolean
    ) {
        val camera = boundCamera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(camera.cameraControl)
            val optionsBuilder = CaptureRequestOptions.Builder()

            // White Balance
            val awbMode = when (proSettings.whiteBalance) {
                WhiteBalancePreset.AUTO -> CaptureRequest.CONTROL_AWB_MODE_AUTO
                WhiteBalancePreset.DAYLIGHT -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                WhiteBalancePreset.CLOUDY -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                WhiteBalancePreset.INCANDESCENT -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                WhiteBalancePreset.FLUORESCENT -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                WhiteBalancePreset.MANUAL_KELVIN -> {
                    when {
                        proSettings.customKelvin < 3600 -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                        proSettings.customKelvin < 4600 -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
                        proSettings.customKelvin < 5800 -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                        else -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                    }
                }
            }
            optionsBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, awbMode)

            // Manual Focus
            if (proSettings.manualFocusDistance != null) {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                val maxDiopters = 10.0f
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    (proSettings.manualFocusDistance * maxDiopters).coerceIn(0f, maxDiopters)
                )
            } else {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

            // Manual ISO & Shutter Speed
            if (proSettings.iso != null || proSettings.shutterSpeedNs != null) {
                proSettings.iso?.let {
                    optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
                }
                proSettings.shutterSpeedNs?.let {
                    optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
                }
            }

            // Video Stabilization
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (videoStabilizationEnabled) {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                } else {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                }
            )

            camera2Control.setCaptureRequestOptions(optionsBuilder.build())
            setExposureCompensation(proSettings.exposureCompensationIndex)
        } catch (e: Exception) {
            Log.w(TAG, "Camera2 parameter override partially unsupported: ${e.message}")
        }
    }

    /**
     * Captures a high-resolution photo from ImageCapture (or falls back to the live PreviewView
     * frame if the emulator camera stream times out).
     */
    fun capturePhotoBitmap(
        isFrontLens: Boolean,
        onBitmapCaptured: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            val previewBmp = boundPreviewView?.bitmap
            if (previewBmp != null) {
                onBitmapCaptured(previewBmp)
            } else {
                onError("Camera is still initializing.")
            }
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rawBitmap = image.toBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        val matrix = Matrix().apply {
                            if (rotation != 0) postRotate(rotation.toFloat())
                            if (isFrontLens) postScale(-1f, 1f)
                        }
                        val oriented = if (rotation != 0 || isFrontLens) {
                            Bitmap.createBitmap(
                                rawBitmap,
                                0,
                                0,
                                rawBitmap.width,
                                rawBitmap.height,
                                matrix,
                                true
                            )
                        } else {
                            rawBitmap
                        }
                        onBitmapCaptured(oriented)
                    } catch (e: Exception) {
                        Log.e(TAG, "Bitmap conversion failed, falling back to viewfinder bitmap", e)
                        boundPreviewView?.bitmap?.let(onBitmapCaptured)
                            ?: onError("Failed to decode captured photo.")
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "ImageCapture failed (${exception.message}), using live viewfinder frame")
                    val fallback = boundPreviewView?.bitmap
                    if (fallback != null) {
                        onBitmapCaptured(fallback)
                    } else {
                        onError(exception.localizedMessage ?: "Failed to capture photo.")
                    }
                }
            }
        )
    }

    fun grabViewfinderBitmap(): Bitmap? {
        return try {
            boundPreviewView?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        enableAudio: Boolean,
        onEvent: (VideoRecordStatus) -> Unit
    ) {
        val vidCap = videoCapture ?: run {
            onEvent(VideoRecordStatus.Error("Video recorder is not ready."))
            return
        }

        if (activeRecording != null) {
            stopVideoRecording()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "GALAXY_VID_$timestamp")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GalaxyHDCamera")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        var pending = vidCap.output.prepareRecording(context, mediaStoreOutputOptions)
        if (enableAudio) {
            pending = pending.withAudioEnabled()
        }

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onEvent(VideoRecordStatus.Recording(isPaused = false, durationNs = 0L))
                }
                is VideoRecordEvent.Status -> {
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = false,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos
                        )
                    )
                }
                is VideoRecordEvent.Pause -> {
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = true,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos
                        )
                    )
                }
                is VideoRecordEvent.Resume -> {
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = false,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos
                        )
                    )
                }
                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (!recordEvent.hasError()) {
                        onEvent(VideoRecordStatus.Finalized(recordEvent.outputResults.outputUri))
                    } else {
                        Log.e(TAG, "Video recording error code: ${recordEvent.error}")
                        onEvent(
                            VideoRecordStatus.Error(
                                "Video recording stopped (${recordEvent.cause?.localizedMessage ?: "Error ${recordEvent.error}"})"
                            )
                        )
                    }
                }
            }
        }
    }

    fun pauseVideoRecording() {
        activeRecording?.pause()
    }

    fun resumeVideoRecording() {
        activeRecording?.resume()
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun mapFlashMode(setting: FlashSetting): Int {
        return when (setting) {
            FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
            FlashSetting.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }
}

sealed interface VideoRecordStatus {
    data class Recording(val isPaused: Boolean, val durationNs: Long) : VideoRecordStatus
    data class Finalized(val outputUri: Uri) : VideoRecordStatus
    data class Error(val message: String) : VideoRecordStatus
}
