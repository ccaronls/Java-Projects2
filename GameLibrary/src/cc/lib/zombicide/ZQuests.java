package cc.lib.zombicide;

import java.util.List;

import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.zombicide.quests.WolfQuestKnowYourEnemy;
import cc.lib.zombicide.quests.WolfQuestTheAmbush;
import cc.lib.zombicide.quests.WolfQuestTheEvilTwins;
import cc.lib.zombicide.quests.ZQuestBigGameHunting;
import cc.lib.zombicide.quests.ZQuestDeadTrail;
import cc.lib.zombicide.quests.ZQuestFamine;
import cc.lib.zombicide.quests.ZQuestInCaligineAbditus;
import cc.lib.zombicide.quests.ZQuestTheAbomination;
import cc.lib.zombicide.quests.ZQuestTheBlackBook;
import cc.lib.zombicide.quests.ZQuestTheCommandry;
import cc.lib.zombicide.quests.ZQuestTheEvilTemple;
import cc.lib.zombicide.quests.ZQuestTheHellHole;
import cc.lib.zombicide.quests.ZQuestTheNecromancer;
import cc.lib.zombicide.quests.ZQuestTheShepherds;
import cc.lib.zombicide.quests.ZQuestTrialByFire;
import cc.lib.zombicide.quests.ZQuestTutorial;
import cc.lib.zombicide.quests.WolfQuestWelcomeToWulfsburg;
import static cc.lib.zombicide.ZQuestFlags.*;

