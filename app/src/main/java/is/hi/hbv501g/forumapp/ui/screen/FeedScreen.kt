package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.hbv501g.forumapp.data.model.FeedSort
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.PostCard
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val username: String = "",
    val selectedSort: FeedSort = FeedSort.HOT,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class FeedViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { session ->
                _uiState.update { it.copy(username = session?.username.orEmpty()) }
            }
        }
        refresh()
    }

    fun refresh() {
        loadFeed(_uiState.value.selectedSort)
    }

    fun updateSort(sort: FeedSort) {
        _uiState.update { it.copy(selectedSort = sort) }
        loadFeed(sort)
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    private fun loadFeed(sort: FeedSort) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val page = repository.getFeed(sort = sort)
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load feed") }
            }
        }
    }
}

@Composable
fun FeedRoute(
    repository: ForumRepository,
    onOpenPost: (String) -> Unit,
    onCreatePost: () -> Unit,
    onCreateCommunity: () -> Unit
) {
    val viewModel: FeedViewModel = viewModel(
        factory = simpleViewModelFactory { FeedViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FeedScreen(
        state = state,
        onSelectSort = viewModel::updateSort,
        onRefresh = viewModel::refresh,
        onLogout = viewModel::logout,
        onOpenPost = onOpenPost,
        onCreatePost = onCreatePost,
        onCreateCommunity = onCreateCommunity
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    state: FeedUiState,
    onSelectSort: (FeedSort) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onOpenPost: (String) -> Unit,
    onCreatePost: () -> Unit,
    onCreateCommunity: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.username.isBlank()) "Feed" else "Feed • ${state.username}"
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onCreatePost) {
                        Icon(Icons.Default.Add, contentDescription = "Create post")
                    }
                    IconButton(onClick = onCreateCommunity) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Create community")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SortChips(
                selected = state.selectedSort,
                onSelect = onSelectSort,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (!state.error.isNullOrBlank()) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.isLoading && state.posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = state.posts, key = { it.id }) { post ->
                        PostCard(post = post, onClick = { onOpenPost(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChips(
    selected: FeedSort,
    onSelect: (FeedSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Sort",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedSort.entries.forEach { sort ->
                FilterChip(
                    selected = sort == selected,
                    onClick = { onSelect(sort) },
                    label = { Text(sort.name) }
                )
            }
        }
    }
}
