/**
 * What: Stateless check for whether the device has a validated internet
 *       connection, used to gate writes before Firestore silently queues them
 *       offline.
 * Who: Used by CreateGroupScreen, EditProfileScreen, GroupDetailScreen,
 *       AvatarViewerScreen, FriendsScreen, and AddFriendSearchScreen.
 * When: Called inline from those screens right before a network-dependent
 *       action (save / upload / create) to surface an offline state up front.
 */

package com.cs5520group15.memorycircle.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Lightweight connectivity check. Used by screens that need to gate writes
 * (e.g. CreateGroup, AddPost) so we don't silently let Firestore queue the
 * write into its offline-persistence cache and "succeed" without network —
 * the user wants to know up-front that there's no connection.
 *
 * Requires the ACCESS_NETWORK_STATE permission in AndroidManifest.xml.
 */
object NetworkUtil {

    /** True if the device currently has a validated internet connection. */
    fun isOnline(context: Context): Boolean {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
