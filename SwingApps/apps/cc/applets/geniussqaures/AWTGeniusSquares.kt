package cc.applets.geniussqaures

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.geniussquare.UIGeniusSquares
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.utils.FileUtils
import java.io.File

class AWTGeniusSquares internal constructor() : AWTComponent() {
	val frame: AWTFrame
	val game = object : UIGeniusSquares() {
		override fun repaint() {
			this@AWTGeniusSquares.repaint()
		}
	}
	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		game.paint(g, mouseX, mouseY)
	}

	override fun onClick() {
		game.doClick()
	}

	override fun onDragStarted(x: Int, y: Int) {
		game.startDrag()
	}

	override fun onDragStopped() {
		game.stopDrag()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			Utils.setDebugEnabled()
			AWTGeniusSquares()
		}
	}

	init {
		setMouseEnabled(true)
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		val saveFile = File(settings, "gs.save")
		frame = object : AWTFrame("Genius Squares") {
			override fun onWindowClosing() {
				game.pauseTimer()
				game.trySaveToFile(saveFile)
			}

			override fun onMenuItemSelected(menu: String, subMenu: String) {
				when (subMenu) {
					"New Game" -> game.newGame()
					"Reset Pieces" -> game.resetPieces()
					"Toggle Autofit" -> {
						val autoFit = !game.isAutoFitPieces
						game.isAutoFitPieces = autoFit
						setProperty("autofit", autoFit)
					}
				}
				repaint()
			}
		}
		frame.addMenuBarMenu("GeniusSquares", "New Game", "Reset Pieces", "Toggle Autofit")
		game.isAutoFitPieces = frame.getBooleanProperty("autofit", game.isAutoFitPieces)
		if (!game.tryLoadFromFile(saveFile)) game.newGame()
		frame.add(this)
		frame.setPropertiesFile(File(settings, "gui.properties"))
		if (!frame.restoreFromProperties()) {
			frame.centerToScreen(640, 480)
		}
		game.resumeTimer()
	}
}