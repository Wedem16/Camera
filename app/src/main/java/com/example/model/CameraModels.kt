package com.example.model

import android.net.Uri

enum class CameraMode(
    val label: String,
    val badgeTitle: String,
    val description: String,
    val isDefaultCarousel: Boolean
) {
    PORTRAIT(
        label = "PORTRAIT",
        badgeTitle = "Studio Portrait Bokeh",
        description = "Adjustable f/1.4–f/16 optical-style background blur and subject isolation",
        isDefaultCarousel = true
    ),
    PHOTO(
        label = "PHOTO",
        badgeTitle = "Galaxy Photo",
        description = "High-resolution capture with Smart Scene Optimization",
        isDefaultCarousel = true
    ),
    VIDEO(
        label = "VIDEO",
        badgeTitle = "Pro Video",
        description = "Up to 4K 60fps recording with Super Steady stabilization & pause/resume",
        isDefaultCarousel = true
    ),
    PRO(
        label = "PRO",
        badgeTitle = "Manual Pro",
        description = "Full Camera2 manual control over ISO, Shutter Speed, WB, Focus & EV",
        isDefaultCarousel = true
    ),
    NIGHT(
        label = "NIGHT",
        badgeTitle = "Nightography",
        description = "Multi-frame low-light shadow boost and highlight preservation",
        isDefaultCarousel = false
    ),
    HDR(
        label = "HDR",
        badgeTitle = "Rich Tone HDR",
        description = "Expanded dynamic range recovering deep shadows and bright skies",
        isDefaultCarousel = false
    ),
    BEAUTY(
        label = "BEAUTY",
        badgeTitle = "Studio Beauty",
        description = "Natural dermal smoothing, skin tone warmth, and clarity enhancement",
        isDefaultCarousel = false
    ),
    PANORAMA(
        label = "PANORAMA",
        badgeTitle = "Ultra-Wide Panorama",
        description = "Cinematic 21:9 / 2.4:1 wide-field panoramic horizon framing",
        isDefaultCarousel = false
    )
}

enum class FlashSetting(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

enum class TimerSetting(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_5(5, "5s"),
    SEC_10(10, "10s")
}

enum class PhotoResolutionSetting(
    val shortLabel: String,
    val fullTitle: String,
    val jpegQuality: Int,
    val upscaleFactor: Float
) {
    HD_12MP("HD 12MP", "12MP (4000×3000 Dual Pixel)", 92, 1.0f),
    ULTRA_HD_50MP("UHD 50MP", "50MP Ultra HD Detail Enhancer", 100, 1.25f)
}

enum class AspectRatioSetting(
    val label: String,
    val ratioValue: Float? // width / height in portrait or null for Full screen
) {
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("FULL", null)
}

enum class VideoResolutionSetting(val shortLabel: String, val fullTitle: String) {
    FHD_1080P("FHD", "Full HD 1080p (1920×1080)"),
    UHD_4K("UHD 4K", "Ultra HD 4K (3840×2160)")
}

enum class VideoFpsSetting(val fps: Int, val label: String) {
    FPS_30(30, "30fps"),
    FPS_60(60, "60fps")
}

enum class WhiteBalancePreset(val label: String, val kelvin: Int) {
    AUTO("Auto", 5500),
    DAYLIGHT("Daylight", 5200),
    CLOUDY("Cloudy", 6500),
    INCANDESCENT("Tungsten", 3200),
    FLUORESCENT("Fluorescent", 4000),
    MANUAL_KELVIN("Custom K", 5500)
}

data class ProCameraSettings(
    val iso: Int? = null, // null = Auto, else 50..3200
    val shutterSpeedNs: Long? = null, // null = Auto, else ns (e.g., 1/4000s = 250_000L to 1s = 1_000_000_000L)
    val whiteBalance: WhiteBalancePreset = WhiteBalancePreset.AUTO,
    val customKelvin: Int = 5500, // 2300..10000K
    val manualFocusDistance: Float? = null, // null = Auto AF, else 0.0f (Infinity) .. 1.0f (Macro)
    val exposureCompensationIndex: Int = 0
) {
    val formattedIso: String
        get() = iso?.toString() ?: "AUTO"

    val formattedShutter: String
        get() {
            val ns = shutterSpeedNs ?: return "AUTO"
            return when {
                ns >= 1_000_000_000L -> "${ns / 1_000_000_000L}s"
                ns >= 100_000_000L -> "1/${(1_000_000_000L / ns).coerceAtLeast(1)}s"
                else -> "1/${(1_000_000_000L / ns).coerceAtLeast(1)}"
            }
        }

    val formattedWb: String
        get() = if (whiteBalance == WhiteBalancePreset.AUTO) {
            "AUTO"
        } else {
            "${if (whiteBalance == WhiteBalancePreset.MANUAL_KELVIN) customKelvin else whiteBalance.kelvin}K"
        }

    val formattedFocus: String
        get() = manualFocusDistance?.let {
            if (it < 0.15f) "INF" else String.format("%.1f", it)
        } ?: "AF"
}

enum class ProParameterTab(val title: String) {
    NONE("NONE"),
    ISO("ISO"),
    SHUTTER("SPEED"),
    EV("EV"),
    FOCUS("FOCUS"),
    WB("WB")
}

data class HardwareCapabilities(
    val hasRearCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val hasFlashUnit: Boolean = true,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val zoomPresets: List<Float> = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f),
    val supportsUltraWideOptical: Boolean = false,
    val supports4K: Boolean = false,
    val supports60Fps: Boolean = true,
    val supportsVideoStabilization: Boolean = true,
    val supportsManualSensor: Boolean = true,
    val minExposureIndex: Int = -8,
    val maxExposureIndex: Int = 8,
    val exposureStepEv: Float = 0.25f,
    val minFocusDistanceDiopters: Float = 10.0f,
    val hardwareLevelLabel: String = "LIMITED"
)

