package cc.lib.swing

import cc.lib.board.BCell
import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.board.CustomBoard
import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.utils.FileUtils
import cc.lib.utils.GException
import cc.lib.utils.Table
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.io.File
import java.util.Collections
import java.util.LinkedList

abstract class AWTBoardBuilder<V : BVertex, E : BEdge, C : BCell, T : CustomBoard<V, E, C>> : AWTComponent() {
	private val log = LoggerFactory.getLogger(javaClass)
	val frame: AWTFrame = object : AWTFrame() {
		override fun onMenuItemSelected(menu: String, subMenu: String) {
			this@AWTBoardBuilder.onMenuItemSelected(menu, subMenu)
		}

		override fun onWindowClosing() {
			board.trySaveToFile(boardFile)
		}
	}

	class KeyAction(val key: String, val description: String, val action: Runnable) : Comparable<KeyAction> {
		val compareKey: Int
			get() {
				val c = key[0]
				return if (Character.isUpperCase(c)) 256 + c.toInt() else c.toInt()
			}

		override operator fun compareTo(o: KeyAction): Int {
			return Integer.compare(compareKey, o.compareKey)
		}
	}

	enum class PickMode {
		VERTEX,
		EDGE,
		CELL
	}

	abstract inner class Tool(val name: String) {
		override fun toString(): String {
			return name
		}

		override fun equals(o: Any?): Boolean {
			if (this === o) return true
			val s = o.toString()
			return Utils.isEquals(name, s)
		}

		open fun onPick() {
			when (pickMode) {
				PickMode.VERTEX -> {
					// add a new vertex if we are not highlighting an existing one
					val picked = highlightedIndex
					if (picked < 0) {
						val v = board.addVertex(mousePos)
						val s: Int = selectedIndex
						pushUndoAction() {
							board.removeVertex(v)
							selectedIndex = s
						}
						highlightedIndex = v
					}
					if (multiSelect) {
						if (highlightedIndex >= 0 && !selected.contains(highlightedIndex)) selected.add(highlightedIndex) else selected.remove(highlightedIndex as Any)
					} else {
						selectedIndex = highlightedIndex
					}
				}
				else -> if (multiSelect) {
					if (highlightedIndex >= 0 && !selected.contains(highlightedIndex)) selected.add(highlightedIndex) else selected.remove(highlightedIndex as Any)
				} else {
					selectedIndex = highlightedIndex
				}
			}
		}

		open fun onDraw(g: AWTGraphics) {}

		/*
        Override this is init tools as necessary
         */
		open fun onActivated() {}
		open fun onKeyTyped(keyCode: Int): Boolean {
			return false
		}
	}

	var tools = mutableListOf(
		object : Tool("NONE") {},
		object : Tool("EDGE BUILDER") {
			override fun onActivated() {
				setPickMode(PickMode.VERTEX)
				setMultiselect(false)
			}

			override fun onPick() {
				if (pickMode == PickMode.VERTEX && !multiSelect) {
					val sel: Int = selectedIndex
					if (highlightedIndex < 0) {
						super.onPick()
					}
					if (highlightedIndex >= 0 && sel >= 0 && highlightedIndex != sel) {
						val h = highlightedIndex
						board.getOrAddEdge(sel, h)
						pushUndoAction {
							board.removeEdge(sel, h)
						}
					}
					selectedIndex = highlightedIndex
				} else {
					super.onPick()
				}
			}
		},
		object : Tool("CELL BUILDER") {
			val wrappedIndices: MutableList<Int> = ArrayList()
			var inside = false
			override fun onActivated() {
				setPickMode(PickMode.VERTEX)
				setMultiselect(true)
			}

			override fun onPick() {
				if (inside) {
					onCellAdded(board.getCell(board.addCell(*wrappedIndices.toIntArray())))
				} else {
					super.onPick()
				}
			}

			override fun onDraw(g: AWTGraphics) {
				val vmap: MutableMap<BVertex, Int> = HashMap()
				for (idx in selected) {
					vmap[board.getVertex(idx)] = idx
				}
				val wrapped = Utils.computeGiftWrapVertices(vmap.keys)
				wrappedIndices.clear()
				for (v in wrapped) {
					vmap[v]?.let { wrappedIndices.add(it) }
				}
				if (Utils.isPointInsidePolygon(mouse, wrapped).also { inside = it }) {
					g.color = GColor.RED
				} else {
					g.color = GColor.ORANGE
				}
				g.begin()
				for (v in wrapped) {
					g.vertex(v)
				}
				g.drawLineLoop(4f)
			}
		}
	)
	var DEFAULT_FILE = File("AWTBoardBuilder.default")
	val board: T
	var backgroundImageId = -1
	var highlightedIndex = -1
	private var boardFile: File? = null
	val undoList = LinkedList<() -> Unit>()
	var pickMode = PickMode.VERTEX
		private set
	private var showNumbers = true
	var showHelp = false
	var rect: GRectangle? = null
	var multiSelect = false
	var selected: MutableList<Int> = ArrayList()
	val mouse = MutableVector2D()
	val actions: MutableMap<Int, KeyAction> = HashMap()
	var tool: Tool = tools[0]
	var progress = 0f

