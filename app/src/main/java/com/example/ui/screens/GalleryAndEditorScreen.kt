package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CropAspectPreset
import com.example.model.HonorAiGenPreset
import com.example.model.HonorAiGenUiState
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.MediaItem
import com.example.model.PhotoEditState
import com.example.model.PhotoFilterPreset
import com.example.ui.theme.AiCyan
import com.example.ui.theme.AiViolet
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.GalaxyAmber
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RecordCrimson
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryZinc
import com.example.viewmodel.CameraUiState
import com.example.viewmodel.CameraViewModel
import java.util.Locale

@Composable
fun GalleryScreen(
    state: CameraUiState,
    onSelectMedia: (MediaItem) -> Unit,
    onOpenEditor: (MediaItem) -> Unit,
    onTriggerAiAutoEditAndUpscale: (MediaItem) -> Unit,
    onSelectHonorAiPreset: (HonorAiGenPreset) -> Unit = {},
    onUpdateCustomHonorAiPrompt: (String) -> Unit = {},
    onDismissHonorAiSheet: () -> Unit = {},
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onImportSamplePhoto: () -> Unit,
    onBackToCamera: () -> Unit
) {
    BackHandler(onBack = onBackToCamera)

    val selected = state.selectedMediaItem
    var splitCompareEnabled by remember { mutableStateOf(false) }
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    var detailZoomMultiplier by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                IconButton(
                    onClick = onBackToCamera,
                    modifier = Modifier.testTag("gallery_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Camera",
                        tint = TextPrimaryWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gallery_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.mediaList.size} items • Pictures/Camera",
                        style = MaterialTheme.typography.labelSmall,
                        color = GalaxyAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedButton(
                onClick = onImportSamplePhoto,
                modifier = Modifier.testTag("import_sample_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = GalaxyAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sample Trio",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyAmber,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (state.mediaList.isEmpty() || selected == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_camera_hero_lens_1794590959493),
                    contentDescription = "Camera Gallery",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DividerSubtle, RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No Photos or Videos Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap 'Load Sample Trio' to generate an Original JPEG (~0.8 MB), a 14-Bit Linear RAW DNG (~7.2 MB), and an Honor MagicLM 2x AI Master (~5.4 MB) for live side-by-side comparison.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryZinc
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onBackToCamera,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GalaxyAmber,
                            contentColor = AmoledBlack
                        ),
                        modifier = Modifier.testTag("empty_gallery_return_camera")
                    ) {
                        Text("Return to Camera", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onImportSamplePhoto,
                        modifier = Modifier.testTag("empty_gallery_load_sample")
                    ) {
                        Text("Load Sample Trio", color = GalaxyAmber)
                    }
                }
            }
        } else {
            // Find matching ORIGINAL item for Before/After comparison
            val baseStem = remember(selected.displayName) {
                selected.displayName
                    .substringBefore("_AI_EDIT")
                    .substringBefore("_AI2x")
                    .substringBefore("_AI4x")
                    .substringBefore("_HONOR_AI")
                    .substringBefore("_RAW")
                    .substringBefore("_ORIG")
                    .substringBefore(".")
            }
            val pairedOriginalItem = remember(selected, state.mediaList) {
                state.mediaList.firstOrNull { item ->
                    !item.isVideo &&
                        item.variantBadge == "ORIGINAL" &&
                        item.displayName.startsWith(baseStem)
                } ?: state.mediaList.firstOrNull { !it.isVideo && it.variantBadge == "ORIGINAL" }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .pointerInput(splitCompareEnabled) {
                        if (splitCompareEnabled) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val w = size.width.coerceAtLeast(1)
                                splitFraction = (change.position.x / w.toFloat()).coerceIn(0.08f, 0.92f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selected.isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setVideoURI(selected.uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        update = { videoView ->
                            videoView.setVideoURI(selected.uri)
                            videoView.start()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    // Main Photo (or Right Side in Split Compare)
                    AsyncImage(
                        model = selected.uri,
                        contentDescription = selected.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .graphicsLayer {
                                scaleX = detailZoomMultiplier
                                scaleY = detailZoomMultiplier
                            }
                    )

                    // Left Side Overlay when Split Compare is active
                    if (splitCompareEnabled) {
                        val beforeUri = pairedOriginalItem?.uri ?: selected.uri
                        val isSameFileFallback = pairedOriginalItem == null || pairedOriginalItem.id == selected.id
                        val fallbackFlatFilter = remember(isSameFileFallback) {
                            if (isSameFileFallback) {
                                val cm = ColorMatrix().apply { setToSaturation(0.75f) }
                                ColorFilter.colorMatrix(cm)
                            } else {
                                null
                            }
                        }

                        AsyncImage(
                            model = beforeUri,
                            contentDescription = "Original Before Comparison",
                            contentScale = ContentScale.Fit,
                            colorFilter = fallbackFlatFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .graphicsLayer {
                                    scaleX = detailZoomMultiplier
                                    scaleY = detailZoomMultiplier
                                }
                                .drawWithContent {
                                    clipRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width * splitFraction,
                                        bottom = size.height
                                    ) {
                                        this@drawWithContent.drawContent()
                                    }
                                    val splitX = size.width * splitFraction
                                    drawLine(
                                        color = GalaxyAmber,
                                        start = androidx.compose.ui.geometry.Offset(splitX, 0f),
                                        end = androidx.compose.ui.geometry.Offset(splitX, size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                    drawCircle(
                                        color = GalaxyAmber,
                                        radius = 14.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(splitX, size.height * 0.5f)
                                    )
                                }
                        )
                    }
                }

                // File Info + ORIGINAL vs HONOR AI GEN / AI EDIT / RAW DNG Variant Badge
                val isAiUpscaled = selected.variantBadge.startsWith("AI") || selected.variantBadge.startsWith("HONOR AI")
                val isRawDng = selected.variantBadge == "RAW DNG"
                val badgeColor = when {
                    isAiUpscaled -> EmeraldOptimal
                    isRawDng -> AiCyan
                    else -> GalaxyAmber
                }
                val sizeDeltaLabel = remember(selected, pairedOriginalItem) {
                    val origBytes = pairedOriginalItem?.sizeBytes ?: 0L
                    if (origBytes > 0L && selected.id != pairedOriginalItem?.id && selected.sizeBytes > origBytes) {
                        val mult = selected.sizeBytes.toFloat() / origBytes.toFloat()
                        String.format(Locale.US, " • %.1f× Bigger than Original (%s)", mult, pairedOriginalItem?.formattedSize ?: "")
                    } else if (isRawDng) {
                        " • 14-Bit Linear Sensor DNG Master"
                    } else if (isAiUpscaled) {
                        " • Honor MagicLM Neural SR + 64³ 3D LUT"
                    } else {
                        " • Standard 8-Bit Native JPEG"
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianGlass)
                        .border(
                            width = 1.dp,
                            color = badgeColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selected.variantBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmoledBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = selected.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimaryWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selected.formattedResolution} • ${selected.formattedSize}$sizeDeltaLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor
                        )
                    }
                }

                // Bottom Overlay Controls: Split Compare (Before/After) + 4x Detail Zoom Loupe
                if (!selected.isVideo) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmoledBlack.copy(alpha = 0.82f))
                            .border(1.dp, DividerSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (splitCompareEnabled) GalaxyAmber else ObsidianElevated)
                                .clickable { splitCompareEnabled = !splitCompareEnabled }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("gallery_split_compare_toggle")
                        ) {
                            Text(
                                text = if (splitCompareEnabled) "◄ Split Compare ON ►" else "Compare vs Original",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (splitCompareEnabled) AmoledBlack else TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (detailZoomMultiplier > 1.05f) EmeraldOptimal else ObsidianElevated)
                                .clickable {
                                    detailZoomMultiplier = when {
                                        detailZoomMultiplier < 1.5f -> 2.5f
                                        detailZoomMultiplier < 3.2f -> 4.0f
                                        else -> 1.0f
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("gallery_detail_zoom_toggle")
                        ) {
                            Text(
                                text = if (detailZoomMultiplier <= 1.05f) "1x Fit (Tap for 4x Detail)" else "${String.format(Locale.US, "%.1fx", detailZoomMultiplier)} Pixel Zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (detailZoomMultiplier > 1.05f) AmoledBlack else TextPrimaryWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Filmstrip showing both ORIGINAL and AI UPSCALE files with labels
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.mediaList, key = { "${it.isVideo}_${it.id}" }) { item ->
                    val isCurrent = item.id == selected.id && item.isVideo == selected.isVideo
                    val isItemAi = item.variantBadge.startsWith("AI") || item.variantBadge.startsWith("HONOR AI")
                    val isItemRaw = item.variantBadge == "RAW DNG"
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = when {
                                    isCurrent -> GalaxyAmber
                                    isItemAi -> EmeraldOptimal.copy(alpha = 0.8f)
                                    isItemRaw -> AiCyan.copy(alpha = 0.8f)
                                    else -> DividerSubtle
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMedia(item) }
                            .testTag("gallery_thumb_${item.id}")
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Top-right MB size pill on each thumbnail
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(AmoledBlack.copy(alpha = 0.78f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = item.formattedSize,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isItemAi -> EmeraldOptimal
                                    isItemRaw -> AiCyan
                                    else -> GalaxyAmber
                                }
                            )
                        }
                        if (item.isVideo) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Video item",
                                tint = TextPrimaryWhite,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(24.dp)
                            )
                        }
                        // Label badge at bottom of each thumbnail ("ORIGINAL", "RAW DNG", "AI EDIT + 2x")
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(AmoledBlack.copy(alpha = 0.82f))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.variantBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isItemAi -> EmeraldOptimal
                                    isItemRaw -> AiCyan
                                    else -> TextPrimaryWhite
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (state.isUpscalingInProgress) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianElevated)
                        .border(1.dp, EmeraldOptimal, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = EmeraldOptimal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${state.upscaleStatusStage.ifBlank { "AI Auto-Edit & Upscaling..." }} (${state.upscaleProgressPercent}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!selected.isVideo) {
                    GalleryActionButton(
                        icon = Icons.Default.AutoAwesome,
                        label = "Honor AI Gen",
                        accent = EmeraldOptimal,
                        onClick = { onTriggerAiAutoEditAndUpscale(selected) },
                        testTag = "gallery_ai_auto_edit_button",
                        modifier = Modifier.weight(1f)
                    )
                    GalleryActionButton(
                        icon = Icons.Default.Edit,
                        label = "Edit & LUTs",
                        accent = GalaxyAmber,
                        onClick = { onOpenEditor(selected) },
                        testTag = "gallery_edit_button",
                        modifier = Modifier.weight(1f)
                    )
                }
                GalleryActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    accent = AiCyan,
                    onClick = onShareSelected,
                    testTag = "gallery_share_button",
                    modifier = Modifier.weight(1f)
                )
                GalleryActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    accent = RecordCrimson,
                    onClick = onDeleteSelected,
                    testTag = "gallery_delete_button",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (state.showHonorAiGenSheet && selected != null && !selected.isVideo) {
        HonorAiGenerationBottomSheet(
            state = state,
            selectedMedia = selected,
            onSelectPreset = onSelectHonorAiPreset,
            onUpdateCustomPrompt = onUpdateCustomHonorAiPrompt,
            onGenerate = { onTriggerAiAutoEditAndUpscale(selected) },
            onDismiss = onDismissHonorAiSheet
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HonorAiGenerationBottomSheet(
    state: CameraUiState,
    selectedMedia: MediaItem,
    onSelectPreset: (HonorAiGenPreset) -> Unit,
    onUpdateCustomPrompt: (String) -> Unit,
    onGenerate: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HONOR 600 AI PHOTO GENERATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldOptimal,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AIGC Detail Reconstruction & Studio Mastering (gemini-nano-banana-2.1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryZinc
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Honor AI Sheet",
                        tint = TextPrimaryWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preset Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HonorAiGenPreset.entries.forEach { preset ->
                    val isSelected = state.selectedHonorAiPreset == preset
                    Column(
                        modifier = Modifier
                            .width(175.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) EmeraldOptimal.copy(alpha = 0.18f) else ObsidianElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldOptimal else DividerSubtle,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectPreset(preset) }
                            .padding(10.dp)
                            .testTag("honor_ai_preset_${preset.name.lowercase()}")
                    ) {
                        Text(
                            text = preset.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) EmeraldOptimal else GalaxyAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimaryWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryZinc,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.customHonorAiPrompt,
                onValueChange = onUpdateCustomPrompt,
                label = { Text("Optional Custom AI Instruction (e.g. realistic brick & foliage detail)", color = TextSecondaryZinc) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("honor_ai_custom_prompt_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Render HonorAiGenUiState (Loading / Success / Error / Idle)
            when (val aiState = state.honorAiGenUiState) {
                is HonorAiGenUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, EmeraldOptimal, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = EmeraldOptimal)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiState.stageMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldOptimal,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reconstructing photorealistic details (${state.upscaleProgressPercent}%)...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryZinc
                        )
                    }
                }
                is HonorAiGenUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianElevated)
                            .border(1.dp, EmeraldOptimal, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GENERATED: ${aiState.presetTitle} (${aiState.bitmap.width}×${aiState.bitmap.height})",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldOptimal,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = aiState.bitmap.asImageBitmap(),
                            contentDescription = "Honor 600 AI Generated Result",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                is HonorAiGenUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(RecordCrimson.copy(alpha = 0.14f))
                            .border(1.dp, RecordCrimson, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Honor 600 AI Generation Notice",
                            style = MaterialTheme.typography.titleSmall,
                            color = RecordCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryWhite
                        )
                    }
                }
                is HonorAiGenUiState.Idle -> {
                    Text(
                        text = "Source: ${selectedMedia.displayName} (${selectedMedia.formattedResolution}) • Original file is always preserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryZinc
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onGenerate,
                enabled = state.honorAiGenUiState !is HonorAiGenUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldOptimal,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_honor_ai_gen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate with ${state.selectedHonorAiPreset.title}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GalleryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimaryWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false
        )
    }
}

private enum class EditorToolTab {
    LUTS,
    LOG_CURVE,
    ADJUST,
    CROP_STAMP
}

@Composable
fun PhotoEditorScreen(
    state: CameraUiState,
    viewModel: CameraViewModel,
    onBackToGallery: () -> Unit
) {
    BackHandler(onBack = onBackToGallery)

    val mediaItem = state.selectedMediaItem ?: return
    val editState: PhotoEditState = state.photoEditState

    var activeToolTab by remember { mutableStateOf(EditorToolTab.LUTS) }
    var selectedCategory by remember { mutableStateOf(LutCategory.ALL) }
    var sourceBitmap by remember(mediaItem.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(mediaItem.uri) {
        viewModel.loadPreviewBitmapForEditor(mediaItem) { bmp ->
            sourceBitmap = bmp
        }
    }

    val renderedPreview = remember(sourceBitmap, editState) {
        sourceBitmap?.let { viewModel.renderPreviewWithEdits(it, editState) }
    }

    val displayedLuts = remember(selectedCategory) {
        if (selectedCategory == LutCategory.ALL) {
            PhotoFilterPreset.entries
        } else {
            PhotoFilterPreset.entries.filter {
                it == PhotoFilterPreset.ORIGINAL || it.category == selectedCategory
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToGallery,
                    modifier = Modifier.testTag("editor_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Gallery",
                        tint = TextPrimaryWhite
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.editor_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryWhite
                    )
                    Text(
                        text = "${editState.filter.label} • ${editState.logProfile.shortCode} • ${editState.cropAspect.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GalaxyAmber
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.applyAiAutoEditInEditor() },
                    enabled = !state.isApplyingEditorAi,
                    modifier = Modifier.testTag("editor_ai_magic_button")
                ) {
                    if (state.isApplyingEditorAi) {
                        CircularProgressIndicator(
                            color = AiCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Auto-Edit",
                            tint = AiCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Auto",
                        style = MaterialTheme.typography.labelMedium,
                        color = AiCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { viewModel.resetPhotoEdits() },
                    modifier = Modifier.testTag("editor_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Edits",
                        tint = TextSecondaryZinc
                    )
                }

                Button(
                    onClick = { viewModel.saveEditedPhotoCopy() },
                    enabled = !state.isSavingEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GalaxyAmber,
                        contentColor = AmoledBlack
                    ),
                    modifier = Modifier.testTag("editor_save_button")
                ) {
                    if (state.isSavingEdit) {
                        CircularProgressIndicator(
                            color = AmoledBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Copy", fontWeight = FontWeight.Bold)
                }
            }
        }

        state.lastAiAutoEditRecipe?.let { recipe ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianElevated)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recipe.styleTitle}: ${recipe.summary}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldOptimal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (recipe.usedCloudGemini) "GEMINI AI" else "ON-DEVICE AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = AiCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (renderedPreview != null) {
                Image(
                    bitmap = renderedPreview.asImageBitmap(),
                    contentDescription = "Edited Photo Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                CircularProgressIndicator(color = GalaxyAmber)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (activeToolTab) {
                EditorToolTab.LUTS -> {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LutCategory.entries.forEach { cat ->
                                val isCat = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isCat) AiCyan else ObsidianElevated)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = cat.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCat) AmoledBlack else TextPrimaryWhite
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            displayedLuts.forEach { preset ->
                                val selected = editState.filter == preset
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) GalaxyAmber else ObsidianElevated)
                                        .clickable { viewModel.updateEditFilter(preset) }
                                        .minimumInteractiveComponentSize()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag("editor_filter_${preset.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterVintage,
                                        contentDescription = preset.label,
                                        tint = if (selected) AmoledBlack else GalaxyAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) AmoledBlack else TextPrimaryWhite
                                    )
                                    Text(
                                        text = preset.category.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) AmoledBlack.copy(alpha = 0.75f) else TextSecondaryZinc
                                    )
                                }
                            }
                        }
                    }
                }

                EditorToolTab.LOG_CURVE -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LogProfile.entries.forEach { logProfile ->
                            val selected = editState.logProfile == logProfile
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) AiCyan else ObsidianElevated)
                                    .clickable { viewModel.updateEditLogProfile(logProfile) }
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("editor_log_${logProfile.name.lowercase()}")
                            ) {
                                Text(
                                    text = "${logProfile.shortCode} (${logProfile.dynamicRangeStops})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) AmoledBlack else AiCyan
                                )
                                Text(
                                    text = logProfile.fullTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) AmoledBlack else TextPrimaryWhite
                                )
                            }
                        }
                    }
                }

                EditorToolTab.ADJUST -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CompactEditorSlider(
                            label = "BRIGHTNESS",
                            valueText = String.format(Locale.US, "%+.0f", editState.brightness),
                            value = editState.brightness,
                            range = -60f..60f,
                            onValueChange = viewModel::updateEditBrightness
                        )
                        CompactEditorSlider(
                            label = "CONTRAST",
                            valueText = String.format(Locale.US, "%.2fx", editState.contrast),
                            value = editState.contrast,
                            range = 0.6f..1.6f,
                            onValueChange = viewModel::updateEditContrast
                        )
                        CompactEditorSlider(
                            label = "SATURATION",
                            valueText = String.format(Locale.US, "%.2fx", editState.saturation),
                            value = editState.saturation,
                            range = 0.0f..2.0f,
                            onValueChange = viewModel::updateEditSaturation
                        )
                        CompactEditorSlider(
                            label = "WARMTH",
                            valueText = String.format(Locale.US, "%+.0f", editState.warmth),
                            value = editState.warmth,
                            range = -40f..40f,
                            onValueChange = viewModel::updateEditWarmth
                        )
                        CompactEditorSlider(
                            label = "VIGNETTE",
                            valueText = "${(editState.vignette * 100).toInt()}%",
                            value = editState.vignette,
                            range = 0f..1f,
                            onValueChange = viewModel::updateEditVignette
                        )
                    }
                }

                EditorToolTab.CROP_STAMP -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.rotateEditedPhoto90() },
                                modifier = Modifier.testTag("editor_rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Rotate90DegreesCcw,
                                    contentDescription = "Rotate 90°",
                                    tint = GalaxyAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate (${editState.rotationDegrees}°)", color = GalaxyAmber)
                            }

                            CropAspectPreset.entries.forEach { aspect ->
                                val isSel = editState.cropAspect == aspect
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSel) GalaxyAmber else ObsidianElevated)
                                        .clickable { viewModel.updateEditCropAspect(aspect) }
                                        .minimumInteractiveComponentSize()
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = aspect.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSel) AmoledBlack else TextPrimaryWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Timestamp Watermark",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryWhite
                                )
                                Text(
                                    text = "Add device model and date/time stamp to edited copy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryZinc
                                )
                            }
                            Switch(
                                checked = editState.showGalaxyWatermark,
                                onCheckedChange = { viewModel.toggleEditWatermark() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AmoledBlack,
                                    checkedTrackColor = GalaxyAmber
                                ),
                                modifier = Modifier.testTag("editor_watermark_switch")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditorTabPill(
                    icon = Icons.Default.AutoFixHigh,
                    label = "24 LUTS",
                    selected = activeToolTab == EditorToolTab.LUTS,
                    onClick = { activeToolTab = EditorToolTab.LUTS },
                    testTag = "editor_tab_filters"
                )
                EditorTabPill(
                    icon = Icons.Default.ColorLens,
                    label = "10 LOGS",
                    selected = activeToolTab == EditorToolTab.LOG_CURVE,
                    onClick = { activeToolTab = EditorToolTab.LOG_CURVE },
                    testTag = "editor_tab_logs"
                )
                EditorTabPill(
                    icon = Icons.Default.Tune,
                    label = "ADJUST",
                    selected = activeToolTab == EditorToolTab.ADJUST,
                    onClick = { activeToolTab = EditorToolTab.ADJUST },
                    testTag = "editor_tab_adjust"
                )
                EditorTabPill(
                    icon = Icons.Default.CropRotate,
                    label = "CROP",
                    selected = activeToolTab == EditorToolTab.CROP_STAMP,
                    onClick = { activeToolTab = EditorToolTab.CROP_STAMP },
                    testTag = "editor_tab_crop"
                )
            }
        }
    }
}

@Composable
private fun CompactEditorSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryZinc,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = GalaxyAmber,
                activeTrackColor = GalaxyAmber,
                inactiveTrackColor = DividerSubtle
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            color = EmeraldOptimal,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun EditorTabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GalaxyAmber else AmoledBlack.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) AmoledBlack else TextSecondaryZinc,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AmoledBlack else TextSecondaryZinc
        )
    }
}
