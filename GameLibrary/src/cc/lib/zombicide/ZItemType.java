package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZItemType implements ZEquipmentType<ZItem> {
    AAHHHH(null, ZActionType.NOTHING, null, "Stop Searching and place a zombie in the room being searched."),
    TORCH(ZEquipmentClass.THROWABLE, ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Draw 2 cards when searching. Spend an action, discard, and select a dragon bile at range 0-1 to ignite. Resolve dragon Fire.") {
        @Override
        public void onThrown(ZGame game, ZCharacter thrower, int targetZoneIdx) {
            ZZone zone = game.getBoard().getZone(targetZoneIdx);
            game.onEquipmentThrown(thrower.getPlayerName(), ZIcon.TORCH, targetZoneIdx);
            if (!zone.isDragonBile()) {
                game.addLogMessage("Throwing the Torch had no effect");
            } else {
                game.performDragonFire(thrower, zone.getZoneIndex());
            }
        }
    },

    DRAGON_BILE(ZEquipmentClass.THROWABLE, ZActionType.THROW_ITEM, ZEquipSlotType.HAND, "Spend an action, discard and place a dragon bile token at range 0-1") {
        @Override
        public void onThrown(ZGame game, ZCharacter thrower, int targetZoneIdx) {
            game.addLogMessage(thrower.name() + " threw the dragon Bile!");
            game.onEquipmentThrown(thrower.getPlayerName(), ZIcon.DRAGON_BILE, targetZoneIdx);
            game.getBoard().getZone(targetZoneIdx).setDragonBile(true);
        }
    },
    WATER(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 1 experience point"),
    SALTED_MEAT(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 2 experience point"),
    APPLES(ZEquipmentClass.CONSUMABLE, ZActionType.CONSUME, ZEquipSlotType.BACKPACK, "Consume and gain 3 experience point"),
    PLENTY_OF_ARROWS(ZEquipmentClass.BOW, ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bows. The new result takes place of old."), // user can reroll ranged arrow attacks if they want
    PLENTY_OF_BOLTS(ZEquipmentClass.CROSSBOW, ZActionType.NOTHING, ZEquipSlotType.BACKPACK, "You may re-roll all ranged attacked involving bolts. The new result takes place of old."),
    BARRICADE(ZEquipmentClass.CONSUMABLE, ZActionType.BARRICADE_DOOR, ZEquipSlotType.HAND, "Close and barricade a door. Takes 3 turns to execute."),
    ;

    ZItemType(ZEquipmentClass clazz, ZActionType actionType, ZEquipSlotType slot, String description) {
        this.equipClass = clazz;
        this.actionType = actionType;
        this.slot = slot;
        this.description = description;
    }

    final ZEquipmentClass equipClass;
    final ZEquipSlotType slot;
    final ZActionType actionType;
    final String description;

    @Override
    public ZItem create() {
        return new ZItem(this);
    }

    @Override
    public boolean isActionType(ZActionType type) {
        return type == actionType;
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

    @Override
    public ZEquipmentClass getEquipmentClass() {
        return equipClass;
    }
}
