package cc.lib.zombicide;

public interface ZEquipment {

    ZEquipSlot getSlot();

    default boolean canOpenDoor() {
        return false;
    }

    default boolean canConsume() {
        return false;
    }

    String name();
}
