package cc.lib.zombicide.quests;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

public class ZQuestTheCommandry extends ZQuest {

    static {
        addAllFields(ZQuestTheCommandry.class);
    }

    ZDoor blueDoor, greenDoor;
    int blueDoorKeyZone = -1;
    int greenDoorKeyZone = -1;

    public ZQuestTheCommandry() {
        super(ZQuests.The_Commandry);
    }

    ZQuestTheCommandry(ZQuests q) {
        super(q);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:red:ods:de", "z1:spn", "z2:i:ww:de", "z3", "z4", "z5", "z6:i:dw:we:ods", "z7:spn", "z8:i:gvd1:ds:ww:red" },
                { "z9:i:vd2:we",     "z10",   "z2:i:ww:we:ods", "z11", "z12:i:exit:bluedn:ww:ws:we", "z13", "z14:i:ww:we", "z15", "z16" },
                { "z9:i:red:we:ods", "z17",   "z18:i:ww:de:ods", "z19", "z20", "z21", "z14:i:dw:ods:de", "z22", "z23:i:wn:ww:red" },
                { "z24:i:we::ods",   "z25",   "z26:i:ww:ws:ode", "z27:i:dn:ws", "z28:i:wn:greends:odw", "z28:i:dn:ws:ode", "z29:i:ws:we:odn", "z30", "z31:i:ww" },
                { "z32:i:gvd3:we",    "z33",   "z34", "z35", "z36", "z37", "z38", "z39", "z31:i:ww:ods" },
                { "z32:i:red:ws",    "z32:i:wn:ws:ode", "z40:i:wn:ws:ode", "z41:i:wn:ws:de", "z42:sps:start:ws", "z43:i:dw:wn:ws:ode", "z44:i:wn:ws:ode", "z45:i:wn:ws:red", "z45:i:odn:ws:vd4" },
                { "", "", "", "z46:v:gvd1:ww", "z46:v", "z46:v:gvd3:we", "z47:v:vd2", "z47:v", "z47:v:vd4" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "greends":
                greenDoor = new ZDoor(pos, ZDir.SOUTH, GColor.GREEN);
                break;
            case "greende":
                greenDoor = new ZDoor(pos, ZDir.EAST, GColor.GREEN);
                break;
            case "bluedn":
                blueDoor = new ZDoor(pos, ZDir.NORTH, GColor.BLUE);
                break;
            case "bluede":
                blueDoor = new ZDoor(pos, ZDir.EAST, GColor.BLUE);
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == blueDoorKeyZone) {
            game.addLogMessage(c.name() + " has unlocked the Blue Door");
            game.unlockDoor(blueDoor);
            blueDoorKeyZone = -1;
        }

        if (c.getOccupiedZone() == greenDoorKeyZone) {
            game.addLogMessage(c.name() + " has unlocked the Green Door");
            game.unlockDoor(greenDoor);
            greenDoorKeyZone = -1;
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return isAllPlayersInExit(game) ? 100 : 0;
    }

    @Override
    public String getQuestFailedReason(ZGame game) {
        if (Utils.count(game.getBoard().getAllCharacters(), object -> object.isDead()) > 0) {
            return "Not all players survived.";
        }
        return super.getQuestFailedReason(game);
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("4R", 180, ZTile.getQuadrant(0, 0)),
                new ZTile("6R", 270, ZTile.getQuadrant(0, 3)),
                new ZTile("5R", 270, ZTile.getQuadrant(0, 6)),
                new ZTile("7R", 90, ZTile.getQuadrant(3, 0)),
                new ZTile("8R", 180, ZTile.getQuadrant(3, 3)),
                new ZTile("9R", 180, ZTile.getQuadrant(3, 6)),
        };
    }

    @Override
    public void init(ZGame game) {
        while (greenDoorKeyZone == blueDoorKeyZone) {
            greenDoorKeyZone = Utils.randItem(getRedObjectives());
            blueDoorKeyZone = Utils.randItem(getRedObjectives());
        }
        game.getBoard().setDoorLocked(blueDoor);
        game.getBoard().setDoorLocked(greenDoor);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Escape through the underpass.")
                    .addRow("2.", "Unlock the Green Door", greenDoorKeyZone < 0)
                    .addRow("3.", "Unlock the Blue Door", blueDoorKeyZone < 0)
                );
    }
}
