package cc.android.game.superrobotron

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import cc.android.game.superrobotron.databinding.RoboviewBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.LayoutFactory
import cc.lib.game.Utils
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 5/31/22.
 */
class SuperRobotronActivity : CCActivityBase(), View.OnTouchListener {

	// ---------------------------------------------------------//
	// ANDROID
	// ---------------------------------------------------------//
	lateinit var binding: RoboviewBinding
	lateinit var roboRenderer: RoboRenderer

	override fun getLayoutFactory(): LayoutFactory {
		return LayoutFactory(this, R.layout.roboview, null)
	}

	override fun onLayoutCreated(binding: ViewDataBinding, viewModel: ViewModel?) {
		this.binding = binding as RoboviewBinding
		setContentView(binding.root)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding.roboLayout.setOnTouchListener(this)
		roboRenderer = RoboRenderer(binding.roboView1)
		binding.roboView1.setRenderer(roboRenderer)
		hideNavigationBar()
	}

	override fun onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy()
	}

	override fun onPause() {
		super.onPause()
	}

	override fun onResume() {
		super.onResume()
	}

	override fun onTouch(v: View, event: MotionEvent): Boolean {
		val action = event.action and MotionEvent.ACTION_MASK
		//int pIndex = (event.getActionIndex() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		//Log.d("DPadView", "viewId=" + view.getId() + ", aIndex=" + event.getActionIndex() + ", pointerId=" + event.getPointerId(0));

		//Log.d("DPadView", "View=" + view.getId() + ", action=" + action + ", pCount=" + event.getPointerCount() + ", pIndex=" + pIndex);
		//Log.d("DPadView", "" + event.get
		Log.d("onTouch", "action=" + action + ", numPointers=" + event.pointerCount)

		// find center x,y of the pointers

		val dpadLeft = binding.dPadLeft
		val dpadRight = binding.dPadRight

		// each pointer is a touch bu a unique finger
		for (i in 0 until event.pointerCount) {
			//Log.d("Pointer " + i, "id=" + event.getPointerId(i) + ", x=" + event.getX(i) + ", y=" + event.getY(i) );
			val x = event.getX(i)
			val y = event.getY(i)
			if (Utils.isPointInsideRect(x, y, dpadLeft.left.toFloat(), dpadRight.top.toFloat(), dpadLeft.width.toFloat(), dpadLeft.height.toFloat())) {
				dpadLeft.doTouch(event, x - dpadLeft.left, y - dpadLeft.top)
				roboRenderer.robotron.setPlayerMovement(dpadLeft.dx.toInt(), dpadLeft.dy.toInt())
			} else if (Utils.isPointInsideRect(x, y, dpadRight.left.toFloat(), dpadRight.top.toFloat(), dpadRight.width.toFloat(), dpadRight.height.toFloat())) {
				dpadRight.doTouch(event, x - dpadRight.left, y - dpadRight.top)
				//if (event.getAction() == MotionEvent.)
				val firing = event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE
				roboRenderer.robotron.setPlayerMissleVector(dpadRight.dx.roundToInt(), dpadRight.dy.roundToInt())
				roboRenderer.robotron.setPlayerFiring(firing)
			} else if (Utils.isPointInsideRect(x, y, binding.roboView1.left.toFloat(), binding.roboView1.top.toFloat(), binding.roboView1.width.toFloat(), binding.roboView1.height.toFloat())) {
				//binding.roboView1.doTouch(event, x - binding.roboView1.left, y - binding.roboView1.top)
			}
		}
		return true
	}
}