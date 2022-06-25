package cc.game.soc.ui

import cc.game.soc.core.Dice
import cc.game.soc.core.DiceEvent
import cc.game.soc.core.DiceType
import cc.lib.game.*
import cc.lib.ui.UIComponent
import java.util.*
import kotlin.coroutines.CoroutineContext

class UIDiceRenderer(component: UIComponent, attach: Boolean) : UIRenderer(component, attach) {
	private var diceRect: GDimension? = null
	private var dice: List<Dice> = ArrayList()
	private var picked = -1
	private var pickable = 0

	fun initImages(_shipImageId: Int, redTradeCityImageId: Int, greenSciCityImageId: Int, bluePoliCityImageId: Int) {
		shipImageId = _shipImageId
		tradeCityImageId = redTradeCityImageId
		scienceCityImageId = greenSciCityImageId
		politicsCityImageId = bluePoliCityImageId
	}

	fun setDice(dice: List<Dice>) {
		this.dice = dice
		getComponent<UIComponent>().redraw()
	}

	fun setPickableDice(num: Int) {
		pickable = num
		getComponent<UIComponent>().redraw()
	}

	val pickedDiceNums: IntArray
		get() {
			val nums = IntArray(pickable)
			for (i in 0 until pickable) {
				nums[i] = dice!![i].num
			}
			return nums
		}

	fun spinDice(spinTimeMs: Long, which: List<Dice>) {
		spinAnim = object : UIAnimation2(spinTimeMs) {
			var delayMillis: Long = 10
			override fun draw(g: APGraphics, position: Float, dt: Float) {
				for (d in which) {
					d.setNum(1 + Utils.rand() % 6, false)
				}
				drawPrivate(g, 0, 0, which)
				kotlin.runCatching {
					block(delayMillis)
					getComponent<UIComponent>().redraw()
				}
			}

		}.start<UIAnimation2>().also {
			getComponent<UIComponent>().redraw()
			it.block(spinTimeMs + 500)
		}
	}

	private var spinAnim: UIAnimation2? = null
	fun setDiceRect(rect: GDimension?) {
		diceRect = rect
	}

	private val dim: GDimension
		private get() = diceRect?:GDimension(getComponent<UIComponent>().width.toFloat(), getComponent<UIComponent>().height.toFloat())

	override fun draw(g: APGraphics, pickX: Int, pickY: Int) {
		picked = -1
		if (Utils.isEmpty(dice)) return
		if (spinAnim != null) {
			spinAnim = if (spinAnim!!.isDone) {
				null
			} else {
				spinAnim!!.update(g)
				return
			}
		}
		drawPrivate(g, pickX, pickY, dice)

		//g.setColor(GColor.BLACK);
		//g.drawRect(0, 0, diceRect.width, diceRect.height, 3);
	}

	private fun drawPrivate(g: APGraphics, pickX: Int, pickY: Int, dice: List<Dice>) {
		g.pushMatrix()
		run {
			val dim = dim
			var dieDim = dim.height
			val len = dice.size
			if (dieDim * len + dieDim / 4 * (len - 1) > dim.width) {
				dieDim = 4 * dim.width / (5 * len - 1)
				g.translate(0f, dim.height / 2 - dieDim / 2)
			}
			val spacing = dieDim / 4
			val dw = dieDim * len + spacing * (len - 1)
			g.translate(dim.width / 2 - dw / 2, 0f)
			g.pushMatrix()
			run {
				g.begin()
				for (index in 0 until len) {
					g.setName(index)
					g.vertex(0f, 0f)
					g.vertex(dieDim, dieDim)
					g.translate(dieDim + spacing, 0f)
				}
			}
			g.popMatrix()
			picked = g.pickRects(pickX, pickY)
			val index = 0
			for (d in dice) {
				drawDie(g, dieDim, d.type, d.num)
				if (index == picked) {
					g.color = GColor.RED
					g.pushMatrix()
					g.scale(1.1f, 1.1f)
					g.drawRoundedRect(0f, 0f, dieDim, dieDim, 1f, dieDim / 4)
					g.popMatrix()
				}
				g.translate(dieDim + spacing, 0f)
			}
		}
		g.popMatrix()
	}

	fun drawDie(g: AGraphics, dim: Float, type: DiceType, num: Int) {
		when (type) {
			DiceType.Event -> drawEventDie(g, dim, num)
			DiceType.RedYellow -> drawDie(g, dim, GColor.RED, GColor.YELLOW, num)
			DiceType.WhiteBlack -> drawDie(g, dim, GColor.WHITE, GColor.BLACK, num)
			DiceType.YellowRed -> drawDie(g, dim, GColor.YELLOW, GColor.RED, num)
		}
	}

	fun doClick() {
		dice?.takeIf { picked >= 0 && picked < it.size }?.let { dice ->
			var num = dice[picked].num
			if (++num > 6) {
				num = 1
			}
			dice[picked].setNum(num, true)
		}
		getComponent<UIComponent>().redraw()
	}

	fun drawEventDie(g: AGraphics, dim: Float, dieNum: Int) {
		g.color = GColor.WHITE
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		when (DiceEvent.fromDieNum(dieNum)) {
			DiceEvent.AdvanceBarbarianShip -> g.drawImage(shipImageId, 0f, 0f, dim, dim)
			DiceEvent.PoliticsCard -> g.drawImage(politicsCityImageId, 0f, 0f, dim, dim)
			DiceEvent.ScienceCard -> g.drawImage(scienceCityImageId, 0f, 0f, dim, dim)
			DiceEvent.TradeCard -> g.drawImage(tradeCityImageId, 0f, 0f, dim, dim)
		}
	}

	fun drawDie(g: AGraphics, dim: Float, dieColor: GColor?, dotColor: GColor, numDots: Int) {
		g.color = dieColor
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		g.color = dotColor
		val dd2 = dim / 2
		val dd4 = dim / 4
		val dd34 = dim * 3 / 4
		val dotSize = dim / 8
		val oldDotSize = g.setPointSize(dotSize)
		g.begin()
		when (numDots) {
			1 -> g.vertex(dd2, dd2)
			2 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
			}
			3 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd34, dd34)
			}
			4 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
			}
			5 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd2, dd2)
			}
			6 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			else -> assert(false // && "Invalid die");
			)
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
	}

	companion object {
		// There are 2 dice renderers, one with and without an associated event card renderer. hmmmmm
		// I dont want to init twice since the event card renderer is a bit hidden. Hmmmm.
		private var shipImageId = -1
		private var tradeCityImageId = -1
		private var scienceCityImageId = -1
		private var politicsCityImageId = -1
	}
}