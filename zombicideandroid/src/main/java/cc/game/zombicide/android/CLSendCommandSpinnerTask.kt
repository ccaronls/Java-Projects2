package cc.game.zombicide.android

import cc.lib.android.SpinnerTask
import cc.lib.net.GameCommand
import cc.lib.utils.KLock

open class CLSendCommandSpinnerTask(val context: ZombicideActivity) : SpinnerTask<GameCommand>(context), ZMPCommon.CLListener {
	private val clientMgr: ZClientMgr = context.clientMgr!!
	private val lock = KLock()

	override fun onPreExecute() {
		super.onPreExecute()
		clientMgr.addListener(this)
	}

	open val timeout = 10000L

	override suspend fun doIt(args: GameCommand?) {
		requireNotNull(context.clientMgr?.client).sendCommand(requireNotNull(args))
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