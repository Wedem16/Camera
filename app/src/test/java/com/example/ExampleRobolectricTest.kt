package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CameraMode
import com.example.model.LogProfile
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoResolutionSetting
import com.example.model.TimerSetting
import com.example.model.VideoResolutionSetting
import com.example.model.WhiteBalancePreset
import com.example.viewmodel.ActiveAppScreen
import com.example.viewmodel.CameraViewModel
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
    fun `read app_name from context matches Galaxy HD Camera`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Galaxy HD Camera", appName)
    }

    @Test
    fun `cameraViewModel defaults to 4K UHD and supports up to 1000 Mbps bitrate plus LUTs and Log profiles`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        // Default Photo and Video resolution are 4K UHD
        assertEquals(PhotoResolutionSetting.UHD_4K, viewModel.uiState.value.photoResolution)
        assertEquals(VideoResolutionSetting.UHD_4K, viewModel.uiState.value.videoResolution)

        // Set bitrate to 1000 Mbps
        viewModel.updateVideoBitrateMbps(1000)
        assertEquals(1000, viewModel.uiState.value.videoBitrateMbps)

        // Select Film & Travel LUTs and Cinema Log Profile
        viewModel.selectActiveLut(PhotoFilterPreset.KODAK_PORTRA_400)
        viewModel.selectLogProfile(LogProfile.SONY_SLOG3)
        assertEquals(PhotoFilterPreset.KODAK_PORTRA_400, viewModel.uiState.value.activeLut)
        assertEquals(LogProfile.SONY_SLOG3, viewModel.uiState.value.activeLogProfile)

        // Switch to PRO mode and update manual settings
        viewModel.selectMode(CameraMode.PRO)
        assertEquals(CameraMode.PRO, viewModel.uiState.value.activeMode)

        viewModel.updateProIso(800)
        viewModel.updateProShutter(4_000_000L) // 1/250s
        viewModel.updateProWhiteBalance(WhiteBalancePreset.MANUAL_KELVIN, 4800)
        viewModel.updateProManualFocus(0.5f)

        val pro = viewModel.uiState.value.proSettings
        assertEquals(800, pro.iso)
        assertEquals("1/250", pro.formattedShutter)
        assertEquals("4800K", pro.formattedWb)
        assertEquals(0.5f, pro.manualFocusDistance)

        // Verify diagnostic logs recorded the events
        assertTrue(viewModel.uiState.value.diagnosticLogs.isNotEmpty())
    }

    @Test
    fun `cameraViewModel cycles timer, photo resolution, and gallery editor state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        assertEquals(TimerSetting.OFF, viewModel.uiState.value.timerSetting)
        viewModel.cycleTimerSetting()
        assertEquals(TimerSetting.SEC_3, viewModel.uiState.value.timerSetting)

        // Cycle photo resolution: UHD_4K -> ULTRA_HD_50MP -> HD_12MP
        assertEquals(PhotoResolutionSetting.UHD_4K, viewModel.uiState.value.photoResolution)
        viewModel.togglePhotoResolution()
        assertEquals(PhotoResolutionSetting.ULTRA_HD_50MP, viewModel.uiState.value.photoResolution)

        viewModel.openGallery()
        assertEquals(ActiveAppScreen.GALLERY_VIEWER, viewModel.uiState.value.activeScreen)
        viewModel.closeGalleryToCamera()
        assertEquals(ActiveAppScreen.CAMERA_VIEWFINDER, viewModel.uiState.value.activeScreen)

        viewModel.updateEditFilter(PhotoFilterPreset.SANTORINI_AEGEAN)
        viewModel.updateEditLogProfile(LogProfile.ARRI_LOGC4)
        viewModel.updateEditBrightness(20f)
        viewModel.rotateEditedPhoto90()
        assertEquals(PhotoFilterPreset.SANTORINI_AEGEAN, viewModel.uiState.value.photoEditState.filter)
        assertEquals(LogProfile.ARRI_LOGC4, viewModel.uiState.value.photoEditState.logProfile)
        assertEquals(20f, viewModel.uiState.value.photoEditState.brightness, 0.01f)
        assertEquals(90, viewModel.uiState.value.photoEditState.rotationDegrees)

        assertTrue(viewModel.uiState.value.aiSceneOptimizerEnabled)
        viewModel.toggleAiSceneOptimizer()
        assertFalse(viewModel.uiState.value.aiSceneOptimizerEnabled)
    }
}
