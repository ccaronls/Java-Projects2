package cc.game.soc.ui;

import cc.game.soc.core.SOC;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;

public final class UIBarbarianRenderer extends UIRenderer {

	private int baseImage;
	private int shipImage;
    private int distance = -1;//positions.length-1;

    private final static float IMAGE_WIDTH = 1f/454;
    private final static float IMAGE_HEIGHT = 1f/502;

    private final static Vector2D[] positions = {
        new Vector2D(64, 431).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(192,429).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(319,404).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(284,283).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(199,194).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(128,89).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(255,74).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
        new Vector2D(383,66).scaledBy(IMAGE_WIDTH, IMAGE_HEIGHT),
    };

	public UIBarbarianRenderer(UIComponent component) {
	    super(component);
	    component.setRenderer(this);
	}

	public void initAssets(int baseImage, int shipImage) {
        this.baseImage = baseImage;
        this.shipImage = shipImage;
    }

    private AAnimation<AGraphics> anim = null;
    //private float minShipDim, maxShipDim;

    private float shipDim = 0;

    public float textBorderPadding = 5;

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

        if (distance >= 0 && distance < positions.length) {
            int d = (positions.length-1) - distance;
            final float scale = 1f + (1f / (positions.length - 1));
            //float shipDim = minShipDim + (maxShipDim - minShipDim) * scale * d;
            Vector2D v = positions[distance].scaledBy(wid, hgt);
            float sh2 = shipDim / 2;
            g.drawImage(shipImage, v.sub(sh2, sh2), v.add(sh2, sh2));
        }

        // draw the settlers vs barbrian strengths in either uppleft hand corner or lower right hand corner
        UISOC soc = UISOC.getInstance();
        int barbStr = SOC.computeBarbarianStrength(soc, soc.getBoard());
        int catanStr = SOC.computeCatanStrength(soc, soc.getBoard());
        String text = String.format("%-10s %d\n%-10s %d", "Settlers:", catanStr, "Barbarians:", barbStr);
        final float tb2 = textBorderPadding*2;
        GDimension dim = g.getTextDimension(text, wid-tb2);
        if (distance < positions.length/2) {
            // upper left hand corner
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(0, 0, dim.width+tb2, dim.height+tb2);
            g.setColor(GColor.CYAN);
            g.drawWrapString(textBorderPadding, textBorderPadding, wid-tb2, text);
        } else {
            // lower right hand corner
            g.setColor(GColor.TRANSLUSCENT_BLACK);
            g.drawFilledRect(wid-dim.width-tb2, hgt-dim.height-tb2, dim.width+tb2, dim.height+tb2);
            g.setColor(GColor.CYAN);
            g.drawWrapString(wid-dim.width-textBorderPadding, hgt-dim.height-textBorderPadding, wid-tb2, text);
        }
	}

	public void setDistance(final int nextDistance) {

        if (nextDistance < distance && distance < positions.length) {

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

    public void onBarbarianAttack(int catanStrength, int barbarianStrength, String[] playerStatus) {
    }
}
