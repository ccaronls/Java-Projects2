package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

@Keep
public class ZSkillLevel extends Reflector<ZSkillLevel> implements Comparable<ZSkillLevel> {

    static {
        addAllFields(ZSkillLevel.class);
    }

    public static boolean ULTRA_RED_MODE = true;

    public enum Color {
        BLUE(0, 7, GColor.BLUE),
        YELLOW(8, 19, GColor.YELLOW),
        ORANGE(20, 42, GColor.ORANGE),
        RED(43, 43, GColor.RED);

        Color(int dangerPts, int maxPts, GColor color) {
            this.dangerPts = dangerPts;
            this.maxPts = maxPts;
            this.color = color;
        }

        public final int dangerPts;
        public final int maxPts;
        public final GColor color;
    }

    public final static int NUM_LEVELS = Color.values().length;

    private final Color color;
    private final int ultra;

    public ZSkillLevel() {
        this(null, -1);
    }

    private ZSkillLevel(Color lvl, int ultra) {
        this.color = lvl;
        this.ultra = ultra;
    }

    public Color getColor() {
        return color;
    }

    public Color getDifficultyColor() {
        if (ultra > 0)
            return Color.RED;
        return color;
    }

    public int getUltra() {
        return ultra;
    }

    @Override
    public int compareTo(ZSkillLevel o) {
        if (ultra > o.ultra)
            return Integer.compare(ultra, o.ultra);
        return color.compareTo(o.color);
    }

    public ZSkillLevel nextLevel() {
        switch (color) {
            case BLUE:
                return new ZSkillLevel(Color.YELLOW, ultra);
            case YELLOW:
                return new ZSkillLevel(Color.ORANGE, ultra);
            case ORANGE:
                return new ZSkillLevel(Color.RED, ultra);
        }
        return new ZSkillLevel(Color.BLUE, ultra+1);
    }

    public static ZSkillLevel getLevel(int expPts) {
        int ultra = 0;
        Color lvl = Color.RED;
        if (ULTRA_RED_MODE) {
            ultra = expPts / Color.RED.maxPts;
            expPts = expPts % Color.RED.maxPts;
        }
        for (Color sl : Color.values()) {
            if (expPts <= sl.maxPts) {
                lvl = sl;
                break;
            }
        }
        return new ZSkillLevel(lvl, ultra);
    }

    public int getPtsToNextLevel(int curPts) {
        if (ULTRA_RED_MODE) {

        }
        if (color == Color.RED)
            return 0;
        return Color.values()[color.ordinal()+1].dangerPts - curPts;
    }

    public int getDangerPts() {
        return color.dangerPts;
    }

    @Override
    public String toString() {
        return color.name() + Utils.getRepeatingChars('+', ultra);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZSkillLevel that = (ZSkillLevel) o;
        return ultra == that.ultra &&
                color == that.color;
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(color, ultra);
    }
}
