package cc.game.zombicide.p2p

import cc.lib.ksp.remote.ISvrExecuteRemote

interface IZServer {
	fun broadcastExecuteMethodOnRemote(cmd: ISvrExecuteRemote)

	fun start()
}