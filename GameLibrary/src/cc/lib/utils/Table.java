package cc.lib.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import cc.lib.game.Utils;

/**
 * Usefull for printing out tables of data
 *
 */
public final class Table {

    public interface Model {
        /**
         * Return 0 for LEFT, 1 for CENTER and 2 for RIGHT
         * @param row
         * @param col
         * @return
         */
        default int getTextAlignment(int row, int col) {
            return 0;
        }

        default String getStringValue(Object obj) {
            if (obj == null)
                return "";
            if (obj instanceof Boolean) {
                return ((Boolean)obj).booleanValue() ? "yes" : "no";
            }
            return obj.toString();
        }
    }

    private final List<String> header;
    private final List<Vector<Object>> rows = new ArrayList<>();
    private final Model model;
    private int totalWidth=0;
    private int totalHeight=0;
    private int padding = 1;
    private boolean border = true;

    public Table() {
        this(new Model() {});
    }

    public Table(Model model) {
        header = new ArrayList<>();
        this.model=model;
    }

    public Table(String ... header) {
        this(header, new Model() {});
    }

    public Table(String [] header, Model model) {
        this.header = new ArrayList<>(Arrays.asList(header));
        this.model = model;
    }

    public Table(Object [][] data) {
        this(data, new Model() {});
    }
    public Table(Object [][] data, Model model) {
        this.header = new ArrayList<>();
        for (Object [] d : data) {
            addRow(d);
        }
        this.model = model;
    }

    public Table setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public Table setNoBorder() {
        this.border = false;
        return this;
    }

    public Table(String [] header, Object [][] data) {
        this(header, data, new Model() {});
    }

