package cc.lib.zombicide;

public class ZArmor extends ZEquipment<ZArmorType> {

    static {
        addAllFields(ZArmor.class);
    }

    public ZArmor() {
        this(null);
    }

    ZArmor(ZArmorType type) {
        this.type = type;
    }

    final ZArmorType type;

    @Override
    public ZEquipSlotType getSlotType() {
        return type.slotType;
    }

    @Override
    public boolean canEquip() {
        return true;
    }

    @Override
    public boolean isArmor() {
        return true;
    }

    @Override
    public String name() {
        return type.name();
    }

    int getRating(ZZombieType type) {
        return this.type.getRating(type);
    }

    @Override
    public ZArmorType getType() {
        return type;
    }

}
