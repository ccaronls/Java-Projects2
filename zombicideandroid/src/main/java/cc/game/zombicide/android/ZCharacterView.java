package cc.game.zombicide.android;

import android.content.Context;
import android.util.AttributeSet;

import cc.lib.android.DroidGraphics;
import cc.lib.android.UIComponentView;

public class ZCharacterView extends UIComponentView {

    public ZCharacterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void preDrawInit(DroidGraphics g) {
        super.preDrawInit(g);
        g.setTextHeight(18);
    }
}
