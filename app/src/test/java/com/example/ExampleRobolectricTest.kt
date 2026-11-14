package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
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
        assertEquals(PhotoFilterPreset.ORIGINAL, state.activeLut)
        assertEquals(LogProfile.STANDARD_REC709, state.activeLogProfile)
        assertEquals(AiUpscaleMode.ORIGINAL, state.aiUpscaleMode)
        assertEquals(1.0f, state.extraHybridZoomFactor, 0.001f)

        // All zoom profiles have neutral 1.0f multipliers and 0 overlay alpha (no painterly smearing)
        ZoomOptimizationProfile.entries.forEach { profile ->
            assertEquals(1.0f, profile.contrastMultiplier, 0.001f)
            assertEquals(1.0f, profile.saturationMultiplier, 0.001f)
            assertEquals(0, profile.microDetailOverlayAlpha)
        }
    }

    @Test
    fun `cameraViewModel cycles AiUpscaleMode and OnDeviceSuperResolutionEngine upscales 2x and 4x`() = runBlocking {
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

        // Verify tiled background super-resolution produces 2x dimensions and preserves source bitmap
        val srEngine = OnDeviceSuperResolutionEngine(app)
        val src = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(60, 140, 80))
        }
        var lastProgress = 0
        val res2x = srEngine.upscaleBitmapTiled(src, AiUpscaleMode.UPSCALE_2X) { pct ->
            lastProgress = pct
        }
        assertEquals(128, res2x.bitmap.width)
        assertEquals(96, res2x.bitmap.height)
        assertEquals(64, src.width)
        assertEquals(48, src.height)
        assertEquals(100, lastProgress)

        // Verify MediaItem variant badges distinguish ORIGINAL vs AI UPSCALE 2x / 4x
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

        assertEquals("ORIGINAL", origItem.variantBadge)
        assertEquals("AI UPSCALE 2x", ai2xItem.variantBadge)
        assertEquals("AI UPSCALE 4x", ai4xItem.variantBadge)
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
}
