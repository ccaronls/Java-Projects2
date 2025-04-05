package cc.lib.game

interface IShape {
	fun contains(x: Float, y: Float): Boolean
	operator fun contains(v: IVector2D): Boolean {
		return contains(v.x, v.y)
	}

	fun drawOutlined(g: AGraphics)
	fun drawFilled(g: AGraphics)
	val center: IVector2D
	val area: Float
	fun enclosingRect(): IRectangle
	val radius: Float
}
