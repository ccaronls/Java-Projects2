package cc.lib.zombicide;

public enum ZEquipSlot {
    LEFT_HAND("LH"),
    BODY("Bo"),
    RIGHT_HAND("RH"),
    BACKPACK("BP");

    ZEquipSlot(String shorthand) {
        this.shorthand = shorthand;
    }

    final String shorthand;

}
