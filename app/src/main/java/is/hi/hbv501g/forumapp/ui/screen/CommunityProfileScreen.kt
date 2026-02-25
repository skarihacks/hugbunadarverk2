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
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityProfileUiState(
    val communityName: String,
    val isJoined: Boolean = false
)

class CommunityProfileViewModel(
    private val repository: ForumRepository,
    communityName: String
) : ViewModel() {

    private val normalizedName = communityName.trim()
    private val _uiState = MutableStateFlow(
        CommunityProfileUiState(communityName = normalizedName)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.joinedCommunitiesFlow.collect { joined ->
                val isJoined = joined.any { it.equals(normalizedName, ignoreCase = true) }
                _uiState.update { it.copy(isJoined = isJoined) }
            }
        }
    }

    fun toggleMembership() {
        repository.toggleCommunityMembership(normalizedName)
    }
}

@Composable
fun CommunityProfileRoute(
    communityName: String,
    repository: ForumRepository,
    onBack: () -> Unit
) {
    val viewModel: CommunityProfileViewModel = viewModel(
        key = communityName,
        factory = simpleViewModelFactory { CommunityProfileViewModel(repository, communityName) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CommunityProfileScreen(
        state = state,
        onBack = onBack,
        onToggleMembership = viewModel::toggleMembership
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityProfileScreen(
    state: CommunityProfileUiState,
    onBack: () -> Unit,
    onToggleMembership: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "r/${state.communityName}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (state.isJoined) {
                    "You are a member of this community."
                } else {
                    "You are not a member of this community."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Button(
                onClick = onToggleMembership,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isJoined) "Leave community" else "Join community")
            }
        }
    }
}
