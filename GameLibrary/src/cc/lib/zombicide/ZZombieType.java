package cc.lib.zombicide;

public enum ZZombieType {
    Walker(1, 1, 1, false, 1),
    Fatty(2, 1, 1, false, 2),
    Runner(1, 1, 2, false, 3),
    Abomination(3, 1, 1, true, 2),
    Necromancer(1, 1, 0, false, 4);

    ZZombieType(int minDamageToDestroy, int expProvided, int actionsPerTurn, boolean ignoresArmor, int rangedPriority) {
        this.minDamageToDestroy = minDamageToDestroy;
        this.expProvided = expProvided;
        this.actionsPerTurn = actionsPerTurn;
        this.ignoresArmor = ignoresArmor;
        this.rangedPriority = rangedPriority;
    }

    final int minDamageToDestroy;
    final int expProvided;
    final int actionsPerTurn;
    final boolean ignoresArmor;
    final int rangedPriority;
}