	init {
		board = newBoard()
		setMouseEnabled(true)
		setPadding(10)
		initFrame(frame)
		frame.add(this)
		try {
			val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
			if (!frame.loadFromFile(File(settings, propertiesFileName))) frame.centerToScreen(640, 480)
			DEFAULT_FILE = File(settings, defaultBoardFileName)
		} catch (e: Exception) {
			e.printStackTrace()
			frame.centerToScreen(640, 480)
		}
		board.setDimension(width.toFloat(), height.toFloat())
		initActions()
		focusTraversalKeysEnabled = false
	}

	protected abstract fun newBoard(): T
	protected abstract val propertiesFileName: String
	protected abstract val defaultBoardFileName: String

	/**
	 * This is a good place to add top bar menus
	 * @param frame
	 */
	private fun initFrame(frame: AWTFrame) {
		registerTools()
		frame.addMenuBarMenu("File", "New Board", "Load Board", "Load Image", "Clear Image", "Save As...", "Save")
		frame.addMenuBarMenu("Select", "All", "None")
		frame.addMenuBarMenu("Mode", *Utils.toStringArray(PickMode.values(), false))
		frame.addMenuBarMenu("Tool", *Utils.toStringArray(tools))
		frame.addMenuBarMenu("Action", *actionMenuActions.toTypedArray())
		registerActionBarItems(frame)
	}

	protected val actionMenuActions: ArrayList<String>
		protected get() {
			val list = ArrayList<String>()
			list.addAll(Utils.toList("Compute", "Clear", "Undo", "Generate Grid"))
			return list
		}

	protected open fun registerActionBarItems(frame: AWTFrame) {}

	/**
	 * Override this method to add your tools.
	 * Be sure to @CallSuper
	 */
	protected open fun registerTools() {}

	/**
	 * Handle menu pushes. Call super if not handled.
	 *
	 * @param menu
	 * @param subMenu
	 */
	protected open fun onMenuItemSelected(menu: String, subMenu: String) {
		when (menu) {
			"File" -> onFileMenu(subMenu)
			"Select" -> onSelectMenu(subMenu)
			"Mode" -> onModeMenu(PickMode.valueOf(subMenu))
			"Action" -> onActionMenu(subMenu)
			"Tool" -> setTool(subMenu)
			else -> log.warn("Unhandled case %s", menu)
		}
	}

	override fun init(g: AWTGraphics) {
		val p = frame.getProperties()
		boardFile = File(p.getProperty("boardFile", DEFAULT_FILE.path))
		pickMode = PickMode.valueOf(p.getProperty("pickMode", pickMode.name))
		multiSelect = java.lang.Boolean.valueOf(p.getProperty("multiSelect", "false"))
		progress += 0.1f
		val image = p.getProperty("image")
		if (image != null) backgroundImageId = g.loadImage(image)
		progress = 0.75f
		board.tryLoadFromFile(boardFile)
		progress = 1f
		setTool(p.getProperty("tool", tool.name))
	}

	private fun setTool(name: String) {
		tool = tools.first { it.name == name }
		tool.onActivated()
		frame.setProperty("tool", name)
	}

