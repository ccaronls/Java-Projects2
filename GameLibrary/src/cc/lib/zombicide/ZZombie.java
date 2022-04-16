package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public final class ZZombie extends ZActor<ZZombieType> {

    static {
        addAllFields(ZZombie.class);
    }

    final ZZombieType type;
    private int imageIdx = -1;
    boolean destroyed = false;

    @Override
    protected int getActionsPerTurn() {
        return type.actionsPerTurn;
    }

    public ZZombie() {
        this(ZZombieType.Walker, -1);
    }

    public ZZombie(ZZombieType type, int zone) {
        super(zone);
        this.type = type;
        onBeginRound();
    }

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
        if (type.imageDims == null) {
            return GDimension.EMPTY;
        }
        return type.imageDims[getIdx()];
    }

    @Override
    ZCellQuadrant getSpawnQuadrant() {
        switch (type) {
            case Abomination:
            case Wolfbomination:
                return ZCellQuadrant.CENTER;
        }
        return super.getSpawnQuadrant();
    }

    @Override
    long getMoveSpeed() {
        if (type == ZZombieType.Runner) {
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
    protected boolean performAction(ZActionType action, ZGame game) {
        if (action == ZActionType.MELEE) {
            actionsLeftThisTurn = 0;
            return false; // zombies are done once they attack
        }
        return super.performAction(action, game);
    }

    @Override
    public void draw(AGraphics g) {
        if (isAlive() || isAnimating()) {
            super.draw(g);
            if (actionsLeftThisTurn > 1) {
                g.setColor(GColor.WHITE);
                float oldHgt = g.setTextHeight(10);
                g.drawJustifiedString(getRect().getCenterBottom(), Justify.CENTER, Justify.BOTTOM, String.valueOf(actionsLeftThisTurn));
                g.setTextHeight(oldHgt);
            }
        }
    }

    @Override
    public boolean isAlive() {
        return !destroyed;
    }
}

