package cc.experiments

import cc.lib.game.AnimatedImage
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.utils.getOrNull
import cc.lib.utils.rotate
import java.awt.event.KeyEvent
import java.io.File

/**
 * Ordering here is according to vertical position in CharacterTiles
 */
enum class TileDirection {
	EAST,
	SOUTHEAST,
	SOUTH,
	SOUTHWEST,
	WEST,
	NORTHWEST,
	NORTH,
	NORTHEAST
}

class TileAnimation(val name: String, id: Int) : AnimatedImage(id, GDimension(128, 128), 1300) {
	var direction = TileDirection.EAST
		set(value) {
			field = value
			yPos = value.ordinal
		}
}

class AnimatedTileTest : AWTComponent() {

	init {
		setMouseEnabled(true)
	}

	var tiles = mutableListOf<TileAnimation>()
	var animIndex = 0

	val keyMap = mutableSetOf<Int>()
	var direction = TileDirection.EAST

	val keyLookup = listOf(
		listOf(KeyEvent.VK_RIGHT, KeyEvent.VK_UP) to TileDirection.NORTHEAST,
		listOf(KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN) to TileDirection.SOUTHEAST,
		listOf(KeyEvent.VK_RIGHT) to TileDirection.EAST,
		listOf(KeyEvent.VK_LEFT, KeyEvent.VK_UP) to TileDirection.NORTHWEST,
		listOf(KeyEvent.VK_LEFT, KeyEvent.VK_DOWN) to TileDirection.SOUTHWEST,
		listOf(KeyEvent.VK_LEFT) to TileDirection.WEST,
		listOf(KeyEvent.VK_UP) to TileDirection.NORTH,
		listOf(KeyEvent.VK_DOWN) to TileDirection.SOUTH,
	)

	fun updateDirection() {
		keyLookup.firstOrNull {
			keyMap.containsAll(it.first)
		}?.let { (_, direction) ->
			this.direction = direction
		}
	}


	override fun onDimensionChanged(g: AWTGraphics, width: Int, height: Int) {
		super.onDimensionChanged(g, width, height)
	}

	override fun init(g: AWTGraphics) {
		val dir = "SwingApps/CharacterTiles/"
		File(dir).list().forEach { path ->
			g.loadImage(dir + path).takeIf { it > 0 }?.let {
				tiles.add(TileAnimation(path, it).start() as TileAnimation)
			}
		}
	}

	override fun paint(g: AWTGraphics) {
		val padding = 10
		var yPos = padding
		tiles.forEachIndexed { index, it ->
			if (index == animIndex) {
				g.color = GColor.RED
			} else {
				g.color = GColor.BLACK
			}
			g.drawJustifiedString(width - padding, yPos, Justify.RIGHT, Justify.TOP, it.name)
			yPos += g.textHeight.toInt()
		}
		updateDirection()
		g.pushMatrix()
		g.scale(2f)
		tiles.getOrNull(animIndex)?.let {
			it.direction = direction
			it.update(g)
		}
		g.popMatrix()
		redraw()
	}

	override fun onClick() {
		redraw()
	}

	override fun onKeyPressed(evt: KeyEvent) {
		keyMap.add(evt.keyCode)
		if (evt.keyCode == KeyEvent.VK_N) {
			animIndex = animIndex.rotate(tiles.size)
		}
	}

	override fun onKeyReleased(evt: KeyEvent) {
		keyMap.remove(evt.keyCode)
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			val frame: AWTFrame = object : AWTFrame("Animated Tile Test") {
				override fun onWindowClosing() {
					try {
						//app.figures.saveToFile(app.figuresFile);
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
			frame.add(AnimatedTileTest())
			frame.centerToScreen(600, 500)
		}
	}
}
