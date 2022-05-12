package cc.lib.monopoly;

import cc.lib.game.GColor;
import cc.lib.game.Utils;

// there are 36 squares
public enum Square {
    GO                      (SquareType.OTHER,              0,  Board.WHITE),
    MEDITERRANEAN_AVE       (SquareType.PROPERTY,           23, Board.BROWN, 60, 50, 2, 10, 30, 90, 160, 250),
    COMM_CHEST1             (SquareType.COMMUNITY_CHEST,    0,  Board.WHITE),
    BALTIC_AVE              (SquareType.PROPERTY,           17, Board.BROWN, 60, 50, 4, 20, 60, 180, 320, 450),
    INCOME_TAX              (SquareType.TAX,                0,  Board.WHITE, 200),
    READING_RAILROAD        (SquareType.RAIL_ROAD,          5,  Board.WHITE, 200),
    ORIENTAL_AVE            (SquareType.PROPERTY,           21, Board.LIGHT_BLUE, 100, 50, 6, 30, 90, 270, 400, 550),
    CHANCE1                 (SquareType.CHANCE,             0,  Board.WHITE),
    VERMONT_AVE             (SquareType.PROPERTY,           8,  Board.LIGHT_BLUE, 100, 50, 6, 30, 90, 270, 400, 550),
    CONNECTICUT_AVE         (SquareType.PROPERTY,           7,  Board.LIGHT_BLUE, 120, 50, 8, 40, 100, 300, 450, 600),
    VISITING_JAIL           (SquareType.OTHER,              0,  Board.WHITE), // actual jail is a special state
    ST_CHARLES_PLACE        (SquareType.PROPERTY,           16, Board.PURPLE, 140, 100, 10, 50, 150, 450, 625, 750),
    ELECTRIC_COMPANY        (SquareType.UTILITY,            25, Board.WHITE, 150),
    STATES_AVE              (SquareType.PROPERTY,           15, Board.PURPLE, 140, 100, 10, 50, 150, 450, 625, 750),
    VIRGINIA_AVE            (SquareType.PROPERTY,           18, Board.PURPLE, 160, 100, 12, 60, 180, 500, 700, 900),
    PENNSYLVANIA_RAILROAD   (SquareType.RAIL_ROAD,          10, Board.WHITE, 200),
    ST_JAMES_PLACE          (SquareType.PROPERTY,           3,  Board.ORANGE, 180, 100, 14, 70, 200, 550, 750, 950),
    COMM_CHEST2             (SquareType.COMMUNITY_CHEST,    0,  Board.WHITE),
    TENNESSEE_AVE           (SquareType.PROPERTY,           2,  Board.ORANGE, 180, 100, 14, 70, 200, 550, 750, 950),
    NEW_YORK_AVE            (SquareType.PROPERTY,           1,  Board.ORANGE, 200, 100, 16, 80, 220, 600, 800, 1000),
    FREE_PARKING            (SquareType.OTHER,              0,  Board.WHITE),
    KENTUCKY_AVE            (SquareType.PROPERTY,           6,  Board.RED, 220, 150, 18, 90, 250, 700, 875, 1050),
    CHANCE2                 (SquareType.CHANCE,             0,  Board.WHITE),
    INDIANA_AVE             (SquareType.PROPERTY,           9,  Board.RED, 220, 150, 18, 90, 250, 700, 875, 1050),
    ILLINOIS_AVE            (SquareType.PROPERTY,           4,  Board.RED, 240, 150, 20, 100, 300, 750, 925, 1100),
    B_AND_O_RAILROAD        (SquareType.RAIL_ROAD,          13, Board.WHITE, 200),
    ATLANTIC_AVE            (SquareType.PROPERTY,           12, Board.YELLOW, 260, 150, 22, 110, 330, 800, 975, 1150),
    VENTNOR_AVE             (SquareType.PROPERTY,           11, Board.YELLOW, 260, 150, 22, 110, 330, 800, 975, 1150),
    WATER_WORKS             (SquareType.UTILITY,            22, Board.WHITE, 150),
    MARVIN_GARDINS          (SquareType.PROPERTY,           20, Board.YELLOW, 280, 150, 24, 120, 360, 850, 1025, 1200),
    GOTO_JAIL               (SquareType.OTHER,              0,  Board.WHITE),
    PACIFIC_AVE             (SquareType.PROPERTY,           28, Board.GREEN, 300, 200, 26, 130, 390, 900, 1100, 1275),
    NORTH_CAROLINA_AVE      (SquareType.PROPERTY,           24, Board.GREEN, 300, 200, 26, 130, 390, 900, 1100, 1300),
    COMM_CHEST3             (SquareType.COMMUNITY_CHEST,    0,  Board.WHITE),
    PENNSYLVANIA_AVE        (SquareType.PROPERTY,           27, Board.GREEN, 320, 200, 28, 150, 450, 1000, 1200, 1400),
    SHORT_LINE_RAILROAD     (SquareType.RAIL_ROAD,          19, Board.WHITE, 200),
    CHANCE3                 (SquareType.CHANCE,             0,  Board.WHITE),
    PARK_PLACE              (SquareType.PROPERTY,           26, Board.BLUE, 350, 200, 35, 175, 500, 1100, 1300, 1500),
    LUXURY_TAX              (SquareType.TAX,                0,  Board.WHITE, 100),
    BOARDWALK               (SquareType.PROPERTY,           14, Board.BLUE, 400, 200, 50, 200, 600, 1400, 1700, 2000);

