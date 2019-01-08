package cc.lib.monopoly;

import cc.lib.game.GColor;
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

}
