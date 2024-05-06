package cc.android.test.p2p

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import cc.android.test.R
import cc.lib.game.GColor
import cc.lib.mp.android.P2PActivity
import cc.lib.net.AClientConnection
import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import java.util.LinkedList

class WifiTest2 : P2PActivity(), View.OnClickListener, AGameClient.Listener, AGameServer.Listener,
	AClientConnection.Listener, Runnable {
	lateinit var listView: ListView
	lateinit var editText: EditText
	lateinit var bStart: View
	lateinit var bShowClients: View
	lateinit var bDisconnect: View
	val items = LinkedList<ListItem>()
	val adapter = MyAdapter()
	var serverController: P2PServer? = null

	override val connectPort = 31313
	override val version = "1.0"
	override val maxConnections = 2

	enum class State {
		DISCONNECTED,
		HOSTING,
		CLIENT
	}

	class ListItem(val text: String, val color: Int, val leftJustify: Boolean)
	inner class MyAdapter : BaseAdapter() {
		override fun getCount(): Int {
			return items.size
		}

		override fun getItem(i: Int): Any? {
			return null
		}

		override fun getItemId(i: Int): Long {
			return 0
		}

		override fun getView(i: Int, _view: View?, viewGroup: ViewGroup): View {
			val tv = (_view ?: TextView(this@WifiTest2)) as TextView
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24f)
			val item = items[i]
			tv.text = item.text
			tv.setTextColor(item.color)
			tv.gravity = if (item.leftJustify) Gravity.LEFT else Gravity.RIGHT
			return tv
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.wifi2_activity)
		findViewById<View>(R.id.bStart).also { bStart = it }.setOnClickListener(this)
		findViewById<View>(R.id.bClients).also { bShowClients = it }.setOnClickListener(this)
		findViewById<View>(R.id.bDisconnect).also { bDisconnect = it }.setOnClickListener(this)
		findViewById<View>(R.id.bSend).setOnClickListener(this)
		listView = findViewById<ListView>(R.id.listview).also {
			it.setAdapter(adapter)
		}
		editText = findViewById(R.id.edittext)
		initButtons(State.DISCONNECTED)
	}

	override fun onStart() {
		super.onStart()
		p2pInit()
	}

	override fun onResume() {
		super.onResume()
		hideKeyboard()
	}

	fun getMessageCommand(msg: String?, color: Int, source: String?): GameCommand {
		return GameCommand(TEXT_MSG).setArg("color", color).setName(source!!).setMessage(
			msg!!
		)
	}

	override fun onP2PReady() {
		val handle = prefs.getString("handle", null)
		val et = EditText(this)
		et.hint = "Handle"
		et.setText(handle)
		newDialogBuilder().setTitle("Set Handle")
			.setView(et)
			.setPositiveButton("Ok") { dialog, which ->
				prefs.edit().putString("handle", et.text.toString()).apply()
			}
			.show()
	}

	override fun onP2PClient(p2pClient: P2PClient) {
		p2pClient.getClient().addListener(this)
		p2pClient.getClient().setDisplayName(prefs.getString("handle", "Unknown")!!)
		runOnUiThread(UpdateButtonsRunnable(State.CLIENT))
	}

	override fun onP2PServer(p2pServer: P2PServer?) {
		p2pServer!!.openConnections()
		p2pServer.getServer().addListener(this)
		serverController = p2pServer
		runOnUiThread(UpdateButtonsRunnable(State.HOSTING))
	}

	override fun onP2PShutdown() {
		initButtons(State.DISCONNECTED)
	}

	private fun sendText() {
		val text = editText.text.toString()
		if (text.isNotEmpty()) {
			addListEntry(text, Color.BLACK, true)
			if (client != null) {
				client!!.sendCommand(getMessageCommand(text, myColor, null))
			} else if (server != null) {
				server!!.broadcastCommand(getMessageCommand(text, Color.RED, deviceName))
			}
		}
		editText.text.clear()
	}

	// *********************************************************************************
	//
	//       CLIENT
	//
	// *********************************************************************************
	override fun onClick(view: View) {
		when (view.id) {
			R.id.bStart -> p2pStart()
			R.id.bClients -> serverController!!.openConnections()
			R.id.bDisconnect -> {
				p2pShutdown()
				initButtons(State.DISCONNECTED)
			}

			R.id.bSend -> sendText()
		}
	}

	@Synchronized
	fun addListEntry(text: String, color: Int, leftJustify: Boolean) {
		items.addFirst(ListItem(text, color, leftJustify))
		runOnUiThread(this)
	}

	override fun run() {
		adapter.notifyDataSetChanged()
	}

	fun initButtons(state: State) {
		when (state) {
			State.DISCONNECTED -> {
				bDisconnect.isEnabled = false
				bShowClients.isEnabled = false
				bStart.isEnabled = true
			}

			State.HOSTING -> {
				bDisconnect.isEnabled = true
				bShowClients.isEnabled = true
				bStart.isEnabled = false
			}

			State.CLIENT -> {
				bDisconnect.isEnabled = true
				bShowClients.isEnabled = false
				bStart.isEnabled = false
			}
		}
	}

	var colors = intArrayOf(
		Color.BLUE,
		Color.CYAN,
		Color.MAGENTA,
		GColor.ORANGE.toARGB(),
		Color.GREEN,
		Color.DKGRAY,
		Color.GRAY,
		Color.LTGRAY
	)
	var curColor = 0

	// GameServer callbacks
	@Synchronized
	override fun onConnected(conn: AClientConnection) {
		addListEntry("Client Connected: " + conn.displayName, colors[curColor], false)
		conn.sendCommand(GameCommand(ASSIGN_COLOR).setArg("color", colors[curColor]))
		conn.addListener(this)
		curColor = (curColor + 1) % colors.size
	}

	override fun onReconnected(conn: AClientConnection) {
		addListEntry("Client Reconnected: " + conn.displayName, Color.YELLOW, false)
	}

	override fun onDisconnected(conn: AClientConnection, reason: String) {
		addListEntry(
			"""Client Disconnected: ${conn.displayName}
  $reason""", Color.RED, false
		)
	}

	override fun onCommand(conn: AClientConnection, cmd: GameCommand) {
		if (cmd.type == TEXT_MSG) {
			val color = cmd.getInt("color")
			addListEntry(conn.displayName + ":" + cmd.getMessage(), color, false)
			cmd.setName(conn.displayName)
			for (c in server!!.connectionValues) {
				if (c === conn) continue
				c!!.sendCommand(cmd)
			}
		}
	}

	override fun onServerStopped() {
		runOnUiThread(UpdateButtonsRunnable(State.DISCONNECTED))
	}

	// GameClient callbacks
	var myColor = Color.BLACK
	override fun onCommand(cmd: GameCommand) {
		when (cmd.type) {
			TEXT_MSG -> {
				val color = cmd.getInt("color")
				addListEntry(cmd.getName() + ":" + cmd.getMessage(), color, false)
			}

			ASSIGN_COLOR -> {
				myColor = cmd.getInt("color")
				addListEntry("Color Assigned", myColor, true)
			}

			else -> {
				addListEntry(cmd.type.name() + ":" + cmd.getMessage(), Color.RED, true)
			}
		}
	}

	override fun onMessage(msg: String) {
		addListEntry("MSG: $msg", Color.BLUE, false)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		addListEntry("Disconnected: $reason", Color.RED, true)
		runOnUiThread(UpdateButtonsRunnable(State.DISCONNECTED))
	}

	override fun onConnected() {
		addListEntry("Connected", Color.GREEN, true)
		runOnUiThread(UpdateButtonsRunnable(State.CLIENT))
	}

	internal inner class UpdateButtonsRunnable(val state: State) : Runnable {
		override fun run() {
			initButtons(state)
		}
	}

	companion object {
		var TEXT_MSG = GameCommandType("TEXT_MSG")
		var ASSIGN_COLOR = GameCommandType("ASSIGN_COLOR")
	}
}
