package cc.lib.utils;

import cc.lib.game.Utils;

/**
 * Usefull for printing out tables of data
 *
 */
public class Table {

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
            return obj.toString();
        }

        default boolean hasBorder() {
            return true;
        }

        /**
         * Return 0 for no divider
         * @return
         */
        default int cellPadding() {
            return 1;
        }
    }

    final String [] header;
    final Object [][] data;
    final int [] maxWidth;
    final int [] maxHeight;
    final Model model;

    public Table(String [] header, Object [][] data) {
        this(header, data, new Model() {});
    }

    public Table(String [] header, Object [][] data, Model model) {
        this.header = header;
        this.data = data;
        this.model = model;
        maxWidth = new int [header.length];
        maxHeight = new int[data.length];
        for (int i=0; i<header.length; i++) {
            maxWidth[i] = Math.max(maxWidth[i], header[i].length());
        }
        for (int r=0; r<data.length; r++) {
            for (int c=0; c<data[r].length; c++) {
                String entry =  model.getStringValue(data[r][c]);
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
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int indent) {
        final boolean border = model.hasBorder();
        final String padding = Utils.getRepeatingChars(' ', model.cellPadding());
        final String divider= padding + "|" + padding;
        final StringBuffer buf = new StringBuffer();
        final String indentStr = Utils.getRepeatingChars(' ', indent);
        final String borderStrFront = (border ? "|" : "") + padding;
        final String borderStrEnd   = padding + (border ? "|" : "");

        // Divider under header
        final String headerPadding = Utils.getRepeatingChars('-', model.cellPadding());
        final String headerDivFront = (border ? "+" : "") + headerPadding;
        final String headerDivMid   = headerPadding + "+" + headerPadding;
        final String headerDivEnd   = headerPadding + (border ? "+" : "");
        buf.append(indentStr).append(headerDivFront);
        for (int i=0; i<header.length-1; i++) {
            buf.append(Utils.getRepeatingChars('-', maxWidth[i])).append(headerDivMid);
        }
        final int last = header.length-1;
        buf.append(Utils.getRepeatingChars('-', maxWidth[last])).append(headerDivEnd);
        final String horzDivider = buf.toString();

        if (border) {
            buf.append("\n");
        } else {
            buf.setLength(0);
        }
        // Header
        buf.append(indentStr).append(borderStrFront);
        for (int i=0; i<header.length-1; i++) {
            buf.append(getJustifiedString(header[i], 1, maxWidth[i]));
            buf.append(divider);
        }
        buf.append(getJustifiedString(header[last], 1, maxWidth[last]))
            .append(borderStrEnd)
            .append("\n");

        buf.append(horzDivider);
        // Cell
        for (int r=0; r<data.length; r++) {
            for (int h=0; h<maxHeight[r]; h++) {
                buf.append("\n").append(indentStr).append(borderStrFront);
                for (int c = 0; c < data[r].length-1; c++) {
                    buf.append(getJustifiedCellString(r, c, h));
                    buf.append(divider);
                }
                buf.append(getJustifiedCellString(r, data[r].length-1, h))
                    .append(borderStrEnd);
            }
        }
        if (border) {
            buf.append("\n").append(horzDivider);
        }

        return buf.toString();
    }

    private String getJustifiedString(String s, int justify, int cellWidth) {
        switch (justify) {
            case 0:
                return String.format("%-" + cellWidth + "s", s);
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

    private String getJustifiedCellString(int r, int c, int h) {
        String str = getCellString(r, c, h);
        return getJustifiedString(str, model.getTextAlignment(r, c), maxWidth[c]);
    }

    private String getCellString(int r, int c, int h) {
        Object o = data[r][c];
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
}