    public Table(String [] header, Object [][] data, Model model) {
        this.header = new ArrayList<>(Arrays.asList(header));
        for (Object [] d : data) {
            addRow(d);
        }
        this.model = model;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public int getColumns() {
        int columns = header.size();
        for (List row : rows) {
            columns = Math.max(columns, row.size());
        }
        return columns;
    }

    public Table addRow(Object ... row) {
        rows.add(new Vector(Arrays.asList(row)));
        return this;
    }

    public Table addRow(String label, Object ... items) {
        Vector nv = new Vector();
        nv.add(label);
        nv.addAll(Utils.toList(items));
        rows.add(nv);
        return this;
    }

    public Table addColumn(String header, Object ... items) {
        return addColumn(header, Arrays.asList(items));
    }

    public Table addColumn(String header, List column) {

        int col = this.header.size();
        this.header.add(header);
        for (int i=0; i<column.size(); i++) {
            if (i>=rows.size()) {
                // add a row with
                rows.add(new Vector());
            }
            while (rows.get(i).size()<=col) {
                rows.get(i).add(null);
            }
            rows.get(i).set(col, column.get(i));
        }
        return this;
    }

    public Table addColumnNoHeader(List column) {
        int col = Math.max(header.size(), rows.size() > 0 ? rows.get(0).size() : 0);
        for (int i=0; i<column.size(); i++) {
            if (i>=rows.size()) {
                // add a row with
                rows.add(new Vector());
            }
            while (rows.get(i).size()<=col) {
                rows.get(i).add(null);
            }
            rows.get(i).set(col, column.get(i));
        }
        return this;
    }

    public String toString(int indent) {
        if (header.size() == 0 && rows.isEmpty())
            return "";

        final int columns = getColumns();
        if (columns == 0)
            return "";
        final int [] maxWidth = new int [columns];
        final int [] maxHeight = new int[rows.size()];
        for (int i=0; i<columns && i<header.size(); i++) {
            maxWidth[i] = Math.max(maxWidth[i], header.get(i).length());
        }
        for (int r=0; r<rows.size(); r++) {
            for (int c=0; c<rows.get(r).size(); c++) {
                String entry =  model.getStringValue(rows.get(r).get(c));
                if (entry.indexOf("\n") >= 0) {
                    String [] parts = entry.split("[\n]+");
                    for (String s : parts) {
                        maxWidth[c] = Math.max(maxWidth[c], s.length());
                    }
                    maxHeight[r] = Math.max(maxHeight[r], parts.length);
                    // split up the string into lines for
                } else {
                    maxWidth[c] = Math.max(maxWidth[c], entry.length());
                    maxHeight[r] = Math.max(maxHeight[r], 1);
                }
            }
        }

        final StringBuffer buf = new StringBuffer();
        final String paddingChars = Utils.getRepeatingChars(' ', padding);
        final String divider= paddingChars + (padding > 0 ? "|" : " ") + paddingChars;
        final String indentStr = Utils.getRepeatingChars(' ', indent);
        final String borderStrFront = (border ? "|" : "") + paddingChars;
        final String borderStrEnd   = paddingChars + (border ? "|" : "");

        // Divider under header
        final String headerPadding = Utils.getRepeatingChars('-', padding);
        final String headerDivFront = (border ? "+" : "") + headerPadding;
        final String headerDivMid = headerPadding + "+" + headerPadding;
        final String headerDivEnd = headerPadding + (border ? "+" : "");
        buf.append(indentStr).append(headerDivFront);
        for (int i = 0; i < columns - 1; i++) {
            buf.append(Utils.getRepeatingChars('-', maxWidth[i])).append(headerDivMid);
        }
        final int last = columns - 1;
        buf.append(Utils.getRepeatingChars('-', maxWidth[last])).append(headerDivEnd);
        final String horzDivider = buf.toString();
        if (border) {
            buf.append("\n");
        } else {
            buf.setLength(0);
        }
        String delim = "";
        if (header.size() > 0) {
            // Header
            buf.append(indentStr).append(borderStrFront);
            for (int i = 0; i < columns - 1; i++) {
                if (i < header.size())
                    buf.append(getJustifiedString(header.get(i), 1, maxWidth[i]));
                else
                    buf.append(Utils.getRepeatingChars(' ', maxWidth[i]));
                buf.append(divider);
            }
            if (last < header.size()) {
                buf.append(getJustifiedString(header.get(last), 1, maxWidth[last]));
            } else {
                buf.append(Utils.getRepeatingChars(' ', maxWidth[last]));
            }
            buf.append(borderStrEnd).append("\n");

            buf.append(horzDivider);
            delim = "\n";
        }
        // Cell
        for (int r=0; r<rows.size(); r++) {
            for (int h=0; h<maxHeight[r]; h++) {
                buf.append(delim).append(indentStr).append(borderStrFront);
                delim = "\n";
                for (int c = 0; c < rows.get(r).size()-1; c++) {
                    buf.append(getJustifiedCellString(r, c, h, maxWidth[c]));
                    buf.append(divider);
                }
                int col = rows.get(r).size()-1;
                buf.append(getJustifiedCellString(r, col, h, maxWidth[col]))
                    .append(borderStrEnd);
            }
        }
        if (border) {
            buf.append("\n").append(horzDivider);
        }

        String str = buf.toString();

        totalWidth = Utils.sum(maxWidth) + (getColumns()-1) * (padding*2+1) + (border ? 2 + padding*2 : 0);
        totalHeight = 1;
        int newline = str.indexOf('\n');
        while (newline >= 0) {
            totalHeight++;
            newline = str.indexOf('\n', newline+1);
        }

        return str;
    }

    private String getJustifiedString(String s, int justify, int cellWidth) {
        switch (justify) {
            case 0: {
                if (cellWidth == 0)
                    return "";
                return String.format("%-" + cellWidth + "s", s);
            }
            case 1: {
                int frontPadding = (cellWidth - s.length()) / 2;
                int frontWidth = (cellWidth - frontPadding);
                int backPadding = cellWidth - frontWidth;
                return String.format("%" + frontWidth + "s", s) + Utils.getRepeatingChars(' ', backPadding);
            }
            case 2:
                return String.format("%" + cellWidth + "s", s);
        }
        throw new RuntimeException("Invalid justify: " + justify);
    }

    private String getJustifiedCellString(int r, int c, int h, int maxWidth) {
        String str = getCellString(r, c, h);
        return getJustifiedString(str, model.getTextAlignment(r, c), maxWidth);
    }

    private String getCellString(int r, int c, int h) {
        Object o = rows.get(r).get(c);
        String s = model.getStringValue(o);
        if (s.indexOf('\n') < 0) {
            if (h == 0)
                return s;
            return "";
        }
        String [] parts = s.split("[\n]+");
        if (parts.length > h)
            return parts[h];
        return "";
    }

    /**
     * Only valid after call to toString
     * @return
     */
    public int getTotalWidth() {
        return totalWidth;
    }

    /**
     * Only valid after call to toString
     * @return
     */
    public int getTotalHeight() {
        return totalHeight;
    }
}
