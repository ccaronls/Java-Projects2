package cc.lib.risk;

/**
 * Created by Chris Caron on 9/13/21.
 */
public enum Action {

    CANCEL,
    ATTACK,
    MOVE,
    END,

    ONE_ARMY,
    TWO_ARMIES,
    THREE_ARMIES;

    int getArmies() {
        switch (this) {
            case ONE_ARMY:
                return 1;
            case TWO_ARMIES:
                return 2;
            case THREE_ARMIES:
                return 3;
        }
        return 0;
    }
}
