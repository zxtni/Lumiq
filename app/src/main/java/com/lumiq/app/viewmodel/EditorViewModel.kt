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
package com.lumiq.app.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Core Tools
enum class Tool {
    None, Crop, Rotate, Adjust
}

// Adjustment Types
enum class AdjustmentType {
    Brightness, Contrast, Saturation, Warmth
}

data class AdjustmentState(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val rotation: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

data class EditorUiState(
    val image: Bitmap? = null,
    val isLoading: Boolean = false,
    val activeTool: Tool = Tool.None,
    val activeAdjustment: AdjustmentType? = null,
    val adjustments: AdjustmentState = AdjustmentState(),
    val saveMessage: String? = null
)

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<AdjustmentState>()
    private val redoStack = ArrayDeque<AdjustmentState>()

    init {
        // Placeholder setup...
        val width = 800
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.DKGRAY)
        _uiState.update { it.copy(image = bitmap) }
    }

    fun setImage(bitmap: Bitmap) {
        _uiState.update { it.copy(image = bitmap, adjustments = AdjustmentState()) }
        undoStack.clear()
        redoStack.clear()
    }

    fun selectTool(tool: Tool) {
        if (tool == Tool.Rotate) {
             captureStateForUndo()
             _uiState.update { 
                 val currentRotation = it.adjustments.rotation
                 val newRotation = (currentRotation + 90f) % 360f
                 it.copy(adjustments = it.adjustments.copy(rotation = newRotation))
             }
             return
        }
        _uiState.update { 
            it.copy(
                activeTool = if (it.activeTool == tool) Tool.None else tool,
                activeAdjustment = if (tool == Tool.Adjust && it.activeAdjustment == null) AdjustmentType.Brightness else null
            ) 
        }
    }

    fun updateCrop(left: Float, top: Float, right: Float, bottom: Float) {
        _uiState.update { 
            it.copy(adjustments = it.adjustments.copy(
                cropLeft = left.coerceIn(0f, 1f),
                cropTop = top.coerceIn(0f, 1f),
                cropRight = right.coerceIn(0f, 1f),
                cropBottom = bottom.coerceIn(0f, 1f)
            )) 
        }
    }

    fun selectAdjustment(type: AdjustmentType) {
        _uiState.update { it.copy(activeAdjustment = type) }
    }

    fun updateAdjustmentValue(value: Float) {
        val type = uiState.value.activeAdjustment ?: return
        _uiState.update { state ->
            val newAdjustments = when (type) {
                AdjustmentType.Brightness -> state.adjustments.copy(brightness = value)
                AdjustmentType.Contrast -> state.adjustments.copy(contrast = value)
                AdjustmentType.Saturation -> state.adjustments.copy(saturation = value)
                AdjustmentType.Warmth -> state.adjustments.copy(warmth = value)
            }
            state.copy(adjustments = newAdjustments)
        }
    }
    
    fun captureStateForUndo() {
        val currentAdjustments = uiState.value.adjustments
        undoStack.addLast(currentAdjustments)
        redoStack.clear()
        if (undoStack.size > 20) undoStack.removeFirst()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = uiState.value.adjustments
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        _uiState.update { it.copy(adjustments = previous) }
    }
    
    fun redo() {
        if (redoStack.isEmpty()) return
        val current = uiState.value.adjustments
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        _uiState.update { it.copy(adjustments = next) }
    }

    fun saveImage(context: android.content.Context) {
        val currentState = uiState.value
        val originalBitmap = currentState.image ?: return
        
        // Run in background (Coroutine)
        // For simple MVP we just do it here but wrap in thread
        Thread {
            try {
                // 1. Apply Adjustments
                val adj = currentState.adjustments
                
                // Rotation
                val matrix = android.graphics.Matrix()
                matrix.postRotate(adj.rotation)
                
                val rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )
                
                // Crop
                val w = rotatedBitmap.width
                val h = rotatedBitmap.height
                val cropX = (adj.cropLeft * w).toInt()
                val cropY = (adj.cropTop * h).toInt()
                val cropW = ((adj.cropRight - adj.cropLeft) * w).toInt().coerceAtLeast(1)
                val cropH = ((adj.cropBottom - adj.cropTop) * h).toInt().coerceAtLeast(1)
                
                val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropW, cropH)
                
                // Color Filter
                val finalBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(finalBitmap)
                val paint = android.graphics.Paint()
                
                val brightness = adj.brightness * 255f
                val contrast = adj.contrast
                val contrastOffset = (1f - contrast) * 128f
                val cmB = android.graphics.ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, contrastOffset + brightness,
                    0f, contrast, 0f, 0f, contrastOffset + brightness,
                    0f, 0f, contrast, 0f, contrastOffset + brightness,
                    0f, 0f, 0f, 1f, 0f
                ))
                val cmS = android.graphics.ColorMatrix()
                cmS.setSaturation(adj.saturation)
                
                val warmth = adj.warmth
                val rW = if (warmth > 0) warmth * 50f else 0f
                val bW = if (warmth < 0) -warmth * 50f else 0f
                val cmW = android.graphics.ColorMatrix(floatArrayOf(
                    1f, 0f, 0f, 0f, rW,
                    0f, 1f, 0f, 0f, (rW * 0.5f),
                    0f, 0f, 1f, 0f, bW,
                    0f, 0f, 0f, 1f, 0f
                ))
                
                val combined = android.graphics.ColorMatrix()
                combined.set(cmS)
                combined.postConcat(cmB)
                combined.postConcat(cmW)
                
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(combined)
                canvas.drawBitmap(croppedBitmap, 0f, 0f, paint)
                
                // Save to MediaStore
                val filename = "LUMIQ_${System.currentTimeMillis()}.jpg"
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LUMIQ")
                    }
                }
                
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                    _uiState.update { s -> s.copy(saveMessage = "Saved!") }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { s -> s.copy(saveMessage = "Error saving") }
            }
        }.start()
    }
}
