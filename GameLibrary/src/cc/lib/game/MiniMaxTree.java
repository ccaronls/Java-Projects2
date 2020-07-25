package cc.lib.game;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Created by chriscaron on 10/7/17.
 */

public abstract class MiniMaxTree {

    final static Logger log = LoggerFactory.getLogger(MiniMaxTree.class);

    private int maxSearchDepth = 4;

    private long startTime = 0;
    private LinkedList<IMove> moveList = new LinkedList<>();

    private static boolean kill = false;

    public static IMove lastSearchResult = null;

    static int evalCount = 0;
    static long evalTimeTotalMSecs = 0;

    enum Algorithm {
        minimax,
        miniMaxAB,
        negamax,
        negamaxAB
    }

    public static Algorithm algorithm = Algorithm.minimax;

    public MiniMaxTree() {}

    public MiniMaxTree(int maxSearchDepth) {
        this.maxSearchDepth = maxSearchDepth;
    }

    boolean isThinking() {
        return startTime > 0;
    }

    int getThinkingTimeSecs() {
        return (int)(Math.max(0, System.currentTimeMillis() - startTime)) / 1000;
    }

    public void forceRebuildMovesList(IGame game) {
        moveList.clear();
        buildMovesList(game);
    }
/*
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
*/
    public void buildMovesList(IGame game) {

        if (moveList.size() > 0 && moveList.getFirst().getPlayerNum() == game.getTurn())
            return;
        evalCount = 0;
        evalTimeTotalMSecs = 0;
        kill = false;
        moveList.clear();

        //log.debug("perform minimax search on game\n" + game.getInfoString());
        // minmax search moves
        startTime = System.currentTimeMillis();

        long bestValue = 0;
        IMove root = lastSearchResult = makeEmptyMove(game);
        switch (algorithm) {
            case minimax:
                bestValue = miniMaxR(game, root, true, maxSearchDepth, 0);
                break;
            case miniMaxAB:
                bestValue = miniMaxABR(game, root, true, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE);
                break;
            case negamax:
//                root.bestValue = negamaxR(game, root, -1, maxSearchDepth, 0);
                bestValue = negamaxR(game, root, game.getTurn() != 0 ? 1 : -1, maxSearchDepth, 0);
                break;
            case negamaxAB:
                bestValue = negamaxABR(game, root, 1, maxSearchDepth, 0, Long.MIN_VALUE, Long.MAX_VALUE);
                break;
        }

        float evalTimeMS = (int)(System.currentTimeMillis() - startTime);
        startTime = 0;
        log.debug(algorithm + " ran in %3.2f seconds with best value of %s", evalTimeMS/1000, bestValue);
        log.debug("Time spent in eval %3.4f seconds", ((float)(evalTimeTotalMSecs))/1000);
        IMove m = pathMap.get(root);
        if (m == null)
            throw new AssertionError("Invalid Path generated");
        while (m != null) {
            // move the 'path' node to front so that it appears first in the xml
            /*
            if (movePathNodeToFront && root.children != null) {
                if (!root.children.remove(m))
                    throw new AssertionError();
                root.children.add(0, m);
            }*/
            moveList.add(m);
            root = m;
            m = pathMap.get(m);
        }
        onMoveListGenerated(moveList);
        //printMovesListToLog();
    }

    protected abstract void onMoveListGenerated(LinkedList<IMove> moveList);

    protected abstract IMove makeEmptyMove(IGame game);

    public static boolean randomizeDuplicates = true;

    private final Map<IMove, IMove> pathMap = new HashMap<>();

    void setPath(IMove parent, IMove child) {
        pathMap.put(parent, child);
    }

    protected void setMoveValue(IMove move, long value) {

    }

