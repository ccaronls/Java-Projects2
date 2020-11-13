package cc.lib.zombicide;

public enum ZSkillLevel {
    BLUE(0, 7),
    YELOW(8, 19),
    ORANGE(20, 42),
    RED(43, Integer.MAX_VALUE);

    ZSkillLevel(int dangerPts, int maxPts) {
        this.dangerPts = dangerPts;
        this.maxPts = maxPts;
    }

    final int dangerPts;
    final int maxPts;

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
}
