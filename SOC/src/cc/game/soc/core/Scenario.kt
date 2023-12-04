package cc.game.soc.core

import cc.lib.reflector.Reflector

/**
 * Created by chriscaron on 4/2/18.
 */
class Scenario : Reflector<Scenario> {
	companion object {
		init {
			addAllFields(Scenario::class.java)
		}
	}

	constructor() {}
	constructor(soc: SOC, aituing: Map<String, Double>?) {
		mBoard = soc.board
		mRules = soc.rules
		aiTuning = aituing
	}

	//members mirror those in SOC so we can save a scenario and load into an SOC without all the other
	// crap associated with SOC
    @JvmField
    var mBoard: Board? = null
	var mRules: Rules? = null
	@JvmField
    var aiTuning: Map<String, Double>? = null
}