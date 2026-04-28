package cc.lib.swing

import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridLayout
import java.awt.LayoutManager
import javax.swing.JPanel

open class AWTPanel : JPanel {
	constructor(rows: Int, cols: Int, vararg components: Component) : this(GridLayout(rows, cols), *components) {}
	constructor(lm: LayoutManager, vararg components: Component) : super(lm) {
		for (c in components) {
			add(c)
		}
	}

	constructor(vararg components: Component) {
		for (c in components) {
			add(c)
		}
	}

	fun addTop(comp: Component) {
		require(layout is BorderLayout)
		add(comp, BorderLayout.NORTH)
	}

	fun addBottom(comp: Component) {
		require(layout is BorderLayout)
		add(comp, BorderLayout.SOUTH)
	}

	fun addCenter(comp: Component) {
		require(layout is BorderLayout)
		add(comp, BorderLayout.CENTER)
	}

	fun addLeft(comp: Component) {
		require(layout is BorderLayout)
		add(comp, BorderLayout.WEST)
	}

	fun addRight(comp: Component) {
		require(layout is BorderLayout)
		add(comp, BorderLayout.EAST)
	}
}