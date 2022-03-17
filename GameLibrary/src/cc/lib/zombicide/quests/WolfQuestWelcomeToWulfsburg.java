package cc.lib.zombicide.quests;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

public class WolfQuestWelcomeToWulfsburg extends ZQuest {

    static {
        addAllFields(WolfQuestWelcomeToWulfsburg.class);
    }

    int blueKeyZone, greenKeyZone;

    public WolfQuestWelcomeToWulfsburg() {
        super(ZQuests.Welcome_to_Wulfsberg);
    }

    @Override
    public ZBoard loadBoard() {

        final String [][] map = {
                { "z0:exit:i:de:ws", "z1:spn",          "z2:i:ww:ws:ode",       "z3:i:ode:ds",      "z4:i:ode:ods",     "z5:i:ods" },
                { "z6",              "z7",              "z8",                   "z9",               "z10:i:ww:ods",     "z10:i:ods" },
                { "z11:i:wn:ode:ws", "z12:i:wn:ws",     "z12:i:wn:ods:de",      "z13:i:wn:ode",     "z14:i:we",         "z15:i:odn" },
                { "z16:i",           "z16:i",           "z16:i:we",             "z17:t2:rn",         "z18:t3:rn",         "z18:t3:rn" },
                { "z16:i:ds",        "z16:i:ws",        "z16:i:we:ws",          "z19:t1:re",         "z18:t3:rs",         "z18:t3:rs" },
                { "z20:spw",         "z21",             "z22",                  "z23",              "z24",              "z25:spe" },
                { "z26",             "z27:i:ww:wn:ws:ode", "z28:i:red:dn:de:ws", "z29",             "z30:i:dw:red:wn:ws:ode", "z31:i:dn:ws" },
                { "z32",             "z33",             "z34",                  "z35:st",           "z36",              "z37" },
                { "z38:i:wn:ode",    "z39:i:dn:red:we", "z40:sps",              "z41:i:dw:wn:red",  "z42:i:wn:we",      "z43" }
        };

        return load(map);
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("5R", 180, ZTile.getQuadrant(0, 0, board)),
                new ZTile("8V", 180, ZTile.getQuadrant(0, 3, board)),

                new ZTile("1V", 270, ZTile.getQuadrant(3, 0, board)),
                new ZTile("10V", 180, ZTile.getQuadrant(3, 3, board)),

                new ZTile("6V", 0, ZTile.getQuadrant(6, 0, board)),
                new ZTile("3V", 180, ZTile.getQuadrant(6, 3, board))


        };
    }

    @Override
    public void init(ZGame game) {
        blueKeyZone = Utils.randItem(getRedObjectives());
        do {
            greenKeyZone = Utils.randItem(getRedObjectives());
        } while (greenKeyZone == blueKeyZone);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        int totalChars = game.getAllCharacters().size();
        int numInZone = Utils.count(game.getBoard().getAllCharacters(), object -> object.getOccupiedZone() == getExitZone());
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("", "Use the Towers for cover to execute ranged attacks on enemies")
                        .addRow("1.", "Collect all Objectives", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                        .addRow("2.", "Find the BLUE objective hidden among RED objectives for a random vault item.", blueKeyZone < 0)
                        .addRow("3.", "Find the GREEN objective hidden among RED objectives for a random vault item.", greenKeyZone < 0)
                        .addRow("4.", "Get all players into the EXIT zone.", String.format("%d of %d", numInZone, totalChars))
                        .addRow("5.", "Exit zone must be cleared of zombies.")
                        .addRow("6.", "All Players must survive.")
                );
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == blueKeyZone) {
            ZEquipment e = getRandomVaultArtifact();
            game.addLogMessage(c.getLabel() + " has found the BLUE key and also a " + e.getLabel());
            game.giftEquipment(c, e);
            blueKeyZone = -1;
        } else if (c.getOccupiedZone() == greenKeyZone) {
            ZEquipment e = getRandomVaultArtifact();
            game.addLogMessage(c.getLabel() + " has found the GREEN key and also a " + e.getLabel());
            game.giftEquipment(c, e);
            greenKeyZone = -1;
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getNumStartObjectives() + game.getAllCharacters().size();
        int numCompleted = getNumFoundObjectives();
        for (ZPlayerName c : game.getAllCharacters()) {
            if (c.getCharacter().getOccupiedZone() == getExitZone())
                numCompleted++;
        }
        int percentCompleted = numCompleted*100 / numTasks;
        if (game.getBoard().getZombiesInZone(getExitZone()).size() > 0)
            percentCompleted --;
        return percentCompleted;
    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.getAllCharacters(), object -> object.getCharacter().isDead()) > 0) {
            return "Not all players survived.";
        }
        return super.getQuestFailedReason(game);
    }

}
