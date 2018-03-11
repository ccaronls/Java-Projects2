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
        };

        d.setNumPlayers(2);
        d.initGame(6, 12, 1);
        d.startNewGame();

        Reflector.KEEP_INSTANCES = true;
        d.loadFromFile(new File("dominos.in"));
    }

}