	override val initProgress: Float
		get() = progress

	override fun onDimensionChanged(g: AWTGraphics, width: Int, height: Int) {
		rect = null
	}

	@Synchronized
	override fun paint(g: AWTGraphics) {
		if (rect == null) {
			rect = GRectangle(0f, 0f, g.viewportWidth.toFloat(), g.viewportHeight.toFloat())
			board.setDimension(rect!!.dimension)
		}
		g.setIdentity()
		g.ortho(rect)
		mouse.assign(mouseX.toFloat(), mouseY.toFloat())
		if (backgroundImageId >= 0) {
			g.drawImage(backgroundImageId, GRectangle(0f, 0f, width.toFloat(), height.toFloat()))
		} else {
			g.clearScreen(GColor.DARK_GRAY)
		}
		g.color = GColor.YELLOW
		g.setLineWidth(3f)
		board.drawEdges(g)
		g.setPointSize(5f)
		g.color = GColor.GREEN
		board.drawVerts(g)
		g.color = GColor.BLUE
		board.drawCells(g, 0.9f)
		when (pickMode) {
			PickMode.VERTEX -> drawVertexMode(g, mouse)
			PickMode.EDGE -> drawEdgeMode(g, mouseX, mouseY)
			PickMode.CELL -> {
				g.screenToViewport(mouse)
				drawCellMode(g, mouseX, mouseY)
			}
		}
		tool.onDraw(g)
		g.ortho()
		g.color = GColor.RED
		val lines: MutableList<String> = ArrayList()
		getDisplayData(lines)
		val buf = StringBuffer()
		for (s in lines) {
			if (buf.length > 0) buf.append("\n")
			buf.append(s)
		}
		g.color = GColor.YELLOW
		g.drawWrapStringOnBackground((width - 10).toFloat(), 10f, (width / 4).toFloat(), Justify.RIGHT, Justify.TOP, buf.toString(), GColor.TRANSLUSCENT_BLACK, 3f)
		if (showHelp) {
			drawHelp(g)
		}
		checkScrolling()
	}

	fun drawHelp(g: AGraphics) {
		g.color = GColor.YELLOW
		val table = Table()
		actions.values.toSortedSet().forEach { action ->
			table.addRow(action.key, action.description)
		}
		table.draw(g, g.viewport.center)
	}

	fun checkScrolling() {
		val width = aPGraphics.viewportWidth
		val height = aPGraphics.viewportHeight
		val margin = Math.min(width, height) / 10
		val mouseX = mouseX
		val mouseY = mouseY
		val scrollSpeed = 3
		var refresh = false
		if (mouseX > 0 && mouseX < margin) {
			if (rect!!.left > 0) {
				rect!!.left -= scrollSpeed.toFloat()
				refresh = true
			}
		} else if (mouseX < width && mouseX > width - margin) {
			if (rect!!.left + rect!!.width < width) {
				rect!!.left += scrollSpeed.toFloat()
				refresh = true
			}
		}
		if (mouseY > 0 && mouseY < margin) {
			if (rect!!.top > 0) {
				rect!!.top -= scrollSpeed.toFloat()
				refresh = true
			}
		} else if (mouseY < height && mouseY > height - margin) {
			if (rect!!.top + rect!!.height < height) {
				rect!!.top += scrollSpeed.toFloat()
				refresh = true
			}
		}
		if (refresh && isFocused) {
			Utils.waitNoThrow(this, 30)
			redraw()
		}
	}

	protected open fun getDisplayData(lines: MutableList<String>) {
		var path = boardFile!!.name
		val parent = boardFile!!.parentFile
		if (parent != null) {
			path = parent.name + "/" + path
		}
		lines.add(path)
		lines.add(pickMode.name + ":" + (if (multiSelect) "MULTI:" else "") + tool.toString())
		lines.add(String.format("V:%d E:%d C:%d", board.numVerts, board.numEdges, board.numCells))
	}

	var selectedIndex: Int
		get() = if (selected.size > 0) selected[0] else -1
		set(idx) {
			if (multiSelect) {
				if (selected.contains(idx)) {
					selected.remove(idx as Any)
				} else if (idx >= 0) {
					selected.add(idx)
				}
			} else {
				selected.clear()
				if (idx >= 0) selected.add(idx)
			}
			frame.setProperty("selected", selected)
		}

