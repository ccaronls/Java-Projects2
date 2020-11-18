package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.utils.Table;

public final class ZZombie extends ZActor {

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

    ZZombie(ZZombieType type, int zone) {
        super(zone);
        this.type = type;
        prepareTurn();
    }

    final ZZombieType type;

    @Override
    protected int getImageId() {
        return type.imageId;
    }

    @Override
    public String name() {
        return type.commonName;
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, int width, int height) {
        Table info = new Table().setNoBorder();
        info.addRow("Min Hits", type.minDamageToDestroy);
        info.addRow("Actions", type.actionsPerTurn);
        info.addRow("Experience", type.expProvided);
        info.addRow("Ignores Armor", type.ignoresArmor);
        info.addRow("Ranged Priority", type.rangedPriority);
        Table outer = new Table();
        outer.addColumn(name(), info.toString());
        return g.drawString(outer.toString(), 0, 0);
    }
}

