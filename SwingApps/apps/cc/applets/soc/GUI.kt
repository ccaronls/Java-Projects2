package cc.applets.soc

import cc.game.soc.core.*
import cc.game.soc.ui.*
import cc.game.soc.ui.MenuItem
import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.MutableVector2D
import cc.lib.swing.*
import cc.lib.utils.FileUtils
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.*
import java.lang.reflect.Field
import java.util.*
import javax.swing.*
import javax.swing.filechooser.FileFilter

class GUI(private val frame: AWTFrame, val props: UIProperties) : ActionListener, MenuItem.Action {
	val log = LoggerFactory.getLogger(GUI::class.java)
	val QUIT = MenuItem("Quit", "Quit current game", this)
	val BACK = MenuItem("Back", "", this)
	val EXIT = MenuItem("Exit", "Exit the Application", this)
	val NEW_GAME = MenuItem("New Game", "Start a new game", this)
	val RESTORE = MenuItem("Restore", "Restore previously saved game", this)
	val CONFIG_BOARD = MenuItem("Configure Board", "Open configure board mode", this)
	val CONFIG_SETTINGS = MenuItem("Config Settings", "Configure game settings", this)
	val CHOOSE_NUM_PLAYERS = MenuItem("--", "", this)
	val CHOOSE_COLOR = MenuItem("--", "", this)
	val START = MenuItem("Start", "Start the game", this)
	val RESTART = MenuItem("Restart", "Start the game", this)
	val START_MULTIPLAYER = MenuItem("Start MP", "Start game and wait for players to join", this)
	val GEN_HEX_BOARD = MenuItem("New Hexagon", "Generate a hexagon shaped board", this)
	val GEN_HEX_BOARD_SMALL = MenuItem("Small", "Generate a small hexagon shaped board", this)
	val GEN_HEX_BOARD_MEDIUM = MenuItem("Medium", "Generate a medium hexagon shaped board", this)
	val GEN_HEX_BOARD_LARGE = MenuItem("Large", "Generate a large hexagon shaped board", this)
	val GEN_RECT_BOARD = MenuItem("New Rectangle", "Generate a rectangular shaped board", this)
	val GEN_RECT_BOARD_SMALL = MenuItem("Small", "Generate a small rectangular shaped board", this)
	val GEN_RECT_BOARD_MEDIUM = MenuItem("Medium", "Generate a medium rectangular shaped board", this)
	val GEN_RECT_BOARD_LARGE = MenuItem("Large", "Generate a large shaped board", this)
	val TRIM_BOARD = MenuItem("Trim Board", "Remove empty tiles", this)
	val ASSIGN_RANDOM = MenuItem("Assign Random", "Assign values to the random tiles", this)
	val SAVE_BOARD_AS_DEFAULT = MenuItem("Save as Default", "Save current board as default board", this)
	val LOAD_DEFAULT = MenuItem("Load Default", "Load the default board", this)
	val SAVE_BOARD = MenuItem("Save", "Overwrite board changes", this)
	val SAVE_BOARD_AS = MenuItem("Save as New", "Save as a new board", this)
	val LOAD_BOARD = MenuItem("Load Board", "Load a board", this)
	val SAVE_SCENARIO = MenuItem("Save Scenario", "Save the current board and game configuration as a scenario", this)
	val LOAD_SCENARIO = MenuItem("Load Scenario", "Load a current scenario board and game configuration", this)

	// Debugging
	val COMPUTE_DISTANCES = MenuItem("Compute Distances", "Dump distance info to the console", this)
	val LOAD_DEBUG = MenuItem("Load Debug", "Load the Debugging Board", this)
	val SAVE_DEBUG = MenuItem("Save Debug", "Save existing board as Debug", this)
	val AITUNING_NEXT_OPTIMAL_INDEX = MenuItem("Next Path", "Select the next path for the optimal path", this)
	val AITUNING_PREV_OPTIMAL_INDEX = MenuItem("Prev Path", "Select the previous path for the optimal path", this)
	val AITUNING_ACCEPT_OPTIMAL = MenuItem("Accept", "Accept the current path", this)
	val AITUNING_REFRESH = MenuItem("Refresh", "Refresh aiTuning properties from test area", this)
	val DEBUG_BOARD = MenuItem("Debug Board", "Open board debugging screen", this)
	val RESET_BOARD = MenuItem("Reset Board", "Clear current board of structures and routes", this)
	val RESET_BOARD_ISLANDS = MenuItem("Reset Islands", "Remove island ", this)
	val REWIND_GAME = MenuItem("Rewind Game", "Rewind the game to previous state", this)
	val SHOW_RULES = MenuItem("Rules", "Display the Rules", this)
	val BUILDABLES_POPUP = MenuItem("Buildables", "Show the buildables popup", this)

	enum class MenuState {
		MENU_START,
		MENU_START_MP,
		MENU_CHOOSE_NUM_PLAYERS,
		MENU_CHOOSE_COLOR,
		MENU_GAME_SETUP,
		MENU_PLAY_GAME,
		MENU_CONFIG_BOARD,
		MENU_CHOOSE_DEFAULT_BOARD_SIZE,
		MENU_CHOOSE_CUSTOM_BOARD_SIZE,
		MENU_DEBUGGING,
		MENU_REWIND
	}

	private val soc: UISOC
	private val menu = JPanel()
	private val consoleComponent: SOCComponent = object : SOCComponent() {
		override fun init(g: AWTGraphics) {
			setMouseEnabled(true)
		}

		public override fun onMouseWheel(rotation: Int) {
			console.scroll(rotation)
		}
	}
	private val console: UIConsoleRenderer = UIConsoleRenderer(consoleComponent)
	private val boardComp: SOCComponent = object : SOCComponent() {
		override val imagesToLoad: Array<Array<Any?>>
			protected get() = arrayOf(
				arrayOf("desert.GIF", GColor.WHITE),
				arrayOf("water.GIF", GColor.WHITE),
				arrayOf("gold.GIF", null),
				arrayOf("undiscoveredtile.GIF", null),
				arrayOf("foresthex.GIF", null),
				arrayOf("hillshex.GIF", null),
				arrayOf("mountainshex.GIF", null),
				arrayOf("pastureshex.GIF", null),
				arrayOf("fieldshex.GIF", null),
				arrayOf("knight_basic_inactive.GIF", null),
				arrayOf("knight_basic_active.GIF", null),
				arrayOf("knight_strong_inactive.GIF", null),
				arrayOf("knight_strong_active.GIF", null),
				arrayOf("knight_mighty_inactive.GIF", null),
				arrayOf("knight_mighty_active.GIF", null),
				arrayOf("cardFrame.GIF", GColor.WHITE)
			)

		override fun init(g: AWTGraphics) {
			super.init(g)
			setMouseEnabled(true)
			setMinimumSize(256, 256)
			boardRenderer.renderFlag = props.getIntProperty("renderFlag", 0)
		}

		override fun onImagesLoaded(ids: IntArray) {
			boardRenderer.initImages(ids[0], ids[1], ids[2], ids[3],
				ids[4], ids[5], ids[6], ids[7], ids[8], ids[9],
				ids[10], ids[11], ids[12], ids[13], ids[14])
		}

		override fun onMouseWheel(rotation: Int) {
			boardRenderer.pickHandler?.let {
				if (it is MyCustomPickHandler)
					it.onMouseWheel(rotation)
			}
		}
	}
	private val barbarianComp: SOCComponent = object : SOCComponent() {
		override val imagesToLoad: Array<Array<Any?>>
			protected get() = arrayOf(arrayOf("barbarians_tile.GIF", null), arrayOf("barbarians_piece.GIF", null))

		override fun onImagesLoaded(ids: IntArray) {
			barbarianRenderer.initAssets(ids[0], ids[1])
		}
	}

	internal inner class DiceComponent : SOCComponent() {
		override fun init(g: AWTGraphics) {
			setMinimumSize(30, 30)
			setPreferredSize(60, 30)
			object : Thread() {
				override fun run() {
					val ids = IntArray(4)
					ids[0] = g.loadImage("dicesideship2.GIF")
					progress = 0.25f
					val cityId = g.loadImage("dicesidecity2.GIF")
					ids[1] = g.newTransformedImage(cityId, AWTImageColorFilter(GColor.WHITE, GColor.RED))
					progress = 0.5f
					ids[2] = g.newTransformedImage(cityId, AWTImageColorFilter(GColor.WHITE, GColor.GREEN))
					progress = 0.75f
					ids[3] = g.newTransformedImage(cityId, AWTImageColorFilter(GColor.WHITE, GColor.BLUE))
					g.deleteImage(cityId)
					diceRenderers.initImages(ids[0], ids[1], ids[2], ids[3])
					progress = 1f
				}
			}.start()
		}
	}

	private val diceComponent = DiceComponent()
	private val playerComponents = arrayOf(
		SOCComponent(),
		SOCComponent(),
		SOCComponent(),
		SOCComponent(),
		SOCComponent(),
		SOCComponent(),
		SOCComponent(),
		SOCComponent()
	)
	private val eventCardComp = DiceComponent()
	private val barbarianRenderer = UIBarbarianRenderer(barbarianComp)
	private val playerRenderers = arrayOf(
		UIPlayerRenderer(playerComponents[0]),
		UIPlayerRenderer(playerComponents[1]),
		UIPlayerRenderer(playerComponents[2]),
		UIPlayerRenderer(playerComponents[3]),
		UIPlayerRenderer(playerComponents[4]),
		UIPlayerRenderer(playerComponents[5]),
		UIPlayerRenderer(playerComponents[6]),
		UIPlayerRenderer(playerComponents[7]))
	private val boardRenderer = UIBoardRenderer(boardComp)
	private val diceRenderers = UIDiceRenderer(diceComponent, true)
	private val eventCardRenderer = UIEventCardRenderer(eventCardComp)
	private val menuStack = Stack<MenuState>()
	private var localPlayer: UIPlayerUser? = null
	private var popup: JFrame? = null
	private val westBorderPanel = JPanel()
	private val cntrBorderPanel = JPanel()
	private val eastGridPanel = JPanel()
	private val westGridPanel = JPanel()
	private val playerChooser: JSpinner
	private val middleLeftPanel = AWTPanelStack()
	private val helpText = AWTWrapLabel()
	private val aiTuning = Properties()

