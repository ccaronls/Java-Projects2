package cc.game.dominos.core;

import junit.framework.TestCase;

import java.io.File;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 3/10/18.
 */

public class TestDeserializeKeepMembers extends TestCase {

    public void test1() throws Exception {
        Dominos d = new Dominos() {
            @Override
            public void redraw() {

            }

            @Override
            protected void onPlayerConnected(Player player) {

            }

            @Override
            protected void onAllPlayersJoined() {

            }
        };


        Reflector.KEEP_INSTANCES = true;
        d.loadFromFile(new File("dominos.save"));
        int size = d.getPool().size();


        d.setNumPlayers(2);
        d.initGame(12, 1000, 1);
        d.startNewGame();

        d.loadFromFile(new File("dominos.save"));
        assertTrue(d.getPool().size() == size);

    }

}
