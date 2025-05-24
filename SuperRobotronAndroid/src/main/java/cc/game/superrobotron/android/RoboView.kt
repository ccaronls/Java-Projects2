package cc.game.superrobotron.android

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import cc.game.superrobotron.RobotronRemote
import cc.lib.android.CCActivityBase
import cc.lib.android.DroidGraphics
import cc.lib.android.UIComponentView
import cc.lib.game.AGraphics
import cc.lib.game.APGraphics
import cc.lib.game.GColor
import cc.lib.ui.UIComponent
import cc.lib.ui.UIRenderer

const val SCREEN_WIDTH = 600
const val SCREEN_HEIGHT = 600

class RoboRenderer(component: UIComponent) : UIRenderer(component) {

	val activity = (component as RoboView).context as CCActivityBase

	val robotron = object : RobotronRemote() {
		override val imageKey: Int
			get() = R.drawable.key
		override val imageLogo: Int
			get() = R.drawable.logo
		override val animJaws: IntArray by lazy {
			G.loadImageCells("pngs/jaws.png", 32, 32, 8, 9, true, GColor.BLACK)
		}
		override val animLava: IntArray by lazy {
			G.loadImageCells("pngs/lavapit.png", 32, 32, 8, 25, true, GColor.BLACK)
		}
		override val animPeople: Array<IntArray> by lazy {
			arrayOf(
				G.loadImageCells("pngs/people.png", 32, 32, 4, 16, true, GColor.BLACK),
				G.loadImageCells("pngs/people2.png", 32, 32, 4, 16, true, GColor.BLACK),
				G.loadImageCells("pngs/people3.png", 32, 32, 4, 16, true, GColor.BLACK)
			)
		}
		override val clock: Long
			get() = SystemClock.elapsedRealtime()

		override val instructions = "Use 'D' pads to move and fire"

		override var high_score: Int = 0
			set(value) {
				if (field != value) {
					field = value
					activity.prefs.edit().putInt("high_score", value).apply()
				}
			}

		init {
			high_score = activity.prefs.getInt("high_score", 0)
		}
	}

	override fun draw(g: AGraphics) {
		robotron.drawGame(g)
	}

	override fun updateMouseOrTouch(g: APGraphics, mx: Int, my: Int) {
		val pos = g.screenToViewport(mx, my)
		robotron.setCursor(pos.Xi(), pos.Yi())
	}

	override fun onClick() {
		robotron.setCursorPressed(true)
		robotron.setCursorPressed(false)
	}
}

class RoboView(context: Context, attrs: AttributeSet) : UIComponentView<RoboRenderer>(context, attrs) {

	var paused = false

	override fun preDrawInit(g: DroidGraphics) {
		renderer.robotron.initGraphics(g)
		renderer.robotron.setDimension(SCREEN_WIDTH, SCREEN_HEIGHT)
		g.setTextHeight(16f, false)
		g.setLineThicknessModePixels(false)
		g.setTextStyles(AGraphics.TextStyle.MONOSPACE)
	}

	val targetDelay = 1000 / 20 // 20 is the target FPS

	override fun onDraw(canvas: Canvas) {
		val time = SystemClock.elapsedRealtime()
		super.onDraw(canvas)
		if (paused)
			return
		val dt = SystemClock.elapsedRealtime() - time
		if (dt > targetDelay) // 30 FPS
			postInvalidate()
		else
			postInvalidateDelayed(targetDelay - dt)
	}
}