package cc.lib.monopoly;

import cc.lib.game.GColor;

public class Board {

    public static final GColor LIGHT_BLUE  = new GColor(161, 216, 250);
    public static final GColor PURPLE      = new GColor(207, 40, 137);
    public static final GColor ORANGE      = new GColor(243, 133, 33);

    // there are 36 squares
    public enum Square {
        GO(Type.OTHER, 0, null, 0),
        MEDITERRANEAN_AVE(Type.PROPERTY, 60, GColor.BROWN, 50),
        COMM_CHEST1(Type.COMM_CHEST, 0, null, 0),
        BALTIC_AVE(Type.PROPERTY, 60, GColor.BROWN, 50),
        INCOME_TAX(Type.TAX, 200, null, 0),
        READING_RR(Type.RAIL_ROAD, 200, null, 0),
        ORIENTAL_AVE(Type.PROPERTY, 100, LIGHT_BLUE, 50),
        CHANCE1(Type.CHANCE, 0, null, 0),
        VERMONT_AVE(Type.PROPERTY, 100, LIGHT_BLUE, 50),
        CONNECTICUT_AVE(Type.PROPERTY, 120, LIGHT_BLUE, 50),
        VISITING_JAIL(Type.OTHER, 0, null, 0), // actual jail is a special state
        ST_CHARLES_PLACE(Type.PROPERTY, 140, PURPLE, 100),
        ELECTRIC_COMPANY(Type.UTILITY, 150, null, 0),
        STATES_AVE(Type.PROPERTY, 140, PURPLE, 100),
        VIRGINIA_AVE(Type.PROPERTY, 160, PURPLE, 100),
        PENNSYLVANIA_RR(Type.RAIL_ROAD, 200, null, 0),
        ST_JAMES_PLACE(Type.PROPERTY, 180, ORANGE, 100),
        COMM_CHEST2(Type.COMM_CHEST, 0, null, 0),
        TENNESSEE_AVE(Type.PROPERTY, 180, ORANGE, 100),
        NEW_YORK_AVE(Type.PROPERTY, 200, ORANGE, 100),
        FREE_PARKING(Type.OTHER, 0, null, 0),
        KENTUCKY_AVE(Type.PROPERTY, 220, GColor.RED, 150),
        CHANCE2(Type.CHANCE, 0, null, 0),
        INDIANA_AVE(Type.PROPERTY, 220, GColor.RED, 150),
        ILLINOIS_AVE(Type.PROPERTY, 240, GColor.RED, 150),
        B_AND_O_RR(Type.RAIL_ROAD, 200, null, 0),
        ATLANTIC_AVE(Type.PROPERTY, 260, GColor.YELLOW, 150),
        VENTNOR_AVE(Type.PROPERTY, 260, GColor.YELLOW, 150),
        WATER_WORKS(Type.UTILITY, 150, null, 0),
        MARVIN_GARDINS(Type.PROPERTY, 280, GColor.YELLOW, 150),
        GOTO_JAIL(Type.OTHER, 0, null, 0),
        PACIFIC_AVE(Type.PROPERTY, 300, GColor.GREEN, 200),
        NORTH_CAROLINA_AVE(Type.PROPERTY, 300, GColor.GREEN, 200),
        COMM_CHEST3(Type.COMM_CHEST, 0, null, 0),
        PENNSYLVANIA_AVE(Type.PROPERTY, 320, GColor.GREEN, 200),
        SHORT_LINE_RR(Type.RAIL_ROAD, 200, null, 0),
        CHANCE3(Type.CHANCE, 0, null, 0),
        PARK_PLACE(Type.PROPERTY, 350, GColor.BLUE, 200),
        LUXURY_TAX(Type.TAX, 100, null, 0),
        BOARDWALK(Type.PROPERTY, 400, GColor.BLUE, 200);

        Square(Type type, int price, GColor color, int unitPrice) {
            this.type = type;
            this.price = price;
            this.color = color;
            this.unitPrice = unitPrice;
        }

        final int price;
        final int unitPrice;
        final Type type;
        final GColor color;
    }

    public enum Type {
        PROPERTY,
        COMM_CHEST,
        TAX,
        CHANCE,
        RAIL_ROAD,
        UTILITY,
        OTHER
    }


}
