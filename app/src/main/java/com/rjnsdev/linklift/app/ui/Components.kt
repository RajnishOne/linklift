package com.rjnsdev.linklift.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rjnsdev.linklift.app.chipTextStyle
import com.rjnsdev.linklift.app.isLargeFont
import com.rjnsdev.linklift.app.ui.theme.LinkLiftAccent
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTheme
import com.rjnsdev.linklift.app.ui.theme.LinkLiftAccentBright
import com.rjnsdev.linklift.app.ui.theme.LinkLiftBackground
import com.rjnsdev.linklift.app.ui.theme.LinkLiftCard
import com.rjnsdev.linklift.app.ui.theme.LinkLiftYouTubeRed
import com.rjnsdev.linklift.app.ui.theme.LinkLiftInstagramPink
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextMuted
import com.rjnsdev.linklift.app.ui.theme.LinkLiftTextSecondary
import java.util.Locale

data class PlatformBrand(
    val name: String,
    val color: Color,
    val icon: ImageVector,
) {
    val first: String get() = name
    val second: Color get() = color
    val third: ImageVector get() = icon
}

internal fun getPlatformBrand(url: String?, host: String? = null): PlatformBrand? {
    if (!url.isNullOrBlank()) {
        if (isYouTubeUrl(url)) return PlatformBrand("YouTube", LinkLiftYouTubeRed, Icons.Rounded.PlayArrow)
        if (isInstagramUrl(url)) return PlatformBrand("Instagram", LinkLiftInstagramPink, Icons.Rounded.CameraAlt)
        val lower = url.lowercase(Locale.ROOT)
        if (lower.contains("youtube") || lower.contains("youtu.be")) {
            return PlatformBrand("YouTube", LinkLiftYouTubeRed, Icons.Rounded.PlayArrow)
        }
        if (lower.contains("instagram") || lower.contains("instagr.am")) {
            return PlatformBrand("Instagram", LinkLiftInstagramPink, Icons.Rounded.CameraAlt)
        }
    }
    val h = host?.lowercase(Locale.ROOT).orEmpty()
    if (h.contains("youtube") || h.contains("youtu.be")) {
        return PlatformBrand("YouTube", LinkLiftYouTubeRed, Icons.Rounded.PlayArrow)
    }
    if (h.contains("instagram") || h.contains("instagr.am")) {
        return PlatformBrand("Instagram", LinkLiftInstagramPink, Icons.Rounded.CameraAlt)
    }
    return null
}

@Composable
internal fun PlatformBadge(
    url: String? = null,
    host: String? = null,
    modifier: Modifier = Modifier,
) {
    val platform = getPlatformBrand(url, host) ?: return
    val (name, color, icon) = platform
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = name,
                style = chipTextStyle(MaterialTheme.typography.labelSmall),
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
internal fun NeonCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = LinkLiftCard.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            content = content,
        )
    }
}

@Composable
internal fun TitleActionRow(
    title: String,
    action: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    actionStyle: TextStyle = MaterialTheme.typography.labelLarge,
    titleColor: Color = Color.White,
    actionColor: Color = Color.White,
) {
    if (isLargeFont()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = titleStyle, color = titleColor)
            if (!action.isNullOrBlank()) {
                Text(text = action, style = actionStyle, color = actionColor)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = titleStyle,
                color = titleColor,
            )
            if (!action.isNullOrBlank()) {
                Text(
                    text = action,
                    modifier = Modifier.widthIn(max = 160.dp),
                    style = actionStyle,
                    color = actionColor,
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, action: String? = null) {
    TitleActionRow(title = title, action = action)
}

@Composable
internal fun MetricPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(min = 72.dp),
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = chipTextStyle(MaterialTheme.typography.labelLarge),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun InfoChip(
    title: String,
    value: String,
    accentColor: Color? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor?.copy(alpha = 0.55f) ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor?.copy(alpha = 0.85f) ?: LinkLiftTextMuted,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor ?: Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor ?: Color.White,
                    fontWeight = if (accentColor != null) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    NeonCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LinkLiftTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun MergeBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = "MERGE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = chipTextStyle(MaterialTheme.typography.labelSmall),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
internal fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LinkLiftBackground,
                        Color(0xFF141414),
                        Color(0xFF0A0A0A),
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF222222).copy(alpha = 0.35f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    )
                )
        )
        content()
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Section header", showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
private fun SectionHeaderPreview() {
    LinkLiftTheme(darkTheme = true) {
        SectionHeader(title = "Supported links", action = "Popular apps")
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Section header — large font",
    fontScale = 1.5f,
    showBackground = true,
    backgroundColor = 0xFF0F0F0F,
)
@Composable
private fun SectionHeaderLargeFontPreview() {
    LinkLiftTheme(darkTheme = true) {
        SectionHeader(title = "Supported links", action = "Popular apps")
    }
}

@Composable
internal fun ExpandableDescriptionText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LinkLiftTextSecondary,
    suffix: String? = null,
) {
    val fullText = remember(text, suffix) {
        if (suffix.isNullOrBlank()) text else if (text.isBlank()) suffix else "$text • $suffix"
    }
    if (fullText.isBlank()) return

    var isExpanded by rememberSaveable(fullText) { mutableStateOf(false) }
    var isOverflowing by remember(fullText) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .animateContentSize()
            .clickable(enabled = isExpanded || isOverflowing) { isExpanded = !isExpanded }
    ) {
        Text(
            text = fullText,
            style = style,
            color = color,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!isExpanded) {
                    isOverflowing = textLayoutResult.hasVisualOverflow
                }
            }
        )
        if (isOverflowing || isExpanded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = if (isExpanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Show less" else "Show more",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

