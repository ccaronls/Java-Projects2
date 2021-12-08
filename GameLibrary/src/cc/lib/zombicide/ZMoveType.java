package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.ui.IButton;

@Keep
public enum ZMoveType implements IButton {
    END_TURN("Expend all remaining actions and end turn"),
    INVENTORY("View inventory. The first change of inventory cost an action. You can perform as many subsequent inventory actions for free for the rest of the round."), // equip things, drop things, etc.
    TRADE("Like inventory the first trade costs an action and subsequent trades are free for the rest of the round. You can only trade with players in your zone and if the zone is free of zombies."),
    WALK("Walk to a different zone at the cost of 1 action."),
    JUMP("Jump to 2 zones line of sight from current position for free"),
    CHARGE("Move up to 2 zones to a zone occupied by at least one zombie for free as often as you like."),
    WALK_DIR(null),
    SWITCH_ACTIVE_CHARACTER(null),
    USE_LEFT_HAND(null),
    USE_RIGHT_HAND(null),
    MELEE_ATTACK("Use an equipped melee weapon at cost of 1 action."),
    RANGED_ATTACK("Use an equipped range weapon at cost pf 1 action."),
    MAGIC_ATTACK("Use an equipped spell at cost of 1 action"),
    THROW_ITEM("Throw an equipped item after choosing target zone."), // torch / dragon bile
    RELOAD("Reload an equipped range weapon that requires it."),
    OPERATE_DOOR("Spend an action to open a door. Your best option for opening a door will be chosen automatically."),
    SEARCH("Spend an action to search area for loot."),
    CONSUME("Spend an action to consume an item from you backpack."),
    EQUIP(null),
    KEEP(null),
    UNEQUIP(null),
    GIVE(null),
    TAKE(null),
    DISPOSE("After disposing an item it is returned to the deck."),
    TAKE_OBJECTIVE("Zones that contain an 'X' are objectives. Take an objective to claim its EXP and whatever else might be associated with it like keys ot special loot."),
    // drop or collect items from vault
    DROP_ITEM("Dropping an item in the vault allows anyone to pick it up later."),
    PICKUP_ITEM("Take an item from the vault into your inventory or open slot."),
    MAKE_NOISE("Spend an action to draw zombies toward you. Zombies that have line of sight to a different player will not be affected by make noise."),
    SHOVE("Push all zombies in your zone to a neighboring zone."),
    REROLL(null),
    KEEP_ROLL(null),
    ENCHANT("Spend an action to cast a spell on a coplayer who is line of sight to yourself."),
    BORN_LEADER("You can give one of your own actions to another player of your choice."),
    BLOODLUST_MELEE("Spend an action to move up to 2 zones and perform melee."),
    BLOODLUST_RANGED("Spend an action to move up to 2 zones and perform ranged."),
    BLOODLUST_MAGIC("Spend an action to move up to 2 spaces and perform magic."),
    IGNITE("Ignite a Dragon Bile within range")
    ;

    final String toolTipText;

    ZMoveType(String toolTipText) {
        this.toolTipText = toolTipText;
    }

    @Override
    public String getLabel() {
        return Utils.toPrettyString(name());
    }

    @Override
    public String getTooltipText() {
        return toolTipText;
    }
}
