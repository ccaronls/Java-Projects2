package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.utils.Reflector;

public abstract class ZEquipment<T extends Enum<T>> extends Reflector<ZEquipment<T>> implements IButton {

    public abstract ZEquipSlotType getSlotType();

    public boolean canOpenDoor() {
        return false;
    }

    public boolean canConsume() {
        return false;
    }

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

    public boolean isThrowable() { return false; }

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

    public abstract String getCardString(ZCharacter c, ZGame game);

    @Override
    public String getTooltipText() {
        if (getType() instanceof IButton) {
            return ((IButton)getType()).getTooltipText();
        }
        return null;
    }

    @Override
    public String toString() {
        return Utils.getPrettyString(getType().name());
    }

    @Override
    public String getLabel() {
        return Utils.getPrettyString(getType().name());
    }
}
