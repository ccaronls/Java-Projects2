package cc.lib.zombicide


import cc.lib.reflector.Reflector
import cc.lib.utils.repeat

class ZSkillLevel private constructor(val color: ZColor, val ultra: Int) : Reflector<ZSkillLevel>(), Comparable<ZSkillLevel> {

	constructor() : this(ZColor.BLUE, 0)

	val pts = color.dangerPts + ZColor.RED.dangerPts * ultra

	companion object {
		@JvmField
		var ULTRA_RED_MODE = true

		@JvmField
		val NUM_LEVELS = ZColor.values().size

		@JvmStatic
		fun getLevel(expPts: Int): ZSkillLevel {
			var level = ZSkillLevel()
			var next = level.nextLevel()
			while (expPts >= next.pts) {
				level = next
				next = level.nextLevel()
				if (!ULTRA_RED_MODE && level.color == ZColor.RED)
					break
			}
			return level
		}

        init {
            addAllFields(ZSkillLevel::class.java)
        }
    }

    val difficultyColor: ZColor
        get() = if (ultra > 0) ZColor.RED else color

    override fun compareTo(other: ZSkillLevel): Int {
	    return if (ultra != other.ultra)
		    ultra.compareTo(other.ultra)
	    else
		    color.compareTo(other.color)
    }

    fun nextLevel(): ZSkillLevel = when (color) {
	    ZColor.BLUE -> ZSkillLevel(ZColor.YELLOW, ultra)
	    ZColor.YELLOW -> ZSkillLevel(ZColor.ORANGE, ultra)
	    ZColor.ORANGE -> ZSkillLevel(ZColor.RED, ultra)
	    else -> if (ULTRA_RED_MODE)
		    ZSkillLevel(ZColor.YELLOW, ultra + 1)
	    else
		    ZSkillLevel(ZColor.RED, 0)
    }

    fun getPtsToNextLevel(curPts: Int): Int {
	    if (!ULTRA_RED_MODE && color == ZColor.RED)
		    return 0
	    return nextLevel().pts - curPts
    }

    val dangerPts: Int
        get() = color.dangerPts

    override fun toString(): String {
        return "${color.name} ${'+'.repeat(ultra)}"
    }

    override fun equals(o: Any?): Boolean {
	    if (this === o) return true
	    if (o == null || javaClass != o.javaClass) return false
	    val that = o as ZSkillLevel
	    return ultra == that.ultra &&
		    color === that.color
    }

	override fun hashCode(): Int {
		return cc.lib.utils.hashCode(color, ultra)
	}

	val isUltra: Boolean
		get() = ultra > 0
}