package cc.lib.monopoly;

import cc.lib.game.GColor;

public class Board {

    public static final GColor LIGHT_BLUE  = new GColor(161, 216, 250);
    public static final GColor PURPLE      = new GColor(207, 40, 137);
    public static final GColor ORANGE      = new GColor(243, 133, 33);

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
