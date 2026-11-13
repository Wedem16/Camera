package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.data.VideoRecordStatus
import com.example.model.AiCoachUiState
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.CropAspectPreset
import com.example.model.DiagnosticLogEntry
import com.example.model.FlashSetting
import com.example.model.GeminiSceneCritique
import com.example.model.HardwareCapabilities
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.MediaItem
import com.example.model.PhotoEditState
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoResolutionSetting
import com.example.model.ProCameraSettings
import com.example.model.ProParameterTab
import com.example.model.RealTimeSceneMetrics
import com.example.model.TimerSetting
import com.example.model.VideoFpsSetting
import com.example.model.VideoResolutionSetting
import com.example.model.WhiteBalancePreset
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
    val aspectRatio: AspectRatioSetting = AspectRatioSetting.RATIO_9_16,
    val videoResolution: VideoResolutionSetting = VideoResolutionSetting.UHD_4K,
    val videoFps: VideoFpsSetting = VideoFpsSetting.FPS_60,
    val videoBitrateMbps: Int = 250, // Adjustable from 10 Mbps up to 1000 Mbps
    val activeLut: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val lutIntensity: Float = 1.0f,
    val selectedLutCategory: LutCategory = LutCategory.ALL,
    val activeLogProfile: LogProfile = LogProfile.STANDARD_REC709,
    val videoStabilizationEnabled: Boolean = true,
    val gridLinesEnabled: Boolean = true,
    val watermarkEnabled: Boolean = true,
    val volumeButtonShutterEnabled: Boolean = true,
    val aiSceneOptimizerEnabled: Boolean = true,
    val currentZoomRatio: Float = 1.0f,
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
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val hardwareController = CameraHardwareController(application)
    private val aiRepository = AiSceneOptimizerRepository()
    private val galleryRepository = MediaGalleryRepository(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val logIdGenerator = AtomicLong(1L)
    private var focusDismissJob: Job? = null
    private var bannerDismissJob: Job? = null
    private var countdownJob: Job? = null

    private val realTimeAnalyzer = aiRepository.createRealTimeAnalyzer { metrics ->
        _uiState.update { it.copy(sceneMetrics = metrics) }
    }

    init {
        appendDiagnosticLog("INIT", "Galaxy 4K UHD Engine initialized | Default 4K 3840×2160 | Bitrate 250 Mbps")
        refreshGallery()
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
        showStatusBanner(if (next) "Live Pro Telemetry & Logs HUD ON" else "Live Pro Logs HUD OFF")
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
            },
            onZoomChanged = { zoom ->
                _uiState.update { it.copy(currentZoomRatio = zoom) }
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

    // LUT & Cinema Log Controls
    fun selectLutCategory(category: LutCategory) {
        _uiState.update { it.copy(selectedLutCategory = category) }
    }

    fun selectActiveLut(lut: PhotoFilterPreset) {
        _uiState.update { it.copy(activeLut = lut) }
        appendDiagnosticLog("LUT", "Applied LUT: ${lut.label} (${lut.category.label})")
        showStatusBanner("LUT: ${lut.label}")
    }

    fun updateLutIntensity(intensity: Float) {
        _uiState.update { it.copy(lutIntensity = intensity.coerceIn(0f, 1f)) }
    }

    fun selectLogProfile(profile: LogProfile) {
        _uiState.update { it.copy(activeLogProfile = profile) }
        appendDiagnosticLog("LOG-CURVE", "Active Log Profile: ${profile.fullTitle} (${profile.dynamicRangeStops})")
        showStatusBanner("Log Profile: ${profile.fullTitle} (${profile.dynamicRangeStops})")
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
                currentZoomRatio = 1.0f
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
            AspectRatioSetting.RATIO_9_16 -> AspectRatioSetting.RATIO_3_4
            AspectRatioSetting.RATIO_3_4 -> AspectRatioSetting.RATIO_1_1
            AspectRatioSetting.RATIO_1_1 -> AspectRatioSetting.RATIO_FULL
            AspectRatioSetting.RATIO_FULL -> AspectRatioSetting.RATIO_9_16
        }
        _uiState.update { it.copy(aspectRatio = next) }
        appendDiagnosticLog("ASPECT", "Aspect ratio -> ${next.label}")
        showStatusBanner("Aspect Ratio: ${next.label}")
    }

    fun togglePhotoResolution() {
        val next = when (_uiState.value.photoResolution) {
            PhotoResolutionSetting.UHD_4K -> PhotoResolutionSetting.ULTRA_HD_50MP
            PhotoResolutionSetting.ULTRA_HD_50MP -> PhotoResolutionSetting.HD_12MP
            PhotoResolutionSetting.HD_12MP -> PhotoResolutionSetting.UHD_4K
        }
        _uiState.update { it.copy(photoResolution = next) }
        appendDiagnosticLog("PHOTO-RES", "Photo resolution -> ${next.fullTitle}")
        showStatusBanner("Photo Quality: ${next.fullTitle}")
    }

    fun toggleVideoResolution(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val next = if (_uiState.value.videoResolution == VideoResolutionSetting.UHD_4K) {
            VideoResolutionSetting.FHD_1080P
        } else {
            VideoResolutionSetting.UHD_4K
        }
        _uiState.update { it.copy(videoResolution = next) }
        appendDiagnosticLog("VIDEO-RES", "Video resolution -> ${next.fullTitle}")
        showStatusBanner("Video Resolution: ${next.fullTitle}")
        bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun toggleVideoFps(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val next = if (_uiState.value.videoFps == VideoFpsSetting.FPS_60) {
            VideoFpsSetting.FPS_30
        } else {
            VideoFpsSetting.FPS_60
        }
        _uiState.update { it.copy(videoFps = next) }
        appendDiagnosticLog("FPS", "Video frame rate -> ${next.label}")
        showStatusBanner("Frame Rate: ${next.label}")
        bindCameraToLifecycle(lifecycleOwner, previewView)
    }

    fun toggleVideoStabilization() {
        val next = !_uiState.value.videoStabilizationEnabled
        _uiState.update { it.copy(videoStabilizationEnabled = next) }
        hardwareController.applyCamera2Parameters(_uiState.value.proSettings, next)
        appendDiagnosticLog("OIS/EIS", "Super Steady Stabilization -> $next")
        showStatusBanner(if (next) "Super Steady Video Stabilization ON" else "Video Stabilization OFF")
    }

    fun toggleAiSceneOptimizer() {
        val next = !_uiState.value.aiSceneOptimizerEnabled
        _uiState.update { it.copy(aiSceneOptimizerEnabled = next) }
        showStatusBanner(if (next) "AI Scene Optimizer Enabled" else "AI Scene Optimizer Disabled")
    }

    fun toggleGridLines() {
        _uiState.update { it.copy(gridLinesEnabled = !it.gridLinesEnabled) }
    }

    fun toggleWatermark() {
        _uiState.update { it.copy(watermarkEnabled = !it.watermarkEnabled) }
    }

    fun toggleVolumeButtonShutter() {
        _uiState.update { it.copy(volumeButtonShutterEnabled = !it.volumeButtonShutterEnabled) }
    }

    fun setZoomRatio(ratio: Float) {
        val caps = _uiState.value.hardwareCapabilities
        if (ratio < caps.minZoomRatio && ratio == 0.5f) {
            showStatusBanner("0.5x Ultra-Wide lens not separate on this device; set to widest optical (${String.format("%.1fx", caps.minZoomRatio)}).")
        }
        val effective = ratio.coerceIn(caps.minZoomRatio, caps.maxZoomRatio)
        hardwareController.setZoomRatio(effective)
        _uiState.update { it.copy(currentZoomRatio = effective) }
        appendDiagnosticLog("ZOOM", "Optical/Digital Zoom -> ${String.format("%.2fx", effective)}")
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
            "Capturing ${state.photoResolution.shortLabel} | LUT=${state.activeLut.label} | Log=${state.activeLogProfile.shortCode}"
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
                    val savedUri = galleryRepository.processAndSaveCapturedPhoto(
                        rawBitmap = capturedBitmap,
                        mode = state.activeMode,
                        resolution = state.photoResolution,
                        aspectRatio = state.aspectRatio,
                        proSettings = state.proSettings,
                        activeLut = state.activeLut,
                        lutIntensity = state.lutIntensity,
                        activeLogProfile = state.activeLogProfile,
                        aiSceneOptimizerEnabled = state.aiSceneOptimizerEnabled,
                        detectedScene = state.sceneMetrics.detectedScene,
                        portraitBlurStrength = state.portraitBlurStrength,
                        beautySmoothness = state.beautySmoothness,
                        addWatermark = state.watermarkEnabled
                    )
                    _uiState.update { it.copy(isCapturingPhoto = false) }
                    if (savedUri != null) {
                        appendDiagnosticLog("MEDIASTORE", "Saved ${state.photoResolution.shortLabel} JPEG -> $savedUri")
                        refreshGallery()
                        showStatusBanner("Saved ${state.photoResolution.shortLabel} (${state.activeLut.label} • ${state.activeLogProfile.shortCode})")
                    } else {
                        appendDiagnosticLog("ERROR", "Failed saving 4K JPEG to MediaStore")
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
                            showStatusBanner("Saved ${state.videoResolution.shortLabel} @ ${state.videoBitrateMbps} Mbps to Gallery")
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

    // Hybrid AI Scene Coach (Firebase AI Logic - gemini-3.8-flash)
    fun requestGeminiSceneCritique() {
        _uiState.update {
            it.copy(
                showAiCoachSheet = true,
                aiCoachState = AiCoachUiState.Analyzing
            )
        }
        appendDiagnosticLog("AI-COACH", "Sending frame to Gemini 3.8 Flash Vision Coach...")
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
                appendDiagnosticLog("SAMPLE", "Imported 4K (3840×2160) Studio Sample Shot")
                showStatusBanner("Imported 4K (3840×2160) Flagship Studio Sample Shot")
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
                    filter = it.activeLut,
                    logProfile = it.activeLogProfile
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
                appendDiagnosticLog("EDITOR", "Saved 4K LUT/Log edited photo -> $savedUri")
                showStatusBanner("Saved 4K edited copy to Gallery")
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

    fun showStatusBanner(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
        bannerDismissJob?.cancel()
        bannerDismissJob = viewModelScope.launch {
            delay(2600L)
            _uiState.update { it.copy(statusBannerMessage = null) }
        }
    }
}