	internal class ColorString(val color: GColor, val name: String)

	private val playerColors: Array<ColorString>
	private var defaultBoardFile: File
	private lateinit var saveGameFile: File
	private lateinit var saveRulesFile: File
	private lateinit var debugBoard: File
	private val boardNameLabel = JLabel("Untitled")
	val board: Board
		get() = soc.board
	val rules: Rules
		get() = soc.rules

	private fun clearMenu() {
		menu.removeAll()
	}

	private fun setupDimensions(w: Int, h: Int) {
		val boardW = w * 2 / 3
		boardComp.setPreferredSize(boardW, h * 3 / 4)
		val sideDim = Dimension(w / 6, h)
		eastGridPanel.preferredSize = sideDim
		westGridPanel.preferredSize = sideDim
		consoleComponent.setPreferredSize(boardW, h / 5)
	}

	internal enum class LayoutType {
		LAYOUT_DEFAULT,  // just buttons on left the board on the right
		LAYOUT_INGAME,  // buttons on lower left, playerinfo on upper left, board upper center, console lower center
		LAYOUT_CONFIGURE
		// 2 menus of buttons on left
	}

	private var currentLayoutType: LayoutType? = null
	private fun initLayout(type: LayoutType) {
		if (currentLayoutType != null && currentLayoutType == type) return  // nothing to do
		currentLayoutType = type
		console?.clear()
		when (type) {
			LayoutType.LAYOUT_DEFAULT -> {
				eastGridPanel.removeAll()
				westGridPanel.removeAll()
				val buttons = JPanel()
				buttons.add(menu)
				westGridPanel.add(JSeparator())
				westGridPanel.add(buttons)
			}
			LayoutType.LAYOUT_INGAME -> {
				eastGridPanel.removeAll()
				westGridPanel.removeAll()

				// NEW WAY
				// basically, the user is always on the left and the remaining players are always on the right
				var userPlayerIndex = -1
				run {
					var i = 0
					while (i < soc.numPlayers) {
						val p = soc.getPlayerByPlayerNum(i + 1) as UIPlayer
						playerRenderers[i].setPlayer(i + 1)
						if (p is UIPlayerUser) {
							userPlayerIndex = i
						}
						i++
					}
				}
				playerComponents[userPlayerIndex].setMouseEnabled(false)
				if (soc.rules.isEnableCitiesAndKnightsExpansion) {
					westGridPanel.add(barbarianComp)
					playerComponents[userPlayerIndex].setMouseEnabled(true)
				}
				westGridPanel.add(playerComponents[userPlayerIndex])
				var i = 0
				while (i < soc.numPlayers) {
					if (i == userPlayerIndex) {
						i++
						continue
					}
					eastGridPanel.add(playerComponents[i])
					i++
				}
				middleLeftPanel.removeAll()
				val diceHelpPanel = middleLeftPanel.push()
				diceHelpPanel.layout = BorderLayout()
				diceHelpPanel.add(helpText, BorderLayout.SOUTH)
				if (rules.isEnableEventCards) {
					diceHelpPanel.add(eventCardComp, BorderLayout.CENTER)
				} else {
					diceHelpPanel.add(diceComponent, BorderLayout.CENTER)
				}
				westGridPanel.add(middleLeftPanel)
				val menuPanel = JScrollPane()
				menuPanel.layout = ScrollPaneLayout()
				menuPanel.viewport.add(menu)
				westGridPanel.add(menuPanel)
				if (!soc.isRunning) {
					menuStack.push(MenuState.MENU_GAME_SETUP)
				}
			}
			LayoutType.LAYOUT_CONFIGURE -> initConfigBoardLayout()
		}
		frame.validate()
		frame.repaint()
	}

