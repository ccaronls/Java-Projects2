package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class SlashedAnimation extends ZActorAnimation {

    final int claws = Utils.randItem(ZIcon.CLAWS.imageIds);
    GRectangle rect;

    public SlashedAnimation(ZActor actor) {
        super(actor, 1000);
        rect = actor.getRect();
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        AImage img = g.getImage(claws);
        g.setTransparencyFilter(1f-position);
        g.drawImage(claws, rect.fit(img));
        g.removeFilter();
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
