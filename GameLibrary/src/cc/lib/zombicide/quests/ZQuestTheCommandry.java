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
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZWallFlag;

public class ZQuestTheCommandry extends ZQuest9x6 {

    static {
        addAllFields(ZQuestTheCommandry.class);
    }

    ZDoor blueDoor, greenDoor;
    int blueDoorKeyZone = -1;
    int greenDoorKeyZone = -1;

    public ZQuestTheCommandry() {
        super("The Commandry");
    }

    ZQuestTheCommandry(String title) {
        super(title);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:red:ods:de", "z1:sp", "z2:i:ww:de", "z3", "z4", "z5", "z6:i:dw:we:ods", "z7:sp", "z8:i:vd1:ds:ww:red" },
                { "z9:i:vd2:we",     "z10",   "z2:i:ww:we:ods", "z11", "z12:i:exit:bluedn:ww:ws:we", "z13", "z14:i:ww:we", "z15", "z16" },
                { "z9:i:red:we:ods", "z17",   "z18:i:ww:de:ods", "z19", "z20", "z21", "z14:i:dw:ods:de", "z22", "z23:i:wn:ww:red" },
                { "z24:i:we::ods",   "z25",   "z26:i:ww:ws:ode", "z27:i:dn:ws", "z27:i:wn:greends:odw", "z28:i:dn:ws:ode", "z29:i:ws:we:odn", "z30", "z31:i:ww" },
                { "z32:i:vd3:we",    "z33",   "z34", "z35", "z36", "z37", "z38", "z39", "z31:i:ww:ods" },
                { "z32:i:red:ws",    "z32:i:wn:ws:ode", "z40:i:wn:ws:ode", "z41:i:wn:ws:de", "z42:sp:start:ws", "z43:i:dw:wn:ws:ode", "z44:i:wn:ws:ode", "z45:i:wn:ws:red", "z45:i:odn:ws:vd4" },
                { "", "", "", "z46:v:vd1:ww", "z46:v", "z46:v:vd3:we", "z47:v:vd2", "z47:v", "z47:v:vd4" }
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
    public void processObjective(ZGame game, ZCharacter c, ZMove move) {
        super.processObjective(game, c, move);
        if (move.integer == blueDoorKeyZone) {
            game.getCurrentUser().showMessage(c.name() + " has unlocked the Blue Door");
            game.board.setDoor(blueDoor, ZWallFlag.CLOSED);
        }

        if (move.integer == greenDoorKeyZone) {
            game.getCurrentUser().showMessage(c.name() + " has unlocked the Green Door");
            game.board.setDoor(greenDoor, ZWallFlag.CLOSED);
        }
    }

    @Override
    public boolean isQuestComplete(ZGame game) {
        return game.board.getZombiesInZone(exitZone).size() == 0 && !(Utils.filter(game.getAllCharacters(), object -> object.getOccupiedZone() != exitZone).size() > 0);
    }

    @Override
    public boolean isQuestFailed(ZGame game) {
        return Utils.filter(game.getAllCharacters(), object -> object.isDead()).size() > 0;
    }

    @Override
    protected String[] getTileIds() {
        return new String[] { "4R", "6R", "5R", "7R", "8R", "9R" };
    }

    @Override
    protected int[] getTileRotations() {
        return new int[] { 180, 270, 270, 90, 180, 180 };
    }

    @Override
    public void init(ZGame game) {
        while (greenDoorKeyZone == blueDoorKeyZone) {
            greenDoorKeyZone = Utils.randItem(redObjectives);
            blueDoorKeyZone = Utils.randItem(redObjectives);
        }
        game.board.setDoorLocked(blueDoor);
        game.board.setDoorLocked(greenDoor);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Escape through the underpass.")
                    .addRow("2.", "Unlock the Green Door", game.board.getDoor(greenDoor) != ZWallFlag.LOCKED)
                    .addRow("3.", "Unlock the Blue Door", game.board.getDoor(blueDoor) != ZWallFlag.LOCKED)
                );
    }
}
