package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.utils.Table;

public class ZSpell extends ZEquipment<ZSpellType> {

    static {
        addAllFields(ZSpell.class);
    }

    final ZSpellType type;

    public ZSpell() {
        this(null);
    }

    ZSpell(ZSpellType type) {
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
    public Table getCardInfo(ZCharacter c, ZGame game) {
        Table t = new Table(type.name()).setNoBorder();
        return t.addRow(Utils.wrapTextWithNewlines(type.description, 20));
    }

}
