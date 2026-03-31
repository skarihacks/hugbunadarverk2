package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.hbv501g.forumapp.ui.component.ForumBrand
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
    val joinedCommunities: Set<String> = emptySet(),
    val postScores: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class FeedViewModel(private val repository: ForumRepository) : ViewModel() {

    private val voteManager = repository.voteManager
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessionFlow.collect { session ->
                _uiState.update { it.copy(username = session?.username.orEmpty()) }
            }
        }
        viewModelScope.launch {
            repository.joinedCommunitiesFlow.collect { joined ->
                _uiState.update { it.copy(joinedCommunities = joined) }
            }
        }
        viewModelScope.launch {
            voteManager.postScores.collect { scores ->
                _uiState.update { it.copy(postScores = scores) }
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

    fun votePost(postId: String, direction: Int) {
        val currentPost = _uiState.value.posts.firstOrNull { it.id == postId } ?: return
        val currentScore = voteManager.postScore(postId) ?: currentPost.score

        voteManager.optimisticPostVote(postId, currentScore, direction)

        viewModelScope.launch {
            try {
                repository.vote(postId, "POST", direction)
                val updatedPost = repository.getPost(postId)
                voteManager.confirmPostScore(postId, updatedPost.score)
            } catch (exception: RepositoryException) {
                voteManager.revertPostScore(postId, currentScore)
                _uiState.update { it.copy(error = exception.message) }
            }
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
    onOpenCommunities: () -> Unit,
    onOpenSearch: () -> Unit,
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
        onOpenCommunities = onOpenCommunities,
        onOpenSearch = onOpenSearch,
        onLogout = viewModel::logout,
        onVotePost = viewModel::votePost,
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
    onOpenCommunities: () -> Unit,
    onOpenSearch: () -> Unit,
    onLogout: () -> Unit,
    onVotePost: (String, Int) -> Unit,
    onOpenPost: (String) -> Unit,
    onCreatePost: () -> Unit,
    onCreateCommunity: () -> Unit
) {
    val joinedCommunitiesLower = state.joinedCommunities.map { it.lowercase() }.toSet()
    val visiblePosts = if (joinedCommunitiesLower.isEmpty()) {
        state.posts
    } else {
        state.posts.filter { post -> post.community.lowercase() in joinedCommunitiesLower }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            FeedHeader(
                username = state.username,
                onRefresh = onRefresh,
                onCreatePost = onCreatePost,
                onCreateCommunity = onCreateCommunity,
                onOpenCommunities = onOpenCommunities,
                onOpenSearch = onOpenSearch,
                onLogout = onLogout
            )

            SortChips(
                selected = state.selectedSort,
                onSelect = onSelectSort,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (!state.error.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            if (state.joinedCommunities.isNotEmpty()) {
                Text(
                    text = "Showing joined communities only (${state.joinedCommunities.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(items = visiblePosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            score = state.postScores[post.id] ?: post.score,
                            onUpvote = { onVotePost(post.id, 1) },
                            onDownvote = { onVotePost(post.id, -1) },
                            onClick = { onOpenPost(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    username: String,
    onRefresh: () -> Unit,
    onCreatePost: () -> Unit,
    onCreateCommunity: () -> Unit,
    onOpenCommunities: () -> Unit,
    onOpenSearch: () -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForumBrand(compact = true)
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Create community") },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onCreateCommunity()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Browse communities") },
                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenCommunities()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Search") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenSearch()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log out") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onCreatePost) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = "New post",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(
                        text = "Refresh",
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
            text = "Browse by",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedSort.entries.forEach { sort ->
                FilterChip(
                    selected = sort == selected,
                    onClick = { onSelect(sort) },
                    label = { Text(sort.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = sort == selected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
