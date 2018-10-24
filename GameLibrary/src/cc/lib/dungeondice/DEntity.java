package cc.lib.dungeondice;

import cc.lib.game.AGraphics;
import cc.lib.ui.UIAnimation;
import cc.lib.utils.Reflector;

public abstract class DEntity extends Reflector<DEntity> {

    static {
        addAllFields(DEntity.class);
    }

    int hp, str, dex, attack, defense, spd;

    UIAnimation animation;

    public DEntity() {}

    public DEntity(int hp, int str, int dex, int attack, int defense) {
        this.hp = hp;
        this.str = str;
        this.dex = dex;
        this.attack = attack;
        this.defense = defense;
    }

    public abstract String getName();

    public abstract void draw(AGraphics g, float radius);
}
