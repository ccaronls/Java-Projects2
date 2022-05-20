package cc.applets.zombicide;

import cc.lib.board.CustomBoard;
import cc.lib.swing.AWTBoardBuilder;

public class AWTZombicideBoardBuilder extends AWTBoardBuilder {

    @Override
    protected CustomBoard newBoard() {
        return null;
    }

    @Override
    protected String getPropertiesFileName() {
        return "zombicide.bb.properties";
    }

    @Override
    protected String getDefaultBoardFileName() {
        return "zombicide.backup.board";
    }
}
