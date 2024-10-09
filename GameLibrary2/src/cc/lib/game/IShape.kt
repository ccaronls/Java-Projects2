package cc.lib.game

interface IShape {
	fun contains(x: Float, y: Float): Boolean
	infix operator fun contains(v: IVector2D): Boolean {
		return contains(v.x, v.y)
	}

	fun drawOutlined(g: AGraphics)
	fun drawFilled(g: AGraphics)
	val center: IVector2D
	val area: Float
	val enclosingRect: IRectangle
	val radius: Float
}
