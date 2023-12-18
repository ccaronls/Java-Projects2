package cc.lib.swing

import cc.lib.game.Utils
import cc.lib.ui.IButton
import cc.lib.utils.FileUtils
import java.awt.Image
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JButton

open class AWTButton : JButton, ActionListener {
	private var data: Any? = null

	constructor(icon: Image) : super(ImageIcon(icon)) {
		addActionListener(this)
	}

	constructor(source: IButton) : this(source, null) {
		addActionListener(this)
	}

	constructor(source: IButton, listener: ActionListener?) : super(HTML_PREFIX + source.label + HTML_SUFFIX) {
		actionCommand = source.label
		listener?.let { addActionListener(it) }
		var ttt = source.tooltipText
		if (ttt != null) {
			if (ttt.length >= 64) {
				ttt = Utils.wrapTextWithNewlines(ttt, 64)
				ttt = String.format("<html>%s</html>", ttt.replace("[\n]+".toRegex(), "<br/>"))
			}
			toolTipText = ttt
		}
	}

	fun setTooltip(text: String?, maxChars: Int): AWTButton {
		toolTipText = Utils.wrapTextWithNewlines(text, maxChars)
		return this
	}

	constructor(label: String, selected: Boolean) : super(HTML_PREFIX + label + HTML_SUFFIX) {
		actionCommand = label
		isSelected = selected
	}

	constructor(label: String) : super(HTML_PREFIX + label + HTML_SUFFIX) {
		actionCommand = label
		addActionListener(this)
	}

	constructor(label: String, listener: ActionListener?) : super(HTML_PREFIX + label + HTML_SUFFIX) {
		actionCommand = label
		addActionListener(listener)
	}

	constructor(label: String, data: Any, listener: ActionListener) : super(HTML_PREFIX + label + HTML_SUFFIX) {
		addActionListener(listener)
		actionCommand = label
		this.data = data
	}

	override fun actionPerformed(e: ActionEvent) {
		onAction()
	}

	protected open fun onAction() {
		System.err.println("Unhandled action")
	}

	fun toggleSelected() {
		isSelected = !isSelected
	}

	fun <T> getData(): T? {
		return data as T?
	}

	companion object {
		private const val HTML_PREFIX = "<html><center><p>"
		private const val HTML_SUFFIX = "</p></center></html>"

		@Throws(Exception::class)
		fun createWithImage(fileOrResource: String?): AWTButton {
			FileUtils.openFileOrResource(fileOrResource).use { reader ->
				val img: Image = ImageIO.read(reader)
				return AWTButton(img)
			}
		}
	}
}