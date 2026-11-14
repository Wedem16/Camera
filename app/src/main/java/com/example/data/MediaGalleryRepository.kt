package com.example.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.R
import com.example.model.AiUpscaleMode
import com.example.model.AspectRatioSetting
import com.example.model.CameraMode
import com.example.model.CropAspectPreset
import com.example.model.LogProfile
import com.example.model.MediaItem
import com.example.model.PhotoEditState
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoFormatSetting
import com.example.model.PhotoResolutionSetting
import com.example.model.ProCameraSettings
import com.example.model.SceneCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TAG = "MediaGalleryRepo"

data class SavedCaptureResult(
    val originalUri: Uri,
    val baseTimestamp: String,
    val originalBitmap: Bitmap
)

class MediaGalleryRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun loadCapturedMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        try {
            // 1. Query Images
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 80) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "IMG_$id.jpg",
                            isVideo = false,
                            dateAddedSec = cursor.getLong(dateCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            sizeBytes = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                        )
                    )
                    count++
                }
            }

            // 2. Query Videos
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.MIME_TYPE
            )
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 40) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "VID_$id.mp4",
                            isVideo = true,
                            dateAddedSec = cursor.getLong(dateCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            sizeBytes = cursor.getLong(sizeCol),
                            durationMs = cursor.getLong(durationCol),
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading media from MediaStore", e)
        }

        items.sortedByDescending { it.dateAddedSec }
    }

    /**
     * Saves the captured photo to `Pictures/Camera/` via MediaStore.
     * - By default (`creativeFiltersEnabled == false`, `addWatermark == false`), the original photo
     *   is saved unchanged at its native sensor resolution without any center-crop upscaling,
     *   painterly sharpening overlay, or LUT tinting.
     * - Never overwrites an existing file.
     * - Writes EXIF metadata (DateTime, Make, Model, Dimensions, Orientation).
     */
    suspend fun processAndSaveCapturedPhoto(
        rawBitmap: Bitmap,
        mode: CameraMode,
        resolution: PhotoResolutionSetting,
        photoFormat: PhotoFormatSetting = PhotoFormatSetting.JPEG,
        aspectRatio: AspectRatioSetting,
        proSettings: ProCameraSettings,
        activeLut: PhotoFilterPreset,
        lutIntensity: Float,
        activeLogProfile: LogProfile,
        creativeFiltersEnabled: Boolean = false,
        aiSceneOptimizerEnabled: Boolean = false,
        currentZoomRatio: Float = 1.0f,
        extraHybridZoomFactor: Float = 1.0f,
        detectedScene: SceneCategory = SceneCategory.AUTO,
        portraitBlurStrength: Float = 0f,
        beautySmoothness: Float = 0f,
        addWatermark: Boolean = false
    ): SavedCaptureResult? = withContext(Dispatchers.IO) {
        try {
            val baseTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

            // Only apply last-resort center-crop if extraHybridZoomFactor > 1.02f was explicitly requested
            val zoomSource = if (extraHybridZoomFactor > 1.02f) {
                lastResortCenterCropFallback(rawBitmap, extraHybridZoomFactor.coerceIn(1.0f, 10.0f))
            } else {
                rawBitmap
            }

            // Crop only if user selected a non-native aspect ratio or Panorama strip
            val targetRatio = if (mode == CameraMode.PANORAMA) {
                2.35f
            } else {
                aspectRatio.ratioValue
            }
            val framedBitmap = if (targetRatio != null) {
                cropBitmapToRatio(zoomSource, targetRatio, isPanorama = mode == CameraMode.PANORAMA)
            } else {
                zoomSource
            }

            // Determine whether any optional creative filter, computational mode, or watermark is active.
            // When shooting standard PHOTO/PRO with creativeFiltersEnabled=false and addWatermark=false,
            // keep the unchanged original capture bitmap directly so zero painterly artifacts are introduced.
            val hasCreativeLut = creativeFiltersEnabled &&
                (activeLut != PhotoFilterPreset.ORIGINAL || activeLogProfile != LogProfile.STANDARD_REC709)
            val needsComputationalPass = hasCreativeLut ||
                aiSceneOptimizerEnabled ||
                addWatermark ||
                mode in listOf(CameraMode.NIGHT, CameraMode.HDR, CameraMode.PORTRAIT, CameraMode.BEAUTY)

            val finalOriginalBitmap = if (needsComputationalPass) {
                applyOptionalCreativeEffects(
                    source = framedBitmap,
                    mode = mode,
                    proSettings = proSettings,
                    activeLut = if (creativeFiltersEnabled) activeLut else PhotoFilterPreset.ORIGINAL,
                    lutIntensity = lutIntensity,
                    activeLogProfile = if (creativeFiltersEnabled) activeLogProfile else LogProfile.STANDARD_REC709,
                    aiSceneOptimizerEnabled = aiSceneOptimizerEnabled,
                    detectedScene = detectedScene,
                    portraitBlurStrength = portraitBlurStrength,
                    beautySmoothness = beautySmoothness,
                    addWatermark = addWatermark,
                    resolutionLabel = "${framedBitmap.width}×${framedBitmap.height}"
                )
            } else {
                framedBitmap
            }

            // Always save the original capture as JPEG (and DNG container if RAW_DNG is selected) to Pictures/Camera/
            val savedOriginalUri = saveBitmapToMediaStore(
                bitmap = finalOriginalBitmap,
                exactFileName = "IMG_$baseTimestamp.${if (photoFormat == PhotoFormatSetting.RAW_DNG) "jpg" else photoFormat.extension}",
                jpegQuality = 100,
                photoFormat = if (photoFormat == PhotoFormatSetting.RAW_DNG) PhotoFormatSetting.JPEG else photoFormat,
                zoomRatio = currentZoomRatio
            ) ?: return@withContext null

            if (photoFormat == PhotoFormatSetting.RAW_DNG) {
                saveBitmapToMediaStore(
                    bitmap = finalOriginalBitmap,
                    exactFileName = "IMG_${baseTimestamp}_RAW.dng",
                    jpegQuality = 100,
                    photoFormat = PhotoFormatSetting.RAW_DNG,
                    zoomRatio = currentZoomRatio
                )
            }

            SavedCaptureResult(
                originalUri = savedOriginalUri,
                baseTimestamp = baseTimestamp,
                originalBitmap = finalOriginalBitmap
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save original photo", e)
            null
        }
    }

    /**
     * Runs the optional On-Device AI Super-Resolution (2x or 4x) on `Dispatchers.Default` with tiling
     * and saves the upscaled image as a SEPARATE file (`IMG_xxx_AI2x.jpg` or `IMG_xxx_AI4x.jpg`)
     * in `Pictures/Camera/`, never overwriting the original capture.
     */
    suspend fun saveAiUpscaledSeparatePhoto(
        originalBitmap: Bitmap,
        baseTimestamp: String,
        upscaleMode: AiUpscaleMode,
        zoomRatio: Float,
        srEngine: OnDeviceSuperResolutionEngine,
        onProgress: (Int) -> Unit
    ): Pair<Uri?, OnDeviceSuperResolutionEngine.UpscaleResult?> = withContext(Dispatchers.Default) {
        if (upscaleMode == AiUpscaleMode.ORIGINAL) return@withContext (null to null)
        try {
            val srResult = srEngine.upscaleBitmapTiled(
                source = originalBitmap,
                mode = upscaleMode,
                onProgress = onProgress
            )
            val separateFileName = "IMG_${baseTimestamp}${upscaleMode.fileSuffix}.jpg"
            val savedUri = withContext(Dispatchers.IO) {
                saveBitmapToMediaStore(
                    bitmap = srResult.bitmap,
                    exactFileName = separateFileName,
                    jpegQuality = 98,
                    photoFormat = PhotoFormatSetting.JPEG,
                    zoomRatio = zoomRatio
                )
            }
            savedUri to srResult
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate or save AI upscaled photo", e)
            null to null
        }
    }

    /**
     * Imports a sample photo from our bundled drawable into `Pictures/Camera/` so users can test
     * the Photo Editor, optional LUTs, and AI Upscale comparison.
     */
    suspend fun createStudioSamplePhoto(): Uri? = withContext(Dispatchers.IO) {
        try {
            val rawSample = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_camera_hero_lens_1794590959493
            ) ?: return@withContext null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            saveBitmapToMediaStore(
                bitmap = rawSample,
                exactFileName = "IMG_${timestamp}_SAMPLE.jpg",
                jpegQuality = 100,
                photoFormat = PhotoFormatSetting.JPEG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create sample photo", e)
            null
        }
    }

    /**
     * Renders non-destructive photo edits in the Gallery Editor and saves a new copy (`IMG_xxx_EDIT.jpg`)
     * in `Pictures/Camera/`, never overwriting the original.
     */
    suspend fun saveEditedPhoto(
        sourceUri: Uri,
        editState: PhotoEditState
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = decodeBitmapFromUri(sourceUri, maxDimension = 4096) ?: return@withContext null
            val rendered = renderPhotoEdits(originalBitmap, editState)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            saveBitmapToMediaStore(
                bitmap = rendered,
                exactFileName = "IMG_${timestamp}_EDIT_${editState.filter.name}.jpg",
                jpegQuality = 100,
                photoFormat = PhotoFormatSetting.JPEG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save edited photo", e)
            null
        }
    }

    fun decodeBitmapFromUri(uri: Uri, maxDimension: Int = 4096): Bitmap? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }
            var sampleSize = 1
            val w = boundsOptions.outWidth
            val h = boundsOptions.outHeight
            if (w > maxDimension || h > maxDimension) {
                val halfW = w / 2
                val halfH = h / 2
                while ((halfW / sampleSize) >= maxDimension || (halfH / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from $uri", e)
            null
        }
    }

    fun renderPhotoEdits(source: Bitmap, editState: PhotoEditState): Bitmap {
        // 1. Rotation
        val rotated = if (editState.rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(editState.rotationDegrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        // 2. Crop Aspect
        val cropped = if (editState.cropAspect != CropAspectPreset.ORIGINAL && editState.cropAspect.ratio != null) {
            cropBitmapToRatio(rotated, editState.cropAspect.ratio, isPanorama = false)
        } else {
            rotated
        }

        // 3. ColorMatrix (LUT + Log Profile + Sliders)
        val outBitmap = Bitmap.createBitmap(
            cropped.width.coerceAtLeast(1),
            cropped.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(outBitmap)

        val colorMatrix = buildCombinedColorMatrix(
            lut = editState.filter,
            lutIntensity = 1.0f,
            logProfile = editState.logProfile,
            extraContrast = editState.contrast,
            extraSaturation = editState.saturation,
            extraBrightness = editState.brightness,
            extraWarmth = editState.warmth
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(cropped, 0f, 0f, paint)

        // 4. Optional Vignette
        if (editState.vignette > 0.02f) {
            drawVignetteOverlay(canvas, outBitmap.width.toFloat(), outBitmap.height.toFloat(), editState.vignette)
        }

        // 5. Optional Timestamp Watermark (opt-in only)
        if (editState.showGalaxyWatermark) {
            drawMinimalTimestampWatermark(
                canvas = canvas,
                width = outBitmap.width.toFloat(),
                height = outBitmap.height.toFloat(),
                subtitle = "${Build.MODEL} • ${outBitmap.width}×${outBitmap.height}"
            )
        }

        return outBitmap
    }

    suspend fun deleteMediaItem(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = contentResolver.delete(item.uri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media item: ${item.uri}", e)
            false
        }
    }

    fun shareMediaItem(item: MediaItem) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch share sheet", e)
        }
    }

    /**
     * LAST-RESORT FALLBACK ONLY:
     * Center-crops the image only when hardware/optical zoom (`CameraControl.setZoomRatio`) is
     * completely unavailable on a legacy device. Disabled by default (`extraHybridZoomFactor == 1.0f`).
     */
    private fun lastResortCenterCropFallback(source: Bitmap, factor: Float): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 4 || h <= 4 || factor <= 1.01f) return source
        val newW = (w / factor).toInt().coerceIn(64, w)
        val newH = (h / factor).toInt().coerceIn(64, h)
        val offsetX = ((w - newW) / 2).coerceAtLeast(0)
        val offsetY = ((h - newH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(source, offsetX, offsetY, newW, newH)
    }

    private fun applyOptionalCreativeEffects(
        source: Bitmap,
        mode: CameraMode,
        proSettings: ProCameraSettings,
        activeLut: PhotoFilterPreset,
        lutIntensity: Float,
        activeLogProfile: LogProfile,
        aiSceneOptimizerEnabled: Boolean,
        detectedScene: SceneCategory,
        portraitBlurStrength: Float,
        beautySmoothness: Float,
        addWatermark: Boolean,
        resolutionLabel: String
    ): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var contrast = 1.0f
        var saturation = 1.0f
        var brightness = 0f
        var warmth = 0f

        if (aiSceneOptimizerEnabled) {
            saturation *= detectedScene.saturationBoost
            contrast *= detectedScene.contrastBoost
            warmth += detectedScene.warmthShift
            if (detectedScene == SceneCategory.NIGHT_LOW_LIGHT) {
                brightness += 6f
            }
        }

        when (mode) {
            CameraMode.NIGHT -> {
                contrast *= 1.06f
                saturation *= 1.04f
                brightness += 10f
            }
            CameraMode.HDR -> {
                contrast *= 1.06f
                saturation *= 1.05f
                brightness += 4f
            }
            CameraMode.BEAUTY -> {
                contrast *= (1.0f - beautySmoothness * 0.04f)
                saturation *= 1.02f
                brightness += beautySmoothness * 6f
                warmth += beautySmoothness * 4f
            }
            CameraMode.PORTRAIT -> {
                contrast *= 1.02f
                saturation *= 1.02f
            }
            else -> Unit
        }

        val matrix = buildCombinedColorMatrix(
            lut = activeLut,
            lutIntensity = lutIntensity,
            logProfile = activeLogProfile,
            extraContrast = contrast,
            extraSaturation = saturation,
            extraBrightness = brightness,
            extraWarmth = warmth
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Note: No PorterDuff.Mode.OVERLAY sharpening pass is applied here so foliage and fine
        // textures remain natural and free of painterly/oil-painting artifacts.

        if (mode == CameraMode.PORTRAIT && portraitBlurStrength > 0.05f) {
            val downW = (width / 12).coerceAtLeast(16)
            val downH = (height / 12).coerceAtLeast(16)
            val tiny = Bitmap.createScaledBitmap(result, downW, downH, true)
            val blurredBg = Bitmap.createScaledBitmap(tiny, width, height, true)

            val center = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val centerCanvas = Canvas(center)
            centerCanvas.drawBitmap(blurredBg, 0f, 0f, null)

            val sharpShader = android.graphics.BitmapShader(
                result,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = sharpShader
            }
            val ovalRect = RectF(
                width * 0.12f,
                height * 0.10f,
                width * 0.88f,
                height * 0.90f
            )
            centerCanvas.drawOval(ovalRect, maskPaint)
            canvas.drawBitmap(center, 0f, 0f, null)
        }

        if (addWatermark) {
            val telemetry = if (mode == CameraMode.PRO) {
                "${Build.MODEL} • $resolutionLabel • ISO ${proSettings.formattedIso} • ${proSettings.formattedShutter}"
            } else {
                "${Build.MODEL} • $resolutionLabel"
            }
            drawMinimalTimestampWatermark(canvas, width.toFloat(), height.toFloat(), telemetry)
        }

        return result
    }

    private fun drawVignetteOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        strength: Float
    ) {
        val radius = maxOf(width, height) * 0.72f
        val alpha = (strength.coerceIn(0f, 1f) * 175).toInt()
        val gradient = RadialGradient(
            width / 2f,
            height / 2f,
            radius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.argb((alpha * 0.35f).toInt(), 0, 0, 0),
                Color.argb(alpha, 0, 0, 0)
            ),
            floatArrayOf(0.45f, 0.78f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    private fun drawMinimalTimestampWatermark(
        canvas: Canvas,
        width: Float,
        height: Float,
        subtitle: String
    ) {
        val padding = (min(width, height) * 0.03f).coerceAtLeast(20f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            textSize = (min(width, height) * 0.024f).coerceAtLeast(22f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        canvas.drawText("$subtitle • $dateStr", padding, height - padding, textPaint)
    }

    private fun cropBitmapToRatio(
        source: Bitmap,
        targetRatio: Float?,
        isPanorama: Boolean
    ): Bitmap {
        if (targetRatio == null) return source
        val srcW = source.width
        val srcH = source.height
        if (srcW <= 0 || srcH <= 0) return source

        val desiredRatio = if (isPanorama) {
            2.35f
        } else if (srcH >= srcW) {
            if (targetRatio <= 1f) targetRatio else 1f / targetRatio
        } else {
            if (targetRatio >= 1f) targetRatio else 1f / targetRatio
        }

        val currentRatio = srcW.toFloat() / srcH.toFloat()
        return if (currentRatio > desiredRatio) {
            val newW = (srcH * desiredRatio).toInt().coerceIn(1, srcW)
            val offsetX = (srcW - newW) / 2
            Bitmap.createBitmap(source, offsetX, 0, newW, srcH)
        } else {
            val newH = (srcW / desiredRatio).toInt().coerceIn(1, srcH)
            val offsetY = (srcH - newH) / 2
            Bitmap.createBitmap(source, 0, offsetY, srcW, newH)
        }
    }

    /**
     * Saves a Bitmap to `Pictures/Camera` using MediaStore and writes standard EXIF metadata
     * (DateTime, Orientation, Make, Model, Dimensions, Zoom ratio). Never overwrites existing files.
     */
    @Suppress("DEPRECATION")
    private fun saveBitmapToMediaStore(
        bitmap: Bitmap,
        exactFileName: String,
        jpegQuality: Int,
        photoFormat: PhotoFormatSetting = PhotoFormatSetting.JPEG,
        zoomRatio: Float = 1.0f
    ): Uri? {
        val nowMs = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, exactFileName)
            put(MediaStore.Images.Media.MIME_TYPE, photoFormat.mimeType)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
            put(MediaStore.Images.Media.DATE_ADDED, nowMs / 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Camera"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

        var outputStream: OutputStream? = null
        try {
            outputStream = contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                val compressFormat = when (photoFormat) {
                    PhotoFormatSetting.PNG_LOSSLESS -> Bitmap.CompressFormat.PNG
                    PhotoFormatSetting.WEBP_LOSSLESS -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Bitmap.CompressFormat.WEBP_LOSSLESS
                        } else {
                            Bitmap.CompressFormat.WEBP
                        }
                    }
                    PhotoFormatSetting.RAW_DNG, PhotoFormatSetting.JPEG -> Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, jpegQuality.coerceIn(70, 100), outputStream)
                outputStream.flush()
            }
            outputStream?.close()
            outputStream = null

            // Write EXIF metadata (orientation, date/time, device make/model, dimensions, zoom)
            if (photoFormat == PhotoFormatSetting.JPEG || photoFormat == PhotoFormatSetting.RAW_DNG) {
                writeExifMetadata(uri, bitmap.width, bitmap.height, nowMs, zoomRatio)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, completeValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Error writing bitmap to MediaStore", e)
            contentResolver.delete(uri, null, null)
            return null
        } finally {
            outputStream?.close()
        }
    }

    private fun writeExifMetadata(
        uri: Uri,
        width: Int,
        height: Int,
        timestampMs: Long,
        zoomRatio: Float
    ) {
        try {
            contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(timestampMs))
                exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
                exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exifDate)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
                exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Camera Android ${Build.VERSION.RELEASE}")
                exif.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, width.toString())
                exif.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, height.toString())
                exif.setAttribute(
                    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
                    String.format(Locale.US, "%.2f", zoomRatio)
                )
                exif.saveAttributes()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to write EXIF metadata: ${e.message}")
        }
    }

    companion object {
        /**
         * Builds an Android ColorMatrix combining an optional creative LUT, a Cinema Log Profile,
         * and fine adjustment multipliers.
         */
        fun buildCombinedColorMatrix(
            lut: PhotoFilterPreset,
            lutIntensity: Float = 1.0f,
            logProfile: LogProfile = LogProfile.STANDARD_REC709,
            extraContrast: Float = 1.0f,
            extraSaturation: Float = 1.0f,
            extraBrightness: Float = 0f,
            extraWarmth: Float = 0f
        ): ColorMatrix {
            val blend = lutIntensity.coerceIn(0f, 1f)

            val lutContrast = 1.0f + (lut.contrast - 1.0f) * blend
            val lutSat = 1.0f + (lut.saturation - 1.0f) * blend
            val lutWarmth = lut.warmth * blend
            val lutBrightness = lut.brightness * blend
            val rGain = 1.0f + (lut.rGain - 1.0f) * blend
            val gGain = 1.0f + (lut.gGain - 1.0f) * blend
            val bGain = 1.0f + (lut.bGain - 1.0f) * blend

            val totalContrast = (lutContrast * logProfile.contrastFactor * extraContrast).coerceIn(0.35f, 2.4f)
            val totalSaturation = (lutSat * logProfile.saturationFactor * extraSaturation).coerceIn(0f, 2.6f)
            val totalBrightness = (lutBrightness + logProfile.shadowLift + extraBrightness).coerceIn(-100f, 100f)
            val totalWarmth = (lutWarmth + logProfile.warmthOffset + extraWarmth).coerceIn(-70f, 70f)

            val cm = ColorMatrix()
            cm.setSaturation(totalSaturation)

            val translate = (0.5f * (1.0f - totalContrast) * 255f) + totalBrightness
            val rWarm = totalWarmth * 1.15f
            val bCool = -totalWarmth * 1.15f

            val gradeMatrix = ColorMatrix(
                floatArrayOf(
                    totalContrast * rGain, 0f, 0f, 0f, translate + rWarm,
                    0f, totalContrast * gGain, 0f, 0f, translate + (totalWarmth * 0.22f),
                    0f, 0f, totalContrast * bGain, 0f, translate + bCool,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(gradeMatrix)
            return cm
        }
    }
}
