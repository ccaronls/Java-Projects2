package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWeaponType;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.ui.UIZombicide;

/**
 * Created by Chris Caron on 8/24/21.
 */
public class WolfQuestTheAmbush extends ZQuest {

    static {
        addAllFields(WolfQuestTheAmbush.class);
    }

    Grid.Pos blueSpawnPos = null;
    List<Integer> occupyZones = new ArrayList<>();

    public WolfQuestTheAmbush() {
        super(ZQuests.The_Ambush);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:red:i:ds:we", "z1:spn", "z2:i:ww:ws:ode", "z3:i:red:ds", "z3:i:ws:ode", "z4:i:ws:de",             "z5:spn", "z6:i:ww:ds:ode", "z7:i:red:ws" },
                { "z8","z9","z10",                              "z11","z12:st","z13",                                   "z14","z15","z16:spe" },
                { "z17:i:wn:ode", "z18:i:dn","z18:i:wn:ode",    "z19:i:red:wn:we:ods","z20","z21:i:red:wn:ww:ode",      "z26:i:wn:ode", "z22:i:dn:we", "z23" },

                { "z24:t2:rn","z25:t3:rn:occupy","z25:t3:rn:re","z32:i:we","z27","z28:i:ww:odn",                        "z29:t2:rn:rw","z29:t2:rn:re","z30" },
                { "z31:t1:re","z25:t3:rs","z25:t3:rs:re",       "z32:i:ds:we","z33","z28:i:dw:ws",                      "z34:t1:rw:re","z35:t3:occupy","z35:t3:rn" },
                { "z36:blspw","z37","z38",                      "z39","z40","z41",                                      "z42",         "z35:t3:rw","z35:t3:" }
        };


        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "blspw":
                blueSpawnPos = pos;
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.WEST, false, true, true));
                break;
            case "spn":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.NORTH, true, false, false));
                break;
            case "spe":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_RED, ZDir.EAST, true, false, false));
                break;
            case "occupy":
                occupyZones.add(grid.get(pos).zoneIndex);
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    private List<Integer> getOccupiedZones(ZGame game) {
        List<Integer> playerZones = Utils.map(game.getAllCharacters(), pl -> pl.getCharacter().getOccupiedZone());
        playerZones.retainAll(occupyZones);
        return playerZones;
    }

    @Override
    public int getPercentComplete(ZGame game) {
        ZCell blueCell = game.board.getCell(blueSpawnPos);
        int total = getNumStartObjectives() + occupyZones.size() + 1;
        int found = getNumFoundObjectives() + getOccupiedZones(game).size() + (blueCell.numSpawns > 0 ? 0 : 1);
        return found * 100 / total;
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("8R", 0, ZTile.getQuadrant(0, 0)),
                new ZTile("5R", 0, ZTile.getQuadrant(0, 3)),
                new ZTile("3V", 180, ZTile.getQuadrant(0, 6)),

                new ZTile("10V", 180, ZTile.getQuadrant(3, 0)),
                new ZTile("9V", 270, ZTile.getQuadrant(3, 3)),
                new ZTile("11V", 90, ZTile.getQuadrant(3, 6))
        };
    }

    @Override
    public void init(ZGame game) {
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (getVaultItemsRemaining().size() > 0) {
            game.giftEquipment(c, getVaultItemsRemaining().remove(0));
        }
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.toList(ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.BASTARD_SWORD, ZWeaponType.EARTHQUAKE_HAMMER);
    }

    @Override
    public int getMaxNumZombiesOfType(ZZombieType type) {
        switch (type) {
            case Necromancer:
                return 3;
        }
        return super.getMaxNumZombiesOfType(type);
    }

    @Override
    public void drawQuest(UIZombicide game, AGraphics g) {
        for (int zIdx : occupyZones) {
            ZZone zone = game.board.getZone(zIdx);
            GRectangle rect = zone.getRectangle().scaledBy(.25f, .25f);
            g.setColor(GColor.GREEN);//.withAlpha(.5f));
            g.drawLine(rect.getTopLeft(), rect.getBottomRight(), 10);
            g.drawLine(rect.getTopRight(), rect.getBottomLeft(), 10);
            if (Utils.count(game.board.getActorsInZone(zIdx), a -> a instanceof ZCharacter) > 0) {
                g.drawCircle(rect.getCenter(), rect.getRadius(), 10);
            }
        }
    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (game.getAllLivingCharacters().size() < 2)
            return "Not enough players alive to complete quest";
        return super.getQuestFailedReason(game);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        ZCell blueCell = game.board.getCell(blueSpawnPos);
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Collect all Objectives. Each objective gives a vault item.", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                        .addRow("2.", "Eliminate the BLUE spawn zone using normal necromancer rules.", blueCell.numSpawns == 0)
                        .addRow("3.", "Occupy each tower with at least one player", String.format("%d of %d", getOccupiedZones(game).size(), occupyZones.size()))
                        .addRow("4.", "RED spawn zones can spawn Necromancers but they cannot be removed.")
                );
    }
}
