package cc.lib.swing

import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager2

/**
 * Created by chriscaron on 4/18/18.
 */
class AWTButtonLayout : LayoutManager2 {
	@Synchronized
	override fun invalidateLayout(target: Container) {
	}

	override fun addLayoutComponent(name: String, comp: Component) {}
	override fun removeLayoutComponent(comp: Component) {}
	override fun addLayoutComponent(comp: Component, constraints: Any) {}

	constructor() {}
	constructor(target: Container) {
		layoutContainer(target)
	}

	override fun preferredLayoutSize(target: Container): Dimension {
		val size = Dimension()
		val nComponents = target.componentCount
		if (heights.size != nComponents) {
			heights = IntArray(nComponents)
		}
		for (i in 0 until nComponents) {
			val c = target.getComponent(i)
			val d = c.preferredSize
			size.width = Math.max(size.width, d.width)
			size.height += d.height
			heights[i] = d.height
		}
		val insets = target.insets
		size.width = Math.min(size.width + insets.left + insets.right, Int.MAX_VALUE)
		size.height = Math.min(size.height + insets.top + insets.bottom, Int.MAX_VALUE)
		return size
	}

	override fun minimumLayoutSize(target: Container): Dimension {
		val size = Dimension()
		val nComponents = target.componentCount
		if (heights.size != nComponents) {
			heights = IntArray(nComponents)
		}
		for (i in 0 until nComponents) {
			val c = target.getComponent(i)
			val d = c.minimumSize
			size.width = Math.max(size.width, d.width)
			size.height += d.height
			heights[i] = d.height
		}
		val insets = target.insets
		size.width = Math.min(size.width + insets.left + insets.right, Int.MAX_VALUE)
		size.height = Math.min(size.height + insets.top + insets.bottom, Int.MAX_VALUE)
		return size
	}

	override fun maximumLayoutSize(target: Container): Dimension {
		val size = Dimension()
		val nComponents = target.componentCount
		if (heights.size != nComponents) {
			heights = IntArray(nComponents)
		}
		for (i in 0 until nComponents) {
			val c = target.getComponent(i)
			val d = c.maximumSize
			size.width = Math.max(size.width, d.width)
			size.height += d.height
			heights[i] = d.height
		}
		val insets = target.insets
		size.width = Math.min(size.width + insets.left + insets.right, Int.MAX_VALUE)
		size.height = Math.min(size.height + insets.top + insets.bottom, Int.MAX_VALUE)
		return size
	}

	@Synchronized
	override fun getLayoutAlignmentX(target: Container): Float {
		return 0f
	}

	@Synchronized
	override fun getLayoutAlignmentY(target: Container): Float {
		return 0f
	}

	override fun layoutContainer(target: Container) {
		val nChildren = target.componentCount
		val alloc = target.size
		val `in` = target.insets
		alloc.width -= `in`.left + `in`.right
		alloc.height -= `in`.top + `in`.bottom

		// flush changes to the container
		val x = `in`.left
		var y = `in`.top
		var i = 0
		while (i < nChildren && i < heights.size) {
			val c = target.getComponent(i)
			val h = heights[i]
			c.setBounds(x, y, alloc.width, h)
			y += h
			i++
		}
	}

	var heights = IntArray(0)
}