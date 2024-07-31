package cc.lib.utils

/**
 * Created by Chris Caron on 7/19/24.
 */
enum class DPad(val dx: Int, val dy: Int) {
	UP(0, -1),
	DOWN(0, 1),
	LEFT(-1, 0),
	RIGHT(0, 1),
	CENTER(0, 0)
}