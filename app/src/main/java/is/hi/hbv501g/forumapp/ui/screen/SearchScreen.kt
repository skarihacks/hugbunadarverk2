package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbv501g.forumapp.data.model.Community
import com.hbv501g.forumapp.data.model.Post
import com.hbv501g.forumapp.data.model.SearchUser
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val posts: List<Post> = emptyList(),
    val communities: List<Community> = emptyList(),
    val users: List<SearchUser> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel(private val repository: ForumRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value, error = null) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val results = repository.search(query)
                _uiState.update {
                    it.copy(
                        posts = results.posts,
                        communities = results.communities,
                        users = results.users,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Search failed") }
            }
        }
    }
}

@Composable
fun SearchRoute(
    repository: ForumRepository,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onOpenUser: (String) -> Unit
) {
    val viewModel: SearchViewModel = viewModel(
        factory = simpleViewModelFactory { SearchViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::search,
        onOpenPost = onOpenPost,
        onOpenCommunity = onOpenCommunity,
        onOpenUser = onOpenUser
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String) -> Unit,
    onOpenUser: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search posts, communities, users") },
                    trailingIcon = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    singleLine = true
                )
            }

            if (!state.error.isNullOrBlank()) {
                item {
                    Text(text = state.error, color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.posts.isNotEmpty()) {
                item { SectionTitle("Posts") }
                items(state.posts, key = { it.id }) { post ->
                    ResultRow(
                        title = post.title,
                        subtitle = "r/${post.community} • u/${post.author}",
                        onClick = { onOpenPost(post.id) }
                    )
                }
            }

            if (state.communities.isNotEmpty()) {
                item { SectionTitle("Communities") }
                items(state.communities, key = { it.id }) { community ->
                    ResultRow(
                        title = "r/${community.name}",
                        subtitle = community.description.orEmpty(),
                        onClick = { onOpenCommunity(community.name) }
                    )
                }
            }

            if (state.users.isNotEmpty()) {
                item { SectionTitle("Users") }
                items(state.users, key = { it.id }) { user ->
                    ResultRow(
                        title = "u/${user.username}",
                        subtitle = user.status,
                        onClick = { onOpenUser(user.username) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ResultRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
