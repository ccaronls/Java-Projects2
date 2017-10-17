package cc.android.checkerboard;

import cc.lib.game.MiniMaxTree;
import cc.lib.game.MiniMaxTree.MMTreeNode;
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

    final MiniMaxTree mmtCheckers = new MiniMaxTree<Checkers>() {

        @Override
        protected long evaluate(Checkers game, MMTreeNode t, int playerNum) {
            return Robot.this.evaluateCheckersBoard(game, t, playerNum);
        }
    };

    final MiniMaxTree mmtChess = new MiniMaxTree<Chess>() {

        @Override
        protected long evaluate(Chess game, MMTreeNode t, int playerNum) {
            return Robot.this.evaluateChessBoard(game, t, playerNum);
        }
    };

    void doRobot(ACheckboardGame game, MMTreeNode<Move, ACheckboardGame> root) {
        MiniMaxTree mmt;
        if (game instanceof Checkers)
            mmt = mmtCheckers;
        else
            mmt = mmtChess;

        switch (type) {
            case RANDOM:
                doRandomRobot(root);
                break;
            case MINIMAX_BOT1: {
                mmt.buildTree(game, root, 1);
                break;
            }
            case MINIMAX_BOT2: {
                mmt.buildTree(game, root, 2);
                break;
            }
        }
    }

    protected long evaluateChessBoard(Chess game, MMTreeNode node, int playerNum) {

        int dPcCount = 0;
        int dPcValue = 0;
        int dPawnAdv = 0;

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
                //for (Piece p : game..getBoard[rank]) {
                Piece p = game.getPiece(rank, col);
                if (p.playerNum < 0)
                    continue;

                final int scale = p.playerNum == playerNum ? 1 : -1;
                int value = 0;

                switch (p.type) {
                    case PAWN:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.playerNum));
                        value = 1; break;
                    case PAWN_IDLE:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.playerNum));
                        value = 2; break;
                    case PAWN_ENPASSANT:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.playerNum));
                        value = 4; break;
                    case PAWN_TOSWAP:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.playerNum));
                        value = 1000; break;
                    case BISHOP:
                        value = 3; break;
                    case KNIGHT:
                        value = 3; break;
                    case ROOK:
                        value = 5; break;
                    case ROOK_IDLE:
                        value = 6; break;
                    case QUEEN:
                        value = 8; break;
                    case CHECKED_KING:
                        value = -100; break;
                    case CHECKED_KING_IDLE:
                        value = -99; break;
                    case UNCHECKED_KING:
                        value = 100; break;
                    case UNCHECKED_KING_IDLE:
                        value = 100; break;
                    default:
                        continue;
                }

                dPcCount += scale;
                dPcValue += scale * value;
            }
        }

        long d = dPawnAdv * 100 + dPcCount * 1000 + dPcValue * 10000;
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
                    "Player", game.getTurn(), d,
                    "dPcCount ", dPcCount,
                    "dPcValue ", dPcValue,
                    "dPawnAdv ", dPawnAdv
            ));
        }
        return d + (Utils.rand() % 10 - 5); // add some noise to resolve dups
    }

    /**
     * Returns a number in INF to -INF range. -INF would be game over 'I lost'
     * The value returned must reflect the whole board, not just one side.
     *
     * So eval(P) = -eval(opponent(P))
     *
     * @return
     */
    protected long evaluateCheckersBoard(Checkers game, MMTreeNode node, int playerNum) {

        int dPc=0;
        int dKing=0;
        int dAdv=0;

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
            //for (Piece p : game..getBoard[rank]) {
                Piece p = game.getPiece(rank, col);

                if (p.playerNum == playerNum) {
                    switch (p.type) {
                        case CHECKER:
                            dPc++; break;
                        case KING:
                            dKing++; break;
                    }

                } else if (p.playerNum >= 0) {
                    switch (p.type) {
                        case CHECKER:
                            dPc--; break;
                        case KING:
                            dKing--; break;
                    }
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
                    "Player", game.getTurn(), d,
                    "dPcs  ", dPc,
                    "dKings", dKing,
                    "dAdv  ", dAdv
            ));
        }

        d += (Utils.rand() % 10 - 5); // add a fudge factor to keep AI from doing same move over and over

        //Log.d("AI", "Board evaluates too: " + d);
        return d;
    }

    private void doRandomRobot(MMTreeNode<Move, ACheckboardGame> tree) {
        ACheckboardGame game = tree.getGame();
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
