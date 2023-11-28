package cc.lib.swing

import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.dungeondice.CellType
import cc.lib.dungeondice.DBoard
import cc.lib.dungeondice.DCell
import cc.lib.game.Utils

class AWTDungeonBuilder : AWTBoardBuilder<BVertex, BEdge, DCell, DBoard>() {
	override fun newBoard(): DBoard {
		return DBoard()
	}

	var cellType = CellType.EMPTY
	override fun registerTools() {
		super.registerTools()
		frame.addMenuBarMenu("CELL", *Utils.toStringArray(CellType.values()))
	}

	override fun init(g: AWTGraphics) {
		super.init(g)
		cellType = CellType.valueOf(frame.getStringProperty("cellType", cellType.name))
	}

	override fun onMenuItemSelected(menu: String, subMenu: String) {
		when (menu) {
			"CELL" -> onCellMenu(subMenu)
			else -> super.onMenuItemSelected(menu, subMenu)
		}
	}

	fun onCellMenu(item: String?) {
		cellType = CellType.valueOf(item!!)
		frame.setProperty("cellType", cellType.name)
		repaint()
	}

	/*
    @Override
    protected void pickCellSingleSelect() {
        super.pickCellSingleSelect();
        if (getSelectedIndex() >= 0) {
            DCell cell = board.getCell(getSelectedIndex());
            cell.setType(cellType);
        }
    }
*/
	override val propertiesFileName: String = "ddbuilder.properties"

	override val defaultBoardFileName: String = "ddungeon.backup.board"

	override fun getDisplayData(lines: MutableList<String>) {
		super.getDisplayData(lines)
		lines.add(cellType.name)
		if (highlightedIndex >= 0) {
			when (pickMode) {
				PickMode.CELL -> lines.add((board.getCell(highlightedIndex) as DCell).cellType.name)
				else -> Unit
			}
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			AWTDungeonBuilder()
		}
	}
}