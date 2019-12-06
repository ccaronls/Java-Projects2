package cc.lib.checkerboard;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class AIPlayer extends Player {

    private final static Logger log = LoggerFactory.getLogger(AIPlayer.class);

    static {
        addAllFields(AIPlayer.class);
    }

    private int maxSearchDepth = 4;

    @Omit
    private long startTime = 0;
    @Omit
    private LinkedList<Move> moveList = new LinkedList<>();

    private static boolean kill = false;

    public AIPlayer() {}

    public AIPlayer(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
    }

    boolean isThinking() {
        return startTime > 0;
    }

    long getThinkingTime() {
        return Math.max(0, System.currentTimeMillis() - startTime);
    }

    public void forceRebuildMovesList(Game game) {
        moveList.clear();
        buildMovesList(game);
    }

    void buildMovesList(Game game) {

        if (moveList.size() > 0 && moveList.getFirst().getPlayerNum() == game.getTurn())
            return;

        kill = false;
        moveList.clear();
        log.debug("perform minimax search on game");
        // minmax search moves
        startTime = System.currentTimeMillis();
        Move root = new Move(null, game.getTurn());
        try (Writer out = new FileWriter("minimax_tree.xml")) {
            out.write("<root>\n");
            long minimaxvalue = miniMaxR(out, game, root, maxSearchDepth, true, 0);
            //out.write(INDENT_LEVELS[0] + "<value>"+minimaxvalue+"</value>\n");
            out.write("</root>\n");
            log.debug("MiniMax serach result: " + minimaxvalue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        float evalTimeMS = (int)(System.currentTimeMillis() - startTime);
        startTime = 0;
        log.debug("Minimax ran in %3.2f seconds", evalTimeMS/1000);
        Move m = root.path;
        while (m != null) {
            moveList.add(m);
            m = m.path;
        }
        printMovesListToLog();
    }

    /*
    function minimax(node, depth, maximizingPlayer) is
    if depth = 0 or node is a terminal node then
        return the heuristic value of node
    if maximizingPlayer then
        value := −∞
        for each child of node do
            value := max(value, minimax(child, depth − 1, FALSE))
        return value
    else (* minimizing player *)
        value := +∞
        for each child of node do
            value := min(value, minimax(child, depth − 1, TRUE))
        return value
     */
    static long miniMaxR(Writer out, Game game, Move root, int depth, boolean maximizePlayer, int indent) throws IOException {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        if (indent >= INDENT_LEVELS.length || depth == 0 || game.isGameOver()) {
            return game.getRules().evaluate(game, game.getMostRecentMove());
        }
        if (maximizePlayer) {
            long value = Long.MIN_VALUE;
            Move path = null;
            out.write(INDENT_LEVELS[indent] + "<max>\n");
            for (Move m : game.getMoves()) {
                out.write(INDENT_LEVELS[indent] + "<move>" + m + "</move>\n");
                game.executeMove(m);
                boolean sameTurn = game.getTurn() == m.getPlayerNum();
                long v = miniMaxR(out, game, m, sameTurn ? depth : depth-1, sameTurn, indent+1);
                m.minimaxValue = v;
                if (v >= value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            out.write(INDENT_LEVELS[indent] + "</max>\n");
            root.path = path;
            return value;
        } else { /* minimizing */
            long value = Long.MAX_VALUE;
            Move path = null;
            out.write(INDENT_LEVELS[indent] + "<min>\n");
            for (Move m : game.getMoves()) {
                out.write(INDENT_LEVELS[indent + 1] + "<move>" + m + "</move>\n");
                game.executeMove(m);
                boolean sameTurn = game.getTurn() == m.getPlayerNum();
                long v = miniMaxR(out, game, m, sameTurn ? depth : depth-1, !sameTurn, indent+1);
                m.minimaxValue = v;
                if (v <= value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            out.write(INDENT_LEVELS[indent] + "</min>\n");
            root.path = path;
            return value;
        }
    }

    private final static String [] INDENT_LEVELS;

    static {
        INDENT_LEVELS = new String[64];
        String indent = "  ";
        for (int i=0; i<64; i++) {
            INDENT_LEVELS[i] = indent;
            indent += "  ";
        }
    }


    void printMovesListToLog() {
        StringBuffer str = new StringBuffer();
        int curTurn = -1;//game.getTurn();
        for (Move m : moveList) {
            if (str.length()>0)
                str.append("->");
            if (m.getPlayerNum() != curTurn) {
                str.append(m);
                curTurn = m.getPlayerNum();
            } else {
                str.append(m.getMoveType());
                if (m.hasEnd())
                    str.append(" ").append(Move.toStr(m.getEnd()));
                if (m.hasCaptured())
                    str.append(" cap:").append(Move.toStr(m.getCaptured(0)));
            }
        }
        log.debug("Moves: " + str);
    }

    @Override
    public Piece choosePieceToMove(Game game, List<Piece> pieces) {
        buildMovesList(game);
        return game.getPiece(moveList.getFirst().getStart());
    }

    @Override
    public Move chooseMoveForPiece(Game game, List<Move> moves) {
        buildMovesList(game);
        return moveList.removeFirst();
    }

    public void cancel() {
        kill = true;
        Thread.yield();
    }

    /**
     *
     * @param game
     * @param move
     */
    protected void onMoveEvaluated(Game game, Move move) {
        //if (numNodes % 1000 == 0)
        //    System.out.print('.');
    }

    protected void onMoveListGenerated(List<Move> moveList) {}
}
