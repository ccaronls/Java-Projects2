package cc.lib.monopoly;

import java.util.List;

public class Monopoly {

    List<Player> players;

    public enum MoveType {
        ROLL_DICE,
        PURCHASE,
        BUY_UNIT, // house or hotel
        PAY_BOND,
        FORFEIT,
    }


}