@Keep
public enum ZQuests {
    Tutorial(FLAG_BLACK_PLAGUE,"TUTORIAL:DANSE MACABRE", "War is nothing new for us. Our counts\n" +
            "and dukes are always fighting amongst\n" +
            "themselves. For the peasantry, it usually just\n" +
            "involves a change in taxes and rents, assuming you\n" +
            "survive. But this time, the duke and his army went\n" +
            "off and were never seen again. Well, not until the\n" +
            "hordes emerged. Pretty sure a lot of the tougher\n" +
            "ones came from his troops. Now everything’s a\n" +
            "brutal mess. Now we’re all equals, facing the danse\n" +
            "macabre together. There’s no time for social\n" +
            "snobbery when the hordes are at your door. We\n" +
            "stand together, and throw death back in their teeth. "),
    The_Abomination(FLAG_DEBUG, "The Abomination", "Test Abomination!"),
    The_Necromancer(FLAG_DEBUG, "The Necromancer", "Test Necromancer!"),
    Big_Game_Hunting(FLAG_BLACK_PLAGUE,"Big Game Hunting","We quickly discovered the starting point\n" +
            "of the zombie invasion. Other survivors\n" +
            "spotted a huge zombie wandering the streets, and some\n" +
            "kind of sick wizard directing the horde to engulf us.\n" +
            "It took us two days to pinpoint the Necromancer’s\n" +
            "location, and understand the Abomination can’t be\n" +
            "killed by any weapon at our disposal. Let’s raid the\n" +
            "Necromancer’s laboratory and take them both out\n" +
            "with a secret brew of our own: Dragon Fire.\n" +
            "Let the hunt begin!"),
    The_Black_Book(FLAG_BLACK_PLAGUE,"The Black Book", "Now we know. It’s not just our village. The\n" +
            "zombie plague has spread across the land.\n" +
            "What’s going on? The Necromancer we killed held\n" +
            "notes in his laboratory, most of them referring to a\n" +
            "mysterious Black Book and other items of power.\n" +
            "Exploring the surroundings could prove useful to\n" +
            "get a better grasp about the threat we’re facing. Of\n" +
            "course, there are zombies on the way, familiar faces\n" +
            "turned to monsters...\n" +
            "Hey, that one owed me money!"),
    The_Shepherds(FLAG_BLACK_PLAGUE,"The Shepherds", "Necromancers are everywhere. They’re\n" +
            "spreading chaos and seizing power in the\n" +
            "whole kingdom! Against a menace this big, there\n" +
            "is almost nothing we could do. Almost. We know\n" +
            "we’re good at survival as long as we stand together.\n" +
            "Our plan is to reach out and find other survivors\n" +
            "to create an army of our own. Four days we’ve spent,\n" +
            "traveling to the next village, which\n" +
            "is currently under attack, but not\n" +
            "completely overrun. Let’s get in\n" +
            "the fray and help these people!"),
    Famine(FLAG_BLACK_PLAGUE, "Famine", "Afew days have passed. These\n" +
            "zombies are, for the most part,\n" +
            "stupid as hell. But they never tire, or\n" +
            "need food, or even sleep. Alas, we’re all\n" +
            "too human. We need food and a secure\n" +
            "shelter.\n" +
            "There are many vaults beneath this\n" +
            "town. Clever survivors could hole up\n" +
            "and rest for a while. But, we still need\n" +
            "to gather supplies to last a couple of days\n" +
            "while we plan our next move. This war\n" +
            "may last far longer than anyone - even\n" +
            "the Necromancers - expected."),
    The_Commandry(FLAG_BLACK_PLAGUE, "The Commandry", "This capital has been taken. People died\n" +
            "by the thousands, but some areas are\n" +
            "still unharmed. The Necromancers seem content\n" +
            "to battle the nobility in the castle, and leave the\n" +
            "commoners corralled for when their zombie hordes\n" +
            "need ready reinforcements.\n" +
            "We need a way in, to establish communications with\n" +
            "the people still alive. It’s also been suggested we\n" +
            "learn more about the infection. The Black Book\n" +
            "says little on it. If we can learn more,\n" +
            "we can plan a bold move to end this.\n" +
            "First we need a way past the city walls.\n" +
            "Some survivors we rescued speak of a\n" +
            "secret passage beneath the commandry\n" +
            "nearby. However, it’s guarded. Clearly\n" +
            "the Necromancers are aware of it. If\n" +
            "we can dispatch the guardians, we can\n" +
            "get inside, and get our plan in motion."),
    In_Caligine_Abditus(FLAG_BLACK_PLAGUE, "In Caligine Abdicus", "We’re now in the city, but not as close to\n" +
            "the Necromancers as we would like. The\n" +
            "area is eerily quiet, and there’s desolation as far as\n" +
            "we can see. That won’t last. As soon as they hear\n" +
            "us, they’ll be rushing to kill us. We must proceed as\n" +
            "stealthily as possible.\n" +
            "Clovis and Baldric both noticed strange Latin\n" +
            "writings on some walls. It seems someone here used\n" +
            "a network of underground passages to get around\n" +
            "town. And Clovis is right when he says\n" +
            "that not everybody knows Latin... Only\n" +
            "the highly educated know this language\n" +
            "on sight, not to mention being able to\n" +
            "write it. It’s probably a Necromancer!\n" +
            "Wait. Clovis knows how to read?"),
    Dead_Trail(FLAG_BLACK_PLAGUE, "Dead Trail", "There is no way we can get any further\n" +
            "unnoticed. That’s good, for my fingers are\n" +
            "itching for some zombie bashing, and I was getting\n" +
            "tired of walking on tiptoe.\n" +
            "There are dark signs and symbols on the walls here.\n" +
            "The necromancers are hanging around. We don’t\n" +
            "know what kind of ritual they are performing, but\n" +
            "we must try to make it fail. Let’s see what happens\n" +
            "if we destroy these wicked scriptures...\n" +
            "Fortunately, this is the foundry\n" +
            "district. These Orc weaponsmiths seem\n" +
            "to know their job pretty well. New toys!"),
    The_Evil_Temple(FLAG_BLACK_PLAGUE, "The Evil Temple", "This is the center of necromantic\n" +
            "power.Cursed idols are everywhere, and\n" +
            "a huge Abomination is locked up in the temple.\n" +
            "Plus, it seems the Necromancers have figured out\n" +
            "how we’re dealing with their biggest beasts. Dragon\n" +
            "Bile is scarce, and our supplies gone. But, there\n" +
            "are vaults here. All the old parts of town had them.\n" +
            "Killing that beast could draw the Necromancers to\n" +
            "us. There may be thousands of zombies, but there\n" +
            "can’t be too many more Necromancers. … Right?"),
    The_Hell_Hole(FLAG_BLACK_PLAGUE,"The Hell Hole", "I think we stumbled upon the place our Duke made\n" +
            "his last stand before the town fell. All who sought\n" +
            "his protection gathered in the temple, under the protection\n" +
            "of the gods, the remaining soldiers and the Duke himself.\n" +
            "It wasn’t enough, unfortunately. After a huge fight,\n" +
            "the zombies killed everyone. And now, this is a hellhole\n" +
            "vomiting zombies. We have no choice but to fight them\n" +
            "and destroy this forsaken place once and for all.\n" +
            "Hey, do I see the Duke? Nothing personal, Your Grace!"),
    Trial_by_Fire(FLAG_BLACK_PLAGUE, "Trial by fire", "We’re in the heart of the city, the place where\n" +
            "all zombies converged. It seems we’re not\n" +
            "the first ones to get here. Heroes or mercenaries\n" +
            "of some sort tried to clean the place before us, and\n" +
            "failed. However, they locked the most impressive\n" +
            "Abomination we’ve seen so far in a nearby magic\n" +
            "school. The beast is trapped and is waiting for someone\n" +
            "– or something – to break its bonds. Its roaring\n" +
            "lures every zombie around like a beacon.\n" +
            "And the Necromancers are still nowhere\n" +
            "to be seen."),
    // Wulfsburg
    Welcome_to_Wulfsberg(FLAG_WOLFBURG, "Welcome to Wulfsburg", "The prosperous city of Wulfsburg earned its\n" +
            "name due to the many wolf packs roaming\n" +
            "the surrounding forests and mountains. Nobles and\n" +
            "merchants built tall towers here, the better to view\n" +
            "the scenic valley (and display their wealth and status).\n" +
            "With the plague’s coming, the wolf packs attacked\n" +
            "wandering zombies, and fell victim to the infection\n" +
            "themselves. Now hungry for living flesh, the wolfz’\n" +
            "made the city their new hunting ground.\n" +
            "Wulfsburg has become a Necromancer outpost,\n" +
            "populated with hidden, terrified survivors. We’re on\n" +
            "our way to liberate the city. Breaching the inner\n" +
            "city will take time, however, and we’ll need supplies.\n" +
            "Fresh food is scarce, but still to be had."),
    Know_Your_Enemy(FLAG_WOLFBURG, "Know your Enemy",
            "Wulfsburg sustained some unusual damage,\n" +
            "as if a civil war had raged inside. In\n" +
            "some place, people were not killed by zombies but by\n" +
            "soldiers. We don’t know yet if survivor groups are\n" +
            "prone to fighting one another here, or if someone\n" +
            "tried to invade the infested city, killing any\n" +
            "survivors they ran across in the process. Exploring\n" +
            "the area could give us a clue.\n" +
            "Come to think of it, Wulfsburg\n" +
            "was known for its elven beer. That\n" +
            "would be a rare treat!"),
    The_Evil_Twins(FLAG_WOLFBURG, "The Evil Twins",
            "We found a soldier’s journal among the\n" +
                    "bloodstained houses. It seems a foreign\n" +
                    "prince had come to Wulfsburg shortly after the\n" +
                    "invasion began with his private army. Even its\n" +
                    "fallen state, the city retains its wealth. Zombies\n" +
                    "aren’t interested in treasure, so he thought it’d be\n" +
                    "an easy conquest.\n" +
                    "The blood-spattered journal ends with an entry\n" +
                    "about a pair of abominations stalking the final few\n" +
                    "survivors from the prince’s retinue. The ‘Evil\n" +
                    "Twins’ they were called, and they seem to haunt\n" +
                    "the Usurer’s Ward, a block away.\n" +
                    "They’ve surely caught our scent now as well, and\n" +
                    "could attack at any time. So, we’ll attack first.\n" +
                    "The best defense is sometimes all-out offense,\n" +
                    "right?"),
    The_Ambush(FLAG_WOLFBURG, "The Ambush",
            "We were returning to our haven as night\n" +
            "began to fall, when the wizard spotted some\n" +
            "esoteric writing on the walls. Before our eyes they\n" +
            "flared to brilliance, and we heard shuffling footsteps!\n" +
            "An ambush! Someone placed zombie lures all around,\n" +
            "and the infected are hot on our trail! We must resist\n" +
            "long enough to destroy the lures and secure our escape.\n" +
            "The Necromancers know we’re here, and consider\n" +
            "us a threat. I don’t know what to think about this\n" +
            "flattering change."),
    Immortal(FLAG_WOLFBURG, "Immortal",
            "Night has fallen, and the\n" +
                    "zombies are still dogging our\n" +
                    "footsteps by the dozen. To make\n" +
                    "matters worse, we seem to have killed\n" +
                    "the same Necromancer at least four\n" +
                    "times. He keeps coming back, over\n" +
                    "and over. He’s rallying his hordes and\n" +
                    "summoning more. We’re experienced\n" +
                    "survivors, but we’re still just mortals.\n" +
                    "We either figure out how to kill him\n" +
                    "permanently, or die from exhaustion.\n" +
                    "The wizards say he likely has some\n" +
                    "kind of magical anchors binding him\n" +
                    "to this area. If we destroy them, we\n" +
                    "can probably kill him for good and take\n" +
                    "a rest. (And then I can enjoy some\n" +
                    "of that Elven brew I found before.\n" +
                    "Don’t tell anyone!)"),
    Zombie_Court(FLAG_WOLFBURG, "Zombie Court", ""),
    Blood_Red(FLAG_WOLFBURG, "Blood Red", ""),
    The_Ghost_Door(FLAG_WOLFBURG, "The Ghost Door", ""),
    The_Zombie_Army(FLAG_WOLFBURG, "The Zombie Army", ""),
    A_Coin_For_The_Ferryman(FLAG_WOLFBURG, "A Coin for the Ferryman", ""),

