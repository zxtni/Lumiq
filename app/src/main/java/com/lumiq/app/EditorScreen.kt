/*
 * Copyright (c) 2026 zxtni
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lumiq.app

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumiq.app.viewmodel.AdjustmentType
import com.lumiq.app.viewmodel.EditorViewModel
import com.lumiq.app.viewmodel.Tool

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@Composable
fun EditorScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Load initial URI if provided and not loaded yet
    androidx.compose.runtime.LaunchedEffect(initialUri) {
         if (initialUri != null && uiState.image == null && !uiState.isLoading) {
             // Basic Load
              val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, initialUri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, initialUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> 
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            }
            viewModel.setImage(bitmap)
         }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Load bitmap logic... (Duplicated for now, ideal refactor to helper function)
             val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> 
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            }
            viewModel.setImage(bitmap)
        }
    }

    // Toast Effect
    androidx.compose.runtime.LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
             android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { viewModel.undo() },
                    onLongPress = { viewModel.redo() }
                )
            }
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = "Import",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { 
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        ) 
                    }
            )
            
            // App Title in center for "Premium" feel? Or empty?
            // "LUMIQ" text is requested to be "Large titles, thin labels".
            // Maybe just stick to minimal icons as requested "Icons only by default" (though that was bottom rail).
            
            Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = "Save",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { 
                        viewModel.saveImage(context)
                    }
            )
        }

        // 1. Main Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, bottom = 120.dp), // Space for tools and header
            contentAlignment = Alignment.Center
        ) {
            uiState.image?.let { bitmap ->
                // Apply color matrix for preview
                // Brightness: [1, 0, 0, 0, brightness]
                // Contrast: [contrast, 0, 0, 0, (1-contrast)*128] (Simplified)
                // Saturation: Standard matrix
                
                val brightness = uiState.adjustments.brightness * 255f
                val contrast = uiState.adjustments.contrast
                val contrastOffset = (1f - contrast) * 128f
                
                // 1. Brightness & Contrast Matrix
                // R' = R * c + c_off + b
                // 1. Brightness & Contrast Matrix
                // R' = R * c + c_off + b
                val cmB = android.graphics.ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, contrastOffset + brightness,
                    0f, contrast, 0f, 0f, contrastOffset + brightness,
                    0f, 0f, contrast, 0f, contrastOffset + brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                // 2. Saturation
                val cmS = android.graphics.ColorMatrix()
                cmS.setSaturation(uiState.adjustments.saturation)
                
                // 3. Warmth (Calculated simplified)
                // Warmth adds Red/Yellow, Cool adds Blue
                val warmth = uiState.adjustments.warmth
                val rW = if (warmth > 0) warmth * 50f else 0f
                val bW = if (warmth < 0) -warmth * 50f else 0f
                val cmW = android.graphics.ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, rW,
                    0f, 1f, 0f, 0f, (rW * 0.5f), // Yellowish
                    0f, 0f, 1f, 0f, bW,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                // Combine: S * B * W (Order usually: Brightness/Contrast -> Saturation -> Tint)
                // Matrix M = S * B * W
                val combinedAndroid = android.graphics.ColorMatrix()
                combinedAndroid.set(cmS)
                combinedAndroid.postConcat(cmB)
                combinedAndroid.postConcat(cmW)
                
                val combined = ColorMatrix(combinedAndroid.array)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Editing Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .rotate(uiState.adjustments.rotation),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(combined)
                    )
                    
                    // CROP OVERLAY
                    // For MVP simplicity: We will just overlay a box that changes strictly *visual* crop state
                    // In a production app, the crop logic with rotation is complex relative reference frames.
                    // We will implement a simplified center-focused Crop Overlay that updates the ViewModel values.
                    
                    if (uiState.activeTool == Tool.Crop) {
                         CropOverlay(
                             currentLeft = uiState.adjustments.cropLeft,
                             currentTop = uiState.adjustments.cropTop,
                             currentRight = uiState.adjustments.cropRight,
                             currentBottom = uiState.adjustments.cropBottom,
                             onCropChange = { l, t, r, b ->
                                 viewModel.updateCrop(l, t, r, b)
                             }
                         )
                    }
                }
            }
        }

        // 2. Bottom Tool Rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            
            // Adjustment Panel (Expands upwards)
            AnimatedVisibility(
                visible = uiState.activeTool == Tool.Adjust,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AdjustmentPanel(
                    activeType = uiState.activeAdjustment,
                    adjustments = uiState.adjustments,
                    onSelectType = { viewModel.selectAdjustment(it) },
                    onValueChange = { viewModel.updateAdjustmentValue(it) }
                )
            }

            // Main Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolButton(
                    icon = Icons.Rounded.Crop,
                    label = "Crop",
                    isActive = uiState.activeTool == Tool.Crop,
                    onClick = { viewModel.selectTool(Tool.Crop) }
                )
                ToolButton(
                    icon = Icons.Rounded.RotateRight,
                    label = "Rotate",
                    isActive = uiState.activeTool == Tool.Rotate,
                    onClick = { viewModel.selectTool(Tool.Rotate) }
                )
                ToolButton(
                    icon = Icons.Rounded.Tune,
                    label = "Tune",
                    isActive = uiState.activeTool == Tool.Adjust,
                    onClick = { viewModel.selectTool(Tool.Adjust) }
                )
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val tint by androidx.compose.animation.animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), label = "toolTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

@Composable
fun AdjustmentPanel(
    activeType: AdjustmentType?,
    adjustments: com.lumiq.app.viewmodel.AdjustmentState,
    onSelectType: (AdjustmentType) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.Black.copy(alpha = 0.3f)) // Glassy overlay effect
            .padding(16.dp)
    ) {
        // Slider for active adjustment
        if (activeType != null) {
            val value = when(activeType) {
                AdjustmentType.Brightness -> adjustments.brightness
                AdjustmentType.Contrast -> adjustments.contrast
                AdjustmentType.Saturation -> adjustments.saturation
                AdjustmentType.Warmth -> adjustments.warmth
            }
            
            val range = when(activeType) {
                AdjustmentType.Brightness -> -1f..1f
                AdjustmentType.Contrast -> 0.5f..1.5f
                AdjustmentType.Saturation -> 0f..2f
                AdjustmentType.Warmth -> -1f..1f
            }

            // Custom minimal slider
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adjustment Types Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AdjustmentIcon(
                icon = Icons.Rounded.Brightness6,
                isActive = activeType == AdjustmentType.Brightness,
                onClick = { onSelectType(AdjustmentType.Brightness) }
            )
            AdjustmentIcon(
                icon = Icons.Rounded.Contrast,
                isActive = activeType == AdjustmentType.Contrast,
                onClick = { onSelectType(AdjustmentType.Contrast) }
            )
            AdjustmentIcon(
                icon = Icons.Rounded.InvertColors, // Saturation
                isActive = activeType == AdjustmentType.Saturation,
                onClick = { onSelectType(AdjustmentType.Saturation) }
            )
            AdjustmentIcon(
                icon = Icons.Rounded.Thermostat, // Warmth
                isActive = activeType == AdjustmentType.Warmth,
                onClick = { onSelectType(AdjustmentType.Warmth) }
            )
        }
    }
}

@Composable
fun AdjustmentIcon(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface 

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
