package cc.android.game.superrobotron

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import cc.android.game.superrobotron.databinding.RoboviewBinding
import cc.lib.android.CCActivityBase
import cc.lib.android.DPadView
import cc.lib.android.DPadView.OnDpadListener
import cc.lib.android.LayoutFactory
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 5/31/22.
 */
class SuperRobotronActivity : CCActivityBase() {

	// ---------------------------------------------------------//
	// ANDROID
	// ---------------------------------------------------------//
	lateinit var binding: RoboviewBinding
	lateinit var roboRenderer: RoboRenderer

	override fun getLayoutFactory() = LayoutFactory(this, R.layout.roboview, null)

	override fun onLayoutCreated(binding: ViewDataBinding, viewModel: ViewModel?) {
		this.binding = binding as RoboviewBinding
		setContentView(binding.root)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		roboRenderer = RoboRenderer(binding.roboView1)
		binding.roboView1.setRenderer(roboRenderer)
		binding.dPadLeft.setOnDpadListener(object : OnDpadListener {

			override fun dpadPressed(view: DPadView, dirFlag: Int) {
				roboRenderer.robotron.setPlayerMovement(
					DPadView.PadDir.toDx(dirFlag), DPadView.PadDir.toDy(dirFlag)
				)
			}

			override fun dpadReleased(view: DPadView) {
				roboRenderer.robotron.setPlayerMovement(0, 0)
			}
		})
		binding.dPadRight.setOnDpadListener(object : OnDpadListener {
			override fun dpadMoved(view: DPadView, dx: Float, dy: Float) {
				roboRenderer.robotron.setPlayerMissleVector(dx.roundToInt(), dy.roundToInt())
				roboRenderer.robotron.setPlayerFiring(true)
			}

			override fun dpadReleased(view: DPadView) {
				roboRenderer.robotron.setPlayerFiring(false)
			}
		})
		binding.homeButton.setOnClickListener {
			roboRenderer.robotron.setGameStateIntro()
		}
		binding.pauseButton.setOnClickListener {
			with(binding.roboView1) {
				paused = !paused
				binding.pauseButton.text = if (paused) "RESUME" else "PAUSE"
				invalidate()
			}
		}

		hideNavigationBar()
	}
}