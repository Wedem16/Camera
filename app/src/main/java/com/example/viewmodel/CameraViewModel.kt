package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AiSceneOptimizerRepository
import com.example.data.CameraHardwareController
import com.example.data.MediaGalleryRepository
import com.example.data.OnDeviceSuperResolutionEngine
import com.example.data.VideoRecordStatus
import com.example.model.AiAutoEditRecipe
import com.example.model.AiCoachUiState
import com.example.model.AiUpscaleMode
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.CropAspectPreset
import com.example.model.DiagnosticLogEntry
import com.example.model.FlashSetting
import com.example.model.GeminiSceneCritique
import com.example.model.HardwareCapabilities
import com.example.model.HonorAiGenPreset
import com.example.model.HonorAiGenUiState
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.MediaItem
import com.example.model.PhotoEditState
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoFormatSetting
import com.example.model.PhotoResolutionSetting
import com.example.model.ProCameraSettings
import com.example.model.ProParameterTab
import com.example.model.RealTimeSceneMetrics
import com.example.model.TimerSetting
import com.example.model.VideoFormatSetting
import com.example.model.VideoFpsSetting
import com.example.model.VideoResolutionSetting
import com.example.model.WhiteBalancePreset
import com.example.model.ZoomOptimizationProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class ActiveAppScreen {
    CAMERA_VIEWFINDER,
    GALLERY_VIEWER,
    PHOTO_EDITOR
}

