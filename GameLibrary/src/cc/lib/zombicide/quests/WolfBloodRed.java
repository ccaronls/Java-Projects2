package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZColor;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSkillLevel;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWeaponType;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfBloodRed extends ZQuest {

    static {
        addAllFields(WolfBloodRed.class);
    }

    int greenObjZone=-1;
    int blueObjZone=-1;


    public WolfBloodRed() {
        super(ZQuests.Blood_Red);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                { "z0:i:ws:ode:red","z1:i:ws:de","z2",          "z3","z4","z5",                 "z6:xspn","z7:i:dw:ws","z8:i:odw:ws:red"       },
                { "z9:xspw","z10","z11",                         "z12:i:ww:wn:we","z13","z14:i:ww:wn:we",    "z15","z16","z17"      },
                { "z18","z19:i:red:wn:ww:ws:ode","z20:i:red:wn:ws:ode",     "z12:i:ds:we","z21","z14:i:ds:ww:ode",              "z22:i:wn:red:ode","z23:i:red:wn:de","z50"      },

                { "z24","z25:t1:rn","z26:t2:rn:re",                "z27","z28","z29",                      "z30:t3:rn:rw:vd2","z30:t3:rn:re","z31:xspe"   },
                { "z32","z33:t3:rw:rn","z33:t3:re",             "z34","z35:i:wn:we:ww:gvd1","z36",      "z30:t3:rw","z30:t3:re","z46:t1"     },
                { "z47:xsps","z33:t3:rw","z33:t3:re:gvd2",          "z37","z35:i:ww:we:ws:vd1:red","z38",      "z39:st","z40:t2:rw","z40:t2"       },

                { "z41:v:vd1:wn:ww","z41:v:wn","z41:v:wn:vd2:we",     "z42","z43","z44",                  "z45:v:gvd1:ww:wn","z45:v:wn","z45:v:wn:we:gvd2"  }

        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        // setup spawns so they cannot be removed if necro killed
        switch (cmd) {
            case "xspn":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.NORTH, false, true, false));
                break;
            case "xsps":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.SOUTH, false, true, false));
                break;
            case "xspw":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.WEST, false, true, false));
                break;
            case "xspe":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.EAST, false, true, false));
                break;
            default:
                super.loadCmd(grid, pos, cmd);

        }
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("2R", 270, ZTile.getQuadrant(0, 0, board)),
                new ZTile("9V", 90, ZTile.getQuadrant(0, 3, board)),
                new ZTile("3V", 0, ZTile.getQuadrant(0, 6, board)),

                new ZTile("10V", 270, ZTile.getQuadrant(3, 0, board)),
                new ZTile("6X", 0, ZTile.getQuadrant(3, 3, 2, 3, board)),
                new ZTile("11R", 270, ZTile.getQuadrant(3, 6, board))
        };

    }

    @Override
    public void init(ZGame game) {
        while (blueObjZone == greenObjZone) {
            blueObjZone = Utils.randItem(getRedObjectives());
            greenObjZone = Utils.randItem(getRedObjectives());
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == greenObjZone) {
            greenObjZone = -1;
            game.giftEquipment(c, getRandomVaultArtifact());
        } else if (c.getOccupiedZone() == blueObjZone) {
            blueObjZone = -1;
            game.giftEquipment(c, getRandomVaultArtifact());
        }
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.toList(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.EARTHQUAKE_HAMMER, ZWeaponType.DRAGON_FIRE_BLADE);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        ZSkillLevel ULTRA_RED = new ZSkillLevel(ZColor.RED, 1);
        int numPlayers = game.getAllCharacters().size();
        int numAtUltraRed = Utils.count(game.getAllCharacters(), pl -> pl.getCharacter().getSkillLevel().compareTo(ULTRA_RED) >= 0);
        int total = numPlayers + getNumStartRedObjectives();
        int completed = numAtUltraRed + getNumFoundObjectives();
        return completed*100 / total;

    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.getBoard().getAllCharacters(), object -> object.isDead()) > 0) {
            return "Not all players survived.";
        }
        return super.getQuestFailedReason(game);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        ZSkillLevel ULTRA_RED = new ZSkillLevel(ZColor.RED, 1);
        int numPlayers = game.getAllCharacters().size();
        int numAtUltraRed = Utils.count(game.getAllCharacters(), pl -> pl.getCharacter().getSkillLevel().compareTo(ULTRA_RED) >= 0);
        return new Table(getName()).addRow(new Table().setNoBorder()
            .addRow("1.", "Collect all objectives. Some of the objectives give a random vault item", String.format("%d of %d", getNumFoundObjectives(), getNumStartRedObjectives()))
            .addRow("2.", "Get all survivors to ultra RED danger level", String.format("%d of %d", numAtUltraRed, numPlayers))
            .addRow("3.", "Only spawns from Necromancers can be removed", "")
        );
    }
}
