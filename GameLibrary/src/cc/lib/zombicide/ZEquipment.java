package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

@Reflector.EnumInterfcae
public interface ZEquipment {

    ZEquipSlotType getSlotType();

    default boolean canOpenDoor() {
        return false;
    }

    default boolean canConsume() {
        return false;
    }

    String name();

    boolean canEquip();

    default boolean isMelee() {
        return false;
    }

    default boolean isMagic() {
        return false;
    }

    default boolean isRanged() {
        return false;
    }

    default boolean isEnchantment() {
        return false;
    }

    default boolean isArmor() {
        return false;
    }

    default String displayName() {
        return Utils.getPrettyString(name());
    }
}
