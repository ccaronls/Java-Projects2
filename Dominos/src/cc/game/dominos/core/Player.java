package cc.game.dominos.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.net.ClientConnection;
import cc.lib.utils.Reflector;
import cc.lib.utils.SyncList;

public class Player extends Reflector<Player> {

    static {
        addAllFields(Player.class);
    }

    List<Tile> tiles = new SyncList<>(new ArrayList<Tile>());
    int score;
    private int playerNum = -1; // 0 index
    private String name = null;
    boolean smart = false;

    @Omit
    private ClientConnection connection = null;

    public Player() {}

    public Player(int playerNum) {
        this.playerNum = playerNum;
        this.name = "Player " + (playerNum+1);
    }

    public String getName() {
        if (connection != null) {
            return connection.getName();
        }
        if (name == null)
            name = "Player " + (playerNum+1);
        return name;
    }

    /**
     * 0 index player num
     *
     * @return
     */
    public int getPlayerNum() {
        return playerNum;
    }

    /**
     *
     * @param playerNum
     */
    public void setPlayerNum(int playerNum) {
        this.playerNum = playerNum;
    }

    /**
     *
     * @param conn
     */
    public void connect(ClientConnection conn) {
        this.connection = conn;
    }

    /**
     *
     * @return
     */
    public ClientConnection getConnection() {
        return connection;
    }

    /**
     * Set the display name for this player. name from client connection takes precedence.
     * @param name
     */
    public final void setName(String name) {
        this.name = name;
    }

    @Omit
    final GRectangle outlineRect = new GRectangle();

    final void reset() {
        tiles.clear();
        score = 0;
    }

    final synchronized Tile findTile(int n1, int n2) {
        for (Tile p : tiles) {
            if (p.pip1 == n1 && p.pip2 == n2)
                return p;
            if (p.pip2 == n1 && p.pip1 == n2)
                return p;
        }
        return null;
    }

    /**
     * Override to change behavior. Base method does random pick of availabel choices
     *
     * @param game
     * @param moves
     * @return
     */
    @Keep
    public Move chooseMove(Dominos game, List<Move> moves) {
        if (connection != null && connection.isConnected()) {
            return connection.executeOnRemote(MPConstants.USER_ID, game, moves);
        }
        if (smart) {
            Move best = null;
            int bestPts = 0;
            for (Move m : moves) {
                Board copy = game.getBoard().deepCopy();
                copy.doMove(m.piece, m.endpoint, m.placment);
                int pts = copy.computeEndpointsTotal();
                if (pts % 5 == 0) {
                    if (bestPts < pts) {
                        bestPts = pts;
                        best = m;
                    }
                }
            }
            if (best != null)
                return best;
        }
        return moves.get(Utils.rand() % moves.size());
    }

    /**
     *
     * @return
     */
    public final List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    /**
     *
     * @return
     */
    public final int getScore() {
        return score;
    }

    /**
     *
     * @return
     */
    public boolean isPiecesVisible() { return false; }

}
