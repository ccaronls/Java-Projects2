package cc.lib.mp.android

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import cc.lib.android.SpinnerTask
import cc.lib.mp.android.P2PActivity.P2PServer
import cc.lib.net.AClientConnection
import cc.lib.net.AGameServer
import cc.lib.utils.KLock
import java.net.InetAddress

abstract class P2PClientConnectionsDialog(
	private val context: P2PActivity,
	private val server: AGameServer,
	serverName: String
) : BaseAdapter(), DialogInterface.OnClickListener, Runnable, DialogInterface.OnDismissListener,
	DialogInterface.OnCancelListener, View.OnClickListener, AClientConnection.Listener,
	AGameServer.Listener {
	private val helper: P2PHelper
	private var dialog: Dialog? = null
	private var lvPlayers: ListView? = null

	init {
		val groupFormedLock = KLock(1)
		helper = object : P2PHelper(context) {
			override fun onP2PEnabled(enabled: Boolean) {
				if (!enabled) {
					dialog!!.dismiss()
					onError("Failed to init P2P")
				}
			}

			override fun onThisDeviceUpdated(device: WifiP2pDevice) {}
			override fun onPeersList(peers: List<WifiP2pDevice>) {}
			override fun onGroupFormed(addr: InetAddress, ipAddress: String) {
				try {
					Log.d(TAG, "onGroupFormedAsServer: $ipAddress")
					if (!server.isRunning) server.listen()
					groupFormedLock.release()
				} catch (e: Exception) {
					onError(e.javaClass.simpleName + " " + e.message)
					if (dialog != null) dialog!!.dismiss()
				}
			}
		}
		helper.setDeviceName(serverName)
		helper.start(server)
		object : SpinnerTask<Void>(context) {

			init {
				progressMessage = context.getString(R.string.popup_title_starting_server)
			}

			@Throws(Exception::class)
			override suspend fun doIt(args: Void?) {
				groupFormedLock.block(20000)
				if (!server.isRunning) throw Exception("Timeout trying to start server")
			}

			override fun onSuccess() {
				Toast.makeText(context, "Server started", Toast.LENGTH_LONG).show()
				onServerSuccess(object : P2PServer {
					override fun getServer(): AGameServer {
						return server
					}

					override fun openConnections() {
						show()
					}
				})
			}

			override fun onCancelButtonClicked() {
				groupFormedLock.release()
				shutdown()
			}

			override fun onError(e: Exception) {
				context.newDialogBuilder().setTitle(R.string.popup_title_error)
					.setMessage(R.string.popup_msg_failed_to_start_server)
					.setPositiveButton(R.string.popup_button_ok) { dialog1: DialogInterface?, which: Int -> shutdown() }
					.setCancelable(false)
					.show()
			}
		}.execute()
	}

	abstract fun onServerSuccess(server: P2PServer)

	fun show() {
		if (dialog == null) {
			lvPlayers = ListView(context)
			lvPlayers!!.adapter = this
			dialog = context.newDialogBuilder().setTitle(R.string.popup_title_connected_clients)
				.setView(lvPlayers)
				.setPositiveButton(R.string.popup_button_close, null)
				.setNegativeButton(R.string.popup_button_disconnect, this)
				.show().also {
					it.setOnDismissListener(this)
				}
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		this.dialog = null
		lvPlayers = null
	}

	override fun onConnected(conn: AClientConnection) {
		conn.addListener(this)
		lvPlayers?.post(this)
	}

	override fun onReconnected(conn: AClientConnection) {
		lvPlayers?.post(this)
	}

	override fun onDisconnected(c: AClientConnection, reason: String) {
		lvPlayers?.post(this)
	}

	override fun onServerStopped() {
		helper.stop()
	}

	override fun run() {
		notifyDataSetChanged()
	}

	override fun onClick(dialog: DialogInterface, which: Int) {
		shutdown()
	}

	override fun getCount(): Int {
		return server.numClients
	}

	override fun getItem(position: Int): Any? {
		return null
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, _v: View?, parent: ViewGroup): View {
		val v = _v ?: View.inflate(context, R.layout.client_connections_dialog_item, null)

		val conn = server.getConnection(position)
		val tv = v.findViewById<TextView>(R.id.tv_clientname)
		val b_kick = v.findViewById<Button>(R.id.b_kickclient)
		if (conn!!.isKicked) {
			b_kick.setText(R.string.popup_button_unkick)
		} else {
			b_kick.setText(R.string.popup_button_kick)
		}
		b_kick.tag = conn
		b_kick.setOnClickListener(this)
		tv.text = conn.name
		tv.setTextColor(if (conn.isConnected) Color.GREEN else Color.RED)
		tv.setBackgroundColor(if (position % 2 == 0) Color.BLACK else Color.DKGRAY)
		return v
	}

	override fun onClick(v: View) {
		val conn = v.tag as AClientConnection
		if (conn.isKicked) {
			conn.unkick()
		} else {
			conn.kick()
		}
		notifyDataSetChanged()
	}

	override fun onCancel(dialog: DialogInterface) {
		shutdown()
	}

	fun onError(msg: String?) {
		shutdown()
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
	}

	private fun shutdown() {
		helper.stop()
		context.p2pShutdown()
	}
}
