package cc.game.soc.ui;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

public final class UIBarbarianRenderer implements UIRenderer {

	private int baseImage;
	private int shipImage;
    private final UIComponent component;
    private int distance = -1;//positions.length-1;

    private final static float IMAGE_WIDTH = 1f/454;
    private final static float IMAGE_HEIGHT = 1f/502;

    private final static Vector2D[] positions = {
        new Vector2D(192,429).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(319,404).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(284,283).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(199,194).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(128,89).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(255,74).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(383,66).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
    };

	public UIBarbarianRenderer(UIComponent component) {
	    this.component = component;
	    component.setRenderer(this);
	}

	public void initAssets(int baseImage, int shipImage) {
        this.baseImage = baseImage;
        this.shipImage = shipImage;
    }

    private AAnimation<AGraphics> anim = null;
    //private float minShipDim, maxShipDim;

    private float shipDim = 0;

	@Override
	public final void draw(APGraphics g, int pickX, int pickY) {
        final float wid = component.getWidth();
        final float hgt = component.getHeight();

        g.drawImage(baseImage, 0, 0, wid, hgt);
        //minShipDim = wid/9;
        //maxShipDim = wid/7;
        shipDim = wid/8;

	    if (anim != null) {
            if (anim.isDone())
                anim = null;
            else {
                anim.update(g);
                return;
            }
        }

        if (distance >= 0) {
            int d = (positions.length-1) - distance;
            final float scale = 1f + (1f / (positions.length - 1));
            //float shipDim = minShipDim + (maxShipDim - minShipDim) * scale * d;
            Vector2D v = positions[distance].scaledBy(wid, hgt);
            float sh2 = shipDim / 2;
            g.drawImage(shipImage, v.sub(sh2, sh2), v.add(sh2, sh2));
        }
	}

	public void setDistance(final int nextDistance) {

        if (nextDistance < distance) {

            anim = new UIAnimation(2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    final float scale = 1f + (1f / (positions.length - 1));
                    final float wid = component.getWidth();
                    final float hgt = component.getHeight();

                    final int d0 = (positions.length-1) - distance;
                    final int d1 = (positions.length-1) - nextDistance;

                    //final float min = minShipDim + (maxShipDim - minShipDim) * scale * d0;
                    //final float max = minShipDim + (maxShipDim - minShipDim) * scale * d1;

                    final Vector2D v0 = positions[distance].scaledBy(wid, hgt);
                    final Vector2D v1 = positions[nextDistance].scaledBy(wid, hgt);

                    Vector2D pos = v0.add(v1.sub(v0).scaledBy(position));
                    float sh2 = 0.5f * shipDim;//min + (max-min)*position;

                    g.drawImage(shipImage, pos.sub(sh2, sh2), pos.add(sh2, sh2));
                    component.redraw();
                }

            }.start();
            component.redraw();

            Utils.waitNoThrow(anim, -1);
        }

        distance = nextDistance;
        component.redraw();

    }

    @Override
    public void doClick() {

    }

    @Override
    public void startDrag(float x, float y) {

    }

    @Override
    public void endDrag() {

    }

    public void onBarbarianAttack(int catanStrength, int barbarianStrength, String[] playerStatus) {
    }
}
