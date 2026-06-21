/**
 * What: Jetpack Compose UI for the Login screen — collects credentials and signs
 *       the user in, with a link out to registration.
 * Who:  Wired into the nav graph by MemoryCircleNavigation; reached pre-auth as the
 *       start destination when no signed-in user is present.
 * When: Composed when the user navigates to the Login route (the start destination
 *       when no session exists).
 */

package com.cs5520group15.memorycircle.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
 * What: Login screen where users enter email and password to sign in.
 * Who: Called by MemoryCircleNavigation as the start destination.
 * When: Shown when the app first launches.
 */
@Composable
fun LoginScreen(
    onLoginSuccess:       () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel:            LoginViewModel = viewModel()
) {
    val email     by viewModel.email.collectAsStateWithLifecycle()
    val password  by viewModel.password.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LoginViewModel.LoginEvent.ShowSnackbar   -> snackbarHostState.showSnackbar(event.message)
                is LoginViewModel.LoginEvent.NavigateToHome -> onLoginSuccess()
            }
        }
    }

    LoginContent(
        email                = email,
        password             = password,
        isLoading            = isLoading,
        snackbarHostState    = snackbarHostState,
        onEmailChange        = viewModel::onEmailChange,
        onPasswordChange     = viewModel::onPasswordChange,
        onLoginClick         = viewModel::onLoginClick,
        onForgotPassword     = viewModel::onForgotPassword,
        onNavigateToRegister = onNavigateToRegister
    )
}

/**
 * Stateless body — takes plain values + callbacks so it renders in @Preview
 * without touching Firebase. LoginScreen above is the thin wrapper that wires
 * the ViewModel and event collection.
 */
@Composable
private fun LoginContent(
    email:                String,
    password:             String,
    isLoading:            Boolean,
    snackbarHostState:    SnackbarHostState,
    onEmailChange:        (String) -> Unit,
    onPasswordChange:     (String) -> Unit,
    onLoginClick:         () -> Unit,
    onForgotPassword:     (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    initialShowForgotDialog: Boolean = false
) {
    var passwordVisible  by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(initialShowForgotDialog) }

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
            Spacer(modifier = Modifier.height(80.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_favorite),
                    contentDescription = "Logo",
                    tint = Brown,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text  = "MemoryCircle",
                style = MaterialTheme.typography.displayLarge,
                color = Ink
            )
            Text(
                text  = "cherish every moment",
                style = MaterialTheme.typography.bodyMedium,
                color = InkTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text      = "Welcome back,\nyour memories await",
                style     = MaterialTheme.typography.titleLarge,
                color     = Ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text     = "EMAIL",
                style    = MaterialTheme.typography.labelSmall,
                color    = InkSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value         = email,
                onValueChange = onEmailChange,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(28.dp),
                placeholder   = { Text("sarah@example.com", color = Brown.copy(alpha = 0.6f)) },
                leadingIcon   = {
                    Icon(
                        painter = painterResource(R.drawable.ic_email),
                        contentDescription = null,
                        tint = Brown
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine    = true,
                colors        = brandFieldColorsOnGradient()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text     = "PASSWORD",
                style    = MaterialTheme.typography.labelSmall,
                color    = InkSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value         = password,
                onValueChange = onPasswordChange,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(28.dp),
                placeholder   = { Text("••••••••", color = Brown.copy(alpha = 0.6f)) },
                leadingIcon   = {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = Brown
                    )
                },
                trailingIcon  = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.ic_visibility
                                else R.drawable.ic_visibility_off
                            ),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
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

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { showForgotDialog = true }) {
                    Text("Forgot password?", color = Brown, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            PrimaryButton(
                label   = "Sign In",
                onClick = onLoginClick,
                loading = isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "New to MemoryCircle? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGreen
                    )
                }
            }
        }

        if (showForgotDialog) {
            ForgotPasswordDialog(
                initialEmail = email,
                onDismiss    = { showForgotDialog = false },
                onSend       = { resetEmail ->
                    onForgotPassword(resetEmail)
                    showForgotDialog = false
                }
            )
        }
    }
}

/**
 * What: Dialog where the user enters an email to receive a password reset link.
 * Who: Shown by LoginContent when the user taps "Forgot password?".
 * When: While showForgotDialog is true.
 */
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss:    () -> Unit,
    onSend:       (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    val isValidEmail = resetEmail.isNotBlank() && resetEmail.contains("@") && resetEmail.contains(".")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
        title = {
            Text("Reset password", style = MaterialTheme.typography.titleLarge, color = Ink)
        },
        text = {
            Column {
                Text(
                    "Enter your email and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value         = resetEmail,
                    onValueChange = { resetEmail = it },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(16.dp),
                    singleLine    = true,
                    placeholder   = { Text("sarah@example.com", color = Brown.copy(alpha = 0.6f)) },
                    leadingIcon   = {
                        Icon(
                            painter = painterResource(R.drawable.ic_email),
                            contentDescription = null,
                            tint = Brown
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Sage,
                        unfocusedBorderColor = Beige
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(resetEmail) },
                enabled = isValidEmail
            ) {
                Text("Send reset link", color = if (isValidEmail) AccentGreen else BrownDisabled)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Brown)
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Previews — each one targets the whole screen at a different UI state.
// ---------------------------------------------------------------------------

/** Default empty form. */
@Preview(showBackground = true, name = "Login · empty")
@Composable
fun LoginScreenPreview() {
    MemoryCircleTheme {
        LoginContent(
            email                = "",
            password             = "",
            isLoading            = false,
            snackbarHostState    = remember { SnackbarHostState() },
            onEmailChange        = {},
            onPasswordChange     = {},
            onLoginClick         = {},
            onForgotPassword     = {},
            onNavigateToRegister = {}
        )
    }
}

/** Form filled in, ready to submit. */
@Preview(showBackground = true, name = "Login · filled")
@Composable
fun LoginScreenFilledPreview() {
    MemoryCircleTheme {
        LoginContent(
            email                = "ada@example.com",
            password             = "secret123",
            isLoading            = false,
            snackbarHostState    = remember { SnackbarHostState() },
            onEmailChange        = {},
            onPasswordChange     = {},
            onLoginClick         = {},
            onForgotPassword     = {},
            onNavigateToRegister = {}
        )
    }
}

/** Loading spinner on the Sign In button — login request in flight. */
@Preview(showBackground = true, name = "Login · loading")
@Composable
fun LoginScreenLoadingPreview() {
    MemoryCircleTheme {
        LoginContent(
            email                = "ada@example.com",
            password             = "secret123",
            isLoading            = true,
            snackbarHostState    = remember { SnackbarHostState() },
            onEmailChange        = {},
            onPasswordChange     = {},
            onLoginClick         = {},
            onForgotPassword     = {},
            onNavigateToRegister = {}
        )
    }
}

/** Forgot-password dialog open. */
@Preview(showBackground = true, name = "Login · forgot dialog")
@Composable
fun LoginScreenForgotDialogPreview() {
    MemoryCircleTheme {
        LoginContent(
            email                   = "ada@example.com",
            password                = "",
            isLoading               = false,
            snackbarHostState       = remember { SnackbarHostState() },
            onEmailChange           = {},
            onPasswordChange        = {},
            onLoginClick            = {},
            onForgotPassword        = {},
            onNavigateToRegister    = {},
            initialShowForgotDialog = true
        )
    }
}
