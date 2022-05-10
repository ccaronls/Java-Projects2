package cc.lib.zombicide

import cc.lib.zombicide.ZDir.Companion.compassValues
import cc.lib.game.GColor
import cc.lib.zombicide.ZSpawnCard.ActionType
import cc.lib.game.GRectangle

import cc.lib.logger.LoggerFactory
import cc.lib.utils.*
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.Pair

open class ZGame() : Reflector<ZGame>() {
    companion object {
        @JvmField
        var DEBUG = false
        private val log = LoggerFactory.getLogger(ZGame::class.java)
        val GAME_LOST = 2
        val GAME_WON = 1
        @JvmStatic
        fun initDice(difficulty: ZDifficulty): IntArray {
            val countPerNum: Int
            val step: Int
            when (difficulty) {
                ZDifficulty.EASY -> {
                    countPerNum = 20
                    step = -1
                }
                ZDifficulty.MEDIUM -> {
                    countPerNum = 30
                    step = -1
                }
                else               -> {
                    countPerNum = 30
                    step = 0
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
            assertTrue(idx == dice.size)
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

    class State internal constructor(val state: ZState, val player: ZPlayerName?, val equipment: ZEquipment<*>?, val skillLevel: ZSkillLevel?) : Reflector<State>() {
        constructor() : this(ZState.INIT, null, null, null) {}
    }

    private val stateStack = Stack<State>()
    @JvmField
    var board: ZBoard = ZBoard()

    @Omit
    private val users: MutableList<ZUser> = ArrayList()
    var questInitialized = false
        private set
    lateinit var quest: ZQuest
        private set
    var currentUser = 0
    private var startUser = 0

    @Alternate(variations = ["searchables"])
    private val lootDeck = LinkedList<ZEquipment<*>>()
    private var spawnMultiplier = 1
    var roundNum = 0
    private var gameOverStatus = 0 // 0 == in play, 1, == game won, 2 == game lost
    private lateinit var currentQuest: ZQuests
    private lateinit var dice: IntArray
    private var difficulty = ZDifficulty.EASY
    @JvmOverloads
    fun pushState(state: ZState, player: ZPlayerName?, e: ZEquipment<*>? = null, skill: ZSkillLevel? = null) {
        pushState(State(state, player, e, skill))
    }

    fun pushState(state: State) {
        val oldPlayer = currentCharacter
        if (state.player != oldPlayer)
            onCurrentCharacterUpdated(oldPlayer, state.player)
        stateStack.push(state)
    }

    fun popState() {
        val curPlayer = currentCharacter
        stateStack.pop()
        currentCharacter?.let {
            if (curPlayer != it)
                onCurrentCharacterUpdated(curPlayer, it)
        }
    }

    protected open fun onCurrentUserUpdated(user: ZUser) {
        log.debug("%s updated", user)
    }

    protected open fun onCurrentCharacterUpdated(priorPlayer: ZPlayerName?, player: ZPlayerName?) {
        log.debug("%s updated too %s", priorPlayer, player)
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
        currentUser = 0
        gameOverStatus = 0
        spawnMultiplier = 1
        dice = initDice(difficulty)
        setState(ZState.INIT, null)
    }

    fun clearCharacters() {
        board.removeCharacters()
    }

    fun clearUsersCharacters() {
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

    fun reload() {
        loadQuest(currentQuest)
    }

    fun getNumKills(vararg types: ZZombieType): Int {
        var num = 0
        for (type: ZZombieType in types) {
            for (c: ZCharacter in board.getAllCharacters()) {
                num += c.getKills(type)
            }
        }
        return num
    }

    fun addUser(user: ZUser) {
        if (!users.contains(user)) users.add(user)
    }

    fun removeUser(user: ZUser) {
        users.remove(user)
        if (currentUser >= users.size) currentUser = 0
        if (startUser >= users.size) startUser = 0
    }

    protected open fun initQuest(quest: ZQuest) {}

    fun addCharacter(pl: ZPlayerName): ZCharacter {
        val cell = board.getCellsOfType(ZCellType.START).random()
        val c = pl.create()
        c.occupiedZone = cell.zoneIndex
        if (!board.spawnActor(c))
            throw GException("Failed to add $pl to board")
        return c
    }

    fun removeCharacter(nm: ZPlayerName) {
        board.removeActor(nm.character)
    }

    fun loadQuest(newQuest: ZQuests) {
        log.debug("Loading quest: $newQuest")
        val prevQuest : ZQuest? = if (questInitialized) this.quest else null
        quest = newQuest.load()
        synchronized(board) {
            board = quest.loadBoard()
            if (prevQuest == null || prevQuest.name != this.quest.name)
                initQuest(quest)
            initGame()
            val startCells: MutableList<ZCell> = ArrayList()
            val it: Grid.Iterator<ZCell> = board.getCellsIterator()
            while (it.hasNext()) {
                val cell: ZCell = it.next()
                if (cell.isCellTypeEmpty) {
                    continue
                }
                val zone: ZZone = board.zones[cell.zoneIndex]
                when (cell.environment) {
                    ZCell.ENV_OUTDOORS -> zone.type = ZZoneType.OUTDOORS
                    ZCell.ENV_BUILDING -> zone.type = ZZoneType.BUILDING
                    ZCell.ENV_VAULT -> zone.type = ZZoneType.VAULT
                    ZCell.ENV_TOWER -> zone.type = ZZoneType.TOWER
                }
                // add doors for the zone
                for (dir: ZDir in compassValues) {
                    when (cell.getWallFlag(dir)) {
                        ZWallFlag.CLOSED, ZWallFlag.OPEN -> {
                            val pos: Grid.Pos = it.pos
                            val next: Grid.Pos = board.getAdjacent(pos, (dir))
                            zone.doors.add(ZDoor(pos, next, dir, GColor.RED))
                        }
                    }
                }
                for (type: ZCellType in ZCellType.values()) {
                    if (cell.isCellType(type)) {
                        when (type) {
                            ZCellType.START -> startCells.add(cell)
                            ZCellType.OBJECTIVE_BLACK, ZCellType.OBJECTIVE_RED, ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN -> zone.isObjective = true
                            ZCellType.VAULT_DOOR_VIOLET -> addVaultDoor(cell, zone, it.pos, GColor.MAGENTA)
                            ZCellType.VAULT_DOOR_GOLD -> addVaultDoor(cell, zone, it.pos, GColor.GOLD)
                        }
                    }
                }
            }
            if (startCells.size == 0) {
                throw IllegalArgumentException("No start cells specified")
            }

            // position all the characters here
            board.removeCharacters()
            var curCellIndex = 0
            for (u: ZUser in users) {
                for (pl: ZPlayerName in u.players) {
                    val c: ZCharacter = pl.create()
                    c.color = u.getColor()
                    val cell: ZCell = startCells[curCellIndex]
                    curCellIndex = curCellIndex.rotate(startCells.size)
                    c.occupiedZone = cell.zoneIndex
                    if (!board.spawnActor(c))
                        throw GException("Failed to add $pl to board")
                }
            }
            quest.init(this)
        }
        currentQuest = newQuest
        questInitialized = true
    }

    private fun addVaultDoor(cell: ZCell, zone: ZZone, pos: Grid.Pos, color: GColor) {
        // add a vault door leading to the cell specified by vaultFlag
        assertTrue(cell.vaultId > 0)
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
                zone.doors.add(ZDoor(pos, it2.pos, if (cell.environment == ZCell.ENV_VAULT) ZDir.ASCEND else ZDir.DESCEND, color))
                break
            }
        }
    }

    fun spawnZombies(count: Int, name: ZZombieType, zone: Int) {
        var count = count
        var name = name
        if (count == 0) return
        while (true) {
            val _name = name
            val numOnBoard = board.getAllZombies().count { it.type == _name }
            log.debug("Num %s on board is %d and trying to spawn %d more", name, numOnBoard, count)
            if (numOnBoard + count > quest.getMaxNumZombiesOfType(name)) {
                when (name) {
                    ZZombieType.Necromancer -> {
                        name = ZZombieType.Abomination
                        continue
                    }
                    ZZombieType.Abomination -> {
                        name = ZZombieType.Fatty
                        count *= 2
                        continue
                    }
                    ZZombieType.Fatty, ZZombieType.Runner -> {
                        name = ZZombieType.Walker
                        count *= 2
                        continue
                    }
                }
            }
            spawnZombiesInternal(name, count, zone)
            break
        }
    }

    private fun spawnZombiesInternal(type: ZZombieType, count: Int, zone: Int) {
        var count = count
        log.debug("spawn zombies %s X %d in zone %d", type, count, zone)
        if ((count > 0) && type.canDoubleSpawn && (spawnMultiplier > 1)) {
            log.debug("**** Spawn multiplier applied %d", spawnMultiplier)
            addLogMessage("Spawn Multiplier X $spawnMultiplier Applied")
            count *= spawnMultiplier
            spawnMultiplier = 1
        }
        var numCurrent = board.getAllZombies().count { it.type == type }
        val max = quest.getMaxNumZombiesOfType(type)
        log.debug("Num current %ss = %d with a max of %d", type, numCurrent, max)
        for (i in 0 until count) {
            if (numCurrent >= max) break
            numCurrent++
            val zombie = ZZombie(type, zone)
            quest.onZombieSpawned(this, zombie, zone)
            if (board.spawnActor(zombie))
                onZombieSpawned(zombie)
        }
    }

    protected open fun onZombieSpawned(zombie: ZZombie) {}
    val state: ZState
        get() {
            if (stateStack.empty()) pushState(ZState.BEGIN_ROUND, null)
            return stateStack.peek()!!.state
        }
    val stateEquipment: ZEquipment<*>?
        get() {
            if (stateStack.isEmpty()) throw GException("Invalid state")
            if (stateStack.peek()!!.equipment == null) throw GException("null equipment in state")
            return stateStack.peek()!!.equipment
        }

    private fun setState(state: ZState, c: ZPlayerName?) {
        stateStack.clear()
        pushState(state, c)
    }

    protected open fun onQuestComplete() {
        addLogMessage("Quest Complete")
    }

    private val isGameSetup: Boolean
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

    fun addTradeOptions(ch: ZCharacter, options: MutableList<ZMove>) {
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

    fun addWalkOptions(ch: ZCharacter, options: MutableList<ZMove>, action: ZActionType?) {
        ZDir.values().filter { board.canMove(ch, it) }.forEach { dir ->
            options.add(ZMove.newWalkDirMove(dir, action))
        }
        val accessibleZones = board.getAccessableZones(ch.occupiedZone, 1, 1, ZActionType.MOVE)
        if (accessibleZones.isNotEmpty()) options.add(ZMove.newWalkMove(accessibleZones, action))
    }

    fun isHandMoveAvailable(ch: ZCharacter, slot: ZEquipSlot, types: List<ZMoveType>): Boolean {
        for (ac: ZActionType in arrayOf(ZActionType.THROW_ITEM, ZActionType.RANGED, ZActionType.MELEE, ZActionType.MAGIC, ZActionType.ENCHANTMENT)) {
            val equip = ch.getSlot(slot)
            if (equip != null && equip.type.isActionType(ac)) {
                when (ac) {
                    ZActionType.THROW_ITEM -> if (types.contains(ZMoveType.THROW_ITEM)) return true
                    ZActionType.RANGED -> if (types.contains(ZMoveType.RANGED_ATTACK)) return true
                    ZActionType.MELEE -> if (types.contains(ZMoveType.MELEE_ATTACK)) return true
                    ZActionType.MAGIC -> if (types.contains(ZMoveType.MAGIC_ATTACK)) return true
                    ZActionType.ENCHANTMENT -> if (types.contains(ZMoveType.ENCHANT)) return true
                }
            }
        }
        return false
    }

    fun addHandOptions(ch: ZCharacter, options: MutableList<ZMove>) {
	    options.map { it.type}.let { types ->
		    if (isHandMoveAvailable(ch, ZEquipSlot.LEFT_HAND, types)) {
			    options.add(ZMove.newUseLeftHand())
		    }
		    if (isHandMoveAvailable(ch, ZEquipSlot.RIGHT_HAND, types)) {
			    options.add(ZMove.newUseRightHand())
		    }
	    }
    }

    private fun removeDeadZombies() {
        //for (z: ZActor<*> in Utils.filter(board.getAllActors(), { a: ZActor<*>? -> a is ZZombie })) {
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
    open fun runGame(): Boolean {
        log.debug("runGame %s", state)
        if (!isGameSetup) {
            log.error("Invalid Game")
            assertTrue(false)
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
        val user = getCurrentUser()
        removeDeadZombies()
        when (state) {
            ZState.INIT -> {
                for (cell: ZCell in board.getCells()) {
                    for (type: ZCellType in ZCellType.values()) {
                        if (cell.isCellType(type)) {
                            when (type) {
                                ZCellType.WALKER      -> spawnZombies(1, ZZombieType.Walker, cell.zoneIndex)
                                ZCellType.RUNNER      -> spawnZombies(1, ZZombieType.Runner, cell.zoneIndex)
                                ZCellType.FATTY       -> spawnZombies(1, ZZombieType.Fatty, cell.zoneIndex)
                                ZCellType.NECROMANCER -> spawnZombies(1, ZZombieType.Necromancer, cell.zoneIndex)
                                ZCellType.ABOMINATION -> spawnZombies(1, ZZombieType.Abomination, cell.zoneIndex)
                            }
                        }
                    }
                }
                setState(ZState.BEGIN_ROUND, null)
                onCurrentUserUpdated(getCurrentUser())
                return true
            }
            ZState.BEGIN_ROUND -> {
                onBeginRound(roundNum)
                if (roundNum > 0) setState(ZState.SPAWN, null) else setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null)
                roundNum++
                addLogMessage("Begin Round $roundNum")
                onStartRound(roundNum)
                if (currentUser != startUser) {
                    currentUser = startUser
                    onCurrentUserUpdated(getCurrentUser())
                }
                for (a: ZActor<*> in board.getAllActors()) a.onBeginRound()
                board.resetNoise()
            }
            ZState.SPAWN -> {

                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                val highestSkill = highestSkillLevel
                var zIdx = 0
                while (zIdx < board.getNumZones()) {
                    if (board.isZoneSpawnable(zIdx)) {
                        spawnZombies(zIdx, highestSkill)
                    }
                    zIdx++
                }
                setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null)
            }
            ZState.PLAYER_STAGE_CHOOSE_CHARACTER -> {

                // for each user, they choose each of their characters in any order and have them
                // perform all of their actions
                val options: MutableList<ZPlayerName> = ArrayList()

                // any player who has done a move and is not a tactician must continue to move
                for (nm: ZPlayerName in currentUserCharacters) {
                    val c = nm.character
                    if ((c.actionsLeftThisTurn > 0) && (c.actionsLeftThisTurn < c.actionsPerTurn) && !c.hasAvailableSkill(ZSkill.Tactician)) {
                        options.add(nm)
                        break
                    }
                }
                if (options.size == 0) {
                	options.addAll(currentUserCharacters.filter { it.character.actionsLeftThisTurn > 0 })
                }

                //if (options.size() > 0) {
                // add characters who have organized and can do so again
                //    options.addAll(Utils.filter(getAllLivingCharacters(), object -> object.actionsLeftThisTurn == 0 && object.inventoryThisTurn));
                //}
                var currentCharacter: ZPlayerName? = null
                if (options.size == 0) {
                    if (users.size > 1) {
                        currentUser = (currentUser + 1) % users.size
                        onCurrentUserUpdated(getCurrentUser())
                    }
                    if (currentUser == startUser) {
                        if (users.size > 2) startUser = (startUser + 1) % users.size
                        setState(ZState.ZOMBIE_STAGE, null)
                    }
                } else if (options.size == 1) {
                    currentCharacter = options[0]
                } else {
                    currentCharacter = getCurrentUser().chooseCharacter(options)
                }
                if (currentCharacter != null) {
                    pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, currentCharacter)
                }
            }
            ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION -> {
                val cur = currentCharacter!!
                val ch = cur.character
                if (!ch.isStartingWeaponChosen) {
                    if (ch.type.startingEquipment.size > 1) {
                        val type = getCurrentUser().chooseStartingEquipment(ch.type, listOf(*ch.type.startingEquipment))
                                ?: return false
                        ch.setStartingEquipment(type)
                    } else {
                        ch.setStartingEquipment(ch.type.startingEquipment[0])
                    }
                }
                val actionsLeft: Int = ch.actionsLeftThisTurn
                val options = LinkedList<ZMove>()

                // determine players available moves
                for (skill: ZSkill in ch.getAvailableSkills()) {
                    skill.addSpecialMoves(this, ch, options)
                }
                if (actionsLeft > 0) {

                    // check for organize
                    if (ch.allEquipment.size > 0) options.add(ZMove.newInventoryMove())
                    val zoneCleared = isClearedOfZombies(ch.occupiedZone)

                    // check for trade with another character in the same zone (even if they are dead)
                    if (zoneCleared) {
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
                    addWalkOptions(ch, options, ZActionType.MOVE)
                    run {
                        val melee: List<ZWeapon> = ch.meleeWeapons
                        if (melee.size > 0) {
                            options.add(ZMove.newMeleeAttackMove(melee))
                        }
                    }
                    run {
                        val ranged: List<ZWeapon> = ch.rangedWeapons
                        if (ranged.size > 0) {
                            options.add(ZMove.newRangedAttackMove(ranged))
                            for (slot: ZWeapon in ranged) {
                                if (!slot.isLoaded) {
                                    options.add(ZMove.newReloadMove(slot))
                                }
                            }
                        }
                    }
                    run {
                        val magic: List<ZWeapon> = ch.magicWeapons
                        if (magic.size > 0) {
                            options.add(ZMove.newMagicAttackMove(magic))
                        }
                    }
                    run {
                        val items: List<ZEquipment<*>> = ch.throwableEquipment
                        if (items.size > 0) {
                            options.add(ZMove.newThrowEquipmentMove(items))
                        }
                    }
                    val spells = ch.spells
                    if (spells.size > 0) {
                        options.add(ZMove.newEnchantMove(spells))
                    }
                    if (zone.type === ZZoneType.VAULT) {
                        val takables: List<ZEquipment<*>> = quest.getVaultItems(ch.occupiedZone)
                        if (takables.size > 0) {
                            options.add(ZMove.newPickupItemMove(takables))
                        }
                        val items = ch.allEquipment
                        if (items.size > 0) {
                            options.add(ZMove.newDropItemMove(items))
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
                    if (doors.size > 0) {
                        options.add(ZMove.newToggleDoor(doors))
                    }
                    if (barricadeDoors.size > 0 && ch.actionsLeftThisTurn >= ZActionType.BARRICADE_DOOR.costPerTurn()) {
                        options.add(ZMove.newBarricadeDoor(barricadeDoors))
                    }
                }
                if (options.size == 0) {
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null)
                    return false
                }
                options.addFirst(ZMove.newEndTurn())
                addHandOptions(ch, options)
                if (canSwitchActivePlayer()) options.add(ZMove.newSwitchActiveCharacter())

                // make sure no dups
                var i = 0
                while (i < options.size - 1) {
                    var ii = i + 1
                    while (ii < options.size) {
                        if ((options[i] == options[ii])) throw GException("Duplicate options:\n  " + options[i] + "\n  " + options[ii])
                        ii++
                    }
                    i++
                }
                val move = getCurrentUser().chooseMoveInternal((cur), options)
                return performMove(ch, move)
            }
            ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL -> {
                val cur = currentCharacter!!
                val ch = cur.character
                val options = cur.character.getRemainingSkillsForLevel(stateStack.peek()!!.skillLevel!!.color.ordinal)
                log.debug("Skill options for " + stateStack.peek()!!.skillLevel + " : " + options)
                when(options.size) {
                    0 -> {
                        stateStack.pop()
                        null
                    }
                    1 -> options[0]
                    else -> getCurrentUser().chooseNewSkill(cur, options)
                }?.let { skill ->
                    log.debug("New Skill Chosen: $skill")
                    onNewSkillAquired(cur, skill)
                    ch.addSkill(skill)
                    skill.onAcquired(this, ch)
                    options.remove(skill)
                    popState()
                    return true
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT -> {
                val cur = currentCharacter!!
                val equip = stateEquipment!!
                val options: MutableList<ZMove> = object : ArrayList<ZMove>() {
                    init {
                        add(ZMove.newKeepMove(equip))
                        add(ZMove.newDisposeEquipmentMove(equip))
                    }
                }
                if (cur.character.actionsLeftThisTurn > 0 && equip.isConsumable) {
                    options.add(ZMove.newConsumeMove(equip, null))
                }
                val move = getCurrentUser().chooseMoveInternal(cur, options)
                // need to pop first since performMove might push TODO: Consider remove?
                popState()
                if (!performMove(cur.character, move)) {
                    pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur, equip)
                    return false
                }
                return true
            }
            ZState.ZOMBIE_STAGE -> {

                // sort them such that filled zones have their actions performed first
                val zoneArr = Array(board.zones.size) { Pair(it, board.getActorsInZone(it)) }
                zoneArr.sortWith { o0, o1 ->
                    val z0 = board.zones[o0.first]
                    val z1 = board.zones[o1.first]
                    val numZ0 = o0.second.size
                    val numZ1 = o1.second.size
                    val maxPerCell = ZCellQuadrant.values().size
                    val numEmptyZ0 = z0.cells.size * maxPerCell - numZ0
                    val numEmptyZ1 = z1.cells.size * maxPerCell - numZ1
                    // order such that zones with fewest empty slots have their zombies move first
                    numEmptyZ0.compareTo(numEmptyZ1)
                }
                for (p in zoneArr) {
                    val zombies = p.second.filterIsInstance<ZZombie>()
                    for (zombie: ZZombie in zombies) {
                        var path: MutableList<ZDir>? = null
                        while (zombie.actionsLeftThisTurn > 0) {
                            val victims = board.getCharactersInZone(zombie.occupiedZone).filter { 
                                !it.isInvisible && it.isAlive
                            }
                            if (victims.size > 1) {
                                Collections.sort(victims, WoundingComparator(zombie.type))
                            }
                            if (victims.isNotEmpty()) {
                                val victim = victims[0]
                                zombie.performAction(ZActionType.MELEE, this)
                                if (playerDefends(victim, zombie.type)) {
                                    addLogMessage("${victim.name()} defends against ${zombie.name()}")
                                    onCharacterDefends(victim.type, zombie.position)
                                } else {
                                    playerWounded(victim, zombie, ZAttackType.NORMAL, 1, zombie.type.name)
                                }
                            } else {
                                if (path == null) {
                                    if (zombie.type === ZZombieType.Necromancer) {
                                        if (isZoneEscapableForNecromancers(zombie.occupiedZone)) {
                                            // necromancer is escaping!
                                            zombie.performAction(ZActionType.MOVE, this)
                                            onNecromancerEscaped(zombie)
                                            quest.onNecromancerEscaped(this, zombie)
                                            board.removeActor(zombie)
                                            continue
                                        }
                                        path = getZombiePathTowardNearestSpawn(zombie).toMutableList()
                                    }
                                    if (path == null)
                                        path = getZombiePathTowardVisibleCharactersOrLoudestZone(zombie).toMutableList()
                                    if (path.isEmpty()) {
                                        // make zombies move around randomly
                                        val zones = board.getAccessableZones(zombie.occupiedZone, 1, zombie.actionsPerTurn, ZActionType.MOVE)
                                        if (zones.isEmpty()) {
                                            path = ArrayList()
                                        } else {
                                            val paths = board.getShortestPathOptions(zombie.occupiedCell, zones.random())
                                            if (paths!!.isEmpty()) {
                                                path = ArrayList()
                                            } else {
                                                path = paths.random().toMutableList()
                                            }
                                        }
                                    } else {
                                        onZombiePath(zombie, path)
                                    }
                                }
                                if (path.isEmpty()) {
                                    zombie.performAction(ZActionType.NOTHING, this)
                                } else {
                                    moveActorInDirection(zombie, path.removeFirst(), ZActionType.MOVE)
                                }
                            }
                        }
                    }
                }
                allLivingCharacters.forEach { 
                    it.character.onEndOfRound(this)
                }
                setState(ZState.BEGIN_ROUND, null)
                return true
            }
            ZState.PLAYER_ENCHANT_SPEED_MOVE -> {

                // compute all empty of zombie zones 1 or 2 units away form current position
                val zones = board.getAccessableZones(currentCharacter!!.character.occupiedZone, 1, 2, ZActionType.MOVE)
                if (zones.size == 0) {
                    popState()
                } else {
                    val speedMove = getCurrentUser().chooseZoneToWalk(currentCharacter!!, zones)
                    if (speedMove != null) {
                        moveActor(currentCharacter!!.character, speedMove, 200, null)
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
                if (areas.size > 0) {
                    val cur = currentCharacter!!
                    val zIdx = user.chooseSpawnAreaToRemove(cur, areas)
                    if (zIdx != null) {
                        board.removeSpawn(areas[zIdx])
                        onCharacterDestroysSpawn(cur, zIdx)
                        popState()
                        return true
                    }
                } else {
                    popState()
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_WEAPON_FROM_DECK -> {
                user.chooseEquipmentClass(currentCharacter!!, listOf(*ZEquipmentClass.values()))?.let { clazz ->
                    val choices = allSearchables.filter { e: ZEquipment<*> -> e.type.equipmentClass === clazz }
                    user.chooseEquipmentInternal(currentCharacter!!, choices)?.let { equip ->
                        popState()
                        lootDeck.remove(equip)
                        giftEquipment(currentCharacter!!.character, equip)
                        return true
                    }
                }
                return false
            }
            ZState.PLAYER_STAGE_CHOOSE_VAULT_ITEM -> {
                quest.vaultItemsRemaining.takeIf { it.isNotEmpty() }?.let { items ->
                    user.chooseEquipmentInternal(currentCharacter!!, items)?.let {
                        popState()
                        items.remove(it)
                        giftEquipment(currentCharacter!!.character, it)
                        return true
                    }
                    
                }?:run {
                    popState()
                }
                return false
            }
        }
        return false
    }

    protected open fun onCharacterDestroysSpawn(c: ZPlayerName, zoneIdx: Int) {
        log.debug("%s destroys spawn at %d", c, zoneIdx)
    }

    protected open fun onCharacterDefends(cur: ZPlayerName, attackerPosition: ZActorPosition) {
        log.debug("%s defends from %s", cur, attackerPosition)
    }

    protected open fun onNewSkillAquired(c: ZPlayerName, skill: ZSkill) {
        log.debug("%s acquires new skill %s", c, skill)
    }

    private fun playerDefends(cur: ZCharacter, type: ZZombieType): Boolean {
        for (rating: Int in cur.getArmorRatings(type)) {
            addLogMessage("Defensive roll")
            val dice = rollDice(1)
            if (dice[0]!! >= rating) return true
        }
        return false
    }

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

    protected open fun onGameLost() {
        log.debug("GAME LOST")
    }

    fun playerWounded(victim: ZCharacter, attacker: ZActor<*>, attackType: ZAttackType, amount: Int, reason: String) {
        if (!victim.isDead) {
            victim.wound(amount)
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

    open fun onCharacterAttacked(character: ZPlayerName, attackerPosition: ZActorPosition, attackType: ZAttackType, characterPerished: Boolean) {
        log.debug("%s attacked from %s with %s and %s", character, attackerPosition, attackType, if (characterPerished) "Died" else "Was Wounded")
    }

    open fun onEquipmentThrown(c: ZPlayerName, icon: ZIcon, zone: Int) {
        log.debug("%s throws %s into %d", c, icon, zone)
    }

    fun isZoneEscapableForNecromancers(zoneIdx: Int): Boolean {
        val zone = board.getZone(zoneIdx)
        for (pos: Grid.Pos in zone.getCells()) {
            for (area: ZSpawnArea in board.getCell(pos).spawnAreas) {
                if (area.isEscapableForNecromancers) return true
            }
        }
        return false
    }

    fun canZoneSpawnNecromancers(zoneIdx: Int): Boolean {
        val zone = board.getZone(zoneIdx)
        for (pos: Grid.Pos in zone.getCells()) {
            for (area: ZSpawnArea in board.getCell(pos).spawnAreas) {
                if (area.isCanSpawnNecromancers) return true
            }
        }
        return false
    }

    fun getZombiePathTowardNearestSpawn(zombie: ZZombie): List<ZDir> {
        val pathsMap: MutableMap<Int, List<ZDir>> = HashMap()
        var shortestPath: Int? = null
        board.zones.filter { z: ZZone -> isZoneEscapableForNecromancers(z.zoneIndex) }.forEach { zone ->
            val paths: List<List<ZDir>> = board.getShortestPathOptions(zombie.occupiedCell, zone.zoneIndex)
            if (paths.isNotEmpty()) {
                pathsMap[zone.zoneIndex] = paths.get(0)
                if (shortestPath == null || paths.size < pathsMap[shortestPath]!!.size) {
                    shortestPath = zone.zoneIndex
                }
            }
        }
        return if (shortestPath == null) emptyList() else (pathsMap.get(shortestPath))!!
    }

    fun getZombiePathTowardVisibleCharactersOrLoudestZone(zombie: ZZombie): List<ZDir> {
        // zombie will move toward players it can see first and then noisy areas second
        var maxNoise = 0
        var targetZone = -1
        board.getAllCharacters().filter { ch: ZCharacter -> !ch.isInvisible && ch.isAlive }.forEach { c ->
            if (board.canSee(zombie.occupiedZone, c.occupiedZone)) {
                val noiseLevel = board.getZone(c.occupiedZone).noiseLevel
                if (maxNoise < noiseLevel) {
                    targetZone = c.occupiedZone
                    maxNoise = noiseLevel
                }
            }
        }
        if (targetZone < 0) {
            // move to noisiest zone
            for (zone in 0 until board.getNumZones()) {
                val noiseLevel = board.getZone(zone).noiseLevel
                if (noiseLevel > maxNoise) {
                    maxNoise = noiseLevel
                    targetZone = zone
                }
            }
        }
        if (targetZone >= 0) {
            val paths: List<List<ZDir>> = board.getShortestPathOptions(zombie.occupiedCell, targetZone)
            if (paths.isNotEmpty()) {
                return paths.random()
            }
        }
        return emptyList()
    }

    protected fun onStartRound(roundNum: Int) {}
    private fun useEquipment(c: ZCharacter, e: ZEquipment<*>): Boolean {
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

    private fun performMove(cur: ZCharacter, move: ZMove?): Boolean {
        var move: ZMove = move ?: return false
        log.debug("performMove:%s", move)
        val user = getCurrentUser()
        when (move.type) {
            ZMoveType.END_TURN -> {
                cur.clearActions()
                popState()
            }
            ZMoveType.SWITCH_ACTIVE_CHARACTER -> {
                if (canSwitchActivePlayer()) {
                    var idx = 0
                    for (nm: ZPlayerName in user.players) {
                        if (nm === cur.type) {
                            break
                        }
                        idx++
                    }
                    var i = (idx + 1) % user.players.size
                    while (i != idx) {
                        val c = user.players[i].character
                        if (c.isAlive && c.actionsLeftThisTurn > 0) {
                            popState()
                            pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, c.type)
                            return true
                        }
                        i = (i + 1) % user.players.size
                    }
                }
                return false
            }
            ZMoveType.TAKE_OBJECTIVE -> {
                cur.performAction(ZActionType.OBJECTIVE, this)
                val zone = board.getZone(cur.occupiedZone)
                zone.isObjective = false
                for (pos: Grid.Pos in zone.cells) {
                    ZCellType.values().filter { type: ZCellType -> type.isObjective }.forEach { ct ->
	                    board.getCell(pos).setCellType(ct, false)
                    }
                }
                addLogMessage(cur.name() + " Found an OBJECTIVE")
                quest.processObjective(this, cur)
                return true
            }
            ZMoveType.INVENTORY -> {

                // give options of which slot to organize
                val slots: MutableList<ZEquipSlot> = ArrayList()
                if (cur.leftHand != null) slots.add(ZEquipSlot.LEFT_HAND)
                if (cur.rightHand != null) slots.add(ZEquipSlot.RIGHT_HAND)
                if (cur.body != null) slots.add(ZEquipSlot.BODY)
                if (cur.numBackpackItems > 0) slots.add(ZEquipSlot.BACKPACK)
                val selectedSlot = getCurrentUser().chooseSlotToOrganize(cur.type, slots)
                        ?: return false
                // choose which equipment from the slot to organize
                var selectedEquipment: ZEquipment<*>? = null
                when (selectedSlot) {
                    ZEquipSlot.BACKPACK   -> if (cur.numBackpackItems > 1) {
                        // add
                        selectedEquipment = getCurrentUser().chooseEquipmentInternal(cur.type, cur.getBackpack())
                    } else {
                        selectedEquipment = cur.getBackpack().first()
                    }
                    ZEquipSlot.BODY       -> selectedEquipment = cur.body
                    ZEquipSlot.LEFT_HAND  -> selectedEquipment = cur.leftHand
                    ZEquipSlot.RIGHT_HAND -> selectedEquipment = cur.rightHand
                }
                if (selectedEquipment == null) return false

                // we have a slot and an equipment from the slot to do something with
                // we can:
                //   dispose, unequip, equip to an empty slot or consume
                val options: MutableList<ZMove> = ArrayList()
                if (selectedEquipment.isConsumable && cur.actionsLeftThisTurn > 0) {
                    options.add(ZMove.newConsumeMove(selectedEquipment, selectedSlot))
                }
                when (selectedSlot) {
                    ZEquipSlot.BACKPACK                                          -> if (selectedEquipment.isEquippable(cur)) {
                        for (slot: ZEquipSlot in cur.getEquippableSlots(selectedEquipment)) {
                            options.add(ZMove.newEquipMove(selectedEquipment, selectedSlot, slot))
                        }
                    }
                    ZEquipSlot.RIGHT_HAND, ZEquipSlot.LEFT_HAND, ZEquipSlot.BODY -> {
                        if (!cur.isBackpackFull) {
                            options.add(ZMove.newUnequipMove(selectedEquipment, selectedSlot))
                        }
                    }
                }
                options.add(ZMove.newDisposeMove(selectedEquipment, selectedSlot))
                user.chooseMoveInternal(cur.type, options)?.let {
                    return performMove(cur, it)
                }
                return false
            }
            ZMoveType.TRADE -> {
                when (move.list!!.size) {
                    1 -> move.list!![0] as ZPlayerName
                    else -> user.chooseTradeCharacter(cur.type, move.list as List<ZPlayerName>)
                }?.let { other ->
                    val options: MutableList<ZMove> = ArrayList()
                    // we can take if our backpack is not full or give if their backpack is not full
                    for (eq: ZEquipment<*> in cur.allEquipment) {
                        if (other.character.canTake(eq)) {
                            options.add(ZMove.newGiveMove(other, eq))
                        }
                    }
                    for (eq: ZEquipment<*> in other.character.allEquipment) {
                        if (cur.canTake(eq)) {
                            options.add(ZMove.newTakeMove(other, eq))
                        }
                    }
                    user.chooseMoveInternal(cur.type, options)?.let { move ->
                        return performMove(cur, move)
                    }
                }
                return false
            }
            ZMoveType.WALK -> {
                var zone = move.integer
                if (zone == null) zone = user.chooseZoneToWalk(cur.type, move.list as List<Int>)
                if (zone != null) {
                    moveActor(cur, zone, cur.moveSpeed, ZActionType.MOVE)
                    return true
                    //cur.performAction(ZActionType.MOVE, this);
                }
                return false
            }
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
                moveActorInDirection(cur, ZDir.values()[move.integer!!], move.action)
                return true
            }
            ZMoveType.USE_LEFT_HAND -> {
                return cur.getSlot(ZEquipSlot.LEFT_HAND)?.let {
                    useEquipment(cur, it)
                    true
                }?:false
            }
            ZMoveType.USE_RIGHT_HAND -> {
                return cur.getSlot(ZEquipSlot.RIGHT_HAND)?.let {
                    useEquipment(cur, it)
                    true
                }?:false
            }
            ZMoveType.MELEE_ATTACK -> {
                val weapons: List<ZWeapon> = move.list as List<ZWeapon>
                when (weapons.size) {
                    1 -> weapons[0]
                    else -> user.chooseWeaponSlotInternal(cur.type, weapons)
                }?.let { weapon ->
                    val stat = cur.getWeaponStat(weapon, ZActionType.MELEE, this, cur.occupiedZone)
                    if (performAttack(weapon, stat!!, cur.occupiedZone)) {
                        cur.performAction(ZActionType.MELEE, this)
                        return true
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
                        val zones = board.getAccessableZones(cur.occupiedZone, slot.type.throwMinRange, slot.type.throwMaxRange, ZActionType.THROW_ITEM).toMutableList()
                        zones.add(cur.occupiedZone)
                        zoneIdx = getCurrentUser().chooseZoneToThrowEquipment(cur.type, slot, zones)
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
                if (cur.isDualWielding && cur.body !== weapon) {
                    (cur.getSlot(ZEquipSlot.LEFT_HAND) as ZWeapon).reload()
                    (cur.getSlot(ZEquipSlot.RIGHT_HAND) as ZWeapon).reload()
                    addLogMessage(currentCharacter!!.name + " Reloaded both their " + weapon.label + "s")
                } else {
                    weapon.reload()
                    addLogMessage(currentCharacter!!.name + " Reloaded their " + weapon.label)
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
                            door.toggle(board)
                            //showMessage(currentCharacter.name() + " has opened a " + door.name());
                            // spawn zombies in the newly exposed zone and any adjacent
                            val otherSide = door.otherSide
                            if (board.getZone(board.getCell(otherSide.cellPosStart).zoneIndex).canSpawn()) {
                                val highest = highestSkillLevel
                                val spawnZones = HashSet<Int>()
                                board.getUndiscoveredIndoorZones(otherSide.cellPosStart, spawnZones)
                                log.debug("Zombie spawn zones: $spawnZones")
                                for (zone: Int in spawnZones) {
                                    spawnZombies(zone, highest)
                                }
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
                    assertTrue(!door.isClosed(board))
                    val barricade = cur.getEquipmentOfType(ZItemType.BARRICADE)!!
                    door.toggle(board, true)
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
                while (lootDeck.size > 0 && numCardsDrawn-- > 0) {
                    val equip = lootDeck.removeLast()
                    if (equip.type === ZItemType.AHHHH) {
                        addLogMessage("Aaaahhhh!!!")
                        onAhhhhhh(cur.type)
                        // spawn zombie right here right now
                        //spawnZombies(1, ZZombieType.Walker, cur.occupiedZone);
                        spawnZombies(cur.occupiedZone)
                        putBackInSearchables(equip)
	                    break // stop the search
                    } else {
                        found.add(equip)
                        quest.onEquipmentFound(this, equip)
                        pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur.type, equip)
                        if (equip!!.isDualWieldCapable && cur.hasAvailableSkill(ZSkill.Matching_set)) {
                            onBonusAction(cur.type, ZSkill.Matching_set)
                            pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur.type, equip.type.create())
                        }
                    }
                }
                if (found.size > 0) onEquipmentFound(cur.type, found)
                cur.performAction(ZActionType.SEARCH, this)
                return true
            }
            ZMoveType.EQUIP -> {
                val prev = cur.getSlot(move.toSlot!!)
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment!!, move.fromSlot!!)
                }
                cur.attachEquipment(move.equipment!!, move.toSlot!!)
                if (prev != null && !cur.isBackpackFull) {
                    cur.attachEquipment(prev, ZEquipSlot.BACKPACK)
                }
                cur.performAction(ZActionType.INVENTORY, this)
                return true
            }
            ZMoveType.UNEQUIP -> {
                cur.removeEquipment(move.equipment!!, move.fromSlot!!)
                cur.attachEquipment(move.equipment!!, ZEquipSlot.BACKPACK)
                cur.performAction(ZActionType.INVENTORY, this)
                return true
            }
            ZMoveType.TAKE -> {
                move.character!!.character.removeEquipment(move.equipment!!)
                cur.attachEquipment(move.equipment!!)
                cur.performAction(ZActionType.INVENTORY, this)
                return true
            }
            ZMoveType.GIVE -> {
                cur.removeEquipment(move.equipment!!)
                move.character!!.character.attachEquipment(move.equipment!!)
                cur.performAction(ZActionType.INVENTORY, this)
                return true
            }
            ZMoveType.DISPOSE -> {
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment!!, move.fromSlot!!)
                    cur.performAction(ZActionType.INVENTORY, this)
                }
                putBackInSearchables(move.equipment!!)
                return true
            }
            ZMoveType.KEEP -> {
                val equip = move.equipment!!
                var slot = cur.getEmptyEquipSlotForOrNull(equip)
                if (slot == null) {
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
                    }
                    getCurrentUser().chooseMoveInternal(cur.type, options)?.let { move ->
                        cur.removeEquipment(move.equipment!!, move.fromSlot!!)
                        putBackInSearchables(move.equipment!!)
                        slot = move.fromSlot
                    }
                }
                slot?.let {
                    cur.attachEquipment(equip, it)
                    return true
                }
            }
            ZMoveType.CONSUME -> {
                val item = move.equipment as ZItem
                val slot = move.fromSlot
                when (item.type) {
                    ZItemType.WATER, ZItemType.APPLES, ZItemType.SALTED_MEAT -> {
                        addExperience(cur, item.type.expWhenConsumed)
                        if (slot != null) {
                            cur.removeEquipment(item, slot)
                        }
                        cur.performAction(ZActionType.CONSUME, this)
                        putBackInSearchables(item)
                    }
                    else                                                     -> throw GException("Unhandled case: $item")
                }
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
                addNoise(move.integer!!, maxNoise + 1 - board.getZone(move.integer!!).noiseLevel)
                addLogMessage(cur.name() + " made alot of noise to draw the zombies!")
                cur.performAction(ZActionType.MAKE_NOISE, this)
                return true
            }
            ZMoveType.SHOVE -> {
                val targetZone = getCurrentUser().chooseZoneToShove(cur.type, move.list as List<Int>)
                if (targetZone != null) {
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
                //var spell: ZSpell? = null
                //var target: ZPlayerName? = null
                when (move.list!!.size) {
                    1 -> move.list!![0] as ZSpell
                    else -> getCurrentUser().chooseSpell(cur.type, cur.spells)
                }?.let { spell ->
                    allLivingCharacters.filter { board.canSee(cur.occupiedZone, it.character.occupiedZone) }.let { targets ->
                        user.chooseCharacterForSpell(cur.type, spell, targets)?.let { target ->
                            spell.type.doEnchant(this, target.character) //target.availableSkills.add(spell.type.skill);
                            cur.performAction(ZActionType.ENCHANTMENT, this)
                            return true
                        }
                    }
                }
                return false
            }
            ZMoveType.BORN_LEADER -> {
                val chosen = move.character?:getCurrentUser().chooseCharacterToBequeathMove(cur.type, move.list as List<ZPlayerName>)
                if (chosen != null) {
                    if (chosen.character.actionsLeftThisTurn > 0) {
                        chosen.character.addExtraAction()
                    } else {
                        chosen.character.addAvailableSkill(ZSkill.Plus1_Action)
                    }
                    cur.performAction(ZActionType.BEQUEATH_MOVE, this)
                    return true
                }
                return false
            }
            ZMoveType.BLOODLUST_MELEE, ZMoveType.BLOODLUST_MAGIC, ZMoveType.BLOODLUST_RANGED -> return performBloodlust(cur, move)
            else                                                                             -> log.error("Unhandled move: %s", move.type)
        }
        return false
    }

    private fun performBloodlust(cur: ZCharacter, move: ZMove): Boolean {
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
                performAttack(weapon, weapon.getStatForAction(action)!!, zone)
                cur.performAction(action, this)
                cur.removeAvailableSkill(move.skill!!)
                return true
            }
        }
        return false
    }

    protected open fun onAhhhhhh(c: ZPlayerName) {
        log.debug("AHHHHHHH!")
    }

    protected open fun onNecromancerEscaped(necro: ZZombie) {
        log.debug("necromancer %s escaped", necro)
    }

    protected open fun onEquipmentFound(c: ZPlayerName, equipment: List<ZEquipment<*>>) {
        log.debug("%s found %d eqipments", c, equipment.size)
    }

    open fun onCharacterHealed(c: ZPlayerName, amt: Int) {
        log.debug("%s healed %d pts", c, amt)
    }

    protected open fun onSkillKill(c: ZPlayerName, skill: ZSkill, z: ZZombie, attackType: ZAttackType) {
        log.debug("%s skill kill on %s with %s", c, z, attackType)
    }

    open fun onRollSixApplied(c: ZPlayerName, skill: ZSkill) {
        log.debug("%s roll six applied to %s", c, skill)
    }

    protected open fun onWeaponReloaded(c: ZPlayerName, w: ZWeapon) {
        log.debug("%s reloaded %s", c, w)
    }

    /*
    static List<ZZombie> filterZombiesForMelee(List<ZZombie> list, int weaponDamage) {
        List<ZZombie> zombies = Utils.filter(list, object -> object.type.minDamageToDestroy <= weaponDamage);
        if (zombies.size() > 1)
            Collections.sort(zombies);
        return zombies;
    }

    static List<ZZombie> filterZombiesForRanged(List<ZZombie> zombies, int weaponDamage) {
        if (zombies.size() > 1)
            Collections.sort(zombies);
        // find the first zombie whom we cannot destroy and remove that one and all after since ranged priority
        int numHittable = 0;
        for (ZZombie z : zombies) {
            if (z.type.minDamageToDestroy <= weaponDamage)
                numHittable++;
        }
//        log.debug("There are %d hittable zombies", numHittable);
        while (zombies.size() > numHittable)
            zombies.remove(zombies.size() - 1);
        return zombies;
    }

    static List<ZZombie> filterZombiesForMarksman(List<ZZombie> zombies, int weaponDamage) {
// marksman zombie sorting works differently
        zombies = Utils.filter(zombies, object -> object.type.minDamageToDestroy <= weaponDamage);
        Collections.reverse(zombies);
        return zombies;
    }
*/
    private fun performRangedOrMagicAttack(cur: ZCharacter, weapons: List<ZWeapon>, zoneIdx: Int?, actionType: ZActionType): Boolean {
        // rules same for both kinda
        var zoneIdx: Int? = zoneIdx
        val user = getCurrentUser()
        when (weapons.size) {
            1 -> weapons[0]
            else -> user.chooseWeaponSlotInternal(cur.type, weapons)
        }?.let { weapon ->
            if (zoneIdx == null) {
                cur.getWeaponStat(weapon, actionType, this, -1)?.let { stat ->
                    val zones = board.getAccessableZones(cur.occupiedZone, stat.minRange, stat.maxRange, actionType)
                    if (zones.isEmpty()) return false
                    zoneIdx = user.chooseZoneForAttack(cur.type, zones)
                }
            }
            zoneIdx?.let {
                cur.getWeaponStat(weapon, actionType, this, it)?.let { stat ->
                    if (performAttack(weapon, stat, it)) {
                        cur.performAction(actionType, this)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun performAttack(weapon: ZWeapon, stat: ZWeaponStat, zoneIdx: Int): Boolean {
        val cur = currentCharacter!!.character
        when (stat.actionType) {
            ZActionType.MELEE -> {
                val zombies = board.getZombiesInZone(cur.occupiedZone).toMutableList()
                Collections.sort(zombies, MarksmanComparator(stat.damagePerHit))
                var hits = resolveHits(cur, zombies.size, weapon.type, stat, zombies.size / 2 - 1, zombies.size / 2 + 1)
                // when we attack with melee there will be some misses and some hits that are defended
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
                onAttack(cur.type, weapon, stat.actionType, stat.numDice, zombiesHit, cur.occupiedZone)
                for (z: ZZombie in zombiesDestroyed) {
                    addExperience(cur, z.type.expProvided)
                    destroyZombie(z, stat.attackType, cur, weapon.type)
                }
                if (weapon.isAttackNoisy) {
                    addNoise(cur.occupiedZone, 1)
                }
                addLogMessage(currentCharacter!!.name + " Scored " + hits + " hits")
                cur.getAvailableSkills().mergeWith(weapon.type.skillsWhenUsed).forEach { skill ->
                    skill.onAttack(this, cur, weapon, stat.actionType, (stat), cur.occupiedZone, hits, zombiesDestroyed)
                }
                if (hits > 0) checkForHitAndRun(cur)
                return true
            }
            ZActionType.MAGIC, ZActionType.RANGED -> {

                //ZWeaponStat stat = cur.getWeaponStat(weapon, actionType, this, zoneIdx);
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
                            log.debug("Ranged Priority: %s", zombies.map { z: ZZombie -> z.type })
                        }
                        val hits = resolveHits(cur, zombies.size, weapon.type, stat, zombies.size / 2 - 1, zombies.size / 2 + 1)
                        var hitsMade = 0
                        val zombiesDestroyed: MutableList<ZZombie> = ArrayList()
                        val actorsHit: MutableList<ZActorPosition> = ArrayList()
                        var i = 0
                        while (i < hits && zombies.size > 0) {
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
                            val friendlyFireOptions = board.getCharactersInZone(zoneIdx).filter { ch: ZCharacter -> ch !== cur && ch.canReceiveFriendlyFire() }.toMutableList()
                            var i = 0
                            while (i < misses && friendlyFireOptions.size > 0) {
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
                                    //playerWounded(victim, cur, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                                    //if (victim.isDead())
                                    if (victim.woundBar + stat.damagePerHit >= ZCharacter.MAX_WOUNDS) {
                                        // killed em
                                        friendlyFireOptions.removeAt(0)
                                    }
                                }
                                i++
                            }
                        }
                        onAttack(cur.type, weapon, stat.actionType, stat.numDice, actorsHit, zoneIdx)
                        for (zombie: ZZombie in zombiesDestroyed) {
                            destroyZombie(zombie, stat.attackType, cur, weapon.type)
                            addExperience(cur, zombie.type.expProvided)
                        }
                        cur.getAvailableSkills().mergeWith(weapon.type.skillsWhenUsed).forEach { skill ->
                            skill.onAttack(this, cur, weapon, stat.actionType, (stat), zoneIdx, hits, zombiesDestroyed)
                        }
                        for (victim: ZCharacter in friendsHit) {
                            playerWounded(victim, cur, stat.attackType, stat.damagePerHit, "Friendly Fire!")
                        }
                        addLogMessage(currentCharacter!!.name + " Scored " + hitsMade + " hits")
                        if (hitsMade > 0) checkForHitAndRun(cur)
                        return true
                    }
                }
            }
        }
        return false
    }

    fun performSkillKill(c: ZCharacter, skill: ZSkill, z: ZZombie, at: ZAttackType) {
        onSkillKill(c.type, skill, z, at)
        addExperience(c, z.type.expProvided)
        destroyZombie(z, at, c, null)
    }

    internal class WoundingComparator(val zType: ZZombieType) : Comparator<ZCharacter> {
        override fun compare(o1: ZCharacter, o2: ZCharacter): Int {
            val v0 = o1.getArmorRating(zType) - o1.woundBar
            val v1 = o2.getArmorRating(zType) - o2.woundBar
            return Integer.compare(v1, v0)
        }
    }

    internal class RangedComparator() : Comparator<ZZombie> {
        override fun compare(o1: ZZombie, o2: ZZombie): Int {
            return Integer.compare(o1.type.rangedPriority, o2.type.rangedPriority)
        }
    }

    // Marksman have reverse of Ranged expect Zombies with minDamage above are prioritized last
    internal class MarksmanComparator(val attackDamage: Int) : Comparator<ZZombie> {
        override fun compare(o1: ZZombie, o2: ZZombie): Int {
            val o1Value = if (o1.type.minDamageToDestroy > attackDamage) 0 else o1.type.rangedPriority
            val o2Value = if (o2.type.minDamageToDestroy > attackDamage) 0 else o2.type.rangedPriority
            return Integer.compare(o2Value, o1Value) // descending order
        }
    }

    private fun resolveHits(cur: ZCharacter, maxHits: Int, type: ZWeaponType, stat: ZWeaponStat, minHitsForAutoReroll: Int, maxHitsForAutoNoReroll: Int): Int {
        var result: Array<Int>
        if (cur.canReroll(stat.attackType)) {
            result = rollDiceWithRerollOption(stat.numDice, stat.dieRollToHit, minHitsForAutoReroll, maxHitsForAutoNoReroll)
        } else {
            result = rollDice(stat.numDice)
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
                cur.getAvailableSkills().mergeWith(type.skillsWhenUsed).forEach { skill ->
                    if (skill.onSixRolled(this, cur, (stat)) && hits < maxHits) {
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

    protected open fun onWeaponGoesClick(c: ZPlayerName, weapon: ZWeapon) {
        log.debug("%s fired unloaded weapon %s", c, weapon)
    }

    protected open fun onCharacterOpenedDoor(cur: ZPlayerName, door: ZDoor) {
        log.debug("%s opened door %s", cur, door)
    }

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
    protected open fun onAttack(attacker: ZPlayerName, weapon: ZWeapon, actionType: ZActionType?, numDice: Int, actorsHit: List<ZActorPosition>, targetZone: Int) {
        log.debug("%s made %d hits in zone %d with %s action %s numDice %d", attacker, actorsHit.size, targetZone, weapon, actionType, numDice)
    }

    protected open fun onBonusAction(pl: ZPlayerName, action: ZSkill) {
        log.debug("%s got bonus action %s", pl, action)
    }

    private fun checkForHitAndRun(cur: ZCharacter) {
        if (cur.hasAvailableSkill(ZSkill.Hit_and_run) && board.getNumZombiesInZone(cur.occupiedZone) == 0) {
            cur.addAvailableSkill(ZSkill.Plus1_free_Move_Action)
            addLogMessage(cur.label + " used Hit and Run for a free move action")
            onBonusAction(cur.type, ZSkill.Plus1_free_Move_Action)
        }
    }

    val highestSkillLevel: ZSkillLevel
        get() {
            var best = ZSkillLevel(ZColor.BLUE)
            for (u: ZUser in users) {
                for (c: ZPlayerName in u.players) {
                    val lvl = c.character.skillLevel
                    if (best.compareTo(lvl) < 0) {
                        best = lvl
                    }
                }
            }
            return best
        }

    fun addExperience(c: ZCharacter, pts: Int) {
        if (pts <= 0) return
        var sl = c.skillLevel
        c.addExperience(pts)
        onCharacterGainedExperience(c.type, pts)
        // make so a user can level up multiple times in a single level up
        // need to push state in reverse order so that the lowest new level choices are first
        val states: MutableList<State> = ArrayList()
        while (!sl.equals(c.skillLevel)) {
            sl = sl.nextLevel()
            addLogMessage(c.name() + " has gained the " + sl.toString() + " skill level")
            states.add(State(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL, c.type, null, sl))
        }
        states.reverse()
        for (s: State in states) pushState(s)
    }

    protected open fun onCharacterGainedExperience(c: ZPlayerName, points: Int) {
        log.info("%s gained %d experence!", c, points)
    }

    fun rollDiceWithRerollOption(numDice: Int, dieNumToHit: Int, minHitsForAutoReroll: Int, maxHitsForAutoNoReroll: Int): Array<Int> {
        var minHitsForAutoReroll = minHitsForAutoReroll
        var maxHitsForAutoNoReroll = maxHitsForAutoNoReroll
        minHitsForAutoReroll = Math.max(minHitsForAutoReroll, 0)
        maxHitsForAutoNoReroll = Math.min(maxHitsForAutoNoReroll, numDice)
        val dice = rollDice(numDice)
        var hits = 0
        for (d: Int in dice) {
            if (d >= dieNumToHit) hits++
        }
        addLogMessage(currentCharacter!!.label + " Scored " + hits + " hits")
        //onRollDice(dice);
        if (hits >= maxHitsForAutoNoReroll) {
            return dice
        }
        if (hits > minHitsForAutoReroll) {
            val plentyOMove = getCurrentUser().chooseMoveInternal(currentCharacter!!,
                    Arrays.asList(ZMove.newReRollMove(), ZMove.newKeepRollMove()))
            if (plentyOMove != null) {
                if (plentyOMove.type === ZMoveType.KEEP_ROLL) return dice
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

    protected open fun onRollDice(roll: Array<Int>) {
        log.info("Rolling dice result is: %s", Arrays.toString(roll))
    }

    fun destroyZombie(zombie: ZZombie, deathType: ZAttackType, killer: ZCharacter, type: ZEquipmentType?) {
        killer.onKilledZombie(zombie, type)
        onZombieDestroyed(killer.type, deathType, zombie.position)
        zombie.destroyed = true //board.removeActor(zombie);
        if (zombie.type === ZZombieType.Necromancer) {
            pushState(ZState.PLAYER_STAGE_CHOOSE_SPAWN_AREA_TO_REMOVE, killer.type)
        }
    }

    protected open fun onZombieDestroyed(c: ZPlayerName, deathType: ZAttackType, zombiePos: ZActorPosition) {
        val zombie = board.getActor<ZZombie>(zombiePos)
        log.info("%s Zombie %s destroyed for %d experience", c.name, zombie.type.name, zombie.type.expProvided)
    }

    val allCharacters: List<ZPlayerName>
        get() = board.getAllCharacters().map { it.type }
    val allLivingCharacters: List<ZPlayerName>
        get() = allCharacters.filter { it.character.isAlive }
    val currentUserCharacters: List<ZPlayerName>
        get() {
            val list: MutableList<ZPlayerName> = ArrayList()
            for (nm: ZPlayerName in getCurrentUser().players) {
                if (nm.character.isAlive) list.add(nm)
            }
            return list
        }

    private fun isClearedOfZombies(zoneIndex: Int): Boolean {
        return board.getNumZombiesInZone(zoneIndex) == 0
    }

    protected open fun onDoubleSpawn(multiplier: Int) {
        log.debug("Double spawn X %d", multiplier)
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
            if (z.type.category === category) z.addExtraAction()
        }
    }

    protected open fun onExtraActivation(category: ZZombieCategory) {
        log.debug("Extra Activation %s", category)
    }

    fun spawnZombies(zoneIdx: Int) {
        spawnZombies(zoneIdx, highestSkillLevel)
    }

    private fun spawnZombies(zoneIdx: Int, level: ZSkillLevel) {
        //ZSpawnArea spawnType = quest.getSpawnType(this, board.getZone(zoneIdx));
        val card = quest.drawSpawnCard(this, zoneIdx, level)
        log.debug("Draw spawn card: $card")
        if (card == null) {
            return
        }
        val action = card.getAction(level.difficultyColor)
        when (action.action) {
            ActionType.NOTHING_IN_SIGHT -> addLogMessage("Nothing in Sight")
            ActionType.SPAWN -> spawnZombiesInternal(action.type!!, action.count, zoneIdx)
            ActionType.DOUBLE_SPAWN -> doubleSpawn()
            ActionType.EXTRA_ACTIVATION_STANDARD -> extraActivation(ZZombieCategory.STANDARD)
            ActionType.EXTRA_ACTIVATION_NECROMANCER -> extraActivation(ZZombieCategory.NECROMANCER)
            ActionType.EXTRA_ACTIVATION_WOLFSBURG -> extraActivation(ZZombieCategory.WOLFSBURG)
        }
    }

    fun getCurrentUser(): ZUser {
        val pl = currentCharacter
        if (pl != null) {
        	users.firstOrNull { it.players.contains(pl) }?.let {
        		return it
	        }
        }
        return users[currentUser.coerceIn(0 until users.size)]
    }

    open val currentCharacter: ZPlayerName?
        get() {
            return if (stateStack.isEmpty()) null else stateStack.peek().player
        }

    protected fun moveActor(actor: ZActor<*>, toZone: Int, speed: Long, actionType: ZActionType?) {
        val fromZone = actor.occupiedZone
        val fromPos = actor.occupiedCell
        val fromRect = actor.getRect(board)
        board.moveActor(actor, toZone)
        doMove(actor, fromZone, fromPos, fromRect, speed, actionType)
    }

    protected open fun moveActorInDirection(actor: ZActor<*>, dir: ZDir, action: ZActionType?) {
        val fromZone = actor.occupiedZone
        val fromPos = actor.occupiedCell
        val fromRect = actor.getRect(board)
        val next = board.getAdjacent(fromPos, dir)
        board.moveActor(actor, next)
        doMove(actor, fromZone, fromPos, fromRect, actor.moveSpeed, action)
    }

    fun moveActorInDirectionDebug(actor: ZActor<*>, dir: ZDir) {
        val fromZone = actor.occupiedZone
        val fromPos = actor.occupiedCell
        //GRectangle fromRect = actor.getRect(board);
        val next = board.getAdjacent(fromPos, dir)
        board.moveActor(actor, next)
        //doMove(actor, fromZone, fromPos, fromRect, actor.getMoveSpeed());
    }

    private fun doMove(actor: ZActor<*>, fromZone: Int, fromPos: Grid.Pos, fromRect: GRectangle, speed: Long, actionType: ZActionType?) {
        val toZone = actor.occupiedZone
        val toPos = actor.occupiedCell
        val toRect = actor.getRect(board)
        if (board.getZone(fromZone).type === ZZoneType.VAULT && board.getZone(toZone).type !== ZZoneType.VAULT) {
            // ascending the stairs
            val fromVaultRect = GRectangle(board.getCell(fromPos)).scale(.2f)
            val toVaultRect = GRectangle(board.getCell(toPos)).scale(.5f)
            onActorMoved(actor, fromRect, fromVaultRect, speed / 2)
            onActorMoved(actor, toVaultRect, toRect, speed / 2)
        } else if (board.getZone(fromZone).type !== ZZoneType.VAULT && board.getZone(toZone).type === ZZoneType.VAULT) {
            // descending the stairs
            val fromVaultRect = GRectangle(board.getCell(fromPos)).scale(.2f)
            val toVaultRect = GRectangle(board.getCell(toPos)).scale(.5f)
            onActorMoved(actor, fromRect, fromVaultRect, speed / 2)
            onActorMoved(actor, toVaultRect, toRect, speed / 2)
        } else {
            onActorMoved(actor, fromRect, toRect, speed)
        }
        if (toZone != fromZone && actionType != null) {
            actor.performAction(ZActionType.MOVE, this)
        }
    }

    protected open fun onBeginRound(roundNum: Int) {
        log.debug("Begin round %d", roundNum)
    }

    protected open fun onActorMoved(actor: ZActor<*>, start: GRectangle, end: GRectangle, speed: Long) {
        log.debug("actor %s moved from %s to %s with speed %d", actor, start, end, speed)
    }

    fun make(count: Int, e: ZEquipmentType): List<ZEquipment<*>> {
        val list: MutableList<ZEquipment<*>> = ArrayList()
        for (i in 0 until count) {
            list.add(e.create())
        }
        return list
    }

    fun initLootDeck() {
        lootDeck.clear()
        lootDeck.addAll(make(4, ZItemType.BARRICADE))
        lootDeck.addAll(make(4, ZItemType.AHHHH))
        lootDeck.addAll(make(2, ZItemType.APPLES))
        lootDeck.addAll(make(2, ZWeaponType.AXE))
        lootDeck.addAll(make(2, ZArmorType.CHAINMAIL))
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
        quest.processLootDeck(lootDeck)
        lootDeck.shuffle()
    }

    fun addNoise(zoneIdx: Int, noise: Int) {
        onNoiseAdded(zoneIdx)
        board.getZone(zoneIdx).addNoise(noise)
        //        showMessage("Noise was made in zone " + zoneIdx);
    }

    protected open fun onNoiseAdded(zoneIndex: Int) {
        log.debug("Noise added at %d", zoneIndex)
    }

    protected open fun onZombiePath(zombie: ZZombie, path: List<ZDir>) {}

    val gameSummaryTable: Table
        get() {
            val summary = Table("PLAYER", "KILLS", "FAV WEAPONS", "STATUS", "EXP", "LEVEL").setNoBorder()
            for (c: ZCharacter in board.getAllCharacters()) {
                summary.addRow(c.type, c.killsTable, c.favoriteWeaponsTable, if (c.isDead) "KIA" else "Alive", c.exp, c.skillLevel)
            }
            val gameStatus: String?
            when (gameOverStatus) {
                GAME_LOST -> gameStatus = quest.getQuestFailedReason(this)
                GAME_WON -> gameStatus = String.format("Completed")
                else      -> gameStatus = String.format("In Progress: %d%% Completed", quest.getPercentComplete(this).coerceIn(0 .. 100))
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
        if (cur.character.actionsLeftThisTurn == cur.character.actionsPerTurn)
            return true
        return cur.character.hasAvailableSkill(ZSkill.Tactician)
    }

    val allSearchables: List<ZEquipment<*>>
        get() = Collections.unmodifiableList(lootDeck)

    open fun onIronRain(c: ZPlayerName, targetZone: Int) {
        log.debug("%s unleashed iron rain at %d", c, targetZone)
    }

    protected open fun onDoorUnlocked(door: ZDoor) {
        log.debug("%s unlocked", door)
    }

    fun unlockDoor(door: ZDoor) {
        assertTrue(board.getDoor(door) === ZWallFlag.LOCKED)
        board.setDoor(door, ZWallFlag.CLOSED)
        onDoorUnlocked(door)
    }

    fun lockDoor(door: ZDoor) {
        assertTrue(board.getDoor(door) !== ZWallFlag.LOCKED)
        board.setDoor(door, ZWallFlag.LOCKED)
    }

    fun isDoorLocked(door: ZDoor): Boolean {
        return door.isLocked(board)
    }

    open fun addLogMessage(msg: String) {
        log.info(msg)
    }

    fun performDragonFire(cur: ZCharacter, zoneIdx: Int) {
        onDragonBileExploded(zoneIdx)
        board.getZone(zoneIdx).isDragonBile = false
        var exp = 0
        var num = 0
        addLogMessage(cur.name() + " ignited the dragon bile!")
        board.getActorsInZone(zoneIdx).filter { a -> a.isAlive }.forEach { a ->
            if (a is ZZombie) {
                val z = a
                exp += z.type.expProvided
                destroyZombie(z, ZAttackType.FIRE, cur, ZItemType.DRAGON_BILE)
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
        val slot = c.getEmptyEquipSlotForOrNull(e)
        if (slot != null) {
            c.attachEquipment((e), slot)
        } else {
            pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, c.type, e)
        }
    }

    private fun tryOpenDoor(cur: ZCharacter, door: ZDoor): Boolean {
        if (!door.isJammed) return true
        if (cur.tryOpenDoor(this)) {
            onCharacterOpenedDoor(cur.type, door)
            return true
        }
        onCharacterOpenDoorFailed(cur.type, door)
        return false
    }

    protected open fun onDragonBileExploded(zoneIdx: Int) {
        log.debug("Dragon bil eexploded in %d", zoneIdx)
    }

    fun chooseEquipmentFromSearchables() {
        pushState(ZState.PLAYER_STAGE_CHOOSE_WEAPON_FROM_DECK, currentCharacter)
    }

    fun chooseVaultItem() {
        pushState(ZState.PLAYER_STAGE_CHOOSE_VAULT_ITEM, currentCharacter)
    }

    fun giftRandomVaultArtifact(c: ZCharacter) {
        quest.vaultItemsRemaining.takeIf { it.isNotEmpty() }?.let {
            val equip = it.removeRandom()
            addLogMessage("${c.label} has been gifted a ${equip.label}")
            giftEquipment(c, equip)
        }
    }
}