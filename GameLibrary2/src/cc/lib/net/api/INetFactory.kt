interface INetFactory {
	fun createServer(): IGameServer

	fun createClient(): IGameClient

	fun createCommand(code: Byte): ICommand
}