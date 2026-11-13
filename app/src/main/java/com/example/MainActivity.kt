package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.configureAppCheck
import com.example.ui.screens.CameraViewfinderScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.GalaxyCameraTheme
import com.example.viewmodel.ActiveAppScreen
import com.example.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge full-bleed camera display
        enableEdgeToEdge()

        setContent {
            GalaxyCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    GalaxyCameraAppRoot(viewModel = cameraViewModel)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (cameraViewModel.onVolumeKeyShutterPressed()) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun GalaxyCameraAppRoot(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val camGranted = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audGranted = result[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(camGranted, audGranted)
    }

    val cameraOnlyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { camGranted ->
        val audGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(camGranted, audGranted)
    }

    LaunchedEffect(Unit) {
        val camGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val audGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(camGranted, audGranted)
    }

    when (state.activeScreen) {
        ActiveAppScreen.CAMERA_VIEWFINDER -> {
            CameraViewfinderScreen(
                state = state,
                viewModel = viewModel,
                onRequestCameraAndAudioPermissions = {
                    multiPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                },
                onRequestCameraOnlyPermission = {
                    cameraOnlyLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }

        ActiveAppScreen.GALLERY_VIEWER -> {
            GalleryScreen(
                state = state,
                onSelectMedia = viewModel::selectMediaItem,
                onOpenEditor = viewModel::openPhotoEditor,
                onShareSelected = viewModel::shareSelectedMediaItem,
                onDeleteSelected = viewModel::deleteSelectedMediaItem,
                onImportSamplePhoto = viewModel::importStudioSamplePhoto,
                onBackToCamera = viewModel::closeGalleryToCamera
            )
        }

        ActiveAppScreen.PHOTO_EDITOR -> {
            PhotoEditorScreen(
                state = state,
                viewModel = viewModel,
                onBackToGallery = viewModel::closePhotoEditor
            )
        }
    }
}
