package cc.game.android.risk;

import android.util.Log;

import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;

/**
 * Created by Chris Caron on 9/23/21.
 */
abstract class RiskAnim extends AAnimation<AGraphics> {

    int zOrder = 0;

    public RiskAnim(long durationMSecs) {
        super(durationMSecs);
    }

    @Override
    protected void onDone() {
        Log.d("RiskAnim", "onDone");
        super.onDone();
    }

    RiskAnim setZOrder(int order) {
        zOrder = order;
        return this;
    }

    public int getZOrder() {
        return zOrder;
    }
}
