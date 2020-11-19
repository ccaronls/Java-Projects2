package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZBoard extends Reflector<ZBoard> {

    static {
        addAllFields(ZBoard.class);
    }

    Grid<ZCell> grid; // TODO: Use Grid Container Type
    List<ZZone> zones;
    @Omit
    boolean loaded = false;

    public ZBoard() {

    }

    public ZBoard(Grid<ZCell> grid, List<ZZone> zones) {
        this.grid = grid;
        this.zones = zones;
    }

    public int getRows() {
        return grid.getRows();
    }

    public int getColumns() {
        return grid.getCols();
    }

    public ZZone getZone(int index) {
        return zones.get(index);
    }

    Iterable<ZZone> getZones() {
        return zones;
    }

    /**
     * Get list of accessable zones
     *
     * @param zoneIndex
     * @param distance
     * @return
     */
    public List<Integer> getAccessableZones(int zoneIndex, int distance) {
        if (distance == 0)
            return Collections.singletonList(zoneIndex);

        List<Integer> result = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            // fan out in all direction to given distance
            //for (int dir = 0; dir <4; dir++) {
            for (ZDir dir : ZDir.values()) {
                Grid.Pos pos = cellPos;
                boolean open = true;
                for (int dist = 0; dist <distance; dist++) {
                    if (!grid.get(pos).getWallFlag(dir).isOpen()) {
                        open = false;
                        break;
                    }
                    pos = dir.getAdjacent(pos);
                }
                if (open && grid.get(pos).zoneIndex != zoneIndex) {
                    result.add(grid.get(pos).zoneIndex);
                }
            }
        }
        for (ZDoor vd : zones.get(zoneIndex).doors) {
            if (vd instanceof ZVaultDoor && !vd.isClosed(this))
                result.add(grid.get(((ZVaultDoor)vd).cellPosExit).zoneIndex);
        }
        return result;
    }

    public boolean canSee(int fromZone, int toZone) {
        for (Grid.Pos pos0: zones.get(fromZone).cells) {
            for (Grid.Pos pos1: zones.get(toZone).cells) {
                if (canSeeCell(pos0, pos1))
                    return true;
            }
        }
        return false;
    }

    public boolean canSeeCell(Grid.Pos fromCell, Grid.Pos toCell) {
        ZDir dir = ZDir.getDirFrom(fromCell, toCell);
        if (dir == null)
            return false;

        int zoneChanges = 0;
        int curZoneId = grid.get(fromCell).zoneIndex;

        while (!fromCell.equals(toCell)) {
            ZCell cell = grid.get(fromCell);
            // can only see 1 one zone difference
            if (cell.isInside() && cell.zoneIndex != curZoneId) {
                if (++zoneChanges > 1)
                    return false;
                curZoneId = cell.zoneIndex;
            }

            if (!cell.getWallFlag(dir).isOpen()) {
                return false;
            }

            fromCell = dir.getAdjacent(fromCell);
        }

        return true;
    }



    /**
     * Returns a list of directions the zombie can move
     * @See DIR_NORTH, DIR_SOUTH, DIR_EAST, DIR_WEST
     *
     * @param fromPos
     * @param toZoneIndex
     * @return
     */
    public Collection<ZDir> getShortestPathOptions(Grid.Pos fromPos, int toZoneIndex) {
        ZZone toZone = zones.get(toZoneIndex);

        List<List<ZDir>> allPaths = new ArrayList<>();
        int maxDist = grid.getRows() * grid.getCols();

        for (Grid.Pos bCellPos : toZone.cells) {
            List<List<ZDir>> paths = getShortestPathOptions(fromPos, bCellPos, maxDist);
            //for (List l : paths) {
            //    maxDist = Math.min(maxDist, l.size());
            //}
            allPaths.addAll(paths);
        }

        Iterator<List<ZDir>> it = allPaths.iterator();
        while (it.hasNext()) {
            if (it.next().size() > maxDist)
                it.remove();
        }

        Set<ZDir> dirs = new HashSet<>();
        for (List<ZDir> p : allPaths) {
            dirs.add(p.get(0));
        }

        return dirs;
    }

    private List<List<ZDir>> getShortestPathOptions(Grid.Pos fromCell, Grid.Pos toCell, int maxDist) {
        List<List<ZDir>> paths = new ArrayList<>();
        Set<Grid.Pos> visited = new HashSet<>();
        searchPathsR(fromCell, toCell, new int[] { maxDist }, new LinkedList<>(), paths, visited);
        return paths;
    }

    private void searchPathsR(Grid.Pos fromPos, Grid.Pos toPos, int [] maxDist, LinkedList<ZDir> curPath, List<List<ZDir>> paths, Set<Grid.Pos> visited) {
        if (fromPos.equals(toPos)) {
            if (curPath.size() > 0) {
                paths.add(new ArrayList<>(curPath));
                maxDist[0] = Math.min(maxDist[0], curPath.size());
            }
            return;
        }
        if (curPath.size() >= maxDist[0])
            return;
        if (visited.contains(fromPos))
            return;
        visited.add(fromPos);
        ZCell fromCell = grid.get(fromPos);

        for (ZDir dir : ZDir.getCompassValues()) {
            if (fromCell.getWallFlag(dir) == ZWallFlag.NONE) {
                curPath.addLast(dir);
                searchPathsR(dir.getAdjacent(fromPos), toPos, maxDist, curPath, paths, visited);
                curPath.removeLast();
            }
        }


        ZZone fromZone = zones.get(fromCell.getZoneIndex());
        for (ZDoor door : fromZone.getDoors()) {
            if (door.getCellPos().equals(fromPos) && !door.isClosed(this)) {
                curPath.addLast(door.getMoveDirection());
                searchPathsR(door.getCellPosEnd(), toPos, maxDist, curPath, paths, visited);
                curPath.removeLast();
            }
        }
    }

    public ZCell getCell(Grid.Pos pos) {
        return grid.get(pos);
    }

    public ZCell getCell(int row, int col) {
        return grid.get(row, col);
    }

    public ZWallFlag getDoor(ZCellDoor door) {
        return getCell(door.cellPos).getWallFlag(door.dir);
    }

    public void setDoor(ZCellDoor door, ZWallFlag flag) {
        getCell(door.cellPos).setWallFlag(door.dir, flag);
        door = door.getOtherSide(this);
        getCell(door.cellPos).setWallFlag(door.dir, flag);
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (ZCell cell : grid.getCells()) {
            if (cell.getRect().contains(mouseX, mouseY)) {
                return cell.zoneIndex;
            }
        }
        return -1;
    }

    public ZActor pickActor(AGraphics g, int mouseX, int mouseY) {
        return null;
    }

    public synchronized void loadCells(AGraphics g) {
        if (!loaded)
            initCellRects(g, g.getViewportWidth(), g.getViewportHeight());
        loaded = true;
    }

    public void setSpawnZone(int zoneIdx, boolean isSpawn) {
        ZZone zone = zones.get(zoneIdx);
        zone.isSpawn = true;
        getCell(zone.cells.get(0)).cellType = ZCellType.SPAWN;
    }

    /**
     * return zone highlighted by mouseX, mouseY
     *
     * @param g
     * @param mouseX
     * @param mouseY
     * @return
     */
    public int drawZones(AGraphics g, int mouseX, int mouseY) {
        loadCells(g);
        int result = -1;
        for (int i=0; i<zones.size(); i++) {
            ZZone zone = zones.get(i);
            for (Grid.Pos pos : zone.cells) {
                ZCell cell = getCell(pos);
                if (cell.cellType == ZCellType.EMPTY)
                    continue;
                switch (zone.type) {
                    case VAULT:
                        g.setColor(GColor.BROWN);
                        break;
                    case BUILDING:
                        g.setColor(GColor.ORANGE);
                        break;
                    case OUTDOORS:
                        g.setColor(GColor.LIGHT_GRAY);
                        break;
                }
                g.drawFilledRect(cell.rect);
                drawCellWalls(g, cell, 1);
                if (cell.rect.contains(mouseX, mouseY)) {
                    result = i;
                    g.setColor(GColor.RED.withAlpha(32));
                    g.drawFilledRect(cell.rect);
                }
                String text = "";
                switch (cell.cellType) {
                    case OBJECTIVE:
                        if (zone.objective) {
                            // draw a big red X om the center of the cell
                            GRectangle redX = cell.rect.scaledBy(.25f, .25f);
                            g.setColor(GColor.RED);
                            g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 10);
                            g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 10);
                        }
                        break;
                    case EXIT:
                        text += "EXIT";
                        break;
                    case SPAWN:
                        if (zone.isSpawn) {
                            text += "SPAWN";
                        }
                        break;
                }
                if (zone.dragonBile) {
                    g.setColor(GColor.DARK_OLIVE.withAlpha(.3f));
                    cell.getRect().drawFilled(g);
                }
                if (text.length() > 0) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 2);
                }
            }
            for (ZDoor vd : zone.doors) {
                vd.draw(g, this);
            }
            if (zone.noiseLevel > 0) {
                g.setColor(GColor.CYAN);
                g.drawJustifiedStringOnBackground(getCell(zone.cells.get(0)).rect.getTopLeft().addEq(10, 10), Justify.LEFT, Justify.TOP, String.valueOf(zone.noiseLevel), GColor.TRANSLUSCENT_BLACK, 10, 10);
            }
        }
        return result;
    }

    public void drawCellWalls(AGraphics g, ZCell cell, float scale) {
        loadCells(g);
        g.pushMatrix();
        Vector2D center = cell.rect.getCenter();
        g.translate(center);
        Vector2D v0 = cell.rect.getTopLeft().subEq(center);
        Vector2D v1 = cell.rect.getTopRight().subEq(center);
        g.scale(scale);

        for (ZDir dir : ZDir.values()) {
            Vector2D dv = v1.sub(v0).scaleEq(.33f);
            Vector2D dv0 = v0.add(dv);
            Vector2D dv1 = dv0.add(dv);

            g.pushMatrix();
            g.rotate(dir.rotation);
            g.setColor(GColor.BLACK);
            switch (cell.getWallFlag(dir)) {
                case WALL:
                    g.drawLine(v0, v1, 3);
                    break;
                case OPEN: {
                    g.drawLine(v0, dv0, 3);
                    g.drawLine(dv1, v1, 3);
                    break;
                }
                case LOCKED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(GColor.RED);
                    g.drawLine(dv0, dv1, 4);
                    break;
                case CLOSED:
                    g.drawLine(v0, v1, 3);
                    g.setColor(GColor.GREEN);
                    g.drawLine(dv0, dv1, 4);
                    break;
            }
            g.popMatrix();
        }
        g.popMatrix();
    }

    public Grid.Pos drawDebug(AGraphics g, int mouseX, int mouseY) {
        loadCells(g);
        g.clearScreen(GColor.LIGHT_GRAY);
        Grid.Pos returnCell = null;

        Grid.Iterator<ZCell> it = grid.iterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.cellType == ZCellType.EMPTY)
                continue;
            switch (cell.environment) {
                case ZCell.ENV_BUILDING:
                    g.setColor(GColor.YELLOW); break;
                case ZCell.ENV_OUTDOORS:
                    g.setColor(GColor.WHITE); break;
                case ZCell.ENV_VAULT:
                    g.setColor(GColor.ORANGE); break;
            }
            g.drawFilledRect(cell.rect);
            drawCellWalls(g, cell, .97f);
            String text = "Zone " + cell.zoneIndex + "\n" + cell.cellType;
            if (cell.rect.contains(mouseX, mouseY)) {
                List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1);
                text = "1 Unit away:\n" + accessible;
                returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2);
                text += "\n2 Units away:\n" + accessible2;
            }
            g.setColor(GColor.CYAN);
            for (ZActor a : cell.occupied) {
                if (a != null) {
                    text += "\n" + a.name();
                }
            }
            g.drawJustifiedStringOnBackground(cell.rect.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 3);

        }
        return returnCell;
    }

    public GDimension initCellRects(AGraphics g, int width, int height) {
        int rows = getRows();
        int cols = getColumns();

        int dim = Math.min(width/cols, height/rows);

        for (Grid.Iterator<ZCell> it = grid.iterator(); it.hasNext(); ) {
            ZCell cell = it.next();
            cell.rect = new GRectangle(it.getPos().getColumn()*dim, it.getPos().getRow()*dim, dim, dim);
            zones.get(cell.zoneIndex).center.addEq(cell.rect.getCenter());
        }

        for (ZZone zone : zones) {
            zone.center.scaleEq(1.0f / zone.cells.size());
        }

        return new GDimension(dim, dim);
    }

    /**
     *
     * @param actor
     * @param zoneIndex
     */
    public void addActor(ZActor actor, int zoneIndex) {
        ZZone zone = zones.get(zoneIndex);
        boolean added = false;
        for (int c=0;!added && c < zone.cells.size(); c++) {
            Grid.Pos cellPos = zone.cells.get(zone.nextCell);
            zone.nextCell = (zone.nextCell + 1) % zone.cells.size();
            if (addActorToCell(actor, cellPos)) {
                added = true;
                break;
            }
        }
        if (!added) {
            //throw new AssertionError("Failed to add Actor");
            System.err.println("Zone " + zoneIndex + " is full!");

            if (actor instanceof ZCharacter) {
                // replace the lowest priority zombie withthe chararcter
                List<ZZombie> zombies = getZombiesInZone(zoneIndex);
                assert(zombies.size() > 0);
                Collections.sort(zombies, (o1, o2) -> Integer.compare(o1.type.ordinal(), o2.type.ordinal()));
                ZZombie removed = zombies.get(0);
                removeActor(removed);
                addActorToCell(actor, removed.occupiedCell);
            }
        }
    }

    public void removeActor(ZActor actor) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.occupied[actor.occupiedQuadrant] = null;
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        //actor.occupiedZone = -1;
        //actor.occupiedQuadrant = -1;
        //actor.occupiedCell = null;
    }

    /**
     * To be called after load game - will make sure actors asyncronized with cell quadrants
     * @param actors
     */
    void syncActors(List<ZActor> actors) {
        for (ZActor actor : actors) {
            ZZone zone = zones.get(actor.occupiedZone);
            ZCell cell = getCell(actor.occupiedCell);
            cell.occupied[actor.occupiedQuadrant] = actor;
        }
    }

    public ZActor drawActors(AGraphics g, ZTiles tiles, int mx, int my) {
        ZActor picked = null;
        for (ZCell cell : getCells()) {
            for (int i=0; i<cell.occupied.length; i++) {
                ZActor a = cell.occupied[i];
                if (a == null)
                    continue;
                AImage img = g.getImage(tiles.getImage(a.getType()));
                if (img != null) {
                    GRectangle rect = cell.getQuadrant(i).fit(img);
                    if (rect.contains(mx, my))
                        picked = a;
                    g.drawImage(tiles.getImage(a.getType()), rect);
                    a.rect = rect;
                }
            }
        }
        return picked;
    }

    // TODO - Move this to the renderer so we can do better highlighting in android canvas
    public GRectangle drawActor(AGraphics g, ZTiles tiles, ZActor actor, GColor outline) {
        int id=tiles.getImage(actor.getType());
        AImage img = g.getImage(id);
        if (img != null) {
            if (actor.rect == null)
                actor.rect = getCell(actor.occupiedCell).getQuadrant(actor.occupiedQuadrant).fit(img);
            if (outline != null) {
                GColor oldColor = g.getColor();
                g.setColor(outline);
                g.drawRect(actor.rect, 2);
                g.setColor(oldColor);
            }
            g.drawImage(id, actor.rect);
            return actor.rect;
        }
        return null;
    }

    /**
     * Iterate over all cells
     * @return
     */
    public Iterable<ZCell> getCells() {
        return grid.getCells();
    }

    public void drawZoneOutline(AGraphics g, int zoneIndex) {
        ZZone zone = getZone(zoneIndex);
        for (Grid.Pos cellPos : zone.cells) {
            g.drawRect(getCell(cellPos).rect, 1);
        }
    }

    public List<ZZombie> getZombiesInZone(int zoneIdx) {
        return Utils.filter(getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getCharactersInZone(int zoneIdx) {
        return Utils.filter(getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public List getActorsInZone(int zoneIndex) {
        List<ZActor> actors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            for (ZActor a : getCell(cellPos).occupied) {
                if (a != null) {
                    actors.add(a);
                }
            }
        }
        return actors;
    }

    public List getAllActors() {
        List<ZActor> actors = new ArrayList<>();
        for (ZCell cell : grid.getCells()) {
            for (ZActor a : cell.occupied) {
                if (a != null)
                    actors.add(a);
            }
        }
        return actors;
    }

    public List<ZZombie> getAllZombies() {
        return Utils.filter(getAllActors(), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getAllCharacters() {
        return Utils.filter(getAllActors(), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    /*
    public List<ZCellDoor> getDoorsForZone(int occupiedZone, ZWallFlag ... flags) {
        List<ZCellDoor> doors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(occupiedZone).cells) {
            ZCell cell = getCell(cellPos);
            for (int i=0; i<4; i++) {
                if (Utils.linearSearch(flags, cell.walls[i]) >= 0) {
                    doors.add(new ZCellDoor(cellPos, i));
                }
            }
        }
        return doors;
    }

    Grid.Pos getAdjacentPos(Grid.Pos pos, int direction) {
        int col = pos.getColumn();
        int row = pos.getRow();
        switch (direction) {
            case DIR_NORTH:
                if (row <= 0)
                    return null;
                row--;
                break;
            case DIR_SOUTH:
                if (row >= getRows())
                    return null;
                row++;
                break;
            case DIR_EAST:
                if (col >= getColumns())
                    return null;
                col++;
                break;
            case DIR_WEST:
                if (col <= 0)
                    return null;
                col--;
                break;
        }
        return new Grid.Pos(row, col);
    }*/

    public void moveActorInDirection(ZActor actor, ZDir direction) {
        Grid.Pos pos = actor.occupiedCell;
        Grid.Pos adj = direction.getAdjacent(pos);
        if (adj != null) {
            removeActor(actor);
            addActorToCell(actor, adj);
        }
    }

    public boolean addActorToCell(ZActor actor, Grid.Pos pos) {
        ZCell cell = getCell(pos);
        int qi = actor.occupiedQuadrant;
        for (int i = 0; i < cell.occupied.length; i++) {
            if (cell.occupied[qi] == null) {
                cell.occupied[qi] = actor;
                actor.occupiedZone = cell.zoneIndex;
                actor.occupiedCell = pos;
                actor.occupiedQuadrant = qi;
                zones.get(cell.zoneIndex).noiseLevel += actor.getNoise();
                return true;
            }
            qi = (qi+1)%cell.occupied.length;
        }
        return false;
    }

    public void getUndiscoveredIndoorZones(Grid.Pos startPos, Set<Integer> undiscovered) {
        ZCell cell = getCell(startPos);
        if (cell.discovered)
            return;
        cell.discovered = true;
        ZZone zone = zones.get(cell.zoneIndex);
        if (!zone.isSearchable())
            return;
        undiscovered.add(cell.zoneIndex);
        for (ZDir dir : ZDir.values()) {
            switch (cell.getWallFlag(dir)) {
                case NONE:
                case OPEN:
                    getUndiscoveredIndoorZones(dir.getAdjacent(startPos), undiscovered);
                    break;
            }
        }
    }

    void resetNoise() {
        for (ZZone zone : zones) {
            zone.noiseLevel = 0;
            for (Grid.Pos pos: zone.cells) {
                for (ZActor a : getCell(pos).occupied) {
                    if (a != null && a instanceof ZCharacter) {
                        zone.noiseLevel ++;
                    }
                }
            }
        }
    }

    public boolean canMove(ZActor actor, ZDir dir) {
        return getCell(actor.occupiedCell).getWallFlag(dir).isOpen();
    }
}
