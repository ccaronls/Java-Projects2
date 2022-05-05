package cc.lib.zombicide

import cc.lib.game.*
import cc.lib.logger.LoggerFactory
import cc.lib.ui.IButton
import cc.lib.utils.Pair
import cc.lib.utils.Table
import cc.lib.zombicide.ZEquipSlot.Companion.wearableValues
import cc.lib.zombicide.ZSkillLevel.Companion.getLevel
import java.util.*

class ZCharacter(override val type: ZPlayerName=ZPlayerName.Ann, skillz: Array<Array<ZSkill>> = emptyArray()) : ZActor<ZPlayerName>(-1), Table.Model, IButton {
    companion object {
        val log = LoggerFactory.getLogger(ZCharacter::class.java)
        const val MAX_BACKPACK_SIZE = 5
        const val MAX_WOUNDS = 4
        const val MOVE_SPEED_MILIS = 750
        const val PRIORITY = 100

        init {
            addAllFields(ZCharacter::class.java)
        }
    }

    var woundBar = 0
        private set
    var exp = 0
        private set

    private val actionsDoneThisTurn: MutableList<ZActionType> = ArrayList()
    private val allSkills: MutableList<ZSkill> = ArrayList() // all skills based on the characters level and choices
    private val availableSkills: MutableSet<ZSkill> = HashSet() // skills from all skills minus ones used that are once per turn

    @Omit
    private var cachedSkills: MutableList<ZSkill>? = null // cached from getAvailableSkills() - includes skills given by weapons or armor
    private val skillsRemaining: Array<MutableList<ZSkill>> = skillz.map { it.toMutableList() }.toTypedArray()
    private val backpack: MutableList<ZEquipment<*>> = ArrayList()
    var leftHand: ZEquipment<*>? = null
        private set
    var rightHand: ZEquipment<*>? = null
        private set
    var body: ZEquipment<*>? = null
        private set
    private val kills = IntArray(ZZombieType.values().size)
    private val favoriteWeapons: MutableMap<ZEquipmentType, Int> = HashMap()
    private var fallen = false
    private var forceInvisible = false
    var color = GColor.WHITE
    var zonesMoved = 0
        private set
    var isStartingWeaponChosen = false
        private set

    init {
        val blueSkills = getRemainingSkillsForLevel(0)
        allSkills.addAll(blueSkills)
        blueSkills.clear()
    }

