package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public final class ZZombie extends ZActor<ZZombieType> implements Comparable<ZZombie> {

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
    private int imageIdx = -1;

    private int getIdx() {
        if (imageIdx < 0 || imageIdx >= type.imageOptions.length)
            imageIdx = Utils.rand() % type.imageOptions.length;
        return imageIdx;
    }

    @Override
    public String name() {
        return type.name();
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, float width, float height) {
        Table info = new Table(getLabel()).setNoBorder();
        info.addRow("Min Hits", type.minDamageToDestroy);
        info.addRow("Actions", type.actionsPerTurn);
        info.addRow("Experience", type.expProvided);
        info.addRow("Ignores Armor", type.ignoresArmor);
        info.addRow("Ranged Priority", type.attackPriority);
        Table outer = new Table().setNoBorder();
        outer.addRow(info, type.description);
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
        return type.imageOptions[getIdx()];
    }

    @Override
    public int getOutlineImageId() {
        return type.imageOutlineOptions[getIdx()];
    }

    @Override
    public GDimension getDimension() {
        return type.imageDims[getIdx()];
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

    public String getDescription() {
        return type.description;
    }

    @Override
    public int compareTo(ZZombie o) {
        if (o.type.minDamageToDestroy == type.minDamageToDestroy) {
            return Integer.compare(type.attackPriority, o.type.attackPriority);
        };
        return Integer.compare(o.type.minDamageToDestroy, type.minDamageToDestroy);
    }

    @Override
    protected boolean performAction(ZActionType action, ZGame game) {
        if (action == ZActionType.MELEE) {
            actionsLeftThisTurn = 0;
            return false; // zombies are done once they attack
        }
        return super.performAction(action, game);
    }
}

