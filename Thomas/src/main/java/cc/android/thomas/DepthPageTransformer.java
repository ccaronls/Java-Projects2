package cc.android.thomas;

import android.support.v4.view.ViewPager;
import android.view.View;

import cc.lib.game.Utils;

public class DepthPageTransformer implements ViewPager.PageTransformer {
    private final float sizeScale;
    private final float alphaScale;

    public DepthPageTransformer() {
        this(.75f, .85f);
    }

    public DepthPageTransformer(float scale, float alphaScale) {
        this.sizeScale = scale;
        this.alphaScale = alphaScale;
    }

    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();

        // [0-1] -> [1-.75]
        float scale = 1f - Math.abs(position)*(1f- sizeScale);

        view.setElevation(-Math.abs(position));

        //if (position < -1 || position > 1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
        //    view.setAlpha(0);
        //} else
        if (position < 0) {

            view.setAlpha(Utils.clamp(alphaScale*(1f+position), 0, 1));
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setTranslationY(-pageHeight*position* sizeScale);
            // the view is above current

        } else if (position > 0) {

            view.setAlpha(Utils.clamp(alphaScale*(1f-position), 0, 1));
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setTranslationY(-pageHeight*position*sizeScale);

            // the view is below current

        } else { // [-1,0]
            // Use the default slide transition when moving to the left page
            view.setAlpha(1);
            view.setTranslationY(0);
            view.setScaleX(1);
            view.setScaleY(1);
        }

    }
}

