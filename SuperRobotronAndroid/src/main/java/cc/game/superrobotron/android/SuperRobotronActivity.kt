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
import cc.lib.math.MutableVector2D
import cc.lib.mp.android.P2PActivity
import cc.lib.net.INetClient
import cc.lib.net.INetServer
import cc.lib.utils.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class ConnectMode(
	val homeButtonVisible: Boolean,
	val pausedButtonVisible: Boolean,
	val connectionsButtonVisible: Boolean,
	val hostButtonVisible: Boolean,
	val joinButtonVisible: Boolean,
	val disconnectButtonVisible: Boolean,
) {
	DISCONNECTED(
		true,
		true,
		false,
		true,
		true,
		false
	),
	HOST(
		true,
		true,
		true,
		false,
		false,
		true
	),
	CLIENT(
		false,
		false,
		false,
		false,
		false,
		true
	)
}

class SRViewModel : ViewModel() {
	val pauseVisible = MutableLiveData(false)
	val isPaused = MutableLiveData(false)
	val connectedMode = MutableLiveData(ConnectMode.DISCONNECTED)
	val gameRunning = MutableLiveData(false)
	val deviceName = MutableLiveData("")

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
class SuperRobotronActivity : P2PActivity(), INetClient.Listener, INetServer.Listener {

	// ---------------------------------------------------------//
	// ANDROID
	// ---------------------------------------------------------//
	lateinit var binding: RoboviewBinding
	lateinit var roboRenderer: RoboRenderer

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

		viewModel.viewModelScope.launch {
			binding.padLeft.direction.onEach {
				roboRenderer.robotron.setPlayerMovement(it.normalized(MutableVector2D()))
			}.collect()
		}
		viewModel.viewModelScope.launch {
			binding.padRight.direction.onEach {
				roboRenderer.robotron.setPlayerMissileVector(it.normalized(MutableVector2D()))
			}.collect()
		}
		viewModel.viewModelScope.launch {
			binding.padRight.touching.onEach {
				roboRenderer.robotron.setPlayerFiring(it)
			}.collect()
		}
		binding.homeButton.setOnClickListener {
			roboRenderer.robotron.setGameStateIntro()
			setPaused(false)
		}
		binding.pauseButton.setOnClickListener {
			setPaused(!binding.roboView1.paused)
		}
		binding.hostButton.setOnClickListener {
			launchIn(Dispatchers.IO) {
				if (p2pCreateGroup()) {
					try {
						p2pServer.start(deviceName)
						roboRenderer.robotron.server = p2pServer
						viewModel.connectedMode.postValue(ConnectMode.HOST)
					} catch (e: Throwable) {
						showErrorDialog(e)
					}
				}
			}
		}

		binding.joinButton.setOnClickListener {
			p2pOpenJoinGameDialog(p2pClient).also { onComplete ->
				launchIn(Dispatchers.IO) {
					onComplete.await()?.let {
						try {
							p2pClient.connect(it)
							viewModel.connectedMode.postValue(ConnectMode.CLIENT)
							roboRenderer.robotron.client = p2pClient
						} catch (e: Throwable) {
							showErrorDialog(e)
						}
					}
				}
			}
		}

		binding.connectionsButton.setOnClickListener {
			p2pOpenClientConnectionsDialog(p2pServer)
		}

		binding.disconnectButton.setOnClickListener {
			roboRenderer.robotron.disconnect()
			viewModel.connectedMode.postValue(ConnectMode.DISCONNECTED)
		}
		binding.deviceNameText.setOnClickListener {
			launchIn {
				viewModel.deviceName.postValue(changeDeviceName().await())
			}
		}
		viewModel.deviceName.postValue(deviceName)
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

	val p2pClient: RoboClient by lazy {
		RoboClient(roboRenderer.robotron, deviceName, 0).also {
			it.addListener(this)
		}
	}
	val p2pServer: RoboServer by lazy {
		RoboServer(roboRenderer.robotron, deviceName).also {
			it.addListener(this)
		}
	}

	override suspend fun onClientConnected(clientId: Int) {
		prefs.edit().putInt("clientId", clientId).apply()
		viewModel.connectedMode.postValue(ConnectMode.CLIENT)
	}

	override suspend fun onClientDisconnected(reason: String) {
		viewModel.connectedMode.postValue(ConnectMode.DISCONNECTED)
	}

	override fun onServerStopped() {
		viewModel.connectedMode.postValue(ConnectMode.DISCONNECTED)
	}

}