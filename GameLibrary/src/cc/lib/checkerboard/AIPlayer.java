package cc.lib.checkerboard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;

public class AIPlayer extends Player {

    public static Algorithm algorithm = Algorithm.miniMaxAB;
    public static Move lastSearchResult = null;
    public static boolean movePathNodeToFront = true;
    public static boolean randomizeDuplicates = true;

    private int maxSearchDepth = 3;

    private final static Logger log = LoggerFactory.getLogger(AIPlayer.class);

    static {
        addAllFields(AIPlayer.class);
    }

    @Omit
    private long startTime = 0;
    @Omit
    private LinkedList<Move> moveList = new LinkedList<>();

    private static boolean kill = false;

    static int evalCount = 0;
    static long evalTimeTotalMSecs = 0;

    enum Algorithm {
        minimax,
        miniMaxAB,
        negamax,
        negamaxAB
    }

    public AIPlayer() {}

    public AIPlayer(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
    }

    public final boolean isThinking() {
        return startTime > 0;
    }

    int getThinkingTimeSecs() {
        return (int)(Math.max(0, System.currentTimeMillis() - startTime)) / 1000;
    }

    public void forceRebuildMovesList(Game game) {
        moveList.clear();
        buildMovesList(game);
    }

    static void dumpTree(Writer out, Move root) throws IOException {
        dumpTreeR(out, root, "", null);
    }

    private static void dumpTreeR(Writer out, Move root, String indent, Move parent) throws IOException {

        if (root == null)
            return;
        out.write(indent + root.getXmlStartTag(parent));
        String endTag = root.getXmlEndTag(parent);
        //if (parent != null && parent.path == root)
          //  out.write("== PATH ==");
        if (root.getMoveType() != null)
            out.write(root.toString());
        //out.write(INDENT_LEVELS[indent] + INDENT_LEVELS[0] + "<move>[" + root.bestValue + "] " + root + "</move>\n");
        if (root.children != null) {
            out.write("\n");
            endTag = indent + endTag;
            for (Move child : root.children) {
                dumpTreeR(out, child, indent + "  ", root);
            }
        }
        out.write(endTag+"\n");
    }

    static class MoveException extends GException {
        final Move move;
        MoveException(Move move) {
            this.move = move;
        }
    }

