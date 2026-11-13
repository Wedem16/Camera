package com.example.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.data.MediaGalleryRepository
import com.example.model.CameraMode
import com.example.model.DiagnosticLogEntry
import com.example.model.FlashSetting
import com.example.model.LogProfile
import com.example.model.PhotoFilterPreset
import com.example.model.TimerSetting
import com.example.ui.components.AiSceneCoachBottomSheet
import com.example.ui.components.BitrateSelectorBottomSheet
import com.example.ui.components.CameraSettingsDialog
import com.example.ui.components.LutAndLogBottomSheet
import com.example.ui.components.MoreModesBottomSheet
import com.example.ui.components.ProControlsOverlay
import com.example.ui.theme.AiCyan
import com.example.ui.theme.AiViolet
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GalaxyAmber
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.RecordCrimson
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryZinc
import com.example.viewmodel.CameraUiState
import com.example.viewmodel.CameraViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CameraViewfinderScreen(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onRequestCameraAndAudioPermissions: () -> Unit,
    onRequestCameraOnlyPermission: () -> Unit
) {
    if (!state.hasCameraPermission) {
        PermissionOnboardingContent(
            onRequestCameraAndAudio = onRequestCameraAndAudioPermissions,
            onRequestCameraOnly = onRequestCameraOnlyPermission,
            onOpenGallery = viewModel::openGallery
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Re-bind automatically when switching between Video Mode (2-stream 4K UHD high-bitrate)
    // and Photo Modes (3-stream Max-Res 4K/50MP ImageCapture)
    val isVideoMode = state.activeMode == CameraMode.VIDEO
    DisposableEffect(lifecycleOwner, state.hasCameraPermission, isVideoMode) {
        viewModel.bindCameraToLifecycle(lifecycleOwner, previewView)
        onDispose { }
    }

    // Live Android ColorMatrix for real-time viewfinder LUT + Cinema Log preview
    val liveAndroidColorMatrix = remember(state.activeLut, state.lutIntensity, state.activeLogProfile) {
        if (state.activeLut == PhotoFilterPreset.ORIGINAL && state.activeLogProfile == LogProfile.STANDARD_REC709) {
            null
        } else {
            MediaGalleryRepository.buildCombinedColorMatrix(
                lut = state.activeLut,
                lutIntensity = state.lutIntensity,
                logProfile = state.activeLogProfile
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val density = LocalDensity.current

        // 1. Live CameraX Viewfinder Layer + Real-Time LUT/Log Color Grading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (abs(zoomChange - 1f) > 0.01f) {
                            val nextZoom = state.currentZoomRatio * zoomChange
                            viewModel.setZoomRatio(nextZoom)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        viewModel.onViewfinderTapped(tapOffset.x, tapOffset.y)
                    }
                }
                .testTag("camera_viewfinder_surface")
        ) {
            AndroidView(
                factory = { previewView },
                update = { view ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (liveAndroidColorMatrix != null) {
                            view.setRenderEffect(
                                android.graphics.RenderEffect.createColorFilterEffect(
                                    android.graphics.ColorMatrixColorFilter(liveAndroidColorMatrix)
                                )
                            )
                        } else {
                            view.setRenderEffect(null)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Computational Mode & Live LUT/Log Framing Overlay
            ViewfinderModeOverlayCanvas(
                mode = state.activeMode,
                gridLinesEnabled = state.gridLinesEnabled,
                portraitBlurStrength = state.portraitBlurStrength,
                manualFocusActive = state.activeMode == CameraMode.PRO && state.proSettings.manualFocusDistance != null,
                activeLut = state.activeLut,
                activeLogProfile = state.activeLogProfile,
                lutIntensity = state.lutIntensity
            )

            // 3. Tap-to-Focus Reticle + Interactive Exposure/Brightness Slider
            state.focusPoint?.let { focusOffset ->
                val boxSizePx = with(density) { 78.dp.toPx() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset {
                        IntOffset(
                            (focusOffset.x - boxSizePx / 2).roundToInt(),
                            (focusOffset.y - boxSizePx / 2).roundToInt()
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .border(2.dp, GalaxyAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GalaxyAmber)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianGlass)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Exposure Brightness",
                            tint = GalaxyAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = state.touchExposureEv,
                            onValueChange = { viewModel.onTouchExposureAdjusted(it) },
                            valueRange = -2.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = GalaxyAmber,
                                activeTrackColor = GalaxyAmber,
                                inactiveTrackColor = DividerSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("tap_exposure_slider")
                        )
                    }
                }
            }

            // 4. Shutter Capture Flash Veil
            AnimatedVisibility(
                visible = state.shutterFlashTriggered,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f))
                )
            }

            // 5. Countdown Timer Overlay (3s, 5s, 10s)
            AnimatedVisibility(
                visible = state.activeCountdownSec > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(ObsidianGlass)
                        .border(3.dp, GalaxyAmber, CircleShape)
                ) {
                    Text(
                        text = "${state.activeCountdownSec}",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 60.sp,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Top Floating Samsung Quick Controls Bar + LUT / Log / Bitrate / Logs HUD Strip
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopQuickSettingsBar(
                state = state,
                onCycleFlash = viewModel::cycleFlashSetting,
                onToggleTorch = viewModel::toggleTorchShortcut,
                onCycleTimer = viewModel::cycleTimerSetting,
                onCycleAspectRatio = viewModel::cycleAspectRatio,
                onToggleResolution = {
                    if (state.activeMode == CameraMode.VIDEO) {
                        viewModel.toggleVideoResolution(lifecycleOwner, previewView)
                    } else {
                        viewModel.togglePhotoResolution()
                    }
                },
                onToggleFps = {
                    viewModel.toggleVideoFps(lifecycleOwner, previewView)
                },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Second Quick Row: Active Video Timecode OR Mode + Bitrate (up to 1000 Mbps) + LUT/Log + Logs HUD + AI Coach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isRecordingVideo) {
                    VideoRecordingTimecodeBadge(
                        durationMs = state.recordingDurationMs,
                        bytesRecorded = state.recordingBytes,
                        isPaused = state.isVideoPaused,
                        resolutionLabel = state.videoResolution.shortLabel,
                        bitrateMbps = state.videoBitrateMbps
                    )
                }

                // Bitrate Quick Pill (10 - 1000 Mbps)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianGlass)
                        .border(1.dp, GalaxyAmber.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.setShowBitrateSheet(true) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("open_bitrate_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Video Bitrate",
                        tint = GalaxyAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.videoBitrateMbps} Mbps",
                        style = MaterialTheme.typography.labelMedium,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Film & Travel LUT + Cinema Log Quick Pill
                val hasActiveLutOrLog = state.activeLut != PhotoFilterPreset.ORIGINAL ||
                    state.activeLogProfile != LogProfile.STANDARD_REC709
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianGlass)
                        .border(
                            width = 1.dp,
                            color = if (hasActiveLutOrLog) EmeraldOptimal else DividerSubtle,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.setShowLutLogSheet(true) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("open_lut_log_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "LUT & Log Profile",
                        tint = if (hasActiveLutOrLog) EmeraldOptimal else AiCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.activeLut == PhotoFilterPreset.ORIGINAL) {
                            "LUT • ${state.activeLogProfile.shortCode}"
                        } else {
                            "${state.activeLut.label} • ${state.activeLogProfile.shortCode}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasActiveLutOrLog) EmeraldOptimal else TextPrimaryWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Live Pro Telemetry & Diagnostic Logs Toggle Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (state.showDiagnosticLogsOverlay) AiCyan.copy(alpha = 0.22f) else ObsidianGlass)
                        .border(
                            width = 1.dp,
                            color = if (state.showDiagnosticLogsOverlay) AiCyan else DividerSubtle,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.toggleDiagnosticLogsOverlay() }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_logs_hud_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Live Telemetry Logs",
                        tint = if (state.showDiagnosticLogsOverlay) AiCyan else TextSecondaryZinc,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOGS (${state.diagnosticLogs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.showDiagnosticLogsOverlay) AiCyan else TextPrimaryWhite
                    )
                }

                // Hybrid AI Scene Optimizer Pill + Gemini Coach Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianGlass)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(AiCyan, AiViolet)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.requestGeminiSceneCritique() }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("ai_scene_coach_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = stringResource(R.string.ai_optimizer_desc),
                        tint = AiCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (state.aiSceneOptimizerEnabled) {
                            "AI: ${state.sceneMetrics.detectedScene.displayTitle}"
                        } else {
                            "AI Coach"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Live Diagnostic Telemetry & Log Console Overlay (when toggled ON)
            AnimatedVisibility(
                visible = state.showDiagnosticLogsOverlay,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LiveDiagnosticLogsHud(
                    state = state,
                    logs = state.diagnosticLogs,
                    onClearLogs = viewModel::clearDiagnosticLogs
                )
            }

            // Transient Toast / Status Banner
            AnimatedVisibility(
                visible = state.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.statusBannerMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianGlass)
                            .border(1.dp, GalaxyAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GalaxyAmber,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 7. Bottom Thumb-Zone Camera Controls Deck
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            AmoledBlack.copy(alpha = 0.78f),
                            AmoledBlack.copy(alpha = 0.95f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.activeMode) {
                CameraMode.PRO -> {
                    ProControlsOverlay(
                        proSettings = state.proSettings,
                        activeTab = state.activeProTab,
                        capabilities = state.hardwareCapabilities,
                        histogramBins = state.sceneMetrics.histogramBins,
                        averageLuminance = state.sceneMetrics.averageLuminance,
                        onSelectTab = viewModel::selectProTab,
                        onUpdateIso = viewModel::updateProIso,
                        onUpdateShutter = viewModel::updateProShutter,
                        onUpdateWhiteBalance = viewModel::updateProWhiteBalance,
                        onUpdateFocus = viewModel::updateProManualFocus,
                        onUpdateExposureIndex = viewModel::updateProExposureIndex,
                        onResetPro = viewModel::resetProSettings
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                CameraMode.PORTRAIT -> {
                    AuxiliaryEffectSliderBar(
                        label = "STUDIO BOKEH BLUR",
                        valueLabel = "f/${String.format("%.1f", 16f - state.portraitBlurStrength * 14.6f)}",
                        value = state.portraitBlurStrength,
                        onValueChange = viewModel::setPortraitBlurStrength
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CameraMode.BEAUTY -> {
                    AuxiliaryEffectSliderBar(
                        label = "SKIN SMOOTH & TONE",
                        valueLabel = "${(state.beautySmoothness * 100).toInt()}%",
                        value = state.beautySmoothness,
                        onValueChange = viewModel::setBeautySmoothness
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> Unit
            }

            // Optical / Digital Zoom Pills (0.5x, 1x, 2x, 5x, 10x)
            ZoomPresetSelectorBar(
                currentZoom = state.currentZoomRatio,
                presets = state.hardwareCapabilities.zoomPresets,
                minSupported = state.hardwareCapabilities.minZoomRatio,
                maxSupported = state.hardwareCapabilities.maxZoomRatio,
                onSelectZoom = viewModel::setZoomRatio
            )

            Spacer(modifier = Modifier.height(12.dp))

            // One UI Shooting Mode Carousel + MORE Button
            ShootingModeCarousel(
                activeMode = state.activeMode,
                pinnedModes = state.pinnedModes,
                onSelectMode = viewModel::selectMode,
                onOpenMore = { viewModel.setShowMoreModesSheet(true) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Shutter / Video Record / Gallery / Switch Lens Deck
            BottomShutterDeck(
                state = state,
                onOpenGallery = viewModel::openGallery,
                onPrimaryShutter = viewModel::onPrimaryShutterPressed,
                onTogglePauseVideo = viewModel::toggleVideoPauseResume,
                onCaptureLiveSnapshot = viewModel::executeImmediatePhotoCapture,
                onSwitchLens = {
                    viewModel.toggleLensFacing(lifecycleOwner, previewView)
                }
            )
        }

        // Modal Sheets & Dialogs
        if (state.showLutLogSheet) {
            LutAndLogBottomSheet(
                state = state,
                onSelectCategory = viewModel::selectLutCategory,
                onSelectLut = viewModel::selectActiveLut,
                onUpdateIntensity = viewModel::updateLutIntensity,
                onSelectLogProfile = viewModel::selectLogProfile,
                onDismiss = { viewModel.setShowLutLogSheet(false) }
            )
        }

        if (state.showBitrateSheet) {
            BitrateSelectorBottomSheet(
                currentBitrateMbps = state.videoBitrateMbps,
                videoResolutionLabel = state.videoResolution.shortLabel,
                videoFpsLabel = state.videoFps.label,
                onSelectBitrateMbps = { mbps ->
                    viewModel.updateVideoBitrateMbps(mbps, lifecycleOwner, previewView)
                },
                onDismiss = { viewModel.setShowBitrateSheet(false) }
            )
        }

        if (state.showMoreModesSheet) {
            MoreModesBottomSheet(
                activeMode = state.activeMode,
                pinnedModes = state.pinnedModes,
                onSelectMode = viewModel::selectMode,
                onTogglePinMode = viewModel::togglePinModeInCarousel,
                onDismiss = { viewModel.setShowMoreModesSheet(false) }
            )
        }

        if (state.showAiCoachSheet) {
            AiSceneCoachBottomSheet(
                state = state,
                onRetryAnalysis = viewModel::requestGeminiSceneCritique,
                onApplyRecommendedPro = viewModel::applyAiRecommendedProSettings,
                onDismiss = viewModel::dismissAiCoachSheet
            )
        }

        if (state.showSettingsDialog) {
            CameraSettingsDialog(
                state = state,
                onTogglePhotoResolution = viewModel::togglePhotoResolution,
                onToggleVideoResolution = {
                    viewModel.toggleVideoResolution(lifecycleOwner, previewView)
                },
                onToggleVideoFps = {
                    viewModel.toggleVideoFps(lifecycleOwner, previewView)
                },
                onOpenBitrateSheet = { viewModel.setShowBitrateSheet(true) },
                onOpenLutLogSheet = { viewModel.setShowLutLogSheet(true) },
                onToggleDiagnosticLogs = viewModel::toggleDiagnosticLogsOverlay,
                onToggleVideoStabilization = viewModel::toggleVideoStabilization,
                onToggleAiOptimizer = viewModel::toggleAiSceneOptimizer,
                onToggleGridLines = viewModel::toggleGridLines,
                onToggleWatermark = viewModel::toggleWatermark,
                onToggleVolumeShutter = viewModel::toggleVolumeButtonShutter,
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }
    }
}

@Composable
private fun LiveDiagnosticLogsHud(
    state: CameraUiState,
    logs: List<DiagnosticLogEntry>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianGlass)
            .border(1.dp, AiCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRO TELEMETRY & HARDWARE LOGS • ${state.videoResolution.shortLabel} | ${state.videoBitrateMbps} Mbps | ${state.activeLogProfile.shortCode}",
                style = MaterialTheme.typography.labelSmall,
                color = AiCyan,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear logs",
                tint = TextSecondaryZinc,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onClearLogs)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 115.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs, key = { it.id }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryZinc,
                        fontSize = 9.sp
                    )
                    Text(
                        text = "[${entry.tag}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Text(
                        text = entry.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimaryWhite,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopQuickSettingsBar(
    state: CameraUiState,
    onCycleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleResolution: () -> Unit,
    onToggleFps: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianGlass)
            .border(1.dp, DividerSubtle, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("top_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings_control_desc),
                tint = TextPrimaryWhite
            )
        }

        val flashIcon = when (state.flashSetting) {
            FlashSetting.AUTO -> Icons.Default.FlashAuto
            FlashSetting.ON -> Icons.Default.FlashOn
            FlashSetting.OFF -> Icons.Default.FlashOff
        }
        IconButton(
            onClick = onCycleFlash,
            modifier = Modifier.testTag("top_flash_button")
        ) {
            Icon(
                imageVector = flashIcon,
                contentDescription = stringResource(R.string.flash_control_desc),
                tint = if (state.flashSetting == FlashSetting.OFF) TextPrimaryWhite else GalaxyAmber
            )
        }

        IconButton(
            onClick = onToggleTorch,
            modifier = Modifier.testTag("top_torch_button")
        ) {
            Icon(
                imageVector = if (state.torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                contentDescription = stringResource(R.string.torch_control_desc),
                tint = if (state.torchEnabled) GalaxyAmber else TextPrimaryWhite
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCycleTimer)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("top_timer_button")
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = stringResource(R.string.timer_control_desc),
                tint = if (state.timerSetting == TimerSetting.OFF) TextPrimaryWhite else GalaxyAmber,
                modifier = Modifier.size(20.dp)
            )
            if (state.timerSetting != TimerSetting.OFF) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = state.timerSetting.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyAmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.activeMode == CameraMode.VIDEO) {
            TopTextPillButton(
                label = state.videoFps.label,
                highlighted = state.videoFps.fps == 60,
                onClick = onToggleFps,
                testTag = "top_fps_button"
            )
        } else {
            TopTextPillButton(
                label = state.aspectRatio.label,
                highlighted = state.aspectRatio.label.contains("4K"),
                onClick = onCycleAspectRatio,
                testTag = "top_aspect_button"
            )
        }

        val resLabel = if (state.activeMode == CameraMode.VIDEO) {
            state.videoResolution.shortLabel
        } else {
            state.photoResolution.shortLabel
        }
        val resHighlighted = resLabel.contains("4K") || resLabel.contains("50MP")
        TopTextPillButton(
            label = resLabel,
            highlighted = resHighlighted,
            onClick = onToggleResolution,
            testTag = "top_resolution_button"
        )
    }
}

@Composable
private fun TopTextPillButton(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) GalaxyAmber.copy(alpha = 0.2f) else ObsidianElevated)
            .border(
                width = 1.dp,
                color = if (highlighted) GalaxyAmber else DividerSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) GalaxyAmber else TextPrimaryWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VideoRecordingTimecodeBadge(
    durationMs: Long,
    bytesRecorded: Long,
    isPaused: Boolean,
    resolutionLabel: String,
    bitrateMbps: Int
) {
    val totalSecs = durationMs / 1000L
    val mins = totalSecs / 60L
    val secs = totalSecs % 60L
    val formatted = String.format("%02d:%02d", mins, secs)
    val mbSize = bytesRecorded / (1024f * 1024f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(RecordCrimson.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("video_timecode_badge")
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isPaused) GalaxyAmber else TextPrimaryWhite)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isPaused) "PAUSED $formatted" else "REC $formatted",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryWhite
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$resolutionLabel • ${bitrateMbps}M • ${String.format("%.1fMB", mbSize)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimaryWhite.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun AuxiliaryEffectSliderBar(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianGlass)
            .border(1.dp, DividerSubtle, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryZinc
        )
        Spacer(modifier = Modifier.width(10.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = GalaxyAmber,
                activeTrackColor = GalaxyAmber,
                inactiveTrackColor = DividerSubtle
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelMedium,
            color = GalaxyAmber,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ZoomPresetSelectorBar(
    currentZoom: Float,
    presets: List<Float>,
    minSupported: Float,
    maxSupported: Float,
    onSelectZoom: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(ObsidianGlass)
            .border(1.dp, DividerSubtle, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        presets.forEach { preset ->
            val isAvailable = preset in (minSupported - 0.55f)..(maxSupported + 0.1f)
            val isSelected = abs(currentZoom - preset.coerceIn(minSupported, maxSupported)) < 0.25f &&
                (preset != 0.5f || minSupported < 0.9f)
            val label = if (preset == 0.5f) {
                "0.5"
            } else if (isSelected) {
                String.format("%.1fx", currentZoom).removeSuffix(".0x") +
                    if (String.format("%.1fx", currentZoom).endsWith(".0x")) "x" else ""
            } else {
                "${preset.toInt()}x"
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isSelected) 38.dp else 32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GalaxyAmber else ObsidianElevated)
                    .clickable(enabled = isAvailable) { onSelectZoom(preset) }
                    .testTag("zoom_pill_${preset.toString().replace('.', '_')}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isSelected -> AmoledBlack
                        else -> TextPrimaryWhite
                    }
                )
            }
        }
    }
}

@Composable
private fun ShootingModeCarousel(
    activeMode: CameraMode,
    pinnedModes: List<CameraMode>,
    onSelectMode: (CameraMode) -> Unit,
    onOpenMore: () -> Unit
) {
    val displayModes = remember(pinnedModes, activeMode) {
        if (pinnedModes.contains(activeMode)) pinnedModes else pinnedModes + activeMode
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayModes.forEach { mode ->
            val selected = activeMode == mode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(if (selected) ObsidianElevated else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) GalaxyAmber.copy(alpha = 0.6f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelectMode(mode) }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("mode_tab_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) GalaxyAmber else TextPrimaryWhite.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenMore)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("mode_tab_more")
        ) {
            Text(
                text = "MORE",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondaryZinc
            )
        }
    }
}

@Composable
private fun BottomShutterDeck(
    state: CameraUiState,
    onOpenGallery: () -> Unit,
    onPrimaryShutter: () -> Unit,
    onTogglePauseVideo: () -> Unit,
    onCaptureLiveSnapshot: () -> Unit,
    onSwitchLens: () -> Unit
) {
    val shutterScale by animateFloatAsState(
        targetValue = if (state.isCapturingPhoto) 0.88f else 1.0f,
        animationSpec = spring(),
        label = "shutterScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isRecordingVideo) {
            IconButton(
                onClick = onTogglePauseVideo,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(1.5.dp, TextPrimaryWhite, CircleShape)
                    .testTag("video_pause_resume_button")
            ) {
                Icon(
                    imageVector = if (state.isVideoPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (state.isVideoPaused) "Resume video" else "Pause video",
                    tint = GalaxyAmber
                )
            }
        } else {
            val latestMedia = state.mediaList.firstOrNull()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(2.dp, DividerSubtle, CircleShape)
                    .clickable(onClick = onOpenGallery)
                    .testTag("open_gallery_button")
            ) {
                if (latestMedia != null) {
                    AsyncImage(
                        model = latestMedia.uri,
                        contentDescription = stringResource(R.string.gallery_preview_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(R.string.gallery_preview_desc),
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        val isVideoMode = state.activeMode == CameraMode.VIDEO
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((78 * shutterScale).dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = if (state.isRecordingVideo) RecordCrimson else TextPrimaryWhite,
                    shape = CircleShape
                )
                .clickable(onClick = onPrimaryShutter)
                .padding(7.dp)
                .testTag("primary_shutter_button")
        ) {
            if (isVideoMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (state.isRecordingVideo) 32.dp else 58.dp)
                        .clip(if (state.isRecordingVideo) RoundedCornerShape(8.dp) else CircleShape)
                        .background(RecordCrimson)
                ) {
                    if (state.isRecordingVideo) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop recording",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (state.activeMode == CameraMode.NIGHT) GalaxyAmber else TextPrimaryWhite
                        )
                )
            }
        }

        if (state.isRecordingVideo) {
            IconButton(
                onClick = onCaptureLiveSnapshot,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(1.5.dp, TextPrimaryWhite, CircleShape)
                    .testTag("video_snapshot_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture photo while recording",
                    tint = TextPrimaryWhite
                )
            }
        } else {
            IconButton(
                onClick = onSwitchLens,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(1.5.dp, DividerSubtle, CircleShape)
                    .testTag("switch_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera_desc),
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewfinderModeOverlayCanvas(
    mode: CameraMode,
    gridLinesEnabled: Boolean,
    portraitBlurStrength: Float,
    manualFocusActive: Boolean,
    activeLut: PhotoFilterPreset,
    activeLogProfile: LogProfile,
    lutIntensity: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Subtle atmospheric LUT / Log tint overlay for SurfaceView fallback devices
        if (activeLut != PhotoFilterPreset.ORIGINAL && lutIntensity > 0.05f) {
            val warmth = activeLut.warmth * lutIntensity
            val tintColor = when {
                warmth > 8f -> Color(0xFFFFB347).copy(alpha = (0.08f * lutIntensity).coerceAtMost(0.14f))
                warmth < -8f -> Color(0xFF38BDF8).copy(alpha = (0.08f * lutIntensity).coerceAtMost(0.14f))
                else -> Color.Transparent
            }
            if (tintColor != Color.Transparent) {
                drawRect(color = tintColor)
            }
        }
        if (activeLogProfile != LogProfile.STANDARD_REC709) {
            // Flat shadow lift haze representing Log gamma curve
            drawRect(color = Color.White.copy(alpha = 0.05f))
        }

        // 1. 3x3 Rule-of-Thirds Composition Grid
        if (gridLinesEnabled) {
            val gridColor = Color.White.copy(alpha = 0.24f)
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
            drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.5f)
        }

        // 2. Portrait Mode Bokeh Framing Guide
        if (mode == CameraMode.PORTRAIT) {
            val radius = minOf(w, h) * 0.36f
            drawCircle(
                color = GalaxyAmber.copy(alpha = 0.35f + portraitBlurStrength * 0.35f),
                radius = radius,
                center = Offset(w / 2f, h * 0.44f),
                style = Stroke(width = 3f)
            )
        }

        // 3. Panorama Mode Level Horizon & Wide Strip Guide
        if (mode == CameraMode.PANORAMA) {
            val stripHeight = h * 0.26f
            val topY = (h - stripHeight) / 2f
            val bottomY = topY + stripHeight
            drawRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, topY)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(0f, bottomY),
                size = androidx.compose.ui.geometry.Size(w, h - bottomY)
            )
            drawLine(
                color = GalaxyAmber,
                start = Offset(w * 0.08f, h / 2f),
                end = Offset(w * 0.92f, h / 2f),
                strokeWidth = 3f
            )
        }

        // 4. Pro Manual Focus Peaking Ring Indicator
        if (manualFocusActive) {
            drawCircle(
                color = EmeraldOptimal.copy(alpha = 0.55f),
                radius = minOf(w, h) * 0.22f,
                center = Offset(w / 2f, h * 0.45f),
                style = Stroke(width = 2.5f)
            )
        }
    }
}

@Composable
private fun PermissionOnboardingContent(
    onRequestCameraAndAudio: () -> Unit,
    onRequestCameraOnly: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_camera_hero_lens_1794590959493),
            contentDescription = "Galaxy HD Camera Flagship Array",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.5.dp, GalaxyAmber.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "GALAXY HD CAMERA 4K",
            style = MaterialTheme.typography.labelLarge,
            color = GalaxyAmber
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.permission_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimaryWhite,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.permission_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondaryZinc,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestCameraAndAudio,
            colors = ButtonDefaults.buttonColors(
                containerColor = GalaxyAmber,
                contentColor = AmoledBlack
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("grant_camera_audio_button")
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.grant_permissions_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRequestCameraOnly,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("grant_camera_only_button")
            ) {
                Text(
                    text = stringResource(R.string.grant_camera_only_button),
                    color = TextPrimaryWhite
                )
            }

            OutlinedButton(
                onClick = onOpenGallery,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("onboarding_open_gallery_button")
            ) {
                Text(
                    text = stringResource(R.string.open_gallery_button),
                    color = GalaxyAmber
                )
            }
        }
    }
}
