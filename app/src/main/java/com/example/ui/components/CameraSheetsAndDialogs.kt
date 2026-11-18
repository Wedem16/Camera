package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AiCoachUiState
import com.example.model.CameraMode
import com.example.model.GeminiSceneCritique
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.PhotoFilterPreset
import com.example.ui.theme.AiCyan
import com.example.ui.theme.AiViolet
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GalaxyAmber
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RecordCrimson
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryZinc
import com.example.viewmodel.CameraUiState
import java.util.Locale
import kotlin.math.roundToInt

private val BITRATE_PRESETS_MBPS = listOf(24, 50, 100, 200, 300, 400, 600, 800, 1000)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LutAndLogBottomSheet(
    state: CameraUiState,
    onSelectCategory: (LutCategory) -> Unit,
    onSelectLut: (PhotoFilterPreset) -> Unit,
    onUpdateIntensity: (Float) -> Unit,
    onSelectLogProfile: (LogProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredLuts = if (state.selectedLutCategory == LutCategory.ALL) {
        PhotoFilterPreset.entries
    } else {
        PhotoFilterPreset.entries.filter {
            it == PhotoFilterPreset.ORIGINAL || it.category == state.selectedLutCategory
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimaryWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GalaxyAmber)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = "Creative LUTs",
                            tint = AmoledBlack
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "OPTIONAL CREATIVE LUTS & LOGS",
                            style = MaterialTheme.typography.labelLarge,
                            color = GalaxyAmber
                        )
                        Text(
                            text = "Default is Neutral (Off) • Opt-in Film & Travel Color Looks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close LUT & Log Sheet",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOG GAMMA PROFILE (${state.activeLogProfile.shortCode})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AiCyan
                )
                Text(
                    text = state.activeLogProfile.dynamicRangeStops,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldOptimal
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogProfile.entries.forEach { profile ->
                    val selected = state.activeLogProfile == profile
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) AiCyan.copy(alpha = 0.2f) else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (selected) AiCyan else DividerSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLogProfile(profile) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("log_profile_${profile.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = profile.shortCode,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) AiCyan else TextPrimaryWhite
                            )
                            Text(
                                text = profile.dynamicRangeStops,
                                style = MaterialTheme.typography.labelSmall,
                                color = GalaxyAmber
                            )
                        }
                        Text(
                            text = profile.fullTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DividerSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE LUT: ${state.activeLut.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GalaxyAmber
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Strength ${(state.lutIntensity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianElevated)
                            .clickable {
                                onSelectLut(PhotoFilterPreset.ORIGINAL)
                                onSelectLogProfile(LogProfile.STANDARD_REC709)
                                onUpdateIntensity(1.0f)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset LUT and Log",
                            tint = GalaxyAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RESET NEUTRAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyAmber
                        )
                    }
                }
            }

            Slider(
                value = state.lutIntensity,
                onValueChange = onUpdateIntensity,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = GalaxyAmber,
                    activeTrackColor = GalaxyAmber,
                    inactiveTrackColor = DividerSubtle
                ),
                modifier = Modifier.testTag("lut_intensity_slider")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LutCategory.entries.forEach { cat ->
                    val isSel = state.selectedLutCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSel) GalaxyAmber else ObsidianElevated)
                            .clickable { onSelectCategory(cat) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("lut_category_${cat.name.lowercase()}")
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) AmoledBlack else TextPrimaryWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                filteredLuts.forEach { lut ->
                    val isSelected = state.activeLut == lut
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) GalaxyAmber.copy(alpha = 0.18f) else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GalaxyAmber else DividerSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLut(lut) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("viewfinder_lut_${lut.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterVintage,
                                contentDescription = lut.label,
                                tint = if (isSelected) GalaxyAmber else AiCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = lut.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) GalaxyAmber else TextPrimaryWhite,
                                    maxLines = 1
                                )
                                Text(
                                    text = lut.category.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryZinc
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BitrateSelectorBottomSheet(
    currentBitrateMbps: Int,
    videoResolutionLabel: String,
    videoFpsLabel: String,
    onSelectBitrateMbps: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mbPerSecond = currentBitrateMbps / 8.0f
    val gbPerMinute = (mbPerSecond * 60f) / 1024f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimaryWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GalaxyAmber)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Video Bitrate",
                            tint = AmoledBlack
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "VIDEO ENCODER BITRATE",
                            style = MaterialTheme.typography.labelLarge,
                            color = GalaxyAmber
                        )
                        Text(
                            text = "$videoResolutionLabel @ $videoFpsLabel • 10 to 1000 Mbps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Bitrate Sheet",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianElevated)
                    .border(1.dp, GalaxyAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TARGET ENCODER BITRATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryZinc
                    )
                    Text(
                        text = "$currentBitrateMbps Mbps",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "~%.1f MB/sec", mbPerSecond),
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal
                    )
                    Text(
                        text = String.format(Locale.US, "~%.2f GB/min", gbPerMinute),
                        style = MaterialTheme.typography.labelSmall,
                        color = AiCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Slider(
                value = currentBitrateMbps.toFloat(),
                onValueChange = { onSelectBitrateMbps(it.roundToInt().coerceIn(10, 1000)) },
                valueRange = 10f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = GalaxyAmber,
                    activeTrackColor = GalaxyAmber,
                    inactiveTrackColor = DividerSubtle
                ),
                modifier = Modifier.testTag("video_bitrate_slider")
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "QUICK BITRATE PRESETS (MBPS)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryZinc
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                BITRATE_PRESETS_MBPS.forEach { mbps ->
                    val isSel = currentBitrateMbps == mbps
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) GalaxyAmber else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSel) GalaxyAmber else DividerSubtle,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectBitrateMbps(mbps) }
                            .minimumInteractiveComponentSize()
                            .padding(vertical = 10.dp)
                            .testTag("bitrate_preset_$mbps")
                    ) {
                        Text(
                            text = if (mbps == 1000) "1000 MAX" else "$mbps Mbps",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) AmoledBlack else TextPrimaryWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreModesBottomSheet(
    activeMode: CameraMode,
    pinnedModes: List<CameraMode>,
    onSelectMode: (CameraMode) -> Unit,
    onTogglePinMode: (CameraMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimaryWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CAMERA SHOOTING MODES",
                        style = MaterialTheme.typography.labelLarge,
                        color = GalaxyAmber
                    )
                    Text(
                        text = "Tap any mode to launch or pin it to your bottom carousel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryZinc
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close More Modes",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                CameraMode.entries.forEach { mode ->
                    val isSelected = activeMode == mode
                    val isPinned = pinnedModes.contains(mode)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) GalaxyAmber.copy(alpha = 0.16f) else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GalaxyAmber else DividerSubtle,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onSelectMode(mode) }
                            .padding(14.dp)
                            .testTag("more_mode_card_${mode.name.lowercase()}")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) GalaxyAmber else AmoledBlack)
                                ) {
                                    Icon(
                                        imageVector = iconForMode(mode),
                                        contentDescription = mode.badgeTitle,
                                        tint = if (isSelected) AmoledBlack else GalaxyAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onTogglePinMode(mode) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = if (isPinned) "Pinned to carousel" else "Pin to carousel",
                                        tint = if (isPinned) GalaxyAmber else TextSecondaryZinc.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = mode.badgeTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) GalaxyAmber else TextPrimaryWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryZinc,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSceneCoachBottomSheet(
    state: CameraUiState,
    onRetryAnalysis: () -> Unit,
    onApplyRecommendedPro: (GeminiSceneCritique) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimaryWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(AiCyan, AiViolet))
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Scene Coach",
                            tint = AmoledBlack
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HYBRID AI SCENE COACH",
                            style = MaterialTheme.typography.labelLarge,
                            color = AiCyan
                        )
                        Text(
                            text = "On-Device Telemetry + Gemini Vision Critique",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close AI Coach",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianElevated)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REAL-TIME ON-DEVICE SCENE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryZinc
                    )
                    Text(
                        text = state.sceneMetrics.detectedScene.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = GalaxyAmber
                    )
                    Text(
                        text = state.sceneMetrics.detectedScene.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryZinc
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LUMA ${state.sceneMetrics.averageLuminance.toInt()}/255",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal
                    )
                    Text(
                        text = "DR ${(state.sceneMetrics.dynamicRangeRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AiCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val aiState = state.aiCoachState) {
                is AiCoachUiState.Idle, is AiCoachUiState.Analyzing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AiCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing viewfinder composition & lighting with Gemini...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
                }
                is AiCoachUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RecordCrimson.copy(alpha = 0.14f))
                            .border(1.dp, RecordCrimson, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI Critique Notice",
                            style = MaterialTheme.typography.titleMedium,
                            color = RecordCrimson
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryWhite
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRetryAnalysis,
                            modifier = Modifier.testTag("ai_retry_button")
                        ) {
                            Text("Retry Gemini Analysis", color = GalaxyAmber)
                        }
                    }
                }
                is AiCoachUiState.Success -> {
                    val critique = aiState.critique
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, AiCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GEMINI VISION CRITIQUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AiCyan
                                )
                                Text(
                                    text = critique.sceneTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimaryWhite
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GalaxyAmber.copy(alpha = 0.18f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${critique.compositionScore}/100",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GalaxyAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = critique.lightingAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pro Tip: ${critique.proCoachingTip}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldOptimal
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = DividerSubtle)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RecommendedPill("ISO", "${critique.recommendedIso}")
                            RecommendedPill("SHUTTER", critique.recommendedShutterLabel)
                            RecommendedPill("WB", "${critique.recommendedKelvin}K")
                            RecommendedPill("EV", String.format(Locale.US, "%+.1f", critique.recommendedEvShift))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onApplyRecommendedPro(critique) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GalaxyAmber,
                                contentColor = AmoledBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_ai_pro_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apply Recommended Pro Settings",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun RecommendedPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryZinc
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = GalaxyAmber,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CameraSettingsDialog(
    state: CameraUiState,
    onToggleAiAutoEditAfterCapture: () -> Unit,
    onCycleAiUpscaleMode: () -> Unit,
    onToggleCreativeFilters: () -> Unit,
    onTogglePhotoResolution: () -> Unit,
    onCyclePhotoFormat: () -> Unit,
    onToggleVideoResolution: () -> Unit,
    onCycleVideoFormat: () -> Unit,
    onToggleVideoFps: () -> Unit,
    onOpenBitrateSheet: () -> Unit,
    onOpenLutLogSheet: () -> Unit,
    onToggleDiagnosticLogs: () -> Unit,
    onToggleVideoStabilization: () -> Unit,
    onToggleAiOptimizer: () -> Unit,
    onToggleGridLines: () -> Unit,
    onToggleWatermark: () -> Unit,
    onToggleVolumeShutter: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ObsidianSurface)
                .border(1.dp, DividerSubtle, RoundedCornerShape(24.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HONOR 600 CAM • HARDWARE SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = GalaxyAmber
                    )
                    Text(
                        text = "4K 60fps Temporal Denoise • Fast On-Device HDR SR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryZinc
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val caps = state.hardwareCapabilities
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianElevated)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${caps.mainSensorSpec} • ${caps.ultraWideSensorSpec}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AiCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${caps.socPlatformSpec} • ${caps.osPlatformSpec}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldOptimal
                )
                Text(
                    text = "Physical IDs: ${if (caps.physicalCameraIds.isEmpty()) "Logical Default" else caps.physicalCameraIds.joinToString()} • RAW: ${if (caps.supportsRawCapability) "Yes" else "Fallback"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GalaxyAmber
                )
                Text(
                    text = state.activeZoomProfile.pipelineSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryZinc
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SettingToggleRow(
                title = "AI Auto-Edit & Upscale After Capture",
                subtitle = "Saves unchanged Original JPEG + automatic AI-mastered & upscaled copy (_AI_EDIT_2x.jpg)",
                checked = state.aiAutoEditAfterCaptureEnabled,
                onCheckedChange = { onToggleAiAutoEditAfterCapture() },
                testTag = "setting_ai_auto_edit"
            )

            SettingActionRow(
                title = "On-Device AI Upscale Mode",
                subtitle = "Original (default) or separate _AI2x / _AI4x file",
                badge = state.aiUpscaleMode.uiLabel,
                onClick = onCycleAiUpscaleMode,
                testTag = "setting_ai_upscale_mode"
            )

            SettingActionRow(
                title = "Photo Capture Format (JPG / RAW+JPG / PNG)",
                subtitle = state.photoFormat.fullTitle,
                badge = state.photoFormat.badgeLabel,
                onClick = onCyclePhotoFormat,
                testTag = "setting_photo_format"
            )

            SettingActionRow(
                title = "Photo Target Resolution",
                subtitle = state.photoResolution.fullTitle,
                badge = state.photoResolution.shortLabel,
                onClick = onTogglePhotoResolution,
                testTag = "setting_photo_resolution"
            )

            SettingActionRow(
                title = "Video Container Format (MP4 / MOV / MKV)",
                subtitle = state.videoFormat.fullTitle,
                badge = state.videoFormat.badgeLabel,
                onClick = onCycleVideoFormat,
                testTag = "setting_video_format"
            )

            SettingActionRow(
                title = "Video Resolution (4K UHD / 1080p)",
                subtitle = state.videoResolution.fullTitle,
                badge = state.videoResolution.shortLabel,
                onClick = onToggleVideoResolution,
                testTag = "setting_video_resolution"
            )

            SettingActionRow(
                title = "Video Encoder Bitrate (10–1000 Mbps)",
                subtitle = "Configure target bitrate from 10 to 1000 Mbps",
                badge = "${state.videoBitrateMbps} Mbps",
                onClick = {
                    onDismiss()
                    onOpenBitrateSheet()
                },
                testTag = "setting_video_bitrate"
            )

            SettingActionRow(
                title = "Video Frame Rate",
                subtitle = "Smooth 60fps or standard 30fps capture",
                badge = state.videoFps.label,
                onClick = onToggleVideoFps,
                testTag = "setting_video_fps"
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = DividerSubtle
            )

            SettingToggleRow(
                title = "Enable Creative Filters & LUTs",
                subtitle = "Default is OFF (Neutral color). Turn ON to use LUTs/Logs",
                checked = state.creativeFiltersEnabled,
                onCheckedChange = { onToggleCreativeFilters() },
                testTag = "setting_creative_filters"
            )

            if (state.creativeFiltersEnabled) {
                SettingActionRow(
                    title = "Select Creative LUT & Log Curve",
                    subtitle = "${state.activeLut.label} • ${state.activeLogProfile.fullTitle}",
                    badge = state.activeLogProfile.shortCode,
                    onClick = {
                        onDismiss()
                        onOpenLutLogSheet()
                    },
                    testTag = "setting_lut_log"
                )
            }

            SettingToggleRow(
                title = "AI Scene Optimizer",
                subtitle = "Optional scene color adjustment (Default OFF)",
                checked = state.aiSceneOptimizerEnabled,
                onCheckedChange = { onToggleAiOptimizer() },
                testTag = "setting_ai_optimizer"
            )

            SettingToggleRow(
                title = "Live Telemetry & Logs HUD",
                subtitle = "Show real-time hardware, NR/EDGE & zoom logs on screen",
                checked = state.showDiagnosticLogsOverlay,
                onCheckedChange = { onToggleDiagnosticLogs() },
                testTag = "setting_diagnostic_logs"
            )

            SettingToggleRow(
                title = "Optical / Video Stabilization (OIS+EIS)",
                subtitle = "Hardware Camera2 stabilization",
                checked = state.videoStabilizationEnabled,
                onCheckedChange = { onToggleVideoStabilization() },
                testTag = "setting_video_stabilization"
            )

            SettingToggleRow(
                title = "3×3 Composition Grid",
                subtitle = "Rule-of-thirds viewfinder alignment guide",
                checked = state.gridLinesEnabled,
                onCheckedChange = { onToggleGridLines() },
                testTag = "setting_grid_lines"
            )

            SettingToggleRow(
                title = "Timestamp Watermark",
                subtitle = "Default OFF (Unchanged clean photo)",
                checked = state.watermarkEnabled,
                onCheckedChange = { onToggleWatermark() },
                testTag = "setting_watermark"
            )

            SettingToggleRow(
                title = "Volume Key Shutter",
                subtitle = "Press Volume Up/Down to take photo or record video",
                checked = state.volumeButtonShutterEnabled,
                onCheckedChange = { onToggleVolumeShutter() },
                testTag = "setting_volume_shutter"
            )
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryZinc
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GalaxyAmber.copy(alpha = 0.18f))
                .border(1.dp, GalaxyAmber, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = GalaxyAmber,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryZinc
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AmoledBlack,
                checkedTrackColor = GalaxyAmber,
                uncheckedThumbColor = TextSecondaryZinc,
                uncheckedTrackColor = ObsidianElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

fun iconForMode(mode: CameraMode): ImageVector {
    return when (mode) {
        CameraMode.PORTRAIT -> Icons.Default.BlurOn
        CameraMode.PHOTO -> Icons.Default.CameraAlt
        CameraMode.VIDEO -> Icons.Default.Videocam
        CameraMode.PRO -> Icons.Default.Tune
        CameraMode.NIGHT -> Icons.Default.NightlightRound
        CameraMode.HDR -> Icons.Default.HdrOn
        CameraMode.BEAUTY -> Icons.Default.FaceRetouchingNatural
        CameraMode.PANORAMA -> Icons.Default.PanoramaHorizontal
    }
}
