package cc.lib.swing;

import cc.lib.board.CustomBoard;
import cc.lib.dungeondice.DBoard;

public class DDungeonBuilder extends BoardBuilder {

    @Override
    protected CustomBoard newBoard() {
        return new DBoard();
    }
}
