package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.utils.Reflector;
import cc.lib.utils.Table;

public abstract class ZEquipment<T extends ZEquipmentType> extends Reflector<ZEquipment<T>> implements IButton, Comparable<ZEquipment> {

    static {
        addAllFields(ZEquipment.class);
    }

    boolean vaultItem=false;
    ZEquipSlot slot = ZEquipSlot.BACKPACK;

    public abstract ZEquipSlotType getSlotType();

    public boolean isOpenDoorCapable() {
        return false;
    }

    public boolean isConsumable() {
        return false;
    }

    public abstract boolean isEquippable(ZCharacter c);

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

        public final boolean isThrowable() { return getType().isActionType(ZActionType.THROW_ITEM); }

    public boolean isDualWieldCapable() { return false; }

    public boolean isAttackNoisy() { return false; }

    public boolean isOpenDoorsNoisy() { return false; }

    public abstract T getType();

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
    public int compareTo(ZEquipment o) {
        // Consider not using this. Causes a problem when multiple of same equipment for user to choose from
        return getLabel().compareTo(o.getLabel());
    }

    @Override
    public int hashCode() {
        return getType().hashCode();
    }

    public abstract Table getCardInfo(ZCharacter c, ZGame game);

    @Override
    public String getTooltipText() {
        if (getType() instanceof IButton) {
            return ((IButton)getType()).getTooltipText();
        }
        return null;
    }

    @Override
    public String toString() {
        return Utils.toPrettyString(getType().name());
    }

    @Override
    public String getLabel() {
        return Utils.toPrettyString(getType().name());
    }

    public void onEndOfRound(ZGame game) {}

    @Override
    protected boolean isImmutable() {
        return true;
    }
}
