package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Game extends Reflector<Game> implements IGame<Move> {

    static {
        addAllFields(Game.class);
        addAllFields(State.class);
    }

    public final static class State {
        final int index;
        final List<Move> moves;

        public State() {
            this(-1, null);
        }

        public State(int index, List<Move> moves) {
            this.index = index;
            this.moves = moves;
        }

        Move getMove() {
            return moves.get(index);
        }
    }

    public final static int NEAR = 0;
    public final static int FAR  = 1;

    private final Player [] players = new Player[2];
    private Piece [][] board;
    private int ranks, cols, turn;
    int [] selectedPiece = null;
    private List<Move> moves = null;
    private Stack<State> undoStack = new Stack<>();
    private Rules rules;
    private boolean initialized = false;

    void init(int ranks, int columns) {
        initialized = false;
        this.ranks = ranks;
        this.cols = columns;
        board = new Piece[ranks][cols];
    }

    protected void initRank(int rank, int player, PieceType...pieces) {
        for (int i=0; i<pieces.length; i++) {
            Piece p = board[rank][i] = new Piece(rank, i, player, pieces[i]);
            if (p.getType() == PieceType.EMPTY) {
                p.setPlayerNum(-1);
            }
        }
        if (rank == board.length-1)
            initialized = true;
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
        initialized = false;
        selectedPiece = null;
        rules.init(this);
        int num = 0;
        for (Player p : players) {
            p.playerNum = num++;
            p.color = rules.getPlayerColor(p.playerNum);
            p.newGame();
        }
    }

    public void runGame() {
        Player winner = getWinner();
        if (winner != null) {
            onGameOver(winner);
            return;
        }

        if ((winner = rules.getWinner(this)) != null) {
            onGameOver(winner);
            return;
        }

        if (moves == null) {
            moves = rules.computeMoves(this);
        }
        if (moves.size() == 0) {
            getOpponentPlayer().winner = true;
            onGameOver(getWinner());
            return;
        }
        if (selectedPiece == null) {
            List<Piece> movable = getMovablePieces();
            if (movable.size() == 1) {
                selectedPiece = movable.get(0).getPosition();
            }
        }
        if (selectedPiece == null) {
            Piece p = players[turn].choosePieceToMove(this, getMovablePieces());
            if (p != null) {
                selectedPiece = p.getRankCol();
                onPieceSelected(p);
            }
        } else {
            Move m = players[turn].chooseMoveForPiece(this, getMovesforPiece(selectedPiece));
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
        if (!isOnBoard(rank, col))
            throw new AssertionError();
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

    private List<Move> getMovesforPiece(int [] pos) {
        List<Move> pm = new ArrayList<>();
        for (Move m : moves) {
            if (m.getStart() != null && m.getStart()[0] == pos[0] && m.getStart()[1] == pos[1]) {
                pm.add(m);
            }
        }
        return pm;
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

    @Override
    public final void executeMove(Move move) {
        undoStack.push(new State(moves.indexOf(move), moves));
        moves = null;
        rules.executeMove(this, move);
    }

    public final boolean canUndo() {
        return undoStack.size() > 0;
    }

    @Override
    public Move undo() {
        if (undoStack.size() > 0) {
            State state = undoStack.pop();
            Move m = state.getMove();
            moves = state.moves;
            rules.reverseMove(this, m);
            selectedPiece = null;
            return m;
        }
        return null;
    }

    Move getPreviousMove() {
        if (undoStack.size() > 0) {
            return undoStack.peek().getMove();
        }
        return null;
    }

    @Override
    public final List<Move> getMoves() {
        return moves;
    }

    void addMove(Move m) {
        if (moves == null)
            moves = new ArrayList<>();
        moves.add(m);
    }

    @Override
    public final int getTurn() {
        return turn;
    }

    void nextTurn() {
        turn = (turn+1) % players.length;
        moves = null;
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
        setPiece(m.getEnd(), m.getPlayerNum(), m.getEndType());
        clearPiece(m.getStart());
        return getPiece(m.getEnd());
    }

    final void clearPiece(int [] pos ) {
        board[pos[0]][pos[1]].setPlayerNum(-1);
        board[pos[0]][pos[1]].setType(PieceType.EMPTY);
    }

    final void setPiece(int [] pos, int playerNum, PieceType p) {
        board[pos[0]][pos[1]].setType(p);
        board[pos[0]][pos[1]].setPlayerNum(playerNum);
    }

    public final int getRanks() {
        return ranks;
    }

    public final int getColumns() {
        return cols;
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public final Rules getRules() {
        return rules;
    }

    public Player getWinner() {
        for (int i=0; i<players.length; i++) {
            if (players[i].isWinner())
                return players[i];
        }
        return null;
    }


}
