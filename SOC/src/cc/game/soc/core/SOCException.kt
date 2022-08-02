package cc.game.soc.core

class SOCException : RuntimeException {
	constructor() {}
	constructor(message: String?) : super(message) {}
	constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}