/**
 * What: Jetpack Compose UI for the Register screen — collects new-account details
 *       and creates the account, with a link back to login.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached pre-auth from
 *       LoginScreen via its "navigate to register" action.
 * When: Composed when the user navigates to the Register route from Login.
 */

package com.cs5520group15.memorycircle.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs5520group15.memorycircle.R
import com.cs5520group15.memorycircle.ui.common.PrimaryButton
import com.cs5520group15.memorycircle.ui.common.brandFieldColorsOnGradient
import com.cs5520group15.memorycircle.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

/**
 * What: Registration screen where new users create an account.
 * Who: Called by MemoryCircleNavigation when user taps "Create Account" on Login.
 * When: Shown when the user navigates from LoginScreen.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel:         RegisterViewModel = viewModel()
) {
    val name      by viewModel.name.collectAsStateWithLifecycle()
    val email     by viewModel.email.collectAsStateWithLifecycle()
    val password  by viewModel.password.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RegisterViewModel.RegisterEvent.ShowSnackbar   -> snackbarHostState.showSnackbar(event.message)
                is RegisterViewModel.RegisterEvent.NavigateToHome -> onRegisterSuccess()
            }
        }
    }

    RegisterContent(
        name              = name,
        email             = email,
        password          = password,
        isLoading         = isLoading,
        snackbarHostState = snackbarHostState,
        onNameChange      = viewModel::onNameChange,
        onEmailChange     = viewModel::onEmailChange,
        onPasswordChange  = viewModel::onPasswordChange,
        onRegisterClick   = viewModel::onRegisterClick,
        onNavigateToLogin = onNavigateToLogin
    )
}

/**
 * Stateless body — takes plain values + callbacks so it renders in @Preview
 * without touching Firebase. RegisterScreen above is the thin wrapper that
 * wires the ViewModel and event collection.
 */
@Composable
private fun RegisterContent(
    name:              String,
    email:             String,
    password:          String,
    isLoading:         Boolean,
    snackbarHostState: SnackbarHostState,
    onNameChange:      (String) -> Unit,
    onEmailChange:     (String) -> Unit,
    onPasswordChange:  (String) -> Unit,
    onRegisterClick:   () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Cream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Beige, Cream),
                        startY = 0f,
                        endY   = 900f
                    )
                )
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text  = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Ink
            )
            Text(
                text  = "Start preserving your memories",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text     = "NAME",
                style    = MaterialTheme.typography.labelSmall,
                color    = InkSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value         = name,
                onValueChange = onNameChange,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(28.dp),
                placeholder   = { Text("Your full name", color = Brown.copy(alpha = 0.6f)) },
                singleLine    = true,
                colors        = brandFieldColorsOnGradient()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text     = "EMAIL",
                style    = MaterialTheme.typography.labelSmall,
                color    = InkSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value         = email,
                onValueChange = onEmailChange,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(28.dp),
                placeholder   = { Text("sarah@example.com", color = Brown.copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine    = true,
                colors        = brandFieldColorsOnGradient()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text     = "PASSWORD",
                style    = MaterialTheme.typography.labelSmall,
                color    = InkSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value         = password,
                onValueChange = onPasswordChange,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(28.dp),
                placeholder   = { Text("••••••••", color = Brown.copy(alpha = 0.6f)) },
                trailingIcon  = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.ic_visibility
                                else R.drawable.ic_visibility_off
                            ),
                            contentDescription = null,
                            tint = Brown
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine    = true,
                colors        = brandFieldColorsOnGradient()
            )

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryButton(
                label   = "Create Account",
                onClick = onRegisterClick,
                loading = isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

/** Default empty form. */
@Preview(showBackground = true, name = "Register · empty")
@Composable
fun RegisterScreenPreview() {
    MemoryCircleTheme {
        RegisterContent(
            name              = "",
            email             = "",
            password          = "",
            isLoading         = false,
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange      = {},
            onEmailChange     = {},
            onPasswordChange  = {},
            onRegisterClick   = {},
            onNavigateToLogin = {}
        )
    }
}

/** Form filled in, ready to submit. */
@Preview(showBackground = true, name = "Register · filled")
@Composable
fun RegisterScreenFilledPreview() {
    MemoryCircleTheme {
        RegisterContent(
            name              = "Ada Lovelace",
            email             = "ada@example.com",
            password          = "secret123",
            isLoading         = false,
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange      = {},
            onEmailChange     = {},
            onPasswordChange  = {},
            onRegisterClick   = {},
            onNavigateToLogin = {}
        )
    }
}

/** Loading spinner — registration request in flight. */
@Preview(showBackground = true, name = "Register · loading")
@Composable
fun RegisterScreenLoadingPreview() {
    MemoryCircleTheme {
        RegisterContent(
            name              = "Ada Lovelace",
            email             = "ada@example.com",
            password          = "secret123",
            isLoading         = true,
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange      = {},
            onEmailChange     = {},
            onPasswordChange  = {},
            onRegisterClick   = {},
            onNavigateToLogin = {}
        )
    }
}
