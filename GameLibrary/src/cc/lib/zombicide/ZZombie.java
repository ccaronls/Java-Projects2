package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public final class ZZombie extends ZActor<ZZombieType> {

    static {
        addAllFields(ZZombie.class);
    }

    @Override
    protected int getActionsPerTurn() {
        return type.actionsPerTurn;
    }

    public ZZombie() {
        super(-1);
        type = null;
    }

    public ZZombie(ZZombieType type, int zone) {
        super(zone);
        this.type = type;
        onBeginRound();
    }

    final ZZombieType type;
    @Omit
    private int imageId = -1;

    @Override
    public String name() {
        return type.name();
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, int width, int height) {
        Table info = new Table().setNoBorder();
        info.addRow("Min Hits", type.minDamageToDestroy);
        info.addRow("Actions", type.actionsPerTurn);
        info.addRow("Experience", type.expProvided);
        info.addRow("Ignores Armor", type.ignoresArmor);
        info.addRow("Ranged Priority", type.rangedPriority);
        Table outer = new Table().setNoBorder();
        outer.addColumn(name(), info);
        return outer.draw(g);//g.drawString(outer.toString(), 0, 0);
    }

    @Override
    public ZZombieType getType() {
        return type;
    }


    @Override
    public float getScale() {
        return type.getScale();
    }

    @Override
    public int getImageId() {
        if (imageId < 0)
            imageId = type.imageOptions[Utils.rand() % type.imageOptions.length];
        return imageId;
    }

    @Override
    ZCellQuadrant getSpawnQuadrant() {
        switch (type) {
            case Abomination:
                return ZCellQuadrant.CENTER;
        }
        return super.getSpawnQuadrant();
    }

    @Override
    long getMoveSpeed() {
        switch (type) {
            case Runner:
                return 500;
        }
        return super.getMoveSpeed();
    }

    @Override
    int getPriority() {
        return type.ordinal();
    }
}

