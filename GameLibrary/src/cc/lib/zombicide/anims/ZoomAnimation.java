package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ui.UIZBoardRenderer;

public class ZoomAnimation extends ZActorAnimation {

    final float targetZoomPercent;
    final float startZoomPercent;
    final Vector2D startCenter;
    final UIZBoardRenderer renderer;
    final Vector2D dv;
    final float dz;

    public ZoomAnimation(ZActor actor, UIZBoardRenderer renderer, float zoomPercent) {
        this(actor, actor.getRect().getCenter(), renderer, zoomPercent);
    }

    public ZoomAnimation(UIZBoardRenderer renderer, float zoomPercent) {
        this(null, renderer.getBoardCenter(), renderer, zoomPercent);
    }

    /**
     *
     * @param actor
     * @param center
     * @param renderer
     * @param zoomPercent value between 0-1 where 0 is full zoom out and 1 is full zoom into the target rectangle
     */
    public ZoomAnimation(ZActor actor, IVector2D center, UIZBoardRenderer renderer, float zoomPercent) {
        super(actor, 500);
        this.targetZoomPercent = zoomPercent;
        this.renderer = renderer;
        startCenter = new Vector2D(renderer.getBoardCenter());
        startZoomPercent = renderer.getZoomPercent();
        Vector2D cntr = new Vector2D(center);
        // we want the actor to be off to the left or right
        dv = cntr.sub(startCenter);
        dz = targetZoomPercent - startZoomPercent;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        renderer.setZoomPercent(startZoomPercent + dz*position);
    }

    @Override
    public boolean hidesActor() {
        return false;
    }
}
