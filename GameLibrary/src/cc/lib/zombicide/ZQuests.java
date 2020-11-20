package cc.lib.zombicide;

import cc.lib.zombicide.quests.ZQuestBigGameHunting;
import cc.lib.zombicide.quests.ZQuestTheBlackBook;
import cc.lib.zombicide.quests.ZQuestTutorial;

public enum ZQuests {
    Tutorial,
    BigGameHunting,
    TheBlackBook,
    ;

    final ZQuest load() {
        switch (this) {
            case Tutorial:
                return new ZQuestTutorial();
            case BigGameHunting:
                return new ZQuestBigGameHunting();
            case TheBlackBook:
                return new ZQuestTheBlackBook();
        }
        return null;
    }
}
