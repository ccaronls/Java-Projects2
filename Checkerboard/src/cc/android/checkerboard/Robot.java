package cc.android.checkerboard;

import java.util.ArrayList;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 10/5/17.
 */

public class Robot extends Reflector<Robot> {

    static {
        addField(Robot.class, "type");
    }

    public enum RobotType {
        RANDOM, MINIMAX_BOT1, MINIMAX_BOT2,
    }

    private final RobotType type;

    public Robot() { type = RobotType.MINIMAX_BOT1; }

    Robot(int difficulty) {
        this.type = RobotType.values()[difficulty];
    }

    void doRobot(Checkers game, CheckTree root) {
        switch (type) {
            case RANDOM:
                doRandomRobot(root);
                break;
            case MINIMAX_BOT1: {
                doMinimaxRobot(game, root, 1, 1);
                break;
            }
            case MINIMAX_BOT2: {
                doMinimaxRobot(game, root, 2, 1);
                break;
            }
        }
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
    private long doMinimaxRobot(Checkers game, CheckTree root, int depth, int scale) {
        long d = scale < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        if (game.computeMoves() > 0) {
            for (Piece p : game.getPieces()) {
                for (Move m : new ArrayList<>(p.moves)) {
                    game.executeMove(m);
                    CheckTree next = new CheckTree(game, m);
                    next.appendMeta("playerNum=%d, scale=%d, depth=%d", m.playerNum, scale, depth);
                    root.addChild(next);
                    long v;
                    if (game.getCurPlayerNum() == m.playerNum) {
                        // this means we have more move options
                        v = doMinimaxRobot(game, next, depth, scale);
                    } else if (depth > 0) {
                        v = doMinimaxRobot(game, next, depth-1, scale * -1);
                    } else {
                        v = evaluateBoard(game, next, m.playerNum) * scale;
                    }
                    next.setValue(v);
                    next.appendMeta("%s:%s", m.type, v);
                    //d = Math.max(v, d);
                    if (scale < 0)
                        d = Math.min(d, v);
                    else
                        d = Math.max(d, v);
                    game.undo();
                }
            }
            root.sortChildren();
        }
        return d;
    }
/*
    final int [][] jumpable = {
            {0,0,0,0,0,0,0,0},
            {0,4,4,4,4,4,4,0},
            {0,4,4,4,4,4,4,0},
            {0,4,4,4,4,4,4,0},
            {0,4,4,4,4,4,4,0},
            {0,4,4,4,4,4,4,0},
            {0,4,4,4,4,4,4,0},
            {0,0,0,0,0,0,0,0},
    };

    int getJumpIndex(Checkers game, int rank, int cols) {
        int value = 0;
        if (game.isOnBoard(rank, cols)) {
            Piece p = game.getPiece(rank, cols);
            value = -jumpable[rank][cols];
            // check if jumpable from ur->ll by normal piece

            // space can be open, a jumper or a blocker

        }
    }

    /**
     * Returns a number between -1 and 1.
     * The value returned must reflect the whole board, not just one side
     *
     * @return
     */
    protected long evaluateBoard(Checkers game, CheckTree node, int playerNum) {

        int dPc=0;
        int dKing=0;
        int dAdv=0;

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
            //for (Piece p : game..getBoard[rank]) {
                Piece p = game.getPiece(rank, col);

                if (p.playerNum == playerNum) {
                    if (p.stacks == 1) {
                        dPc++;
                        //dAdv += game.getAdvancement(rank, p.playerNum);
                    } else if (p.stacks == 2)
                        dKing++;

                } else if (p.playerNum >= 0) {
                    if (p.stacks == 1) {
                        dPc--;
                        //dAdv -= game.getAdvancement(rank, p.playerNum);
                    } else if (p.stacks == 2)
                        dKing--;
                }
            }
        }

        long d = 100*dPc + 10000*dKing + dAdv;

        if (node != null) {
            node.appendMeta(String.format(
                              //"%1$20s:%2$d
                              "(%3$d)\n"
                            + "%4$s:%5$d\n"
                            + "%6$s:%7$d\n"
                            + "%8$s:%9$d\n"
//                            + "%10$20s:%11$d\n"
//                            + "%12$20s:%13$d\n"
//                            + "%14$20s:%15$d\n"
                    ,
                    "Player", game.getCurPlayerNum(), d,
                    "dPcs  ", dPc,
                    "dKings", dKing,
                    "dAdv  ", dAdv
            ));
        }

        //d += (Utils.rand() % 10 - 5); // add a fudge factor to keep AI from doing same move over and over

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
