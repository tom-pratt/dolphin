package org.dolphinemu.dolphinemu.features.netplay

import android.Manifest
import android.app.Application
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class WifiDirectHostSession(
    application: Application,
    manager: WifiP2pManager,
    onClosed: () -> Unit,
) : WifiDirectSession(application, manager, onClosed) {

    override val TAG: String = "WifiDirectHostSession"

    sealed interface StartAdvertisingResult {
        data object Success : StartAdvertisingResult
        data class Failure(val message: String) : StartAdvertisingResult
    }

    override fun onClose() = Unit

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    suspend fun startAdvertising(hostName: String): StartAdvertisingResult {
        when (val clearGroupResult = clearGroup()) {
            ClearGroupResult.Success -> Unit

            is ClearGroupResult.FailedToRemoveDolphinGroup -> return StartAdvertisingResult.Failure(
                GENERIC_FAILURE_MESSAGE
            )

            is ClearGroupResult.ExistingNonDolphinGroup -> return StartAdvertisingResult.Failure(
                "WiFi direct is being used by another app (${clearGroupResult.networkName})."
            )
        }

        wifiLock.acquire()

        //TODO remove service when starting gameplay
        val serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
            "DolphinNetplay",
            SERVICE_TYPE,
            mapOf(
                TXT_MAP_NAME to hostName,
            ),
        )
        val addLocalServiceResult =
            awaitActionListener { manager.addLocalService(channel, serviceInfo, it) }
        if (addLocalServiceResult is ActionListenerResult.Failure) {
            Log.d(TAG, "addLocalService failed with reason=${addLocalServiceResult.reason}")
            return StartAdvertisingResult.Failure(GENERIC_FAILURE_MESSAGE)
        }

//        //TODO if api 33 use startListening else discoverPeers
//        val startListeningResult = awaitActionListener { manager.startListening(channel, it) }
//        if (startListeningResult is ActionListenerResult.Failure) {
//            Log.d(TAG, "startListening failed with reason=${startListeningResult.reason}")
//            return StartAdvertisingResult.Failure(GENERIC_FAILURE_MESSAGE)
//        }

        val configBuilder = WifiP2pConfig.Builder()
            .setNetworkName(NETWORK_NAME)
            .setPassphrase(PASSPHRASE)
//            .enablePersistentMode(true)

        val config = configBuilder.build()
        val createGroupResult = awaitActionListener { manager.createGroup(channel, config, it) }
        if (createGroupResult is ActionListenerResult.Failure) {
            Log.d(TAG, "createGroup failed with reason=${createGroupResult.reason}")
            return StartAdvertisingResult.Failure(GENERIC_FAILURE_MESSAGE)
        } else {
            Log.d(TAG, "createGroup succeeded")
        }

        val expectedGroup = withTimeoutOrNull(GROUP_FORMATION_TIMEOUT) {
            currentGroupNetworkName.first { it == NETWORK_NAME }
        }
        if (expectedGroup == null) {
            Log.d(TAG, "group did not form within $GROUP_FORMATION_TIMEOUT")
            return StartAdvertisingResult.Failure(GENERIC_FAILURE_MESSAGE)
        }

        return StartAdvertisingResult.Success
    }

    companion object {
        private val GROUP_FORMATION_TIMEOUT = 15.seconds
    }
}
