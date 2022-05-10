package cc.game.zombicide.android

import cc.lib.utils.Reflector
import cc.lib.zombicide.ZQuests
import cc.lib.zombicide.ZDifficulty
import java.util.HashMap
import java.util.HashSet

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
		return if (completedQuests.containsKey(q)) {
			completedQuests[q]!!.ordinal >= minDifficulty.ordinal
		} else false
	}

	fun getCompletedQuests(): Set<ZQuests> {
		return HashSet(completedQuests.keys)
	}
}