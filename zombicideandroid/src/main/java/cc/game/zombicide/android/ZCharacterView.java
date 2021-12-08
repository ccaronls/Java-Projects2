package cc.game.zombicide.android;

import android.content.Context;
import android.util.AttributeSet;

import cc.lib.android.DroidGraphics;
import cc.lib.android.UIComponentView;
import cc.lib.game.GColor;
import cc.lib.zombicide.ui.UIZCharacterRenderer;

public class ZCharacterView extends UIComponentView<UIZCharacterRenderer> {

    public ZCharacterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void preDrawInit(DroidGraphics g) {
        super.preDrawInit(g);
        g.setTextModePixels(true);
        g.setTextHeight(getResources().getDimension(R.dimen.chars_view_text_size));
        getRenderer().setTextColor(new GColor(getResources().getColor(R.color.text_color)));
//        UIZCharacterRenderer.TEXT_COLOR_DIM = new GColor(getResources().getColor(R.color.text_color_dim));
    }
}
