package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public abstract class ZDoor extends Reflector<ZDoor>  {

    public abstract String name();

    public abstract Grid.Pos getCellPos();

    public abstract GRectangle getRect(ZBoard board);

    public abstract boolean isClosed(ZBoard board);

    public abstract void toggle(ZBoard board);

    public abstract void draw(AGraphics g, ZBoard b);

    public abstract <T extends ZDoor> T getOtherSide(ZBoard b);

    public abstract boolean isJammed();

    public abstract boolean canBeClosed(ZCharacter c);
}
