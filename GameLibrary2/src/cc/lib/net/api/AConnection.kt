package cc.lib.net.api

import cc.lib.ksp.remote.RemoteContext
import com.google.gson.GsonBuilder

/**
 * Created by Chris Caron on 10/10/24.
 */
abstract class AConnection : RemoteContext {

	val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
}