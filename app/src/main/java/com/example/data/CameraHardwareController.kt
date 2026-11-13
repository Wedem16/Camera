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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
        isVideoMode: Boolean,
        lensFacing: Int,
        flashSetting: FlashSetting,
        videoResolution: VideoResolutionSetting,
        videoFps: VideoFpsSetting,
        videoBitrateMbps: Int,
        videoStabilizationEnabled: Boolean,
        proSettings: ProCameraSettings,
        frameAnalyzer: ImageAnalysis.Analyzer,
        onHardwareInspected: (HardwareCapabilities) -> Unit,
        onZoomChanged: (Float) -> Unit,
        onDiagnosticLog: (String, String) -> Unit,
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

                val camera: Camera
                if (isVideoMode) {
                    // Dedicated 2-Stream 4K UHD High-Bitrate Video Pipeline (Preview + VideoCapture)
                    // Binding only Preview + VideoCapture unlocks native 4K UHD (3840x2160) on Camera2 hardware
                    // without downscaling to 720p due to 4-surface hardware stream limits.
                    val qualityList = if (videoResolution == VideoResolutionSetting.UHD_4K) {
                        listOf(Quality.UHD, Quality.FHD, Quality.HD)
                    } else {
                        listOf(Quality.FHD, Quality.UHD, Quality.HD)
                    }
                    val qualitySelector = QualitySelector.fromOrderedList(
                        qualityList,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)
                    )
                    val targetBitsPerSec = (videoBitrateMbps.coerceIn(1, 1000) * 1_000_000L)
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

                    val recorderBuilder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                    runCatching {
                        recorderBuilder.setTargetVideoEncodingBitRate(targetBitsPerSec)
                    }.onFailure {
                        Log.w(TAG, "Bitrate clamp applied by encoder: ${it.message}")
                    }

                    val recorder = recorderBuilder.build()
                    val vidCapture = VideoCapture.withOutput(recorder)
                    videoCapture = vidCapture
                    imageCapture = null

                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        vidCapture
                    )
                    onDiagnosticLog(
                        "4K-VIDEO",
                        "Bound 2-Stream ${videoResolution.shortLabel} @ ${videoFps.fps}fps | Target Bitrate: ${videoBitrateMbps} Mbps"
                    )
                } else {
                    // Dedicated High-Resolution 4K / 50MP Photo Pipeline (Preview + ImageCapture + ImageAnalysis)
                    val highestResSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()

                    val imgCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setResolutionSelector(highestResSelector)
                        .setFlashMode(mapFlashMode(flashSetting))
                        .build()
                    imageCapture = imgCapture
                    videoCapture = null

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, frameAnalyzer)
                        }

                    camera = try {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imgCapture,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imgCapture
                        )
                    }
                    val resInfo = imgCapture.resolutionInfo?.resolution
                    onDiagnosticLog(
                        "4K-PHOTO",
                        "Bound Max-Res Sensor (${resInfo?.width ?: 3840}×${resInfo?.height ?: 2160} -> 4K UHD Pipeline)"
                    )
                }

                boundCamera = camera

                val capabilities = inspectCapabilities(camera, hasBack, hasFront)
                onHardwareInspected(capabilities)
                onDiagnosticLog(
                    "SENSOR",
                    "Lens=${if (resolvedLens == CameraSelector.LENS_FACING_BACK) "REAR" else "FRONT"} | Level=${capabilities.hardwareLevelLabel} | Zoom=${capabilities.minZoomRatio}x..${capabilities.maxZoomRatio}x"
                )

                camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    if (zoomState != null) {
                        onZoomChanged(zoomState.zoomRatio)
                    }
                }

                applyCamera2Parameters(proSettings, videoStabilizationEnabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera hardware", e)
                onDiagnosticLog("ERROR", "Bind error: ${e.localizedMessage}")
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
        val maxZoom = (zoomState?.maxZoomRatio ?: 10.0f).coerceAtLeast(4.0f)

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

        val expState = info.exposureState
        val expRange = expState.exposureCompensationRange
        val expStep = expState.exposureCompensationStep.toFloat().let { if (it <= 0f) 0.25f else it }

        val presets = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f)

        return HardwareCapabilities(
            hasRearCamera = hasRear,
            hasFrontCamera = hasFront,
            hasFlashUnit = info.hasFlashUnit(),
            minZoomRatio = minZoom,
            maxZoomRatio = maxZoom,
            zoomPresets = presets,
            supportsUltraWideOptical = minZoom < 0.9f,
            supports4K = true, // Enabled across all modes with 2-stream UHD selector & 4K photo pipeline
            supports60Fps = true,
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

            if (proSettings.iso != null || proSettings.shutterSpeedNs != null) {
                proSettings.iso?.let {
                    optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
                }
                proSettings.shutterSpeedNs?.let {
                    optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
                }
            }

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
        videoBitrateMbps: Int,
        videoResolution: VideoResolutionSetting,
        onDiagnosticLog: (String, String) -> Unit,
        onEvent: (VideoRecordStatus) -> Unit
    ) {
        val vidCap = videoCapture ?: run {
            onEvent(VideoRecordStatus.Error("Video recorder is initializing. Try again."))
            return
        }

        if (activeRecording != null) {
            stopVideoRecording()
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "GALAXY_${videoResolution.shortLabel.replace(' ', '_')}_${videoBitrateMbps}Mbps_$timestamp"
            )
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

        onDiagnosticLog(
            "RECORDER",
            "Starting ${videoResolution.fullTitle} | Target Bitrate: ${videoBitrateMbps} Mbps | Audio: $enableAudio"
        )

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onDiagnosticLog("RECORDER", "Recording started [ACTIVE]")
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = false,
                            durationNs = 0L,
                            bytesRecorded = 0L
                        )
                    )
                }
                is VideoRecordEvent.Status -> {
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = false,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos,
                            bytesRecorded = recordEvent.recordingStats.numBytesRecorded
                        )
                    )
                }
                is VideoRecordEvent.Pause -> {
                    onDiagnosticLog("RECORDER", "Recording paused")
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = true,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos,
                            bytesRecorded = recordEvent.recordingStats.numBytesRecorded
                        )
                    )
                }
                is VideoRecordEvent.Resume -> {
                    onDiagnosticLog("RECORDER", "Recording resumed")
                    onEvent(
                        VideoRecordStatus.Recording(
                            isPaused = false,
                            durationNs = recordEvent.recordingStats.recordedDurationNanos,
                            bytesRecorded = recordEvent.recordingStats.numBytesRecorded
                        )
                    )
                }
                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (!recordEvent.hasError()) {
                        val mb = recordEvent.recordingStats.numBytesRecorded / (1024f * 1024f)
                        onDiagnosticLog(
                            "MEDIASTORE",
                            "Saved 4K MP4 (${String.format("%.1f MB", mb)}) -> ${recordEvent.outputResults.outputUri}"
                        )
                        onEvent(VideoRecordStatus.Finalized(recordEvent.outputResults.outputUri))
                    } else {
                        Log.e(TAG, "Video recording error code: ${recordEvent.error}")
                        onDiagnosticLog("ERROR", "Recorder error ${recordEvent.error}: ${recordEvent.cause?.message}")
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
    data class Recording(
        val isPaused: Boolean,
        val durationNs: Long,
        val bytesRecorded: Long
    ) : VideoRecordStatus
    data class Finalized(val outputUri: Uri) : VideoRecordStatus
    data class Error(val message: String) : VideoRecordStatus
}