	fun clearSelected() {
		selected.clear()
		frame.setProperty("selected", selected)
	}

	protected fun drawVertexMode(g: APGraphics, mouse: Vector2D) {
		g.color = GColor.RED
		g.begin()
		for (idx in selected) {
			g.vertex(board.getVertex(idx))
		}
		g.drawPoints(8f)
		highlightedIndex = board.pickVertex(g, mouse)
		if (highlightedIndex >= 0) {
			val v: BVertex = board.getVertex(highlightedIndex)
			g.begin()
			g.vertex(v)
			g.drawPoints(10f)
			// draw lines from vertex to its adjacent cells
			g.begin()
			for (c in board.getAdjacentCells(v)) {
				g.vertex(v)
				g.vertex(c)
			}
			g.color = GColor.MAGENTA
			g.drawLines()

			// draw lines to adjacent verts
			g.begin()
			for (vIdx in v.adjVerts) {
				val vv: BVertex = board.getVertex(vIdx)
				g.vertex(v)
				g.vertex(vv)
			}
			g.color = GColor.CYAN
			g.drawLines()
		}
		g.color = GColor.BLACK
		if (showNumbers) board.drawVertsNumbered(g)
	}

	protected fun drawEdgeMode(g: APGraphics, mouseX: Int, mouseY: Int) {
		if (selectedIndex >= 0) {
			g.color = GColor.RED
			g.begin()
			val e: BEdge = board.getEdge(selectedIndex)
			board.renderEdge(e, g)
			g.drawLines()
		}
		highlightedIndex = board.pickEdge(g, mouseX, mouseY)
		if (highlightedIndex >= 0) {
			g.color = GColor.MAGENTA
			g.begin()
			val e: BEdge = board.getEdge(highlightedIndex)
			board.renderEdge(e, g)
			val mp = board.getMidpoint(e)
			// draw the lines from midpt of edge to the cntr of its adjacent cells
			for (_c in board.getAdjacentCells(e)) {
				val c = _c as BCell
				g.vertex(mp)
				g.vertex(c)
			}
			g.drawLines()
		}
		g.color = GColor.BLACK
		if (showNumbers) board.drawEdgesNumbered(g)
	}

	protected open fun drawCellMode(g: APGraphics, mouseX: Int, mouseY: Int) {
		for (idx in selected) {
			g.color = GColor.RED
			g.begin()
			val c: BCell = board.getCell(idx)
			board.renderCell(c, g, 0.9f)
			g.setLineWidth(4f)
			g.drawLineLoop()
			drawExtraCellInfo(g, c as C)
		}
		highlightedIndex = board.pickCell(g, mouse)
		if (highlightedIndex >= 0) {
			g.color = GColor.MAGENTA
			g.begin()
			val cell: BCell = board.getCell(highlightedIndex)
			g.setLineWidth(2f)
			g.color = GColor.RED
			board.drawCellArrowed(cell, g)
			g.drawCircle(cell.x, cell.y, cell.radius)
			g.begin()
			g.vertex(cell)
			g.drawPoints(3f)
			g.begin()
			g.drawRect(board.getCellBoundingRect(highlightedIndex))
			g.begin()
			for (idx in cell.adjCells) {
				val cell2: BCell = board.getCell(idx)
				g.vertex(cell)
				g.vertex(cell2)
			}
			g.color = GColor.CYAN
			g.drawLines()
		}
		g.color = GColor.BLACK
		if (showNumbers) board.drawCellsNumbered(g)
	}

	open fun drawExtraCellInfo(g: APGraphics, cell: C) {}
	fun setBoardFile(file: File?) {
		if (file == null) {
			boardFile = DEFAULT_FILE
			frame.getProperties().remove("boardFile")
			frame.saveProperties()
		} else {
			boardFile = file
			frame.setProperty("boardFile", boardFile!!.absolutePath)
		}
	}

	fun onModeMenu(mode: PickMode) {
		pickMode = mode
		clearSelected()
		frame.setProperty("pickMode", mode.name)
	}

