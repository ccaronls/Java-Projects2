package cc.lib.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chriscaron on 10/7/17.
 */

public abstract class MiniMaxTree<G extends IGame> {

    public static class MMTreeNode<M extends IMove, G extends IGame<M>> extends DescisionTree<G, M> {
        public MMTreeNode(G game) {
            super(game);
        }

        public MMTreeNode(G game, M move) {
            super(game, move);
        }

        public List<M> getPath() {
            List<M> list = new ArrayList<>();
            if (getFirst()==null) {
                if (getMove() != null)
                    list.add(getMove());
            } else {
                int playerNum = getFirst().getMove().getPlayerNum();
                for (MMTreeNode<M,G> t = getFirst(); t != null; t = t.getFirst()) {
                    if (t.getMove().getPlayerNum() != playerNum)
                        break;
                    list.add(t.getMove());
                }
            }
            return list;
        }

    };

    /**
     * Does DFS game evaluation to some number of iterations. In that case when a game has chained moves (like in checkers double jump)
     * then the builder will detect this by the games curPlayerNum not changing. In this case a new sparce tree is built,
     *
     * @param game
     * @param depth
     * @return
     */
    public void buildTree(IGame game, MMTreeNode root, int depth) {
        double d = buildTree(game, root, depth, 1);
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

    private <M extends IMove> long buildTree(IGame<M> game, MMTreeNode root, int depth, int scale) {

        long d = getZeroMovesValue((G)game) * scale;// < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (M m : game.getMoves()) {
            game.executeMove(m);
            MMTreeNode next = new MMTreeNode(game, m);
            //next.appendMeta("playerNum=%d, scale=%d, depth=%d", m.playerNum, scale, depth);
            next.appendMeta("pn(%d)depth(%d)scale(%d)", m.getPlayerNum(), depth, scale);
            onNewNode(next);
            root.addChild(next);
            long v;
            if (game.getTurn() == m.getPlayerNum() && !kill) {
                // this means we have more move options
                v = buildTree(game, next, depth, scale);
            } else if (depth > 0 && !kill) {
                v = buildTree(game, next, depth-1, scale * -1);
            } else {
                v = evaluate((G)game, next, m.getPlayerNum()) * scale;
            }
            next.setValue(v);
            if (scale < 0) {
                d = Math.min(d, v);
            } else {
                d = Math.max(d, v);
            }
            game.undo();
            if (kill)
                break;
        }
        root.sortChildren();
        return d;
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
    protected abstract long evaluate(G game, MMTreeNode t, int playerNum);

    /**
     * Callback for this event. base method does nothing.
     * @param node
     */
    protected void onNewNode(MMTreeNode node) {}

    protected long getZeroMovesValue(G game) {
        return Long.MIN_VALUE;
    }
}
