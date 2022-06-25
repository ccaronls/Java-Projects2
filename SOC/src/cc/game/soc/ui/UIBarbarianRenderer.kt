package cc.game.soc.ui

import cc.game.soc.core.SOC
import cc.lib.game.*
import cc.lib.math.Vector2D
import cc.lib.ui.UIComponent

class UIBarbarianRenderer(component: UIComponent) : UIRenderer(component) {
	private var baseImage = 0
	private var shipImage = 0
	private var distance = -1 //positions.length-1;
	fun initAssets(baseImage: Int, shipImage: Int) {
		this.baseImage = baseImage
		this.shipImage = shipImage
	}

	private var anim: UIAnimation? = null

	//private float minShipDim, maxShipDim;
	private var shipDim = 0f
	var textBorderPadding = 5f
	override fun draw(g: APGraphics, pickX: Int, pickY: Int) {
		val wid = getComponent<UIComponent>().width.toFloat()
		val hgt = getComponent<UIComponent>().height.toFloat()
		g.drawImage(baseImage, 0f, 0f, wid, hgt)
		//minShipDim = wid/9;
		//maxShipDim = wid/7;
		shipDim = wid / 8
		anim?.let { a ->
			anim = if (a.isDone) null else {
				a.update(g)
				return
			}
		}
		if (distance >= 0 && distance < positions.size) {
			val d = positions.size - 1 - distance
			val scale = 1f + 1f / (positions.size - 1)
			//float shipDim = minShipDim + (maxShipDim - minShipDim) * scale * d;
			val v: Vector2D = positions[distance].scaledBy(wid, hgt)
			val sh2 = shipDim / 2
			g.drawImage(shipImage, v.sub(sh2, sh2), v.add(sh2, sh2))
		}

		// draw the settlers vs barbrian strengths in either uppleft hand corner or lower right hand corner
		val soc = UISOC.instance ?: return
		val barbStr = SOC.computeBarbarianStrength(soc, soc.board)
		val catanStr = SOC.computeCatanStrength(soc, soc.board)
		val text = String.format("%-10s %d\n%-10s %d", "Settlers:", catanStr, "Barbarians:", barbStr)
		val tb2 = textBorderPadding * 2
		val dim = g.getTextDimension(text, wid - tb2)
		g.textHeight = RenderConstants.textSizeSmall
		if (distance < positions.size / 2) {
			// upper left hand corner
			g.color = GColor.TRANSLUSCENT_BLACK
			g.drawFilledRect(0f, 0f, dim.width + tb2, dim.height + tb2)
			g.color = GColor.CYAN
			g.drawWrapString(textBorderPadding, textBorderPadding, wid - tb2, text)
		} else {
			// lower right hand corner
			g.color = GColor.TRANSLUSCENT_BLACK
			g.drawFilledRect(wid - dim.width - tb2, hgt - dim.height - tb2, dim.width + tb2, dim.height + tb2)
			g.color = GColor.CYAN
			g.drawWrapString(wid - dim.width - textBorderPadding, hgt - dim.height - textBorderPadding, wid - tb2, text)
		}
	}

	fun setDistance(nextDistance: Int) {
		if (nextDistance < distance && distance < positions.size) {
			anim = object : UIAnimation(2000) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					val scale = 1f + 1f / (positions.size - 1)
					val wid = getComponent<UIComponent>().width.toFloat()
					val hgt = getComponent<UIComponent>().height.toFloat()
					val d0 = positions.size - 1 - distance
					val d1 = positions.size - 1 - nextDistance

					//final float min = minShipDim + (maxShipDim - minShipDim) * scale * d0;
					//final float max = minShipDim + (maxShipDim - minShipDim) * scale * d1;
					val v0: Vector2D = positions[distance].scaledBy(wid, hgt)
					val v1: Vector2D = positions[nextDistance].scaledBy(wid, hgt)
					val pos: Vector2D = v0.add(v1.sub(v0).scaledBy(position))
					val sh2 = 0.5f * shipDim //min + (max-min)*position;
					g.drawImage(shipImage, pos.sub(sh2, sh2), pos.add(sh2, sh2))
					getComponent<UIComponent>().redraw()
				}
			}.start<UIAnimation>().also {
				getComponent<UIComponent>().redraw()
				it.block()
			}
		}
		distance = nextDistance
		getComponent<UIComponent>().redraw()
	}

	fun onBarbarianAttack(catanStrength: Int, barbarianStrength: Int, playerStatus: Array<String>) {
		distance = positions.size - 1
		getComponent<UIComponent>().redraw()
	}

	companion object {
		private const val IMAGE_WIDTH = 1f / 454
		private const val IMAGE_HEIGHT = 1f / 502
		private val positions = arrayOf<Vector2D>(
			Vector2D(64f, 431f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(192f, 429f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(319f, 404f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(284f, 283f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(199f, 194f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(128f, 89f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(255f, 74f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
			Vector2D(383f, 66f).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT))
	}
}