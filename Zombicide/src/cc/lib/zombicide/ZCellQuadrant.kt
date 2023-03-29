package cc.lib.zombicide

import cc.lib.annotation.Keep


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
    CENTER; // make center the very last so that an ABOMINATION has good chance of not squashing someth

    companion object {
        @JvmStatic
        fun valuesForRender(): Array<ZCellQuadrant> {
            return arrayOf(UPPERLEFT, TOP, UPPERRIGHT, LEFT, CENTER, RIGHT, LOWERLEFT, BOTTOM, LOWERRIGHT)
        }
    }
}