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
            listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f),
            caps.zoomPresets
        )

        viewModel.setZoomRatio(0.6f)
        assertEquals(ZoomOptimizationProfile.ULTRAWIDE_12MP_0_6X, viewModel.uiState.value.activeZoomProfile)

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

        // Opt-in creative filters toggle
        viewModel.toggleCreativeFilters()
        assertTrue(viewModel.uiState.value.creativeFiltersEnabled)
        viewModel.selectActiveLut(PhotoFilterPreset.KODAK_PORTRA_400)
        assertEquals(PhotoFilterPreset.KODAK_PORTRA_400, viewModel.uiState.value.activeLut)
        viewModel.toggleCreativeFilters()
        assertFalse(viewModel.uiState.value.creativeFiltersEnabled)
        assertEquals(PhotoFilterPreset.ORIGINAL, viewModel.uiState.value.activeLut)

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

        // 2. 4K / 60fps with 400 Mbps input boosts by 1.6x (640 Mbps) for pro-grade data density
        val config4k60High = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.UHD_4K,
            videoFps = com.example.model.VideoFpsSetting.FPS_60,
            requestedBitrateMbps = 400
        )
        assertEquals(640, config4k60High.effectiveBitrateMbps)
        assertEquals(640_000_000, config4k60High.targetEncodingBitrateBps)

        // 3. 1080p / 30fps prioritizes Quality.FHD first and preserves standard bitrate without 4K60 boost
        val config1080p30 = com.example.data.CameraHardwareController.resolveVideoQualityAndBitrate(
            cameraInfo = null,
            videoResolution = VideoResolutionSetting.FHD_1080P,
            videoFps = com.example.model.VideoFpsSetting.FPS_30,
            requestedBitrateMbps = 80
        )
        assertFalse(config1080p30.is4k60HighDensityMode)
        assertEquals(androidx.camera.video.Quality.FHD, config1080p30.prioritizedQualityOrder.first())
        assertEquals(80, config1080p30.effectiveBitrateMbps)
    }
}
