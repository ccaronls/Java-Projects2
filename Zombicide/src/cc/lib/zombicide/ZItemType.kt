package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZItemType(override val equipmentClass: ZEquipmentClass, val actionType: ZActionType, val slot: ZEquipSlotType, val description: String) : ZEquipmentType {
    TORCH(ZEquipmentClass.THROWABLE, ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Draw 2 cards when searching. Spend an action, discard, and select a dragon bile at range 0-1 to ignite. Resolve dragon Fire.") {
        override fun onThrown(game: ZGame, thrower: ZCharacter, targetZoneIdx: Int) {
            val zone = game.board.getZone(targetZoneIdx)
            game.onEquipmentThrown(thrower.type, ZIcon.TORCH, targetZoneIdx)
            if (!zone.isDragonBile) {
                game.addLogMessage("Throwing the Torch had no effect")
            } else {
                game.performDragonFire(thrower, zone.zoneIndex)
            }
        }
    },
    DRAGON_BILE(ZEquipmentClass.THROWABLE, ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Spend an action, discard and place a dragon bile token at range 0-1") {
        override fun onThrown(game: ZGame, thrower: ZCharacter, targetZoneIdx: Int) {
            game.addLogMessage(thrower.name() + " threw the dragon Bile!")
            game.onEquipmentThrown(thrower.type, ZIcon.DRAGON_BILE, targetZoneIdx)
            game.board.getZone(targetZoneIdx).isDragonBile = true
        }
    },
    WATER(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and heal 1 wound or gain 3 experience points if not wounded.") {
	    override fun consume(char: ZCharacter, game: ZGame) {
		    if (char.isWounded) {
				char.heal(game, 1)
		    } else {
				game.addExperience(char,3)
		    }
	    }
	},
    SALTED_MEAT(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and heal 1 wound or gain +1 Damage for remainder of turn if not wounded.") {
	    override fun consume(char: ZCharacter, game: ZGame) {
		    if (char.isWounded) {
			    char.heal(game, 1)
		    } else {
			    char.addAvailableSkill(ZSkill.Plus1_Damage_Combat)
		    }
	    }
    },
    APPLES(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and heal 1 wound or gain +1 Die Roll Combat for remainder of turn if not wounded.") {
	    override fun consume(char: ZCharacter, game: ZGame) {
		    if (char.isWounded) {
			    char.heal(game, 1)
		    } else {
			    char.addAvailableSkill(ZSkill.Plus1_die_Combat)
		    }
	    }
    },
    PLENTY_OF_ARROWS(ZEquipmentClass.BOW, ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bows. The new result takes place of old."),  // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS(ZEquipmentClass.CROSSBOW, ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bolts. The new result takes place of old."),
    BARRICADE(ZEquipmentClass.CONSUMABLE, ZActionType.BARRICADE_DOOR, ZEquipSlotType.HAND, "Close and barricade a door. Takes 3 turns to execute."),
	AHHHH(ZEquipmentClass.AHHHH, ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "Stop Searching and place a zombie in the room being searched."),
	;

    override fun create(): ZItem {
        return ZItem(this)
    }

    override fun isActionType(type: ZActionType): Boolean {
        return type === actionType
    }

	open fun consume(char : ZCharacter, game : ZGame) {
		throw NotImplementedError()
	}

    override fun getTooltipText(): String? {
        return description
    }
}