package com.hbv501g.forumapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ForumBrand(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tagline: String = "Read slowly. Post clearly."
) {
    val iconSize = if (compact) 42.dp else 68.dp
    val iconRadius = if (compact) 14.dp else 22.dp
    val iconPadding = if (compact) 8.dp else 12.dp
    val dotSize = if (compact) 6.dp else 10.dp
    val lineHeight = if (compact) 5.dp else 8.dp
    val dotSpacing = if (compact) 4.dp else 6.dp
    val textSpacing = if (compact) 12.dp else 14.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(textSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconRadius))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(iconPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dotSpacing)) {
                    BrandDot(dotSize, MaterialTheme.colorScheme.primary)
                    BrandDot(dotSize, MaterialTheme.colorScheme.tertiary)
                }
                BrandLine(
                    modifier = Modifier.fillMaxWidth(),
                    height = lineHeight,
                    color = MaterialTheme.colorScheme.primary
                )
                BrandLine(
                    modifier = Modifier.fillMaxWidth(if (compact) 0.7f else 0.72f),
                    height = lineHeight,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)) {
            Text(
                text = "Forum",
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tagline,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BrandDot(size: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(99.dp))
            .background(color)
    )
}

@Composable
private fun BrandLine(
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(color)
    )
}
