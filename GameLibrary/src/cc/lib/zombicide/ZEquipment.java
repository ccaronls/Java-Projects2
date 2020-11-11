package cc.lib.zombicide;

import cc.lib.utils.Reflector;

@Reflector.EnumInterfcae
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
