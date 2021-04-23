package cc.android.test.circle;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.GColor;

public class CircleActivity extends DroidActivity {

    @Override
    protected void onDraw(DroidGraphics g) {
        g.ortho(0,1,0,1);
        g.setLineThicknessModePixels(false);
        g.clearScreen(GColor.CYAN);
        g.setIdentity();
        g.setColor(GColor.RED);
        g.setLineWidth(10);
        //g.drawCircle(g.getViewportWidth()/2, g.getViewportHeight()/2, g.getViewportWidth()/3);
        g.drawCircle(.5f, .5f, .25f);
        g.drawRect(0.25f, 0.25f, .5f, .5f);
    }
}
