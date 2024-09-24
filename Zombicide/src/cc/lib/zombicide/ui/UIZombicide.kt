package cc.lib.zombicide.ui

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.game.IInterpolator
import cc.lib.game.IRectangle
import cc.lib.game.IVector2D
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.net.ConnectionStatus
import cc.lib.reflector.Reflector
import cc.lib.ui.IButton
import cc.lib.utils.Grid.Pos
import cc.lib.utils.KLock
import cc.lib.utils.Lock
import cc.lib.utils.Table
import cc.lib.utils.forEachAs
import cc.lib.utils.getOrNull
import cc.lib.utils.launchIn
import cc.lib.utils.prettify
import cc.lib.utils.takeIfInstance
import cc.lib.utils.test
import cc.lib.zombicide.ZActionType
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZActorPosition
import cc.lib.zombicide.ZAnimation
import cc.lib.zombicide.ZAttackType
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZColor
import cc.lib.zombicide.ZDir
import cc.lib.zombicide.ZDoor
import cc.lib.zombicide.ZEquipment
import cc.lib.zombicide.ZGame
import cc.lib.zombicide.ZIcon
import cc.lib.zombicide.ZMove
import cc.lib.zombicide.ZMoveType
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZQuest
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZSkill
import cc.lib.zombicide.ZSpawnArea
import cc.lib.zombicide.ZSpawnCard
import cc.lib.zombicide.ZSpell
import cc.lib.zombicide.ZUser
import cc.lib.zombicide.ZWeapon
import cc.lib.zombicide.ZWeaponType
import cc.lib.zombicide.ZZombie
import cc.lib.zombicide.ZZombieCategory
import cc.lib.zombicide.ZZombieType
import cc.lib.zombicide.ZZone
import cc.lib.zombicide.anims.AscendingAngelDeathAnimation
import cc.lib.zombicide.anims.DeathAnimation
import cc.lib.zombicide.anims.DeathStrikeAnimation
import cc.lib.zombicide.anims.DeflectionAnimation
import cc.lib.zombicide.anims.EarthquakeAnimation
import cc.lib.zombicide.anims.ElectrocutionAnimation
import cc.lib.zombicide.anims.EmptyAnimation
import cc.lib.zombicide.anims.FireballAnimation
import cc.lib.zombicide.anims.GroupAnimation
import cc.lib.zombicide.anims.HandOfGodAnimation
import cc.lib.zombicide.anims.InfernoAnimation
import cc.lib.zombicide.anims.LightningAnimation2
import cc.lib.zombicide.anims.MagicOrbAnimation
import cc.lib.zombicide.anims.MakeNoiseAnimation
import cc.lib.zombicide.anims.MeleeAnimation
import cc.lib.zombicide.anims.MjolnirLightningAnimation
import cc.lib.zombicide.anims.MoveAnimation
import cc.lib.zombicide.anims.OverlayTextAnimation
import cc.lib.zombicide.anims.ShieldBlockAnimation
import cc.lib.zombicide.anims.ShootAnimation
import cc.lib.zombicide.anims.SlashedAnimation
import cc.lib.zombicide.anims.SpawnAnimation
import cc.lib.zombicide.anims.StaticAnimation
import cc.lib.zombicide.anims.ThrowAnimation
import cc.lib.zombicide.anims.ZoomAnimation
import cc.lib.zombicide.p2p.ZGameMP
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ConnectedUser(
	val name: String = "",
	val color: GColor = GColor.BLACK,
	val connected: Boolean = false,
	val status: ConnectionStatus = ConnectionStatus.UNKNOWN,
	val startUser: Boolean = false
) : Reflector<ConnectedUser>() {
	companion object {
		init {
			addAllFields(ConnectedUser::class.java)
		}
	}

	override fun toString(): String {
		return "ConnectedUser color: ${ZUser.getColorName(color)} connected: $connected name:$name status:$status start:$startUser"
	}
}

