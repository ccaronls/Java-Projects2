package cc.lib.zombicide;

import java.util.Arrays;

import cc.lib.game.Utils;
import cc.lib.utils.Table;

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
    public boolean isConsumable() {
        return type.isActionType(ZActionType.CONSUME);
    }

    @Override
    public boolean isEquippable(ZCharacter c) {
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
    public ZItemType getType() {
        return type;
    }

    @Override
    public Table getCardInfo(ZCharacter c, ZGame game) {
        Table card = new Table().setNoBorder();
        card.addColumn(getLabel(), Arrays.asList(Utils.wrapTextWithNewlines(type.description, 24)));
        return card;
    }

    @Override
    public String getTooltipText() {
        return type.description;
    }
}
