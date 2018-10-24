package cc.lib.dungeondice;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

public class DPlayer extends DEntity {

    int playerNum;
    boolean key=false;

    public boolean hasKey() {
        return key;
    }

    int cellIndex; // position on borad
    int backCellIndex; // index of cell we came from to prevent going backward
    GColor color;

    protected DMove chooseMove(DMove ... moves) {
        return moves[Utils.randRange(0, moves.length-1)];
    }

    public void init() {
        hp = Utils.max(
                Utils.randRange(2, 12),
                Utils.randRange(2, 12),
                Utils.randRange(2, 12)
        );

        dex = Utils.max(
                Utils.randRange(1, 6),
                Utils.randRange(1, 6)
        );

        str = Utils.randRange(1, 6);

        spd = Utils.randRange(1, 6);
    }

    @Override
    public String getName() {
        return "Player " + (playerNum+1);
    }

    public GColor getColor() {
        return color;
    }

    public boolean rollDice() {
        return true;
    }

    @Override
    public void draw(AGraphics g, float radius) {
        g.setColor(getColor());
        g.setLineWidth(2);
        g.drawCircle(0, -1.5f, 0.5f);
        g.begin();
        g.vertexArray(new float [][] {
                { 0, -1 },
                { 0, .5f },
                { -1, -.5f },
                {  1, -.5f },
                { 0, .5f },
                { -1, 2 },
                { 0, .5f },
                { 1, 2 }
        });
        g.drawLines();
    }
}
