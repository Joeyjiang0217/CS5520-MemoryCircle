package com.cs5520group15.memorycircle.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * What: Holds all UI state and business logic for Login and Register screens.
 * Who: Used by LoginScreen and RegisterScreen.
 * When: Created once and survives configuration changes (e.g. screen rotation).
 */
class AuthViewModel : ViewModel() {

    // --- UI State (StateFlow) ---
    // These are persistent states that survive rotation
    // Private mutable version — only ViewModel can change it
    private val _email    = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _name     = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    // Public read-only version — UI can only observe, not change directly
    val email:     StateFlow<String>  = _email.asStateFlow()
    val password:  StateFlow<String>  = _password.asStateFlow()
    val name:      StateFlow<String>  = _name.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- One-shot Events (Channel) ---
    // Used for things that should only happen once (e.g. show a Snackbar)
    sealed class AuthEvent {
        data class ShowSnackbar(val message: String) : AuthEvent()
        object NavigateToHome     : AuthEvent()
        object NavigateToRegister : AuthEvent()
    }

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // --- Event handlers called by the UI ---

    /**
     * What: Updates the email state when the user types in the email field.
     * Who: Called by LoginScreen and RegisterScreen's email TextField.
     * When: Every time the user types a character.
     */
    fun onEmailChange(value: String) {
        _email.value = value
    }

    /**
     * What: Updates the password state when the user types in the password field.
     * Who: Called by LoginScreen and RegisterScreen's password TextField.
     * When: Every time the user types a character.
     */
    fun onPasswordChange(value: String) {
        _password.value = value
    }

    /**
     * What: Updates the name state when the user types in the name field.
     * Who: Called by RegisterScreen's name TextField.
     * When: Every time the user types a character.
     */
    fun onNameChange(value: String) {
        _name.value = value
    }

    /**
     * What: Validates login input and triggers navigation to Home on success.
     *       Shows a Snackbar if validation fails.
     * Who: Called by LoginScreen when the user taps "Sign In".
     * When: On Sign In button click.
     */
    fun onLoginClick() = viewModelScope.launch {
        // Basic validation
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _events.send(AuthEvent.ShowSnackbar("Please fill in all fields"))
            return@launch
        }
        // Simulate login success (Firebase will replace this later)
        _isLoading.value = true
        kotlinx.coroutines.delay(500)
        _isLoading.value = false
        _events.send(AuthEvent.NavigateToHome)
    }

    /**
     * What: Validates register input and triggers navigation to Home on success.
     *       Shows a Snackbar if validation fails.
     * Who: Called by RegisterScreen when the user taps "Create Account".
     * When: On Create Account button click.
     */
    fun onRegisterClick() = viewModelScope.launch {
        if (_name.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _events.send(AuthEvent.ShowSnackbar("Please fill in all fields"))
            return@launch
        }
        _isLoading.value = true
        kotlinx.coroutines.delay(500)
        _isLoading.value = false
        _events.send(AuthEvent.NavigateToHome)
    }
}