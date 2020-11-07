package cc.lib.zombicide;

public class ZActor {
    int zoneIndex;
    int actionsPerTurn;
    int actionsLeftThisTurn;

    void prepareTurn() {
        actionsLeftThisTurn = actionsPerTurn;
    }

}
