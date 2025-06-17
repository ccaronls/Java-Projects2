package cc.game.ftp.android

import android.util.Log
import android.view.MotionEvent
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.game.AImage
import cc.lib.game.GColor
import cc.lib.game.Utils
import kotlin.math.roundToInt

class FTPActivity : DroidActivity() {
	override fun onDraw(g: DroidGraphics) {
		val w = g.viewportWidth
		val h = g.viewportHeight / 2
		when (state) {
			State.COIN_DROP -> {}
			State.FEEDING -> {}
			State.CHOOSE_TYPE -> {
				val counts = intArrayOf(Type.PENNY.num, Type.NICKEL.num, Type.DIME.num, Type.QUARTER.num)
				wanted = Type.entries[Utils.chooseRandomFromSet(*counts)]
				state = State.FEEDING
			}

			State.INIT -> {
				initCoins(w, h)
				state = State.CHOOSE_TYPE
			}
		}
		g.clearScreen(GColor.WHITE)
		g.drawImage(bank, 0, h, w, h)
		wanted?.let {
			val r = it.radius.roundToInt()
			g.drawImage(bubble, 5, h, r * 4 + 20, r * 4 + 20)
			g.drawImage(it.id, 5 + 10 + r, h + r - 20, r * 2, r * 2)
		}
		for (c in coins) {
			val x = c.x - c.type.image.width / 2
			val y = c.y - c.type.image.height / 2
			//g.setColor(g.TRANSPARENT);
			g.drawImage(c.type.id, x, y, c.type.image.width, c.type.image.height)
			//AColor old = g.getColor();
			//g.setColor(g.GREEN);
			//g.drawCircle(c.x, c.y, c.t.radius);
			//g.setColor(old);
		}
		g.drawImage(bankBottom, 0, h, w, h)
		//g.setColor(g.WHITE);
	}

	override fun onInit(g: DroidGraphics) {
		val w = g.viewportWidth
		val h = g.viewportHeight
		Type.PENNY.setImage(g.getImage(R.drawable.penny), R.drawable.penny)
		Type.NICKEL.setImage(g.getImage(R.drawable.nickel), R.drawable.nickel)
		Type.DIME.setImage(g.getImage(R.drawable.dime), R.drawable.dime)
		Type.QUARTER.setImage(g.getImage(R.drawable.quarter), R.drawable.quarter)
		bank = R.drawable.piggybank
		bankBottom = R.drawable.piggybankbottom
		bubble = R.drawable.bubble
		g.ortho(0f, w.toFloat(), 0f, h.toFloat())

//		getRenderer().setTargetFPS(20);
//		getRenderer().setDrawFPS(false);
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		val x = event.x
		val y = event.y
		var action = "?"
		if (state == State.FEEDING) {
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					action = "DOWN"
					pickCoin(event.x, event.y)
				}

				MotionEvent.ACTION_UP -> {
					action = "UP"
					picked = null
				}

				MotionEvent.ACTION_MOVE -> {
					action = "MOVE"
					picked?.let {
						it.x = event.x
						it.y = event.y
						processCoinMove(it)
					} ?: run {
						pickCoin(event.x, event.y)
					}
				}

				else -> Log.w(Companion.TAG, "Unhanded action " + event.action)
			}
		}
		lastX = event.x
		lastY = event.y
		Log.d("FTP", "x=$x y=$y action=$action picked=$picked")
		return super.onTouchEvent(event)
	}

	enum class State {
		INIT,
		COIN_DROP,
		CHOOSE_TYPE,
		FEEDING
	}

	enum class Type {
		PENNY,
		NICKEL,
		DIME,
		QUARTER;

		fun setImage(i: AImage, id: Int) {
			image = i
			radius = i.height.coerceAtMost(i.width) / 2
			this.id = id
		}

		var radius = 0f
		lateinit var image: AImage
		var id = 0
		var num = 0
	}

	class Coin(val type: Type, var x: Float, var y: Float) {

		init {
			type.num++
		}

		override fun toString(): String {
			return type.name + " x=" + x + " y=" + y
		}
	}

	private fun initCoins(w: Int, h: Int) {
		val num = Utils.rand() % 10 + 10
		for (i in 0 until num) {
			val c = Coin(
				Type.entries.random(),
				(10 + Utils.rand() % (w - 20)).toFloat(),
				(10 + Utils.rand() % (h - 10)).toFloat()
			)
			coins.add(c)
		}
	}

	private fun pickCoin(tx: Float, ty: Float) {
		picked = null
		for (c in coins) {
			if (Utils.fastLen(tx - c.x + c.type.radius, ty - c.y + c.type.radius) < c.type.radius * 2) {
				picked = c
			}
		}
	}

	private fun processCoinMove(c: Coin) {
		val h = graphics.viewportHeight / 2
		val x0 = graphics.viewportWidth * SLOT_LEFT
		val x1 = graphics.viewportWidth * SLOT_RIGHT
		val y0 = h + h * SLOT_LEFT_BOTTOM
		val y1 = h + h * SLOT_RIGHT_BOTTOM
		if (c.x < x0) {
			if (c.y + c.type.radius > y0) {
				c.y = y0 - c.type.radius
			}
		} else if (c.x < x1) {
			val r2 = c.type.radius * c.type.radius
			val d2 = Utils.distSqPointLine(c.x, c.y, x0, y0, x1, y1)
			if (d2 < r2 || c.y > y0) {
				if (c.type == wanted) {
					c.type.num--
					coins.remove(c)
					picked = null
					wanted = null
					state = if (coins.size == 0) {
						State.INIT
					} else {
						State.CHOOSE_TYPE
					}
				} else {
					picked = null
					c.y = h.toFloat()
				}
			}
		} else {
			if (c.y + c.type.radius > y1) {
				c.y = y1 - c.type.radius
			}
		}
	}

	var bank = 0
	var bankBottom = 0
	var bubble = 0
	var state = State.INIT
	var coins = ArrayList<Coin>()
	var picked: Coin? = null
	var lastX = 0f
	var lastY = 0f
	var wanted: Type? = null

	companion object {
		const val TAG = "FTP"
		const val SLOT_LEFT_BOTTOM = 50f / 128
		const val SLOT_RIGHT_BOTTOM = 37f / 128
		const val SLOT_LEFT = 40f / 128
		const val SLOT_RIGHT = 90f / 128
	}
}
