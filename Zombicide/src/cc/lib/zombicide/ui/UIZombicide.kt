package cc.lib.zombicide.ui

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.math.Vector2D
import cc.lib.ui.IButton
import cc.lib.utils.Grid
import cc.lib.utils.Lock
import cc.lib.utils.Table
import cc.lib.zombicide.*
import cc.lib.zombicide.anims.*
import cc.lib.zombicide.p2p.ZGameMP
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

abstract class UIZombicide(characterRenderer: UIZCharacterRenderer, boardRenderer: UIZBoardRenderer<*>) : ZGameMP() {
	enum class UIMode {
		NONE,
		PICK_CHARACTER,
		PICK_ZONE,
		PICK_SPAWN,
		PICK_DOOR,
		PICK_MENU,
		PICK_ZOMBIE
	}

	private var gameRunning = false

	open fun isGameRunning() : Boolean = gameRunning

	var uiMode = UIMode.NONE
		private set
	var boardMessage: String? = null
		protected set(msg) {
			field = msg
			boardRenderer.redraw()
		}
	var options: List<Any> = emptyList()
		private set

	private var result: Any? = null
	@JvmField
    val characterRenderer: UIZCharacterRenderer
	@JvmField
    val boardRenderer: UIZBoardRenderer<*>
	abstract val thisUser: ZUser
	fun refresh() {
		boardRenderer.redraw()
		characterRenderer.redraw()
	}

	fun addPlayerComponentMessage(message: String) {
		characterRenderer.addMessage(message)
	}

	@Synchronized
	fun stopGameThread() {
		gameRunning = false
		setResult(null)
	}

	@Synchronized
	fun startGameThread() {
		if (isGameRunning())
			return
		characterRenderer.clearMessages()
		gameRunning = true
		thread {
			kotlin.runCatching {
				boardRenderer.redraw()
				while (gameRunning && !isGameOver) {
					runGame()
				}
			}.exceptionOrNull()?.let { e ->
				log.error(e)
				e.printStackTrace()
			}
			log.debug("Game thread stopped")
		}
	}

	open fun undo() {}

	private val lock = ReentrantLock()
	private val condition = lock.newCondition()

	open fun <T> waitForUser(expectedType: Class<T>): T? {
		lock.withLock {
			condition.await()
		}
		uiMode = UIMode.NONE
		result?.let {
			if (expectedType.isAssignableFrom(it.javaClass))
				return it as T
		}
		return null
	}

	open fun setResult(result: Any?) {
		boardRenderer.setOverlay(null)
		this.result = result
		lock.withLock {
			condition.signal()
		}
		refresh()
	}

	override var currentUserName: String? = null
		get() = super.currentUserName
		set(name) {
			field = name
			boardMessage = "$name's Turn"
		}

	fun pickCharacter(message: String, characters: List<ZPlayerName>): ZPlayerName? {
		synchronized(this) {
			options = characters.map { it.character }
			uiMode = UIMode.PICK_CHARACTER
			boardMessage = message
		}
		val ch = waitForUser(ZCharacter::class.java)
		return ch?.type
	}

	fun pickZone(message: String, zones: List<Int>): Int? {
		synchronized(this) {
			options = zones
			uiMode = UIMode.PICK_ZONE
			boardRenderer.redraw()
			boardMessage = message
		}
		return waitForUser(Int::class.javaObjectType)
	}

	fun pickSpawn(message: String, areas: List<ZSpawnArea>): Int? {
		synchronized(this) {
			options = areas
			uiMode = UIMode.PICK_SPAWN
			boardRenderer.redraw()
			boardMessage = message
		}
		val area = waitForUser(ZSpawnArea::class.java) ?: return null
		return areas.indexOf(area)
	}

	fun <T: IButton> pickMenu(name: ZPlayerName, message: String, expectedType: Class<T>, moves: List<T>): T? {
		synchronized(this) {
			options = moves
			uiMode = UIMode.PICK_MENU
			if (expectedType == ZMove::class.java)
				boardRenderer.processMoveOptions(name.character, moves as List<ZMove>)
			else
				boardRenderer.processSubMenu(name.character, moves)
			boardMessage = message
		}
		return waitForUser(expectedType)
	}

