package cc.lib.zombicide

import cc.lib.game.GColor
import cc.lib.game.GRectangle
import cc.lib.ksp.remote.IRemote
import cc.lib.ksp.remote.Remote
import cc.lib.ksp.remote.RemoteFunction
import cc.lib.logger.LoggerFactory
import cc.lib.reflector.Alternate
import cc.lib.reflector.Omit
import cc.lib.reflector.RBufferedReader
import cc.lib.reflector.Reflector
import cc.lib.utils.GException
import cc.lib.utils.Grid
import cc.lib.utils.Grid.Pos
import cc.lib.utils.Table
import cc.lib.utils.appendedWith
import cc.lib.utils.getOrNull
import cc.lib.utils.increment
import cc.lib.utils.midPointOrNull
import cc.lib.utils.random
import cc.lib.utils.removeRandom
import cc.lib.utils.rotate
import cc.lib.utils.takeIfInstance
import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.zombicide.ZSpawnCard.ActionType
import java.util.Arrays
import java.util.Collections
import java.util.LinkedList
import java.util.Stack

@Suppress("UNCHECKED_CAST")
@Remote
open class ZGame() : Reflector<ZGame>(), IRemote {
	companion object {
		@JvmField
		var DEBUG = false
		val log = LoggerFactory.getLogger(ZGame::class.java)
		val GAME_LOST = 2
		val GAME_WON = 1

		@JvmStatic
		fun initDice(difficulty: ZDifficulty): IntArray {
			val countPerNum: Int
			val step: Int = when (difficulty) {
				ZDifficulty.EASY -> {
					countPerNum = 20
					-1
				}

				ZDifficulty.MEDIUM -> {
					countPerNum = 30
					-1
				}

				else -> {
					countPerNum = 30
					0
                }
            }
            var len = 0
            var num = 6
            var cnt = countPerNum
            while (num > 0) {
                len += cnt
                cnt += step
                num--
            }
            val dice = IntArray(len)
            cnt = countPerNum
            num = 6
            var idx = 0
            while (num > 0) {
                for (i in 0 until cnt) dice[idx++] = num
                cnt += step
                num--
            }
            require(idx == dice.size)
            dice.shuffle()
            return dice
        }

        @JvmField
        val ACTOR_POS_DATA_DEFENDED = 0
        @JvmField
        val ACTOR_POS_DATA_DAMAGED = 1

        init {
            addAllFields(ZGame::class.java)
            addAllFields(State::class.java)
//            registerClass(ZZombie::class.java)
            registerClass(ZWallFlag::class.java)
            registerClass(ZSkill::class.java)
            registerClass(HashMap::class.java)
            registerClass(HashSet::class.java)
            registerClass(ZCellType::class.java)
            registerClass(ZMove::class.java)
        }
    }

	data class State internal constructor(
		val state: ZState = ZState.INIT,
		val player: ZPlayerName? = null,
		val equipment: ZEquipment<*>? = null,
		val skillLevel: ZSkillLevel? = null,
		val target: ZPlayerName? = null,
		val familiar: ZFamiliarType? = null
	) : Reflector<State>()

    private val stateStack = Stack<State>()
    @JvmField
    var board: ZBoard = ZBoard()

    @Omit
    private val users: MutableList<ZUser> = ArrayList()

    val questInitialized : Boolean
        get() = ::quest.isInitialized
    lateinit var quest: ZQuest
        private set

	@Omit
	private var currentUserIdx = -1
	private var startUser = 0

	@Alternate(variations = ["searchables"])
	private val lootDeck = LinkedList<ZEquipment<*>>()
	private var spawnMultiplier = 1
	var roundNum = 0
	private var gameOverStatus = 0 // 0 == in play, 1, == game won, 2 == game lost
	private lateinit var currentQuest: ZQuests
	private lateinit var dice: IntArray
	private var difficulty = ZDifficulty.EASY
	private val deck = mutableListOf<ZSpawnCard>()
	val rules = ZRules()
	var repeatableMove: ZMove? = null
	var repeatableMovePlayer: ZPlayerName? = null

	open val spawnDeckSize: Int
		get() = deck.size

	open val lootDeckSize: Int
		get() = lootDeck.size

	open val hoardSize: Int
		get() = board.getHoard().map { it.value }.sum()

	fun ZPlayerName.toCharacter(): ZCharacter = board.getCharacter(this)

	fun ZPlayerName.toCharacterOrNull(): ZCharacter? = board.getCharacterOrNull(this)

	fun getStartUser(): ZUser = users[startUser.coerceIn(0 until users.size)]

	private fun updateCurrentUser(idx: Int) {
		if (idx != currentUserIdx) {
			currentUserIdx = idx
			with(users[idx]) {
				onCurrentUserUpdated(name, colorId)
			}
		}
	}

	open fun pushState(state: State) {
		val oldPlayer = currentCharacter?.type
		if (state.player != oldPlayer)
			onCurrentCharacterUpdated(oldPlayer, state.player?.toCharacter())
		stateStack.push(state)
	}

	fun popState() {
		val curPlayer = currentCharacter?.type
		stateStack.pop()
		currentCharacter?.let {
			if (curPlayer != it.type)
				onCurrentCharacterUpdated(curPlayer, it)
		}
	}

	open fun canUndo() = false

	open fun undo() {
		throw NotImplementedError()
	}

	@RemoteFunction
	protected open fun onCurrentUserUpdated(userName: String, colorId: Int) {
		log.debug("%s updated colorID: $colorId", userName)
	}

