package cc.android.game.superrobotron

import android.content.Context
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import cc.game.superrobotron.Robotron
import cc.lib.android.BaseRenderer
import cc.lib.android.GL10Graphics
import cc.lib.android.UIComponentView
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

class RoboRenderer(component: UIComponent) : UIRenderer(component) {

	val robotron = object : Robotron() {
		override val imageKey: Int
			get() = R.drawable.key
		override val imageLogo: Int
			get() = R.drawable.logo
		override val animJaws: IntArray by lazy {
			G.loadImageCells("jaws.png", 32, 32, 8, 9, true, GColor.BLACK)
		}
		override val animLava: IntArray by lazy {
			G.loadImageCells("lavapit.png", 32, 32, 8, 25, true, GColor.BLACK)
		}
		override val animPeople: Array<IntArray> by lazy {
			arrayOf(
				G.loadImageCells("people.png", 32, 32, 4, 16, true, GColor.BLACK),
				G.loadImageCells("people2.png", 32, 32, 4, 16, true, GColor.BLACK),
				G.loadImageCells("people3.png", 32, 32, 4, 16, true, GColor.BLACK)
			)
		}
	}


	override fun draw(g: APGraphics, px: Int, py: Int) {
		robotron.setDimension(320, 320)
		g.ortho(0f, 320f, 0f, 320f)
		robotron.drawGame(g)
	}
}

class RoboView(context: Context, attrs: AttributeSet) : UIComponentView<RoboRenderer>(context, attrs) {

	override fun onDraw(canvas: Canvas) {
		val FPS = 20
		val time = SystemClock.elapsedRealtime()
		super.onDraw(canvas)
		val dt = SystemClock.elapsedRealtime()-time
		if (dt > 1000/FPS) // 30 FPS
			postInvalidate()
		else
			postInvalidateDelayed(33 - dt)
	}
}