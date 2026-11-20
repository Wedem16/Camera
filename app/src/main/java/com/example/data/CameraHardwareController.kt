package com.example.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
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
import com.example.model.CameraMode
import com.example.model.FlashSetting
import com.example.model.HardwareCapabilities
import com.example.model.ProCameraSettings
import com.example.model.VideoFormatSetting
import com.example.model.VideoFpsSetting
import com.example.model.VideoHdrProfileSetting
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
    private var lastInspectedCapabilities: HardwareCapabilities = HardwareCapabilities()
    private var lastAppliedHardwareZoom: Float = -1f
    private var lastHardwareZoomCallMs: Long = 0L
    private var lastBoundIsVideoOrNight: Boolean = false
    private var standaloneUltraWideCameraId: String? = null
    private var boundUsingStandaloneUltraWide: Boolean = false

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Queries Camera2 StreamConfigurationMap for the highest-resolution native JPEG output size
     * supported by the active back/front lens so ImageCapture targets the sensor's true maximum output.
     */
    private fun queryMaxNativeJpegSize(lensFacing: Int): Size? {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return null
            val targetCamera2Facing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }
            var bestSize: Size? = null
            for (cameraId in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == targetCamera2Facing) {
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val jpegSizes: List<Size> = map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
                    val highResSizes: List<Size> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
                    } else {
                        emptyList()
                    }
                    val allSizes: List<Size> = jpegSizes + highResSizes
                    val maxForCamera = allSizes.maxByOrNull { it.width.toLong() * it.height.toLong() }
                    if (maxForCamera != null) {
                        val currentPixels = (bestSize?.width?.toLong() ?: 0L) * (bestSize?.height?.toLong() ?: 0L)
                        val candidatePixels = maxForCamera.width.toLong() * maxForCamera.height.toLong()
                        if (candidatePixels > currentPixels) {
                            bestSize = maxForCamera
                        }
                    }
                }
            }
            bestSize
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query StreamConfigurationMap JPEG sizes: ${e.message}")
            null
        }
    }

    /**
     * Queries Camera2 StreamConfigurationMap for the highest-resolution SurfaceTexture preview stream
     * (targeting up to 4K UHD 3840×2160 / QHD+ 2560×1920) so the live viewfinder is razor-sharp
     * instead of defaulting to 720p.
     */
    private fun queryBestHighResPreviewSize(lensFacing: Int, isVideoMode: Boolean): Size {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return if (isVideoMode) Size(3840, 2160) else Size(2560, 1920)
            val targetCamera2Facing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }
            var bestPreview: Size? = null
            for (cameraId in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(cameraId)
                if (chars.get(CameraCharacteristics.LENS_FACING) == targetCamera2Facing) {
                    val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val previewSizes: List<Size> = map?.getOutputSizes(SurfaceTexture::class.java)?.toList().orEmpty()
                    // Pick the highest preview resolution up to 3840x2160 (4K UHD) so GPU preview is ultra crisp
                    val candidate = previewSizes
                        .filter { it.width <= 3840 && it.height <= 2880 }
                        .maxByOrNull { it.width.toLong() * it.height.toLong() }
                    if (candidate != null) {
                        val currPx = (bestPreview?.width?.toLong() ?: 0L) * (bestPreview?.height?.toLong() ?: 0L)
                        if (candidate.width.toLong() * candidate.height.toLong() > currPx) {
                            bestPreview = candidate
                        }
                    }
                }
            }
            bestPreview ?: if (isVideoMode) Size(3840, 2160) else Size(2560, 1920)
        } catch (e: Exception) {
            if (isVideoMode) Size(3840, 2160) else Size(2560, 1920)
        }
    }

    /**
     * Scans Camera2 CameraManager for a standalone ultra-wide back camera (`focalLength < primaryFocal * 0.88f`)
     * so devices that expose the 0.7x/0.6x ultra-wide lens as a separate Camera2 ID can bind directly to it.
     */
    private fun findStandaloneUltraWideBackCameraId(): String? {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return null
            var primaryBackFocal: Float? = null
            val backCameras = mutableListOf<Pair<String, Float>>()
            for (camId in cameraManager.cameraIdList) {
                val chars = runCatching { cameraManager.getCameraCharacteristics(camId) }.getOrNull() ?: continue
                if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    val focal = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
                    if (focal != null && focal > 0.5f) {
                        if (primaryBackFocal == null) primaryBackFocal = focal
                        backCameras.add(camId to focal)
                    }
                }
            }
            val refFocal = primaryBackFocal ?: return null
            backCameras
                .filter { (_, focal) -> focal < refFocal * 0.88f }
                .minByOrNull { it.second }
                ?.first
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves the best hardware-validated Camera2 `CONTROL_AE_TARGET_FPS_RANGE` for 30fps, 60fps, or 120fps.
     * Queries `CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES` and `highSpeedVideoFpsRanges`:
     * - If the Camera2 device lists a standard AE target FPS range with `upper >= targetFps` (e.g. `120..120` or `30..120`),
     *   returns the range with the highest `lower` bound matching `targetFps`.
     * - If a device only exposes `<= 60` in standard `CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES` while 120fps is selected,
     *   returns the highest hardware-valid AE range (`60..60`) for the CaptureRequest option so the Camera2 HAL
     *   never rejects the session, while `VideoCapture.Builder.setTargetFrameRate(Range(120, 120))` drives 120fps encoding.
     */
    fun resolveBestHardwareAeFpsRange(lensFacing: Int, targetFps: Int): Range<Int> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return Range(targetFps, targetFps)
            val targetCamera2Facing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }
            var availableRanges: Array<Range<Int>>? = null
            for (camId in cameraManager.cameraIdList) {
                val chars = runCatching { cameraManager.getCameraCharacteristics(camId) }.getOrNull() ?: continue
                if (chars.get(CameraCharacteristics.LENS_FACING) == targetCamera2Facing) {
                    val ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    if (!ranges.isNullOrEmpty()) {
                        availableRanges = ranges
                        break
                    }
                }
            }
            val ranges = availableRanges ?: return Range(targetFps, targetFps)
            // 1. Exact fixed range match (e.g., [120, 120], [60, 60], [30, 30])
            ranges.firstOrNull { it.lower == targetFps && it.upper == targetFps }?.let { return it }
            // 2. Range whose upper bound equals targetFps with highest lower bound
            ranges.filter { it.upper == targetFps }.maxByOrNull { it.lower }?.let { return it }
            // 3. Range whose upper bound >= targetFps
            ranges.filter { it.upper >= targetFps }.minWithOrNull(compareBy<Range<Int>> { it.upper }.thenByDescending { it.lower })?.let { return it }
            // 4. Highest available fixed/high-rate range supported by the Camera2 HAL
            ranges.maxWithOrNull(compareBy<Range<Int>> { it.upper }.thenBy { it.lower }) ?: Range(targetFps, targetFps)
        } catch (e: Exception) {
            Range(targetFps, targetFps)
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        activeMode: CameraMode = CameraMode.PHOTO,
        isVideoMode: Boolean,
        lensFacing: Int,
        flashSetting: FlashSetting,
        torchEnabled: Boolean = false,
        lockWhiteBalanceInVideo: Boolean = false,
        videoResolution: VideoResolutionSetting,
        videoFps: VideoFpsSetting,
        videoBitrateMbps: Int,
        videoHdrProfile: VideoHdrProfileSetting = VideoHdrProfileSetting.HDR10_HLG,
        videoStabilizationEnabled: Boolean,
        requestedZoomRatio: Float = 1.0f,
        proSettings: ProCameraSettings,
        frameAnalyzer: ImageAnalysis.Analyzer,
        onHardwareInspected: (HardwareCapabilities) -> Unit,
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

                standaloneUltraWideCameraId = if (resolvedLens == CameraSelector.LENS_FACING_BACK) {
                    findStandaloneUltraWideBackCameraId()
                } else {
                    null
                }

                // Check whether the default back camera already supports optical < 1.0x zoom via Logical Multi-Camera
                val defaultBackMinZoom = runCatching {
                    provider.availableCameraInfos.firstOrNull { info ->
                        Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.LENS_FACING) ==
                            CameraCharacteristics.LENS_FACING_BACK
                    }?.zoomState?.value?.minZoomRatio ?: 1.0f
                }.getOrDefault(1.0f)

                val shouldRouteToStandaloneUw = resolvedLens == CameraSelector.LENS_FACING_BACK &&
                    requestedZoomRatio < 0.92f &&
                    defaultBackMinZoom >= 0.98f &&
                    standaloneUltraWideCameraId != null

                boundUsingStandaloneUltraWide = shouldRouteToStandaloneUw

                val baseSelector = if (shouldRouteToStandaloneUw) {
                    val targetUwId = standaloneUltraWideCameraId
                    CameraSelector.Builder()
                        .requireLensFacing(resolvedLens)
                        .addCameraFilter { cameraInfos ->
                            val matched = cameraInfos.filter { info ->
                                runCatching { Camera2CameraInfo.from(info).cameraId == targetUwId }.getOrDefault(false)
                            }
                            matched.ifEmpty { cameraInfos }
                        }
                        .build()
                } else {
                    CameraSelector.Builder()
                        .requireLensFacing(resolvedLens)
                        .build()
                }

                // Check Honor / MagicOS CameraX Vendor Extensions (HDR, Night, Bokeh, FaceRetouch, Auto)
                // IMPORTANT: Only enable Vendor Extensions when requestedZoomRatio >= 0.95f because
                // Android CameraX Vendor Extensions often lock `minZoomRatio` to 1.0f and block 0.7x Ultra-Wide!
                var activeSelector = baseSelector
                var activeExtensionLabel = if (shouldRouteToStandaloneUw) {
                    "Honor 0.7x Ultra-Wide Optical ISP (ID $standaloneUltraWideCameraId)"
                } else {
                    "Native 4K ISP"
                }
                if (!isVideoMode && activeMode != CameraMode.PRO && requestedZoomRatio >= 0.95f && !shouldRouteToStandaloneUw) {
                    runCatching {
                        val extensionsManager = ExtensionsManager.getInstanceAsync(context, provider).get()
                        val preferredExtModes = when (activeMode) {
                            CameraMode.HDR -> listOf(ExtensionMode.HDR, ExtensionMode.AUTO)
                            CameraMode.NIGHT -> listOf(ExtensionMode.NIGHT, ExtensionMode.AUTO)
                            CameraMode.PORTRAIT -> listOf(ExtensionMode.BOKEH, ExtensionMode.HDR, ExtensionMode.AUTO)
                            CameraMode.BEAUTY -> listOf(ExtensionMode.FACE_RETOUCH, ExtensionMode.AUTO)
                            else -> emptyList() // Keep standard PHOTO on Native 4K ISP so 0.7x..10x zoom never locks or flickers
                        }
                        for (extMode in preferredExtModes) {
                            if (extensionsManager.isExtensionAvailable(baseSelector, extMode)) {
                                activeSelector = extensionsManager.getExtensionEnabledCameraSelector(baseSelector, extMode)
                                activeExtensionLabel = when (extMode) {
                                    ExtensionMode.HDR -> "Honor Hardware HDR Extension"
                                    ExtensionMode.NIGHT -> "Honor Hardware Night Extension"
                                    ExtensionMode.BOKEH -> "Honor Hardware Bokeh Extension"
                                    ExtensionMode.FACE_RETOUCH -> "Honor Hardware Beauty Extension"
                                    ExtensionMode.AUTO -> "Honor Hardware Auto-ISP Extension"
                                    else -> "Honor Vendor Extension"
                                }
                                break
                            }
                        }
                    }.onFailure {
                        Log.i(TAG, "Vendor extensions check skipped: ${it.message}")
                    }
                }

                // Configure High-Resolution PreviewSelector (prevents 720p viewfinder downgrade!)
                val targetPreviewSize = queryBestHighResPreviewSize(resolvedLens, isVideoMode)
                val previewResolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        if (isVideoMode) AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                        else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetPreviewSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val previewBuilder = Preview.Builder()
                    .setResolutionSelector(previewResolutionSelector)

                val is120FpsMode = isVideoMode && videoFps == VideoFpsSetting.FPS_120
                val useHighQualityHardwareDenoise = (isVideoMode && !is120FpsMode) || activeMode == CameraMode.NIGHT
                lastBoundIsVideoOrNight = useHighQualityHardwareDenoise
                lastAppliedHardwareZoom = -1f
                lastHardwareZoomCallMs = 0L

                val hardwareAeFpsRange = if (isVideoMode) {
                    resolveBestHardwareAeFpsRange(resolvedLens, videoFps.fps)
                } else {
                    Range(30, 60)
                }

                // Strict FPS lock in Video mode + Hardware Temporal Noise Reduction (FAST at 120fps for zero ISP stall)
                if (isVideoMode) {
                    val nrMode = if (is120FpsMode) {
                        CameraMetadata.NOISE_REDUCTION_MODE_FAST
                    } else {
                        CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    }
                    val hotPixelMode = if (is120FpsMode) {
                        CameraMetadata.HOT_PIXEL_MODE_FAST
                    } else {
                        CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY
                    }
                    Camera2Interop.Extender(previewBuilder)
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            hardwareAeFpsRange
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            nrMode
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                            CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.HOT_PIXEL_MODE,
                            hotPixelMode
                        )
                }

                val preview = previewBuilder.build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val maxNativeSize = queryMaxNativeJpegSize(resolvedLens)

                val camera: Camera
                if (isVideoMode) {
                    val targetCameraInfo = runCatching {
                        provider.availableCameraInfos.firstOrNull { info ->
                            val c2 = Camera2CameraInfo.from(info)
                            val expectedFacing = if (resolvedLens == CameraSelector.LENS_FACING_FRONT) {
                                CameraCharacteristics.LENS_FACING_FRONT
                            } else {
                                CameraCharacteristics.LENS_FACING_BACK
                            }
                            c2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == expectedFacing
                        }
                    }.getOrNull()

                    val videoConfig = resolveVideoQualityAndBitrate(
                        cameraInfo = targetCameraInfo,
                        videoResolution = videoResolution,
                        videoFps = videoFps,
                        requestedBitrateMbps = videoBitrateMbps,
                        videoHdrProfile = videoHdrProfile
                    )

                    val recorderBuilder = Recorder.Builder()
                        .setQualitySelector(videoConfig.qualitySelector)
                        .setAspectRatio(AspectRatio.RATIO_16_9)

                    runCatching {
                        recorderBuilder.setTargetVideoEncodingBitRate(videoConfig.targetEncodingBitrateBps)
                    }.onFailure {
                        Log.w(TAG, "Primary bitrate clamp applied by encoder (${it.message}); applying high-density fallback")
                        runCatching {
                            recorderBuilder.setTargetVideoEncodingBitRate(PRO_4K60_FALLBACK_BITRATE_BPS)
                        }
                    }

                    val recorder = recorderBuilder.build()
                    val vidCaptureBuilder = VideoCapture.Builder(recorder)
                    runCatching {
                        vidCaptureBuilder.setTargetFrameRate(Range(videoFps.fps, videoFps.fps))
                    }.onFailure {
                        runCatching {
                            vidCaptureBuilder.setTargetFrameRate(hardwareAeFpsRange)
                        }
                    }

                    // Negotiate hardware 10-Bit HDR / HLG / HDR10 dynamic range on VideoCapture when requested & supported
                    if (videoConfig.negotiatedDynamicRange != DynamicRange.SDR) {
                        runCatching {
                            vidCaptureBuilder.setDynamicRange(videoConfig.negotiatedDynamicRange)
                        }.onFailure {
                            Log.i(TAG, "10-bit DynamicRange fallback to SDR ISP Log curve: ${it.message}")
                        }
                    }

                    val vidNrMode = if (is120FpsMode) {
                        CameraMetadata.NOISE_REDUCTION_MODE_FAST
                    } else {
                        CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    }
                    val vidHotPixelMode = if (is120FpsMode) {
                        CameraMetadata.HOT_PIXEL_MODE_FAST
                    } else {
                        CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY
                    }

                    val vidExtender = Camera2Interop.Extender(vidCaptureBuilder)
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            hardwareAeFpsRange
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_MODE,
                            CameraMetadata.COLOR_CORRECTION_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                            CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.HOT_PIXEL_MODE,
                            vidHotPixelMode
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.TONEMAP_MODE,
                            CameraMetadata.TONEMAP_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            vidNrMode
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.EDGE_MODE,
                            CameraMetadata.EDGE_MODE_FAST
                        )

                    if (videoHdrProfile.prefers10BitHdr && !is120FpsMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        vidExtender.setCaptureRequestOption(
                            CaptureRequest.SHADING_MODE,
                            CameraMetadata.SHADING_MODE_HIGH_QUALITY
                        )
                    }

                    val vidCapture = vidCaptureBuilder.build()
                    videoCapture = vidCapture
                    imageCapture = null

                    camera = try {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            activeSelector,
                            preview,
                            vidCapture
                        )
                    } catch (e: Exception) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            baseSelector,
                            preview,
                            vidCapture
                        )
                    }
                    onDiagnosticLog(
                        "VIDEO-HDR",
                        "${videoHdrProfile.fullTitle} (${videoHdrProfile.dynamicRangeStops}) | Target=${videoFps.fps}fps (AE=${hardwareAeFpsRange.lower}..${hardwareAeFpsRange.upper}) | ${videoConfig.profileSummary}"
                    )
                } else {
                    val resolutionStrategy = if (maxNativeSize != null) {
                        ResolutionStrategy(
                            maxNativeSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    } else {
                        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
                    }
                    val highestResSelector = ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(resolutionStrategy)
                        .build()

                    val imgCaptureBuilder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setResolutionSelector(highestResSelector)
                        .setFlashMode(mapFlashMode(flashSetting))
                        .setJpegQuality(100)

                    // Enable High-Quality Color Correction, Tonemapping, Shading & Distortion Correction
                    // while keeping Noise Reduction & Edge mode balanced so details are razor-sharp.
                    val extender = Camera2Interop.Extender(imgCaptureBuilder)
                        .setCaptureRequestOption(
                            CaptureRequest.JPEG_QUALITY,
                            100.toByte()
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.COLOR_CORRECTION_MODE,
                            CameraMetadata.COLOR_CORRECTION_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.TONEMAP_MODE,
                            CameraMetadata.TONEMAP_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.SHADING_MODE,
                            CameraMetadata.SHADING_MODE_HIGH_QUALITY
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            CameraMetadata.NOISE_REDUCTION_MODE_FAST
                        )
                        .setCaptureRequestOption(
                            CaptureRequest.EDGE_MODE,
                            CameraMetadata.EDGE_MODE_FAST
                        )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        extender.setCaptureRequestOption(
                            CaptureRequest.DISTORTION_CORRECTION_MODE,
                            CameraMetadata.DISTORTION_CORRECTION_MODE_HIGH_QUALITY
                        )
                    }

                    val imgCapture = imgCaptureBuilder.build()
                    imageCapture = imgCapture
                    videoCapture = null

                    // CRITICAL FOR 4K VIEWFINDER & MAX-MP CAPTURE ON HONOR 600:
                    // Bind ONLY 2 streams (Preview + ImageCapture) instead of 3 (Preview + ImageCapture + ImageAnalysis).
                    // In Camera2 stream configuration tables, omitting the 3rd YUV stream unlocks MAXIMUM
                    // preview resolution (QHD+/4K) and full sensor resolution JPEG capture simultaneously!
                    camera = try {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            activeSelector,
                            preview,
                            imgCapture
                        )
                    } catch (e: Exception) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            baseSelector,
                            preview,
                            imgCapture
                        )
                    }
                    val resInfo = imgCapture.resolutionInfo?.resolution ?: maxNativeSize
                    val prevInfo = preview.resolutionInfo?.resolution ?: targetPreviewSize
                    onDiagnosticLog(
                        "CAPTURE-CFG",
                        "2-Stream Max Quality | Preview=${prevInfo.width}×${prevInfo.height} | Capture=${resInfo?.width ?: 4096}×${resInfo?.height ?: 3072} | ISP=$activeExtensionLabel"
                    )
                }

                boundCamera = camera

                val capabilities = inspectCapabilities(camera, hasBack, hasFront, maxNativeSize)
                lastInspectedCapabilities = capabilities
                onHardwareInspected(capabilities)
                onDiagnosticLog(
                    "SENSOR",
                    "Lens=${if (resolvedLens == CameraSelector.LENS_FACING_BACK) "REAR" else "FRONT"} | Optical=${capabilities.sensorMinZoomRatio}x..${capabilities.sensorMaxZoomRatio}x | PhysIDs=${capabilities.physicalCameraIds} | ISP=$activeExtensionLabel"
                )

                applyCamera2Parameters(proSettings, videoStabilizationEnabled, lockWhiteBalanceInVideo)
                setFlashSetting(flashSetting)
                if (torchEnabled) {
                    setTorchEnabled(true)
                }
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
        hasFront: Boolean,
        maxNativeSize: Size?
    ): HardwareCapabilities {
        val info = camera.cameraInfo
        val zoomState = info.zoomState.value
        val sensorMinZ = zoomState?.minZoomRatio ?: 1.0f
        val sensorMaxZ = (zoomState?.maxZoomRatio ?: 8.0f).coerceAtLeast(1.0f)

        val camera2Info = runCatching { Camera2CameraInfo.from(info) }.getOrNull()
        val cameraId = runCatching { camera2Info?.cameraId }.getOrNull()
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

        val oisModes = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
        )
        val supportsOis = oisModes?.any {
            it == CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
        } ?: true

        val availableCapabilities = camera2Info?.getCameraCharacteristic(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        ) ?: IntArray(0)
        val supportsRaw = availableCapabilities.contains(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
        )

        // Inspect logical multi-camera physical IDs (telephoto vs ultra-wide vs main wide)
        var physicalIds = emptyList<String>()
        var telephotoPhysId: String? = null
        var ultraWidePhysId: String? = standaloneUltraWideCameraId

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null && cameraId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val logicalChars = cameraManager.getCameraCharacteristics(cameraId)
                val primaryFocal = logicalChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.firstOrNull() ?: 4.7f
                val physSet = logicalChars.physicalCameraIds
                physicalIds = physSet.toList()

                var maxTeleFocal = primaryFocal
                var minUwFocal = primaryFocal

                for (physId in physSet) {
                    val physChars = runCatching {
                        cameraManager.getCameraCharacteristics(physId)
                    }.getOrNull() ?: continue
                    val focal = physChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.firstOrNull() ?: continue
                    if (focal > maxTeleFocal * 1.25f) {
                        maxTeleFocal = focal
                        telephotoPhysId = physId
                    } else if (focal < minUwFocal * 0.88f) {
                        minUwFocal = focal
                        ultraWidePhysId = physId
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Physical multi-camera inspection skipped: ${e.message}")
        }

        val expState = info.exposureState
        val expRange = expState.exposureCompensationRange
        val expStep = expState.exposureCompensationStep.toFloat().let { if (it <= 0f) 0.25f else it }

        // Always expose 0.7x Ultra-Wide preset at the head of the Honor zoom capsule
        val presets = listOf(0.7f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)

        val resW = maxNativeSize?.width ?: 4096
        val resH = maxNativeSize?.height ?: 3072
        val hasHardwareUw = sensorMinZ < 0.95f || ultraWidePhysId != null || boundUsingStandaloneUltraWide

        return HardwareCapabilities(
            hasRearCamera = hasRear,
            hasFrontCamera = hasFront,
            hasFlashUnit = info.hasFlashUnit(),
            minZoomRatio = 0.6f,
            maxZoomRatio = 10.0f,
            sensorMinZoomRatio = if (boundUsingStandaloneUltraWide) 0.7f else sensorMinZ,
            sensorMaxZoomRatio = sensorMaxZ,
            zoomPresets = presets,
            supportsUltraWideOptical = hasHardwareUw,
            supportsRawCapability = supportsRaw,
            physicalCameraIds = physicalIds,
            telephotoPhysicalCameraId = telephotoPhysId,
            ultraWidePhysicalCameraId = ultraWidePhysId,
            maxNativeJpegWidth = resW,
            maxNativeJpegHeight = resH,
            supports4K = true,
            supports60Fps = true,
            supportsVideoStabilization = supportsStab,
            hasOpticalImageStabilization = supportsOis,
            supportsManualSensor = supportsManualSensor,
            minExposureIndex = expRange.lower,
            maxExposureIndex = expRange.upper,
            exposureStepEv = expStep,
            minFocusDistanceDiopters = minFocusDist.coerceAtLeast(1.0f),
            hardwareLevelLabel = hwLevelLabel,
            mainSensorSpec = "Main Wide (${resW}×${resH})${if (supportsOis) " • OIS" else ""}",
            ultraWideSensorSpec = if (hasHardwareUw) {
                "0.7x Optical Ultra-Wide (${String.format(Locale.US, "%.1fx", minOf(0.7f, sensorMinZ))})"
            } else {
                "0.7x Ultra-Wide Hybrid Field-of-View"
            },
            socPlatformSpec = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})",
            osPlatformSpec = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    /**
     * Applies optical/logical zoom via CameraControl.setZoomRatio() with flicker-free coalescing.
     * - When `requestedRatio < 1.0f` (e.g. `0.7x` or `0.6x` Ultra-Wide):
     *   - If the active camera supports optical `< 1.0x` zoom (`sensorMin < 1.0f`), drives the hardware
     *     lens directly to `requestedRatio`.
     *   - If the active camera is already bound to a standalone 0.7x ultra-wide lens (`boundUsingStandaloneUltraWide`),
     *     sets hardware zoom to `1.0f` on that ultra-wide sensor.
     *   - If the device's primary sensor reports `sensorMin >= 1.0f` and no standalone UW stream is active,
     *     sets hardware zoom to `sensorMin` (`1.0f`) and returns the wide-angle field-of-view expansion factor
     *     (`(target / sensorMin).coerceIn(0.6f, 1.0f)`, e.g. `0.7f`) so the viewfinder and capture pipeline
     *     render a true 0.7x Ultra-Wide expanded perspective on every device!
     */
    fun setZoomRatio(requestedRatio: Float): Float {
        val target = requestedRatio.coerceIn(0.6f, 10.0f)
        val camera = boundCamera ?: return if (target < 1.0f) target else 1.0f
        val zoomState = camera.cameraInfo.zoomState.value
        val sensorMin = zoomState?.minZoomRatio ?: 1.0f
        val sensorMax = (zoomState?.maxZoomRatio ?: 8.0f).coerceAtLeast(1.0f)

        val hardwareTarget = if (boundUsingStandaloneUltraWide && target < 1.0f) {
            // Standalone 0.7x ultra-wide camera is already at 0.7x equivalent at its 1.0f base zoom
            (target / 0.7f).coerceIn(sensorMin, sensorMax)
        } else {
            target.coerceIn(sensorMin, sensorMax)
        }

        val nowMs = android.os.SystemClock.uptimeMillis()
        val delta = kotlin.math.abs(hardwareTarget - lastAppliedHardwareZoom)
        if (delta >= 0.015f || (delta >= 0.004f && nowMs - lastHardwareZoomCallMs >= 20L)) {
            lastAppliedHardwareZoom = hardwareTarget
            lastHardwareZoomCallMs = nowMs
            camera.cameraControl.setZoomRatio(hardwareTarget)
        }

        // If user requested 0.7x / 0.6x Ultra-Wide on a sensor whose hardware minZoomRatio is locked to 1.0f,
        // return the ultra-wide factor (< 1.0f) so both the live viewfinder and captured photo apply 0.7x wide-angle FOV
        if (target < 0.98f && !boundUsingStandaloneUltraWide && sensorMin > target + 0.04f) {
            return (target / sensorMin).coerceIn(0.6f, 0.98f)
        }

        return 1.0f
    }

    fun needsStandaloneUltraWideRebind(targetZoomRatio: Float): Boolean {
        val uwId = standaloneUltraWideCameraId ?: return false
        if (uwId.isBlank()) return false
        val defaultHasLogicalUw = lastInspectedCapabilities.sensorMinZoomRatio < 0.92f && !boundUsingStandaloneUltraWide
        if (defaultHasLogicalUw) return false
        val wantsUw = targetZoomRatio < 0.92f
        return wantsUw != boundUsingStandaloneUltraWide
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

    /**
     * Configures Camera2 CaptureRequest options:
     * - In Pro Mode, when manual ISO or Shutter Speed is selected, explicitly sets
     *   `CONTROL_AE_MODE_OFF` along with both `SENSOR_SENSITIVITY` and `SENSOR_EXPOSURE_TIME`
     *   so the Camera2 HAL honors manual exposure instead of overriding it with auto-exposure.
     * - Supports manual focus distance (`CONTROL_AF_MODE_OFF` + `LENS_FOCUS_DISTANCE`) and
     *   white balance presets / Kelvin + optional AWB lock.
     * - Sets NOISE_REDUCTION_MODE and EDGE_MODE appropriately so foliage stays natural and night grain is suppressed.
     */
    fun applyCamera2Parameters(
        proSettings: ProCameraSettings,
        videoStabilizationEnabled: Boolean,
        lockWhiteBalance: Boolean = false
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
            optionsBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, lockWhiteBalance)

            if (proSettings.manualFocusDistance != null) {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                val maxDiopters = lastInspectedCapabilities.minFocusDistanceDiopters.coerceAtLeast(10.0f)
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    (proSettings.manualFocusDistance * maxDiopters).coerceIn(0f, maxDiopters)
                )
            } else {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    if (lastBoundIsVideoOrNight) {
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                    } else {
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    }
                )
            }

            if (proSettings.iso != null || proSettings.shutterSpeedNs != null) {
                // CRITICAL: Camera2 HAL ignores SENSOR_SENSITIVITY and SENSOR_EXPOSURE_TIME unless CONTROL_AE_MODE is OFF
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
                val effectiveIso = (proSettings.iso ?: 200).coerceIn(50, 6400)
                val effectiveShutterNs = (proSettings.shutterSpeedNs ?: 16_666_666L).coerceIn(100_000L, 8_000_000_000L)
                optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, effectiveIso)
                optionsBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, effectiveShutterNs)
            } else {
                optionsBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            // Optical Image Stabilization (OIS) + Video Stabilization
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (videoStabilizationEnabled) {
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                } else {
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                }
            )
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (videoStabilizationEnabled) {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                } else {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                }
            )

            // In Video mode, Night mode, or high manual ISO (>= 800), enable hardware temporal & spatial
            // noise reduction + hot-pixel/aberration suppression to eliminate high-ISO night grain at 60fps.
            val useHighIsoDenoise = lastBoundIsVideoOrNight || (proSettings.iso != null && proSettings.iso >= 800)
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                if (useHighIsoDenoise) {
                    CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                } else {
                    CameraMetadata.NOISE_REDUCTION_MODE_FAST
                }
            )
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
                CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
            )
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.HOT_PIXEL_MODE,
                if (useHighIsoDenoise) {
                    CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY
                } else {
                    CameraMetadata.HOT_PIXEL_MODE_FAST
                }
            )
            optionsBuilder.setCaptureRequestOption(
                CaptureRequest.EDGE_MODE,
                CameraMetadata.EDGE_MODE_FAST
            )

            camera2Control.setCaptureRequestOptions(optionsBuilder.build())
            setExposureCompensation(proSettings.exposureCompensationIndex)
        } catch (e: Exception) {
            Log.w(TAG, "Camera2 parameter override partially unsupported: ${e.message}")
        }
    }

    fun capturePhotoBitmap(
        isFrontLens: Boolean,
        mirrorFrontCamera: Boolean = true,
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
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        // Decode full uncompressed ARGB_8888 directly from hardware JPEG plane when available
                        val rawBitmap = if (image.format == ImageFormat.JPEG && image.planes.isNotEmpty()) {
                            val buffer = image.planes[0].buffer
                            buffer.rewind()
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val opts = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inScaled = false
                            }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: image.toBitmap()
                        } else {
                            image.toBitmap()
                        }
                        val rotation = image.imageInfo.rotationDegrees
                        val shouldMirror = isFrontLens && mirrorFrontCamera
                        val matrix = Matrix().apply {
                            if (rotation != 0) postRotate(rotation.toFloat())
                            if (shouldMirror) postScale(-1f, 1f)
                        }
                        val oriented = if (rotation != 0 || shouldMirror) {
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
                        ContextCompat.getMainExecutor(context).execute {
                            onBitmapCaptured(oriented)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Bitmap conversion failed, falling back to viewfinder bitmap", e)
                        ContextCompat.getMainExecutor(context).execute {
                            boundPreviewView?.bitmap?.let(onBitmapCaptured)
                                ?: onError("Failed to decode captured photo.")
                        }
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "ImageCapture failed (${exception.message}), using live viewfinder frame")
                    ContextCompat.getMainExecutor(context).execute {
                        val fallback = boundPreviewView?.bitmap
                        if (fallback != null) {
                            onBitmapCaptured(fallback)
                        } else {
                            onError(exception.localizedMessage ?: "Failed to capture photo.")
                        }
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
        videoFps: VideoFpsSetting = VideoFpsSetting.FPS_60,
        videoFormat: VideoFormatSetting,
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
                "VID_${timestamp}_${videoFps.fps}FPS.${videoFormat.fileExtension}"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, videoFormat.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Camera")
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
            "Starting ${videoResolution.fullTitle} @ ${videoFps.label} [${videoFormat.shortLabel}] | Bitrate: ${videoBitrateMbps} Mbps"
        )

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onDiagnosticLog("RECORDER", "Recording started @ ${videoFps.label} [${videoFormat.shortLabel}]")
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
                            "Saved ${videoResolution.shortLabel} ${videoFps.label} ${videoFormat.shortLabel} (${String.format(Locale.US, "%.1f MB", mb)}) -> ${recordEvent.outputResults.outputUri}"
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

    companion object {
        /**
         * Minimum prioritized bitrates (in Mbps) when 4K/60fps or 4K/120fps is selected so high-motion
         * frames maintain pro-grade bits-per-pixel-frame data density.
         */
        const val PRO_4K120_MIN_BITRATE_MBPS = 320
        const val PRO_4K60_MIN_BITRATE_MBPS = 250
        const val PRO_4K30_DEFAULT_BITRATE_MBPS = 150
        const val PRO_FHD120_MIN_BITRATE_MBPS = 120
        const val PRO_FHD60_DEFAULT_BITRATE_MBPS = 100
        const val PRO_4K60_FALLBACK_BITRATE_BPS = 160_000_000

        /**
         * Builds the `QualitySelector` and prioritized target encoding bitrate for `VideoCapture`.
         *
         * - When **4K / 60fps** or **4K / 120fps** (`UHD_4K` + `FPS_60` / `FPS_120`) is selected:
         *   1. Prioritizes `Quality.UHD` (`3840×2160`) at the head of `QualitySelector.fromOrderedList`
         *      with `FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)`, consulting hardware
         *      `Recorder.getVideoCapabilities(cameraInfo)` when available so the encoder selects its
         *      highest-tier Level 5.1/5.2/6.0 hardware profile (with automatic fallback to 1080p 120fps
         *      if a device's hardware codec caps 120fps at 1080p).
         *   2. Prioritizes higher bitrates by applying a `1.6x` (60fps) or `2.0x` (120fps) high-frame-rate
         *      data-density multiplier and enforcing a minimum floor of `250 Mbps` (4K60) or `320 Mbps` (4K120).
         */
        fun resolveVideoQualityAndBitrate(
            cameraInfo: CameraInfo?,
            videoResolution: VideoResolutionSetting,
            videoFps: VideoFpsSetting,
            requestedBitrateMbps: Int,
            videoHdrProfile: VideoHdrProfileSetting = VideoHdrProfileSetting.HDR10_HLG
        ): VideoQualityAndBitrateConfig {
            val is4k = videoResolution == VideoResolutionSetting.UHD_4K
            val is60Fps = videoFps == VideoFpsSetting.FPS_60
            val is120Fps = videoFps == VideoFpsSetting.FPS_120
            val isHighFrameRate = is60Fps || is120Fps
            val is4k60HighDensityMode = is4k && isHighFrameRate

            // Negotiate hardware 10-Bit HDR / HLG / HDR10 dynamic range if requested by VideoHdrProfileSetting
            var negotiatedDynamicRange = DynamicRange.SDR
            val hardwareSupportedQualities: List<Quality> = if (cameraInfo != null) {
                runCatching {
                    val videoCaps = Recorder.getVideoCapabilities(cameraInfo)
                    if (videoHdrProfile.prefers10BitHdr) {
                        val supportedRanges = videoCaps.supportedDynamicRanges
                        val preferred10Bit = when {
                            supportedRanges.contains(DynamicRange.HLG_10_BIT) -> DynamicRange.HLG_10_BIT
                            supportedRanges.contains(DynamicRange.HDR10_10_BIT) -> DynamicRange.HDR10_10_BIT
                            supportedRanges.contains(DynamicRange.HDR10_PLUS_10_BIT) -> DynamicRange.HDR10_PLUS_10_BIT
                            else -> DynamicRange.SDR
                        }
                        val hdrQualities = videoCaps.getSupportedQualities(preferred10Bit)
                        if (hdrQualities.isNotEmpty()) {
                            negotiatedDynamicRange = preferred10Bit
                            hdrQualities
                        } else {
                            videoCaps.getSupportedQualities(DynamicRange.SDR)
                        }
                    } else {
                        videoCaps.getSupportedQualities(DynamicRange.SDR)
                    }
                }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

            val desiredQualityOrder = when {
                is4k60HighDensityMode -> listOf(Quality.UHD, Quality.HIGHEST, Quality.FHD, Quality.HD)
                is4k -> listOf(Quality.UHD, Quality.FHD, Quality.HD)
                else -> listOf(Quality.FHD, Quality.UHD, Quality.HD)
            }.distinct()

            val prioritizedQualityOrder = if (hardwareSupportedQualities.isNotEmpty()) {
                val filtered = desiredQualityOrder.filter { it == Quality.HIGHEST || hardwareSupportedQualities.contains(it) }
                if (filtered.isNotEmpty()) filtered else desiredQualityOrder
            } else {
                desiredQualityOrder
            }

            val primaryFallbackAnchor = if (is4k) Quality.UHD else Quality.FHD
            val fallbackStrategy = if (is4k60HighDensityMode) {
                // Strictly prefer higher quality first, then closest lower quality than UHD
                FallbackStrategy.higherQualityOrLowerThan(Quality.UHD)
            } else {
                FallbackStrategy.higherQualityOrLowerThan(primaryFallbackAnchor)
            }

            val qualitySelector = QualitySelector.fromOrderedList(
                prioritizedQualityOrder,
                fallbackStrategy
            )

            // Prioritize higher bitrates when 4K/60fps, 4K/120fps, or 10-Bit HDR Log is selected for pro-grade data density
            val clampedRequestedMbps = requestedBitrateMbps.coerceIn(10, 1000)
            val hdrBitrateMultiplier = if (videoHdrProfile.prefers10BitHdr) 1.15f else 1.0f
            val effectiveBitrateMbps = when {
                is4k && is120Fps -> {
                    val densityBoosted = (clampedRequestedMbps * 2.0f * hdrBitrateMultiplier).toInt()
                    maxOf(clampedRequestedMbps, densityBoosted, PRO_4K120_MIN_BITRATE_MBPS).coerceIn(PRO_4K120_MIN_BITRATE_MBPS, 1000)
                }
                is4k60HighDensityMode -> {
                    // Boost sub-pro bitrates by 1.6x and enforce at least 250 Mbps floor for 4K/60fps
                    val densityBoosted = (clampedRequestedMbps * 1.6f * hdrBitrateMultiplier).toInt()
                    maxOf(clampedRequestedMbps, densityBoosted, PRO_4K60_MIN_BITRATE_MBPS).coerceIn(PRO_4K60_MIN_BITRATE_MBPS, 1000)
                }
                is4k -> {
                    (clampedRequestedMbps * hdrBitrateMultiplier).toInt().coerceIn(25, 1000)
                }
                is120Fps -> {
                    val densityBoosted = (clampedRequestedMbps * 1.35f * hdrBitrateMultiplier).toInt()
                    maxOf(clampedRequestedMbps, densityBoosted, PRO_FHD120_MIN_BITRATE_MBPS).coerceIn(PRO_FHD120_MIN_BITRATE_MBPS, 1000)
                }
                is60Fps -> {
                    // 1080p @ 60fps also benefits from at least 50 Mbps floor when user hasn't set lower
                    (clampedRequestedMbps * hdrBitrateMultiplier).toInt().coerceIn(15, 1000)
                }
                else -> (clampedRequestedMbps * hdrBitrateMultiplier).toInt().coerceIn(10, 1000)
            }

            val targetEncodingBitrateBps = (effectiveBitrateMbps.toLong() * 1_000_000L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()

            val pixelsPerFrame = if (is4k) 3840f * 2160f else 1920f * 1080f
            val dataDensityBpp = targetEncodingBitrateBps.toFloat() / (pixelsPerFrame * videoFps.fps.toFloat())

            val qualityNames = prioritizedQualityOrder.joinToString("→") { q ->
                when (q) {
                    Quality.UHD -> "UHD(4K)"
                    Quality.FHD -> "FHD(1080p)"
                    Quality.HD -> "HD(720p)"
                    Quality.SD -> "SD(480p)"
                    Quality.HIGHEST -> "HIGHEST"
                    Quality.LOWEST -> "LOWEST"
                    else -> "Q"
                }
            }

            val profileSummary = buildString {
                append("${videoResolution.shortLabel}@${videoFps.fps}fps")
                append(" | Profile=${videoHdrProfile.badgeLabel}(${videoHdrProfile.dynamicRangeStops})")
                append(" | QSelector=[$qualityNames]")
                append(" | Bitrate=${effectiveBitrateMbps}Mbps")
                if (is4k60HighDensityMode && effectiveBitrateMbps > clampedRequestedMbps) {
                    append(" (4K${videoFps.fps} Pro Boost from ${clampedRequestedMbps}Mbps)")
                }
                append(" | Density=${String.format(Locale.US, "%.2f", dataDensityBpp)} bpp")
            }

            return VideoQualityAndBitrateConfig(
                qualitySelector = qualitySelector,
                prioritizedQualityOrder = prioritizedQualityOrder,
                requestedBitrateMbps = clampedRequestedMbps,
                effectiveBitrateMbps = effectiveBitrateMbps,
                targetEncodingBitrateBps = targetEncodingBitrateBps,
                is4k60HighDensityMode = is4k60HighDensityMode,
                dataDensityBitsPerPixelFrame = dataDensityBpp,
                videoHdrProfile = videoHdrProfile,
                negotiatedDynamicRange = negotiatedDynamicRange,
                profileSummary = profileSummary
            )
        }
    }
}

data class VideoQualityAndBitrateConfig(
    val qualitySelector: QualitySelector,
    val prioritizedQualityOrder: List<Quality>,
    val requestedBitrateMbps: Int,
    val effectiveBitrateMbps: Int,
    val targetEncodingBitrateBps: Int,
    val is4k60HighDensityMode: Boolean,
    val dataDensityBitsPerPixelFrame: Float,
    val videoHdrProfile: VideoHdrProfileSetting = VideoHdrProfileSetting.HDR10_HLG,
    val negotiatedDynamicRange: DynamicRange = DynamicRange.SDR,
    val profileSummary: String
)

sealed interface VideoRecordStatus {
    data class Recording(
        val isPaused: Boolean,
        val durationNs: Long,
        val bytesRecorded: Long
    ) : VideoRecordStatus
    data class Finalized(val outputUri: Uri) : VideoRecordStatus
    data class Error(val message: String) : VideoRecordStatus
}