	fun pickDoor(message: String, doors: List<ZDoor>): ZDoor? {
		synchronized(this) {
			options = doors
			uiMode = UIMode.PICK_DOOR
			boardRenderer.redraw()
			boardMessage = message
		}
		return waitForUser(ZDoor::class.java)
	}

	override fun addLogMessage(msg: String) {
		super.addLogMessage(msg)
		addPlayerComponentMessage(msg)
	}

	fun showObjectivesOverlay() {
		boardRenderer.setOverlay(quest.getObjectivesOverlay(this))
	}

	fun <T : ZEquipment<*>> showEquipmentOverlay(player: ZPlayerName, list: List<T>) {
		val table = Table(object : Table.Model {
			override fun getMaxCharsPerLine(): Int {
				return 32
			}
		})
		for (t in list) {
			table.addColumnNoHeaderVarArg(t.getCardInfo(player.character, this))
		}
		boardRenderer.setOverlay(table)
	}

	fun showQuestTitleOverlay() {
		boardRenderer.setOverlay(object : OverlayTextAnimation(quest.name, boardRenderer.numOverlayTextAnimations) {
			override fun onDone() {
				super.onDone()
				showObjectivesOverlay()
			}
		})
	}

	fun showSummaryOverlay() {
		boardRenderer.setOverlay(gameSummaryTable)
	}

	override fun initQuest(quest: ZQuest) {
		boardRenderer.clearTiles()
	}

	override fun onEquipmentThrown(actor: ZPlayerName, icon: ZIcon, zone: Int) {
		super.onEquipmentThrown(actor, icon, zone)
		val animLock = Lock(1)
		if (actor.character.occupiedZone != zone) {
			actor.character.addAnimation(object : ThrowAnimation(actor.character, board.getZone(zone).center, icon) {
				override fun onDone() {
					super.onDone()
					animLock.release()
				}
			})
			boardRenderer.redraw()
			animLock.block()
		}
	}

	override fun onRollDice(roll: Array<Int>) {
		super.onRollDice(roll)
		characterRenderer.addWrappable(ZDiceWrappable(roll))
	}

	override fun onDragonBileExploded(zone: Int) {
		super.onDragonBileExploded(zone)
		val rects = Utils.map<Grid.Pos, IRectangle>(board.getZone(zone).getCells()) { pos: Grid.Pos? -> board.getCell(pos!!) }
		boardRenderer.addPreActor(InfernoAnimation(rects))
		Utils.waitNoThrow(this, 1000)
	}

	override fun onCloseSpawnArea(c: ZCharacter, zone: Int, area: ZSpawnArea) {
		super.onCloseSpawnArea(c, zone, area)
		val lock = Lock(1)
		boardRenderer.addPreActor(object : HandOfGodAnimation(c, area) {
			override fun onDone() {
				super.onDone()
				lock.release()
			}
		})
		lock.block()
	}

	override fun onZombieDestroyed(c: ZPlayerName, deathType: ZAttackType, pos: ZActorPosition) {
		super.onZombieDestroyed(c, deathType, pos)
		val zombie = board.getActor<ZZombie>(pos)
		zombie.addAnimation(DeathAnimation(zombie))
		/*
		val lock = Lock()
		zombie.addAnimation(DeathAnimation(zombie))
		when (deathType) {
		// ELECTROCUTION ANIMATION MOVED TO ATTACK SEQUENCE SO IT CAN BE DISPLAYED SIMULTANEOUSLY
		// WHEN THE ELECTRIC STRANDS ARE SHOOTING
			ZAttackType.ELECTROCUTION -> {
				lock.acquire()
				zombie.addAnimation(object : ElectrocutionAnimation(zombie) {
					override fun onDone() {
						super.onDone()
						lock.release()
					}
				})
				boardRenderer.redraw()
				lock.block()
				zombie.addAnimation(DeathAnimation(zombie))
			}
			ZAttackType.FIRE,
			ZAttackType.DISINTEGRATION,
			ZAttackType.BLADE,
			ZAttackType.CRUSH,
			ZAttackType.RANGED_ARROWS,
			ZAttackType.RANGED_BOLTS,
			ZAttackType.RANGED_THROW,
			ZAttackType.EARTHQUAKE,
			ZAttackType.MENTAL_STRIKE,
			ZAttackType.NORMAL,
			else                                                             -> zombie.addAnimation(DeathAnimation(zombie))                                                                                                                                                                        -> zombie.addAnimation(DeathAnimation(zombie))
		}*/
	}

