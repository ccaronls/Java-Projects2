package cc.lib.zombicide;

public enum ZZombieType {
    // TODO: Dont break MVC. Here we make assumptions about assets for zombies.
    Walker1(1, 1, 1, false, 1),
    Walker2(1, 1, 1, false, 1),
    Walker3(1, 1, 1, false, 1),
    Walker4(1, 1, 1, false, 1),
    Walker5(1, 1, 1, false, 1),
    Fatty1(2, 1, 1, false, 2),
    Fatty2(2, 1, 1, false, 2),
    Runner1( 1, 1, 2, false, 3),
    Runner2(1, 1, 2, false, 3),
    Abomination( 3, 1, 1, true, 2),
    Necromancer( 1, 1, 0, false, 4);

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

    ZZombieName getCommonName() {
        switch (this) {
            case Walker1:
            case Walker2:
            case Walker3:
            case Walker4:
            case Walker5:
                return ZZombieName.Walker;
            case Fatty1:
            case Fatty2:
                return ZZombieName.Fatty;
            case Runner1:
            case Runner2:
                return ZZombieName.Runner;
            case Abomination:
                return ZZombieName.Abomination;
            case Necromancer:
                return ZZombieName.Necromancer;
        }
        assert(false);
        return null;
    }
}
