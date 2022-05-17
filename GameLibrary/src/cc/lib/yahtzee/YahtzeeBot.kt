package cc.lib.yahtzee

class YahtzeeBot : Yahtzee() {
	override fun onChooseKeepers(keeprs: BooleanArray): Boolean {

		// This is the hard one.  Need to consider many factors ....
		throw RuntimeException("Not implemented")
	}

	override fun onChooseSlotAssignment(choices: List<YahtzeeSlot>): YahtzeeSlot? {
		var max = -1
		var best: YahtzeeSlot? = null
		for (slot in choices!!) {
			val score = slot.getScore(this)
			if (score > max) {
				max = score
				best = slot
			}
		}
		return best
	}
}