package com.hbv501g.forumapp.ui.component

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hbv501g.forumapp.BuildConfig
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.ui.util.toRelativeTimeLabel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward

@Composable
fun PostCard(
    post: Post,
    score: Int = post.score,
    onUpvote: (() -> Unit)? = null,
    onDownvote: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = post.community.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "u/${post.author}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val mediaModel = resolveMediaModel(post)
            if (post.type == "MEDIA" && mediaModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = post.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            if (post.type == "LINK" && !post.url.isNullOrBlank()) {
                val url = post.url.trim()
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { runCatching { uriHandler.openUri(url) } }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            if (!post.body.isNullOrBlank()) {
                LinkifiedBodyText(
                    text = post.body,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    if (onUpvote != null) {
                        IconButton(onClick = onUpvote) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Upvote",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onDownvote != null) {
                        IconButton(onClick = onDownvote) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Downvote",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = post.createdAt.toRelativeTimeLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkifiedBodyText(
    text: String,
    style: TextStyle,
    maxLines: Int
) {
    val uriHandler = LocalUriHandler.current
    val linkRegex = Regex("""https?://[^\s]+""")
    val links = linkRegex.findAll(text).toList()

    if (links.isEmpty()) {
        Text(
            text = text,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val annotated = buildAnnotatedString {
        append(text)
        links.forEach { match ->
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.tertiary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
            addStringAnnotation(
                tag = "URL",
                annotation = match.value,
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }

    ClickableText(
        text = annotated,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onClick = { offset ->
            val annotation = annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
            if (annotation != null) {
                runCatching { uriHandler.openUri(annotation.item) }
            }
        }
    )
}
private fun resolveMediaModel(post: Post): Any? {
    resolveMediaUrl(post.mediaUrl)?.let { return it }
    return decodeBase64Media(post.mediaBase64)
}

private fun decodeBase64Media(raw: String?): ByteArray? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null

    val payload = if (value.startsWith("data:", ignoreCase = true) && value.contains(",")) {
        value.substringAfter(",")
    } else {
        value
    }

    return runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull()
}

private fun resolveMediaUrl(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    val base = BuildConfig.BASE_URL.trimEnd('/')
    val path = if (value.startsWith("/")) value else "/$value"
    return "$base$path"
}
