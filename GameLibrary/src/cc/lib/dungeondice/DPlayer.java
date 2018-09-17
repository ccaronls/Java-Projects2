package cc.lib.dungeondice;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class DPlayer extends Reflector<DPlayer> {

    int hp, dex, str, spd, def, attack; // stats

    enum Pet {

    };

    Pet pet;
    int cellIndex; // position on borad

    protected DMove chooseMove(DMove ... moves) {
        return moves[Utils.randRange(0, moves.length-1)];
    }

    public void init() {
        hp = Utils.max(
                Utils.randRange(2, 12),
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
}
