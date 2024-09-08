package cc.game.zombicide.android

import android.content.Intent
import android.os.Bundle
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.game.AAnimation
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.game.GRectangle
import cc.lib.game.Justify
import cc.lib.math.Vector2D

class Splash : DroidActivity() {
	lateinit var animation: AAnimation<AGraphics>
	lateinit var rect: GRectangle
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (BuildConfig.DEBUG) {
			transition()
			return
		}
		animation = object : AAnimation<AGraphics>(3000) {
			override fun draw(g: AGraphics, position: Float, dt: Float) {
				g.clearScreen(GColor.WHITE)
				val cntr = Vector2D((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat())
				val minDim = GDimension((g.viewportWidth / 4).toFloat(), (g.viewportHeight / 4).toFloat())
				val maxDim = GDimension((g.viewportWidth / 2).toFloat(), (g.viewportHeight / 2).toFloat())
				rect = GRectangle().withDimension(minDim.interpolateTo(maxDim, position)).withCenter(cntr)
				//g.setColor(GColor.RED);
				//rect.drawOutlined(g, 5);
				val img = g.getImage(R.drawable.zgravestone)
				g.drawImage(R.drawable.zgravestone, rect.fit(img))
			}

			override fun onDone() {
				animation = object : AAnimation<AGraphics>(2000) {
					override fun draw(g: AGraphics, position: Float, dt: Float) {
						var position = position
						val popupTime = 300f
						if (elapsedTime < popupTime) {
							position = elapsedTime.toFloat() / popupTime
							//g.clearScreen(GColor.WHITE.interpolateTo(GColor.BLACK, position));
						} else {
							position = 1f
							g.color = GColor.WHITE
							val yPos = (g.viewportHeight / 6).toFloat()
							g.setTextHeight(yPos / 2, true)
							g.drawJustifiedString(
								(g.viewportWidth / 2).toFloat(),
								yPos,
								Justify.CENTER,
								Justify.CENTER,
								getString(R.string.app_name)
							)
						}
						//g.clearScreen(GColor.BLACK);
						val handRect = GRectangle(rect)
						handRect.w /= 2f
						handRect.h /= 2f
						handRect.x += handRect.w
						handRect.y += handRect.h
						var img = g.getImage(R.drawable.zgravestone)
						g.drawImage(R.drawable.zgravestone, rect!!.fit(img))
						img = g.getImage(R.drawable.zicon)
						handRect.y += handRect.h * (1f - position)
						handRect.h *= position
						g.drawImage(R.drawable.zicon, handRect.fit(img))
					}

					override fun onDone() {
						transition()
					}
				}.start()
			}
		}.start()
	}

	override fun onDraw(g: DroidGraphics) {
		animation.update(g)
		redraw()
	}

	fun transition() {
		val intent = Intent(this, ZombicideActivity::class.java)
		startActivity(intent)
		finish()
	}
}