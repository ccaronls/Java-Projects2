package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public abstract class ZUser {

    final List<ZCharacter> characters = new ArrayList<>();

    void prepareTurn() {
        for (ZCharacter c : characters)
            c.prepareTurn();
    }

    public void addCharacter(ZCharacter c) {
        characters.add(c);
    }

    public abstract Integer chooseCharacter(List<Integer> options);

    public abstract ZMove chooseMove(ZGame zGame, ZCharacter cur, List<ZMove> options);

    public abstract ZSkill chooseNewSkill(ZGame game, ZCharacter character, List<ZSkill> skillOptions);

    public abstract Integer chooseZoneForBile(ZGame zGame, ZCharacter cur, List<Integer> accessable);
}
