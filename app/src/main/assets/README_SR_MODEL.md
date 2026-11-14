# On-Device Super-Resolution Model (`app/src/main/assets/`)

## Supported TFLite Model Files
Place a quantized or FP16 Real-ESRGAN / ESRGAN TensorFlow Lite model in this `app/src/main/assets/` directory with one of the following names:
- `realesrgan_x2.tflite` (2x Super-Resolution)
- `realesrgan_x4.tflite` (4x Super-Resolution)

### Where to Download
1. **Qualcomm AI Hub (Snapdragon 7 Gen 4 / Honor 600 NPU & GPU optimized)**:
   - https://aihub.qualcomm.com/models/real_esrgan_general_x4v3
   - https://aihub.qualcomm.com/models/esrgan
2. **Kaggle Models / TensorFlow Hub (ESRGAN-TF2 TFLite)**:
   - https://www.kaggle.com/models/kaggle/esrgan-tf2/tfLite

## Built-In Tiled Fallback
When no `.tflite` binary is present in `assets/` (to avoid bloating the Git repository), `OnDeviceSuperResolutionEngine` automatically uses its **Tiled Lanczos-3 Resampling + Mild Edge-Preserving Unsharp Mask** pipeline on `Dispatchers.Default`, processing the image in 512×512 tiles with 16px overlap to prevent OutOfMemoryErrors on high-resolution sensors.
