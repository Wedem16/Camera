package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.HardwareCapabilities
import com.example.model.ProCameraSettings
import com.example.model.ProParameterTab
import com.example.model.WhiteBalancePreset
import com.example.ui.theme.AiCyan
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GalaxyAmber
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryZinc

private val ISO_OPTIONS = listOf(null, 50, 100, 200, 400, 800, 1600, 3200)

private val SHUTTER_OPTIONS = listOf<Pair<String, Long?>>(
    "AUTO" to null,
    "1/4000" to 250_000L,
    "1/2000" to 500_000L,
    "1/1000" to 1_000_000L,
    "1/500" to 2_000_000L,
    "1/250" to 4_000_000L,
    "1/125" to 8_000_000L,
    "1/60" to 16_666_666L,
    "1/30" to 33_333_333L,
    "1/15" to 66_666_666L,
    "1/4" to 250_000_000L,
    "1s" to 1_000_000_000L
)

@Composable
fun ProControlsOverlay(
    proSettings: ProCameraSettings,
    activeTab: ProParameterTab,
    capabilities: HardwareCapabilities,
    histogramBins: List<Float>,
    averageLuminance: Float,
    onSelectTab: (ProParameterTab) -> Unit,
    onUpdateIso: (Int?) -> Unit,
    onUpdateShutter: (Long?) -> Unit,
    onUpdateWhiteBalance: (WhiteBalancePreset, Int) -> Unit,
    onUpdateFocus: (Float?) -> Unit,
    onUpdateExposureIndex: (Int) -> Unit,
    onResetPro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Live Luminance Histogram + Sensor Status + Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveHistogramBadge(
                bins = histogramBins,
                averageLuminance = averageLuminance
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianGlass)
                    .border(1.dp, DividerSubtle, RoundedCornerShape(20.dp))
                    .clickable(onClick = onResetPro)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("pro_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.pro_reset_label),
                    tint = GalaxyAmber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.pro_reset_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyAmber
                )
            }
        }

        // Expandable Parameter Wheel/Slider Strip
        AnimatedVisibility(
            visible = activeTab != ProParameterTab.NONE,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianGlass)
                    .border(1.dp, DividerSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (activeTab) {
                    ProParameterTab.ISO -> IsoSelectorRow(
                        selectedIso = proSettings.iso,
                        onSelectIso = onUpdateIso
                    )
                    ProParameterTab.SHUTTER -> ShutterSelectorRow(
                        selectedShutterNs = proSettings.shutterSpeedNs,
                        onSelectShutter = onUpdateShutter
                    )
                    ProParameterTab.EV -> ExposureSliderRow(
                        currentIndex = proSettings.exposureCompensationIndex,
                        minIndex = capabilities.minExposureIndex,
                        maxIndex = capabilities.maxExposureIndex,
                        stepEv = capabilities.exposureStepEv,
                        onIndexChanged = onUpdateExposureIndex
                    )
                    ProParameterTab.FOCUS -> FocusSliderRow(
                        focusDistance = proSettings.manualFocusDistance,
                        onFocusChanged = onUpdateFocus
                    )
                    ProParameterTab.WB -> WhiteBalanceRow(
                        selectedPreset = proSettings.whiteBalance,
                        customKelvin = proSettings.customKelvin,
                        onUpdateWb = onUpdateWhiteBalance
                    )
                    ProParameterTab.NONE -> Unit
                }
            }
        }

        // 5-Column Pro Parameter Telemetry Deck (ISO, SPEED, EV, FOCUS, WB)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ObsidianGlass)
                .border(1.dp, DividerSubtle, RoundedCornerShape(18.dp))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val evFormatted = String.format(
                "%+.1f",
                proSettings.exposureCompensationIndex * capabilities.exposureStepEv
            )

            ProMetricButton(
                title = "ISO",
                value = proSettings.formattedIso,
                isActive = activeTab == ProParameterTab.ISO,
                isManual = proSettings.iso != null,
                onClick = { onSelectTab(ProParameterTab.ISO) },
                testTag = "pro_tab_iso"
            )
            ProMetricButton(
                title = "SPEED",
                value = proSettings.formattedShutter,
                isActive = activeTab == ProParameterTab.SHUTTER,
                isManual = proSettings.shutterSpeedNs != null,
                onClick = { onSelectTab(ProParameterTab.SHUTTER) },
                testTag = "pro_tab_shutter"
            )
            ProMetricButton(
                title = "EV",
                value = evFormatted,
                isActive = activeTab == ProParameterTab.EV,
                isManual = proSettings.exposureCompensationIndex != 0,
                onClick = { onSelectTab(ProParameterTab.EV) },
                testTag = "pro_tab_ev"
            )
            ProMetricButton(
                title = "FOCUS",
                value = proSettings.formattedFocus,
                isActive = activeTab == ProParameterTab.FOCUS,
                isManual = proSettings.manualFocusDistance != null,
                onClick = { onSelectTab(ProParameterTab.FOCUS) },
                testTag = "pro_tab_focus"
            )
            ProMetricButton(
                title = "WB",
                value = proSettings.formattedWb,
                isActive = activeTab == ProParameterTab.WB,
                isManual = proSettings.whiteBalance != WhiteBalancePreset.AUTO,
                onClick = { onSelectTab(ProParameterTab.WB) },
                testTag = "pro_tab_wb"
            )
        }
    }
}

