package com.hbv501g.forumapp.ui.component

import android.util.Base64
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hbv501g.forumapp.BuildConfig
import com.hbv501g.forumapp.data.model.Post

@Composable
fun PostCard(
    post: Post,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val context = LocalContext.current
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "r/${post.community} • u/${post.author}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium
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
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            if (post.type == "LINK" && !post.url.isNullOrBlank()) {
                val url = post.url.trim()
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        runCatching { uriHandler.openUri(url) }
                    }
                )
            }
            if (!post.body.isNullOrBlank()) {
                LinkifiedBodyText(
                    text = post.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4
                )
            }
            Text(
                text = "Score ${post.score} • ${post.createdAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
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

@Composable
private fun LinkifiedBodyText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
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
                    color = MaterialTheme.colorScheme.primary,
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
