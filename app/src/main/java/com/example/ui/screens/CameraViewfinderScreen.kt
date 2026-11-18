package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.example.model.AiUpscaleMode
import com.example.model.CameraMode
import com.example.model.DiagnosticLogEntry
import com.example.model.FlashSetting
import com.example.model.LogProfile
import com.example.model.PhotoFilterPreset
import com.example.model.ProCameraSettings
import com.example.model.TimerSetting
import com.example.ui.components.AiSceneCoachBottomSheet
import com.example.ui.components.BitrateSelectorBottomSheet
import com.example.ui.components.CameraSettingsDialog
import com.example.ui.components.HonorQuickMenuPopupCard
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
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
            // Hardware-accelerated SurfaceView (PERFORMANCE) avoids TextureView downscaling blur
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, state.hasCameraPermission, state.activeMode) {
        viewModel.bindCameraToLifecycle(lifecycleOwner, previewView)
        onDispose { }
    }

    // Real-Time Horizon Level Sensor (Accelerometer tilt angle in degrees)
    var deviceRollDegrees by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context, state.levelEnabled) {
        if (!state.levelEnabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val values = event?.values ?: return
                    if (values.size >= 2) {
                        val ax = values[0]
                        val ay = values[1]
                        val rawDeg = Math.toDegrees(atan2(ax.toDouble(), ay.toDouble())).toFloat()
                        // Smooth low-pass filter so the level line glides without jitter
                        deviceRollDegrees = deviceRollDegrees * 0.82f + rawDeg * 0.18f
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (sensorManager != null && accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    // Live Android ColorMatrix combining Creative LUT/Log (when enabled) AND Pro Mode Manual Overrides
    val liveAndroidColorMatrix = remember(
        state.creativeFiltersEnabled,
        state.activeLut,
        state.lutIntensity,
        state.activeLogProfile,
        state.activeMode,
        state.proSettings,
        state.hardwareCapabilities.exposureStepEv
    ) {
        val hasCreative = state.creativeFiltersEnabled &&
            (state.activeLut != PhotoFilterPreset.ORIGINAL || state.activeLogProfile != LogProfile.STANDARD_REC709)
        val proMatrix = if (state.activeMode == CameraMode.PRO && state.proSettings.hasAnyManualOverride) {
            MediaGalleryRepository.buildProManualColorMatrix(
                proSettings = state.proSettings,
                exposureStepEv = state.hardwareCapabilities.exposureStepEv
            )
        } else {
            null
        }

        when {
            hasCreative && proMatrix != null -> {
                MediaGalleryRepository.buildCombinedColorMatrix(
                    lut = state.activeLut,
                    lutIntensity = state.lutIntensity,
                    logProfile = state.activeLogProfile
                ).apply { postConcat(proMatrix) }
            }
            hasCreative -> {
                MediaGalleryRepository.buildCombinedColorMatrix(
                    lut = state.activeLut,
                    lutIntensity = state.lutIntensity,
                    logProfile = state.activeLogProfile
                )
            }
            proMatrix != null -> proMatrix
            else -> null
        }
    }

    // Draggable Floating Shutter Button state
    var floatingShutterOffsetX by remember { mutableFloatStateOf(240f) }
    var floatingShutterOffsetY by remember { mutableFloatStateOf(780f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // 1. Live CameraX Viewfinder Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var lastPinchUpdateMs = 0L
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val now = android.os.SystemClock.uptimeMillis()
                        if (abs(zoomChange - 1f) > 0.012f && (now - lastPinchUpdateMs >= 16L)) {
                            lastPinchUpdateMs = now
                            val nextZoom = state.currentZoomRatio * zoomChange
                            viewModel.setZoomRatio(nextZoom, isContinuousGesture = true)
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

            // 2. Composition Grid, Horizon Level Indicator, Pro Focus Peaking & Mode Guides
            ViewfinderModeOverlayCanvas(
                mode = state.activeMode,
                gridLinesEnabled = state.gridLinesEnabled,
                levelEnabled = state.levelEnabled,
                deviceRollDegrees = deviceRollDegrees,
                portraitBlurStrength = state.portraitBlurStrength,
                proSettings = state.proSettings,
                exposureStepEv = state.hardwareCapabilities.exposureStepEv,
                creativeFiltersEnabled = state.creativeFiltersEnabled,
                activeLut = state.activeLut,
                activeLogProfile = state.activeLogProfile,
                lutIntensity = state.lutIntensity
            )

            // 3. Tap-to-Focus Reticle + Interactive Exposure Compensation Slider
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

            // 4. Draggable Floating Shutter Button (when enabled in Honor Settings)
            if (state.floatingShutterEnabled) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                floatingShutterOffsetX.coerceIn(24f, (maxWidthPx - 180f).coerceAtLeast(24f)).roundToInt(),
                                floatingShutterOffsetY.coerceIn(180f, (maxHeightPx - 360f).coerceAtLeast(180f)).roundToInt()
                            )
                        }
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(2.5.dp, TextPrimaryWhite, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                floatingShutterOffsetX = (floatingShutterOffsetX + dragAmount.x)
                                    .coerceIn(24f, (maxWidthPx - 180f).coerceAtLeast(24f))
                                floatingShutterOffsetY = (floatingShutterOffsetY + dragAmount.y)
                                    .coerceIn(180f, (maxHeightPx - 360f).coerceAtLeast(180f))
                            }
                        }
                        .clickable { viewModel.onPrimaryShutterPressed() }
                        .padding(6.dp)
                        .testTag("floating_shutter_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(TextPrimaryWhite.copy(alpha = 0.92f))
                    )
                }
            }

            // 5. Shutter Capture Flash Veil
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

            // 6. Countdown Timer Overlay (3s, 5s, 10s)
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

        // 7. Top Native Honor Black Header Bar & Contextual Pills
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AmoledBlack.copy(alpha = 0.94f),
                            AmoledBlack.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 6.dp, start = 12.dp, end = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HonorTopHeaderBar(
                state = state,
                onCycleFlash = viewModel::cycleFlashSetting,
                onToggleTorch = viewModel::toggleTorchShortcut,
                onCycleTimer = viewModel::cycleTimerSetting,
                onCycleAspectRatio = viewModel::cycleAspectRatio,
                onToggleResolution = {
                    if (state.isVideoStreamMode) {
                        viewModel.toggleVideoResolution(lifecycleOwner, previewView)
                    } else {
                        viewModel.togglePhotoResolution()
                    }
                },
                onToggleFps = {
                    viewModel.toggleVideoFps(lifecycleOwner, previewView)
                },
                onOpenFilters = {
                    if (!state.creativeFiltersEnabled) {
                        viewModel.toggleCreativeFilters()
                    }
                    viewModel.setShowLutLogSheet(true)
                },
                onToggleQuickMenu = viewModel::toggleQuickMenuPopup,
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Second Quick Row: Compact 4K Honor AI + Preset + Super-Res Upscale + Format + Bitrate + Logs + AI Coach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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

                // Compact Honor 600 4K AI Generation Toggle Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (state.aiAutoEditAfterCaptureEnabled) AiCyan.copy(alpha = 0.22f) else ObsidianGlass
                        )
                        .border(
                            width = 1.dp,
                            color = if (state.aiAutoEditAfterCaptureEnabled) AiCyan else DividerSubtle,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { viewModel.toggleAiAutoEditAfterCapture() }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("toggle_ai_auto_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Honor 600 4K AI Generation",
                        tint = if (state.aiAutoEditAfterCaptureEnabled) AiCyan else TextSecondaryZinc,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.aiAutoEditAfterCaptureEnabled) {
                            "4K AI: ON"
                        } else {
                            "4K AI: OFF"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.aiAutoEditAfterCaptureEnabled) AiCyan else TextPrimaryWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Compact Honor 600 AIGC Style Cycler Pill (AIGC DETAIL / HARCOURT / AI HDR / AI NIGHT)
                if (state.aiAutoEditAfterCaptureEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianGlass)
                            .border(1.dp, EmeraldOptimal.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                            .clickable {
                                val presets = com.example.model.HonorAiGenPreset.entries
                                val nextIdx = (presets.indexOf(state.selectedHonorAiPreset) + 1) % presets.size
                                viewModel.selectHonorAiPreset(presets[nextIdx])
                            }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("cycle_honor_ai_preset_button")
                    ) {
                        Text(
                            text = state.selectedHonorAiPreset.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldOptimal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Step 5: Optional AI Upscale Mode Toggle Pill ("Original" / "AI Upscale 2x" / "AI Upscale 4x")
                val isUpscaleActive = state.aiUpscaleMode != AiUpscaleMode.ORIGINAL
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isUpscaleActive) EmeraldOptimal.copy(alpha = 0.20f) else ObsidianGlass
                        )
                        .border(
                            width = 1.dp,
                            color = if (isUpscaleActive) EmeraldOptimal else DividerSubtle,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { viewModel.cycleAiUpscaleMode() }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("toggle_ai_zoom_quality_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Upscale Mode",
                        tint = if (isUpscaleActive) EmeraldOptimal else TextSecondaryZinc,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.aiUpscaleMode.uiLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUpscaleActive) EmeraldOptimal else TextPrimaryWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Capture Format Quick Pill (Photo: JPG / RAW+JPG / PNG / WEBP | Video: MP4 / HEVC MP4 / MOV / MKV)
                val formatBadgeLabel = if (state.isVideoStreamMode) {
                    state.videoFormat.badgeLabel
                } else {
                    state.photoFormat.badgeLabel
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianGlass)
                        .border(1.dp, GalaxyAmber.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .clickable {
                            if (state.isVideoStreamMode) {
                                viewModel.cycleVideoFormat()
                            } else {
                                viewModel.cyclePhotoFormat()
                            }
                        }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("cycle_capture_format_button")
                ) {
                    Text(
                        text = "FMT: $formatBadgeLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Video Bitrate Quick Pill (10 - 1000 Mbps)
                if (state.isVideoStreamMode) {
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
                }

                // Optional Creative LUT & Log Quick Pill
                if (state.creativeFiltersEnabled) {
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
                }

                // Live Telemetry & Diagnostic Logs Toggle Pill
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

                // AI Scene Coach Button
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

            // Honor Contextual Viewfinder Tip Pill (matching "Move further away" in Screenshot_20261118_144446)
            val contextualTip = when {
                state.activeMode == CameraMode.SUPER_MACRO -> "Keep 4cm away from subject"
                state.activeMode == CameraMode.PORTRAIT && state.currentZoomRatio >= 2.0f -> "Move further away"
                state.activeMode == CameraMode.PRO && state.proSettings.hasAnyManualOverride ->
                    "PRO MANUAL • ISO ${state.proSettings.formattedIso} • S ${state.proSettings.formattedShutter} • WB ${state.proSettings.formattedWb}"
                else -> null
            }
            if (contextualTip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xAD2C2C2E))
                        .padding(horizontal = 18.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = contextualTip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Background AI Super-Resolution Progress Indicator Banner
            AnimatedVisibility(
                visible = state.isUpscalingInProgress,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianGlass)
                        .border(1.dp, EmeraldOptimal, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("ai_upscale_progress_banner")
                ) {
                    CircularProgressIndicator(
                        color = EmeraldOptimal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    val stageText = state.upscaleStatusStage.ifBlank {
                        "Running ${state.aiUpscaleMode.uiLabel}..."
                    }
                    Text(
                        text = "$stageText (${state.upscaleProgressPercent}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal,
                        fontWeight = FontWeight.Bold
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

        // 8. Bottom Honor Camera Control Deck (`Screenshot_20261118_144446` & `144457`)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode-Specific Overlay Sliders (Pro 6-Column Deck / Portrait Bokeh / Beauty)
            when (state.activeMode) {
                CameraMode.PRO -> {
                    ProControlsOverlay(
                        proSettings = state.proSettings,
                        activeTab = state.activeProTab,
                        capabilities = state.hardwareCapabilities,
                        photoFormat = state.photoFormat,
                        histogramBins = state.sceneMetrics.histogramBins,
                        averageLuminance = state.sceneMetrics.averageLuminance,
                        onSelectTab = viewModel::selectProTab,
                        onUpdateMetering = viewModel::updateProMeteringMode,
                        onUpdateIso = viewModel::updateProIso,
                        onUpdateShutter = viewModel::updateProShutter,
                        onUpdateWhiteBalance = viewModel::updateProWhiteBalance,
                        onUpdateFocus = viewModel::updateProManualFocus,
                        onToggleFocusPeaking = viewModel::toggleProFocusPeaking,
                        onUpdateExposureIndex = viewModel::updateProExposureIndex,
                        onCyclePhotoFormat = viewModel::cyclePhotoFormat,
                        onResetPro = viewModel::resetProSettings
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                CameraMode.PORTRAIT -> {
                    AuxiliaryEffectSliderBar(
                        label = "APERTURE",
                        valueLabel = "f/${String.format(Locale.US, "%.1f", 16f - state.portraitBlurStrength * 14.6f)}",
                        value = state.portraitBlurStrength,
                        onValueChange = viewModel::setPortraitBlurStrength
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                CameraMode.BEAUTY -> {
                    AuxiliaryEffectSliderBar(
                        label = "BEAUTY",
                        valueLabel = "${(state.beautySmoothness * 100).toInt()}%",
                        value = state.beautySmoothness,
                        onValueChange = viewModel::setBeautySmoothness
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                else -> Unit
            }

            // Honor Floating Viewfinder Overlay Row (Left Bokeh/Beauty icons, Center Zoom Capsule, Right AI/LUT icons)
            HonorFloatingViewfinderRow(
                state = state,
                onSelectPortrait = {
                    if (state.activeMode == CameraMode.PORTRAIT) {
                        viewModel.selectMode(CameraMode.PHOTO)
                    } else {
                        viewModel.selectMode(CameraMode.PORTRAIT)
                    }
                },
                onSelectBeauty = {
                    if (state.activeMode == CameraMode.BEAUTY) {
                        viewModel.selectMode(CameraMode.PHOTO)
                    } else {
                        viewModel.selectMode(CameraMode.BEAUTY)
                    }
                },
                onToggleAiScene = viewModel::toggleAiSceneOptimizer,
                onOpenLutFrame = {
                    if (!state.creativeFiltersEnabled) {
                        viewModel.toggleCreativeFilters()
                    }
                    viewModel.setShowLutLogSheet(true)
                },
                onSelectZoom = viewModel::setZoomRatio
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pure Black Honor Bottom Deck (Chevron `^`, Mode Carousel, Shutter Row)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmoledBlack)
                    .navigationBarsPadding()
                    .padding(top = 4.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Up-Chevron `^` button that opens the Honor Frosted Quick Menu Card (Screenshot_20261118_144446)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.toggleQuickMenuPopup() }
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .testTag("bottom_quick_menu_chevron")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Open Quick Menu",
                        tint = TextSecondaryZinc,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Honor Uppercase Mode Carousel (NIGHT, PORTRAIT, PHOTO, VIDEO, PRO, MORE)
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
        }

        // 9. Honor Frosted Quick Menu Popup Card Overlay (`Screenshot_20261118_144502`)
        AnimatedVisibility(
            visible = state.showQuickMenuPopup,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 175.dp)
        ) {
            HonorQuickMenuPopupCard(
                state = state,
                onCycleFlash = viewModel::cycleFlashSetting,
                onToggleSpecifications = {
                    if (state.isVideoStreamMode) {
                        viewModel.toggleVideoResolution(lifecycleOwner, previewView)
                    } else {
                        viewModel.togglePhotoResolution()
                    }
                },
                onCycleAspectRatio = viewModel::cycleAspectRatio,
                onToggleMuteAudio = viewModel::toggleMute,
                onOpenFilters = {
                    if (!state.creativeFiltersEnabled) {
                        viewModel.toggleCreativeFilters()
                    }
                    viewModel.setShowLutLogSheet(true)
                },
                onToggleGridlines = viewModel::toggleGridLines,
                onToggleLevel = viewModel::toggleLevel,
                onCycleAiUpscale = viewModel::cycleAiUpscaleMode,
                onOpenFullSettings = { viewModel.setShowSettingsDialog(true) },
                onDismiss = { viewModel.setShowQuickMenuPopup(false) }
            )
        }

        // 10. Modal Sheets & Full-Screen Honor Dialogs
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
                onToggleAiAutoEditAfterCapture = viewModel::toggleAiAutoEditAfterCapture,
                onCycleAiUpscaleMode = viewModel::cycleAiUpscaleMode,
                onToggleCreativeFilters = viewModel::toggleCreativeFilters,
                onTogglePhotoResolution = viewModel::togglePhotoResolution,
                onCyclePhotoFormat = viewModel::cyclePhotoFormat,
                onCycleAspectRatio = viewModel::cycleAspectRatio,
                onCycleFocalLength = viewModel::cycleDefaultFocalLength,
                onToggleRecommendFilters = viewModel::toggleRecommendFilters,
                onToggleKeepOriginalWithAiEnhance = viewModel::toggleKeepOriginalWithAiEnhance,
                onToggleVideoResolution = {
                    viewModel.toggleVideoResolution(lifecycleOwner, previewView)
                },
                onCycleVideoFormat = viewModel::cycleVideoFormat,
                onToggleVideoFps = {
                    viewModel.toggleVideoFps(lifecycleOwner, previewView)
                },
                onToggleAutoFpsLowLight = viewModel::toggleAutoFpsLowLight,
                onToggleLockWhiteBalanceInVideo = viewModel::toggleLockWhiteBalanceInVideo,
                onToggleEfficientVideoFormat = viewModel::toggleEfficientVideoFormat,
                onToggleScreenOffRecording = viewModel::toggleScreenOffRecording,
                onOpenBitrateSheet = { viewModel.setShowBitrateSheet(true) },
                onOpenLutLogSheet = { viewModel.setShowLutLogSheet(true) },
                onToggleDiagnosticLogs = viewModel::toggleDiagnosticLogsOverlay,
                onToggleVideoStabilization = viewModel::toggleVideoStabilization,
                onToggleAiOptimizer = viewModel::toggleAiSceneOptimizer,
                onCycleTimer = viewModel::cycleTimerSetting,
                onToggleGridLines = viewModel::toggleGridLines,
                onToggleLevel = viewModel::toggleLevel,
                onToggleMute = viewModel::toggleMute,
                onToggleMirrorReflection = viewModel::toggleMirrorReflection,
                onToggleFloatingShutter = viewModel::toggleFloatingShutter,
                onToggleLocationTag = viewModel::toggleLocationTag,
                onToggleRememberSettings = viewModel::toggleRememberSettings,
                onToggleAutoLensSwitching = viewModel::toggleAutoLensSwitching,
                onToggleWatermark = viewModel::toggleWatermark,
                onToggleVolumeShutter = viewModel::toggleVolumeButtonShutter,
                onRestoreDefaults = viewModel::restoreDefaultCameraSettings,
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }
    }
}

/**
 * Native Honor Camera Top Header Bar (`Screenshot_20261118_144446` & `144457`).
 * Left: Flash icon, Torch icon, Timer icon, Settings icon.
 * Right: Filter 3-ring icon, `HDR` / `4K · 60` resolution badge, and 6-dot Quick Menu Grid icon.
 */
@Composable
private fun HonorTopHeaderBar(
    state: CameraUiState,
    onCycleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleResolution: () -> Unit,
    onToggleFps: () -> Unit,
    onOpenFilters: () -> Unit,
    onToggleQuickMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Group: Flash + Torch + Timer + Aspect/FPS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val flashIcon = when (state.flashSetting) {
                FlashSetting.AUTO -> Icons.Default.FlashAuto
                FlashSetting.ON -> Icons.Default.FlashOn
                FlashSetting.OFF -> Icons.Default.FlashOff
            }
            IconButton(
                onClick = onCycleFlash,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_flash_button")
            ) {
                Icon(
                    imageVector = flashIcon,
                    contentDescription = stringResource(R.string.flash_control_desc),
                    tint = if (state.flashSetting == FlashSetting.OFF) TextPrimaryWhite else GalaxyAmber,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_torch_button")
            ) {
                Icon(
                    imageVector = if (state.torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = stringResource(R.string.torch_control_desc),
                    tint = if (state.torchEnabled) GalaxyAmber else TextPrimaryWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(19.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCycleTimer)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("top_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = stringResource(R.string.timer_control_desc),
                    tint = if (state.timerSetting == TimerSetting.OFF) TextPrimaryWhite.copy(alpha = 0.8f) else GalaxyAmber,
                    modifier = Modifier.size(19.dp)
                )
                if (state.timerSetting != TimerSetting.OFF) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = state.timerSetting.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.isVideoStreamMode) {
                TopTextPillButton(
                    label = state.videoFps.label,
                    highlighted = state.videoFps.fps == 60,
                    onClick = onToggleFps,
                    testTag = "top_fps_button"
                )
            } else {
                TopTextPillButton(
                    label = state.aspectRatio.label.replace(" 4K", ""),
                    highlighted = state.aspectRatio.ratioValue == null,
                    onClick = onCycleAspectRatio,
                    testTag = "top_aspect_button"
                )
            }
        }

        // Right Group: Honor 3-Ring Filter Icon + HDR / 4K·60 Badge + Quick Menu Grid + Settings Gear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onOpenFilters,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_filter_ring_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterVintage,
                    contentDescription = "Creative Filters",
                    tint = if (state.creativeFiltersEnabled && state.activeLut != PhotoFilterPreset.ORIGINAL) {
                        GalaxyAmber
                    } else {
                        TextPrimaryWhite
                    },
                    modifier = Modifier.size(21.dp)
                )
            }

            // Honor Circular/Pill HDR or 4K·60 Badge
            val resLabel = if (state.isVideoStreamMode) {
                "${if (state.videoResolution.widthPx >= 3840) "4K" else "1080"}·${state.videoFps.fps}"
            } else if (state.activeMode == CameraMode.PORTRAIT || state.activeMode == CameraMode.HDR) {
                "HDR"
            } else {
                state.photoResolution.shortLabel
            }
            TopTextPillButton(
                label = resLabel,
                highlighted = true,
                onClick = onToggleResolution,
                testTag = "top_resolution_button"
            )

            // Honor 6-Dot Quick Menu Grid Button (`Screenshot_20261118_144446`)
            IconButton(
                onClick = onToggleQuickMenu,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_quick_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Quick Menu",
                    tint = if (state.showQuickMenuPopup) GalaxyAmber else TextPrimaryWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_control_desc),
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Native Honor Viewfinder Floating Bottom Overlay (`Screenshot_20261118_144446`).
 * Left column: Aperture/Bokeh toggle & Beauty toggle.
 * Center: Compact translucent capsule Zoom Selector (`0.6x`, `1x`, `2x`, `3x`, `5x`, `10x`).
 * Right column: AI Camera toggle & Film/LUT frame button.
 */
@Composable
private fun HonorFloatingViewfinderRow(
    state: CameraUiState,
    onSelectPortrait: () -> Unit,
    onSelectBeauty: () -> Unit,
    onToggleAiScene: () -> Unit,
    onOpenLutFrame: () -> Unit,
    onSelectZoom: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Floating Column (Bokeh Portrait + Studio Beauty)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HonorFloatingCircleIcon(
                icon = Icons.Default.BlurOn,
                contentDescription = "Portrait Bokeh",
                isActive = state.activeMode == CameraMode.PORTRAIT,
                onClick = onSelectPortrait,
                testTag = "floating_bokeh_button"
            )
            HonorFloatingCircleIcon(
                icon = Icons.Default.FaceRetouchingNatural,
                contentDescription = "Beauty Retouch",
                isActive = state.activeMode == CameraMode.BEAUTY,
                onClick = onSelectBeauty,
                testTag = "floating_beauty_button"
            )
        }

        // Center Optical / Logical Zoom Selector Bar (`0.6x`, `1x`, `2x`, `3x`, `5x`, `10x`)
        ZoomPresetSelectorBar(
            currentZoom = state.currentZoomRatio,
            presets = state.hardwareCapabilities.zoomPresets,
            onSelectZoom = onSelectZoom
        )

        // Right Floating Column (AI Scene Camera + Film/LUT Frame)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HonorFloatingCircleIcon(
                icon = Icons.Default.AutoAwesome,
                contentDescription = "AI Scene Camera",
                isActive = state.aiSceneOptimizerEnabled,
                onClick = onToggleAiScene,
                testTag = "floating_ai_scene_button"
            )
            HonorFloatingCircleIcon(
                icon = Icons.Default.FilterFrames,
                contentDescription = "Film & LUT Frames",
                isActive = state.creativeFiltersEnabled && state.activeLut != PhotoFilterPreset.ORIGINAL,
                onClick = onOpenLutFrame,
                testTag = "floating_lut_frame_button"
            )
        }
    }
}

@Composable
private fun HonorFloatingCircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x99242426))
            .border(
                width = if (isActive) 1.5.dp else 0.8.dp,
                color = if (isActive) GalaxyAmber else DividerSubtle,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) GalaxyAmber else TextPrimaryWhite,
            modifier = Modifier.size(20.dp)
        )
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
            val caps = state.hardwareCapabilities
            Text(
                text = "${caps.mainSensorSpec} • ${caps.socPlatformSpec} • ${state.activeZoomProfile.badgeLabel}",
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
private fun TopTextPillButton(
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) GalaxyAmber.copy(alpha = 0.18f) else ObsidianElevated)
            .border(
                width = 1.dp,
                color = if (highlighted) GalaxyAmber else DividerSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
    val formatted = String.format(Locale.US, "%02d:%02d", mins, secs)
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
            text = "$resolutionLabel • ${bitrateMbps}M • ${String.format(Locale.US, "%.1fMB", mbSize)}",
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

/**
 * Native Honor Translucent Zoom Capsule (`Screenshot_20261118_144446`).
 * Active zoom preset is shown in Honor Amber (`#F5A623`) inside a dark circular pill.
 */
@Composable
private fun ZoomPresetSelectorBar(
    currentZoom: Float,
    presets: List<Float>,
    onSelectZoom: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x99242426))
            .border(0.8.dp, DividerSubtle, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        presets.forEach { preset ->
            val isSelected = abs(currentZoom - preset) < 0.18f
            val label = when {
                preset < 1.0f -> if (isSelected) String.format(Locale.US, "%.1fx", currentZoom) else String.format(Locale.US, "%.1f", preset)
                isSelected -> {
                    val formatted = String.format(Locale.US, "%.1fx", currentZoom)
                    if (formatted.endsWith(".0x")) "${currentZoom.roundToInt()}x" else formatted
                }
                else -> "${preset.roundToInt()}"
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isSelected) 36.dp else 30.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF141416) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) GalaxyAmber.copy(alpha = 0.65f) else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onSelectZoom(preset) }
                    .testTag("zoom_pill_${preset.toString().replace('.', '_')}")
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) GalaxyAmber else TextPrimaryWhite
                )
            }
        }
    }
}

