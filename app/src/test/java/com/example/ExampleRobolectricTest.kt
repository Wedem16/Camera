package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CameraMode
import com.example.model.PhotoFilterPreset
import com.example.model.PhotoResolutionSetting
import com.example.model.TimerSetting
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
    fun `cameraViewModel switches shooting modes and updates Pro settings`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        // Default mode is PHOTO
        assertEquals(CameraMode.PHOTO, viewModel.uiState.value.activeMode)

        // Switch to PRO mode
        viewModel.selectMode(CameraMode.PRO)
        assertEquals(CameraMode.PRO, viewModel.uiState.value.activeMode)

        // Update Pro ISO, Shutter, WB, and Manual Focus
        viewModel.updateProIso(800)
        viewModel.updateProShutter(4_000_000L) // 1/250s
        viewModel.updateProWhiteBalance(WhiteBalancePreset.MANUAL_KELVIN, 4800)
        viewModel.updateProManualFocus(0.5f)

        val pro = viewModel.uiState.value.proSettings
        assertEquals(800, pro.iso)
        assertEquals("1/250", pro.formattedShutter)
        assertEquals("4800K", pro.formattedWb)
        assertEquals(0.5f, pro.manualFocusDistance)

        // Reset Pro settings back to Auto
        viewModel.resetProSettings()
        assertEquals("AUTO", viewModel.uiState.value.proSettings.formattedIso)
    }

    @Test
    fun `cameraViewModel cycles timer, photo resolution, and gallery editor state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = CameraViewModel(app)

        // Cycle timer: OFF -> 3s -> 5s -> 10s -> OFF
        assertEquals(TimerSetting.OFF, viewModel.uiState.value.timerSetting)
        viewModel.cycleTimerSetting()
        assertEquals(TimerSetting.SEC_3, viewModel.uiState.value.timerSetting)
        viewModel.cycleTimerSetting()
        assertEquals(TimerSetting.SEC_5, viewModel.uiState.value.timerSetting)

        // Toggle photo resolution between UHD 50MP and HD 12MP
        assertEquals(PhotoResolutionSetting.ULTRA_HD_50MP, viewModel.uiState.value.photoResolution)
        viewModel.togglePhotoResolution()
        assertEquals(PhotoResolutionSetting.HD_12MP, viewModel.uiState.value.photoResolution)

        // Navigate to Gallery and back
        viewModel.openGallery()
        assertEquals(ActiveAppScreen.GALLERY_VIEWER, viewModel.uiState.value.activeScreen)
        viewModel.closeGalleryToCamera()
        assertEquals(ActiveAppScreen.CAMERA_VIEWFINDER, viewModel.uiState.value.activeScreen)

        // Update photo editor filter & sliders
        viewModel.updateEditFilter(PhotoFilterPreset.VIVID_GALAXY)
        viewModel.updateEditBrightness(20f)
        viewModel.rotateEditedPhoto90()
        assertEquals(PhotoFilterPreset.VIVID_GALAXY, viewModel.uiState.value.photoEditState.filter)
        assertEquals(20f, viewModel.uiState.value.photoEditState.brightness, 0.01f)
        assertEquals(90, viewModel.uiState.value.photoEditState.rotationDegrees)

        // Toggle AI Scene Optimizer
        assertTrue(viewModel.uiState.value.aiSceneOptimizerEnabled)
        viewModel.toggleAiSceneOptimizer()
        assertFalse(viewModel.uiState.value.aiSceneOptimizerEnabled)
    }
}
