package cc.lib.zombicide.quests;

import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZQuest;

public class ZQuestTutorial extends ZQuest {

    public ZQuestTutorial() {
        super("Tutorial");
    }

    @Override
    public ZBoard loadBoard() {
        // 6x3
        final String [][] map = {
                { "z0:i:wn:ww", "z0:i:wn", "z1:wn:dw:fatty", "z2:i:wn:ws:we", "z3:sp:ww:wn:de", "z4:i:dw:wn:we:ws:exit" },
                { "z5:sp:wn:ww:ws", "z6:ldn:we:walker", "z7:ww:ds:we", "z8:obj:wn:ww:ws", "z9",               "z10:obj:wn:we:ws" },
                { "z11:obj:i:wn:ww:ws:ode", "z12:start:ws:odw:we", "z13:i:ww:ws:dn:runner", "z13:i:wn:we:ws:v", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:v" },
        };

        return load(map);
    }
}