abstract class UIZombicide(
	val characterRenderer: UIZCharacterRenderer,
	val boardRenderer: UIZBoardRenderer
) : ZGameMP(), UIZBoardRenderer.Listener {
	enum class UIMode {
		NONE,
		PICK_CHARACTER,
		PICK_ZONE,
		PICK_SPAWN,
		PICK_DOOR,
		PICK_MENU,
		PICK_ZOMBIE,
	}

	init {
		boardRenderer.addListener(this)
		instance = this
	}

	private var gameRunning = false

	open fun isGameRunning(): Boolean = gameRunning || client?.isConnected == true

	var uiMode = UIMode.NONE
		private set
	var boardMessage: String? = null
		protected set(msg) {
			field = msg
			boardRenderer.boardMessage = msg
		}

	open val connectedUsersInfo = listOf<ConnectedUser>()

	val synchronizeLock = Object()

	var options: List<Any> = ArrayList()
		private set

	var buttonRoot: UIZButton? = null

	suspend fun setOptions(mode: UIMode, options: List<Any>) {
		uiMode = mode
		this.options = options
		buttonRoot?.clearTree()
		buttonRoot = null
		/*
				log.debug("options: " + options.joinToString {
					if (it is ZZone) {
						"""${it.cells.joinToString()}
						${it.X()}, ${it.Y()}, ${it.width}, ${it.height}
						""".trimIndent()
					} else {
						toString()
					}
				})*/

//		boardRenderer.waitForAnimations()
		boardRenderer.pushZoomRect()
		boardRenderer.pickableStack.clear()

		when (uiMode) {
			UIMode.PICK_ZOMBIE,
			UIMode.PICK_CHARACTER,
			UIMode.PICK_ZONE,
			UIMode.PICK_DOOR,
			UIMode.PICK_SPAWN -> {
				boardRenderer.pickableStack.push(options as List<UIZButton>)
			}

			UIMode.PICK_MENU -> {
				buttonRoot = processMoves(options.filterIsInstance<ZMove>()).takeIf { it.first != null }?.also {
					boardRenderer.pickableStack.push(it.getChildren())
				}

			}

			else -> focusOnMainMenu()
		}

		boardRenderer.redraw()
	}

	internal class BoardButton(val move: ZMove) : UIZButton() {

		var rectangle = GRectangle()

		override val resultObject = move

		override fun onAttached(parent: UIZButton) {
			rectangle.setPosition(parent.getRect().topRight)
		}

		override fun draw(g: AGraphics, game: ZGame, selected: Boolean) {
			g.color = test(selected, GColor.RED, GColor.YELLOW)
			g.drawJustifiedString(getRect().topRight, Justify.RIGHT, move.getLabel())
		}

		override fun getRect(): IRectangle = rectangle
	}

	fun processMoves(options: List<ZMove>): UIZButton {

		val root: UIZButton = object : UIZButton() {}

		fun addButton(zone: ZZone, move: ZMove) {
			if (zone.parent == null)
				root.addChild(zone)
			zone.addChild(BoardButton(move))
		}

		fun addButton(character: ZCharacter, move: ZMove) {
			if (character == currentCharacter) {
				addButton(board.getZone(currentCharacter!!.occupiedZone), move)
				return
			} else if (character.parent == null) {
				board.getZone(character.occupiedZone).addChild(character)
			}
			character.addChild(BoardButton(move))
		}

		fun addButton(door: ZDoor, move: ZMove) {
			if (door.parent == null) {
				val cellPos = test(
					requireCurrentCharacter.occupiedZone == board.getZone(door.cellPosStart)?.zoneIndex,
					door.cellPosStart, door.cellPosEnd
				)
				board.getZone(cellPos)?.addChild(door)
			}
			door.addChild(BoardButton(move))
		}

		for (move in options) {
			when (move.type) {
				ZMoveType.TRADE -> move.list?.map { board.getCharacter(it as ZPlayerName) }
					?.forEach { c: ZCharacter ->
						addButton(c, ZMove(move, c, "Trade ${c.getLabel()}"))
					}

				ZMoveType.WALK -> move.list?.forEachAs { zoneIdx: Int ->
					addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, zoneIdx))
				}

				ZMoveType.MELEE_ATTACK, ZMoveType.RANGED_ATTACK, ZMoveType.MAGIC_ATTACK -> move.list?.forEachAs { w: ZWeapon ->
					for (stat in w.type.stats.filter { it.actionType == move.action }) {
						for (zoneIdx in board.getAccessibleZones(
							requireCurrentCharacter,
							stat.minRange,
							stat.maxRange,
							stat.actionType
						)) {
							addButton(board.getZone(zoneIdx), ZMove(move, w, zoneIdx, "${move.type.shortName} ${w.getLabel()}"))
						}
					}
				}

				ZMoveType.THROW_ITEM -> {
					val zones = board.getAccessibleZones(requireCurrentCharacter, 0, 1, ZActionType.THROW_ITEM)
					move.list?.forEachAs { item: ZEquipment<*> ->
						for (zoneIdx in zones) {
							addButton(board.getZone(zoneIdx), ZMove(move, item, zoneIdx, "Throw ${item.getLabel()}"))
						}
					}
				}

				ZMoveType.OPERATE_DOOR -> move.list?.forEachAs { door: ZDoor ->
					addButton(
						door, ZMove(
							move, door,
							if (door.isClosed(board))
								"Open"
							else
								"Close"
						)
					)
				}

				ZMoveType.BARRICADE -> move.list?.forEachAs { door: ZDoor ->
					addButton(door, ZMove(move, door, "Barricade"))
				}
				//ZMoveType.SEARCH, ZMoveType.CONSUME, ZMoveType.EQUIP, ZMoveType.UNEQUIP, ZMoveType.GIVE, ZMoveType.TAKE, ZMoveType.DISPOSE -> addClickable(cur.getRect(), move)
				ZMoveType.TAKE_OBJECTIVE -> board.getZone(requireCurrentCharacter.occupiedZone).addChild(BoardButton(move))
				//ZMoveType.DROP_ITEM -> move.list?.forEachAs { e :ZEquipment<*> ->
				//	addClickable(cur.getRect(), ZMove(move, e, "Drop ${e.label}"))
				//}
				ZMoveType.PICKUP_ITEM -> move.list?.forEachAs { e: ZEquipment<*> ->
					addButton(requireCurrentCharacter, ZMove(move, e, "Pickup ${e.getLabel()}"))
				}

				ZMoveType.SHOVE -> move.list?.forEachAs { zoneIdx: Int ->
					addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, "Shove"))
				}

				ZMoveType.CHARGE -> move.list?.forEachAs { zoneIdx: Int ->
					addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, "Charge"))
				}

				ZMoveType.BORN_LEADER, // Born leader can be a spell?
				ZMoveType.WALK_DIR,
				ZMoveType.USE_SLOT -> Unit

				ZMoveType.ENCHANT -> {
					board.getAllCharacters().filter {
						it.isAlive && board.canSee(requireCurrentCharacter.occupiedZone, it.occupiedZone)
					}.forEach { c ->
						move.list?.forEachAs { spell: ZSpell ->
							addButton(c, ZMove(move, spell, c.type, spell.getLabel()))
						}
					}
				}
