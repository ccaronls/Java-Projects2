package cc.lib.swing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import javax.swing.JComponent
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

/**
 * Created by Chris Caron on 5/5/26.
 *
 * Scope tied to the life of an AWT component, usually a button or menu.
 * Scope is cancelled when the component is removed from it hierarchy.
 */
class AWTScope(comp: JComponent) : CoroutineScope {

	private val job = SupervisorJob()

	override val coroutineContext = Dispatchers.Swing + job

	init {
		comp.addAncestorListener(object : AncestorListener {
			override fun ancestorAdded(p0: AncestorEvent?) {}

			override fun ancestorRemoved(p0: AncestorEvent?) {
				job.cancel()
			}

			override fun ancestorMoved(p0: AncestorEvent?) {}
		})
	}
}