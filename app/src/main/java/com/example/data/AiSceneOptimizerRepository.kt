package com.example.data

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.AiAutoEditRecipe
import com.example.model.GeminiSceneCritique
import com.example.model.HonorAiGenPreset
import com.example.model.PhotoFilterPreset
import com.example.model.RealTimeSceneMetrics
import com.example.model.SceneCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.abs

private const val TAG = "AiSceneOptimizerRepo"

class AiSceneOptimizerRepository {

    private val sceneCritiqueSchema = Schema.obj(
        properties = mapOf(
            "sceneTitle" to Schema.string(description = "Short 2-4 word title of the detected photography scene"),
            "compositionScore" to Schema.integer(description = "Composition & lighting score from 1 to 100"),
            "lightingAnalysis" to Schema.string(description = "Concise 1-sentence evaluation of dynamic range, exposure, and subject lighting"),
            "proCoachingTip" to Schema.string(description = "Actionable 1-sentence framing or camera technique tip"),
            "recommendedIso" to Schema.integer(description = "Recommended manual ISO value (50, 100, 200, 400, 800, 1600, or 3200)"),
            "recommendedShutterLabel" to Schema.string(description = "Recommended shutter speed such as 1/1000, 1/250, 1/125, 1/60, or 1/15"),
            "recommendedKelvin" to Schema.integer(description = "Recommended white balance temperature in Kelvin between 2800 and 7500"),
            "recommendedEvShift" to Schema.double(description = "Recommended EV exposure shift between -1.5 and 1.5")
        )
    )

