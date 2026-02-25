package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.hbv501g.forumapp.data.model.Comment
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
    val comments: List<Comment> = emptyList(),
    val commentInput: String = "",
    val isLoading: Boolean = true,
    val isSubmittingComment: Boolean = false,
    val error: String? = null,
    val commentError: String? = null
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

    fun updateCommentInput(value: String) {
        _uiState.update { it.copy(commentInput = value, commentError = null) }
    }

    fun submitComment() {
        val snapshot = _uiState.value
        if (snapshot.isSubmittingComment) {
            return
        }

        val body = snapshot.commentInput.trim()
        if (body.isBlank()) {
            _uiState.update { it.copy(commentError = "Comment cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingComment = true, commentError = null) }
            try {
                repository.createComment(postId = postId, body = body)
                val updatedComments = repository.getComments(postId)
                _uiState.update {
                    it.copy(
                        comments = updatedComments,
                        commentInput = "",
                        isSubmittingComment = false,
                        commentError = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isSubmittingComment = false, commentError = exception.message) }
            } catch (_: Throwable) {
                _uiState.update {
                    it.copy(
                        isSubmittingComment = false,
                        commentError = "Could not create comment"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val post = repository.getPost(postId)
                val comments = repository.getComments(postId)
                _uiState.update {
                    it.copy(
                        post = post,
                        comments = comments,
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
        onRefresh = viewModel::refresh,
        onCommentInputChange = viewModel::updateCommentInput,
        onSubmitComment = viewModel::submitComment
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostDetailScreen(
    state: PostDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCommentInputChange: (String) -> Unit,
    onSubmitComment: () -> Unit
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.error.isNullOrBlank()) {
                item {
                    Text(text = state.error, color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.isLoading && state.post == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                state.post?.let { post ->
                    item {
                        PostCard(post = post)
                    }
                }
            }

            item {
                Text(
                    text = "Add a comment",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                OutlinedTextField(
                    value = state.commentInput,
                    onValueChange = onCommentInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Write your comment") },
                    enabled = !state.isSubmittingComment
                )
            }

            if (!state.commentError.isNullOrBlank()) {
                item {
                    Text(text = state.commentError, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Button(
                    onClick = onSubmitComment,
                    enabled = !state.isSubmittingComment,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSubmittingComment) "Posting..." else "Post comment")
                }
            }

            item {
                Text(
                    text = "Comments (${state.comments.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!state.isLoading && state.comments.isEmpty()) {
                item {
                    Text(
                        text = "No comments yet. Be the first to comment.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            items(items = state.comments, key = { it.id }) { comment ->
                CommentCard(comment = comment)
            }
        }
    }
}

@Composable
private fun CommentCard(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "u/${comment.author}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Score ${comment.score} • ${comment.createdAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
