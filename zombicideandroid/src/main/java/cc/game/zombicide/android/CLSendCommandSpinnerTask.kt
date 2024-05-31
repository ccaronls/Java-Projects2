package cc.game.zombicide.android

import cc.lib.android.SpinnerTask
import cc.lib.net.GameCommand
import cc.lib.utils.Lock

open class CLSendCommandSpinnerTask(val context: ZombicideActivity) : SpinnerTask<GameCommand>(context), ZMPCommon.CLListener {
	private val clientMgr: ZClientMgr = context.clientMgr!!
	private val lock = Lock()

	override fun onPreExecute() {
		super.onPreExecute()
		clientMgr.addListener(this)
	}

	open val timeout = 10000L

	@Throws(Exception::class)
	override fun doIt(vararg args: GameCommand) {
		requireNotNull(context.client).sendCommand(args[0])
		if (timeout > 0)
			lock.acquireAndBlock(timeout) {
				throw Exception("timeout")
			}
		else
			lock.acquireAndBlock()
	}

	fun release() {
		lock.release()
	}

	override fun onCancelButtonClicked() {
		lock.release()
	}

	override fun onCompleted() {
		clientMgr.removeListener(this)
	}

	override fun onNetError(e: Exception) {
		onError(e)
	}

}