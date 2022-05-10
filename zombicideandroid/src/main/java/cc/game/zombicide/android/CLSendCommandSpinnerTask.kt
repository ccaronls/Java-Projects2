package cc.game.zombicide.android

import cc.lib.android.SpinnerTask
import cc.lib.net.GameClient
import cc.lib.net.GameCommand
import cc.lib.net.GameCommandType
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal open class CLSendCommandSpinnerTask(context: ZombicideActivity, private val responseType: GameCommandType) : SpinnerTask<GameCommand>(context), GameCommandType.Listener {
	private val client: GameClient = context.client
	private val lock = ReentrantLock()
	private val cont = lock.newCondition()

	override fun onPreExecute() {
		super.onPreExecute()
		responseType.addListener(this)
	}

	@Throws(Exception::class)
	override fun doIt(vararg args: GameCommand) {
		client.sendCommand(args[0])
		lock.withLock {
			cont.await(20, TimeUnit.SECONDS)
		}
	}

	override fun onCommand(cmd: GameCommand) {
		lock.withLock {
			cont.signal()
		}
	}

	override fun onCompleted() {
		responseType.removeListener(this)
	}

}