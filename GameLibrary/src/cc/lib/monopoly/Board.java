package cc.lib.monopoly;

import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

public class Board {

    public static final GColor WHITE                = GColor.WHITE;
    public static final GColor BROWN                = GColor.BROWN;
    public static final GColor LIGHT_BLUE           = new GColor(161, 216, 250);
    public static final GColor PURPLE               = new GColor(207, 40, 137);
    public static final GColor ORANGE               = new GColor(243, 133, 33);
    public static final GColor RED                  = GColor.RED;
    public static final GColor YELLOW               = GColor.YELLOW;
    public static final GColor GREEN                = GColor.GREEN.darkened(.4f);
    public static final GColor BLUE                 = GColor.BLUE;
    public static final GColor CHANCE_ORANGE        = new GColor(0xFFF67A20);
    public static final GColor COMM_CHEST_BLUE      = new GColor(0xFF86D5F6);
    public static final GColor BOARD_COLOR          = new GColor(0xFFD2E5D2);


    // values based on the board asset. rendered image will be scaled
    public final static float BOARD_DIMENSION = 1500;
    public final static float BOARD_CORNER_DIMENSION = 200;

    public final static Vector2D [] COMM_CHEST_RECT = {
            new Vector2D(265, 447),
            new Vector2D(445, 267),
            new Vector2D(560, 382),
            new Vector2D(381, 558)
    };

    public final static Vector2D [] CHANCE_RECT = {
            new Vector2D(942, 1125),
            new Vector2D(1122, 943),
            new Vector2D(1236, 1059),
            new Vector2D(1057, 1239)
    };

    public final static Vector2D [] CENTER_RECT = {
            new Vector2D(1500/2-150, 1500/2+100),
            new Vector2D(1500/2+150, 1500/2+100),
            new Vector2D(1500/2+150, 1500/2-100),
            new Vector2D(1500/2-150, 1500/2-100),
    };

    private final float dim; // dimension of whole board
    private final float scale;
    private final float borderDim; // dimension of a corner square
    private final float cellDim; // width of short side of a rect

    public Board(float dim) {
        this.dim = dim;
        scale = dim / BOARD_DIMENSION;
        borderDim = BOARD_CORNER_DIMENSION * scale;
        cellDim = (dim - borderDim*2) / 9;
    }

    public GRectangle getSqaureBounds(Square sq) {
        int index = sq.ordinal();
        switch (sq) {
            case FREE_PARKING:
                return new GRectangle(0, 0, borderDim, borderDim);
            case GOTO_JAIL:
                return new GRectangle(dim-borderDim, 0, borderDim, borderDim);
            case GO:
                return new GRectangle(dim-borderDim, dim-borderDim, borderDim, borderDim);
            case VISITING_JAIL:
                return new GRectangle(0, dim-borderDim, borderDim, borderDim);
        }

        if (index < Square.VISITING_JAIL.ordinal()) {
            return new GRectangle(dim - borderDim - cellDim*(index), dim-borderDim, cellDim, borderDim);
        }

        if (index < Square.FREE_PARKING.ordinal()) {
            index -= Square.VISITING_JAIL.ordinal();
            return new GRectangle(0, dim-borderDim - cellDim*(index), borderDim, cellDim);
        }

        if (index < Square.GOTO_JAIL.ordinal()) {
            index -= Square.FREE_PARKING.ordinal();
            return new GRectangle(borderDim + (index-1) * cellDim, 0, cellDim, borderDim);
        }

        index -= Square.GOTO_JAIL.ordinal();
        return new GRectangle(dim-borderDim, borderDim + (index-1)*cellDim, borderDim, cellDim);
    }

    /**
     * Gives the center of the inner edge of the square unless sq is a corner then it gives the inner corner
     * @param sq
     * @return
     */
    public Vector2D getInnerEdge(Square sq) {
        int index = sq.ordinal();
        switch (sq) {
            case FREE_PARKING:
                return new Vector2D(borderDim, borderDim);
            case GOTO_JAIL:
                return new Vector2D(dim-borderDim, borderDim);
            case GO:
                return new Vector2D(dim-borderDim, dim-borderDim);
            case VISITING_JAIL:
                return new Vector2D(borderDim, dim-borderDim);
        }

        if (index < Square.VISITING_JAIL.ordinal()) {
            return new Vector2D(dim - borderDim - cellDim*(index)+cellDim/2, dim-borderDim);
        }

        if (index < Square.FREE_PARKING.ordinal()) {
            index -= Square.VISITING_JAIL.ordinal();
            return new Vector2D(borderDim, dim-borderDim - cellDim*(index)+cellDim/2);
        }

        if (index < Square.GOTO_JAIL.ordinal()) {
            index -= Square.FREE_PARKING.ordinal();
            return new Vector2D(borderDim + (index-1) * cellDim+cellDim/2, borderDim);
        }

        index -= Square.GOTO_JAIL.ordinal();
        return new Vector2D(dim-borderDim, borderDim + (index-1)*cellDim+cellDim/2);
    }

