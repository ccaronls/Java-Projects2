package cc.lib.checkers;

import cc.lib.game.DescisionTree;
import cc.lib.game.IGame;
import cc.lib.game.IMove;
import cc.lib.game.MiniMaxTree;
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
        EASY, MEDIUM, HARD,
    }

    final RobotType type;

    public Robot() { type = RobotType.MEDIUM; }

    public Robot(int difficulty) {
        this.type = RobotType.values()[difficulty];
    }

    public RobotType getDifficulty() {
        return type;
    }

    public RobotType getType() {
        return type;
    }

    final MiniMaxTree mmtCheckers = new MiniMaxTree() {
        @Override
        protected long evaluate(IGame game, DescisionTree t, int playerNum) {
            return Robot.this.evaluateCheckersBoard((Checkers)game, t, playerNum);
        }

        @Override
        protected void onNewNode(IGame game, DescisionTree node) {
            onNewMove((Move)node.getMove());
        }
    };

    final MiniMaxTree mmtChess = new MiniMaxTree() {

        @Override
        protected void onNewNode(IGame game, DescisionTree node) {
            Move m = (Move)node.getMove();
            Piece p = ((Chess)game).getPiece(m.getStart());
            if (m.hasEnd()) {
                node.appendMeta("%s->%dx%d", p.getType().abbrev, m.getEnd()[0], m.getEnd()[1]);
            } else {
                node.appendMeta("%s %s", m.getMoveType().name(), p.getType().abbrev);
            }
            onNewMove(m);
        }

        @Override
        protected long evaluate(IGame game, DescisionTree t, int playerNum) {
            return Robot.this.evaluateChessBoard((Chess)game, t, playerNum);
        }

        @Override
        protected long getZeroMovesValue(IGame game) {
            if (null != ((Chess)game).findPiece(game.getTurn(), PieceType.CHECKED_KING_IDLE, PieceType.CHECKED_KING))
                return super.getZeroMovesValue(game);
            return 0; // if we have no moves and our king is not in check then this is a draw
        }
    };

    protected void onNewMove(Move m) {}

    public void doRobot(ACheckboardGame game, DescisionTree root) {
        MiniMaxTree mmt;
        if (game instanceof Checkers)
            mmt = mmtCheckers;
        else
            mmt = mmtChess;

        startMethodTracing();
        switch (type) {
            case EASY:
                doRandomRobot(game, root);
                break;
            case MEDIUM: {
                mmt.buildTree(game, root, 3);
                break;
            }
            case HARD: {
                mmt.buildTree(game, root, 5);
                break;
            }
        }
        stopMethodTracing();
    }

    protected void startMethodTracing() {}

    protected void stopMethodTracing() {}


    protected long evaluateChessBoard(Chess game, DescisionTree node, int playerNum) {

        int dPcCount = 0;
        int dPcValue = 0;
        int dPawnAdv = 0;

        final int [][] bValue = new int[8][8];

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
                Piece p = game.board[rank][col];//getPiece(rank, col);

                if (p.getPlayerNum() < 0)
                    continue;

                final int scale = p.getPlayerNum() == playerNum ? 1 : -1;

                int value = 0;

                switch (p.getType()) {
                    case PAWN:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.getPlayerNum()));
                        value = 1;
                        break;
                    case PAWN_IDLE:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.getPlayerNum()));
                        value = 1;
                        break;
                    case PAWN_ENPASSANT:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.getPlayerNum()));
                        value = 1;
                        break;
                    case PAWN_TOSWAP:
                        dPawnAdv += scale * Math.abs(rank-game.getStartRank(p.getPlayerNum()));
                        value = 1000;
                        break;
                    case BISHOP:
                        value = 3;
                        break;
                    case KNIGHT:
                        value = 3;
                        break;
                    case ROOK:
                        value = 5;
                        break;
                    case ROOK_IDLE:
                        value = 5;
                        break;
                    case QUEEN:
                        value = 8;
                        break;
                    case CHECKED_KING:
                        value = -2;
                        break;
                    case CHECKED_KING_IDLE:
                        value = -1;
                        break;
                    case UNCHECKED_KING:
                        value = 0;
                        break;
                    case UNCHECKED_KING_IDLE:
                        value = 1;
                        break;
                    default:
                        Utils.assertTrue(false);
                        continue;
                }

                dPcCount += scale;
                dPcValue += scale * value;

                if (game.isSquareAttacked(rank, col, p.getPlayerNum())) {
                    bValue[rank][col] += value;
                }
                if (game.isSquareAttacked(rank, col, game.getOpponent(p.getPlayerNum()))) {
                    bValue[rank][col] -= value;
                }


            }
        }

        long dAttackMatrix = 0;
        for (int i=0; i<8; i++) {
            dAttackMatrix += Utils.sum(bValue[i]);
        }
        long d  = dPawnAdv * 100
                //+dPcCount * 1000
                 +dPcValue * 10000
                 //+dAttackMatrix * 10
                ;
        if (node != null) {
            node.appendMeta(String.format(
                    //"%1$20s:%2$d
                    "(%3$d)\n"
                            + "%4$s:%5$d\n"
                            + "%6$s:%7$d\n"
                            + "%8$s:%9$d\n"
                            + "%10$20s:%11$d\n"
//                            + "%12$20s:%13$d\n"
//                            + "%14$20s:%15$d\n"
                    ,"Player", game.getTurn(), d
                    ,"dPcCount ", dPcCount
                    ,"dPcValue ", dPcValue
                    ,"dPawnAdv ", dPawnAdv
                    ,"dAttackMatrix", dAttackMatrix
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
    protected long evaluateCheckersBoard(Checkers game, DescisionTree node, int playerNum) {

        int dPc=0;
        int dKing=0;
        int dAdv=0;
        int moves=0;

        for (int rank=0; rank<game.RANKS; rank++) {
            for (int col=0; col<game.COLUMNS; col++) {
            //for (Piece p : game..getBoard[rank]) {
                Piece p = game.board[rank][col];//getPiece(rank, col);
                Utils.assertTrue(p != null && p.getType() != null);

                if (p.getPlayerNum() == playerNum) {
                    switch (p.getType()) {
                        case CHECKER:
                            dPc++;
                            dAdv += game.getAdvancementFromStart(p.getPlayerNum(), rank);
                            break;
                        case FLYING_KING:
                        case KING:
                            dKing++; break;
                    }

                } else if (p.getPlayerNum() >= 0) {
                    switch (p.getType()) {
                        case CHECKER:
                            dPc--;
                            dAdv -= game.getAdvancementFromStart(p.getPlayerNum(), rank);
                            break;
                        case FLYING_KING:
                        case KING:
                            dKing--; break;
                    }
                }
            }
        }

        moves = game.computeMoves();
        long d = 100*dPc + 1000*dKing + 10*dAdv + moves;

        if (node != null) {
            node.appendMeta(String.format(
                              //"%1$20s:%2$d
                              "(%3$d)\n"
                            + "%4$s:%5$d\n"
                            + "%6$s:%7$d\n"
                            + "%8$s:%9$d\n"
                            + "%10$s:%11$d\n"
//                            + "%12$20s:%13$d\n"
//                            + "%14$20s:%15$d\n"
                    ,
                    "Player", game.getTurn(), d,
                    "dPcs  ", dPc,
                    "dKings", dKing,
                    "dAdv  ", dAdv,
                    "moves ", moves
            ));
        }

        d += (Utils.rand() % 10 - 5); // add a fudge factor to keep AI from doing same move over and over

        //Log.d("AI", "Board evaluates too: " + d);
        return d;
    }

    private void doRandomRobot(ACheckboardGame game, DescisionTree<Move> tree) {
        int n = game.computeMoves();
        if (n > 0) {
            int mvNum = Utils.rand() % n;
            Piece p;
            for (int i = 0; i < game.RANKS; i++) {
                for (int ii = 0; ii < game.COLUMNS; ii++) {
                    if ((p = game.board[i][ii]).getNumMoves() > mvNum) {
                        Move m = p.getMove(mvNum);
                        tree.setMove(m);
                    } else {
                        mvNum -= p.getNumMoves();
                    }
                }
            }
        }
    }

}
