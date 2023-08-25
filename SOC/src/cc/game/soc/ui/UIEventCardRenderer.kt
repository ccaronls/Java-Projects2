package cc.game.soc.ui

import cc.game.soc.core.EventCard
import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.ui.UIComponent

class UIEventCardRenderer(component: UIComponent) : UIRenderer(component) {

    val diceComps: UIDiceRenderer = UIDiceRenderer(component, false)
	val log = LoggerFactory.getLogger(javaClass)

	private var eventCard: EventCard? = null

	private var dealAnim: AAnimation<AGraphics>? = null
	var cw = 0f
	var ch = 0f
	var arc = 0f
	var padding = RenderConstants.thickLineThickness

	@Synchronized
	override fun draw(g: APGraphics, px: Int, py: Int) {
		eventCard?.let { card ->
			g.textHeight = RenderConstants.textSizeSmall
			g.setTextStyles(AGraphics.TextStyle.NORMAL)
			val cardText = card.type.getNameId()
			val helpText: String? = card.getHelpText(UISOC.instance.rules)
			val production = card.production
			ch = getComponent<UIComponent>().height.toFloat()
			cw = ch * 2 / 3
			if (cw > getComponent<UIComponent>().width / 2) {
				cw = (getComponent<UIComponent>().width / 2).toFloat()
				ch = cw * 3 / 2
			}
			val tw = g.viewportWidth - cw - 3 * padding
			val r = g.getTextDimension(helpText, tw)
			val cx = g.viewportWidth - cw
			val cy = g.viewportHeight - ch
			arc = Math.min(cw, ch) / 5
			g.color = GColor.WHITE
			g.setTextStyles(AGraphics.TextStyle.NORMAL)
			g.drawWrapString(padding, cy + padding, tw, helpText)
			val dieDim = cw / 2 - 4 * padding
			val dy = cy + ch - 2 * padding - dieDim
			g.pushMatrix()
			g.translate(cx, cy)
			dealAnim?.let { anim ->
				anim.update(g)
				if (anim.isDone) {
					dealAnim = null
				}
				getComponent<UIComponent>().redraw()
			} ?: run {
				drawCard(g, production, cardText)
			}
			g.popMatrix()
			g.pushMatrix()
			g.translate(cx, dy)
			diceComps.setDiceRect(GDimension(cw, dieDim))
			diceComps.draw(g, px, py)
			diceComps.setDiceRect(null)
			g.popMatrix()
		}
	}

	private fun drawCard(g: AGraphics, production: Int, txt: String) {
		val fh = g.textHeight
		g.color = GColor.BLUE
		g.drawFilledRoundedRect(0f, 0f, cw, ch, arc)
		g.color = GColor.YELLOW
		g.drawFilledRoundedRect(padding, padding, cw - 2 * padding, ch - 2 * padding, arc - 2)
		g.color = GColor.BLACK
		g.drawWrapString(cw / 2, ch / 3, cw, Justify.CENTER, Justify.TOP, txt)
		if (production > 0) {
			g.setTextStyles(AGraphics.TextStyle.BOLD)
			g.color = GColor.BLUE
			val ovalThickness = RenderConstants.thinLineThickness
			val ovalWidth = fh * 2
			val ovalHeight = fh + ovalThickness + RenderConstants.textMargin
			g.drawFilledOval(cw / 2 - ovalWidth / 2, padding * 2, ovalWidth, ovalHeight)
			g.color = GColor.YELLOW
			g.drawFilledOval(cw / 2 - ovalWidth / 2 + ovalThickness, padding * 2 + ovalThickness, ovalWidth - ovalThickness * 2, ovalHeight - ovalThickness * 2)
			g.color = GColor.BLACK
			g.drawJustifiedString(cw / 2, padding * 2 + ovalHeight / 2, Justify.CENTER, Justify.CENTER, production.toString())
		}
	}

	fun setEventCard(newCard: EventCard) {
		log.debug("setEventCard $newCard")
		eventCard?.also { oldCard ->
			val productionIn = oldCard.production
			val productionOut = newCard.production
			val txtIn = oldCard.name
			val txtOut = newCard.name
			dealAnim = object : UIAnimation(500, 1, true) {
				override fun draw(g: AGraphics, position: Float, dt: Float) {
					g.pushMatrix()
					g.translate(cw / 2, 0f)
					g.scale(1f - position, 1f)
					g.translate(-cw / 2, 0f)
					if (repeat == 0) drawCard(g, productionIn, txtIn) else drawCard(g, productionOut, txtOut)
					g.popMatrix()
				}

				override fun onDone() {
					eventCard = newCard
					super.onDone()
				}
			}.start<UIAnimation>().also {
				redraw()
				it.block(it.duration + 100)
			}
		}?:run {
			eventCard = newCard
			redraw()
		}
	}

	override fun reset() {
		eventCard = null
		redraw()
	}

}