package cc.lib.swing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Created by Chris Caron on 4/20/26.
 */
open class AWTDialog(frame: AWTFrame, title: String? = null) : JDialog(frame, null, true) {

	val dialogScope: CoroutineScope by lazy {
		val job = SupervisorJob()
		CoroutineScope(Dispatchers.Swing + job).also {

			addWindowListener(object : WindowAdapter() {
				override fun windowClosing(p0: WindowEvent?) {
					job.cancel()
				}

				override fun windowClosed(p0: WindowEvent?) {
					job.cancel()
				}
			})
		}
	}

	init {
		minimumSize = Dimension(160, 120)
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(object : WindowAdapter() {
			override fun windowClosing(p0: WindowEvent?) {
				removeWindowListener(this)
				parent.isEnabled = true
				parent.isVisible = true
				onWindowClosing()
			}

			override fun windowClosed(p0: WindowEvent?) {
				super.windowClosed(p0)
			}
		})
	}

	val content = JPanel().also { panel ->
		panel.layout = BorderLayout()
		add(panel)
		title?.let {
			panel.add(AWTLabel(title, 1, 16f, true), BorderLayout.NORTH)
		}
	}

	fun closePopup() {
		SwingUtilities.invokeLater {
			parent.isEnabled = true
			isVisible = false
		}
	}

	fun showPopup() {
		isUndecorated = true
		parent.isEnabled = false
		pack()
		val x = parent.x + parent.width / 2 - width / 2
		val y = parent.y + parent.height / 2 - height / 2
		setLocation(x, y)
		isResizable = false
		isVisible = true
		isAlwaysOnTop = true
	}

	fun setBody(compoennt: JComponent) {
		content.add(compoennt, BorderLayout.CENTER)
	}

	fun setFooter(compoennt: JComponent) {
		content.add(compoennt, BorderLayout.SOUTH)
	}

	open fun onWindowClosing() {}
}