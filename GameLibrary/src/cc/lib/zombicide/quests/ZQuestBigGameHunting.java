package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWeaponType;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZone;

public class ZQuestBigGameHunting extends ZQuest {

    static {
        addAllFields(ZQuestBigGameHunting.class);
    }

    @Omit
    String [][] map = {
            { "z0:i:wn:ww:ds:gvd1",  "z29:i:wn:de:ws:red:odw", "z1:spn:wn:de",           "z2:i:wn:ode:ws",   "z3:i:wn:ode:ws",   "z9:i:vd2:wn:we",  "z28:v:wn:we:vd2" },
            { "z5:ww",              "z6",                     "z7",                     "z8:we",            "z9:i:ods",           "z9:i:ods:we:red",     "z28:v:we" },
            { "z10:ww:we:start",    "z11:i:ww:wn::ws:red:ode","z12:i:wn:ds:ode",        "z13:i:wn:red:ds:ode","z14:i:ws:we:odn",   "z15:i:ds:we:odn", "z28:v:we:ws:vd3" },
            { "z16:ww:ds",          "z17",                    "z18",                    "z19",              "z20",                "z21:we:spe:dn:ds", "z27:v:we:gvd1" },
            { "z22:i:ww:we:vd3",    "z23",                    "z24:i:wn:ww:we",         "z25:i:wn",         "z25:i:wn",           "z25:i:dn:we",     "z27:v:we" },
            { "z22:i:ww:red:ws:de", "z26:ws:sps:de",           "z24:i:red:ww:we:ws:dw",  "z25:i:ws",         "z25:i:blue:ws",      "z25:i:ws:gvd4:we", "z27:v:we:ws:gvd4" }

    };

    int blueObjZone = -1;
    int blueRevealZone = -1;
    boolean skipKillAbomination=false;

    public ZQuestBigGameHunting() {
        super(ZQuests.Big_Game_Hunting);
    }

    @Override
    public ZBoard loadBoard() {
        return load(map);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        // check for necro / abom in special spawn places
        game.board.getZone(c.getOccupiedZone()).setObjective(false);
        if (c.getOccupiedZone() == blueRevealZone) {
            getRedObjectives().add(blueObjZone);
            game.addLogMessage("The Labratory objective is revealed!");
            game.board.getZone(blueObjZone).setObjective(true);
            game.spawnZombies(1, ZZombieType.Necromancer, blueObjZone);
            blueRevealZone = -1;
        }
        if (getRedObjectives().size() == 0 && game.getNumKills(ZZombieType.Abomination) == 0) {
            if (Utils.count(game.board.getAllZombies(), object -> object.getType()==ZZombieType.Abomination) == 0) {
                // spawn an abomination somewhere far form where all the characters are
                List<ZZone> spawnZones = game.board.getSpawnZones();
                if (spawnZones.size() > 0) {
                    ZZone zone = Utils.randItem(spawnZones);
                    game.spawnZombies(1, ZZombieType.Abomination, zone.getZoneIndex());
                } else {
                    skipKillAbomination = true;
                }
            }
        }
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        switch (cmd) {
            case "blue":
                blueObjZone = cell.zoneIndex;
                cell.setCellType(ZCellType.OBJECTIVE_BLUE, true);
                break;

            default:
                super.loadCmd(grid, pos, cmd);

        }
    }

    @Override
    public void init(ZGame game) {
        blueRevealZone = Utils.randItem(getRedObjectives());
        game.board.getZone(blueObjZone).setObjective(false); // this does not get revealed until the blueRevealZone found
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("2R", 90, ZTile.getQuadrant(0, 0)),
                new ZTile("8V", 180, ZTile.getQuadrant(0, 3)),
                new ZTile("9V", 90, ZTile.getQuadrant(3, 0)),
                new ZTile("1V", 90, ZTile.getQuadrant(3, 3)),
        };
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        boolean allObjCollected = getRedObjectives().size() == 0 && blueRevealZone < 0;
        boolean exposeLaboratory = blueRevealZone < 0;
        boolean necroKilled = game.getNumKills(ZZombieType.Necromancer) > 0;
        boolean abomKilled = game.getNumKills(ZZombieType.Abomination) > 0;

        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Collect all objectives. One of the objectives\nexposes the laboratory objective.", allObjCollected)
                    .addRow("2.", "Find the Laboratory Objective", exposeLaboratory)
                    .addRow("3.", "Kill at least 1 Necromancer.", necroKilled)
                    .addRow("4.", "Kill at least 1 Abomination.", abomKilled)
                    .addRow("5.", "Not all players need to survive.")
                );
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numTasks = getNumStartObjectives() + 2;
        int numCompleted = getNumFoundObjectives();
        if (skipKillAbomination || game.getNumKills(ZZombieType.Abomination) > 0)
            numCompleted++;
        if (game.getNumKills(ZZombieType.Necromancer) > 0)
            numCompleted++;
        return numCompleted * 100 / numTasks;
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.asList(ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE);
    }
}