	override fun onActorMoved(actor: ZActor<*>, start: GRectangle, end: GRectangle, speed: Long) {
		super.onActorMoved(actor, start, end, speed)
		actor.addAnimation(MoveAnimation(actor, start, end, speed))
		boardRenderer.redraw()
	}

	override fun onZombieSpawned(zombie: ZZombie) {
		super.onZombieSpawned(zombie)
		zombie.addAnimation(SpawnAnimation(zombie, board))
		when (zombie.type) {
			ZZombieType.Abomination -> {
				boardRenderer.addOverlay(OverlayTextAnimation("A B O M I N A T I O N ! !", boardRenderer.numOverlayTextAnimations))
				Utils.waitNoThrow(this, 500)
			}
			ZZombieType.Necromancer -> {
				boardRenderer.addOverlay(OverlayTextAnimation("N E C R O M A N C E R ! !", boardRenderer.numOverlayTextAnimations))
				Utils.waitNoThrow(this, 500)
			}
		}
		boardRenderer.redraw()
	}

	override fun onCharacterDefends(cur: ZPlayerName, attackerPosition: ZActorPosition) {
		super.onCharacterDefends(cur, attackerPosition)
		val actor = board.getActor<ZActor<*>>(attackerPosition)
		actor.addAnimation(ShieldBlockAnimation(cur.character))
		boardRenderer.redraw()
	}

