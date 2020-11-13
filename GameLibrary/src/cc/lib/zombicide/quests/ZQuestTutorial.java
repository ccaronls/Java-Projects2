package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZWallFlag;

public class ZQuestTutorial extends ZQuest {

    public ZQuestTutorial() {
        super("Tutorial");
    }

    @Override
    public ZBoard loadBoard() {
        // 6x3
        final String [][] map = {
                { "z0:i:wn:ww", "z0:i:wn", "z1:wn:dw:fatty", "z2:i:wn:ws:we", "z3:green:greende:ww:wn", "z4:i:wn:we:ws:exit" },
                { "z5:sp:wn:ww:ws", "z6:bluedn:we:walker", "z7:ww:ds:we", "z8:red:wn:ww:ws", "z9",               "z10:red:wn:we:ws" },
                { "z11:blue:i:wn:ww:ws:ode", "z12:start:ws:odw:we", "z13:i:ww:ws:dn:runner", "z13:i:wn:we:ws:v", "z14:ws:ww:redde", "z15:i:dw:ws:we:wn:v" },
        };

        return load(map);
    }

    ZDoor blueDoor=null, greenDoor=null, redDoor=null;
    int greenSpawnZone=-1;
    int blueKeyZone=-1;
    int greenKeyZone=-1;
    int redKeyZone=-1;

    @Override
    protected void loadCmd(int row, int col, String cmd) {
        ZCell cell = getCell(row, col);
        int zoneIndex = cell.getZoneIndex();
        switch (cmd) {
            case "green":
                greenSpawnZone = zoneIndex;
                cell.cellType = ZCellType.OBJECTIVE;
                break;
            case "blue":
                blueKeyZone = zoneIndex;
                cell.cellType = ZCellType.OBJECTIVE;
                break;
            case "red":
                if (greenKeyZone >= 0) {
                    redKeyZone = zoneIndex;
                } else if (redKeyZone >= 0) {
                    greenKeyZone = zoneIndex;
                } else if (Utils.flipCoin()) {
                    greenKeyZone = zoneIndex;
                } else {
                    redKeyZone = zoneIndex;
                }
                break;
            case "redde":
                redDoor = new ZDoor(row, col, ZBoard.DIR_EAST);
                super.loadCmd(row, col, "lde");
                break;
            case "bluedn":
                blueDoor = new ZDoor(row, col, ZBoard.DIR_NORTH);
                super.loadCmd(row, col, "ldn");
                break;
            case "greende":
                greenDoor = new ZDoor(row, col, ZBoard.DIR_WEST);
                super.loadCmd(row, col, "lde");
                break;
            default:
                super.loadCmd(row, col, cmd);
        }
    }

    @Override
    public void addMoves(ZGame zGame, ZCharacter cur, List<ZMove> options) {
        if (cur.getOccupiedZone() == blueKeyZone) {
            options.add(ZMove.newObjectiveMove(blueKeyZone));
        } else if (cur.getOccupiedZone() == greenKeyZone) {
            options.add(ZMove.newObjectiveMove(greenKeyZone));
        } else if (cur.getOccupiedZone() == redKeyZone) {
            options.add(ZMove.newObjectiveMove(redKeyZone));
        }
    }

    @Override
    public void processObjective(ZGame zGame, ZCharacter c, ZMove move) {
        zGame.addExperience(c, 5);
        if (move.integer == redKeyZone) {
            zGame.board.setDoor(redDoor, ZWallFlag.CLOSED);
            zGame.getCurrentUser().showMessage(c.name() + " has unlocked the RED door");
            redKeyZone = -1;
        } else if (move.integer == blueKeyZone) {
            zGame.board.setDoor(blueDoor, ZWallFlag.CLOSED);
            zGame.getCurrentUser().showMessage(c.name() + " has unlocked the BLUE door");
            blueKeyZone = -1;
        } else if (move.integer == greenKeyZone) {
            zGame.board.setDoor(greenDoor, ZWallFlag.CLOSED);
            zGame.board.getZone(greenSpawnZone).isSpawn = true;
            zGame.getCurrentUser().showMessage(c.name() + " has unlocked the GREEN door");
            zGame.getCurrentUser().showMessage(c.name() + " has created a new spawn zone!");
            greenKeyZone = -1;
        } else {
            throw new AssertionError("Invsalid move for objective: " + move);
        }
    }

    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZTiles tiles) {

        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "4V", "9R" }, new int [] { 90, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant1 = new GRectangle(getCell(0, 0).getRect().getTopLeft(),
                getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant2 = new GRectangle(getCell(0, 3).getRect().getTopLeft(),
                getCell(2, 5).getRect().getBottomRight());
        g.drawImage(tileIds[0], quadrant1);
        g.drawImage(tileIds[1], quadrant2);
    }
}
