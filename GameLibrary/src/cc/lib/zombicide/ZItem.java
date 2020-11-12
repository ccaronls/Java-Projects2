package cc.lib.zombicide;

public enum ZItem implements ZEquipment {
    AAHHHH(false, null, "Stop Searching and place a zombie in the room being searched."),
    TORCH(false, ZEquipSlotType.HAND, "Draw 2 cards when searching. Spend an action, discard, and select a dragon bile at range 0-1 to ignite. Resolve dragon Fire."),
    DRAGON_BILE(true, ZEquipSlotType.HAND, "Spend an action, discard and place a dragon bile token at range 0-1"),
    WATER(true, ZEquipSlotType.BACKPACK, "Discard and gain 1 experience point"),
    SALTED_MEAT(true, ZEquipSlotType.BACKPACK, "Discard and gain 1 experience point"),
    APPLES(true, ZEquipSlotType.BACKPACK, "Discard and gain 1 experience point"),
    PLENTY_OF_ARROWS(false, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bows. The new result takes place of old."), // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS(false, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bolts. The new result takes place of old."),
    ;

    ZItem(boolean canConsume, ZEquipSlotType slot, String description) {
        this.canConsume = canConsume;
        this.slot = slot;
        this.description = description;
    }

    private final ZEquipSlotType slot;
    private final boolean canConsume;
    public final String description;

    @Override
    public ZEquipSlotType getSlotType() {
        return slot;
    }

    @Override
    public boolean canConsume() {
        return canConsume;
    }

    @Override
    public boolean canEquip() {
        return slot.canEquip();
    }

    @Override
    public boolean isMelee() {
        return false;
    }

    @Override
    public boolean isMagic() {
        return false;
    }

    @Override
    public boolean isRanged() {
        return false;
    }


}
