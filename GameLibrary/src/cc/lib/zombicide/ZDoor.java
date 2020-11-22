package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public abstract class ZDoor extends Reflector<ZDoor>  {

    /**
     * Human readable text
     * @return
     */
    public abstract String name();

    /**
     * Cell at entering part of the door
     * @return
     */
    public abstract Grid.Pos getCellPos();

    /**
     * Cell at the exiting part of the door
     * @return
     */
    public abstract Grid.Pos getCellPosEnd();

    /**
     * Direction actor to move to enter the door
     * @return
     */
    public abstract ZDir getMoveDirection();

    /**
     * Graphical rectable of this door
     * @param board
     * @return
     */
    public abstract GRectangle getRect(ZBoard board);

    /**
     *
     * @param board
     * @return
     */
    public abstract boolean isClosed(ZBoard board);

    /**
     * Oopen or close the door
     * @param board
     */
    public abstract void toggle(ZBoard board);

    /**
     * Render the door
     * @param g
     * @param b
     */
    public abstract void draw(AGraphics g, ZBoard b);

    /**
     * Get the door representation of opposing cell
     * @param b
     * @param <T>
     * @return
     */
    public abstract <T extends ZDoor> T getOtherSide(ZBoard b);

    /**
     * return whether the player needs weapon with canOpenDoor status to open this door
     * @return
     */
    public abstract boolean isJammed();

    /**
     * return whether this door can be closed by a player
     * @param c
     * @return
     */
    public abstract boolean canBeClosed(ZCharacter c);

    /**
     *
     * @return
     */
    public boolean isLocked(ZBoard board) {
        return false;
    }

    /**
     *
     * @return
     */
    public GColor getLockedColor() {
        return GColor.RED;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ZDoor))
            return false;
        ZDoor o2 = (ZDoor)obj;
        return (getCellPos().equals(o2.getCellPos()) && getCellPosEnd().equals(o2.getCellPosEnd()));
    }
}
