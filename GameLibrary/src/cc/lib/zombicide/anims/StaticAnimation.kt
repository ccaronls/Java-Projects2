package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class StaticAnimation extends ZActorAnimation {

    final int imageId;
    final GRectangle rect;
    final boolean fadeOut;

    public StaticAnimation(ZActor actor, long duration, int imageId, GRectangle rect) {
        this(actor, duration, imageId, rect, false);
    }


    public StaticAnimation(ZActor actor, long duration, int imageId, GRectangle rect, boolean fadeOut) {

        super(actor, duration);
        this.imageId = imageId;
        this.rect = rect;
        this.fadeOut = fadeOut;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        AImage img = g.getImage(imageId);
        if (fadeOut) {
            g.setTransparencyFilter(1f-position);
        }
        g.drawImage(imageId, rect.fit(img));
        if (fadeOut)
            g.removeFilter();
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}