	@Synchronized
	fun onActionMenu(item: String) {
		when (item) {
			"Compute" -> board.compute()
			"Undo" -> if (undoList.size > 0) {
				undoList.removeLast().invoke()
			}
			"Clear" -> {
				board.clear()
				clearSelected()
			}
			"Generate Grid" -> {
				val rows = AWTNumberPicker.Builder().setLabel("Rows").setMin(1).setMax(100)
					.setValue(frame.getIntProperty("gui.gridRows", 1)).build { _, newValue: Int ->
						frame.setProperty("gui.gridRows", newValue)
					}
				val cols = AWTNumberPicker.Builder().setLabel("Columns").setMin(1).setMax(100)
					.setValue(frame.getIntProperty("gui.gridCols", 1)).build { _, newValue: Int ->
						frame.setProperty("gui.gridCols", newValue)
					}
				val panel = AWTPanel(rows, cols)
				val popup = AWTFrame("Generate Grid")
				popup.add(panel)
				val build: AWTButton = object : AWTButton("Generate") {
					override fun onAction() {
						board.generateGrid(rows.value, cols.value, viewportWidth.toFloat(), viewportHeight.toFloat())
						popup.closePopup()
					}
				}
				val cancel: AWTButton = object : AWTButton("Cancel") {
					override fun onAction() {
						popup.closePopup()
					}
				}
				popup.add(AWTPanel(build, cancel), BorderLayout.SOUTH)
				popup.showAsPopup(frame)
			}
		}
		repaint()
	}

	protected open val boardFileExtension: String?
		protected get() = null

	@Synchronized
	fun onFileMenu(item: String) {
		when (item) {
			"New Board" -> {
				board.clear()
				clearSelected()
				setBoardFile(null)
			}
			"Load Board" -> {
				val file = frame.showFileOpenChooser("Load Board", boardFileExtension, "board")
				if (file != null) {
					try {
						with(CustomBoard<BVertex, BEdge, BCell>()) {
							loadFromFile(file)
							board.copyFrom(this)
						}
						setBoardFile(file)
					} catch (e: Exception) {
						frame.showMessageDialog("Error", """
 	Failed to load file
 	${file.absolutePath}
 	
 	${e.javaClass.simpleName}:${e.message}
 	""".trimIndent())
					}
				}
			}
			"Load Image" -> {
				val file = frame.showFileOpenChooser("Load Image", "png", null)
				if (file != null) {
					backgroundImageId = aPGraphics.loadImage(file.absolutePath)
					if (backgroundImageId < 0) {
						frame.showMessageDialog("Error", """
 	Failed to load image
 	${file.absolutePath}
 	""".trimIndent())
					} else {
						frame.getProperties().setProperty("image", file.absolutePath)
						frame.saveProperties()
					}
				}
			}
			"Save As..." -> {
				val file = frame.showFileSaveChooser("Save Board", "board", "Generic Boards", null)
				if (file != null) {
					try {
						board.saveToFile(file)
						setBoardFile(file)
					} catch (e: Exception) {
						frame.showMessageDialog("Error", """
 	Failed to Save file
 	${file.absolutePath}
 	
 	${e.javaClass.simpleName}:${e.message}
 	""".trimIndent())
					}
				}
			}
			"Save" -> {
				if (boardFile != null) {
					try {
						board.saveToFile(boardFile)
					} catch (e: Exception) {
						frame.showMessageDialog("Error", """
 	Failed to Save file
 	${boardFile!!.absolutePath}
 	
 	${e.javaClass.simpleName}:${e.message}
 	""".trimIndent())
					}
				}
			}
			"Clear Image" -> {
				backgroundImageId = -1
				frame.getProperties().remove("image")
			}
		}
		repaint()
	}

