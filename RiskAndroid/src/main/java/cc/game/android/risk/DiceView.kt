package cc.game.android.risk

import android.content.Context
import android.util.AttributeSet
import android.view.View
import cc.lib.android.DroidGraphics
import cc.lib.android.DroidView
import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Utils
import cc.lib.utils.Lock

/**
 * Created by Chris Caron on 8/20/21.
 */
class DiceView(context: Context, attrs: AttributeSet?) : DroidView(context, attrs), Runnable, View.OnClickListener {
	var dieNums = intArrayOf(6, 8, 9, 10, 12, 15, 20)
	var finalNum = 6
	var dieNum = 0
	var maxDieNums = 0
	var delay: Long = 0
	var rolling = false
	var dieColor = GColor.WHITE
	var pipColor = GColor.BLACK
	lateinit var lock: Lock
	override fun onPaint(g: DroidGraphics) {
		g.setLineWidth(5f)
		if (rolling) drawDie(g, height.toFloat(), dieNum) else {
			drawDie(g, height.toFloat(), finalNum)
			g.color = GColor.CYAN
			g.drawRoundedRect(0f, 0f, width.toFloat(), height.toFloat(), (width / 4).toFloat())
		}
	}

	fun setColors(dieColor: GColor, pipColor: GColor) {
		this.dieColor = dieColor
		this.pipColor = pipColor
		postInvalidate()
	}

	@Synchronized
	fun rollDice(resultDie: Int, lock: Lock) {
		delay = 10
		rolling = true
		finalNum = resultDie
		this.lock = lock
		lock.acquire()
		run()
	}

	override fun onClick(v: View) {
		delay = 10
		rolling = true
		run()
	}

	@Synchronized
	override fun run() {
		if (rolling) {
			dieNum = Utils.rand() % dieNums[maxDieNums] + 1
			if (delay < 400) {
				delay += 25
				postDelayed(this, delay)
			} else {
				rolling = false
				lock.release()
			}
		}
		postInvalidate()
	}

	fun addPips() {
		if (maxDieNums < dieNums.size - 1) {
			maxDieNums++
			dieNum = dieNums[maxDieNums]
			postInvalidate()
		}
	}

	fun removePips() {
		if (maxDieNums > 0) {
			maxDieNums--
			dieNum = dieNums[maxDieNums]
			postInvalidate()
		}
	}

	fun drawDie(g: AGraphics, dim: Float, pips: Int) {
		g.color = dieColor
		val arc = dim / 4
		g.drawFilledRoundedRect(0f, 0f, dim, dim, arc)
		g.color = pipColor
		val dd2 = dim / 2
		val dd4 = dim / 4
		val dd34 = dim * 3 / 4
		val dotSize = dim / 8
		val oldDotSize = g.setPointSize(dotSize)
		if (dieNums[maxDieNums] > 9) {
			g.textHeight = dim / 2
			g.drawJustifiedString(dd2, dd2, Justify.CENTER, Justify.CENTER, pips.toString())
			return
		}
		g.begin()
		when (pips) {
			1 -> g.vertex(dd2, dd2)
			2 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
			}
			3 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd34, dd34)
			}
			4 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
			}
			5 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd2, dd2)
			}
			6 -> {
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			7 -> {
				g.vertex(dd2, dd2)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			8 -> {
				g.vertex(dd2, dd4)
				g.vertex(dd2, dd34)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			9 -> {
				g.vertex(dd2, dd4)
				g.vertex(dd2, dd2)
				g.vertex(dd2, dd34)
				g.vertex(dd4, dd4)
				g.vertex(dd34, dd34)
				g.vertex(dd4, dd34)
				g.vertex(dd34, dd4)
				g.vertex(dd4, dd2)
				g.vertex(dd34, dd2)
			}
			else -> {
				g.drawJustifiedString(dd2, dd2, Justify.CENTER, Justify.CENTER, pips.toString())
				return
			}
		}
		g.drawPoints()
		g.setPointSize(oldDotSize)
	}

	init {
		val arr = context.obtainStyledAttributes(attrs, R.styleable.DiceView)
		dieColor = GColor.fromARGB(arr.getColor(R.styleable.DiceView_dieColor, dieColor.toARGB()))
		pipColor = GColor.fromARGB(arr.getColor(R.styleable.DiceView_pipColor, pipColor.toARGB()))
		arr.recycle()
	}
}