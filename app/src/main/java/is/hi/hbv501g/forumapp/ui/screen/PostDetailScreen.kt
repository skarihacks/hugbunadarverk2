package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.PostCard
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailUiState(
    val post: Post? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class PostDetailViewModel(
    private val repository: ForumRepository,
    private val postId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val post = repository.getPost(postId)
                _uiState.update {
                    it.copy(
                        post = post,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load post") }
            }
        }
    }
}

@Composable
fun PostDetailRoute(
    postId: String,
    repository: ForumRepository,
    onBack: () -> Unit
) {
    val viewModel: PostDetailViewModel = viewModel(
        key = postId,
        factory = simpleViewModelFactory { PostDetailViewModel(repository, postId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PostDetailScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailScreen(
    state: PostDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            if (!state.error.isNullOrBlank()) {
                Text(text = state.error, color = MaterialTheme.colorScheme.error)
            }

            if (state.isLoading && state.post == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                state.post?.let { post ->
                    PostCard(post = post)
                }
            }
        }
    }
}
