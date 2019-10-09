package cc.lib.probot;

public enum Type {
        EM("Empty"),  // EMPTY
        DD("Coin"),  // DOT
        SE("Start East"),  // Start facing east
        SS("Start South"),  // Start facing south
        SW("Start West"),  // Start facing west
        SN("Start North"),  // Start facing north
        LH0("Horz Lazer Red"), // horz lazer on by default
        LV0("Vert Lazer Red"), // vert lazer on by default
        LB0("Button Red"), // lazer0 toggle
        LH1("Horz Lazer Blue"), // horz lazer on by default
        LV1("Vert Lazer Blue"), // vert lazer on by default
        LB1("Button Blue"), // lazer1 toggle
        LH2("Horz Lazer Green"), // horz lazer on by default
        LV2("Vert Lazer Green"), // vert lazer on by default
        LB2("Button Green"), // lazer2 toggle
        LB("Button All");   // universal lazer toggle

        public final String displayName;

        Type(String nm) {
            displayName = nm;
        }
    };