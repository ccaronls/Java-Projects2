package cc.lib.zombicide;

public enum ZItem implements ZEquipment {
    TORCH(false, ZEquipSlot.HAND, "Draw 2 cards when searching. Spend an action, discard, and select a dragon bile at range 0-1 to ignite. Resolve dragon Fire."),
    DRAGON_BILE(true, ZEquipSlot.HAND, "Spend an action, discard and place a dragon bile token at range 0-1"),
    WATER(true, ZEquipSlot.BACKPACK, "Discard and gain 1 experience point"),
    SALTED_MEAT(true, ZEquipSlot.BACKPACK, "Discard and gain 1 experience point"),
    APPLES(true, ZEquipSlot.BACKPACK, "Discard and gain 1 experience point"),
    PLENTY_OF_ARROWS(false, ZEquipSlot.BACKPACK, "You may re-roll all ranged attacked involving bows. The new result takes place of old."), // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS(false, ZEquipSlot.BACKPACK, "You may re-roll all ranged attacked involving bolts. The new result takes place of old."),
    ;

    ZItem(boolean canConsume, ZEquipSlot slot, String description) {
        this.canConsume = canConsume;
        this.slot = slot;
        this.description = description;
    }

    private final ZEquipSlot slot;
    private final boolean canConsume;
    public final String description;

    @Override
    public ZEquipSlot getSlot() {
        return slot;
    }

    @Override
    public boolean canConsume() {
        return canConsume;
    }
}
