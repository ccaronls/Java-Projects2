package cc.lib.zombicide;

import cc.lib.zombicide.quests.ZQuestTutorial;

public enum ZQuests {
    Tutorial
    ;

    ZQuest load() {
        switch (this) {
            case Tutorial:
                return new ZQuestTutorial();
        }
        return null;
    }
}