	override fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, player: ZPlayerName?) {
		super.onCurrentCharacterUpdated(priorPlayer, player)
		if (priorPlayer != null) {
			val animLock = Lock(1)
			// Add an animation to end of any existing animations to block until all are completed
			priorPlayer.character.addAnimation(object : EmptyAnimation(priorPlayer.character) {
				override fun onDone() {
					super.onDone()
					animLock.release()
				}
			})
			boardRenderer.redraw()
			characterRenderer.redraw()
			animLock.block()
		}
	}

	override fun onCharacterAttacked(character: ZPlayerName, attackerPosition: ZActorPosition, attackType: ZAttackType, perished: Boolean) {
		super.onCharacterAttacked(character, attackerPosition, attackType, perished)
		val attacker = board.getActor<ZActor<*>>(attackerPosition)
		when (attackType) {
			ZAttackType.ELECTROCUTION -> attacker.addAnimation(ElectrocutionAnimation(character.character))
			ZAttackType.NORMAL,
			ZAttackType.FIRE,
			ZAttackType.DISINTEGRATION,
			ZAttackType.BLADE,
			ZAttackType.CRUSH,
			ZAttackType.RANGED_ARROWS,
			ZAttackType.RANGED_BOLTS,
			ZAttackType.RANGED_THROW,
			ZAttackType.EARTHQUAKE,
			ZAttackType.MENTAL_STRIKE -> attacker.addAnimation(SlashedAnimation(character.character))
			else                                                                                                                                                                                                                                     -> attacker.addAnimation(SlashedAnimation(character.character))
		}
		if (perished) {
			val lock = Lock(1)
			with (character.character) {
				val group = GroupAnimation(this, true)
				group.addSequentially(AscendingAngelDeathAnimation(this))
				group.addSequentially(object : ZActorAnimation(character.character, 2000) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						val img = g.getImage(ZIcon.GRAVESTONE.imageIds[0])
						val rect = GRectangle(actor.getRect().fit(img))
						rect.y += rect.h * (1f - position)
						rect.h *= position
						g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect)
					}

					override fun onDone() {
						lock.release()
					}
				})
				addAnimation(group)
			}
			boardRenderer.redraw()
			lock.block()
		}
		boardRenderer.redraw()
	}

	override fun onAhhhhhh(c: ZPlayerName) {
		super.onAhhhhhh(c)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "AHHHHHH!", c.character))
		Utils.waitNoThrow(this, 500)
	}

	override fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
		super.onEquipmentFound(c, equipment)
		if (thisUser.players.contains(c)) {
			val info = Table().setModel(object : Table.Model {
				override fun getCornerRadius(): Float {
					return 20f
				}

				override fun getBackgroundColor(): GColor {
					return GColor.TRANSLUSCENT_BLACK
				}
			})
			info.addRowList(Utils.map(equipment) { e: ZEquipment<*> -> e.getCardInfo(c.character, this) })
			boardRenderer.setOverlay(info)
		} else {
			for (e in equipment) {
				boardRenderer.addPostActor(HoverMessage(boardRenderer, "+" + e.label, c.character))
				Utils.waitNoThrow(this, 500)
			}
		}
	}

	override fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
		super.onCharacterGainedExperience(c, points)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("+%d EXP", points), c.character))
		Utils.waitNoThrow(this, 500)
	}

	override fun onGameLost() {
		super.onGameLost()
		boardRenderer.waitForAnimations()
		boardRenderer.addOverlay(object : OverlayTextAnimation("Y O U   L O S T", boardRenderer.numOverlayTextAnimations) {
			override fun onDone() {
				super.onDone()
				showSummaryOverlay()
			}
		})
	}

	override fun onQuestComplete() {
		super.onQuestComplete()
		boardRenderer.waitForAnimations()
		boardRenderer.addOverlay(object : OverlayTextAnimation("C O M P L E T E D", 0) {
			override fun onDone() {
				super.onDone()
				showSummaryOverlay()
			}
		})
	}

	override fun onDoubleSpawn(multiplier: Int) {
		super.onDoubleSpawn(multiplier)
		boardRenderer.addOverlay(OverlayTextAnimation(String.format("DOUBLE SPAWN X %d", multiplier), boardRenderer.numOverlayTextAnimations))
		Utils.waitNoThrow(this, 500)
	}

	override fun onNewSkillAquired(c: ZPlayerName, skill: ZSkill) {
		super.onNewSkillAquired(c, skill)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("%s Acquired", skill.label), c.character))
		characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.label, skill.label))
	}

	override fun onExtraActivation(category: ZZombieCategory) {
		super.onExtraActivation(category)
		boardRenderer.addOverlay(OverlayTextAnimation(String.format("EXTRA ACTIVATION %s", category), boardRenderer.numOverlayTextAnimations))
		Utils.waitNoThrow(this, 500)
	}

	override fun onSkillKill(c: ZPlayerName, skill: ZSkill, z: ZZombie, attackType: ZAttackType) {
		super.onSkillKill(c, skill, z, attackType)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("%s Kill!!", skill.label), z))
	}

	override fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
		super.onRollSixApplied(c, skill)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("Roll Six!! %s", skill.label), c.character))
	}

	override fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
		super.onWeaponReloaded(c, w)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("%s Reloaded", w.label), c.character))
	}

	override fun onNoiseAdded(zoneIndex: Int) {
		val lock = Lock(1)
		super.onNoiseAdded(zoneIndex)
		val zone = board.getZone(zoneIndex)
		boardRenderer.addPreActor(object : MakeNoiseAnimation(zone.center) {
			override fun onDone() {
				super.onDone()
				lock.release()
			}
		})
		boardRenderer.redraw()
		lock.block()
	}

	override fun onWeaponGoesClick(c: ZPlayerName, weapon: ZWeapon) {
		super.onWeaponGoesClick(c, weapon)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "CLICK", c.character))
	}

	override fun onBeginRound(roundNum: Int) {
		super.onBeginRound(roundNum)
		boardRenderer.waitForAnimations()
		if (roundNum == 0)
			showQuestTitleOverlay()
	}

	private fun onAttackMelee(attacker: ZCharacter, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, hits: MutableList<ZActorPosition>, targetZone: Int) {
		when (weapon.type) {
			ZWeaponType.EARTHQUAKE_HAMMER -> {
				val animLock = Lock(numDice)
				val currentZoom = boardRenderer.zoomPercent
				if (currentZoom < 1) {
					attacker.addAnimation(EmptyAnimation(attacker, 500))
					boardRenderer.addPreActor(ZoomAnimation(attacker.getRect(board).center, boardRenderer, 1f))
				}
				var i = 0
				while (i < numDice) {
					attacker.addAnimation(object : MeleeAnimation(attacker, board) {
						override fun onDone() {
							super.onDone()
							animLock.release()
						}
					})
					val g = GroupAnimation(attacker)
					for (pos in hits) {
						val z = board.getActor<ZActor<*>>(pos)
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
			else                          -> {
				val animLock = Lock(numDice)
				val currentZoom = boardRenderer.zoomPercent
				if (currentZoom < 1) {
					attacker.addAnimation(EmptyAnimation(attacker, 500))
					boardRenderer.addPreActor(ZoomAnimation(attacker.getRect(board).center, boardRenderer, 1f))
				}
				var i = 0
				while (i < numDice) {
					if (i < hits.size) {
						val pos = hits[i]
						val victim = board.getActor<ZActor<*>>(pos)
						require(victim !== attacker)
						attacker.addAnimation(object : MeleeAnimation(attacker, board) {
							override fun onDone() {
								super.onDone()
								if (pos.data == ACTOR_POS_DATA_DEFENDED) {
									victim.addAnimation(ShieldBlockAnimation(victim))
								} else {
									victim.addAnimation(SlashedAnimation(victim))
								}
								animLock.release()
							}
						})
					} else {
						attacker.addAnimation(object : MeleeAnimation(attacker, board) {
							override fun onDone() {
								super.onDone()
								animLock.release()
								boardRenderer.addPostActor(HoverMessage(boardRenderer, "MISS!!", attacker))
							}
						})
					}
					i++
				}
				boardRenderer.redraw()
				animLock.block()
				boardRenderer.animateZoomTo(currentZoom)
			}
		}


	}

	private fun onAttackRanged(attacker: ZCharacter, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, hits: MutableList<ZActorPosition>, targetZone: Int) {
		when (weapon.type) {
			ZWeaponType.MJOLNIR -> {
				val hammerSpeed = 600L
				val animLock = Lock()
				if (hits.isEmpty()) {
					animLock.acquire()
					attacker.addAnimation(object : ShootAnimation(attacker, hammerSpeed, board.getZone(targetZone).rectangle.randomPointInside, ZIcon.MJOLNIR) {
						override fun onDone() {
							animLock.release()
						}
					}.setOscillating<ShootAnimation>(true).setRepeats(1))
				} else {
					val group = GroupAnimation(attacker)
					var prev : ZActor<*> = attacker
					hits.forEach {
						val victim = board.getActor<ZActor<*>>(it)
						animLock.acquire()
						group.addSequentially(object : ShootAnimation(prev, hammerSpeed, victim, ZIcon.MJOLNIR) {
							override fun onDone() {
								animLock.release()
							}
						})
						prev = victim
					}
					animLock.acquire()
					group.addSequentially(object: ShootAnimation(prev, hammerSpeed, attacker, ZIcon.MJOLNIR) {
						override fun onDone() {
							animLock.release()
						}
					})
					attacker.addAnimation(group)
				}
				boardRenderer.redraw()
				animLock.block()
			}
			ZWeaponType.DAGGER -> {
				val group = GroupAnimation(attacker)
				val animLock = Lock(numDice)
				var delay = 200
				var i = 0
				while (i < numDice) {
					if (i < hits.size) {
						val pos = hits[i]
						val victim = board.getActor<ZActor<*>>(pos)
						group.addAnimation(delay, object : ThrowAnimation(attacker, victim, ZIcon.DAGGER, .1f, 400) {
							override fun onDone() {
								if (pos.data == ACTOR_POS_DATA_DEFENDED) {
									victim.addAnimation(GroupAnimation(victim)
										.addAnimation(ShieldBlockAnimation(victim))
										.addAnimation(DeflectionAnimation(victim, Utils.randItem(ZIcon.DAGGER.imageIds), dir.opposite))
									)
								} else {
									victim.addAnimation(SlashedAnimation(victim))
								}
								animLock.release()
							}
						})
					} else {
						val center: IVector2D = board.getZone(targetZone).rectangle.randomPointInside
						group.addAnimation(delay, object : ThrowAnimation(attacker, center, ZIcon.DAGGER, .1f, 400) {
							override fun onDone() {
								boardRenderer.addPostActor(HoverMessage(boardRenderer, "MISS!!", attacker))
								animLock.release()
							}
						})
					}
					delay += 200
					i++
				}
				attacker.addAnimation(group)
				boardRenderer.redraw()
				animLock.block()
			}
			else               -> {
				val group = GroupAnimation(attacker)
				val animLock = Lock(numDice)
				var delay = 0
				var i = 0
				while (i < numDice) {
					if (i < hits.size) {
						val pos = hits[i]
						val victim = board.getActor<ZActor<*>>(pos)
						group.addAnimation(delay, object : ShootAnimation(attacker, 300, victim, ZIcon.ARROW) {
							override fun onDone() {
								val arrowId = ZIcon.ARROW.imageIds[dir.ordinal]
								if (pos.data == ACTOR_POS_DATA_DEFENDED) {
									victim.addAnimation(GroupAnimation(victim)
										.addAnimation(ShieldBlockAnimation(victim))
										.addAnimation(DeflectionAnimation(victim, arrowId, dir.opposite))
									)
								} else {
									victim.addAnimation(StaticAnimation(victim, 800, arrowId, r, true))
								}
								animLock.release()
							}
						})
					} else {
						val center: IVector2D = board.getZone(targetZone).rectangle.randomPointInside
						group.addAnimation(delay, object : ShootAnimation(attacker, 300, center, ZIcon.ARROW) {
							override fun onDone() {
								boardRenderer.addPostActor(HoverMessage(boardRenderer, "MISS!!", attacker))
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

	private fun onAttackMagic(attacker: ZCharacter, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, hits: MutableList<ZActorPosition>, targetZone: Int) {
		when (weapon.type) {
			ZWeaponType.DEATH_STRIKE -> {
				val animLock = Lock(1)
				val zoneRect = board.getZone(targetZone).rectangle
				val targetRect = zoneRect.scaledBy(.5f) //.moveBy(0, -1);
				attacker.addAnimation(object : DeathStrikeAnimation(attacker, targetRect, numDice) {
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
				val animLock = Lock(1)
				attacker.addAnimation(object : MagicOrbAnimation(attacker, board.getZone(targetZone).center) {
					override fun onDone() {
						super.onDone()
						animLock.release()
					}
				})
				boardRenderer.redraw()
				animLock.block()
			}
			ZWeaponType.FIREBALL -> {
				val group = GroupAnimation(attacker)
				val animLock = Lock(numDice)
				var delay = 0
				var i = 0
				while (i < numDice) {
					if (hits.size > 0) {
						val pos: ZActorPosition = hits.removeAt(0)
						val victim = board.getActor<ZActor<*>>(pos)
						group.addAnimation(delay, object : FireballAnimation(attacker, victim) {
							override fun onDone() {
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
								boardRenderer.addPostActor(HoverMessage(boardRenderer, "MISS!!", attacker))
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
				val rects = Utils.map<Grid.Pos, IRectangle>(board.getZone(targetZone).getCells()) { pos: Grid.Pos? -> board.getCell(pos!!) }
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
						val actorRect = (board.getActor(hits[i]) as ZActor<*>).getRect().scaledBy(.5f)
						targets.add(Vector2D.getLinearInterpolator(actorRect.randomPointInside, actorRect.randomPointInside))
					} else {
						val rect = board.getZone(targetZone).rectangle.scaledBy(.5f)
						targets.add(Vector2D.getLinearInterpolator(rect.randomPointInside, rect.randomPointInside))
					}
					i++
				}
				val dir = ZDir.getDirFrom(attacker.occupiedCell, board.getZone(targetZone).cells[0])?:ZDir.NORTH
				when (weapon.type) {
					ZWeaponType.MJOLNIR -> attacker.addAnimation(object : MjolnirLightningAnimation(attacker, targets, dir) {
						override fun onPhaseStarted(g: AGraphics, phase: Int) {
							super.onPhaseStarted(g, phase)
							if (phase == 1)
								animLock1.release()
						}
						override fun onDone() {
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
							animLock2.release()
						}
					})
				}
				boardRenderer.redraw()
				animLock1.block()
				hits.forEach {
					val victim = board.getActor<ZActor<*>>(it)
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
		}

	}

	override fun onAttack(_attacker: ZPlayerName, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, hits: List<ZActorPosition>, targetZone: Int) {
		super.onAttack(_attacker, weapon, actionType, numDice, hits, targetZone)
		val hits = hits.toMutableList()
		val attacker = _attacker.character
		when (actionType) {
			ZActionType.MELEE -> onAttackMelee(attacker, weapon, actionType, numDice, hits, targetZone)
			ZActionType.RANGED -> onAttackRanged(attacker, weapon, actionType, numDice, hits, targetZone)
			ZActionType.MAGIC -> onAttackMagic(attacker, weapon, actionType, numDice, hits, targetZone)
		}
	}

	protected override fun onZombiePath(zombie: ZZombie, path: List<ZDir>) {
		super.onZombiePath(zombie, path)
		/*
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
        });*/
	}

	override fun onCharacterOpenedDoor(cur: ZPlayerName, door: ZDoor) {
		super.onCharacterOpenedDoor(cur, door)
	}

	override fun onCharacterHealed(c: ZPlayerName, amt: Int) {
		super.onCharacterHealed(c, amt)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, String.format("+%d wounds healed", amt), c.character))
	}

	override fun onCharacterDestroysSpawn(c: ZPlayerName, zoneIdx: Int) {
		super.onCharacterDestroysSpawn(c, zoneIdx)
	}

	override fun onCharacterOpenDoorFailed(cur: ZPlayerName, door: ZDoor) {
		super.onCharacterOpenDoorFailed(cur, door)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "Open Failed", door.getRect(board).center))
	}

	override fun onIronRain(c: ZPlayerName, targetZone: Int) {
		super.onIronRain(c, targetZone)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "LET IT RAIN!!", board.getZone(targetZone).center))
	}

	override fun onDoorUnlocked(door: ZDoor) {
		super.onDoorUnlocked(door)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "DOOR UNLOCKED", door.getRect(board).center))
	}

	override fun onBonusAction(pl: ZPlayerName, action: ZSkill) {
		super.onBonusAction(pl, action)
		boardRenderer.addPostActor(HoverMessage(boardRenderer, "BONUS ACTION " + action.label, pl.character))
	}

	companion object {
		var log = LoggerFactory.getLogger(UIZombicide::class.java)
		@JvmStatic
        lateinit var instance: UIZombicide
			private set

		val initialized : Boolean
			get() = ::instance.isInitialized && instance.questInitialized
	}

	init {
		instance = this
		this.characterRenderer = characterRenderer
		this.boardRenderer = boardRenderer
	}
}