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

data class CreateCommunityUiState(
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdCommunityName: String? = null
)

class CreateCommunityViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCommunityUiState())
    val uiState = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, error = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value, error = null) }
    }

    fun clearCreatedCommunity() {
        _uiState.update { it.copy(createdCommunityName = null) }
    }

    fun submit() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting) {
            return
        }
        if (snapshot.name.isBlank()) {
            _uiState.update { it.copy(error = "Community name is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val name = repository.createCommunity(
                    name = snapshot.name,
                    description = snapshot.description
                )
                _uiState.update {
                    it.copy(
                        name = "",
                        description = "",
                        isSubmitting = false,
                        error = null,
                        createdCommunityName = name
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isSubmitting = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, error = "Could not create community") }
            }
        }
    }
}

@Composable
fun CreateCommunityRoute(
    repository: ForumRepository,
    onBack: () -> Unit
) {
    val viewModel: CreateCommunityViewModel = viewModel(
        factory = simpleViewModelFactory { CreateCommunityViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdCommunityName) {
        if (state.createdCommunityName == null) {
            return@LaunchedEffect
        }
        viewModel.clearCreatedCommunity()
        onBack()
    }

    CreateCommunityScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::updateName,
        onDescriptionChange = viewModel::updateDescription,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCommunityScreen(
    state: CreateCommunityUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create community") },
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
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Community name") },
                singleLine = true,
                enabled = !state.isSubmitting
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Description (optional)") },
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
                Text(if (state.isSubmitting) "Creating..." else "Create community")
            }
        }
    }
}
