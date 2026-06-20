/**
 * What: ViewModel that holds UI state and business logic for the Login screen.
 * Who:  Used by LoginScreen.
 * When: Created when LoginScreen is first composed; survives configuration changes.
 */

package com.cs5520group15.memorycircle.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs5520group15.memorycircle.data.AuthRepository
import com.cs5520group15.memorycircle.data.Result
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * What: Holds all UI state and business logic for the Login screen.
 *       Mirror of RegisterViewModel; separated so each screen carries only the
 *       fields and actions it actually needs.
 * Who: Used by LoginScreen.
 * When: Created once and survives configuration changes.
 */
class LoginViewModel : ViewModel() {

    private val _email     = MutableStateFlow("")
    private val _password  = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    val email:     StateFlow<String>  = _email.asStateFlow()
    val password:  StateFlow<String>  = _password.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    sealed class LoginEvent {
        data class ShowSnackbar(val message: String) : LoginEvent()
        object NavigateToHome : LoginEvent()
    }

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChange(value: String)    { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    /**
     * What: Validates login input and signs in via Firebase. Navigates Home on
     *       success or pops a Snackbar on failure / missing fields.
     * Who: Called by LoginScreen on Sign In tap.
     * When: On Sign In button click.
     */
    fun onLoginClick() = viewModelScope.launch {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _events.send(LoginEvent.ShowSnackbar("Please fill in all fields"))
            return@launch
        }
        _isLoading.value = true
        when (val result = AuthRepository.login(_email.value.trim(), _password.value)) {
            is Result.Success -> {
                _isLoading.value = false
                _events.send(LoginEvent.NavigateToHome)
            }
            is Result.Error -> {
                _isLoading.value = false
                _events.send(LoginEvent.ShowSnackbar(result.message))
            }
            is Result.Loading -> { /* repository returns terminal states */ }
        }
    }

    /**
     * What: Sends a Firebase password-reset email. Confirms via Snackbar.
     * Who: Called by LoginScreen's Forgot-password dialog.
     * When: On "Send reset link" tap.
     */
    fun onForgotPassword(email: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _events.send(LoginEvent.ShowSnackbar("Please enter your email"))
            return@launch
        }
        _isLoading.value = true
        when (val result = AuthRepository.sendPasswordReset(email.trim())) {
            is Result.Success -> {
                _isLoading.value = false
                _events.send(LoginEvent.ShowSnackbar("Password reset link sent to $email"))
            }
            is Result.Error -> {
                _isLoading.value = false
                _events.send(LoginEvent.ShowSnackbar(result.message))
            }
            is Result.Loading -> { }
        }
    }
}
