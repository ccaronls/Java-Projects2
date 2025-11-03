package cc.applets.zombicide

import cc.lib.board.BCell
import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.board.CustomBoard
import cc.lib.swing.AWTBoardBuilder

class AWTZombicideBoardBuilder : AWTBoardBuilder<BVertex, BEdge, BCell, CustomBoard<BVertex, BEdge, BCell>>(
	"zombicide.bb.properties", "zombicide.backup.board"
) {
	override fun newBoard(): CustomBoard<BVertex, BEdge, BCell> {
		TODO()
	}
}