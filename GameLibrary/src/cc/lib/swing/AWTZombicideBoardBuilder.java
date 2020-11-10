package cc.lib.swing;

import cc.lib.board.CustomBoard;

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
