package cc.lib.zombicide.anims;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.IVector2D;
import cc.lib.math.Vector2D;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.ui.UIZBoardRenderer;

public class ZoomAnimation extends ZActorAnimation {

    final float targetZoom;
    final float startZoom;
    final Vector2D startCenter;
    final UIZBoardRenderer renderer;
    final Vector2D dv;
    final float dz;

    /**
     * This version zooms to the zone the actor is standing
     * @param actor
     * @param renderer
     * @param board
     */
    public static ZoomAnimation build(ZActor actor, UIZBoardRenderer renderer, ZBoard board) {
        ZZone zone = board.getZone(actor.getOccupiedZone());
        GRectangle rect = zone.getRectangle();
        float targetZoom = Math.max(rect.getWidth(), rect.getHeight());
        IVector2D center = rect.getCenter();
        return new ZoomAnimation(actor, center, renderer, targetZoom);
    }

    public ZoomAnimation(ZActor actor, UIZBoardRenderer renderer, float targetZoom) {
        this(actor, actor.getRect().getCenter(), renderer, targetZoom);
    }

    public ZoomAnimation(ZActor actor, IVector2D center, UIZBoardRenderer renderer, float targetZoom) {
        super(actor, 500);
        this.targetZoom = targetZoom;
        this.renderer = renderer;
        startCenter = new Vector2D(renderer.getBoardCenter());
        startZoom = renderer.getZoomAmt();
        Vector2D cntr = new Vector2D(center);
        // we want the actor to be off to the left or right
        dv = cntr.sub(startCenter);
        dz = targetZoom - startZoom;
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        renderer.setBoardCenter(startCenter.add(dv.scaledBy(position)));
        renderer.setZoomAmt(startZoom + dz*position);
    }

    @Override
    protected boolean hidesActor() {
        return false;
    }
}
