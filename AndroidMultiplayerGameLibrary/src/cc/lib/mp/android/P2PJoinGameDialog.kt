package cc.lib.mp.android

import android.app.Dialog
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import cc.lib.utils.clearAndAddAll
import cc.lib.utils.launchIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.net.InetAddress

abstract class PeerChooserDialog(
	val context: P2PActivity,
	val peers: Flow<List<Pair<InetAddress, String>>>
) : BaseAdapter() {
	private val lvHost: ListView = ListView(context)
	private val peersList = mutableListOf<Pair<InetAddress, String>>()
	private lateinit var dialog: Dialog
	private var choice: InetAddress? = null
	private val collectJob: Job

	init {
		lvHost.adapter = this
		lvHost.onItemClickListener = OnItemClickListener { parent, view, position, id ->
			choice = view.tag as InetAddress
			close()
		}
		dialog = context.newDialogBuilder().setTitle(R.string.popup_title_join_game)
			.setView(lvHost)
			.setNegativeButton(R.string.popup_button_cancel) { _, _ -> close() }
			.setOnCancelListener { close() }
			.show()
		dialog.setOnDismissListener { close() }
		collectJob = launchIn {
			peers.onEach {
				peersList.clearAndAddAll(it)
				notifyDataSetChanged()
			}.collect()
		}
	}

	abstract fun onConnectionChoice(address: InetAddress?)

	private fun close() {
		collectJob.cancel()
		onConnectionChoice(choice)
		dialog.dismiss()
	}

	final override fun getCount(): Int {
		return peersList.size
	}

	final override fun getItem(position: Int): Any? {
		return null
	}

	final override fun getItemId(position: Int): Long {
		return 0
	}

	override fun getView(position: Int, v: View?, parent: ViewGroup): View {
		with(v ?: View.inflate(context, R.layout.list_item_peer, null)) {
			val device = peersList[position].second
			tag = peersList[position].first
			val tvPeer = findViewById<TextView>(R.id.tvPeer)
			tvPeer.text = device
			tvPeer.setBackgroundColor(if (position % 2 == 0) Color.BLACK else Color.DKGRAY)
			return this
		}
	}

}
