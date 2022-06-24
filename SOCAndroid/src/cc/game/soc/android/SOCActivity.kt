package cc.game.soc.android

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cc.game.soc.core.*
import cc.game.soc.core.Rules.Variation
import cc.game.soc.ui.*
import cc.lib.android.*
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.net.ClientConnection
import cc.lib.net.GameClient
import cc.lib.net.GameCommand
import cc.lib.net.GameServer
import cc.lib.utils.FileUtils
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.util.*

/**
 * Created by chriscaron on 2/15/18.
 */
class SOCActivity() : CCActivityBase(), MenuItem.Action, View.OnClickListener, GameServer.Listener, GameClient.Listener {
	lateinit var soc: UISOC
	lateinit var rulesFile: File
	lateinit var gameFile: File
	override lateinit var content: View
	lateinit var vBarbarian: SOCView<UIBarbarianRenderer>
	lateinit var vEvent: SOCView<UIEventCardRenderer>
	lateinit var vBoard: SOCView<UIBoardRenderer>
	lateinit var vDice: SOCView<UIDiceRenderer>
	lateinit var vPlayers: Array<SOCView<UIPlayerRenderer>>
	lateinit var vConsole: SOCView<UIConsoleRenderer>
	lateinit var lvMenu: ListView
	lateinit var svPlayers: ScrollView
	lateinit var tvHelpText: TextView
	val dialogStack = Stack<Dialog>()
	var helpItem = -1
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (BuildConfig.DEBUG) dumpAssets()
		RenderConstants.textMargin = resources.getDimension(R.dimen.margin)
		RenderConstants.textSizeBig = resources.getDimension(R.dimen.text_big)
		RenderConstants.textSizeSmall = resources.getDimension(R.dimen.text_sm)
		RenderConstants.thickLineThickness = resources.getDimension(R.dimen.line_thick)
		RenderConstants.thinLineThickness = resources.getDimension(R.dimen.line_thin)
		content = View.inflate(this, R.layout.soc_activity, null)
		setContentView(content)
		vBarbarian = findViewById(R.id.soc_barbarian)
		vEvent = findViewById(R.id.soc_event_cards)
		vBoard = findViewById(R.id.soc_board)
		vDice = findViewById(R.id.soc_dice)
		vPlayers = arrayOf(
			findViewById(R.id.soc_player_1),
			findViewById(R.id.soc_player_2),
			findViewById(R.id.soc_player_3),
			findViewById(R.id.soc_player_4),
			findViewById(R.id.soc_player_5),
			findViewById(R.id.soc_player_6)
		)
		if (vPlayers.size != SOC.MAX_PLAYERS) {
			throw AssertionError()
		}
		lvMenu = findViewById(R.id.soc_menu_list)
		vConsole = findViewById(R.id.soc_console)
		vConsole.getRenderer().setMinVisibleLines(5)
		svPlayers = findViewById<View>(R.id.svPlayers) as ScrollView
		vDice.getRenderer().initImages(R.drawable.dicesideship2, R.drawable.dicesidecity_red2, R.drawable.dicesidecity_green2, R.drawable.dicesidecity_blue2)
		tvHelpText = findViewById<View>(R.id.tvHelpText) as TextView
		QUIT = MenuItem(getString(R.string.menu_item_quit), getString(R.string.menu_item_quit_help), this)
		BUILDABLES = MenuItem(getString(R.string.menu_item_buildables), getString(R.string.menu_item_buildables_help), this)
		RULES = MenuItem(getString(R.string.menu_item_rules), getString(R.string.menu_item_rules_help), this)
		START = MenuItem(getString(R.string.menu_item_start), getString(R.string.menu_item_start_help), this)
		CONSOLE = MenuItem(getString(R.string.menu_item_console), getString(R.string.menu_item_console_help), this)
		SINGLE_PLAYER = MenuItem(getString(R.string.menu_item_sp), null, this)
		MULTI_PLAYER = MenuItem(getString(R.string.menu_item_mp), null, this)
		RESUME = MenuItem(getString(R.string.menu_item_resume), null, this)
		LOADSAVED = MenuItem("Load Saved", null, this)
		BOARDS = MenuItem(getString(R.string.menu_item_boards), null, this)
		SCENARIOS = MenuItem(getString(R.string.menu_item_scenarios), null, this)
		val menu = ArrayList<Array<Any?>>()
		val adapter: BaseAdapter = object : ArrayListAdapter<Array<Any?>>(this, menu, R.layout.menu_list_item) {
			override fun initItem(v: View, position: Int, item: Array<Any?>) {
				val mi = item[0] as MenuItem
				val title = item[1] as String?
				val helpText = item[2] as String?
				val extra = item[3]
				val vDivider = v.findViewById<View>(R.id.ivDivider)
				val tvTitle = v.findViewById<View>(R.id.tvTitle) as TextView
				val tvHelp = v.findViewById<View>(R.id.tvHelp) as TextView
				val bAction = v.findViewById<View>(R.id.bAction)
				val content = v.findViewById<View>(R.id.layoutContent)
				if (mi == DIVIDER) {
					vDivider.visibility = View.VISIBLE
					content.visibility = View.GONE
					tvHelp.visibility = View.GONE
				} else {
					vDivider.visibility = View.GONE
					content.visibility = View.VISIBLE
					tvTitle.text = title
					tvHelp.text = helpText
					bAction.setOnClickListener {
						mi.action.onAction(mi, extra)
					}
					tvHelp.visibility = if (helpItem == position) View.VISIBLE else View.GONE
					if (!Utils.isEmpty(helpText)) {
						v.setOnClickListener {
							if (helpItem == position) helpItem = -1 else helpItem = position
							notifyDataSetChanged()
						}
					} else {
						v.setOnClickListener(null)
					}
				}
			}
		}
		lvMenu.adapter = adapter
		gameFile = File(filesDir, "save.txt")
		rulesFile = File(filesDir, "rules.txt")
		val players = arrayOfNulls<UIPlayerRenderer>(SOC.MAX_PLAYERS)
		for (i in players.indices) {
			players[i] = vPlayers[i].getRenderer()
		}
		soc = object : UISOC(players, vBoard.getRenderer(), vDice.getRenderer(), vConsole.getRenderer(), vEvent.getRenderer(), vBarbarian.getRenderer()) {
			override fun addMenuItem(item: MenuItem, title: String?, helpText: String?, extra: Any?) {
				menu.add(arrayOf(
					item, title, helpText, extra
				))
			}

			override fun completeMenu() {
				addMenuItem(DIVIDER)
				super.completeMenu()
				//                    addMenuItem(CONSOLE);
				addMenuItem(BUILDABLES)
				addMenuItem(RULES)
				addMenuItem(QUIT)
				runOnUiThread(object : Runnable {
					override fun run() {
						adapter.notifyDataSetChanged()
					}
				})
			}

			override fun clearMenu() {
				helpItem = -1
				menu.clear()
				runOnUiThread(object : Runnable {
					override fun run() {
						adapter.notifyDataSetChanged()
					}
				})
			}

			override fun redraw() {
				runOnUiThread(object : Runnable {
					override fun run() {
						if (soc.curPlayerNum > 0) {
							svPlayers.smoothScrollTo(0, vPlayers[soc.curPlayerNum - 1].top)
							//content.invalidate();
							//vConsole.requestLayout();
							vConsole.redraw()
							for (v: SOCView<*> in vPlayers) {
								//v.requestLayout();
								v.redraw()
							}
							vBarbarian.redraw()
							tvHelpText.text = helpText
							vBoard.redraw()
							vDice.redraw()
						}
					}
				})
			}

			override fun showOkPopup(title: String, message: String) {
				runOnUiThread {
					newDialog(true).setTitle(title).setMessage(message).setNeutralButton("OK", object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							soc.notifyWaitObj()
						}
					}).show()
				}
				Utils.waitNoThrow(this, -1)
			}

			override fun showChoicePopup(title: String, choices: List<String>): String? {
				val choice = arrayOfNulls<String>(1)
				runOnUiThread {
					val items = choices.toTypedArray()
					newDialog(false).setTitle(title).setItems(items, object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							choice[0] = items[which]
						}
					})
				}
				Utils.waitNoThrow(this, -1)
				return (choice[0])
			}

			override fun getServerName(): String {
				return Build.BRAND + "." + Build.PRODUCT
			}

			override fun onShouldSaveGame() {
				trySaveToFile(gameFile)
				if (BuildConfig.DEBUG)
					checkExternalStoragePermissions(1002)
			}

			override fun onGameOver(winnerNum: Int) {
				super.onGameOver(winnerNum)
				clearMenu()
				addMenuItem(QUIT)
				runOnUiThread { adapter.notifyDataSetChanged() }
			}

			override fun onRunError(e: Throwable) {
				super.onRunError(e)
				if (BuildConfig.DEBUG) {
					// write crashes to sdcard for eval later
					try {
						val tmpDir = File.createTempFile("crash", "", externalStorageDirectory)
						tmpDir.delete()
						tmpDir.mkdir()
						val crash = File(tmpDir, "stack.txt")
						FileUtils.stringToFile(e.toString(), crash)
						FileUtils.copyFile(gameFile, tmpDir)
					} catch (ee: Exception) {
					}
				}
				runOnUiThread(object : Runnable {
					override fun run() {
						showStartMenu()
						newDialog(true).setTitle("Error").setMessage("An error occurred:\n" + e.javaClass.simpleName + "  " + e.message)
							.setNegativeButton("Ignore", null)
							.setPositiveButton("Report", object : DialogInterface.OnClickListener {
								override fun onClick(dialog: DialogInterface, which: Int) {
									try {
										val saveFile = File(cacheDir, "gameError.txt")
										if (gameFile.exists()) {
											FileUtils.copyFile(gameFile, saveFile)
										} else {
											saveToFile(saveFile)
										}
										EmailHelper.sendEmail(this@SOCActivity, saveFile, "sebisoftware@gmail.com", "SOC Crash log", Utils.toString(e.stackTrace))
									} catch (e: Exception) {
										e.printStackTrace()
									}
								}
							}).show()
					}
				})
			}

			override fun printinfo(playerNum: Int, txt: String) {
				vConsole.scrollTo(0, 0)
				super.printinfo(playerNum, txt)
			}

			override fun cancel() {
				if (user.client.isConnected) {
					user.client.cancelRemote()
				}
				super.cancel()
			}
		}
		soc.setBoard(vBoard.getRenderer().getBoard())
		val rules = Rules()
		if (rules.tryLoadFromFile(rulesFile)) {
			soc.setRules(rules)
		}
		val aiTuning = Properties()
		try {
			val `in` = assets.open("aituning.properties")
			try {
				aiTuning.load(`in`)
			} finally {
				`in`.close()
			}
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
	}

	val user = UIPlayerUser()
	val DIVIDER = MenuItem(null, null, null)
	var QUIT: MenuItem? = null
	var BUILDABLES: MenuItem? = null
	var RULES: MenuItem? = null
	var START: MenuItem? = null
	var CONSOLE: MenuItem? = null
	var SINGLE_PLAYER: MenuItem? = null
	var MULTI_PLAYER: MenuItem? = null
	var RESUME: MenuItem? = null
	var LOADSAVED: MenuItem? = null
	var BOARDS: MenuItem? = null
	var SCENARIOS: MenuItem? = null
	fun quitGame() {
		soc.server.stop()
		soc.clear()
		vBoard.getRenderer().setPickHandler(null)
		soc.stopRunning()
		user.client.disconnect("player quit")
		soc.clearMenu()
		showStartMenu()
	}

	override fun onAction(item: MenuItem, extra: Any?) {
		if (item == QUIT) {
			newDialog(true).setTitle("Confirm").setMessage("Ae you sure you want to quit the game?")
				.setPositiveButton("Quit") { dialog, which -> quitGame() }.show()
		} else if (item == BUILDABLES) {
			showBuildablesDialog()
		} else if (item == RULES) {
			showRulesDialog()
		} else if (item == START) {
			soc.clearMenu()
			soc.board.assignRandom()
			soc.startGameThread()
			vBoard.getRenderer().clearCached()
		} else if (item == CONSOLE) {
			//showConsole();
		} else if (item == SINGLE_PLAYER) {
			showSinglePlayerDialog()
		} else if (item == MULTI_PLAYER) {
			showMultiPlayerDialog()
		} else if (item == RESUME) {
			try {
				if (BuildConfig.DEBUG) {
					//FileUtils.copyFile(gameFile, externalStorageDirectory);
					// User can put a fixed file onto sdcard to overwrite the current save file.
					// It will be deleted when done
					val fixed = File(externalStorageDirectory, "fixed.txt")
					if (fixed.exists()) {
						FileUtils.copyFile(fixed, gameFile)
						fixed.delete()
					}
				}
				object : SpinnerTask<String>(this) {
					@Throws(Exception::class)
					override fun doIt(vararg args: String) {
						soc.loadFromFile(gameFile)
					}

					override fun onSuccess() {
						initGame()
					}
				}.execute()
			} catch (e: Exception) {
				(extra as View).isEnabled = false
				soc.clear()
				showError(e)
			}
		} else if (item == LOADSAVED) {
			externalStorageDirectory.list { dir, name -> name.endsWith(".txt") }?.let { files ->
				newDialog(true).setTitle("Load saved").setItems(files) { dialog, which ->
					if (soc.tryLoadFromFile(File(externalStorageDirectory, files[which]))) {
						initGame()
					} else {
						Toast.makeText(this@SOCActivity, "Problem loading '" + files[which], Toast.LENGTH_LONG).show()
					}
				}.setNegativeButton("Cancel", null).show()
			}
		} else if (item == BOARDS) {
			showBoardsDialog()
		} else if (item == SCENARIOS) {
			showScenariosDialog()
		}
	}

	fun showError(e: Exception) {
		newDialog(true).setTitle("ERROR").setMessage("AN error occured: " + e.javaClass.simpleName + " " + e.message).show()
	}

	fun showMultiPlayerDialog() {
		val mpItems = arrayOf(
			"Host",
			"Join",
			"Resume"
		)
		newDialog(true).setTitle("MULTIPLAYER")
			.setItems(mpItems, object : DialogInterface.OnClickListener {
				override fun onClick(dialog: DialogInterface, which: Int) {
					when (which) {
						0 -> showHostMultiPlayerDialog()
						1 -> showJoinMultiPlayerDialog()
						2 -> showResumeMultiPlayerDialog()
					}
				}
			}).show()
	}

	fun showJoinMultiPlayerDialog() {
		/*
        user.client.register(NetCommon.SOC_ID, UISOC.getInstance());
        user.client.register(NetCommon.USER_ID, user);

        mpGame = new MPGameManager(this, user.client, NetCommon.PORT, "soc") {
            @Override
            public void onAllClientsJoined() {
//                initGame();
            }
        };
        mpGame.showJoinGameDialog();
        user.client.addListener(this);*/
	}

	override fun onCommand(cmd: GameCommand) {
		try {
			if ((cmd.type == NetCommon.SVR_TO_CL_INIT)) {
				val soc = UISOC.getInstance()
				soc.clear()
				val num = cmd.getInt("numPlayers")
				val playerNum = cmd.getInt("playerNum")
				for (i in 0 until playerNum - 1) {
					soc.addPlayer(UIPlayer())
				}
				soc.addPlayer(user)
				for (i in playerNum until num) {
					soc.addPlayer(UIPlayer())
				}
				cmd.parseReflector("soc", soc)
				runOnUiThread(object : Runnable {
					override fun run() {
						initGame()
						soc.redraw()
					}
				})
			} else if ((cmd.type == NetCommon.SVR_TO_CL_UPDATE)) {
				UISOC.getInstance().merge(cmd.getString("diff"))
				UISOC.getInstance().refreshComponents()
				UISOC.getInstance().redraw()
			}
			soc.clearMenu()
			soc.completeMenu()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onMessage(msg: String) {
		log.info("onMessage: %s", msg)
	}

	override fun onDisconnected(reason: String, serverInitiated: Boolean) {
		log.info("onDisconnected: %s", reason)
	}

	override fun onConnected() {
		log.info("onConnected")
	}

//	interface Callback<T> {
//		fun onComplete(argument: T)
//	}

	val colorStrings = arrayOf(
		"Red", "Green", "Blue", "Yellow", "Orange", "Pink"
	)
	val colors = arrayOf(
		GColor.RED, GColor.GREEN, GColor.BLUE.lightened(.2f), GColor.YELLOW, GColor.ORANGE, GColor.PINK
	)

	fun showChooseColorDialog(andThen: (Int) -> Unit) {
		newDialog(true).setTitle("Pick Color").setItems(colorStrings, object : DialogInterface.OnClickListener {
			override fun onClick(dialog: DialogInterface, which: Int) {
				andThen(which)
			}
		}).show()
	}

	fun showResumeMultiPlayerDialog() {
		object : SpinnerTask<String>(this) {
			@Throws(Exception::class)
			override fun doIt(vararg args: String) {
				soc.loadFromFile(gameFile)
			}

			override fun onSuccess() {
				soc.redraw()
				/*
                mpGame = new MPGameManager(SOCActivity.this, soc.server, soc.getNumPlayers()-1) {
                    @Override
                    public void onAllClientsJoined() {
                        log.info("All client joined");
                        initGame();
                    }
                };
                mpGame.startHostMultiplayer();*/
			}
		}.execute()
	}

	fun showHostMultiPlayerDialog() {
		/*
		showChooseNumPlayersDialog() { param ->
			showChooseColorDialog(object : (Int)->(Int) {
					override fun onComplete(which: Int) {
						user.color = colors.get((which))
						soc.clear()
						soc.addPlayer(user)
						while (soc.numPlayers < numPlayers) {
							val p = UIPlayer()
							p.color = soc.availableColors.entries.iterator().next().value
							soc.addPlayer(p)
						}
						/*
                        mpGame = new MPGameManager(SOCActivity.this, soc.server, numPlayers-1) {
                            @Override
                            public void onAllClientsJoined() {
                                log.info("All client joined");
                                soc.initGame();
                                initGame();
                            }
                        };
                        mpGame.startHostMultiplayer();
                         */
					}
				})
			}
		})*/
	}

	fun showChooseNumPlayersDialog(andThen: (Int) -> Unit) {
		val minPlayers = soc.rules.minPlayers
		val maxPlayers = soc.rules.maxPlayers
		if (minPlayers >= maxPlayers) {
			andThen(minPlayers)
		} else {
			val items = arrayOfNulls<String>(maxPlayers - minPlayers)
			var index = 0
			for (i in minPlayers until maxPlayers) {
				items[index++] = i.toString()
			}
			newDialog(true).setTitle("Num Players").setItems(items, object : DialogInterface.OnClickListener {
				override fun onClick(dialog: DialogInterface, which: Int) {
					val numPlayers = which + minPlayers
					andThen(numPlayers)
				}
			}).setNegativeButton("Cancel", null).show()
		}
	}

	fun showSinglePlayerDialog() {
		showChooseNumPlayersDialog { numPlayers ->
			showChooseColorDialog { which ->
				user.color = colors.get(which)
				soc.clear()
				soc.addPlayer(user)
				for (i in 1 until numPlayers) {
					val nextColor = (which + i) % colors.size
					soc.addPlayer(UIPlayer(colors[nextColor]))
				}
				soc.initGame()
				initGame()
			}
		}
	}

	fun showScenariosDialog() {
		assets.list("scenarios")?.let { scenarios ->
			newDialog(true).setTitle("Load Scenario").setItems(scenarios, object : DialogInterface.OnClickListener {
				override fun onClick(dialog: DialogInterface, which: Int) {
					object : SpinnerTask<String>(this@SOCActivity) {
						@Throws(Exception::class)
						override fun doIt(vararg args: String) {
							val `in` = assets.open("scenarios/" + scenarios[which])
							try {
								val scenario = SOC()
								scenario.deserialize(`in`)
								soc.copyFrom(scenario)
							} finally {
								`in`.close()
							}
						}

						override fun onSuccess() {
							vBoard.invalidate()
							vBoard.getRenderer().clearCached()
						}
					}.execute()
				}
			}).setNegativeButton("Cancel", null).show()
		}?:run {
			Toast.makeText(this, "No Scenarios", Toast.LENGTH_LONG).show()
		}
	}

	fun showBoardsDialog() {
		assets.list("boards")?.let { boards ->
			newDialog(true).setTitle("Load Board").setItems(boards, object : DialogInterface.OnClickListener {
				override fun onClick(dialog: DialogInterface, which: Int) {
					try {
						val `in` = assets.open("boards/" + boards[which])
						try {
							val b = Board()
							b.deserialize(`in`)
							soc.board = b
							vBoard.getRenderer().clearCached()
							vBoard.invalidate()
						} finally {
							`in`.close()
						}
					} catch (e: IOException) {
						showError(e)
					}
				}
			}).setNegativeButton("Cancel", null).show()
		} ?: run {
			Toast.makeText(this, "No Boards", Toast.LENGTH_LONG).show()
		}
	}

	fun showStartMenu() {
		vBarbarian.visibility = View.GONE
		vEvent.visibility = View.GONE
		vDice.visibility = View.GONE
		svPlayers.visibility = View.GONE
		tvHelpText.visibility = View.GONE
		lvMenu.visibility = View.VISIBLE
		soc.clearMenu()
		soc.addMenuItem(SINGLE_PLAYER)
		soc.addMenuItem(MULTI_PLAYER)
		soc.addMenuItem(RESUME)
		if (BuildConfig.DEBUG) {
			soc.addMenuItem(LOADSAVED)
		}
		if (BuildConfig.DEBUG) soc.addMenuItem(BOARDS)
		soc.addMenuItem(SCENARIOS)
		soc.addMenuItem(RULES)
		vBoard.getRenderer().clearCached()
	}

	fun initGame() {
		var index = 1
		svPlayers.visibility = View.VISIBLE
		for (i in 1 until vPlayers.size) {
			vPlayers.get(i).visibility = View.GONE
		}
		for (i in 1..soc.numPlayers) {
			if (soc.getPlayerByPlayerNum(i) is UIPlayerUser) {
				vPlayers[0].getRenderer().setPlayer(i)
			} else {
				vPlayers[index].setVisibility(View.VISIBLE)
				vPlayers[index++].getRenderer().setPlayer(i)
			}
		}
		soc.clearMenu()
		if (user.client.isConnected) {
			soc.clearMenu()
			vBoard.getRenderer().clearCached()
		} else soc.addMenuItem(START)
		soc.completeMenu()
		vBarbarian.visibility = if (soc.rules.isEnableCitiesAndKnightsExpansion) View.VISIBLE else View.GONE
		if (soc.rules.isEnableEventCards) {
			vEvent.visibility = View.VISIBLE
			vDice.setVisibility(View.GONE)
		} else {
			vEvent.visibility = View.GONE
			vDice.setVisibility(View.VISIBLE)
		}
		clearDialogs()
		vBoard.getRenderer().clearCached()
		tvHelpText.visibility = View.VISIBLE
	}

	fun showBuildablesDialog() {
		val columnNames = Vector<String>()
		columnNames.add("Buildable")
		for (r: ResourceType in ResourceType.values()) {
			columnNames.add(" ${r.name} ")
		}
		val rowData = Vector<Vector<String>>()
		for (b: BuildableType in BuildableType.values()) {
			if (b.isAvailable(soc)) {
				val row = Vector<String>()
				row.add(b.name)
				for (r: ResourceType? in ResourceType.values()) row.add(b.getCost(r).toString())
				rowData.add(row)
			}
		}
		val table = TableLayout(this)
		val header = TableRow(this)
		val params = TableLayout.LayoutParams()
		params.width = TableLayout.LayoutParams.WRAP_CONTENT
		//params.rightMargin = (int)getResources().getDimension(R.dimen.margin);
		for (s: String? in columnNames) {
			val t = TextView(this)
			t.text = s
			header.addView(t)
		}
		table.addView(header)
		for (r: Vector<String> in rowData) {
			var gravity = Gravity.LEFT
			val row = TableRow(this)
			for (s: String? in r) {
				val t = TextView(this)
				t.text = s
				t.gravity = gravity
				gravity = Gravity.CENTER
				row.addView(t)
			}
			table.addView(row)
		}
		newDialog(true).setTitle("Buildables").setView(table).setNegativeButton("Ok", null).show()
	}

	internal inner class RuleItem : Comparable<RuleItem> {
		val `var`: Variation
		val min: Int
		val max: Int
		val stringId: String
		val order: Int
		val field: Field?

		constructor(`var`: Variation) {
			this.`var` = `var`
			max = 0
			min = max
			field = null
			stringId = `var`.stringId
			order = 0
		}

		constructor(rule: Rules.Rule, field: Field?) {
			`var` = rule.variation
			min = rule.minValue
			max = rule.maxValue
			this.field = field
			stringId = rule.stringId
			order = rule.order
		}

		override fun compareTo(o: RuleItem): Int {
			if (`var` != o.`var`) return `var`.compareTo(o.`var`)
			if (order != o.order) return order - o.order
			if (field == null) return -1
			return if (o.field == null) 1 else field.name.compareTo(o.field.name)
		}
	}

	internal inner class RulesAdapter(val rules: Rules, val canEdit: Boolean) : BaseAdapter(), CompoundButton.OnCheckedChangeListener, View.OnClickListener {
		val rulesList: MutableList<RuleItem> = ArrayList()
		override fun getCount(): Int {
			return rulesList.size
		}

		override fun getItem(position: Int): Any? {
			return null
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		override fun getView(position: Int, v: View?, parent: ViewGroup): View {
			var v = v?:View.inflate(this@SOCActivity, R.layout.rules_list_item, null)
			val tvHeader = v.findViewById<View>(R.id.tvHeader) as TextView
			//TextView tvName    = (TextView)v.findViewById(R.id.tvName);
			val tvDesc = v.findViewById<View>(R.id.tvDescription) as TextView
			val cb = v.findViewById<View>(R.id.cbEnabled) as CompoundButton
			val bEdit = v.findViewById<View>(R.id.bEdit) as Button
			try {
				val item = rulesList[position]
				if (item.field == null) {
					// header
					tvHeader.visibility = View.VISIBLE
					//tvName.setVisibility(View.GONE);
					tvDesc.visibility = View.GONE
					cb.visibility = View.GONE
					bEdit.visibility = View.GONE
					tvHeader.text = item.stringId
				} else if ((item.field.type == Boolean::class.javaPrimitiveType)) {
					// checkbox
					tvHeader.visibility = View.GONE
					//tvName.setVisibility(View.GONE);
					tvDesc.visibility = View.VISIBLE
					cb.visibility = View.VISIBLE
					bEdit.visibility = View.GONE
					//tvName.setText(item.field.getName());
					tvDesc.text = item.stringId
					cb.setOnCheckedChangeListener(null)
					cb.isChecked = item.field.getBoolean(rules)
					cb.isEnabled = canEdit
					cb.tag = item
					cb.setOnCheckedChangeListener(this)
				} else if ((item.field.type == Int::class.javaPrimitiveType)) {
					// numberpicker
					tvHeader.visibility = View.GONE
					//tvName.setVisibility(View.GONE);
					tvDesc.visibility = View.VISIBLE
					cb.visibility = View.GONE
					bEdit.visibility = View.VISIBLE
					//tvName.setText(item.field.getName());
					tvDesc.text = item.stringId
					val value = item.field.getInt(rules)
					bEdit.text = value.toString()
					bEdit.isEnabled = canEdit
					bEdit.tag = item
					bEdit.setOnClickListener(this)
				} else {
					throw AssertionError("Dont know how to handle field: " + item.field.name)
				}
			} catch (e: Exception) {
				throw AssertionError(e)
			}
			return v
		}

		override fun onClick(v: View) {
			try {
				val item = v.tag as RuleItem
				val value = item.field!!.getInt(rules)
				val np = CCNumberPicker.newPicker(this@SOCActivity, value, item.min, item.max, null)
				newDialog(false).setTitle(item.field.name).setView(np).setNegativeButton("Cancel", null)
					.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							try {
								item.field.setInt(rules, np.value)
								(v as Button).text = np.value.toString()
							} catch (e: Exception) {
								e.printStackTrace()
							}
						}
					}).show()
			} catch (e: Exception) {
				throw AssertionError(e)
			}
		}

		override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
			val item = buttonView.tag as RuleItem
			try {
				item.field!!.setBoolean(rules, isChecked)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}

		init {
			for (v: Variation in Variation.values()) {
				if (!canEdit) {
					if (v == Variation.SEAFARERS && !rules.isEnableSeafarersExpansion) continue
					if (v == Variation.CAK && !rules.isEnableCitiesAndKnightsExpansion) continue
				}
				rulesList.add(RuleItem(v))
			}
			for (f: Field in Rules::class.java.declaredFields) {
				val anno = f.annotations
				for (a: Annotation in anno) {
					if ((a.annotationClass == Rules.Rule::class.java)) {
						val ruleVar = a as Rules.Rule
						if (!canEdit) {
							if (ruleVar.variation == Variation.SEAFARERS && !rules.isEnableSeafarersExpansion) continue
							if (ruleVar.variation == Variation.CAK && !rules.isEnableCitiesAndKnightsExpansion) continue
						}
						f.isAccessible = true
						rulesList.add(RuleItem(ruleVar, f))
					}
				}
			}
			Collections.sort(rulesList)
		}
	}

	fun showRulesDialog() {
		val canEdit = !soc.isRunning && !user.client.isConnected
		val rules = soc.rules.deepCopy()
		val lv = ListView(this)
		lv.adapter = RulesAdapter(rules, canEdit)
		val b = newDialog(true).setTitle("Rules").setView(lv)
		if (canEdit) {
			b.setNegativeButton("Discard", null)
				.setNeutralButton("Save", object : DialogInterface.OnClickListener {
					override fun onClick(dialog: DialogInterface, which: Int) {
						rules.trySaveToFile(rulesFile)
						soc.rules = rules
					}
				}).setPositiveButton("Keep", object : DialogInterface.OnClickListener {
					override fun onClick(dialog: DialogInterface, which: Int) {
						soc.rules = rules
					}
				}).show()
		} else {
			b.setNegativeButton("Ok", null).show()
		}
	}

	fun copyFileToExt() {
		try {
//            FileUtils.copyFile(saveFile, externalStorageDirectory);
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onResume() {
		super.onResume()
		showStartMenu()
	}

	override fun onPause() {
		super.onPause()
		soc.stopRunning()
	}

	protected fun newDialog(cancelable: Boolean): AlertDialog.Builder {
		val b: AlertDialog.Builder = object : AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog) {
			override fun show(): AlertDialog {
				val d = super.show()
				dialogStack.push(d)
				return d
			}
		}
		if (cancelable) {
			if (dialogStack.size > 0) {
				b.setNegativeButton("Back", object : DialogInterface.OnClickListener {
					override fun onClick(dialog: DialogInterface, which: Int) {
						dialogStack.pop().show()
					}
				})
			} else {
				b.setNegativeButton("Cancel", null)
			}
		}
		return b
	}

	fun clearDialogs() {
		while (dialogStack.size > 0) {
			dialogStack.pop().dismiss()
		}
	}

	override fun onClick(v: View) {
		when (v.id) {
		}
	}

	override fun onConnected(conn: ClientConnection) {
		log.info("Clinet connected: " + conn.displayName)
	}

	override fun onReconnection(conn: ClientConnection) {
		log.info("Clinet reconnected: " + conn.displayName)
	}

	override fun onClientDisconnected(conn: ClientConnection) {
		log.info("Clinet disconnected: " + conn.displayName)
		runOnUiThread(object : Runnable {
			override fun run() {
				newDialog(false).setTitle("ERROR").setMessage("You have been disconnected")
					.setPositiveButton("Exit", object : DialogInterface.OnClickListener {
						override fun onClick(dialog: DialogInterface, which: Int) {
							quitGame()
						}
					}).show()
			}
		})
	}

	override fun onAllPermissionsGranted(code: Int) {
		if (code == 1002) {
			try {
				FileUtils.backupFile(externalStorageDirectory.absolutePath + "/socsave.txt", 20)
				FileUtils.copyFile(gameFile, File(externalStorageDirectory, "socsave.txt"))
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
}