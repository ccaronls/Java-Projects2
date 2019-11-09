package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Game extends Reflector<Game> implements IGame<Move> {

    static {
        addAllFields(Game.class);
    }

    public final static int NEAR = 0;
    public final static int FAR  = 1;

    final Player [] players = new Player[2];
    Piece [][] board;
    int ranks, cols, turn;
    int [] selectedPiece = null;
    Stack<Move> undoStack = new Stack<>();
    Rules rules;

    void init(int ranks, int columns) {
        this.ranks = ranks;
        this.cols = columns;
        board = new Piece[ranks][cols];
    }

    protected void initRank(int rank, int player, PieceType...pieces) {
        for (int i=0; i<pieces.length; i++) {
            Piece p = board[rank][i] = new Piece(rank, i, player, pieces[i]);
            switch (p.getType()) {
                case EMPTY:
                case UNAVAILABLE:
                    p.setPlayerNum(-1);
                    break;
            }
        }
    }


    public void setPlayer(int position, Player p) {
        players[position] = p;
        p.color = rules.getPlayerColor(position);
        p.playerNum = position;
    }

    public final int getOpponent() {
        return getOpponent(getTurn());
    }

    public final int getOpponent(int player) {
        if (player == NEAR)
            return FAR;
        if (player == FAR)
            return NEAR;
        throw new AssertionError();
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    public void newGame() {
        undoStack.clear();
        selectedPiece = null;
        rules.init(this);
        for (Player p : players) {
            p.newGame();
        }
    }

    public void runGame() {
        if (selectedPiece == null) {
            if (rules.computeMoves(this, true) == 0) {
                onGameOver(rules.getWinner(this));
                return;
            }
            List<Piece> movable = getMovablePieces();
            Piece p = players[turn].choosePieceToMove(this, movable);
            if (p != null) {
                selectedPiece = p.getRankCol();
                onPieceSelected(p);
            }
        } else {
            Move m = players[turn].chooseMoveForPiece(this, getPiece(selectedPiece).getMovesList());
            if (m != null) {
                onMoveChosen(m);
                executeMove(m);
            }
            selectedPiece = null;
        }
    }

    public Piece getPiece(int [] pos) {
        return board[pos[0]][pos[1]];
    }

    public final Piece getPiece(int rank, int col) {
        return board[rank][col];
    }

    final void setTurn(int turn) {
        this.turn = turn;
    }

    public final boolean isOnBoard(int rank, int col) {
        return rank >= 0 && col >= 0 && rank < ranks && col < cols;
    }

    private List<Piece> getMovablePieces() {
        List<Piece> pieces = new ArrayList<>();
        for (int i=0; i<board.length; i++) {
            for (int ii=0; ii<board[0].length; ii++) {
                Piece p = board[i][ii];
                if (p.getNumMoves() > 0) {
                    pieces.add(p);
                }
            }
        }
        return pieces;
    }

    public final Player getPlayer(int side) {
        return players[side];
    }

    public final Player getCurrentPlayer() {
        return players[turn];
    }

    public final Player getOpponentPlayer() {
        return players[getOpponent()];
    }

    protected void onPieceSelected(Piece p) {}

    protected void onMoveChosen(Move m) {}

    protected void onGameOver(Player winner) {}

    public void onPieceCaptured(int [] pos, PieceType type) {}

    public void onPieceUncaptured(int [] pos, PieceType type) {}

    @Override
    public final void executeMove(Move move) {
        rules.executeMove(this, move);
    }

    public final boolean canUndo() {
        return undoStack.size() > 0;
    }

    @Override
    public final Move undo() {
        if (undoStack.size() > 0) {
            Move m = null;
            synchronized (undoStack) {
                m = undoStack.pop();
            }
            rules.reverseMove(this, m, true);
            return m;
        }
        return null;
    }

    @Override
    public final Iterable<Move> getMoves() {
        List<Move> moves = new ArrayList<>();
        for (Piece p : getMovablePieces()) {
            moves.addAll(p.getMovesList());
        }
        return moves;
    }

    @Override
    public final int getTurn() {
        return turn;
    }

    void nextTurn() {
        turn = (turn+1) % players.length;
        clearMoves();
    }

    public final void clearMoves() {
        for (int i = 0; i < board.length; i++) {
            for (int ii = 0; ii < board[0].length; ii++) {
                board[i][ii].clearMoves();
            }
        }
    }

    /**
     * This is the rank closest to the given side
     *
     * @param side
     * @return
     */
    public final int getStartRank(int side) {
        switch (side) {
            case FAR:
                return 0;
            case NEAR:
                return ranks-1;
        }
        Utils.assertTrue(false, "Unexpected side " + side);
        return -1;
    }

    /**
     * Get how many units of advancement the rank is for a side
     *
     * @param side
     * @param rank
     * @return
     */
    public final int getAdvancementFromStart(int side, int rank) {
        return getStartRank(side) + rank*getAdvanceDir(side);
    }

    /**
     * This is the rank increment based on side
     * @param side
     * @return
     */
    public final int getAdvanceDir(int side) {
        switch (side) {
            case FAR:
                return 1;
            case NEAR:
                return -1;
        }
        Utils.assertTrue(false, "Unexpected side " + side);
        return 0;
    }

    public Piece movePiece(Move m) {
        Piece p = getPiece(m.getStart());
        setBoard(m.getEnd(), p);
        clearPiece(m.getStart());
        return p;
    }

    final void setBoard(int [] pos, Piece p) {
        board[pos[0]][pos[1]] = p;
        p.setRankCol(pos);
    }

    final void clearPiece(int [] pos ) {
        board[pos[0]][pos[1]] = new Piece(pos, -1, PieceType.EMPTY);
    }

    final void setPiece(int [] pos, int playerNum, PieceType p) {
        board[pos[0]][pos[1]].setType(p);
        board[pos[0]][pos[1]].setPlayerNum(playerNum);
    }
}
