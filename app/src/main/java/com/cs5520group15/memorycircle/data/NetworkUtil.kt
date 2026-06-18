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
