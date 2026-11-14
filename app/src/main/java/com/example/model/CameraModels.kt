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
        badgeTitle = "4K Studio Portrait",
        description = "Adjustable f/1.4–f/16 optical-style background blur and subject isolation",
        isDefaultCarousel = true
    ),
    PHOTO(
        label = "PHOTO",
        badgeTitle = "4K Galaxy Photo",
        description = "True 4K Ultra HD capture with AI Zoom Super-Resolution & Film/Travel LUTs",
        isDefaultCarousel = true
    ),
    VIDEO(
        label = "VIDEO",
        badgeTitle = "4K Pro Video",
        description = "4K UHD 3840×2160 recording up to 1000 Mbps in MP4, HEVC, RAW MOV & MKV",
        isDefaultCarousel = true
    ),
    PRO(
        label = "PRO",
        badgeTitle = "Manual Pro 4K",
        description = "Full Camera2 manual control over ISO, Shutter, WB, Focus, RAW DNG, LUT & Log",
        isDefaultCarousel = true
    ),
    NIGHT(
        label = "NIGHT",
        badgeTitle = "4K Nightography",
        description = "Multi-frame low-light shadow boost and highlight preservation",
        isDefaultCarousel = false
    ),
    HDR(
        label = "HDR",
        badgeTitle = "4K Rich Tone HDR",
        description = "Expanded dynamic range recovering deep shadows and bright skies",
        isDefaultCarousel = false
    ),
    BEAUTY(
        label = "BEAUTY",
        badgeTitle = "4K Studio Beauty",
        description = "Natural dermal smoothing, skin tone warmth, and clarity enhancement",
        isDefaultCarousel = false
    ),
    PANORAMA(
        label = "PANORAMA",
        badgeTitle = "4K Ultra-Wide Pano",
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
    val targetLongEdgePx: Int,
    val targetShortEdgePx: Int
) {
    ULTRA_HD_200MP("MAX MP", "Native Sensor Max Resolution (Unchanged JPEG)", 100, 4608, 3456),
    ULTRA_HD_50MP("50MP", "High Resolution (4096×3072)", 100, 4096, 3072),
    UHD_4K("4K UHD", "4K Ultra HD (3840×2160)", 100, 3840, 2160),
    HD_12MP("12MP", "12MP Standard (3264×2448)", 98, 3264, 2448)
}

/**
 * Optional On-Device AI Super-Resolution Upscale Mode.
 * Always saves the unchanged original JPEG first, and when 2x or 4x is selected,
 * generates a separate `_AI2x.jpg` or `_AI4x.jpg` file on `Dispatchers.Default`.
 */
enum class AiUpscaleMode(
    val shortLabel: String,
    val fullTitle: String,
    val scaleFactor: Int,
    val fileSuffix: String
) {
    ORIGINAL("Original", "Original (Unchanged Native JPEG)", 1, ""),
    UPSCALE_2X("AI Upscale 2x", "Original + Separate 2x Super-Res (_AI2x.jpg)", 2, "_AI2x"),
    UPSCALE_4X("AI Upscale 4x", "Original + Separate 4x Super-Res (_AI4x.jpg)", 4, "_AI4x");

    val uiLabel: String get() = shortLabel
}

/**
 * Optical & Hardware Multi-Camera Zoom Profiles.
 * Uses neutral 1.0f multipliers so native captures are never smeared or artificially oversharpened.
 */
