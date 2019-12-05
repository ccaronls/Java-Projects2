package cc.lib.checkerboard;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.DescisionTree;
import cc.lib.game.IGame;
import cc.lib.game.MiniMaxTree;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class AIPlayer extends Player {

    private final static Logger log = LoggerFactory.getLogger(AIPlayer.class);

    @Omit
    private int numNodes = 0;
    @Omit
    private long startTime = 0;
    @Omit
    private LinkedList<Move> moveList = new LinkedList<>();
    @Omit
    private MiniMaxTree mmt = new MiniMaxTree() {
        @Override
        protected long evaluate(IGame game, DescisionTree t) {
            return ((Game)game).getRules().evaluate(((Game)game), (Move)t.getMove());
        }

        @Override
        protected void onNewNode(IGame game, DescisionTree node) {
            onMoveEvaluated((Game)game, (Move)node.getMove());
        }

        @Override
        protected long getZeroMovesValue(IGame game) {
            return ((Game)game).getRules().getZeroMovesValue((Game)game);
        }
    };

    boolean isThinking() {
        return startTime > 0;
    }

    long getThinkingTime() {
        return Math.max(0, System.currentTimeMillis() - startTime);
    }

    synchronized void buildMovesList(Game game) {

        // see if opponent followed our desc tree. If they did, then no need to rebuild
        if (!moveList.isEmpty() && moveList.getFirst().getPlayerNum() != game.getTurn()) {
            List<Move> history = game.getMoveHistory();
            for (Move m : history) {
                if (m.equals(moveList.peek())) {
                    Move popped = moveList.pop();
                    log.debug("Popping:" + popped);
                    if (moveList.isEmpty())
                        break;
                } else {
                    break;
                }
            }
        }

        if (moveList.isEmpty() || moveList.getFirst().getPlayerNum() != game.getTurn()) {
            log.debug("Building decision tree");
            startTime = System.currentTimeMillis();
            moveList.clear();
            numNodes = 0;
            DescisionTree root = new DescisionTree();
            mmt.buildTree(game, root, 4); // TODO: Difficulty
            float buildTimeSecs = (float)(System.currentTimeMillis() - startTime) / 1000;
            log.debug(String.format("%d moves evaluated in %5.2f seconds", numNodes, buildTimeSecs));
            DescisionTree dt = root.findDominantChild();
            while (dt.getParent() != null) {
                if (dt.getParent()==null)
                    throw new NullPointerException();
                moveList.addFirst((Move)dt.getMove());
                dt = dt.getParent();
            }
            if (moveList.size() == 0)
                throw new AssertionError("Failed to create a move list");
            //log.debug("MoveList:" + moveList);
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
            onMoveListGenerated(Collections.unmodifiableList(moveList));
            startTime = 0;
        } else {
            log.debug("Skipping rebuild");
        }
        log.debug("Next Move:" + moveList.getFirst());
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
        mmt.killNoBlock();
    }

    /**
     *
     * @param game
     * @param move
     */
    protected void onMoveEvaluated(Game game, Move move) {
        numNodes++;
        //if (numNodes % 1000 == 0)
        //    System.out.print('.');
    }

    protected void onMoveListGenerated(List<Move> moveList) {}
}
