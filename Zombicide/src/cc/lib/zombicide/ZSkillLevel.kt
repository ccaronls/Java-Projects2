package cc.lib.zombicide


import cc.lib.reflector.Reflector
import cc.lib.utils.repeat

class ZSkillLevel private constructor(val color: ZColor, val ultra: Int) : Reflector<ZSkillLevel>(), Comparable<ZSkillLevel> {

	constructor() : this(ZColor.BLUE, 0)

	val pts = color.dangerPts + ZColor.RED.dangerPts * ultra

	companion object {
		val NUM_LEVELS = ZColor.values().size

		@JvmStatic
		fun getLevel(expPts: Int, rules: ZRules): ZSkillLevel {
			var level = ZSkillLevel()
			var next = level.nextLevel(rules)
			while (expPts >= next.pts) {
				level = next
				next = level.nextLevel(rules)
				if (!rules.ultraRed && level.color == ZColor.RED)
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

	fun nextLevel(rules: ZRules): ZSkillLevel = when (color) {
		ZColor.BLUE -> ZSkillLevel(ZColor.YELLOW, ultra)
		ZColor.YELLOW -> ZSkillLevel(ZColor.ORANGE, ultra)
		ZColor.ORANGE -> ZSkillLevel(ZColor.RED, ultra)
		else -> if (rules.ultraRed)
			ZSkillLevel(ZColor.YELLOW, ultra + 1)
		else
			ZSkillLevel(ZColor.RED, 0)
	}

	fun getPtsToNextLevel(curPts: Int, rules: ZRules): Int {
		if (!rules.ultraRed && color == ZColor.RED)
			return 0
		return nextLevel(rules).pts - curPts
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