	@RemoteFunction
	protected open fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, character: ZCharacter?) {
		log.debug("%s updated too %s", priorPlayer ?: "none", character?.type)
	}

	fun setDifficulty(difficulty: ZDifficulty) {
		this.difficulty = difficulty
		dice = initDice(difficulty)
	}

	fun getDifficulty(): ZDifficulty {
		return difficulty
	}

    private fun initGame() {
        initLootDeck()
	    roundNum = 0
	    currentUserIdx = 0
	    gameOverStatus = 0
        spawnMultiplier = 1
        dice = initDice(difficulty)
	    stateStack.clear()
	    stateStack.push(State(ZState.INIT))
    }

    fun clearCharacters() {
        board.removeCharacters()
        for (u: ZUser in users) {
            u.clearCharacters()
        }
    }

    fun getUsers(): Iterable<ZUser> {
        return users
    }

    fun setUsers(vararg users: ZUser) {
        this.users.clear()
	    this.users.addAll(users)
    }

	val isGameOver: Boolean
		get() = gameOverStatus != 0
	val isGameWon: Boolean
		get() = gameOverStatus == GAME_WON
	val isGameLost: Boolean
		get() = gameOverStatus == GAME_LOST


	override fun deserialize(reader: RBufferedReader) {
		super.deserialize(reader)
		initQuest(quest)
	}

	open fun reload() {
		loadQuest(currentQuest)
	}

	fun getNumKills(vararg types: ZZombieType): Int = board.getAllCharacters().sumOf { character ->
		types.sumOf { character.getKills(it) }
	}

	fun addUser(user: ZUser) {
		if (!users.contains(user)) users.add(user)
	}

	fun removeUser(user: ZUser) {
		users.remove(user)
		if (currentUserIdx >= users.size) currentUserIdx = 0
		if (startUser >= users.size) startUser = 0
    }

    protected open fun initQuest(quest: ZQuest) {}

	@Omit
	private var startZoneCounter = 0

	fun addCharacter(pl: ZPlayerName): ZCharacter {
		val cell = with(board.getCellsOfType(ZCellType.START)) {
			val idx = startZoneCounter++ % size
			get(idx)
		}
		val c = pl.create()
		c.occupiedZone = cell.zoneIndex
		require(board.spawnActor(c)) { "Failed to add $pl to board" }
		return c
	}

	fun removeCharacter(nm: ZPlayerName): ZCharacter? {
		return board.getAllCharacters().firstOrNull { it.type == nm }?.let {
			board.removeActor(nm.name)
			it
		}
	}

	open fun loadQuest(newQuest: ZQuests) {
		log.debug("Loading quest: $newQuest")
		val prevQuest: ZQuest? = if (questInitialized) this.quest else null
		quest = newQuest.load()
		synchronized(board) {
			board = quest.loadBoard()
			if (prevQuest == null || prevQuest.name != this.quest.name) {
				initQuest(quest)
			}
			initGame()
			val startCells: MutableList<ZCell> = ArrayList()
            val it: Grid.Iterator<ZCell> = board.getCellsIterator()
            while (it.hasNext()) {
	            val cell: ZCell = it.next()
	            if (cell.isCellTypeEmpty) {
		            continue
	            }
	            val zone: ZZone = board.zones[cell.zoneIndex]
	            val type = when (cell.environment) {
		            ZCellEnvironment.OUTDOORS -> ZZoneType.OUTDOORS
		            ZCellEnvironment.BUILDING -> ZZoneType.BUILDING
		            ZCellEnvironment.VAULT -> ZZoneType.VAULT
		            ZCellEnvironment.TOWER -> ZZoneType.TOWER
		            ZCellEnvironment.WATER -> ZZoneType.WATER
		            ZCellEnvironment.HOARD -> ZZoneType.HOARD
	            }
	            require(zone.type == ZZoneType.UNSET || zone.type == type) { "Zone ${zone.zoneIndex} is not of uniform type" }
	            zone.type = type
	            // add doors for the zone
	            for (dir: ZDir in compassValues) {
		            when (cell.getWallFlag(dir)) {
			            ZWallFlag.CLOSED, ZWallFlag.OPEN -> {
				            val pos: Pos = it.pos
				            val next: Pos = board.getAdjacent(pos, (dir))
				            zone.doors.add(ZDoor(pos, next, dir, GColor.RED))
			            }

			            else -> Unit
		            }
	            }
	            for (type: ZCellType in ZCellType.entries) {
		            if (cell.isCellType(type)) {
			            when (type) {
				            ZCellType.START -> startCells.add(cell)
				            ZCellType.OBJECTIVE_BLACK,
				            ZCellType.OBJECTIVE_RED,
				            ZCellType.OBJECTIVE_BLUE,
				            ZCellType.OBJECTIVE_GREEN -> zone.isObjective = true

				            ZCellType.VAULT_DOOR_VIOLET -> addVaultDoor(cell, zone, it.pos, GColor.MAGENTA)
				            ZCellType.VAULT_DOOR_GOLD -> addVaultDoor(cell, zone, it.pos, GColor.GOLD)
				            else -> Unit
                        }
                    }
                }
            }
            if (startCells.size == 0) {
	            error("No start cells specified")
            }

            // position all the characters here
            board.removeCharacters()
            var curCellIndex = 0
            for (u: ZUser in users) {
                for (pl: ZPlayerName in u.players) {
	                val c: ZCharacter = pl.create()
	                c.colorId = u.colorId
	                val cell: ZCell = startCells[curCellIndex]
	                curCellIndex = curCellIndex.rotate(startCells.size)
	                c.occupiedZone = cell.zoneIndex
	                if (!board.spawnActor(c))
		                error("Failed to add $pl to board")
                }
            }
			quest.init(this)
			deck.clear()
			deck.addAll(quest.buildDeck(difficulty, rules))
        }
        currentQuest = newQuest
    }

	private fun addVaultDoor(cell: ZCell, zone: ZZone, pos: Pos, color: GColor) {
		// add a vault door leading to the cell specified by vaultFlag
		require(cell.vaultId > 0)
		val it2 = board.getCellsIterator()
		while (it2.hasNext()) {
			val cell2 = it2.next()
			if (cell === cell2) {
				continue
			}
			if (cell.vaultType !== cell2.vaultType) {
				continue
			}
			if (cell.vaultId == cell2.vaultId) {
				zone.doors.add(ZDoor(pos, it2.pos, cell.environment.getVaultDirection(), color))
				return
			}
		}
		error("Unable to add vault door at $pos, zone: ${zone.zoneIndex}")
	}

	fun spawnZombies(_count: Int, type: ZZombieType, _zone: Int) {
		var zone = _zone
		var count = _count
		log.debug("spawn zombies %s X %d in zone %d", type, count, zone)
		if ((count > 0) && type.canDoubleSpawn && (spawnMultiplier > 1)) {
			log.debug("**** Spawn multiplier applied %d", spawnMultiplier)
			addLogMessage("Spawn Multiplier X $spawnMultiplier Applied")
			count *= spawnMultiplier
			spawnMultiplier = 1
		}
		for (i in 0 until count) {
			when (type) {
				ZZombieType.SwampTroll -> {
					// special case spawns in a water closest to noisiest zone
					val noisiest = board.getMaxNoiseLevelZones().midPointOrNull() ?: board.center
					board.getZonesOfType(ZZoneType.WATER).minByOrNull { wz ->
						wz.center.sub(noisiest).magSquared()
					}?.let {
						zone = it.zoneIndex
					} ?: continue
				}

				ZZombieType.Ratz -> extraActivation(ZZombieCategory.RAT_SWARMS)
				ZZombieType.NecromanticDragon -> {
					// only one ND on the board at a time
					board.getAllActors().firstOrNull {
						it is ZZombie && it.type == ZZombieType.NecromanticDragon
					} ?: continue
					// spawns in the center tile with most survivors
					with(board.getZone(zone).cells[0]) {
						val newRow = (row / 3) * 3 + 1
						val newCol = (column / 3) * 3 + 1
						board.getZone(Pos(newRow, newCol))
					}?.let {
						zone = it.zoneIndex
					} ?: continue
					performDragonStomp(zone)
				}

				ZZombieType.RatKing -> {
					spawnZombies(
						when (highestSkillLevel.difficultyColor) {
							ZColor.BLUE -> 1
							ZColor.YELLOW -> 2
							ZColor.ORANGE -> 4
							ZColor.RED -> 6
						}, ZZombieType.Ratz, zone
					)
				}

				else -> Unit
			}
			val zombie = ZZombie(type, zone)
			if (board.spawnActor(zombie)) {
				zombie.onBeginRound(this)
				quest.onZombieSpawned(this, zombie, zone)
				onZombieSpawned(zombie)
			}
		}
	}

	private fun performDragonStomp(zoneIdx: Int) {
		board.getAllActors().filterIsInstance<ZZombie>().filter { it.occupiedZone == zoneIdx }
			.forEach {
				destroyZombie(it, ZAttackType.CRUSH, null, null)
			}
	}

	@RemoteFunction
	open fun onZombieSpawned(zombie: ZZombie) {}

	val state: ZState
		get() {
			return stateStack.peek().state
		}

	val stateEquipment: ZEquipment<*>?
		get() {
			if (stateStack.isEmpty())
				throw GException("Invalid state")
			if (stateStack.peek().equipment == null)
				throw GException("null equipment in state")
			return stateStack.peek().equipment
		}

	val stateData: State
		get() = stateStack.peek()

    private fun setState(state: State) {
        if (stateStack.size > 0)
        	stateStack.pop()
        pushState(state)
    }

	@RemoteFunction
	protected open fun onQuestComplete() {
		addLogMessage("Quest Complete")
	}

    val isGameSetup: Boolean
        get() {
            if (board.isEmpty()) {
                System.err.println("Empty Board!")
                return false
            }
            if (users.size == 0) {
                System.err.println("No users!")
                return false
            }
            if (allCharacters.isEmpty()) {
                System.err.println("No characters!")
                return false
            }
            if (!questInitialized) {
                System.err.println("Quest not initialized")
                return false
            }
            return true
        }

    fun addTradeOptions(ch: ZCharacter, options: MutableCollection<ZMove>) {
        if (ch.canTrade()) {
            // check for trade with another character in the same zone (even if they are dead)
	        if (isClearedOfZombies(ch.occupiedZone)) {
		        board.getCharactersInZone(ch.occupiedZone).filter {
			        it != ch && (it.canTrade() || ch.canTrade())
		        }.takeIf { it.isNotEmpty() }?.map { it.type }?.also {
			        options.add(ZMove.newTradeMove(it))
		        }
	        }
        }
    }

	fun addWalkOptions(ch: ZCharacter, options: MutableCollection<ZMove>) {
		ZDir.entries.forEach { dir ->
			board.getMoveType(ch, dir)?.takeIf { it.costPerTurn <= ch.actionsLeftThisTurn }
				?.let { action ->
					options.add(ZMove.newWalkDirMove(dir, action))
				}
		}

		ZActionType.entries.filter { it.isMovement && it.costPerTurn <= ch.actionsLeftThisTurn }
			.forEach { action ->
				board.getAccessibleZones(ch, 1, 1, action).takeIf { it.isNotEmpty() }?.let {
					options.add(ZMove.newWalkMove(it, action))
				}
			}
	}

	fun isMoveAvailable(ch: ZCharacter, slot: ZEquipSlot, types: List<ZMoveType>): Boolean {
		for (ac: ZActionType in arrayOf(
			ZActionType.THROW_ITEM,
			ZActionType.RANGED,
			ZActionType.MELEE,
			ZActionType.MAGIC,
			ZActionType.ENCHANTMENT
		)) {
			val equip = ch.getSlot(slot)
			if (equip != null && equip.type.isActionType(ac)) {
				when (ac) {
					ZActionType.THROW_ITEM -> if (types.contains(ZMoveType.THROW_ITEM)) return true
					ZActionType.RANGED -> if (types.contains(ZMoveType.RANGED_ATTACK)) return true
					ZActionType.MELEE -> if (types.contains(ZMoveType.MELEE_ATTACK)) return true
					ZActionType.MAGIC -> if (types.contains(ZMoveType.MAGIC_ATTACK)) return true
					ZActionType.ENCHANTMENT -> if (types.contains(ZMoveType.ENCHANT)) return true
					else -> Unit
				}
            }
        }
        return false
    }

    fun addHandOptions(ch: ZCharacter, options: MutableList<ZMove>) {
	    options.map { it.type}.let { types ->
		    listOf(ZEquipSlot.LEFT_HAND, ZEquipSlot.RIGHT_HAND, ZEquipSlot.BODY).forEach {
			    if (isMoveAvailable(ch, it, types)) {
				    options.add(ZMove.newUseSlot(it))
			    }
		    }
	    }
    }

    private fun removeDeadZombies() {
	    //for (z: ZActor in Utils.filter(board.getAllActors(), { a: ZActor<*>? -> a is ZZombie })) {
	    board.getAllZombies().forEach { z ->
            if (!z.isAlive && !z.isAnimating) {
                board.removeActor(z)
            }
        }
    }

	/**
	 *
	 * @return true if something changed, false otherwise
	 */
	open suspend fun runGame(): Boolean {
		log.debug("runGame %s", stateStack.joinToString())
		if (!isGameSetup) {
			log.error("Invalid Game")
			require(false)
			return false
		}
		if (isGameOver)
			return false
		quest.getQuestFailedReason(this)?.let {
			gameLost(it)
		    return false
	    }
	    if (quest.getPercentComplete(this) >= 100) {
		    gameWon()
		    return false
	    }

	    removeDeadZombies()
	    if (stateStack.empty())
		    pushState(State(ZState.BEGIN_ROUND))

	    when (state) {
		    ZState.INIT -> {
			    for (cell: ZCell in board.getCells()) {
				    for (type: ZCellType in ZCellType.entries) {
					    if (cell.isCellType(type)) {
						    when (type) {
							    ZCellType.WALKER -> spawnZombies(1, ZZombieType.Walker, cell.zoneIndex)
							    ZCellType.RUNNER -> spawnZombies(1, ZZombieType.Runner, cell.zoneIndex)
							    ZCellType.FATTY -> spawnZombies(1, ZZombieType.Fatty, cell.zoneIndex)
							    ZCellType.NECROMANCER -> spawnZombies(1, ZZombieType.Necromancer, cell.zoneIndex)
							    ZCellType.ABOMINATION -> spawnZombies(1, ZZombieType.Abomination, cell.zoneIndex)
							    ZCellType.RATZ -> spawnZombies(1, ZZombieType.Ratz, cell.zoneIndex)

							    ZCellType.CATAPULT -> board.spawnActor(
								    ZSiegeEngine(
									    cell.zoneIndex,
									    ZSiegeTypeEngineType.CATAPULT
								    )
							    )

							    else -> Unit
						    }
                        }
                    }
                }
			    setState(State(ZState.BEGIN_ROUND))
			    onCurrentUserUpdated(getCurrentUser().name, getCurrentUser().colorId)
                return true
            }
            ZState.BEGIN_ROUND -> {
	            onBeginRound(roundNum)
	            setState(State(if (roundNum > 0) ZState.SPAWN else ZState.PLAYER_STAGE_CHOOSE_CHARACTER))
	            roundNum++
	            addLogMessage("Begin Round $roundNum")
	            onStartRound(roundNum)
	            updateCurrentUser(startUser)
	            for (a: ZActor in board.getAllActors()) a.onBeginRound(this)
	            repeatableMove = null
	            repeatableMovePlayer = null
            }
            ZState.SPAWN -> {

	            // search cells and randomly decide on spawning depending on the
	            // highest skill level of any remaining players
	            val highestSkill = highestSkillLevel
	            val spawnableZones = board.zones.filterIndexed { idx, _ ->
		            board.isZoneSpawnable(idx)
	            }
	            spawnableZones.forEachIndexed { index, zZone ->
		            onSpawnZoneSpawning(GRectangle(zZone), index, spawnableZones.size)
		            spawnZombies(zZone.zoneIndex, highestSkill)
		            onSpawnZoneSpawned(index, spawnableZones.size)
	            }
	            board.resetNoise()
	            setState(State(ZState.PLAYER_STAGE_CHOOSE_CHARACTER))
	            return true
            }
            ZState.PLAYER_STAGE_CHOOSE_CHARACTER -> {
	            // get a pair list of all remaining movable characters and their users
	            repeatableMove = null
	            repeatableMovePlayer = null
	            val map = users.mapIndexed { index: Int, user: ZUser ->
		            user.players.map {
			            Pair(it.toCharacter(), index)
		            }
	            }.flatten().filter {
		            it.first.isAlive && it.first.actionsLeftThisTurn > 0
	            }.sortedBy {
		            // we want to first elems in the list to be currentUser or its closest neightbor that is greater in value
		            // example: if there are 3 players and the current is 1, then the ordering should be:
		            // 1, 2, 0 if the current still
		            (it.second - currentUserIdx + users.size) % users.size
	            }
	            log.debug("remaining characters are: ${map.joinToString("\n") { "${it.second} -> ${it.first.type}" }}")
	            // no characters left?
	            if (map.isEmpty()) {
		            startUser = startUser.increment(users.size)
		            setState(State(ZState.ZOMBIE_STAGE))
		            return true
	            }
	            updateCurrentUser(map[0].second)
	            val options = map.filter { it.second == currentUserIdx }.map { it.first.type }
	            if (options.size == 1) {
		            options[0]
	            } else {
		            getCurrentUser().chooseCharacter(options)
	            }?.let {
		            pushState(State(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, it))
		            return true
	            }
	            return false
            }
            ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION -> {
	            val ch = requireCurrentCharacter
	            repeatableMovePlayer = ch.type
	            if (!ch.isStartingWeaponChosen) {
		            if (ch.type.startingEquipment.size > 1) {
			            val type = getCurrentUser().chooseStartingEquipment(
				            ch.type,
				            listOf(*ch.type.startingEquipment)
			            )
				            ?: return false
			            ch.setStartingEquipment(type)
		            } else {
			            ch.setStartingEquipment(ch.type.startingEquipment[0])
		            }
		            onCurrentCharacterUpdated(null, ch)
	            }
	            if (rules.familiars && !ch.isStartingFamiliarChosen) {
		            val familiarTypesInPlay = board.getAllActors().filter { it is ZFamiliar }.map {
			            it.type
		            }
		            ZFamiliarType.entries.filter { !familiarTypesInPlay.contains(it) }
			            .takeIf { it.isNotEmpty() }?.let { list ->
			            getCurrentUser().chooseFamiliar(ch.type, list)?.let {
				            ch.addFamiliar(it)
			            } ?: return false
		            }

	            }
	            val actionsLeft: Int = ch.actionsLeftThisTurn
	            val options = LinkedHashSet<ZMove>()

	            // determine players available moves
	            for (skill: ZSkill in ch.getAvailableSkills()) {
		            skill.addSpecialMoves(this, ch, options)
	            }

	            options.add(ZMove.newOrganize())

	            if (actionsLeft > 0) {
                    // check for organize
                    val zoneCleared = isClearedOfZombies(ch.occupiedZone)

                    // check for trade with another character in the same zone (even if they are dead)
                    if (!isOrganizeEnabled && zoneCleared) {
                        addTradeOptions(ch, options)
                    }

                    // add any moves determined by the quest
		            quest.addMoves(this, ch, options)
		            val zone = board.getZone(ch.occupiedZone)

		            // check for search
		            if (zoneCleared && ch.canSearch(zone)) {
			            options.add(ZMove.newSearchMove(ch.occupiedZone))
		            }
		            options.add(ZMove.newMakeNoiseMove(ch.occupiedZone))

		            // check for move up, down, right, left
		            addWalkOptions(ch, options)
		            ch.meleeWeapons.toSet().takeIf { it.isNotEmpty() }?.let {
			            options.add(ZMove.newMeleeAttackMove(it.toList()))
		            }
		            ch.rangedWeapons.toSet().takeIf { it.isNotEmpty() }?.let { ranged ->
			            options.add(ZMove.newRangedAttackMove(ranged.toList()))
			            for (slot: ZWeapon in ranged) {
				            if (!slot.isLoaded) {
					            options.add(ZMove.newReloadMove(slot))
				            }
			            }
		            }
		            ch.magicWeapons.toSet().takeIf { it.isNotEmpty() }?.let {
			            options.add(ZMove.newMagicAttackMove(it.toList()))
		            }
		            ch.throwableEquipment.toSet().takeIf { it.isNotEmpty() }?.let {
			            options.add(ZMove.newThrowEquipmentMove(it.toList()))
		            }
		            ch.spells.toSet().takeIf { it.isNotEmpty() }?.let {
			            options.add(ZMove.newEnchantMove(it.toList()))
		            }
		            if (zone.type === ZZoneType.VAULT) {
			            quest.getVaultItems(ch.occupiedZone).toSet().takeIf { it.isNotEmpty() }
				            ?.let {
					            options.add(ZMove.newPickupItemMove(it.toList()))
				            }
			            ch.allEquipment.toSet().takeIf { it.isNotEmpty() }?.let {
				            options.add(ZMove.newDropItemMove(it.toList()))
			            }
		            }
		            val doors: MutableList<ZDoor> = ArrayList()
                    val barricadeDoors: MutableList<ZDoor> = ArrayList()
                    for (door: ZDoor in zone.doors) {
                        if (!door.isClosed(board) && !door.canBeClosed(ch)) {
                            if (ch.canBarricadeDoors()) {
                                barricadeDoors.add(door)
                            }
                            continue
                        }
	                    if (door.isJammed && !ch.canUnjamDoor()) continue
	                    if (door.isLocked(board)) continue
	                    doors.add(door)
                    }
		            if (doors.isNotEmpty()) {
			            options.add(ZMove.newToggleDoor(doors))
		            }
		            if (barricadeDoors.isNotEmpty() && ch.actionsLeftThisTurn >= ZActionType.BARRICADE_DOOR.costPerTurn) {
			            options.add(ZMove.newBarricadeDoor(barricadeDoors))
		            }
		            board.getActor(ZSiegeTypeEngineType.CATAPULT.name)
			            ?.takeIfInstance<ZSiegeEngine>()?.let { catapult ->
			            if (ch.actionsLeftThisTurn >= catapult.type.costPerAction) {
				            if (catapult.occupiedZone == ch.occupiedZone) {
					            board.getAccessibleZones(catapult, 1, 1, ZActionType.CATAPULT_MOVE)
						            .filter {
							            board.getActorsInZone(it).count { it.isSiegeEngine } == 0
						            } // only 1 siege engine per zone
						            .takeIf { it.isNotEmpty() }?.let {
							            options.add(ZMove.newMoveSiegeEngine(ch.type, it))
						            }

					            // for firing a catapult, find all zones with at least 1 zombie of any type or a destructable spawn zone
					            board.zones.filter { board.isZoneTargetForCatapult(it) }.map {
						            it.zoneIndex
					            }.toSet().filter {
						            board.getDistanceBetweenZones(it, ch.occupiedZone) >= 1
					            }.takeIf { it.isNotEmpty() }?.let {
						            options.add(ZMove.newFireCatapultScatterShot(ch.type, it))
						            options.add(ZMove.newFireCatapultGrapeShot(ch.type, it))
						            options.add(ZMove.newFireCatapultBoulder(ch.type, it))
					            }
				            }
			            }
		            }
	            }

	            ch.familiars.filter { it.familiar?.hasMoveOptions() == true }.forEach {
		            options.add(ZMove.newFamiliarMove(ch.type, it.familiar!!))
	            }

	            if (options.size == 0) {
		            setState(State(ZState.PLAYER_STAGE_CHOOSE_CHARACTER))
		            return false
	            }

	            val optionsList = LinkedList(options).also {
		            it.addFirst(ZMove.newEndTurn())
	            }
	            //addHandOptions(ch, optionsList)
                if (canSwitchActivePlayer())
                	optionsList.add(ZMove.newSwitchActiveCharacter())


                // make sure no dups
                var i = 0
                while (i < optionsList.size - 1) {
                    var ii = i + 1
                    while (ii < optionsList.size) {
	                    // We can get duplicates yo!!! Like:
	                    /*
						  ZMove{type=RELOAD, integer=null, equipment=Crossbow, fromSlot=null, toSlot=null, list=null}
						  ZMove{type=RELOAD, integer=null, equipment=Crossbow, fromSlot=null, toSlot=null, list=null}
						 */
	                    if ((optionsList[i] == optionsList[ii])) throw GException("Duplicate options:\n  " + optionsList[i] + "\n  " + optionsList[ii])
	                    ii++
                    }
	                i++
                }
	            if (canUndo())
		            optionsList.add(ZMove.newUndoMove())
	            if (optionsList.firstOrNull { it.action == repeatableMove?.action } == null)
		            repeatableMove = null
	            val move = getCurrentUser().chooseMove(ch.type, optionsList)
	            return performMove(ch, move)
            }
            ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL -> {
	            val ch = requireCurrentCharacter
	            val options = ch.getRemainingSkillsForLevel(stateStack.peek().skillLevel!!.color.ordinal)
	            log.debug("Skill options for " + stateStack.peek().skillLevel + " : " + options)
	            when (options.size) {
		            0 -> {
			            stateStack.pop()
			            null
		            }
		            1 -> options[0]
		            else -> getCurrentUser().chooseNewSkill(ch.type, options)
	            }?.let { skill ->
		            log.debug("New Skill Chosen: $skill")
		            onNewSkillAcquired(ch.type, skill)
                    ch.addSkill(skill)
                    skill.onAcquired(this, ch)
                    options.remove(skill)
                    popState()
                    return true
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT -> {
	            val ch = requireCurrentCharacter
	            val equip = stateEquipment!!
                val options: MutableList<ZMove> = object : ArrayList<ZMove>() {
                    init {
	                    ch.getEquippableSlots(equip, true).takeIf { it.isNotEmpty() }?.forEach {
		                    add(ZMove.newEquipMove(equip, null, it))
	                    } ?: add(ZMove.newKeepMove(equip))
	                    add(ZMove.newDisposeMove(equip))
                    }
                }
	            if (ch.actionsLeftThisTurn > 0 && equip.isConsumable) {
		            options.add(ZMove.newConsumeMove(equip, null))
	            }
	            val move = getCurrentUser().chooseMove(ch.type, options)
	            // need to pop first since performMove might push TODO: Consider remove?
	            popState()
	            if (!performMove(ch, move)) {
		            pushState(State(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, ch.type, equip))
		            return false
	            }
	            return true
            }
            ZState.ZOMBIE_STAGE -> {
	            performZombieStage()
                allLivingCharacters.forEach {
	                it.onEndOfRound(this)
                }
                setState(State(ZState.BEGIN_ROUND))
	            return false
            }
            ZState.PLAYER_ENCHANT_SPEED_MOVE -> {

                // compute all empty of zombie zones 1 or 2 units away form current position
	            val zones =
		            board.getAccessibleZones(requireCurrentCharacter, 1, 2, ZActionType.MOVE)
                if (zones.isEmpty()) {
                    popState()
                } else {
	                getCurrentUser().chooseZoneToWalk(requireCurrentCharacter.type, zones)?.let { speedMove ->
		                moveActor(requireCurrentCharacter, speedMove, 200, null)
		                popState()
		                return true
	                }
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_SPAWN_AREA_TO_REMOVE -> {
                val areas: MutableList<ZSpawnArea> = ArrayList()
                for (cell: ZCell in board.getCells()) {
                    areas.addAll(cell.spawnAreas.filter { a: ZSpawnArea -> a.isCanBeRemovedFromBoard })
                }
                if (areas.isNotEmpty()) {
	                val cur = requireCurrentCharacter
	                getCurrentUser().chooseSpawnAreaToRemove(cur.type, areas)?.let { zIdx ->
		                board.removeSpawn(areas[zIdx])
		                quest.onSpawnZoneRemoved(this, areas[zIdx])
		                onCharacterDestroysSpawn(cur.type, zIdx)
		                popState()
		                return true
	                }
                } else {
                    popState()
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_WEAPON_FROM_DECK -> {
	            val allClasses = allSearchables.map { it.type.equipmentClass }.filter { it != ZEquipmentClass.AHHHH }.toSet()
	            val ch = requireCurrentCharacter
	            getCurrentUser().chooseEquipmentClass(ch.type, allClasses.toList())?.let { clazz ->
		            val choices =
			            allSearchables.filter { e: ZEquipment<*> -> e.type.equipmentClass === clazz }
				            .toSet()
		            getCurrentUser().chooseEquipmentInternal(ch.type, choices.toList())
			            ?.let { equip ->
				            popState()
				            lootDeck.remove(equip)
				            giftEquipment(ch, equip)
				            return true
			            }
	            }
	            return false
            }
            ZState.PLAYER_STAGE_CHOOSE_VAULT_ITEM -> {
                quest.vaultItemsRemaining.takeIf { it.isNotEmpty() }?.also { items ->
	                getCurrentUser().chooseEquipmentInternal(requireCurrentCharacter.type, items)
		                ?.let {
			                popState()
			                items.remove(it)
			                giftEquipment(requireCurrentCharacter, it)
			                return true
		                }

                }?:run {
                    popState()
                }
	            return false
            }

		    ZState.PLAYER_STAGE_ORGANIZE -> {
			    val ch = requireCurrentCharacter
			    val secondary: ZCharacter? = board.getActor(stateData.target?.name) as ZCharacter?
			    setState(
				    State(
					    state = ZState.PLAYER_STAGE_ORGANIZE,
					    player = ch.type,
					    target = secondary?.type
				    )
			    )
			    getCurrentUser().organizeStart(ch.type, secondary?.type)
			    return performOrganize(ch, secondary)
		    }

		    ZState.PLAYER_STAGE_CHOOSE_FAMILIAR_ACTION -> {
			    val ch = requireCurrentCharacter
			    val familiarType = requireNotNull(stateData.familiar)
			    board.getActor(familiarType.name)?.takeIfInstance<ZFamiliar>()?.let { familiar ->
				    val options = mutableListOf<ZMove>()
				    if (familiar.zoneMovesRemaining > 0) {
					    board.getAccessibleZones(familiar, 1, 1, ZActionType.MOVE)
						    .takeIf { it.isNotEmpty() }?.let {
						    options.add(ZMove.newWalkMove(it, null))
					    }
				    }
				    if (lootDeck.isNotEmpty() && familiar.canSearch()) {
					    options.add(ZMove.newSearchMove(familiar.occupiedZone))
				    }
				    options.add(ZMove.newMeleeAttackMove(listOf(familiar.weapon)))
				    getCurrentUser().chooseMove(ch.type, options)?.let { move ->
					    when (move.type) {
						    ZMoveType.WALK -> return performWalk(familiar, move).also {
							    if (!familiar.hasMoveOptions())
								    popState()
						    }

						    ZMoveType.SEARCH -> {
							    val equip = lootDeck.removeLast()
							    if (equip.type == ZItemType.AHHHH) {
								    TODO()
							    } else if (familiar.occupiedZone == ch.occupiedZone) {
								    // treat this like the user searched
								    TODO()
							    } else {
								    familiar.equipment = equip
							    }
						    }

						    ZMoveType.MELEE_ATTACK -> {
							    familiar.weapon.getStatForAction(ZActionType.MELEE)?.let { stat ->
								    if (performMeleeAttack(
										    familiar,
										    familiar.weapon,
										    stat,
										    familiar.occupiedZone
									    )
								    ) {
									    familiar.performAction(ZActionType.MELEE, this)
									    return true
								    }
							    }

						    }

						    else -> error("Unhandled familiar movev: $move")
					    }
				    }
			    } ?: popState()
		    }
	    }
        return false
    }

	private fun performZombieStage() {
		// for this version we take the approach of trying to move to open spaces
		val pathMap = mutableMapOf<ZZombie, MutableList<ZDir>>()

		fun getPathForZombie(zombie: ZZombie, cb: () -> List<ZDir>): MutableList<ZDir> {
			pathMap[zombie]?.let {
				return it
			}
			var path = cb()
			if (path.isEmpty()) {
				val zones =
					board.getAccessibleZones(zombie, 1, zombie.actionsPerTurn, ZActionType.MOVE)
				if (zones.isEmpty()) {
					path = ArrayList()
				} else {
					val paths = board.getShortestPathOptions(
						zombie.type,
						zombie.occupiedCell,
						zones.random()
					)
					if (paths.isEmpty()) {
						path = ArrayList()
					} else {
						path = paths.random()
					}
				}
			}
			if (path.isNotEmpty()) {
				onZombiePath(zombie.getId(), path)
			}
			return path.toMutableList().also {
				pathMap.put(zombie, it)
			}
		}

		val zombies = board.getAllZombies().filter { !it.destroyed }.toMutableList()

		if (zombies.isEmpty()) {
			return
		}

		onZombieStageBegin()

		// we want to perform all the attacks at the end so the animations work better
		val attackList = mutableListOf<Pair<ZZombie, ZCharacter>>()

		// sort?
		while (zombies.isNotEmpty()) {
			var numMoved = 0
			zombies.removeAll { it.actionsLeftThisTurn == 0 }
			// find a zombie who can move
			zombies.forEach { zombie ->
				tryZombieAttack(zombie)?.let {
					attackList.add(Pair(zombie, it))
				} ?: run {
					val path = if (zombie.type.isNecromancer) {
						if (zombie.startZone != zombie.occupiedZone && board.isZoneEscapableForNecromancers(
								zombie.occupiedZone
							)
						) {
							// necromancer is escaping!
							zombie.performAction(ZActionType.MOVE, this)
							onNecromancerEscaped(zombie.position)
							quest.onNecromancerEscaped(this, zombie)
							board.removeActor(zombie)
							ArrayList()
						} else {
							getPathForZombie(zombie) { board.getZombiePathTowardNearestSpawn(zombie) }
						}
					} else {
						getPathForZombie(zombie) { board.getZombiePathTowardVisibleCharactersOrLoudestZone(zombie) }
					}

					if (path.isEmpty()) {
						zombie.performAction(ZActionType.NOTHING, this)
					}

					// if the zone we are moving too is open, then move. Otherwise if there are no open places anywhere then exit out
					else if (moveActorInDirectionIfPossible(zombie, path.first(), ZActionType.MOVE)) {
						path.removeFirst()
						numMoved++
					}
				}
			}

			zombies.removeAll(attackList.map { it.first })

			if (numMoved == 0)
				zombies.clear()
		}

		onZombieStageMoveDone()

		doZombieAttacks(attackList)

		onZombieStageEnd()
	}

	open val isOrganizeEnabled = false

	@RemoteFunction
	protected open fun onCharacterDestroysSpawn(c: ZPlayerName, zoneIdx: Int) {
		log.debug("%s destroys spawn at %d", c, zoneIdx)
	}

	@RemoteFunction
	protected open fun onCharacterDefends(cur: ZPlayerName, attackerPosition: ZActorPosition) {
		log.debug("%s defends from %s", cur, attackerPosition)
	}

	@RemoteFunction
	protected open fun onNewSkillAcquired(c: ZPlayerName, skill: ZSkill) {
		log.debug("%s acquires new skill %s", c, skill)
	}

	private fun playerDefends(cur: ZCharacter, type: ZZombieType): Boolean {
		for (rating: Int in cur.getArmorRatings(type)) {
			addLogMessage("Defensive roll")
			val dice = rollDice(1)
			if (dice[0] >= rating) return true
		}
		return false
	}

	private fun doZombieAttacks(list: List<Pair<ZZombie, ZCharacter>>) {
		list.forEach { (zombie, victim) ->
			onZombieAttack(zombie.position, victim.type, ZActionType.MELEE)
			zombie.performAction(ZActionType.MELEE, this)
			if (playerDefends(victim, zombie.type)) {
				addLogMessage("${victim.name()} defends against ${zombie.name()}")
				onCharacterDefends(victim.type, zombie.position)
			} else {
				playerWounded(
					victim,
					zombie,
					ZAttackType.FAMILIAR,
					zombie.type.damagePerHit,
					zombie.type.name
				)
			}
		}
	}

	private fun tryZombieAttack(zombie: ZZombie): ZCharacter? {
		val victims = board.getCharactersInZone(zombie.occupiedZone).filter {
			!it.isInvisible && it.isAlive
		}
		if (victims.size > 1) {
			Collections.sort(victims, WoundingComparator(zombie.type))
		}
		if (victims.isNotEmpty()) {
			return victims[0]
		}
		return null
	}

	private fun tryZombieAttackNow(zombie: ZZombie) {
		val victims = board.getCharactersInZone(zombie.occupiedZone).filter {
			!it.isInvisible && it.isAlive
		}
		if (victims.size > 1) {
			Collections.sort(victims, WoundingComparator(zombie.type))
		}
		if (victims.isNotEmpty()) {
			val victim = victims[0]
			onZombieAttack(zombie.position, victim.type, ZActionType.MELEE)
			zombie.performAction(ZActionType.MELEE, this)
			if (playerDefends(victim, zombie.type)) {
				addLogMessage("${victim.name()} defends against ${zombie.name()}")
				onCharacterDefends(victim.type, zombie.position)
			} else {
				playerWounded(
					victim,
					zombie,
					ZAttackType.FAMILIAR,
					zombie.type.damagePerHit,
					zombie.type.name
				)
			}
		}
	}

	@RemoteFunction
	protected open fun onZombieAttack(zombiePos: ZActorPosition, victim: ZPlayerName, type: ZActionType) {}

	fun gameLost(msg: String) {
		gameOverStatus = GAME_LOST
		addLogMessage(("Game Lost $msg").trim { it <= ' ' })
		onGameLost()
	}

	fun gameWon() {
		gameOverStatus = GAME_WON
		addLogMessage("Game Won!!!")
		onQuestComplete()
	}

	@RemoteFunction
	protected open fun onGameLost() {
		log.debug("GAME LOST")
	}

	fun playerWounded(victim: ZCharacter, attacker: ZActor, attackType: ZAttackType, amount: Int, reason: String) {
		if (!victim.isDead) {
			victim.wound(amount, attackType)
			if (victim.isDead) {
				victim.clearActions()
				addLogMessage(victim.name() + " has been killed by a " + reason)
				onCharacterAttacked(victim.type, attacker.position, attackType, true)
				//removeCharacter(victim);
			} else {
				addLogMessage(victim.name() + " has been wounded by a " + reason)
				onCharacterAttacked(victim.type, attacker.position, attackType, false)
            }
        }
    }

	@RemoteFunction
	protected open fun onCharacterAttacked(character: ZPlayerName, attackerPosition: ZActorPosition, attackType: ZAttackType, characterPerished: Boolean) {
		log.debug("%s attacked from %s with %s and %s", character, attackerPosition, attackType, if (characterPerished) "Died" else "Was Wounded")
	}

	@RemoteFunction
	open fun onEquipmentThrown(c: ZPlayerName, icon: ZIcon, zone: Int) {
		log.debug("%s throws %s into %d", c, icon, zone)
	}

	@RemoteFunction
	protected open fun onStartRound(roundNum: Int) {}

	private suspend fun useEquipment(c: ZCharacter, e: ZEquipment<*>): Boolean {
		if (e.isMagic) {
			return performMove(c, ZMove.newMagicAttackMove(listOf(e as ZWeapon)))
		} else if (e.isMelee) {
			return performMove(c, ZMove.newMeleeAttackMove(listOf(e as ZWeapon)))
		} else if (e.isRanged) {
			return performMove(c, ZMove.newRangedAttackMove(listOf(e as ZWeapon)))
		} else if (e.isThrowable) {
			return performMove(c, ZMove.newThrowEquipmentMove(listOf<ZEquipment<*>>(e as ZItem)))
		}
		return false
	}

	private suspend fun performWalk(cur: ZActor, move: ZMove): Boolean {
		val zone = move.integer ?: getCurrentUser().chooseZoneToWalk(
			requireCurrentCharacter.type,
			move.list as List<Int>
		)
		if (zone != null) {
			moveActor(cur, zone, cur.moveSpeed, move.action)
			return true
			//cur.performAction(ZActionType.MOVE, this);
		}
		return false

	}

	private suspend fun performMove(cur: ZCharacter, _move: ZMove?): Boolean {
		val move: ZMove = _move ?: return false
		if (move != repeatableMove)
			repeatableMove = null
		log.debug("performMove:%s", move)
		val user = getCurrentUser()
		when (move.type) {
			ZMoveType.END_TURN -> {
				cur.clearActions()
				popState()
				return true
			}

			ZMoveType.SWITCH_ACTIVE_CHARACTER -> {
				if (canSwitchActivePlayer()) {
					val map = users.mapIndexed { index, _user ->
						_user.players.map {
							Pair(it.toCharacter(), index)
						}
					}.flatten()
						.filter { it.first.type == currentCharacter?.type || it.first.isAlive && it.first.actionsLeftThisTurn > 0 }
					if (map.isEmpty())
						return false
					val idx =
						(map.indexOfFirst { it.first.type == currentCharacter?.type } + 1) % map.size
					if (idx < 0)
						return false
	                map.getOrNull(idx)?.let {
		                popState()
		                updateCurrentUser(it.second)
		                pushState(State(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, it.first.type))
	                }
                }
                return false
            }
            ZMoveType.TAKE_OBJECTIVE -> {
                cur.performAction(ZActionType.OBJECTIVE, this)
                val zone = board.getZone(cur.occupiedZone)
                zone.isObjective = false
	            for (pos: Pos in zone.cells) {
		            ZCellType.entries.filter { type: ZCellType -> type.isObjective }
			            .forEach { ct ->
				            board.getCell(pos).setCellType(ct, false)
			            }
	            }
                addLogMessage(cur.name() + " Found an OBJECTIVE")
                quest.processObjective(this, cur)
                return true
            }
	        ZMoveType.ORGANIZE -> {
		        if (isOrganizeEnabled) {
			        pushState(State(ZState.PLAYER_STAGE_ORGANIZE, currentCharacter?.type))
			        return true
		        }

                // give options of which slot to organize
                val slots: MutableList<ZEquipSlot> = ArrayList()
                if (cur.leftHand != null) slots.add(ZEquipSlot.LEFT_HAND)
                if (cur.rightHand != null) slots.add(ZEquipSlot.RIGHT_HAND)
                if (cur.body != null) slots.add(ZEquipSlot.BODY)
                if (cur.numBackpackItems > 0) slots.add(ZEquipSlot.BACKPACK)
                val selectedSlot = getCurrentUser().chooseSlotToOrganize(cur.type, slots)
                        ?: return false
                // choose which equipment from the slot to organize
		        val selectedEquipment: ZEquipment<out ZEquipmentType> = when (selectedSlot) {
			        ZEquipSlot.BACKPACK -> if (cur.numBackpackItems > 1) {
				        // add
				        getCurrentUser().chooseEquipmentInternal(cur.type, cur.getBackpack())
			        } else {
				        cur.getBackpack().first()
			        }

			        ZEquipSlot.BODY -> cur.body
			        ZEquipSlot.LEFT_HAND -> cur.leftHand
			        ZEquipSlot.RIGHT_HAND -> cur.rightHand
		        } ?: return false

		        // we have a slot and an equipment from the slot to do something with
		        // we can:
                //   dispose, unequip, equip to an empty slot or consume
                val options: MutableList<ZMove> = ArrayList()
                if (selectedEquipment.isConsumable && cur.actionsLeftThisTurn > 0) {
                    options.add(ZMove.newConsumeMove(selectedEquipment, selectedSlot))
                }
                when (selectedSlot) {
                    ZEquipSlot.BACKPACK -> if (selectedEquipment.isEquippable(cur)) {
	                    for (slot: ZEquipSlot in cur.getEquippableSlots(selectedEquipment, false)) {
		                    options.add(ZMove.newEquipMove(selectedEquipment, selectedSlot, slot, ZActionType.INVENTORY))
	                    }
                    }
                    ZEquipSlot.RIGHT_HAND, ZEquipSlot.LEFT_HAND, ZEquipSlot.BODY -> {
                        if (!cur.isBackpackFull) {
                            options.add(ZMove.newUnequipMove(selectedEquipment, selectedSlot))
                        }
                    }
                }
                options.add(ZMove.newDisposeMove(selectedEquipment, selectedSlot))
		        user.chooseMove(cur.type, options)?.let {
			        return performMove(cur, it)
		        }
                return false
            }
            ZMoveType.TRADE -> {
	            when (move.list!!.size) {
		            1 -> move.list[0] as ZPlayerName
		            else -> user.chooseTradeCharacter(cur.type, move.list as List<ZPlayerName>)
	            }?.toCharacter()?.let { other ->
		            val options: MutableList<ZMove> = ArrayList()
		            // we can take if our backpack is not full or give if their backpack is not full
		            for (eq: ZEquipment<*> in cur.allEquipment) {
			            if (other.canTake(eq)) {
				            options.add(ZMove.newGiveMove(other.type, eq, null))
			            }
		            }
		            for (eq: ZEquipment<*> in other.allEquipment) {
			            if (cur.canTake(eq)) {
				            options.add(ZMove.newTakeMove(other.type, eq, null))
			            }
		            }
		            user.chooseMove(cur.type, options)?.let { move ->
			            return performMove(cur, move)
		            }
	            }
	            return false
            }

			ZMoveType.WALK -> return performWalk(cur, move)
			ZMoveType.JUMP -> {
				var zone = move.integer
				if (zone == null) zone = user.chooseZoneToWalk(cur.type, move.list as List<Int>)
				if (zone != null) {
					moveActor(cur, zone, cur.moveSpeed / 2, null)
					cur.removeAvailableSkill(ZSkill.Jump)
					return true
					//cur.performAction(ZActionType.MOVE, this);
				}
				return false
            }
            ZMoveType.IGNITE -> {
                var zoneToIgnite = move.integer
                if (zoneToIgnite == null) {
                    val ignitableZones: List<Int> = move.list as List<Int>
                    if (ignitableZones.size == 1) {
                        zoneToIgnite = ignitableZones[0]
                    }
                }
                if (zoneToIgnite == null) {
                    zoneToIgnite = user.chooseZoneToIgnite(cur.type, move.list as List<Int>)
                }
	            if (zoneToIgnite != null) {
		            performDragonFire(cur, zoneToIgnite)
		            return true
	            }
	            return false
            }
	        ZMoveType.WALK_DIR -> {
		        moveActorInDirection(cur, ZDir.entries[move.integer!!], move.action)
		        return true
	        }
	        ZMoveType.USE_SLOT -> {
		        return cur.getSlot(move.fromSlot!!)?.let {
			        useEquipment(cur, it)
			        true
		        } ?: false
	        }
	        ZMoveType.MELEE_ATTACK -> {
		        val weapons: List<ZWeapon> = move.list as List<ZWeapon>
		        when (weapons.size) {
			        1 -> weapons[0]
			        else -> user.chooseWeaponSlotInternal(cur.type, weapons)
		        }?.let { weapon ->
			        cur.getWeaponStat(weapon, ZActionType.MELEE, this, cur.occupiedZone)
				        ?.let { stat ->
					        if (performAttack(cur, weapon, stat, cur.occupiedZone)) {
						        cur.performAction(ZActionType.MELEE, this)
						        return true
					        }
				        }
		        }
                return false
            }
            ZMoveType.MAGIC_ATTACK -> {
                return performRangedOrMagicAttack(cur, move.list as List<ZWeapon>, move.integer, ZActionType.MAGIC)
            }
            ZMoveType.RANGED_ATTACK -> {
                return performRangedOrMagicAttack(cur, move.list as List<ZWeapon>, move.integer, ZActionType.RANGED)
            }
            ZMoveType.THROW_ITEM -> {
                val slots: List<ZEquipment<*>> = move.list as List<ZEquipment<*>>
                when (slots.size) {
                    1 -> slots[0]
                    else -> getCurrentUser().chooseEquipmentToThrowInternal(cur.type, slots)
                }?.let { slot ->
                    val zoneIdx: Int?
                    if (move.integer != null) {
                        zoneIdx = move.integer
                    } else {
	                    board.getAccessibleZones(
		                    cur,
		                    slot.type.throwMinRange,
		                    slot.type.throwMaxRange,
		                    ZActionType.THROW_ITEM
	                    ).toMutableList().apply {
		                    add(cur.occupiedZone)
		                    zoneIdx =
			                    getCurrentUser().chooseZoneToThrowEquipment(cur.type, slot, this)
	                    }
                    }
                    if (zoneIdx != null) {
                        slot.type.onThrown(this, cur, zoneIdx)
                        cur.removeEquipment(slot)
                        cur.performAction(ZActionType.THROW_ITEM, this)
                        putBackInSearchables(slot)
                        return true
                    }
                }
                return false
            }
            ZMoveType.RELOAD -> {
	            val weapon = move.equipment as ZWeapon
	            if (cur.isDualWielding(weapon)) {
		            cur.weapons.filter { it.type == weapon.type }.forEach { it.reload() }
		            addLogMessage("${requireCurrentCharacter.type} Reloaded all their ${weapon.getLabel()}s")
	            } else {
		            weapon.reload()
		            addLogMessage("${requireCurrentCharacter.type} Reloaded their ${weapon.getLabel()}")
	            }
	            cur.performAction(ZActionType.RELOAD, this)
	            return true
            }
            ZMoveType.OPERATE_DOOR -> {
                val doors: List<ZDoor> = move.list as List<ZDoor>
                when (doors.size) {
                    1    -> doors[0]
                    else -> user.chooseDoorToToggleInternal(cur.type, doors)
                }?.let { door ->
                    if (door.isClosed(board)) {
                        if (tryOpenDoor(cur, door)) {
                        	quest.onDoorOpened(this, door, cur)
                            door.toggle(board)
	                        onDoorToggled(cur.type, door)
                            //showMessage(currentCharacter.name() + " has opened a " + door.name());
                            // spawn zombies in the newly exposed zone and any adjacent
                            val otherSide = door.otherSide
                            if (board.getZone(board.getCell(otherSide.cellPosStart).zoneIndex).canSpawn()) {
	                            val highest = highestSkillLevel
	                            val spawnZones = HashSet<Int>()
	                            board.getUndiscoveredIndoorZones(otherSide.cellPosStart, spawnZones)
	                            log.debug("Zombie spawn zones: $spawnZones")
	                            onSpawnZoneSpawning(GRectangle().apply {
		                            spawnZones.forEach {
			                            addEq(board.getZone(it))
		                            }
	                            }, 0, 1)
	                            for (zone: Int in spawnZones) {
		                            spawnZombies(zone, highest)
	                            }
	                            onSpawnZoneSpawned(0, 1)
                            }
                        }
                        cur.performAction(ZActionType.OPEN_DOOR, this)
                    } else {
                        cur.performAction(ZActionType.CLOSE_DOOR, this)
                        door.toggle(board)
                    }
                    return true
                }
                return false
            }
            ZMoveType.BARRICADE -> {
                val doors: List<ZDoor> = move.list as List<ZDoor>
                when (doors.size) {
                    1 -> doors[0]
                    else -> user.chooseDoorToToggleInternal(cur.type, doors)
                }?.let { door ->
                    require(!door.isClosed(board))
	                val barricade = requireNotNull(cur.getEquipmentOfType(ZItemType.BARRICADE))
                    door.toggle(board, true)
	                onDoorToggled(cur.type, door)
                    cur.removeEquipment(barricade)
                    putBackInSearchables(barricade)
                    cur.performAction(ZActionType.BARRICADE_DOOR, this)
                    return true
                }
                return false
            }
            ZMoveType.SEARCH -> {

                // draw from top of the deck
                var numCardsDrawn = if (cur.isEquipped(ZItemType.TORCH)) 2 else 1
                val found: MutableList<ZEquipment<*>> = ArrayList()
                while (lootDeck.isNotEmpty() && numCardsDrawn-- > 0) {
                    val equip = lootDeck.removeLast()
                    if (equip.type === ZItemType.AHHHH) {
                        addLogMessage("Aaaahhhh!!!")
                        onAhhhhhh(cur.type)
                        // spawn zombie right here right now and give an activation
                        spawnZombies(1, ZZombieType.Walker, cur.occupiedZone)
	                    board.getZombiesInZone(cur.occupiedZone).forEach {
		                    tryZombieAttackNow(it)
	                    }
                        //spawnZombies(cur.occupiedZone)
                        putBackInSearchables(equip)
	                    break // stop the search
                    } else {
                        found.add(equip)
                        quest.onEquipmentFound(this, equip)
                        pushState(State(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur.type, equip))
	                    if (equip.isDualWieldCapable && cur.hasAvailableSkill(ZSkill.Matching_set)) {
		                    onBonusAction(cur.type, ZSkill.Matching_set)
		                    pushState(
			                    State(
				                    ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT,
				                    cur.type,
				                    equip.type.create()
			                    )
		                    )
	                    }
                    }
                }
                if (found.isNotEmpty()) onEquipmentFound(cur.type, found)
                cur.performAction(ZActionType.SEARCH, this)
                return true
            }
            ZMoveType.EQUIP -> {
                val prev = cur.getSlot(move.toSlot!!)
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment!!, move.fromSlot)
                }
                cur.attachEquipment(move.equipment!!, move.toSlot)
                if (prev != null) {
                	if (!cur.isBackpackFull) {
		                cur.attachEquipment(prev, ZEquipSlot.BACKPACK)
	                } else {
	                	putBackInSearchables(prev)
	                }
                }
	            move.action?.let {
		            cur.performAction(it, this)
	            }
                return true
            }
            ZMoveType.UNEQUIP -> {
	            cur.removeEquipment(move.equipment!!, move.fromSlot!!)
	            cur.attachEquipment(move.equipment, ZEquipSlot.BACKPACK)
	            cur.performAction(ZActionType.INVENTORY, this)
                return true
            }
            ZMoveType.TAKE -> {
	            move.character?.toCharacter()?.let { giver ->
		            val slot = giver.removeEquipment(move.equipment!!)
		            cur.attachEquipment(move.equipment, move.toSlot)?.let { prevEquipped ->
			            if (giver.canEquip(slot, prevEquipped)) {
				            giver.attachEquipment(prevEquipped, slot)
			            } else if (cur.canEquip(ZEquipSlot.BACKPACK, prevEquipped)) {
				            cur.attachEquipment(prevEquipped, ZEquipSlot.BACKPACK)
			            } else {
				            putBackInSearchables(prevEquipped)
			            }
		            }
		            cur.performAction(ZActionType.INVENTORY, this)
	            }
	            return true
            }
            ZMoveType.GIVE -> {
                val slot = cur.removeEquipment(move.equipment!!)
	            move.character?.toCharacter()?.let { taker ->
		            taker.attachEquipment(move.equipment, move.toSlot)?.let { prevEquipped ->
			            // we positioned onto existing.
			            // 1. Try to swap with current
			            if (cur.canEquip(slot, prevEquipped)) {
				            cur.attachEquipment(prevEquipped, slot)
			            } else if (taker.canEquip(ZEquipSlot.BACKPACK, prevEquipped)) {
				            taker.attachEquipment(prevEquipped, ZEquipSlot.BACKPACK)
			            } else {
				            // dispose
				            putBackInSearchables(prevEquipped)
			            }
		            }
		            cur.performAction(ZActionType.INVENTORY, this)
		            return true
	            }
	            return false
            }
            ZMoveType.DISPOSE -> {
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment!!, move.fromSlot)
                    cur.performAction(ZActionType.INVENTORY, this)
                }
                putBackInSearchables(move.equipment!!)
                return true
            }
            ZMoveType.KEEP -> {
	            val equip = move.equipment!!
	            cur.getEmptyEquipSlotsFor(equip).firstOrNull()?.let { slot ->
		            cur.attachEquipment(equip, slot)
		            return true
	            } ?: run {
		            // need to make room
		            val options: MutableList<ZMove> = ArrayList()
		            for (e: ZEquipment<*> in cur.getBackpack()) {
			            options.add(ZMove.newDisposeMove(e, ZEquipSlot.BACKPACK))
		            }
		            when (equip.slotType) {
			            ZEquipSlotType.BODY -> options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.BODY)!!, ZEquipSlot.BODY))
			            ZEquipSlotType.HAND -> {
				            options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.LEFT_HAND)!!, ZEquipSlot.LEFT_HAND))
				            options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.RIGHT_HAND)!!, ZEquipSlot.RIGHT_HAND))
		                    if (cur.canEquipBody(equip)) {
			                    options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.BODY)!!, ZEquipSlot.BODY))
		                    }
	                    }
	                    else -> Unit
                    }
		            getCurrentUser().chooseMove(cur.type, options)?.let { move ->
			            cur.removeEquipment(move.equipment!!, move.fromSlot!!)
			            putBackInSearchables(move.equipment)
			            //slot = move.fromSlot
		            }
                }
            }
            ZMoveType.CONSUME -> {
                val item = move.equipment as ZItem
                val slot = move.fromSlot
	            item.type.consume(cur, this)
                if (slot != null) {
                    cur.removeEquipment(item, slot)
                }
                cur.performAction(ZActionType.CONSUME, this)
                putBackInSearchables(item)
                return true
            }
            ZMoveType.PICKUP_ITEM -> {
                val equip = getCurrentUser().chooseItemToPickupInternal(cur.type, move.list as List<ZEquipment<*>>)
                if (equip != null) {
                    if (cur.tryEquip(equip) == null) {
                        val keep = ZMove.newKeepMove(equip)
                        if (!performMove(cur, keep)) return false
                    }
                    quest.pickupItem(cur.occupiedZone, equip)
                    cur.performAction(ZActionType.PICKUP_ITEM, this)
                    return true
                }
                return false
            }
            ZMoveType.DROP_ITEM -> {
                val equip = getCurrentUser().chooseItemToDropInternal(cur.type, move.list as List<ZEquipment<*>>)
                if (equip != null) {
                    quest.dropItem(cur.occupiedZone, equip)
                    cur.removeEquipment(equip)
                    cur.performAction(ZActionType.DROP_ITEM, this)
                    return true
                }
                return false
            }
            ZMoveType.MAKE_NOISE -> {
                val maxNoise = board.getMaxNoiseLevel()
                addNoise(move.integer!!, maxNoise + 1 - board.getZone(move.integer).noiseLevel)
                addLogMessage(cur.name() + " made alot of noise to draw the zombies!")
                cur.performAction(ZActionType.MAKE_NOISE, this)
                return true
            }
            ZMoveType.SHOVE -> {
	            when (move.list!!.size) {
		            1 -> move.list[0] as Int
		            else -> getCurrentUser().chooseZoneToShove(cur.type, move.list as List<Int>)
	            }?.let { targetZone ->
		            // shove all zombies in this zone into target zone
		            for (z: ZZombie in board.getZombiesInZone(cur.occupiedZone)) {
			            moveActor(z, targetZone, 300, null)
		            }
		            cur.performAction(ZActionType.SHOVE, this)
		            return true
	            }
	            return false
            }
            ZMoveType.ENCHANT -> {
                when (move.list!!.size) {
	                1 -> move.list[0] as ZSpell
	                else -> getCurrentUser().chooseSpell(cur.type, move.list as List<ZSpell>)
                }?.let { spell ->
	                allLivingCharacters.filter { board.canSee(cur.occupiedZone, it.occupiedZone) }.map {
		                it.type
	                }.let { targets ->
		                user.chooseCharacterForSpell(cur.type, spell, targets)?.let { target ->
			                spell.type.doEnchant(this, target.toCharacter()) //target.availableSkills.add(spell.type.skill);
			                cur.performAction(ZActionType.ENCHANTMENT, this)
			                cur.useSpell(spell.type)
			                return true
		                }
	                }
                }
                return false
            }
            ZMoveType.BORN_LEADER -> {
	            move.character
		            ?: getCurrentUser().chooseCharacterToBequeathMove(cur.type, move.list as List<ZPlayerName>)?.toCharacter()?.let { chosen ->
			            if (chosen.actionsLeftThisTurn > 0) {
				            chosen.addExtraAction()
			            } else {
				            chosen.addAvailableSkill(ZSkill.Plus1_Action)
			            }
			            cur.performAction(ZActionType.BEQUEATH_MOVE, this)
			            return true
		            }
	            return false
            }
            ZMoveType.BLOODLUST_MELEE,
            ZMoveType.BLOODLUST_MAGIC,
            ZMoveType.BLOODLUST_RANGED -> return performBloodlust(cur, move)
	        ZMoveType.CLOSE_SPAWN_PORTAL -> return performCloseSpawnPortal(cur, move)
	        ZMoveType.ORGANIZE_DONE -> {
		        if (state == ZState.PLAYER_STAGE_ORGANIZE)
			        popState()
		        user.organizeEnd()
		        return true
	        }
	        ZMoveType.ORGANIZE_SLOT -> {
		        getCurrentUser().chooseOrganize(cur.type, move.list as List<ZMove>)?.let {
			        return performMove(cur, it)
		        }
	        }
	        ZMoveType.ORGANIZE_TRADE -> {
		        setState(State(state = ZState.PLAYER_STAGE_ORGANIZE, player = currentCharacter?.type, target = move.character))
	        }
			ZMoveType.ORGANIZE_TAKE -> {
				stateData.target?.toCharacter()?.let { char ->
					char.removeEquipment(move.equipment!!)
					cur.attachEquipment(move.equipment, move.toSlot)
					cur.performAction(ZActionType.INVENTORY, this)
					return true
				}
			}
			ZMoveType.CHARGE -> {
				board.getAccessibleZones(cur, 1, 2, ZActionType.MOVE).filter {
					board.getNumZombiesInZone(it) > 0
				}.takeIf { it.isNotEmpty() }?.let {
					(if (it.size == 1) it[0] else getCurrentUser().chooseZoneToWalk(
						cur.type,
						it
					))?.let { zone ->
						moveActor(cur, zone, cur.moveSpeed / 2, null)
						return true
						// free action
					}
				}
			}

			ZMoveType.CATAPULT_FIRE_SCATTERSHOT -> {
				getCurrentUser().chooseZoneForCatapult(
					cur.type,
					ZWeaponType.SCATTERSHOT,
					move.list as List<Int>
				)?.let {
					return performLaunchCatapult(cur, it, ZWeaponType.SCATTERSHOT)
				}
			}

			ZMoveType.CATAPULT_FIRE_GRAPESHOT -> {
				getCurrentUser().chooseZoneForCatapult(
					cur.type,
					ZWeaponType.GRAPESHOT,
					move.list as List<Int>
				)?.let {
					performLaunchCatapult(cur, it, ZWeaponType.GRAPESHOT)
					return true
				}
			}

			ZMoveType.CATAPULT_FIRE_BOULDER -> {
				getCurrentUser().chooseZoneForCatapult(
					cur.type,
					ZWeaponType.BOULDER,
					move.list as List<Int>
				)?.let {
					performLaunchCatapult(cur, it, ZWeaponType.BOULDER)
					return true
				}
			}

			ZMoveType.SIEGE_ENGINE_MOVE -> {
				getCurrentUser().chooseZoneToWalk(cur.type, move.list as List<Int>)
					?.let { zoneIdx ->
						val catapult =
							board.getActorsInZone(cur.occupiedZone).first { it.isSiegeEngine }
						moveActor(catapult, zoneIdx, catapult.moveSpeed, ZActionType.CATAPULT_MOVE)
						moveActor(cur, zoneIdx, catapult.moveSpeed, ZActionType.CATAPULT_MOVE)
						return true
					}
			}

			ZMoveType.BALLISTA_FIRE_BOLT -> {
				TODO()
			}

			ZMoveType.FAMILIAR_MOVE -> {
				pushState(
					State(
						state = ZState.PLAYER_STAGE_CHOOSE_FAMILIAR_ACTION,
						player = cur.type,
						familiar = move.familiar
					)
				)
			}

			ZMoveType.KEEP_ROLL,
			ZMoveType.REROLL -> log.error("Unhandled move: %s", move.type)

			ZMoveType.UNDO -> {
				undo()
			}
		}
		return false
	}

	@RemoteFunction
	protected open fun onCatapultFired(
		pl: ZPlayerName,
		fromZone: Int,
		toZone: Int,
		type: ZWeaponType
	) = Unit

	private suspend fun performLaunchCatapult(
		cur: ZCharacter,
		zoneIdx: Int,
		type: ZWeaponType
	): Boolean {
		with(type.create()) {
			cur.getWeaponStat(this, ZActionType.CATAPULT_FIRE, this@ZGame, zoneIdx)?.let { stat ->
				if (board.getZone(zoneIdx).type == ZZoneType.HOARD) {
					performHoardAttack(zoneIdx, type)
				} else if (performAttack(cur, this, stat, zoneIdx)) {
					cur.performAction(ZActionType.CATAPULT_FIRE, this@ZGame)
					return true
				}
			}
		}
		return false
	}

	private fun performHoardAttack(zoneIdx: Int, weapon: ZWeaponType) {
		val hoardZombies: MutableList<ZZombieType> =
			board.getHoard().map { (type, count) -> Array(count) { type }.toList() }.flatten()
				.sortedBy {
					it.targetingPriority
				}.toMutableList()

		if (hoardZombies.isEmpty())
			return

		val stat: ZWeaponStat = weapon.stats.first { it.actionType == ZActionType.CATAPULT_FIRE }

		weapon.skillsWhileEquipped.forEach {
			it.modifyStat(stat, ZActionType.CATAPULT_FIRE, requireCurrentCharacter, this, zoneIdx)
		}

		val hits = mutableListOf<ZZombieType>()
		rollDice(stat.numDice).forEach {
			if (it >= stat.dieRollToHit && stat.damagePerHit >= hoardZombies.first().minDamageToDestroy) {
				hits.add(hoardZombies.removeFirst())
			}
		}

		onAttack(
			requireCurrentCharacter.type,
			weapon,
			ZActionType.CATAPULT_FIRE,
			stat.numDice,
			listOf(),
			zoneIdx
		)

		hits.forEach {
			board.addToHoard(it, -1)
		}

		onZombieHoardAttacked(requireCurrentCharacter.type, hits)
	}

	@RemoteFunction
	protected open fun onZombieHoardAttacked(player: ZPlayerName, hits: List<ZZombieType>) {}

	// TODO: Add 'incoming equipment' parameter to allow for the popup when items found, picked up, gifted etc.
	private suspend fun performOrganize(cur: ZCharacter, secondary: ZCharacter?): Boolean {
		// find all the players we can trade with
		val moves: MutableMap<ZPlayerName, MutableMap<ZEquipSlot, MutableList<ZMove>>> =
			mutableMapOf()

		fun getListFor(name: ZPlayerName, slot: ZEquipSlot): MutableList<ZMove> =
			moves.getOrPut(
				name
			) { mutableMapOf() }.getOrPut(
				slot
			) { mutableListOf() }

		val extraMoves = mutableListOf<ZMove>()
		
		// for each non-null primary / secondary slots, compute all the possible move they can execute

		if (cur.canDoAction(ZActionType.INVENTORY) && board.getNumZombiesInZone(cur.occupiedZone) == 0) {
			// we can trade
			board.getCharactersInZone(cur.occupiedZone).filter {
				it.type != cur.type && it.type != secondary?.type
			}.forEach {
				extraMoves.add(ZMove.newOrganizeTrade(it.type))
			}

			secondary?.let { sec ->
				sec.allEquipment.forEach { equip ->
					cur.getEmptyEquipSlotsFor(equip).forEach { slot ->
						getListFor(sec.type, equip.slot!!).add(ZMove.newOrganizeTakeMove(cur.type, equip, slot))
					}
				}

				cur.allEquipment.forEach { equip ->
					sec.getEmptyEquipSlotsFor(equip).forEach { slot ->
						getListFor(cur.type, equip.slot!!).add(ZMove.newGiveMove(sec.type, equip, slot))
					}
				}
			}
		}

		if (cur.canDoAction(ZActionType.INVENTORY)) {
			cur.allEquipment.forEach { equip ->
				getListFor(cur.type, equip.slot!!).add(ZMove.newDisposeMove(equip, equip.slot))
				cur.getEquippableSlots(equip, false).filter { it != equip.slot }.forEach { slot ->
					getListFor(cur.type, equip.slot!!).add(ZMove.newEquipMove(equip, equip.slot, slot, ZActionType.INVENTORY, cur.type))
				}
			}
		}

		if (cur.canDoAction(ZActionType.CONSUME)) {
			cur.allEquipment.forEach { equip ->
				if (equip.isConsumable) {
					getListFor(cur.type, equip.slot!!).add(ZMove.newConsumeMove(equip, equip.slot))
				}
			}
		}
		
		val allMoves : List<ZMove> = moves.flatMap { entry -> 
			entry.value.map { 
				ZMove.newOrganizeSlot(entry.key, it.key, it.value)
			}.toList()
		}.toMutableList().also {
			it.addAll(extraMoves)
		}

		getCurrentUser().chooseOrganize(cur.type, allMoves)?.let { move ->
			return performMove(cur, move)
		}

		return false
	}

	@RemoteFunction
	protected open fun onCloseSpawnArea(c: ZCharacter, zone: Int, area: ZSpawnArea) {}

	private fun performCloseSpawnPortal(cur: ZCharacter, move: ZMove) : Boolean {
		board.getZone(cur.occupiedZone).cells.forEach { pos ->
			board.getCell(pos).spawnAreas.forEach {
				if (it.isCanBeRemovedFromBoard) {
					board.removeSpawn(it)
					onCloseSpawnArea(cur, cur.occupiedZone, it)
					cur.performAction(ZActionType.CLOSE_PORTAL, this)
					return true
				}
			}
		}
		return false
	}
