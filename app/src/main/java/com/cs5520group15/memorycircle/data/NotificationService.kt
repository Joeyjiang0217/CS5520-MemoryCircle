package com.cs5520group15.memorycircle.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cs5520group15.memorycircle.R

/**
 * What: Thin wrapper over Android's NotificationManager. Creates a single
 *       high-importance channel once (idempotent) so push triggers anywhere
 *       in the app can call NotificationService.show(...) without worrying
 *       about channel registration. Each call uses a fresh id so back-to-back
 *       notifications don't replace each other in the shade.
 *
 *       The small icon reuses ic_friends — it's already a monochrome vector
 *       drawable so it tints correctly inside the status bar without us
 *       having to ship a dedicated notification asset.
 * Who: Used by NotificationsRepository's Firestore listeners.
 * When: NotificationService.init(appContext) is called once from
 *       MainActivity.onCreate; show() is called per detected event.
 */
object NotificationService {

    private const val CHANNEL_ID   = "memorycircle_default"
    private const val CHANNEL_NAME = "MemoryCircle"

    private var initialized = false
    private var nextId      = 1000

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friend requests, group activity, and new memory posts"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a heads-up notification with the given title + body. Each call
     * gets a unique id so multiple notifications stack instead of replacing
     * each other. SecurityException (no POST_NOTIFICATIONS permission on
     * API 33+) is swallowed — settings UI is the user's recourse.
     */
    fun show(context: Context, title: String, body: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_friends)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        runCatching {
            NotificationManagerCompat.from(context).notify(nextId++, builder.build())
        }
    }
}
