package cc.lib.zombicide

import cc.lib.game.Utils
import cc.lib.utils.Reflector

class ZSkillLevel(val color: ZColor = ZColor.BLUE, val ultra: Int = -1) : Reflector<ZSkillLevel>(), Comparable<ZSkillLevel> {
    companion object {
        @JvmField
        var ULTRA_RED_MODE = true
        @JvmField
        val NUM_LEVELS = ZColor.values().size
        @JvmStatic
        fun getLevel(expPts: Int): ZSkillLevel {
            var expPts = expPts
            var ultra = 0
            var lvl = ZColor.RED
            if (ULTRA_RED_MODE) {
                ultra = expPts / ZColor.RED.maxPts
                expPts = expPts % ZColor.RED.maxPts
            }
            for (sl in ZColor.values()) {
                if (expPts <= sl.maxPts) {
                    lvl = sl
                    break
                }
            }
            return if (lvl === ZColor.BLUE && ultra > 0) ZSkillLevel(ZColor.RED, ultra - 1) else ZSkillLevel(lvl, ultra)
        }

        init {
            addAllFields(ZSkillLevel::class.java)
        }
    }

    constructor(lvl: ZColor) : this(lvl, 0) {}

    val difficultyColor: ZColor
        get() = if (ultra > 0) ZColor.RED else color

    override fun compareTo(o: ZSkillLevel): Int {
        return if (ultra != o.ultra) Integer.compare(ultra, o.ultra) else color.compareTo(o.color)
    }

    fun nextLevel(): ZSkillLevel {
        when (color) {
            ZColor.BLUE -> return ZSkillLevel(ZColor.YELLOW, ultra)
            ZColor.YELLOW -> return ZSkillLevel(ZColor.ORANGE, ultra)
            ZColor.ORANGE -> return ZSkillLevel(ZColor.RED, ultra)
        }
        return if (ULTRA_RED_MODE) ZSkillLevel(ZColor.YELLOW, ultra + 1) else ZSkillLevel(ZColor.RED, ultra)
    }

    fun getPtsToNextLevel(curPts: Int): Int {
        var curPts = curPts
        if (ULTRA_RED_MODE) {
            curPts = curPts % ZColor.RED.dangerPts
        } else if (color === ZColor.RED) return 0
        var idx = (color.ordinal + 1) % NUM_LEVELS
        if (idx == 0) idx++
        return ZColor.values()[idx].dangerPts - curPts
    }

    val dangerPts: Int
        get() = color.dangerPts

    override fun toString(): String {
        return color.name + Utils.getRepeatingChars('+', ultra)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ZSkillLevel
        return ultra == that.ultra &&
                color === that.color
    }

    override fun hashCode(): Int {
        return Utils.hashCode(color, ultra)
    }
}