	private fun initConfigBoardLayout() {
		eastGridPanel.removeAll()
		westGridPanel.removeAll()
		val buttons = JPanel()
		buttons.add(menu)
		val chooser = JPanel()
		chooser.layout = GridLayout(0, 1)
		val grp: AWTRadioButtonGroup<Any> = object : AWTRadioButtonGroup<Any>(chooser) {
			override fun onChange(extra: Any) {
				if (extra is TileType) {
					boardRenderer.pickHandler = object : PickHandler {
						override val pickMode: PickMode = PickMode.PM_TILE

						override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
							val t = bc.board.getTile(pickedValue)
							t.type = extra
							if (t.resource == null) {
								t.dieNum = 0
							}
						}

						override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
							g.color = GColor.YELLOW
							bc.drawTileOutline(g, board.getTile(highlightedIndex), 2f)
						}

						override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
							g.color = GColor.YELLOW
							bc.drawTileOutline(g, board.getTile(index), 2f)
						}

						override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {}
						override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
							return true
						}
					}
				} else if (extra is PickHandler) {
					boardRenderer.pickHandler = extra
				}
			}
		}
		for (c in TileType.values()) {
			grp.addButton(formatString(c.name), c)
		}
		grp.addButton("Islands", object : PickHandler {
			override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
				var islandNum = board.getTile(pickedValue).islandNum
				if (board.getTile(pickedValue).islandNum > 0) {
					board.removeIsland(islandNum)
				} else {
					islandNum = board.createIsland(pickedValue)
					console.addText(GColor.BLACK, "Found island: $islandNum")
				}
			}

			override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				bc.drawIslandOutlined(g, highlightedIndex)
			}

			override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {}
			override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {}
			override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
				return !bc.board.getTile(index).isWater
			}

			override val pickMode = PickMode.PM_TILE
		})
		grp.addButton("Pirate Route", object : PickHandler {
			var indices = computePirateRouteTiles()
			private fun computePirateRouteTiles(): List<Int> {
				val tIndex = board.pirateRouteStartTile
				if (tIndex < 0) {
					return board.getTilesOfType(TileType.WATER)
				}
				val start = board.getTile(tIndex)
				var tile = start
				while (tile.pirateRouteNext >= 0) {
					tile = board.getTile(tile.pirateRouteNext)
					if (tile === start) {
						// the route is in a loop, so no options
						return emptyList()
					}
				}
				val result: MutableList<Int> = ArrayList()
				for (index in board.getTilesAdjacentToTile(tile)) {
					val tt = board.getTile(index)
					if (index == board.pirateRouteStartTile || tt.isWater && tt.pirateRouteNext < 0) result.add(board.getTileIndex(tt))
				}
				return result
			}

			override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
				bc.board.addPirateRoute(pickedValue)
				indices = computePirateRouteTiles()
			}

			override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				g.color = GColor.BLACK
				bc.drawTileOutline(g, board.getTile(highlightedIndex), 2f)
			}

			override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
				g.color = GColor.RED
				bc.drawTileOutline(g, board.getTile(index), 2f)
			}

			override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {
				g.color = GColor.BLACK
				var t = board.pirateRouteStartTile
				val tiles = ArrayList<IVector2D>()
				while (t >= 0) {
					val tile = board.getTile(t)
					tiles.add(tile)
					bc.drawTileOutline(g, tile, 2f)
					t = tile.pirateRouteNext
					if (t == board.pirateRouteStartTile) {
						tiles.add(board.getTile(t))
						break
					}
				}
				g.begin()
				g.vertexList(tiles)
				g.drawLineStrip(2f)
			}

			override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
				return indices.contains(index)
			}

			override val pickMode = PickMode.PM_TILE
		})
		grp.addButton("Close routes", object : PickHandler {
			override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
				val r = board.getRoute(pickedValue)
				if (r.isClosed) {
					r.isClosed = false
				} else {
					r.isClosed = true
				}
			}

			override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val rt = board.getRoute(highlightedIndex)
				if (rt.isClosed) g.color = GColor.BLACK else g.color = GColor.WHITE
				bc.drawRoad(g, rt, true)
			}

			override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
				val rt = board.getRoute(index)
				if (rt.isClosed) g.color = GColor.BLACK.withAlpha(120) else g.color = GColor.WHITE.withAlpha(120)
				bc.drawRoad(g, rt, false)
			}

			override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {
				// TODO Auto-generated method stub
			}

			override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
				return true
			}

			override val pickMode = PickMode.PM_EDGE
		})
		grp.addButton("Pirate Fortress", object : PickHandler {
			var indices: MutableList<Int> = ArrayList()
			override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
				val v = bc.board.getVertex(pickedValue)
				if (v.type == VertexType.OPEN) {
					v.setPirateFortress()
				} else {
					v.setOpen()
				}
			}

			override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val v = board.getVertex(highlightedIndex)
				g.color = GColor.BLACK
				bc.drawSettlement(g, v, 0, true)
			}

			override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
				val v = board.getVertex(index)
				if (v.type == VertexType.PIRATE_FORTRESS) {
					g.color = GColor.BLACK
				} else {
					g.color = GColor.BLACK.withAlpha(120)
				}
				bc.drawSettlement(g, v, 0, true)
			}

			override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {}
			override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
				return indices.contains(index)
			}

			override val pickMode: PickMode
				get() {
					indices.clear()
					for (i in 0 until board.numAvailableVerts) {
						val v = board.getVertex(i)
						if (v.canPlaceStructure() && v.type == VertexType.OPEN) {
							indices.add(i)
						}
					}
					return PickMode.PM_VERTEX
				}
		})
		grp.addButton("Settlements", object : PickHandler {

			override val pickMode = PickMode.PM_VERTEX

			override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
				val v = bc.board.getVertex(pickedValue)
				if (v.type == VertexType.OPEN) {
					v.setOpenSettlement()
				} else {
					v.setOpen()
				}
			}

			override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
				val v = bc.board.getVertex(index)
				g.color = GColor.TRANSLUSCENT_BLACK
				bc.drawSettlement(g, v, 0, false)
			}

			override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {
				var index = 1
				for (vIndex in bc.board.getVertIndicesOfType(0, VertexType.OPEN_SETTLEMENT)) {
					val v = bc.board.getVertex(vIndex)
					g.color = GColor.LIGHT_GRAY
					bc.drawSettlement(g, v, 0, false)
					val mv = g.transform(v)
					g.color = GColor.YELLOW
					val text = index++.toString()
					g.drawJustifiedString(mv.X(), mv.Y(), Justify.CENTER, Justify.CENTER, text)
				}
			}

			override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
				val v = bc.board.getVertex(highlightedIndex)
				bc.drawSettlement(g, v, 0, true)
			}

			override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
				val v = bc.board.getVertex(index)
				return if (v.type != VertexType.OPEN && v.type != VertexType.SETTLEMENT) false else v.player == 0 && v.canPlaceStructure()
				// TODO Auto-generated method stub
			}
		})
		eastGridPanel.add(chooser)
		westGridPanel.add(buttons)
	}

	private fun formatString(str: String): String {
		var str = str
		var formatted = ""
		str = str.toLowerCase()
		val split = str.split("_".toRegex()).toTypedArray()
		for (i in split.indices) {
			val s = split[i]
			val delim = if (i > 0 && i % 2 == 0) "\n" else " "
			formatted += Character.toUpperCase(s[0]).toString() + s.substring(1) + delim
		}
		return formatted.trim { it <= ' ' }
	}

	internal enum class DebugPick(val mode: PickMode, val vType: VertexType?, val rType: RouteType?, val tType: TileType?) {
		OPEN_SETTLEMENT(PickMode.PM_VERTEX, VertexType.OPEN_SETTLEMENT, null, null),
		SETTLEMENT(PickMode.PM_VERTEX, VertexType.SETTLEMENT, null, null),
		CITY(PickMode.PM_VERTEX, VertexType.CITY, null, null),
		CITY_WALL(PickMode.PM_VERTEX, VertexType.WALLED_CITY, null, null),
		KNIGHT(PickMode.PM_VERTEX, VertexType.BASIC_KNIGHT_ACTIVE, null, null),
		ROAD(PickMode.PM_EDGE, null, RouteType.ROAD, null),
		SHIP(PickMode.PM_EDGE, null, RouteType.SHIP, null),
		WARSHIP(PickMode.PM_EDGE, null, RouteType.WARSHIP, null),
		MERCHANT(PickMode.PM_TILE, null, null, null),
		ROBBER(PickMode.PM_TILE, null, null, null),
		PIRATE(PickMode.PM_TILE, null, null, null),
		FORTRESS(PickMode.PM_VERTEX, VertexType.PIRATE_FORTRESS, null, null),
		PATH(PickMode.PM_VERTEX, VertexType.OPEN, null, null);
	}

	private fun addMenuItem(op: Component) {
		//menu.add(Box.createHorizontalGlue());
		menu.add(op)
		//menu.add(Box.createHorizontalGlue());
	}

	private fun initMenu() {
		if (board.name.isEmpty()) {
			boardNameLabel.text = "Untitled"
		} else {
			boardNameLabel.text = board.name
		}
		log.debug("MenuStack: $menuStack")
		clearMenu()
		if (menuStack.size > 0) {
			when (menuStack.peek()) {
				MenuState.MENU_START -> {
					initLayout(LayoutType.LAYOUT_DEFAULT)
					addMenuItem(getMenuOpButton(NEW_GAME))
					addMenuItem(getMenuOpButton(RESTORE))
					addMenuItem(getMenuOpButton(CONFIG_BOARD))
					addMenuItem(getMenuOpButton(CONFIG_SETTINGS))
					addMenuItem(getMenuOpButton(DEBUG_BOARD))
					addMenuItem(getMenuOpButton(SAVE_SCENARIO))
					addMenuItem(getMenuOpButton(LOAD_SCENARIO))
					addMenuItem(getMenuOpButton(EXIT))
				}
				MenuState.MENU_CHOOSE_NUM_PLAYERS -> {
					initLayout(LayoutType.LAYOUT_DEFAULT)
					var i = rules.minPlayers
					while (i <= rules.maxPlayers) {

//    			for (int i=0; i<playerColors.length; i++) {
						addMenuItem(getMenuOpButton(CHOOSE_NUM_PLAYERS, i.toString(), null, i))
						i++
					}
					addMenuItem(getMenuOpButton(QUIT, "Back", "Go back to previous menu"))
				}
				MenuState.MENU_CHOOSE_COLOR -> {
					initLayout(LayoutType.LAYOUT_DEFAULT)
					for (cs in playerColors) {
						addMenuItem(getMenuOpButton(CHOOSE_COLOR, cs.name, null, cs.color))
					}
					addMenuItem(getMenuOpButton(BACK))
				}
				MenuState.MENU_GAME_SETUP -> {
					initLayout(LayoutType.LAYOUT_INGAME)
					addMenuItem(getMenuOpButton(START))
					addMenuItem(getMenuOpButton(START_MULTIPLAYER))
					addMenuItem(getMenuOpButton(CONFIG_BOARD))
					addMenuItem(getMenuOpButton(CONFIG_SETTINGS))
					addMenuItem(getMenuOpButton(BACK))
				}
				MenuState.MENU_REWIND -> {
					addMenuItem(getMenuOpButton(RESTART))
					addMenuItem(getMenuOpButton(REWIND_GAME))
				}
				MenuState.MENU_PLAY_GAME -> initLayout(LayoutType.LAYOUT_INGAME)
				MenuState.MENU_DEBUGGING -> {
					buildDebugLayout()
				}
				MenuState.MENU_CONFIG_BOARD -> {
					initLayout(LayoutType.LAYOUT_CONFIGURE)
					addMenuItem(getMenuOpButton(LOAD_DEFAULT))
					addMenuItem(getMenuOpButton(LOAD_BOARD))
					addMenuItem(getMenuOpButton(GEN_HEX_BOARD))
					addMenuItem(getMenuOpButton(GEN_RECT_BOARD))
					addMenuItem(getMenuOpButton(TRIM_BOARD))
					addMenuItem(getMenuOpButton(ASSIGN_RANDOM))
					addMenuItem(getMenuOpButton(SAVE_BOARD_AS_DEFAULT))
					if (board.name != null) {
						if (File(board.name).isFile) addMenuItem(getMenuOpButton(SAVE_BOARD))
					}
					addMenuItem(getMenuOpButton(SAVE_BOARD_AS))
					addMenuItem(getMenuOpButton(BACK))
				}
				MenuState.MENU_CHOOSE_DEFAULT_BOARD_SIZE -> {
					initLayout(LayoutType.LAYOUT_CONFIGURE)
					addMenuItem(getMenuOpButton(GEN_HEX_BOARD_SMALL))
					addMenuItem(getMenuOpButton(GEN_HEX_BOARD_MEDIUM))
					addMenuItem(getMenuOpButton(GEN_HEX_BOARD_LARGE))
					addMenuItem(getMenuOpButton(BACK))
				}
				MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE -> {
					initLayout(LayoutType.LAYOUT_CONFIGURE)
					addMenuItem(getMenuOpButton(GEN_RECT_BOARD_SMALL))
					addMenuItem(getMenuOpButton(GEN_RECT_BOARD_MEDIUM))
					addMenuItem(getMenuOpButton(GEN_RECT_BOARD_LARGE))
					addMenuItem(getMenuOpButton(BACK))
				}
				else                                     -> log.error("Unhandled case : " + menuStack.peek())
			}
		}
		frame.validate()
		frame.repaint()
	}

	private fun buildDebugLayout() {
		initLayout(LayoutType.LAYOUT_DEFAULT)
		for (f in RenderFlag.values()) {
			addMenuItem(
				object : MyToggleButton(f.name, boardRenderer.getRenderFlag(f)) {
					override fun onChecked() {
						boardRenderer.setRenderFlag(f, true)
					}

					override fun onUnchecked() {
						boardRenderer.setRenderFlag(f, false)
					}
				}
			)
		}
		val choiceButtons = JPanel()
		choiceButtons.layout = BoxLayout(choiceButtons, BoxLayout.Y_AXIS)
		val pickChoice: AWTRadioButtonGroup<DebugPick> = object : AWTRadioButtonGroup<DebugPick>(choiceButtons) {
			protected override fun onChange(mode: DebugPick) {
				boardRenderer.pickHandler = object : PickHandler {
					var vertex0 = -1
					var vertex1 = -1
					var d: IDistances? = null
					override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
						val v: Vertex
						val r: Route
						when (mode) {
							DebugPick.CITY, DebugPick.CITY_WALL, DebugPick.SETTLEMENT -> {
								v = board.getVertex(pickedValue)
								if (v.player == 0) {
									v.setPlayerAndType(curPlayerNum, mode.vType!!)
								} else {
									v.setOpen()
								}
							}
							DebugPick.OPEN_SETTLEMENT -> {
								v = board.getVertex(pickedValue)
								if (v.type == VertexType.OPEN_SETTLEMENT) {
									v.setOpen()
								} else {
									v.setOpenSettlement()
								}
							}
							DebugPick.KNIGHT -> {
								v = board.getVertex(pickedValue)
								when (v.type) {
									VertexType.BASIC_KNIGHT_INACTIVE  -> v.setPlayerAndType(curPlayerNum, VertexType.BASIC_KNIGHT_ACTIVE)
									VertexType.BASIC_KNIGHT_ACTIVE    -> v.setPlayerAndType(curPlayerNum, VertexType.STRONG_KNIGHT_INACTIVE)
									VertexType.STRONG_KNIGHT_INACTIVE -> v.setPlayerAndType(curPlayerNum, VertexType.STRONG_KNIGHT_ACTIVE)
									VertexType.STRONG_KNIGHT_ACTIVE   -> v.setPlayerAndType(curPlayerNum, VertexType.MIGHTY_KNIGHT_INACTIVE)
									VertexType.MIGHTY_KNIGHT_INACTIVE -> v.setPlayerAndType(curPlayerNum, VertexType.MIGHTY_KNIGHT_ACTIVE)
									VertexType.MIGHTY_KNIGHT_ACTIVE   -> v.setOpen()
									else                              -> v.setPlayerAndType(curPlayerNum, VertexType.BASIC_KNIGHT_INACTIVE)
								}
							}
							DebugPick.FORTRESS -> {
								v = board.getVertex(pickedValue)
								if (v.type == VertexType.PIRATE_FORTRESS) {
									v.pirateHealth = v.pirateHealth - 1
									if (v.pirateHealth <= 0) v.setOpen()
								} else {
									v.setOpen()
									v.setPirateFortress()
									v.pirateHealth = 3
								}
							}
							DebugPick.ROAD, DebugPick.SHIP, DebugPick.WARSHIP -> {
								r = board.getRoute(pickedValue)
								if (r.player == 0) {
									r.type = mode.rType!!
									board.setPlayerForRoute(r, curPlayerNum, mode.rType!!)
								} else {
									board.setRouteOpen(r)
								}
							}
							DebugPick.MERCHANT -> if (board.merchantPlayer == curPlayerNum && board.merchantTileIndex == pickedValue) {
								board.setMerchant(-1, 0)
							} else {
								board.setMerchant(pickedValue, curPlayerNum)
							}
							DebugPick.ROBBER -> if (board.robberTileIndex == pickedValue) board.setRobber(-1) else board.setRobber(pickedValue)
							DebugPick.PIRATE -> if (board.pirateTileIndex == pickedValue) board.setPirate(-1) else board.setPirate(pickedValue)
							DebugPick.PATH -> {
								if (pickedValue == vertex0) {
									vertex0 = -1
								} else if (pickedValue == vertex1) {
									vertex1 = -1
								} else if (vertex0 < 0) {
									vertex0 = pickedValue
								} else {
									vertex1 = pickedValue
								}
							}
						}
					}

					override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
						when (mode.mode) {
							PickMode.PM_EDGE -> {
								g.color = getPlayerColor(curPlayerNum)
								val e = board.getRoute(highlightedIndex)
								bc.drawEdge(g, e, mode.rType, e.player, true)
							}
							PickMode.PM_TILE -> {
								g.color = GColor.YELLOW
								bc.drawTileOutline(g, board.getTile(highlightedIndex), RenderConstants.thinLineThickness)
							}
							PickMode.PM_VERTEX -> {
								val v = board.getVertex(highlightedIndex)
								if (mode == DebugPick.PATH) {
									g.color = GColor.BLACK
									g.begin()
									g.vertex(v)
									g.drawPoints(10f)
								} else {
									g.color = getPlayerColor(curPlayerNum)
									bc.drawVertex(g, v, mode.vType!!, v.player, true)
								}
							}
							PickMode.PM_CUSTOM, PickMode.PM_NONE -> {
							}
						}
					}

					override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
						when (mode.mode) {
							PickMode.PM_EDGE -> {
								g.color = getPlayerColor(curPlayerNum).withAlpha(100)
								val e = board.getRoute(index)
								bc.drawEdge(g, e, e.type, e.player, true)
							}
							PickMode.PM_TILE -> {
							}
							PickMode.PM_VERTEX -> {
								g.color = getPlayerColor(curPlayerNum).withAlpha(100)
								val v = board.getVertex(index)
								bc.drawVertex(g, v, v.type, v.player, true)
							}
							PickMode.PM_CUSTOM, PickMode.PM_NONE -> {
							}
						}
					}

					override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {
						g.color = GColor.YELLOW
						g.begin()
						if (vertex0 >= 0) {
							g.vertex(board.getVertex(vertex0))
						}
						if (vertex1 >= 0) {
							g.vertex(board.getVertex(vertex1))
						}
						g.drawPoints(10f)
						if (vertex0 >= 0 && vertex1 >= 0) {
							if (d == null) {
								d = board.computeDistances(rules, curPlayerNum)
							}
							g.begin()
							val path = d!!.getShortestPath(vertex0, vertex1)
							for (i in path.indices) {
								g.vertex(board.getVertex(path[i]))
							}
							g.drawLineStrip(5f)
							val v: IVector2D = board.getVertex(path[0])
							g.drawWrapStringOnBackground(v.x, v.y, (g.viewportWidth / 2).toFloat(), "Dist from " + vertex0 + "->" + vertex1 + " = " + d!!.getDist(vertex0, vertex1), GColor.TRANSLUSCENT_BLACK, 5f)
						}
					}

					override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
						return true
					}

					override val pickMode = mode.mode
				}
			}
		}
		//choiceButtons.add(new JLabel("PICK CHOICE"));
		for (pm in DebugPick.values()) {
			pickChoice.addButton(pm.name, pm)
		}
		choiceButtons.add(AWTPanel(JLabel("Player:"), playerChooser))
		choiceButtons.add(getMenuOpButton(RESET_BOARD))
		choiceButtons.add(getMenuOpButton(RESET_BOARD_ISLANDS))
		choiceButtons.add(getMenuOpButton(COMPUTE_DISTANCES))
		choiceButtons.add(getMenuOpButton(LOAD_DEBUG))
		choiceButtons.add(getMenuOpButton(SAVE_DEBUG))
		eastGridPanel.removeAll()
		//eastGridPanel.add(new JPanel());
		eastGridPanel.add(choiceButtons)
		addMenuItem(getMenuOpButton(BACK))
	}

	override fun onAction(op: MenuItem, extra: Any?) {
		if (op == BACK) {
			if (menuStack.size > 0) {
				menuStack.pop()
				initMenu()
			} else {
				(extra as JButton).isEnabled = false
			}
			boardRenderer.pickHandler = null
		} else if (op == EXIT) {
			synchronized(soc) { System.exit(0) }
		} else if (op == DEBUG_BOARD) {
			menuStack.push(MenuState.MENU_DEBUGGING)
			initMenu()
		} else if (op == RESET_BOARD) {
			board.reset()
		} else if (op == RESET_BOARD_ISLANDS) {
			board.clearIslands()
		} else if (op == NEW_GAME) {
			val b = soc.board
			b.tryRefreshFromFile()
			menuStack.push(MenuState.MENU_GAME_SETUP)
			menuStack.push(MenuState.MENU_CHOOSE_COLOR)
			menuStack.push(MenuState.MENU_CHOOSE_NUM_PLAYERS)
			initMenu()
		} else if (op == RESTORE) {
			try {
				soc.stopRunning()
				loadGame(saveGameFile)
				menuStack.push(MenuState.MENU_PLAY_GAME)
				soc.startGameThread()
				initMenu()
			} catch (e: Exception) {
				e.printStackTrace()
				(extra as JButton).isEnabled = false
				e.printStackTrace()
			}
		} else if (op == CONFIG_BOARD) {
			menuStack.push(MenuState.MENU_CONFIG_BOARD)
			initMenu()
		} else if (op == CONFIG_SETTINGS) {
			showConfigureGameSettingsPopup(rules.deepCopy(), true)
		} else if (op == CHOOSE_NUM_PLAYERS) {
			initPlayers(extra as Int)
			menuStack.pop()
			initMenu()
		} else if (op == CHOOSE_COLOR) {
			// reload the board
			setPlayerColor(extra as GColor)
			menuStack.pop()
			initMenu()
		} else if (op == START) {
			if (board.isReady) {
				board.assignRandom()
				menuStack.clear()
				menuStack.push(MenuState.MENU_START)
				menuStack.push(MenuState.MENU_PLAY_GAME)
				initMenu()
				clearSaves()
				soc.initUI()
				soc.initGame()
				soc.startGameThread()
			} else {
				log.error("Board not ready")
			}
		} else if (op == RESTART) {
			menuStack.pop()
			initMenu()
			soc.startGameThread()
		} else if (op == START_MULTIPLAYER) {
			if (board.isReady) {
				try {
					soc.server.listen()
				} catch (e: Exception) {
					showOkPopup("ERROR", "Failed to start server. " + e.javaClass.simpleName + ":" + e.message)
					return
				}
				try {
					/*
                    jmdns = JmDNS.create(InetAddress.getLocalHost());

                    // Register a service
                    ServiceInfo serviceInfo = ServiceInfo.create(NetCommon.DNS_SERVICE_ID,
                            "Senators of Katan", NetCommon.PORT,
                            "name=" + System.getProperty("user.name") + ",numplayers=" + soc.getNumPlayers());
                    jmdns.registerService(serviceInfo);

                    // Unregister all services
                    jmdns.unregisterAllServices();

                     */
					console.addText(GColor.BLACK, "Broadcasting on Bonjour")
				} catch (e: Exception) {
					soc.server.stop()
					showOkPopup("ERROR", "Failed to register Bonjour service. " + e.javaClass.simpleName + ":" + e.message)
					return
				}
				board.assignRandom()
				menuStack.clear()
				menuStack.clear()
				menuStack.push(MenuState.MENU_START)
				menuStack.push(MenuState.MENU_PLAY_GAME)
				initMenu()
			} else {
				log.error("Board not ready")
			}
		} else if (op == GEN_HEX_BOARD) {
			menuStack.push(MenuState.MENU_CHOOSE_DEFAULT_BOARD_SIZE)
			initMenu()
		} else if (op == GEN_HEX_BOARD_SMALL) {
			board.generateHexBoard(4, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == GEN_HEX_BOARD_MEDIUM) {
			board.generateHexBoard(5, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == GEN_HEX_BOARD_LARGE) {
			board.generateHexBoard(6, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == GEN_RECT_BOARD) {
			menuStack.push(MenuState.MENU_CHOOSE_CUSTOM_BOARD_SIZE)
			initMenu()
		} else if (op == GEN_RECT_BOARD_SMALL) {
			board.generateRectBoard(6, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == GEN_RECT_BOARD_MEDIUM) {
			board.generateRectBoard(8, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == GEN_RECT_BOARD_LARGE) {
			board.generateRectBoard(10, TileType.WATER)
			menuStack.pop()
			initMenu()
		} else if (op == TRIM_BOARD) {
			board.trim()
		} else if (op == ASSIGN_RANDOM) {
			board.assignRandom()
		} else if (op == SAVE_BOARD_AS_DEFAULT) {
			saveBoard(defaultBoardFile)
		} else if (op == LOAD_DEFAULT) {
			if (loadBoard(defaultBoardFile)) {
				frame.repaint()
			}
		} else if (op == SAVE_BOARD) {
			saveBoard(File(board.name))
		} else if (op == SAVE_BOARD_AS) {
			val chooser = JFileChooser()
			var baseDir = File(props.getProperty(PROP_BOARDS_DIR, "assets/boards"))
			if (!baseDir.exists() && !baseDir.mkdirs()) {
				showOkPopup("ERROR", "Failed to ceate directory tree '$baseDir'")
			} else if (!baseDir.isDirectory) {
				showOkPopup("ERROR", "Not a directory '$baseDir'")
			} else {
				chooser.currentDirectory = baseDir
				chooser.dialogTitle = "Save Board"
				chooser.fileSelectionMode = JFileChooser.FILES_ONLY
				chooser.fileFilter = getExtensionFilter("txt", true)
				val result = chooser.showSaveDialog(frame)
				if (result == JFileChooser.APPROVE_OPTION) {
					val file = chooser.selectedFile
					baseDir = file.parentFile
					var fileName = file.absolutePath
					if (!fileName.endsWith(".txt")) fileName += ".txt"
					saveBoard(File(fileName))
				}
			}
		} else if (op == LOAD_BOARD) {
			val chooser = JFileChooser()
			chooser.fileFilter = getExtensionFilter("txt", true)
			val boardsDir = File(props.getProperty(PROP_BOARDS_DIR, "assets/boards"))
			if (!boardsDir.isDirectory) {
				showOkPopup("ERROR", "Boards directory missing")
			} else if (boardsDir.list().size == 0) {
				showOkPopup("ERROR", "No Boards in boards directory")
			} else {
				chooser.currentDirectory = boardsDir
				chooser.dialogTitle = "Load Board"
				chooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
				val result = chooser.showOpenDialog(frame)
				if (result == JFileChooser.APPROVE_OPTION) {
					val file = chooser.selectedFile
					loadBoard(file)
				}
			}
		} else if (op == REWIND_GAME) {
			soc.stopRunning()
			try {
				FileUtils.restoreFile(saveGameFile.absolutePath)
				loadGame(saveGameFile)
				//soc.startGameThread();
				clearMenu()
				if (currentMenu != MenuState.MENU_REWIND) {
					menuStack.push(MenuState.MENU_REWIND)
				}
				initMenu()
				soc.redraw()
			} catch (e: Exception) {
				(extra as JButton).isEnabled = false
			}
		} else if (op == SHOW_RULES) {
			showConfigureGameSettingsPopup(rules, false)
		} else if (op == QUIT) {
			quitToMainMenu()
		} else if (op == BUILDABLES_POPUP) {
			val columnNames = Vector<String>()
			columnNames.add("Buildable")
			for (r in ResourceType.values()) {
				columnNames.add(r.getNameId())
			}
			val rowData = Vector<Vector<Any>>()
			for (b in BuildableType.values()) {
				if (b.isAvailable(sOC)) {
					val row = Vector<Any>()
					row.add(b.name)
					for (r in ResourceType.values()) row.add(b.getCost(r).toString())
					rowData.add(row)
				}
			}
			val table = JTable(rowData, columnNames)
			table.columnModel.getColumn(0).minWidth = 100
			val view = JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
			view.viewport.add(table)
			this.showOkPopup("BUILDABLE", view)
		} else if (op == SAVE_SCENARIO) {
			var txt = FileUtils.stripExtension(File(board.name).name)
			if (txt.length == 0) txt = "MyScenario"
			val nameField = JTextField(txt)
			val panel = AWTPanel(GridLayout(0, 1), JLabel("Enter Scenario name"), nameField)
			showPopup("Save Current Board and Rules as Scenario", panel, arrayOf(
				PopupButton("Cancel"),
				object : PopupButton("Save") {
					override fun doAction(): Boolean {
						board.name = nameField.text
						val chooser = JFileChooser()
						val scenarioDir = File(props.getProperty(PROP_SCENARIOS_DIR, "assets/scenarios"))
						if (!scenarioDir.exists()) {
							if (!scenarioDir.mkdirs()) {
								showOkPopup("ERROR", "Failed to create directory '$scenarioDir'")
								return true
							}
						} else if (!scenarioDir.isDirectory) {
							showOkPopup("ERROR", "Not a directory '$scenarioDir'")
							return true
						}
						chooser.selectedFile = File(scenarioDir, nameField.text)
						chooser.currentDirectory = scenarioDir
						chooser.dialogTitle = "Save Scenario"
						chooser.fileSelectionMode = JFileChooser.FILES_ONLY
						chooser.fileFilter = getExtensionFilter("txt", true)
						val result = chooser.showSaveDialog(frame)
						if (result == JFileChooser.APPROVE_OPTION) {
							val file = chooser.selectedFile
							var fileName = file.absolutePath
							if (!fileName.endsWith(".txt")) fileName += ".txt"
							try {
								val aituning: MutableMap<String, Double> = HashMap()
								for (key in aiTuning.keys) {
									aituning[key as String] = java.lang.Double.valueOf(aiTuning.getProperty(key))
								}
								val scenario = Scenario(soc, aituning)
								scenario.saveToFile(File(fileName))
							} catch (e: Exception) {
								e.printStackTrace()
							}
						}
						return true
					}
				}
			))
		} else if (op == LOAD_SCENARIO) {
			val scenariosDir = File(props.getProperty(PROP_SCENARIOS_DIR, "assets/scenarios"))
			if (!scenariosDir.isDirectory) {
				showOkPopup("ERROR", "Cant find scenarios directory '$scenariosDir'")
			} else {
				val chooser = JFileChooser()
				chooser.currentDirectory = scenariosDir
				chooser.dialogTitle = "Load Scenario"
				chooser.fileSelectionMode = JFileChooser.FILES_ONLY
				chooser.fileFilter = getExtensionFilter("txt", true)
				val result = chooser.showOpenDialog(frame)
				if (result == JFileChooser.APPROVE_OPTION) {
					val file = chooser.selectedFile
					try {
						loadGame(file)
						props.setProperty("scenario", file.absolutePath)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		} else if (op == COMPUTE_DISTANCES) {
			val t = System.currentTimeMillis()
			val distances = board.computeDistances(rules, curPlayerNum)
			val dt = System.currentTimeMillis() - t
			val buf = StringBuffer()
			for (i in 0 until board.numAvailableVerts - 1) {
				for (ii in i + 1 until board.numAvailableVerts) {
					val dist = distances.getDist(i, ii)
					if (dist != IDistances.DISTANCE_INFINITY.toInt()) {
						buf.append(String.format("DIST %-3d -> %-3d = %d\n", i, ii, dist))
					}
				}
			}
			println("got Distances in $dt MSecs:\n$buf")
		} else if (op == LOAD_DEBUG) {
			loadBoard(debugBoard)
		} else if (op == SAVE_DEBUG) {
			saveBoard(debugBoard)
		} else if (op == AITUNING_NEXT_OPTIMAL_INDEX) {
			optimalIndex = (optimalIndex + 1) % optimalOptions!!.size
		} else if (op == AITUNING_PREV_OPTIMAL_INDEX) {
			optimalIndex = (optimalOptions!!.size + optimalIndex - 1) % optimalOptions!!.size
		} else if (op == AITUNING_ACCEPT_OPTIMAL) {
			soc.setReturnValue(optimalOptions!![optimalIndex])
		} else if (op == AITUNING_REFRESH) {
			try {
				// yikes! is there a better way to do this?
				val area = (middleLeftPanel.top().getComponent(0) as JScrollPane).viewport.view as JTextArea
				val txt = area.text
				val lines = txt.split("[\n]".toRegex()).toTypedArray()
				for (i in 1 until lines.size) {
					val line = lines[i]
					val parts = line.split("[ ]+".toRegex()).toTypedArray()
					aiTuning.setProperty(parts[0], parts[1])
				}
				val out = FileOutputStream(AI_TUNING_FILE)
				try {
					aiTuning.store(out, "Generated by SOC Swing Utility")
				} finally {
					out.close()
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		soc.notifyWaitObj()
	}

	@Throws(IOException::class)
	private fun loadGame(file: File) {
		soc.loadFromFile(file)
		soc.board.name = file.absolutePath
		initMenu()
	}

	@Synchronized
	fun closePopup() {
		popup?.let {
//			synchronized(popup!!) { popup.notify() }
			it.isVisible = false
			popup = null
		}
		frame.isEnabled = true
		frame.isVisible = true
	}

	fun quitToMainMenu() {
		soc.stopRunning()
		//soc.clear();
		boardRenderer.pickHandler = null
		console.clear()
		menuStack.clear()
		menuStack.push(MenuState.MENU_START)
		initMenu()
	}

	private fun getMenuOpButton(op: MenuItem): OpButton {
		return getMenuOpButton(op, op.title, op.helpText, null)
	}

	private fun getMenuOpButton(op: MenuItem, txt: String, helpText: String): OpButton {
		return getMenuOpButton(op, txt, helpText, null)
	}

	private fun getMenuOpButton(op: MenuItem, text: String, helpText: String?, extra: Any?): OpButton {
		val button = OpButton(op, text, extra)
		button.addActionListener(this)
		button.toolTipText = helpText
		return button
	}

	private fun initPlayers(numPlayers: Int) {
		soc.clear()
		val players = arrayOfNulls<Player>(numPlayers)
		localPlayer = UIPlayerUser()
		players[0] = localPlayer
		localPlayer!!.color = playerColors[0].color
		for (i in 1 until numPlayers) {
			val p = UIPlayer()
			p.color = playerColors[i].color
			p.playerNum = i + 1
			players[i] = p
		}

		// now shuffle the player nums
		for (i in 0 until numPlayers) {
			players[i]!!.playerNum = i + 1
			soc.addPlayer(players[i]!!)
		}
	}

	private fun setPlayerColor(color: GColor?) {
		log.debug("setPlayerColor too: $color")
		assert(color != null)
		val temp = localPlayer!!.color
		localPlayer!!.color = color
		for (i in 1..soc.numPlayers) {
			if (i == localPlayer!!.playerNum) continue
			val p = getGUIPlayer(i)
			if (p.color == color) {
				p.color = temp
				break
			}
		}
	}

	private fun saveBoard(file: File): Boolean {
		try {
			board.name = file.absolutePath
			board.saveToFile(file)
			boardNameLabel.text = file.name
			initMenu()
		} catch (e: IOException) {
			log.error(e.message)
			return false
		}
		return true
	}

	private fun loadBoard(file: File): Boolean {
		return try {
			val b = Board()
			b.loadFromFile(file)
			board.copyFrom(b)
			boardNameLabel.text = file.name
			true
		} catch (e: Exception) {
			log.error(e.message)
			false
		}
	}

	fun getGUIPlayer(playerNum: Int): UIPlayer {
		return soc.getPlayerByPlayerNum(playerNum) as UIPlayer
	}

	val sOC: SOC
		get() = soc

	fun getPlayerColor(playerNum: Int): GColor {
		if (playerNum < 1) return GColor.GRAY
		val p = getGUIPlayer(playerNum)
			?: return playerColors[playerNum - 1].color
		return p.color
	}

	val curPlayerNum: Int
		get() = if (currentMenu == MenuState.MENU_DEBUGGING) playerChooser.value as Int else soc.curPlayerNum
	private var optimalIndex = 0
	private var optimalOptions: List<BotNode>? = null
	private fun getBotNodeDetails(node: BotNode, maxKeyWidth: Int, maxValues: Map<String, Double>): String {
		val info = StringBuffer()
		info.append(String.format("""
	%-${Math.max(5, maxKeyWidth)}s FACTOR  VALUE MAX TOT

	""".trimIndent(), "PROPERTY"))
		for (key in node.keys) {
			val factor = AITuning.getInstance().getScalingFactor(key)
			val value = node.getValue(key)
			val percentMax = (100 * value / maxValues[key]!!).toInt()
			val percentTot = (100.0 * factor * value / node.getValue()).toInt()
			info.append(String.format("""
	%-${maxKeyWidth}s %1.4f %1.4f %3d %3d

	""".trimIndent(), key, factor, value, percentMax, percentTot))
		}
		return info.toString()
	}

	internal interface MyCustomPickHandler : CustomPickHandler {
		fun onMouseWheel(rotation: Int)
	}

	internal class NodeRect(val r: GRectangle, val s: String)

	private fun initNodeRectsArray(g: AGraphics, leafs: Collection<BotNode>, nodeRects: Array<NodeRect?>, ypos: Int) {
		var ypos = ypos
		var index = 0
		val fontHeight = g.textHeight
		val padding = 2

		// need to setup the same transform as UIBoardRenderer
		var width = g.viewportWidth.toFloat()
		val height = g.viewportHeight.toFloat()
		val dim = Math.min(width, height)
		g.pushMatrix()
		g.setIdentity()
		g.translate(width / 2, height / 2)
		g.scale(dim, dim)
		g.translate(-0.5f, -0.5f)
		for (node in leafs) {
			// find the best node from the root that suits us.
			// start moving up the tree and pick the top most that is an edge, vertex or tile node
			var n = node
			var best = node
			var desc = n.description
			while (n.parent != null) {
				n = n.parent!!
				if (n is BotNodeVertex ||
					n is BotNodeRoute ||
					n is BotNodeTile) {
					best = n
				}
				if (n.parent != null) desc = n.description
			}
			n = best
			val v = MutableVector2D(n.getBoardPosition(board))
			if (true || v.isZero) {

				// this method seems easier
				val s = "$index $desc"
				val r = GRectangle(padding.toFloat(), ypos.toFloat(), g.getTextWidth(s), fontHeight)
				nodeRects[index] = NodeRect(r, s)
				ypos += (fontHeight + padding * 2 + 1).toInt()
			} else {
				g.transform(v)
				val s = index.toString()
				width = g.getTextWidth(s)
				val r = GRectangle(v.X() - width / 2, v.Y() - fontHeight / 2, width, fontHeight)
				for (i in 0 until index) {
					if (nodeRects[i]!!.r.isIntersectingWidth(r)) {
						r.x = nodeRects[i]!!.r.x
						r.y = nodeRects[i]!!.r.y + nodeRects[i]!!.r.h + padding
						break
					}
				}
				nodeRects[index] = NodeRect(r, s)
			}
			index++
		}
		g.popMatrix()
	}

	fun showPopup(pop: JFrame) {
		popup?.isVisible = false
		popup = pop
		pop.isUndecorated = true
		frame.isEnabled = false
		pop.minimumSize = Dimension(160, 120)
		pop.pack()
		val x = frame.x + frame.width / 2 - pop.width / 2
		val y = frame.y + frame.height / 2 - pop.height / 2
		pop.setLocation(x, y)
		pop.isResizable = false
		pop.isVisible = true
	}

	fun showPopup(title: String?, view: JComponent?, button: Array<PopupButton?>) {
		val frame = JFrame()
		frame.title = title
		val container = JPanel()
		container.border = BorderFactory.createLineBorder(Color.BLACK, 3)
		container.layout = BorderLayout()
		container.add(JLabel(title), BorderLayout.NORTH)
		container.add(view, BorderLayout.CENTER)
		val buttons = Container()
		container.add(buttons, BorderLayout.SOUTH)
		buttons.layout = GridLayout(1, 0)
		for (i in button.indices) {
			if (button[i] != null) {
				buttons.add(button[i])
				button[i]!!.addActionListener(this)
			} else {
				buttons.add(JLabel())
			}
		}
		frame.contentPane = container
		showPopup(frame)
	}

	fun showPopup(name: String?, msg: String?,
	              leftButton: PopupButton?,
	              middleButton: PopupButton?,
	              rightButton: PopupButton?) {
		val frame = JFrame()
		frame.isAlwaysOnTop = true
		frame.title = name
		val label = JTextArea(msg)
		label.lineWrap = true
		val container = JPanel()
		container.border = BorderFactory.createLineBorder(Color.BLACK, 3)
		container.layout = BorderLayout()
		container.add(JLabel(name), BorderLayout.NORTH)
		container.add(label, BorderLayout.CENTER)
		val buttons = Container()
		container.add(buttons, BorderLayout.SOUTH)
		buttons.layout = GridLayout(1, 0)
		if (leftButton != null) {
			buttons.add(leftButton)
			leftButton.addActionListener(this)
		} else buttons.add(JLabel())
		if (middleButton != null) {
			buttons.add(middleButton)
			middleButton.addActionListener(this)
		} else buttons.add(JLabel())
		if (rightButton != null) {
			buttons.add(rightButton)
			rightButton.addActionListener(this)
		} else buttons.add(JLabel())
		frame.contentPane = container
		showPopup(frame)
	}

	fun showPopup(name: String?, msg: String?, leftButton: PopupButton?, rightButton: PopupButton?) {
		showPopup(name, msg, leftButton, null, rightButton)
	}

	fun showPopup(name: String?, msg: String?, middleButton: PopupButton?) {
		showPopup(name, msg, null, middleButton, null)
	}

	fun showOkPopup(name: String?, view: JComponent?) {
		val button = PopupButton("OK")
		showPopup(name, view, arrayOf(null, button, null))
	}

	fun showOkPopup(name: String?, msg: String?) {
		val button = PopupButton("OK")
		showPopup(name, msg, button)
	}

	fun showConfigureGameSettingsPopup(rules: Rules, editable: Boolean) {
		val view = JPanel()
		val panel = JScrollPane()
		panel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		//panel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.preferredSize = Dimension(frame.width, frame.height)
		panel.viewport.add(view)
		view.layout = GridBagLayout()
		val cons = GridBagConstraints()
		cons.fill = GridBagConstraints.BOTH
		cons.anchor = GridBagConstraints.WEST
		val components = HashMap<JComponent, Field>()
		val numCols = 10
		try {
			var `var`: Variation? = null
			val fields = Rules::class.java.declaredFields
			for (f in fields) {
				val anno = f.annotations
				for (a in anno) {
					if (a.annotationClass == Rules.Rule::class) {
						cons.gridx = 0
						f.isAccessible = true
						val ruleVar = a as Rules.Rule
						if (`var` != ruleVar.variation) {
							`var` = ruleVar.variation
							cons.fill = GridBagConstraints.HORIZONTAL
							cons.gridwidth = numCols
							view.add(JLabel(sOC.getString(`var`!!.stringId)), cons)
							cons.gridy++
							view.add(JSeparator(), cons)
							cons.gridy++
							cons.fill = GridBagConstraints.NONE
						}
						cons.gridx = 0
						cons.gridwidth = 1
						if (f.type == Boolean::class.javaPrimitiveType) {
							if (editable) {
								val button = JCheckBox("", f.getBoolean(rules))
								view.add(button, cons)
								components[button] = f
							} else {
								view.add(JLabel(if (f.getBoolean(rules)) "Enabled" else "Disabled"), cons)
							}
						} else if (f.type == Int::class.javaPrimitiveType) {
							if (editable) {
								val spinner = JSpinner(SpinnerNumberModel(f.getInt(rules), ruleVar.minValue, ruleVar.maxValue, 1))
								view.add(spinner, cons)
								components[spinner] = f
							} else {
								view.add(JLabel("" + f.getInt(rules)), cons)
							}
						} else {
							System.err.println("Dont know how to handle field type:" + f.type)
						}
						cons.gridx = 1
						cons.gridwidth = numCols - 1
						val txt = "<html><div WIDTH=900>" + sOC.getString(ruleVar.stringId) + "</div></html>"
						val label = JLabel(txt)
						view.add(label, cons)
						cons.gridy++
						break
					}
				}
			}
			val buttons = arrayOfNulls<PopupButton>(4)
			buttons[0] = object : PopupButton("View\nDefaults") {
				override fun doAction(): Boolean {
					Thread { showConfigureGameSettingsPopup(Rules(), true) }.start()
					return false
				}
			}
			buttons[1] = object : PopupButton("Save\nAs Default") {
				override fun doAction(): Boolean {
					try {
						for (c in components.keys) {
							val f = components[c]
							if (c is JToggleButton) {
								val value = c.isSelected
								f!!.setBoolean(rules, value)
							} else if (c is JSpinner) {
								val value = c.value as Int
								f!!.setInt(rules, value)
							}
						}
						rules.saveToFile(saveRulesFile.absoluteFile)
						this@GUI.rules.copyFrom(rules)
					} catch (e: Exception) {
						e.printStackTrace()
					}
					return true
				}
			}
			buttons[2] = object : PopupButton("Keep") {
				override fun doAction(): Boolean {
					try {
						// TODO: fix cut-paste code
						for (c in components.keys) {
							val f = components[c]
							if (c is JToggleButton) {
								val value = c.isSelected
								f!!.setBoolean(rules, value)
							} else if (c is JSpinner) {
								val value = c.value as Int
								f!!.setInt(rules, value)
							}
						}
						rules.copyFrom(rules)
					} catch (e: Exception) {
						e.printStackTrace()
					}
					return true
				}
			}
			buttons[3] = PopupButton("Cancel")
			this.showPopup("CONFIGURE GAME SETTINGS", panel, buttons)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	// END POPUPS
	// OVERRIDES
	override fun actionPerformed(e: ActionEvent) {
		if (e.source is OpButton) {
			val button = e.source as OpButton
			val op = button.item
			op.action.onAction(op, button.extra)
		} else if (e.source is PopupButton) {
			val button = e.source as PopupButton
			if (button.doAction()) {
				closePopup()
			}
		}
		frame.repaint()
	}

	val currentMenu: MenuState
		get() = if (menuStack.size == 0) MenuState.MENU_START else menuStack.peek()

	private fun clearSaves() {
		try {
			FileUtils.deleteDirContents(SETTINGS_FOLDER, "playerAI*")
			FileUtils.deleteDirContents(SETTINGS_FOLDER, "socsave*")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	companion object {
		const val PROP_AI_TUNING_ENABLED = "aituning.enable"
		const val PROP_SCENARIOS_DIR = "scenariosDirectory"
		const val PROP_BOARDS_DIR = "boardsDirectory"
		val SETTINGS_FOLDER = FileUtils.getOrCreateSettingsDirectory(GUI::class.java)
		val AI_TUNING_FILE = File("assets/aituning.properties")
		@Throws(Exception::class)
		@JvmStatic
		fun main(args: Array<String>) {
			val all = UIManager.getInstalledLookAndFeels()
			val lafs = HashMap<String, String>()
			for (i in all.indices) {
				lafs[all[i].name] = all[i].className
			}
			UIManager.setLookAndFeel(lafs["Metal"])
			val frame = AWTFrame()
			frame.title = "Senators of Coran"
			try {
				PlayerBot.DEBUG_ENABLED = true
				Utils.setDebugEnabled(true)
				AGraphics.DEBUG_ENABLED = true
				val props = UIProperties()
				val propsFile = File(SETTINGS_FOLDER, "gui.properties")
				props.load(propsFile.absolutePath)
				GUI(frame, props)
				println(props.toString().replace(",", "\n"))
				if (!frame.loadFromFile(propsFile)) {
					frame.centerToScreen(640, 480)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}

		fun getExtensionFilter(ext: String?, acceptDirectories: Boolean): FileFilter {
			return object : FileFilter() {
				override fun accept(file: File): Boolean {
					return if (file.isDirectory && acceptDirectories) true else file.name.endsWith(ext!!)
				}

				override fun getDescription(): String {
					return "SOC Board Files"
				}
			}
		}
	}

	init {
		ToolTipManager.sharedInstance().dismissDelay = Int.MAX_VALUE
		soc = object : UISOC(playerRenderers, boardRenderer, diceRenderers, console, eventCardRenderer, barbarianRenderer) {
			override fun addMenuItem(item: MenuItem, title: String, helpText: String, extra: Any?) {
				this@GUI.addMenuItem(getMenuOpButton(item, title, helpText, extra))
			}

			override fun clearMenu() {
				menu.removeAll()
			}

			override fun redraw() {
				frame.repaint()
			}

			override val serverName: String
				get() = System.getProperty("user.name")

			override fun showOkPopup(title: String, message: String) {
				JOptionPane.showMessageDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE)
			}

			override fun completeMenu() {
				run {
					val sep: JComponent = JSeparator()
					val d = sep.preferredSize
					d.height = 32
					sep.preferredSize = d
					menu.add(sep)
				}
				if (canCancel()) {
					addMenuItem(CANCEL)
				} else {
					menu.add(JLabel(""))
				}
				val aiTuningEnabled = props.getBooleanProperty(PROP_AI_TUNING_ENABLED, false)
				val tuneAI = JToggleButton("AI Tuning")
				tuneAI.isSelected = aiTuningEnabled
				tuneAI.addActionListener { e -> props.setProperty(PROP_AI_TUNING_ENABLED, (e.source as JToggleButton).isSelected) }
				menu.add(tuneAI)
				menu.add(getMenuOpButton(SHOW_RULES))
				menu.add(getMenuOpButton(BUILDABLES_POPUP))
				menu.add(getMenuOpButton(REWIND_GAME))
				menu.add(getMenuOpButton(QUIT))
				this@GUI.helpText.text = helpText
				frame.validate()
			}

			override fun chooseOptimalPath(optimal: BotNode?, leafs: List<BotNode>): BotNode? {
				var optimal = optimal
				if (props.getBooleanProperty(PROP_AI_TUNING_ENABLED, false) == false) return optimal
				if (!isRunning) return optimal
				val leftPanelOffset = IntArray(1)
				val maxValues = HashMap<String, Double>()
				var maxKeyWidth = 0
				for (n in leafs) {
					for (key in n.keys) {
						maxKeyWidth = Math.max(maxKeyWidth, key.length)
						val v = n.getValue(key)
						if (maxValues.containsKey(key)) {
							maxValues[key] = Math.max(v, maxValues[key]!!)
						} else {
							maxValues[key] = v
						}
					}
				}
				clearMenu()

				//menu.add(getMenuOpButton(MenuItem.NEXT_OPTIMAL_INDEX));
				//menu.add(getMenuOpButton(MenuItem.PREV_OPTIMAL_INDEX));
				menu.add(getMenuOpButton(AITUNING_ACCEPT_OPTIMAL))
				val refresh = getMenuOpButton(AITUNING_REFRESH)
				menu.add(refresh)
				if (optimal != null) optimalIndex = leafs.indexOf(optimal) else optimal = leafs[optimalIndex]
				optimalOptions = leafs
				val g = boardComp.aPGraphics
				val fontHeight = g.textHeight
				val ypos = -leftPanelOffset[0] * fontHeight
				val nodeRects = arrayOfNulls<NodeRect>(leafs.size)
				initNodeRectsArray(g, leafs, nodeRects, Math.round(ypos))
				val padding = 2
				val maxKeyWidthf = maxKeyWidth
				val optimalInfo = getBotNodeDetails(optimal, maxKeyWidth, maxValues)
				val nodeArea = JTextArea()
				nodeArea.addKeyListener(object : KeyListener {
					override fun keyTyped(e: KeyEvent) {}
					override fun keyReleased(e: KeyEvent) {}
					override fun keyPressed(e: KeyEvent) {
						if (e.keyChar.toInt() == KeyEvent.VK_ENTER) {
							refresh.doClick()
							e.consume()
						}
					}
				})
				val area = middleLeftPanel.push()
				val pane = JScrollPane()
				pane.viewport.add(nodeArea)
				area.add(pane)
				nodeArea.font = Font.decode("courier-plain-10")
				nodeArea.text = optimalInfo
				completeMenu()
				val handler: MyCustomPickHandler = object : MyCustomPickHandler {
					var lastHighlighted = -1
					override fun onMouseWheel(clicks: Int) {
						leftPanelOffset[0] = Math.max(0, leftPanelOffset[0] + clicks)
						val ypos = -leftPanelOffset[0] * fontHeight
						initNodeRectsArray(g, leafs, nodeRects, Math.round(ypos))
						boardComp.repaint()
					}

					override fun onPick(bc: UIBoardRenderer, pickedValue: Int) {
						val n = leafs[pickedValue]
						if (n.data is Vertex) {
							val v = n.data as Vertex
							v.setPlayerAndType(curPlayerNum, VertexType.SETTLEMENT)
							val d = board.computeDistances(rules, curPlayerNum)
							console.addText(GColor.BLACK, d.toString())
							v.setOpen()
						}
						if (n.data is Route) {
							val r = n.data as Route
							//r.setType(RouteType.SHIP);
							board.setPlayerForRoute(r, curPlayerNum, RouteType.SHIP)
							val d = board.computeDistances(rules, curPlayerNum)
							console.addText(GColor.BLACK, d.toString())
							board.setRouteOpen(r)
						}

						// rewrite the aituning properties (to the text pane, user must visually inspect and commit) such that the picked botnode becomes the most dominant.
						// there will be cases when this is not possible, in which case, algorithm will need additional factors introduced to give favor to the node we want to 'win'
						val best = leafs[0]
						val delta = best.getValue() - n.getValue()
						if (delta > 0) {
							var deltaPos = 0.0
							var deltaNeg = 0.0
							val propsToChange = Properties()
							for (key in best.keys) {
								val factor = AITuning.getInstance().getScalingFactor(key)
								val b = factor * best.getValue(key)
								val t = factor * n.getValue(key)
								val dt = b - t
								if (dt > 0) {
									deltaPos += dt
								} else if (dt < 0) {
									// capture those variable where 'n' beats 'best'; these will be scaled up to make 'n' win
									deltaNeg -= dt
									propsToChange.setProperty(key, "")
								} // else ignore
							}
							if (deltaNeg > 0 && deltaPos > 0 && propsToChange.size > 0) {
								var str = ""
								var newScale = deltaPos / deltaNeg
								newScale = Math.floor(newScale + 1) // we want > than, not =
								for (key in propsToChange.keys) {
									val f = AITuning.getInstance().getScalingFactor(key as String)
									val fnew = f * newScale
									str += String.format("%-20s %f\n", key, fnew)
									propsToChange.setProperty(key, fnew.toString())
								}
								val opt = JOptionPane.showConfirmDialog(frame, "Confirm changes\n\n$str")
								if (opt == JOptionPane.YES_OPTION) {
									aiTuning.putAll(propsToChange)
									try {
										val out = FileOutputStream(AI_TUNING_FILE)
										try {
											aiTuning.store(out, "Generated by SOC Swing Utility")
										} finally {
											out.close()
										}
									} catch (e: Exception) {
										e.printStackTrace()
									}
								}
							} else {
								console.addText(GColor.BLACK, "Node has no max values")
							}
						}
						notifyWaitObj()
					}

					override fun onHighlighted(bc: UIBoardRenderer, g: APGraphics, highlightedIndex: Int) {
						if (lastHighlighted != highlightedIndex) {
							val node = leafs[highlightedIndex]
							val info = getBotNodeDetails(node, maxKeyWidthf, maxValues)
							nodeArea.text = info
						}
						lastHighlighted = highlightedIndex
						var node = leafs[highlightedIndex]
						onDrawPickable(bc, g, highlightedIndex)
						val nr = nodeRects[highlightedIndex]
						var text = node.description
						while (node.parent != null) {
							node = node.parent!!
							text = node.description + " => " + text
						}
						text = "$highlightedIndex: $text"
						node = leafs[highlightedIndex]
						val info = String.format("%.6f\n", node.getValue()) + text
						g.pushMatrix()
						g.setIdentity()
						g.color = GColor.RED
						g.drawJustifiedStringOnBackground(nr!!.r.x, nr.r.y, Justify.LEFT, Justify.TOP, nr.s, GColor.TRANSLUSCENT_BLACK, 2f)
						g.color = GColor.YELLOW
						g.drawWrapStringOnBackground(nr.r.x + nr.r.w, nr.r.y, (g.viewportWidth / 2).toFloat(), info, GColor.TRANSLUSCENT_BLACK, 2f)
						g.popMatrix()
						g.color = GColor.BLACK
						val v = MutableVector2D()
						g.begin()
						while (node.parent != null) {
							v.set(node.getBoardPosition(board))
							if (!v.isZero) {
								g.vertex(v)
							}
							node = node.parent!!
						}
						g.drawPoints(15f)
					}

					override fun onDrawPickable(bc: UIBoardRenderer, g: APGraphics, index: Int) {
						val nr = nodeRects[index]
						g.color = GColor.YELLOW
						g.pushMatrix()
						g.setIdentity()
						g.drawJustifiedStringOnBackground(nr!!.r.x, nr.r.y, Justify.LEFT, Justify.TOP, nr.s, GColor.TRANSLUSCENT_BLACK, 2f)
						g.popMatrix()
					}

					override fun onDrawOverlay(bc: UIBoardRenderer, g: APGraphics) {}
					override fun isPickableIndex(bc: UIBoardRenderer, index: Int): Boolean {
						return true
					}

					override val pickMode = PickMode.PM_CUSTOM
					override val numElements = nodeRects.size

					override fun pickElement(b: UIBoardRenderer, g: APGraphics, x: Int, y: Int): Int {
						for (i in nodeRects.indices) {
							val dy = i * g.textHeight
							val nr = nodeRects[i]
							if (nodeRects[i]!!.r.contains(x.toFloat(), y.toFloat())) return i
						}
						return -1
					}
				}
				boardRenderer.pickHandler = handler
				val result = waitForReturnValue<BotNode>(null)
				middleLeftPanel.pop()
				boardRenderer.pickHandler = null
				return result
			}

			override fun onShouldSaveGame() {
				FileUtils.backupFile(saveGameFile.absolutePath, 20)
				trySaveToFile(saveGameFile)
			}

			override fun onRunError(e: Throwable) {
				super.onRunError(e)
				quitToMainMenu()
			}

			override val isAITuningEnabled: Boolean
				get() = props.getBooleanProperty(PROP_AI_TUNING_ENABLED, false)

			override fun showChoicePopup(title: String, choices: List<String>): String? {
				val index = frame.showItemChooserDialog(title, "If you cancel from this dialog you will be disconnected from game", null, *choices.toTypedArray())
				return if (index >= 0) {
					choices[index]
				} else null
			}
		}
		val boardFilename = props.getProperty("gui.defaultBoardFilename", "soc_def_board.txt")
		defaultBoardFile = File(SETTINGS_FOLDER, boardFilename)
		if (!defaultBoardFile.exists()) {
			defaultBoardFile = File(boardFilename)
		}
		saveGameFile = File(SETTINGS_FOLDER, props.getProperty("gui.saveGameFileName", "socsavegame.txt"))
		saveRulesFile = File(SETTINGS_FOLDER, props.getProperty("gui.saveRulesFileName", "socrules.txt"))
		debugBoard = File("boards/debug_board.txt")
		if (saveRulesFile.exists()) {
			rules.loadFromFile(saveRulesFile)
		}
		menuStack.push(MenuState.MENU_START)
		if (!loadBoard(defaultBoardFile)) {
			board.generateDefaultBoard()
			saveBoard(defaultBoardFile)
		}
		playerColors = arrayOf(
			ColorString(GColor.RED, "Red"),
			ColorString(GColor.GREEN.darkened(0.5f), "Green"),
			ColorString(GColor.BLUE, "Blue"),
			ColorString(GColor.ORANGE.darkened(0.1f), "Orange"),
			ColorString(GColor.MAGENTA, "Magenta"),
			ColorString(GColor.PINK, "Pink")
		)
		playerChooser = JSpinner(SpinnerNumberModel(props.getIntProperty("debug.playerNum", 1), 1, playerColors.size, 1))
		playerChooser.addChangeListener { props.setProperty("debug.playerNum", (playerChooser.value as Int)) }
		val scenario = props.getProperty("scenario")
		if (scenario != null) {
			try {
				loadGame(File(scenario))
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		try {
			val reader = FileInputStream(AI_TUNING_FILE)
			try {
				aiTuning.load(reader)
			} finally {
				reader.close()
			}
		} catch (e: FileNotFoundException) {
			AI_TUNING_FILE.createNewFile()
		} catch (e: Exception) {
			e.printStackTrace()
		}
		AITuning.setInstance(object : AITuning() {
			override fun getScalingFactor(property: String): Double {
				if (!aiTuning.containsKey(property)) {
					aiTuning.setProperty(property, "1.0")
					return 1.0
				}
				return java.lang.Double.valueOf(aiTuning.getProperty(property))
			}
		})

		// menu
		menu.layout = AWTButtonLayout()
		cntrBorderPanel.layout = BorderLayout()
		westBorderPanel.layout = BorderLayout()
		eastGridPanel.layout = GridLayout(0, 1)
		//eastGridPanel.setBorder(BorderFactory.createLineBorder(GColor.CYAN, 2));
		westGridPanel.layout = GridLayout(0, 1)
		//westGridPanel.setBorder(BorderFactory.createLineBorder(GColor.CYAN, 2));
		val boardPanel = JPanel(BorderLayout())
		boardPanel.add(boardComp, BorderLayout.CENTER)
		boardPanel.add(boardNameLabel, BorderLayout.NORTH)
		cntrBorderPanel.add(boardPanel)
		setupDimensions(
			props.getIntProperty("gui.w", 640),
			props.getIntProperty("gui.h", 480)
		)
		frame.layout = BorderLayout()
		frame.add(cntrBorderPanel, BorderLayout.CENTER)
		frame.add(eastGridPanel, BorderLayout.EAST)
		frame.add(westGridPanel, BorderLayout.WEST)
		cntrBorderPanel.add(consoleComponent, BorderLayout.SOUTH)
		helpText.border = BorderFactory.createLineBorder(helpText.background, 5)
		frame.background = GColor.LIGHT_GRAY
		initMenu()
	}
}