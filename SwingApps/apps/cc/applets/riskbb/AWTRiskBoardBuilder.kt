package cc.lib.swing

import cc.lib.board.BEdge
import cc.lib.board.BVertex
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.risk.Army
import cc.lib.risk.Region
import cc.lib.risk.RiskBoard
import cc.lib.risk.RiskCell
import java.awt.event.KeyEvent

/**
 * Created by Chris Caron on 9/13/21.
 */
class AWTRiskBoardBuilder : AWTBoardBuilder<BVertex, BEdge, RiskCell, RiskBoard>() {

	val region: Region
		get() = frame.getEnumProperty("region", Region.AFRICA)
	val army: Army
		get() = frame.getEnumProperty("army", Army.BLUE)

	override fun newBoard(): RiskBoard {
		return RiskBoard()
	}

	override val propertiesFileName: String = "risk_builder.properties"

	override val defaultBoardFileName: String = "risk.backup.board"

	override val boardFileExtension: String = "board"

	override fun drawCellMode(g: APGraphics, mouseX: Int, mouseY: Int) {
		super.drawCellMode(g, mouseX, mouseY)
		if (highlightedIndex >= 0) {
			g.color = GColor.CYAN
			val cell = board!!.getCell(highlightedIndex)
			g.begin()
			for (adj in board.getConnectedCells(cell)) {
				g.vertex(cell)
				g.vertex(board.getCell(adj))
			}
			g.drawLines(4f)
			g.color = GColor.WHITE
			var str = Utils.toPrettyString(cell.region)
			if (selectedIndex >= 0) {
				str += """
	            	
	            	DIST TO $selectedIndex=${board.getDistance(selectedIndex, highlightedIndex)}
	            	""".trimIndent()
			}
			g.drawJustifiedStringOnBackground(cell, Justify.CENTER, Justify.CENTER, str, GColor.TRANSLUSCENT_BLACK, 2f, 2f)
		}
	}

	override fun registerTools() {
		super.registerTools()
		registerTool(object : Tool("CONNECT CELLS") {
			override fun onActivated() {
				setPickMode(PickMode.CELL)
				setMultiselect(false)
			}

			override fun onPick() {
				val current = selectedIndex
				super.onPick()
				if (pickMode == PickMode.CELL && !multiSelect) {
					val selectedIndex = selectedIndex
					if (current >= 0 && selectedIndex >= 0 && current != selectedIndex) {
						// create a connection between the cells
						val r0 = board.getCell(current)
						if (r0.connectedCells.contains(selectedIndex)) {
							r0.connectedCells.remove(selectedIndex as Any)
						} else {
							r0.connectedCells.add(selectedIndex)
						}
						val r1 = board.getCell(selectedIndex)
						if (r1.connectedCells.contains(current)) {
							r1.connectedCells.remove(current as Any)
						} else {
							r1.connectedCells.add(current)
						}
					}
				}
			}
		})
		registerTool(object : Tool("Modify Armies") {
			override fun onActivated() {
				setPickMode(PickMode.CELL)
				setMultiselect(true)
				setShowNumbers(false)
			}

			override fun onKeyTyped(keyCode: Int): Boolean {
				when (keyCode) {
					KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> for (cellIdx in selected) {
						val cell = board.getCell(cellIdx)
						if (cell.occupier == null) cell.occupier = army
						cell.numArmies++
					}
					KeyEvent.VK_MINUS, KeyEvent.VK_UNDERSCORE -> for (cellIdx in selected) {
						val cell = board.getCell(cellIdx)
						cell.numArmies = Math.max(0, cell.numArmies--)
					}
					else -> return false
				}
				return true
			}
		})
	}

	override fun drawExtraCellInfo(g: APGraphics, cell: RiskCell) {
		super.drawExtraCellInfo(g, cell)
		if (cell.numArmies > 0 && cell.occupier != null) {
			g.color = cell.occupier!!.color
			g.drawJustifiedString(cell, "" + cell.numArmies + " Armies")
		}
	}

	override fun initActions() {
		super.initActions()
		addAction(KeyEvent.VK_L, "L", "Assign selected cells to a Continent", { assignToRegion() })
	}

	override fun registerActionBarItems(frame: AWTFrame) {
		frame.addMenuBarMenu("Region", *Utils.toStringArray(Region.values()))
		frame.addMenuBarMenu("Army", *Utils.toStringArray(Army.values()))
	}

	protected override fun onCellAdded(cell: RiskCell) {
		cell.region = region
	}

	override fun onMenuItemSelected(menu: String, subMenu: String) {
		when (menu) {
			"Region" -> {
				frame.setProperty("region", subMenu)
			}
			else -> super.onMenuItemSelected(menu, subMenu)
		}
	}

	override fun getDisplayData(lines: MutableList<String>) {
		super.getDisplayData(lines)
		lines.add("Region: $region Army: $army")
	}

	private fun assignToRegion() {
		if (pickMode == PickMode.CELL && multiSelect) {
			var currentRegion: String? = null
			for (idx in selected) {
				val cell = board.getCell(idx)
				if (currentRegion == null) {
					currentRegion = cell.region.name
				} else if (cell.region.name != currentRegion) {
					currentRegion = null
					break
				}
			}
			val idx = frame.showItemChooserDialog("Set Region", null, currentRegion, *Utils.toStringArray(Region.values()))
			if (idx >= 0) {
				for (cellIdx in selected) {
					board!!.getCell(cellIdx).region = Region.values()[idx]
				}
			}
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			AWTRiskBoardBuilder()
		}
	}
}