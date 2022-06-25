package cc.game.soc.ui

/**
 * Created by chriscaron on 2/27/18.
 */
class MenuItem(val title: String?, val helpText: String?, val action: Action?) {
	interface Action {
		fun onAction(item: MenuItem, extra: Any?)
	}

	override fun toString(): String {
		return title?:"null"
	}
}