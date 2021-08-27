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

    ZSkillLevel(ZColor lvl, int ultra) {
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
        if (ultra != o.ultra)
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
        if (ULTRA_RED_MODE)
            return new ZSkillLevel(ZColor.YELLOW, ultra+1);
        return new ZSkillLevel(ZColor.RED, ultra);
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
        if (lvl == ZColor.BLUE && ultra > 0)
            return new ZSkillLevel(ZColor.RED, ultra-1);
        return new ZSkillLevel(lvl, ultra);
    }

    public int getPtsToNextLevel(int curPts) {
        if (ULTRA_RED_MODE) {
            curPts = curPts % ZColor.RED.dangerPts;
        } else if (color == ZColor.RED)
            return 0;
        int idx = (color.ordinal()+1)%NUM_LEVELS;
        if (idx==0)
            idx++;
        return ZColor.values()[idx].dangerPts - curPts;
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
