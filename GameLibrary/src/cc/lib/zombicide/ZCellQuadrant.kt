package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.Utils

@Keep
enum class ZCellQuadrant {
    // the ordering is how actors are added to a cell
    UPPERLEFT,
    LOWERRIGHT,
    UPPERRIGHT,
    LOWERLEFT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    CENTER;

    companion object {
        @JvmStatic
        fun valuesForRender(): Array<ZCellQuadrant> {
            return Utils.toArray(UPPERLEFT, TOP, UPPERRIGHT, LEFT, CENTER, RIGHT, LOWERLEFT, BOTTOM, LOWERRIGHT)
        }
    }
}