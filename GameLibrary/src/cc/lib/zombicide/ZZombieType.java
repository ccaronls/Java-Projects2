package cc.lib.zombicide;

public enum ZZombieType {
    Walker,
    Fatty,
    Runner,
    Abomination,
    Necromancer;

    int mindamageToDestroy;
    int expProvided;
    int actionsPerTurn;
    boolean ignoresArmor;
}
