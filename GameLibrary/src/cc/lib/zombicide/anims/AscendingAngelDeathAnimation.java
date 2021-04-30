package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class AscendingAngelDeathAnimation extends DeathAnimation {

    public AscendingAngelDeathAnimation(ZActor a) {
        super(a);
        setDuration(4000);
        // at the end of the 'ascending angel' grow a tombstone
        a.addAnimation(new ZActorAnimation(a, 2000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GRectangle rect = new GRectangle(actor.getRect());
                rect.y += rect.h*(1f-position);
                rect.h *= position;
                g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect);
            }
        });
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
