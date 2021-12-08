package cc.lib.zombicide;

import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZSpawnArea extends Reflector<ZSpawnArea> {

    static {
        addAllFields(ZSpawnArea.class);
    }

    private ZIcon icon;
    private final ZDir dir;
    private boolean canSpawnNecromancers;
    private boolean isEscapableForNecromancers;
    private boolean canBeRemovedFromBoard;
    private GRectangle rect = null;
    private final Grid.Pos cellPos;

    public ZSpawnArea() {
        this(null,null);
    }

    /**
     * Default behavior for most spawns
     *
     * @param dir
     */
    public ZSpawnArea(Grid.Pos cellPos, ZDir dir) {
        this(cellPos, ZIcon.SPAWN_RED, dir, false, true, true);
    }

    public ZSpawnArea(Grid.Pos cellPos, ZIcon icon, ZDir dir, boolean canSpawnNecromancers, boolean isEscapableForNecromancers, boolean canBeRemovedFromBoard) {
        this.cellPos = cellPos;
        this.icon = icon;
        this.dir = dir;
        this.canSpawnNecromancers = canSpawnNecromancers;
        this.isEscapableForNecromancers = isEscapableForNecromancers;
        this.canBeRemovedFromBoard = canBeRemovedFromBoard;
    }

    public ZIcon getIcon() {
        return icon;
    }

    public ZDir getDir() {
        return dir;
    }

    public GRectangle getRect() {
        return rect;
    }

    public void setRect(GRectangle rect) {
        this.rect = rect;
    }

    public Grid.Pos getCellPos() {
        return cellPos;
    }

    public boolean isCanSpawnNecromancers() {
        return canSpawnNecromancers;
    }

    public boolean isEscapableForNecromancers() {
        return isEscapableForNecromancers;
    }

    public boolean isCanBeRemovedFromBoard() {
        return canBeRemovedFromBoard;
    }

    public void setIcon(ZIcon icon) {
        this.icon = icon;
    }

    public void setCanSpawnNecromancers(boolean canSpawnNecromancers) {
        this.canSpawnNecromancers = canSpawnNecromancers;
    }

    public void setEscapableForNecromancers(boolean escapableForNecromancers) {
        isEscapableForNecromancers = escapableForNecromancers;
    }

    public void setCanBeRemovedFromBoard(boolean canBeRemovedFromBoard) {
        this.canBeRemovedFromBoard = canBeRemovedFromBoard;
    }
}
