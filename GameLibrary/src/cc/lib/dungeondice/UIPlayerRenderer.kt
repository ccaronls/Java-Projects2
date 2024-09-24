package cc.lib.dungeondice

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

class UIPlayerRenderer(component: UIComponent) : UIRenderer(component) {
	var player: DPlayer? = null
	var color = GColor.RED
	override fun draw(g: AGraphics) {
		if (player == null) return
		if (UI.getInstance().turn == player!!.playerNum) {
			g!!.color = color
			g.setLineWidth(5f)
			g.drawRect(0f, 0f, g.viewportWidth.toFloat(), g.viewportHeight.toFloat())
		}
		val txt = StringBuffer()
		txt.append(player!!.name)
		txt.append("\n").append(String.format("%-5s %d", "STR", player!!.str))
		txt.append("\n").append(String.format("%-5s %d", "DEX", player!!.dex))
		txt.append("\n").append(String.format("%-5s %d", "ATT", player!!.attack))
		txt.append("\n").append(String.format("%-5s %d", "DEF", player!!.defense))
		g!!.color = GColor.BLACK
		g.drawString(txt.toString(), 10f, 10f)
		if (player!!.hasKey()) {
			if (keyAsset < 0) {
				keyAsset = g.loadImage("images/key.png", GColor.BLACK)
			}
			g.drawImage(keyAsset, (g.viewportWidth - 30).toFloat(), 5f, 25f, 25f)
		}
	}

	companion object {
		var keyAsset = -1
	}
}