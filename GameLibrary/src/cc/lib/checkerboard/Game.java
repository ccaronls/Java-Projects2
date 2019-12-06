package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class Game extends Reflector<Game> implements IGame<Move> {

    static {
        addAllFields(Game.class);
    }

    public enum GameState {
        PLAYING(false), NEAR_WINS(true), FAR_WINS(true), DRAW(true);

        final boolean gameOver;
        GameState(boolean gameOver) {
            this.gameOver = gameOver;
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
    private GameState gameState = GameState.PLAYING;

    void init(int ranks, int columns) {
        initialized = false;
        this.ranks = ranks;
        this.cols = columns;
        board = new Piece[ranks][cols];
    }

    void clear() {
        for (int r=0; r<getRanks(); r++) {
            for (int c=0; c<getColumns(); c++) {
                board[r][c] = new Piece(r, c, -1, PieceType.EMPTY);
            }
        }
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

    public Piece getSelectedPiece() {
        if (selectedPiece == null)
            return null;
        return getPiece(selectedPiece);
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
        moves = null;
        rules.init(this);
        int num = 0;
        for (Player p : players) {
            p.playerNum = num++;
            p.color = rules.getPlayerColor(p.playerNum);
            p.newGame();
        }
        gameState = GameState.PLAYING;
    }

    public void runGame() {
        switch (gameState) {
            case DRAW:
                onGameOver(null);
                return;
            case FAR_WINS:
                onGameOver(players[FAR]);
                return;
            case NEAR_WINS:
                onGameOver(players[NEAR]);
                return;
        }
        if (rules.isDraw(this)) {
            gameState = GameState.DRAW;
            onGameOver(null);
            return;
        }
        switch (rules.getWinner(this)) {
            case NEAR:
                gameState = GameState.NEAR_WINS;
                onGameOver(getPlayer(NEAR));
                return;
            case FAR:
                gameState = GameState.FAR_WINS;
                onGameOver(getPlayer(FAR));
                return;
        }

        List<Move> moves = getMoves();
        if (moves.size() == 0) {
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
                executeMove(m);
                onMoveChosen(m);
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

    public final boolean isGameOver() {
        return gameState.gameOver;
    }

    protected void onPieceSelected(Piece p) {}

    protected void onMoveChosen(Move m) {}

    /**
     * @param winner draw game if null
     */
    protected void onGameOver(Player winner) {}

    @Override
    public final void executeMove(Move move) {
        if (move.getPlayerNum() != getTurn())
            throw new AssertionError("Invalid move to execute");
        undoStack.push(new State(moves.indexOf(move), moves));
        moves = null;
        clearPieceMoves();
        rules.executeMove(this, move);
        if (moves != null) {
            countPieceMoves();
        }
    }

    public final boolean canUndo() {
        return undoStack.size() > 0;
    }

    private void clearPieceMoves() {
        for (int rank = 0; rank < ranks; rank++) {
            for (int col = 0; col < cols; col++) {
                board[rank][col].numMoves = 0;
            }
        }
    }

    private void countPieceMoves() {
        clearPieceMoves();
        for (Move m : moves) {
            if (m.getStart() != null) {
                getPiece(m.getStart()).numMoves++;
            }
        }
    }

    @Override
    public final Move undo() {
        if (undoStack.size() > 0) {
            State state = undoStack.pop();
            Move m = state.getMove();
            moves = state.moves;
            rules.reverseMove(this, m);
            selectedPiece = null;
            turn = moves.get(0).getPlayerNum();
            countPieceMoves();
            gameState = GameState.PLAYING;
            return m;
        }
        return null;
    }

    @Override
    public final List<Move> getMoves() {
        if (moves == null) {
            moves = rules.computeMoves(this);
            countPieceMoves();
        }
        return moves;
    }

    final List<Move> getMovesInternal() {
        if (moves == null)
            moves = new ArrayList<>();
        return moves;
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
     *
     */
    public final void forfeit(int playerNum) {
        switch (playerNum) {
            case NEAR:
                gameState = GameState.NEAR_WINS;
                break;
            case FAR:
                gameState = GameState.FAR_WINS;
                break;
            default:
                throw new IllegalArgumentException("Invalid parameter " + playerNum);
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
        setPiece(m.getEnd(), m.getPlayerNum(), m.getEndType());
        clearPiece(m.getStart());
        return getPiece(m.getEnd());
    }

    final void clearPiece(int [] pos ) {
        board[pos[0]][pos[1]].setPlayerNum(-1);
        board[pos[0]][pos[1]].setType(PieceType.EMPTY);
    }

    final void setPiece(int rank, int col, int playerNum, PieceType p) {
        board[rank][col].setType(p);
        board[rank][col].setPlayerNum(playerNum);
    }

    final void setPiece(int [] pos, int playerNum, PieceType p) {
        setPiece(pos[0], pos[1], playerNum, p);
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
        switch (gameState) {

            case PLAYING:
                break;
            case NEAR_WINS:
                return players[NEAR];
            case FAR_WINS:
                return players[FAR];
            case DRAW:
                break;
        }
        return null;
    }

    /**
     * Returns history of moves with most recent move (last move) in the first list position.
     * In other words, iterating over the history in order will rewind the game.
     * @return
     */
    public final List<Move> getMoveHistory() {
        List<Move> history = new ArrayList<>();
        for (State st : undoStack) {
            history.add(st.getMove());
        }
        Collections.reverse(history);
        return history;
    }

    /**
     *
     * @return
     */
    public final Move getMostRecentMove() {
        if (undoStack.size() > 0)
            return undoStack.peek().getMove();
        return null;
    }

    String getTurnStr(int turn) {
        if (turn == NEAR)
            return "NEAR";
        if (turn == FAR)
            return "FAR";
        return "UNKNOWN(" + turn + ")";
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(getTurnStr(getTurn())).append(":");
        Player pl = getPlayer(getTurn());
        if (pl != null)
            s.append(pl.getColor());
        s.append("(").append(getTurn()).append(")");
        s.append("\n   ");
        for (int c=0; c<getColumns(); c++) {
            s.append(String.format("%3d ", c));
        }
        s.append("\n   ");
        for (int r=0; r<getRanks(); r++) {
            for (int c=0; c<getColumns(); c++) {
                s.append("+---");
            }
            s.append(String.format("+\n%-3d", r));
            for (int c=0; c<getColumns(); c++) {
                s.append("|");
                Piece p = getPiece(r, c);
                if (p == null) {
                    s.append("nil");
                } else if (p.getPlayerNum() < 0) {
                    if (p.getType() != PieceType.EMPTY) {
                        s.append(" X ");
                    } else {
                        s.append("   ");
                    }
                } else {
                    pl = getPlayer(p.getPlayerNum());
                    if (pl == null) {
                        s.append(p.getPlayerNum());
                    } else {
                        s.append(pl.getColor().name().charAt(0));
                    }
                    s.append(p.getType().abbrev);
                }

            }
            s.append("|\n   ");
        }
        for (int c=0; c<getColumns(); c++) {
            s.append("+---");
        }
        s.append("+\n");
        return s.toString();
    }

}
