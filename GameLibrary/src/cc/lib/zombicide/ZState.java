package cc.lib.zombicide;

public enum ZState {
    INIT,
    BEGIN_ROUND, // reset enchantments
    SPAWN,
    PLAYER_STAGE_CHOOSE_CHARACTER,
    PLAYER_STAGE_CHOOSE_CHARACTER_ACTION,
    PLAYER_STAGE_ORGANIZE,
    PLAYER_STAGE_CHOOSE_ZONE_FOR_BILE,
    PLAYER_STAGE_CHOOSE_NEW_SKILL,
    PLAYER_STAGE_CHOOSE_ZONE_TO_IGNITE,
    ZOMBIE_STAGE,
    ;

}