enum class ZoomOptimizationProfile(
    val badgeLabel: String,
    val sensorLabel: String,
    val pipelineSummary: String,
    val contrastMultiplier: Float,
    val saturationMultiplier: Float,
    val shadowLiftOffset: Float,
    val microDetailOverlayAlpha: Int,
    val microDetailContrast: Float
) {
    ULTRAWIDE_12MP_0_6X(
        badgeLabel = "0.6x Ultra-Wide",
        sensorLabel = "Ultra-Wide Physical Lens",
        pipelineSummary = "Optical ultra-wide lens • Native StreamConfigurationMap output",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    ),
    NATIVE_200MP_1X(
        badgeLabel = "1x Main Wide",
        sensorLabel = "Main Wide Sensor • OIS",
        pipelineSummary = "Unchanged native JPEG • FAST NR & Edge mode (no foliage smearing)",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    ),
    SENSOR_CROP_2X(
        badgeLabel = "2x Optical / In-Sensor",
        sensorLabel = "2x Optical / In-Sensor Zoom",
        pipelineSummary = "Hardware CameraControl.setZoomRatio(2.0x) without software crop-upscale",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    ),
    DEDICATED_3X_SUPER_RES(
        badgeLabel = "3x Telephoto",
        sensorLabel = "Physical Telephoto / Optical Zoom",
        pipelineSummary = "Physical telephoto lens (>2x) or optical CameraControl zoom",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    ),
    DIGITAL_RECONSTRUCTION_4X_5X(
        badgeLabel = "5x Telephoto",
        sensorLabel = "Physical Telephoto / Optical Zoom",
        pipelineSummary = "Physical telephoto lens routing + hardware zoom (no software center-crop)",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    ),
    NATURAL_TELEPHOTO_10X(
        badgeLabel = "10x Zoom",
        sensorLabel = "Telephoto Hardware Zoom",
        pipelineSummary = "Hardware telephoto zoom with natural edge & noise settings",
        contrastMultiplier = 1.0f,
        saturationMultiplier = 1.0f,
        shadowLiftOffset = 0f,
        microDetailOverlayAlpha = 0,
        microDetailContrast = 1.0f
    );

    companion object {
        fun fromZoomRatio(zoom: Float): ZoomOptimizationProfile {
            return when {
                zoom < 0.85f -> ULTRAWIDE_12MP_0_6X
                zoom < 1.55f -> NATIVE_200MP_1X
                zoom < 2.55f -> SENSOR_CROP_2X
                zoom < 3.65f -> DEDICATED_3X_SUPER_RES
                zoom < 7.50f -> DIGITAL_RECONSTRUCTION_4X_5X
                else -> NATURAL_TELEPHOTO_10X
            }
        }
    }
}

/**
 * Photo Capture File Formats (JPEG, RAW DNG + JPEG, Lossless PNG, High-Efficiency WebP/HEIF).
 */
enum class PhotoFormatSetting(
    val shortLabel: String,
    val fullTitle: String,
    val fileExtension: String,
    val mimeType: String
) {
    JPEG("JPG", "4K Fine JPEG (.JPG)", "jpg", "image/jpeg"),
    RAW_DNG("RAW+JPG", "Pro Uncompressed RAW (.DNG + 4K .JPG)", "dng", "image/x-adobe-dng"),
    PNG_LOSSLESS("PNG", "16-Bit Style Lossless PNG (.PNG)", "png", "image/png"),
    WEBP_LOSSLESS("HEIF/WEBP", "Lossless High-Efficiency (.WEBP)", "webp", "image/webp");

    val badgeLabel: String get() = shortLabel
    val extension: String get() = fileExtension
}

/**
 * Video Container & Codec Formats (MP4 AVC, MP4 HEVC, Cinema RAW MOV, Matroska MKV).
 */
enum class VideoFormatSetting(
    val shortLabel: String,
    val fullTitle: String,
    val fileExtension: String,
    val mimeType: String
) {
    MP4_AVC("MP4", "Standard MP4 (H.264 AVC)", "mp4", "video/mp4"),
    MP4_HEVC("HEVC MP4", "Pro 10-Bit Style HEVC (H.265 .MP4)", "mp4", "video/mp4"),
    RAW_CINEMA_MOV("RAW MOV", "Cinema RAW Uncompressed All-I (.MOV)", "mov", "video/quicktime"),
    MKV_MASTER("RAW MKV", "Studio Master Container (.MKV)", "mkv", "video/x-matroska");

    val badgeLabel: String get() = shortLabel
    val extension: String get() = fileExtension
}