    private val autoEditSchema = Schema.obj(
        properties = mapOf(
            "styleTitle" to Schema.string(description = "Short 2-4 word title of the AI photo mastering profile (e.g., Natural HDR Master, Golden Hour Warmth, Night Shadow Lift)"),
            "summary" to Schema.string(description = "Concise 1-sentence description of the exposure, color, and detail adjustments applied"),
            "recommendedLut" to Schema.enumeration(
                values = PhotoFilterPreset.entries.map { it.name },
                description = "Recommended preset name (prefer ORIGINAL for natural color, or a subtle film/travel LUT when appropriate)"
            ),
            "lutIntensity" to Schema.double(description = "Subtle blend strength for the LUT between 0.0 and 0.55"),
            "brightness" to Schema.double(description = "Brightness offset between -22.0 and 22.0 to balance exposure"),
            "contrast" to Schema.double(description = "Contrast multiplier between 0.92 and 1.16 for natural dynamic range"),
            "saturation" to Schema.double(description = "Saturation multiplier between 0.94 and 1.15 for natural colors without oversaturation"),
            "warmth" to Schema.double(description = "Color temperature warmth shift between -16.0 and 16.0"),
            "vignette" to Schema.double(description = "Subtle optical vignette between 0.0 and 0.16"),
            "detailSharpness" to Schema.double(description = "Unsharp mask detail amount between 0.18 and 0.36 that preserves natural texture without halos")
        )
    )

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sceneCritiqueSchema
            }
        )
    }

    private val autoEditGenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = autoEditSchema
            }
        )
    }

    /**
     * Honor 600 AIGC Image Reconstruction & Enhancement Model (`gemini-nano-banana-2.1`).
     * Uses zero-configuration `responseModalities = listOf(ResponseModality.IMAGE)` baseline.
     */
    private val honorAiImageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    /**
     * Performs Honor 600 AI Image Generation / AIGC Detail Reconstruction on a captured photo
     * using `gemini-nano-banana-2.1` (`ResponseModality.IMAGE`).
     * Passes the input bitmap and reconstruction instructions, returning the synthesized high-detail Bitmap.
     */
    suspend fun generateHonorAiReconstructedImage(
        sourceBitmap: Bitmap,
        preset: HonorAiGenPreset,
        customInstruction: String = ""
    ): Bitmap = withContext(Dispatchers.Default) {
        val inputScaled = scaleBitmapForAi(sourceBitmap, 1024)
        val instruction = if (customInstruction.isNotBlank()) {
            "${preset.promptInstruction} Additional user request: ${customInstruction.trim()}"
        } else {
            preset.promptInstruction
        }

        try {
            val response = withTimeout(60_000L) {
                honorAiImageGenModel.generateContent(
                    content {
                        image(inputScaled)
                        text(instruction)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            imagePart?.image
                ?: throw IllegalStateException("Honor 600 AI model returned no synthesized image.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Honor 600 AI image generation timed out after 60s", e)
            throw IllegalStateException("Honor 600 AI Generation timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Honor 600 AI image generation failed", e)
            throw e
        }
    }

    /**
     * Creates a lightweight 4Hz real-time ImageAnalysis.Analyzer that inspects the camera YUV planes
     * to compute luminance, 16-bin histogram, dynamic range, and color warmth for zero-latency
     * on-device AI scene detection.
     */
    fun createRealTimeAnalyzer(
        onMetricsUpdated: (RealTimeSceneMetrics) -> Unit
    ): ImageAnalysis.Analyzer {
        var lastAnalyzedTimestamp = 0L
        return ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
            val now = System.currentTimeMillis()
            if (now - lastAnalyzedTimestamp < 250L) {
                imageProxy.close()
                return@Analyzer
            }
            lastAnalyzedTimestamp = now

            try {
                val planes = imageProxy.planes
                if (planes.isNotEmpty()) {
                    val yBuffer = planes[0].buffer
                    yBuffer.rewind()
                    val ySize = yBuffer.remaining()
                    val step = (ySize / 1200).coerceAtLeast(1)

                    val bins = IntArray(16)
                    var totalLuma = 0L
                    var sampleCount = 0
                    var darkCount = 0
                    var brightCount = 0

                    var i = 0
                    while (i < ySize) {
                        val luma = yBuffer.get(i).toInt() and 0xFF
                        totalLuma += luma
                        val binIdx = (luma / 16).coerceIn(0, 15)
                        bins[binIdx]++
                        if (luma < 45) darkCount++
                        if (luma > 210) brightCount++
                        sampleCount++
                        i += step
                    }

                    var warmthScore = 0f
                    if (planes.size >= 3) {
                        val uBuffer = planes[1].buffer
                        val vBuffer = planes[2].buffer
                        uBuffer.rewind()
                        vBuffer.rewind()
                        val uvSize = minOf(uBuffer.remaining(), vBuffer.remaining())
                        val uvStep = (uvSize / 300).coerceAtLeast(1)
                        var uSum = 0L
                        var vSum = 0L
                        var uvSamples = 0
                        var j = 0
                        while (j < uvSize) {
                            val u = (uBuffer.get(j).toInt() and 0xFF) - 128
                            val v = (vBuffer.get(j).toInt() and 0xFF) - 128
                            uSum += u
                            vSum += v
                            uvSamples++
                            j += uvStep
                        }
                        if (uvSamples > 0) {
                            val avgU = uSum.toFloat() / uvSamples
                            val avgV = vSum.toFloat() / uvSamples
                            warmthScore = ((avgV - avgU * 0.5f) / 48f).coerceIn(-1f, 1f)
                        }
                    }

                    if (sampleCount > 0) {
                        val avgLuma = totalLuma.toFloat() / sampleCount
                        val maxBin = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
                        val normalizedBins = bins.map { (it.toFloat() / maxBin).coerceIn(0.05f, 1.0f) }
                        val darkRatio = darkCount.toFloat() / sampleCount
                        val brightRatio = brightCount.toFloat() / sampleCount
                        val dynamicSpread = (darkRatio + brightRatio).coerceIn(0f, 1f)

                        val detectedScene = classifyScene(
                            avgLuma = avgLuma,
                            darkRatio = darkRatio,
                            brightRatio = brightRatio,
                            warmthScore = warmthScore
                        )

                        onMetricsUpdated(
                            RealTimeSceneMetrics(
                                detectedScene = detectedScene,
                                averageLuminance = avgLuma,
                                dynamicRangeRatio = dynamicSpread,
                                colorWarmthScore = warmthScore,
                                histogramBins = normalizedBins
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Frame analysis skipped: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun classifyScene(
        avgLuma: Float,
        darkRatio: Float,
        brightRatio: Float,
        warmthScore: Float
    ): SceneCategory {
        return when {
            avgLuma < 58f -> SceneCategory.NIGHT_LOW_LIGHT
            darkRatio > 0.25f && brightRatio > 0.20f -> SceneCategory.HIGH_CONTRAST_HDR
            warmthScore > 0.35f && avgLuma in 70f..190f -> SceneCategory.SUNSET_GOLDEN
            warmthScore in 0.18f..0.35f && avgLuma > 95f -> SceneCategory.FOOD_CULINARY
            avgLuma > 165f && warmthScore < 0.12f -> SceneCategory.LANDSCAPE_NATURE
            abs(warmthScore) < 0.15f && avgLuma in 90f..160f -> SceneCategory.PORTRAIT_SUBJECT
            else -> SceneCategory.AUTO
        }
    }

    /**
     * Computes `RealTimeSceneMetrics` (16-bin luminance histogram, average luminance, dynamic range,
     * warmth, and scene classification) from a downscaled viewfinder snapshot so the Camera2 HAL
     * only needs 2 streams (`Preview` + `ImageCapture`) for full 4K/QHD+ viewfinder sharpness.
     */
    fun computeSceneMetricsFromBitmap(bitmap: Bitmap): RealTimeSceneMetrics {
        val sampleBmp = scaleBitmapForAi(bitmap, 160)
        val w = sampleBmp.width.coerceAtLeast(1)
        val h = sampleBmp.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        sampleBmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val bins = IntArray(16)
        var totalLuma = 0L
        var darkCount = 0
        var brightCount = 0
        var rSum = 0L
        var bSum = 0L

        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val luma = ((r * 77 + g * 150 + b * 29) shr 8).coerceIn(0, 255)
            totalLuma += luma
            rSum += r
            bSum += b
            bins[(luma / 16).coerceIn(0, 15)]++
            if (luma < 45) darkCount++
            if (luma > 210) brightCount++
        }

        val count = pixels.size.coerceAtLeast(1)
        val avgLuma = totalLuma.toFloat() / count
        val maxBin = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
        val normalizedBins = bins.map { (it.toFloat() / maxBin).coerceIn(0.05f, 1.0f) }
        val darkRatio = darkCount.toFloat() / count
        val brightRatio = brightCount.toFloat() / count
        val dynamicSpread = (darkRatio + brightRatio).coerceIn(0f, 1f)
        val avgR = rSum.toFloat() / count
        val avgB = bSum.toFloat() / count
        val warmthScore = ((avgR - avgB) / 64f).coerceIn(-1f, 1f)

        val detectedScene = classifyScene(
            avgLuma = avgLuma,
            darkRatio = darkRatio,
            brightRatio = brightRatio,
            warmthScore = warmthScore
        )

        return RealTimeSceneMetrics(
            detectedScene = detectedScene,
            averageLuminance = avgLuma,
            dynamicRangeRatio = dynamicSpread,
            colorWarmthScore = warmthScore,
            histogramBins = normalizedBins
        )
    }

    /**
     * Generates an AI Post-Capture Auto-Edit & Mastering Recipe for a captured photo.
     * 1. First computes fast on-device luminance, shadow/highlight ratio, and RGB color-balance metrics.
     * 2. Requests multimodal Gemini 3.8 Flash mastering parameters (`autoEditGenerativeModel`),
     *    falling back seamlessly to the on-device histogram-driven recipe if offline.
     */
    suspend fun generateAiAutoEditRecipe(
        capturedBitmap: Bitmap,
        upscaleFactor: Int = 2
    ): AiAutoEditRecipe = withContext(Dispatchers.Default) {
        val fallbackRecipe = computeOnDeviceHistogramAutoEditRecipe(capturedBitmap, upscaleFactor)
        val previewBitmap = scaleBitmapForAi(capturedBitmap, 640)

        val prompt = """
            You are an expert computational photography colorist optimizing a smartphone photo captured on an Honor 600 camera.
            Analyze the image's exposure, shadow detail, highlight clipping, foliage/skin textures, and white balance.
            Return a subtle, natural post-capture mastering recipe that improves dynamic range, color accuracy, and crispness WITHOUT oversharpening, haloing, or artificial oversaturation.
        """.trimIndent()

        try {
            val response = withTimeout(30_000L) {
                autoEditGenerativeModel.generateContent(
                    content {
                        image(previewBitmap)
                        text(prompt)
                    }
                )
            }
            val rawJson = response.text
            if (rawJson.isNullOrBlank()) return@withContext fallbackRecipe

            val json = JSONObject(rawJson)
            val lutName = json.optString("recommendedLut", PhotoFilterPreset.ORIGINAL.name)
            val lutPreset = runCatching { PhotoFilterPreset.valueOf(lutName) }
                .getOrDefault(PhotoFilterPreset.ORIGINAL)

            AiAutoEditRecipe(
                styleTitle = json.optString("styleTitle", fallbackRecipe.styleTitle),
                summary = json.optString("summary", fallbackRecipe.summary),
                recommendedLut = lutPreset,
                lutIntensity = json.optDouble("lutIntensity", 0.25).toFloat().coerceIn(0f, 0.55f),
                brightness = json.optDouble("brightness", fallbackRecipe.brightness.toDouble())
                    .toFloat().coerceIn(-22f, 22f),
                contrast = json.optDouble("contrast", fallbackRecipe.contrast.toDouble())
                    .toFloat().coerceIn(0.92f, 1.16f),
                saturation = json.optDouble("saturation", fallbackRecipe.saturation.toDouble())
                    .toFloat().coerceIn(0.94f, 1.15f),
                warmth = json.optDouble("warmth", fallbackRecipe.warmth.toDouble())
                    .toFloat().coerceIn(-16f, 16f),
                vignette = json.optDouble("vignette", fallbackRecipe.vignette.toDouble())
                    .toFloat().coerceIn(0f, 0.16f),
                detailSharpness = json.optDouble("detailSharpness", fallbackRecipe.detailSharpness.toDouble())
                    .toFloat().coerceIn(0.18f, 0.36f),
                upscaleFactor = upscaleFactor,
                usedCloudGemini = true
            )
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.w(TAG, "Gemini Auto-Edit timed out; using on-device histogram AI Auto-Edit recipe")
                return@withContext fallbackRecipe
            }
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "Using on-device histogram AI Auto-Edit recipe (${e.message})")
            fallbackRecipe
        }
    }

    /**
     * Deterministic On-Device Histogram & Color-Balance Auto-Edit analyzer.
     * Samples the captured bitmap to measure shadow crushing, highlight clipping, green foliage
     * dominance, and color cast, producing a natural HDR & micro-contrast recipe immediately.
     */
    fun computeOnDeviceHistogramAutoEditRecipe(
        bitmap: Bitmap,
        upscaleFactor: Int = 2
    ): AiAutoEditRecipe {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val stepX = (w / 48).coerceAtLeast(1)
        val stepY = (h / 48).coerceAtLeast(1)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var lumaSum = 0L
        var darkPixels = 0
        var brightPixels = 0
        var foliagePixels = 0
        var totalSamples = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val luma = (r * 77 + g * 150 + b * 29) shr 8

                rSum += r
                gSum += g
                bSum += b
                lumaSum += luma

                if (luma < 50) darkPixels++
                if (luma > 210) brightPixels++
                if (g > r + 12 && g > b + 12 && luma in 35..200) foliagePixels++

                totalSamples++
                x += stepX
            }
            y += stepY
        }

        if (totalSamples == 0) {
            return AiAutoEditRecipe(upscaleFactor = upscaleFactor)
        }

        val avgLuma = lumaSum.toFloat() / totalSamples
        val avgR = rSum.toFloat() / totalSamples
        val avgB = bSum.toFloat() / totalSamples
        val darkRatio = darkPixels.toFloat() / totalSamples
        val brightRatio = brightPixels.toFloat() / totalSamples
        val foliageRatio = foliagePixels.toFloat() / totalSamples

        // 1. Exposure / Shadow recovery without blowing out highlights
        val brightnessShift = when {
            avgLuma < 85f -> ((110f - avgLuma) * 0.34f).coerceIn(10f, 22f)
            avgLuma > 180f -> -((avgLuma - 168f) * 0.28f).coerceIn(4f, 14f)
            darkRatio > 0.28f && brightRatio < 0.12f -> 12f
            else -> 8f
        }

        // 2. Studio Harcourt & MagicLM dynamic contrast expansion
        val contrastMult = when {
            darkRatio > 0.25f && brightRatio > 0.20f -> 1.15f
            avgLuma < 75f -> 1.20f
            else -> 1.18f
        }

        // 3. Rich Honor True-Tone vibrancy & auto white-balance warmth correction
        val saturationMult = if (foliageRatio > 0.18f) {
            1.22f
        } else {
            1.26f
        }

        val redBlueDelta = avgR - avgB
        val warmthShift = when {
            redBlueDelta < -12f -> 9f  // Neutralize overly cool/blue cast with golden warmth
            redBlueDelta > 28f -> -5f  // Balance extreme tungsten cast
            else -> 6f
        }

        // 4. Adaptive neural acutance detail & Studio LUT blend
        val detailSharpness = when {
            avgLuma < 65f -> 0.44f
            foliageRatio > 0.20f -> 0.50f
            else -> 0.56f
        }

        val (styleTitle, recommendedLut, lutBlend) = when {
            avgLuma < 68f -> Triple("Honor MagicLM Night Vision HDR", PhotoFilterPreset.KODAK_PORTRA_400, 0.36f)
            foliageRatio > 0.18f -> Triple("Honor 200MP Ultra-Clarity & Foliage", PhotoFilterPreset.BALI_TROPICAL, 0.38f)
            darkRatio > 0.22f && brightRatio > 0.18f -> Triple("Honor Studio Harcourt Dynamic HDR", PhotoFilterPreset.SAHARA_GOLDEN, 0.40f)
            else -> Triple("Honor Studio Harcourt Radiant Master", PhotoFilterPreset.VIVID_GALAXY, 0.44f)
        }

        val summary = "MagicLM ${upscaleFactor}x Neural SR • Exposure ${String.format("%+.0f", brightnessShift)} • Contrast ${String.format("%.2fx", contrastMult)} • Vibrance ${String.format("%.2fx", saturationMult)}"

        return AiAutoEditRecipe(
            styleTitle = styleTitle,
            summary = summary,
            recommendedLut = recommendedLut,
            lutIntensity = lutBlend,
            brightness = brightnessShift,
            contrast = contrastMult,
            saturation = saturationMult,
            warmth = warmthShift,
            vignette = 0.09f,
            detailSharpness = detailSharpness,
            upscaleFactor = upscaleFactor,
            usedCloudGemini = false
        )
    }

    /**
     * Runs multimodal Gemini 3.8 Flash scene critique on a scaled viewfinder bitmap.
     */
    suspend fun analyzeFrameWithGemini(
        frameBitmap: Bitmap,
        currentModeLabel: String,
        detectedSceneLabel: String
    ): GeminiSceneCritique {
        val scaledBitmap = scaleBitmapForAi(frameBitmap, 768)
        val prompt = """
            You are the built-in Pro Photography Coach inside the camera app.
            The user is currently in $currentModeLabel mode, and the on-device sensor detected: $detectedSceneLabel.
            Analyze this camera viewfinder frame for composition, subject lighting, dynamic range, and color balance.
            Provide concise, practical mobile photography coaching and exact manual Pro settings (ISO, shutter speed, Kelvin white balance, and EV shift) to capture the best shot.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                generativeModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI Coach returned an empty response.")
            val json = JSONObject(rawJson)
            return GeminiSceneCritique(
                sceneTitle = json.optString("sceneTitle", detectedSceneLabel),
                compositionScore = json.optInt("compositionScore", 88).coerceIn(1, 100),
                lightingAnalysis = json.optString(
                    "lightingAnalysis",
                    "Balanced exposure with clear subject separation."
                ),
                proCoachingTip = json.optString(
                    "proCoachingTip",
                    "Lock focus on the primary subject and lower EV slightly to preserve highlights."
                ),
                recommendedIso = json.optInt("recommendedIso", 100).coerceIn(50, 3200),
                recommendedShutterLabel = json.optString("recommendedShutterLabel", "1/125"),
                recommendedKelvin = json.optInt("recommendedKelvin", 5400).coerceIn(2300, 10000),
                recommendedEvShift = json.optDouble("recommendedEvShift", 0.0).toFloat().coerceIn(-2f, 2f)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini scene analysis timed out", e)
            throw IllegalStateException("AI Scene Coach timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini scene analysis failed", e)
            throw e
        }
    }

    private fun scaleBitmapForAi(source: Bitmap, maxEdge: Int): Bitmap {
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        if (width <= maxEdge && height <= maxEdge) return source
        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (width >= height) {
            targetW = maxEdge
            targetH = (maxEdge / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxEdge
            targetW = (maxEdge * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
