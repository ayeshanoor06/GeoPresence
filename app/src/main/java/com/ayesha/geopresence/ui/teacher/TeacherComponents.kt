package com.ayesha.geopresence.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.ui.components.AttendanceRing

@Composable
fun ClassAttendanceCard(
    info: ClassInfo,
    stat: ClassStat?,
    showDays: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${info.sectionLabel} · ${info.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (showDays && info.days.isNotEmpty()) {
                    Text(
                        text = info.daysLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
                when {
                    stat == null -> Text(
                        "Loading…",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                    stat.error != null -> Text(
                        "Can’t read students. Check assigned sections.",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.error
                    )
                    else -> Text(
                        "Present ${stat.attended} / ${stat.total}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (stat != null && stat.error == null && stat.total > 0) {
                AttendanceRing(
                    percent = stat.percent,
                    trackColor = colors.primaryContainer,
                    progressColor = if (stat.percent < 75) colors.error else colors.primary,
                    textColor = colors.onSurface,
                    diameter = 56.dp,
                    strokeWidth = 6.dp,
                    textStyle = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}