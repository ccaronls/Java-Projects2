package cc.game.android.risk;

import cc.lib.game.AGraphics;
import cc.lib.game.ChainInterpolator;
import cc.lib.game.GColor;
import cc.lib.game.IInterpolator;
import cc.lib.game.InterpolatorUtils;
import cc.lib.game.Justify;

/**
 * Created by Chris Caron on 9/22/21.
 */
class ExpandingTextOverlayAnimation extends RiskAnim {

    final String text;
    final IInterpolator<GColor> colorInterp;
    final IInterpolator<Float> textSizeInterp;

    public ExpandingTextOverlayAnimation(String text, GColor color) {
        super(2000);
        this.text = text;
        GColor color2 = color.withAlpha(0);
        colorInterp = new ChainInterpolator<>(color2.getInterpolator(color),
                color.getInterpolator(color2));
        textSizeInterp = InterpolatorUtils.linear(
                RiskActivity.instance.getResources().getDimension(R.dimen.text_height_overlay_sm),
                RiskActivity.instance.getResources().getDimension(R.dimen.text_height_overlay_lg));
    }

    @Override
    protected void draw(AGraphics g, float position, float dt) {
        g.setColor(colorInterp.getAtPosition(position));
        float old = g.setTextHeight(textSizeInterp.getAtPosition(position));
        g.drawJustifiedString(g.getViewport().getCenter(), Justify.CENTER, Justify.CENTER, text);
        g.setTextHeight(old);
    }
}
