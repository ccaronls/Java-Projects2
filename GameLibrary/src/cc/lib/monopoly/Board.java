package cc.lib.monopoly;

import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

public class Board {

    public static final GColor LIGHT_BLUE  = new GColor(161, 216, 250);
    public static final GColor PURPLE      = new GColor(207, 40, 137);
    public static final GColor ORANGE      = new GColor(243, 133, 33);

    // values based on the board asset. rendered image will be scaled
    public final static float BOARD_DIMENSION = 1500;
    public final static float BOARD_CORRNER_DIMENSION = 200;

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

    private final float dim;
    private final float scale;
    private final float borderDim;
    private final float cellDim;

    public Board(float dim) {
        this.dim = dim;
        scale = dim / BOARD_DIMENSION;
        borderDim = BOARD_CORRNER_DIMENSION * scale;
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
        return cellDim*2/3;
    }

    // TODO: Special case for when a player is in JAIL
    public GRectangle getPiecePlacement(int player, Square sq) {
        int index = sq.ordinal();
        GRectangle rect = getSqaureBounds(sq);
        Vector2D cntr = rect.getCenter();
        float dim = getPieceDimension();
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

    public Position getsQuarePosition(Square sq) {
        switch (sq) {

            case GO:
                return Position.CORNER_BR;
            case MEDITERRANEAN_AVE:
            case COMM_CHEST1:
            case BALTIC_AVE:
            case INCOME_TAX:
            case READING_RR:
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
            case PENNSYLVANIA_RR:
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
            case B_AND_O_RR:
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
            case SHORT_LINE_RR:
            case CHANCE3:
            case PARK_PLACE:
            case LUXURY_TAX:
            case BOARDWALK:
                return Position.RIGHT;
        }
        Utils.unhandledCase(sq);
        return null;
    }

}
