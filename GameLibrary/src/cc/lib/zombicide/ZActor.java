package cc.lib.zombicide;

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

    void prepareTurn() {
        actionsLeftThisTurn = getActionsPerTurn();
    }

    protected abstract int getActionsPerTurn();

    protected abstract int getImageId();

    public abstract String name();

    protected void performAction(ZActionType action) {
        actionsLeftThisTurn--;
    }

    public int getActionsLeftThisTurn() {
        return actionsLeftThisTurn;
    }

}
