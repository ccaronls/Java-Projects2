package cc.lib.zombicide;

public class ZWeapon extends ZEquipment<ZWeaponType> {

    static {
        addAllFields(ZWeapon.class);
    }

    final ZWeaponType type;
    boolean needsReload=false;

    public ZWeapon() {
        this(null);
    }

    ZWeapon(ZWeaponType type) {
        this.type = type;
    }

    @Override
    public boolean canOpenDoor() {
        return type.meleeStats != null && type.meleeStats.dieRollToOpenDoor > 0;
    }

    @Override
    public ZEquipSlotType getSlotType() {
        return ZEquipSlotType.HAND;
    }

    @Override
    public boolean isMelee() {
        return type.meleeStats != null;
    }

    @Override
    public boolean isRanged() {
        return type.rangedStats != null;
    }

    @Override
    public boolean isMagic() {
        return type.magicStats != null;
    }

    @Override
    public boolean canEquip() {
        return true;
    }

    public boolean isNeedsReload() {
        return needsReload;
    }

    @Override
    public String name() {
        return type.name();
    }

    @Override
    public ZWeaponType getType() {
        return type;
    }

}
