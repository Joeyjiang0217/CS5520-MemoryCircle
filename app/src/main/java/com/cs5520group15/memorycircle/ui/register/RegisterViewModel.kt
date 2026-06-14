package com.cs5520group15.memorycircle.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * What: Holds all UI state and business logic for the Register screen.
 *       Mirror of LoginViewModel; separated so each screen carries only the
 *       fields and actions it actually needs.
 * Who: Used by RegisterScreen.
 * When: Created once and survives configuration changes.
 */
class RegisterViewModel : ViewModel() {

    private val _name      = MutableStateFlow("")
    private val _email     = MutableStateFlow("")
    private val _password  = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    val name:      StateFlow<String>  = _name.asStateFlow()
    val email:     StateFlow<String>  = _email.asStateFlow()
    val password:  StateFlow<String>  = _password.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    sealed class RegisterEvent {
        data class ShowSnackbar(val message: String) : RegisterEvent()
        object NavigateToHome : RegisterEvent()
    }

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(value: String)     { _name.value = value }
    fun onEmailChange(value: String)    { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    /**
     * What: Validates register input and triggers navigation to Home on success.
     *       Shows a Snackbar if validation fails.
     * Who: Called by RegisterScreen on Create Account tap.
     * When: On Create Account button click.
     */
    fun onRegisterClick() = viewModelScope.launch {
        if (_name.value.isBlank() || _email.value.isBlank() || _password.value.isBlank()) {
            _events.send(RegisterEvent.ShowSnackbar("Please fill in all fields"))
            return@launch
        }
        _isLoading.value = true
        kotlinx.coroutines.delay(500)
        _isLoading.value = false
        _events.send(RegisterEvent.NavigateToHome)
    }
}