data class CameraUiState(
    val activeScreen: ActiveAppScreen = ActiveAppScreen.CAMERA_VIEWFINDER,
    val hasCameraPermission: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val activeMode: CameraMode = CameraMode.PHOTO,
    val pinnedModes: List<CameraMode> = listOf(
        CameraMode.PORTRAIT,
        CameraMode.PHOTO,
        CameraMode.VIDEO,
        CameraMode.PRO
    ),
    val showMoreModesSheet: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAiCoachSheet: Boolean = false,
    val showLutLogSheet: Boolean = false,
    val showBitrateSheet: Boolean = false,
    val showDiagnosticLogsOverlay: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashSetting: FlashSetting = FlashSetting.AUTO,
    val torchEnabled: Boolean = false,
    val timerSetting: TimerSetting = TimerSetting.OFF,
    val activeCountdownSec: Int = 0,
    val photoResolution: PhotoResolutionSetting = PhotoResolutionSetting.UHD_4K,
    val photoFormat: PhotoFormatSetting = PhotoFormatSetting.JPEG,
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_FULL,
    val videoResolution: VideoResolutionSetting = VideoResolutionSetting.UHD_4K,
    val videoFormat: VideoFormatSetting = VideoFormatSetting.MP4_AVC,
    val videoFps: VideoFpsSetting = VideoFpsSetting.FPS_60,
    val videoBitrateMbps: Int = 250,
    // Step 3: Creative LUTs & watermarks are OFF by default (neutral color, no fake overlays)
    val creativeFiltersEnabled: Boolean = false,
    val activeLut: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val lutIntensity: Float = 1.0f,
    val selectedLutCategory: LutCategory = LutCategory.ALL,
    val activeLogProfile: LogProfile = LogProfile.STANDARD_REC709,
    val videoStabilizationEnabled: Boolean = true,
    val gridLinesEnabled: Boolean = true,
    val watermarkEnabled: Boolean = false,
    val volumeButtonShutterEnabled: Boolean = true,
    val aiSceneOptimizerEnabled: Boolean = false,
    // Step 5: Optional On-Device AI Upscale mode (Original / AI Upscale 2x / AI Upscale 4x)
    val aiUpscaleMode: AiUpscaleMode = AiUpscaleMode.ORIGINAL,
    // Automatic Post-Capture Honor 600 AI Generation & Upscale
    val aiAutoEditAfterCaptureEnabled: Boolean = true,
    val selectedHonorAiPreset: HonorAiGenPreset = HonorAiGenPreset.AIGC_SUPER_RES,
    val honorAiGenUiState: HonorAiGenUiState = HonorAiGenUiState.Idle,
    val showHonorAiGenSheet: Boolean = false,
    val customHonorAiPrompt: String = "",
    val isUpscalingInProgress: Boolean = false,
    val upscaleProgressPercent: Int = 0,
    val upscaleStatusStage: String = "",
    val lastAiAutoEditRecipe: AiAutoEditRecipe? = null,
    val isApplyingEditorAi: Boolean = false,
    val aiZoomAutoEnhanceEnabled: Boolean = false,
    val currentZoomRatio: Float = 1.0f,
    val extraHybridZoomFactor: Float = 1.0f,
    val portraitBlurStrength: Float = 0.65f,
    val beautySmoothness: Float = 0.55f,
    val proSettings: ProCameraSettings = ProCameraSettings(),
    val activeProTab: ProParameterTab = ProParameterTab.ISO,
    val hardwareCapabilities: HardwareCapabilities = HardwareCapabilities(),
    val focusPoint: Offset? = null,
    val touchExposureEv: Float = 0f,
    val shutterFlashTriggered: Boolean = false,
    val isCapturingPhoto: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val isVideoPaused: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val recordingBytes: Long = 0L,
    val sceneMetrics: RealTimeSceneMetrics = RealTimeSceneMetrics(),
    val aiCoachState: AiCoachUiState = AiCoachUiState.Idle,
    val diagnosticLogs: List<DiagnosticLogEntry> = emptyList(),
    val mediaList: List<MediaItem> = emptyList(),
    val selectedMediaItem: MediaItem? = null,
    val photoEditState: PhotoEditState = PhotoEditState(),
    val isSavingEdit: Boolean = false,
    val statusBannerMessage: String? = null
) {
    val activeZoomProfile: ZoomOptimizationProfile
        get() = ZoomOptimizationProfile.fromZoomRatio(currentZoomRatio)
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val hardwareController = CameraHardwareController(application)
    private val aiRepository = AiSceneOptimizerRepository()
    private val galleryRepository = MediaGalleryRepository(application)
    private val srEngine = OnDeviceSuperResolutionEngine(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val logIdGenerator = AtomicLong(1L)
    private var focusDismissJob: Job? = null
    private var bannerDismissJob: Job? = null
    private var countdownJob: Job? = null
    private var sceneSamplerJob: Job? = null

    private val realTimeAnalyzer = aiRepository.createRealTimeAnalyzer { metrics ->
        _uiState.update { it.copy(sceneMetrics = metrics) }
    }

    init {
        appendDiagnosticLog(
            "INIT",
            "Honor 600 Cam | ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | Zero-Lag 60fps 4K Pipeline"
        )
        refreshGallery()
    }

    /**
     * Samples a single viewfinder frame on-demand (e.g., when switching to PRO mode or opening AI Coach)
     * rather than polling `PreviewView.getBitmap()` in a background loop, ensuring zero GPU readback stalls
     * and a locked 60fps viewfinder.
     */
    fun refreshSceneMetricsOnDemand() {
        sceneSamplerJob?.cancel()
        sceneSamplerJob = viewModelScope.launch {
            val current = _uiState.value
            if (current.activeScreen == ActiveAppScreen.CAMERA_VIEWFINDER &&
                current.hasCameraPermission &&
                !current.isCapturingPhoto
            ) {
                val bmp = hardwareController.grabViewfinderBitmap()
                if (bmp != null) {
                    val metrics = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        aiRepository.computeSceneMetricsFromBitmap(bmp)
                    }
                    _uiState.update { it.copy(sceneMetrics = metrics) }
                }
            }
        }
    }

    fun appendDiagnosticLog(tag: String, message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = DiagnosticLogEntry(
            id = logIdGenerator.getAndIncrement(),
            timestamp = timeStr,
            tag = tag,
            message = message
        )
        _uiState.update { current ->
            val updated = (listOf(entry) + current.diagnosticLogs).take(45)
            current.copy(diagnosticLogs = updated)
        }
    }

    fun clearDiagnosticLogs() {
        _uiState.update { it.copy(diagnosticLogs = emptyList()) }
        appendDiagnosticLog("LOGS", "Diagnostic console cleared")
    }

    fun toggleDiagnosticLogsOverlay() {
        val next = !_uiState.value.showDiagnosticLogsOverlay
        _uiState.update { it.copy(showDiagnosticLogsOverlay = next) }
        showStatusBanner(if (next) "Camera Telemetry & Logs HUD ON" else "Camera Logs HUD OFF")
    }

    fun onPermissionsUpdated(cameraGranted: Boolean, audioGranted: Boolean) {
        _uiState.update {
            it.copy(
                hasCameraPermission = cameraGranted,
                hasAudioPermission = audioGranted
            )
        }
        appendDiagnosticLog("PERM", "Camera=$cameraGranted | Microphone=$audioGranted")
        if (cameraGranted) {
            refreshGallery()
        }
    }

    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val state = _uiState.value
        if (!state.hasCameraPermission) return

        hardwareController.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            isVideoMode = state.activeMode == CameraMode.VIDEO,
            activeMode = state.activeMode,
            lensFacing = state.lensFacing,
            flashSetting = state.flashSetting,
            videoResolution = state.videoResolution,
            videoFps = state.videoFps,
            videoBitrateMbps = state.videoBitrateMbps,
            videoStabilizationEnabled = state.videoStabilizationEnabled,
            proSettings = state.proSettings,
            frameAnalyzer = realTimeAnalyzer,
            onHardwareInspected = { caps ->
                _uiState.update { current ->
                    current.copy(hardwareCapabilities = caps)
                }
                val extraScale = hardwareController.setZoomRatio(_uiState.value.currentZoomRatio)
                _uiState.update { current ->
                    current.copy(extraHybridZoomFactor = extraScale)
                }
                if (_uiState.value.activeMode == CameraMode.PRO) {
                    refreshSceneMetricsOnDemand()
                }
            },
            onDiagnosticLog = { tag, msg ->
                appendDiagnosticLog(tag, msg)
            },
            onError = { errorMsg ->
                showStatusBanner(errorMsg)
            }
        )
    }

    fun selectMode(mode: CameraMode) {
        if (_uiState.value.isRecordingVideo) {
            showStatusBanner("Stop video recording before switching camera modes.")
            return
        }
        _uiState.update {
            it.copy(
                activeMode = mode,
                showMoreModesSheet = false
            )
        }
        if (mode == CameraMode.PRO) {
            refreshSceneMetricsOnDemand()
        }
        appendDiagnosticLog("MODE", "Switched to ${mode.badgeTitle} (${mode.name})")
        showStatusBanner("${mode.badgeTitle}: ${mode.description}")
    }

    fun togglePinModeInCarousel(mode: CameraMode) {
        _uiState.update { current ->
            val currentPinned = current.pinnedModes.toMutableList()
            if (currentPinned.contains(mode)) {
                if (currentPinned.size > 2) {
                    currentPinned.remove(mode)
                }
            } else {
                currentPinned.add(mode)
            }
            current.copy(pinnedModes = currentPinned)
        }
    }

    fun setShowMoreModesSheet(visible: Boolean) {
        _uiState.update { it.copy(showMoreModesSheet = visible) }
    }

    fun setShowSettingsDialog(visible: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = visible) }
    }

    fun setShowLutLogSheet(visible: Boolean) {
        _uiState.update { it.copy(showLutLogSheet = visible) }
    }

    fun setShowBitrateSheet(visible: Boolean) {
        _uiState.update { it.copy(showBitrateSheet = visible) }
    }

    // Step 3: Creative LUT & Log Controls (Gated behind Settings toggle, default OFF / neutral color)
    fun toggleCreativeFilters() {
        val next = !_uiState.value.creativeFiltersEnabled
        _uiState.update {
            it.copy(
                creativeFiltersEnabled = next,
                activeLut = if (next) it.activeLut else PhotoFilterPreset.ORIGINAL,
                activeLogProfile = if (next) it.activeLogProfile else LogProfile.STANDARD_REC709
            )
        }
        appendDiagnosticLog("FILTERS", "Creative LUT & Log Filters -> ${if (next) "ENABLED" else "OFF (Neutral)"}")
        showStatusBanner(
            if (next) "Creative Filters & LUTs Enabled"
            else "Creative Filters OFF (Neutral Color)"
        )
    }

    fun selectLutCategory(category: LutCategory) {
        _uiState.update { it.copy(selectedLutCategory = category) }
    }

    fun selectActiveLut(lut: PhotoFilterPreset) {
        _uiState.update {
            it.copy(
                activeLut = lut,
                creativeFiltersEnabled = lut != PhotoFilterPreset.ORIGINAL || it.creativeFiltersEnabled
            )
        }
        appendDiagnosticLog("LUT", "Selected LUT: ${lut.label} (${lut.category.label})")
        showStatusBanner("LUT: ${lut.label}")
    }

    fun updateLutIntensity(intensity: Float) {
        _uiState.update { it.copy(lutIntensity = intensity.coerceIn(0f, 1f)) }
    }

    fun selectLogProfile(profile: LogProfile) {
        _uiState.update {
            it.copy(
                activeLogProfile = profile,
                creativeFiltersEnabled = profile != LogProfile.STANDARD_REC709 || it.creativeFiltersEnabled
            )
        }
        appendDiagnosticLog("LOG-CURVE", "Active Log Profile: ${profile.fullTitle} (${profile.dynamicRangeStops})")
        showStatusBanner("Log Profile: ${profile.fullTitle} (${profile.dynamicRangeStops})")
    }

    // Photo & Video Formats (JPG, RAW+JPG .dng, PNG, WEBP / MP4, HEVC MP4, RAW MOV, RAW MKV)
    fun cyclePhotoFormat() {
        val entries = PhotoFormatSetting.entries
        val currentIdx = entries.indexOf(_uiState.value.photoFormat).coerceAtLeast(0)
        val next = entries[(currentIdx + 1) % entries.size]
        _uiState.update { it.copy(photoFormat = next) }
        appendDiagnosticLog("FORMAT", "Photo Format -> ${next.fullTitle} (.${next.extension})")
        if (next == PhotoFormatSetting.RAW_DNG && !_uiState.value.hardwareCapabilities.supportsRawCapability) {
            showStatusBanner("RAW+JPG selected (Note: Sensor RAW capability not reported by Camera2; saves 100% JPEG + DNG container)")
        } else {
            showStatusBanner("Photo Format: ${next.fullTitle}")
        }
    }

    fun cycleVideoFormat() {
        val entries = VideoFormatSetting.entries
        val currentIdx = entries.indexOf(_uiState.value.videoFormat).coerceAtLeast(0)
        val next = entries[(currentIdx + 1) % entries.size]
        _uiState.update { it.copy(videoFormat = next) }
        appendDiagnosticLog("VID-FMT", "Video Container Format -> ${next.fullTitle} (.${next.extension})")
        showStatusBanner("Video Format: ${next.fullTitle}")
    }

    // Step 5: Optional On-Device AI Upscale Mode ("Original" / "AI Upscale 2x" / "AI Upscale 4x")
    fun cycleAiUpscaleMode() {
        val entries = AiUpscaleMode.entries
        val currentIdx = entries.indexOf(_uiState.value.aiUpscaleMode).coerceAtLeast(0)
        val next = entries[(currentIdx + 1) % entries.size]
        selectAiUpscaleMode(next)
    }

    fun selectAiUpscaleMode(mode: AiUpscaleMode) {
        _uiState.update {
            it.copy(
                aiUpscaleMode = mode,
                aiZoomAutoEnhanceEnabled = mode != AiUpscaleMode.ORIGINAL
            )
        }
        appendDiagnosticLog("AI-UPSCALE", "AI Upscale Mode -> ${mode.uiLabel}")
        showStatusBanner(
            when (mode) {
                AiUpscaleMode.ORIGINAL -> "Capture Mode: Original Only (Unchanged Native JPEG)"
                AiUpscaleMode.UPSCALE_2X -> "AI Upscale 2x Active (Saves Original + Separate _AI2x.jpg)"
                AiUpscaleMode.UPSCALE_4X -> "AI Upscale 4x Active (Saves Original + Separate _AI4x.jpg)"
            }
        )
    }

    fun toggleAiZoomAutoEnhance() {
        cycleAiUpscaleMode()
    }

    fun toggleAiAutoEditAfterCapture() {
        val next = !_uiState.value.aiAutoEditAfterCaptureEnabled
        _uiState.update { it.copy(aiAutoEditAfterCaptureEnabled = next) }
        appendDiagnosticLog(
            "HONOR-AI",
            "Automatic Post-Capture Honor 600 AI Generation -> ${if (next) "ENABLED (${_uiState.value.selectedHonorAiPreset.title})" else "OFF"}"
        )
        showStatusBanner(
            if (next) "Honor 600 AI Gen ON (${_uiState.value.selectedHonorAiPreset.title})"
            else "Honor 600 AI Gen OFF (Saves Original Only)"
        )
    }

    fun selectHonorAiPreset(preset: HonorAiGenPreset) {
        _uiState.update {
            it.copy(
                selectedHonorAiPreset = preset,
                aiAutoEditAfterCaptureEnabled = true
            )
        }
        appendDiagnosticLog("HONOR-AI", "Selected Honor 600 AI Preset -> ${preset.title}")
        showStatusBanner("Honor 600 AI: ${preset.title}")
    }

    fun updateCustomHonorAiPrompt(prompt: String) {
        _uiState.update { it.copy(customHonorAiPrompt = prompt) }
    }

    fun openHonorAiGenSheet() {
        _uiState.update { it.copy(showHonorAiGenSheet = true) }
    }

    fun dismissHonorAiGenSheet() {
        _uiState.update { it.copy(showHonorAiGenSheet = false) }
    }

    // Video Bitrate up to 1000 Mbps
    fun updateVideoBitrateMbps(
        mbps: Int,
        lifecycleOwner: LifecycleOwner? = null,
        previewView: PreviewView? = null
    ) {
        val clamped = mbps.coerceIn(10, 1000)
        _uiState.update { it.copy(videoBitrateMbps = clamped) }
        appendDiagnosticLog("BITRATE", "Target Video Bitrate set to $clamped Mbps (${clamped * 1000} kbps)")
        if (lifecycleOwner != null && previewView != null && _uiState.value.activeMode == CameraMode.VIDEO && !_uiState.value.isRecordingVideo) {
            bindCameraToLifecycle(lifecycleOwner, previewView)
        }
    }

    fun toggleLensFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_uiState.value.isRecordingVideo) {
            showStatusBanner("Stop recording before switching lenses.")
            return
        }
        val caps = _uiState.value.hardwareCapabilities
        val nextLens = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            if (!caps.hasFrontCamera) {
                showStatusBanner("Front camera not detected on this device.")
                return
            }
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update {
            it.copy(
                lensFacing = nextLens,
                torchEnabled = false,
                currentZoomRatio = 1.0f,
                extraHybridZoomFactor = 1.0f
            )
        }
        bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun cycleFlashSetting() {
        val caps = _uiState.value.hardwareCapabilities
        if (!caps.hasFlashUnit) {
            showStatusBanner("Flash unit is not available on the current camera lens.")
            return
        }
        val next = when (_uiState.value.flashSetting) {
            FlashSetting.AUTO -> FlashSetting.ON
            FlashSetting.ON -> FlashSetting.OFF
            FlashSetting.OFF -> FlashSetting.AUTO
        }
        hardwareController.setFlashSetting(next)
        _uiState.update { it.copy(flashSetting = next) }
        appendDiagnosticLog("FLASH", "Flash mode -> ${next.label}")
        showStatusBanner("Flash: ${next.label}")
    }

    fun toggleTorchShortcut() {
        val caps = _uiState.value.hardwareCapabilities
        if (!caps.hasFlashUnit) {
            showStatusBanner("Flashlight not available on front lens.")
            return
        }
        val nextTorch = !_uiState.value.torchEnabled
        hardwareController.setTorchEnabled(nextTorch)
        _uiState.update { it.copy(torchEnabled = nextTorch) }
        appendDiagnosticLog("TORCH", "Torch shortcut -> ${if (nextTorch) "ON" else "OFF"}")
        showStatusBanner(if (nextTorch) "Flashlight Torch ON" else "Flashlight Torch OFF")
    }

    fun cycleTimerSetting() {
        val next = when (_uiState.value.timerSetting) {
            TimerSetting.OFF -> TimerSetting.SEC_3
            TimerSetting.SEC_3 -> TimerSetting.SEC_5
            TimerSetting.SEC_5 -> TimerSetting.SEC_10
            TimerSetting.SEC_10 -> TimerSetting.OFF
        }
        _uiState.update { it.copy(timerSetting = next) }
        showStatusBanner("Timer: ${next.label}")
    }

    fun cycleAspectRatio() {
        val next = when (_uiState.value.aspectRatio) {
            AspectRatioSetting.RATIO_FULL -> AspectRatioSetting.RATIO_3_4
            AspectRatioSetting.RATIO_3_4 -> AspectRatioSetting.RATIO_9_16
            AspectRatioSetting.RATIO_9_16 -> AspectRatioSetting.RATIO_1_1
            AspectRatioSetting.RATIO_1_1 -> AspectRatioSetting.RATIO_FULL
        }
        _uiState.update { it.copy(aspectRatio = next) }
        appendDiagnosticLog("ASPECT", "Aspect ratio -> ${next.label}")
        showStatusBanner("Aspect Ratio: ${next.label}")
    }

    fun togglePhotoResolution() {
        val next = when (_uiState.value.photoResolution) {
            PhotoResolutionSetting.UHD_4K -> PhotoResolutionSetting.ULTRA_HD_50MP
            PhotoResolutionSetting.ULTRA_HD_50MP -> PhotoResolutionSetting.ULTRA_HD_200MP
            PhotoResolutionSetting.ULTRA_HD_200MP -> PhotoResolutionSetting.HD_12MP
            PhotoResolutionSetting.HD_12MP -> PhotoResolutionSetting.UHD_4K
        }
        _uiState.update { it.copy(photoResolution = next) }
        appendDiagnosticLog("PHOTO-RES", "Photo resolution target -> ${next.fullTitle}")
        showStatusBanner("Photo Output: ${next.fullTitle}")
    }

    fun toggleVideoResolution(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val next = if (_uiState.value.videoResolution == VideoResolutionSetting.UHD_4K) {
            VideoResolutionSetting.FHD_1080P
        } else {
            VideoResolutionSetting.UHD_4K
        }
        val currentFps = _uiState.value.videoFps
        val resolvedConfig = CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = next,
            videoFps = currentFps,
            requestedBitrateMbps = _uiState.value.videoBitrateMbps
        )
        _uiState.update {
            it.copy(
                videoResolution = next,
                videoBitrateMbps = resolvedConfig.effectiveBitrateMbps
            )
        }
        appendDiagnosticLog("VIDEO-RES", resolvedConfig.profileSummary)
        showStatusBanner(
            if (resolvedConfig.is4k60HighDensityMode) {
                "4K 60fps Pro Quality: Prioritized ${resolvedConfig.effectiveBitrateMbps} Mbps (${String.format(Locale.US, "%.2f", resolvedConfig.dataDensityBitsPerPixelFrame)} bpp)"
            } else {
                "Video Resolution: ${next.fullTitle} (${resolvedConfig.effectiveBitrateMbps} Mbps)"
            }
        )
        bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun toggleVideoFps(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val next = if (_uiState.value.videoFps == VideoFpsSetting.FPS_60) {
            VideoFpsSetting.FPS_30
        } else {
            VideoFpsSetting.FPS_60
        }
        val currentRes = _uiState.value.videoResolution
        val resolvedConfig = CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = currentRes,
            videoFps = next,
            requestedBitrateMbps = _uiState.value.videoBitrateMbps
        )
        _uiState.update {
            it.copy(
                videoFps = next,
                videoBitrateMbps = resolvedConfig.effectiveBitrateMbps
            )
        }
        appendDiagnosticLog("FPS", resolvedConfig.profileSummary)
        showStatusBanner(
            if (resolvedConfig.is4k60HighDensityMode) {
                "4K 60fps Pro Quality: Prioritized ${resolvedConfig.effectiveBitrateMbps} Mbps (${String.format(Locale.US, "%.2f", resolvedConfig.dataDensityBitsPerPixelFrame)} bpp)"
            } else {
                "Frame Rate: ${next.label} (${resolvedConfig.effectiveBitrateMbps} Mbps)"
            }
        )
        bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun toggleVideoStabilization() {
        val next = !_uiState.value.videoStabilizationEnabled
        _uiState.update { it.copy(videoStabilizationEnabled = next) }
        hardwareController.applyCamera2Parameters(_uiState.value.proSettings, next)
        appendDiagnosticLog("OIS/EIS", "Optical Image Stabilization (OIS) + EIS -> $next")
        showStatusBanner(if (next) "OIS + Video Stabilization ON" else "OIS / Video Stabilization OFF")
    }

    fun toggleAiSceneOptimizer() {
        val next = !_uiState.value.aiSceneOptimizerEnabled
        _uiState.update { it.copy(aiSceneOptimizerEnabled = next) }
        showStatusBanner(if (next) "AI Scene Optimizer Enabled" else "AI Scene Optimizer Disabled (Neutral)")
    }

    fun toggleGridLines() {
        _uiState.update { it.copy(gridLinesEnabled = !it.gridLinesEnabled) }
    }

    fun toggleWatermark() {
        val next = !_uiState.value.watermarkEnabled
        _uiState.update { it.copy(watermarkEnabled = next) }
        showStatusBanner(if (next) "Timestamp Watermark Enabled" else "Watermark OFF (Clean Image)")
    }

    fun toggleVolumeButtonShutter() {
        _uiState.update { it.copy(volumeButtonShutterEnabled = !it.volumeButtonShutterEnabled) }
    }

    fun setZoomRatio(ratio: Float) {
        setZoomRatio(ratio, isContinuousGesture = false)
    }

    fun setZoomRatio(ratio: Float, isContinuousGesture: Boolean) {
        val effective = ratio.coerceIn(0.6f, 10.0f)
        if (isContinuousGesture && kotlin.math.abs(effective - _uiState.value.currentZoomRatio) < 0.015f) {
            return
        }
        val extraScale = hardwareController.setZoomRatio(effective)
        val caps = _uiState.value.hardwareCapabilities
        val profile = ZoomOptimizationProfile.fromZoomRatio(effective)
        _uiState.update {
            it.copy(
                currentZoomRatio = effective,
                extraHybridZoomFactor = extraScale
            )
        }

        if (!isContinuousGesture) {
            val lensRoutingNote = when {
                effective > 2.0f && caps.telephotoPhysicalCameraId != null ->
                    "Physical Telephoto Lens (ID ${caps.telephotoPhysicalCameraId})"
                effective > 2.0f ->
                    "Optical/HAL Zoom (${String.format(Locale.US, "%.1fx", effective.coerceIn(caps.sensorMinZoomRatio, caps.sensorMaxZoomRatio))}, no center-crop upscale)"
                effective < 1.0f && caps.supportsUltraWideOptical ->
                    "Physical Ultra-Wide Lens"
                effective < 1.0f ->
                    "1.0x Wide Native (0.6x physical UW not exposed to third-party apps)"
                else ->
                    "Main Wide Native Sensor"
            }

            appendDiagnosticLog(
                "ZOOM",
                "${profile.badgeLabel} -> $lensRoutingNote"
            )
            if (effective in listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)) {
                showStatusBanner("${profile.badgeLabel}: $lensRoutingNote")
            }
        }
    }

    fun onViewfinderTapped(x: Float, y: Float) {
        hardwareController.triggerTapToFocus(x, y)
        _uiState.update {
            it.copy(
                focusPoint = Offset(x, y),
                touchExposureEv = it.proSettings.exposureCompensationIndex * it.hardwareCapabilities.exposureStepEv
            )
        }
        appendDiagnosticLog("AF/AE", "Tap-to-focus locked at (${x.toInt()}, ${y.toInt()})")
        focusDismissJob?.cancel()
        focusDismissJob = viewModelScope.launch {
            delay(3500L)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun onTouchExposureAdjusted(evValue: Float) {
        val caps = _uiState.value.hardwareCapabilities
        val step = caps.exposureStepEv.coerceAtLeast(0.1f)
        val index = (evValue / step).toInt().coerceIn(caps.minExposureIndex, caps.maxExposureIndex)
        hardwareController.setExposureCompensation(index)
        _uiState.update {
            it.copy(
                touchExposureEv = index * step,
                proSettings = it.proSettings.copy(exposureCompensationIndex = index)
            )
        }
        focusDismissJob?.cancel()
        focusDismissJob = viewModelScope.launch {
            delay(3500L)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun setPortraitBlurStrength(strength: Float) {
        _uiState.update { it.copy(portraitBlurStrength = strength.coerceIn(0f, 1f)) }
    }

    fun setBeautySmoothness(smoothness: Float) {
        _uiState.update { it.copy(beautySmoothness = smoothness.coerceIn(0f, 1f)) }
    }

    // Pro Mode Controls
    fun selectProTab(tab: ProParameterTab) {
        _uiState.update {
            it.copy(activeProTab = if (it.activeProTab == tab) ProParameterTab.NONE else tab)
        }
    }

    fun updateProIso(iso: Int?) {
        val next = _uiState.value.proSettings.copy(iso = iso)
        _uiState.update { it.copy(proSettings = next) }
        hardwareController.applyCamera2Parameters(next, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO-ISO", "ISO -> ${next.formattedIso}")
    }

    fun updateProShutter(shutterNs: Long?) {
        val next = _uiState.value.proSettings.copy(shutterSpeedNs = shutterNs)
        _uiState.update { it.copy(proSettings = next) }
        hardwareController.applyCamera2Parameters(next, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO-SHUTTER", "Shutter Speed -> ${next.formattedShutter}")
    }

    fun updateProWhiteBalance(preset: WhiteBalancePreset, customKelvin: Int = _uiState.value.proSettings.customKelvin) {
        val next = _uiState.value.proSettings.copy(
            whiteBalance = preset,
            customKelvin = customKelvin.coerceIn(2300, 10000)
        )
        _uiState.update { it.copy(proSettings = next) }
        hardwareController.applyCamera2Parameters(next, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO-WB", "White Balance -> ${next.formattedWb}")
    }

    fun updateProManualFocus(focusDist: Float?) {
        val next = _uiState.value.proSettings.copy(manualFocusDistance = focusDist)
        _uiState.update { it.copy(proSettings = next) }
        hardwareController.applyCamera2Parameters(next, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO-FOCUS", "Focus -> ${next.formattedFocus}")
    }

    fun updateProExposureIndex(index: Int) {
        val caps = _uiState.value.hardwareCapabilities
        val clamped = index.coerceIn(caps.minExposureIndex, caps.maxExposureIndex)
        val next = _uiState.value.proSettings.copy(exposureCompensationIndex = clamped)
        _uiState.update { it.copy(proSettings = next) }
        hardwareController.applyCamera2Parameters(next, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO-EV", "Exposure Index -> $clamped")
    }

    fun resetProSettings() {
        val reset = ProCameraSettings()
        _uiState.update { it.copy(proSettings = reset) }
        hardwareController.applyCamera2Parameters(reset, _uiState.value.videoStabilizationEnabled)
        appendDiagnosticLog("PRO", "Reset all Pro Camera2 parameters to AUTO")
        showStatusBanner("Pro settings reset to Auto")
    }

    // Shutter & Recording Triggers
    fun onPrimaryShutterPressed() {
        val state = _uiState.value
        if (state.activeMode == CameraMode.VIDEO) {
            toggleVideoRecording()
        } else {
            triggerPhotoCaptureWithTimer()
        }
    }

    fun onVolumeKeyShutterPressed(): Boolean {
        if (!_uiState.value.volumeButtonShutterEnabled) return false
        if (_uiState.value.activeScreen != ActiveAppScreen.CAMERA_VIEWFINDER) return false
        onPrimaryShutterPressed()
        return true
    }

    private fun triggerPhotoCaptureWithTimer() {
        if (_uiState.value.isCapturingPhoto) return
        if (countdownJob?.isActive == true) {
            countdownJob?.cancel()
            _uiState.update { it.copy(activeCountdownSec = 0) }
            showStatusBanner("Timer cancelled")
            return
        }

        val timerSec = _uiState.value.timerSetting.seconds
        if (timerSec > 0) {
            countdownJob = viewModelScope.launch {
                for (remaining in timerSec downTo 1) {
                    _uiState.update { it.copy(activeCountdownSec = remaining) }
                    delay(1000L)
                }
                _uiState.update { it.copy(activeCountdownSec = 0) }
                executeImmediatePhotoCapture()
            }
        } else {
            executeImmediatePhotoCapture()
        }
    }

    /**
     * Captures the photo from CameraX ImageCapture (`CAPTURE_MODE_MAXIMIZE_QUALITY`):
     * 1. Always saves the unchanged original JPEG (`IMG_xxx.jpg`) to `Pictures/Camera/` first.
     * 2. If `aiUpscaleMode` is `UPSCALE_2X` or `UPSCALE_4X`, runs tiled super-resolution on
     *    `Dispatchers.Default` with live progress updates and saves a SEPARATE file
     *    (`IMG_xxx_AI2x.jpg` or `IMG_xxx_AI4x.jpg`) without overwriting the original.
     */
    fun executeImmediatePhotoCapture() {
        val state = _uiState.value
        if (state.isCapturingPhoto) return

        _uiState.update {
            it.copy(
                isCapturingPhoto = true,
                shutterFlashTriggered = true
            )
        }
        appendDiagnosticLog(
            "CAPTURE",
            "Capturing Native ${state.photoFormat.badgeLabel} | Zoom=${String.format(Locale.US, "%.1fx", state.currentZoomRatio)} | Upscale=${state.aiUpscaleMode.uiLabel}"
        )

        viewModelScope.launch {
            delay(90L)
            _uiState.update { it.copy(shutterFlashTriggered = false) }
        }

        val isFront = state.lensFacing == CameraSelector.LENS_FACING_FRONT
        hardwareController.capturePhotoBitmap(
            isFrontLens = isFront,
            onBitmapCaptured = { capturedBitmap ->
                viewModelScope.launch {
                    val savedCapture = galleryRepository.processAndSaveCapturedPhoto(
                        rawBitmap = capturedBitmap,
                        mode = state.activeMode,
                        resolution = state.photoResolution,
                        photoFormat = state.photoFormat,
                        aspectRatio = state.aspectRatio,
                        proSettings = state.proSettings,
                        activeLut = state.activeLut,
                        lutIntensity = state.lutIntensity,
                        activeLogProfile = state.activeLogProfile,
                        creativeFiltersEnabled = state.creativeFiltersEnabled,
                        aiSceneOptimizerEnabled = state.aiSceneOptimizerEnabled,
                        currentZoomRatio = state.currentZoomRatio,
                        extraHybridZoomFactor = state.extraHybridZoomFactor,
                        detectedScene = state.sceneMetrics.detectedScene,
                        portraitBlurStrength = state.portraitBlurStrength,
                        beautySmoothness = state.beautySmoothness,
                        addWatermark = state.watermarkEnabled
                    )
                    _uiState.update { it.copy(isCapturingPhoto = false) }

                    if (savedCapture != null) {
                        val origW = savedCapture.originalBitmap.width
                        val origH = savedCapture.originalBitmap.height
                        appendDiagnosticLog(
                            "MEDIASTORE",
                            "Saved Original IMG_${savedCapture.baseTimestamp}.jpg (${origW}×${origH}) -> Pictures/Camera/"
                        )
                        refreshGallery()

                        val upscaleMode = state.aiUpscaleMode
                        if (state.aiAutoEditAfterCaptureEnabled) {
                            // Fast On-Device Smart HDR + Bilateral High-ISO Denoise + Tiled Super-Resolution (Zero Cloud Delay)
                            val targetFactor = if (upscaleMode == AiUpscaleMode.UPSCALE_4X) 4 else 2
                            val effectiveUpscaleMode = if (targetFactor == 4) {
                                AiUpscaleMode.UPSCALE_4X
                            } else {
                                AiUpscaleMode.UPSCALE_2X
                            }
                            _uiState.update {
                                it.copy(
                                    isUpscalingInProgress = true,
                                    upscaleProgressPercent = 12,
                                    upscaleStatusStage = "Honor 600 On-Device Smart HDR + ${targetFactor}x Denoise SR..."
                                )
                            }
                            showStatusBanner(
                                "Saved Original (${origW}×${origH}). Running Fast On-Device HDR + ${targetFactor}x Super-Res..."
                            )

                            val recipe = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                val metrics = aiRepository.computeSceneMetricsFromBitmap(savedCapture.originalBitmap)
                                _uiState.update { it.copy(sceneMetrics = metrics) }
                                aiRepository.computeOnDeviceHistogramAutoEditRecipe(
                                    bitmap = savedCapture.originalBitmap,
                                    upscaleFactor = targetFactor
                                )
                            }

                            _uiState.update {
                                it.copy(
                                    lastAiAutoEditRecipe = recipe,
                                    upscaleProgressPercent = 25,
                                    upscaleStatusStage = "${recipe.styleTitle} • ${targetFactor}x Denoise & SR..."
                                )
                            }

                            val (editedUri, srResult) = galleryRepository.saveAiAutoEditedAndUpscaledPhoto(
                                originalBitmap = savedCapture.originalBitmap,
                                baseTimestamp = savedCapture.baseTimestamp,
                                recipe = recipe,
                                upscaleMode = effectiveUpscaleMode,
                                zoomRatio = state.currentZoomRatio,
                                srEngine = srEngine,
                                onProgress = { pct ->
                                    _uiState.update {
                                        it.copy(upscaleProgressPercent = (25 + (pct * 0.75f).toInt()).coerceIn(25, 100))
                                    }
                                }
                            )

                            _uiState.update {
                                it.copy(
                                    isUpscalingInProgress = false,
                                    upscaleProgressPercent = 0,
                                    upscaleStatusStage = ""
                                )
                            }

                            if (editedUri != null && srResult != null) {
                                appendDiagnosticLog(
                                    "HONOR-HDR-SR",
                                    "Saved ${recipe.styleTitle} (${srResult.bitmap.width}×${srResult.bitmap.height}) in ${srResult.elapsedMs}ms"
                                )
                                refreshGallery()
                                showStatusBanner(
                                    "Saved Original + ${recipe.styleTitle} (${srResult.bitmap.width}×${srResult.bitmap.height})"
                                )
                            }
                        } else if (upscaleMode != AiUpscaleMode.ORIGINAL) {
                            _uiState.update {
                                it.copy(
                                    isUpscalingInProgress = true,
                                    upscaleProgressPercent = 5,
                                    upscaleStatusStage = "Running ${upscaleMode.uiLabel}..."
                                )
                            }
                            showStatusBanner(
                                "Saved Original (${origW}×${origH}). Running ${upscaleMode.uiLabel}..."
                            )
                            appendDiagnosticLog(
                                "AI-SR",
                                "Starting tiled ${upscaleMode.uiLabel} on background thread..."
                            )

                            val (upscaledUri, srResult) = galleryRepository.saveAiUpscaledSeparatePhoto(
                                originalBitmap = savedCapture.originalBitmap,
                                baseTimestamp = savedCapture.baseTimestamp,
                                upscaleMode = upscaleMode,
                                zoomRatio = state.currentZoomRatio,
                                srEngine = srEngine,
                                onProgress = { pct ->
                                    _uiState.update { it.copy(upscaleProgressPercent = pct) }
                                }
                            )

                            _uiState.update {
                                it.copy(
                                    isUpscalingInProgress = false,
                                    upscaleProgressPercent = 0,
                                    upscaleStatusStage = ""
                                )
                            }

                            if (upscaledUri != null && srResult != null) {
                                appendDiagnosticLog(
                                    "AI-SR",
                                    "Saved IMG_${savedCapture.baseTimestamp}${upscaleMode.fileSuffix}.jpg (${srResult.bitmap.width}×${srResult.bitmap.height}) via ${srResult.engineDescription} in ${srResult.elapsedMs}ms"
                                )
                                refreshGallery()
                                showStatusBanner(
                                    "Saved Original + IMG_${savedCapture.baseTimestamp}${upscaleMode.fileSuffix}.jpg (${srResult.bitmap.width}×${srResult.bitmap.height})"
                                )
                            } else {
                                showStatusBanner("Original photo saved; AI upscale failed.")
                            }
                        } else {
                            showStatusBanner(
                                "Saved Original Photo (${origW}×${origH}) to Pictures/Camera/"
                            )
                        }
                    } else {
                        appendDiagnosticLog("ERROR", "Failed saving photo to MediaStore")
                        showStatusBanner("Failed to save photo to MediaStore.")
                    }
                }
            },
            onError = { msg ->
                _uiState.update { it.copy(isCapturingPhoto = false) }
                appendDiagnosticLog("ERROR", msg)
                showStatusBanner(msg)
            }
        )
    }

    private fun toggleVideoRecording() {
        val state = _uiState.value
        if (state.isRecordingVideo) {
            hardwareController.stopVideoRecording()
        } else {
            hardwareController.startVideoRecording(
                enableAudio = state.hasAudioPermission,
                videoBitrateMbps = state.videoBitrateMbps,
                videoResolution = state.videoResolution,
                videoFormat = state.videoFormat,
                onDiagnosticLog = { tag, msg -> appendDiagnosticLog(tag, msg) },
                onEvent = { status ->
                    when (status) {
                        is VideoRecordStatus.Recording -> {
                            _uiState.update {
                                it.copy(
                                    isRecordingVideo = true,
                                    isVideoPaused = status.isPaused,
                                    recordingDurationMs = status.durationNs / 1_000_000L,
                                    recordingBytes = status.bytesRecorded
                                )
                            }
                        }
                        is VideoRecordStatus.Finalized -> {
                            _uiState.update {
                                it.copy(
                                    isRecordingVideo = false,
                                    isVideoPaused = false,
                                    recordingDurationMs = 0L,
                                    recordingBytes = 0L
                                )
                            }
                            refreshGallery()
                            showStatusBanner(
                                "Saved ${state.videoResolution.shortLabel} ${state.videoFormat.badgeLabel} (.${state.videoFormat.extension}) @ ${state.videoBitrateMbps} Mbps"
                            )
                        }
                        is VideoRecordStatus.Error -> {
                            _uiState.update {
                                it.copy(
                                    isRecordingVideo = false,
                                    isVideoPaused = false,
                                    recordingDurationMs = 0L,
                                    recordingBytes = 0L
                                )
                            }
                            showStatusBanner(status.message)
                        }
                    }
                }
            )
        }
    }

    fun toggleVideoPauseResume() {
        if (!_uiState.value.isRecordingVideo) return
        if (_uiState.value.isVideoPaused) {
            hardwareController.resumeVideoRecording()
        } else {
            hardwareController.pauseVideoRecording()
        }
    }

    // Hybrid AI Scene Coach (Firebase AI Logic - gemini-2.5-flash)
    fun requestGeminiSceneCritique() {
        _uiState.update {
            it.copy(
                showAiCoachSheet = true,
                aiCoachState = AiCoachUiState.Analyzing
            )
        }
        appendDiagnosticLog("AI-COACH", "Sending frame to Gemini Vision Coach...")
        viewModelScope.launch {
            val frame: Bitmap = hardwareController.grabViewfinderBitmap()
                ?: BitmapFactory.decodeResource(
                    getApplication<Application>().resources,
                    R.drawable.img_camera_hero_lens_1794590959493
                )
                ?: Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)

            try {
                val state = _uiState.value
                val critique = aiRepository.analyzeFrameWithGemini(
                    frameBitmap = frame,
                    currentModeLabel = state.activeMode.badgeTitle,
                    detectedSceneLabel = state.sceneMetrics.detectedScene.displayTitle
                )
                appendDiagnosticLog("AI-COACH", "Score=${critique.compositionScore}/100 | ISO=${critique.recommendedIso} | Shutter=${critique.recommendedShutterLabel}")
                _uiState.update { it.copy(aiCoachState = AiCoachUiState.Success(critique)) }
            } catch (e: Exception) {
                appendDiagnosticLog("AI-ERROR", e.localizedMessage ?: "AI Coach error")
                _uiState.update {
                    it.copy(
                        aiCoachState = AiCoachUiState.Error(
                            e.localizedMessage ?: "Unable to complete AI Scene Critique."
                        )
                    )
                }
            }
        }
    }

    fun applyAiRecommendedProSettings(critique: GeminiSceneCritique) {
        val shutterNs = parseShutterLabelToNs(critique.recommendedShutterLabel)
        val caps = _uiState.value.hardwareCapabilities
        val evStep = caps.exposureStepEv.coerceAtLeast(0.1f)
        val evIndex = (critique.recommendedEvShift / evStep).toInt()
            .coerceIn(caps.minExposureIndex, caps.maxExposureIndex)

        val updatedPro = _uiState.value.proSettings.copy(
            iso = critique.recommendedIso,
            shutterSpeedNs = shutterNs,
            whiteBalance = WhiteBalancePreset.MANUAL_KELVIN,
            customKelvin = critique.recommendedKelvin,
            exposureCompensationIndex = evIndex
        )
        _uiState.update {
            it.copy(
                activeMode = CameraMode.PRO,
                proSettings = updatedPro,
                showAiCoachSheet = false
            )
        }
        hardwareController.applyCamera2Parameters(updatedPro, _uiState.value.videoStabilizationEnabled)
        showStatusBanner("Applied AI Coach Pro Settings (ISO ${critique.recommendedIso}, ${critique.recommendedShutterLabel}, ${critique.recommendedKelvin}K)")
    }

    fun dismissAiCoachSheet() {
        _uiState.update { it.copy(showAiCoachSheet = false) }
    }

    private fun parseShutterLabelToNs(label: String): Long {
        val clean = label.trim().lowercase().removeSuffix("s")
        return if (clean.startsWith("1/")) {
            val denom = clean.removePrefix("1/").toLongOrNull() ?: 125L
            (1_000_000_000L / denom.coerceAtLeast(1L))
        } else {
            val secs = clean.toDoubleOrNull() ?: 0.008
            (secs * 1_000_000_000L).toLong()
        }
    }

    // Gallery & Photo Studio Editor
    fun refreshGallery() {
        viewModelScope.launch {
            val items = galleryRepository.loadCapturedMedia()
            _uiState.update { current ->
                val selected = current.selectedMediaItem?.let { prev ->
                    items.find { it.id == prev.id }
                } ?: items.firstOrNull()
                current.copy(
                    mediaList = items,
                    selectedMediaItem = selected
                )
            }
        }
    }

    fun openGallery() {
        refreshGallery()
        _uiState.update { it.copy(activeScreen = ActiveAppScreen.GALLERY_VIEWER) }
    }

    fun closeGalleryToCamera() {
        _uiState.update { it.copy(activeScreen = ActiveAppScreen.CAMERA_VIEWFINDER) }
    }

    fun selectMediaItem(item: MediaItem) {
        _uiState.update { it.copy(selectedMediaItem = item) }
    }

    fun importStudioSamplePhoto() {
        viewModelScope.launch {
            val uri = galleryRepository.createStudioSamplePhoto()
            if (uri != null) {
                val items = galleryRepository.loadCapturedMedia()
                _uiState.update {
                    it.copy(
                        mediaList = items,
                        selectedMediaItem = items.firstOrNull()
                    )
                }
                appendDiagnosticLog("SAMPLE", "Imported Studio Sample Shot to Pictures/Camera/")
                showStatusBanner("Imported Studio Sample Shot")
            }
        }
    }

    fun openPhotoEditor(item: MediaItem) {
        if (item.isVideo) {
            showStatusBanner("Photo Studio edits still photos; use video player controls below.")
            return
        }
        _uiState.update {
            it.copy(
                selectedMediaItem = item,
                photoEditState = PhotoEditState(
                    filter = if (it.creativeFiltersEnabled) it.activeLut else PhotoFilterPreset.ORIGINAL,
                    logProfile = if (it.creativeFiltersEnabled) it.activeLogProfile else LogProfile.STANDARD_REC709
                ),
                activeScreen = ActiveAppScreen.PHOTO_EDITOR
            )
        }
    }

    fun closePhotoEditor() {
        _uiState.update { it.copy(activeScreen = ActiveAppScreen.GALLERY_VIEWER) }
    }

    fun updateEditFilter(filter: PhotoFilterPreset) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(filter = filter)) }
    }

    fun updateEditLogProfile(logProfile: LogProfile) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(logProfile = logProfile)) }
    }

    fun updateEditBrightness(value: Float) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(brightness = value)) }
    }

    fun updateEditContrast(value: Float) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(contrast = value)) }
    }

    fun updateEditSaturation(value: Float) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(saturation = value)) }
    }

    fun updateEditWarmth(value: Float) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(warmth = value)) }
    }

    fun updateEditVignette(value: Float) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(vignette = value)) }
    }

    fun rotateEditedPhoto90() {
        _uiState.update {
            val nextRot = (it.photoEditState.rotationDegrees + 90) % 360
            it.copy(photoEditState = it.photoEditState.copy(rotationDegrees = nextRot))
        }
    }

    fun updateEditCropAspect(crop: CropAspectPreset) {
        _uiState.update { it.copy(photoEditState = it.photoEditState.copy(cropAspect = crop)) }
    }

    fun toggleEditWatermark() {
        _uiState.update {
            it.copy(photoEditState = it.photoEditState.copy(showGalaxyWatermark = !it.photoEditState.showGalaxyWatermark))
        }
    }

    fun resetPhotoEdits() {
        _uiState.update { it.copy(photoEditState = PhotoEditState()) }
    }

    fun saveEditedPhotoCopy() {
        val target = _uiState.value.selectedMediaItem ?: return
        if (_uiState.value.isSavingEdit) return
        _uiState.update { it.copy(isSavingEdit = true) }

        viewModelScope.launch {
            val savedUri = galleryRepository.saveEditedPhoto(
                sourceUri = target.uri,
                editState = _uiState.value.photoEditState
            )
            val updatedList = galleryRepository.loadCapturedMedia()
            _uiState.update {
                it.copy(
                    isSavingEdit = false,
                    mediaList = updatedList,
                    selectedMediaItem = updatedList.firstOrNull(),
                    activeScreen = ActiveAppScreen.GALLERY_VIEWER
                )
            }
            if (savedUri != null) {
                appendDiagnosticLog("EDITOR", "Saved edited photo copy -> $savedUri")
                showStatusBanner("Saved edited copy to Gallery (Original preserved)")
            } else {
                showStatusBanner("Could not save edited photo.")
            }
        }
    }

    fun deleteSelectedMediaItem() {
        val target = _uiState.value.selectedMediaItem ?: return
        viewModelScope.launch {
            val deleted = galleryRepository.deleteMediaItem(target)
            if (deleted) {
                val updatedList = galleryRepository.loadCapturedMedia()
                _uiState.update {
                    it.copy(
                        mediaList = updatedList,
                        selectedMediaItem = updatedList.firstOrNull()
                    )
                }
                showStatusBanner("Deleted ${target.displayName}")
            } else {
                showStatusBanner("Unable to delete media file.")
            }
        }
    }

    fun shareSelectedMediaItem() {
        val target = _uiState.value.selectedMediaItem ?: return
        galleryRepository.shareMediaItem(target)
    }

    fun loadPreviewBitmapForEditor(item: MediaItem, onLoaded: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                galleryRepository.decodeBitmapFromUri(item.uri, maxDimension = 1600)
            }
            onLoaded(bmp)
        }
    }

    fun renderPreviewWithEdits(source: Bitmap, editState: PhotoEditState): Bitmap {
        return galleryRepository.renderPhotoEdits(source, editState)
    }

    /**
     * Runs Honor 600 AI Image Generation (`gemini-nano-banana-2.1`) on the selected photo in the Gallery
     * and saves the synthesized & 2x super-resolution image as a separate `_HONOR_AI_*.jpg` file.
     * Surfaces any errors transparently to `HonorAiGenUiState.Error`.
     */
    fun triggerAiAutoEditAndUpscaleFromGallery(item: MediaItem) {
        if (item.isVideo) return
        val preset = _uiState.value.selectedHonorAiPreset
        val customPrompt = _uiState.value.customHonorAiPrompt

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showHonorAiGenSheet = true,
                    isUpscalingInProgress = true,
                    upscaleProgressPercent = 10,
                    upscaleStatusStage = "Honor 600 AI Generating (${preset.title})...",
                    honorAiGenUiState = HonorAiGenUiState.Loading(
                        "Synthesizing ${preset.title} with Gemini AI (may take 30–60s)..."
                    )
                )
            }
            val sourceBmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                galleryRepository.decodeBitmapFromUri(item.uri, maxDimension = 2048)
            }
            if (sourceBmp == null) {
                _uiState.update {
                    it.copy(
                        isUpscalingInProgress = false,
                        upscaleProgressPercent = 0,
                        upscaleStatusStage = "",
                        honorAiGenUiState = HonorAiGenUiState.Error("Could not decode source image from MediaStore.")
                    )
                }
                return@launch
            }

            try {
                val generatedBitmap = aiRepository.generateHonorAiReconstructedImage(
                    sourceBitmap = sourceBmp,
                    preset = preset,
                    customInstruction = customPrompt
                )
                _uiState.update {
                    it.copy(
                        upscaleProgressPercent = 70,
                        upscaleStatusStage = "${preset.title} • 2x Super-Res Upscaling...",
                        honorAiGenUiState = HonorAiGenUiState.Loading("Applying 2x Super-Resolution & saving to Gallery...")
                    )
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val (savedUri, finalBitmap) = galleryRepository.saveHonorAiGeneratedPhoto(
                    generatedBitmap = generatedBitmap,
                    baseTimestamp = timestamp,
                    presetBadge = preset.badge,
                    upscaleMode = AiUpscaleMode.UPSCALE_2X,
                    zoomRatio = 1.0f,
                    srEngine = srEngine,
                    onProgress = { pct ->
                        _uiState.update {
                            it.copy(upscaleProgressPercent = (70 + (pct * 0.30f).toInt()).coerceIn(70, 100))
                        }
                    }
                )

                val updatedList = galleryRepository.loadCapturedMedia()
                _uiState.update {
                    it.copy(
                        isUpscalingInProgress = false,
                        upscaleProgressPercent = 0,
                        upscaleStatusStage = "",
                        mediaList = updatedList,
                        selectedMediaItem = updatedList.firstOrNull() ?: item,
                        honorAiGenUiState = HonorAiGenUiState.Success(
                            bitmap = finalBitmap,
                            savedUri = savedUri,
                            presetTitle = preset.title
                        )
                    )
                }
                appendDiagnosticLog(
                    "HONOR-AI",
                    "Generated & saved Honor 600 AI photo (${finalBitmap.width}×${finalBitmap.height}): ${preset.title}"
                )
                showStatusBanner("Saved ${preset.title} (${finalBitmap.width}×${finalBitmap.height})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Honor 600 AI image generation failed."
                appendDiagnosticLog("HONOR-AI-ERR", msg)
                _uiState.update {
                    it.copy(
                        isUpscalingInProgress = false,
                        upscaleProgressPercent = 0,
                        upscaleStatusStage = "",
                        honorAiGenUiState = HonorAiGenUiState.Error(msg)
                    )
                }
            }
        }
    }

    /**
     * Analyzes the currently open photo in the Photo Studio Editor using AI (Gemini Vision +
     * On-Device Histogram fallback) and automatically tunes the editor's sliders & LUT.
     */
    fun applyAiAutoEditInEditor() {
        val target = _uiState.value.selectedMediaItem ?: return
        if (target.isVideo || _uiState.value.isApplyingEditorAi) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingEditorAi = true) }
            val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                galleryRepository.decodeBitmapFromUri(target.uri, maxDimension = 1024)
            }
            if (bmp == null) {
                _uiState.update { it.copy(isApplyingEditorAi = false) }
                showStatusBanner("Unable to analyze photo for AI Auto-Edit.")
                return@launch
            }

            val recipe = aiRepository.generateAiAutoEditRecipe(bmp, upscaleFactor = 2)
            _uiState.update { current ->
                current.copy(
                    isApplyingEditorAi = false,
                    lastAiAutoEditRecipe = recipe,
                    photoEditState = current.photoEditState.copy(
                        filter = recipe.recommendedLut,
                        brightness = recipe.brightness,
                        contrast = recipe.contrast,
                        saturation = recipe.saturation,
                        warmth = recipe.warmth,
                        vignette = recipe.vignette
                    )
                )
            }
            appendDiagnosticLog(
                "AI-EDITOR",
                "Applied ${recipe.styleTitle}: ${recipe.summary}"
            )
            showStatusBanner("${recipe.styleTitle}: ${recipe.summary}")
        }
    }

    fun showStatusBanner(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
        bannerDismissJob?.cancel()
        bannerDismissJob = viewModelScope.launch {
            delay(2800L)
            _uiState.update { it.copy(statusBannerMessage = null) }
        }
    }
}
