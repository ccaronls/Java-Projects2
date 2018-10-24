package cc.lib.dungeondice;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;

public class DEnemy extends DEntity {

    static {
        addAllFields(DEnemy.class);
    }

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

    public DEnemy() {}

    DEnemy(EnemyType type, int hp, int str, int dex, int attack, int defense) {
        super(hp, str, dex, attack, defense);
        this.type = type;
    }

    @Override
    public String getName() {
        return type + "(" + hp + ")";
    }

    @Override
    public void draw(AGraphics g, float radius) {
        switch (type) {
            case RAT:
                g.drawImage(UIGameRenderer.ratId, 0, 0, radius, radius);
                break;
            case SNAKE:
                g.drawImage(UIGameRenderer.snakeId, 0, 0, radius, radius);
                break;
            case SPIDER:
                g.drawImage(UIGameRenderer.spiderId, 0, 0, radius, radius);
                break;
        }
    }
}