/**
 * Native Honor Mode Carousel (`NIGHT`, `PORTRAIT`, `PHOTO`, `VIDEO`, `PRO`, `MORE`).
 * Clean uppercase text with active mode highlighted in Honor Amber (`#F5A623`).
 */
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
            .padding(horizontal = 16.dp),
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
                    .clickable { onSelectMode(mode) }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("mode_tab_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) GalaxyAmber else TextPrimaryWhite.copy(alpha = 0.85f)
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
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("mode_tab_more")
        ) {
            Text(
                text = "MORE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryWhite.copy(alpha = 0.85f)
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
                    .size(54.dp)
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
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(1.5.dp, DividerSubtle, CircleShape)
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

        val isVideoMode = state.isVideoStreamMode
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size((76 * shutterScale).dp)
                .clip(CircleShape)
                .border(
                    width = 3.5.dp,
                    color = if (state.isRecordingVideo) RecordCrimson else TextPrimaryWhite.copy(alpha = 0.88f),
                    shape = CircleShape
                )
                .clickable(onClick = onPrimaryShutter)
                .padding(6.dp)
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
                        .background(TextPrimaryWhite)
                )
            }
        }

        if (state.isRecordingVideo) {
            IconButton(
                onClick = onCaptureLiveSnapshot,
                modifier = Modifier
                    .size(54.dp)
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
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .clickable(onClick = onSwitchLens)
                    .testTag("switch_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = stringResource(R.string.switch_camera_desc),
                    tint = TextPrimaryWhite,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewfinderModeOverlayCanvas(
    mode: CameraMode,
    gridLinesEnabled: Boolean,
    levelEnabled: Boolean,
    deviceRollDegrees: Float,
    portraitBlurStrength: Float,
    proSettings: ProCameraSettings,
    exposureStepEv: Float,
    creativeFiltersEnabled: Boolean,
    activeLut: PhotoFilterPreset,
    activeLogProfile: LogProfile,
    lutIntensity: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.44f)

        // 0. Real-Time Pro Mode Exposure & White Balance Feedback Veil (guarantees visible response on all API levels)
        if (mode == CameraMode.PRO && proSettings.hasAnyManualOverride) {
            var evStops = proSettings.exposureCompensationIndex * exposureStepEv
            proSettings.iso?.let { iso ->
                evStops += (kotlin.math.ln(iso.toFloat() / 200f) / kotlin.math.ln(2f)) * 0.45f
            }
            proSettings.shutterSpeedNs?.let { ns ->
                evStops += (kotlin.math.ln(ns.toDouble() / 8_000_000.0).toFloat() / kotlin.math.ln(2f)) * 0.40f
            }
            if (evStops > 0.12f) {
                drawRect(color = Color.White.copy(alpha = (evStops * 0.08f).coerceAtMost(0.28f)))
            } else if (evStops < -0.12f) {
                drawRect(color = Color.Black.copy(alpha = (abs(evStops) * 0.12f).coerceAtMost(0.42f)))
            }

            val kelvin = if (proSettings.whiteBalance == com.example.model.WhiteBalancePreset.MANUAL_KELVIN) {
                proSettings.customKelvin
            } else {
                proSettings.whiteBalance.kelvin
            }
            if (proSettings.whiteBalance != com.example.model.WhiteBalancePreset.AUTO) {
                val kDelta = (kelvin - 5500f) / 4500f
                if (kDelta > 0.05f) {
                    drawRect(color = Color(0xFFFFA726).copy(alpha = (kDelta * 0.16f).coerceAtMost(0.18f)))
                } else if (kDelta < -0.05f) {
                    drawRect(color = Color(0xFF29B6F6).copy(alpha = (abs(kDelta) * 0.16f).coerceAtMost(0.18f)))
                }
            }
        }

        // Subtle atmospheric LUT / Log tint overlay ONLY if creative filters are explicitly enabled
        if (creativeFiltersEnabled && activeLut != PhotoFilterPreset.ORIGINAL && lutIntensity > 0.05f) {
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
        if (creativeFiltersEnabled && activeLogProfile != LogProfile.STANDARD_REC709) {
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

        // 2. Native Honor Center Horizon Level Indicator (`Screenshot_20261118_144446` & `144457`)
        if (levelEnabled) {
            val isLevel = abs(deviceRollDegrees) < 2.2f || abs(abs(deviceRollDegrees) - 90f) < 2.2f
            val levelColor = if (isLevel) GalaxyAmber else Color.White.copy(alpha = 0.68f)
            val circleRadius = minOf(w, h) * 0.068f
            val outerSpan = minOf(w, h) * 0.34f

            drawCircle(
                color = levelColor,
                radius = circleRadius,
                center = center,
                style = Stroke(width = if (isLevel) 3.2f else 2.2f)
            )
            rotate(degrees = -deviceRollDegrees, pivot = center) {
                // Left wing line
                drawLine(
                    color = levelColor,
                    start = Offset(center.x - outerSpan, center.y),
                    end = Offset(center.x - circleRadius, center.y),
                    strokeWidth = if (isLevel) 3.2f else 2.0f
                )
                // Right wing line
                drawLine(
                    color = levelColor,
                    start = Offset(center.x + circleRadius, center.y),
                    end = Offset(center.x + outerSpan, center.y),
                    strokeWidth = if (isLevel) 3.2f else 2.0f
                )
            }
        }

        // 3. Portrait Mode Bokeh Framing Guide
        if (mode == CameraMode.PORTRAIT) {
            val radius = minOf(w, h) * 0.36f
            drawCircle(
                color = GalaxyAmber.copy(alpha = 0.35f + portraitBlurStrength * 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 3f)
            )
        }

        // 4. Panorama Mode Level Horizon & Wide Strip Guide
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

        // 5. Document Scanner Framing Guide
        if (mode == CameraMode.SCAN_DOCUMENT) {
            val docLeft = w * 0.12f
            val docTop = h * 0.18f
            val docRight = w * 0.88f
            val docBottom = h * 0.72f
            drawRect(
                color = GalaxyAmber.copy(alpha = 0.85f),
                topLeft = Offset(docLeft, docTop),
                size = androidx.compose.ui.geometry.Size(docRight - docLeft, docBottom - docTop),
                style = Stroke(width = 3.5f)
            )
        }

        // 6. Pro Manual Focus Peaking Ring Indicator
        if (mode == CameraMode.PRO && proSettings.manualFocusDistance != null && proSettings.focusPeakingEnabled) {
            val dist = proSettings.manualFocusDistance
            val focusRadius = minOf(w, h) * (0.14f + dist * 0.22f)
            drawCircle(
                color = EmeraldOptimal.copy(alpha = 0.75f),
                radius = focusRadius,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = EmeraldOptimal.copy(alpha = 0.35f),
                radius = focusRadius * 0.65f,
                center = center,
                style = Stroke(width = 1.5f)
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
            contentDescription = "Camera Array",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.5.dp, GalaxyAmber.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "HONOR 600 CAM • 4K 60FPS & AI SUPER-RES",
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
