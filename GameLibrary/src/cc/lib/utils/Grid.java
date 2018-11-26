package cc.lib.utils;

import cc.lib.game.Utils;

public abstract class Grid<T extends Comparable<T>> {

    // row major
    private T [][] grid = null;

    public int getRows() {
        return grid == null ? 0 : grid.length;
    }

    public int getCols() {
        return grid == null ? 0 : grid[0].length;
    }

    public void ensureCapacity(int rows, int cols, T fillValue) {
        if (rows<=0 || cols<=0)
            throw new IllegalArgumentException();
        if (getRows() >= rows && getCols() >= cols)
            return;
        T [][] newGrid = build(rows, cols);
        fill(newGrid, fillValue);
        for (int i=0; i<getRows(); i++) {
            for (int ii=0; ii<getCols(); ii++) {
                newGrid[i][ii] = grid[i][ii];
            }
        }
        grid = newGrid;
    }

    private static <T> void fill(T [][] grid, T fillValue) {
        for (int i=0; i<grid.length; i++) {
            for (int ii=0; ii<grid[0].length; ii++) {
                grid[i][ii] = fillValue;
            }
        }
    }

    protected abstract T[][] build(int rows, int cols);

    public T get(int row, int col) {
        return grid[row][col];
    }

    public void set(int row, int col, T value) {
        grid[row][col] = value;
    }

    public T [][] getGrid() {
        return grid;
    }

    public void minimize(T ... empty) {
        if (grid == null)
            return;
        int minRow=Integer.MAX_VALUE;
        int maxRow=Integer.MIN_VALUE;
        int minCol= Integer.MAX_VALUE;
        int maxCol=Integer.MIN_VALUE;

        for (int i=0; i<getRows(); i++) {
            for (int ii=0; ii<getCols(); ii++) {
                if (grid[i][ii] != null && Utils.linearSearch(empty, grid[i][ii]) < 0) {
                    minRow = Math.min(minRow, i);
                    minCol = Math.min(minCol, ii);
                    maxRow = Math.max(maxRow, i+1);
                    maxCol = Math.max(maxCol, ii+1);
                }
            }
        }

        if (minCol > maxCol || minRow > maxRow) {
            grid = null;
            return;
        }

        if (minRow == 0 && minCol == 0 && maxRow == getRows() && maxCol == getCols())
            return; // nothing to do

        T [][] newGrid = build(maxRow-minRow, maxCol-minCol);
        for (int i=minRow; i<maxRow; i++) {
            for (int ii=minCol; ii<maxCol; ii++) {
                newGrid[i-minRow][ii-minCol] = grid[i][ii];
            }
        }
        grid = newGrid;
    }

    public void setGrid(T [][] grid) {
        this.grid = grid;
    }

    public void clear() {
        grid = null;
    }

    public void init(int rows, int cols, T filler) {
        grid = build(rows, cols);
        fill(grid, filler);
    }
}
