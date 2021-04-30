package cc.lib.zombicide;

import cc.lib.annotation.Keep;

@Keep
public enum ZState {

    INIT,
    BEGIN_ROUND, // reset enchantments
    SPAWN,
    PLAYER_STAGE_CHOOSE_CHARACTER,
    PLAYER_STAGE_CHOOSE_CHARACTER_ACTION,
    PLAYER_STAGE_CHOOSE_NEW_SKILL,
    PLAYER_ENCHANT_SPEED_MOVE,
    PLAYER_STAGE_CHOOSE_ZONE_TO_REMOVE_SPAWN,
    ZOMBIE_STAGE,
    ;

}