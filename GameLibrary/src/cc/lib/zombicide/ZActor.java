package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.utils.Reflector;

public abstract class ZActor extends Reflector<ZActor> {

    static {
        addAllFields(ZActor.class);
    }

    ZActor(int zone) {
        this.occupiedZone = zone;
    }

    int occupiedZone;
    int [] occupiedCell;
    int occupiedQuadrant;
    private int actionsLeftThisTurn;
    @Omit
    GRectangle rect;

    void prepareTurn() {
        actionsLeftThisTurn = getActionsPerTurn();
    }

    protected abstract int getActionsPerTurn();

    protected abstract int getImageId();

    public abstract String name();

    protected void performAction(ZActionType action, ZGame game) {
        actionsLeftThisTurn--;
    }

    public int getActionsLeftThisTurn() {
        return actionsLeftThisTurn;
    }

    public final GRectangle getRect() {
        return rect;
    }

    public abstract void drawInfo(AGraphics g, int width, int height);

    public int getOccupiedZone() {
        return occupiedZone;
    }
}
