package cc.game.sudoku;

import java.util.Random;

public class Sudoku  {

	private static final int CELL_DIM = 3;

	public static final int EASY 	= 1;
	public static final int MEDIUM 	= 2;
	public static final int HARD 	= 3;
	
	public static final int SUDOKU_DIM = CELL_DIM*CELL_DIM;
	
	private int [][] 	solution; // 	
	private int [][] 	board;
	private int [][] 	fixed;
	
	private Random randGen;

    public Sudoku(Random randGen) {
    	solution = makeMatrix();
    	board = makeMatrix();
    	fixed = makeMatrix();
    	this.randGen = randGen;
    }
    
    public int rand() {
    	return Math.abs(randGen.nextInt());
    }
	
    public void shuffle(int [] array, int num){ 
		for (int i=0; i<1000; i++) {
			int a = rand() % num;
			int b = rand() % num;
			int t = array[a];
			array[a] = array[b];
			array[b] = t;
		}
	}
	
    private int getCell(int row, int col) {
		int cell = (row/CELL_DIM) * CELL_DIM + (col/CELL_DIM);
		return cell;
	}

	public int getBoard(int row, int col) {
		return board[row][col];
	}
	
	public void setBoard(int row, int col, int num) {
		board[row][col] = num;
	}
	
	private boolean isInRow(int row, int num) {
		for (int i=0; i<SUDOKU_DIM; i++) {
			if (getBoard(row, i) == num)
				return true;
		}
		return false;
	}
	
	private boolean isInCol(int col, int num) {
		for (int i=0; i<SUDOKU_DIM; i++) {
			if (getBoard(i, col) == num)
				return true;
		}
		return false;
	}
	
	private boolean isInCell(int cell, int num) {
		for (int i=0; i<SUDOKU_DIM; i++) {
			for (int j=0; j<SUDOKU_DIM; j++) {
				if (getCell(i,j) == cell && getBoard(i,j) == num)
					return true;
			}
		}
		return false;
	}
	
	public boolean canSetSquare(int num, int row, int col) {
		final int cell = getCell(row, col);
		return !isInRow(row, num) &&
		       !isInCol(col, num) &&
		       !isInCell(cell, num);
	}
	
	private int [][] makeMatrix() {
		int [][] matrix = new int[SUDOKU_DIM][];
		for (int i=0; i<SUDOKU_DIM; i++)
			matrix[i] = new int[SUDOKU_DIM];
		zero(matrix); // prob not necc, but cant hurt
		return matrix;
	}
	
	private void zero(int [][] matrix) {
		for (int i=0; i<SUDOKU_DIM; i++)
			for (int j=0; j<SUDOKU_DIM; j++)
				matrix[i][j] = 0;
	}
	
	private void copy(int [][] src, int [][] dest) {
		for (int i=0; i<SUDOKU_DIM; i++)
			for (int j=0; j<SUDOKU_DIM; j++)
				dest[i][j] = src[i][j];		
	}
	
	private boolean generateR(int num, int cell) {
		if (num > SUDOKU_DIM)
			return true;
		if (cell >= SUDOKU_DIM) {
			return generateR(num+1, 0);
		}
		int [] cellPos = new int[SUDOKU_DIM];
		for (int i=0; i<SUDOKU_DIM; i++)
			cellPos[i] = i;
		shuffle(cellPos, SUDOKU_DIM);
		for (int i=0; i<SUDOKU_DIM; i++) {
			int cx = cellPos[i] % CELL_DIM;
			int cy = cellPos[i] / CELL_DIM;
			// transform cx, cy to the cell
			int tx = CELL_DIM * (cell%CELL_DIM);
			int ty = CELL_DIM * (cell/CELL_DIM);
			cx += tx;
			cy += ty;
			if (getBoard(cx, cy) == 0 && canSetSquare(num, cx, cy)) {
				setBoard(cx, cy, num);
				if (generateR(num, cell+1)) {
					return true;
				}
				setBoard(cx, cy, 0);
			}
		}
		return false;
	}
	
	private int countOccurances(int num) {
		int count = 0;
		for (int i=0; i<SUDOKU_DIM; i++) {
			for (int j=0; j<SUDOKU_DIM; j++) {
				if (getBoard(i,j) == num)
					count++;
			}
		}
		return count;		
	}
	
	private int countCellElems(int cell) {
		int count = 0;
		for (int i=0; i<SUDOKU_DIM; i++) {
			for (int j=0; j<SUDOKU_DIM; j++) {
				if (getCell(i, j) == cell && getBoard(i,j) > 0)
					count ++;
			}
		}
		return count;		
	}
	
	private int countRowElems(int row) {
	    int count = 0;
	    for (int i=0; i<SUDOKU_DIM; i++) {
	        if (this.getBoard(i, row) > 0)
	            count++;
	    }
	    return count;
	}

    private int countColumnElems(int col) {
        int count = 0;
        for (int i=0; i<SUDOKU_DIM; i++) {
            if (this.getBoard(col, i) > 0)
                count++;
        }
        return count;
    }
	
	private void hide(int difficulty) {
		int numHidden = SUDOKU_DIM*SUDOKU_DIM;
		int minOccurances = 0;
		int minCellElems = 0;
		int minRowColElems = 0;
		switch (difficulty) {
		case EASY:
			numHidden = (numHidden * 1) / 2;
			minOccurances = SUDOKU_DIM / 3;
			minCellElems = SUDOKU_DIM / 3;
			minRowColElems = SUDOKU_DIM / 3 + 1;
			break;

		case MEDIUM:
			numHidden = (numHidden * 5) / 9;
			minOccurances = SUDOKU_DIM / 4;
			minCellElems = SUDOKU_DIM / 4;
            minRowColElems = SUDOKU_DIM / 3;
			break;

		default:
			numHidden = (numHidden * 4) / 7;
			minOccurances = SUDOKU_DIM / 4;
			minCellElems = SUDOKU_DIM / 4;
            minRowColElems = SUDOKU_DIM / 3 - 1;
			break;
		}

		int tries = 0;
		while (tries++ < 10000 && numHidden > 0) {
			final int cx = rand() % SUDOKU_DIM;
			final int cy = rand() % SUDOKU_DIM;
			final int cell = getCell(cx, cy);

			final int num = getBoard(cx, cy);
			if (num == 0)
				continue;

			if (countOccurances(num) <= minOccurances)
				continue;

			if (countCellElems(cell) <= minCellElems)
				continue;
			
			if (countRowElems(cy) <= minRowColElems || countColumnElems(cx) <= minRowColElems)
			    continue;

			setBoard(cx, cy, 0);
			numHidden--;
		}		
		
	}
	
	private void setFixed() {
		zero(fixed);
		for (int i=0; i<SUDOKU_DIM; i++) {
			for (int j=0; j<SUDOKU_DIM; j++) {
				if (getBoard(i,j) != 0) {
					fixed[i][j] = 1;
		      	}
		   	}
		}
	}
	
	/**
	 * 
	 *
	 */
	public void solve() {
		this.copy(solution, board);
	}
	
	/**
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	public boolean canEdit(int row, int col) {
		return fixed[row][col] == 0;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isSolved() {
		for (int i=0; i<SUDOKU_DIM; i++) {
			for (int j=0; j<SUDOKU_DIM; j++) {
				if (getBoard(i,j) == 0)
					return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param difficulty
	 */
	public void generate(int difficulty) {
		zero(board);
		zero(fixed);
		zero(solution);
		generateR(1,0);
		copy(board, solution);
		hide(difficulty);
		setFixed();
	}

}
