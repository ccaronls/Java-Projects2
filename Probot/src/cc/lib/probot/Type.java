package cc.lib.probot;

import cc.lib.annotation.Keep;

@Keep
public enum Type {
    EM("Empty", null),  // EMPTY
    DD("Coin", null),  // DOT
    SE("Start East", Direction.Right),  // Start facing east
    SS("Start South", Direction.Down),  // Start facing south
    SW("Start West", Direction.Left),  // Start facing west
    SN("Start North", Direction.Up),  // Start facing north
    LH0("Horz Lazer Red", null), // horz lazer on by default
    LV0("Vert Lazer Red", null), // vert lazer on by default
    LB0("Button Red", null), // lazer0 toggle
    LH1("Horz Lazer Blue", null), // horz lazer on by default
    LV1("Vert Lazer Blue", null), // vert lazer on by default
    LB1("Button Blue", null), // lazer1 toggle
    LH2("Horz Lazer Green", null), // horz lazer on by default
    LV2("Vert Lazer Green", null), // vert lazer on by default
    LB2("Button Green", null), // lazer2 toggle
    LB("Button All", null);   // universal lazer toggle

    public final String displayName;

    public final Direction direction;

    Type(String nm, Direction dir) {
        displayName = nm;
        this.direction = dir;
    }
};