    Square(SquareType type, int rank, GColor color, int ... costs) {
        this.type = type;
        this.rank = rank;
        this.color = color;
        this.costs = costs;
        Utils.assertTrue(costs.length == 0 || costs.length == 1 || costs.length == 8);
    }

    private final SquareType type;
    private final int rank; // rank according to: https://www.ranker.com/list/all-monopoly-properties/steve-wright
    private final GColor color;
    private final int [] costs;
    static int maxRank = 0;

    public int getTax() {
        return costs[0];
    }

    public int getPrice() {
        return costs[0];
    }

    public int getUnitPrice() {
        if (costs.length > 1)
            return costs[1];
        return 0;
    }

    public boolean isProperty() {
        return type == SquareType.PROPERTY;
    }

    public boolean canPurchase() {
        switch (type) {
            case PROPERTY:
            case UTILITY:
            case RAIL_ROAD:
                return true;
        }
        return false;
    }

    public int getMortgageValue(int upgrades) {
        return getPrice()/2 + getUnitPrice()*upgrades/2;
    }

    public int getMortgageBuybackPrice() {
        return getMortgageValue(0)+getPrice()/10;
    }

    // return rent given number of houses on the property.
    // 1 Hotel = 5 houses
    public int getRent(int houses) {
        if (2+houses >= costs.length)
            throw new IndexOutOfBoundsException("Cannot determine cost for " + houses + " houses of square " + name());
        return costs[2+houses];
    }

    public int getNumForSet() {
        if (isRailroad())
            return 4;

        if (isUtility())
            return 2;

        switch (this) {
            case BALTIC_AVE:
            case MEDITERRANEAN_AVE:
            case PARK_PLACE:
            case BOARDWALK:
                return 2;

            default:
                if (isProperty())
                    return 3;
        }
        return 0;
    }

    public boolean isRailroad() {
        switch (this) {
            case READING_RAILROAD:
            case B_AND_O_RAILROAD:
            case PENNSYLVANIA_RAILROAD:
            case SHORT_LINE_RAILROAD:
                return true;
        }
        return false;
    }

    public boolean isUtility() {
        switch (this) {
            case ELECTRIC_COMPANY:
            case WATER_WORKS:
                return true;
        }
        return false;
    }

    public GColor getColor() {
        return color;
    }

    public SquareType getType() {
        return type;
    }

    public int getRank() {
        return rank;
    }

    public static int getMaxRank() {
        if (maxRank <= 0) {
            for (Square sq : values()) {
                maxRank = Math.max(maxRank, sq.rank);
            }
        }
        return maxRank;
    }
}
