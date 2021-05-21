package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;

public class AscendingAngelDeathAnimation extends DeathAnimation {

    public AscendingAngelDeathAnimation(ZActor a) {
        super(a);
        setDuration(4000);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        super.draw(g, position, dt);
        GRectangle rect = new GRectangle(actor.getRect());
        rect.y -= rect.h * 3 * position;
        g.setTransparencyFilter(.5f - position/3);
        g.drawImage(actor.getImageId(), rect);
        g.removeFilter();
    }

}
