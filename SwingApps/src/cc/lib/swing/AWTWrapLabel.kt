package cc.lib.swing

import javax.swing.JLabel

class AWTWrapLabel : JLabel {
	constructor() : super() {}
	constructor(text: String) {
		setText(text)
	}

	override fun setText(arg0: String) {
		super.setText("<html>" + arg0.replace("\n", "</br>") + "</html>")
	}
}