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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun CropOverlay(
    currentLeft: Float,
    currentTop: Float,
    currentRight: Float,
    currentBottom: Float,
    onCropChange: (Float, Float, Float, Float) -> Unit
) {
    // We render a Box that fills the same area as the Image (which is fillMaxWidth, aspect fit).
    // The "crop rect" is drawn relative to THIS box size.
    // NOTE: This assumes the overlay matches image bounds exactly. 
    // In Box(ContentAlignment.Center) this usually works if aspect ratios match.
    // For MVP we assume the overlay covers the "work area".

    var isDragging by remember { mutableStateOf(false) }
    // Which corner/edge is being dragged?
    // 0: none, 1: TL, 2: TR, 3: BL, 4: BR
    var activeHandle by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Determine touch target (simple quadrants for MVP)
                        val w = size.width
                        val h = size.height
                        val cl = currentLeft * w
                        val ct = currentTop * h
                        val cr = currentRight * w
                        val cb = currentBottom * h
                        
                        val touchX = offset.x
                        val touchY = offset.y
                        
                        // Simple Euclidean distance to corners could be better,
                        // bit here we just check if we are closer to Top-Left than Bottom-Right etc.
                        // Actually let's assume dragging anywhere inside the quadrants pulls that corner
                        
                        // Refined: Check proximity to corners (threshold 40dp)
                        val threshold = 100f // generous touch area
                        
                        if (kotlin.math.hypot(touchX - cl, touchY - ct) < threshold) activeHandle = 1
                        else if (kotlin.math.hypot(touchX - cr, touchY - ct) < threshold) activeHandle = 2
                        else if (kotlin.math.hypot(touchX - cl, touchY - cb) < threshold) activeHandle = 3
                        else if (kotlin.math.hypot(touchX - cr, touchY - cb) < threshold) activeHandle = 4
                        else activeHandle = 0 // Ignore if not near corner
                        
                        isDragging = activeHandle != 0
                    },
                    onDragEnd = {
                        isDragging = false
                        activeHandle = 0
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (activeHandle != 0) {
                            val w = size.width
                            val h = size.height
                            if (w > 0 && h > 0) {
                                val dx = dragAmount.x / w
                                val dy = dragAmount.y / h
                                
                                var l = currentLeft
                                var t = currentTop
                                var r = currentRight
                                var b = currentBottom
                                
                                when (activeHandle) {
                                    1 -> { l += dx; t += dy }
                                    2 -> { r += dx; t += dy }
                                    3 -> { l += dx; b += dy }
                                    4 -> { r += dx; b += dy }
                                }
                                onCropChange(l, t, r, b)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Draw dimmed background outside crop
            // We draw 4 rectangles: Top, Bottom, Left-Middle, Right-Middle
            val cl = currentLeft * w
            val ct = currentTop * h
            val cr = currentRight * w
            val cb = currentBottom * h
            
            val dimColor = Color.Black.copy(alpha = 0.5f)
            
            // Top rect
            drawRect(color = dimColor, topLeft = Offset(0f, 0f), size = Size(w, ct))
            // Bottom rect
            drawRect(color = dimColor, topLeft = Offset(0f, cb), size = Size(w, h - cb))
            // Left rect (between top/bottom)
            drawRect(color = dimColor, topLeft = Offset(0f, ct), size = Size(cl, cb - ct))
            // Right rect
            drawRect(color = dimColor, topLeft = Offset(cr, ct), size = Size(w - cr, cb - ct))
            
            // Draw Frame
            val rectPath = Path().apply {
                addRect(Rect(cl, ct, cr, cb))
            }
            drawPath(path = rectPath, color = Color.White, style = Stroke(width = 2.dp.toPx()))
            
            // Draw Grid Lines (Thirds)
            val thirdW = (cr - cl) / 3f
            val thirdH = (cb - ct) / 3f
            
            // Verticals
            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(cl + thirdW, ct), end = Offset(cl + thirdW, cb), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(cl + thirdW * 2, ct), end = Offset(cl + thirdW * 2, cb), strokeWidth = 1.dp.toPx())
            
            // Horizontals
            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(cl, ct + thirdH), end = Offset(cr, ct + thirdH), strokeWidth = 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.5f), start = Offset(cl, ct + thirdH * 2), end = Offset(cr, ct + thirdH * 2), strokeWidth = 1.dp.toPx())
            
            // Draw Corner Handles
            val handleRadius = 8.dp.toPx()
            drawCircle(Color.White, radius = handleRadius, center = Offset(cl, ct)) // TL
            drawCircle(Color.White, radius = handleRadius, center = Offset(cr, ct)) // TR
            drawCircle(Color.White, radius = handleRadius, center = Offset(cl, cb)) // BL
            drawCircle(Color.White, radius = handleRadius, center = Offset(cr, cb)) // BR
        }
    }
}
