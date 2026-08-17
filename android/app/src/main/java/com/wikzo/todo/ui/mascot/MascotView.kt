package com.wikzo.todo.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The small companion character that lives near the top of [com.wikzo.todo.ui.tasklist.TaskListScreen],
 * reacting to how the task list is going. Fully vector-drawn on a [Canvas] -- this
 * project has no image assets, and a hand-drawn-feeling blob is simple enough to
 * describe as a handful of circles and a couple of paths rather than needing one.
 */
enum class MascotMood {
    /** Default resting state: nothing notable has happened recently. */
    IDLE,

    /** At least one task done, but the list isn't fully cleared yet. */
    HAPPY,

    /** The list was just fully cleared -- shown briefly, then settles back to idle. */
    CELEBRATING,
}

@Composable
fun MascotView(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    // Very slow, barely-there bob -- this runs continuously regardless of mood, so
    // it needs to read as ambient "the mascot is alive," not as an attention-getter.
    val infiniteTransition = rememberInfiniteTransition(label = "mascot-idle-bob")
    val idleBob by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idle-bob",
    )

    // A spring (rather than a tween) is what gives this its "pop" -- it overshoots
    // past 1.12f and settles back, so entering CELEBRATING reads as a little
    // bounce rather than a linear grow.
    val bounceScale by animateFloatAsState(
        targetValue = if (mood == MascotMood.CELEBRATING) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "mascot-bounce",
    )

    val bodyColor = MaterialTheme.colorScheme.secondary
    val faceColor = MaterialTheme.colorScheme.onSecondary

    Canvas(modifier = modifier.size(size)) {
        val bobOffsetPx = idleBob * (this.size.minDimension * 0.035f)

        translate(top = bobOffsetPx) {
            scale(scale = bounceScale, pivot = center) {
                val bodyRadius = this.size.minDimension * 0.34f
                val bodyCenter = center

                // Cloud/blob silhouette: one big circle plus three smaller "bumps"
                // along the top edge, all the same color so they read as one
                // rounded shape rather than four separate circles.
                drawCircle(color = bodyColor, radius = bodyRadius, center = bodyCenter)
                val bumpRadius = bodyRadius * 0.58f
                drawCircle(
                    color = bodyColor,
                    radius = bumpRadius,
                    center = bodyCenter + Offset(-bodyRadius * 0.62f, -bodyRadius * 0.42f),
                )
                drawCircle(
                    color = bodyColor,
                    radius = bumpRadius * 1.05f,
                    center = bodyCenter + Offset(0f, -bodyRadius * 0.72f),
                )
                drawCircle(
                    color = bodyColor,
                    radius = bumpRadius,
                    center = bodyCenter + Offset(bodyRadius * 0.62f, -bodyRadius * 0.42f),
                )

                // Face sits slightly below the blob's own center so it isn't
                // crowded by the top bumps.
                val faceCenter = bodyCenter + Offset(0f, bodyRadius * 0.12f)
                val eyeRadius = bodyRadius * 0.09f
                val eyeOffsetX = bodyRadius * 0.32f
                val eyeOffsetY = bodyRadius * 0.05f
                drawCircle(
                    color = faceColor,
                    radius = eyeRadius,
                    center = faceCenter + Offset(-eyeOffsetX, -eyeOffsetY),
                )
                drawCircle(
                    color = faceColor,
                    radius = eyeRadius,
                    center = faceCenter + Offset(eyeOffsetX, -eyeOffsetY),
                )

                drawMouth(mood = mood, faceCenter = faceCenter, bodyRadius = bodyRadius, color = faceColor)
            }
        }
    }
}

private fun DrawScope.drawMouth(
    mood: MascotMood,
    faceCenter: Offset,
    bodyRadius: Float,
    color: Color,
) {
    val mouthY = faceCenter.y + bodyRadius * 0.32f
    val strokeWidth = bodyRadius * 0.11f
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    when (mood) {
        MascotMood.IDLE -> {
            // Flat, neutral line -- no strong emotion either way.
            val halfWidth = bodyRadius * 0.22f
            drawLine(
                color = color,
                start = Offset(faceCenter.x - halfWidth, mouthY),
                end = Offset(faceCenter.x + halfWidth, mouthY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        MascotMood.HAPPY -> {
            // A gentle upward curve -- a small smile.
            val halfWidth = bodyRadius * 0.3f
            val depth = bodyRadius * 0.16f
            val path = Path().apply {
                moveTo(faceCenter.x - halfWidth, mouthY)
                quadraticTo(faceCenter.x, mouthY + depth, faceCenter.x + halfWidth, mouthY)
            }
            drawPath(path = path, color = color, style = stroke)
        }
        MascotMood.CELEBRATING -> {
            // A bigger, wider grin.
            val halfWidth = bodyRadius * 0.4f
            val depth = bodyRadius * 0.32f
            val path = Path().apply {
                moveTo(faceCenter.x - halfWidth, mouthY - depth * 0.15f)
                quadraticTo(faceCenter.x, mouthY + depth, faceCenter.x + halfWidth, mouthY - depth * 0.15f)
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}
