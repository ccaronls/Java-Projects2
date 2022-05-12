package cc.android.test.lightning;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;

public class LightningTestActivity extends DroidActivity {

    Vector2D [] click = new Vector2D[2];
    GRectangle rect = null;
    int numClicks = 0;

    AAnimation<AGraphics> a = null;

    @Override
    protected void onDraw(DroidGraphics g) {

        g.ortho();
        g.clearScreen(GColor.DARK_OLIVE);

        Justify h = Justify.RIGHT;
        Justify v = Justify.BOTTOM;
        for (int i=0; i<numClicks; i++) {
            g.setColor(GColor.RED);
            g.drawFilledCircle(click[i], 5);
            g.setColor(GColor.WHITE);
            g.drawJustifiedString(click[i], h, v, String.format("%3.1f,%3.1f", click[i].getX(), click[i].getY()));
            h = Justify.LEFT;
            v = Justify.TOP;
        }

        g.setColor(GColor.BLUE);
        if (rect != null) {
            g.drawRect(rect);
        }

        if (a != null) {
            a.update(g);
            redraw();
            if (a.isDone()) {
                a = null;
                rect = null;
                numClicks = 0;
            }
        }

    }

    @Override
    protected void onTap(float x, float y) {
        if (numClicks < 2)
            click[numClicks++] = new Vector2D(x, y);
        if (numClicks == 2 && a == null) {
            //a = new LightningAnimation(click[0], click[1], 5, 3).start();
            //a = new MagicAnimation(click[0], click[1], 5, 10).start();
            rect = new GRectangle(click[0], click[1]);
            //a = new ElectrocutionAnimation(null, rect).start();
        }
    }
}
