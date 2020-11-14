package cc.lib.zombicide;

public enum ZZombieType {
    Walker1("Walker", 1, 1, 1, false, 1),
    Walker2("Walker", 1, 1, 1, false, 1),
    Walker3("Walker", 1, 1, 1, false, 1),
    Walker4("Walker", 1, 1, 1, false, 1),
    Walker5("Walker", 1, 1, 1, false, 1),
    Fatty1("Fatty", 2, 1, 1, false, 2),
    Fatty2("Fatty",2, 1, 1, false, 2),
    Runner1("Runner", 1, 1, 2, false, 3),
    Runner2("Runner",1, 1, 2, false, 3),
    Abomination("Abomination", 3, 1, 1, true, 2),
    Necromancer("Necromancer", 1, 1, 0, false, 4);

    ZZombieType(String commonName, int minDamageToDestroy, int expProvided, int actionsPerTurn, boolean ignoresArmor, int rangedPriority) {
        this.commonName = commonName;
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
    final String commonName;

    public int imageId = -1;

    public final static ZZombieType [] WALKERS = {
            Walker1, Walker2, Walker3, Walker4, Walker5
    };

    public final static ZZombieType [] FATTIES = {
            Fatty1, Fatty2
    };

    public final static ZZombieType [] NECROMANCERS = {
            Necromancer
    };

    public final static ZZombieType [] RUNNERS = {
            Runner1, Runner2
    };

    public final static ZZombieType [] ABOMINATIONS = {
            Abomination
    };

}
