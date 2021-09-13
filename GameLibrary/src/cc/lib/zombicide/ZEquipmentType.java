package cc.lib.zombicide;

import java.util.Collections;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.ui.IButton;
import cc.lib.utils.GException;

public interface ZEquipmentType<T extends ZEquipment> extends IButton {

    T create();

    String name();

    @Override
    default String getLabel() {
        return Utils.toPrettyString(name());
    }

    @Override
    default String getTooltipText() {
        return null;
    }

    /**
     * Additional skills processed when item equipped in hand or body
     * @return
     */
    default List<ZSkill> getSkillsWhileEquipped() { return Collections.emptyList(); }

    /**
     * Additional skills processed when the item used
     *
     * @return
     */
    default List<ZSkill> getSkillsWhenUsed() { return Collections.emptyList(); }

    ZEquipmentClass getEquipmentClass();

    /**
     * Items have can potentially support multiple actions
     *
     * @param type
     * @return
     */
    boolean isActionType(ZActionType type);

    default void onThrown(ZGame game, ZCharacter thrower, int targetZoneIdx) {
        throw new GException("Not a throwable item '" + this + "'");
    }

    default int getThrowMinRange() { return 0; }

    default int getThrowMaxRange() { return 1; }

    default int getDieRollToBlock(ZZombieType type) { return 0; }

    default boolean isShield() { return false; }
}
