package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class ShieldBlockAnimation extends ZActorAnimation {

    GRectangle rect = null;

    public ShieldBlockAnimation(ZActor actor) {
        super(actor, 1000);
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int id = ZIcon.SHIELD.imageIds[0];
        AImage img = g.getImage(id);
        if (rect == null)
            rect = actor.getRect().fit(img).scaledBy(.5f);
        g.setTransparencyFilter(1f-position);
        g.drawImage(id, rect);
        g.removeFilter();
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
