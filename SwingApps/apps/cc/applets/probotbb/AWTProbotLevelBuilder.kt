package cc.applets.probotbb

import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.probot.Direction
import cc.lib.probot.Level
import cc.lib.probot.Probot
import cc.lib.probot.Type
import cc.lib.reflector.Reflector
import cc.lib.swing.*
import cc.lib.utils.FileUtils
import cc.lib.utils.Grid
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTextPane

class AWTProbotLevelBuilder internal constructor() : AWTComponent() {
	val frame: AWTFrame
	val cellType: AWTRadioButtonGroup<Type>
	val prev1: AWTButton
	val next1: AWTButton
	val prev10: AWTButton
	val next10: AWTButton
	val levelNumLabel = JLabel()
	var lazer0: AWTToggleButton? = null
	var lazer1: AWTToggleButton? = null
	var lazer2: AWTToggleButton? = null
	var npTurns: AWTNumberPicker? = null
	var npJumps: AWTNumberPicker? = null
	var npLoops: AWTNumberPicker? = null
	val levelLabel: AWTEditText
	val info = JTextPane()
	val probot: Probot = object : Probot() {
		override fun getStrokeWidth(): Float {
			return 5f
		}
	}
	val levels = ArrayList<Level>()
	var curLevel = 0
	val BACKUP_FILE: File
	val LEVEL_FILE = "levelsFile"
	private val level: Level
		get() = levels[curLevel]

	private fun updateAll() {
		levelNumLabel.text = "Level " + (curLevel + 1) + " of " + levels.size
		val level = level
		grid.setGrid(level.coins)
		lazer0!!.isSelected = level.lazers[0]
		lazer1!!.isSelected = level.lazers[1]
		lazer2!!.isSelected = level.lazers[2]
		prev1.isEnabled = curLevel > 0
		prev10.isEnabled = curLevel > 0
		next10.isEnabled = curLevel < levels.size - 1
		npJumps!!.value = level.numJumps
		npLoops!!.value = level.numLoops
		npTurns!!.value = level.numTurns
		levelLabel.text = level.label
		info.text = level.info
		frame.title = frame.getStringProperty(LEVEL_FILE, "<UNNAMED>")
	}

	var pickCol = -1
	var pickRow = -1
	var grid: Grid<Type> = Grid<Type>()