/*
	private fun performAssignEquipment(cur: ZCharacter, equip: ZEquipment<*>, action : ZActionType) : Boolean {
		val movesToExecute = mutableListOf<ZMove>()
		cur.getEquippableSlots2(equip).let { slotList ->
			val moves = slotList.map { ZMove.newEquipMove(equip, null, it, action)}
			getCurrentUser().chooseMove(cur.type, moves)?.let { moveIdx ->
				val move = moves[moveIdx]
				movesToExecute.add(move)
				cur.getSlot(move.toSlot!!)?.let { oldEquip ->
					// move current to backpack if possible
					if (cur.isBackpackFull) {
						val disposeMoves : List<ZMove> = cur.getBackpack().map { ZMove.newDisposeMove(it, ZEquipSlot.BACKPACK) }
						.toMutableList() .also {
							it.add(ZMove.newDisposeEquipmentMove(oldEquip))
						}
						getCurrentUser().chooseMove(cur.type, disposeMoves)?.let {
							movesToExecute.add(disposeMoves[it])
						}?:return false
					} else {
						cur.attachEquipment(oldEquip, ZEquipSlot.BACKPACK)
					}
				}
			}
		}
	}*/

	private suspend fun performBloodlust(cur: ZCharacter, move: ZMove): Boolean {
		val weapons: List<ZWeapon>
		val action = when (move.type) {
			ZMoveType.BLOODLUST_MAGIC -> {
				weapons = cur.magicWeapons
				ZActionType.MAGIC
			}
			ZMoveType.BLOODLUST_MELEE -> {
				weapons = cur.meleeWeapons
				ZActionType.MELEE
			}
            ZMoveType.BLOODLUST_RANGED -> {
                weapons = cur.rangedWeapons
                ZActionType.RANGED
            }
            else -> return false
        }
        when (move.list!!.size) {
            1 -> move.list[0] as Int
            else -> getCurrentUser().chooseZoneForBloodlust(cur.type, move.list as List<Int>)
        }?.let { zone ->
            var weapon: ZWeapon? = null
            if (move.equipment != null) {
                weapon = move.equipment as ZWeapon
            } else if (weapons.size > 1) {
                weapon = getCurrentUser().chooseWeaponSlotInternal(cur.type, weapons)
            } else if (weapons.size == 1) {
                weapon = weapons[0]
            }
            if (weapon != null) {
                cur.addExtraAction()
                moveActor(cur, zone, cur.moveSpeed / 2, ZActionType.MOVE)
	            val stat = cur.getWeaponStat(weapon, action, this, zone)!!
	            performAttack(cur, weapon, stat, zone)
                cur.performAction(action, this)
                cur.removeAvailableSkill(move.skill!!)
	            return true
            }
        }
	    return false
    }

	@RemoteFunction
	protected open fun onAhhhhhh(c: ZPlayerName) {
		log.debug("AHHHHHHH!")
	}

	@RemoteFunction
	protected open fun onNecromancerEscaped(position: ZActorPosition) {
		log.debug("necromancer %s escaped", board.getActor(position))
	}

	@RemoteFunction
	protected open fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
		log.debug("%s found %d eqipments", c, equipment.size)
	}

	@RemoteFunction
	open fun onCharacterHealed(c: ZPlayerName, amt: Int) {
		log.debug("%s healed %d pts", c, amt)
	}

	@RemoteFunction
	protected open fun onSkillKill(c: ZPlayerName, skill: ZSkill, z: ZActorPosition, attackType: ZAttackType) {
		log.debug("%s skill kill on %s with %s", c, board.getActor(z), attackType)
	}

	@RemoteFunction
	open fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
		log.debug("%s roll six applied to %s", c, skill)
	}

	@RemoteFunction
	protected open fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
		log.debug("%s reloaded %s", c, w)
	}

	private suspend fun performRangedOrMagicAttack(cur: ZCharacter, weapons: List<ZWeapon>, _zoneIdx: Int?, actionType: ZActionType): Boolean {
		// rules same for both kinda
		var zoneIdx: Int? = _zoneIdx
		val user = getCurrentUser()
		when (weapons.size) {
			1 -> weapons[0]
			else -> user.chooseWeaponSlotInternal(cur.type, weapons)
		}?.let { weapon ->
			if (zoneIdx == null) {
				cur.getWeaponStat(weapon, actionType, this, -1)?.let { stat ->
					board.getAccessibleZones(cur, stat.minRange, stat.maxRange, actionType)
		                .takeIf { it.isNotEmpty() }?.let { zones ->
			                zoneIdx = user.chooseZoneForAttack(cur.type, zones)
		                }
                }
            }
            zoneIdx?.let {
                cur.getWeaponStat(weapon, actionType, this, it)?.let { stat ->
	                if (performAttack(cur, weapon, stat, it)) {
		                cur.performAction(actionType, this)
		                return true
	                }
                }
            }
		}
		return false
	}

	private suspend fun performRangedAttack(
		cur: ZCharacter,
		weapon: ZWeapon,
		stat: ZWeaponStat,
		zoneIdx: Int
	): Boolean {
		// process a ranged attack
		if (!weapon.isLoaded) {
			addLogMessage("CLICK! Weapon not loaded!")
			onWeaponGoesClick(cur.type, weapon)
		} else {
			weapon.fireWeapon(this, cur, (stat))
			val zone = board.getZone(zoneIdx)
			if (weapon.type.isFire && zone.isDragonBile) {
				performDragonFire(cur, zone.zoneIndex)
			} else {
				val zombies = board.getZombiesInZone(zoneIdx).toMutableList()
				if (zombies.size > 1) {
					if (cur.useMarksmanForSorting(zoneIdx)) {
						Collections.sort(zombies, MarksmanComparator(stat.damagePerHit))
					} else {
						Collections.sort(zombies, RangedComparator())
					}
					//log.debug("Ranged Priority: %s", zombies.map { z: ZZombie -> z.type })
				}
				val hits = resolveHits(
					cur,
					zombies.size,
					weapon.type,
					stat,
					zombies.size / 2 - 1,
					zombies.size / 2 + 1,
					zoneIdx
				)
				var hitsMade = 0
				val zombiesDestroyed: MutableList<ZZombie> = ArrayList()
				val actorsHit: MutableList<ZActorPosition> = ArrayList()
				var i = 0
				while (i < hits && zombies.isNotEmpty()) {
					val zombie: ZZombie = zombies.removeAt(0)
					val pos = zombie.position
					actorsHit.add(pos)
					if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
						zombiesDestroyed.add(zombie)
						hitsMade++
						pos.setData(ACTOR_POS_DATA_DAMAGED)
					} else {
						pos.setData(ACTOR_POS_DATA_DEFENDED)
					}
					i++
				}
				// pre-process friendly fire actors here
				val friendsHit: MutableList<ZCharacter> = ArrayList()
				if (cur.canFriendlyFire()) {
					val misses = stat.numDice - hitsMade
					val friendlyFireOptions = board.getCharactersInZone(zoneIdx).filter { ch: ZCharacter ->
						ch !== cur && ch.canReceiveFriendlyFire()
					}.toMutableList()
					friendlyFireOptions.forEach {
						it.saveWounds()
					}

					i = 0
					while (i < misses && friendlyFireOptions.isNotEmpty()) {
						if (friendlyFireOptions.size > 1) {
							// sort them in same way we would sort zombie attacks
							Collections.sort(friendlyFireOptions, WoundingComparator(ZZombieType.Walker))
						}
						// friendy fire!
						val victim = friendlyFireOptions[0]
						if (playerDefends(victim, ZZombieType.Walker)) {
							addLogMessage(victim.name() + " defended thyself from friendly fire!")
							//onCharacterDefends(victim.type, cur.getPosition());
							actorsHit.add(victim.position.setData(ACTOR_POS_DATA_DEFENDED))
						} else {
							friendsHit.add(victim)
							actorsHit.add(victim.position.setData(ACTOR_POS_DATA_DAMAGED))
							victim.wound(stat.damagePerHit, ZAttackType.FRIENDLY_FIRE)
							if (victim.isDead) {
								// killed em
								friendlyFireOptions.removeAt(0)
								victim.restoreWounds()
							}
						}
						i++
					}
					friendlyFireOptions.forEach {
						it.restoreWounds()
					}
				}
				onAttack(
					cur.type,
					weapon.type,
					stat.actionType,
					stat.numDice,
					actorsHit,
					zoneIdx
				)
				var exp = 0
				for (zombie: ZZombie in zombiesDestroyed) {
					destroyZombie(zombie, stat.attackType, cur, weapon.type)
					exp += zombie.type.expProvided
				}
				cur.getAvailableSkills().appendedWith(weapon.type.skillsWhenUsed).forEach { skill ->
					skill.onAttack(this, cur, weapon, stat.actionType, (stat), zoneIdx, hits, zombiesDestroyed)
				}
				for (victim: ZCharacter in friendsHit) {
					playerWounded(victim, cur, stat.attackType, stat.damagePerHit, "Friendly Fire!")
				}
				addExperience(cur, exp)
				addLogMessage(cur.name() + " Scored " + hitsMade + " hits for +" + exp + " XP")
				if (hitsMade > 0)
					checkForHitAndRun(cur)
				return true
			}
		}
		return false
	}

	private suspend fun performMeleeAttack(
		cur: ZSurvivor,
		weapon: ZWeapon,
		stat: ZWeaponStat,
		zoneIdx: Int
	): Boolean {
		val zombies = board.getZombiesInZone(cur.occupiedZone).toMutableList()
		val numZombiesInZone = zombies.size
		Collections.sort(zombies, MarksmanComparator(stat.damagePerHit))
		val totalHits = resolveHits(
			cur,
			zombies.size,
			weapon.type,
			stat,
			zombies.size / 2 - 1,
			zombies.size / 2 + 1,
			zoneIdx
		)
		// when we attack with melee there will be some misses and some hits that are defended
		var hits = totalHits
		val zombiesHit: MutableList<ZActorPosition> = ArrayList()
		val zombiesDestroyed: MutableList<ZZombie> = ArrayList()
		val it = zombies.iterator()
		while (it.hasNext()) {
			if (hits <= 0) break
			val z = it.next()
			val pos = z.position
			zombiesHit.add(pos)
			if (z.type.minDamageToDestroy <= stat.damagePerHit) {
				zombiesDestroyed.add(z)
				it.remove()
				hits--
				pos.setData(ACTOR_POS_DATA_DAMAGED)
			} else {
				pos.setData(ACTOR_POS_DATA_DEFENDED)
			}
		}
		// if all the zombies in the zone are killed then dont need to show the misses
		val numDice = if (zombiesHit.isNotEmpty() && zombiesHit.size == numZombiesInZone) numZombiesInZone else stat.numDice
		onAttack(
			cur.playerType,
			weapon.type,
			stat.actionType,
			numDice,
			zombiesHit,
			cur.occupiedZone
		)
		var exp = 0
		for (z: ZZombie in zombiesDestroyed) {
			exp += z.type.expProvided
			destroyZombie(z, stat.attackType, cur, weapon.type)
		}
		if (weapon.type.attackIsNoisy) {
			addNoise(cur.occupiedZone, 1)
		}
		if (cur is ZCharacter && exp > 0)
			addExperience(cur, exp)

		addLogMessage(cur.name() + " Scored " + totalHits + " hits for +" + exp + " XP")
		cur.getAvailableSkills().appendedWith(weapon.type.skillsWhenUsed).forEach { skill ->
			skill.onAttack(
				this,
				cur,
				weapon,
				stat.actionType,
				(stat),
				cur.occupiedZone,
				totalHits,
				zombiesDestroyed
			)
		}
		if (totalHits > 0)
			checkForHitAndRun(cur)
		return true
	}

	private suspend fun performAttack(
		cur: ZCharacter,
		weapon: ZWeapon,
		stat: ZWeaponStat,
		zoneIdx: Int
	): Boolean {
		when (stat.actionType) {
			ZActionType.MELEE -> {
				repeatableMove = ZMove.newMeleeAttackMove(listOf(weapon))
				return performMeleeAttack(cur, weapon, stat, zoneIdx)
			}

			ZActionType.MAGIC -> {
				repeatableMove = ZMove.newMagicAttackMove(listOf(weapon), zoneIdx)
				return performRangedAttack(cur, weapon, stat, zoneIdx)
			}

			ZActionType.RANGED -> {
				repeatableMove = ZMove.newRangedAttackMove(listOf(weapon), zoneIdx)
				return performRangedAttack(cur, weapon, stat, zoneIdx)
			}

			ZActionType.CATAPULT_FIRE -> {
				// TODO:
				return performRangedAttack(cur, weapon, stat, zoneIdx)
			}

			else -> Unit
		}
		return false
	}

	fun performSkillKill(c: ZSurvivor, skill: ZSkill, z: ZZombie, at: ZAttackType) {
		onSkillKill(c.playerType, skill, z.position, at)
		addExperience(c, z.type.expProvided)
		destroyZombie(z, at, c, null)
	}

	internal class WoundingComparator(val zType: ZZombieType) : Comparator<ZCharacter> {
		override fun compare(o1: ZCharacter, o2: ZCharacter): Int {
			val v0 = o1.getArmorRating(zType) - o1.woundBar
			val v1 = o2.getArmorRating(zType) - o2.woundBar
			return v1.compareTo(v0)
		}
    }

    internal class RangedComparator() : Comparator<ZZombie> {
        override fun compare(o1: ZZombie, o2: ZZombie): Int {
	        return o1.type.targetingPriority.compareTo(o2.type.targetingPriority)
        }
    }

    // Marksman have reverse of Ranged expect Zombies with minDamage above are prioritized last
    internal class MarksmanComparator(val attackDamage: Int) : Comparator<ZZombie> {
        override fun compare(o1: ZZombie, o2: ZZombie): Int {
	        val o1Value =
		        if (o1.type.minDamageToDestroy > attackDamage) 0 else o1.type.targetingPriority
	        val o2Value =
		        if (o2.type.minDamageToDestroy > attackDamage) 0 else o2.type.targetingPriority
	        return o2Value.compareTo(o1Value) // descending order
        }
    }

	private suspend fun resolveHits(
		cur: ZSurvivor,
		maxHits: Int,
		type: ZWeaponType,
		stat: ZWeaponStat,
		minHitsForAutoReroll: Int,
		maxHitsForAutoNoReroll: Int,
		targetZone: Int
	): Int {
		var result: Array<Int> = if (cur.canReroll(this, stat.attackType)) {
			rollDiceWithRerollOption(
				stat.numDice,
				stat.dieRollToHit,
				minHitsForAutoReroll,
				maxHitsForAutoNoReroll
			)
		} else {
			rollDice(stat.numDice)
		}
		var hits = 0
		//boolean isRoll6Plus1Die = cur.isRoll6Plus1Die(type, stat.actionType);
		var keepGoing: Boolean
		do {
            keepGoing = false
            for (i in result.indices) {
                if (result[i] >= stat.dieRollToHit) {
                    hits++
                }
            }
            val numSixes = result.count { it == 6 }
            if (numSixes > 0) {
                cur.getAvailableSkills().appendedWith(type.skillsWhenUsed).forEach { skill ->
	                if (skill.onSixRolled(this, cur, (stat), targetZone) && hits < maxHits) {
		                result = rollDice(numSixes)
		                keepGoing = true
	                }
                }
            } else {
	            break
            }
			if (keepGoing) {
				addLogMessage("Rolled $numSixes 6s + 1Die roll each!")
			}
		} while (keepGoing)
		return hits
	}

	@RemoteFunction
	open fun onZoneFrozen(freezer: ZPlayerName, zoneIdx: Int) {
		log.debug("$freezer froze zone $zoneIdx")
	}

	@RemoteFunction
	protected open fun onWeaponGoesClick(c: ZPlayerName, weapon: ZWeapon) {
		log.debug("%s fired unloaded weapon %s", c, weapon)
	}

	@RemoteFunction
	protected open fun onDoorToggled(cur: ZPlayerName, door: ZDoor) {
		log.debug("%s opened door %s", cur, door)
	}

	@RemoteFunction
	protected open fun onCharacterOpenDoorFailed(cur: ZPlayerName, door: ZDoor) {
		log.debug("%s failed to open door %s", cur, door)
	}

	/**
	 * Prior to processing damages and kills this iu
	 * @param attacker
	 * @param weapon
	 * @param actionType
	 * @param numDice
	 * @param actorsHit
	 * @param targetZone
	 */
	@RemoteFunction
	protected open fun onAttack(
		attacker: ZPlayerName,
		weapon: ZWeaponType,
		actionType: ZActionType?,
		numDice: Int,
		actorsHit: List<ZActorPosition>,
		targetZone: Int
	) {
		log.debug(
			"%s made %d hits in zone %d with %s action %s numDice %d",
			attacker,
			actorsHit.size,
			targetZone,
			weapon,
			actionType,
			numDice
		)
	}

	@RemoteFunction
	protected open fun onBonusAction(pl: ZPlayerName, action: ZSkill) {
		log.debug("%s got bonus action %s", pl, action)
	}

	private fun checkForHitAndRun(cur: ZSurvivor) {
		if (cur.hasAvailableSkill(ZSkill.Hit_and_run)) {
			cur.addAvailableSkill(ZSkill.Plus1_free_Move_Action)
			addLogMessage(cur.getLabel() + " used Hit and Run for a free move action")
			onBonusAction(cur.playerType, ZSkill.Plus1_free_Move_Action)
		}
	}

	val highestSkillLevel: ZSkillLevel
		get() = users.maxOfOrNull { user ->
			user.players.maxOfOrNull {
				it.toCharacter().skillLevel
			} ?: ZSkillLevel.getLevel(0, rules)
		} ?: ZSkillLevel.getLevel(0, rules)

	fun addExperience(c: ZSurvivor, pts: Int) {
		if (pts <= 0) return
		var sl = c.skillLevel
		c.addExperience(this, pts)
		onCharacterGainedExperience(c.playerType, pts)
		// make so a user can level up multiple times in a single level up
		// need to push state in reverse order so that the lowest new level choices are first
		val states: MutableList<State> = ArrayList()
		while (sl != c.skillLevel) {
			sl = sl.nextLevel(rules)
			addLogMessage(c.name() + " has gained the " + sl.toString() + " skill level")
			states.add(State(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL, c.playerType, null, sl))
		}
		states.reverse()
		for (s: State in states) pushState(s)
    }

	@RemoteFunction
	protected open fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
		log.info("%s gained %d experence!", c, points)
	}

	private suspend fun rollDiceWithRerollOption(numDice: Int, dieNumToHit: Int, _minHitsForAutoReroll: Int, _maxHitsForAutoNoReroll: Int): Array<Int> {
		var minHitsForAutoReroll = _minHitsForAutoReroll
		var maxHitsForAutoNoReroll = _maxHitsForAutoNoReroll
		minHitsForAutoReroll = minHitsForAutoReroll.coerceAtLeast(0)
		maxHitsForAutoNoReroll = maxHitsForAutoNoReroll.coerceAtMost(numDice)
		val dice = rollDice(numDice)
		var hits = 0
		for (d: Int in dice) {
			if (d >= dieNumToHit) hits++
		}
		addLogMessage(requireCurrentCharacter.getLabel() + " Scored " + hits + " hits")
        //onRollDice(dice);
        if (hits >= maxHitsForAutoNoReroll) {
            return dice
        }
        if (hits > minHitsForAutoReroll) {
	        getCurrentUser().chooseMove(requireCurrentCharacter.type,
		        listOf(ZMove.newReRollMove(), ZMove.newKeepRollMove()))?.let {
		        if (it.type == ZMoveType.KEEP_ROLL)
			        return dice
	        }
        }
        addLogMessage("Bonus roll dice!")
        return rollDice(numDice)
    }

	fun rollDice(num: Int): Array<Int> {
		val result = Array(num) { dice[it] }
		var dieStrEnd = "+"
		var dieStrMid = "|"
		for (i in 0 until num) {
			dieStrMid += String.format(" %d |", result[i])
			dieStrEnd += "---+"
		}
		for (i in 0 until dice.size - num) {
			dice[i] = dice[i + num]
		}
        for (i in 0 until num) {
            dice[i + dice.size - num] = (result[i])
        }
        log.info("Rolled a $dieStrEnd")
		log.info("Rolled a $dieStrMid")
		log.info("Rolled a $dieStrEnd")
		onRollDice(result)
		return result
	}

	@RemoteFunction
	protected open fun onRollDice(roll: Array<Int>) {
		log.info("Rolling dice result is: %s", Arrays.toString(roll))
	}

	fun destroyZombie(
		zombie: ZZombie,
		deathType: ZAttackType,
		killer: ZSurvivor?,
		type: ZEquipmentType?
	) {
		killer?.onKilledZombie(this, zombie, type)
		onZombieDestroyed(deathType, zombie.position)
		zombie.destroyed = true //board.removeActor(zombie);
		if (zombie.type.isNecromancer) {
			pushState(State(ZState.PLAYER_STAGE_CHOOSE_SPAWN_AREA_TO_REMOVE, killer!!.playerType))
		}
	}

	@RemoteFunction
	protected open fun onZombieDestroyed(deathType: ZAttackType, zombiePos: ZActorPosition) {}

	val allCharacters: List<ZCharacter>
		get() = board.getAllCharacters()
	val allLivingCharacters: List<ZCharacter>
		get() = allCharacters.filter { it.isAlive }
	val currentUserCharacters: List<ZCharacter>
		get() = getCurrentUser().players.map { it.toCharacter() }.filter { it.isAlive }

	private fun isClearedOfZombies(zoneIndex: Int): Boolean {
		return board.getNumZombiesInZone(zoneIndex) == 0
	}

	@RemoteFunction
	protected open fun onDoubleSpawn(multiplier: Int) {
		log.debug("Double spawn X %d", multiplier)
	}

	@RemoteFunction
	protected open fun onNothingInSight(zone: Int) {
		addLogMessage("Nothing in Sight")
	}

	private fun doubleSpawn() {
		spawnMultiplier *= 2
		addLogMessage("DOUBLE SPAWN!")
		onDoubleSpawn(spawnMultiplier)
	}

	private fun extraActivation(category: ZZombieCategory) {
		onExtraActivation(category)
		addLogMessage("EXTRA ACTIVATION!")
		for (z: ZZombie in board.getAllZombies()) {
			if (category === ZZombieCategory.ALL || z.type.category === category)
				z.addExtraAction()
		}
		allLivingCharacters.filter { it.hasSkill(ZSkill.Zombie_link) }.forEach {
			it.addAvailableSkill(ZSkill.Plus1_Action)
		}
	}

	@RemoteFunction
	protected open fun onExtraActivation(category: ZZombieCategory) {
		log.debug("Extra Activation %s", category)
	}

	fun spawnZombies(zoneIdx: Int) {
		spawnZombies(zoneIdx, highestSkillLevel)
	}

    private fun spawnZombies(zoneIdx: Int, level: ZSkillLevel) {
	    if (quest.handleSpawnForZone(this, zoneIdx))
		    return
	    //ZSpawnArea spawnType = quest.getSpawnType(this, board.getZone(zoneIdx));
	    if (deck.isEmpty()) {
		    extraActivation(ZZombieCategory.ALL)
		    return
	    }
	    val card = deck.removeLast()
	    log.debug("Draw spawn card: $card")
	    val action = card.getAction(level.difficultyColor)
	    if (action.action == ActionType.SPAWN && action.type?.isNecromancer == true && !board.canZoneSpawnNecromancers(
			    zoneIdx
		    )
	    ) {
		    spawnZombies(zoneIdx, level)
		    deck.add(card)
		    return
	    }
	    onSpawnCard(card, level.difficultyColor)
	    when (action.action) {
		    ActionType.NOTHING_IN_SIGHT -> onNothingInSight(zoneIdx)
		    ActionType.SPAWN -> spawnZombies(
			    action.count,
			    action.type ?: ZZombieType.Walker,
			    zoneIdx
		    )

		    ActionType.DOUBLE_SPAWN -> doubleSpawn()
		    ActionType.EXTRA_ACTIVATION_STANDARD -> extraActivation(ZZombieCategory.STANDARD)
		    ActionType.EXTRA_ACTIVATION_NECROMANCER -> extraActivation(ZZombieCategory.NECROMANCER)
		    ActionType.EXTRA_ACTIVATION_WOLFSBURG -> extraActivation(ZZombieCategory.WOLFSBURG)
		    ActionType.ASSEMBLE_HOARD -> {
			    spawnZombies(action.count, action.type!!, zoneIdx)
			    board.addToHoard(action.type)
		    }

		    ActionType.ENTER_THE_HOARD -> board.spawnHoardZombies(zoneIdx, this)
	    }
    }

	@RemoteFunction
	protected open fun onSpawnCard(card: ZSpawnCard, color: ZColor) {
	}

	fun getCurrentUser(): ZUser = users[currentUserIdx.coerceIn(0 until users.size)]

	fun getUserForCharacter(character: ZCharacter): ZUser? = users.firstOrNull { it.colorId == character.colorId }

	open val currentCharacter: ZCharacter?
		get() = if (stateStack.isEmpty()) null
		else stateStack.peek().player?.toCharacterOrNull()

	val requireCurrentCharacter: ZCharacter
		get() = requireNotNull(currentCharacter)

	protected fun moveActor(actor: ZActor, toZone: Int, speed: Long, actionType: ZActionType?) {
		val fromZone = actor.occupiedZone
		val fromPos = actor.occupiedCell
		val fromRect = actor.getRect(board)
		board.moveActor(actor, toZone)
		doMove(actor, fromZone, fromPos, fromRect, speed, actionType)
	}

	protected open fun moveActorInDirection(actor: ZActor, dir: ZDir, action: ZActionType?) {
		val fromZone = actor.occupiedZone
		val fromPos = actor.occupiedCell
		val fromRect = actor.getRect(board)
		val next = board.getAdjacent(fromPos, dir)
		board.moveActor(actor, next)
		doMove(actor, fromZone, fromPos, fromRect, actor.moveSpeed, action)
	}

	protected open fun moveActorInDirectionIfPossible(actor: ZActor, dir: ZDir, action: ZActionType?): Boolean {
		val fromZone = actor.occupiedZone
		val fromPos = actor.occupiedCell
		val fromRect = actor.getRect(board)
		val next = board.getAdjacent(fromPos, dir)
		if (next.row < 0 || next.column < 0)
			return false
		if (board.getCell(next).isFull)
			return false
		board.moveActor(actor, next)
		doMove(actor, fromZone, fromPos, fromRect, actor.moveSpeed, action)
		return true
	}

	fun moveActorInDirectionDebug(actor: ZActor, dir: ZDir) {
		val fromPos = actor.occupiedCell
		//GRectangle fromRect = actor.getRect(board);
		val next = board.getAdjacent(fromPos, dir)
		board.moveActor(actor, next)
		//doMove(actor, fromZone, fromPos, fromRect, actor.getMoveSpeed());
	}

	private fun doMove(
		actor: ZActor,
		fromZone: Int,
		fromPos: Pos,
		fromRect: GRectangle,
		speed: Long,
		actionType: ZActionType?
	) {
		val toZone = actor.occupiedZone
		val toPos = actor.occupiedCell
		val toRect = actor.getRect(board)
		if (board.getZone(fromZone).type === ZZoneType.VAULT && board.getZone(toZone).type !== ZZoneType.VAULT) {
			// ascending the stairs
			val fromVaultRect = GRectangle(board.getCell(fromPos)).scale(.2f)
			val toVaultRect = GRectangle(board.getCell(toPos)).scale(.5f)
			onActorMoved(actor, fromRect, fromVaultRect, speed / 2)
			onActorMoved(actor, toVaultRect, toRect, speed / 2)
			if (actionType != null) {
				actor.performAction(ZActionType.MOVE, this)
			}
		} else if (board.getZone(fromZone).type !== ZZoneType.VAULT && board.getZone(toZone).type === ZZoneType.VAULT) {
			// descending the stairs
			val fromVaultRect = GRectangle(board.getCell(fromPos)).scale(.2f)
			val toVaultRect = GRectangle(board.getCell(toPos)).scale(.5f)
			onActorMoved(actor, fromRect, fromVaultRect, speed / 2)
			onActorMoved(actor, toVaultRect, toRect, speed / 2)
			if (actionType != null) {
				actor.performAction(ZActionType.MOVE, this)
			}
		} else if (fromZone != toZone) {
			when (board.getCell(fromPos).getWallFlag(board.getDirection(fromZone, toZone))) {
				ZWallFlag.HEDGE -> {
					onActorMoved(actor, fromRect, toRect, speed * 2)
					board.getCell(toPos).takeIf { !it.discovered }?.let { cell ->
						cell.discovered = true
						if (rollDice(1)[0] == 1)
							spawnZombies(1, ZZombieType.OrcWalker, toZone)
					}
				}

				ZWallFlag.LEDGE -> {
					onActorMoved(actor, fromRect, toRect, speed * 2)
					actor.performAction(
						if (board.getZone(fromZone).type == ZZoneType.WATER)
							ZActionType.CLIMB
						else
							ZActionType.MOVE, this
					)
				}

				else -> {
					onActorMoved(actor, fromRect, toRect, speed)
					actionType?.let {
						actor.performAction(it, this)
					}
				}
			}
		} else {
			onActorMoved(actor, fromRect, toRect, speed)
		}
	}

	@RemoteFunction
	protected open fun onBeginRound(roundNum: Int) {
		log.debug("Begin round %d", roundNum)
	}

	private fun onActorMoved(actor: ZActor, start: GRectangle, end: GRectangle, speed: Long) {
		onActorMoved(actor.getId(), start, end, speed)
	}

	@RemoteFunction
	protected open fun onActorMoved(id: String, start: GRectangle, end: GRectangle, speed: Long) {
		log.debug(
			"actor %s moved from %s to %s with speed %d",
			board.getActor(id),
			start,
			end,
			speed
		)
	}

	fun initLootDeck() {

		fun make(count: Int, e: ZEquipmentType): List<ZEquipment<*>> {
			val list: MutableList<ZEquipment<*>> = ArrayList()
			for (i in 0 until count) {
				list.add(e.create())
			}
			return list
		}

		lootDeck.clear()
		lootDeck.addAll(make(4, ZItemType.BARRICADE))
		lootDeck.addAll(make(4, ZItemType.AHHHH))
		lootDeck.addAll(make(2, ZItemType.APPLES))
		lootDeck.addAll(make(2, ZWeaponType.AXE))
		lootDeck.addAll(make(2, ZArmorType.CHAIN_MAIL))
		lootDeck.addAll(make(2, ZWeaponType.CROSSBOW))
		lootDeck.addAll(make(2, ZWeaponType.DAGGER))
		lootDeck.addAll(make(2, ZWeaponType.DEATH_STRIKE))
        lootDeck.addAll(make(3, ZItemType.DRAGON_BILE))
        lootDeck.addAll(make(2, ZWeaponType.FIREBALL))
        lootDeck.addAll(make(2, ZWeaponType.GREAT_SWORD))
        lootDeck.addAll(make(1, ZWeaponType.HAMMER))
        lootDeck.addAll(make(2, ZWeaponType.HAND_CROSSBOW))
        //        searchables.addAll(make(1, ZWeaponType.INFERNO));
        lootDeck.addAll(make(2, ZArmorType.LEATHER))
        lootDeck.addAll(make(2, ZWeaponType.LIGHTNING_BOLT))
        lootDeck.addAll(make(2, ZWeaponType.LONG_BOW))
        lootDeck.addAll(make(1, ZWeaponType.MANA_BLAST))
        //        searchables.addAll(make(1, ZWeaponType.ORCISH_CROSSBOW));
        lootDeck.addAll(make(1, ZArmorType.PLATE))
        lootDeck.addAll(make(3, ZItemType.PLENTY_OF_ARROWS))
        lootDeck.addAll(make(3, ZItemType.PLENTY_OF_BOLTS))
        lootDeck.addAll(make(2, ZWeaponType.REPEATING_CROSSBOW))
        //searchables.addAll(make(1, ZSpellType.REPULSE));
        lootDeck.addAll(make(2, ZItemType.SALTED_MEAT))
		lootDeck.addAll(make(2, ZArmorType.SHIELD))
		lootDeck.addAll(make(1, ZWeaponType.SHORT_BOW))
		lootDeck.addAll(make(2, ZWeaponType.SHORT_SWORD))
		lootDeck.addAll(make(2, ZWeaponType.SWORD))
		lootDeck.addAll(make(4, ZItemType.TORCH))
		lootDeck.addAll(make(2, ZItemType.WATER))
		lootDeck.addAll(make(1, ZSpellType.HEALING))
		lootDeck.addAll(make(1, ZSpellType.INVISIBILITY))
		lootDeck.addAll(make(1, ZSpellType.SPEED))
		lootDeck.addAll(make(1, ZSpellType.HELL_GOAT))
		if (rules.toxicOrcs) {
			lootDeck.addAll(make(4, ZItemType.PLAGUE_MASK))
		}
		quest.processLootDeck(lootDeck)
		lootDeck.shuffle()
	}

	fun addNoise(zoneIdx: Int, noise: Int) {
		onNoiseAdded(zoneIdx)
		board.getZone(zoneIdx).addNoise(noise)
		//        showMessage("Noise was made in zone " + zoneIdx);
	}

	@RemoteFunction
	protected open fun onNoiseAdded(zoneIndex: Int) {
		log.debug("Noise added at %d", zoneIndex)
	}

	@RemoteFunction
	protected open fun onZombieStageBegin() {}

	@RemoteFunction
	protected open fun onZombieStageMoveDone() {}

	@RemoteFunction
	protected open fun onZombieStageEnd() {}

	protected open fun onZombiePath(id: String, path: List<ZDir>) {}

	@RemoteFunction
	protected open fun onSpawnZoneSpawning(rect: GRectangle, nth: Int, num: Int) {
	}

	@RemoteFunction
	protected open fun onSpawnZoneSpawned(nth: Int, num: Int) {
	}

	val gameSummaryTable: Table
		get() {
			val summary =
				Table("PLAYER", "KILLS", "FAV WEAPONS", "STATUS", "EXP", "LEVEL").setNoBorder()
			for (c: ZCharacter in board.getAllCharacters()) {
				summary.addRow(
					c.type,
					c.killsTable,
					c.favoriteWeaponsTable,
					if (c.isDead) c.deathType?.description ?: "KIA" else "Alive",
					c.exp,
					c.skillLevel
				)
			}
			val gameStatus = when (gameOverStatus) {
				GAME_LOST -> quest.getQuestFailedReason(this)
				GAME_WON -> String.format("Completed")
				else -> String.format(
					"In Progress: %d%% Completed",
					quest.getPercentComplete(this).coerceIn(0..100)
				)
			}
			return Table(quest.name)
				.addRow("STATUS: $gameStatus")
				.addRow(Table("SUMMARY").addRow(summary))
		}

    fun putBackInSearchables(e: ZEquipment<*>) {
        lootDeck.addFirst(e)
    }

    private fun canSwitchActivePlayer(): Boolean {
        val cur = currentCharacter ?: return false
	    if (!cur.hasMovedThisTurn)
		    return true
	    return cur.hasAvailableSkill(ZSkill.Tactician)
    }

    val allSearchables: List<ZEquipment<*>>
        get() = Collections.unmodifiableList(lootDeck)

	@RemoteFunction
	open fun onIronRain(c: ZPlayerName, targetZone: Int) {
		log.debug("%s unleashed iron rain at %d", c, targetZone)
	}

	@RemoteFunction
	protected open fun onDoorUnlocked(door: ZDoor) {
		log.debug("%s unlocked", door)
	}

	fun unlockDoor(door: ZDoor) {
		require(board.getDoor(door) === ZWallFlag.LOCKED)
		board.setDoor(door, ZWallFlag.CLOSED)
		onDoorUnlocked(door)
	}

    fun lockDoor(door: ZDoor) {
        require(board.getDoor(door) !== ZWallFlag.LOCKED)
        board.setDoor(door, ZWallFlag.LOCKED)
    }

    fun isDoorLocked(door: ZDoor): Boolean {
        return door.isLocked(board)
    }

	@RemoteFunction(callSuper = true)
	open fun addLogMessage(msg: String) {
		log.info(msg)
	}

	fun performDragonFire(cur: ZSurvivor, zoneIdx: Int) {
		onDragonBileExploded(zoneIdx)
		board.getZone(zoneIdx).isDragonBile = false
		var exp = 0
		var num = 0
		addLogMessage(cur.name() + " ignited the dragon bile!")
		board.getActorsInZone(zoneIdx).filter { a -> a.isAlive }.forEach { a ->
			if (a is ZZombie) {
				exp += a.type.expProvided
				destroyZombie(a, ZAttackType.FIRE, cur, ZItemType.DRAGON_BILE)
				num++
			} else if (a is ZCharacter) {
                // characters caught in the zone get wounded
                playerWounded(a, cur, ZAttackType.FIRE, 4, "Exploding Dragon Bile")
            }
        }
        if (cur.isAlive) {
            addExperience(cur, exp)
            addLogMessage(String.format("%s Destroyed %d zombies for %d total experience pts!", cur.name(), num, exp))
        } else {
            addLogMessage(String.format("%s Destroyed %d zombies and themselves in the process!", cur.name(), num))
        }
        quest.onDragonBileExploded(cur, zoneIdx)
    }

	fun giftEquipment(c: ZCharacter, e: ZEquipment<*>) {
		onEquipmentFound(c.type, listOf(e))
		quest.onEquipmentFound(this, e)
		c.getEmptyEquipSlotsFor(e).firstOrNull()?.also { slot ->
			c.attachEquipment((e), slot)
		} ?: run {
			pushState(State(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, c.type, e))
		}
	}

	private fun tryOpenDoor(cur: ZCharacter, door: ZDoor): Boolean {
		if (!door.isJammed) return true
		if (cur.tryOpenDoor(this)) {
			onDoorToggled(cur.type, door)
			return true
		}
		onCharacterOpenDoorFailed(cur.type, door)
		return false
	}

	@RemoteFunction
	protected open fun onDragonBileExploded(zoneIdx: Int) {
		log.debug("Dragon bil eexploded in %d", zoneIdx)
	}

	fun chooseEquipmentFromSearchables() {
		pushState(State(ZState.PLAYER_STAGE_CHOOSE_WEAPON_FROM_DECK, currentCharacter?.type))
	}

	fun chooseVaultItem() {
		pushState(State(ZState.PLAYER_STAGE_CHOOSE_VAULT_ITEM, currentCharacter?.type))
	}

	fun giftRandomVaultArtifact(c: ZCharacter) {
		quest.vaultItemsRemaining.takeIf { it.isNotEmpty() }?.let {
			val equip = it.removeRandom()
			addLogMessage("${c.getLabel()} has been gifted a ${equip.getLabel()}")
			giftEquipment(c, equip)
		}
	}

	fun setUserName(user: ZUser, name: String?) {
		user.name = name ?: ZUser.USER_COLOR_NAMES[user.colorId]
	}

	fun setUserColorId(user: ZUser, colorId: Int) {
		if (colorId !in ZUser.USER_COLORS.indices)
			throw IllegalArgumentException("color Id out of range")
		user.colorId = colorId
		user.players.forEach {
			board.getCharacterOrNull(it)?.colorId = colorId
		}

	}

	@RemoteFunction
	open fun setBoardMessage(colorId: Int, message: String?) {
	}

}