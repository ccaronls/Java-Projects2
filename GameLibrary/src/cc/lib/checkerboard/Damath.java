package cc.lib.checkerboard;

import cc.lib.game.Utils;

/**
 * Like checkers except jump execute a math operation to get points. Winner is player with most points at end of game.
 *
 * TODO: Set 20 min time limit
 */
class Damath extends Checkers {

    enum Operation {
        ADD, SUB, MUL, DIV;

        int compute(int v0, int v1) {
            switch (this) {

                case ADD:
                    return v0+v1;
                case SUB:
                    return v0-v1;
                case MUL:
                    return v0*v1;
                case DIV:
                    return v0/v1;
            }
            throw new AssertionError("Unhandled case: " + this);
        }
    }

    Operation [][] boardOps = new Operation[8][8];

    int [] score = new int[2];

    @Override
    public void init(Game game) {
        for (int r=0; r<8; r++) {
            for (int c=r%2; c<8; c+=2) {
                boardOps[r][c] = Utils.randItem(Operation.values());
            }
        }

        super.init(game);
        for (int i=0; i<8; i++) {
            for (int ii=0; ii<8; ii++) {
                Piece p = game.getPiece(i, ii);
                if (p.getType() == PieceType.CHECKER) {
                    p.setValue(Utils.randRange(1, 12));
                }
            }
        }
    }

    @Override
    void executeMove(Game game, Move move) {
        if (move.hasCaptured()) {
            Player p = game.getPlayer(move.getPlayerNum());
            int score = 0;
            Operation op = boardOps[move.getCaptured()[0]][move.getCaptured()[1]];

        }


        super.executeMove(game, move);
    }
}
