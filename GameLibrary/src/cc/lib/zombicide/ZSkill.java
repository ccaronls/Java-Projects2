package cc.lib.zombicide;

import java.util.List;

import cc.lib.ui.IButton;

public enum ZSkill implements IButton {
    Plus1_Action("The Survivor has an extra Action he may use as he pleases."),
    Plus1_Damage_Melee("The Survivor gets a +1 Damage bonus with Melee weapons.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                stat.damagePerHit++;
            }
        }
    },
    Plus1_Damage_Ranged("The Survivor gets a +1 Damage bonus with Ranged weapons.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.RANGED) {
                stat.damagePerHit++;
            }
        }
    },
    Plus1_Damage_Magic("The Survivor gets a +1 Damage bonus with Magic weapons.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MAGIC) {
                stat.damagePerHit++;
            }
        }
    },
    //Plus1_Damage_with("[Equipment] – The Survivor gets a +1 Damage bonus with the specified Equipment."),
    Plus1_to_dice_roll_Combat("The Survivor adds 1 to the result of each die he rolls on a Combat Action (Melee, Ranged or Magic). The maximum result is always 6.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            stat.dieRollToHit--;
        }
    },
    Plus1_to_dice_roll_Magic("The Survivor adds 1 to the result of each die he rolls on a Magic Action. The maximum result is always 6.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MAGIC) {
                stat.dieRollToHit--;
            }
        }
    },
    Plus1_to_dice_roll_Melee("The Survivor adds 1 to the result of each die he rolls in Melee Actions. The maximum result is always 6.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                stat.dieRollToHit--;
            }
        }
    },
    Plus1_to_dice_roll_Ranged("The Survivor adds 1 to the result of each die he rolls in Ranged Actions. The maximum result is always 6.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.RANGED) {
                stat.dieRollToHit--;
            }
        }
    },
    Plus1_die_Combat("The Survivor’s weapons and Combat spells roll an extra die in Combat (Melee, Ranged or Magic). Dual weapons and spells gain a die each, for a total of +2 dice per Dual Combat Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            stat.numDice++;
        }
    },
    Plus1_die_Magic("The Survivor’s Combat spells roll an extra die for Magic Actions. Dual Combat spells gain a die each, for a total of +2 dice per Dual Magic Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MAGIC) {
                stat.numDice++;
            }
        }
    },
    Plus1_die_Melee("The Survivor’s Melee weapons roll an extra die for Melee Actions. Dual Melee weapons gain a die each, for a total of +2 dice per Dual Melee Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                stat.numDice++;
            }
        }
    },
    Plus1_die_Ranged("The Survivor’s Ranged weapons roll an extra die for Ranged Actions. Dual Ranged weapons gain a die each, for a total of +2 dice per Dual Ranged Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.RANGED) {
                stat.numDice++;
            }
        }
    },
    Plus1_free_Combat_Action("The Survivor has one extra free Combat Action. This Action may only be used for Melee, Ranged or Magic Actions.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MAGIC:
                case RANGED:
                case MELEE:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Enchantment_Action("The Survivor has one extra free Enchantment Action. This Action may only be used for Enchantment Actions.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case ENCHANTMENT:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Magic_Action("The Survivor has one extra free Magic Action. This Action may only be used for Magic Actions.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MAGIC:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Melee_Action("The Survivor has one extra free Melee Action. This Action may only be used for a Melee Action.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MELEE:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Move_Action("The Survivor has one extra free Move Action. This Action may only be used as a Move Action.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MOVE:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Ranged_Action("The Survivor has one extra free Ranged Action. This Action may only be used as a Ranged Action.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case RANGED:
                    return true;
            }
            return false;
        }
    },
    Plus1_free_Search_Action("The Survivor has one extra free Search Action. This Action may only be used to Search, and the Survivor can still only Search once per Turn.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case SEARCH:
                    return true;
            }
            return false;
        }
    },
    Plus1_max_Range("The Survivor’s Ranged weapons and Combat spells’ maximum Range is increased by 1.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            stat.maxRange++;
        }
    },
    Plus1_Zone_per_Move("The Survivor can move through one extra Zone each time he performs a Move Action. This Skill stacks with other effects benefiting Move Actions. Entering a Zone containing Zombies ends the Survivor’s Move Action.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MOVE:
                    return game.board.getZombiesInZone(character.occupiedZone).size() == 0;
            }
            return false;
        }
    },
    Two_Zones_per_Move_Action("When the Survivor spends one Action to Move, he can move one or two Zones instead of one. Entering a Zone containing Zombies ends the Survivor’s Move Action."),
    Ambidextrous("The Survivor treats all Combat spells, Melee and Ranged weapons as if they had the Dual symbol."),
    Barbarian("When resolving a Melee Action, the Survivor may substitute the Dice number of the Melee weapon(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Melee, still apply.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                int num = game.board.getZombiesInZone(character.occupiedZone).size();
                stat.numDice = Math.max(num, stat.numDice);
            }
        }
    },
    Blitz("Each time the Survivor kills the last Zombie in a Zone, he gets 1 free Move Action to use immediately."),
    Bloodlust("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Combat Action (Melee, Ranged or Magic)."),
    Bloodlust_Magic("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Magic Action, to use immediately."),
    Bloodlust_Melee("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Melee Action, to use immediately."),
    Bloodlust_Ranged("Spend one Action with the Survivor: He Moves up to two Zones to a Zone containing at least one Zombie. He then gains one free Ranged Action, to use immediately."),
    Born_leader("During the Survivor’s Turn, he may give one free Action to another Survivor to use as he pleases. This Action must be used during the recipient’s next Turn or it is lost."),
    Break_in("In order to open doors, the Survivor rolls no dice, and needs no equipment (but still spends an Action to do so). He doesn’t make Noise while using this Skill. However, other prerequisites still apply (such as taking a designated Objective before a door can be opened). Moreover, the Survivor gains one extra free Action that can only be used to open doors.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            if (type == ZActionType.OPEN_DOOR) {
                return true;
            }
            return super.modifyActionsRemaining(character, type, game);
        }
    },
    Charge("The Survivor can use this Skill for free, as often as he pleases, during each of his Turns: He moves up to two Zones to a Zone containing at least one Zombie. Normal Movement rules still apply. Entering a Zone containing Zombies ends the Survivor’s Move Action."),
    Collector("The Survivor gains double the experience each time he kills a Zombie of any type."),
    Collector_Walker("The Survivor gains double the experience each time he kills a Walker."),
    Collector_Runner("The Survivor gains double the experience each time he kills a Runner."),
    Collector_Fatty("The Survivor gains double the experience each time he kills a Fatty."),
    Collector_Abomination("The Survivor gains double the experience each time he kills a Abomination."),
    Collector_Necromancer("The Survivor gains double the experience each time he kills a Necromancer."),
    Destiny("The Survivor can use this Skill once per Turn when he reveals an Equipment card he drew. You can ignore and discard that card, then draw another Equipment card."),
    Free_reload("The Survivor reloads reloadable weapons (Hand Crossbows, Orcish Crossbow, etc.) for free. "),
    Frenzy_Combat("All weapons and Combat spells the Survivor carries gain +1 die per Wound the Survivor suffers. Dual weapons gain a die each, for a total of +2 dice per Wound and per Dual Combat Action (Melee, Ranged or Magic).") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            stat.numDice += character.woundBar;
        }
    },
    Frenzy_Magic("Combat spells the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Combat spells gain a die each, for a total of +2 dice per Wound and per Dual Magic Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MAGIC) {
                stat.numDice += character.woundBar;
            }
        }
    },
    Frenzy_Melee("Melee weapons the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Melee weapons gain a die each, for a total of +2 dice per Wound and per Dual Melee Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                stat.numDice += character.woundBar;
            }
        }
    },
    Frenzy_Ranged("Ranged weapons the Survivor carries gain +1 die per Wound the Survivor suffers. Dual Ranged weapons gain a die each, for a total of +2 dice per Wound and per Dual Ranged Action.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.RANGED) {
                stat.numDice += character.woundBar;
            }
        }
    },
    Hit_and_run("The Survivor can use this Skill for free, just after he resolved a Magic, Melee or Ranged Action resulting in at least a Zombie kill. He can then resolve a free Move Action. The Survivor does not spend extra Actions to perform this free Move Action if Zombies are standing in his Zone."),
    Hold_your_nose("This Skill can be used once per Turn. The Survivor gets a free Search Action in the Zone if he has eliminated a Zombie (even in a Vault or a street Zone) the same Game Round. This Action may only be used to Search, and the Survivor can still only Search once per Turn."),
    Ironclad("The Survivor ignores all Wounds coming from Zombies of any type"),
    Ironclad_Walker("The Survivor ignores all Wounds coming from Walkers"),
    Ironclad_Runner("The Survivor ignores all Wounds coming from Runners"),
    Ironclad_Fatty("The Survivor ignores all Wounds coming from Fatties"),
    Ironclad_Abomination("The Survivor ignores all Wounds coming from Abominations"),
    Iron_hide("The Survivor can make Armor rolls with a 5+ Armor value, even when he does not wear an armor on his Body slot. Wearing an armor, the Survivor adds 1 to the result of each die he rolls for Armor rolls. The maximum result is always 6."),
    Iron_rain("When resolving a Ranged Action, the Survivor may substitute the Dice number of the Ranged weapon(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Ranged, still apply. Is that all you’ve got? – You can use this Skill any time the Survivor is about to get Wounds. Discard one Equipment card in your Survivor’s inventory for each Wound he’s about to receive. Negate a Wound per discarded Equipment card."),
    Jump("The Survivor can use this Skill once during each Activation. The Survivor spends one Action: He moves two Zones into a Zone to which he has Line of Sight. Movement related Skills (like +1 Zone per Move Action or Slippery) are ignored, but Movement penalties (like having Zombies in the starting Zone) apply. Ignore everything in the intervening Zone."),
    Lifesaver("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone containing at least one Zombie at Range 1 from your Survivor. Choose Survivors in the selected Zone to be dragged to your Survivor’s Zone without penalty. This is not a Move Action. A Survivor can decline the rescue and stay in the selected Zone if his controller chooses. Both Zones need to share a clear path. A Survivor can’t cross closed doors or walls, and can’t be extracted into or out of a Vault."),
    Lock_it_down("At the cost of one Action, the Survivor can close an open door in his Zone. Opening or destroying it again later does not trigger a new Zombie Spawn.") {
        @Override
        public boolean canCloseDoors() {
            return true;
        }
    },
    Loud("Once during each of his Turns, the Survivor can make a huge amount of noise! Until this Survivor’s next Turn, the Zone he used this Skill in is considered to have the highest number of Noise tokens on the entire board. If different Survivors have this Skill, only the last one who used it applies the effects."),
    Low_profile("The Survivor can’t get hit by Survivors’ Magic and Ranged Actions. Ignore him when casting a Combat spell or shooting in the Zone he stands in. Game effects that kill everything in the targeted Zone, like Dragon Fire, still kill him, though."),
    Lucky("The Survivor can re-roll once all the dice for each Action (or Armor roll) he takes. The new result takes the place of the previous one. This Skill stacks with the effects of other Skills and Equipment that allows re-rolls."),
    Mana_rain("When resolving a Magic Action, the Survivor may substitute the Dice number of the Combat spell(s) he uses with the number of Zombies standing in the targeted Zone. Skills affecting the dice value, like +1 die: Magic, still apply."),
    Marksman("The Survivor may freely choose the targets of all his Magic and Ranged Actions. Misses don’t hit Survivors. Matching set! – When a Survivor performs a Search Action and draws an Equipment card with the Dual symbol, he can immediately take a second card of the same type from the Equipment deck. Shuffle the deck afterward."),
    Point_blank("The Survivor can resolve Ranged and Magic Actions in his own Zone, no matter the minimum Range. When resolving a Magic or Ranged Action at Range 0, the Survivor freely chooses the targets and can kill any type of Zombies. His Combat spells and Ranged weapons still need to inflict enough Damage to kill his targets. Misses don’t hit Survivors."),
    Reaper_Combat("Use this Skill when assigning hits while resolving a Combat Action (Melee, Ranged or Magic). One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie."),
    Reaper_Magic("Use this Skill when assigning hits while resolving a Magic Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie."),
    Reaper_Melee("Use this Skill when assigning hits while resolving a Melee Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie."),
    Reaper_Ranged("Use this Skill when assigning hits while resolving a Ranged Action. One of these hits can freely kill an additional identical Zombie in the same Zone. Only a single additional Zombie can be killed per Action when using this Skill. The Survivor gains the experience for the additional Zombie."),
    Regeneration("At the end of each Game Round, remove all Wounds the Survivor received. Regeneration doesn’t work if the Survivor has been eliminated."),
    Roll_6_plus1_die_Combat("You may roll an additional die for each '6' rolled on any Combat Action (Melee, Ranged or Magic). Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls (the Plenty Of Arrows Equipment card, for example) must be used before rolling any additional dice for this Skill."),
    Roll_6_plus1_die_Magic("You may roll an additional die for each '6' rolled on a Magic Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow rerolls must be used before rolling any additional dice for this Skill."),
    Roll_6_plus1_die_Melee("You may roll an additional die for each '6' rolled on a Melee Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow rerolls must be used before rolling any additional dice for this Skill."),
    Roll_6_plus1_dies_Ranged("You may roll an additional die for each '6' rolled on a Ranged Action. Keep on rolling additional dice as long as you keep getting '6'. Game effects that allow re-rolls (the Plenty Of Arrows Equipment card, for example) must be used before rolling any additional dice for this Skill."),
    Rotten("At the end of his Turn, if the Survivor has not resolved a Combat Action (Melee, Ranged or Magic) and not produced a Noise token, place a Rotten token next to his base. As long as he has this token, he is totally ignored by all Zombies and is not considered a Noise token. Zombies don’t attack him and will even walk past him. The Survivor loses his Rotten token if he resolves any kind of Combat Action (Melee, Ranged or Magic) or makes noise. Even with the Rotten token, the Survivor still has to spend extra Actions to move out of a Zone crowded with Zombies."),
    Scavenger("The Survivor can Search in any Zone. This includes street Zones, Vault Zones, etc."),
    Search_plus1_card("Draw an extra card when Searching with the Survivor."),
    Shove("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone at Range 1 from your Survivor. All Zombies standing in your Survivor’s Zone are pushed to the selected Zone. This is not a Movement. Both Zones need to share a clear path. A Zombie can’t cross closed doors, ramparts (see the Wulfsburg expansion) or walls, but can be shoved in or out of a Vault.") {
        @Override
        public void addSpecialMoves(ZGame game, ZCharacter character, List<ZMove> moves) {
            // if zombies stand in zone with character they can be shoved away
            if (game.board.getZombiesInZone(character.getOccupiedZone()).size() > 0) {
                List<Integer> shovable = game.board.getAccessableZones(character.getOccupiedZone(), 1, ZActionType.MOVE);
                if (shovable.size() > 0) {
                    moves.add(ZMove.newShoveMove(shovable));
                }
            }
        }

        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            if (type ==  ZActionType.SHOVE) {
                return true;
            }
            return super.modifyActionsRemaining(character, type, game);
        }
    },
    Slippery("The Survivor does not spend extra Actions when he performs a Move Action out of a Zone containing Zombies. Entering a Zone containing Zombies ends the Survivor’s Move Action."),
    Spellbook("All Combat spells and Enchantments in the Survivor’s Inventory are considered equipped in Hand. With this Skill, a Survivor could effectively be considered as having several Combat spells and Enchantments cards equipped in Hand. For obvious reasons, he can only use two identical dual Combat Spells at any given time. Choose any combination of two before resolving Actions or rolls involving the Survivor."),
    Spellcaster("The Survivor has one extra free Action. This Action may only be used for a Magic Action or an Enchantment Action.") {
        @Override
        public boolean modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
            switch (type) {
                case MAGIC:
                case ENCHANTMENT:
                    return true;
            }
            return super.modifyActionsRemaining(character, type, game);
        }
    },
    Sprint("The Survivor can use this Skill once during each of his Turns. Spend one Move Action with the Survivor: He may move two or three Zones instead of one. Entering a Zone containing Zombies ends the Survivor’s Move Action."),
    Super_strength("Consider the Damage value of Melee weapons used by the Survivor to be 3.") {
        @Override
        public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {
            if (actionType == ZActionType.MELEE) {
                stat.damagePerHit = 3;
            }
        }
    },
    //Starts_with_a("[Equipment] – The Survivor begins the game with the indicated Equipment; its card is automatically assigned to him during Setup."),
    //Starts_with_Healing("The Survivor begins the game with Healing Spell; its card is automatically assigned to him during Setup."),
    //Starts_with_GreatSword("The Survivor begins the game with Great Sword; its card is automatically assigned to him during Setup."),
    Steady_hand("The Survivor can ignore other Survivors of his choosing when missing with a Magic or Ranged Action. The Skill does not apply to game effects killing everything in the targeted Zone (such as a Dragon Fire, for example)."),
    Swordmaster("The Survivor treats all Melee weapons as if they had the Dual symbol.") {
        @Override
        public boolean canTwoHand(ZWeapon w) {
            return w.isMelee();
        }
    },

    Tactician("The Survivor’s Turn can be resolved anytime during the Players’ Phase, before or after any other Survivor’s Turn. If several Survivors benefit from this Skill at the same time, choose their Turn order."),
    Taunt("The Survivor can use this Skill, for free, once during each of his Turns. Select a Zone your Survivor can see. All Zombies standing in the selected Zone immediately gain an extra Activation: They try to reach the taunting Survivor by any means available. Taunted Zombies ignore all other Survivors. They do not attack them and cross the Zone they stand in if needed to reach the taunting Survivor."),
    Tough("The Survivor ignores the first Wound he receives from a single Zombie every Zombies’ Phase."),
    Trick_shot("When the Survivor is equipped with Dual Combat spells or Ranged weapons, he can aim at different Zones with each spell/weapon in the same Action."),
    Zombie_link("The Survivor plays an extra Turn each time an Extra Activation card is drawn from the Zombie pile. He plays before the extra-activated Zombies. If several Survivors benefit from this Skill at the same time, choose their Turn order."),
    
    ;
    
    ZSkill(String description) {
        this.description = description;
    }
    
    public final String description;


    @Override
    public String getTooltipText() {
        return description;
    }

    @Override
    public String getLabel() {
        return name().replace('_', ' ');
    }

    /**
     *
     * @param stat
     * @param actionType
     * @param character
     * @param game
     */
    public void modifyStat(ZWeaponStat stat, ZActionType actionType, ZCharacter character, ZGame game) {

    }

    /**
     * Modify characters actions remaining based on the move. return true if applied
     * @param character
     * @param type
     * @param game
     * @return
     */
    public boolean  modifyActionsRemaining(ZCharacter character, ZActionType type, ZGame game) {
        return false;
    }

    /**
     *
     * @return
     */
    public boolean canCloseDoors() {
        return false;
    }

    /**
     *
     * @param w
     * @return
     */
    public boolean canTwoHand(ZWeapon w) { return false; }

    /**
     *
     * @param game
     * @param character
     * @param moves
     */
    public void addSpecialMoves(ZGame game, ZCharacter character, List<ZMove> moves) {}


}
