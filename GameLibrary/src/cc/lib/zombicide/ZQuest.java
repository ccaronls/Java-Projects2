package cc.lib.zombicide;

public class ZQuest {

    public final String name;

    private ZQuest(String name) {
        this.name = name;
    }

    public static ZQuest loadTutorial(ZGame game) {
        // 6x3
        final String [][] quest = {
                { "z0:i:wn:ww:ws", "z0:i::wn:ws:lde0:ds", "z1:wn:dw:fatty", "z2:i:wn:ws:we", "z3:sp:ww:wn:de", "z4:i:dw:wn:we:ws:exit" },
                { "z5:sp:wn:ww:ws", "z6:ldn0:we:walker", "z7:ww:ds:we", "z8:obj:wn:ww:ws", "z9",               "z10:obj:wn:we:ws" },
                { "z11:key0:i:wn:ww:ws:ode", "z12:start:ws:odw:we", "z13:i:ww:ws:dn:runner", "z13:i:wn:we:ws:v", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:v" },
        };

        game.board = new ZBoard();
        game.board.load(quest);

        return new ZQuest("Tutorial");
    }
}
