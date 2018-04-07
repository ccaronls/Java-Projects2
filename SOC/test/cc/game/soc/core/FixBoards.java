package cc.game.soc.core;

import junit.framework.TestCase;

import java.io.File;

/**
 * Created by chriscaron on 4/5/18.
 */

public class FixBoards extends TestCase {

    public void testTrimSavedBoardsAndResave() throws Exception {

        File[] files = new File("assets/boards").listFiles();
        for (File file : files) {
            Board b = new Board();
            b.loadFromFile(file);
            b.trim();
            b.saveToFile(file);
        }

    }



    public void testFixScenarios() throws Exception {
        File [] files = new File("assets/scenarios").listFiles();
        for (File f : files) {
            Scenario s = new Scenario();
            s.loadFromFile(f);
            s.mBoard.trim();
            s.saveToFile(f);
        }

    }

}
