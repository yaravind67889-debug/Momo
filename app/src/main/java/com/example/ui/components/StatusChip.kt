package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MomoStatusCompleted
import com.example.ui.theme.MomoStatusFailed
import com.example.ui.theme.MomoStatusQueued
import com.example.ui.theme.MomoStatusRunning
import com.example.ui.theme.MomoStatusWaiting

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor, label, icon) = when (status.uppercase()) {
        "RUNNING" -> Quad(
            MomoStatusRunning.copy(alpha = 0.15f),
            MomoStatusRunning,
            "Running",
            Icons.Default.PlayArrow
        )
        "QUEUED" -> Quad(
            MomoStatusQueued.copy(alpha = 0.15f),
            MomoStatusQueued,
            "Queued",
            Icons.Default.HourglassTop
        )
        "WAITING_APPROVAL" -> Quad(
            MomoStatusWaiting.copy(alpha = 0.18f),
            MomoStatusWaiting,
            "Approval Needed",
            Icons.Default.Security
        )
        "COMPLETED" -> Quad(
            MomoStatusCompleted.copy(alpha = 0.15f),
            MomoStatusCompleted,
            "Completed",
            Icons.Default.CheckCircle
        )
        "CANCELLED" -> Quad(
            Color.Gray.copy(alpha = 0.15f),
            Color.Gray,
            "Cancelled",
            Icons.Default.Close
        )
        else -> Quad(
            MomoStatusFailed.copy(alpha = 0.15f),
            MomoStatusFailed,
            "Failed",
            Icons.Default.ReportProblem
        )
    }

    val isPulsing = status.uppercase() in listOf("RUNNING", "WAITING_APPROVAL")
    val alphaAnim by if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alphaAnim)
                    .clip(CircleShape)
                    .background(contentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