enum class AspectRatioSetting(
    val label: String,
    val ratioValue: Float? // width / height in portrait or null for Full screen
) {
    RATIO_9_16("16:9 4K", 9f / 16f),
    RATIO_3_4("3:4", 3f / 4f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("FULL", null)
}

enum class VideoResolutionSetting(
    val shortLabel: String,
    val fullTitle: String,
    val widthPx: Int,
    val heightPx: Int
) {
    UHD_4K("4K UHD", "Ultra HD 4K (3840×2160)", 3840, 2160),
    FHD_1080P("FHD 1080p", "Full HD 1080p (1920×1080)", 1920, 1080)
}

enum class VideoFpsSetting(val fps: Int, val label: String) {
    FPS_30(30, "30fps"),
    FPS_60(60, "60fps")
}

/**
 * Cinema Log Color Profiles (Flat gamma curves for high dynamic range grading).
 */
enum class LogProfile(
    val shortCode: String,
    val fullTitle: String,
    val subtitle: String,
    val dynamicRangeStops: String,
    val contrastFactor: Float,
    val saturationFactor: Float,
    val shadowLift: Float,
    val warmthOffset: Float
) {
    STANDARD_REC709(
        shortCode = "Rec.709",
        fullTitle = "Standard Rec.709",
        subtitle = "Natural contrast & color ready for sharing",
        dynamicRangeStops = "11 Stops",
        contrastFactor = 1.0f,
        saturationFactor = 1.0f,
        shadowLift = 0f,
        warmthOffset = 0f
    ),
    GALAXY_LOG(
        shortCode = "G-Log",
        fullTitle = "Samsung Galaxy Log",
        subtitle = "10-bit style flat curve preserving highlights & deep shadows",
        dynamicRangeStops = "14+ Stops",
        contrastFactor = 0.76f,
        saturationFactor = 0.72f,
        shadowLift = 26f,
        warmthOffset = 0f
    ),
    SONY_SLOG3(
        shortCode = "S-Log3",
        fullTitle = "Cine S-Log3 / S-Gamut3",
        subtitle = "Wide latitude cinema log with lifted shadow floor",
        dynamicRangeStops = "15+ Stops",
        contrastFactor = 0.70f,
        saturationFactor = 0.65f,
        shadowLift = 34f,
        warmthOffset = -2f
    ),
    CANON_CLOG3(
        shortCode = "C-Log3",
        fullTitle = "Cinema C-Log3",
        subtitle = "Smooth midtone roll-off with natural skin tone preservation",
        dynamicRangeStops = "14 Stops",
        contrastFactor = 0.78f,
        saturationFactor = 0.74f,
        shadowLift = 22f,
        warmthOffset = 4f
    ),
    ARRI_LOGC4(
        shortCode = "LogC4",
        fullTitle = "ARRI Reveal LogC4",
        subtitle = "Motion-picture highlight retention & soft toe curve",
        dynamicRangeStops = "16 Stops",
        contrastFactor = 0.72f,
        saturationFactor = 0.68f,
        shadowLift = 30f,
        warmthOffset = 2f
    ),
    PANASONIC_VLOG(
        shortCode = "V-Log",
        fullTitle = "Varicam V-Log",
        subtitle = "Neutral color response ideal for travel & documentary LUTs",
        dynamicRangeStops = "14.5 Stops",
        contrastFactor = 0.74f,
        saturationFactor = 0.70f,
        shadowLift = 28f,
        warmthOffset = -1f
    ),
    RED_LOG3G10(
        shortCode = "Log3G10",
        fullTitle = "RED WideGamut Log3G10",
        subtitle = "Deep shadow recovery & cinematic highlight roll-off",
        dynamicRangeStops = "16+ Stops",
        contrastFactor = 0.75f,
        saturationFactor = 0.70f,
        shadowLift = 25f,
        warmthOffset = 3f
    ),
    APPLE_LOG(
        shortCode = "AppleLog",
        fullTitle = "ProRes Mobile Log",
        subtitle = "Modern mobile computational log curve with zero clipping",
        dynamicRangeStops = "13.5 Stops",
        contrastFactor = 0.77f,
        saturationFactor = 0.73f,
        shadowLift = 24f,
        warmthOffset = 0f
    ),
    FUJI_FLOG2(
        shortCode = "F-Log2",
        fullTitle = "Eterna F-Log2",
        subtitle = "Organic filmic latitude designed for 35mm film stocks",
        dynamicRangeStops = "14+ Stops",
        contrastFactor = 0.76f,
        saturationFactor = 0.75f,
        shadowLift = 24f,
        warmthOffset = 2f
    ),
    DJI_DLOG_M(
        shortCode = "D-Log M",
        fullTitle = "Aerial D-Log M",
        subtitle = "Optimized for bright skies, oceans, and landscape travel",
        dynamicRangeStops = "13 Stops",
        contrastFactor = 0.80f,
        saturationFactor = 0.78f,
        shadowLift = 20f,
        warmthOffset = -2f
    )
}

enum class LutCategory(val label: String) {
    ALL("ALL"),
    FILM("FILM STOCK"),
    TRAVEL("TRAVEL"),
    CINEMA("CINEMA"),
    MONO("B&W NOIR")
}

/**
 * 24 Built-in Film, Travel, Cinema & Monochrome LUTs usable live on the viewfinder and in the Photo Studio.
 */
enum class PhotoFilterPreset(
    val label: String,
    val category: LutCategory,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val brightness: Float,
    val rGain: Float = 1.0f,
    val gGain: Float = 1.0f,
    val bGain: Float = 1.0f
) {
    ORIGINAL("Original (No LUT)", LutCategory.ALL, 1.0f, 1.0f, 0f, 0f, 1.0f, 1.0f, 1.0f),

    // Film Stock LUTs
    KODAK_PORTRA_400("Kodak Portra 400", LutCategory.FILM, 1.06f, 1.12f, 14f, 6f, 1.05f, 1.01f, 0.94f),
    KODAK_GOLD_200("Kodak Gold 200", LutCategory.FILM, 1.10f, 1.18f, 22f, 8f, 1.08f, 1.02f, 0.88f),
    KODAK_EKTAR_100("Kodak Ektar 100", LutCategory.FILM, 1.18f, 1.35f, 8f, 2f, 1.07f, 0.99f, 0.95f),
    FUJI_VELVIA_50("Fuji Velvia 50", LutCategory.FILM, 1.20f, 1.40f, 2f, 0f, 1.03f, 1.06f, 1.04f),
    FUJI_SUPERIA_400("Fuji Superia 400", LutCategory.FILM, 1.08f, 1.10f, -6f, 4f, 0.96f, 1.05f, 1.02f),
    FUJI_PRO_400H("Fuji Pro 400H", LutCategory.FILM, 0.96f, 0.94f, -4f, 10f, 0.97f, 1.04f, 1.03f),
    CINESTILL_800T("CineStill 800T", LutCategory.FILM, 1.14f, 1.08f, -20f, -2f, 0.92f, 1.01f, 1.12f),
    POLAROID_600("Polaroid 600 Instant", LutCategory.FILM, 0.94f, 0.88f, 18f, 12f, 1.06f, 1.01f, 0.90f),
    VINTAGE_FILM("Analog 35mm Grain", LutCategory.FILM, 0.95f, 0.85f, 16f, 8f, 1.04f, 1.00f, 0.92f),

    // Travel & Destination LUTs
    KYOTO_STREET("Kyoto Street Neon", LutCategory.TRAVEL, 1.14f, 1.16f, -8f, 2f, 1.02f, 1.00f, 1.08f),
    SANTORINI_AEGEAN("Santorini Blue", LutCategory.TRAVEL, 1.12f, 1.26f, -10f, 8f, 0.95f, 1.03f, 1.12f),
    SAHARA_GOLDEN("Sahara Sunset", LutCategory.TRAVEL, 1.10f, 1.24f, 28f, 6f, 1.12f, 1.02f, 0.85f),
    NORDIC_FJORD("Nordic Fjord Slate", LutCategory.TRAVEL, 1.15f, 0.78f, -14f, -4f, 0.93f, 1.01f, 1.06f),
    BALI_TROPICAL("Bali Emerald Tropics", LutCategory.TRAVEL, 1.12f, 1.28f, 10f, 4f, 0.98f, 1.08f, 1.02f),
    SWISS_ALPINE("Swiss Alpine Crisp", LutCategory.TRAVEL, 1.16f, 1.14f, -6f, 6f, 0.98f, 1.02f, 1.07f),
    AMALFI_COAST("Amalfi Summer", LutCategory.TRAVEL, 1.04f, 1.20f, 20f, 9f, 1.07f, 1.03f, 0.92f),
    HAVANA_RETRO("Havana Sun Ochre", LutCategory.TRAVEL, 1.12f, 1.10f, 24f, 4f, 1.09f, 1.01f, 0.89f),

    // Cinema & Blockbuster LUTs (Disabled by default; optional in Settings)
    VIVID_GALAXY("Natural Vivid", LutCategory.CINEMA, 1.06f, 1.12f, 2f, 2f, 1.02f, 1.01f, 1.02f),
    CINEMA_TEAL("Teal & Orange Cine", LutCategory.CINEMA, 1.18f, 1.12f, -6f, -4f, 1.06f, 0.98f, 1.06f),
    COOL_CYBER("Cyberpunk 2099", LutCategory.CINEMA, 1.20f, 1.25f, -24f, 0f, 1.05f, 0.93f, 1.15f),
    WARM_AMBER("IMAX 70mm Gold", LutCategory.CINEMA, 1.22f, 1.10f, 22f, -2f, 1.09f, 1.01f, 0.88f),
    WES_PASTEL("Grand Pastel 35mm", LutCategory.CINEMA, 0.96f, 1.18f, 18f, 12f, 1.06f, 1.04f, 0.92f),

    // Monochrome / Noir LUTs
    CLASSIC_BW("Ilford HP5 400 B&W", LutCategory.MONO, 1.22f, 0.0f, 0f, 0f, 1.0f, 1.0f, 1.0f),
    LEICA_NOIR_BW("Leica Monochrom Noir", LutCategory.MONO, 1.38f, 0.0f, 0f, -8f, 1.05f, 1.05f, 1.05f)
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
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val whiteBalance: WhiteBalancePreset = WhiteBalancePreset.AUTO,
    val customKelvin: Int = 5500,
    val manualFocusDistance: Float? = null,
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
    val minZoomRatio: Float = 0.6f,
    val maxZoomRatio: Float = 10.0f,
    val sensorMaxZoomRatio: Float = 8.0f,
    val sensorMinZoomRatio: Float = 1.0f,
    val zoomPresets: List<Float> = listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f),
    val supportsUltraWideOptical: Boolean = true,
    val supports4K: Boolean = true,
    val supports60Fps: Boolean = true,
    val supportsVideoStabilization: Boolean = true,
    val hasOpticalImageStabilization: Boolean = true,
    val supportsManualSensor: Boolean = true,
    val supportsRawCapability: Boolean = false,
    val physicalCameraIds: List<String> = emptyList(),
    val telephotoPhysicalCameraId: String? = null,
    val ultraWidePhysicalCameraId: String? = null,
    val maxNativeJpegWidth: Int = 4096,
    val maxNativeJpegHeight: Int = 3072,
    val minExposureIndex: Int = -8,
    val maxExposureIndex: Int = 8,
    val exposureStepEv: Float = 0.25f,
    val minFocusDistanceDiopters: Float = 10.0f,
    val hardwareLevelLabel: String = "LEVEL_3",
    val mainSensorSpec: String = "Main Wide Camera • OIS",
    val ultraWideSensorSpec: String = "Ultra-Wide Camera",
    val socPlatformSpec: String = "Honor / MagicOS Camera2 HAL",
    val osPlatformSpec: String = "Android 14+ / MagicOS"
)

