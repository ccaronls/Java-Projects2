package cc.lib.net.api

import cc.lib.logger.Logger

/**
 * Created by Chris Caron on 6/3/25.
 *
 * Base class for client, server, client connection
 */
interface INetContext<Listener> {

	val log: Logger

	fun addListener(listener: Listener)

	fun removeListener(listener: Listener)

	/**
	 * call super
	 */
	fun close()
}