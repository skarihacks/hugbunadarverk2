package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunitiesUiState(
    val communities: List<String> = emptyList(),
    val joinedCommunities: Set<String> = emptySet(),
    val discoverQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class CommunitiesViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunitiesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.joinedCommunitiesFlow.collect { joined ->
                _uiState.update { it.copy(joinedCommunities = joined) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val discovered = repository.listCommunities()
                val joined = _uiState.value.joinedCommunities
                val all = (discovered + joined)
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase() }

                _uiState.update {
                    it.copy(
                        communities = all,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isLoading = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load communities") }
            }
        }
    }

    fun updateDiscoverQuery(value: String) {
        _uiState.update { it.copy(discoverQuery = value) }
    }
}

@Composable
fun CommunitiesRoute(
    repository: ForumRepository,
    onBack: () -> Unit,
    onOpenCommunity: (String) -> Unit
) {
    val viewModel: CommunitiesViewModel = viewModel(
        factory = simpleViewModelFactory { CommunitiesViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CommunitiesScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onDiscoverQueryChange = viewModel::updateDiscoverQuery,
        onOpenCommunity = onOpenCommunity
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunitiesScreen(
    state: CommunitiesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDiscoverQueryChange: (String) -> Unit,
    onOpenCommunity: (String) -> Unit
) {
    val tabs = listOf("My Communities", "Discover")
    val joinedLower = state.joinedCommunities.map { it.lowercase() }.toSet()
    val myCommunities = state.joinedCommunities
        .sortedBy { it.lowercase() }
    val discoverCommunities = state.communities
        .filter { it.lowercase() !in joinedLower }
        .filter {
            val query = state.discoverQuery.trim()
            query.isBlank() || it.contains(query, ignoreCase = true)
        }

    var activeTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communities") },
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
            TabRow(selectedTabIndex = activeTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (activeTab == 1) {
                OutlinedTextField(
                    value = state.discoverQuery,
                    onValueChange = onDiscoverQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search communities") },
                    singleLine = true
                )
            }

            if (!state.error.isNullOrBlank()) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.isLoading && state.communities.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (activeTab == 0 && myCommunities.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("You have not joined any communities yet")
                }
            } else if (activeTab == 1 && discoverCommunities.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No matching communities")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val listItems = if (activeTab == 0) myCommunities else discoverCommunities
                    items(items = listItems, key = { it.lowercase() }) { community ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCommunity(community) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "r/$community",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Open",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
