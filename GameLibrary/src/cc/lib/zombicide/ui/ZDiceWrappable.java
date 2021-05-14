package cc.lib.zombicide.ui;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;

public class ZDiceWrappable implements UIZCharacterRenderer.IWrappable {


    final Integer [] dieNums;
    public ZDiceWrappable(Integer [] dieNums) {
        this.dieNums = dieNums;
    }

    @Override
    public GDimension drawWrapped(APGraphics g, float maxWidth) {

        float dim = g.getTextHeight()*2;
        float padding = dim/8;

        if (maxWidth < dim) {
            maxWidth = dim;
        }
        int rows = 1;
        float w = maxWidth;

        int dieNumIdx = 0;
        g.pushMatrix();
        g.translate(-dim, 0);
        g.pushMatrix();
        while (true) {
            if (dieNumIdx >= dieNums.length)
                break;
            if (w < 0) {
                g.popMatrix();
                g.translate(0, dim);
                g.pushMatrix();
                w = maxWidth;
            }
            drawDie(g, dim, GColor.WHITE, GColor.BLACK, dieNums[dieNumIdx++]);
            g.translate(-(dim + padding), 0);
        }
        g.popMatrix();
        g.popMatrix();
        return new GDimension(maxWidth, dim*rows);
    }

    public void drawDie(AGraphics g, float dim, GColor dieColor, GColor dotColor, int numDots) {
        g.setColor(dieColor);
        float arc = dim/4;
        g.drawFilledRoundedRect(0, 0, dim, dim, arc);
        g.setColor(dotColor);
        float dd2 = dim/2;
        float dd4 = dim/4;
        float dd34 = (dim*3)/4;
        float dotSize = dim/8;
        float oldDotSize = g.setPointSize(dotSize);
        g.begin();
        switch (numDots) {
            case 1:
                g.vertex(dd2, dd2);
                break;
            case 2:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                break;
            case 3:
                g.vertex(dd4, dd4);
                g.vertex(dd2, dd2);
                g.vertex(dd34, dd34);
                break;
            case 4:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                break;
            case 5:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd2, dd2);
                break;
            case 6:
                g.vertex(dd4, dd4);
                g.vertex(dd34, dd34);
                g.vertex(dd4, dd34);
                g.vertex(dd34, dd4);
                g.vertex(dd4, dd2);
                g.vertex(dd34, dd2);
                break;
            default:
                assert(false);// && "Invalid die");
                break;
        }
        g.drawPoints();
        g.setPointSize(oldDotSize);
    }
}
