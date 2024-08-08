package cc.lib.mp.android

import android.Manifest
import android.content.DialogInterface
import android.os.Build
import cc.lib.android.CCActivityBase
import cc.lib.android.SpinnerTask
import cc.lib.crypt.Cypher
import cc.lib.mp.android.P2PHelper.Companion.isP2PAvailable
import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import cc.lib.net.GameClient
import cc.lib.net.GameServer

/**
 * Created by Chris Caron on 7/17/21.
 *
 * Usage:
 *
 * p2pInit() - Does permissions / availability checks. onP2PReady called when ready or error popup.
 * p2pStart() - Shows a start as server or client dialog. onP2PClient / onP2PServer called when user chooses
 */
abstract class P2PActivity : CCActivityBase() {
	var server: AGameServer? = null
		private set
	var client: AGameClient? = null
		private set
	private var mode = P2PMode.DONT_KNOW

	protected abstract val connectPort: Int
	protected abstract val version: String
	protected abstract val maxConnections: Int
	protected val cypher: Cypher?
		protected get() = null

	enum class P2PMode {
		CLIENT,
		SERVER,
		DONT_KNOW
	}

	override fun onStop() {
		super.onStop()
		p2pShutdown()
	}
	/**
	 * Call this before anything else to make sure the system can run p2p
	 */
	/**
	 * Call this before anything else to make sure the system can run p2p
	 */
	@JvmOverloads
	fun p2pInit(mode: P2PMode = P2PMode.DONT_KNOW) {
		client = null
		server = null
		this.mode = mode
		if (!isP2PAvailable(this)) {
			newDialogBuilder().setTitle(R.string.p2p_popup_title_unsupported)
				.setMessage(R.string.p2p_popup_message_unsupported)
				.setNegativeButton(R.string.popup_button_ok, null).show()
		} else {
			checkPermissions(*requiredPermissions)
		}
	}

	val requiredPermissions: Array<String>
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

	override fun onPermissionLimited(permissionsNotGranted: List<String>) {
		newDialogBuilder().setTitle(R.string.p2p_popup_title_missing_permissions)
			.setMessage(
				getString(
					R.string.p2p_popup_message_missing_permissions,
					permissionsNotGranted.toString()
				)
			)
			.setNegativeButton(R.string.popup_button_ok, null).show()
	}

	override fun onAllPermissionsGranted() {
		onP2PReady()
	}

	/**
	 * Called when p2pInit has completed successfully. Default is to just call p2pStart
	 */
	protected open fun onP2PReady() {
		p2pStart()
	}

	/**
	 * Override this to use your own method for choosing to start as host or client. If not overridden then
	 * the default behavior is a dialog to choose mode. Choosing client mode executes: p2pInitAsClient and choosing
	 * server mode executes p2pInitAsHost. Those methods bring up their own respective dialogs to guide user through
	 * connection process.
	 */
	open fun p2pStart() {
		when (mode) {
			P2PMode.CLIENT -> p2pInitAsClient()
			P2PMode.SERVER -> p2pInitAsServer()
			P2PMode.DONT_KNOW -> newDialogBuilder().setTitle(R.string.p2p_popup_title_choose_mode)
				.setItems(resources.getStringArray(R.array.p2p_popup_choose_mode_items)) { dialog: DialogInterface?, which: Int ->
					when (which) {
						0 -> p2pInitAsClient()
						1 -> p2pInitAsServer()
					}
				}.setNegativeButton(R.string.popup_button_cancel, null).show()
		}
	}

	/**
	 * Interface to functions available only when in host mode
	 */
	interface P2PServer {
		fun getServer(): AGameServer
		fun openConnections()
	}

	/**
	 * Interface to methods available only when in client mode
	 */
	interface P2PClient {
		fun getClient(): AGameClient
	}

	protected fun newGameClient(): AGameClient {
		return GameClient(deviceName, version, cypher)
	}

	fun isRunning(): Boolean {
		return server != null || client != null
	}

	fun p2pInitAsClient() {
		require(!isRunning()) { "P2P Mode already in progress. Call p2pShutdown first." }
		client = newGameClient().also {
			it.addListener(object : AGameClient.Listener {
				override fun onDisconnected(reason: String, serverInitiated: Boolean) {
					runOnUiThread { p2pShutdown() }
				}
			})
			P2PJoinGameDialog(this, it, deviceName, connectPort)
			onP2PClient(object : P2PClient {
				override fun getClient(): AGameClient {
					return it
				}
			})
		}
	}

	/**
	 * Called when the client mode is initialized
	 *
	 * @param p2pClient
	 */
	protected abstract fun onP2PClient(p2pClient: P2PClient)
	protected fun newGameServer(): AGameServer {
		return GameServer(deviceName, connectPort, version, cypher, maxConnections)
	}

	fun p2pInitAsServer() {
		require(!isRunning()) { "P2P Mode already in progress. Call p2pShutdown first." }
		server = newGameServer().also {
			object : P2PClientConnectionsDialog(this, it, deviceName) {
				override fun onServerSuccess(server: P2PServer) {
					onP2PServer(server)
				}
			}
		}
	}

	/**
	 * Called when the server context is ready
	 *
	 * @param p2pServer
	 */
	protected abstract fun onP2PServer(p2pServer: P2PServer)

	/**
	 * Called from UI thread
	 */
	fun p2pShutdown() {
		log.debug("p2pShutdown")
		object : SpinnerTask<Void>(this@P2PActivity) {

			init {
				progressMessage = getString(R.string.p2p_progress_message_disconnecting)
			}

			override suspend fun doIt(args: Void?) {
				server?.stop()
				client?.disconnect()
			}

			override fun onCompleted() {
				server = null
				client = null
				onP2PShutdown()
			}
		}.postExecute()
	}

	val isP2PConnected: Boolean
		get() = client != null || server != null

	protected open fun onP2PShutdown() {}
	val deviceName: String
		get() {
			val name = getString(R.string.app_name)
			return String.format(
				"%s-%s-%s %s-%s",
				Build.BRAND,
				Build.MODEL,
				Build.VERSION.SDK_INT,
				name,
				version
			)
		}
}
