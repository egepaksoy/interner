package com.egepaksoy.interner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.R
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.formatHours

private val Amber = Color(0xFFFFC069)

/** Staj ilerlemesini gösteren, dokununca staj bilgilerini açan başlık kartı. */
@Composable
fun InternshipHero(
    internship: Internship?,
    days: Int,
    hours: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF0F2E57), Color(0xFF1B4F91))
    } else {
        listOf(Color(0xFF0A3A78), Color(0xFF1A6FD0))
    }
    val company = internship?.company.orEmpty()
    val target = internship?.targetDays ?: 0
    val soft = Color.White.copy(alpha = 0.78f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                company.ifBlank { "Staj bilgilerini ekle" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                company.isBlank() -> "Kurum, tarih ve hedef gün sayısını gir"
                else -> listOfNotNull(
                    internship?.department?.takeIf { it.isNotBlank() },
                    dateRange(internship),
                ).joinToString(" · ")
            }
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = soft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(days.toString(), style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text(
                    if (target > 0) " / $target gün" else " gün",
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = soft,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroChip("${formatHours(hours)} saat")
                if (target > 0) {
                    val left = (target - days).coerceAtLeast(0)
                    HeroChip(if (left == 0) "Hedef tamamlandı" else "$left gün kaldı")
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        if (target > 0) {
            ProgressRing((days.toFloat() / target).coerceIn(0f, 1f))
        } else {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_plane),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

private fun dateRange(internship: Internship?): String? {
    val start = internship?.startDate
    val end = internship?.endDate
    if (start == null && end == null) return null
    return "${start?.let(::shortDate) ?: "?"} – ${end?.let(::shortDate) ?: "?"}"
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
    )
}

@Composable
private fun ProgressRing(fraction: Float) {
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, inset, arc, style = Stroke(stroke))
            if (fraction > 0f) {
                drawArc(
                    Amber, -90f, 360f * fraction, false, inset, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "%${(fraction * 100).toInt()}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
