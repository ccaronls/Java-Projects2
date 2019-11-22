package cc.lib.game;

import java.io.PrintWriter;
import java.util.Iterator;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

/**
 * Created by chriscaron on 10/7/17.
 */

public abstract class MiniMaxTree {

    final static Logger log = LoggerFactory.getLogger(MiniMaxTree.class);

    /**
     * Does DFS game evaluation to some number of iterations. In that case when a game has chained moves (like in checkers double jump)
     * then the builder will detect this by the games curPlayerNum not changing. In this case a new sparce tree is built,
     *
     * @param game
     * @param depth
     * @return
     */
    public void buildTree(IGame game, DescisionTree<IMove> root, int depth) {
        try {
            buildTreeR(game, root, depth, 1, 10);
        } catch (Throwable e) {
            log.error(e);
            root.dumpTree(new PrintWriter(System.err));
            throw e;
        }
        synchronized (this) {
            notifyAll();
        }
    }

    private boolean kill = false;

    public final void killAndBlock() {
        if (!this.kill) {
            this.kill = true;
            Utils.waitNoThrow(this, -1);
        }
    }

    public void killNoBlock() {
        this.kill = true;
    }

    private void buildTreeR(IGame<IMove> game, DescisionTree<IMove> root, int depth, int scale, int max) {

        if (kill || depth < 0)
            return;

        long d = getZeroMovesValue(game) * scale;// < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (IMove m : game.getMoves()) {
            if (m.getPlayerNum() < 0)
                throw new AssertionError();
            game.executeMove(m);
            DescisionTree<IMove> next = new DescisionTree<IMove>(m);
            //next.appendMeta("playerNum=%d, scale=%d, depth=%d", m.playerNum, scale, depth);
            next.appendMeta("pn(%d) move(%s)scale(%d)", m.getPlayerNum(), m.toString(), scale);
            onNewNode(game, next);
            long v = evaluate(game, next, m.getPlayerNum()) * scale;
            next.setValue(v);
            root.addChild(next);
            if (scale < 0) {
                d = Math.min(d, v);
            } else {
                d = Math.max(d, v);
            }
            game.undo();
            if (kill)
                return;
        }
        for (DescisionTree<IMove> dt : root.getChildren()) {
            if (max-- <= 0)
                break;
            game.executeMove(dt.getMove());
            boolean sameTurn = game.getTurn() != dt.getMove().getPlayerNum();
            buildTreeR(game, dt, sameTurn ? depth : depth-1, sameTurn ? scale : scale * -1, Math.max(3, max/2));
            game.undo();
        }
    }

    /**
     * Implement a valuation method with the following properties
     *
     * If eval(player A) = x then eval(player B) = -x
     * If moves re zero then value is automatically (+/-) INF
     *
     * @param game
     * @param t
     * @param playerNum the player for whom the evaluation is considered.
     * @return
     */
    protected abstract long evaluate(IGame game, DescisionTree<IMove> t, int playerNum);

    /**
     * Callback for this event. base method does nothing.
     * @param node
     */
    protected void onNewNode(IGame game, DescisionTree<IMove> node) {}

    protected long getZeroMovesValue(IGame game) {
        return Long.MIN_VALUE;
    }
}
