package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZIcon;

public class MeleeAnimation extends ZActorAnimation {

    final int id;
    final GRectangle rect;

    public MeleeAnimation(ZActor actor, ZBoard board) {
        super(actor, 400);
        id = Utils.randItem(ZIcon.SLASH.imageIds);
        rect = actor.getRect().scaledBy(1.3f).moveBy(Vector2D.newRandom(.1f));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        AImage img = g.getImage(id);
        g.setTransparencyFilter(1f - position);
        g.drawImage(id, rect.fit(img));
        g.removeFilter();
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}