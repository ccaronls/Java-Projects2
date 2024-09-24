package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.GRectangle
import cc.lib.math.MutableVector2D
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZIcon

open class DeathStrikeAnimation(actor: ZActor, targetRects: List<GRectangle>) : ZActorAnimation(actor, 1L) {
	class Phase(val id: Int, val dur: Long, vararg rects: GRectangle) {
		val rects = arrayOf(*rects)
	}

	var phases: MutableList<Phase> = ArrayList()
	fun drawPhase0(g: AGraphics, position: Float, vararg rects: GRectangle) {
		val id = ZIcon.SKULL.imageIds.random()
		val img = g.getImage(id)
		g.setTransparencyFilter(position)
		g.drawImage(id, rects[0].fit(img))
        g.removeFilter()
    }

    fun drawPhase1(g: AGraphics, position: Float, vararg rects: GRectangle) {
        val id = ZIcon.SKULL.imageIds.random()
        val img = g.getImage(id)
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img))
    }

    fun drawPhase2(g: AGraphics, position: Float, vararg rects: GRectangle) {
        val id = ZIcon.SKULL.imageIds.random()
        val img = g.getImage(id)
        g.pushMatrix()
        val rect = rects[0].shaked(0.1f, 0f)
        g.drawImage(id, rect.fit(img))
        g.popMatrix()
    }

    fun drawPhase3(g: AGraphics, position: Float, vararg rects: GRectangle) {
        val id = ZIcon.SKULL.imageIds.random()
        val img = g.getImage(id)
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img))
        g.removeFilter()
    }

    fun drawPhase4(g: AGraphics, position: Float, vararg rects: GRectangle) {
        val id = ZIcon.SKULL.imageIds.random()
        val img = g.getImage(id)
        g.setTransparencyFilter(1f - position)
        g.drawImage(id, rects[0].getInterpolationTo(rects[1], position).fit(img))
        g.removeFilter()
    }

    override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {
        val entry = phases[phase]
        when (entry.id) {
            0 -> drawPhase0(g, positionInPhase, *entry.rects)
            1 -> drawPhase1(g, positionInPhase, *entry.rects)
            2 -> drawPhase2(g, positionInPhase, *entry.rects)
            3 -> drawPhase3(g, positionInPhase, *entry.rects)
            4 -> drawPhase4(g, positionInPhase, *entry.rects)
        }
    }

    override fun hidesActor(): Boolean {
        return false
    }

    companion object {
        var phaseFadeInDur: Long = 800 // 0
        var phaseDropDur: Long = 200 // 1
        var phaseShakeDur: Long = 200 // 2
        var phaseRiseDur: Long = 300 // 3
        var phaseFadeOutDur: Long = 400 // 4
    }

    init {
	    val mid = MutableVector2D()
	    targetRects.forEach {
		    mid.addEq(it.center)
	    }
	    mid.scaleEq(1f / targetRects.size)
	    val targetRect = targetRects[0].withCenter(mid)
	    val startRect = targetRect.movedBy(0f, -targetRect.height)
	    val endRect = targetRect.movedBy(0f, -targetRect.height / 2)
	    phases.add(Phase(0, phaseFadeInDur, startRect))
	    var target = endRect
	    var start = startRect
	    targetRects.forEachIndexed { index, target ->
//		    target = targetRect.movedBy((targetRect.width / 2).randomSigned(), 0f)
		    phases.add(Phase(1, phaseDropDur, start, target))
		    start = endRect
		    phases.add(Phase(2, phaseShakeDur, target))
		    if (index < targetRects.size - 1) {
			    phases.add(Phase(3, phaseRiseDur, target, endRect))
		    }
	    }
	    phases.add(Phase(4, phaseFadeOutDur, target, endRect))
	    setDurations(phases.map { it.dur })
    }
}
