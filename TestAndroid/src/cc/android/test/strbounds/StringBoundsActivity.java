package cc.android.test.strbounds;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;

public class StringBoundsActivity extends DroidActivity {

    Vector2D [] click = new Vector2D[2];
    GRectangle rect = null;
    int numClicks = 0;

    AAnimation<AGraphics> a = null;

    @Override
    protected void onDraw(DroidGraphics g) {

        g.ortho(-1, 1, 1, -1);

        float HEIGHT = 50;

        g.setTextHeight(HEIGHT);
        g.setColor(GColor.RED);
        g.drawLine(-1, 0, 1, 0);
        g.drawLine(-.7f, -1, -.7f, 1);
        g.drawLine(.7f, -1, .7f, 1);

        g.setColor(GColor.RED);
        GDimension rect = g.drawJustifiedString(-.7f, 0, Justify.LEFT, Justify.TOP, "Aqy");
        g.setColor(GColor.WHITE);
        rect.drawOutlined(g, 1);

        g.setColor(GColor.RED);
        rect = g.drawJustifiedString(0, 0, Justify.CENTER, Justify.CENTER, "Aqy\nAqy");
        g.setColor(GColor.WHITE);
        rect.drawOutlined(g, 1);

        g.setColor(GColor.RED);
        rect = g.drawJustifiedString(.7f, 0, Justify.RIGHT, Justify.BOTTOM, "Aqy\nAqy\nAqy");
        g.setColor(GColor.WHITE);
        rect.drawOutlined(g, 1);

        g.ortho();

        float midY= g.getViewportHeight()/2;
        float midX = g.getViewportWidth()/2;
        g.drawLine(midX, midY, midX, midY-HEIGHT);
        g.drawLine(midX, midY, midX, midY+HEIGHT);

    }
}
