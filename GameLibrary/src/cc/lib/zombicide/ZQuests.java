package cc.lib.zombicide;

import cc.lib.zombicide.quests.ZQuestBigGameHunting;
import cc.lib.zombicide.quests.ZQuestTheAbomination;
import cc.lib.zombicide.quests.ZQuestTheBlackBook;
import cc.lib.zombicide.quests.ZQuestTutorial;

public enum ZQuests {
    Tutorial,
    Big_Game_Hunting,
    The_Black_Book,
    The_Abomination
    ;

    final ZQuest load() {
        switch (this) {
            case Tutorial:
                return new ZQuestTutorial();
            case Big_Game_Hunting:
                return new ZQuestBigGameHunting();
            case The_Black_Book:
                return new ZQuestTheBlackBook();
            case The_Abomination:
                return new ZQuestTheAbomination();
        }
        return null;
    }
}
