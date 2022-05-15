package cc.lib.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import cc.lib.game.Utils;

/**
 * A grid is a 2D array of generic type with methods to perform operations
 * On its elements as well as the size of the grid
 *
 * @param <T>
 */
public final class Grid<T> extends Reflector<Grid<T>> {

    static {
        addAllFields(Grid.class);
        addAllFields(Pos.class);
    }

    public static class Pos extends Reflector<Pos> {

        private final int row, col;

        public Pos() {
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

        @Override
        public int hashCode() {
            return Utils.hashCode(row, col);
        }

        public int getIndex() {
            return row << 16 | col;
        }

        public static Pos fromIndex(int index) {
            return new Pos(index >>> 16, index & 0xffff);
        }

        public boolean isAdjacentTo(Pos pos) {
            if (row == pos.getRow()) {
                return Math.abs(col-pos.getColumn()) == 1;
            } else if (col == pos.getColumn()) {
                return Math.abs(row - pos.getRow()) == 1;
            }
            return false;
        }
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

        public void set(T value) {
            grid.set(pos.row, pos.col, value);
        }
    }

    private List<List<T>> grid = null;

    /**
     *
     */
    public Grid() {}

    /**
     *
     * @param rows
     * @param cols
     */
    public Grid(int rows, int cols) {
        this(rows, cols, null);
    }

    public Grid(int rows, int cols, T fillValue) {
        this.grid = build(rows, cols, fillValue);
    }

    /**
     *
     * @param elems
     */
    public Grid(T [][] elems) {
        this.grid = new ArrayList<>(elems.length);
        for (T [] t : elems) {
            List<T> row = new ArrayList<>(t.length);
            for (T e : t) {
                row.add(e);
            }
            grid.add(row);
        }
    }

    /**
     *
     * @return
     */
    public Iterable<T> getCells() {
        return () -> iterator();
    }

    /**
     *
     * @return
     */
    public Iterator<T> iterator() {
        return new Iterator<>(this);
    }

    /**
     *
     * @return
     */
    public int getRows() {
        return grid == null ? 0 : grid.size();
    }

    /**
     *
     * @return
     */
    public int getCols() {
        if (grid == null || grid.size() == 0)
            return 0;
        return grid.get(0).size();
    }

    /**
     *
     * @param rows
     * @param cols
     * @param fillValue
     */
    public void ensureCapacity(int rows, int cols, T fillValue) {
        if (rows<=0 || cols<=0)
            throw new IllegalArgumentException("Grid cannot have 0 rows or columns");
        if (getRows() >= rows && getCols() >= cols)
            return;

        List<List<T>> newGrid = build(Math.max(getRows(), rows), Math.max(getCols(), cols), fillValue);
        for (int i=0; i<getRows(); i++) {
            for (int ii=0; ii<getCols(); ii++) {
                newGrid.get(i).set(ii, get(i, ii));
            }
        }
        grid = newGrid;
    }

    public void fill(T fillValue) {
        for (List<T> l : grid) {
            for (int i=0; i<l.size(); i++) {
                l.set(i, fillValue);
            }
        }
    }

    private static <T> List<List<T>> build(int rows, int cols, T fillValue) {
        List<List<T>> grid = new ArrayList<>(rows);
        for (int i=0; i<rows; i++) {
            List l = new Vector(cols);
            for (int ii=0; ii<cols; ii++) {
                l.add(fillValue);
            }
            grid.add(l);
        }
        return grid;
    }

    /**
     *
     * @param row
     * @param col
     * @return
     */
    public T get(int row, int col) {
        return grid.get(row).get(col);
    }

    /**
     *
     * @param pos
     * @return
     */
    public T get(Pos pos) {
        if (pos == null)
            throw new NullPointerException();
        return grid.get(pos.row).get(pos.col);
    }

    /**
     *
     * @param row
     * @param col
     * @return
     */
    public boolean isValid(int row, int col) {
        return row >= 0 && col >= 0 && row < getRows() && col < getCols();
    }

    /**
     *
     * @param row
     * @param col
     * @param value
     */
    public void set(int row, int col, T value) {
        grid.get(row).set(col, value);
    }

    /**
     *
     * @param grid
     */
    public void assignTo(T [][] grid) {
        for (int i=0; i<grid.length; i++) {
            for (int ii=0; i<grid[0].length; ii++) {
                grid[i][ii] = get(i, ii);
            }
        }
    }

    /**
     *
     * @param empty
     */
    public void minimize(T ... empty) {
        if (grid == null)
            return;
        int minRow=Integer.MAX_VALUE;
        int maxRow=Integer.MIN_VALUE;
        int minCol= Integer.MAX_VALUE;
        int maxCol=Integer.MIN_VALUE;

        for (int i=0; i<getRows(); i++) {
            for (int ii=0; ii<getCols(); ii++) {
                if (get(i, ii) != null && Utils.linearSearch(empty, get(i, ii)) < 0) {
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

        List<List<T>> newGrid = build(maxRow-minRow, maxCol-minCol, null);
        for (int i=minRow; i<maxRow; i++) {
            for (int ii=minCol; ii<maxCol; ii++) {
                newGrid.get(i-minRow).set(ii-minCol, get(i, ii));
            }
        }
        grid = newGrid;
    }

    /**
     *
     * @param grid
     */
    public void setGrid(T [][] grid) {
        if (grid != null && (grid.length == 0 || grid[0].length == 0)) {
            throw new IllegalArgumentException("Supplied grid has 0 length rows or columns");
        }
        this.grid = build(grid.length, grid[0].length, null);
        for (int i=0; i<grid.length; i++) {
            for (int ii=0; ii<grid[0].length; ii++) {
                set(i, ii, grid[i][ii]);
            }
        }
    }

    /**
     *
     */
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
        grid = build(rows, cols, filler);
    }

    /**
     *
     * @param pos
     * @return
     */
    public boolean isOnGrid(Pos pos) {
        return pos.getRow() >= 0 && pos.getRow() < getRows() && pos.getColumn() >= 0 && pos.getColumn() < getCols();
    }

    /**
     *
     * @param row
     * @param col
     * @return
     */
    public boolean isOnGrid(int row, int col) {
        return row >= 0 && row < getRows() && col >= 0 && col < getCols();
    }

    public boolean isEmpty() {
        return getRows()==0 && getCols()==0;
    }
}
