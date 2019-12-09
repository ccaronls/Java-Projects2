package cc.lib.checkerboard;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
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

    int getThinkingTimeSecs() {
        return (int)(Math.max(0, System.currentTimeMillis() - startTime)) / 1000;
    }

    public void forceRebuildMovesList(Game game) {
        moveList.clear();
        buildMovesList(game);
    }

    static void dumpTree(Writer out, Move root, int indent) throws IOException {

        if (indent >= INDENT_LEVELS.length)
            return;
        if (root.children == null)
            return;
        String endTag = "";
        if (root.maximize < 0) {
            out.write(INDENT_LEVELS[indent] + "<min>");
            endTag = "</min>\n";
        }
        else if (root.maximize > 0) {
            out.write(INDENT_LEVELS[indent] + "<max>");
            endTag = "</max>\n";
        }
        out.write("[" + root.bestValue + "]"+root);
        //out.write(INDENT_LEVELS[indent] + INDENT_LEVELS[0] + "<move>[" + root.bestValue + "] " + root + "</move>\n");
        if (root.children.size() > 0) {
            out.write("\n");
            endTag = INDENT_LEVELS[indent] + endTag;
            for (Move child : root.children) {
                dumpTree(out, child, indent + 1);
            }
        }
        out.write(endTag);
    }

    void buildMovesList(Game _game) {

        if (moveList.size() > 0 && moveList.getFirst().getPlayerNum() == _game.getTurn())
            return;

        kill = false;
        moveList.clear();
        Game game = _game.deepCopy();
        game.copyFrom(_game);

        log.debug("perform minimax search on game\n" + game.getInfoString());
        // minmax search moves
        startTime = System.currentTimeMillis();

        Move root = new Move(null, game.getTurn());
        root.bestValue = miniMaxR(game, root, maxSearchDepth, true, 0);
                //negamaxR(game, root, 1, maxSearchDepth);
        log.debug("MiniMax serach result: " + root.bestValue);
        try (Writer out = new FileWriter("minimax_tree.xml")) {
            out.write("<root minimax=\"" + root.bestValue + "\">\n");
            dumpTree(out, root, 0);
            out.write("</root>\n");
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
    static long miniMaxR(Game game, Move root, int depth, boolean maximizePlayer, int actualDepth) {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner;
        switch (winner=game.getRules().getWinner(game)) {
            case Game.NEAR:
            case Game.FAR:
                return root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth;
        }
        boolean isDraw = game.getRules().isDraw(game);
        if (depth == 0 || isDraw) {
            return game.getRules().evaluate(game, game.getMostRecentMove());
        }
        root.children = Collections.unmodifiableList(game.getMoves());
        if (maximizePlayer) {
            long value = Long.MIN_VALUE;
            Move path = null;
            root.maximize = 1;
            for (Move m : root.children) {
                game.executeMove(m);
                boolean sameTurn = game.getTurn() == m.getPlayerNum();
                long v = miniMaxR(game, m, sameTurn ? depth : depth-1, sameTurn, actualDepth+1);
                //v -= 100*actualDepth;
                m.bestValue = v;
                if (v >= value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            root.path = path;
            return value;
        } else { /* minimizing */
            long value = Long.MAX_VALUE;
            Move path = null;
            root.maximize = -1;
            for (Move m : root.children) {
                game.executeMove(m);
                boolean sameTurn = game.getTurn() == m.getPlayerNum();
                long v = miniMaxR(game, m, sameTurn ? depth : depth-1, !sameTurn, actualDepth+1);
                //v += 100*actualDepth;
                m.bestValue = v;
                if (v <= value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            root.path = path;
            return value;
        }
    }

    /*
    function negamax(node, depth, color) is
    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node
    value := −∞
    for each child of node do
        value := max(value, −negamax(child, depth − 1, −color))
    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, 1)
(* Initial call for Player B's root node *)
negamax(rootNode, depth, −1)
     */

    private static long negamaxR(Game game, Move root, long scale, int depth) {
        if (scale == 0)
            throw new AssertionError();
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner;
        switch (winner=game.getRules().getWinner(game)) {
            case Game.NEAR:
            case Game.FAR:
                return scale * root.getPlayerNum()==winner ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
        boolean isDraw = game.getRules().isDraw(game);
        if (depth == 0 || isDraw) {
            return scale * game.getRules().evaluate(game, game.getMostRecentMove());
        }
        root.children = new ArrayList<>(game.getMoves());
        long value = Long.MIN_VALUE;
        Move path=null;
        for (Move child : root.children) {
            game.executeMove(child);
            boolean sameTurn = game.getTurn() == child.getPlayerNum();
            long v = -negamaxR(game, child, sameTurn ? scale : scale * -1, sameTurn ? depth : depth-1);
            child.bestValue = v;
            child.maximize = (int)scale;
            if (v >= value) {
                path = child;
                value = v;
            }
            game.undo();
        }
        root.path = path;
        return value;
    }

    // TODO: negimax with alpha - beta pruning
    /*
    function negamax(node, depth, α, β, color) is
    alphaOrig := α

    (* Transposition Table Lookup; node is the lookup key for ttEntry *)
    ttEntry := transpositionTableLookup(node)
    if ttEntry is valid and ttEntry.depth ≥ depth then
        if ttEntry.flag = EXACT then
            return ttEntry.value
        else if ttEntry.flag = LOWERBOUND then
            α := max(α, ttEntry.value)
        else if ttEntry.flag = UPPERBOUND then
            β := min(β, ttEntry.value)

        if α ≥ β then
            return ttEntry.value

    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node

    childNodes := generateMoves(node)
    childNodes := orderMoves(childNodes)
    value := −∞
    for each child in childNodes do
        value := max(value, −negamax(child, depth − 1, −β, −α, −color))
        α := max(α, value)
        if α ≥ β then
            break

    (* Transposition Table Store; node is the lookup key for ttEntry *)
    ttEntry.value := value
    if value ≤ alphaOrig then
        ttEntry.flag := UPPERBOUND
    else if value ≥ β then
        ttEntry.flag := LOWERBOUND
    else
        ttEntry.flag := EXACT
    ttEntry.depth := depth
    transpositionTableStore(node, ttEntry)

    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, −∞, +∞, 1)
     */

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
