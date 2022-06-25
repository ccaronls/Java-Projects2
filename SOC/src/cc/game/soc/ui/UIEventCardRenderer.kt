package cc.game.soc.ui

import cc.game.soc.core.EventCard
import cc.game.soc.core.EventCardType
import cc.game.soc.core.Rules
import cc.lib.game.*
import cc.lib.ui.UIComponent

class UIEventCardRenderer(component: UIComponent) : UIRenderer(component) {
	//private float minCardWidth = 0;
    val diceComps: UIDiceRenderer
	private var eventCard: EventCard = EventCard(EventCardType.NoEvent, 8)
	private var dealAnim: AAnimation<AGraphics>? = null
	var cw = 0f
	var ch = 0f
	var arc = 0f
	var padding = RenderConstants.thickLineThickness
	@Synchronized
	override fun draw(g: APGraphics, px: Int, py: Int) {
		g.textHeight = RenderConstants.textSizeSmall
		g.setTextStyles(AGraphics.TextStyle.NORMAL)
		var cardText = "New Year"
		var helpText: String? = "Event cards wil be shuffled on next event card drawn."
		var production = 0
		val soc = UISOC.instance
		cardText = eventCard.type.name
		helpText = eventCard.getHelpText(soc.rules)
		production = eventCard.production
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
		if (dealAnim != null) {
			dealAnim!!.update(g)
			if (dealAnim!!.isDone) {
				dealAnim = null
			}
			getComponent<UIComponent>().redraw()
		} else {
			drawCard(g, production, cardText)
		}
		g.popMatrix()
		if (diceComps != null) {
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

	fun setEventCard(card: EventCard?) {
		if (card == null) return
		if (eventCard == null) {
			eventCard = card
			getComponent<UIComponent>().redraw()
			return
		}
		val productionIn = eventCard.production
		val productionOut = card.production
		val txtIn = eventCard.name
		val txtOut = card.name
		object : UIAnimation(500, 1, true) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.pushMatrix()
				g.translate(cw / 2, 0f)
				g.scale(1f - position, 1f)
				g.translate(-cw / 2, 0f)
				if (repeat == 0) drawCard(g, productionIn, txtIn) else drawCard(g, productionOut, txtOut)
				g.popMatrix()
			}

			override fun onDone() {
				eventCard = card
				super.onDone()
			}
		}.start<UIAnimation>().also {
			getComponent<UIComponent>().redraw()
			it.block(it.duration + 100)
		}
	}

	init {
		diceComps = UIDiceRenderer(component, false)
	}
}