enum class SceneCategory(
    val displayTitle: String,
    val subtitle: String,
    val saturationBoost: Float,
    val contrastBoost: Float,
    val warmthShift: Float
) {
    AUTO("Neutral Auto", "Unchanged Native Sensor Color", 1.0f, 1.0f, 0f),
    NIGHT_LOW_LIGHT("Night Scene", "Multi-frame Shadow & Noise Optimization", 1.04f, 1.04f, 2f),
    PORTRAIT_SUBJECT("Portrait Studio", "Natural Skin Tone & Depth Separation", 1.02f, 1.01f, 3f),
    LANDSCAPE_NATURE("Landscape", "Natural Sky & Foliage Detail", 1.04f, 1.03f, 0f),
    FOOD_CULINARY("Culinary", "Natural Warmth & Crisp Texture", 1.06f, 1.04f, 6f),
    SUNSET_GOLDEN("Golden Hour", "Amber Horizon & Highlight Recovery", 1.06f, 1.03f, 8f),
    HIGH_CONTRAST_HDR("Backlit HDR", "Dynamic Shadow Lift & Highlight Guard", 1.03f, 1.06f, 0f),
    MACRO_DETAIL("Close-Up Detail", "Micro-Contrast & Edge Crispness", 1.03f, 1.04f, 0f)
}

