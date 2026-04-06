package cc.lib.mp.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import cc.lib.android.CCActivityBase
import cc.lib.android.toaster
import cc.lib.net.INetClient
import cc.lib.net.INetServer
import cc.lib.utils.launchIn
import cc.lib.utils.toInetAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.net.InetAddress

/**
 * Created by Chris Caron on 7/17/21.
 *
 * Usage:
 *
 * p2pInit() - Does permissions / availability checks. onP2PReady called when ready or error popup.
 * p2pStart() - Shows a start as server or client dialog. onP2PClient / onP2PServer called when user chooses
 */
abstract class P2PActivity : CCActivityBase() {

	private var initialized = CompletableDeferred<Boolean>()
	private lateinit var p2p: P2P


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		p2p = P2P(this)
	}

	override fun onStop() {
		p2p.shutDown()
		super.onStop()
	}

	/**
	 * Call this before anything else to make sure the system can run p2p
	 */
	fun p2pInit(): CompletableDeferred<Boolean> {
		if (!initialized.isCompleted) {
			launchIn {
				if (p2p.isAvailable()) {
					checkPermissions(*requiredPermissions)
				} else {
					toaster("P2P Not Supported")
					initialized.complete(false)
				}
			}
		}
		return initialized
	}

	private val requiredPermissions: Array<String>
		get() = mutableListOf(
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.CHANGE_WIFI_STATE,
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.CHANGE_NETWORK_STATE,
			Manifest.permission.INTERNET
		).also {
			it.addAll(extraPermissions)
		}.toTypedArray<String>()
	protected val extraPermissions: Array<String>
		protected get() = emptyArray()

	final override fun onPermissionLimited(permissionsNotGranted: List<String>) {
		initialized.complete(false)
		newDialogBuilder().setTitle(R.string.p2p_popup_title_missing_permissions)
			.setMessage(
				getString(
					R.string.p2p_popup_message_missing_permissions,
					permissionsNotGranted.toString()
				)
			)
			.setNegativeButton(R.string.popup_button_ok, null).show()
	}

	final override fun onAllPermissionsGranted() {
		initialized.complete(true)
		toaster("P2P Initialized SUCCESS")
	}

	/**
	 * Clients call this to get a list of servers to connect to. If the user chooses an address then complete
	 * with the InetAddress of that server. Otherwise complete with null if user cancelled.
	 */
	fun p2pOpenJoinGameDialog(client: INetClient): CompletableDeferred<InetAddress?> {
		val addressCompletable = CompletableDeferred<InetAddress?>()
		launchIn {
			if (p2pInit().await())
				p2p.startPeerDiscovery().await()
			client.startDiscovery()
			val flow1 = p2p.peers.map {
				it.map { p2pDevice ->
					Pair(
						p2pDevice.deviceAddress.toInetAddress(),
						"${p2pDevice.deviceName}:${p2p.statusToString(p2pDevice.status)}",
					)
				}.toMap().toMutableMap()
			}

			val flow2 = client.discoveredHosts.map {
				it.map { host ->
					Pair(
						host.value.hostAddress.toInetAddress(),
						"${host.value.hostName}:${host.value.serverName}",
					)
				}.toMap()
			}


			object : PeerChooserDialog(this@P2PActivity, combine(flow1, flow2) { f1, f2 ->
				f1.also {
					it.putAll(f2)
				}.toList()
			}) {
				override fun onConnectionChoice(address: InetAddress?) {
					addressCompletable.complete(address)
				}
			}
		}
		return addressCompletable
	}

	/**
	 * When running as a server we can see the connected clients here
	 */
	fun p2pOpenClientConnectionsDialog(server: INetServer) {
		P2PClientConnectionsDialog(this, server)
	}

	val deviceName: String
		get() = prefs.getString("deviceName", null)
			?: "${Build.BRAND}-${Build.MODEL}-${Build.VERSION.SDK_INT}-${getString(R.string.app_name)}"

	fun changeDeviceName(): CompletableDeferred<String> {
		val name = CompletableDeferred<String>()
		showEditTextDialog("Change Display Name", deviceName, "") {
			prefs.edit().putString("deviceName", it).apply()
		}.setOnDismissListener {
			name.complete(deviceName)
		}
		return name
	}

	suspend fun p2pCreateGroup(): Boolean {
		if (!p2p.isAvailable())
			return false
		return p2p.createGroup().await()
	}

}
