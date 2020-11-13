package cc.lib.utils;

import cc.lib.game.Utils;

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
 */
public class Grid<T> extends Reflector<Grid<T>> {

    static {
        addAllFields(Grid.class);
        addAllFields(Pos.class);
    }

    public static class Pos extends Reflector<Pos> {

        private final int row, col;

        Pos() {
            this(0,0);
        }

        public Pos(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return col;
        }

        @Override
        public String toString() {
            return "Pos{" +
                    "row=" + row +
                    ", col=" + col +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos pos = (Pos) o;
            return row == pos.row &&
                    col == pos.col;
        }

//        @Override
//        public int hashCode() {
//            return Objects.hash(row, col);
//        }
    }

    public static class Iterator<T> implements java.util.Iterator<T> {
        private int row=0, col=0;
        private final Grid<T> grid;
        private Pos pos;

        Iterator(Grid<T> grid) {
            this.grid = grid;
        }

        @Override
        public boolean hasNext() {
            return row<grid.getRows();
        }

        @Override
        public T next() {
            pos = new Pos(row, col);
            T next = grid.get(pos);
            if (++col == grid.getCols()) {
                col=0;
                row++;
            }
            return next;
        }

        public Pos getPos() {
            return pos;
        }

    }

    public Iterable<T> getCells() {
        return () -> iterator();
    }

    public Iterator<T> iterator() {
        return new Iterator<>(this);
    }

    public Grid() {}

    public Grid(T [][] grid) {
        this.grid = grid;
    }

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
            throw new IllegalArgumentException("Grid cannot have 0 rows or columns");
        if (getRows() >= rows && getCols() >= cols)
            return;
        T [][] newGrid = build(Math.max(getRows(), rows), Math.max(getCols(), cols));
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

    protected T[][] build(int rows, int cols) {
        throw new RuntimeException("Not implemented");
    }

    public T get(int row, int col) {
        return grid[row][col];
    }

    public T get(Pos pos) {
        return grid[pos.row][pos.col];
    }

    public boolean isValid(int row, int col) {
        return row >= 0 && col >= 0 && row < grid.length && col < grid[row].length;
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
        if (grid != null && (grid.length == 0 || grid[0].length == 0)) {
            throw new IllegalArgumentException("Supplied grid has 0 length rows or columns");
        }
        this.grid = grid;
    }

    public void clear() {
        grid = null;
    }

    /**
     * Rebuild the brid with the given size and with the given initial value
     *
     * @param rows
     * @param cols
     * @param filler
     */
    public void init(int rows, int cols, T filler) {
        grid = build(rows, cols);
        fill(grid, filler);
    }
}
