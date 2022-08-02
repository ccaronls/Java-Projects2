package cc.game.soc.ui

import cc.game.soc.core.StringResource
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

/**
 * Created by chriscaron on 2/27/18.
 */
abstract class UIRenderer @JvmOverloads internal constructor(component: UIComponent?, attach: Boolean = true) : UIRenderer(component, attach), StringResource {
	open fun reset() {}
}