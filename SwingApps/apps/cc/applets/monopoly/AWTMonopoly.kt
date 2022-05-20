package cc.applets.monopoly

import cc.lib.game.AGraphics
import cc.lib.logger.LoggerFactory
import cc.lib.monopoly.*
import cc.lib.monopoly.Player.CardChoiceType
import cc.lib.swing.*
import cc.lib.swing.AWTFrame.OnListItemChoosen
import cc.lib.utils.FileUtils
import cc.lib.utils.Reflector
import cc.lib.utils.prettify
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JLabel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.ChangeEvent
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class AWTMonopoly internal constructor() : AWTComponent() {
	val SAVE_FILE: File
	val RULES_FILE: File
	val frame: AWTFrame
	val lock = ReentrantLock()
	val cond = lock.newCondition()

	fun showRulesPopup() {
		val rules = monopoly.rules
		val popup = AWTFrame()
		val content = AWTPanel(BorderLayout())
		val npStart = AWTNumberPicker.Builder().setLabel("Start $").setMin(500).setMax(1500).setStep(100).setValue(rules.startMoney).build { _, value: Int -> rules.startMoney = value }
		val npWin = AWTNumberPicker.Builder().setLabel("Win $").setMin(2000).setMax(5000).setStep(500).setValue(rules.valueToWin).build(null)
		val npTaxScale = AWTNumberPicker.Builder().setLabel("Tax Scale %").setMin(0).setMax(200).setStep(25).setValue((100f * rules.taxScale).roundToInt()).build(null)
		val jailBump = AWTToggleButton("Jail Bump", rules.jailBumpEnabled)
		val jailMulti = AWTToggleButton("Jail Multiplier", rules.jailMultiplier)
		val jailMaxTurns = AWTNumberPicker.Builder().setLabel("Max Jail Turns").setMin(2).setMax(5).setValue(rules.maxTurnsInJail).build(null)
		content.addTop(AWTLabel("RULES", 1, 20f, true))
		val buttons = AWTPanel(0, 1)
		val pickers = AWTPanel(1, 0)
		pickers.add(npStart)
		pickers.add(npWin)
		pickers.add(npTaxScale)
		pickers.add(jailMaxTurns)
		content.add(pickers)
		content.addRight(buttons)
		buttons.add(jailBump)
		buttons.add(jailMulti)
		buttons.add(object : AWTButton("Apply") {
			override fun onAction() {
				rules.valueToWin = npWin.value
				rules.taxScale = 0.01f * npTaxScale.value
				rules.startMoney = npStart.value
				rules.jailBumpEnabled = jailBump.isSelected
				rules.jailMultiplier = jailMulti.isSelected
				rules.maxTurnsInJail = jailMaxTurns.value
				popup.closePopup()
				try {
					Reflector.serializeToFile<Any>(rules, RULES_FILE)
				} catch (e: Exception) {
					frame.showMessageDialog("ERROR", """Error save rules to file $RULES_FILE
${e.javaClass.simpleName} ${e.message}""")
				}
			}
		})
		content.addBottom(object : AWTButton("Cancel") {
			override fun onAction() {
				popup.closePopup()
			}
		})
		popup.contentPane = content
		popup.showAsPopup(frame)
	}

	init {
		setMouseEnabled(true)
		setPadding(5)
		val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		SAVE_FILE = File(settings, "game.save")
		RULES_FILE = File(settings, "rules.save")

		frame = object : AWTFrame("Monopoly") {
			/*
            @Override
            protected void onWindowClosing() {
                if (monopoly.isGameRunning())
                    monopoly.trySaveToFile(SAVE_FILE);
            }*/
			override fun onMenuItemSelected(menu: String, subMenu: String) {
				when (menu) {
					"File" -> when (subMenu) {
						"New Game" -> showListChooserDialog(object : OnListItemChoosen {
							override fun itemChoose(item: Int) {
								showListChooserDialog(object : OnListItemChoosen {
									override fun itemChoose(pc: Int) {
										monopoly.stopGameThread()
										monopoly.initPlayers(item + 2, Piece.values()[pc])
										monopoly.newGame()
										monopoly.startGameThread()
									}

									override fun cancelled() {}
								}, "Choose Piece", *Piece.values().toList().prettify())
							}

							override fun cancelled() {}
						}, "How Many Players?", "2", "3", "4")
						"Resume"   -> if (monopoly.tryLoadFromFile(SAVE_FILE)) {
							monopoly.startGameThread()
						}
						"Rules"    -> showRulesPopup()
					}
				}
			}
		}
		frame.addMenuBarMenu("File", "New Game", "Resume", "Rules")
		frame.add(this)
		if (!frame.loadFromFile(File(settings, "monopoly.properties")))
			frame.centerToScreen(800, 600)
	}

	val monopoly: UIMonopoly = object : UIMonopoly() {
		override fun repaint() {
			this@AWTMonopoly.repaint()
		}

		override fun getImageId(p: Piece): Int {
			return pieceMap[p]!!
		}

		override val boardImageId: Int
			get() = Images.monopoly_board.boardImageId

		override fun showChooseMoveMenu(player: Player, moves: List<MoveType>): MoveType? {
			val result = arrayOfNulls<MoveType>(1)
			val strings = arrayOfNulls<String>(moves.size)
			var index = 0
			for (mt in moves) {
				when (mt) {
					MoveType.PURCHASE_UNBOUGHT -> {
						val sq = getPurchasePropertySquare()
						strings[index++] = "Purchase " + prettify(sq.name) + " $" + sq.price
					}
					MoveType.PAY_BOND -> {
						strings[index++] = String.format("%s $%d", prettify(mt.name), player.jailBond)
					}
					else                       -> strings[index++] = prettify(mt.name)
				}
			}
			trySaveToFile(SAVE_FILE)
			frame.showListChooserDialog(object : OnListItemChoosen {
				override fun itemChoose(index: Int) {
					result[0] = moves[index]
					when (result[0]) {
						MoveType.PURCHASE_UNBOUGHT -> {
							showPropertyPopup("Purchase?", "Buy",
								getPurchasePropertySquare(), {
									lock.withLock { cond.signal() }
								}
							) {
								result[0] = null
								lock.withLock { cond.signal() }
							}
						}
						MoveType.PURCHASE          -> {
							showPropertyPopup("Purchase?", "Buy",
								player.square, { lock.withLock { cond.signal() } }
							) {
								result[0] = null
								lock.withLock { cond.signal() }
							}
						}
						else                       -> lock.withLock { cond.signal() }
					}
				}

				override fun cancelled() {
					if (canCancel())
						cancel()
					else
						stopGameThread()
					lock.withLock { cond.signal() }
				}
			}, "Choose Move " + player.piece, *strings)
			lock.withLock { cond.await() }
			return result[0]
		}

		override fun showChooseCardMenu(player: Player, cards: List<Card>, type: CardChoiceType): Card? {
			val result = arrayOfNulls<Card>(1)
			val items = arrayOfNulls<String>(cards.size)
			var index = 0
			for (c in cards) {
				when (type) {
					CardChoiceType.CHOOSE_CARD_TO_MORTGAGE -> items[index++] = prettify(c.property.name) + " Mortgage $" + c.property.getMortgageValue(c.houses)
					CardChoiceType.CHOOSE_CARD_TO_UNMORTGAGE -> items[index++] = prettify(c.property.name) + " Buyback $" + c.property.mortgageBuybackPrice
					CardChoiceType.CHOOSE_CARD_FOR_NEW_UNIT -> items[index++] = prettify(c.property.name) + " Unit $" + c.property.unitPrice
				}
			}
			frame.showListChooserDialog(object : OnListItemChoosen {
				override fun itemChoose(index: Int) {
					result[0] = cards[index]
					lock.withLock { cond.signal() }
				}

				override fun cancelled() {
					if (canCancel())
						cancel()
					else
						stopGameThread()
					lock.withLock { cond.signal() }
				}
			}, player.piece.toString() + " " + prettify(type.name), *items)
			lock.withLock { cond.await() }
			return result[0]
		}

		override fun showChooseTradeMenu(player: Player, trades: List<Trade>): Trade? {
			val result = arrayOfNulls<Trade>(1)
			frame.showListChooserDialog(object : OnListItemChoosen {
				override fun itemChoose(index: Int) {
					val t = trades[index]
					val trader = t.trader
					val title = "Buy " + prettify(t.card.property.name) + " from " + prettify(trader.piece.name)
					showPropertyPopup(title, "Buy for $" + t.price, t.card.property, {
						result[0] = t
						lock.withLock { cond.signal() }
					}) { lock.withLock { cond.signal() } }
				}

				override fun cancelled() {
					if (canCancel())
						cancel()
					else
						stopGameThread()
					lock.withLock { cond.signal() }
				}
			}, "Choose Trade" + player.piece, *trades.prettify())
			lock.withLock { cond.await() }
			return result[0]
		}

		override fun showMarkSellableMenu(user: PlayerUser, sellable: List<Card>): Boolean {
			val popup = AWTFrame()
			val listener = ActionListener { e: ActionEvent? ->
				lock.withLock { cond.signal() }
				popup.closePopup()
			}
			val content = AWTPanel(BorderLayout())
			val list = AWTPanel(0, 2)
			for (card in sellable) {
				val price = user.getSellableCardCost(card)
				list.add(JLabel(card.property.name))
				val spinner = JSpinner(SpinnerNumberModel(price, 0, 2000, 50))
				spinner.addChangeListener { e: ChangeEvent? -> user.setSellableCard(card, (spinner.value as Int)) }
				list.add(spinner)
			}
			content.add(AWTLabel("Mark Sellable " + user.piece.name, 1, 20f, true), BorderLayout.NORTH)
			content.add(list, BorderLayout.CENTER)
			content.add(AWTButton("DONE", listener), BorderLayout.SOUTH)
			popup.contentPane = content
			popup.showAsPopup(frame)
			lock.withLock { cond.await() }
			return true
		}
	}

	fun showPropertyPopup(title: String?, yesLabel: String?, sq: Square?, onBuyRunnable: Runnable, onDontBuyRunnble: Runnable): AWTFrame {
		val popup = AWTFrame()
		val content = AWTPanel(BorderLayout())
		content.add(AWTLabel(title, 1, 20f, true), BorderLayout.NORTH)
		val sqPanel: AWTComponent = object : AWTComponent() {
			override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
				monopoly.drawPropertyCard(g, g.viewportWidth.toFloat(), 0f, sq!!, null)
			}
		}
		sqPanel.setPreferredSize(monopoly.DIM / 3, monopoly.DIM / 4 * 2)
		content.add(sqPanel)
		content.add(AWTPanel(1, 2, object : AWTButton(yesLabel) {
			override fun onAction() {
				popup.closePopup()
				onBuyRunnable.run()
			}
		}, object : AWTButton("Dont Buy") {
			override fun onAction() {
				popup.closePopup()
				onDontBuyRunnble.run()
			}
		}), BorderLayout.SOUTH)
		popup.contentPane = content
		popup.showAsPopup(frame)
		return popup
	}

	var numImagesLoaded = 0

	internal enum class Images {
		monopoly_board,
		car,
		dog,
		iron,
		ship,
		shoe,
		thimble,
		tophat,
		wheelbarrow;

		var boardImageId = -1
	}

	var pieceMap: MutableMap<Piece, Int> = HashMap()
	override fun init(g: AWTGraphics) {
		try {
			val rules:Rules = Reflector.deserializeFromFile(RULES_FILE)
			monopoly.rules.copyFrom(rules)
		} catch (e: Exception) {
			e.printStackTrace()
		}

		g.addSearchPath("SwingApps/images")
		for (img in Images.values()) {
			img.boardImageId = g.loadImage("${img.name}.png")
			numImagesLoaded++
		}
		pieceMap[Piece.BOAT] = Images.ship.boardImageId
		pieceMap[Piece.CAR] = Images.car.boardImageId
		pieceMap[Piece.DOG] = Images.dog.boardImageId
		pieceMap[Piece.HAT] = Images.tophat.boardImageId
		pieceMap[Piece.IRON] = Images.iron.boardImageId
		pieceMap[Piece.SHOE] = Images.shoe.boardImageId
		pieceMap[Piece.THIMBLE] = Images.thimble.boardImageId
		pieceMap[Piece.WHEELBARROW] = Images.wheelbarrow.boardImageId
	}

	override fun getInitProgress(): Float {
		return numImagesLoaded.toFloat() / Images.values().size
	}

	override fun paint(g: AWTGraphics, mouseX: Int, mouseY: Int) {
		monopoly.paint(g, mouseX, mouseY)
	}

	companion object {
		private val log = LoggerFactory.getLogger(AWTMonopoly::class.java)
		@JvmStatic
		fun main(args: Array<String>) {
			AGraphics.DEBUG_ENABLED = true
			//Utils.setDebugEnabled()
			AWTMonopoly()
		}
	}

}