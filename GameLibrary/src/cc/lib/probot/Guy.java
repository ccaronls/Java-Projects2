package cc.lib.probot;

import cc.lib.game.GColor;
import cc.lib.utils.Reflector;

public class Guy extends Reflector<Guy> {

    static {
        addAllFields(Guy.class);
    }

    public int posx=0, posy=2;
    public Direction dir= Direction.Right;
    public GColor color = GColor.RED;

    public Guy() {}

    public Guy(int posx, int posy, Direction dir, GColor color) {
        this.posx = posx;
        this.posy = posy;
        this.dir = dir;
        this.color = color;
    }
}
