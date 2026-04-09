package cc.lib.mp.android

import android.app.Dialog
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import cc.lib.net.INetConnection
import cc.lib.net.INetServer

class P2PClientConnectionsDialog(
	private val context: P2PActivity,
	private val server: INetServer
) : BaseAdapter(),
    INetServer.Listener {
	private val dialog: Dialog
	private val lvPlayers: ListView = ListView(context)

	init {
		lvPlayers.adapter = this
		dialog = context.newDialogBuilder().setTitle(R.string.popup_title_connected_clients)
			.setView(lvPlayers)
			.setPositiveButton(R.string.popup_button_close, null)
			.show()
	}

	override suspend fun onNewConnection(conn: INetConnection) {
		notifyDataSetChanged()
	}

	override suspend fun onConnectionReconnected(conn: INetConnection) {
		notifyDataSetChanged()
	}

	override suspend fun onConnectionDisconnected(conn: INetConnection, reason: String) {
		notifyDataSetChanged()
	}

	override fun getCount(): Int {
		return server.connections.size
	}

	override fun getItem(position: Int): Any? {
		return null
	}

	override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, _v: View?, parent: ViewGroup): View {
		val v = _v ?: View.inflate(context, R.layout.client_connections_dialog_item, null)

		val conn = server.connections[position]
		val tv = v.findViewById<TextView>(R.id.tv_clientname)
		val b_kick = v.findViewById<Button>(R.id.b_kickclient)
		if (conn.kicked) {
			b_kick.setText(R.string.popup_button_unkick)
		} else {
			b_kick.setText(R.string.popup_button_kick)
		}
		b_kick.tag = conn
		b_kick.setOnClickListener { v ->
			val conn = v.tag as INetConnection
			conn.kicked = !conn.kicked
			notifyDataSetChanged()
		}
		tv.text = conn.displayName
		tv.setTextColor(if (conn.connected) Color.GREEN else Color.RED)
		tv.setBackgroundColor(if (position % 2 == 0) Color.BLACK else Color.DKGRAY)
		return v
	}
}
