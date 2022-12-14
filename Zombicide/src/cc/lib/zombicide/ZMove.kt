package cc.lib.zombicide


import cc.lib.ui.IButton
import cc.lib.utils.Reflector
import cc.lib.utils.prettify
import java.util.*

class ZMove private constructor(val type: ZMoveType=ZMoveType.END_TURN, val integer: Int?, val character: ZPlayerName?, val equipment: ZEquipment<*>?, val fromSlot: ZEquipSlot?, val toSlot: ZEquipSlot?, val list: List<*>?, val skill: ZSkill?, val action: ZActionType?) : Reflector<ZMove>(), IButton {
    companion object {
        fun newEndTurn(): ZMove {
            return ZMove(ZMoveType.END_TURN)
        }

        fun newWalkMove(zones: List<Int>, action: ZActionType?): ZMove {
            return ZMove(ZMoveType.WALK, zones, action)
        }

        fun newJumpMove(zones: List<Int>): ZMove {
            return ZMove(ZMoveType.JUMP, zones)
        }

        fun newChargeMove(zones: List<Int>): ZMove {
            return ZMove(ZMoveType.CHARGE, zones)
        }

        fun newUseLeftHand(): ZMove {
            return ZMove(ZMoveType.USE_LEFT_HAND)
        }

        fun newUseRightHand(): ZMove {
            return ZMove(ZMoveType.USE_RIGHT_HAND)
        }

        fun newToggleDoor(doors: List<ZDoor>): ZMove {
            return ZMove(ZMoveType.OPERATE_DOOR, doors)
        }

        fun newBarricadeDoor(doors: List<ZDoor>): ZMove {
            return ZMove(ZMoveType.BARRICADE, doors)
        }

        fun newSearchMove(zoneIndex: Int): ZMove {
            return ZMove(ZMoveType.SEARCH, zoneIndex)
        }

        fun newMeleeAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(ZMoveType.MELEE_ATTACK, weapons)
        }

