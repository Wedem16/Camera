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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MacroOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AiCoachUiState
import com.example.model.AiUpscaleMode
import com.example.model.CameraMode
import com.example.model.FlashSetting
import com.example.model.GeminiSceneCritique
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.PhotoFilterPreset
import com.example.model.VideoHdrProfileSetting
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
private val HonorCardDark = Color(0xFF1C1C1E)
private val HonorDividerDark = Color(0xFF2C2C2E)
private val HonorQuickCardBg = Color(0xEB242426)
private val HonorQuickCircleBg = Color(0xFF3A3A3C)

/**
 * Native Honor Camera Frosted Quick Menu Popup Card (`Screenshot_20261118_144502`).
 * Features a floating Settings gear button above the top-right corner, a collapse chevron at top center,
 * and a 4-column grid of circular quick-action toggles (Flash, Specifications, Ratio, Audio, Filters,
 * Gridlines, Level, 4K AI SR / Video HDR Log).
 */
@Composable
fun HonorQuickMenuPopupCard(
    state: CameraUiState,
    onCycleFlash: () -> Unit,
    onToggleSpecifications: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleMuteAudio: () -> Unit,
    onOpenFilters: () -> Unit,
    onToggleGridlines: () -> Unit,
    onToggleLevel: () -> Unit,
    onCycleAiUpscale: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Floating Settings Gear Button above top-right of the frosted popup card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(bottom = 10.dp, end = 4.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(HonorQuickCardBg)
                .border(1.dp, DividerSubtle, CircleShape)
                .clickable(onClick = onOpenFullSettings)
                .testTag("quick_menu_settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Open Settings",
                tint = TextPrimaryWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        // Frosted Rounded Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HonorQuickCardBg)
                .border(1.dp, DividerSubtle, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top collapse chevron
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .testTag("quick_menu_close_chevron")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close Quick Menu",
                    tint = TextSecondaryZinc,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val specBadge = if (state.isVideoStreamMode) {
                "${if (state.videoResolution.widthPx >= 3840) "4K" else "1080P"}·${state.videoFps.fps}"
            } else {
                state.photoResolution.shortLabel
            }

            // Row 1: Flash, Specifications, Ratio, Audio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickMenuCircleItem(
                    icon = when (state.flashSetting) {
                        FlashSetting.AUTO -> Icons.Default.FlashAuto
                        FlashSetting.ON -> Icons.Default.FlashOn
                        FlashSetting.OFF -> Icons.Default.FlashOff
                    },
                    badgeText = null,
                    label = "Flash",
                    isActive = state.flashSetting != FlashSetting.OFF,
                    onClick = onCycleFlash,
                    testTag = "quick_menu_flash"
                )

                QuickMenuCircleItem(
                    icon = null,
                    badgeText = specBadge,
                    label = "Specifications",
                    isActive = true,
                    onClick = onToggleSpecifications,
                    testTag = "quick_menu_specifications"
                )

                QuickMenuCircleItem(
                    icon = null,
                    badgeText = state.aspectRatio.label.replace(" 4K", ""),
                    label = "Ratio",
                    isActive = state.aspectRatio.ratioValue != null,
                    onClick = onCycleAspectRatio,
                    testTag = "quick_menu_ratio"
                )

                QuickMenuCircleItem(
                    icon = if (state.muteShutterAndAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.Default.Mic,
                    badgeText = null,
                    label = "Audio",
                    isActive = !state.muteShutterAndAudio,
                    onClick = onToggleMuteAudio,
                    testTag = "quick_menu_audio"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Row 2: Filters, Gridlines, Level, 4K AI SR (or Video HDR / HDR Log in Video Mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickMenuCircleItem(
                    icon = Icons.Default.FilterVintage,
                    badgeText = null,
                    label = "Filters",
                    isActive = state.creativeFiltersEnabled && state.activeLut != PhotoFilterPreset.ORIGINAL,
                    onClick = onOpenFilters,
                    testTag = "quick_menu_filters"
                )

                QuickMenuCircleItem(
                    icon = Icons.Default.GridOn,
                    badgeText = null,
                    label = "Gridlines",
                    isActive = state.gridLinesEnabled,
                    onClick = onToggleGridlines,
                    testTag = "quick_menu_gridlines"
                )

                QuickMenuCircleItem(
                    icon = Icons.Default.Straighten,
                    badgeText = null,
                    label = "Level",
                    isActive = state.levelEnabled,
                    onClick = onToggleLevel,
                    testTag = "quick_menu_level"
                )

                if (state.isVideoStreamMode) {
                    QuickMenuCircleItem(
                        icon = Icons.Default.HdrOn,
                        badgeText = state.videoHdrProfile.badgeLabel,
                        label = "HDR / Log",
                        isActive = state.videoHdrProfile != VideoHdrProfileSetting.SDR_REC709,
                        onClick = onCycleAiUpscale,
                        testTag = "quick_menu_ai_sr"
                    )
                } else {
                    QuickMenuCircleItem(
                        icon = Icons.Default.AutoAwesome,
                        badgeText = if (state.aiUpscaleMode == AiUpscaleMode.ORIGINAL) null else "${state.aiUpscaleMode.scaleFactor}x",
                        label = state.aiUpscaleMode.uiLabel,
                        isActive = state.aiUpscaleMode != AiUpscaleMode.ORIGINAL,
                        onClick = onCycleAiUpscale,
                        testTag = "quick_menu_ai_sr"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun QuickMenuCircleItem(
    icon: ImageVector?,
    badgeText: String?,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) GalaxyAmber.copy(alpha = 0.24f) else HonorQuickCircleBg
                )
                .border(
                    width = if (isActive) 1.5.dp else 0.dp,
                    color = if (isActive) GalaxyAmber else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) GalaxyAmber else TextPrimaryWhite,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) GalaxyAmber else TextPrimaryWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) GalaxyAmber else TextPrimaryWhite,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LutAndLogBottomSheet(
    state: CameraUiState,
    onSelectCategory: (LutCategory) -> Unit,
    onSelectLut: (PhotoFilterPreset) -> Unit,
    onUpdateIntensity: (Float) -> Unit,
    onSelectLogProfile: (LogProfile) -> Unit,
    onSelectVideoHdrProfile: (VideoHdrProfileSetting) -> Unit = {},
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
                            text = "HONOR CREATIVE LUTS & HDR LOG",
                            style = MaterialTheme.typography.labelLarge,
                            color = GalaxyAmber
                        )
                        Text(
                            text = "10-Bit Video HDR / Magic-Log & Film Color Looks",
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

            // Video HDR & Cinema HDR Log Profile Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VIDEO HDR & MAGIC-LOG PROFILE (${state.videoHdrProfile.badgeLabel})",
                    style = MaterialTheme.typography.labelSmall,
                    color = GalaxyAmber
                )
                Text(
                    text = state.videoHdrProfile.dynamicRangeStops,
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
                VideoHdrProfileSetting.entries.forEach { hdrProfile ->
                    val selected = state.videoHdrProfile == hdrProfile
                    val accent = if (hdrProfile.isLogCurve) AiCyan else GalaxyAmber
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) accent.copy(alpha = 0.2f) else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (selected) accent else DividerSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectVideoHdrProfile(hdrProfile) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("video_hdr_profile_${hdrProfile.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = hdrProfile.badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accent else TextPrimaryWhite
                            )
                            Text(
                                text = hdrProfile.dynamicRangeStops,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOptimal
                            )
                        }
                        Text(
                            text = hdrProfile.fullTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryZinc
                        )
                    }
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

/**
 * Full-Screen Translucent Honor `MORE` Modes Overlay (`Screenshot_20261118_144516`).
 * Displays a clean 3-column grid of outline icons + uppercase labels (`MULTI-VIDEO`, `SLOW-MO`,
 * `PANORAMA`, `TIME-LAPSE`, `UNDERWATER`, `SUPER MACRO`, `HIGH-RES`, `SCAN DOCUMENT`, `HDR`, `BEAUTY`, etc.).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoreModesBottomSheet(
    activeMode: CameraMode,
    pinnedModes: List<CameraMode>,
    onSelectMode: (CameraMode) -> Unit,
    onTogglePinMode: (CameraMode) -> Unit,
    onDismiss: () -> Unit
) {
    // Prioritize Honor MORE modes first, followed by standard carousel modes
    val orderedModes = listOf(
        CameraMode.MULTI_VIDEO,
        CameraMode.SLOW_MO,
        CameraMode.PANORAMA,
        CameraMode.TIME_LAPSE,
        CameraMode.UNDERWATER,
        CameraMode.SUPER_MACRO,
        CameraMode.HIGH_RES,
        CameraMode.SCAN_DOCUMENT,
        CameraMode.HDR,
        CameraMode.BEAUTY,
        CameraMode.NIGHT,
        CameraMode.PRO
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.86f))
                .clickable(onClick = onDismiss)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Title + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MORE SHOOTING MODES",
                        style = MaterialTheme.typography.labelLarge,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close More Modes",
                            tint = TextPrimaryWhite
                        )
                    }
                }

                // Center 3-column Honor MORE Grid (matching Screenshot_20261118_144516)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    maxItemsInEachRow = 3
                ) {
                    orderedModes.forEach { mode ->
                        val isSelected = activeMode == mode
                        val isPinned = pinnedModes.contains(mode)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(98.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectMode(mode) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("more_mode_card_${mode.name.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) GalaxyAmber.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) GalaxyAmber else Color.White.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = iconForMode(mode),
                                    contentDescription = mode.label,
                                    tint = if (isSelected) GalaxyAmber else TextPrimaryWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) GalaxyAmber else TextPrimaryWhite,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onTogglePinMode(mode) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = if (isPinned) "Pinned" else "Pin to carousel",
                                    tint = if (isPinned) GalaxyAmber else TextSecondaryZinc.copy(alpha = 0.55f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isPinned) "Pinned" else "Pin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPinned) GalaxyAmber else TextSecondaryZinc,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                // Bottom Mode indicator matching Honor's bottom bar ("PRO   MORE")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeMode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondaryZinc,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Text(
                        text = "MORE",
                        style = MaterialTheme.typography.labelLarge,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
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

/**
 * Full-Screen Native Honor `< Settings` Page (`Screenshot_20261118_144546`, `144555`, `144600`, `144507`).
 * Includes GENERAL, PHOTO, VIDEO, and OTHER rounded dark cards (`#1C1C1E` on `#000000`) with
 * Honor Amber (`#F5A623`) switches and a bottom `Restore defaults` button.
 */
@Composable
fun CameraSettingsDialog(
    state: CameraUiState,
    onToggleAiAutoEditAfterCapture: () -> Unit,
    onCycleAiUpscaleMode: () -> Unit,
    onToggleCreativeFilters: () -> Unit,
    onTogglePhotoResolution: () -> Unit,
    onCyclePhotoFormat: () -> Unit,
    onCycleAspectRatio: () -> Unit = {},
    onCycleFocalLength: () -> Unit = {},
    onToggleRecommendFilters: () -> Unit = {},
    onToggleKeepOriginalWithAiEnhance: () -> Unit = {},
    onToggleVideoResolution: () -> Unit,
    onCycleVideoFormat: () -> Unit,
    onCycleVideoHdrProfile: () -> Unit = {},
    onToggleVideoFps: () -> Unit,
    onToggleAutoFpsLowLight: () -> Unit = {},
    onToggleLockWhiteBalanceInVideo: () -> Unit = {},
    onToggleEfficientVideoFormat: () -> Unit = {},
    onToggleScreenOffRecording: () -> Unit = {},
    onOpenBitrateSheet: () -> Unit,
    onOpenLutLogSheet: () -> Unit,
    onToggleDiagnosticLogs: () -> Unit,
    onToggleVideoStabilization: () -> Unit,
    onToggleAiOptimizer: () -> Unit,
    onCycleTimer: () -> Unit = {},
    onToggleGridLines: () -> Unit,
    onToggleLevel: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleMirrorReflection: () -> Unit = {},
    onToggleFloatingShutter: () -> Unit = {},
    onToggleLocationTag: () -> Unit = {},
    onToggleRememberSettings: () -> Unit = {},
    onToggleAutoLensSwitching: () -> Unit = {},
    onToggleWatermark: () -> Unit,
    onToggleVolumeShutter: () -> Unit,
    onRestoreDefaults: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Honor Top Header Bar (`< Settings`)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimaryWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // SECTION 1: GENERAL (Screenshot_20261118_144546)
                HonorSectionTitle("GENERAL")
                HonorSettingsCard {
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Description,
                        title = "Watermark",
                        subtitle = "Add Honor 600 Cam timestamp & lens watermark to photos",
                        checked = state.watermarkEnabled,
                        onCheckedChange = { onToggleWatermark() },
                        testTag = "setting_watermark"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.BookmarkBorder,
                        title = "Remember settings",
                        subtitle = "Remember last used camera mode, filters, and Pro settings",
                        checked = state.rememberSettingsEnabled,
                        onCheckedChange = { onToggleRememberSettings() },
                        testTag = "setting_remember_settings"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.ViewCarousel,
                        title = "Customize status bar",
                        subtitle = "Display live sensor telemetry & diagnostic logs HUD",
                        checked = state.showDiagnosticLogsOverlay,
                        onCheckedChange = { onToggleDiagnosticLogs() },
                        testTag = "setting_diagnostic_logs"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Cameraswitch,
                        title = "Auto lens switching",
                        subtitle = "Automatically switch to macro or telephoto lens when zooming",
                        checked = state.autoLensSwitchingEnabled,
                        onCheckedChange = { onToggleAutoLensSwitching() },
                        testTag = "setting_auto_lens_switching"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "In-camera AI button actions",
                        subtitle = "Configure Honor 600 AI post-capture mastering & super-resolution",
                        valueText = if (state.aiAutoEditAfterCaptureEnabled) "4K AI ON" else "Off",
                        onClick = onToggleAiAutoEditAfterCapture,
                        testTag = "setting_ai_auto_edit"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: PHOTO (Screenshot_20261118_144546 & 144555)
                HonorSectionTitle("PHOTO")
                HonorSettingsCard {
                    HonorChevronSettingRow(
                        icon = Icons.Default.AspectRatio,
                        title = "Aspect ratio",
                        subtitle = null,
                        valueText = state.aspectRatio.label.replace(" 4K", ""),
                        onClick = onCycleAspectRatio,
                        testTag = "setting_photo_aspect_ratio"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.FilterCenterFocus,
                        title = "Focal length",
                        subtitle = "Set default 1x/1.5x/2x optical focal length",
                        valueText = "${state.defaultFocalLengthMm}mm",
                        onClick = onCycleFocalLength,
                        testTag = "setting_focal_length"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.PhotoSizeSelectActual,
                        title = "Photo target resolution",
                        subtitle = state.photoResolution.fullTitle,
                        valueText = state.photoResolution.shortLabel,
                        onClick = onTogglePhotoResolution,
                        testTag = "setting_photo_resolution"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.HighQuality,
                        title = "Photo capture format",
                        subtitle = state.photoFormat.fullTitle,
                        valueText = state.photoFormat.badgeLabel,
                        onClick = onCyclePhotoFormat,
                        testTag = "setting_photo_format"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "On-Device AI Upscale mode",
                        subtitle = "Original (default) or separate _AI2x / _AI4x file",
                        valueText = state.aiUpscaleMode.uiLabel,
                        onClick = onCycleAiUpscaleMode,
                        testTag = "setting_ai_upscale_mode"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Smart capture",
                        subtitle = "Use Volume Up/Down keys to snap photos or record video",
                        checked = state.volumeButtonShutterEnabled,
                        onCheckedChange = { onToggleVolumeShutter() },
                        testTag = "setting_volume_shutter"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.FilterVintage,
                        title = "Recommend filters",
                        subtitle = "Enable optional Film, Travel & Cinema LUTs and Log profiles",
                        checked = state.creativeFiltersEnabled,
                        onCheckedChange = { onToggleCreativeFilters() },
                        testTag = "setting_creative_filters"
                    )
                    if (state.creativeFiltersEnabled) {
                        HonorSettingDivider()
                        HonorChevronSettingRow(
                            icon = Icons.Default.ColorLens,
                            title = "Select Creative LUT & Log Curve",
                            subtitle = "${state.activeLut.label} • ${state.activeLogProfile.fullTitle}",
                            valueText = state.activeLogProfile.shortCode,
                            onClick = {
                                onDismiss()
                                onOpenLutLogSheet()
                            },
                            testTag = "setting_lut_log"
                        )
                    }
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.HighQuality,
                        title = "Keep original photo with AI Enhance",
                        subtitle = "Always preserve the unchanged native JPEG alongside AI enhanced copies",
                        checked = state.keepOriginalWithAiEnhance,
                        onCheckedChange = { onToggleKeepOriginalWithAiEnhance() },
                        testTag = "setting_keep_original_ai"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 3: VIDEO (Screenshot_20261118_144555)
                HonorSectionTitle("VIDEO")
                HonorSettingsCard {
                    val videoSpecText = if (state.videoResolution.widthPx >= 3840) {
                        "16:9 4K [${state.videoFps.fps} fps]"
                    } else {
                        "16:9 1080p [${state.videoFps.fps} fps]"
                    }
                    HonorChevronSettingRow(
                        icon = Icons.Default.VideoSettings,
                        title = "Specifications",
                        subtitle = "Tap to toggle 4K UHD / 1080p resolution",
                        valueText = videoSpecText,
                        onClick = onToggleVideoResolution,
                        testTag = "setting_video_resolution"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.HdrOn,
                        title = "Video HDR & Magic-Log Profile",
                        subtitle = "${state.videoHdrProfile.fullTitle} (${state.videoHdrProfile.dynamicRangeStops})",
                        valueText = state.videoHdrProfile.badgeLabel,
                        onClick = onCycleVideoHdrProfile,
                        testTag = "setting_video_hdr_profile"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.Videocam,
                        title = "Video frame rate",
                        subtitle = "Strict 60fps lock with temporal denoise or 30fps",
                        valueText = state.videoFps.label,
                        onClick = onToggleVideoFps,
                        testTag = "setting_video_fps"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.Speed,
                        title = "Video encoder bitrate",
                        subtitle = "Prioritizes high data density (up to 1000 Mbps) for 4K 60fps",
                        valueText = "${state.videoBitrateMbps} Mbps",
                        onClick = {
                            onDismiss()
                            onOpenBitrateSheet()
                        },
                        testTag = "setting_video_bitrate"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.VideoFile,
                        title = "Video container format",
                        subtitle = state.videoFormat.fullTitle,
                        valueText = state.videoFormat.badgeLabel,
                        onClick = onCycleVideoFormat,
                        testTag = "setting_video_format"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Tune,
                        title = "Auto FPS",
                        subtitle = "Automatically reduce the frame rate in low light to brighten the video.",
                        checked = state.autoFpsLowLightEnabled,
                        onCheckedChange = { onToggleAutoFpsLowLight() },
                        testTag = "setting_auto_fps"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Lock,
                        title = "Lock white balance",
                        subtitle = "Avoid color temperature shifting when recording videos.",
                        checked = state.lockWhiteBalanceInVideo,
                        onCheckedChange = { onToggleLockWhiteBalanceInVideo() },
                        testTag = "setting_lock_video_wb"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.VideoFile,
                        title = "Efficient video format",
                        subtitle = "Save up to 35% of space with HEVC H.265 encoding.",
                        checked = state.efficientVideoFormatEnabled,
                        onCheckedChange = { onToggleEfficientVideoFormat() },
                        testTag = "setting_efficient_video_format"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Screen-off recording",
                        subtitle = "When recording videos for more than 3 minutes without any operations, the screen dims to save power.",
                        checked = state.screenOffRecordingEnabled,
                        onCheckedChange = { onToggleScreenOffRecording() },
                        testTag = "setting_screen_off_recording"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Videocam,
                        title = "Optical / Video stabilization (OIS+EIS)",
                        subtitle = "Hardware Camera2 optical & electronic stabilization",
                        checked = state.videoStabilizationEnabled,
                        onCheckedChange = { onToggleVideoStabilization() },
                        testTag = "setting_video_stabilization"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 4: OTHER (Screenshot_20261118_144555 & 144600)
                HonorSectionTitle("OTHER")
                HonorSettingsCard {
                    HonorSwitchSettingRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI camera",
                        subtitle = "Recognize scenes and optimize color and dynamic range",
                        checked = state.aiSceneOptimizerEnabled,
                        onCheckedChange = { onToggleAiOptimizer() },
                        testTag = "setting_ai_optimizer"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.Timer,
                        title = "Timer",
                        subtitle = null,
                        valueText = state.timerSetting.label,
                        onClick = onCycleTimer,
                        testTag = "setting_timer"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.GridOn,
                        title = "Gridlines",
                        subtitle = null,
                        checked = state.gridLinesEnabled,
                        onCheckedChange = { onToggleGridLines() },
                        testTag = "setting_grid_lines"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Straighten,
                        title = "Level",
                        subtitle = null,
                        checked = state.levelEnabled,
                        onCheckedChange = { onToggleLevel() },
                        testTag = "setting_level"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = if (state.muteShutterAndAudio) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Mute",
                        subtitle = null,
                        checked = state.muteShutterAndAudio,
                        onCheckedChange = { onToggleMute() },
                        testTag = "setting_mute"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.Flip,
                        title = "Mirror reflection",
                        subtitle = "Front camera only.",
                        checked = state.mirrorReflectionEnabled,
                        onCheckedChange = { onToggleMirrorReflection() },
                        testTag = "setting_mirror_reflection"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.TouchApp,
                        title = "Floating shutter",
                        subtitle = "Add a movable shutter button.",
                        checked = state.floatingShutterEnabled,
                        onCheckedChange = { onToggleFloatingShutter() },
                        testTag = "setting_floating_shutter"
                    )
                    HonorSettingDivider()
                    HonorSwitchSettingRow(
                        icon = Icons.Default.LocationOn,
                        title = "Location tag",
                        subtitle = "Tag photos and videos with location information.",
                        checked = state.locationTagEnabled,
                        onCheckedChange = { onToggleLocationTag() },
                        testTag = "setting_location_tag"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feedback & About Card (Screenshot_20261118_144600)
                val caps = state.hardwareCapabilities
                HonorSettingsCard {
                    HonorChevronSettingRow(
                        icon = Icons.Default.Info,
                        title = "Feedback",
                        subtitle = "${caps.mainSensorSpec} • ${caps.ultraWideSensorSpec}",
                        valueText = "",
                        onClick = onToggleDiagnosticLogs,
                        testTag = "setting_feedback_row"
                    )
                    HonorSettingDivider()
                    HonorChevronSettingRow(
                        icon = Icons.Default.CameraAlt,
                        title = "About",
                        subtitle = "Honor 600 Cam • ${caps.socPlatformSpec} • ${caps.osPlatformSpec}",
                        valueText = "v10.0",
                        onClick = {},
                        testTag = "setting_about_row"
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom "Restore defaults" pill button (matching Screenshot_20261118_144600)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(HonorCardDark)
                        .clickable {
                            onRestoreDefaults()
                            onDismiss()
                        }
                        .testTag("setting_restore_defaults_button")
                ) {
                    Text(
                        text = "Restore defaults",
                        style = MaterialTheme.typography.titleMedium,
                        color = GalaxyAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun HonorSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondaryZinc,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun HonorSettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HonorCardDark)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        content()
    }
}

@Composable
private fun HonorSettingDivider() {
    HorizontalDivider(
        color = HonorDividerDark,
        thickness = 0.7.dp,
        modifier = Modifier.padding(start = 40.dp)
    )
}

@Composable
private fun HonorChevronSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    valueText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimaryWhite,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryZinc,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valueText.isNotEmpty()) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryZinc
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondaryZinc,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HonorSwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .minimumInteractiveComponentSize()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimaryWhite,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimaryWhite,
                    fontWeight = FontWeight.Medium
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryZinc,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GalaxyAmber,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF48484A)
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
        CameraMode.MULTI_VIDEO -> Icons.Default.ViewCarousel
        CameraMode.SLOW_MO -> Icons.Default.SlowMotionVideo
        CameraMode.TIME_LAPSE -> Icons.Default.Timelapse
        CameraMode.UNDERWATER -> Icons.Default.Pool
        CameraMode.SUPER_MACRO -> Icons.Default.MacroOff
        CameraMode.HIGH_RES -> Icons.Default.HighQuality
        CameraMode.SCAN_DOCUMENT -> Icons.Default.DocumentScanner
    }
}