data class RealTimeSceneMetrics(
    val detectedScene: SceneCategory = SceneCategory.AUTO,
    val averageLuminance: Float = 128f,
    val dynamicRangeRatio: Float = 0.5f,
    val colorWarmthScore: Float = 0.0f,
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

data class DiagnosticLogEntry(
    val id: Long,
    val timestamp: String,
    val tag: String,
    val message: String
)

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
        get() = if (width > 0 && height > 0) "${width}×${height}" else "Native"

    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024f * 1024f)
            return if (mb >= 1f) String.format("%.1f MB", mb)
            else String.format("%d KB", (sizeBytes / 1024L).coerceAtLeast(1L))
        }

    val variantBadge: String
        get() = when {
            isVideo -> "VIDEO"
            displayName.contains("_AI4x", ignoreCase = true) -> "AI UPSCALE 4x"
            displayName.contains("_AI2x", ignoreCase = true) -> "AI UPSCALE 2x"
            displayName.endsWith(".dng", ignoreCase = true) -> "RAW DNG"
            else -> "ORIGINAL"
        }
}

enum class CropAspectPreset(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    WIDE_16_9("16:9 4K", 16f / 9f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    SQUARE_1_1("1:1", 1f)
}

data class PhotoEditState(
    val filter: PhotoFilterPreset = PhotoFilterPreset.ORIGINAL,
    val logProfile: LogProfile = LogProfile.STANDARD_REC709,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val warmth: Float = 0f,
    val vignette: Float = 0.15f,
    val rotationDegrees: Int = 0,
    val cropAspect: CropAspectPreset = CropAspectPreset.ORIGINAL,
    val showGalaxyWatermark: Boolean = true
)
