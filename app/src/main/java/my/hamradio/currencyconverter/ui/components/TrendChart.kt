package my.hamradio.currencyconverter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.data.model.ChartPoint

@Composable
fun TrendChart(
    points: List<ChartPoint>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillGradientColors: List<Color> = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.0f)),
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minVal = points.minOf { it.value }
    val maxVal = points.maxOf { it.value }
    val range = if (maxVal == minVal) 1.0 else (maxVal - minVal)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Interactive tooltip header
            val currentPoint = selectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()
            if (currentPoint != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = currentPoint.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.4f", currentPoint.value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(points) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                val x = change.position.x
                                val idx = ((x / size.width) * (points.size - 1)).toInt()
                                selectedIndex = idx.coerceIn(0, points.size - 1)
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }
                    .pointerInput(points) {
                        detectTapGestures(
                            onPress = { offset ->
                                val idx = ((offset.x / size.width) * (points.size - 1)).toInt()
                                selectedIndex = idx.coerceIn(0, points.size - 1)
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val padY = 20.dp.toPx()
                val usableHeight = h - 2 * padY

                // Draw horizontal grid lines (min, mid, max)
                val gridColor = Color.Gray.copy(alpha = 0.2f)
                drawLine(gridColor, Offset(0f, padY), Offset(w, padY), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, padY + usableHeight / 2), Offset(w, padY + usableHeight / 2), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, h - padY), Offset(w, h - padY), strokeWidth = 1.dp.toPx())

                val path = Path()
                val fillPath = Path()

                val stepX = w / (points.size - 1).coerceAtLeast(1)

                points.forEachIndexed { index, pt ->
                    val normY = (pt.value - minVal) / range
                    val x = index * stepX
                    val y = (h - padY) - (normY * usableHeight).toFloat()

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h - padY)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevPt = points[index - 1]
                        val prevNormY = (prevPt.value - minVal) / range
                        val prevX = (index - 1) * stepX
                        val prevY = (h - padY) - (prevNormY * usableHeight).toFloat()

                        val cx = (prevX + x) / 2
                        path.cubicTo(cx, prevY, cx, y, x, y)
                        fillPath.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }

                fillPath.lineTo(w, h - padY)
                fillPath.close()

                // Draw gradient under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = fillGradientColors,
                        startY = padY,
                        endY = h
                    )
                )

                // Draw main curve
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw indicator dot if selected
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val pt = points[idx]
                        val normY = (pt.value - minVal) / range
                        val selX = idx * stepX
                        val selY = (h - padY) - (normY * usableHeight).toFloat()

                        // Vertical guide line
                        drawLine(
                            color = lineColor.copy(alpha = 0.5f),
                            start = Offset(selX, padY),
                            end = Offset(selX, h - padY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Outer ring & inner dot
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(selX, selY)
                        )
                    }
                }
            }
        }
    }
}
