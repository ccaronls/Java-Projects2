package cc.lib.zombicide


import cc.lib.reflector.Reflector
import cc.lib.ui.IButton

data class ZMove(
	val type: ZMoveType = ZMoveType.END_TURN,
	val integer: Int? = null,
	val character: ZPlayerName? = null,
	val equipment: ZEquipment<*>? = null,
	val fromSlot: ZEquipSlot? = null,
	val toSlot: ZEquipSlot? = null,
	val list: List<*>? = null,
	val skill: ZSkill? = null,
	val action: ZActionType? = null,
	val text: String? = null,
	val familiar: ZFamiliarType? = null
) : Reflector<ZMove>(), IButton {
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

		fun newUseSlot(slot: ZEquipSlot): ZMove {
			return ZMove(type = ZMoveType.USE_SLOT, fromSlot = slot)
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
			return ZMove(type = ZMoveType.MELEE_ATTACK, list = weapons, action = ZActionType.MELEE)
		}

		fun newRangedAttackMove(weapons: List<ZWeapon>, zoneIdx: Int? = null): ZMove {
			return ZMove(type = ZMoveType.RANGED_ATTACK, list = weapons, action = ZActionType.RANGED, integer = zoneIdx)
		}

		fun newMagicAttackMove(weapons: List<ZWeapon>, zoneIdx: Int? = null): ZMove {
			return ZMove(type = ZMoveType.MAGIC_ATTACK, list = weapons, action = ZActionType.MAGIC, integer = zoneIdx)
		}

		fun newThrowEquipmentMove(slots: List<ZEquipment<*>?>): ZMove {
			return ZMove(type = ZMoveType.THROW_ITEM, list = slots)
		}

		fun newTradeMove(tradeOptions: List<ZPlayerName>): ZMove {
			return ZMove(type = ZMoveType.TRADE, list = tradeOptions)
		}

        fun newConsumeMove(equip: ZEquipment<*>, slot: ZEquipSlot?): ZMove {
            return ZMove(type = ZMoveType.CONSUME, equipment = equip, fromSlot = slot)
        }

        fun newEquipMove(equip: ZEquipment<*>, fromSlot: ZEquipSlot?, toSlot: ZEquipSlot, action: ZActionType? = null, player : ZPlayerName?=null): ZMove {
            return ZMove(type = ZMoveType.EQUIP, equipment = equip, fromSlot = fromSlot, toSlot = toSlot, action = action, character = player)
        }

        fun newKeepMove(equip: ZEquipment<*>): ZMove {
            return ZMove(type = ZMoveType.KEEP, equipment = equip)
        }

        fun newUnequippedMove(equip: ZEquipment<*>, slot: ZEquipSlot): ZMove {
	        return ZMove(type = ZMoveType.UNEQUIP, equipment = equip, fromSlot = slot)
        }

        fun newDisposeMove(equip: ZEquipment<*>, slot: ZEquipSlot?=null): ZMove {
            return ZMove(type = ZMoveType.DISPOSE, equipment = equip, fromSlot = slot?:equip.slot)
        }

        fun newGiveMove(taker: ZPlayerName, toGive: ZEquipment<*>, toSlot : ZEquipSlot?): ZMove {
            return ZMove(type = ZMoveType.GIVE, character = taker, equipment = toGive, fromSlot = toGive.slot, toSlot = toSlot)
        }

        fun newTakeMove(giver: ZPlayerName, toTake: ZEquipment<*>, toSlot : ZEquipSlot?): ZMove {
            return ZMove(type = ZMoveType.TAKE, character = giver, equipment = toTake, fromSlot = toTake.slot, toSlot = toSlot)
        }

	    fun newOrganizeTakeMove(taker: ZPlayerName, toTake: ZEquipment<*>, toSlot : ZEquipSlot?): ZMove {
		    return ZMove(type = ZMoveType.ORGANIZE_TAKE, character = taker, equipment = toTake, fromSlot = toTake.slot, toSlot = toSlot)
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

		fun newPickupItemMove(character: ZPlayerName, equip: ZEquipment<*>, toSlot: ZEquipSlot): ZMove {
			return ZMove(character = character, type = ZMoveType.PICKUP_ITEM, equipment = equip, toSlot = toSlot)
		}

		fun newOrganizePickupMove(moves: List<ZMove>): ZMove {
			return ZMove(type = ZMoveType.ORGANIZE_PICKUP, list = moves)
		}

		fun newDropItemMove(items: List<ZEquipment<*>>): ZMove {
			return ZMove(type = ZMoveType.DROP_ITEM, list = items)
		}

		fun newDropItemMove(item: ZEquipment<*>): ZMove {
			return ZMove(type = ZMoveType.DROP_ITEM, equipment = item)
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

        fun newIgniteMove(ignitableZones: List<Int>): ZMove {
            return ZMove(type = ZMoveType.IGNITE, list = ignitableZones)
        }

	    fun newCloseSpawnPortal(zone: Int) : ZMove {
		    return ZMove(type = ZMoveType.CLOSE_SPAWN_PORTAL, integer = zone, skill = ZSkill.Hand_of_God)
	    }

	    fun newOrganize() : ZMove {
	    	return ZMove(type = ZMoveType.ORGANIZE)
	    }

	    fun newOrganizeDone() : ZMove {
		    return ZMove(type = ZMoveType.ORGANIZE_DONE)
	    }

		fun newOrganizeTrade(player: ZPlayerName): ZMove {
			return ZMove(type = ZMoveType.ORGANIZE_TRADE, character = player)
		}

		fun newOrganizeSlot(player: ZPlayerName, slot: ZEquipSlot?, moves: List<ZMove>): ZMove {
			return ZMove(
				type = ZMoveType.ORGANIZE_SLOT,
				fromSlot = slot,
				character = player,
				list = moves
			)
		}

		fun newMoveSiegeEngine(player: ZPlayerName, zoneOptions: List<Int>): ZMove {
			return ZMove(type = ZMoveType.SIEGE_ENGINE_MOVE, character = player, list = zoneOptions)
		}

		fun newFireCatapultScatterShot(player: ZPlayerName, zoneOptions: List<Int>): ZMove {
			return ZMove(
				type = ZMoveType.CATAPULT_FIRE_SCATTERSHOT,
				character = player,
				list = zoneOptions
			)
		}

		fun newFireCatapultGrapeShot(player: ZPlayerName, zoneOptions: List<Int>): ZMove {
			return ZMove(
				type = ZMoveType.CATAPULT_FIRE_GRAPESHOT,
				character = player,
				list = zoneOptions
			)
		}

		fun newFireCatapultBoulder(player: ZPlayerName, zoneOptions: List<Int>): ZMove {
			return ZMove(
				type = ZMoveType.CATAPULT_FIRE_BOULDER,
				character = player,
				list = zoneOptions
			)
		}


		fun newFireBallistaBolt(player: ZPlayerName, directionOptions: List<ZDir>): ZMove {
			return ZMove(
				type = ZMoveType.BALLISTA_FIRE_BOLT,
				character = player,
				list = directionOptions
			)
		}

		fun newFamiliarMove(player: ZPlayerName, familiar: ZFamiliar): ZMove {
			return ZMove(ZMoveType.FAMILIAR_MOVE, character = player, familiar = familiar.type)
		}

		init {
			addAllFields(ZMove::class.java)
		}
	}

	constructor(copy: ZMove, singleListElement: Any, text: String) : this(copy.type, copy.integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action, text)
	constructor(copy: ZMove, singleListElement: Any, integer: Int, text: String? = null) : this(copy.type, integer, copy.character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action, text)
	constructor(copy: ZMove, singleListElement: Any, character: ZPlayerName?, text: String) : this(copy.type, copy.integer, character, copy.equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action, text)
	constructor(copy: ZMove, singleListElement: Any, equipment: ZEquipment<*>?, text: String) : this(copy.type, copy.integer, copy.character, equipment, copy.fromSlot, copy.toSlot, listOf(singleListElement), copy.skill, copy.action, text)

	override fun equals(o: Any?): Boolean {
		if (this === o) return true
		if (o == null) return false
		if (o is ZMoveType) return type === o
		if (o !is ZMove) return false
		val zMove = o
		return (type === zMove.type && cc.lib.utils.isEqual(equipment, zMove.equipment)
			&& cc.lib.utils.isEqual(integer, zMove.integer)
			&& character === zMove.character
			&& fromSlot === zMove.fromSlot
			&& toSlot === zMove.toSlot
			&& skill === zMove.skill)
	}

	override fun hashCode(): Int {
		return type.hashCode()
	}

	override fun getTooltipText(): String? = equipment?.getTooltipText()
		?: skill?.getTooltipText()
		?: type.toolTipText
		?: character?.getTooltipText()

	override fun getLabel(): String {
		if (text != null)
			return text
		var label = type.getLabel()
		equipment?.let {
			label += " ${it.getLabel()}"
		}
		if (fromSlot != null) {
			label += " from ${fromSlot.getLabel()}"
		}
		if (toSlot != null)
			label += " to ${toSlot.getLabel()}"
		if (type == ZMoveType.ORGANIZE_TRADE) {
			character?.name?.let {
				label += " with $it"
			}
		}
		if (type == ZMoveType.PICKUP_ITEM && list?.size == 1) {
			label = "Pickup ${list[0]}"
		}
		if (type == ZMoveType.ENCHANT && list?.size == 1) {
			label = list[0].toString()
		}
		return label
	}

	override fun toString(): String {
		return super.toString()
	}
}