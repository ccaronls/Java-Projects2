package cc.android.pacboy

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder

class PacBoyView : GLSurfaceView {
	private var pb: PacBoyRenderer? = null
	private fun initView() {
		if (!isInEditMode) {
			setRenderer(PacBoyRenderer(this).also { pb = it })
			if (BuildConfig.DEBUG) debugFlags = DEBUG_CHECK_GL_ERROR // | DEBUG_LOG_GL_CALLS);
			renderMode = RENDERMODE_WHEN_DIRTY
			setOnTouchListener(pb)
		}
	}

	constructor(context: Context?) : super(context) {
		initView()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		initView()
	}

	override fun surfaceDestroyed(holder: SurfaceHolder) {
		if (pb != null) {
			pb!!.shutDown()
			pb = null
		}
	}

	fun setPaused(paused: Boolean) {
		pb!!.isPaused = paused
	}

	fun initMaze(width: Int, height: Int, difficulty: Int) {
		pb!!.newMaze(width, height, difficulty)
	}

	fun initIntro() {
		pb!!.setupIntro()
	}

	val score: Int
		get() = pb!!.score

	override fun performClick(): Boolean {
		return super.performClick()
	}

	val difficulty: Int
		get() = pb!!.difficulty
}
