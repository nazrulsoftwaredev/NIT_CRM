package com.nit.crm.features.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nit.crm.features.MainViewModel

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

val OffsetListSaver: Saver<SnapshotStateList<Offset>, Any> = listSaver(
    save = { list ->
        list.map { listOf(it.x, it.y) }
    },
    restore = { saved ->
        val list = mutableStateListOf<Offset>()
        (saved as List<List<Float>>).forEach { 
            list.add(Offset(it[0], it[1]))
        }
        list
    }
)

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    points: SnapshotStateList<Offset>
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val view = androidx.compose.ui.platform.LocalView.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .onGloballyPositioned { size = it.size }
            .pointerInput(size) {
                if (size.width == 0 || size.height == 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    // Normalize
                    points.add(Offset(down.position.x / size.width, down.position.y / size.height))
                    var drag = down
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (!anyPressed) {
                            points.add(Offset.Unspecified)
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == drag.id } ?: break
                        if (change.positionChanged()) {
                            change.consume()
                            // Normalize
                            points.add(Offset(change.position.x / size.width, change.position.y / size.height))
                            drag = change
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            var isFirst = true
            for (point in points) {
                if (point == Offset.Unspecified) {
                    isFirst = true
                } else {
                    // Scale back
                    val sx = point.x * this.size.width
                    val sy = point.y * this.size.height
                    if (isFirst) {
                        path.moveTo(sx, sy)
                        isFirst = false
                    } else {
                        path.lineTo(sx, sy)
                    }
                }
            }
            drawPath(path = path, color = Color.Black, style = Stroke(width = 4f))
        }
    }
}

@Composable
fun SignatureCaptureScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    
    // Use rememberSaveable to persist points during rotation
    val points = rememberSaveable(saver = OffsetListSaver) { 
        mutableStateListOf<Offset>().apply {
            addAll(viewModel.initialSignaturePoints)
        }
    }

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sign Here",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = { points.clear() }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            SignaturePad(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                points = points
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (points.isNotEmpty()) {
                        viewModel.onSignatureCapturedAction?.invoke(points.toList())
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Signature")
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun createBitmapFromPoints(points: List<Offset>, width: Int = 800, height: Int = 320): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val validPoints = points.filter { it != Offset.Unspecified }
    if (validPoints.isEmpty()) return bitmap

    // Since points are normalized (0-1), we just scale them to the bitmap size
    // We also want to auto-crop/center them for a professional look
    val minX = validPoints.minOf { it.x }
    val maxX = validPoints.maxOf { it.x }
    val minY = validPoints.minOf { it.y }
    val maxY = validPoints.maxOf { it.y }

    val pWidth = maxX - minX
    val pHeight = maxY - minY

    // Calculate scale to fit in the bitmap with padding
    val targetPadding = 40f
    val availableW = width - targetPadding * 2
    val availableH = height - targetPadding * 2
    
    val scaleX = if (pWidth > 0) availableW / pWidth else 1f
    val scaleY = if (pHeight > 0) availableH / pHeight else 1f
    val scale = minOf(scaleX, scaleY)

    val dx = (width - pWidth * scale) / 2f - minX * scale
    val dy = (height - pHeight * scale) / 2f - minY * scale

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    val path = android.graphics.Path()
    var isFirst = true
    for (point in points) {
        if (point == Offset.Unspecified) {
            isFirst = true
        } else {
            val sx = point.x * scale + dx
            val sy = point.y * scale + dy
            if (isFirst) {
                path.moveTo(sx, sy)
                isFirst = false
            } else {
                path.lineTo(sx, sy)
            }
        }
    }
    canvas.drawPath(path, paint)
    return bitmap
}
