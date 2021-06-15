package cc.game.zombicide.android;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.lib.utils.Reflector;
import cc.lib.zombicide.ZDiffuculty;
import cc.lib.zombicide.ZQuests;

public class Stats extends Reflector<Stats> {

    static {
        addAllFields(Stats.class);
    }

    private final Map<ZQuests, ZDiffuculty> completedQuests = new HashMap<>();

    public void completeQuest(ZQuests quest, ZDiffuculty difficulty) {
        completedQuests.put(quest, difficulty);
    }

    public boolean isQuestCompleted(ZQuests q, ZDiffuculty minDifficulty) {
        if (completedQuests.containsKey(q)) {
            return completedQuests.get(q).ordinal() >= minDifficulty.ordinal();
        }
        return false;
    }

    public Set<ZQuests> getCompletedQuests() {
        return new HashSet<>(completedQuests.keySet());
    }
}
