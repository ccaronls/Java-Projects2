package cc.lib.zombicide;

import cc.lib.game.Utils;
import cc.lib.utils.Table;

public class ZArmor extends ZEquipment<ZArmorType> {

    static {
        addAllFields(ZArmor.class);
    }

    public ZArmor() {
        this(null);
    }

    ZArmor(ZArmorType type) {
        this(type, 0);
    }

    ZArmor(ZArmorType type, int adjustment) {
        this.type = type;
        this.adjustment = adjustment;
    }

    private final ZArmorType type;
    private final int adjustment;

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

    int getRating(ZZombieType type) {
        return this.type.getRating(type) + adjustment;
    }

    @Override
    public ZArmorType getType() {
        return type;
    }

    @Override
    public Table getCardInfo(ZCharacter c, ZGame game) {

        /*

        SHIELD
        specialInfo
        ---------------
        RATING
        Walker      | 0
        Fatty       | 5
        Runner      | 5
        Necromancer | 5
        Abomination | 5
        Special
         */



        Table ratings = new Table().setNoBorder();
        for (ZZombieType type : Utils.asList(ZZombieType.Walker, ZZombieType.Fatty, ZZombieType.Runner, ZZombieType.Necromancer, ZZombieType.Abomination)) {
            ratings.addRow(type, getRating(type));
        }
        Table main = new Table(type.name()).setNoBorder().addRow(ratings);
        if (type.specialAbilityDescription != null) {
            main.addRow(Utils.wrapTextWithNewlines(type.specialAbilityDescription, 32));
        }
        return main;
    }
}
