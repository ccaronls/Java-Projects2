package cc.lib.checkerboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Reflector;

public class Game extends Reflector<Game> implements IGame<Move> {

    static final Logger log = LoggerFactory.getLogger(Game.class);

    static int counter = 0;

    private final static boolean DEBUG = false;

    static {
        addAllFields(Game.class);
    }

    public enum GameState {
        PLAYING(false, -1),
        NEAR_WINS(true, NEAR),
        FAR_WINS(true, FAR),
        DRAW(true, -1);

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
    int selectedPiece = -1;
    List<Move> moves = null;
    private Stack<State> undoStack = new Stack<>();
    private Rules rules;
    private boolean initialized = false;
    private GameState gameState = GameState.PLAYING;
    private int [] numPieces = null;

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
        selectedPiece = -1;
        undoStack.clear();
        initialized = false;
        numPieces = null;
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
            if (p.getType().flag == 0) {
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
        if (selectedPiece < 0)
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
        selectedPiece = -1;
        moves = null;
        rules.init(this);
        int num = 0;
        for (Player p : players) {
            p.playerNum = num++;
            p.color = rules.getPlayerColor(p.playerNum);
            p.newGame();
        }
        countPieces();
        refreshGameState();
    }

    void countPieces() {
        log.debug("Counting pieces");
        numPieces = new int[2];
        for (int i=0; i<ranks; i++) {
            for (int ii=0; ii<cols; ii++) {
                if (!isOnBoard(i, ii))
                    continue;
                Piece p = getPiece(i, ii);
                if (p.getPlayerNum() >= 0) {
                    numPieces[p.getPlayerNum()]++;
                }
            }
        }
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

        if (selectedPiece < 0) {
            List<Piece> movable = getMovablePieces();
            if (movable.size() == 1) {
                selectedPiece = movable.get(0).getPosition();
            }
        }
        if (selectedPiece < 0) {
            Piece p = players[turn].choosePieceToMove(this, getMovablePieces());
            if (p != null) {
                selectedPiece = p.getPosition();
                onPieceSelected(p);
                return; // we dont want to block twice in one 'runGame'
            }
        } else {
            Move m = players[turn].chooseMoveForPiece(this, getMovesforPiece(selectedPiece));
            if (m != null) {
                onMoveChosen(m);
                executeMove(m);
            }
            selectedPiece = -1;
        }
    }

    /**
     *
     * @param pos
     * @return
     */
    public final Piece getPiece(int pos) {
        return board[pos>>8][pos&(0xff)];
    }

    /**
     *
     * @param rank
     * @param col
     * @return
     */
    public final Piece getPiece(int rank, int col) {
        //if (!isOnBoard(rank, col)) {
        //    throw new GException("Not on board [" + rank + "," + col + "]");
       // }
        return board[rank][col];
    }

    private void validatePiceCounts() {
        int [] numPieces = new int[2];
        for (int i=0; i<ranks; i++) {
            for (int ii=0; ii<cols; ii++) {
                if (!isOnBoard(i, ii))
                    continue;
                Piece p = getPiece(i, ii);
                if (p.getPlayerNum() >= 0) {
                    numPieces[p.getPlayerNum()]++;
                }
            }
        }

        if (this.numPieces[0] != numPieces[0]) {
            log.error("Piece count wrong");

        }
        if (this.numPieces[1] != numPieces[1]) {
            log.error("Piece count wrong");
        }
    }

    public Iterable<Piece> getPieces(final int playerNum) {
        if (numPieces == null)
            countPieces();
        else if (DEBUG) {
            validatePiceCounts();
        }

        //log.debug("Piece count for player %d = %d", 0, numPieces[0]);
        //log.debug("Piece count for player %d = %d", 1, numPieces[1]);

        return () -> {
            int num = 0;
            int pNum = playerNum;
            int startRank = 0;
            int advDir = 1;
            if  (pNum == NEAR) {
                num = numPieces[NEAR];
                pNum = FAR;
                startRank = getStartRank(playerNum);
                advDir = getAdvanceDir(playerNum);
            } else if (playerNum == FAR) {
                num = numPieces[FAR];
                pNum = NEAR;
                startRank = getStartRank(playerNum);
                advDir = getAdvanceDir(playerNum);
            } else {
                num = numPieces[NEAR] + numPieces[FAR];
            }

            return new PieceIterator(pNum, startRank, advDir, num);
        };
    }

    class PieceIterator implements Iterator<Piece> {

