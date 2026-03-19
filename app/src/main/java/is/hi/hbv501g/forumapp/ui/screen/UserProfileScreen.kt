package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.hbv501g.forumapp.data.model.UserProfile
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.PostCard
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class UserProfileViewModel(
    private val repository: ForumRepository,
    private val username: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = repository.getUserProfile(username)
                _uiState.update { it.copy(profile = profile, isLoading = false, error = null) }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load profile") }
            }
        }
    }
}

@Composable
fun UserProfileRoute(
    username: String,
    repository: ForumRepository,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit
) {
    val viewModel: UserProfileViewModel = viewModel(
        key = username,
        factory = simpleViewModelFactory { UserProfileViewModel(repository, username) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    UserProfileScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onOpenPost = onOpenPost
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileScreen(
    state: UserProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPost: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User profile") },
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

            state.profile?.let { profile ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("u/${profile.username}", style = MaterialTheme.typography.headlineSmall)
                        Text("Karma ${profile.karma}", color = MaterialTheme.colorScheme.secondary)
                        Text(profile.status, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                items(profile.posts.items, key = { it.id }) { post ->
                    PostCard(post = post, onClick = { onOpenPost(post.id) })
                }
            }
        }
    }
}
