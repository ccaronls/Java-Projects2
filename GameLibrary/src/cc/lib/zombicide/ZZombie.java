package cc.lib.zombicide;

import cc.lib.game.AGraphics;

public class ZZombie extends ZActor {

    static {
        addAllFields(ZZombie.class);
    }

    @Override
    protected int getActionsPerTurn() {
        return type.actionsPerTurn;
    }

    public ZZombie() {
        this(null, -1);
    }

    ZZombie(ZZombieType type, int zone) {
        super(zone);
        this.type = type;
    }

    final ZZombieType type;

    @Override
    protected int getImageId() {
        return type.imageId;
    }

    @Override
    public String name() {
        return type.name();
    }

    @Override
    public void drawInfo(AGraphics g, int width, int height) {
        String txt = name()
                + "\nMin Hits        : " + type.minDamageToDestroy
                + "\nActions         : " + type.actionsPerTurn
                + "\nExperience      : " + type.expProvided
                + "\nIgnores Armor   : " + type.ignoresArmor
                + "\nRanged Priority : " + type.rangedPriority;
        g.drawString(txt, 0, 0);
    }
}

