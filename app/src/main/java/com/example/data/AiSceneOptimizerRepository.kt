package com.example.data

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.model.GeminiSceneCritique
import com.example.model.RealTimeSceneMetrics
import com.example.model.SceneCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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

    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sceneCritiqueSchema
            }
        )
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

                    // Estimate warmth from U/V planes if present
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
                            // Positive V (red/amber) vs negative U (blue) indicates warm scene
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
     * Runs multimodal Gemini 3.8 Flash scene critique on a scaled viewfinder bitmap.
     */
    suspend fun analyzeFrameWithGemini(
        frameBitmap: Bitmap,
        currentModeLabel: String,
        detectedSceneLabel: String
    ): GeminiSceneCritique {
        // Scale down bitmap to max 768px edge so payload is well under 20MB and ultra-fast
        val scaledBitmap = scaleBitmapForAi(frameBitmap, 768)
        val prompt = """
            You are the built-in Pro Photography Coach inside the Samsung Galaxy HD Camera app.
            The user is currently in $currentModeLabel mode, and the on-device sensor detected: $detectedSceneLabel.
            Analyze this camera viewfinder frame for composition, subject lighting, dynamic range, and color balance.
            Provide concise, practical mobile photography coaching and exact manual Pro settings (ISO, shutter speed, Kelvin white balance, and EV shift) to capture the best flagship shot.
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
