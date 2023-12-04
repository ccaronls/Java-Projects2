package cc.game.zombicide.android

import cc.lib.reflector.Reflector
import cc.lib.zombicide.ZDifficulty
import cc.lib.zombicide.ZQuests

class Stats : Reflector<Stats>() {
	companion object {
		init {
			addAllFields(Stats::class.java)
		}
	}

	private val completedQuests: MutableMap<ZQuests, ZDifficulty> = HashMap()

	fun completeQuest(quest: ZQuests, difficulty: ZDifficulty) {
		completedQuests[quest] = difficulty
	}

	fun isQuestCompleted(q: ZQuests, minDifficulty: ZDifficulty): Boolean {
		return completedQuests[q]?.let {
			it.ordinal >= minDifficulty.ordinal
		}?:false
	}

	fun getCompletedQuests(): Set<ZQuests> {
		return HashSet(completedQuests.keys)
	}
}