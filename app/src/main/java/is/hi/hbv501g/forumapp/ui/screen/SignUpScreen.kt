package com.hbv501g.forumapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

data class SignUpUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class SignUpViewModel(private val repository: ForumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun signUp() {
        val snapshot = _uiState.value
        val validationError = validate(snapshot)
        if (snapshot.isSubmitting || validationError != null) {
            if (validationError != null) {
                _uiState.update { it.copy(error = validationError) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                repository.registerAndLogin(
                    username = snapshot.username,
                    email = snapshot.email,
                    password = snapshot.password
                )
                _uiState.update {
                    it.copy(
                        password = "",
                        confirmPassword = "",
                        isSubmitting = false,
                        error = null
                    )
                }
            } catch (exception: RepositoryException) {
                _uiState.update { it.copy(isSubmitting = false, error = exception.message) }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, error = "Sign up failed") }
            }
        }
    }

    private fun validate(state: SignUpUiState): String? {
        val username = state.username.trim()
        val email = state.email.trim()

        if (username.length !in 3..32) {
            return "Username must be 3-32 characters"
        }
        if (!username.matches(Regex("^[A-Za-z0-9_]+$"))) {
            return "Username can only contain letters, numbers and underscores"
        }
        if (!email.contains("@") || email.length < 5) {
            return "Enter a valid email"
        }
        if (state.password.length !in 8..64) {
            return "Password must be 8-64 characters"
        }
        if (state.password != state.confirmPassword) {
            return "Passwords do not match"
        }
        return null
    }
}

@Composable
fun SignUpRoute(
    repository: ForumRepository,
    onBackToLogin: () -> Unit
) {
    val viewModel: SignUpViewModel = viewModel(
        factory = simpleViewModelFactory { SignUpViewModel(repository) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpScreen(
        state = state,
        onUsernameChange = viewModel::updateUsername,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onConfirmPasswordChange = viewModel::updateConfirmPassword,
        onSubmit = viewModel::signUp,
        onBackToLogin = onBackToLogin
    )
}

@Composable
private fun SignUpScreen(
    state: SignUpUiState,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            enabled = !state.isSubmitting
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
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

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm password") },
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
            enabled = !state.isSubmitting
        ) {
            Text(if (state.isSubmitting) "Creating account..." else "Sign up")
        }

        TextButton(
            onClick = onBackToLogin,
            enabled = !state.isSubmitting
        ) {
            Text("Already have an account? Login")
        }
    }
}
