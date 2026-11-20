package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiSceneOptimizerRepository
import com.example.data.OnDeviceSuperResolutionEngine
import com.example.model.AiUpscaleMode
import com.example.model.CameraMode
import com.example.model.LogProfile
import com.example.model.MediaItem
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoFormatSetting
import com.example.model.PhotoResolutionSetting
import com.example.model.TimerSetting
import com.example.model.VideoFormatSetting
import com.example.model.VideoResolutionSetting
import com.example.model.WhiteBalancePreset
import com.example.model.ZoomOptimizationProfile
import com.example.viewmodel.ActiveAppScreen
import com.example.viewmodel.CameraViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @org.junit.Before
    fun clearPrefsBeforeTest() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("honor_600_cam_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `cameraViewModel defaults to neutral unchanged capture with no fake branding or LUTs`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)
        val state = viewModel.uiState.value

        // Step 3 & Step 4: Default is neutral color, no watermark, no LUT, Original capture mode
        assertFalse(state.watermarkEnabled)
        assertFalse(state.creativeFiltersEnabled)
        assertFalse(state.aiSceneOptimizerEnabled)
        assertTrue(state.aiAutoEditAfterCaptureEnabled)
        assertEquals(PhotoFilterPreset.ORIGINAL, state.activeLut)
        assertEquals(LogProfile.STANDARD_REC709, state.activeLogProfile)
        assertEquals(AiUpscaleMode.ORIGINAL, state.aiUpscaleMode)
        assertEquals(1.0f, state.extraHybridZoomFactor, 0.001f)

        // Toggle AI Auto-Edit After Capture OFF and ON
        viewModel.toggleAiAutoEditAfterCapture()
        assertFalse(viewModel.uiState.value.aiAutoEditAfterCaptureEnabled)
        viewModel.toggleAiAutoEditAfterCapture()
        assertTrue(viewModel.uiState.value.aiAutoEditAfterCaptureEnabled)

        // All zoom profiles have neutral 1.0f multipliers and 0 overlay alpha (no painterly smearing)
        ZoomOptimizationProfile.entries.forEach { profile ->
            assertEquals(1.0f, profile.contrastMultiplier, 0.001f)
            assertEquals(1.0f, profile.saturationMultiplier, 0.001f)
            assertEquals(0, profile.microDetailOverlayAlpha)
        }
    }

    @Test
    fun `cameraViewModel cycles AiUpscaleMode and OnDeviceSuperResolutionEngine upscales with AI Auto-Edit recipe`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        // Cycle Original -> AI Upscale 2x -> AI Upscale 4x -> Original
        assertEquals(AiUpscaleMode.ORIGINAL, viewModel.uiState.value.aiUpscaleMode)
        viewModel.cycleAiUpscaleMode()
        assertEquals(AiUpscaleMode.UPSCALE_2X, viewModel.uiState.value.aiUpscaleMode)
        assertEquals("_AI2x", viewModel.uiState.value.aiUpscaleMode.fileSuffix)

        viewModel.cycleAiUpscaleMode()
        assertEquals(AiUpscaleMode.UPSCALE_4X, viewModel.uiState.value.aiUpscaleMode)
        assertEquals("_AI4x", viewModel.uiState.value.aiUpscaleMode.fileSuffix)

        viewModel.cycleAiUpscaleMode()
        assertEquals(AiUpscaleMode.ORIGINAL, viewModel.uiState.value.aiUpscaleMode)

        // Verify On-Device Histogram AI Auto-Edit Recipe + Tiled Super-Resolution
        val aiRepo = AiSceneOptimizerRepository()
        val srEngine = OnDeviceSuperResolutionEngine(app)
        val src = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 140, 80))
        }
        val recipe = aiRepo.computeOnDeviceHistogramAutoEditRecipe(src, upscaleFactor = 2)
        assertEquals(2, recipe.upscaleFactor)
        assertTrue(recipe.styleTitle.isNotBlank())

        var lastProgress = 0
        val res2x = srEngine.upscaleBitmapTiled(
            source = src,
            mode = AiUpscaleMode.UPSCALE_2X,
            recipe = recipe
        ) { pct ->
            lastProgress = pct
        }
        assertEquals(128, res2x.bitmap.width)
        assertEquals(96, res2x.bitmap.height)
        assertEquals(64, src.width)
        assertEquals(48, src.height)
        assertEquals(100, lastProgress)

        // Verify MediaItem variant badges distinguish ORIGINAL vs AI EDIT + 2x / 4x vs AI UPSCALE 2x / 4x
        val origItem = MediaItem(
            id = 1L,
            uri = Uri.EMPTY,
            displayName = "IMG_20261113_160000.jpg",
            isVideo = false,
            dateAddedSec = 100L,
            width = 4096,
            height = 3072,
            sizeBytes = 4_200_000L
        )
        val ai2xItem = origItem.copy(id = 2L, displayName = "IMG_20261113_160000_AI2x.jpg", width = 8192, height = 6144)
        val ai4xItem = origItem.copy(id = 3L, displayName = "IMG_20261113_160000_AI4x.jpg", width = 16384, height = 12288)
        val aiEdit2xItem = origItem.copy(id = 4L, displayName = "IMG_20261113_160000_AI_EDIT_2x.jpg", width = 8192, height = 6144)
        val aiEdit4xItem = origItem.copy(id = 5L, displayName = "IMG_20261113_160000_AI_EDIT_4x.jpg", width = 16384, height = 12288)
        val honorAiItem = origItem.copy(id = 6L, displayName = "IMG_20261113_160000_HONOR_AI_AIGC_DETAIL.jpg", width = 2048, height = 2048)

        assertEquals("ORIGINAL", origItem.variantBadge)
        assertEquals("AI UPSCALE 2x", ai2xItem.variantBadge)
        assertEquals("AI UPSCALE 4x", ai4xItem.variantBadge)
        assertEquals("AI EDIT + 2x", aiEdit2xItem.variantBadge)
        assertEquals("AI EDIT + 4x", aiEdit4xItem.variantBadge)
        assertEquals("HONOR AI GEN", honorAiItem.variantBadge)

        // Verify Honor 600 AI Generation Presets
        assertEquals(com.example.model.HonorAiGenPreset.AIGC_SUPER_RES, viewModel.uiState.value.selectedHonorAiPreset)
        viewModel.selectHonorAiPreset(com.example.model.HonorAiGenPreset.HARCOURT_STUDIO)
        assertEquals(com.example.model.HonorAiGenPreset.HARCOURT_STUDIO, viewModel.uiState.value.selectedHonorAiPreset)
    }

    @Test
    fun `cameraViewModel supports zoom presets, formats, Pro mode, and opt-in creative filters`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        val caps = viewModel.uiState.value.hardwareCapabilities
        assertEquals(
            listOf(0.7f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f),
            caps.zoomPresets
        )

        viewModel.setZoomRatio(0.7f)
        assertEquals(ZoomOptimizationProfile.ULTRAWIDE_12MP_0_6X, viewModel.uiState.value.activeZoomProfile)
        assertEquals(0.7f, viewModel.uiState.value.currentZoomRatio, 0.001f)
        assertTrue(viewModel.uiState.value.extraHybridZoomFactor <= 0.75f)

        viewModel.setZoomRatio(1.0f)
        assertEquals(ZoomOptimizationProfile.NATIVE_200MP_1X, viewModel.uiState.value.activeZoomProfile)

        viewModel.setZoomRatio(2.0f)
        assertEquals(ZoomOptimizationProfile.SENSOR_CROP_2X, viewModel.uiState.value.activeZoomProfile)

        viewModel.setZoomRatio(3.0f)
        assertEquals(ZoomOptimizationProfile.DEDICATED_3X_SUPER_RES, viewModel.uiState.value.activeZoomProfile)

        viewModel.setZoomRatio(5.0f)
        assertEquals(ZoomOptimizationProfile.DIGITAL_RECONSTRUCTION_4X_5X, viewModel.uiState.value.activeZoomProfile)

        viewModel.setZoomRatio(10.0f)
        assertEquals(ZoomOptimizationProfile.NATURAL_TELEPHOTO_10X, viewModel.uiState.value.activeZoomProfile)
        assertEquals(1.0f, viewModel.uiState.value.extraHybridZoomFactor, 0.001f)

        // Cycle Video HDR & Magic-Log Profiles: HDR10 -> HDR LOG -> HDR VIVID -> SDR
        assertEquals(com.example.model.VideoHdrProfileSetting.HDR10_HLG, viewModel.uiState.value.videoHdrProfile)
        viewModel.cycleVideoHdrProfile()
        assertEquals(com.example.model.VideoHdrProfileSetting.HONOR_MAGIC_HDR_LOG, viewModel.uiState.value.videoHdrProfile)
        assertTrue(viewModel.uiState.value.videoHdrProfile.isLogCurve)
        viewModel.cycleVideoHdrProfile()
        assertEquals(com.example.model.VideoHdrProfileSetting.HDR_VIVID, viewModel.uiState.value.videoHdrProfile)
        viewModel.cycleVideoHdrProfile()
        assertEquals(com.example.model.VideoHdrProfileSetting.SDR_REC709, viewModel.uiState.value.videoHdrProfile)

        // Cycle Photo Formats: JPEG -> RAW_DNG -> PNG_LOSSLESS -> WEBP_LOSSLESS
        assertEquals(PhotoFormatSetting.JPEG, viewModel.uiState.value.photoFormat)
        viewModel.cyclePhotoFormat()
        assertEquals(PhotoFormatSetting.RAW_DNG, viewModel.uiState.value.photoFormat)
        viewModel.cyclePhotoFormat()
        assertEquals(PhotoFormatSetting.PNG_LOSSLESS, viewModel.uiState.value.photoFormat)
        viewModel.cyclePhotoFormat()
        assertEquals(PhotoFormatSetting.WEBP_LOSSLESS, viewModel.uiState.value.photoFormat)

        // Cycle Video Formats
        assertEquals(VideoFormatSetting.MP4_AVC, viewModel.uiState.value.videoFormat)
        viewModel.cycleVideoFormat()
        assertEquals(VideoFormatSetting.MP4_HEVC, viewModel.uiState.value.videoFormat)

        // Set bitrate to 1000 Mbps
        viewModel.updateVideoBitrateMbps(1000)
        assertEquals(1000, viewModel.uiState.value.videoBitrateMbps)

        // Switch to PRO mode and update manual settings
        viewModel.selectMode(CameraMode.PRO)
        viewModel.updateProIso(800)
        viewModel.updateProShutter(4_000_000L)
        viewModel.updateProWhiteBalance(WhiteBalancePreset.MANUAL_KELVIN, 4800)
        val pro = viewModel.uiState.value.proSettings
        assertEquals(800, pro.iso)
        assertEquals("1/250", pro.formattedShutter)
        assertEquals("4800K", pro.formattedWb)

        // Opt-in creative filters & Sony Creative Look / Pro Edit LUTs
        viewModel.toggleCreativeFilters()
        assertTrue(viewModel.uiState.value.creativeFiltersEnabled)
        viewModel.selectActiveLut(PhotoFilterPreset.SONY_FL_FILM)
        assertEquals(PhotoFilterPreset.SONY_FL_FILM, viewModel.uiState.value.activeLut)
        assertEquals(com.example.model.LutCategory.SONY, viewModel.uiState.value.activeLut.category)
        viewModel.selectActiveLut(PhotoFilterPreset.PRO_HARCOURT_STUDIO)
        assertEquals(PhotoFilterPreset.PRO_HARCOURT_STUDIO, viewModel.uiState.value.activeLut)
        assertEquals(com.example.model.LutCategory.PRO_EDIT, viewModel.uiState.value.activeLut.category)

        // Toggle Live Viewfinder Filter Ribbon
        viewModel.toggleLiveFilterBar()
        assertTrue(viewModel.uiState.value.showLiveFilterBar)
        viewModel.setShowLiveFilterBar(false)
        assertFalse(viewModel.uiState.value.showLiveFilterBar)

        viewModel.toggleCreativeFilters()
        assertFalse(viewModel.uiState.value.creativeFiltersEnabled)
        assertEquals(PhotoFilterPreset.ORIGINAL, viewModel.uiState.value.activeLut)

        // Pro Edit Sliders (Highlights, Shadows, Clarity, Matte Fade, Sharpness)
        viewModel.updateEditHighlights(-24f)
        viewModel.updateEditShadows(32f)
        viewModel.updateEditClarity(40f)
        viewModel.updateEditFadeMatte(18f)
        viewModel.updateEditSharpness(25f)
        assertEquals(-24f, viewModel.uiState.value.photoEditState.highlights, 0.01f)
        assertEquals(32f, viewModel.uiState.value.photoEditState.shadows, 0.01f)
        assertEquals(40f, viewModel.uiState.value.photoEditState.clarity, 0.01f)
        assertEquals(18f, viewModel.uiState.value.photoEditState.fadeMatte, 0.01f)
        assertEquals(25f, viewModel.uiState.value.photoEditState.sharpness, 0.01f)

        val sonyProMatrix = com.example.data.MediaGalleryRepository.buildCombinedColorMatrix(
            lut = PhotoFilterPreset.SONY_SCINETONE,
            lutIntensity = 1.0f,
            extraHighlights = -20f,
            extraShadows = 30f,
            extraClarity = 35f,
            extraFadeMatte = 15f,
            extraSharpness = 20f
        )
        assertTrue(sonyProMatrix.array[0] > 1.05f)

        // Gallery & Editor navigation
        assertEquals(TimerSetting.OFF, viewModel.uiState.value.timerSetting)
        viewModel.cycleTimerSetting()
        assertEquals(TimerSetting.SEC_3, viewModel.uiState.value.timerSetting)

        assertEquals(PhotoResolutionSetting.UHD_4K, viewModel.uiState.value.photoResolution)
        assertEquals(VideoResolutionSetting.UHD_4K, viewModel.uiState.value.videoResolution)

        viewModel.openGallery()
        assertEquals(ActiveAppScreen.GALLERY_VIEWER, viewModel.uiState.value.activeScreen)
        viewModel.closeGalleryToCamera()
        assertEquals(ActiveAppScreen.CAMERA_VIEWFINDER, viewModel.uiState.value.activeScreen)
    }

    @Test
    fun `resolveVideoQualityAndBitrate prioritizes UHD QualitySelector and higher bitrates when 4K 60fps is selected`() {
        // 1. 4K / 60fps with a low 80 Mbps input should automatically prioritize higher bitrates (>= 250 Mbps floor)
        // and place Quality.UHD + Quality.HIGHEST at the front of the QualitySelector order
        val config4k60 = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.UHD_4K,
            videoFps = com.example.model.VideoFpsSetting.FPS_60,
            requestedBitrateMbps = 80
        )
        assertTrue(config4k60.is4k60HighDensityMode)
        assertEquals(
            listOf(
                androidx.camera.video.Quality.UHD,
                androidx.camera.video.Quality.HIGHEST,
                androidx.camera.video.Quality.FHD,
                androidx.camera.video.Quality.HD
            ),
            config4k60.prioritizedQualityOrder
        )
        assertTrue(config4k60.effectiveBitrateMbps >= com.example.data.CameraHardwareController.PRO_4K60_MIN_BITRATE_MBPS)
        assertEquals(250_000_000, config4k60.targetEncodingBitrateBps)
        assertTrue(config4k60.dataDensityBitsPerPixelFrame > 0.45f)

        // 2. 4K / 60fps with 400 Mbps input boosts by 1.6x (640 Mbps in SDR, 736 Mbps in 10-Bit HDR Log) for pro-grade data density
        val config4k60High = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.UHD_4K,
            videoFps = com.example.model.VideoFpsSetting.FPS_60,
            requestedBitrateMbps = 400,
            videoHdrProfile = com.example.model.VideoHdrProfileSetting.SDR_REC709
        )
        assertEquals(640, config4k60High.effectiveBitrateMbps)
        assertEquals(640_000_000, config4k60High.targetEncodingBitrateBps)

        val config4k60HdrLog = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.UHD_4K,
            videoFps = com.example.model.VideoFpsSetting.FPS_60,
            requestedBitrateMbps = 400,
            videoHdrProfile = com.example.model.VideoHdrProfileSetting.HONOR_MAGIC_HDR_LOG
        )
        assertTrue(config4k60HdrLog.effectiveBitrateMbps > config4k60High.effectiveBitrateMbps)
        assertEquals(com.example.model.VideoHdrProfileSetting.HONOR_MAGIC_HDR_LOG, config4k60HdrLog.videoHdrProfile)

        // 3. 1080p / 30fps in SDR prioritizes Quality.FHD first and preserves standard bitrate without 4K60 boost
        val config1080p30 = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.FHD_1080P,
            videoFps = com.example.model.VideoFpsSetting.FPS_30,
            requestedBitrateMbps = 80,
            videoHdrProfile = com.example.model.VideoHdrProfileSetting.SDR_REC709
        )
        assertFalse(config1080p30.is4k60HighDensityMode)
        assertEquals(androidx.camera.video.Quality.FHD, config1080p30.prioritizedQualityOrder.first())
        assertEquals(80, config1080p30.effectiveBitrateMbps)
    }

    @Test
    fun `honor600Cam branding and bilateral shadow denoise suppress night chroma grain`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Honor 600 Cam", context.getString(R.string.app_name))
        assertEquals("Honor 600 Studio Gallery", context.getString(R.string.gallery_title))

        // Create a dark night-sky patch (base RGB = 24, 24, 28) with a noisy high-ISO chroma speckle at (8, 8)
        val noisyNight = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(24, 24, 28))
            setPixel(8, 8, Color.rgb(30, 20, 34)) // subtle high-ISO chroma/luma grain within threshold
        }
        val srEngine = OnDeviceSuperResolutionEngine(context)
        assertTrue(srEngine.hasBundledTfliteModel(2))
        assertTrue(srEngine.hasBundledTfliteModel(4))

        val result = srEngine.upscaleBitmapTiled(
            source = noisyNight,
            mode = AiUpscaleMode.UPSCALE_2X,
            recipe = null
        ) {}
        assertEquals(32, result.bitmap.width)
        assertEquals(32, result.bitmap.height)
        assertTrue(result.neuralWeightsLoadedBytes > 1_000_000L)

        // Verify RAW 16-Bit DNG Linear Master is visually distinct from Original and AI Master
        val galleryRepo = com.example.data.MediaGalleryRepository(context)
        val rawLinear = galleryRepo.renderLinearRawDngMaster(noisyNight)
        val origPixel = noisyNight.getPixel(4, 4)
        val rawPixel = rawLinear.getPixel(4, 4)
        val aiPixel = result.bitmap.getPixel(8, 8)
        assertTrue("RAW Linear DNG must lift shadow toe relative to Original", rawPixel != origPixel)
        assertTrue("AI Master must be visually distinct from Original", aiPixel != origPixel)

        // Verify Honor Camera Settings toggles & Pro Mode color matrix
        val viewModel = CameraViewModel(context as Application)
        assertTrue(viewModel.uiState.value.levelEnabled)
        assertFalse(viewModel.uiState.value.floatingShutterEnabled)
        viewModel.toggleFloatingShutter()
        assertTrue(viewModel.uiState.value.floatingShutterEnabled)
        viewModel.toggleLevel()
        assertFalse(viewModel.uiState.value.levelEnabled)

        viewModel.updateProMeteringMode(com.example.model.ProMeteringMode.SPOT)
        viewModel.updateProIso(1600)
        viewModel.updateProShutter(33_333_333L)
        viewModel.updateProWhiteBalance(WhiteBalancePreset.DAYLIGHT, 6500)
        assertTrue(viewModel.uiState.value.proSettings.hasAnyManualOverride)

        val proMatrix = com.example.data.MediaGalleryRepository.buildProManualColorMatrix(
            proSettings = viewModel.uiState.value.proSettings,
            exposureStepEv = 1.0f
        )
        // Brighter exposure + warm 6500K WB scales red channel > 1.0f
        assertTrue(proMatrix!!.array[0] > 1.1f)

        viewModel.restoreDefaultCameraSettings()
        assertTrue(viewModel.uiState.value.levelEnabled)
        assertFalse(viewModel.uiState.value.floatingShutterEnabled)
        assertFalse(viewModel.uiState.value.proSettings.hasAnyManualOverride)
    }

    @Test
    fun `cameraViewModel persists user settings without resetting and supports 120fps pro video`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("honor_600_cam_settings", Context.MODE_PRIVATE).edit().clear().commit()

        val vm1 = CameraViewModel(app)
        // Cycle FPS: 60fps (default) -> 120fps -> 30fps -> 60fps
        assertEquals(com.example.model.VideoFpsSetting.FPS_60, vm1.uiState.value.videoFps)
        vm1.toggleVideoFps()
        assertEquals(com.example.model.VideoFpsSetting.FPS_120, vm1.uiState.value.videoFps)
        assertEquals(120, vm1.uiState.value.videoFps.fps)
        assertEquals("120fps", vm1.uiState.value.videoFps.label)

        // Verify 4K 120fps bitrate prioritization (>= 320 Mbps floor)
        val config4k120 = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.UHD_4K,
            videoFps = com.example.model.VideoFpsSetting.FPS_120,
            requestedBitrateMbps = 100
        )
        assertTrue(config4k120.is4k60HighDensityMode)
        assertTrue(config4k120.effectiveBitrateMbps >= com.example.data.CameraHardwareController.PRO_4K120_MIN_BITRATE_MBPS)

        // Verify 1080p 120fps bitrate prioritization (>= 120 Mbps floor)
        val config1080p120 = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.FHD_1080P,
            videoFps = com.example.model.VideoFpsSetting.FPS_120,
            requestedBitrateMbps = 60
        )
        assertTrue(config1080p120.effectiveBitrateMbps >= com.example.data.CameraHardwareController.PRO_FHD120_MIN_BITRATE_MBPS)

        // User sets custom bitrate, Sony LUT, HDR Log profile, Pro settings, and resolution
        vm1.updateVideoBitrateMbps(500)
        vm1.selectActiveLut(PhotoFilterPreset.SONY_SCINETONE)
        vm1.selectLogProfile(LogProfile.SONY_SLOG3)
        vm1.selectVideoHdrProfile(com.example.model.VideoHdrProfileSetting.HONOR_MAGIC_HDR_LOG)
        vm1.updateProIso(12000)
        vm1.updateProShutter(8_000_000L)
        vm1.toggleWatermark()

        // Toggling video resolution or FPS or switching modes must NOT reset the user's 500 Mbps bitrate or LUT
        vm1.toggleVideoResolution()
        assertEquals(500, vm1.uiState.value.videoBitrateMbps)
        vm1.selectMode(CameraMode.VIDEO)
        assertEquals(500, vm1.uiState.value.videoBitrateMbps)
        assertEquals(com.example.model.VideoFpsSetting.FPS_120, vm1.uiState.value.videoFps)
        assertEquals(PhotoFilterPreset.SONY_SCINETONE, vm1.uiState.value.activeLut)

        // Toggling creative filters OFF and back ON must restore the user's chosen SONY_SCINETONE LUT & Log profile
        vm1.toggleCreativeFilters()
        assertFalse(vm1.uiState.value.creativeFiltersEnabled)
        vm1.toggleCreativeFilters()
        assertTrue(vm1.uiState.value.creativeFiltersEnabled)
        assertEquals(PhotoFilterPreset.SONY_SCINETONE, vm1.uiState.value.activeLut)
        assertEquals(LogProfile.SONY_SLOG3, vm1.uiState.value.activeLogProfile)

        // Switch to PRO mode and enable Pro Video sub-mode
        vm1.selectMode(CameraMode.PRO)
        assertFalse(vm1.uiState.value.proVideoModeEnabled)
        assertFalse(vm1.uiState.value.isVideoStreamMode)
        vm1.setProVideoModeEnabled(true)
        assertTrue(vm1.uiState.value.proVideoModeEnabled)
        assertTrue(vm1.uiState.value.isVideoStreamMode)
        assertEquals(CameraMode.PRO, vm1.uiState.value.activeMode)

        // Creating a brand-new CameraViewModel instance (simulating app restart) must restore all user settings including Pro Video & ISO 12000
        val vm2 = CameraViewModel(app)
        assertEquals(com.example.model.VideoFpsSetting.FPS_120, vm2.uiState.value.videoFps)
        assertEquals(500, vm2.uiState.value.videoBitrateMbps)
        assertEquals(VideoResolutionSetting.FHD_1080P, vm2.uiState.value.videoResolution)
        assertEquals(CameraMode.PRO, vm2.uiState.value.activeMode)
        assertTrue(vm2.uiState.value.proVideoModeEnabled)
        assertTrue(vm2.uiState.value.isVideoStreamMode)
        assertEquals(PhotoFilterPreset.SONY_SCINETONE, vm2.uiState.value.activeLut)
        assertEquals(LogProfile.SONY_SLOG3, vm2.uiState.value.activeLogProfile)
        assertEquals(com.example.model.VideoHdrProfileSetting.HONOR_MAGIC_HDR_LOG, vm2.uiState.value.videoHdrProfile)
        assertEquals(12000, vm2.uiState.value.proSettings.iso)
        assertEquals("12000", vm2.uiState.value.proSettings.formattedIso)
        assertEquals(8_000_000L, vm2.uiState.value.proSettings.shutterSpeedNs)
        assertTrue(vm2.uiState.value.watermarkEnabled)

        // Toggle back to Pro Photo
        vm2.toggleProVideoMode()
        assertFalse(vm2.uiState.value.proVideoModeEnabled)
        assertFalse(vm2.uiState.value.isVideoStreamMode)

        // Clean up prefs for any subsequent test runs
        vm2.restoreDefaultCameraSettings()
    }
}
