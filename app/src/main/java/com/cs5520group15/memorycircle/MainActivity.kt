/**
 * What: The launcher Activity; initializes the notification stack, requests
 *       runtime notification permission, and hosts the Compose UI
 *       (MemoryCircleTheme + MemoryCircleNavigation).
 * Who:  Declared in AndroidManifest as the launcher; started by the Android OS.
 * When: Instantiated by the OS when the app is launched from the home screen;
 *       onCreate runs once per Activity creation.
 */

package com.cs5520group15.memorycircle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cs5520group15.memorycircle.data.NotificationsRepository
import com.cs5520group15.memorycircle.ui.navigation.MemoryCircleNavigation
import com.cs5520group15.memorycircle.ui.theme.MemoryCircleTheme

class MainActivity : ComponentActivity() {

    // Android 13+ requires runtime permission for system notifications. The
    // result itself is ignored — denial just means notifications never
    // surface (no crash), and the user can grant it later from system
    // settings.
    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Wire up the notification stack once per process. init() is
        // idempotent and the AuthStateListener inside the repo handles
        // sign-in / sign-out rebinding automatically.
        NotificationsRepository.init(applicationContext)
        ensureNotificationPermission()

        setContent {
            MemoryCircleTheme {
                MemoryCircleNavigation()
            }
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
