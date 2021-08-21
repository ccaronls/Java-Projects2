package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

@Keep
public class ZSkillLevel extends Reflector<ZSkillLevel> implements Comparable<ZSkillLevel> {

    static {
        addAllFields(ZSkillLevel.class);
    }

    public static boolean ULTRA_RED_MODE = true;

    public final static int NUM_LEVELS = ZColor.values().length;

    private final ZColor color;
    private final int ultra;

    public ZSkillLevel() {
        this(null, -1);
    }

    private ZSkillLevel(ZColor lvl, int ultra) {
        this.color = lvl;
        this.ultra = ultra;
    }

    public ZColor getColor() {
        return color;
    }

    public ZColor getDifficultyColor() {
        if (ultra > 0)
            return ZColor.RED;
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
                return new ZSkillLevel(ZColor.YELLOW, ultra);
            case YELLOW:
                return new ZSkillLevel(ZColor.ORANGE, ultra);
            case ORANGE:
                return new ZSkillLevel(ZColor.RED, ultra);
        }
        return new ZSkillLevel(ZColor.BLUE, ultra+1);
    }

    public static ZSkillLevel getLevel(int expPts) {
        int ultra = 0;
        ZColor lvl = ZColor.RED;
        if (ULTRA_RED_MODE) {
            ultra = expPts / ZColor.RED.maxPts;
            expPts = expPts % ZColor.RED.maxPts;
        }
        for (ZColor sl : ZColor.values()) {
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
        if (color == ZColor.RED)
            return 0;
        return ZColor.values()[color.ordinal()+1].dangerPts - curPts;
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