        int rank=0;
        int col=0;
        int advDir;
        int notPlayerNum;
        int ranks;
        int num;
        Piece next = null;

        PieceIterator(int notPlayerNum, int startRank, int advDir, int maxPieces) {
            ranks = getRanks();
            rank = startRank;
            this.notPlayerNum = notPlayerNum;
            this.advDir = advDir;
            this.num = maxPieces;
        }

        @Override
        public boolean hasNext() {
            if (num <= 0)
                return false;
            next = null;
            final int end = advDir > 0 ? getRanks() : -1;
            for ( ; rank != end; rank+=advDir) {
                for ( ; col < getColumns(); col++) {
                    int p = board[rank][col].getPlayerNum();
                    if (p >= 0 && p != notPlayerNum) {
                        next = board[rank][col++];
                        num--;
                        return true;
                    }
                }
                col=0;
            }
            Utils.assertTrue(false, "Should not get here");
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
        return board[rank][col].getType() != PieceType.BLOCKED;
    }

    private List<Piece> getMovablePieces() {
        List<Piece> pieces = new ArrayList<>();
        for (Piece p : getPieces(getTurn())) {
            if (p.getNumMoves() > 0)
                pieces.add(p);
        }
        Utils.assertTrue(pieces.size() > 0);
        return pieces;
    }

    private List<Move> getMovesforPiece(int pos) {
        List<Move> pm = new ArrayList<>();
        for (Move m : moves) {
            if (m.getStart() == pos) {
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

    @Override
    public final synchronized void executeMove(Move move) {
        if (move.getPlayerNum() != getTurn())
            throw new GException("Invalid move to execute");
        State state = new State(moves.indexOf(move) // <-- TODO: Make this faster
                , moves);
        Utils.assertTrue(undoStack.size() < 1024);

        //
        moves = null;
        clearPieceMoves();
        if (DEBUG)
            System.out.println("COUNTER:" + (counter++) + "\nGAME BEFORE MOVE: " + move + "\n" + this);
        rules.executeMove(this, move);
        undoStack.push(state);
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
                if (m.getStart() >= 0) {
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
            selectedPiece = -1;
            //turn = moves.get(0).getPlayerNum();
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

    final boolean hasNoMoreMOves() {
        return moves == null || moves.size() == 0;
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
        Piece p = getPiece(m.getStart());
        Utils.assertTrue(p.getPlayerNum() == getTurn());//, "Logic Error: Not player " + p.getPlayerNum() + "'s turn: " + getTurn() + "'s turn. For Piece:\n" + p);
        Utils.assertTrue(p.getType().flag != 0);//, "Logic Error: Moving an empty piece. For Move:\n" + m);
        copyPiece(m.getStart(), m.getEnd());
        clearPiece(m.getStart());
        p = getPiece(m.getEnd());
        p.setType(m.getEndType());
        return p;
    }

    final void clearPiece(int rank, int col) {
        numPieces[board[rank][col].getPlayerNum()]--;
        board[rank][col].clear();

    }

    final void clearPiece(int pos ) {
        clearPiece(pos>>8, pos&0xff);
    }

    final Piece setPiece(int rank, int col, int playerNum, PieceType p) {
        board[rank][col].setType(p);
        board[rank][col].setPlayerNum(playerNum);
        if (numPieces != null)
            numPieces[playerNum] ++;
        return board[rank][col];
    }

    final void setPiece(int pos, Piece pc) {
        final int rank = pos >> 8;
        final int col = pos & 0xff;
        final int pnum = board[rank][col].getPlayerNum();
        if (pnum >= 0)
            throw new GException("Cannot assign to non-empty square");
        board[rank][col].copyFrom(pc);
        numPieces[pc.getPlayerNum()]++;
    }

    final Piece setPiece(int pos, int playerNum, PieceType p) {
        return setPiece(pos>>8, pos&(0xff), playerNum, p);
    }

    final void copyPiece(Piece from, Piece to) {
        to.copyFrom(from);
        numPieces[to.getPlayerNum()]++;
    }

    final void copyPiece(int fromPos, int toPos) {
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
                    captured.add(m.getCapturedType());
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
                    } else if (p.getStackSize() > 1) {
                        s.append(p.getType().abbrev.charAt(0) + String.valueOf(p.getStackSize()));
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

    Move getPreviousMove(int playerNum) {
        for (int i=undoStack.size()-1; i>= 0; i--) {
            Move m = undoStack.get(i).getMove();
            if (m.getPlayerNum() == playerNum)
                return m;
        }
        return null;
    }
}
