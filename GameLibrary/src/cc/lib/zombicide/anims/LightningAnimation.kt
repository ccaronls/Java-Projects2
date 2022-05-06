package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.math.Vector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZBoard
import java.util.*

class LightningAnimation(actor: ZActor<*>, start: Vector2D, end: Vector2D, sections: Int, strands: Int) : ZActorAnimation(actor, 150, 3) {
	var dv: Vector2D
	val mag: Float
	val start: Vector2D
	val end: Vector2D
	val sections : Array<LinkedList<Vector2D>>
	val sectionLen: Float
	val numSections: Int

	constructor(actor: ZActor<*>, board: ZBoard, targetZone: Int, strands: Int) : this(actor, actor.rect.center, board.getZone(targetZone).center.add(Vector2D.newRandom(.3f)), 4, strands) {}

	override fun onRepeat(n: Int) {
		for (l in sections) {
			l.clear()
			l.add(start)
		}
	}

	override fun draw(g: AGraphics, position: Float, dt: Float) {
		val randLenFactor = .8f
		val randAngFactor = 30f
		if (position <= .52f) {
			val sec = Utils.clamp(Math.round(position * 2 * (numSections + 1)) + 1, 1, numSections + 1)
			for (l in sections) {
				while (sec > l.size) {
					val m = sectionLen * (l.size + 1)
					val n = l.first.add(dv.scaledBy(randLenFactor * m))
					//n.addEq(Vector2D.newRandom(sectionLen / (maxRandomFactor/sec)));
					n.addEq(dv.rotate(Utils.randFloatX(randAngFactor)).scaledBy((1f - randLenFactor) * m))
					l.add(n)
				}
			}
		} else {
			for (l in sections) {
				var sec = numSections + 1 - Math.round((position - .5f) * 2 * (numSections + 1))
				if (sec < 1) sec = 1
				while (sec < l.size) {
					l.removeFirst()
				}
			}
		}
		g.color = GColor.WHITE
		g.setLineWidth(2f)
		for (l in sections) {
			g.begin()
			for (v in l) {
				g.vertex(v)
			}
			g.drawLineStrip()
			g.end()
		}
	}

	override fun hidesActor(): Boolean {
		return false
	}

	init {
		dv = end.sub(start)
		mag = dv.mag()
		dv = dv.scaledBy(1.0f / mag)
		this.start = start
		this.end = end
		numSections = sections
		sectionLen = mag / (numSections + 1)
		this.sections = Array(strands) {
			LinkedList<Vector2D>()
		}
		onRepeat(0)
	}
}