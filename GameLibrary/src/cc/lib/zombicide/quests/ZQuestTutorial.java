package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellDoor;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZWallFlag;

public class ZQuestTutorial extends ZQuest {

    static {
        addAllFields(ZQuestTutorial.class);
    }


    public ZQuestTutorial() {
        super("Tutorial");
    }

    @Override
    public ZBoard loadBoard() {
        // 6x3
        final String [][] map = {
                { "z0:i:wn:ww", "z0:i:wn", "z1:wn:dw:fatty", "z2:i:wn:ws:we", "z3:green:greende:ww:wn", "z4:i:wn:we:ws:exit" },
                { "z5:sp:wn:ww:ws", "z6:bluedn:we:walker", "z7:ww:ds:we", "z8:red:wn:ww:ws", "z9",               "z10:red:wn:we:ws" },
                { "z11:blue:i:wn:ww:ws:ode", "z12:start:ws:odw:we", "z13:i:ww:ws:dn:runner", "z13:i:wn:we:ws:vd1", "z14:ws:ww:de", "z15:i:dw:ws:we:wn:vd2" },
                { "",                       "",                     "",                      "z16:v:wn:ws:ww:vd1", "z16:v:wn:ws", "z16:v:wn:ws:we:vd2" },
        };

        getVaultItems().add(ZItemType.DRAGON_BILE.create());
        getVaultItems().add(ZItemType.TORCH.create());

        return load(map);
    }

    ZCellDoor blueDoor=null, greenDoor=null;
    int [] redZones = new int[2];
    int greenSpawnZone=-1;
    int blueKeyZone=-1;
    int greenKeyZone=-1;
    int numRed=0;

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZCell cell = grid.get(pos);
        int zoneIndex = cell.getZoneIndex();
        switch (cmd) {
            case "green":
                greenSpawnZone = zoneIndex;
                break;
            case "blue":
                blueKeyZone = zoneIndex;
                cell.cellType = ZCellType.OBJECTIVE;
                break;
            case "red":
                cell.cellType = ZCellType.OBJECTIVE;
                redZones[numRed++] = zoneIndex;
                break;
            case "bluedn":
                blueDoor = new ZCellDoor(pos, ZDir.NORTH);
                super.loadCmd(grid, pos, "ldn");
                break;
            case "greende":
                greenDoor = new ZCellDoor(pos, ZDir.EAST);
                super.loadCmd(grid, pos, "lde");
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public void addMoves(ZGame zGame, ZCharacter cur, List<ZMove> options) {
        for (int red: redZones) {
            if (cur.getOccupiedZone() == red)
                options.add(ZMove.newObjectiveMove(red));
        }
        if (cur.getOccupiedZone() == blueKeyZone) {
            options.add(ZMove.newObjectiveMove(blueKeyZone));
        }
    }

    @Override
    public void processObjective(ZGame zGame, ZCharacter c, ZMove move) {
        zGame.addExperience(c, 5);
        if (move.integer == blueKeyZone) {
            zGame.board.setDoor(blueDoor, ZWallFlag.CLOSED);
            zGame.getCurrentUser().showMessage(c.name() + " has unlocked the BLUE door");
            blueKeyZone = -1;
        } else if (move.integer == greenKeyZone) {
            zGame.board.setDoor(greenDoor, ZWallFlag.CLOSED);
            zGame.board.setSpawnZone(greenSpawnZone, true);
            zGame.getCurrentUser().showMessage(c.name() + " has unlocked the GREEN door");
            zGame.getCurrentUser().showMessage(c.name() + " has created a new spawn zone!");
            greenKeyZone = -1;
        } else {
            //throw new AssertionError("Invalid move for objective: " + move);
        }
    }

    @Omit
    int [] tileIds = null;

    @Override
    public void drawTiles(AGraphics g, ZBoard board, ZTiles tiles) {

        if (tileIds == null) {
            tileIds = tiles.loadTiles(g, new String [] { "4V", "9R" }, new int [] { 90, 90 });
        }

        if (tileIds.length == 0)
            return;

        GRectangle quadrant1 = new GRectangle(board.getCell(0, 0).getRect().getTopLeft(),
                board.getCell(2, 2).getRect().getBottomRight());
        GRectangle quadrant2 = new GRectangle(board.getCell(0, 3).getRect().getTopLeft(),
                board.getCell(2, 5).getRect().getBottomRight());
        g.drawImage(tileIds[0], quadrant1);
        g.drawImage(tileIds[1], quadrant2);
    }


    @Override
    public void init(ZGame game) {
        greenKeyZone = redZones[Utils.rand()%numRed];
    }
}
