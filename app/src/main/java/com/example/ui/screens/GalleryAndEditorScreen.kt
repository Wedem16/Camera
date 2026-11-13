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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CropAspectPreset
import com.example.model.LogProfile
import com.example.model.LutCategory
import com.example.model.MediaItem
import com.example.model.PhotoEditState
import com.example.model.PhotoFilterPreset
import com.example.ui.theme.AiCyan
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

@Composable
fun GalleryScreen(
    state: CameraUiState,
    onSelectMedia: (MediaItem) -> Unit,
    onOpenEditor: (MediaItem) -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onImportSamplePhoto: () -> Unit,
    onBackToCamera: () -> Unit
) {
    BackHandler(onBack = onBackToCamera)

    val selected = state.selectedMediaItem

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
                    onClick = onBackToCamera,
                    modifier = Modifier.testTag("gallery_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Camera",
                        tint = TextPrimaryWhite
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.gallery_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimaryWhite
                    )
                    Text(
                        text = "${state.mediaList.size} items in Android MediaStore",
                        style = MaterialTheme.typography.labelSmall,
                        color = GalaxyAmber
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
                    text = "4K Sample",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyAmber
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
                    contentDescription = "Galaxy Studio",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, DividerSubtle, RoundedCornerShape(24.dp))
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Your Galaxy 4K Studio Gallery is Empty",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimaryWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Capture 4K photos or 1000 Mbps 4K videos in the viewfinder, or load a 4K Studio Sample Shot to test the 24 Film/Travel LUTs & 10 Cinema Log curves.",
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
                        Text("Load 4K Sample", color = GalaxyAmber)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
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
                    AsyncImage(
                        model = selected.uri,
                        contentDescription = selected.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianGlass)
                        .border(1.dp, DividerSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = selected.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimaryWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (selected.isVideo) "VIDEO MP4" else "PHOTO 4K JPEG"} • ${selected.formattedResolution} • ${selected.formattedSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyAmber
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.mediaList, key = { "${it.isVideo}_${it.id}" }) { item ->
                    val isCurrent = item.id == selected.id && item.isVideo == selected.isVideo
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) GalaxyAmber else DividerSubtle,
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
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!selected.isVideo) {
                    GalleryActionButton(
                        icon = Icons.Default.Edit,
                        label = "Edit & LUTs",
                        accent = GalaxyAmber,
                        onClick = { onOpenEditor(selected) },
                        testTag = "gallery_edit_button"
                    )
                }
                GalleryActionButton(
                    icon = Icons.Default.Share,
                    label = "Share",
                    accent = AiCyan,
                    onClick = onShareSelected,
                    testTag = "gallery_share_button"
                )
                GalleryActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    accent = RecordCrimson,
                    onClick = onDeleteSelected,
                    testTag = "gallery_delete_button"
                )
            }
        }
    }
}

@Composable
private fun GalleryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimaryWhite
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    Text("Save 4K", fontWeight = FontWeight.Bold)
                }
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
                            valueText = String.format("%+.0f", editState.brightness),
                            value = editState.brightness,
                            range = -60f..60f,
                            onValueChange = viewModel::updateEditBrightness
                        )
                        CompactEditorSlider(
                            label = "CONTRAST",
                            valueText = String.format("%.2fx", editState.contrast),
                            value = editState.contrast,
                            range = 0.6f..1.6f,
                            onValueChange = viewModel::updateEditContrast
                        )
                        CompactEditorSlider(
                            label = "SATURATION",
                            valueText = String.format("%.2fx", editState.saturation),
                            value = editState.saturation,
                            range = 0.0f..2.0f,
                            onValueChange = viewModel::updateEditSaturation
                        )
                        CompactEditorSlider(
                            label = "WARMTH",
                            valueText = String.format("%+.0f", editState.warmth),
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
                                    text = "Galaxy 4K EXIF Watermark",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimaryWhite
                                )
                                Text(
                                    text = "Render signature bottom metadata bar with LUT & Log",
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
