package cc.android.thomas

import android.os.Build
import android.view.View
import androidx.viewpager.widget.ViewPager
import cc.lib.game.Utils

class DepthPageTransformer @JvmOverloads constructor(private val sizeScale: Float = .75f, private val alphaScale: Float = .85f) : ViewPager.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        val pageWidth = view.width
        val pageHeight = view.height

        // [0-1] -> [1-.75]
        val scale = 1f - Math.abs(position) * (1f - sizeScale)
        if (Build.VERSION.SDK_INT >= 21) view.elevation = -Math.abs(position)

        //if (position < -1 || position > 1) { // [-Infinity,-1)
        // This page is way off-screen to the left.
        //    view.setAlpha(0);
        //} else
        if (position < 0) {
            view.alpha = Utils.clamp(alphaScale * (1f + position), 0f, 1f)
            view.scaleX = scale
            view.scaleY = scale
            view.translationY = -pageHeight * position * sizeScale
            // the view is above current
        } else if (position > 0) {
            view.alpha = Utils.clamp(alphaScale * (1f - position), 0f, 1f)
            view.scaleX = scale
            view.scaleY = scale
            view.translationY = -pageHeight * position * sizeScale

            // the view is below current
        } else { // [-1,0]
            // Use the default slide transition when moving to the left page
            view.alpha = 1f
            view.translationY = 0f
            view.scaleX = 1f
            view.scaleY = 1f
        }
    }
}