    @Synchronized
    fun clear() {
        Arrays.fill(kills, 0)
        body = null
        rightHand = body
        leftHand = rightHand
        backpack.clear()
        allSkills.clear()
        availableSkills.clear()
        cachedSkills = null
        exp = 0
        woundBar = 0
        isStartingWeaponChosen = false
        favoriteWeapons.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other !is ZCharacter) false else other.type === type
    }

    override val imageId: Int
        get() = type.imageId
    override val outlineImageId: Int
        get() = type.outlineImageId

    override fun getDimension(): GDimension {
        return type.imageDim
    }

    fun addExp(exp: Int) {
        this.exp += exp
    }

    override fun onBeginRound() {
        actionsDoneThisTurn.clear()
        availableSkills.clear()
        availableSkills.addAll(allSkills)
        cachedSkills = null
        zonesMoved = 0
        fallen = isDead
        super.onBeginRound()
    }

    @Synchronized
    @Throws(Exception::class)
    override fun deserialize(`in`: MyBufferedReader) {
        super.deserialize(`in`)
        type.character = this
        cachedSkills = null
    }

    override fun getMaxCharsPerLine(): Int {
        return 256
    }

    val killsTable: Table
        get() {
            val tab = Table(this).setNoBorder().setPadding(0)
            var added = false
            for (nm in ZZombieType.values()) {
                if (kills.size > nm.ordinal && kills[nm.ordinal] > 0) {
                    tab.addRow(nm.toString() + " x " + kills[nm.ordinal])
                    added = true
                }
            }
            if (!added) {
                tab.addRow("None")
            }
            return tab
        }
    val favoriteWeaponsTable: Table
        get() {
            val tab = Table(this).setNoBorder().setPadding(0)
            val list = Utils.toList(favoriteWeapons)
            Collections.sort(list) { o1: Pair<ZEquipmentType?, Int?>, o2: Pair<ZEquipmentType?, Int?> -> Integer.compare(o2.second!!, o1.second!!) }
            for (p in list) {
                tab.addRow(p.first.label + " x " + p.second)
            }
            return tab
        }

    override fun name(): String {
        return type.label
    }

    fun onKilledZombie(zombie: ZZombie, type: ZEquipmentType?) {
        kills[zombie.type.ordinal]++
        if (type != null) Utils.incrementCountingMap(favoriteWeapons, type, 1)
    }

    override fun performAction(action: ZActionType, game: ZGame): Boolean {
        if (action === ZActionType.MOVE) {
            zonesMoved++
        }
        for (skill in getAvailableSkills()) {
            when (skill.modifyActionsRemaining(this, action, game)) {
                1 -> {
                    removeAvailableSkill(skill)
                    game.addLogMessage(name() + " used " + skill.label)
                    //game.addLogMessage(name() + " used " + skill.getLabel() + " for a free action");
                    return true
                }
                -1 -> {
                    game.addLogMessage(name() + " used " + skill.label)
                    return true
                }
            }
        }
        when (action) {
            ZActionType.INVENTORY, ZActionType.CONSUME -> if (!hasAvailableSkill(ZSkill.Inventory)) {
                addAvailableSkill(ZSkill.Inventory)
            }
        }
        if (action.oncePerTurn()) {
            actionsDoneThisTurn.add(action)
        }
        if (isInvisible && action.breaksInvisibility()) {
            removeAvailableSkill(ZSkill.Invisible)
        }
        return super.performAction(action, game)
    }

    fun tryOpenDoor(game: ZGame): Boolean {
        val weapons = weapons
        // order the weapons so that the best choice for door is in the front
        weapons.sortedWith { o1: ZWeapon, o2: ZWeapon ->
            // first order by % to open
            val v1 = o1.openDoorValue
            val v2 = o2.openDoorValue
            v2.compareTo(v1)
        }
        for (w in weapons) {
            val openDoorStat = w.openDoorStat
            if (openDoorStat != null) {
                if (w.type.openDoorsIsNoisy) game.addNoise(occupiedZone, 1)
                if (openDoorStat.dieRollToOpenDoor > 1) {
                    val die = game.rollDice(1)
                    if (die[0] < openDoorStat.dieRollToOpenDoor) {
                        game.addLogMessage(name() + " Failed to open the door with their " + w)
                        return false
                    }
                }
                game.addLogMessage(name() + " Used their " + w + " to break open the door")
                return true
            }
        }
        return false
    }

    // TODO: consider implementing modifyActionsRemaining for Plus1_Action
    public override val actionsPerTurn: Int
        get() {
            var actions = 3
            for (s in getAvailableSkills()) {
                if (s === ZSkill.Plus1_Action) // TODO: consider implementing modifyActionsRemaining for Plus1_Action
                    actions++
            }
            return actions
        }

    fun canUnjamDoor(): Boolean {
        for (w in weapons) {
            if (w.isOpenDoorCapable) return true
        }
        return false
    }

    fun canBarricadeDoors(): Boolean {
        return isInPossession(ZItemType.BARRICADE)
    }

    private fun getEquipped(): List<ZEquipment<*>> = listOf(leftHand, rightHand, body).filterNotNull()
    
    /**
     *
     * @return
     */
    val weapons: List<ZWeapon>
        get() = getEquipped().filterIsInstance<ZWeapon>()

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    val armor: List<ZArmor>
        get() = getEquipped().filterIsInstance<ZArmor>()
    
    val spells: List<ZSpell>
        get() {
            val spells = getEquipped().filterIsInstance<ZSpell>().toMutableList()
            if (getAvailableSkills().contains(ZSkill.Spellbook)) {
                spells.addAll(backpack.filterIsInstance<ZSpell>())
            }
            return spells
        }

    /**
     *
     * @return
     */
    val isDualWielding: Boolean
        get() {
            if (leftHand == null || rightHand == null) return false
            if (leftHand!!.type != rightHand!!.type) return false
            if (leftHand!!.isDualWieldCapable) return true
            if (leftHand != null && leftHand is ZWeapon) {
                for (skill in getAvailableSkills()) {
                    if (skill.canTwoHand(leftHand!! as ZWeapon)) return true
                }
            }
            return false
        }

    fun isDualWielding(weapon: ZWeapon): Boolean {
        return (canDualWield(weapon)
                && leftHand != null && leftHand!!.type == weapon.type && rightHand != null && rightHand!!.type == weapon.type)
    }

    fun canDualWield(weapon: ZWeapon): Boolean {
        if (weapon.isDualWieldCapable) return true
        for (skill in getAvailableSkills()) {
            if (skill.canTwoHand(weapon)) return true
        }
        return false
    }

    val numBackpackItems: Int
        get() = backpack.size

    fun getBackpackItem(index: Int): ZEquipment<*> {
        return backpack[index]
    }

    fun getBackpack(): List<ZEquipment<*>> {
        return backpack
    }

    fun getEmptyEquipSlotFor(e: ZEquipment<*>): ZEquipSlot {
        if (e.isEquippable(this)) {
            if (body == null && canEquipBody(e)) {
                return ZEquipSlot.BODY
            }
            when (e.slotType) {
                ZEquipSlotType.HAND -> {
                    if (leftHand == null) {
                        return ZEquipSlot.LEFT_HAND
                    }
                    if (rightHand == null) {
                        return ZEquipSlot.RIGHT_HAND
                    }
                }
                ZEquipSlotType.BODY -> if (body == null) {
                    return ZEquipSlot.BODY
                }
            }
        }
        Utils.assertTrue(!isBackpackFull)
        return ZEquipSlot.BACKPACK
    }

    fun getEmptyEquipSlotForOrNull(e: ZEquipment<*>): ZEquipSlot? {
        if (e.isEquippable(this)) {
            if (body == null && canEquipBody(e)) {
                return ZEquipSlot.BODY
            }
            when (e.slotType) {
                ZEquipSlotType.HAND -> {
                    if (leftHand == null) {
                        return ZEquipSlot.LEFT_HAND
                    }
                    if (rightHand == null) {
                        return ZEquipSlot.RIGHT_HAND
                    }
                }
                ZEquipSlotType.BODY -> if (body == null) {
                    return ZEquipSlot.BODY
                }
            }
        }
        return if (isBackpackFull) null else ZEquipSlot.BACKPACK
    }

    /**
     * If user can equip the item the result slot equipped too. null otherwise
     * @param e
     * @return
     */
    fun tryEquip(e: ZEquipment<*>): ZEquipSlot? {
        if (e.isEquippable(this)) {
            if (body == null && canEquipBody(e)) {
                body = e
                e.slot = ZEquipSlot.BODY
                cachedSkills = null
                return ZEquipSlot.BODY
            }
            when (e.slotType) {
                ZEquipSlotType.HAND -> {
                    if (leftHand == null) {
                        leftHand = e
                        cachedSkills = null
                        e.slot = ZEquipSlot.LEFT_HAND
                        return e.slot
                    }
                    if (rightHand == null) {
                        rightHand = e
                        cachedSkills = null
                        e.slot = ZEquipSlot.RIGHT_HAND
                        return e.slot
                    }
                }
                ZEquipSlotType.BODY -> if (body == null) {
                    body = e
                    cachedSkills = null
                    e.slot = ZEquipSlot.BODY
                    return e.slot
                }
            }
        }
        if (backpack.size < MAX_BACKPACK_SIZE) {
            backpack.add(e)
            e.slot = ZEquipSlot.BACKPACK
            return e.slot
        }
        return null
    }

    fun removeEquipment(type: Any, slot: ZEquipSlot): ZEquipment<*>? {
        var removed: ZEquipment<*>? = null
        when (slot) {
            ZEquipSlot.BODY -> {
                removed = body
                body = null
            }
            ZEquipSlot.BACKPACK -> {
                val idx = backpack.indexOf(type)
                Utils.assertTrue(idx >= 0)
                removed = backpack.removeAt(idx)
            }
            ZEquipSlot.LEFT_HAND -> {
                removed = leftHand
                leftHand = null
            }
            ZEquipSlot.RIGHT_HAND -> {
                removed = rightHand
                rightHand = null
            }
        }
        cachedSkills = null
        removed?.slot = null
        return removed
    }

    @JvmOverloads
    fun attachEquipment(equipment: ZEquipment<*>, slot: ZEquipSlot = getEmptyEquipSlotFor(equipment)): ZEquipment<*>? {
        equipment.slot = slot
        var prev: ZEquipment<*>? = null
        when (slot) {
            ZEquipSlot.RIGHT_HAND -> {
                prev = rightHand
                rightHand = equipment
            }
            ZEquipSlot.LEFT_HAND -> {
                prev = leftHand
                leftHand = equipment
            }
            ZEquipSlot.BODY -> {
                prev = body
                body = equipment
            }
            ZEquipSlot.BACKPACK -> backpack.add(equipment)
        }
        cachedSkills = null
        return prev
    }

    val skillLevel: ZSkillLevel
        get() = getLevel(exp)

    @Synchronized
    fun getInfoTable(game: ZGame): Table {

        /*

        Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
        -----------------------------------------------------------------------

        <left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


         */
        val info = Table(this).setNoBorder().setPadding(0)
        if (isDualWielding) {
            info.addColumn("Each Hand(DW)", listOf(getSlotInfo(ZEquipSlot.LEFT_HAND, game)))
            info.addColumn(ZEquipSlot.BODY.label, listOf(getSlotInfo(ZEquipSlot.BODY, game)))
        } else {
            info.addColumn(ZEquipSlot.LEFT_HAND.label, listOf(getSlotInfo(ZEquipSlot.LEFT_HAND, game)))
            info.addColumn(ZEquipSlot.BODY.label, listOf(getSlotInfo(ZEquipSlot.BODY, game)))
            info.addColumn(ZEquipSlot.RIGHT_HAND.label, listOf(getSlotInfo(ZEquipSlot.RIGHT_HAND, game)))
        }
        val slotInfo = getSlotInfo(ZEquipSlot.BACKPACK, game)
        info.addColumn(ZEquipSlot.BACKPACK.label + if (isBackpackFull) " (full)" else "", listOf(slotInfo))
        val stats = Table(this).setNoBorder().setPadding(0)
        var armorRating = StringBuilder("none")
        val ratings = getArmorRatings(ZZombieType.Walker)
        if (ratings.size > 0) {
            armorRating = StringBuilder("" + ratings[0] + "+")
            for (i in 1 until ratings.size) {
                armorRating.append("/").append(ratings[i]).append("+")
            }
        }
        stats.addRow("Moves", String.format("%d of %d", actionsLeftThisTurn, actionsPerTurn))
        stats.addRow("Wounds", String.format("%d of %d", woundBar, MAX_WOUNDS))
        stats.addRow("Armor Rolls", armorRating.toString())
        val sl = skillLevel
        val ptsToNxt = sl.getPtsToNextLevel(exp)
        stats.addRow("Skill", sl)
        stats.addRow("Exp", exp)
        stats.addRow("Next level", ptsToNxt)
        stats.addRow("Dual\nWielding", isDualWielding)
        info.addColumn("Stats", listOf(stats))
        if (getAvailableSkills().size > 0) {
            val skills = Table(this).setNoBorder().addColumnNoHeader(Utils.toStringArray(getAvailableSkills(), true))
            info.addColumn("Skills", skills)
        }
        val main = Table(this).setNoBorder()
        if (isDead) {
            main.addRow(String.format("%s (%s) Killed in Action",
                    type.label, type.characterClass))
        } else {
            main.addRow(String.format("%s (%s) Body:%s",
                    type.label, type.characterClass, type.alternateBodySlot
            ))
        }
        main.addRow(info)
        return main
    }

    val isBackpackFull: Boolean
        get() = backpack.size == MAX_BACKPACK_SIZE

    override fun drawInfo(g: AGraphics, game: ZGame, width: Float, height: Float): GDimension? {
        return getInfoTable(game).draw(g)
    }

    fun canTrade(): Boolean {
        return allEquipment.size > 0
    }

    fun canSearch(zone: ZZone): Boolean {
        if (actionsDoneThisTurn.contains(ZActionType.SEARCH)) return false
        return if (getAvailableSkills().contains(ZSkill.Scavenger)) true else zone.isSearchable
    }

    fun canTake(e: ZEquipment<*>): Boolean {
        if (!isBackpackFull) return true
        if (!e.isEquippable(this)) return false
        when (e.slotType) {
            ZEquipSlotType.BODY -> return body == null
            ZEquipSlotType.HAND -> {
                return if (canEquipBody(e) && body == null) true else leftHand == null || rightHand == null
            }
        }
        return false
    }

    fun getEquippableSlots(equip: ZEquipment<*>): List<ZEquipSlot> {
        val options: MutableList<ZEquipSlot> = ArrayList()
        val canEquip = !isBackpackFull || null != Utils.findFirstOrNull(backpack) { e: ZEquipment<*>? -> e === equip }
        val canWield = equip.isEquippable(this)
        when (equip.slotType) {
            ZEquipSlotType.BODY -> if (canWield && (body == null || canEquip)) options.add(ZEquipSlot.BODY)
            ZEquipSlotType.HAND -> if (canWield) {
                if (leftHand == null || canEquip) options.add(ZEquipSlot.LEFT_HAND)
                if (rightHand == null || canEquip) options.add(ZEquipSlot.RIGHT_HAND)
                if (body == null || canEquip) {
                    if (canEquipBody(equip)) {
                        options.add(ZEquipSlot.BODY)
                    }
                }
            }
            ZEquipSlotType.BACKPACK -> if (!isBackpackFull) {
                options.add(ZEquipSlot.BACKPACK)
            }
        }
        return options
    }

    fun canEquipBody(equip: ZEquipment<*>): Boolean {
        return type.alternateBodySlot == equip.type.equipmentClass
    }

    val meleeWeapons: List<ZWeapon>
        get() {
            val slots: MutableList<ZWeapon> = ArrayList()
            if (isDualWielding && leftHand?.isMelee == true) {
                slots.add(leftHand as ZWeapon)
            } else {
                if (leftHand?.isMelee == true) slots.add(leftHand as ZWeapon)
                if (rightHand?.isMelee == true) slots.add(rightHand as ZWeapon)
            }
            if (body != null && body!!.isMelee) slots.add(body as ZWeapon)
            return slots
        }
    val rangedWeapons: List<ZWeapon>
        get() {
            val slots: MutableList<ZWeapon> = ArrayList()
            if (isDualWielding) {
                leftHand?.let {
                    if (it.isRanged && isLoaded(it))
                        slots.add(it as ZWeapon)
                }
            } else {
                arrayOf(leftHand, rightHand).filterNotNull().forEach {
                    if (it.isRanged && isLoaded(it))
                        slots.add(it as ZWeapon)
                }
            }
            body?.let {
                if (it.isRanged && isLoaded(it))
                    slots.add(it as ZWeapon)
            }
            return slots
        }
    val magicWeapons: List<ZWeapon>
        get() {
            val slots: MutableList<ZWeapon> = ArrayList()
            if (isDualWielding && leftHand?.isMagic == true) {
                slots.add(leftHand as ZWeapon)
            } else {
                if (leftHand != null && leftHand?.isMagic == true) slots.add(leftHand as ZWeapon)
                if (rightHand != null && rightHand?.isMagic == true) slots.add(rightHand as ZWeapon)
            }
            if (body != null && body?.isMagic == true) slots.add(body as ZWeapon)
            if (getAvailableSkills().contains(ZSkill.Spellbook)) {
                slots.addAll(backpack.filterIsInstance<ZWeapon>().filter { it.isMagic })
            }
            return slots
        }
    val throwableEquipment: List<ZEquipment<*>>
        get() {
            val slots: MutableList<ZEquipment<*>> = ArrayList()
            if (leftHand != null && leftHand!!.isThrowable) slots.add(leftHand!!)
            if (rightHand != null && rightHand!!.isThrowable) slots.add(rightHand!!)
            if (body != null && body!!.isThrowable) slots.add(body!!)
            return slots
        }

    fun canReload(): Boolean {
        return false
    }

    fun isLoaded(weapon: ZEquipment<*>): Boolean {
        return true
    }

    fun getWeaponStat(weapon: ZWeapon, actionType: ZActionType, game: ZGame, targetZone: Int): ZWeaponStat? {
        return weapon.getStatForAction(actionType)?.let { _stat ->
            val stat = _stat.copy()
            for (skill in Utils.mergeLists(getAvailableSkills(), weapon.type.skillsWhenUsed)) {
                skill.modifyStat(stat, actionType, this, game, targetZone)
            }
            if (weapon.slot !== ZEquipSlot.BODY && isDualWielding(weapon)) {
                stat.numDice *= 2
            }
            stat
        }
    }

    fun getSlot(slot: ZEquipSlot): ZEquipment<*>? {
        when (slot) {
            ZEquipSlot.BODY -> return body
            ZEquipSlot.RIGHT_HAND -> return rightHand
            ZEquipSlot.LEFT_HAND -> return leftHand
        }
        return null
    }

    fun getArmorRatings(type: ZZombieType?): List<Int> {
        val ratings: MutableList<Int> = ArrayList()
        var skillRating = 0
        for (skill in getAvailableSkills()) {
            skillRating = Math.max(skillRating, skill.getArmorRating(type))
        }
        for (slot in wearableValues()) {
            val e = getSlot(slot)
            if (e != null) {
                var rating = e.type.getDieRollToBlock(type!!)
                if (rating > 0) {
                    if (e.type.isShield) {
                        ratings.add(rating)
                    } else {
                        if (skillRating > 0) rating -= 1
                        ratings.add(rating)
                        skillRating = 0
                    }
                }
            }
        }
        if (skillRating > 0) {
            ratings.add(skillRating)
        }
        return ratings
    }

    /**
     *
     * @param type
     * @return
     */
    fun getArmorRating(type: ZZombieType?): Int {
        var rating = 0
        for (i in getArmorRatings(type)) {
            rating += i
        }
        return rating
    }

    /**
     *
     * @return
     */
    val isDead: Boolean
        get() = woundBar >= MAX_WOUNDS

    /**
     *
     * @return
     */
    override val isAlive: Boolean
        get() = woundBar < MAX_WOUNDS
    override val noise: Int
        get() = if (isInvisible) 0 else 1
    override var isInvisible: Boolean
        get() = forceInvisible || getAvailableSkills().contains(ZSkill.Invisible)
        set(enable) {
            forceInvisible = enable
        }

    fun getSlotInfo(slot: ZEquipSlot?, game: ZGame): Table? {
        when (slot) {
            ZEquipSlot.RIGHT_HAND -> {
                return if (rightHand == null) null else rightHand!!.getCardInfo(this, game)
            }
            ZEquipSlot.LEFT_HAND -> {
                return if (leftHand == null) null else leftHand!!.getCardInfo(this, game)
            }
            ZEquipSlot.BODY -> {
                return if (body == null) {
                    null
                } else body!!.getCardInfo(this, game)
            }
            ZEquipSlot.BACKPACK -> {
                if (backpack.size == 0) return null
                val table = Table().setNoBorder()
                for (e in backpack) {
                    table.addRow(e)
                }
                return table
            }
        }
        return null
    }

    val allEquipment: List<ZEquipment<*>>
        get() {
            val all: MutableList<ZEquipment<*>> = ArrayList()
            all.addAll(backpack)
            all.addAll(listOf(leftHand, body, rightHand).filterNotNull())
            return all
        }

    fun removeEquipment(equip: ZEquipment<*>) {
        when (equip.slot) {
            ZEquipSlot.BACKPACK -> {
                val success = backpack.remove(equip)
                Utils.assertTrue(success)
            }
            ZEquipSlot.LEFT_HAND -> {
                Utils.assertTrue(leftHand === equip)
                leftHand = null
                cachedSkills = null
            }
            ZEquipSlot.RIGHT_HAND -> {
                Utils.assertTrue(rightHand === equip)
                rightHand = null
                cachedSkills = null
            }
            ZEquipSlot.BODY -> {
                Utils.assertTrue(body === equip)
                body = null
                cachedSkills = null
            }
        }
        cachedSkills = null
        equip.slot = null
    }

    /**
     * Return true if a equip type is in any of the users slots or backpack
     * @param type
     * @return
     */
    fun isInPossession(type: ZEquipmentType): Boolean {
        for (e in allEquipment) {
            if (e.type == type) return true
        }
        return false
    }

    fun getEquipmentOfType(type: ZEquipmentType): ZEquipment<*>? {
        return allEquipment.firstOrNull { it.type == type }
    }

    fun isEquipped(type: ZEquipmentType): Boolean {
        return Utils.count(Utils.toArray(leftHand, rightHand, body)) { e: ZEquipment<*>? -> e!!.type === type } > 0
    }

    fun canReroll(action: ZAttackType): Boolean {
        if (getAvailableSkills().contains(ZSkill.Lucky)) return true
        when (action) {
            ZAttackType.RANGED_ARROWS -> return isInPossession(ZItemType.PLENTY_OF_ARROWS)
            ZAttackType.RANGED_BOLTS -> return isInPossession(ZItemType.PLENTY_OF_BOLTS)
        }
        return false
    }

    fun onEndOfRound(game: ZGame) {
        for (skill in getAvailableSkills()) {
            skill.onEndOfTurn(game, this)
        }
        availableSkills.clear()
        for (e in Arrays.asList(leftHand, rightHand, body)) {
            e?.onEndOfRound(game)
        }
        availableSkills.addAll(allSkills)
        cachedSkills = null
    }

    override val moveSpeed: Long
        get() = MOVE_SPEED_MILIS.toLong()
    override val priority: Int
        get() = PRIORITY

    fun canFriendlyFire(): Boolean {
        for (s in getAvailableSkills()) {
            if (s.avoidsInflictingFriendlyFire()) return false
        }
        return true
    }

    fun canReceiveFriendlyFire(): Boolean {
        if (isDead) return false
        for (s in getAvailableSkills()) {
            if (s.avoidsReceivingFriendlyFire()) return false
        }
        return true
    }

    override fun draw(g: AGraphics) {
        if (fallen) {
            g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect.fit(g.getImage(ZIcon.GRAVESTONE.imageIds[0])))
        } else {
            drawPedestal(g)
            super.draw(g)
        }
    }

    protected fun drawPedestal(g: AGraphics) {
        val hgt = 0.04f
        val rect = rect
        if (color != null) {
            g.color = color!!.darkened(.5f)
            val x = rect.x + 0.02f
            val w = rect.w - 0.04f
            g.drawFilledRect(x, rect.y + rect.h - hgt / 4, w, hgt)
            g.drawFilledOval(x, rect.y + rect.h + hgt / 4, w, hgt)
            g.color = color
            g.drawFilledOval(x, rect.y + rect.h - hgt / 2, w, hgt)
        }
    }

    fun getAvailableSkills(): List<ZSkill> {
        cachedSkills?.also {
            return it
        }
        val skillz = ArrayList<ZSkill>()
        skillz.addAll(availableSkills)
            for (e in arrayOf(leftHand, body, rightHand)) {
                if (e != null) {
                skillz.addAll(e.type.skillsWhileEquipped)
                }
            }
        cachedSkills = skillz
        return skillz.toList()
    }

    fun hasAvailableSkill(skill: ZSkill): Boolean {
        return getAvailableSkills().contains(skill)
    }

    fun useMarksmanForSorting(zoneIdx: Int): Boolean {
        for (skill in getAvailableSkills()) {
            if (skill.useMarksmanForSorting(occupiedZone, zoneIdx)) {
                return true
            }
        }
        return false
    }

    fun hasSkill(skill: ZSkill): Boolean {
        return allSkills.contains(skill)
    }

    fun addSkill(skill: ZSkill) {
        allSkills.add(skill)
        availableSkills.add(skill)
    }

    fun addAvailableSkill(skill: ZSkill) {
        availableSkills.add(skill)
        cachedSkills = null
    }

    fun removeAvailableSkill(skill: ZSkill) {
        availableSkills.remove(skill)
        cachedSkills = null
    }

    fun getRemainingSkillsForLevel(level: Int): MutableList<ZSkill> {
        if (level < skillsRemaining.size)
            return skillsRemaining[level]
        return mutableListOf()
    }

    fun setFallen(fallen: Boolean) {
        this.fallen = fallen
    }

    fun heal(game: ZGame, amt: Int): Boolean {
        if (woundBar > 0) {
            game.addLogMessage(String.format("%s has %d wounds healed.", name(), amt))
            woundBar = Math.max(0, woundBar - amt)
            return true
        }
        return false
    }

    fun wound(amt: Int) {
        woundBar += amt
    }

    fun getKills(type: ZZombieType): Int {
        return kills[type.ordinal]
    }

    fun addExperience(pts: Int) {
        exp += pts
    }

    fun setStartingEquipment(type: ZEquipmentType) {
        attachEquipment(type.create())
        isStartingWeaponChosen = true
    }

    override val isNoisy: Boolean
        get() = isAlive && !isInvisible
    override val scale: Float
        get() {
            when (type) {
                ZPlayerName.Nelly -> return 1.4f
                ZPlayerName.Baldric -> return 1.2f
            }
            return super.scale
        }
}