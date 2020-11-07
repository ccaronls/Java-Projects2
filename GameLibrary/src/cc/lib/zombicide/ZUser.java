package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public abstract class ZUser {

    List<ZCharacter> charactoers = new ArrayList<>();

    void prepareTurn() {
        for (ZCharacter c : charactoers)
            c.prepareTurn();
    }

    public abstract int chooseCharacter(List<Integer> options);
}
