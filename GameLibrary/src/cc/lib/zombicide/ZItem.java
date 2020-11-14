package cc.lib.zombicide;

public class ZItem extends ZEquipment<ZItemType> {

    static {
        addAllFields(ZItem.class);
    }

    public ZItem() {
        this(null);
    }

    ZItem(ZItemType type) {
        this.type = type;
    }

    final ZItemType type;

    @Override
    public ZEquipSlotType getSlotType() {
        return type.slot;
    }

    @Override
    public boolean canConsume() {
        return type.canConsume;
    }

    @Override
    public boolean canEquip() {
        return type.slot.canEquip();
    }

    @Override
    public boolean isMelee() {
        return false;
    }

    @Override
    public boolean isMagic() {
        return false;
    }

    @Override
    public boolean isRanged() {
        return false;
    }

    @Override
    public String name() {
        return type.name();
    }

    @Override
    public ZItemType getType() {
        return type;
    }

}
