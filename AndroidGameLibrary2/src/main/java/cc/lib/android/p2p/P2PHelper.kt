package cc.lib.mp.android

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import cc.lib.android.CCActivityBase
import cc.lib.android.R
import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import java.net.InetAddress

@SuppressLint("MissingPermission")
abstract class P2PHelper(activity: CCActivityBase) : BroadcastReceiver(), PeerListListener,
	ConnectionInfoListener, GroupInfoListener, ActivityLifecycleCallbacks {
	private val activity: CCActivityBase
	private val p2pMgr: WifiP2pManager
	private val p2pFilter: IntentFilter
	private val channel: WifiP2pManager.Channel
	private val peers: MutableList<WifiP2pDevice> = ArrayList()
	private var server: AGameServer? = null
	private var client: AGameClient? = null
	private var registered = false

	init {
		require(isP2PAvailable(activity)) { "P2P Not supported" }
		this.activity = activity
		p2pFilter = IntentFilter()
		p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
		//p2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
		p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
		p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
		p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
		p2pMgr = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
		channel = p2pMgr.initialize(activity, activity.mainLooper, null)
	}

	protected abstract fun onP2PEnabled(enabled: Boolean)
	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action
		Log.d(TAG, "onReceive: $action")
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
			// Determine if Wifi P2P mode is enabled or not, alert
			// the Activity.
			val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
			if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				onP2PEnabled(true)
				if (server != null) {
					p2pMgr.createGroup(channel, object : MyActionListener("createGroup(server)") {
						override fun onSuccess() {
							p2pMgr.requestGroupInfo(channel, this@P2PHelper)
						}
					})
				}
			} else {
				onP2PEnabled(false)
			}
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

			// The peer list has changed! We should probably do something about
			// that.
			p2pMgr.requestPeers(channel, this)
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {

			// Connection state changed! We should probably do something about
			// that.
			p2pMgr.requestPeers(channel, this)
			Log.d(TAG, "P2P peers changed")
			val networkInfo = intent
				.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
			if (networkInfo!!.isConnected) {

				// We are connected with the other device, request connection
				// info to find group owner IP
				p2pMgr.requestConnectionInfo(channel, this)
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
				onThisDeviceUpdated(device)
			}
		}
	}

	protected abstract fun onThisDeviceUpdated(device: WifiP2pDevice)
	fun start(server: AGameServer) {
		require(!(client != null || this.server != null)) { "Already started" }
		this.server = server
		setRegistered(true)
		activity.application.registerActivityLifecycleCallbacks(this)
	}

	@Synchronized
	private fun setRegistered(enable: Boolean) {
		if (enable == registered) return
		registered = if (enable) {
			activity.registerReceiver(this, p2pFilter)
			true
		} else {
			activity.unregisterReceiver(this)
			false
		}
	}

	override fun onGroupInfoAvailable(group: WifiP2pGroup?) {
		if (group != null) {
			Log.i(
				TAG, """
 	Group Info: 
 	Interface: ${group.getInterface()}
 	Network Name: ${group.networkName}
 	Owner: ${group.owner.deviceName}
 	passPhrase: ${group.passphrase}
 	""".trimIndent()
			)
		}
	}

	fun start(client: AGameClient) {
		require(!(this.client != null || server != null)) { "Already started" }
		this.client = client
		setRegistered(true)
		activity.application.registerActivityLifecycleCallbacks(this)
		p2pMgr.discoverPeers(channel, MyActionListener("start(client)"))
	}

	fun stop() {
		if (server != null) {
			p2pMgr.removeGroup(channel, MyActionListener("removeGroup"))
		}
		client = null
		server = null
		p2pMgr.stopPeerDiscovery(channel, MyActionListener("stopPeerDiscovery"))
		setRegistered(false)
		activity.application.unregisterActivityLifecycleCallbacks(this)
	}

	private open inner class MyActionListener internal constructor(val action: String) :
		WifiP2pManager.ActionListener {
		override fun onSuccess() {
			Log.d(TAG, "$action Success")
		}

		override fun onFailure(reason: Int) {
			val msg = action + " Failure " + getFailureReasonString(reason)
			Log.e(TAG, msg)
			activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_LONG).show() }
		}
	}

	protected abstract fun onPeersList(peers: List<WifiP2pDevice>)
	override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
		val refreshedPeers: List<WifiP2pDevice> = ArrayList(peerList.deviceList)
		if (refreshedPeers != peers) {
			synchronized(peers) {
				peers.clear()
				peers.addAll(refreshedPeers)
			}

			// If an AdapterView is backed by this data, notify it
			// of the change. For instance, if you have a ListView of
			// available peers, trigger an update.
			//((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

			// Perform any other updates needed based on the new list of
			// peers connected to the Wi-Fi P2P network.
			p2pMgr.requestGroupInfo(channel, this)
			onPeersList(peers)
		}
		if (peers.size == 0) {
			Log.d(TAG, "No devices found")
		}
	}

	@Throws(Exception::class)
	fun connect(device: WifiP2pDevice) {
		try {
			val config = WifiP2pConfig()
			config.deviceAddress = device.deviceAddress
			config.wps.setup = WpsInfo.PBC
			config.groupOwnerIntent = 0
			p2pMgr.connect(
				channel,
				config,
				object : MyActionListener("connect(" + device.deviceName + ")") {
					override fun onSuccess() {
						super.onSuccess()
						p2pMgr.requestConnectionInfo(channel, this@P2PHelper)
					}
				})
		} catch (e: Throwable) {
			throw Exception(e)
		}
	}

	fun setDeviceName(name: String) {
		try {
			val m = p2pMgr.javaClass.getMethod(
				"setDeviceName",
				*arrayOf(
					WifiP2pManager.Channel::class.java, String::class.java,
					WifiP2pManager.ActionListener::class.java
				)
			)
			m.invoke(p2pMgr, channel, name, MyActionListener("setDeviceName($name)"))
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
		Log.d(TAG, "onConnectionInfoAvailable: $info")

		// After the group negotiation, we can determine the group owner
		// (server).
		if (info != null && info.groupOwnerAddress != null && info.groupFormed) {
			val groupOwnerAddress = info.groupOwnerAddress.hostAddress
			// Do whatever tasks are specific to the group owner.
			// One common case is creating a group owner thread and accepting
			// incoming connections.
			onGroupFormed(info.groupOwnerAddress, groupOwnerAddress)
		}
	}

	protected abstract fun onGroupFormed(addr: InetAddress, ipAddress: String)
	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
	override fun onActivityStarted(activity: Activity) {
		Log.i(TAG, "activity started")
		setRegistered(true)
	}

	override fun onActivityResumed(activity: Activity) {}
	override fun onActivityPaused(activity: Activity) {}
	override fun onActivityStopped(activity: Activity) {
		Log.w(TAG, "activity stopped")
		setRegistered(false)
	}

	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
	override fun onActivityDestroyed(activity: Activity) {}
	private fun getFailureReasonString(reason: Int): String {
		when (reason) {
			WifiP2pManager.ERROR -> return activity.getString(R.string.wifi_failure_reason_error)
			WifiP2pManager.BUSY -> return activity.getString(R.string.wifi_failure_reason_busy)
			WifiP2pManager.P2P_UNSUPPORTED -> return activity.getString(R.string.wifi_failure_reason_p2p_unsupported)
		}
		return activity.getString(R.string.wifi_failure_reason_unknown)
	}

	companion object {
		@JvmField
		val TAG = "P2PGame" + P2PHelper::class.java.simpleName

		@JvmStatic
		fun isP2PAvailable(context: Context): Boolean {
			if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
				Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
				return false
			}
			val p2pMgr = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
				?: return false
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				val wifiManager =
					context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
				if (!wifiManager.isP2pSupported) {
					return false
				}
			}
			return true
		}

		@JvmStatic
		fun statusToString(status: Int, activity: Context): String {
			when (status) {
				WifiP2pDevice.AVAILABLE -> return activity.getString(R.string.wifi_conn_status_available)
				WifiP2pDevice.CONNECTED -> return activity.getString(R.string.wifi_conn_status_connected)
				WifiP2pDevice.FAILED -> return activity.getString(R.string.wifi_conn_status_failed)
				WifiP2pDevice.INVITED -> return activity.getString(R.string.wifi_conn_status_invited)
				WifiP2pDevice.UNAVAILABLE -> return activity.getString(R.string.wifi_conn_status_unavailable)
			}
			return activity.getString(R.string.wifi_conn_status_unknown)
		}
	}
}
