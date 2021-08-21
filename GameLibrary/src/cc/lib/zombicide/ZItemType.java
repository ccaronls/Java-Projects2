package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZItemType implements ZEquipmentType<ZItem> {
    AAHHHH(ZActionType.NOTHING, null, "Stop Searching and place a zombie in the room being searched."),
    TORCH(ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Draw 2 cards when searching. Spend an action, discard, and select a dragon bile at range 0-1 to ignite. Resolve dragon Fire."),
    DRAGON_BILE(ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Spend an action, discard and place a dragon bile token at range 0-1"),
    WATER(ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 1 experience point"),
    SALTED_MEAT(ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 2 experience point"),
    APPLES(ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 3 experience point"),
    PLENTY_OF_ARROWS(ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bows. The new result takes place of old."), // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS(ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bolts. The new result takes place of old."),
    ;

    ZItemType(ZActionType actionType, ZEquipSlotType slot, String description) {
        this.actionType = actionType;
        this.slot = slot;
        this.description = description;
    }

    final ZEquipSlotType slot;
    final ZActionType actionType;
    final String description;

    @Override
    public ZItem create() {
        return new ZItem(this);
    }

    @Override
    public ZActionType getActionType() {
        return actionType;
    }

    public int getExpWhenConsumed() {
        switch (this) {
            case SALTED_MEAT:
                return 2;
            case APPLES:
                return 3;
        }
        return 1;
    }

    @Override
    public String getTooltipText() {
        return description;
    }
}
