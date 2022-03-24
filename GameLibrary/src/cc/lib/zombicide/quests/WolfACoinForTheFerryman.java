package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfACoinForTheFerryman extends ZQuest {

    static {
        addAllFields(WolfACoinForTheFerryman.class);
    }

    List<ZDoor> lockedDoors = new ArrayList<>();
    Grid.Pos blueKeyPos = null;

    public WolfACoinForTheFerryman() {
        super(ZQuests.A_Coin_For_The_Ferryman);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                { "z0:i:ww:ws","z1","z2:i::ww::ws:de:red",              "z10","z11","z12",                                          "z20","z21:spn","z22:i:ww"   },
                { "z3","z4","z5",                                       "z13","z14:i:wn::lde:ldw:exit","z15",                       "z23:i:dn:ww:we:ods","z24","z22:i:ww:ws"   },
                { "z6:i:wn:we:ods","z7","z8:i:wn:ww:de:ods",            "z16","z17","z18",                                          "z25:i:dw:we:red","z26","z27"   },

                { "z30:i:we:ws","z31","z32:i:ww",                       "z40:t3:rn:rw","z41:t3:blue:rn","z42:t3:rn:re",             "z50:i:ds:ode","z51:i:wn:we:ws","z52"   },
                { "z33","z34","z32:i:ww",                                 "z43:t3:rw","z44:t3","z45:t3:re:rs",                      "z53","z54","z55"   },
                { "z35:i:wn:we:red","z36","z37:v:dw:wn:ws",               "z46:t3:rw:rs:re","z47:t2:rs","z48:t1:rs:st",             "z56","z57:i:wn:dw:ws:ode","z58:i:wn:ods"   },

                { "z60:i:de:ods","z61","z62:i:ww:ws:ode",               "z70:i:ws","z70:i:ws:ode","z71:i:de:ws",                    "z80","z81:i:ww:ws:ode","z82:i:ws"    },
                { "z63:i:we","z64","z65",                               "z72","z73","z74",                                          "z83","z84","z85"              },
                { "z63:i:we","z66:sps","z67:i:ww:dn:ode",                 "z75:i:wn:ode","z76:i:wn","z77:i:wn:red:ode",             "z86:i:dn:ode","z87:i:wn:we","z88"    }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {

            case "ldw":
                lockedDoors.add(new ZDoor(pos, ZDir.WEST, GColor.BLUE));
                break;
            case "lde":
                lockedDoors.add(new ZDoor(pos, ZDir.EAST, GColor.BLUE));
                break;
            case "blue":
                // blue key hidden until all other objectives taken
                blueKeyPos = pos;
                break;
            case "red":
                if (getNumStartObjectives()>0)
                    break; // for DEBUG allow only 1
                else {
                    super.loadCmd(grid, pos, "st");
                }
                // fallthrough
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[]{
                new ZTile("7V", 270, ZTile.getQuadrant(0, 0)),
                new ZTile("6R", 0, ZTile.getQuadrant(0, 3)),
                new ZTile("1R", 0, ZTile.getQuadrant(0, 6)),

                new ZTile("5R", 90, ZTile.getQuadrant(3, 0)),
                new ZTile("10R", 270, ZTile.getQuadrant(3, 3)),
                new ZTile("2R", 90, ZTile.getQuadrant(3, 6)),

                new ZTile("8R", 90, ZTile.getQuadrant(6, 0)),
                new ZTile("4R", 270, ZTile.getQuadrant(6, 3)),
                new ZTile("3V", 180, ZTile.getQuadrant(6, 6))

        };
    }

    @Override
    public void init(ZGame game) {
        for (ZDoor door : lockedDoors)
            game.getBoard().setDoorLocked(door);
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        int zIdx;
        if (blueKeyPos != null && (zIdx=game.getBoard().getCell(blueKeyPos).getZoneIndex()) == c.getOccupiedZone()) {
            game.addLogMessage("The PORTAL is unlocked!!");
            game.getBoard().getZone(zIdx).setObjective(false);
            for (ZDoor door : lockedDoors) {
                game.unlockDoor(door);
            }
            blueKeyPos = null;
        } else {
            super.processObjective(game, c);
            if (getNumUnfoundObjectives() == 0) {
                game.addLogMessage("The BLUE key is revealed!!!");
                game.getBoard().setObjective(blueKeyPos, ZCellType.OBJECTIVE_BLUE);
            }
        }
    }

    @Override
    public void addMoves(ZGame game, ZCharacter cur, List<ZMove> options) {
        super.addMoves(game, cur, options);
        if (blueKeyPos != null) {
            int idx = game.getBoard().getCell(blueKeyPos).getZoneIndex();
            if (idx == cur.getOccupiedZone()) {
                options.add(ZMove.newObjectiveMove(idx));
            }
        }
    }

    boolean isExitZoneOccupied(ZGame game) {
        return Utils.count(game.getAllLivingCharacters(), c -> c.getCharacter().getOccupiedZone() == getExitZone()) > 0;
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int total = getNumStartObjectives() + 2;
        int completed = getNumFoundObjectives() + (isExitZoneOccupied(game) ? 1 : 0) + (blueKeyPos == null ? 1 : 0);

        return completed * 100 / total;
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Take all of the objective to reveal the BLUE key", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                    .addRow("2.", "Take the BLUE key to unlock the BLUE doors and the escape portal", blueKeyPos == null)
                    .addRow("3.", "Enter the portal and banish the Necromancers for good", isExitZoneOccupied(game))

                );
    }
}
