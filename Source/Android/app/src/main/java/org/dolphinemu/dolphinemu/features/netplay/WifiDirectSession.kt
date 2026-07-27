package org.dolphinemu.dolphinemu.features.netplay

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.content.IntentCompat
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

abstract class WifiDirectSession(
    protected val application: Application,
    protected val manager: WifiP2pManager,
    private val onClosed: () -> Unit,
) : BroadcastReceiver() {

    protected abstract val TAG: String

    protected val channel: WifiP2pManager.Channel =
        manager.initialize(application, application.mainLooper, null)

    private val wifiManager: WifiManager by lazy {
        application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    protected val wifiLock: WifiManager.WifiLock by lazy {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiManager.createWifiLock(mode, "dolphin-netplay").apply {
            setReferenceCounted(false)
        }
    }

    private val _currentGroup =
        MutableSharedFlow<WifiP2pGroup?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _currentWifiP2pInfo =
        MutableSharedFlow<WifiP2pInfo?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _peers = MutableStateFlow<Collection<WifiP2pDevice>>(emptyList())

    protected val currentGroupNetworkName = _currentGroup
        .map { it?.networkName }
        .distinctUntilChanged()

    protected val currentHostAddress = _currentWifiP2pInfo
        .map { it?.groupOwnerAddress?.hostAddress }
        .distinctUntilChanged()

    protected val peers = _peers.asStateFlow()

    init {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        }
        application.registerReceiver(this, intentFilter)
    }

    @Volatile
    var isClosed = false
        private set

    protected abstract fun onClose()

    suspend fun close() {
        if (isClosed) return
        isClosed = true

        withContext(NonCancellable) {
            onClose()
            clearGroup()
            channel.close() //TODO measure how long this takes
            application.unregisterReceiver(this@WifiDirectSession)
            wifiLock.release()
            onClosed()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchExistingGroup() {
        manager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            _currentGroup.tryEmit(group)
        }
    }

    protected sealed interface ClearGroupResult {
        data object Success : ClearGroupResult
        data object FailedToRemoveDolphinGroup : ClearGroupResult
        data class ExistingNonDolphinGroup(val networkName: String) : ClearGroupResult
    }

    protected suspend fun clearGroup(): ClearGroupResult {
        fetchExistingGroup()

        //withTimeout due to the break scenario
        return currentGroupNetworkName
            .transformLatest { current ->
                when (current) {
                    null -> emit(ClearGroupResult.Success)

                    else -> {//NETWORK_NAME, "DIRECT-dolphin-netplay", "DIRECT-bb-dolphin-netplay" -> {
                        // Attempt to remove stale dolphin group. Note that, similar to all WifiP2p
                        // ActionListeners, success does not indicate the group was removed. only
                        // that the request succeeded. Hence, the timeout since we are still waiting
                        // for a null group to come through.
                        repeat(3) {
                            val removeGroupResult =
                                awaitActionListener { manager.removeGroup(channel, it) }
                            when (removeGroupResult) {
                                ActionListenerResult.Success -> delay(10.seconds)
                                is ActionListenerResult.Failure -> delay(2.seconds)
                            }
                        }
                        emit(ClearGroupResult.FailedToRemoveDolphinGroup)
                    }

//                    else -> emit(GroupPreparationResult.ExistingNonDolphinGroup(current))
                }
            }.first()
    }

    override fun onReceive(context: Context, intent: Intent) {
        logWifiP2pBroadcast(intent, TAG)

        when (intent.action) {
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val group = IntentCompat.getParcelableExtra(
                    intent,
                    WifiP2pManager.EXTRA_WIFI_P2P_GROUP,
                    WifiP2pGroup::class.java,
                )
                _currentGroup.tryEmit(group)

                val wifiP2pInfo = IntentCompat.getParcelableExtra(
                    intent,
                    WifiP2pManager.EXTRA_WIFI_P2P_INFO,
                    WifiP2pInfo::class.java,
                )
                _currentWifiP2pInfo.tryEmit(wifiP2pInfo)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                val peers = IntentCompat.getParcelableExtra(
                    intent,
                    WifiP2pManager.EXTRA_P2P_DEVICE_LIST,
                    WifiP2pDeviceList::class.java,
                )
                _peers.value = peers?.deviceList.orEmpty()
            }
        }
    }

    protected sealed interface ActionListenerResult {
        data object Success : ActionListenerResult
        data class Failure(val reason: Int) : ActionListenerResult
    }

    protected suspend fun awaitActionListener(block: (WifiP2pManager.ActionListener) -> Unit): ActionListenerResult =
        suspendCancellableCoroutine { continuation ->
            block(
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        continuation.resume(ActionListenerResult.Success)
                    }

                    override fun onFailure(reason: Int) {
                        continuation.resume(ActionListenerResult.Failure(reason))
                    }
                }
            )
        }

    companion object {
        protected const val NETWORK_NAME = "DIRECT-aa-dolphin-netplay"

        protected const val PASSPHRASE = "dolphinnetplay"

        protected const val SERVICE_TYPE = "_dolphinnetplay._tcp"

        protected const val TXT_MAP_NAME = "name"

        protected const val GENERIC_FAILURE_MESSAGE =
            "Enabling WiFi direct failed. Try restarting the device."
    }
}
