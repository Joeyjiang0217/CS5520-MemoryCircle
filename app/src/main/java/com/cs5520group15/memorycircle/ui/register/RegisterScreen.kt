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
    onRegisterSuccess:  () -> Unit,
    onNavigateToLogin:  () -> Unit,
    viewModel: RegisterViewModel = viewModel()
) {
    val name      by viewModel.name.collectAsStateWithLifecycle()
    val email     by viewModel.email.collectAsStateWithLifecycle()
    val password  by viewModel.password.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var passwordVisible   by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RegisterViewModel.RegisterEvent.ShowSnackbar   -> snackbarHostState.showSnackbar(event.message)
                is RegisterViewModel.RegisterEvent.NavigateToHome -> onRegisterSuccess()
            }
        }
    }

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
                onValueChange = viewModel::onNameChange,
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
                onValueChange = viewModel::onEmailChange,
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
                onValueChange = viewModel::onPasswordChange,
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
                onClick = viewModel::onRegisterClick,
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

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MemoryCircleTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}
