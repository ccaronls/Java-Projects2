package cc.game.rocketman.core;

import cc.lib.game.AGraphics;
import cc.lib.reflector.Reflector;

public class RocketManLevel extends Reflector {

	static {
		addAllFields(RocketManLevel.class);
	}
	
	public enum CellType {
		NONE,
		BLOCK
	}
	
	// dimensions of a single cell as drawn on the screen
	private int cellWidth = 10;
	private int cellHeight = 10;
	
	private CellType [][] cells = new CellType[64][32]; // column major
	
	public RocketManLevel() {
		for (int i=0; i<cells.length; i++) {
			for (int ii=0; ii<cells[i].length; ii++) {
				cells[i][ii] = CellType.NONE;
			}
		}
	}
	
	public int getNumCellColumns() {
		return cells.length;
	}
	
	public int getNumCellRows() {
		return cells[0].length;
	}
	
	public int getCellWidth() {
		return cellWidth;
	}
	
	public int getCellHeight() {
		return cellHeight;
	}
	
	/**
     * Load textures, etc.
     * @param g
     */
    public void initLevel(AGraphics g) {
        
    }
    
    /**
     * Render a frame
     * 
     * @param g
     */
    public void drawLevel(AGraphics g) {
        
    }

	public void setCell(int col, int row, CellType block) {
		if (col >= 0 && col < this.getNumCellColumns() && row >= 0 && row < this.getNumCellRows()) {
			cells[col][row] = block;
		}
	}

	public CellType getCellType(int col, int row) {
		if (col >= 0 && col < this.getNumCellColumns() && row >= 0 && row < this.getNumCellRows()) {
			return cells[col][row];
		}
		return CellType.NONE;
	}
	
}