    public float getPieceDimension() {
        return cellDim/2;
    }

    public GRectangle getPiecePlacement(int player, Square sq) {
        float dim = getPieceDimension();
        GRectangle rect = getSqaureBounds(sq);
        Vector2D cntr = rect.getCenter();
        if (sq == Square.VISITING_JAIL) {
            // special case to place around the outer edge
            switch (player) {
                case 0: // left middle/top
                    return new GRectangle(rect.x, cntr.Y()-dim, dim, dim);
                case 1: // bottom middle
                    return new GRectangle(cntr.X()-dim, rect.y+rect.h-dim, dim, dim);
                case 2: // left middle/bottom
                    return new GRectangle(rect.x, cntr.getY(), dim, dim);
            }
            // bottom right
            return new GRectangle(cntr.X(), rect.y+rect.h-dim, dim, dim);
        }
        switch (player) {
            case 0:
                return new GRectangle(cntr.X()-dim, cntr.Y()-dim, dim, dim);
            case 1:
                return new GRectangle(cntr.X(), cntr.Y(), dim, dim);
            case 2:
                return new GRectangle(cntr.X(), cntr.Y()-dim, dim, dim);
        }
        return new GRectangle(cntr.X()-dim, cntr.Y(), dim, dim);
    }

    public GRectangle getPiecePlacementJail(int playerNum) {
        GRectangle rect = getSqaureBounds(Square.VISITING_JAIL);
        float dim = rect.w/4;
        rect.x += dim;
        rect.w -= dim;
        rect.h -= dim;
        dim = getPieceDimension();
        rect.w = dim;
        rect.h = dim;
        switch (playerNum) {
            case 3:
                rect.y += dim;
                break;
            case 1:
                rect.x += dim; rect.y += dim;
                break;
            case 2:
                rect.x += dim;
                break;
        }
        return rect;
    }

    public enum Position {
        CORNER_TL,
        TOP,
        CORNER_TR,
        RIGHT,
        CORNER_BR,
        BOTTOM,
        CORNER_BL,
        LEFT
    }

    // TODO: Make this apart of Square enum
    public Position getSquarePosition(Square sq) {
        switch (sq) {

            case GO:
                return Position.CORNER_BR;
            case MEDITERRANEAN_AVE:
            case COMM_CHEST1:
            case BALTIC_AVE:
            case INCOME_TAX:
            case READING_RAILROAD:
            case ORIENTAL_AVE:
            case CHANCE1:
            case VERMONT_AVE:
            case CONNECTICUT_AVE:
                return Position.BOTTOM;
            case VISITING_JAIL:
                return Position.CORNER_BL;
            case ST_CHARLES_PLACE:
            case ELECTRIC_COMPANY:
            case STATES_AVE:
            case VIRGINIA_AVE:
            case PENNSYLVANIA_RAILROAD:
            case ST_JAMES_PLACE:
            case COMM_CHEST2:
            case TENNESSEE_AVE:
            case NEW_YORK_AVE:
                return Position.LEFT;
            case FREE_PARKING:
                return Position.CORNER_TL;
            case KENTUCKY_AVE:
            case CHANCE2:
            case INDIANA_AVE:
            case ILLINOIS_AVE:
            case B_AND_O_RAILROAD:
            case ATLANTIC_AVE:
            case VENTNOR_AVE:
            case WATER_WORKS:
            case MARVIN_GARDINS:
                return Position.TOP;
            case GOTO_JAIL:
                return Position.CORNER_TR;
            case PACIFIC_AVE:
            case NORTH_CAROLINA_AVE:
            case COMM_CHEST3:
            case PENNSYLVANIA_AVE:
            case SHORT_LINE_RAILROAD:
            case CHANCE3:
            case PARK_PLACE:
            case LUXURY_TAX:
            case BOARDWALK:
                return Position.RIGHT;
        }
        Utils.unhandledCase(sq);
        return null;
    }

    public float getScale() {
        return scale;
    }

}
