package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;

/**
 * Created by Chris Caron on 8/24/21.
 */
public class WolfQuestTheEvilTwins extends ZQuest {

    static {
        addAllFields(WolfQuestTheEvilTwins.class);
    }

    int greenObjective =-1;
    int blueObjective =-1;

    static int NUM_TWINS = 2;

    public WolfQuestTheEvilTwins() {
        super(ZQuests.The_Evil_Twins);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:gvd1", "z0:i:ode:ws", "z1:i:ws:ode",            "z2:i:ws:de", "z3:sp", "z4:i:dw:ws:ode",                "z5:i:ws:ode", "z6:i:vd1:ws:we", "z7" },
                { "z0:i:we:ods", "z8", "z9",                            "z10", "z11", "z12",                                    "z13","z14", "z15" },
                { "z16:i:red:we:ods", "z17", "z18:i:wn:ww:ds:ode",      "z19:i:red:wn:we:ods", "z20", "z21:i:wn:ww:de:ods",     "z22", "z23:i:wn:ws:dw:ode", "z24:i:wn" },

                { "z25:i:we:ods", "z26", "z27",                         "z28:i:dw:we:ods", "z29", "z30:i:ww:de:ods",            "z31", "z32:t1:rn", "z33:t2:rn" },
                { "z34:i:ws:we", "z35", "z36:i:wn:ww:we:ods",           "z37:i:ods", "z37:i:wn:ws", "z37:i:ods:we",             "z38", "z39:t3:rw:rn", "z39:t3" },
                { "z40:spw", "z41", "z42:i:ww:ode",                      "z43:i:ds:we", "z44", "z45:i:ds:de:ww",                 "z46", "z39:t3:rw:rs", "z39:t3:rs" },

                { "z48", "z49:t1:rn", "z50:t2:rn:re",                   "z51", "z52", "z53",                                    "z54", "z55", "z56" },
                { "z57:t3:rn", "z57:t3:rn", "z50:t2:re:rs",             "z59", "z60:i:wn:ws:we:ww", "z61:st",                   "z62:i:wn:we:ww", "z63", "z64:i:dw:wn" },
                { "z57:t3:vd2:ws", "z57:t3:re:ws", "z65:sps:ws",        "z66", "z67", "z68:exit",                               "z62:i:red:dw:we", "z69:sps", "z64:i:gvd2:ww" },

                { "", "", "",                                           "z70:v:vd1:wn:ww", "z70:v:wn", "z70:v:wn:we:vd2",       "z71:v:ww:wn:gvd1", "z71:v:wn", "z71:v:gvd2:wn:we" }
        };


        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numThings = getNumStartRedObjectives() + NUM_TWINS + 1;
        int numFound = getNumFoundObjectives();
        int numKilled = game.getNumKills(ZZombieType.Abomination);
        int allInZone = isAllPlayersInExit(game) ? 1 : 0;
        return (numFound + numKilled + allInZone) * 100 / numThings;
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("7R", 180, ZTile.getQuadrant(0, 0, board)),
                new ZTile("4V", 90, ZTile.getQuadrant(0, 3, board)),
                new ZTile("2R", 90, ZTile.getQuadrant(0, 6, board)),

                new ZTile("3V", 90, ZTile.getQuadrant(3, 0, board)),
                new ZTile("5V", 180, ZTile.getQuadrant(3, 3, board)),
                new ZTile("10V", 270, ZTile.getQuadrant(3, 6, board)),

                new ZTile("11V", 180, ZTile.getQuadrant(6, 0, board)),
                new ZTile("6R", 0, ZTile.getQuadrant(6, 3, board)),
                new ZTile("9V", 90, ZTile.getQuadrant(6, 6, board)),
        };
    }

    @Override
    public void init(ZGame game) {
        while (blueObjective == greenObjective) {
            blueObjective = Utils.randItem(getRedObjectives());
            greenObjective = Utils.randItem(getRedObjectives());
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueObjective) {
            game.spawnZombies(blueObjective, ZZombieType.Abomination, 1);
            blueObjective = -1;
        } else if (move.integer == greenObjective) {
            game.spawnZombies(greenObjective, ZZombieType.Abomination, 1);
            greenObjective = -1;
        }
    }

    @Override
    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Abomination:
                return 2;
        }
        return super.getMaxNumZombiesOfType(type);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        List<ZCharacter> chars = Utils.map(game.getAllCharacters(), c -> c.getCharacter());
        int totalChars = chars.size();
        int numInExit = Utils.count(chars, object -> object.getOccupiedZone() == getExitZone());
        int numAbominationsKilled = game.getNumKills(ZZombieType.Abomination);
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Find and eliminate the Evil Twin Abominations hidden among the RED objectives", String.format("%d of %d", numAbominationsKilled, NUM_TWINS))
                        .addRow("2.", "Find BLUE Twin hiddem among RED objectives.", blueObjective >= 0)
                        .addRow("3.", "Find GREEN Twin hidden among RED objectives.", greenObjective >= 0)
                        .addRow("4.", "Get all players into the EXIT zone.", String.format("%d of %d", numInExit, totalChars))
                        .addRow("5.", "Exit zone must be cleared of zombies.")
                        .addRow("6.", "All Players must survive.")
                );
    }
}
