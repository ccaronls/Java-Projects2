package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GColor;

@Keep
public enum ZSkillLevel {
    BLUE(0, 7, GColor.BLUE),
    YELLOW(8, 19, GColor.YELLOW),
    ORANGE(20, 42, GColor.ORANGE),
    RED(43, Integer.MAX_VALUE, GColor.RED);

    ZSkillLevel(int dangerPts, int maxPts, GColor color) {
        this.dangerPts = dangerPts;
        this.maxPts = maxPts;
        this.color = color;
    }

    final int dangerPts;
    final int maxPts;
    final GColor color;

    public static ZSkillLevel getLevel(int expPts) {
        for (ZSkillLevel sl : values()) {
            if (expPts <= sl.maxPts)
                return sl;
        }
        return RED;
    }

    public int getPtsToNextLevel(int curPts) {
        if (this == RED)
            return 0;
        return values()[ordinal()+1].dangerPts - curPts;
    }

    public GColor getColor() {
        return color;
    }

    public int getDangerPts() {
        return dangerPts;
    }
}
