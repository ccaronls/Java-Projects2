package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZIcon;

public class FireballAnimation extends ZActorAnimation {

    final Vector2D path;
    final Vector2D start;
    final GRectangle rect;

    public FireballAnimation(ZActor actor, Vector2D end) {
        super(actor, 500);
        this.rect = actor.getRect().scaledBy(.5f);
        this.start = rect.getCenter();
        path = end.sub(start);
        setDuration(Math.round(path.mag()* 700));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        int id = Utils.randItem(ZIcon.FIREBALL.imageIds);
        AImage img = g.getImage(id);
        //GRectangle rect = attacker.getRect(board).scaledBy(.5f).fit(img);
        Vector2D pos = start.add(path.scaledBy(position));
        GRectangle r = rect.fit(img).setCenter(pos);
        g.drawImage(id, r);
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}