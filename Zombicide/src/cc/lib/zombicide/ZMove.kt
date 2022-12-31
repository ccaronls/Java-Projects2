package cc.lib.zombicide


import cc.lib.ui.IButton
import cc.lib.utils.Reflector
import cc.lib.utils.prettify

class ZMove constructor(
	val type: ZMoveType=ZMoveType.END_TURN,
	val integer: Int?=null,
	val character: ZPlayerName?=null,
	val equipment: ZEquipment<*>?=null,
	val fromSlot: ZEquipSlot?=null,
	val toSlot: ZEquipSlot?=null,
	val list: List<*>?=null,
	val skill: ZSkill?=null,
	val action: ZActionType?=null) : Reflector<ZMove>(), IButton {
    companion object {
        fun newEndTurn(): ZMove {
            return ZMove(type = ZMoveType.END_TURN)
        }

        fun newWalkMove(zones: List<Int>, action: ZActionType?): ZMove {
            return ZMove(type = ZMoveType.WALK, list = zones, action = action)
        }

        fun newJumpMove(zones: List<Int>): ZMove {
            return ZMove(type = ZMoveType.JUMP, list = zones)
        }

        fun newChargeMove(zones: List<Int>): ZMove {
            return ZMove(type = ZMoveType.CHARGE, list = zones)
        }

        fun newUseLeftHand(): ZMove {
            return ZMove(type = ZMoveType.USE_LEFT_HAND)
        }

        fun newUseRightHand(): ZMove {
            return ZMove(type = ZMoveType.USE_RIGHT_HAND)
        }

        fun newToggleDoor(doors: List<ZDoor>): ZMove {
            return ZMove(type = ZMoveType.OPERATE_DOOR, list = doors)
        }

        fun newBarricadeDoor(doors: List<ZDoor>): ZMove {
            return ZMove(type = ZMoveType.BARRICADE, list = doors)
        }

        fun newSearchMove(zoneIndex: Int): ZMove {
            return ZMove(type = ZMoveType.SEARCH, integer = zoneIndex)
        }

        fun newMeleeAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(type = ZMoveType.MELEE_ATTACK, list = weapons)
        }

        fun newRangedAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(type = ZMoveType.RANGED_ATTACK, list = weapons)
        }

        fun newMagicAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(type = ZMoveType.MAGIC_ATTACK, list = weapons)
        }

        fun newThrowEquipmentMove(slots: List<ZEquipment<*>?>): ZMove {
            return ZMove(type = ZMoveType.THROW_ITEM, list = slots)
        }

        fun newInventoryMove(): ZMove {
            return ZMove(type = ZMoveType.INVENTORY)
        }

        fun newTradeMove(tradeOptions: List<ZPlayerName>): ZMove {
            return ZMove(type = ZMoveType.TRADE, list = tradeOptions)
        }

        fun newConsumeMove(equip: ZEquipment<*>, slot: ZEquipSlot?): ZMove {
            return ZMove(type = ZMoveType.CONSUME, equipment = equip, fromSlot = slot)
        }

        fun newEquipMove(equip: ZEquipment<*>, fromSlot: ZEquipSlot, toSlot: ZEquipSlot): ZMove {
            return ZMove(type = ZMoveType.EQUIP, equipment = equip, fromSlot = fromSlot, toSlot = toSlot)
        }

        fun newKeepMove(equip: ZEquipment<*>): ZMove {
            return ZMove(type = ZMoveType.KEEP, equipment = equip)
        }

        fun newUnequipMove(equip: ZEquipment<*>, slot: ZEquipSlot): ZMove {
            return ZMove(type = ZMoveType.UNEQUIP, equipment = equip, fromSlot = slot)
        }

        fun newDisposeMove(equip: ZEquipment<*>, slot: ZEquipSlot): ZMove {
            return ZMove(type = ZMoveType.DISPOSE, equipment = equip, fromSlot = slot)
        }

        fun newGiveMove(taker: ZPlayerName, toGive: ZEquipment<*>): ZMove {
            return ZMove(type = ZMoveType.GIVE, character = taker, equipment = toGive, fromSlot = toGive.slot)
        }

        fun newTakeMove(giver: ZPlayerName, toTake: ZEquipment<*>): ZMove {
            return ZMove(type = ZMoveType.TAKE, character = giver, equipment = toTake, fromSlot = toTake.slot)
        }

        fun newObjectiveMove(zone: Int): ZMove {
            return ZMove(type = ZMoveType.TAKE_OBJECTIVE, integer = zone)
        }

        fun newReloadMove(slot: ZWeapon): ZMove {
            return ZMove(type = ZMoveType.RELOAD, equipment = slot)
        }

        fun newPickupItemMove(takables: List<ZEquipment<*>>): ZMove {
            return ZMove(type = ZMoveType.PICKUP_ITEM, list = takables)
        }

        fun newDropItemMove(items: List<ZEquipment<*>>): ZMove {
            return ZMove(type = ZMoveType.DROP_ITEM, list = items)
        }

        fun newWalkDirMove(dir: ZDir, action: ZActionType?): ZMove {
            return ZMove(type = ZMoveType.WALK_DIR, integer = dir.ordinal, action = action)
        }

        fun newSwitchActiveCharacter(): ZMove {
            return ZMove(type = ZMoveType.SWITCH_ACTIVE_CHARACTER)
        }

        fun newMakeNoiseMove(occupiedZone: Int): ZMove {
            return ZMove(type = ZMoveType.MAKE_NOISE, integer = occupiedZone)
        }

        fun newShoveMove(toZones: List<Int>): ZMove {
            return ZMove(type = ZMoveType.SHOVE, list = toZones)
        }

        fun newReRollMove(): ZMove {
            return ZMove(type = ZMoveType.REROLL)
        }

        fun newKeepRollMove(): ZMove {
            return ZMove(type = ZMoveType.KEEP_ROLL)
        }

        fun newEnchantMove(spells: List<ZSpell>): ZMove {
            return ZMove(type = ZMoveType.ENCHANT, list = spells)
        }

        fun newBornLeaderMove(options: List<ZPlayerName>): ZMove {
            return ZMove(type = ZMoveType.BORN_LEADER, list = options)
        }

        fun newBloodlustMeleeMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(type = ZMoveType.BLOODLUST_MELEE, list = zones, skill = skill)
        }

        fun newBloodlustRangedMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(type = ZMoveType.BLOODLUST_RANGED, list = zones, skill = skill)
        }

        fun newBloodlustMagicMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(type = ZMoveType.BLOODLUST_MAGIC, list = zones, skill = skill)
        }

        fun newDisposeEquipmentMove(e: ZEquipment<*>): ZMove {
            return ZMove(type = ZMoveType.DISPOSE, equipment = e)
        }

        fun newIgniteMove(ignitableZones: List<Int>): ZMove {
            return ZMove(type = ZMoveType.IGNITE, list = ignitableZones)
        }

	    fun newCloseSpawnPortal(zone: Int) : ZMove {
		    return ZMove(type = ZMoveType.CLOSE_SPAWN_PORTAL, integer = zone, skill = ZSkill.Hand_of_God)
	    }

        init {
            addAllFields(ZMove::class.java)
        }
    }
	constructor(copy: ZMove, singleListElement: Any) : this(copy.type, copy.integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, integer: Int) : this(copy.type, integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, character: ZPlayerName?) : this(copy.type, copy.integer, character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, equipment: ZEquipment<*>?) : this(copy.type, copy.integer, copy.character, equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action) {}
