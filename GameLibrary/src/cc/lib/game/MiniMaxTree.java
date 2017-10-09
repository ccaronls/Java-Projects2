package cc.lib.game;

import java.util.List;

/**
 * Created by chriscaron on 10/7/17.
 */

public abstract class MiniMaxTree {

    public interface IGame {
        void executeMove(IMove move);
        void undo();
        List<IMove> getMoves();
        int getCurPlayerNum();
    }

    public interface IMove {
        int getPlayerNum();
    }

    public static class MMTreeNode extends DescisionTree<IGame, IMove> {
        public MMTreeNode(IGame game) {
            super(game);
        }

        public MMTreeNode(IGame game, IMove move) {
            super(game, move);
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
    public MMTreeNode buildTree(IGame game, int depth) {
        MMTreeNode root = new MMTreeNode(game);
        double d = buildTree(game, root, depth, 1);
        synchronized (this) {
            notifyAll();
        }
        return root;
    }

    private boolean kill = false;

    public final void killAndBlock() {
        if (!this.kill) {
            this.kill = true;
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {}
        }
    }

    public void killNoBlock() {
        this.kill = true;
    }

    private <M> long buildTree(IGame game, MMTreeNode root, int depth, int scale) {

        long d = scale < 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (IMove m : game.getMoves()) {
            game.executeMove(m);
            MMTreeNode next = new MMTreeNode(game, m);
            //next.appendMeta("playerNum=%d, scale=%d, depth=%d", m.playerNum, scale, depth);
            root.addChild(next);
            long v;
            if (game.getCurPlayerNum() == m.getPlayerNum()) {
                // this means we have more move options
                v = buildTree(game, next, depth, scale);
            } else if (depth > 0) {
                v = buildTree(game, next, depth-1, scale * -1);
            } else {
                v = evauate(game, next, m.getPlayerNum()) * scale;
            }
            next.setValue(v);
            //next.appendMeta("%s:%s", m.type, v);
            //d = Math.max(v, d);
            if (scale < 0)
                d = Math.min(d, v);
            else
                d = Math.max(d, v);
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
     *
     * If moves == 0 then the game is over with value -INF
     *
     * @param game
     * @param t
     * @param playerNum the player for whom the evaluation is considered.
     * @return
     */
    protected abstract long evauate(IGame game, MMTreeNode t, int playerNum);

}
