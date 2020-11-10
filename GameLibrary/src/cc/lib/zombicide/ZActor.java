package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;

public class ZActor {
    int occupiedZone;
    int actionsPerTurn;
    int actionsLeftThisTurn;
    GRectangle boundingRectangle;

    void prepareTurn() {
        actionsLeftThisTurn = actionsPerTurn;
    }

    public void draw(AGraphics g) {

    }
}
