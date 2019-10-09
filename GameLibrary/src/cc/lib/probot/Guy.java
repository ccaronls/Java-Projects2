package cc.lib.probot;

import cc.lib.game.GColor;

public class Guy {
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
