package cc.android.checkerboard;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 10/5/17.
 */

public class Robot {

    public enum RobotType {
        RANDOM, MINIMAX_BOT1, MINIMAX_BOT4,
    }

    private final RobotType type;

    Robot(int difficulty) {
        this.type = RobotType.values()[difficulty];
    }

    CheckTree doRobot(Checkers game) {
        Checkers copy = new Checkers();
        copy.copyFrom(game);
        CheckTree root = new CheckTree(copy);
        switch (type) {
            case RANDOM:
                doRandomRobot(root);
                break;
            case MINIMAX_BOT1: {
                doMinimaxRobot(copy, root, 1, 1);
                break;
            }
            case MINIMAX_BOT4: {
                doMinimaxRobot(copy, root, 4, 1);
                break;
            }
        }
        return root;
    }

    /**
     * This version will try each move I can make (including multiple steps) then check all of the next players move to analyze the board for myself
     *
     * After some research I realize a minimax descision tree will choose a path that minimizes the risk for each move.
     * In other words, the possible moves of the opponent after I have moved have the highest minimum.
     *
     * In checkers a 'move' is potentially several steps (chain). So, when there a chain detected (the playerNum has not changed)
     * then the 'depth' does not increment
     *
     * @param game
     * @param root
     */
    private double doMinimaxRobot(Checkers game, CheckTree root, int depth, int scale) {
        double d = Double.MIN_VALUE;
        if (game.computeMoves() > 0) {
            final int playerNum = game.getCurPlayerNum();
            for (Piece p : game.getPieces()) {
                for (Move m : p.moves) {
                    game.executeMove(m);
                    CheckTree next = new CheckTree(game, m);
                    root.addChild(next);
                    double v;
                    if (game.getCurPlayerNum() == playerNum) {
                        v = doMinimaxRobot(game, next, depth, scale);
                    } else if (depth > 0) {
                        v = doMinimaxRobot(game, next, depth, scale * -1);
                    } else {
                        v = evaluateBoard(game, next) * scale;
                        next.setValue(v);
                        break;
                    }
                    d = Math.max(v, d);
                    game.undo();
                }
            }
            root.sortChildren();
        }
        root.setValue(d);
        return d;
    }


    /**
     * Returns a number between -1 and 1.
     * The value returned must reflect the whole board, not just one side
     *
     * @return
     */
    protected double evaluateBoard(Checkers game, CheckTree node) {
        int mine = 0;
        int theirs = 0;

        int mineKings=0;
        int theirKings=0;

        int mineAdvance = 0;
        int theirAdvance = 0;

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
            //for (Piece p : game..getBoard[rank]) {
                Piece p = game.getPiece(rank, col);
                if (p.playerNum == game.getCurPlayerNum()) {
                    if (p.stacks == 1) {
                        mine++;
                        mineAdvance += game.getAdvancement(rank, p.playerNum);
                    } else if (p.stacks == 2)
                        mineKings++;

                } else if (p.playerNum >= 0) {
                    if (p.stacks == 1) {
                        theirs++;
                        theirAdvance += game.getAdvancement(rank, p.playerNum);
                    } else if (p.stacks == 2)
                        theirKings ++;
                }
            }
        }

        double d = 0.01 * (mine - theirs)
                + 0.1 * (mineKings - theirKings)
                + 0.001 * (mineAdvance - theirAdvance);
        if (node != null) {
            node.meta += String.format(
                    "%1$20s:%2$d (%3$f)\n"
                            + "%4$20s:%5$d\n"
                            + "%6$20s:%7$d\n"
                            + "%8$20s:%9$d\n"
                            + "%10$20s:%11$d\n"
                            + "%12$20s:%13$d\n"
                            + "%14$20s:%15$d\n"
                    ,
                    "Player", game.getCurPlayerNum(), d,
                    "Mine Pcs", mine,
                    "Theirs Pcs", theirs,
                    "Mine Kings", mineKings,
                    "Their Kings", theirKings,
                    "Mine Adv", mineAdvance,
                    "Their Adv", theirAdvance
            );
        }

        d += 0.000001 * (Utils.rand() % 100 - 50); // add a fudge factor to keep AI from doing same move over and over

        //Log.d("AI", "Board evaluates too: " + d);
        return d;
    }

    private void doRandomRobot(CheckTree tree) {
        Checkers game = tree.getGame();
        int n = game.computeMoves();
        if (n > 0) {
            int mvNum = Utils.rand() % n;
            Piece p;
            for (int i = 0; i < game.RANKS; i++) {
                for (int ii = 0; ii < game.COLUMNS; ii++) {
                    if ((p = game.getPiece(i, ii)).moves.size() > mvNum) {
                        Move m = p.moves.get(mvNum);
                        tree.setMove(m);
                    } else {
                        mvNum -= p.moves.size();
                    }
                }
            }
        }
    }

}
