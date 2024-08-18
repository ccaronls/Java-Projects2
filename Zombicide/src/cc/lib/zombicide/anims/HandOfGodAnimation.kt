package cc.lib.zombicide.anims

import cc.lib.game.AGraphics
import cc.lib.game.LightningStrand
import cc.lib.zombicide.ZActor
import cc.lib.zombicide.ZActorAnimation
import cc.lib.zombicide.ZSpawnArea

/**
 * Created by Chris Caron on 12/15/22.
 *
 * For fist 1 sec start a strand from sky to each corner of the spawn area.
 * For last second scale down the spawn area to closed
 */
open class HandOfGodAnimation(actor: ZActor, val spawn: ZSpawnArea) : ZActorAnimation(actor, 500, 500, 500, 500, 1500, 800) {

	companion object {
		val minSec = 10
		val maxSec = 15
		val excite = .7f
		val bend = 0f
	}

	val start = spawn.getRect().center.toMutable().setY(0f)
	val spawnRect = spawn.getRect()
	val interps = arrayOf(
		start.linearInterpolateTo(spawnRect.topLeft),
		start.linearInterpolateTo(spawnRect.bottomRight),
		start.linearInterpolateTo(spawnRect.topRight),
		start.linearInterpolateTo(spawnRect.bottomLeft),
		start.linearInterpolateTo(spawnRect.center)
	)

	override fun drawPhase(g: AGraphics, positionInPhase: Float, positionInAnimation: Float, phase: Int) {

		val spawnScale : Float = if (phase == 4) (1f - positionInPhase) else 1f
		val dir = spawn.dir
		val id = spawn.icon.imageIds[dir.ordinal]
		val scaledRect = spawn.getRect().scaledBy(spawnScale)
		if (phase < 5)
			g.drawImage(id, scaledRect)

		g.setLineWidth(1f)
		when (phase) {
			0 -> LightningStrand.drawLightning(g, start, interps[0].getAtPosition(positionInPhase), minSec, maxSec, excite, bend)
			1 -> {
				LightningStrand.drawLightning(g, start, interps[0].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[1].getAtPosition(positionInPhase), minSec, maxSec, excite, bend)
			}
			2 -> {
				LightningStrand.drawLightning(g, start, interps[0].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[1].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[2].getAtPosition(positionInPhase), minSec, maxSec, excite, bend)
			}
			3 -> {
				LightningStrand.drawLightning(g, start, interps[0].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[1].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[2].getAtPosition(1f), minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, interps[3].getAtPosition(positionInPhase), minSec, maxSec, excite, bend)
			}
			4 -> {
				LightningStrand.drawLightning(g, start, scaledRect.topRight, minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, scaledRect.topLeft, minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, scaledRect.bottomRight, minSec, maxSec, excite, bend)
				LightningStrand.drawLightning(g, start, scaledRect.bottomLeft, minSec, maxSec, excite, bend)
			}
			5 -> {
				for (i in 0..3)
					LightningStrand.drawLightning(g, start, interps[4].getAtPosition(1f-positionInPhase), minSec, maxSec, excite, bend)
			}
		}
	}
}