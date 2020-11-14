package cc.lib.zombicide;

public class ZEnchantment extends ZEquipment<ZSpellType> {

    static {
        addAllFields(ZEnchantment.class);
    }

    final ZSpellType type;

    public ZEnchantment() {
        this(null);
    }

    ZEnchantment(ZSpellType type) {
        this.type = type;
    }

    @Override
    public ZEquipSlotType getSlotType() {
        return ZEquipSlotType.HAND;
    }

    @Override
    public boolean canEquip() {
        return true;
    }

    @Override
    public boolean isEnchantment() {
        return true;
    }

    @Override
    public ZSpellType getType() {
        return type;
    }

    @Override
    public String name() {
        return type.name();
    }

}