    void buildMovesList(Game _game) {

        if (moveList.size() > 0 && moveList.getFirst().getPlayerNum() == _game.getTurn())
            return;
        evalCount = 0;
        evalTimeTotalMSecs = 0;
        kill = false;
        moveList.clear();
        Game game = new Game();
        game.copyFrom(_game);

        //log.debug("perform minimax search on game\n" + game.getInfoString());
        // minmax search moves
        startTime = System.currentTimeMillis();

        Move root = lastSearchResult = new Move(null, game.getTurn());
        try {
            switch (algorithm) {
                case minimax:
                    root.bestValue = miniMaxR(game, root, true, maxSearchDepth, 0);
                    break;
                case miniMaxAB:
                    root.bestValue = miniMaxABR(game, root, true, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE);
                    break;
                case negamax:
                    //                root.bestValue = negamaxR(game, root, -1, maxSearchDepth, 0);
                    root.bestValue = negamaxR(game, root, game.getTurn() != 0 ? 1 : -1, maxSearchDepth, 0);
                    break;
                case negamaxAB:
                    root.bestValue = negamaxABR(game, root, 1, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE);
                    break;
            }
        } catch (MoveException e) {
            e.printStackTrace();
            log.error("Error executing moves: " + e.move.getPathString());
        } catch (Throwable e) {
            e.printStackTrace();
            if (false) {
                String fname = algorithm.name() + "_error.xml";
                File file = new File(fname);
                try (Writer out = new FileWriter(file)) {
                    dumpTree(out, root);
                    log.error(e);
                    log.error("Write decision tree state to file '" + file.getCanonicalPath() + "'");
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            log.error("Game state at error:%s\n", game.toString());
            game.trySaveToFile(new File("game_" + algorithm.name() + "_error.txt"));
        }
        float evalTimeMS = (int)(System.currentTimeMillis() - startTime);
        startTime = 0;
        log.debug(algorithm + " ran in %3.2f seconds with best value of %s", evalTimeMS/1000, root.bestValue);
        log.debug("Time spent in eval %3.4f seconds", ((float)(evalTimeTotalMSecs))/1000);
        Move m = root.path;
        while (m != null) {
            // move the 'path' node to front so that it appears first in the xml
            if (movePathNodeToFront && root.children != null) {
                if (!root.children.remove(m))
                    throw new AssertionError();
                root.children.add(0, m);
            }
            moveList.add(m);
            root = m;
            m = m.path;
        }
        onMoveListGenerated(moveList);
        printMovesListToLog();
    }

    static long evaluate(Game game, int actualDepth) {
        final long startTime = System.currentTimeMillis();
        long value = game.getRules().evaluate(game, game.getMostRecentMove());
        if (value > Long.MIN_VALUE) {
            value -= actualDepth; // shorter paths that lead to the same value are scored higher.
        }
        if (randomizeDuplicates && value != 0 && value > Long.MIN_VALUE / 1000 && value < Long.MAX_VALUE / 1000) {
            // add randomness to boards with same value
            value *= 100;
            if (value < 0) {
                value += Utils.randRange(-99, 0);
            } else if (value > 0) {
                value += Utils.randRange(0, 99);
            }
        }
        evalTimeTotalMSecs += (System.currentTimeMillis() - startTime);
        ++evalCount;
        /*
        if (++evalCount % 200 == 0) {
            System.out.print('.');
            if (evalCount % (200*50) == 0)
                System.out.println();
        }*/
        return value;
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
    static long miniMaxR(Game game, Move root, boolean maximizePlayer, int depth, int actualDepth) {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner;
        switch (winner=game.getWinnerNum()) {
            case Game.NEAR:
            case Game.FAR:
                return root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, actualDepth);
        }
        root.children = new ArrayList<>(game.getMoves());
        if (maximizePlayer) {
            long value = Long.MIN_VALUE;
            Move path = null;
            root.maximize = 1;
            for (Move m : root.children) {
                game.executeMove(m);
                boolean sameTurn = m.getPlayerNum() == game.getTurn(); // if turn has not changed
                long v = miniMaxR(game, m, sameTurn, sameTurn ? depth : depth-1, actualDepth+1);
                m.bestValue = v;
                if (v > value) {
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
                boolean sameTurn = m.getPlayerNum() == game.getTurn();
                long v = miniMaxR(game, m, !sameTurn, !sameTurn ? depth : depth-1, actualDepth+1);
                //v += 100*actualDepth;
                m.bestValue = v;
                if (v < value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            root.path = path;
            return value;
        }
    }

    static Comparator<Move> SORT_ASCENDING = (Move m0, Move m1) -> Integer.compare(m0.getCompareValue(), m1.getCompareValue());

    static Comparator<Move> SORT_DESCENDING = (Move m0, Move m1) -> Integer.compare(m1.getCompareValue(), m0.getCompareValue());

    static long miniMaxABR(Game game, Move root, boolean maximizePlayer, int depth, int actualDepth, long alpha, long beta) {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        int winner;
        switch (winner=game.getWinnerNum()) {
            case Game.NEAR:
            case Game.FAR:
                return root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth;
        }
        if (game.isDraw())
            return 0;
        if (kill || depth <= 0 || actualDepth > 30) {
            return evaluate(game, actualDepth);
        }
        root.children = new ArrayList<>(game.getMoves());
        //if (maximizePlayer) {
        //    Collections.sort(root.children, SORT_DESCENDING);
        //} else {
        //    Collections.sort(root.children, SORT_ASCENDING);
       // }

        long value = maximizePlayer ? Long.MIN_VALUE : Long.MAX_VALUE;
        Move path = null;
        root.maximize = maximizePlayer ? 1 : -1;
        for (Move m : root.children) {
            m.parent = root;
//            String gameBeforeMove = game.toString();
            try {
                game.executeMove(m);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MoveException(m);
            }
            boolean sameTurn = m.getPlayerNum() == game.getTurn(); // if turn has not changed
            long v;
            if (maximizePlayer) {
                v = miniMaxABR(game, m, sameTurn, sameTurn ? depth : depth-1, actualDepth+1, alpha, beta);
            } else {
                v = miniMaxABR(game, m, !sameTurn, !sameTurn ? depth : depth-1, actualDepth+1, alpha, beta);
            }
            game.undo();
            m.bestValue = v;
            if (maximizePlayer) {
                if (v > value) {
                    path = m;
                    value = v;
                }
                alpha = Math.max(alpha, value);
                if (alpha > beta)
                    break;
            } else {
                if (v < value) {
                    path = m;
                    value = v;
                }
                beta = Math.min(beta, value);
                if (beta < alpha)
                    break;
            }
        }
        root.path = path;
        return value;
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

    private static long negamaxR(Game game, Move root, int color, int depth, int actualDepth) {
        if (color == 0)
            throw new AssertionError();
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner;
        switch (winner=game.getWinnerNum()) {
            case Game.NEAR:
            case Game.FAR:
                return (root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth) * color;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, actualDepth) * color;
        }
        root.children = new ArrayList<>(game.getMoves());
        long value = Long.MIN_VALUE;
        Move path=null;
        for (Move m : root.children) {
            game.executeMove(m);
            boolean sameTurn = game.getTurn() == m.getPlayerNum();
            long v;
            if (sameTurn) {
                v = negamaxR(game, m, color, depth, actualDepth + 1);
            } else {
                v = -negamaxR(game, m, -color, depth - 1, actualDepth + 1);
            }
            m.bestValue = v;
            m.maximize = color;
            game.undo();

            if (v > value) {
                path = m;
                value = v;
            }
        }
        root.path = path;
        return value;
    }


    // Negamax with alpha - beta pruning simple
    /*
    function negamax(node, depth, α, β, color) is
    if depth = 0 or node is a terminal node then
        return color × the heuristic value of node

    childNodes := generateMoves(node)
    childNodes := orderMoves(childNodes)
    value := −∞
    foreach child in childNodes do
        value := max(value, −negamax(child, depth − 1, −β, −α, −color))
        α := max(α, value)
        if α ≥ β then
            break (* cut-off *)
    return value
(* Initial call for Player A's root node *)
negamax(rootNode, depth, −∞, +∞, 1)
     */

    long negamaxABR(Game game, Move root, final int color, int depth, int actualDepth, long alpha, long beta) {
        if (color == 0)
            throw new AssertionError();
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner;
        switch (winner=game.getWinnerNum()) {
            case Game.NEAR:
            case Game.FAR:
                return (root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth) * color;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, actualDepth) * color;
        }
        root.children = new ArrayList<>(game.getMoves());
        Collections.sort(root.children, (Move o1, Move o2) -> {
            if (o1.getCompareValue() < o2.getCompareValue())
                return -color;
            if (o1.getCompareValue() > o2.getCompareValue())
                return color;
            return 0;
        });
        long value = Long.MIN_VALUE;
        Move path=null;
        root.maximize = color;
        for (Move child : root.children) {
            game.executeMove(child);
            boolean sameTurn = root.getPlayerNum() == child.getPlayerNum();
            long v;
            if (sameTurn) {
                v = negamaxABR(game, child, color, depth, actualDepth+1, alpha, beta);
            } else {
                v = -negamaxABR(game, child, color*-1, depth-1, actualDepth+1, -beta, -alpha);
            }
            //if (sameTurn)
            //    v *= -1;
            child.bestValue = v;
            //child.maximize = color;
            if (v >= value) {
                path = child;
                value = v;
            }
            game.undo();
            alpha = Math.max(alpha, value);
            if (alpha > beta)
                break;

        }
        root.path = path;
        return value;
    }

    // TODO: negimax with alpha - beta pruning version 2
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
                if (m.getEndType() != null && m.getEndType() != m.getStartType())
                    str.append(" becomes:").append(m.getEndType());
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

    public static void cancel() {
        kill = true;
        Thread.yield();
    }

    /**
     *
     * @param game
     * @param move
     */
    protected void onMoveEvaluated(Game game, Move move) {
        if (evalCount % 1000 == 0)
            log.debug("Eval Count: %d", evalCount);
    }

    protected void onMoveListGenerated(List<Move> moveList) {
        log.debug("nodes evaluated = " + evalCount);
    }
}
