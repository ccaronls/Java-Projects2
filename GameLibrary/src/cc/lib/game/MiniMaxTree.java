package cc.lib.game;

import java.io.PrintWriter;

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
    public final void buildTree(IGame game, DescisionTree root, int depth) {
        kill = false;
        try {
            buildTreeR(0, game, root, depth, 1, 10);
        } catch (Throwable e) {
            log.error(e);
            root.dumpTreeXML(new PrintWriter(System.err));
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

    private void buildTreeR(int curDepth, IGame<IMove> game, DescisionTree root, int depth, int scale, int max) {

        if (kill || depth < 0 || curDepth > 10)
            return;

        for (IMove m : game.getMoves()) {
            if (m.getPlayerNum() < 0)
                throw new AssertionError();
            game.executeMove(m);
            //next.appendMeta("playerNum=%d, scale=%d, depth=%d", m.playerNum, scale, depth);
            long v = evaluate(game, m) * scale;
            DescisionTree next = new DescisionTree(m, v, scale);
            next.appendMeta("pn(%d) move(%s)scale(%d)", m.getPlayerNum(), m.toString(), scale);
            onNewNode(game, next);
            root.addChild(next);
            game.undo();
            if (kill)
                return;
        }
        for (DescisionTree dt : root.getChildren(max)) {
            if (kill)
                break;
            game.executeMove(dt.getMove());
            boolean sameTurn = game.getTurn() == dt.getMove().getPlayerNum();
            buildTreeR(sameTurn ? curDepth+1 : 0, game, dt, sameTurn ? depth : depth-1, sameTurn ? scale : scale * -1, Math.max(3, max/2));
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
