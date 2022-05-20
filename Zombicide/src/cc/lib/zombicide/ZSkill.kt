package cc.lib.zombicide

import cc.lib.annotation.Keep

import cc.lib.ui.IButton
import cc.lib.utils.prettify
import cc.lib.zombicide.ZCharacter
import cc.lib.zombicide.ZWeapon

@Keep
enum class ZSkill(val description: String) : IButton {
    Plus1_Action("The Survivor has an extra Action he may use as he pleases."),
    Plus1_Damage_Melee("The Survivor gets a +1 Damage bonus with Melee weapons.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                stat.damagePerHit++
            }
        }
    },
    Plus1_Damage_Ranged("The Survivor gets a +1 Damage bonus with Ranged weapons.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.RANGED) {
                stat.damagePerHit++
            }
        }
    },
    Plus1_Damage_Magic("The Survivor gets a +1 Damage bonus with Magic weapons.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MAGIC) {
                stat.damagePerHit++
            }
        }
    },
    Plus1_Damage_DualWielding("The survivor get +1 damage when dual wielding a melee weapon.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE && character.isDualWielding) {
                stat.damagePerHit++
            }
        }
    },  //Plus1_Damage_with("[Equipment] – The Survivor gets a +1 Damage bonus with the specified Equipment."),
    Plus1_to_dice_roll_Combat("The Survivor adds 1 to the result of each die he rolls on a Combat Action (Melee, Ranged or Magic). The maximum result is always 6.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            stat.dieRollToHit--
        }
    },
    Plus1_to_dice_roll_Magic("The Survivor adds 1 to the result of each die he rolls on a Magic Action. The maximum result is always 6.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MAGIC) {
                stat.dieRollToHit--
            }
        }
    },
    Plus1_to_dice_roll_Melee("The Survivor adds 1 to the result of each die he rolls in Melee Actions. The maximum result is always 6.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                stat.dieRollToHit--
            }
        }
    },
    Plus1_to_dice_roll_Ranged("The Survivor adds 1 to the result of each die he rolls in Ranged Actions. The maximum result is always 6.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.RANGED) {
                stat.dieRollToHit--
            }
        }
    },
    Plus1_die_Combat("The Survivor’s weapons and Combat spells roll an extra die in Combat (Melee, Ranged or Magic). Dual weapons and spells gain a die each, for a total of +2 dice per Dual Combat Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            stat.numDice++
        }
    },
    Plus1_die_Magic("The Survivor’s Combat spells roll an extra die for Magic Actions. Dual Combat spells gain a die each, for a total of +2 dice per Dual Magic Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MAGIC) {
                stat.numDice++
            }
        }
    },
    Plus1_die_Melee("The Survivor’s Melee weapons roll an extra die for Melee Actions. Dual Melee weapons gain a die each, for a total of +2 dice per Dual Melee Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                stat.numDice++
            }
        }
    },
    Plus1_die_Ranged("The Survivor’s Ranged weapons roll an extra die for Ranged Actions. Dual Ranged weapons gain a die each, for a total of +2 dice per Dual Ranged Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.RANGED) {
                stat.numDice++
            }
        }
    },
    Plus1_free_Combat_Action("The Survivor has one extra free Combat Action. This Action may only be used for Melee, Ranged or Magic Actions.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            when (type) {
                ZActionType.MAGIC, ZActionType.RANGED, ZActionType.MELEE -> return 1
            }
            return super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            Plus1_free_Melee_Action.addSpecialMoves(game, character, moves)
            Plus1_free_Ranged_Action.addSpecialMoves(game, character, moves)
            Plus1_free_Magic_Action.addSpecialMoves(game, character, moves)
        }
    },
    Plus1_free_Enchantment_Action("The Survivor has one extra free Enchantment Action. This Action may only be used for Enchantment Actions.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.ENCHANTMENT) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                val spells = character.spells
                if (spells.size > 0) {
                    moves.add(ZMove.newEnchantMove(spells))
                }
            }
        }
    },
    Plus1_free_Magic_Action("The Survivor has one extra free Magic Action. This Action may only be used for Magic Actions.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.MAGIC) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                val magic = character.magicWeapons
                if (magic.size > 0) {
                    moves.add(ZMove.newMagicAttackMove(magic))
                }
            }
        }
    },
    Plus1_free_Melee_Action("The Survivor has one extra free Melee Action. This Action may only be used for a Melee Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.MELEE) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                val melee = character.meleeWeapons
                if (melee.size > 0) {
                    moves.add(ZMove.newMeleeAttackMove(melee))
                }
            }
        }
    },
    Plus1_free_Move_Action("The Survivor has one extra free Move Action. This Action may only be used as a Move Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.MOVE) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                game.addWalkOptions(character, moves, null)
            }
        }
    },
    Plus1_free_Ranged_Action("The Survivor has one extra free Ranged Action. This Action may only be used as a Ranged Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.RANGED) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                val ranged = character.rangedWeapons
                if (ranged.size > 0) {
                    moves.add(ZMove.newRangedAttackMove(ranged))
                }
            }
        }
    },
    Plus1_free_Search_Action("The Survivor has one extra free Search Action. This Action may only be used to Search, and the Survivor can still only Search once per Turn.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.SEARCH) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Plus1_max_Range("The Survivor’s Ranged weapons and Combat spells’ maximum Range is increased by 1.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            when (actionType) {
                ZActionType.RANGED, ZActionType.MAGIC -> stat.maxRange++
            }
        }
    },
    Plus1_Zone_per_Move("The Survivor can move through one extra Zone each time he performs a Move Action. This Skill stacks with other effects benefiting Move Actions. Entering a Zone containing Zombies ends the Survivor’s Move Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.MOVE) {
                if (game.board.getNumZombiesInZone(character.occupiedZone) == 0) {
                    if (character.zonesMoved % 2 == 0) 1 else -1
                } else 1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Plus1_die_Melee_Weapon("Gain +1 die with another equipped melee weapon") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            // if another melee weapon equipped then +1 die
            if (actionType === ZActionType.MELEE && character.weapons.count { it.isMelee } > 1) {
                stat.numDice++
            }
        }
    },
    Two_Zones_per_Move_Action("When the Survivor spends one Action to Move, he can move one or two Zones instead of one. Entering a Zone containing Zombies ends the Survivor’s Move Action."),
    Ambidextrous("The Survivor treats all Combat spells, Melee and Ranged weapons as if they had the Dual symbol."),
    Barbarian("When resolving a Melee Action, the Survivor may substitute the Dice number of the Melee weapon(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Melee, still apply.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                val num = game.board.getNumZombiesInZone(character.occupiedZone)
                stat.numDice = Math.max(num, stat.numDice)
            }
        }
    },
    Blitz("Each time the Survivor kills the last Zombie in a Zone, he gets 1 free Move Action to use immediately."),
    Bloodlust("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Combat Action (Melee, Ranged or Magic).") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn > 0) {
                game.board.getAccessableZones(character.occupiedZone, 1, 2, ZActionType.MOVE)
	                .filter { game.board.getNumZombiesInZone(it) > 0 }
	                .takeIf { it.isNotEmpty()}?.let { zones ->
	                    if (character.meleeWeapons.isNotEmpty()) moves.add(ZMove.newBloodlustMeleeMove(zones, this))
	                    if (character.rangedWeapons.isNotEmpty()) moves.add(ZMove.newBloodlustRangedMove(zones, this))
	                    if (character.magicWeapons.isNotEmpty()) moves.add(ZMove.newBloodlustMagicMove(zones, this))
	                }
            }
        }
    },
    Bloodlust_Magic("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Magic Action, to use immediately.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn > 0 && character.magicWeapons.isNotEmpty()) {
                game.board.getAccessableZones(character.occupiedZone, 1, 2, ZActionType.MOVE)
	                .filter { game.board.getNumZombiesInZone(it) > 0 }
	                .takeIf { it.isNotEmpty() }?.let { zones ->
		                moves.add(ZMove.newBloodlustMagicMove(zones, this))
	                }
            }
        }
    },
    Bloodlust_Melee("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Melee Action, to use immediately.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn > 0 && character.meleeWeapons.size > 0) {
	            game.board.getAccessableZones(character.occupiedZone, 1, 2, ZActionType.MOVE)
		            .filter { game.board.getNumZombiesInZone(it) > 0 }
		            .takeIf { it.isNotEmpty() }?.let { zones ->
			            moves.add(ZMove.newBloodlustMeleeMove(zones, this))
		            }
            }
        }
    },
    Bloodlust_Ranged("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Ranged Action, to use immediately.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn > 0 && character.rangedWeapons.size > 0) {
	            game.board.getAccessableZones(character.occupiedZone, 1, 2, ZActionType.MOVE)
		            .filter { game.board.getNumZombiesInZone(it) > 0 }
		            .takeIf { it.isNotEmpty() }?.let { zones ->
			            moves.add(ZMove.newBloodlustRangedMove(zones, this))
		            }
            }
        }
    },
    Born_leader("During the Survivor’s Turn, he may give one free Action to another Survivor to use as he pleases. This Action must be used during the recipient’s next Turn or it is lost.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn > 0) {
                game.board.getAllCharacters().filter {
                    it != character && it.isAlive
                }.takeIf { list ->
                    list.isNotEmpty()
                }?.map { it.type }?.let { options ->
                    moves.add(ZMove.newBornLeaderMove(options))
                }
            }
        }
    },
    Break_in("In order to open doors, the Survivor rolls no dice, and needs no equipment (but still spends an Action to do so). He does’nt make Noise while using this Skill. However, other prerequisites still apply (such as taking a designated Objective before a door can be opened). Moreover, the Survivor gains one extra free Action that can only be used to open doors.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.OPEN_DOOR) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Charge("The Survivor can use this Skill for free, as often as he pleases, during each of his Turns: He moves up to two Zones to a Zone containing at least one Zombie. Normal Movement rules still apply. Entering a Zone containing Zombies ends the Survivor’s Move Action.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            var zones = game.board.getAccessableZones(character.occupiedZone, 1, 2, ZActionType.MOVE)
            zones.filter { game.board.getNumZombiesInZone(it) > 0 }
	            .takeIf { it.isNotEmpty() }
	            ?.let { zones ->
	            moves.add(ZMove.newChargeMove(zones))
            }
        }
    },
    Collector("The Survivor gains double the experience each time he kills a Zombie of any type."),
    Collector_Walker("The Survivor gains double the experience each time he kills a Walker."),
    Collector_Runner("The Survivor gains double the experience each time he kills a Runner."),
    Collector_Fatty("The Survivor gains double the experience each time he kills a Fatty."),
    Collector_Abomination("The Survivor gains double the experience each time he kills a Abomination."),
    Collector_Necromancer("The Survivor gains double the experience each time he kills a Necromancer."),
    Destiny("The Survivor can use this Skill once per Turn when he reveals an Equipment card he drew. You can ignore and discard that card, then draw another Equipment card."),  //Dragon_Aura("Gain +4 Armor till end of round."),
    Inventory("User can make modification to their inventory or, if occupied zone is free from zombies, trade with others in their same zone for no cost") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                if (!moves.contains(ZMoveType.INVENTORY)) moves.add(ZMove.newInventoryMove())
                game.addTradeOptions(character, moves)
            }
        }

        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.INVENTORY) {
                -1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Free_reload("The Survivor reloads reloadable weapons (Hand Crossbows, Orcish Crossbow, etc.) for free. ") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            if (!weapon.isLoaded) {
                game.addLogMessage("Free Reload!")
                weapon.reload()
            }
        }

        override fun onAcquired(game: ZGame, c: ZCharacter) {
            // reload all of the players wielded weapons
            for (w in c.weapons) {
                if (!w.isLoaded) {
                    game.addLogMessage("Weapon " + w.type.label + " is reloaded!")
                    w.reload()
                }
            }
        }
    },
    Frenzy_Combat("All weapons and Combat spells the Survivor carries gain +1 die per Wound the Survivor suffers. Dual weapons gain a die each, for a total of +2 dice per Wound and per Dual Combat Action (Melee, Ranged or Magic).") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            stat.numDice += character.woundBar
        }
    },
    Frenzy_Magic("Combat spells the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Combat spells gain a die each, for a total of +2 dice per Wound and per Dual Magic Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MAGIC) {
                stat.numDice += character.woundBar
            }
        }
    },
    Frenzy_Melee("Melee weapons the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Melee weapons gain a die each, for a total of +2 dice per Wound and per Dual Melee Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                stat.numDice += character.woundBar
            }
        }
    },
    Frenzy_Ranged("Ranged weapons the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Ranged weapons gain a die each, for a total of +2 dice per Wound and per Dual Ranged Action.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.RANGED) {
                stat.numDice += character.woundBar
            }
        }
    },
    Healing("At end of turn you have a wound healed.") {
        override fun onEndOfTurn(game: ZGame, c: ZCharacter): Boolean {
            if (c.heal(game, 1)) {
                game.addLogMessage(c.name() + " has a wound healed.")
                game.onCharacterHealed(c.type, 1)
                return true
            }
            return false
        }
    },
    Hit_and_run("The Survivor can use this Skill for free, just after he resolved a Magic, Melee or Ranged Action resulting in at least a Zombie kill. He can then resolve a free Move Action. The Survivor does not spend extra Actions to perform this free Move Action if Zombies are standing in his Zone."),
    Hold_your_nose("This Skill can be used once per Turn. The Survivor gets a free Search Action in the Zone if he has eliminated a Zombie (even in a Vault or a street Zone) the same Game Round. This Action may only be used to Search, and the Survivor can still only Search once per Turn."),
    Ironclad("The Survivor ignores all Wounds coming from Zombies of any type"),
    Ironclad_Walker("The Survivor ignores all Wounds coming from Walkers"),
    Ironclad_Runner("The Survivor ignores all Wounds coming from Runners"),
    Ironclad_Fatty("The Survivor ignores all Wounds coming from Fatties"),
    Ironclad_Abomination("The Survivor ignores all Wounds coming from Abominations"),
    Iron_hide("The Survivor can make Armor rolls with a 5+ Armor value, even when he does not wear an armor on his Body slot. Wearing an armor, the Survivor adds 1 to the result of each die he rolls for Armor rolls. The maximum result is always 6.") {
        override fun getArmorRating(type: ZZombieType?): Int {
            return 5
        }
    },
    Steel_hide("The Survivor can make Armor rolls with a 4+ Armor value, even when he does not wear an armor on his Body slot. Wearing an armor, the Survivor adds 1 to the result of each die he rolls for Armor rolls. The maximum result is always 6.") {
        override fun getArmorRating(type: ZZombieType?): Int {
            return 4
        }
    },
    Iron_rain("When resolving a Ranged Action, the Survivor may substitute the Dice number of the Ranged weapon(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Ranged, still apply. ") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.RANGED) {
                val numInZone = game.board.getNumZombiesInZone(targetZone)
                if (numInZone > stat.numDice) {
                    game.addLogMessage(String.format("%s applied Iron Rain!", character.label))
                    game.onIronRain(character.type, targetZone)
                    stat.numDice = numInZone
                }
            }
        }
    },  //    Is_that_all_you_got("You can use this Skill any time the Survivor is about to get Wounds. Discard one Equipment card in your Survivor’s inventory for each Wound he’s about to receive. Negate a Wound per discarded Equipment card."),
    Jump("The Survivor can use this Skill once during each Activation. The Survivor spends one Action: He moves two Zones into a Zone to which he has Line of Sight. Movement related Skills (like +1 Zone per Move Action or Slippery) are ignored, but Movement penalties (like having Zombies in the starting Zone) apply. Ignore everything in the intervening Zone.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            val zones = game.board.getAccessableZones(character.occupiedZone, 2, 2, ZActionType.MOVE)
            if (zones.size > 0) moves.add(ZMove.newJumpMove(zones))
        }
    },
    Lifesaver("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone containing at least one Zombie at Range 1 from your Survivor. Choose Survivors in the selected Zone to be dragged to your Survivor’s Zone without penalty. This is not a Move Action. A Survivor can decline the rescue and stay in the selected Zone if his controller chooses. Both Zones need to share a clear path. A Survivor can’t cross closed doors or walls, and can’t be extracted into or out of a Vault."),
    Lock_it_down("At the cost of one Action, the Survivor can close an open door in his Zone. Opening or destroying it again later does not trigger a new Zombie Spawn.") {
        override fun canCloseDoors(): Boolean {
            return true
        }
    },
    Loud("Once during each of his Turns, the Survivor can make a huge amount of noise! Until this Survivor’s next Turn, the Zone he used this Skill in is considered to have the highest number of Noise tokens on the entire board. If different Survivors have this Skill, only the last one who used it applies the effects."),
    Low_profile("The Survivor can’t get hit by Survivors’ Magic and Ranged Actions. Ignore him when casting a Combat spell or shooting in the Zone he stands in. Game effects that kill everything in the targeted Zone, like Dragon Fire, still kill him, though.") {
        override fun avoidsReceivingFriendlyFire(): Boolean {
            return true
        }
    },
    Lucky("The Survivor can re-roll once all the dice for each Action (or Armor roll) he takes. The new result takes the place of the previous one. This Skill stacks with the effects of other Skills and Equipment that allows re-rolls."),
    Mana_rain("When resolving a Magic Action, the Survivor may substitute the Dice number of the Combat spell(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Magic, still apply."),
    Marksman("The Survivor may freely choose the targets of all his Magic and Ranged Actions. Misses don’t hit Survivors.") {
        override fun avoidsInflictingFriendlyFire(): Boolean {
            return true
        }

        override fun useMarksmanForSorting(playerZone: Int, targetZone: Int): Boolean {
            return true
        }
    },
    Matching_set("When a Survivor performs a Search Action and draws an Equipment card with the Dual symbol, he can immediately take a second card of the same type from the Equipment deck. Shuffle the deck afterward."),
    Point_blank("The Survivor can resolve Ranged and Magic Actions in his own Zone, no matter the minimum Range. When resolving a Magic or Ranged Action at Range 0, the Survivor freely chooses the targets and can kill any type of Zombies. His Combat spells and Ranged weapons still need to inflict enough Damage to kill his targets. Misses don’t hit Survivors.") {
        override fun avoidsInflictingFriendlyFire(): Boolean {
            return true
        }

        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            when (actionType) {
                ZActionType.RANGED, ZActionType.MAGIC -> stat.minRange = 0
            }
        }

        override fun useMarksmanForSorting(playerZone: Int, targetZone: Int): Boolean {
            return playerZone == targetZone
        }
    },
    Reaper_Combat("Use this Skill when assigning hits while resolving a Combat Action (Melee, Ranged or Magic). One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie.") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            val zombiesLeftInZone = game.board.getZombiesInZone(targetZone).toMutableList()
            for (zombieKilled in destroyedZombies) {
                val it = zombiesLeftInZone.iterator()
                while (it.hasNext()) {
                    val z = it.next()
                    if (z.type === zombieKilled.type) {
                        game.performSkillKill(c, this, z, stat.attackType)
                        it.remove()
                        break
                    }
                }
            }
        }
    },
    Reaper_Magic("Use this Skill when assigning hits while resolving a Magic Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie.") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            if (actionType !== ZActionType.MAGIC) return
            val zombiesLeftInZone = game.board.getZombiesInZone(targetZone).toMutableList()
            for (zombieKilled in destroyedZombies) {
                val it = zombiesLeftInZone.iterator()
                while (it.hasNext()) {
                    val z = it.next()
                    if (z.type === zombieKilled.type) {
                        game.performSkillKill(c, this, z, stat.attackType)
                        it.remove()
                        break
                    }
                }
            }
        }
    },
    Reaper_Melee("Use this Skill when assigning hits while resolving a Melee Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie.") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            if (actionType !== ZActionType.MELEE) return
            val zombiesLeftInZone = game.board.getZombiesInZone(targetZone).toMutableList()
            for (zombieKilled in destroyedZombies) {
                val it = zombiesLeftInZone.iterator()
                while (it.hasNext()) {
                    val z = it.next()
                    if (z.type === zombieKilled.type) {
                        game.performSkillKill(c, this, z, stat.attackType)
                        it.remove()
                        break
                    }
                }
            }
        }
    },
    Reaper_Ranged("Use this Skill when assigning hits while resolving a Ranged Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie.") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            if (actionType !== ZActionType.RANGED) return
            val zombiesLeftInZone = game.board.getZombiesInZone(targetZone).toMutableList()
            for (zombieKilled in destroyedZombies) {
                val it = zombiesLeftInZone.iterator()
                while (it.hasNext()) {
                    val z = it.next()
                    if (z.type === zombieKilled.type) {
                        game.performSkillKill(c, this, z, stat.attackType)
                        it.remove()
                        break
                    }
                }
            }
        }
    },
    Regeneration("At the end of each Game Round, remove all Wounds the Survivor received. Regeneration does’nt work if the Survivor has been eliminated."),
    Roll_6_plus1_die_Combat("You may roll an additional die for each '6' rolled on any Combat Action (Melee, Ranged or Magic). Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls (the Plenty Of Arrows Equipment card, for example) must be used before rolling any additional dice for this Skill.") {
        override fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
            when (stat.actionType) {
                ZActionType.MAGIC, ZActionType.MELEE, ZActionType.RANGED -> {
                    game.onRollSixApplied(c.type, this)
                    return true
                }
            }
            return false
        }
    },
    Roll_6_plus1_die_Magic("You may roll an additional die for each '6' rolled on a Magic Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls must be used before rolling any additional dice for this Skill.") {
        override fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
            if (stat.actionType === ZActionType.MAGIC) {
                game.onRollSixApplied(c.type, this)
                return true
            }
            return false
        }
    },
    Roll_6_plus1_die_Melee("You may roll an additional die for each '6' rolled on a Melee Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls must be used before rolling any additional dice for this Skill.") {
        override fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
            if (stat.actionType === ZActionType.MELEE) {
                game.onRollSixApplied(c.type, this)
                return true
            }
            return false
        }
    },
    Roll_6_plus1_die_Ranged("You may roll an additional die for each '6' rolled on a Ranged Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls (the Plenty Of Arrows Equipment card, for example) must be used before rolling any additional dice for this Skill.") {
        override fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
            if (stat.actionType === ZActionType.RANGED) {
                game.onRollSixApplied(c.type, this)
                return true
            }
            return false
        }
    },
    Invisible("At the end of his Turn, if the Survivor has not resolved a Combat Action (Melee, Ranged or Magic) and not produced a Noise token, place a Rotten token next to his base. As long as he has this token, he is totally ignored by all Zombies and is not considered a Noise token. Zombies don’t attack him and will even walk past him. The Survivor loses his Rotten token if he resolves any kind of Combat Action (Melee, Ranged or Magic) or makes noise. Even with the Rotten token, the Survivor still has to spend extra Actions to move out of a Zone crowded with Zombies."),
    Scavenger("The Survivor can Search in any Zone. This includes street Zones, Vault Zones, etc."),
    Search_plus1_card("Draw an extra card when Searching with the Survivor."),
    Shove("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone at Range 1 from your Survivor. All Zombies standing in your Survivor’s Zone are pushed to the selected Zone. This is not a Movement. Both Zones need to share a clear path. A Zombie can’t cross closed doors, ramparts (see the Wulfsburg expansion) or walls, but can be shoved in or out of a Vault.") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            // if zombies stand in zone with character they can be shoved away
            if (game.board.getNumZombiesInZone(character.occupiedZone) > 0) {
                val shovable = game.board.getAccessableZones(character.occupiedZone, 1, 1, ZActionType.MOVE)
                if (shovable.size > 0) {
                    moves.add(ZMove.newShoveMove(shovable))
                }
            }
        }

        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.SHOVE) {
                1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Slippery("The Survivor does not spend extra Actions when he performs a Move Action out of a Zone containing Zombies. Entering a Zone containing Zombies ends the Survivor’s Move Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            return if (type === ZActionType.MOVE && game.board.getNumZombiesInZone(character.priorZone) > 0) {
                -1
            } else super.modifyActionsRemaining(character, type, game)
        }
    },
    Spellbook("All Combat spells and Enchantments in the Survivor’s Inventory are considered equipped in Hand. With this Skill, a Survivor could effectively be considered as having several Combat spells and Enchantments cards equipped in Hand. For obvious reasons, he can only use two identical dual Combat Spells at any given time. Choose any combination of two before resolving Actions or rolls involving the Survivor."),
    Spellcaster("The Survivor has one extra free Action. This Action may only be used for a Magic Action or an Enchantment Action.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            when (type) {
                ZActionType.MAGIC, ZActionType.ENCHANTMENT -> return 1
            }
            return super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0) {
                val spells = character.spells
                if (spells.size > 0) {
                    moves.add(ZMove.newEnchantMove(spells))
                }
                val weapons = character.magicWeapons
                if (weapons.size > 0) {
                    moves.add(ZMove.newMagicAttackMove(weapons))
                }
            }
        }
    },
    Speed("Can move up to 2 unoccupied by zombie zones for free.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            if (type === ZActionType.MOVE) {
                if (game.board.getNumZombiesInZone(character.occupiedZone) > 0) return super.modifyActionsRemaining(character, type, game)
                return if (character.zonesMoved > 1) 1 else -1
            }
            return super.modifyActionsRemaining(character, type, game)
        }
    },
    Sprint("The first 3 moves are free unless a zone with zombies is entered.") {
        override fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
            if (type === ZActionType.MOVE) {
                if (game.board.getNumZombiesInZone(character.occupiedZone) == 0) {
                    return if (character.zonesMoved > 2) 1 else -1
                }
            }
            return super.modifyActionsRemaining(character, type, game)
        }

        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            if (character.actionsLeftThisTurn == 0 && character.zonesMoved < 3 && game.board.getNumZombiesInZone(character.occupiedZone) == 0) {
                game.addWalkOptions(character, moves, null)
            }
        }
    },
    Super_strength("Consider the Damage value of Melee weapons used by the Survivor to be 3.") {
        override fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {
            if (actionType === ZActionType.MELEE) {
                stat.damagePerHit = 3
            }
        }
    },  //Starts_with_a("[Equipment] – The Survivor begins the game with the indicated Equipment; its card is automatically assigned to him during Setup."),

    //Starts_with_Healing("The Survivor begins the game with Healing Spell; its card is automatically assigned to him during Setup."),
    //Starts_with_GreatSword("The Survivor begins the game with Great Sword; its card is automatically assigned to him during Setup."),
    Steady_hand("The Survivor can ignore other Survivors of his choosing when missing with a Magic or Ranged Action. The Skill does not apply to game effects killing everything in the targeted Zone (such as a Dragon Fire, for example).") {
        override fun avoidsInflictingFriendlyFire(): Boolean {
            return true
        }
    },
    Swordmaster("The Survivor treats all Melee weapons as if they had the Dual symbol.") {
        override fun canTwoHand(w: ZWeapon): Boolean {
            return w.isMelee
        }
    },
    Tactician("The Survivor’s Turn can be resolved anytime during the Players’ Phase, before or after any other Survivor’s Turn. If several Survivors benefit from this Skill at the same time, choose their Turn order."),
    Taunt("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone your Survivor can see. All Zombies standing in the selected Zone immediately gain an extra Activation: They try to reach the taunting Survivor by any means available. Taunted Zombies ignore all other Survivors. They do not attack them and cross the Zone they stand in if needed to reach the taunting Survivor."),
    Tough("The Survivor ignores the first Wound he receives from a single Zombie every Zombies’ Phase."),
    Trick_shot("When the Survivor is equipped with Dual Combat spells or Ranged weapons, he can aim at different Zones with each spell/weapon in the same Action."),
    Zombie_link("The Survivor plays an extra Turn each time an Extra Activation card is drawn from the Zombie pile. He plays before the extra-activated Zombies. If several Survivors benefit from this Skill at the same time, choose their Turn order."),
    Roll_6_Plus1_Damage("If any one of the die rolled is a 6 then add 1 to the damage. Additional sixes to not increase beyond 1.") {
        override fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
            if (stat.damagePerHit < 3) {
                game.addLogMessage("+1 Damage Applied!")
                game.onRollSixApplied(c.type, this)
                stat.damagePerHit++
            }
            return false
        }
    },
    Hit_Heals("A successful hit results in healing of a single wound.") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            c.heal(game, destroyedZombies.size)
        }
    },
    Hit_4_Dragon_Fire("If an attack results in 4 successful hits then dragon fire in the target zone") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            if (hits >= 4) {
                game.performDragonFire(c, targetZone)
            }
        }
    },
    Ignite_Dragon_Fire("Can ignite dragon fire without torch at range 0-1") {
        override fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {
            game.board.getAccessableZones(character.occupiedZone, 1, 1, ZActionType.THROW_ITEM)
	            .filter { game.board.getZone(it).isDragonBile }
	            .takeIf { it.isNotEmpty() }
	            ?.let { ignitableZones ->
		            moves.add(ZMove.newIgniteMove(ignitableZones))
            }
        }
    },
    Auto_Reload("Auto reloads a ranged weapon at the end of turn") {
        override fun onEndOfTurn(game: ZGame, c: ZCharacter): Boolean {
            return super.onEndOfTurn(game, c)
        }
    },
    Two_For_One_Melee("Each Successful hit generates two hits in the target zone. Similar to reaper but without the condition the zombie type must be the same") {
        override fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {
            var hits = hits
            val zombiesLeftInZone = game.board.getZombiesInZone(targetZone)
	            .filter { z -> z.type.minDamageToDestroy <= stat.damagePerHit }
	            .toMutableList()
            while (zombiesLeftInZone.size > 0 && hits-- > 0) {
                val z = zombiesLeftInZone.removeAt(0)
                game.performSkillKill(c, this, z, stat.attackType)
            }
        }
    };

    override fun getTooltipText(): String {
        return description
    }

    override fun getLabel(): String {
        return prettify(name)
    }

    /**
     *
     * @param stat
     * @param actionType
     * @param character
     * @param game
     */
    open fun modifyStat(stat: ZWeaponStat, actionType: ZActionType, character: ZCharacter, game: ZGame, targetZone: Int) {}

    /**
     * Modify characters actions remaining based on the move. return true if applied
     * @param character
     * @param type
     * @param game
     * @return returns 0 when noting changed. 1 when action used and should be removed, -1 when used but should not be removed
     */
    open fun modifyActionsRemaining(character: ZCharacter, type: ZActionType, game: ZGame): Int {
        return 0
    }

    open fun onAcquired(game: ZGame, c: ZCharacter) {}

    /**
     *
     * @return
     */
    open fun canCloseDoors(): Boolean {
        return false
    }

    /**
     *
     * @param w
     * @return
     */
    open fun canTwoHand(w: ZWeapon): Boolean {
        return false
    }

    /**
     *
     * @param game
     * @param character
     * @param moves
     */
    open fun addSpecialMoves(game: ZGame, character: ZCharacter, moves: MutableList<ZMove>) {}

    /**
     * Return true if skill was applied
     *
     * @param game
     * @param c
     * @return
     */
    open fun onEndOfTurn(game: ZGame, c: ZCharacter): Boolean {
        return false
    }

    open fun avoidsInflictingFriendlyFire(): Boolean {
        return false
    }

    open fun avoidsReceivingFriendlyFire(): Boolean {
        return false
    }

    open fun getArmorRating(type: ZZombieType?): Int {
        return 0
    }

    open fun onAttack(game: ZGame, c: ZCharacter, weapon: ZWeapon, actionType: ZActionType, stat: ZWeaponStat, targetZone: Int, hits: Int, destroyedZombies: List<ZZombie>) {}

    /**
     * Return true to re-roll
     * Modify stat does not require returning true
     * @param stat
     * @return
     */
    open fun onSixRolled(game: ZGame, c: ZCharacter, stat: ZWeaponStat): Boolean {
        return false
    }

    open fun useMarksmanForSorting(playerZone: Int, targetZone: Int): Boolean {
        return false
    }
}