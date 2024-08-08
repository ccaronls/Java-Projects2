package cc.lib.mp.android

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import cc.lib.android.SpinnerTask
import cc.lib.mp.android.P2PHelper.Companion.statusToString
import cc.lib.net.AGameClient
import cc.lib.net.GameCommand
import cc.lib.utils.KLock
import java.net.InetAddress

class P2PJoinGameDialog(
	val context: P2PActivity,
	val client: AGameClient,
	clientName: String?,
	val connectPort: Int
) : BaseAdapter(), OnItemClickListener, DialogInterface.OnClickListener,
	DialogInterface.OnCancelListener, Runnable, AGameClient.Listener {
	val lvHost: ListView = ListView(context)
	val p2pDevices: MutableList<WifiP2pDevice> = ArrayList()
	val dialog: Dialog
	val helper: P2PHelper
	val connectLock = KLock(1)

	init {
		lvHost.adapter = this
		lvHost.onItemClickListener = this
		helper = object : P2PHelper(context) {
			override fun onP2PEnabled(enabled: Boolean) {
				Log.d(TAG, "P2P Enabled: $enabled")
			}

			override fun onThisDeviceUpdated(device: WifiP2pDevice) {
				Log.d(TAG, "Device Updated: $device")
			}

			override fun onPeersList(peers: List<WifiP2pDevice>) {
				synchronized(p2pDevices) {
					p2pDevices.clear()
					p2pDevices.addAll(peers)
				}
				lvHost.post(this@P2PJoinGameDialog) // notify dataset changed
			}

			override fun onGroupFormed(addr: InetAddress, ipAddress: String) {
				Log.d(TAG, "onGroupFormedAsClient: $ipAddress")
				client.connectAsync(addr, connectPort) { success: Boolean? ->
					if (!success!!) {
						showError(context.getString(R.string.popup_error_msg_failed_to_connect))
					}
					connectLock.release()
				}
			}
		}
		client.addListener(this)
		helper.setDeviceName(clientName!!)
		helper.start(client)
		dialog = context.newDialogBuilder().setTitle(R.string.popup_title_join_game).setView(lvHost)
			.setNegativeButton(R.string.popup_button_cancel, this).setOnCancelListener(this).show()
	}

	override fun onCancel(dialog: DialogInterface) {
		shutdown()
	}

	override fun run() {
		notifyDataSetChanged()
	}

	override fun getCount(): Int {
		return p2pDevices.size
	}

	override fun getItem(position: Int): Any? {
		return null
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, v: View?, parent: ViewGroup): View {
		with(v ?: View.inflate(context, R.layout.list_item_peer, null)) {
			var device: WifiP2pDevice
			synchronized(p2pDevices) { device = p2pDevices[position] }
			tag = device
			val tvPeer = findViewById<TextView>(R.id.tvPeer)
			tvPeer.text = context.getString(
				R.string.join_game_dialog_client_label,
				device.deviceName,
				statusToString(device.status, context)
			)
			tvPeer.setBackgroundColor(if (position % 2 == 0) Color.BLACK else Color.DKGRAY)
			return this
		}
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		// cancel out of the dialog
		shutdown()
	}

	override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
		val d = view.tag as WifiP2pDevice
		object : SpinnerTask<Void>(context) {

			init {
				progressMessage = context.getString(R.string.popup_progress_msg_pleasewait_for_invite)
			}

			override suspend fun doIt(args: Void?) {
				helper.connect(d)
				connectLock.block(30000)
			}

			override fun onSuccess() {}
			override fun onError(e: Exception) {
				connectLock.release()
				super.onError(e)
			}

			override fun onCancelButtonClicked() {
				connectLock.release()
			}
		}.execute()
	}

	override fun onCommand(cmd: GameCommand) {}
	override fun onMessage(msg: String) {}
	override fun onDisconnected(reason: String, serverInitiated: Boolean) {}
	override fun onConnected() {
		client.removeListener(this) // TODO: We could stay alive and throw up dialog again if disconnected
		helper.stop()
		dialog.dismiss()
	}

	fun showError(msg: String?) {
		Log.e(P2PHelper.TAG, msg!!)
		context.runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
	}

	private fun shutdown() {
		dialog.dismiss()
		helper.stop()
		context.p2pShutdown()
	}
}