	private fun onSelectMenu(subMenu: String) {
		when (subMenu) {
			"All" -> {
				setMultiselect(true)
				when (pickMode) {
					PickMode.VERTEX -> {
						selected.clear()
						repeat(board.numVerts) {
							selected.add(it)
						}
					}
					PickMode.EDGE -> {
						selected.clear()
						repeat(board.numEdges) {
							selected.add(it)
						}
					}
					PickMode.CELL -> {
						selected.clear()
						repeat(board.numCells) {
							selected.add(it)
						}
					}
				}
				frame.setProperty("selected", selected)
			}
			"Invert" -> {
				setMultiselect(true)
				val newSel: List<Int>
				when (pickMode) {
					PickMode.VERTEX -> {
						newSel = Utils.filter(Utils.getRangeIterator(0, board.numVerts - 1), Utils.Filter { i: Int -> !selected.contains(i) })
						selected.clear()
						selected.addAll(newSel)
					}
					PickMode.EDGE -> {
						newSel = Utils.filter(Utils.getRangeIterator(0, board.numEdges - 1), Utils.Filter { i: Int -> !selected.contains(i) })
						selected.clear()
						selected.addAll(newSel)
					}
					PickMode.CELL -> {
						newSel = Utils.filter(Utils.getRangeIterator(0, board.numCells - 1), Utils.Filter { i: Int -> !selected.contains(i) })
						selected.clear()
						selected.addAll(newSel)
					}
				}
				frame.setProperty("selected", selected)
			}
		}
		redraw()
	}

	override fun onClick() {
		tool.onPick()
		repaint()
	}

	/*
    protected void pickVertexMultiSelect() {
        if (highlightedIndex >= 0) {
            if (selected.contains(highlightedIndex)) {
                selected.remove((Object)highlightedIndex);
            } else {
                selected.add(highlightedIndex);
            }
        }
    }

    protected void pickVertexSingleSelect() {

        int picked = highlightedIndex;
        if (picked < 0) {
            final int v = board.addVertex(getMousePos());
            final int s = getSelectedIndex();
            pushUndoAction(new Action() {
                @Override
                public void undo() {
                    board.removeVertex(v);
                    setSelectedIndex(s);
                }
            });
            highlightedIndex = v;
        }
        repaint();
    }

    protected void pickEdge() {
        setSelectedIndex(highlightedIndex);
        repaint();
    }

    protected void pickCellSingleSelect() {
        setSelectedIndex(highlightedIndex);
        repaint();
    }

    protected void pickCellMultiselect() {
        int idx = board.pickCell(getAPGraphics(), mouse);
        if (idx >= 0) {
            selected.add(idx);
        }
        redraw();
    }*/
	protected fun pushUndoAction(cb: () -> Unit) {
		undoList.add(cb)
		if (undoList.size > 100) undoList.removeAt(0)
	}

	override fun onDragStarted(x: Int, y: Int) {
		if (selectedIndex < 0) {
			selectedIndex = board.pickVertex(aPGraphics, mouse)
		}
		repaint()
	}

	override fun onDrag(x: Int, y: Int, dx: Int, dy: Int) {
		if (selectedIndex >= 0) {
			board.moveVertexBy(selectedIndex, getMousePos(dx, dy))
		}
		super.onDrag(x, y, dx, dy)
	}

	override fun onDragStopped() {
		selected.clear()
		repaint()
	}

	protected open fun initActions() {
		addAction(KeyEvent.VK_ESCAPE, "ESC", "Clear Selected", Runnable { selected.clear() })
		addAction(KeyEvent.VK_V, "V", "Set PickMode to VERTEX", Runnable { setPickMode(PickMode.VERTEX) })
		addAction(KeyEvent.VK_E, "E", "Set PickMode EDGE", Runnable { setPickMode(PickMode.EDGE) })
		addAction(KeyEvent.VK_C, "C", "Set PickMode CELL", Runnable { setPickMode(PickMode.CELL) })
		addAction(KeyEvent.VK_M, "M", "Set toggle MULTI-SELECT", Runnable { setMultiselect(!multiSelect) })
		addAction(KeyEvent.VK_H, "H", "Toggle Show Help", Runnable { showHelp = !showHelp })
		addAction(Utils.toIntArray(KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE), "DELETE", "Remove Selected Item", Runnable { deleteSelected() })
		addAction(KeyEvent.VK_TAB, "TAB", "Toggle Pick Modes", Runnable { setPickMode(Utils.incrementValue(pickMode, *PickMode.values())) })
		addAction(KeyEvent.VK_N, "N", "Toggle Show Numbers", Runnable { showNumbers = !showNumbers })
		addAction(Utils.toIntArray(KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS), "+", "Zoom in", Runnable { zoomIn() })
		addAction(KeyEvent.VK_MINUS, "-", "Zoom out", Runnable { zoomOut() })
		addAction(KeyEvent.VK_LEFT, "<", "Adjust Vertex Left", Runnable { moveVertex(-1f, 0f) })
		addAction(KeyEvent.VK_RIGHT, ">", "Adjust Vertex Right", Runnable { moveVertex(1f, 0f) })
		addAction(KeyEvent.VK_UP, "^", "Adjust Vertex Up", Runnable { moveVertex(0f, -1f) })
		addAction(KeyEvent.VK_DOWN, "v", "Adjust Vertex Down", Runnable { moveVertex(0f, 1f) })
	}