    ;

    ZQuests(int flag, String displayName, String description) {
        this.flag = flag;
        this.displayName = displayName;
        this.description = description;
    }

    final int flag;
    final String displayName;
    final String description;

    ZQuest load() {
        switch (this) {
            case Tutorial:
                return new ZQuestTutorial();
            case Big_Game_Hunting:
                return new ZQuestBigGameHunting();
            case The_Black_Book:
                return new ZQuestTheBlackBook();
            case The_Abomination:
                return new ZQuestTheAbomination();
            case The_Necromancer:
                return new ZQuestTheNecromancer();
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
            case The_Hell_Hole:
                return new ZQuestTheHellHole();
            case Trial_by_Fire:
                return new ZQuestTrialByFire();
            case Welcome_to_Wulfsberg:
                return new WolfQuestWelcomeToWulfsburg();
            case Know_Your_Enemy:
                return new WolfQuestKnowYourEnemy();
            case The_Evil_Twins:
                return new WolfQuestTheEvilTwins();
            case The_Ambush:
                return new WolfQuestTheAmbush();
            case Immortal:
                break;
            case Zombie_Court:
                break;
            case Blood_Red:
                break;
            case The_Ghost_Door:
                break;
            case The_Zombie_Army:
                break;
            case A_Coin_For_The_Ferryman:
                break;
        }
        Utils.assertTrue(false);
        return null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static List<ZQuests> questsBlackPlague() {
        return Utils.filterItems(quest -> 0 != (quest.flag & FLAG_BLACK_PLAGUE), values());
    }

    public static List<ZQuests> questsWolfsburg() {
        return Utils.filterItems(quest -> 0 != (quest.flag & FLAG_WOLFBURG), values());
    }

    boolean isWolfburg() {
        return 0 != (flag & FLAG_WOLFBURG);
    }
}
