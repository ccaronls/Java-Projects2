package cc.lib.mp.android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Parcelable
import android.util.Log
import cc.lib.android.toaster
import cc.lib.utils.launchIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Chris Caron on 4/3/26.
 */

// TODO: Make this sealed
@SuppressLint("MissingPermission")
class P2P(val activity: P2PActivity) : BroadcastReceiver() {

	val peers: StateFlow<List<WifiP2pDevice>>
		get() = _peers

	fun createGroup(): CompletableDeferred<Boolean> {
		if (groupFormed.isCompleted)
			return groupFormed
		startJob("Creating Group ...", groupFormed) {
			var tries = 1
			do {
				p2pMgr.createGroup(channel, P2PActionListener {
					groupFormed.complete(true)
				})
				delay(1000L * tries)
			} while (tries++ < 5 && groupFormed.isActive)
			if (groupFormed.isActive)
				groupFormed.complete(false)
		}
		return groupFormed
	}

	fun startPeerDiscovery(): CompletableDeferred<Boolean> {
		val ready = CompletableDeferred<Boolean>()
		startJob("Start Peer Discovery ...", ready) {
			withTimeoutOrNull(3000) {
				p2pMgr.discoverPeers(channel, P2PActionListener {
					ready.complete(true)
				})
			} ?: ready.complete(false)
		}
		return ready
	}

	fun stopPeerDiscovery(): CompletableDeferred<Boolean> {
		val stopped = CompletableDeferred<Boolean>()
		startJob("Stopping Peer discovery ...", stopped) {
			withTimeoutOrNull(1000) {
				p2pMgr.stopPeerDiscovery(channel, P2PActionListener {
					stopped.complete(true)
				})
			}
			stopped.complete(false)
		}
		return stopped
	}

	fun releaseGroup(): CompletableDeferred<Boolean> {
		val released = CompletableDeferred<Boolean>()
		startJob("Releasing Group ...", released) {
			if (groupFormed.isCompleted && groupFormed.await()) {
				withTimeoutOrNull(3000) {
					suspendCoroutine {
						p2pMgr.removeGroup(channel, P2PActionListener {
							groupFormed = CompletableDeferred() // recreate this so we can create a new one in the future
							it.resume(Unit)
							released.complete(true)
						})
					}
				}
				released.complete(false)
			}
		}
		return released
	}

	fun shutDown(): CompletableDeferred<Boolean> {
		val released = CompletableDeferred<Boolean>()
		currentJob?.cancel()
		startJob(null, released) {
			releaseGroup().await()
			stopPeerDiscovery().await()
			if (registered)
				activity.unregisterReceiver(this@P2P)
			registered = false
			released.complete(true)
		}
		return released
	}

	// INTERNAL ///////////////////////////////////////////////////


	inner class P2PActionListener(val onSuccessCB: () -> Unit) : ActionListener {
		override fun onSuccess() {
			onSuccessCB()
		}

		override fun onFailure(reason: Int) {
			activity.toaster(statusToString(reason))
		}
	}

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action
		Log.d(TAG, "onReceive: $action")
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
			// Determine if Wifi P2P mode is enabled or not, alert
			// the Activity.
			val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				p2pEnabled.complete(true)
			}
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

			// The peer list has changed! We should probably do something about
			// that.
			p2pMgr.requestPeers(channel, peerListener)
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {

			// Connection state changed! We should probably do something about
			// that.
			p2pMgr.requestPeers(channel, peerListener)
			Log.d(TAG, "P2P peers changed")
			val networkInfo = intent
				.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
			if (networkInfo!!.isConnected) {

				// We are connected with the other device, request connection
				// info to find group owner IP
				p2pMgr.requestConnectionInfo(channel) { info ->
					info.groupFormed
				}
			} else {
				// its a disconnect
			}
		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
			//DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
			//      .findFragmentById(R.id.frag_list);
			val device =
				intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice?
			if (device != null) {
				Log.d(TAG, "Device Changed: " + device.deviceName)
				//onThisDeviceUpdated(device)
			}
		}
	}

	private fun startJob(message: String?, completed: CompletableDeferred<Boolean>, block: suspend CoroutineScope.() -> Unit) {
		require(!completed.isCompleted)
		currentJob = CoroutineScope(Dispatchers.Main).launch { block() }
		currentJob?.invokeOnCompletion {
			completed.complete(false)
		}
		message?.let { msg ->
			launchIn {
				withTimeoutOrNull(1000) {
					completed.await()
				} ?: run {
					activity.showProgressDialog(message, completed, false)
					completed.await()
					currentJob?.cancel()
				}
			}
		}
	}

	private val peerListener = PeerListListener { list ->
		_peers.value = list.deviceList.toList()
	}

	private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())

	private val p2pMgr: WifiP2pManager by lazy {
		activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
	}
	private val p2pFilter by lazy {
		IntentFilter().apply {
			addAction(android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
			//p2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
			addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
			addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
			addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
		}
	}
	private val channel: WifiP2pManager.Channel by lazy {
		p2pMgr.initialize(activity, activity.mainLooper, {
			// channel has been disconnected, try to restart ?

		})
	}

	fun isAvailable(): Boolean {
		if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
			Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
			return false
		}

		activity.getSystemService(Context.WIFI_P2P_SERVICE) ?: return false
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			val wifiManager =
				activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
			if (!wifiManager.isP2pSupported) {
				return false
			}
		}
		return true
	}

	private val p2pEnabled = CompletableDeferred<Boolean>()
	private var groupFormed = CompletableDeferred<Boolean>()
	private var currentJob: Job? = null
	private var registered = false
	private val TAG = "P2P"

	fun statusToString(status: Int): String {
		when (status) {
			WifiP2pDevice.AVAILABLE -> return activity.getString(R.string.wifi_conn_status_available)
			WifiP2pDevice.CONNECTED -> return activity.getString(R.string.wifi_conn_status_connected)
			WifiP2pDevice.FAILED -> return activity.getString(R.string.wifi_conn_status_failed)
			WifiP2pDevice.INVITED -> return activity.getString(R.string.wifi_conn_status_invited)
			WifiP2pDevice.UNAVAILABLE -> return activity.getString(R.string.wifi_conn_status_unavailable)
		}
		return activity.getString(R.string.wifi_conn_status_unknown)
	}


	init {
		if (isAvailable()) {
			if (!registered)
				activity.registerReceiver(this, p2pFilter)
			registered = true
		}
	}

}