    long evaluate(IGame game, IMove move, int actualDepth) {
        final long startTime = System.currentTimeMillis();
        long value = evaluate(game, move);
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
    long miniMaxR(IGame game, IMove root, boolean maximizePlayer, int depth, int actualDepth) {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner=game.getWinnerNum();
        if (winner >= 0) {
            return root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, root, actualDepth);
        }
        List<IMove> moves = new ArrayList<>(game.getMoves());
        if (maximizePlayer) {
            long value = Long.MIN_VALUE;
            IMove path = null;
            //root.maximize = 1;
            for (IMove m : moves) {
                game.executeMove(m);
                boolean sameTurn = m.getPlayerNum() == game.getTurn(); // if turn has not changed
                long v = miniMaxR(game, m, sameTurn, sameTurn ? depth : depth-1, actualDepth+1);
                setMoveValue(m, v);
                if (v > value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            setPath(root, path);
            //root.path = path;
            return value;
        } else { /* minimizing */
            long value = Long.MAX_VALUE;
            IMove path = null;
            //root.maximize = -1;
            for (IMove m : moves) {
                game.executeMove(m);
                boolean sameTurn = m.getPlayerNum() == game.getTurn();
                long v = miniMaxR(game, m, !sameTurn, !sameTurn ? depth : depth-1, actualDepth+1);
                //v += 100*actualDepth;
                setMoveValue(m, v);
                if (v < value) {
                    path = m;
                    value = v;
                }
                game.undo();
            }
            setPath(root, path);
            return value;
        }
    }

    long miniMaxABR(IGame game, IMove root, boolean maximizePlayer, int depth, int actualDepth, long alpha, long beta) {
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner=game.getWinnerNum();
        if (winner >= 0) {
            return root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, root, actualDepth);
        }
        List<IMove> moves = new ArrayList<>(game.getMoves());

        long value = maximizePlayer ? Long.MIN_VALUE : Long.MAX_VALUE;
        IMove path = null;
        //root.maximize = maximizePlayer ? 1 : -1;
        for (IMove m : moves) {
            game.executeMove(m);
            boolean sameTurn = m.getPlayerNum() == game.getTurn(); // if turn has not changed
            long v;
            if (maximizePlayer) {
                v = miniMaxABR(game, m, sameTurn, sameTurn ? depth : depth-1, actualDepth+1, alpha, beta);
            } else {
                v = miniMaxABR(game, m, !sameTurn, !sameTurn ? depth : depth-1, actualDepth+1, alpha, beta);
            }
            game.undo();
            setMoveValue(m, v);
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
        setPath(root, path);
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

    private long negamaxR(IGame game, IMove root, int color, int depth, int actualDepth) {
        if (color == 0)
            throw new AssertionError();
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner=game.getWinnerNum();
        if (winner >= 0) {
            return (root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth) * color;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, root, actualDepth) * color;
        }
        List<IMove> moves = new ArrayList<>(game.getMoves());
        long value = Long.MIN_VALUE;
        IMove path=null;
        for (IMove m : moves) {
            game.executeMove(m);
            boolean sameTurn = game.getTurn() == m.getPlayerNum();
            long v;
            if (sameTurn) {
                v = negamaxR(game, m, color, depth, actualDepth + 1);
            } else {
                v = -negamaxR(game, m, -color, depth - 1, actualDepth + 1);
            }
            setMoveValue(m, v);
            //m.maximize = color;
            game.undo();

            if (v > value) {
                path = m;
                value = v;
            }
        }
        setPath(root, path);
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

    long negamaxABR(IGame game, IMove root, final int color, int depth, int actualDepth, long alpha, long beta) {
        if (color == 0)
            throw new AssertionError();
        if (root == null || root.getPlayerNum() < 0)
            throw new AssertionError();
        if (kill)
            return 0;
        int winner=game.getWinnerNum();
        if (winner >= 0) {
            return (root.getPlayerNum()==winner ? Long.MAX_VALUE - actualDepth : Long.MIN_VALUE + actualDepth) * color;
        }
        if (game.isDraw())
            return 0;
        if (depth <= 0) {
            return evaluate(game, root, actualDepth) * color;
        }
        ArrayList<IMove> moves = new ArrayList<>(game.getMoves());
        Collections.sort(moves, (IMove o1, IMove o2) -> {
            if (o1.getCompareValue() < o2.getCompareValue())
                return -color;
            if (o1.getCompareValue() > o2.getCompareValue())
                return color;
            return 0;
        });
        long value = Long.MIN_VALUE;
        IMove path=null;
        //root.maximize = color;
        for (IMove child : moves) {
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
            //child.bestValue = v;
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
        setPath(root, path);
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



    /**
     *
     */
    public final void cancel() {
        kill = true;
        Thread.yield();
    }

    /**
     * Implement a valuation method with the following properties
     *
     * If eval(player A) = x then eval(player B) = -x
     * If moves re zero then value is automatically (+/-) INF
     *
     * @param game
     * @param move
     * @return
     */
    protected abstract long evaluate(IGame game, IMove move);

    /**
     * Callback for this event. base method does nothing.
     * @param node
     */
    protected void onNewNode(IGame game, DescisionTree node) {}

    protected abstract long getZeroMovesValue(IGame game);
}
