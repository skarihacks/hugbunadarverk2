package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.repository.RepositoryException
import com.hbv501g.forumapp.ui.component.ForumBrand
import com.hbv501g.forumapp.ui.component.simpleViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun updateIdentifier(value: String) {
        _uiState.update { it.copy(identifier = value, error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        val snapshot = _uiState.value
        if (snapshot.isSubmitting || snapshot.identifier.isBlank() || snapshot.password.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.login(snapshot.identifier, snapshot.password)
                _uiState.update { it.copy(password = "", isSubmitting = false, error = null) }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isSubmitting = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, error = "Login failed") }
            }
        }
    }
}

@Composable
fun LoginRoute(
    repository: ForumRepository,
    onOpenSignUp: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel(
        factory = simpleViewModelFactory { LoginViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        state = state,
        onIdentifierChange = viewModel::updateIdentifier,
        onPasswordChange = viewModel::updatePassword,
        onSubmit = viewModel::login,
        onOpenSignUp = onOpenSignUp
    )
}

@Composable
private fun LoginScreen(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenSignUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ForumBrand()
                Text(
                    text = "A calmer place for ideas worth returning to.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(30.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Log in",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Continue with your username or email.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.identifier,
                        onValueChange = onIdentifierChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username or Email") },
                        singleLine = true,
                        enabled = !state.isSubmitting
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !state.isSubmitting
                    )

                    if (!state.error.isNullOrBlank()) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSubmitting && state.identifier.isNotBlank() && state.password.isNotBlank()
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text("Log in")
                        }
                    }

                    TextButton(
                        onClick = onOpenSignUp,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Create account")
                    }
                }
            }
        }
    }
}
