package com.hbv501g.forumapp.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PostDraftType {
    TEXT,
    LINK,
    MEDIA
}

data class SelectedMedia(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

data class CreatePostUiState(
    val type: PostDraftType = PostDraftType.TEXT,
    val communityName: String = "",
    val title: String = "",
    val body: String = "",
    val url: String = "",
    val media: SelectedMedia? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdPostId: String? = null
)

class CreatePostViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    fun updateType(value: PostDraftType) {
        _uiState.update {
            it.copy(
                type = value,
                error = null
            )
        }
    }

    fun updateCommunityName(value: String) {
        _uiState.update { it.copy(communityName = value, error = null) }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    fun updateBody(value: String) {
        _uiState.update { it.copy(body = value, error = null) }
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(url = value, error = null) }
    }

    fun attachMedia(fileName: String, mimeType: String, bytes: ByteArray) {
        _uiState.update {
            it.copy(
                media = SelectedMedia(
                    fileName = fileName,
                    mimeType = mimeType,
                    bytes = bytes
                ),
                error = null
            )
        }
    }

    fun clearMedia() {
        _uiState.update { it.copy(media = null, error = null) }
    }

    fun clearCreatedPost() {
        _uiState.update { it.copy(createdPostId = null) }
    }

    fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) {
            return
        }

        val validationError = validate(snapshot)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val post = when (snapshot.type) {
                    PostDraftType.TEXT -> repository.createTextPost(
                        communityName = snapshot.communityName,
                        title = snapshot.title,
                        body = snapshot.body
                    )

                    PostDraftType.LINK -> repository.createLinkPost(
                        communityName = snapshot.communityName,
                        title = snapshot.title,
                        url = snapshot.url,
                        body = snapshot.body.ifBlank { null }
                    )

                    PostDraftType.MEDIA -> {
                        val media = snapshot.media!!
                        repository.createMediaPost(
                            communityName = snapshot.communityName,
                            title = snapshot.title,
                            body = snapshot.body.ifBlank { null },
                            mediaBytes = media.bytes,
                            mediaFileName = media.fileName,
                            mediaMimeType = media.mimeType
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        title = "",
                        body = "",
                        url = "",
                        media = null,
                        isSubmitting = false,
                        createdPostId = post.id,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isSubmitting = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, error = "Could not create post") }
            }
        }
    }

    private fun validate(state: CreatePostUiState): String? {
        if (state.communityName.isBlank()) return "Community is required"
        if (state.title.isBlank()) return "Title is required"

        return when (state.type) {
            PostDraftType.TEXT -> {
                if (state.body.isBlank()) "Text posts require a body" else null
            }

            PostDraftType.LINK -> {
                val url = state.url.trim()
                if (url.isBlank()) {
                    "Link posts require a URL"
                } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "URL must start with http:// or https://"
                } else {
                    null
                }
            }

            PostDraftType.MEDIA -> {
                if (state.media == null) "Media posts require an image" else null
            }
        }
    }
}

@Composable
fun CreatePostRoute(
    repository: ForumRepository,
    onBack: () -> Unit,
    onPostCreated: (String) -> Unit
) {
    val viewModel: CreatePostViewModel = viewModel(
        factory = simpleViewModelFactory { CreatePostViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdPostId) {
        val postId = state.createdPostId ?: return@LaunchedEffect
        onPostCreated(postId)
        viewModel.clearCreatedPost()
    }

    CreatePostScreen(
        state = state,
        onBack = onBack,
        onTypeChange = viewModel::updateType,
        onCommunityChange = viewModel::updateCommunityName,
        onTitleChange = viewModel::updateTitle,
        onBodyChange = viewModel::updateBody,
        onUrlChange = viewModel::updateUrl,
        onAttachMedia = viewModel::attachMedia,
        onClearMedia = viewModel::clearMedia,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostScreen(
    state: CreatePostUiState,
    onBack: () -> Unit,
    onTypeChange: (PostDraftType) -> Unit,
    onCommunityChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAttachMedia: (fileName: String, mimeType: String, bytes: ByteArray) -> Unit,
    onClearMedia: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/*"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: "image-${UUID.randomUUID()}.jpg"

        onAttachMedia(fileName, mime, bytes)
    }

    val titleText = when (state.type) {
        PostDraftType.TEXT -> "Create text post"
        PostDraftType.LINK -> "Create link post"
        PostDraftType.MEDIA -> "Create media post"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostDraftType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            OutlinedTextField(
                value = state.communityName,
                onValueChange = onCommunityChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Community name") },
                singleLine = true,
                enabled = !state.isSubmitting
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                enabled = !state.isSubmitting,
                maxLines = 2
            )

            if (state.type == PostDraftType.LINK) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") },
                    singleLine = true,
                    enabled = !state.isSubmitting
                )
            }

            if (state.type == PostDraftType.MEDIA) {
                Button(
                    onClick = { mediaPicker.launch("image/*") },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.media == null) "Pick image" else "Change image")
                }

                if (state.media != null) {
                    Text(
                        text = "Selected: ${state.media.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Button(
                        onClick = onClearMedia,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove image")
                    }
                }
            }

            OutlinedTextField(
                value = state.body,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = {
                    val label = when (state.type) {
                        PostDraftType.TEXT -> "Body"
                        PostDraftType.LINK -> "Body (optional)"
                        PostDraftType.MEDIA -> "Caption (optional)"
                    }
                    Text(label)
                },
                enabled = !state.isSubmitting
            )

            if (!state.error.isNullOrBlank()) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSubmitting) "Posting..." else "Post")
            }
        }
    }
}
