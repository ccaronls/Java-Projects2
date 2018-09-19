package cc.lib.dungeondice;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class DEnemy extends Reflector<DEnemy> {

    public enum EnemyType {
        RAT(5),
        SNAKE(4),
        SPIDER(3);

        DEnemy newEnemy() {
            switch (this) {
                case RAT:
                    return new DEnemy(this, Utils.randRange(1, 6), 2, 3, 0, 1);
                case SNAKE:
                    return new DEnemy(this, Utils.randRange(1, 6), 2, 4, 1, 0);
                case SPIDER:
                    return new DEnemy(this, Utils.randRange(2, 12), 3, 4, 2, 1);
                default:
                    Utils.unhandledCase(this);
            }
            return null;
        }

        EnemyType(int chance) {
            this.chanceToFight = chance;
        }

        final int chanceToFight;
    }

    static {
        addAllFields(DEnemy.class);
    }

    EnemyType type;
    int hp;
    int str, dex, attack, defense;

    public DEnemy() {}

    DEnemy(EnemyType type, int hp, int str, int dex, int attack, int defense) {
        this.type = type;
        this.hp = hp;
        this.str = str;
        this.dex = dex;
        this.attack = attack;
        this.defense = defense;
    }
}