	init {
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		BACKUP_FILE = File(settings, "levels_backup.txt")
		frame = object : AWTFrame() {
			override fun onWindowClosing() {
				try {
					Reflector.serializeToFile<Any>(levels, BACKUP_FILE)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		run {
			if (BACKUP_FILE.isFile) {
				try {
					levels.addAll(Reflector.deserializeFromFile(BACKUP_FILE))
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (levels.size == 0) {
				levels.add(Level())
			}
			probot.setLevel(0, levels[0])
		}
		setMouseEnabled(true)
		frame.add(this)
		frame.addMenuBarMenu("File", { ev ->
			when (ev.actionCommand) {
				"New" -> {
					frame.setProperty(LEVEL_FILE, null)
					levels.clear()
					levels.add(Level())
					updateAll()
				}
				"Open" -> {
					val toOpen = frame.showFileOpenChooser("Open Levels File", "txt", null)
					if (toOpen != null) {
						frame.log.info("Opening '%s'", toOpen)
						try {
							val newLevels = Reflector.deserializeFromFile<List<Level>>(toOpen)
							frame.setProperty(LEVEL_FILE, toOpen.absolutePath)
							levels.clear()
							levels.addAll(newLevels)
							updateAll()
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
				}
				"Save" -> try {
					val levelsStr = frame.getProperties().getProperty(LEVEL_FILE)
					frame.log.info("Saving to '%s'", levelsStr)
					if (levelsStr == null) {
						val levelsFile = frame.showFileSaveChooser("Choose File to save", "Probot Levels", null, null)
						if (levelsFile != null) {
							Reflector.serializeToFile<Any>(levels, levelsFile)
							frame.setProperty(LEVEL_FILE, levelsFile.absolutePath)
						}
					} else {
						Reflector.serializeToFile<Any>(levels, File(levelsStr))
					}
					updateAll()
				} catch (e: Exception) {
					frame.setProperty(LEVEL_FILE, null)
					e.printStackTrace()
				}
				"Save as" -> {
					var levelsFile: File? = null
					val str = frame.getProperties().getProperty(LEVEL_FILE)
					if (str != null) {
						levelsFile = File(str)
					}
					val newFile = frame.showFileSaveChooser("Choose File to save", "Probot Levels", null, levelsFile)
					if (newFile != null) {
						frame.log.info("Saving to '%d'", newFile)
						try {
							Reflector.serializeToFile<Any>(levels, newFile)
							frame.setProperty(LEVEL_FILE, newFile.absolutePath)
							updateAll()
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
				}
			}
		}, "New", "Open", "Save", "Save as")
		val rhs = AWTPanel(0, 1)
		cellType = object : AWTRadioButtonGroup<Type>(rhs) {
			override fun onChange(extra: Type) {}
		}
		for (t in Type.values()) {
			cellType.addButton(t.displayName, t)
		}
		frame.add(rhs, BorderLayout.EAST)
		val lhs = AWTPanel()
		lhs.layout = AWTButtonLayout(lhs)
		frame.add(lhs, BorderLayout.WEST)
		info.addKeyListener(object : KeyListener {
			override fun keyTyped(e: KeyEvent) {
				levels[curLevel].info = info.text
			}

			override fun keyPressed(e: KeyEvent) {
				levels[curLevel].info = info.text
			}

			override fun keyReleased(e: KeyEvent) {
				levels[curLevel].info = info.text
			}
		})
		lhs.add(object : AWTButton("Clear") {
			override fun onAction() {
				grid.init(1, 1, Type.EM)
				level.coins = Array(grid.rows) { row ->
					Array(grid.cols) { col ->
						level.coins[row][col]
					}
				}
				this@AWTProbotLevelBuilder.repaint()
			}
		})
		lhs.add(object : AWTButton("Reload") {
			override fun onAction() {
				if (BACKUP_FILE.isFile) {
					try {
						levels.clear()
						levels.addAll(Reflector.deserializeFromFile(BACKUP_FILE))
						updateAll()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		})
		lhs.add(object : AWTButton("Insert\nBefore") {
			override fun onAction() {
				levels.add(curLevel, Level())
				probot.setLevel(curLevel, levels[curLevel])
				updateAll()
				this@AWTProbotLevelBuilder.repaint()
			}
		})
		lhs.add(AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Turns").build { _, newValue: Int -> level.numTurns = newValue }.also { npTurns = it })
		lhs.add(AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Loops").build { _, newValue: Int -> level.numLoops = newValue }.also { npLoops = it })
		lhs.add(AWTNumberPicker.Builder().setMin(-1).setMax(10).setValue(-1).setLabel("Jumps").build { _, newValue: Int -> level.numJumps = newValue }.also { npJumps = it })
		lhs.add(object : AWTToggleButton("Lazer 0") {
			override fun onToggle(on: Boolean) {
				level.lazers[0] = on
				probot.setLazerEnabled(0, on)
				this@AWTProbotLevelBuilder.repaint()
			}
		}.also { lazer0 = it })
		lhs.add(object : AWTToggleButton("Lazer 1") {
			override fun onToggle(on: Boolean) {
				level.lazers[1] = on
				probot.setLazerEnabled(1, on)
				this@AWTProbotLevelBuilder.repaint()
			}
		}.also { lazer1 = it })
		lhs.add(object : AWTToggleButton("Lazer 2") {
			override fun onToggle(on: Boolean) {
				level.lazers[2] = on
				probot.setLazerEnabled(2, on)
				this@AWTProbotLevelBuilder.repaint()
			}
		}.also { lazer2 = it })
		info.preferredSize = Dimension(100, 300)
		lhs.add(JScrollPane(info))
		prev1 = object : AWTButton("<") {
			override fun onAction() {
				if (curLevel > 0) {
					probot.setLevel(--curLevel, levels[curLevel])
					updateAll()
					this@AWTProbotLevelBuilder.repaint()
				}
			}
		}
		next1 = object : AWTButton(">") {
			override fun onAction() {
				if (curLevel == levels.size - 1) {
					levels.add(Level())
				}
				probot.setLevel(++curLevel, levels[curLevel])
				updateAll()
				this@AWTProbotLevelBuilder.repaint()
			}
		}
		prev10 = object : AWTButton("<<") {
			override fun onAction() {
				if (curLevel > 0) {
					curLevel = Math.max(0, curLevel - 10)
					probot.setLevel(curLevel, levels[curLevel])
					updateAll()
					this@AWTProbotLevelBuilder.repaint()
				}
			}
		}
		next10 = object : AWTButton(">>") {
			override fun onAction() {
				synchronized(this@AWTProbotLevelBuilder) {
					if (curLevel < levels.size - 1) {
						curLevel = Math.min(curLevel + 10, levels.size - 1)
						probot.setLevel(curLevel, levels[curLevel])
						updateAll()
						this@AWTProbotLevelBuilder.repaint()
					}
				}
			}
		}
		levelLabel = object : AWTEditText("", 32) {
			override fun onTextChanged(newText: String) {
				level.label = newText
			}
		}
		val top = AWTPanel(levelLabel, prev10, prev1, levelNumLabel, next1, next10)
		frame.add(top, BorderLayout.NORTH)
		if (!frame.loadFromFile(File(settings, "probotlevelbuilder.properties"))) frame.centerToScreen(800, 640)
		grid.setGrid(level.coins)
		updateAll()
	}

	@Synchronized
	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		if (levels.size == 0) return
		// draw a grid
		val cellDim = Math.min(g.viewportWidth, g.viewportHeight) / 20
		val viewWidth = g.viewportWidth
		val viewHeight = g.viewportHeight

//        probot.setLevel(curLevel, getLevel());
		probot.draw(g, grid.cols * cellDim, grid.rows * cellDim)
		g.color = GColor.WHITE
		for (i in 0..cellDim) {
			g.drawLine(0f, (i * cellDim).toFloat(), viewWidth.toFloat(), (i * cellDim).toFloat())
			g.drawLine((i * cellDim).toFloat(), 0f, (i * cellDim).toFloat(), viewHeight.toFloat())
		}
		pickCol = mouseX / cellDim
		pickRow = mouseY / cellDim

		//Utils.println("paint mx=" + mouseX + " my=" + mouseY);
		g.color = GColor.RED
		g.drawRect((pickCol * cellDim).toFloat(), (pickRow * cellDim).toFloat(), cellDim.toFloat(), cellDim.toFloat())
	}

	@Synchronized
	override fun keyPressed(evt: KeyEvent) {
		if (!grid.isValid(pickCol, pickRow)) return
		var t = grid[pickRow, pickRow]
		var values = arrayOf<Type?>()
		when (t) {
			Type.EM, Type.DD -> values = Utils.toArray(Type.EM, Type.DD)
			Type.SE, Type.SS, Type.SW, Type.SN -> values = Utils.toArray(Type.SE, Type.SS, Type.SW, Type.SN)
			Type.LH0, Type.LV0, Type.LH1, Type.LV1, Type.LH2, Type.LV2 -> values = Utils.toArray(Type.LH0, Type.LV0, Type.LH1, Type.LV1, Type.LH2, Type.LV2)
			Type.LB0, Type.LB1, Type.LB2, Type.LB -> values = Utils.toArray(Type.LB0, Type.LB1, Type.LB2, Type.LB)
		}
		when (evt.keyCode) {
			KeyEvent.VK_LEFT -> t = Utils.decrementValue(t, *values)
			KeyEvent.VK_RIGHT -> t = Utils.incrementValue(t, *values)
		}
		grid[pickRow, pickCol] = t
		repaint()
	}

	override fun onClick() {
		if (pickCol < 0 || pickRow < 0) {
			return
		}
		grid.ensureCapacity(pickRow + 1, pickCol + 1, Type.EM)
		level.coins = Array(grid.rows) { row ->
			Array(grid.cols) { col ->
				grid.get(row, col)
			}
		}
		cellType.getChecked()?.let {
			grid[pickRow, pickCol] = cellType.getChecked()
		}
		repaint()
	}

	fun drawGuy(g: AWTGraphics, x: Int, y: Int, radius: Int, dir: Direction) {
		g.drawFilledCircle(x, y, radius)
		g.color = GColor.BLACK
		when (dir) {
			Direction.Right -> g.drawLine(x.toFloat(), y.toFloat(), (x + radius).toFloat(), y.toFloat(), 4f)
			Direction.Down -> g.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + radius).toFloat(), 4f)
			Direction.Left -> g.drawLine(x.toFloat(), y.toFloat(), (x - radius).toFloat(), y.toFloat(), 4f)
			Direction.Up -> g.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y - radius).toFloat(), 4f)
		}
	}

	companion object {
		@Throws(Exception::class)
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			AWTProbotLevelBuilder()
		}
	}
}