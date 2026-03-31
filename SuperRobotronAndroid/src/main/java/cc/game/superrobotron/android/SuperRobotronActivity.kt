package cc.game.superrobotron.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.game.superrobotron.GAME_STATE_GAME_OVER
import cc.game.superrobotron.GAME_STATE_INTRO
import cc.game.superrobotron.GAME_STATE_NEXT_LEVEL
import cc.game.superrobotron.GAME_STATE_PLAY
import cc.game.superrobotron.RoboClient
import cc.game.superrobotron.RoboServer
import cc.game.superrobotron.Robotron
import cc.game.superrobotron.android.databinding.RoboviewBinding
import cc.lib.android.DPadView
import cc.lib.android.DPadView.OnDpadListener
import cc.lib.math.Vector2D
import cc.lib.mp.android.P2PActivity
import cc.lib.net.INetClient
import cc.lib.net.PortAllocator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class ConnectMode(
	val hostButtonVisible: Boolean,
	val joinButtonVisible: Boolean,
	val disconnectButtonVisible: Boolean,
) {
	DISCONNECTED(
		true, true, false
	),
	HOST(
		false, false, true
	),
	CLIENT(
		false, false, true
	)
}

class SRViewModel : ViewModel() {
	val pauseVisible = MutableLiveData(false)
	val isPaused = MutableLiveData(false)
	val connectedMode = MutableLiveData(ConnectMode.DISCONNECTED)
	val gameRunning = MutableLiveData(false)

	fun setRobotron(robotron: Robotron) {
		viewModelScope.launch {
			robotron.game_state_flow.onEach {
				when (it) {
					GAME_STATE_INTRO -> {
						gameRunning.postValue(false)
						pauseVisible.postValue(false)
					}

					GAME_STATE_PLAY,
					GAME_STATE_GAME_OVER,
					GAME_STATE_NEXT_LEVEL -> {
						gameRunning.postValue(true)
						if (robotron.client == null)
							pauseVisible.postValue(true)
					}
				}
			}.collect()
		}
	}
}

/**
 * Created by Chris Caron on 5/31/22.
 */
class SuperRobotronActivity : P2PActivity<RoboClient, RoboServer>(), INetClient.Listener {

	// ---------------------------------------------------------//
	// ANDROID
	// ---------------------------------------------------------//
	lateinit var binding: RoboviewBinding
	lateinit var roboRenderer: RoboRenderer

	override val connectPort: Int = PortAllocator.SUPER_ROBOTRON_PORT

	val viewModel by viewModels<SRViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		this.binding = RoboviewBinding.inflate(layoutInflater).also {
			it.viewModel = viewModel
			it.lifecycleOwner = this
		}

		setContentView(binding.root)

		roboRenderer = RoboRenderer(binding.roboView1)
		binding.roboView1.setRenderer(roboRenderer)
		binding.dPadLeft.setOnDpadListener(object : OnDpadListener {

			override fun dpadMoved(view: DPadView, dx: Float, dy: Float) {
				val v = Vector2D(dx, dy).normalized()
				roboRenderer.robotron.setPlayerMovement(v)
			}

			override fun dpadReleased(view: DPadView) {
				roboRenderer.robotron.setPlayerMovement(Vector2D.ZERO)
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
			setPaused(false)
		}
		binding.pauseButton.setOnClickListener {
			setPaused(!binding.roboView1.paused)
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

	override fun onResume() {
		super.onResume()
		viewModel.setRobotron(roboRenderer.robotron)
	}

	fun setPaused(paused: Boolean) {
		with(binding.roboView1) {
			this.paused = paused
			viewModel.isPaused.value = paused
			invalidate()
		}
	}

	override fun newGameServer(deviceName: String): RoboServer {
		return RoboServer(roboRenderer.robotron, deviceName)
	}

	override fun newGameClient(deviceName: String): RoboClient {
		return RoboClient(roboRenderer.robotron, deviceName, prefs.getInt("clientId", 0)).also {
			it.addListener(this)
		}
	}

	override fun onP2PClient(p2pClient: RoboClient) {
		this.p2pClient = p2pClient
	}

	override fun onP2PServer(p2pServer: RoboServer) {
		this.p2pServer = p2pServer
	}

	var p2pClient: RoboClient? = null
	var p2pServer: RoboServer? = null

	override suspend fun onClientConnected(clientId: Int) {
		prefs.edit().putInt("clientId", clientId).apply()
		viewModel.connectedMode.postValue(ConnectMode.CLIENT)
	}

	override suspend fun onClientDisconnected(reason: String) {
		viewModel.connectedMode.postValue(ConnectMode.DISCONNECTED)
	}

	override fun startServer(svr: RoboServer) {
		svr.listen()
		viewModel.connectedMode.postValue(ConnectMode.HOST)
	}

}