enum class SceneCategory(
    val displayTitle: String,
    val subtitle: String,
    val saturationBoost: Float,
    val contrastBoost: Float,
    val warmthShift: Float
) {
    AUTO("Smart Auto", "Balanced Galaxy Color Science", 1.05f, 1.03f, 0f),
    NIGHT_LOW_LIGHT("Night Scene", "Multi-frame Shadow & Noise Optimization", 1.12f, 1.10f, 4f),
    PORTRAIT_SUBJECT("Portrait Studio", "Natural Skin Tone & Depth Separation", 1.04f, 1.02f, 6f),
    LANDSCAPE_NATURE("Vivid Landscape", "Enhanced Sky Blues & Foliage Greens", 1.22f, 1.08f, 2f),
    FOOD_CULINARY("Gourmet Food", "Rich Warmth & Crisp Texture Pop", 1.25f, 1.08f, 14f),
    SUNSET_GOLDEN("Golden Hour", "Amber Horizon & Highlight Recovery", 1.20f, 1.06f, 18f),
    HIGH_CONTRAST_HDR("Backlit HDR", "Dynamic Shadow Lift & Highlight Guard", 1.10f, 1.15f, 0f),
    MACRO_DETAIL("Close-Up Detail", "Micro-Contrast & Edge Crispness", 1.08f, 1.12f, 0f)
}

data class RealTimeSceneMetrics(
    val detectedScene: SceneCategory = SceneCategory.AUTO,
    val averageLuminance: Float = 128f, // 0..255
    val dynamicRangeRatio: Float = 0.5f,
    val colorWarmthScore: Float = 0.0f, // -1 (cool) .. +1 (warm)
    val histogramBins: List<Float> = List(16) { 0.2f }
)

data class GeminiSceneCritique(
    val sceneTitle: String,
    val compositionScore: Int,
    val lightingAnalysis: String,
    val proCoachingTip: String,
    val recommendedIso: Int,
    val recommendedShutterLabel: String,
    val recommendedKelvin: Int,
    val recommendedEvShift: Float
)

sealed interface AiCoachUiState {
    data object Idle : AiCoachUiState
    data object Analyzing : AiCoachUiState
    data class Success(val critique: GeminiSceneCritique) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val isVideo: Boolean,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val mimeType: String = "image/jpeg"
) {
    val formattedResolution: String
        get() = if (width > 0 && height > 0) "${width}×${height}" else "HD"

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024f * 1024f)
            return if (mb >= 1f) String.format("%.1f MB", mb)
            else String.format("%d KB", (sizeBytes / 1024L).coerceAtLeast(1L))
        }
}

enum class PhotoFilterPreset(
    val label: String,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val brightness: Float
) {
    ORIGINAL("Original", 1.0f, 1.0f, 0f, 0f),
    VIVID_GALAXY("Galaxy Vivid", 1.12f, 1.32f, 4f, 4f),
    WARM_AMBER("Warm Gold", 1.06f, 1.15f, 22f, 5f),
    COOL_CYBER("Cyber Frost", 1.14f, 1.10f, -24f, 0f),
    CLASSIC_BW("Noir B&W", 1.25f, 0.0f, 0f, -4f),
    CINEMA_TEAL("Cinema Grade", 1.18f, 0.90f, -12f, -6f),
    VINTAGE_FILM("Analog 35mm", 0.95f, 0.85f, 16f, 8f)
}

enum class CropAspectPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE_1_1("1:1", 1f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    WIDE_16_9("16:9", 16f / 9f)
}

data class PhotoEditState(
    val filter: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val brightness: Float = 0f, // -60f .. +60f
    val contrast: Float = 1.0f, // 0.6f .. 1.6f
    val saturation: Float = 1.0f, // 0.0f .. 2.0f
    val warmth: Float = 0f, // -40f .. +40f
    val vignette: Float = 0.15f, // 0.0f .. 1.0f
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val cropAspect: CropAspectPreset = CropAspectPreset.ORIGINAL,
    val showGalaxyWatermark: Boolean = true
)
