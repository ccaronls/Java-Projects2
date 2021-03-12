package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

public class Game extends Reflector<Game> implements IGame<Move>, Iterable<Piece> {

    static {
        addAllFields(Game.class);
    }

    public enum GameState {
        PLAYING(false, -1), NEAR_WINS(true, NEAR), FAR_WINS(true, FAR), DRAW(true, -1);

        final boolean gameOver;
        final int playerNum;

        GameState(boolean gameOver, int playerNum) {
            this.gameOver = gameOver;
            this.playerNum = playerNum;
        }
    }

    public final static int NEAR = 0;
    public final static int FAR  = 1;

    private final Player [] players = new Player[2];
    Piece [][] board;
    private int ranks, cols, turn;
    int [] selectedPiece = null;
    List<Move> moves = null;
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
        gameState = GameState.PLAYING;
        moves = null;
        selectedPiece = null;
        undoStack.clear();
        initialized = false;
    }

    /**
     *
     * @param rank
     * @param player
     * @param pieces
     */
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

    /**
     *
     * @param position
     * @param p
     */
    public void setPlayer(int position, Player p) {
        players[position] = p;
        p.color = rules.getPlayerColor(position);
        p.playerNum = position;
    }

    /**
     *
     * @return
     */
    public final int getOpponent() {
        return getOpponent(getTurn());
    }

    /**
     *
     * @return
     */
    public Piece getSelectedPiece() {
        if (selectedPiece == null)
            return null;
        return getPiece(selectedPiece);
    }

    /**
     *
     * @param player
     * @return
     */
    public static int getOpponent(int player) {
        if (player == NEAR)
            return FAR;
        if (player == FAR)
            return NEAR;
        throw new GException();
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    /**
     * Called after players have been assigned
     */
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
        refreshGameState();
    }

    /**
     * Called repeatedly after newGame
     */
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

    /**
     *
     * @param pos
     * @return
     */
    public final Piece getPiece(int [] pos) {
        return board[pos[0]][pos[1]];
    }

    /**
     *
     * @param rank
     * @param col
     * @return
     */
    public final Piece getPiece(int rank, int col) {
        //if (!isOnBoard(rank, col)) {
        //    throw new AssertionError("Not on board [" + rank + "," + col + "]");
       // }
        return board[rank][col];
    }

    public Iterable<Piece> getPieces() {
        return this;
    }

    public Iterator<Piece> iterator() {
        return new PieceIterator();
    }

    class PieceIterator implements Iterator<Piece> {

        int rank=0;
        int col=0;
        Piece next = null;

        void reset() {
            rank=0;
            col=0;
            next = null;
        }

        @Override
        public boolean hasNext() {
            next = null;
            for ( ; rank<getRanks(); rank++) {
                for ( ; col < getColumns(); col++) {
                    if (board[rank][col].getType().flag != 0) {
                        next = board[rank][col++];
                        return true;
                    }
                }
                col=0;
            }
            return false;
        }

        @Override
        public Piece next() {
            return next;
        }
    }

    /**
     *
     * @param turn
     */
    public final void setTurn(int turn) {
        this.turn = turn;
    }

    /**
     *
     * @param rank
     * @param col
     * @return
     */
    public final boolean isOnBoard(int rank, int col) {
        if (rank < 0 || col < 0 || rank >= ranks || col >= cols)
            return false;
        return board[rank][col].type != PieceType.BLOCKED;
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

    /**
     *
     * @param side
     * @return
     */
    public final Player getPlayer(int side) {
        return players[side];
    }

    /**
     *
     * @return
     */
    public final Player getCurrentPlayer() {
        return players[turn];
    }

    /**
     *
     * @return
     */
    public final Player getOpponentPlayer() {
        return players[getOpponent()];
    }

    /**
     *
     * @return
     */
    public final boolean isGameOver() {
        return gameState.gameOver;
    }

    /**
     *
     * @param p
     */
    protected void onPieceSelected(Piece p) {}

    /**
     *
     * @param m
     */
    protected void onMoveChosen(Move m) {}

    /**
     * @param winner draw game if null
     */
    protected void onGameOver(Player winner) {}

    static int counter = 0;

    final static boolean DEBUG = false;

    @Override
    public final synchronized void executeMove(Move move) {
        if (move.getPlayerNum() != getTurn())
            throw new GException("Invalid move to execute");
        State state = new State(moves.indexOf(move) // <-- TODO: Make this faster
                , moves);
        if (rules instanceof Chess) {
            state.enpassants = new ArrayList<>();
            for (Piece p : getPieces()) {
                int rank = p.getRank();
                int col = p.getCol();
                if (move.getStart()[0] != rank && move.getStart()[1] != col) {
                    if (p.getPlayerNum() == getTurn() && p.getType() == PieceType.PAWN_ENPASSANT) {
                        state.enpassants.add(new int[]{rank, col});
                    }
                }
            }
        }
        Utils.assertTrue(undoStack.size() < 1024);

        undoStack.push(state);
        //
        moves = null;
        clearPieceMoves();
        if (DEBUG)
            System.out.println("COUNTER:" + (counter++) + "\nGAME BEFORE MOVE: " + move + "\n" + this);
        rules.executeMove(this, move);
        if (state.enpassants != null) {
            for (int [] pos : state.enpassants) {
                getPiece(pos).setType(PieceType.PAWN);
            }
        }
        if (moves != null) {
            countPieceMoves();
        }
        String gameAfter = null;
        if (DEBUG) {
            gameAfter = toString();
            System.out.println("GAME AFTER:\n" + gameAfter);
        }
        refreshGameState();
        if (DEBUG) {
            String gameAfter2 = toString();
            System.out.println("GAME STATE AFTER REFRESH:\n" + gameAfter2);
            if (!gameAfter.equals(gameAfter2))
                throw new GException("Logic Error: Game State changed after refresh");
        }
    }

    public final void refreshGameState() {
        if (rules.isDraw(this)) {
            gameState = GameState.DRAW;
        } else switch (rules.getWinner(this)) {
            case NEAR:
                gameState = GameState.NEAR_WINS;
                break;
            case FAR:
                gameState = GameState.FAR_WINS;
                break;
            default:
                gameState = GameState.PLAYING;
        }
    }

    /**
     *
     * @return
     */
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

    int countPieceMoves() {
        int totalMoves = 0;
        if (moves != null) {
            clearPieceMoves();
            for (Move m : moves) {
                if (m.getStart() != null) {
                    getPiece(m.getStart()).numMoves++;
                    totalMoves++;
                }
            }
        }
        return totalMoves;
    }

    @Override
    public final synchronized Move undo() {
        if (undoStack.size() > 0) {
            State state = undoStack.pop();
            Move m = state.getMove();
            moves = state.moves;
            rules.reverseMove(this, m);
            selectedPiece = null;
            //turn = moves.get(0).getPlayerNum();
            countPieceMoves();
            if (state.enpassants != null) {
                for (int [] pos : state.enpassants) {
                    Piece p = getPiece(pos);
                    if (p.getType() != PieceType.PAWN)
                        throw new GException("Logic Error: Not a pawn: " + p);
                    getPiece(pos).setType(PieceType.PAWN_ENPASSANT);
                }
            }
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
     * @param playerNum NEAR, FAR or -1 for DRAWGAME
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
                gameState = GameState.DRAW;
                break;
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
        throw new GException("Logic Error: not a valid side: " + side);
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
        throw new GException("Logic Error: not a valid side: " + side);
    }

    public Piece movePiece(Move m) {
        Piece p;
        if ((p=getPiece(m.getStart())).getPlayerNum() != getTurn())
            throw new GException("Logic Error: Not player " + p.getPlayerNum() + "'s turn: " + getTurn() + "'s turn. For Piece:\n" + p);
        if (p.getType()==PieceType.EMPTY)
            throw new GException("Logic Error: Moving an empty piece. For Move:\n" + m);
        copyPiece(m.getStart(), m.getEnd());
        clearPiece(m.getStart());
        p = getPiece(m.getEnd());
        p.setType(m.getEndType());
        return p;
    }

    final void clearPiece(int rank, int col) {
        board[rank][col].clear();
    }

    final void clearPiece(int [] pos ) {
        clearPiece(pos[0], pos[1]);
    }

    final void setPiece(int rank, int col, int playerNum, PieceType p) {
        board[rank][col].setType(p);
        board[rank][col].setPlayerNum(playerNum);
    }

    final void setPiece(int [] pos, int playerNum, PieceType p) {
        setPiece(pos[0], pos[1], playerNum, p);
    }
    final void copyPiece(Piece from, Piece to) {
        to.copyFrom(from);
    }

    final void copyPiece(int [] fromPos, int [] toPos) {
        Piece from = getPiece(fromPos);
        Piece to   = getPiece(toPos);
        copyPiece(from, to);
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

    public final void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public final Rules getRules() {
        return rules;
    }

    public final Player getWinner() {
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
     *
     * @return
     */
    public final int getWinnerNum() {
        return gameState.playerNum;
    }

    /**
     *
     * @return
     */
    public final boolean isDraw() {
        return gameState == GameState.DRAW;
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
    public final int getMoveHistoryCount() {
        return undoStack.size();
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

    public final List<PieceType> getCapturedPieces(int playerNum) {
        List<PieceType> captured = new ArrayList<>();
        synchronized (undoStack) {
            for (State s : undoStack) {
                Move m = s.getMove();
                if (m.hasCaptured() && m.getPlayerNum() == playerNum) {
                    for (int i=0; i<m.getNumCaptured(); i++) {
                        captured.add(m.getCapturedType(i));
                    }
                }
            }
        }
        return captured;
    }
    @Override
    public final String toString() {
        StringBuffer s = new StringBuffer(gameState.name());
        int turn = getTurn();
        switch (gameState) {
            case PLAYING:
                break;
            case NEAR_WINS:
                turn = NEAR;
                break;
            case FAR_WINS:
                turn = FAR;
                break;
            case DRAW:
                break;
        }
        s.append("(").append(turn).append(") ").append(getTurnStr(turn));
        if (undoStack.size() > 0) {
            State state = undoStack.peek();
            if (state.enpassants != null) {
                for (int [] pos : state.enpassants) {
                    s.append("[").append(pos[0]).append(",").append(pos[1]).append("] ");
                }
            }
        }
        for (int ii=0; ii<2; ii++, turn = getOpponent(turn)) {
            Player pl = getPlayer(turn);
            s.append("\n");
            if (pl != null)
                s.append(pl.getColor());
            else
                s.append("<INVESTIGATE: NULL COLOR>");
            s.append("(").append(turn).append(")");
            List<PieceType> captured = getCapturedPieces(turn);
            if (captured.size() > 0) {
                s.append(" cap:");
                int[] counts = new int[PieceType.values().length];
                for (PieceType pt : captured) {
                    counts[pt.getDisplayType().ordinal()]++;
                }
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] == 0)
                        continue;
                    if (counts[i] == 1)
                        s.append(PieceType.values()[i].abbrev).append(" ");
                    else
                        s.append(PieceType.values()[i].abbrev).append(" X ").append(counts[i]).append(" ");
                }
            }
        }
        s.append("\n   ");
        for (int c=0; c<getColumns(); c++) {
            s.append(String.format("%3d ", c));
        }
        s.append("\n   ");
        for (int r=0; r<getRanks(); r++) {
            for (int c=0; c<getColumns(); c++) {
                s.append("+---");
            }
            s.append(String.format("+\n%2d ", r));
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
                    Player pl = getPlayer(p.getPlayerNum());
                    if (pl == null) {
                        s.append(p.getPlayerNum());
                    } else {
                        s.append(pl.getColor().name().charAt(0));
                    }
                    if (p.isCaptured()) {
                        s.append(p.getType().abbrev.charAt(0) + "*");
                    } else {
                        s.append(p.getType().abbrev);
                    }
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

    public final boolean isBoardsEqual(Game other) {
        if (getTurn() != other.getTurn())
            return false;
        if (getColumns() != other.getColumns() || getRanks() != other.getRanks())
            return false;
        for (int r = 0; r<other.getRanks(); r++) {
            for (int c = 0; c<other.getColumns(); c++) {
                Piece p0, p1;
                if (!(p0=other.getPiece(r, c)).equals((p1=other.getPiece(r, c)))) {
                    //System.out.println("Piece at position " + r + "," + c + " differ: " + p0 + "\n" + p1);
                    return false;
                }
            }
        }
        return true;
    }
}
