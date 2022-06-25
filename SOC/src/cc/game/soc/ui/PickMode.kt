package cc.game.soc.ui

/**
 * Created by chriscaron on 2/28/18.
 */
enum class PickMode {
	PM_NONE,
	PM_EDGE,
	PM_TILE,
	PM_VERTEX,
	PM_CUSTOM
	// CUSTOM must be associated with a CustomPickHandler
}