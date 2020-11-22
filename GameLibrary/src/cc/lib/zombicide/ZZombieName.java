package cc.lib.zombicide;

import static cc.lib.zombicide.ZZombieType.*;

public enum ZZombieName {
    Walker(Walker1, Walker2, Walker3, Walker4, Walker5),
    Fatty(Fatty1, Fatty2),
    Runner(Runner1, Runner2),
    Necromancer(ZZombieType.Necromancer),
    Abomination(ZZombieType.Abomination);

    ZZombieName(ZZombieType ... types) {
        this.TYPES = types;
    }

    final ZZombieType [] TYPES;
}
