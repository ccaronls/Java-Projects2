package cc.lib.zombicide;

public enum ZState {
    INIT,
    BEGIN_ROUND, // reset enchantments
    SPAWN,
    PLAYER_STAGE_CHOOSE_CHARACTER,
    PLAYER_STAGE_CHOOSE_CHARACTER_ACTION,
    PLAYER_STAGE_CHOOSE_NEW_SKILL,
    ZOMBIE_STAGE,
    ;

}