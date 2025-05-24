package cc.game.superrobotron.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cc.game.superrobotron.RoboClient
import cc.game.superrobotron.RoboServer
import cc.game.superrobotron.android.databinding.RoboviewBinding
import cc.lib.android.DPadView
import cc.lib.android.DPadView.OnDpadListener
import cc.lib.crypt.Cypher
import cc.lib.math.Vector2D
import cc.lib.mp.android.P2PActivity
import cc.lib.net.AClientConnection
import cc.lib.net.AGameClient
import cc.lib.net.AGameServer
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import cc.lib.net.PortAllocator

class SRViewModel : ViewModel() {
	val pauseVisible = MutableLiveData(false)
	val isPaused = MutableLiveData(false)
	val clientMode = MutableLiveData(false)
	val hostMode = MutableLiveData(false)
	val connected = MutableLiveData(false)
	val gameRunning = MutableLiveData(false)
}

/**
 * Created by Chris Caron on 5/31/22.
 */
class SuperRobotronActivity : P2PActivity<RoboClient, RoboServer>(), AGameClient.Listener, AGameServer.Listener {

	// ---------------------------------------------------------//
	// ANDROID
	// ---------------------------------------------------------//
	lateinit var binding: RoboviewBinding
	lateinit var roboRenderer: RoboRenderer

	override val connectPort: Int = PortAllocator.SUPER_ROBOTRON_PORT
	override val version = "0.1"
	override val maxConnections = 4

	val viewModel by viewModels<SRViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.binding = RoboviewBinding.inflate(layoutInflater).also {
			it.viewModel = viewModel
		}

		setContentView(binding.root)

		roboRenderer = RoboRenderer(binding.roboView1)
		binding.roboView1.setRenderer(roboRenderer)
		binding.dPadLeft.setOnDpadListener(object : OnDpadListener {

			override fun dpadMoved(view: DPadView, dx: Float, dy: Float) {
				val v = Vector2D(dx, dy).normalized()
				roboRenderer.robotron.setPlayerMovement(v.x, v.y)
			}

			override fun dpadReleased(view: DPadView) {
				roboRenderer.robotron.setPlayerMovement(0, 0)
			}
		})
		binding.dPadRight.setOnDpadListener(object : OnDpadListener {
			override fun dpadMoved(view: DPadView, dx: Float, dy: Float) {
				roboRenderer.robotron.setPlayerMissileVector(dx, dy)
				roboRenderer.robotron.setPlayerFiring(true)
			}

			override fun dpadReleased(view: DPadView) {
				roboRenderer.robotron.setPlayerFiring(false)
			}
		})
		binding.homeButton.setOnClickListener {
			roboRenderer.robotron.setGameStateIntro()
		}
		binding.pauseButton.setOnClickListener {
			with(binding.roboView1) {
				paused = !paused
				viewModel.isPaused.value = paused
				invalidate()
			}
		}
		binding.hostButton.setOnClickListener {
			p2pInitAsServer()
		}

		binding.joinButton.setOnClickListener {
			p2pInitAsClient()
		}

		binding.disconnectButton.setOnClickListener {
			p2pShutdown()
		}

		hideNavigationBar()
	}

	override fun newGameServer(deviceName: String, port: Int, version: String, cypher: Cypher?, maxConnections: Int): RoboServer {
		return RoboServer(roboRenderer.robotron, version, cypher)
	}

	override fun newGameClient(deviceName: String, version: String, cypher: Cypher?): RoboClient {
		return RoboClient(roboRenderer.robotron, deviceName, version, cypher)
	}

	override fun onP2PClient(p2pClient: RoboClient) {
		this.p2pClient = p2pClient
	}

	override fun onP2PServer(p2pServer: RoboServer) {
		this.p2pServer = p2pServer
	}

	var p2pClient: RoboClient? = null
	var p2pServer: RoboServer? = null

	val initGameCmd = GameCommandType("GAME_INIT")

	override fun onCommand(cmd: GameCommand) {
		if (cmd.type == initGameCmd) {
			roboRenderer.robotron.this_player = cmd.getInt("id")
			cmd.getReflector("game", roboRenderer.robotron)
		}
	}

	override fun onMessage(msg: String) {
		super.onMessage(msg)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		super.onDisconnected(reason, serverInitiated)
	}

	override fun onConnected() {
	}

	override fun onPing(time: Int) {
		super.onPing(time)
	}

	override fun onPropertiesChanged() {
		super.onPropertiesChanged()
	}


	// server

	override fun onConnected(conn: AClientConnection) {
		roboRenderer.robotron.players.add()
		conn.sendCommand(GameCommand(initGameCmd).also {
			it.setArg("id", roboRenderer.robotron.players.size - 1)
			it.setArg("game", roboRenderer.robotron)
		})
	}

	override fun onServerStopped() {
		p2pServer = null
	}
}