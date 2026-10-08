package com.dmytrosamoilov.offhand.core.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.dmytrosamoilov.offhand.core.common.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Follows the default network for the life of the process; the callback is
// registered once and never needs to come off.
class AndroidNetworkMonitor(
    context: Context,
) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val mutableIsUnmetered = MutableStateFlow(isCurrentNetworkUnmetered())
    override val isUnmetered: StateFlow<Boolean> = mutableIsUnmetered.asStateFlow()

    init {
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                mutableIsUnmetered.value = capabilities.isUnmetered()
            }

            override fun onLost(network: Network) {
                mutableIsUnmetered.value = false
            }
        })
    }

    private fun isCurrentNetworkUnmetered(): Boolean =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.isUnmetered() ?: false

    private fun NetworkCapabilities.isUnmetered(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
