package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public abstract class ZActor extends Reflector<ZActor> {

    static {
        addAllFields(ZActor.class);
    }

    ZActor(int zone) {
        this.occupiedZone = zone;
    }

    int occupiedZone;
    Grid.Pos occupiedCell;
    int occupiedQuadrant;
    private int actionsLeftThisTurn;
    GRectangle rect;

    void prepareTurn() {
        actionsLeftThisTurn = getActionsPerTurn();
    }

    protected abstract int getActionsPerTurn();

    protected abstract int getImageId();

    public abstract String name();

    protected boolean performAction(ZActionType action, ZGame game) {
        actionsLeftThisTurn-=action.costPerTurn();
        return false;
    }

    public int getActionsLeftThisTurn() {
        return actionsLeftThisTurn;
    }

    public final GRectangle getRect() {
        return rect;
    }

    public abstract void drawInfo(AGraphics g,ZGame game, int width, int height);

    public int getOccupiedZone() {
        return occupiedZone;
    }

    public int getNoise() {
        return 0;
    }
}
