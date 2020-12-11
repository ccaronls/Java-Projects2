package cc.lib.zombicide;

import cc.lib.zombicide.quests.ZQuestBigGameHunting;
import cc.lib.zombicide.quests.ZQuestDeadTrail;
import cc.lib.zombicide.quests.ZQuestFamine;
import cc.lib.zombicide.quests.ZQuestInCaligineAbditus;
import cc.lib.zombicide.quests.ZQuestTheAbomination;
import cc.lib.zombicide.quests.ZQuestTheBlackBook;
import cc.lib.zombicide.quests.ZQuestTheCommandry;
import cc.lib.zombicide.quests.ZQuestTheEvilTemple;
import cc.lib.zombicide.quests.ZQuestTheShepherds;
import cc.lib.zombicide.quests.ZQuestTutorial;

public enum ZQuests {
    Tutorial,
    Big_Game_Hunting,
    The_Black_Book,
    The_Abomination,
    The_Shepherds,
    Famine,
    The_Commandry,
    In_Caligine_Abditus,
    Dead_Trail,
    The_Evil_Temple,
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
            case The_Shepherds:
                return new ZQuestTheShepherds();
            case Famine:
                return new ZQuestFamine();
            case The_Commandry:
                return new ZQuestTheCommandry();
            case In_Caligine_Abditus:
                return new ZQuestInCaligineAbditus();
            case Dead_Trail:
                return new ZQuestDeadTrail();
            case The_Evil_Temple:
                return new ZQuestTheEvilTemple();
        }
        assert(false);
        return null;
    }
}
