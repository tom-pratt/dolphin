package org.dolphinemu.dolphinemu.features.netplay

import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.content.IntentCompat

private const val TAGS = "WifiDirect"

/**
 * Logs the contents of a WifiP2p broadcast. Purely diagnostic - the receiver still parses the
 * intent itself for the extras it acts on.
 */
fun logWifiP2pBroadcast(intent: Intent, TAG: String = TAGS) {
    when (intent.action) {
        WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION state=${describeP2pState(state)}")
        }

        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
            val peers = IntentCompat.getParcelableExtra(
                intent,
                WifiP2pManager.EXTRA_P2P_DEVICE_LIST,
                WifiP2pDeviceList::class.java,
            )?.deviceList.orEmpty()
            Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION peers=${peers.size}")
            peers.forEach { Log.d(TAG, "  peer ${describeDevice(it)}") }
        }

        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
            val info = IntentCompat.getParcelableExtra(
                intent,
                WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                WifiP2pInfo::class.java,
            )
            val group = IntentCompat.getParcelableExtra(
                intent,
                WifiP2pManager.EXTRA_WIFI_P2P_GROUP,
                WifiP2pGroup::class.java,
            )
            Log.d(
                TAG,
                "WIFI_P2P_CONNECTION_CHANGED_ACTION groupFormed=${info?.groupFormed}" +
                    " isGroupOwner=${info?.isGroupOwner}" +
                    " groupOwnerAddress=${info?.groupOwnerAddress?.hostAddress}",
            )
            Log.d(TAG, "  group=${group?.let(::describeGroup)}")
            group?.clientList.orEmpty().forEach { Log.d(TAG, "  client ${describeDevice(it)}") }
        }

        WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
            val device = IntentCompat.getParcelableExtra(
                intent,
                WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                WifiP2pDevice::class.java,
            )
            Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ${device?.let(::describeDevice)}")
        }

        WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)
            Log.d(TAG, "WIFI_P2P_DISCOVERY_CHANGED_ACTION state=${describeDiscoveryState(state)}")
        }
    }
}

private fun describeP2pState(state: Int): String = when (state) {
    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> "ENABLED"
    WifiP2pManager.WIFI_P2P_STATE_DISABLED -> "DISABLED"
    else -> "UNKNOWN($state)"
}

private fun describeDiscoveryState(state: Int): String = when (state) {
    WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED -> "STARTED"
    WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED -> "STOPPED"
    else -> "UNKNOWN($state)"
}

private fun describeDevice(device: WifiP2pDevice): String =
    "name=${device.deviceName} address=${device.deviceAddress}" +
        " status=${describeDeviceStatus(device.status)}"

private fun describeDeviceStatus(status: Int): String = when (status) {
    WifiP2pDevice.CONNECTED -> "CONNECTED"
    WifiP2pDevice.INVITED -> "INVITED"
    WifiP2pDevice.FAILED -> "FAILED"
    WifiP2pDevice.AVAILABLE -> "AVAILABLE"
    WifiP2pDevice.UNAVAILABLE -> "UNAVAILABLE"
    else -> "UNKNOWN($status)"
}

private fun describeGroup(group: WifiP2pGroup): String {
    val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) group.frequency else null
    return "network=${group.networkName} isGroupOwner=${group.isGroupOwner}" +
        " frequency=$frequency interface=${group.`interface`}" +
        " clients=${group.clientList.orEmpty().size}"
}
