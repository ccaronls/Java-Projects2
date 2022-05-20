package cc.lib.zombicide

import cc.lib.utils.Reflector
import java.util.*

/**
 * Created by Chris Caron on 3/10/22.
 */
class ZObjective : Reflector<ZObjective>() {
    companion object {
        init {
            addAllFields(ZObjective::class.java)
        }
    }

    val objectives: MutableList<Int> = ArrayList()
    val found: MutableList<Int> = ArrayList()
}