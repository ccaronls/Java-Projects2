package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZBoard extends Reflector<ZBoard> {

    private final static Logger log = LoggerFactory.getLogger(ZBoard.class);

    static {
        addAllFields(ZBoard.class);
    }

    private Grid<ZCell> grid;
    private List<ZZone> zones;
    private int zoom = 0;

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

    public int getNumZones() {
        return zones.size();
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
    public List<Integer> getAccessableZones(int zoneIndex, int distance, ZActionType action) {
        if (distance == 0)
            return Collections.singletonList(zoneIndex);

        List<Integer> result = new ArrayList<>();

        ZDir [] options = action == ZActionType.MOVE ? ZDir.values() : ZDir.getCompassValues();

        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            // fan out in all direction to given distance
            //for (int dir = 0; dir <4; dir++) {
            for (ZDir dir : options) {
                Grid.Pos pos = cellPos;
                boolean open = true;
                for (int dist = 0; dist <distance; dist++) {
                    if (!grid.get(pos).getWallFlag(dir).isOpen()) {
                        open = false;
                        break;
                    }
                    pos = getAdjacent(pos, dir);
                }
                if (open && grid.get(pos).zoneIndex != zoneIndex) {
                    result.add(grid.get(pos).zoneIndex);
                }
            }
        }
        return result;
    }

    public Grid.Pos getAdjacent(Grid.Pos from, ZDir dir) {
        switch (dir) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return new Grid.Pos(from.getRow() + dir.dy, from.getColumn() + dir.dx);

            case ASCEND:
            case DESCEND:
                return findDoor(from, dir).getCellPosEnd();
        }
        Utils.assertTrue(false);
        return null;
    }

    public ZDoor findDoor(Grid.Pos pos, ZDir dir) {
        for (ZDoor door : zones.get(getCell(pos).zoneIndex).doors) {
            if (door.getCellPosStart().equals(pos) && door.getMoveDirection()==dir) {
                return door;
            }
        }
        //Utils.assertTrue(false);
        return null;
    }

    public boolean canSee(int fromZone, int toZone) {
        if (fromZone == toZone)
            return true;
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

            fromCell = getAdjacent(fromCell, dir);
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
    public List<List<ZDir>> getShortestPathOptions(Grid.Pos fromPos, int toZoneIndex) {
        if (grid.get(fromPos).zoneIndex == toZoneIndex)
            return Collections.emptyList();

        ZZone toZone = zones.get(toZoneIndex);

        List<List<ZDir>> allPaths = new ArrayList<>();
        int maxDist = (grid.getRows() + grid.getCols()) * 2;

        Set<Grid.Pos> visited = new HashSet<>();
        for (Grid.Pos bCellPos : toZone.cells) {
            List<List<ZDir>> paths = getShortestPathOptions(fromPos, bCellPos, visited, maxDist);
            for (List l : paths) {
                maxDist = Math.min(maxDist, l.size());
            }
            allPaths.addAll(paths);
        }

        Iterator<List<ZDir>> it = allPaths.iterator();
        while (it.hasNext()) {
            if (it.next().size() > maxDist)
                it.remove();
        }

        return allPaths;
    }

    private List<List<ZDir>> getShortestPathOptions(Grid.Pos fromCell, Grid.Pos toCell, Set<Grid.Pos> visited, int maxDist) {
        List<List<ZDir>> paths = new ArrayList<>();
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

        for (ZDir dir : ZDir.valuesSorted(fromPos, toPos)) {
            if (fromCell.getWallFlag(dir).isOpen()) {
                Grid.Pos nextPos = getAdjacent(fromPos, dir);
                if (visited.contains(nextPos))
                    continue;

                // is the cell full?
                if (getCell(nextPos).isFull())
                    continue;
                curPath.addLast(dir);
                searchPathsR(nextPos, toPos, maxDist, curPath, paths, visited);
                curPath.removeLast();
            }
        }


        ZZone fromZone = zones.get(fromCell.getZoneIndex());
        for (ZDoor door : fromZone.getDoors()) {
            if (door.getCellPosStart().equals(fromPos) && !door.isClosed(this)) {
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

    public ZWallFlag getDoor(ZDoor door) {
        return getCell(door.getCellPosStart()).getWallFlag(door.getMoveDirection());
    }

    public void setDoor(ZDoor door, ZWallFlag flag) {
        getCell(door.getCellPosStart()).setWallFlag(door.getMoveDirection(), flag);
        if (door.getCellPosEnd() != null) {
            door = door.getOtherSide();
            getCell(door.getCellPosStart()).setWallFlag(door.getMoveDirection(), flag);
        }
    }

    public void setDoorLocked(ZDoor door) {
        addLockedDoor(door);
        addLockedDoor(door.getOtherSide());
    }

    private void addLockedDoor(ZDoor door) {
        ZCell cell = getCell(door.getCellPosStart());
        ZZone zone = zones.get(cell.zoneIndex);
        if (zone.doors.contains(door))
            throw new AssertionError("Already have a door like that.");
        cell.setWallFlag(door.getMoveDirection(), ZWallFlag.LOCKED);
        zone.doors.add(door);
    }

    public void setSpawnZone(int zoneIdx, boolean isSpawn) {
        ZZone zone = zones.get(zoneIdx);
        zone.isSpawn = isSpawn;
        getCell(zone.cells.get(0)).setCellType(ZCellType.SPAWN_NORTH, true);
    }

    public int getMaxNoiseLevel() {
        int maxNoise = 0;
        for (ZZone z : zones) {
            if (z.noiseLevel > maxNoise) {
                maxNoise = z.noiseLevel;
            }
        }
        return maxNoise;
    }

    public ZZone getMaxNoiseLevelZone() {
        int maxNoise = 0;
        ZZone maxZone = null;
        for (ZZone z : zones) {
            if (z.noiseLevel > maxNoise) {
                maxZone = z;
                maxNoise = z.noiseLevel;
            } else if (z.noiseLevel == maxNoise) {
                maxZone = null; // when multiple zones share same noise level, then neither are the max
            }
        }
        return maxZone;
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
            if (getCell(cellPos).isFull())
                continue;
            addActorToCell(actor, cellPos);
            added = true;
            break;
        }
        if (!added && actor.getPriority() > 2) {
            //throw new AssertionError("Failed to add Actor");
            log.warn("Zone " + zoneIndex + " is full!");

            int minPriority = 100;
            Grid.Pos minPos = null;
            for (Grid.Pos pos : zone.cells) {
                ZCell cell = getCell(pos);
                ZCellQuadrant q = cell.findLowestPriorityOccupant();
                int priority = cell.getOccupant(q).getPriority();
                if (priority < minPriority || minPos == null) {
                    minPriority = priority;
                    minPos = pos;
                }
            }
            addActorToCell(actor, minPos);
        }
    }

    public void moveActor(ZActor actor, int toZone) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        addActor(actor, toZone);
    }

    public void moveActor(ZActor actor, Grid.Pos cellPos) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        addActorToCell(actor, cellPos);
    }

    public void removeActor(ZActor actor) {
        ZCell cell = getCell(actor.occupiedCell);
        cell.setQuadrant(null, actor.occupiedQuadrant);
        zones.get(cell.zoneIndex).noiseLevel -= actor.getNoise();
        //actor.occupiedZone = -1;
        //actor.occupiedQuadrant = -1;
        //actor.occupiedCell = null;
    }

    /**
     * Iterate over all cells
     * @return
     */
    public Grid.Iterator<ZCell> getCellsIterator() {
        return grid.iterator();
    }

    /**
     * Iterate over all cells
     * @return
     */
    public Iterable<ZCell> getCells() {
        return grid.getCells();
    }

    public List<ZZombie> getZombiesInZone(int zoneIdx) {
        return Utils.filter((List)getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getCharactersInZone(int zoneIdx) {
        return Utils.filter((List)getActorsInZone(zoneIdx), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public List<ZActor> getActorsInZone(int zoneIndex) {
        List<ZActor> actors = new ArrayList<>();
        for (Grid.Pos cellPos : zones.get(zoneIndex).cells) {
            for (ZActor a : getCell(cellPos).getOccupant()) {
                if (a != null) {
                    actors.add(a);
                }
            }
        }
        return actors;
    }

    public List<ZActor> getAllLiveActors() {
        List<ZActor> actors = new ArrayList<>();
        for (ZCell cell : grid.getCells()) {
            for (ZActor a : cell.getOccupant()) {
                if (a != null)
                    actors.add(a);
            }
        }
        return actors;
    }

    public List<ZZombie> getAllZombies() {
        return Utils.filter((List)getAllLiveActors(), (Utils.Filter<ZActor>) object -> object instanceof ZZombie);
    }

    public List<ZCharacter> getAllCharacters() {
        return Utils.filter((List)getAllLiveActors(), (Utils.Filter<ZActor>) object -> object instanceof ZCharacter);
    }

    public void addActorToCell(ZActor actor, Grid.Pos pos) {
        ZCell cell = getCell(pos);
        ZCellQuadrant current = actor.occupiedQuadrant;
        if (current == null) {
            current = actor.getSpawnQuadrant();
        }
        if (current == null || cell.getOccupant(current) != null) {
            current = cell.findLowestPriorityOccupant();
        }
        Utils.assertTrue(current != null);
        if (cell.getOccupant(current) != null && cell.getOccupant(current).getPriority() >= actor.getPriority())
            return;
        cell.setQuadrant(actor, current);
        actor.occupiedZone = cell.zoneIndex;
        actor.occupiedCell = pos;
        actor.occupiedQuadrant = current;
        zones.get(cell.zoneIndex).noiseLevel += actor.getNoise();
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
            if (cell.getWallFlag(dir).isOpen())
                getUndiscoveredIndoorZones(getAdjacent(startPos, dir), undiscovered);
        }
    }

    void resetNoise() {
        for (ZZone zone : zones) {
            zone.noiseLevel = 0;
            for (Grid.Pos pos: zone.cells) {
                for (ZActor a : getCell(pos).getOccupant()) {
                    if (a != null && a instanceof ZCharacter) {
                        zone.noiseLevel ++;
                    }
                }
            }
        }
    }

    /**
     *
     * @param actor
     * @param dir
     * @return
     */
    public boolean canMove(ZActor actor, ZDir dir) {
        return getCell(actor.occupiedCell).getWallFlag(dir).isOpen();
    }

    /**
     *
     * @return
     */
    public GDimension getDimension() {
        if (grid == null || grid.getCols() == 0 || grid.getRows() == 0)
            return new GDimension(0,0);

        Vector2D br = grid.get(grid.getRows()-1, grid.getCols()-1).getBottomRight();
        return new GDimension(br.getX(), br.getY());
    }

    public GRectangle getZoomedRectangle(IVector2D center) {
        GDimension dim = getDimension();
        GDimension zoomed = new GDimension(dim.width-zoom, dim.height-zoom);

        GRectangle rect = new GRectangle(zoomed).withCenter(center);
        rect.x = Utils.clamp(rect.x, 0, dim.width-rect.w);
        rect.y = Utils.clamp(rect.y, 0, dim.height-rect.h);
        return rect;
    }

    public void zoom(int amount) {
        int z = zoom + amount;
        if (z >= 0 && z < getMaxZoom()) {
            zoom = z;
        }
    }

    int getMaxZoom() {
        return Math.min(getRows(), getColumns());
    }

    public ZDir getDirection(int fromZone, int toZone) {
        ZZone start = getZone(fromZone);
        ZZone end   = getZone(toZone);
        Vector2D dv = end.getCenter().sub(start.getCenter());
        if (dv.isZero())
            return ZDir.EAST;
        float angle = dv.angleOf();
        if (angle >270-45 && angle <270+45)
            return ZDir.NORTH;
        if (angle >180-45 && angle <180+45)
            return ZDir.WEST;
        if (angle >90-45 && angle <90+45)
            return ZDir.SOUTH;
        return ZDir.EAST;
    }
}