/*
    private constructor(type: ZMoveType, action: ZActionType) : this(type, null, null, null, null, null, null, null, action) {}
    private constructor(type: ZMoveType, num: Int? = null) : this(type, num, null, null, null, null, null, null, null) {}
    private constructor(type: ZMoveType, num: Int, action: ZActionType?) : this(type, num, null, null, null, null, null, null, action) {}
    private constructor(type: ZMoveType, list: List<*>) : this(type, null, null, null, null, null, list, null, null) {}
    private constructor(type: ZMoveType, list: List<*>, action: ZActionType?) : this(type, null, null, null, null, null, list, null, action) {}
    private constructor(type: ZMoveType, list: List<*>, skill: ZSkill) : this(type, null, null, null, null, null, list, skill, null) {}
    private constructor(type: ZMoveType, equip: ZEquipment<*>, fromSlot: ZEquipSlot?) : this(type, null, null, equip, fromSlot, null, null, null, null) {}
    private constructor(type: ZMoveType, targetIndex: Int, character: ZPlayerName?, equip: ZEquipment<*>, fromSlot: ZEquipSlot?, toSlot: ZEquipSlot?, list: List<*>?) : this(type, targetIndex, character, equip, fromSlot, toSlot, list, null, null) {}
*/
    override fun toString(): String {
        return "ZMove{" +
                "type=" + type +
                ", integer=" + integer +
                ", equipment=" + equipment +
                ", fromSlot=" + fromSlot +
                ", toSlot=" + toSlot +
                ", list=" + list +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null) return false
        if (o is ZMoveType) return type === o
        if (o !is ZMove) return false
        val zMove = o
        return (type === zMove.type && cc.lib.utils.isEqual(equipment, zMove.equipment)
                && cc.lib.utils.isEqual(integer, zMove.integer)
                && character === zMove.character && fromSlot === zMove.fromSlot && toSlot === zMove.toSlot && skill === zMove.skill)
    }

	override fun hashCode(): Int {
		return type.hashCode()
	}

    override fun getTooltipText(): String? {
        return equipment?.tooltipText?:run {
            skill?.tooltipText?:run {
                character?.tooltipText?:type.toolTipText
            }
        }
    }

    override fun getLabel(): String {
        var label = prettify(type.name)
	    equipment?.let {
		    label += " ${it.label}"
	    }
        if (fromSlot != null) {
            label += " from ${fromSlot.label}"
        }
        if (toSlot != null)
        	label += " to ${toSlot.label}"
        return label
    }
}