@Composable
private fun LiveHistogramBadge(
    bins: List<Float>,
    averageLuminance: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianGlass)
            .border(1.dp, DividerSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width(72.dp)
                .height(24.dp)
        ) {
            val count = bins.size.coerceAtLeast(1)
            val barWidth = size.width / count
            bins.forEachIndexed { idx, norm ->
                val barHeight = (norm.coerceIn(0.08f, 1f)) * size.height
                val color = when {
                    idx < 3 -> AiCyan
                    idx > 12 -> GalaxyAmber
                    else -> TextPrimaryWhite.copy(alpha = 0.85f)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(idx * barWidth + 1f, size.height - barHeight),
                    size = Size((barWidth - 2f).coerceAtLeast(2f), barHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "LUMA",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryZinc,
                fontSize = 9.sp
            )
            Text(
                text = "${averageLuminance.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldOptimal
            )
        }
    }
}

@Composable
private fun ProMetricButton(
    title: String,
    value: String,
    isActive: Boolean,
    isManual: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) ObsidianElevated else Color.Transparent)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) GalaxyAmber else TextSecondaryZinc
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isActive || isManual -> GalaxyAmber
                else -> TextPrimaryWhite
            }
        )
    }
}

@Composable
private fun IsoSelectorRow(
    selectedIso: Int?,
    onSelectIso: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ISO_OPTIONS.forEach { isoVal ->
            val isSelected = selectedIso == isoVal
            val label = isoVal?.toString() ?: "AUTO"
            DialChip(
                label = label,
                selected = isSelected,
                onClick = { onSelectIso(isoVal) }
            )
        }
    }
}

@Composable
private fun ShutterSelectorRow(
    selectedShutterNs: Long?,
    onSelectShutter: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SHUTTER_OPTIONS.forEach { (label, nsVal) ->
            val isSelected = selectedShutterNs == nsVal
            DialChip(
                label = label,
                selected = isSelected,
                onClick = { onSelectShutter(nsVal) }
            )
        }
    }
}

@Composable
private fun ExposureSliderRow(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    stepEv: Float,
    onIndexChanged: (Int) -> Unit
) {
    val safeMin = if (minIndex < maxIndex) minIndex else -8
    val safeMax = if (maxIndex > minIndex) maxIndex else 8
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EXPOSURE COMPENSATION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryZinc
            )
            Text(
                text = String.format("%+.2f EV", currentIndex * stepEv),
                style = MaterialTheme.typography.labelMedium,
                color = GalaxyAmber
            )
        }
        Slider(
            value = currentIndex.toFloat().coerceIn(safeMin.toFloat(), safeMax.toFloat()),
            onValueChange = { onIndexChanged(it.toInt()) },
            valueRange = safeMin.toFloat()..safeMax.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = GalaxyAmber,
                activeTrackColor = GalaxyAmber,
                inactiveTrackColor = DividerSubtle
            ),
            modifier = Modifier.testTag("pro_ev_slider")
        )
    }
}

@Composable
private fun FocusSliderRow(
    focusDistance: Float?,
    onFocusChanged: (Float?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialChip(
                    label = "AF AUTO",
                    selected = focusDistance == null,
                    onClick = { onFocusChanged(null) }
                )
                DialChip(
                    label = "MANUAL",
                    selected = focusDistance != null,
                    onClick = { if (focusDistance == null) onFocusChanged(0.35f) }
                )
            }
            Text(
                text = if (focusDistance == null) "Continuous AF" else if (focusDistance < 0.15f) "Infinity ∞" else "Macro ${String.format("%.2f", focusDistance)}",
                style = MaterialTheme.typography.labelMedium,
                color = GalaxyAmber
            )
        }
        if (focusDistance != null) {
            Slider(
                value = focusDistance,
                onValueChange = { onFocusChanged(it) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldOptimal,
                    activeTrackColor = EmeraldOptimal,
                    inactiveTrackColor = DividerSubtle
                ),
                modifier = Modifier.testTag("pro_focus_slider")
            )
        }
    }
}

@Composable
private fun WhiteBalanceRow(
    selectedPreset: WhiteBalancePreset,
    customKelvin: Int,
    onUpdateWb: (WhiteBalancePreset, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WhiteBalancePreset.entries.forEach { preset ->
                DialChip(
                    label = preset.label,
                    selected = selectedPreset == preset,
                    onClick = { onUpdateWb(preset, customKelvin) }
                )
            }
        }
        if (selectedPreset == WhiteBalancePreset.MANUAL_KELVIN) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "COLOR TEMPERATURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryZinc
                )
                Text(
                    text = "${customKelvin}K",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyAmber
                )
            }
            Slider(
                value = customKelvin.toFloat(),
                onValueChange = { onUpdateWb(WhiteBalancePreset.MANUAL_KELVIN, it.toInt()) },
                valueRange = 2300f..10000f,
                colors = SliderDefaults.colors(
                    thumbColor = GalaxyAmber,
                    activeTrackColor = GalaxyAmber,
                    inactiveTrackColor = DividerSubtle
                ),
                modifier = Modifier.testTag("pro_wb_kelvin_slider")
            )
        }
    }
}

@Composable
private fun DialChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) GalaxyAmber else ObsidianElevated)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.Black else TextPrimaryWhite
        )
    }
}
