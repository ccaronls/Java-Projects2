package cc.game.geniussquares.android

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.geniussqaure.UIGeniusSquares
import java.io.File

/**
 * Created by chriscaron on 2/15/18.
 */
class GeniusSquaresActivity : DroidActivity() {
	val gs: UIGeniusSquares = object : UIGeniusSquares() {
		override fun repaint() {
			redraw()
		}
	}
	lateinit var saveFile: File
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		saveFile = File(filesDir, "gs.save")
		content.setBackgroundColor(Color.GRAY)
		val topBar = View.inflate(this, R.layout.menu_bar, null)
		getTopBar().addView(topBar)
		findViewById<View>(R.id.buttonMenu).setOnClickListener {
			val options = arrayOf(
				"New Game",
				"Reset Pieces")
			gs.pauseTimer()
			newDialogBuilder().setTitle("Options").setItems(options) { _, which: Int ->
				when (which) {
					0 -> gs.newGame()
					1 -> gs.resetPieces()
				}
				gs.resumeTimer()
			}.setNegativeButton("Cancel", null).show()
		}
	}

	override fun onResume() {
		super.onResume()
		if (!gs.tryLoadFromFile(saveFile)) gs.newGame()
		gs.resumeTimer()
	}

	override fun onPause() {
		super.onPause()
		gs.pauseTimer()
		gs.trySaveToFile(saveFile)
	}

	var tx = -1
	var ty = -1
	var dragging = false
	override fun onDraw(g: DroidGraphics) {
		g.setTextModePixels(true)
		g.paint.isAntiAlias = false
		gs.paint(g, tx, ty)
		content.postInvalidateDelayed(500)
	}

	override fun onTouchDown(x: Float, y: Float) {
		Log.i(TAG, "onTouchDown")
		tx = Math.round(x)
		ty = Math.round(y)
		redraw()
	}

	override fun onTouchUp(x: Float, y: Float) {
		Log.i(TAG, "onTouchUp")
		if (dragging) {
			gs.stopDrag()
			dragging = false
		}
		tx = -1 //Math.round(x);
		ty = -1 //Math.round(y);
		redraw()
	}

	override fun onDrag(x: Float, y: Float) {
		Log.i(TAG, "onDrag")
		if (!dragging) {
			gs.startDrag()
			dragging = true
		}
		tx = Math.round(x)
		ty = Math.round(y)
		redraw()
	}

	override fun onTap(x: Float, y: Float) {
		Log.i(TAG, "onTap")
		tx = Math.round(x)
		ty = Math.round(y)
		content.invalidate()
		content.postDelayed({
			ty = -1
			tx = ty
			gs.doClick()
		}, 1)
	}

	companion object {
		private val TAG = GeniusSquaresActivity::class.java.simpleName
	}
}