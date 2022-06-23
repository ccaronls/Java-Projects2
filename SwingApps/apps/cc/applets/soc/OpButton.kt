package cc.applets.soc

import cc.game.soc.ui.MenuItem
import javax.swing.JButton

class OpButton internal constructor(val item: MenuItem, text: String, extra: Any?) : JButton(text) {
	@JvmField
    val extra: Any

	init {
		this.extra = extra ?: this
		this.toolTipText = item.helpText
	}
}