//				ZMoveType.BORN_LEADER -> for (c in (move.list as List<ZCharacter>)) {
//					addClickable(c.getRect(), ZMove(move, c.type, c.type, "))
//				}
				ZMoveType.BLOODLUST_MELEE -> for (w in requireCurrentCharacter.meleeWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.getLabel()}"))
					}
				}

				ZMoveType.BLOODLUST_RANGED -> for (w in requireCurrentCharacter.rangedWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.getLabel()}"))
					}
				}

				ZMoveType.BLOODLUST_MAGIC -> for (w in requireCurrentCharacter.magicWeapons) {
					move.list?.forEachAs { zoneIdx: Int ->
						addButton(board.getZone(zoneIdx), ZMove(move, zoneIdx, w, "Bloodlust ${w.getLabel()}"))
					}
				}

				else -> addButton(requireCurrentCharacter, move)

			}
		}

		return root
	}

	private var result: Any? = null
	abstract val thisUser: ZUser

	abstract fun focusOnMainMenu()

	abstract fun focusOnBoard()

	fun refresh() {
		boardRenderer.board = board
		boardRenderer.redraw()
		characterRenderer.redraw()
	}

	override fun loadQuest(newQuest: ZQuests) {
		super.loadQuest(newQuest)
		boardRenderer.board = board
	}

	fun addPlayerComponentMessage(message: String) {
		characterRenderer.addMessage(message)
	}

	fun stopGameThread() {
		boardRenderer.stopAnimations()
		gameRunning = false
		setResult(null)
	}

	fun isReady(): Boolean = allCharacters.firstOrNull { !it.isReady } == null

	fun startGameThread() {
		if (isGameRunning())
			return
		val gameDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
		gameRunning = true
		readyLock.reset()
		launchIn(gameDispatcher) {
			while (!isReady()) {
				setBoardMessage(-1, "Not Ready")
				readyLock.acquireAndBlock()
			}
			characterRenderer.clearMessages()
			try {
				while (gameRunning && !isGameOver) {
					boardRenderer.setCurrentCharacter(currentCharacter)
					characterRenderer.actorInfo = currentCharacter
					runGame()
				}
			} catch (e: Throwable) {
				log.error(e)
				e.printStackTrace()
			}
			log.debug("Game thread stopped")
		}
	}

	abstract fun undo()

	val readyLock = KLock()
	var continuation: Continuation<Any?>? = null

	open suspend fun <T> waitForUser(expectedType: Class<T>): T? {
		log.debug("waitForUser type: ${expectedType.simpleName}")
		// TODO: Put suspend back in when we have migrated network code to kotlin so we can call invokeSuspend
		result = suspendCoroutine {
			continuation = it
		}
		log.debug("waitForUser resumed result: $result")
		uiMode = UIMode.NONE
		result?.let {
			if (expectedType.isAssignableFrom(it.javaClass))
				return it as T
		}
		return null
	}

	open fun setResult(result: Any?) {
		log.debug("set result $result")
		continuation?.let {
			if (result != null) {
				boardRenderer.setOverlay(null)
			}
			launchIn {
				boardRenderer.popAllZoomRects()
			}
			boardRenderer.pickableStack.clear()
			this.result = result
			it.resume(result)
			continuation = null
			refresh()
		} ?: log.error("continuation is null")
	}

	override suspend fun pushState(state: State) {
		if (isGameRunning())
			super.pushState(state)
	}

	override suspend fun onCurrentUserUpdated(userName: String, colorId: Int) {
		super.onCurrentUserUpdated(userName, colorId)
		boardMessage = "$userName's Turn"
	}

	suspend fun pickCharacter(
		name: ZPlayerName?,
		message: String,
		characters: List<ZPlayerName>
	): ZPlayerName? {
		name?.toCharacter().apply {
			boardRenderer.setCurrentCharacter(this)
			characterRenderer.actorInfo = this
		}
		setOptions(UIMode.PICK_CHARACTER, characters.map { it.toCharacter() })
		boardMessage = message
		return waitForUser(ZCharacter::class.java)?.type
	}

	suspend fun pickZone(name: ZPlayerName, message: String, zones: List<Int>): Int? {
		name.toCharacter().apply {
			boardRenderer.setCurrentCharacter(this)
			characterRenderer.actorInfo = this
		}
		setOptions(UIMode.PICK_ZONE, zones.map { board.getZone(it) })
		boardMessage = message
		return waitForUser(Int::class.javaObjectType)
	}

	suspend fun pickSpawn(name: ZPlayerName, message: String, areas: List<ZSpawnArea>): Int? {
		name.toCharacter().apply {
			boardRenderer.setCurrentCharacter(this)
			characterRenderer.actorInfo = this
		}
		val _areas = areas.map { spawn ->
			board.getCell(spawn.cellPos).spawns.firstOrNull { it?.dir == spawn.dir }
		}.filterNotNull()
		setOptions(UIMode.PICK_SPAWN, _areas)
		boardMessage = message
		val area = waitForUser(ZSpawnArea::class.java) ?: return null
		return areas.indexOf(area)
	}

	suspend fun <T : IButton> pickMenu(
		name: ZPlayerName,
		message: String,
		expectedType: Class<T>,
		moves: List<T>
	): T? {
		name.toCharacter().apply {
			boardRenderer.setCurrentCharacter(this)
			characterRenderer.actorInfo = this
		}
		setOptions(UIMode.PICK_MENU, moves)
		boardMessage = message
		return waitForUser(expectedType)
	}

	suspend fun pickDoor(name: ZPlayerName, message: String, doors: List<ZDoor>): ZDoor? {
		name.toCharacter().apply {
			boardRenderer.setCurrentCharacter(this)
			characterRenderer.actorInfo = this
		}
		setOptions(UIMode.PICK_DOOR, doors)
		boardMessage = message
		return waitForUser(ZDoor::class.java)
	}

	open suspend fun showOrganizeDialog(primary: ZPlayerName, secondary: ZPlayerName?, undos: Int) {
		throw NotImplementedError()
	}

	open suspend fun closeOrganizeDialog() {
		throw NotImplementedError()
	}

	open suspend fun updateOrganize(character: ZCharacter, list: List<ZMove>, undos: Int): ZMove? {
		throw NotImplementedError()
	}

	override suspend fun addLogMessage(msg: String) {
		super.addLogMessage(msg)
		addPlayerComponentMessage(msg)
	}

	fun showObjectivesOverlay() {
		boardRenderer.hideOrSetOverlay(quest.getObjectivesOverlay(this))
	}

	fun <T : ZEquipment<*>> showEquipmentOverlay(player: ZPlayerName, list: List<T>) {
		val table = Table(object : Table.Model {
			override fun getMaxCharsPerLine(): Int {
				return 32
			}
		})
		for (t in list) {
			table.addColumnNoHeaderVarArg(t.getCardInfo(player.toCharacter(), this))
		}
		boardRenderer.setOverlay(table)
	}

	fun showQuestTitleOverlay() {
		boardRenderer.setOverlay(object : OverlayTextAnimation(quest.name, boardRenderer.numOverlayTextAnimations) {
			override fun onDone() {
				showObjectivesOverlay()
			}
		})
	}

	open fun playSound(sound: ZSound) {}

	fun showSummaryOverlay() {
		boardRenderer.hideOrSetOverlay(gameSummaryTable)
	}

	override fun initQuest(quest: ZQuest) {
		boardRenderer.quest = quest
		refresh()
	}

	private fun setCharacterSkillsOverlay(c: ZCharacter) {
		val table = Table().addColumn("Skills", c.getSkillsTable().setNoBorder())
			.addColumn("Backpack", c.getBackpackTable(this).setNoBorder())

		boardRenderer.setOverlay(table)
	}

	fun showCharacterExpandedOverlay() {
		characterRenderer.actorInfo?.takeIfInstance<ZCharacter>()?.let {
			setCharacterSkillsOverlay(it)
		} ?: boardRenderer.highlightedResult?.takeIfInstance<ZCharacter>()?.let {
			setCharacterSkillsOverlay(it)
		}
	}

	override fun onActorHighlighted(actor: ZActor?) {
		actor?.let {
			characterRenderer.actorInfo = it
		}
	}

	override suspend fun onEquipmentThrown(actor: ZPlayerName, icon: ZIcon, zone: Int) {
		super.onEquipmentThrown(actor, icon, zone)
		actor.toCharacter().takeIf { it.occupiedZone != zone }?.let { actor ->
			actor.addAnimation(ThrowAnimation(actor, board.getZone(zone).getRect().center, icon))
			boardRenderer.waitForAnimations()
		}
	}

	override suspend fun onRollDice(roll: Array<Int>) {
		super.onRollDice(roll)
		characterRenderer.addWrapped(ZDiceWrapped(roll))
	}

	override suspend fun onDragonBileExploded(zone: Int) {
		super.onDragonBileExploded(zone)
		board.getZone(zone).getCells().map { board.getCell(it) }.apply {
			boardRenderer.addPreActor(InfernoAnimation(this))
		}
		boardRenderer.wait(1000)
	}

	override suspend fun onZombieDestroyed(deathType: ZAttackType, pos: ZActorPosition) {
		super.onZombieDestroyed(deathType, pos)
		val zombie = board.getActor(pos)
		zombie.addAnimation(DeathAnimation(zombie))
	}

	override suspend fun onActorMoved(id: String, start: GRectangle, end: GRectangle, speed: Long) {
		super.onActorMoved(id, start, end, speed)
		board.getActor(id)?.let {
/*			it.addAnimation(object : EmptyAnimation(it, 1) {
				init {
					rect = start
				}
			})*/
			if (it is ZCharacter) {
				boardRenderer.animateZoomToIfNotContained(start, end)
				// animate the window to keep the player on the screen
				boardRenderer.animateZoomDelta(end.center.sub(start.center), it.moveSpeed + 100)
				it.addAnimation(MoveAnimation(it, start, end, speed))
				boardRenderer.waitForAnimations()
			} else {
				it.addAnimation(MoveAnimation(it, start, end, speed))
			}
		}
	}

	override suspend fun onZombieSpawned(zombie: ZZombie) {
		log.debug("onSpawnZoneSpawned <<<<< ----")
		super.onZombieSpawned(zombie)

		zombie.addAnimation(SpawnAnimation(zombie, board))
		when (zombie.type) {
			ZZombieType.Abomination -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"A B O M I N A T I O N ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
			}

			ZZombieType.OrcNecromancer,
			ZZombieType.Necromancer -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"N E C R O M A N C E R ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
				animateNecromancerEscapeRoute(zombie)
			}

			ZZombieType.RatKing -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"R A T   K I N G ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
				animateNecromancerEscapeRoute(zombie)
			}

			ZZombieType.SwampTroll -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"S W A M P   T R O L L ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
			}

			ZZombieType.NecromanticDragon -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"N E C R O M A N T I C\nD R A G O N ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
			}

			ZZombieType.Wolfbomination -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"W O L F B O M I N A T I O N ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
			}

			ZZombieType.LordOfSkulls -> {
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"L O R D   O F   S K U L L S ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"G L O O M ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
				boardRenderer.addOverlay(
					OverlayTextAnimation(
						"D O O M ! !",
						boardRenderer.numOverlayTextAnimations
					)
				)
				boardRenderer.wait(500)
			}

			else -> Unit
		}
		boardRenderer.redraw()
	}

	private suspend fun animateNecromancerEscapeRoute(necro: ZZombie) {
		board.getZombiePathTowardNearestSpawn(necro).takeIf { it.isNotEmpty() }?.let { path ->
			necro.escapeZone?.let { escapeZone ->
				boardRenderer.pushZoomRect()
				val rect = GRectangle(necro.getRect())
				path.forEach {
					rect.addEq(it.dv, 1f, 1f)
				}
				rect.addEq(board.getCell(escapeZone.cellPos))
				boardRenderer.animateZoomTo(rect)
				boardRenderer.waitForAnimations()
				boardRenderer.addPostActor(object : ZAnimation(500, 6, true) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						g.color = GColor.YELLOW.withAlpha(position)
						boardRenderer.drawPath(g, necro, path)
						g.pushMatrix()
						g.scale(1.1f + position * .1f)
						escapeZone.drawOutlined(g)
						g.popMatrix()
					}
				})
				boardRenderer.waitForAnimations()
				boardRenderer.popZoomRect()
			}
		}
	}

	override suspend fun onCharacterDefends(cur: ZPlayerName, attackerPosition: ZActorPosition) {
		super.onCharacterDefends(cur, attackerPosition)
		val actor = board.getActor(attackerPosition)
		actor.addAnimation(ShieldBlockAnimation(cur.toCharacter()))
		boardRenderer.redraw()
	}

	override suspend fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, character: ZCharacter?) {
		super.onCurrentCharacterUpdated(priorPlayer, character)
		priorPlayer?.toCharacter()?.let { player ->
			player.addAnimation(EmptyAnimation(player))
			boardRenderer.waitForAnimations()
		}
		boardRenderer.setCurrentCharacter(character)
		characterRenderer.actorInfo = character
		characterRenderer.redraw()
	}

	open inner class ChargeAttackAnimation(source: ZActor, dest: ZActor) : ZActorAnimation(source, 150L, 600L, 0L) {

		val dv = dest.getRect().center.sub(source.getRect().center).scaledBy(.75f)

		override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
			when (phase) {
				0 -> {
					val rect = GRectangle(actor.getRect()).moveBy(dv.scaledBy(positionInPhase))
					g.drawImage(actor.imageId, rect)
				}
				1 -> {
					val rect = GRectangle(actor.getRect()).moveBy(dv.scaledBy(1f - positionInPhase))
					g.drawImage(actor.imageId, rect)
				}
			}
		}
	}

	override suspend fun onZombieAttack(zombiePos: ZActorPosition, victim: ZPlayerName, type: ZActionType) {
		super.onZombieAttack(zombiePos, victim, type)
		boardRenderer.waitForAnimations()
		val zombie = board.getActor(zombiePos)
		boardRenderer.animateZoomTo(zombie.getRect(board))
		boardRenderer.waitForAnimations()
		when (type) {
			ZActionType.MELEE -> {
				zombie.addAnimation(ChargeAttackAnimation(zombie, victim.toCharacter()))
			}

			else -> Unit
		}
		boardRenderer.redraw()
		boardRenderer.waitForAnimations()
	}

	override suspend fun onNothingInSight(zone: Int) {
		super.onNothingInSight(zone)
		boardRenderer.addHoverMessage("Nothing In Sight", board.getZone(zone))
	}

	override suspend fun onSpawnZoneSpawning(rect: GRectangle, nth: Int, num: Int) {
		super.onSpawnZoneSpawning(rect, nth, num)
		if (nth == 0) {
			boardRenderer.pushZoomRect()
		} else {
			boardRenderer.waitForAnimations()
		}
		boardRenderer.animateZoomTo(rect)
		boardRenderer.waitForAnimations()
	}

	override suspend fun onSpawnZoneSpawned(nth: Int, num: Int) {
		super.onSpawnZoneSpawned(nth, num)
		boardRenderer.waitForAnimations()
		if (nth == num - 1)
			boardRenderer.popZoomRect()
		boardRenderer.setOverlay(null)
	}

	override suspend fun onCharacterAttacked(
		character: ZPlayerName,
		attackerPosition: ZActorPosition,
		attackType: ZAttackType,
		perished: Boolean
	) {
		super.onCharacterAttacked(character, attackerPosition, attackType, perished)
		val attacker = board.getActor(attackerPosition)
		val ch = character.toCharacter()
		when (attackType) {
			ZAttackType.ELECTROCUTION -> attacker.addAnimation(ElectrocutionAnimation(ch))
			/*
			ZAttackType.NORMAL,
			ZAttackType.FIRE,
			ZAttackType.DISINTEGRATION,
			ZAttackType.BLADE,
			ZAttackType.CRUSH,
			ZAttackType.RANGED_ARROWS,
			ZAttackType.RANGED_BOLTS,
			ZAttackType.RANGED_THROW,*/
			ZAttackType.EARTHQUAKE,
			ZAttackType.MENTAL_STRIKE -> attacker.addAnimation(EarthquakeAnimation(ch, 400))
			else -> attacker.addAnimation(SlashedAnimation(ch))
		}
		if (perished) {
			with(ch) {
				val group = GroupAnimation(this, true)
				group.addSequentially(AscendingAngelDeathAnimation(this))
				group.addSequentially(object : ZActorAnimation(this, 2000) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						val img = g.getImage(ZIcon.GRAVESTONE.imageIds[0])
						val rect = GRectangle(actor.getRect().fit(img))
						rect.y += rect.h * (1f - position)
						rect.h *= position
						g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect)
					}
				})
				addAnimation(group)
			}
			boardRenderer.waitForAnimations()
		}
		boardRenderer.redraw()
	}

	override suspend fun onAhhhhhh(c: ZPlayerName) {
		super.onAhhhhhh(c)
		boardRenderer.addHoverMessage("AHHHHHH!", c.toCharacter())
	}

	override suspend fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
		super.onEquipmentFound(c, equipment)
		if (thisUser.hasPlayer(c)) {
			val info = Table().setModel(object : Table.Model {
				override fun getCornerRadius(): Float {
					return 20f
				}

				override fun getBackgroundColor(): GColor {
					return GColor.TRANSLUSCENT_BLACK
				}
			})
			info.addRowList(equipment.map { e: ZEquipment<*> -> e.getCardInfo(c.toCharacter(), this) })
			boardRenderer.setOverlay(info)
		} else {
			for (e in equipment) {
				boardRenderer.addHoverMessage("+" + e.getLabel(), c.toCharacter())
			}
		}
	}

	override suspend fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
		super.onCharacterGainedExperience(c, points)
		boardRenderer.addHoverMessage(String.format("+%d EXP", points), c.toCharacter())
	}

	override suspend fun onGameLost() {
		super.onGameLost()
		boardMessage = "GAME LOST"
		boardRenderer.waitForAnimations()
		boardRenderer.addOverlay(object : OverlayTextAnimation("Y O U   L O S T", boardRenderer.numOverlayTextAnimations) {
			override fun onDone() {
				showSummaryOverlay()
			}
		})
		boardRenderer.waitForAnimations()
	}

	override suspend fun onQuestComplete() {
		super.onQuestComplete()
		boardMessage = "QUEST COMPLETED"
		boardRenderer.waitForAnimations()
		boardRenderer.addOverlay(object : OverlayTextAnimation("C O M P L E T E D", 0) {
			override fun onDone() {
				showSummaryOverlay()
			}
		})
		boardRenderer.waitForAnimations()
	}

	override suspend fun onDoubleSpawn(multiplier: Int) {
		super.onDoubleSpawn(multiplier)
		boardRenderer.addOverlay(
			OverlayTextAnimation(
				String.format(
					"DOUBLE SPAWN X %d",
					multiplier
				), boardRenderer.numOverlayTextAnimations
			)
		)
		boardRenderer.wait(500)
	}

	override suspend fun onNewSkillAcquired(c: ZPlayerName, skill: ZSkill) {
		super.onNewSkillAcquired(c, skill)
		boardRenderer.addHoverMessage(String.format("%s Acquired", skill.getLabel()), c.toCharacter())
		characterRenderer.addMessage(String.format("%s has acquired the %s skill", c.getLabel(), skill.getLabel()))
	}

	override suspend fun onExtraActivation(category: ZZombieCategory) {
		super.onExtraActivation(category)
		boardRenderer.addOverlay(
			OverlayTextAnimation(
				String.format(
					"EXTRA ACTIVATION %s",
					category.prettify()
				), boardRenderer.numOverlayTextAnimations
			)
		)
		boardRenderer.wait(500)
	}

	override suspend fun onSkillKill(c: ZPlayerName, skill: ZSkill, zombiePostion: ZActorPosition, attackType: ZAttackType) {
		super.onSkillKill(c, skill, zombiePostion, attackType)
		boardRenderer.addHoverMessage(String.format("%s Kill!!", skill.getLabel()), board.getActor(zombiePostion))
	}

	override suspend fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
		super.onRollSixApplied(c, skill)
		boardRenderer.addHoverMessage(String.format("Roll Six!! %s", skill.getLabel()), c.toCharacter())
	}

	override suspend fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
		super.onWeaponReloaded(c, w)
		boardRenderer.addHoverMessage(String.format("%s Reloaded", w.getLabel()), c.toCharacter())
	}

	override suspend fun onNoiseAdded(zoneIndex: Int) {
		super.onNoiseAdded(zoneIndex)
		val zone = board.getZone(zoneIndex)
		boardRenderer.animateZoomToIfNotContained(zone)
		boardRenderer.addPreActor(MakeNoiseAnimation(zone.center))
		boardRenderer.waitForAnimations()
	}

	override suspend fun onBeginRound(roundNum: Int) {
		super.onBeginRound(roundNum)
		if (roundNum == 0)
			showQuestTitleOverlay()
	}

	override suspend fun onSpawnCard(card: ZSpawnCard, color: ZColor) {
		super.onSpawnCard(card, color)
		//boardRenderer.setOverlay(card.toTable(color))
	}

	private suspend fun onAttackMelee(
		attacker: ZCharacter,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		hits: List<ZActorPosition>,
		targetZone: Int
	) {
		when (weapon) {
			ZWeaponType.EARTHQUAKE_HAMMER -> {
				val animLock = Lock(numDice)
				val currentZoom = boardRenderer.getZoomedRect()
				attacker.addAnimation(EmptyAnimation(attacker, 500))
				boardRenderer.addPreActor(ZoomAnimation(attacker.getRect(board), boardRenderer))
				var i = 0
				while (i < numDice) {
					attacker.addAnimation(object : MeleeAnimation(attacker, board) {
						override fun onDone() {
							animLock.release()
						}
					})
					val g = GroupAnimation(attacker)
					for (pos in hits) {
						val z = board.getActor(pos)
						if (pos.data == ACTOR_POS_DATA_DAMAGED)
							g.addAnimation(0, EarthquakeAnimation(z, attacker, 300))
						else
							g.addAnimation(0, ShieldBlockAnimation(z))
					}
					attacker.addAnimation(g)
					i++
				}
				boardRenderer.redraw()
				animLock.block()
				boardRenderer.animateZoomTo(currentZoom)
			}
			else -> {
				boardRenderer.pushZoomRect()
				boardRenderer.animateZoomToIfNotContained(board.getZone(attacker.occupiedZone))
				for (i in 0 until numDice) {
					if (i < hits.size) {
						val pos = hits[i]
						val victim = board.getActor(pos)
						require(victim !== attacker)
						playSound(ZSound.SWORD_SLASH)
						attacker.addAnimation(object : MeleeAnimation(attacker, board) {
							override fun onDone() {
								if (pos.data == ACTOR_POS_DATA_DEFENDED) {
									victim.addAnimation(ShieldBlockAnimation(victim))
								} else {
									victim.addAnimation(SlashedAnimation(victim))
								}
							}
						})
					} else {
						playSound(ZSound.SWORD_SLASH)
						attacker.addAnimation(object : MeleeAnimation(attacker, board) {
							override fun onDone() {
								boardRenderer.addHoverMessage("MISS!!", attacker)
							}
						})
					}
				}
				boardRenderer.waitForAnimations()
				boardRenderer.popZoomRect()
			}
		}


	}

	private suspend fun onAttackRanged(
		attacker: ZCharacter,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		hits: List<ZActorPosition>,
		targetZone: Int
	) {
		when (weapon) {
			ZWeaponType.MJOLNIR -> {
				val hammerSpeed = 600L
				val animLock = Lock()
				if (hits.isEmpty()) {
					animLock.acquire()
					attacker.addAnimation(object : ShootAnimation(
						attacker,
						hammerSpeed,
						board.getZone(targetZone).getRandomPointInside(),
						ZIcon.MJOLNIR
					) {
						override fun onDone() {
							animLock.release()
						}
					}.setOscillating<ShootAnimation>(true).setRepeats(1))
				} else {
					val group = GroupAnimation(attacker)
					var prev: ZActor = attacker
					hits.forEach {
						val victim = board.getActor(it)
						animLock.acquire()
						group.addSequentially(object : ShootAnimation(prev, hammerSpeed, victim.center, ZIcon.MJOLNIR) {
							override fun onDone() {
								animLock.release()
							}
						})
						prev = victim
					}
					animLock.acquire()
					group.addSequentially(object : ShootAnimation(prev, hammerSpeed, attacker.center, ZIcon.MJOLNIR) {
						override fun onDone() {
							super.onDone()
							animLock.release()
						}
					})
					attacker.addAnimation(group)
				}
				boardRenderer.redraw()
				animLock.block()
			}

			ZWeaponType.SCATTERSHOT -> doThrowAnimation(
				attacker,
				numDice,
				hits,
				targetZone,
				ZIcon.BOULDER,
				.5f,
				.2f,
				700
			)

			ZWeaponType.GRAPESHOT -> doThrowAnimation(
				attacker,
				numDice,
				hits,
				targetZone,
				ZIcon.BOULDER,
				1f,
				.4f,
				1000
			)

			ZWeaponType.BOULDER -> doThrowAnimation(
				attacker,
				numDice,
				hits,
				targetZone,
				ZIcon.BOULDER,
				1.5f,
				.5f,
				1500
			)

			ZWeaponType.DAGGER -> doThrowAnimation(
				attacker,
				numDice,
				hits,
				targetZone,
				ZIcon.DAGGER,
				.4f,
				.1f,
				400
			)

			else -> {
				val group = GroupAnimation(attacker)
				val animLock = Lock(numDice)
				var delay = 0
				var i = 0
				while (i < numDice) {
					if (i < hits.size) {
						val pos = hits[i]
						val victim = board.getActor(pos)
						group.addAnimation(
							delay,
							object : ShootAnimation(attacker, 300, victim.center, ZIcon.ARROW) {
								override fun onDone() {
									super.onDone()
									val arrowId = ZIcon.ARROW.imageIds[dir.ordinal]
									if (pos.data == ACTOR_POS_DATA_DEFENDED) {
										victim.addAnimation(
											GroupAnimation(victim)
												.addAnimation(ShieldBlockAnimation(victim))
												.addAnimation(
													DeflectionAnimation(
														victim,
														arrowId,
														dir.opposite
													)
												)
										)
									} else {
										victim.addAnimation(
											StaticAnimation(
												victim,
												800,
												arrowId,
												r,
												true
											)
										)
									}
									animLock.release()
								}
							})
					} else if (actionType != ZActionType.CATAPULT_FIRE) {
						val center: IVector2D = board.getZone(targetZone).getRandomPointInside()
						group.addAnimation(
							delay,
							object : ShootAnimation(attacker, 300, center, ZIcon.ARROW) {
								override fun onDone() {
									super.onDone()
									boardRenderer.addHoverMessage("MISS!!", attacker)
									animLock.release()
								}
							})
					}
					delay += 100
					i++
				}
				attacker.addAnimation(group)
				boardRenderer.redraw()
				animLock.block()
			}
		}

	}

	private suspend fun doThrowAnimation(
		attacker: ZActor,
		numDice: Int,
		hits: List<ZActorPosition>,
		targetZone: Int,
		icon: ZIcon,
		scale: Float,
		arc: Float,
		duration: Long
	) {
		val group = GroupAnimation(attacker)
		var delay = 200
		var i = 0
		while (i < numDice) {
			if (i < hits.size) {
				val pos = hits[i]
				val victim = board.getActor(pos)
				group.addAnimation(
					delay,
					object : ThrowAnimation(attacker, victim.center, icon, arc, duration, scale) {
						override fun onDone() {
							super.onDone()
							if (pos.data == ACTOR_POS_DATA_DEFENDED) {
								victim.addAnimation(
									GroupAnimation(victim)
										.addAnimation(ShieldBlockAnimation(victim))
										.addAnimation(
											DeflectionAnimation(
												victim,
												Utils.randItem(icon.imageIds),
												dir.opposite
											)
										)
								)
							} else {
								victim.addAnimation(SlashedAnimation(victim))
							}
						}
					})
			} else {
				val center: IVector2D = board.getZone(targetZone).getRandomPointInside()
				group.addAnimation(
					delay,
					object : ThrowAnimation(attacker, center, icon, arc, duration, scale) {
						override fun onDone() {
							super.onDone()
							boardRenderer.addHoverMessage("MISS!!", attacker)
						}
					})
			}
			delay += 200
			i++
		}
		attacker.addAnimation(group)
		boardRenderer.waitForAnimations()
	}

	fun Pos.toRect() = GRectangle(column.toFloat(), row.toFloat(), 1f, 1f)

	private suspend fun onAttackMagic(
		attacker: ZCharacter,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		_hits: List<ZActorPosition>,
		targetZone: Int
	) {
		val hits = _hits.toMutableList()
		when (weapon) {
			ZWeaponType.DEATH_STRIKE -> {
				val animLock = Lock(1)
				val targetRects = mutableListOf<GRectangle>()
				repeat(numDice) { index ->
					targetRects.add(
						_hits.getOrNull(index)?.toRect(board) ?: board.getZone(targetZone).cells.random().toRect().scaledBy(.5f)
					)
				}
				attacker.addAnimation(object : DeathStrikeAnimation(attacker, targetRects) {
					override fun onDone() {
						super.onDone()
						animLock.release()
					}
				})
				boardRenderer.redraw()
				animLock.block()
			}
			ZWeaponType.MANA_BLAST, ZWeaponType.DISINTEGRATE -> {

				// TODO: Disintegrate should look meaner than mana blast
				attacker.addAnimation(MagicOrbAnimation(attacker, board.getZone(targetZone).center))
				boardRenderer.waitForAnimations()
			}
			ZWeaponType.FIREBALL -> {
				val group = GroupAnimation(attacker)
				val animLock = Lock(numDice)
				var delay = 0
				var i = 0
				while (i < numDice) {
					if (hits.size > 0) {
						val pos: ZActorPosition = hits.removeAt(0)
						val victim = board.getActor(pos)
						group.addAnimation(delay, object : FireballAnimation(attacker, victim.center) {
							override fun onDone() {
								super.onDone()
								if (pos.data == ACTOR_POS_DATA_DEFENDED) {
									victim.addAnimation(ShieldBlockAnimation(victim))
								} else {
									boardRenderer.addPostActor(InfernoAnimation(victim.getRect()))
								}
								animLock.release()
							}
						})
					} else {
						val end: Vector2D = board.getZone(targetZone).center.add(Vector2D.newRandom(0.3f))
						group.addAnimation(delay, object : FireballAnimation(attacker, end) {
							override fun onDone() {
								super.onDone()
								boardRenderer.addHoverMessage("MISS!!", attacker)
								animLock.release()
							}
						})
					}
					delay += 150
					i++
				}
				attacker.addAnimation(group)
				boardRenderer.redraw()
				animLock.block()
			}
			ZWeaponType.INFERNO -> {
				val lock = Lock(1)
				val rects =
					Utils.map<Pos, IRectangle>(board.getZone(targetZone).getCells()) { pos: Pos? -> board.getCell(pos!!) }
				boardRenderer.addPreActor(object : InfernoAnimation(rects) {
					override fun onDone() {
						super.onDone()
						lock.release()
					}
				})
				boardRenderer.redraw()
				lock.block()
			}
			ZWeaponType.MJOLNIR,
			ZWeaponType.LIGHTNING_BOLT -> {
				val animLock1 = Lock(1)
				val animLock2 = Lock(1)
				val targets: MutableList<IInterpolator<Vector2D>> = ArrayList()
				var i = 0
				while (i < numDice) {
					if (i < hits.size) {
						val actorRect = (board.getActor(hits[i])).getRect().scaledBy(.5f)
						targets.add(Vector2D.getLinearInterpolator(actorRect.randomPointInside, actorRect.randomPointInside))
					} else {
						board.getZone(targetZone).apply {
							targets.add(Vector2D.getLinearInterpolator(getRandomPointInside(.5f), getRandomPointInside(.5f)))
						}
					}
					i++
				}
				val dir = ZDir.getDirFrom(attacker.occupiedCell, board.getZone(targetZone).cells[0])?:ZDir.NORTH
				when (weapon) {
					ZWeaponType.MJOLNIR -> attacker.addAnimation(object :
						MjolnirLightningAnimation(attacker, targets, dir) {
						override fun onPhaseStarted(g: AGraphics, phase: Int) {
							super.onPhaseStarted(g, phase)
							if (phase == 1)
								animLock1.release()
						}

						override fun onDone() {
							super.onDone()
							animLock2.release()
						}
					})
					else -> attacker.addAnimation(object : LightningAnimation2(attacker, targets) {
						override fun onPhaseStarted(g: AGraphics, phase: Int) {
							super.onPhaseStarted(g, phase)
							if (phase == 1)
								animLock1.release()
						}
						override fun onDone() {
							super.onDone()
							animLock2.release()
						}
					})
				}
				boardRenderer.redraw()
				animLock1.block()
				hits.forEach {
					val victim = board.getActor(it)
					if (it.data == ACTOR_POS_DATA_DEFENDED) {
						victim.addAnimation(ShieldBlockAnimation(victim))
					} else {
						boardRenderer.addPostActor(ElectrocutionAnimation(victim))
					}
				}
				boardRenderer.redraw()
				animLock2.block()
				print("")
			}
			ZWeaponType.EARTHQUAKE -> {
				val animLock = Lock()
				for (z in board.getZombiesInZone(targetZone)) {
					animLock.acquire()
					z.addAnimation(object : EarthquakeAnimation(z) {
						override fun onDone() {
							super.onDone()
							animLock.release()
						}
					})
				}
				boardRenderer.redraw()
				animLock.block()
			}
			else -> Unit
		}

	}

	override suspend fun onAttack(
		attacker: ZPlayerName,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		actorsHit: List<ZActorPosition>,
		targetZone: Int
	) {
		super.onAttack(attacker, weapon, actionType, numDice, actorsHit, targetZone)
		val attacker = attacker.toCharacter()
		when (actionType) {
			ZActionType.MELEE -> onAttackMelee(
				attacker,
				weapon,
				actionType,
				numDice,
				actorsHit,
				targetZone
			)

			ZActionType.CATAPULT_FIRE,
			ZActionType.RANGED -> onAttackRanged(
				attacker,
				weapon,
				actionType,
				numDice,
				actorsHit,
				targetZone
			)

			ZActionType.MAGIC -> onAttackMagic(
				attacker,
				weapon,
				actionType,
				numDice,
				actorsHit,
				targetZone
			)

			else -> Unit
		}
	}

	override suspend fun onZombieStageBegin() {
		super.onZombieStageBegin()
		boardRenderer.pushZoomRect()
		boardRenderer.animateZoomTo(ZoomType.FILL_FIT)
		boardRenderer.waitForAnimations()
	}

	override suspend fun onZombieStageMoveDone() {
		super.onZombieStageMoveDone()
		boardRenderer.wait(500)
		boardRenderer.waitForAnimations()
	}

	override suspend fun onZombieStageEnd() {
		super.onZombieStageEnd()
		boardRenderer.waitForAnimations()
		boardRenderer.popZoomRect()
		boardRenderer.waitForAnimations()
	}

	/*
	protected override fun onZombiePath(posiiton: ZActorPosition, path: List<ZDir>) {
	super.onZombiePath(posiiton, path)
	final Vector2D start = zombie.getRect().getCenter();
	boardRenderer.addPostActor(new ZAnimation(1000) {

		@Override
		protected void draw(AGraphics g, float position, float dt) {
			GColor pathColor = GColor.YELLOW.withAlpha(1f-position);
			g.setColor(pathColor);
			g.begin();
			g.vertex(start);
			MutableVector2D next = new MutableVector2D(start);
			for (ZDir dir : path) {
				next.addEq(dir.dx, dir.dy);
				g.vertex(next);
			}
			g.drawLineStrip(3);
		}
	});
}*/

	override suspend fun onDoorToggled(cur: ZPlayerName, door: ZDoor) {
		super.onDoorToggled(cur, door)
		/*
		if (door.isClosed(board))
			boardRenderer.closeDoor(door)
		else
			boardRenderer.openDoor(door)
		 */
	}

	override suspend fun onCharacterHealed(c: ZPlayerName, amt: Int) {
		super.onCharacterHealed(c, amt)
		boardRenderer.addHoverMessage(String.format("+%d wounds healed", amt), c.toCharacter())
	}

	override suspend fun onCharacterDestroysSpawn(c: ZPlayerName, zoneIdx: Int, area: ZSpawnArea) {
		super.onCharacterDestroysSpawn(c, zoneIdx, area)
		boardRenderer.addPreActor(object : ZAnimation(1500) {
			val r = GRectangle()
			override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
				r.set(area.getRect())
				r.scale(1f - positionInPhase)
				area.draw(g, r)
			}
		})
	}

	override suspend fun onCloseSpawnArea(c: ZCharacter, zone: Int, area: ZSpawnArea) {
		super.onCloseSpawnArea(c, zone, area)
		boardRenderer.addPreActor(HandOfGodAnimation(c, area))
		boardRenderer.waitForAnimations()
	}

	override suspend fun onCharacterOpenDoorFailed(cur: ZPlayerName, door: ZDoor) {
		super.onCharacterOpenDoorFailed(cur, door)
		boardRenderer.addHoverMessage("Open Failed", door)
	}

	override suspend fun onIronRain(c: ZPlayerName, targetZone: Int) {
		super.onIronRain(c, targetZone)
		boardRenderer.addHoverMessage("LET IT RAIN!!", board.getZone(targetZone))
	}

	override suspend fun onDoorUnlocked(door: ZDoor) {
		super.onDoorUnlocked(door)
		boardRenderer.pushZoomRect()
		boardRenderer.animateZoomTo(door.getRect())
		boardRenderer.waitForAnimations()
		boardRenderer.addHoverMessage("DOOR UNLOCKED", door.getRect())
		boardRenderer.addPostActor(object : ZAnimation(1000) {
			val rect = door.getRect()
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.setTransparencyFilter(1f - position)
				boardRenderer.drawPadlock(g, rect)
				g.removeFilter()
			}
		})
		boardRenderer.waitForAnimations()
		boardRenderer.popZoomRect()
	}

	override suspend fun onBonusAction(pl: ZPlayerName, action: ZSkill) {
		super.onBonusAction(pl, action)
		boardRenderer.addHoverMessage("BONUS ACTION " + action.getLabel(), pl.toCharacter())
	}

	override suspend fun onZombieHoardAttacked(player: ZPlayerName, hits: List<ZZombieType>) {
		characterRenderer.addMessage("$player destroyed ${hits.size} in the hoard")
	}

	companion object {
		var log = LoggerFactory.getLogger(UIZombicide::class.java)

		@JvmStatic
		lateinit var instance: UIZombicide
			private set

		val initialized: Boolean
			get() = ::instance.isInitialized && instance.questInitialized
	}

}