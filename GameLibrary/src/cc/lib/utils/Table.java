package cc.lib.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Justify;
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
        default Justify getTextAlignment(int row, int col) {
            return Justify.LEFT;
        }

        default String getStringValue(Object obj) {
            if (obj == null)
                return "";
            if (obj instanceof Boolean) {
                return ((Boolean)obj).booleanValue() ? "yes" : "no";
            }
            return obj.toString();
        }

        default float getCornerRadius() {
            return 5;
        }

        default GColor getBorderColor(AGraphics g) {
            return g.getColor();
        }

        default GColor getHeaderColor(AGraphics g) {
            return g.getColor();
        }

        default GColor getCellColor(AGraphics g, int row, int col) {
            return g.getColor();
        }

        default GColor getBackgroundColor() {
            return GColor.TRANSLUSCENT_BLACK;
        }
    }

    private final List<String> header;
    private final List<Vector<Object>> rows = new ArrayList<>();
    private final Model model;
    private int totalWidth=0;
    private int totalHeight=0;
    private int padding = 1;
    private int borderWidth = 2;
    private float[] maxWidth;
    private float[] maxHeight;


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
        this.borderWidth = 0;
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

    public GDimension getDimension(AGraphics g) {
        if (header.size() == 0 && rows.isEmpty())
            return GDimension.EMPTY;

        final int columns = getColumns();
        if (columns == 0)
            return GDimension.EMPTY;

        maxWidth = new float[columns];
        maxHeight = new float[rows.size()];
        float cellPadding = Math.max(4, padding*g.getTextHeight()/2);

        float headrHeight = 0;
        if (header != null && header.size() > 0) {
            for (int i = 0; i < columns && i < header.size(); i++) {
                maxWidth[i] = Math.max(maxWidth[i], g.getTextWidth(header.get(i)));
            }
            headrHeight += g.getTextHeight() + cellPadding*2;
        }
        for (int r = 0; r < rows.size(); r++) {
            for (int c = 0; c < rows.get(r).size(); c++) {
                Object o = rows.get(r).get(c);
                if (o instanceof Table) {
                    GDimension d2 = ((Table)o).getDimension(g);
                    maxHeight[r] = Math.max(maxHeight[r], d2.height);
                    maxWidth[c] = Math.max(maxWidth[c], d2.width);
                } else {
                    String entry = model.getStringValue(o);
                    if (entry.indexOf("\n") >= 0) {
                        String[] parts = entry.split("[\n]+");
                        for (String s : parts) {
                            maxWidth[c] = Math.max(maxWidth[c], g.getTextWidth(s));
                        }
                        maxHeight[r] = Math.max(maxHeight[r], g.getTextHeight() * parts.length);
                        // split up the string into lines for
                    } else {
                        maxWidth[c] = Math.max(maxWidth[c], g.getTextWidth(entry));
                        maxHeight[r] = Math.max(maxHeight[r], g.getTextHeight());
                    }
                }
            }
        }
        maxWidth[0] += cellPadding/2;
        maxWidth[maxWidth.length-1] += cellPadding/2;
        for (int i=1; i<maxWidth.length-1; i++) {
            maxWidth[i] += cellPadding;
        }
        if (borderWidth > 0 && maxWidth.length > 1) {
            maxWidth[0] += cellPadding/2;
            maxWidth[maxWidth.length-1] += cellPadding/2;
        }
        float dimWidth = Utils.sum(maxWidth);
        float dimHeight = Utils.sum(maxHeight) + headrHeight;
        dimWidth += borderWidth*2;
        dimHeight += borderWidth*2;

        return new GDimension(dimWidth, dimHeight);
    }

    public GDimension draw(AGraphics g) {
        GDimension dim = getDimension(g);
        if (dim == GDimension.EMPTY)
            return dim;

        g.pushMatrix();
        float outerPadding = 0;
        if (borderWidth > 0) {
            g.translate(borderWidth, borderWidth);
            // if there is a border, then there is padding arounf between border and text
            GColor cur = g.getColor();
            g.setColor(model.getBackgroundColor());
            float radius = model.getCornerRadius();
            g.drawFilledRoundedRect(0, 0, dim.width, dim.height, radius);
            g.setColor(cur);
            g.setColor(model.getBorderColor(g));
            g.drawRoundedRect(dim, borderWidth, radius);
            g.translate(borderWidth, borderWidth);
        } else {
            // if there is no border then no padding
        }

        // TODO: Draw vertical divider lines

        // check for header. if so render and draw a divider line
        float cellPadding = Math.max(4, padding*g.getTextHeight()/2);
        if (header != null && header.size() > 0) {
            g.setColor(model.getHeaderColor(g));
            float x=outerPadding;
            for (int i=0; i<header.size(); i++) {
                g.drawJustifiedString(x + maxWidth[i]/2, 0, Justify.CENTER, header.get(i));
                x += maxWidth[i];
            }
            g.translate(0, g.getTextHeight() + cellPadding);
            g.drawLine(0, 0, dim.width-borderWidth, 0, borderWidth);
            g.translate(0, cellPadding);
        }

        // draw the rows
        for (int i=0; i<rows.size(); i++) {
            g.pushMatrix();
            for (int ii=0; ii<rows.get(i).size(); ii++) {
                Object o = rows.get(i).get(ii);
                if (o != null) {
                    if (o instanceof Table) {
                        ((Table)o).draw(g);
                    } else {
                        String txt = model.getStringValue(o);
                        Justify hJust = model.getTextAlignment(i, ii);
                        g.setColor(model.getCellColor(g, i, ii));
                        g.drawWrapString(0, 0, maxWidth[ii], hJust, Justify.TOP, txt);
                    }
                }
                g.translate(maxWidth[ii], 0);
            }
            g.popMatrix();
            g.translate(0, maxHeight[i]);
        }

        g.popMatrix();
        return dim;
    }

    /**
     *
     * @param indent
     * @return
     */
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

        final boolean border = borderWidth > 0;
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
                    buf.append(getJustifiedString(header.get(i), Justify.CENTER, maxWidth[i]));
                else
                    buf.append(Utils.getRepeatingChars(' ', maxWidth[i]));
                buf.append(divider);
            }
            if (last < header.size()) {
                buf.append(getJustifiedString(header.get(last), Justify.CENTER, maxWidth[last]));
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

    private String getJustifiedString(String s, Justify justify, int cellWidth) {
        switch (justify) {
            case LEFT: {
                if (cellWidth == 0)
                    return "";
                return String.format("%-" + cellWidth + "s", s);
            }
            case CENTER: {
                int frontPadding = (cellWidth - s.length()) / 2;
                int frontWidth = (cellWidth - frontPadding);
                int backPadding = cellWidth - frontWidth;
                return String.format("%" + frontWidth + "s", s) + Utils.getRepeatingChars(' ', backPadding);
            }
            case RIGHT:
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
