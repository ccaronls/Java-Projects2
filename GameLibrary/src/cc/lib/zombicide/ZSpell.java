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
    public String getCardString(ZCharacter c, ZGame game) {
        return new Table(new String [] { type.name() },
                new Object [][] {
                        {Utils.wrapText(type.description, 32)}
                }).toString();
    }
}
