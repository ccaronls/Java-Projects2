package cc.lib.zombicide.quests;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

public class WolfQuestKnowYourEnemy extends ZQuest {

    public WolfQuestKnowYourEnemy() {
        super(ZQuests.Know_Your_Enemy);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:we",        "z1:spn", "z3:i:ww", "z4:t1:rw", "z44:t2",    "z5:red:v:vd1:ww:ws" },
                { "z0:i:red:de:ws", "z6",     "z3:we:ds", "z7"     , "z8:t3:rw", "z8:t3" },
                { "z9:exit",        "z10",    "z11",      "z12",     "z8:t3:rs:rw", "z13:i:ww:wn:ws" },

                { "z14:i:dn:red:we:ods", "z15", "z16:i:dn:de:ww:red", "z17", "z18", "z19:spe" },
                { "z20:i:we",       "z21",    "z16:i:ww:we:ods",     "z22",  "z23:i:dn:ww:we:ws:red", "z24" },
                { "z20:i:ds:we",    "z25",    "z26:i:ww:de:ods",     "z27",  "z28",   "z29" },

                { "z30:spw",        "z31:st", "z32:i:ww",            "z33:t2:rw:rn", "z34:t3:rn", "z34:t3:rn" },
                { "z35:i:wn:de:ods","z36",    "z32:i:ww:ws:we:v:vd1",  "z37:t1",       "z34:t3:rw:rs", "z34:t3:rs" },
                { "z38:i:odn:we",   "z39",    "z40",                 "z41",          "z42",         "z43" }

        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getNumStartObjectives() + game.getAllCharacters().size();
        int numCompleted = getNumFoundObjectives();
        for (ZCharacter c : game.board.getAllCharacters()) {
            if (c.getOccupiedZone() == getExitZone())
                numCompleted++;
        }
        int percentCompleted = numCompleted*100 / numTasks;
        if (game.board.getZombiesInZone(getExitZone()).size() > 0)
            percentCompleted --;
        return percentCompleted;
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("9V", 270, ZTile.getQuadrant(0, 0)),
                new ZTile("11R", 90, ZTile.getQuadrant(0, 3)),

                new ZTile("4R", 180, ZTile.getQuadrant(3, 0)),
                new ZTile("6R", 0, ZTile.getQuadrant(3, 3)),

                new ZTile("1R", 0, ZTile.getQuadrant(6, 0)),
                new ZTile("10V", 180, ZTile.getQuadrant(6, 3)),

        };
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        int totalChars = game.getAllCharacters().size();
        int numInZone = Utils.count(game.board.getAllCharacters(), object -> object.getOccupiedZone() == getExitZone());
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("", "Use the Towers for cover to execute ranged attacks on enemies")
                        .addRow("1.", "Collect all Objectives", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                        .addRow("2.", "A Random Artifact is in the Vault - Go get it!", getNumFoundVaultItems() > 0)
                        .addRow("3.", "Get all players into the EXIT zone.", String.format("%d of %d", numInZone, totalChars))
                        .addRow("4.", "Exit zone must be cleared of zombies.", isExitClearedOfZombies(game))
                        .addRow("5.", "All Players must survive.")
                );

    }
    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.board.getAllCharacters(), object -> object.isDead()) > 0) {
            return "Not all players survived.";
        }
        return super.getQuestFailedReason(game);
    }

}
