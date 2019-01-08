package cc.lib.monopoly;

import cc.lib.game.GColor;

// there are 36 squares
public enum Square {
    GO(Board.Type.OTHER, GColor.WHITE),
    MEDITERRANEAN_AVE(Board.Type.PROPERTY, GColor.BROWN, 60, 50, 2, 10, 30, 90, 160, 250),
    COMM_CHEST1(Board.Type.COMM_CHEST, GColor.WHITE),
    BALTIC_AVE(Board.Type.PROPERTY, GColor.BROWN, 60, 50, 4, 20, 60, 180, 320, 450),
    INCOME_TAX(Board.Type.TAX, GColor.WHITE, 200),
    READING_RR(Board.Type.RAIL_ROAD, GColor.WHITE, 200),
    ORIENTAL_AVE(Board.Type.PROPERTY, Board.LIGHT_BLUE, 100, 50, 6, 30, 90, 270, 400, 550),
    CHANCE1(Board.Type.CHANCE, GColor.WHITE),
    VERMONT_AVE(Board.Type.PROPERTY, Board.LIGHT_BLUE, 100, 50, 6, 30, 90, 270, 400, 550),
    CONNECTICUT_AVE(Board.Type.PROPERTY, Board.LIGHT_BLUE, 120, 50, 8, 40, 100, 300, 450, 600),
    VISITING_JAIL(Board.Type.OTHER, GColor.WHITE), // actual jail is a special state
    ST_CHARLES_PLACE(Board.Type.PROPERTY, Board.PURPLE, 140, 100, 10, 50, 150, 450, 625, 750),
    ELECTRIC_COMPANY(Board.Type.UTILITY, GColor.WHITE, 150),
    STATES_AVE(Board.Type.PROPERTY, Board.PURPLE, 140, 100, 10, 50, 150, 450, 625, 750),
    VIRGINIA_AVE(Board.Type.PROPERTY, Board.PURPLE, 160, 100, 12, 60, 180, 500, 700, 900),
    PENNSYLVANIA_RR(Board.Type.RAIL_ROAD, GColor.WHITE, 200),
    ST_JAMES_PLACE(Board.Type.PROPERTY, Board.ORANGE, 180, 100, 14, 70, 200, 550, 750, 950),
    COMM_CHEST2(Board.Type.COMM_CHEST, GColor.WHITE),
    TENNESSEE_AVE(Board.Type.PROPERTY, Board.ORANGE, 180, 100, 14, 70, 200, 550, 750, 950),
    NEW_YORK_AVE(Board.Type.PROPERTY, Board.ORANGE, 200, 100, 16, 80, 220, 600, 800, 1000),
    FREE_PARKING(Board.Type.OTHER, GColor.WHITE),
    KENTUCKY_AVE(Board.Type.PROPERTY, GColor.RED, 220, 150, 18, 90, 250, 700, 875, 1050),
    CHANCE2(Board.Type.CHANCE, GColor.WHITE),
    INDIANA_AVE(Board.Type.PROPERTY, GColor.RED, 220, 150, 18, 90, 250, 700, 875, 1050),
    ILLINOIS_AVE(Board.Type.PROPERTY, GColor.RED, 240, 150, 20, 100, 300, 750, 925, 1100),
    B_AND_O_RR(Board.Type.RAIL_ROAD, GColor.WHITE, 200),
    ATLANTIC_AVE(Board.Type.PROPERTY, GColor.YELLOW, 260, 150, 22, 110, 330, 800, 975, 1150),
    VENTNOR_AVE(Board.Type.PROPERTY, GColor.YELLOW, 260, 150, 22, 110, 330, 800, 975, 1150),
    WATER_WORKS(Board.Type.UTILITY, GColor.WHITE, 150),
    MARVIN_GARDINS(Board.Type.PROPERTY, GColor.YELLOW, 280, 150, 24, 120, 360, 850, 1025, 1200),
    GOTO_JAIL(Board.Type.OTHER, GColor.WHITE),
    PACIFIC_AVE(Board.Type.PROPERTY, GColor.GREEN, 300, 200, 26, 130, 390, 900, 1100, 1275),
    NORTH_CAROLINA_AVE(Board.Type.PROPERTY, GColor.GREEN, 300, 200, 26, 130, 390, 900, 1100),
    COMM_CHEST3(Board.Type.COMM_CHEST, GColor.WHITE),
    PENNSYLVANIA_AVE(Board.Type.PROPERTY, GColor.GREEN, 320, 200, 28, 150, 450, 1000, 1200, 1400),
    SHORT_LINE_RR(Board.Type.RAIL_ROAD, GColor.WHITE, 200),
    CHANCE3(Board.Type.CHANCE, GColor.WHITE),
    PARK_PLACE(Board.Type.PROPERTY, GColor.BLUE, 350, 200, 35, 175, 500, 1100, 1300, 1500),
    LUXURY_TAX(Board.Type.TAX, GColor.WHITE, 100),
    BOARDWALK(Board.Type.PROPERTY, GColor.BLUE, 400, 200, 50, 200, 600, 1400, 1700, 2000);

    Square(Board.Type type, GColor color, int ... costs) {
        this.type = type;
        this.color = color;
        this.costs = costs;
    }

    final Board.Type type;
    final GColor color;
    private final int [] costs;

    int getTax() {
        return costs[0];
    }

    int getPrice() {
        return costs[0];
    }

    int getHousePrice() {
        return costs[1];
    }

    boolean isProperty() {
        return costs.length>2;
    }

    int getMortgageValue() {
        return getPrice()/2;
    }

    int getMortgageBuybackPrice() {
        return getMortgageValue()+getPrice()/10;
    }

    // return rent given number of houses on the property.
    // 1 Hotel = 5 houses
    int getRent(int houses) {
        return costs[2+houses];
    }

    int getNumForSet() {
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

    boolean isRailroad() {
        switch (this) {
            case READING_RR:
            case B_AND_O_RR:
            case PENNSYLVANIA_RR:
            case SHORT_LINE_RR:
                return true;
        }
        return false;
    }

    boolean isUtility() {
        switch (this) {
            case ELECTRIC_COMPANY:
            case WATER_WORKS:
                return true;
        }
        return false;
    }
}
