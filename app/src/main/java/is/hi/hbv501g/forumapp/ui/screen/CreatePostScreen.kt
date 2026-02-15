package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val communityName: String = "",
    val title: String = "",
    val body: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdPostId: String? = null
)

class CreatePostViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    fun updateCommunityName(value: String) {
        _uiState.update { it.copy(communityName = value, error = null) }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, error = null) }
    }

    fun updateBody(value: String) {
        _uiState.update { it.copy(body = value, error = null) }
    }

    fun clearCreatedPost() {
        _uiState.update { it.copy(createdPostId = null) }
    }

    fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) {
            return
        }
        if (snapshot.communityName.isBlank() || snapshot.title.isBlank() || snapshot.body.isBlank()) {
            _uiState.update { it.copy(error = "Community, title and body are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val post = repository.createTextPost(
                    communityName = snapshot.communityName,
                    title = snapshot.title,
                    body = snapshot.body
                )
                _uiState.update {
                    it.copy(
                        title = "",
                        body = "",
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
        onCommunityChange = viewModel::updateCommunityName,
        onTitleChange = viewModel::updateTitle,
        onBodyChange = viewModel::updateBody,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostScreen(
    state: CreatePostUiState,
    onBack: () -> Unit,
    onCommunityChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create text post") },
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

            OutlinedTextField(
                value = state.body,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Body") },
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