        fun newRangedAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(ZMoveType.RANGED_ATTACK, weapons)
        }

        fun newMagicAttackMove(weapons: List<ZWeapon>): ZMove {
            return ZMove(ZMoveType.MAGIC_ATTACK, weapons)
        }

        fun newThrowEquipmentMove(slots: List<ZEquipment<*>?>): ZMove {
            return ZMove(ZMoveType.THROW_ITEM, slots)
        }

        fun newInventoryMove(): ZMove {
            return ZMove(ZMoveType.INVENTORY)
        }

        fun newTradeMove(tradeOptions: List<ZPlayerName>): ZMove {
            return ZMove(ZMoveType.TRADE, tradeOptions)
        }

        fun newConsumeMove(equip: ZEquipment<*>, slot: ZEquipSlot?): ZMove {
            return ZMove(ZMoveType.CONSUME, equip, slot)
        }

        fun newEquipMove(equip: ZEquipment<*>, fromSlot: ZEquipSlot, toSlot: ZEquipSlot): ZMove {
            return ZMove(ZMoveType.EQUIP, 0, null, equip, fromSlot, toSlot, null)
        }

        fun newKeepMove(equip: ZEquipment<*>): ZMove {
            return ZMove(ZMoveType.KEEP, equip, null)
        }

        fun newUnequipMove(equip: ZEquipment<*>, slot: ZEquipSlot): ZMove {
            return ZMove(ZMoveType.UNEQUIP, equip, slot)
        }

        fun newDisposeMove(equip: ZEquipment<*>, slot: ZEquipSlot): ZMove {
            return ZMove(ZMoveType.DISPOSE, equip, slot)
        }

        fun newGiveMove(taker: ZPlayerName, toGive: ZEquipment<*>): ZMove {
            return ZMove(ZMoveType.GIVE, 0, taker, toGive, null, null, null)
        }

        fun newTakeMove(giver: ZPlayerName, toTake: ZEquipment<*>): ZMove {
            return ZMove(ZMoveType.TAKE, 0, giver, toTake, null, null, null)
        }

        @JvmStatic
        fun newObjectiveMove(zone: Int): ZMove {
            return ZMove(ZMoveType.TAKE_OBJECTIVE, zone)
        }

        fun newReloadMove(slot: ZWeapon): ZMove {
            return ZMove(ZMoveType.RELOAD, slot, slot.slot)
        }

        fun newPickupItemMove(takables: List<ZEquipment<*>>): ZMove {
            return ZMove(ZMoveType.PICKUP_ITEM, takables)
        }

        fun newDropItemMove(items: List<ZEquipment<*>>): ZMove {
            return ZMove(ZMoveType.DROP_ITEM, items)
        }

        fun newWalkDirMove(dir: ZDir, action: ZActionType?): ZMove {
            return ZMove(ZMoveType.WALK_DIR, dir.ordinal, action)
        }

        fun newSwitchActiveCharacter(): ZMove {
            return ZMove(ZMoveType.SWITCH_ACTIVE_CHARACTER)
        }

        fun newMakeNoiseMove(occupiedZone: Int): ZMove {
            return ZMove(ZMoveType.MAKE_NOISE, occupiedZone)
        }

        fun newShoveMove(toZones: List<Int>): ZMove {
            return ZMove(ZMoveType.SHOVE, toZones)
        }

        fun newReRollMove(): ZMove {
            return ZMove(ZMoveType.REROLL)
        }

        fun newKeepRollMove(): ZMove {
            return ZMove(ZMoveType.KEEP_ROLL)
        }

        fun newEnchantMove(spells: List<ZSpell>): ZMove {
            return ZMove(ZMoveType.ENCHANT, spells)
        }

        fun newBornLeaderMove(options: List<ZPlayerName>): ZMove {
            return ZMove(ZMoveType.BORN_LEADER, options)
        }

        fun newBloodlustMeleeMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(ZMoveType.BLOODLUST_MELEE, zones, skill)
        }

        fun newBloodlustRangedMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(ZMoveType.BLOODLUST_RANGED, zones, skill)
        }

        fun newBloodlustMagicMove(zones: List<Int>, skill: ZSkill): ZMove {
            return ZMove(ZMoveType.BLOODLUST_MAGIC, zones, skill)
        }

        fun newDisposeEquipmentMove(e: ZEquipment<*>): ZMove {
            return ZMove(ZMoveType.DISPOSE, e, null)
        }

        fun newIgniteMove(ignitableZones: List<Int>): ZMove {
            return ZMove(ZMoveType.IGNITE, ignitableZones)
        }

	    fun newCloseSpawnPortal(zone: Int) : ZMove {
		    return ZMove(ZMoveType.CLOSE_SPAWN_PORTAL, listOf(zone), ZSkill.Hand_of_God)
	    }

        init {
            addAllFields(ZMove::class.java)
        }
    }

    constructor() : this(ZMoveType.END_TURN) {}
    constructor(copy: ZMove, singleListElement: Any) : this(copy.type, copy.integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList<Any>(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, integer: Int) : this(copy.type, integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList<Any>(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, character: ZPlayerName?) : this(copy.type, copy.integer, character, copy.equipment, copy.fromSlot, copy.toSlot, Arrays.asList<Any>(singleListElement), copy.skill, copy.action) {}
    constructor(copy: ZMove, singleListElement: Any, equipment: ZEquipment<*>?) : this(copy.type, copy.integer, copy.character, equipment, copy.fromSlot, copy.toSlot, Arrays.asList<Any>(singleListElement), copy.skill, copy.action) {}
    private constructor(type: ZMoveType, action: ZActionType) : this(type, null, null, null, null, null, null, null, action) {}
    private constructor(type: ZMoveType, num: Int? = null) : this(type, num, null, null, null, null, null, null, null) {}
    private constructor(type: ZMoveType, num: Int, action: ZActionType?) : this(type, num, null, null, null, null, null, null, action) {}
    private constructor(type: ZMoveType, list: List<*>) : this(type, null, null, null, null, null, list, null, null) {}
    private constructor(type: ZMoveType, list: List<*>, action: ZActionType?) : this(type, null, null, null, null, null, list, null, action) {}
    private constructor(type: ZMoveType, list: List<*>, skill: ZSkill) : this(type, null, null, null, null, null, list, skill, null) {}
    private constructor(type: ZMoveType, equip: ZEquipment<*>, fromSlot: ZEquipSlot?) : this(type, null, null, equip, fromSlot, null, null, null, null) {}
    private constructor(type: ZMoveType, targetIndex: Int, character: ZPlayerName?, equip: ZEquipment<*>, fromSlot: ZEquipSlot?, toSlot: ZEquipSlot?, list: List<*>?) : this(type, targetIndex, character, equip, fromSlot, toSlot, list, null, null) {}

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

    override fun getTooltipText(): String? {
        return equipment?.tooltipText?:run {
            skill?.tooltipText?:run {
                character?.tooltipText?:type.toolTipText
            }
        }
    }

    override fun getLabel(): String {
        var label = prettify(type.name)
	    equipment?.let { label += " ${it.label}"}
        if (toSlot == null && fromSlot != null) {
            label += " from ${fromSlot.label}"
        }
        if (toSlot != null) "$label to ${toSlot.label}"
        return label
    }
}