	fun setShowNumbers(show: Boolean) {
		showNumbers = show
	}

	fun zoomIn() {
		rect!!.scale(.5f)
		frame.setProperty("rect", rect)
	}

	fun zoomOut() {
		rect!!.scale(2f)
		rect!!.left = 0f
		rect!!.top = 0f
		rect!!.width = Math.min(rect!!.width, viewportWidth.toFloat())
		rect!!.height = Math.min(rect!!.height, viewportHeight.toFloat())
		frame.setProperty("rect", rect)
	}

	protected fun addAction(code: Int, key: String, description: String, action: Runnable) {
		actions[code] = KeyAction(key, description, action)
	}

	protected fun addAction(codes: IntArray, key: String, description: String, action: Runnable) {
		val a = KeyAction(key, description, action)
		for (code in codes) {
			actions[code] = a
		}
	}

	private fun deleteSelected() {
		Collections.sort(selected)
		Collections.reverse(selected)
		when (pickMode) {
			PickMode.CELL -> for (idx in selected) board.removeCell(idx)
			PickMode.EDGE -> {
				if (selectedIndex >= 0) {
					val e = board.getEdge(selectedIndex)
					board.removeEdge(selectedIndex)
					pushUndoAction() { board.addEdge(e) }
				}
			}
			PickMode.VERTEX -> for (idx in selected) board.removeVertex(idx)
		}
		selected.clear()
	}

	open fun onKeyTyped(evt: KeyEvent) {
	}

	protected fun moveVertex(dx: Float, dy: Float) {
		if (pickMode == PickMode.VERTEX && selectedIndex >= 0) {
			board.moveVertexBy(selectedIndex, Vector2D(dx, dy))
		}
	}

	@Synchronized
	override fun onKeyPressed(evt: KeyEvent) {
		if (tool.onKeyTyped(evt.keyCode)) {
			evt.consume()
		} else if (actions.containsKey(evt.keyCode)) {
			actions[evt.keyCode]!!.action.run()
			evt.consume()
		} else if (actions.containsKey(evt.extendedKeyCode)) {
			actions[evt.extendedKeyCode]!!.action.run()
			evt.consume()
		} else {
			System.err.println("Unhandled key type:" + evt.keyChar)
		}
		repaint()
	}

	fun setPickMode(mode: PickMode) {
		pickMode = mode
		frame.setProperty("pickMode", pickMode.name)
		selected.clear()
	}

	fun setMultiselect(m: Boolean) {
		multiSelect = m
		frame.setProperty("multiSelect", multiSelect)
		selected.clear()
	}

	/**
	 * to use, override the initFrame method and make calls to this method from there
	 *
	 * @param tool
	 */
	fun registerTool(tool: Tool) {
		if (tools.firstOrNull { it.name == tool.name } != null) throw GException("A tool with name " + tool.name + " already exists")
		tools.add(tool)
	}

	protected open fun onCellAdded(cell: C) {}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			object : AWTBoardBuilder<BVertex, BEdge, BCell, CustomBoard<BVertex, BEdge, BCell>>() {
				override fun newBoard(): CustomBoard<BVertex, BEdge, BCell> {
					return CustomBoard<BVertex, BEdge, BCell>()
				}

				override val propertiesFileName: String
					protected get() = "bb.properties"
				override val defaultBoardFileName: String
					protected get() = "bb.backup.board"
			}
		}
	}
}