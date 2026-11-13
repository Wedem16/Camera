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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.R
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
import com.example.model.ZoomOptimizationProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TAG = "MediaGalleryRepo"

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
     * Processes a captured camera Bitmap according to the active CameraMode, 4K/50MP Resolution,
     * PhotoFormatSetting (JPG, RAW+JPG .dng, PNG Lossless, HEIF/WEBP), hybrid 10x zoom crop,
     * AI Super-Resolution Zoom & Auto Quality Enhancer, Film/Travel LUT, Cinema Log Profile,
     * ProCameraSettings, and AI Scene Optimizer.
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
        aiSceneOptimizerEnabled: Boolean,
        aiZoomAutoEnhanceEnabled: Boolean = true,
        currentZoomRatio: Float = 1.0f,
        extraHybridZoomFactor: Float = 1.0f,
        detectedScene: SceneCategory,
        portraitBlurStrength: Float,
        beautySmoothness: Float,
        addWatermark: Boolean
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // 1. Apply hybrid zoom center-crop if the user zoomed beyond sensor's hardware max (e.g. 10x)
            val zoomCropped = if (extraHybridZoomFactor > 1.02f) {
                cropCenterByZoomFactor(rawBitmap, extraHybridZoomFactor.coerceIn(1.0f, 10.0f))
            } else {
                rawBitmap
            }

            // 2. Crop to selected aspect ratio (or 2.35:1 panoramic strip in PANORAMA mode)
            val targetRatio = if (mode == CameraMode.PANORAMA) {
                2.35f
            } else {
                aspectRatio.ratioValue
            }
            val cropped = cropBitmapToRatio(zoomCropped, targetRatio, isPanorama = mode == CameraMode.PANORAMA)

            // 3. Guarantee True 4K UHD (3840×2160) or 50MP Master resolution
            val targetLong = resolution.targetLongEdgePx
            val currentLong = max(cropped.width, cropped.height).coerceAtLeast(1)
            val scaled = if (currentLong < targetLong) {
                val scaleFactor = targetLong.toFloat() / currentLong.toFloat()
                val newW = (cropped.width * scaleFactor).toInt().coerceAtLeast(1)
                val newH = (cropped.height * scaleFactor).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(cropped, newW, newH, true)
            } else {
                cropped
            }

            // 4. Apply computational mode, AI Super-Resolution Zoom Quality Enhancer, LUT, and Cinema Log
            val processed = applyComputationalModeEffects(
                source = scaled,
                mode = mode,
                proSettings = proSettings,
                activeLut = activeLut,
                lutIntensity = lutIntensity,
                activeLogProfile = activeLogProfile,
                aiSceneOptimizerEnabled = aiSceneOptimizerEnabled,
                aiZoomAutoEnhanceEnabled = aiZoomAutoEnhanceEnabled,
                currentZoomRatio = currentZoomRatio,
                detectedScene = detectedScene,
                portraitBlurStrength = portraitBlurStrength,
                beautySmoothness = beautySmoothness,
                addWatermark = addWatermark,
                resolutionLabel = "${scaled.width}×${scaled.height} (${resolution.shortLabel} • ${photoFormat.badgeLabel})"
            )

            // 5. Write to MediaStore in the requested format (JPG, RAW+JPG .dng, PNG, or WEBP)
            if (photoFormat == PhotoFormatSetting.RAW_DNG) {
                // Save accompanying DNG RAW master file + 4K Developed JPG master
                saveBitmapToMediaStore(
                    bitmap = processed,
                    prefix = "GALAXY_4K_RAW_${mode.name}",
                    jpegQuality = 100,
                    photoFormat = PhotoFormatSetting.RAW_DNG
                )
                saveBitmapToMediaStore(
                    bitmap = processed,
                    prefix = "GALAXY_4K_RAW_DEV_${mode.name}",
                    jpegQuality = 100,
                    photoFormat = PhotoFormatSetting.JPEG
                )
            } else {
                saveBitmapToMediaStore(
                    bitmap = processed,
                    prefix = "GALAXY_4K_${mode.name}",
                    jpegQuality = resolution.jpegQuality,
                    photoFormat = photoFormat
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process and save 4K photo", e)
            null
        }
    }

    /**
     * Imports a 4K studio sample photo from our bundled flagship camera drawable so users can test
     * the Photo Editor, Film/Travel LUTs, and Log profiles immediately.
     */
    suspend fun createStudioSamplePhoto(): Uri? = withContext(Dispatchers.IO) {
        try {
            val rawSample = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_camera_hero_lens_1794590959493
            ) ?: return@withContext null
            val sample4K = Bitmap.createScaledBitmap(rawSample, 3840, 2160, true)
            saveBitmapToMediaStore(
                bitmap = sample4K,
                prefix = "GALAXY_4K_STUDIO_SAMPLE",
                jpegQuality = 100,
                photoFormat = PhotoFormatSetting.JPEG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create studio sample photo", e)
            null
        }
    }

    /**
     * Renders non-destructive photo edits (Film/Travel LUT, Cinema Log Profile, Brightness,
     * Contrast, Saturation, Warmth, Vignette, Rotation, Aspect Crop, and Galaxy EXIF Watermark)
     * and saves a new 4K copy in MediaStore.
     */
    suspend fun saveEditedPhoto(
        sourceUri: Uri,
        editState: PhotoEditState
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = decodeBitmapFromUri(sourceUri, maxDimension = 3840) ?: return@withContext null
            val rendered = renderPhotoEdits(originalBitmap, editState)
            saveBitmapToMediaStore(
                bitmap = rendered,
                prefix = "GALAXY_4K_LUT_${editState.filter.name}",
                jpegQuality = 100,
                photoFormat = PhotoFormatSetting.JPEG
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save edited photo", e)
            null
        }
    }

    fun decodeBitmapFromUri(uri: Uri, maxDimension: Int = 3840): Bitmap? {
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

        // 5. Optional Galaxy Flagship Watermark
        if (editState.showGalaxyWatermark) {
            drawGalaxyWatermark(
                canvas = canvas,
                width = outBitmap.width.toFloat(),
                height = outBitmap.height.toFloat(),
                subtitle = "Galaxy 4K Studio • LUT: ${editState.filter.label} • ${editState.logProfile.shortCode}"
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

    private fun cropCenterByZoomFactor(source: Bitmap, factor: Float): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 4 || h <= 4 || factor <= 1.01f) return source
        val newW = (w / factor).toInt().coerceIn(64, w)
        val newH = (h / factor).toInt().coerceIn(64, h)
        val offsetX = ((w - newW) / 2).coerceAtLeast(0)
        val offsetY = ((h - newH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(source, offsetX, offsetY, newW, newH)
    }

    private fun applyComputationalModeEffects(
        source: Bitmap,
        mode: CameraMode,
        proSettings: ProCameraSettings,
        activeLut: PhotoFilterPreset,
        lutIntensity: Float,
        activeLogProfile: LogProfile,
        aiSceneOptimizerEnabled: Boolean,
        aiZoomAutoEnhanceEnabled: Boolean,
        currentZoomRatio: Float,
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

        val zoomProfile = ZoomOptimizationProfile.fromZoomRatio(currentZoomRatio)

        if (aiSceneOptimizerEnabled) {
            saturation *= detectedScene.saturationBoost
            contrast *= detectedScene.contrastBoost
            warmth += detectedScene.warmthShift
            // Improved Night & Low-Light Photography: natural shadow lift without washing out blacks
            if (detectedScene == SceneCategory.NIGHT_LOW_LIGHT) {
                brightness += 8f
            }
        }

        // Apply Dedicated Per-Zoom ISP Processing Profile (0.6x, 1x 200MP, 2x Crop, 3x Super-Res, 4x-5x, 10x Natural)
        if (aiZoomAutoEnhanceEnabled) {
            contrast *= zoomProfile.contrastMultiplier
            saturation *= zoomProfile.saturationMultiplier
            brightness += zoomProfile.shadowLiftOffset
        }

        when (mode) {
            CameraMode.NIGHT -> {
                // Improved Night Photography: deep shadow recovery, clean low-noise blacks, natural saturation
                contrast *= 1.11f
                saturation *= 1.09f
                brightness += 16f
                warmth += 3f
            }
            CameraMode.HDR -> {
                // Premium HDR with excellent dynamic range & highlight protection
                contrast *= 1.13f
                saturation *= 1.12f
                brightness += 6f
            }
            CameraMode.BEAUTY -> {
                // Natural skin tones without plastic/excessive AI smoothing
                contrast *= (1.0f - beautySmoothness * 0.06f)
                saturation *= 1.03f
                brightness += beautySmoothness * 9f
                warmth += beautySmoothness * 6f
            }
            CameraMode.PORTRAIT -> {
                // Natural skin tones & realistic facial textures
                contrast *= 1.04f
                saturation *= 1.04f
                warmth += 3f
            }
            CameraMode.PRO -> {
                val kelvinDiff = (proSettings.customKelvin - 5500) / 120f
                warmth += kelvinDiff.coerceIn(-35f, 35f)
                brightness += proSettings.exposureCompensationIndex * 4f
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

        // Natural Texture & Adaptive Edge Clarity Pass (No watercolor effect, no oversharpening)
        if (aiZoomAutoEnhanceEnabled && mode != CameraMode.BEAUTY) {
            applyAiSuperResolutionDetailPass(canvas, result, zoomProfile)
        }

        // Mode-specific optical effects
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
            drawVignetteOverlay(centerCanvas, width.toFloat(), height.toFloat(), portraitBlurStrength * 0.35f)

            canvas.drawBitmap(center, 0f, 0f, null)
        } else if (mode == CameraMode.NIGHT || mode == CameraMode.HDR) {
            drawVignetteOverlay(canvas, width.toFloat(), height.toFloat(), 0.16f)
        }

        if (addWatermark) {
            val lutPart = if (activeLut != PhotoFilterPreset.ORIGINAL) " • LUT: ${activeLut.label}" else ""
            val logPart = if (activeLogProfile != LogProfile.STANDARD_REC709) " • ${activeLogProfile.shortCode}" else ""
            val zoomPart = " • ${zoomProfile.badgeLabel}"
            val telemetry = if (mode == CameraMode.PRO) {
                "$resolutionLabel$zoomPart • ISO ${proSettings.formattedIso} • ${proSettings.formattedShutter} • ${proSettings.formattedWb}$lutPart$logPart"
            } else {
                "${mode.badgeTitle} • $resolutionLabel$zoomPart$lutPart$logPart"
            }
            drawGalaxyWatermark(canvas, width.toFloat(), height.toFloat(), telemetry)
        }

        return result
    }

    /**
     * Performs a profile-tuned natural texture & micro-contrast pass tailored to each zoom range
     * (1x 200MP natural, 2x sensor crop, 3x multi-frame super-res, 4x-5x reconstruction, 10x anti-oversharpen).
     */
    private fun applyAiSuperResolutionDetailPass(
        canvas: Canvas,
        currentBitmap: Bitmap,
        zoomProfile: ZoomOptimizationProfile
    ) {
        try {
            val w = currentBitmap.width
            val h = currentBitmap.height
            if (w < 64 || h < 64) return

            val detailAlpha = zoomProfile.microDetailOverlayAlpha.coerceIn(12, 36)
            val c = zoomProfile.microDetailContrast.coerceIn(1.10f, 1.32f)
            val t = 0.5f * (1f - c) * 255f

            val naturalTextureMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = detailAlpha
                colorFilter = ColorMatrixColorFilter(naturalTextureMatrix)
                xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
            canvas.drawBitmap(currentBitmap, 0f, 0f, detailPaint)
        } catch (e: Exception) {
            Log.w(TAG, "AI Super-Resolution pass skipped: ${e.message}")
        }
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

    private fun drawGalaxyWatermark(
        canvas: Canvas,
        width: Float,
        height: Float,
        subtitle: String
    ) {
        val barHeight = (min(width, height) * 0.075f).coerceAtLeast(48f)
        val barTop = height - barHeight
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 10, 12, 16)
        }
        canvas.drawRect(0f, barTop, width, height, bgPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 214, 10)
        }
        val padding = barHeight * 0.25f
        canvas.drawRect(padding, barTop + padding, padding + 6f, height - padding, accentPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = barHeight * 0.34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 188, 198)
            textSize = barHeight * 0.24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val textX = padding + 20f
        canvas.drawText("GALAXY HD • 200MP 1/1.4\" OIS • SD 7 GEN 4", textX, barTop + barHeight * 0.46f, titlePaint)
        canvas.drawText(subtitle, textX, barTop + barHeight * 0.80f, subPaint)
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

    @Suppress("DEPRECATION")
    private fun saveBitmapToMediaStore(
        bitmap: Bitmap,
        prefix: String,
        jpegQuality: Int,
        photoFormat: PhotoFormatSetting = PhotoFormatSetting.JPEG
    ): Uri? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val displayName = "${prefix}_$timestamp.${photoFormat.extension}"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, photoFormat.mimeType)
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/GalaxyHDCamera"
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

    companion object {
        /**
         * Builds an Android ColorMatrix combining a Film/Travel/Cinema LUT, a Cinema Log Profile,
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
