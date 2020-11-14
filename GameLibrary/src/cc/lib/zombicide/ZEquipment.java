package cc.lib.zombicide;

import cc.lib.utils.Reflector;

public abstract class ZEquipment<T extends Enum<T>> extends Reflector<ZEquipment<T>> {

    public abstract ZEquipSlotType getSlotType();

    public boolean canOpenDoor() {
        return false;
    }

    public boolean canConsume() {
        return false;
    }

    public abstract String name();

    public abstract boolean canEquip();

    public boolean isMelee() {
        return false;
    }

    public boolean isMagic() {
        return false;
    }

    public boolean isRanged() {
        return false;
    }

    public boolean isEnchantment() {
        return false;
    }

    public boolean isArmor() {
        return false;
    }

    public abstract Enum<T> getType();

    @Override
    public boolean equals(Object o) {
        if (this == o || getType() == o) return true;
        if (o == null) return false;
        if (!(o instanceof ZEquipment)) {
            return false;
        }
        return getType().equals(((ZEquipment)o).getType());
    }

    @Override
    public int hashCode() {
        return getType().ordinal();
    }

}
