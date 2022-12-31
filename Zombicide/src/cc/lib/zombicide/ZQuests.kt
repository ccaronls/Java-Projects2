package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.ui.IButton

import cc.lib.zombicide.quests.*

const val FLAG_BLACK_PLAGUE = 1
const val FLAG_WOLFBURG = 2
const val FLAG_DEBUG = 4

@Keep
enum class ZQuests(val flag: Int, val displayName: String, val description: String) : IButton {
    Tutorial(FLAG_BLACK_PLAGUE, "TUTORIAL:DANSE MACABRE", "War is nothing new for us. Our counts " +
            "and dukes are always fighting amongst " +
            "themselves. For the peasantry, it usually just " +
            "involves a change in taxes and rents, assuming you " +
            "survive. But this time, the duke and his army went " +
            "off and were never seen again. Well, not until the " +
            "hordes emerged. Pretty sure a lot of the tougher " +
            "ones came from his troops. Now everything’s a " +
            "brutal mess. Now we’re all equals, facing the danse " +
            "macabre together. There’s no time for social " +
            "snobbery when the hordes are at your door. We " +
            "stand together, and throw death back in their teeth. "),
    The_Abomination(FLAG_DEBUG, "The Abomination", "Test Abomination!"),
    The_Necromancer(FLAG_DEBUG, "The Necromancer", "Test Necromancer!"),
	Thors_Hammer(FLAG_DEBUG, "Thor's Hammer", "Obtain Mjolnir and destroy all zombies and spawn zones"),
    Big_Game_Hunting(FLAG_BLACK_PLAGUE, "Big Game Hunting", "We quickly discovered the starting point " +
            "of the zombie invasion. Other survivors " +
            "spotted a huge zombie wandering the streets, and some " +
            "kind of sick wizard directing the horde to engulf us. " +
            "It took us two days to pinpoint the Necromancer’s " +
            "location, and understand the Abomination can’t be " +
            "killed by any weapon at our disposal. Let’s raid the " +
            "Necromancer’s laboratory and take them both out " +
            "with a secret brew of our own: Dragon Fire. " +
            "Let the hunt begin!"),
    The_Black_Book(FLAG_BLACK_PLAGUE, "The Black Book", "Now we know. It’s not just our village. The " +
            "zombie plague has spread across the land. " +
            "What’s going on? The Necromancer we killed held " +
            "notes in his laboratory, most of them referring to a " +
            "mysterious Black Book and other items of power. " +
            "Exploring the surroundings could prove useful to " +
            "get a better grasp about the threat we’re facing. Of " +
            "course, there are zombies on the way, familiar faces " +
            "turned to monsters... " +
            "Hey, that one owed me money!"),
    The_Shepherds(FLAG_BLACK_PLAGUE, "The Shepherds", "Necromancers are everywhere. They’re " +
            "spreading chaos and seizing power in the " +
            "whole kingdom! Against a menace this big, there " +
            "is almost nothing we could do. Almost. We know " +
            "we’re good at survival as long as we stand together. " +
            "Our plan is to reach out and find other survivors " +
            "to create an army of our own. Four days we’ve spent, " +
            "traveling to the next village, which " +
            "is currently under attack, but not " +
            "completely overrun. Let’s get in " +
            "the fray and help these people!"),
    Famine(FLAG_BLACK_PLAGUE, "Famine", "Afew days have passed. These " +
            "zombies are, for the most part, " +
            "stupid as hell. But they never tire, or " +
            "need food, or even sleep. Alas, we’re all " +
            "too human. We need food and a secure " +
            "shelter. " +
            "There are many vaults beneath this " +
            "town. Clever survivors could hole up " +
            "and rest for a while. But, we still need " +
            "to gather supplies to last a couple of days " +
            "while we plan our next move. This war " +
            "may last far longer than anyone - even " +
            "the Necromancers - expected."),
    The_Commandry(FLAG_BLACK_PLAGUE, "The Commandry", "This capital has been taken. People died " +
            "by the thousands, but some areas are " +
            "still unharmed. The Necromancers seem content " +
            "to battle the nobility in the castle, and leave the " +
            "commoners corralled for when their zombie hordes " +
            "need ready reinforcements. " +
            "We need a way in, to establish communications with " +
            "the people still alive. It’s also been suggested we " +
            "learn more about the infection. The Black Book " +
            "says little on it. If we can learn more, " +
            "we can plan a bold move to end this. " +
            "First we need a way past the city walls. " +
            "Some survivors we rescued speak of a " +
            "secret passage beneath the commandry " +
            "nearby. However, it’s guarded. Clearly " +
            "the Necromancers are aware of it. If " +
            "we can dispatch the guardians, we can " +
            "get inside, and get our plan in motion."),
    In_Caligine_Abditus(FLAG_BLACK_PLAGUE, "In Caligine Abdicus", "We’re now in the city, but not as close to " +
            "the Necromancers as we would like. The " +
            "area is eerily quiet, and there’s desolation as far as " +
            "we can see. That won’t last. As soon as they hear " +
            "us, they’ll be rushing to kill us. We must proceed as " +
            "stealthily as possible. " +
            "Clovis and Baldric both noticed strange Latin " +
            "writings on some walls. It seems someone here used " +
            "a network of underground passages to get around " +
            "town. And Clovis is right when he says " +
            "that not everybody knows Latin... Only " +
            "the highly educated know this language " +
            "on sight, not to mention being able to " +
            "write it. It’s probably a Necromancer! " +
            "Wait. Clovis knows how to read?"),
    Dead_Trail(FLAG_BLACK_PLAGUE, "Dead Trail", "There is no way we can get any further " +
            "unnoticed. That’s good, for my fingers are " +
            "itching for some zombie bashing, and I was getting " +
            "tired of walking on tiptoe. " +
            "There are dark signs and symbols on the walls here. " +
            "The necromancers are hanging around. We don’t " +
            "know what kind of ritual they are performing, but " +
            "we must try to make it fail. Let’s see what happens " +
            "if we destroy these wicked scriptures... " +
            "Fortunately, this is the foundry " +
            "district. These Orc weaponsmiths seem " +
            "to know their job pretty well. New toys!"),
    The_Evil_Temple(FLAG_BLACK_PLAGUE, "The Evil Temple", "This is the center of necromantic " +
            "power.Cursed idols are everywhere, and " +
            "a huge Abomination is locked up in the temple. " +
            "Plus, it seems the Necromancers have figured out " +
            "how we’re dealing with their biggest beasts. Dragon " +
            "Bile is scarce, and our supplies gone. But, there " +
            "are vaults here. All the old parts of town had them. " +
            "Killing that beast could draw the Necromancers to " +
            "us. There may be thousands of zombies, but there " +
            "can’t be too many more Necromancers. … Right?"),
    The_Hell_Hole(FLAG_BLACK_PLAGUE, "The Hell Hole", "I think we stumbled upon the place our Duke made " +
            "his last stand before the town fell. All who sought " +
            "his protection gathered in the temple, under the protection " +
            "of the gods, the remaining soldiers and the Duke himself. " +
            "It wasn’t enough, unfortunately. After a huge fight, " +
            "the zombies killed everyone. And now, this is a hellhole " +
            "vomiting zombies. We have no choice but to fight them " +
            "and destroy this forsaken place once and for all. " +
            "Hey, do I see the Duke? Nothing personal, Your Grace!"),
    Trial_by_Fire(FLAG_BLACK_PLAGUE, "Trial by fire", "We’re in the heart of the city, the place where " +
            "all zombies converged. It seems we’re not " +
            "the first ones to get here. Heroes or mercenaries " +
            "of some sort tried to clean the place before us, and " +
            "failed. However, they locked the most impressive " +
            "Abomination we’ve seen so far in a nearby magic " +
            "school. The beast is trapped and is waiting for someone " +
            "– or something – to break its bonds. Its roaring " +
            "lures every zombie around like a beacon. " +
            "And the Necromancers are still nowhere " +
            "to be seen."),  // Wulfsburg
    Welcome_to_Wulfsberg(FLAG_WOLFBURG, "Welcome to Wulfsburg", "The prosperous city of Wulfsburg earned its " +
            "name due to the many wolf packs roaming " +
            "the surrounding forests and mountains. Nobles and " +
            "merchants built tall towers here, the better to view " +
            "the scenic valley (and display their wealth and status). " +
            "With the plague’s coming, the wolf packs attacked " +
            "wandering zombies, and fell victim to the infection " +
            "themselves. Now hungry for living flesh, the wolfz’ " +
            "made the city their new hunting ground. " +
            "Wulfsburg has become a Necromancer outpost, " +
            "populated with hidden, terrified survivors. We’re on " +
            "our way to liberate the city. Breaching the inner " +
            "city will take time, however, and we’ll need supplies. " +
            "Fresh food is scarce, but still to be had."),
    Know_Your_Enemy(FLAG_WOLFBURG, "Know your Enemy",
            "Wulfsburg sustained some unusual damage, " +
                    "as if a civil war had raged inside. In " +
                    "some place, people were not killed by zombies but by " +
                    "soldiers. We don’t know yet if survivor groups are " +
                    "prone to fighting one another here, or if someone " +
                    "tried to invade the infested city, killing any " +
                    "survivors they ran across in the process. Exploring " +
                    "the area could give us a clue. " +
                    "Come to think of it, Wulfsburg " +
                    "was known for its elven beer. That " +
                    "would be a rare treat!"),
    The_Evil_Twins(FLAG_WOLFBURG, "The Evil Twins",
            "We found a soldier’s journal among the " +
                    "bloodstained houses. It seems a foreign " +
                    "prince had come to Wulfsburg shortly after the " +
                    "invasion began with his private army. Even its " +
                    "fallen state, the city retains its wealth. Zombies " +
                    "are’nt interested in treasure, so he thought it’d be " +
                    "an easy conquest. " +
                    "The blood-spattered journal ends with an entry " +
                    "about a pair of abominations stalking the final few " +
                    "survivors from the prince’s retinue. The ‘Evil " +
                    "Twins’ they were called, and they seem to haunt " +
                    "the Usurer’s Ward, a block away. " +
                    "They’ve surely caught our scent now as well, and " +
                    "could attack at any time. So, we’ll attack first. " +
                    "The best defense is sometimes all-out offense, " +
                    "right?"),
    The_Ambush(FLAG_WOLFBURG, "The Ambush",
            "We were returning to our haven as night " +
                    "began to fall, when the wizard spotted some " +
                    "esoteric writing on the walls. Before our eyes they " +
                    "flared to brilliance, and we heard shuffling footsteps! " +
                    "An ambush! Someone placed zombie lures all around, " +
                    "and the infected are hot on our trail! We must resist " +
                    "long enough to destroy the lures and secure our escape. " +
                    "The Necromancers know we’re here, and consider " +
                    "us a threat. I don’t know what to think about this " +
                    "flattering change."),
    Immortal(FLAG_WOLFBURG, "Immortal",
            "Night has fallen, and the " +
                    "zombies are still dogging our " +
                    "footsteps by the dozen. To make " +
                    "matters worse, we seem to have killed " +
                    "the same Necromancer at least four " +
                    "times. He keeps coming back, over " +
                    "and over. He’s rallying his hordes and " +
                    "summoning more. We’re experienced " +
                    "survivors, but we’re still just mortals. " +
                    "We either figure out how to kill him " +
                    "permanently, or die from exhaustion. " +
                    "The wizards say he likely has some " +
                    "kind of magical anchors binding him " +
                    "to this area. If we destroy them, we " +
                    "can probably kill him for good and take " +
                    "a rest. (And then I can enjoy some " +
                    "of that Elven brew I found before. " +
                    "Don’t tell anyone!)"),
    Zombie_Court(FLAG_WOLFBURG, "Zombie Court",
            "We finally discovered the fate of that " +
                    "invading prince and his retinue. They " +
                    "were hunted, encircled, and butchered by a giant " +
                    "zombie wolfz pack. Fresh cadavers are everywhere. " +
                    "Wait. " +
                    "Make that: fresh zombies are everywhere. Well, " +
                    "let’s call this morning training, eh? " +
                    "While we’re here, let’s find the prince’s royal " +
                    "implements; a crown or scepter for example. It " +
                    "would be nice to return them to the king, and a good " +
                    "reminder that the land belongs to free people now!"),
    Blood_Red(FLAG_WOLFBURG, "Blood Red",
            "We’ve discovered a district just packed with " +
                    "zombies. The Necromancers use the wolfz " +
                    "to round them up and herd them here. We don’t know " +
                    "why, but packing such large numbers of infected into " +
                    "such close quarters is a target too tempting to ignore! " +
                    "Let’s show them why it’s called zombicide!"),
    The_Ghost_Door(FLAG_WOLFBURG, "The Ghost Door",
            "Being reliably sturdy and easy to defend, " +
                    "towers are havens of choice for any " +
                    "survivor group, and for Necromancers as well. The " +
                    "wealthy Wulfsburg is home to many towers built " +
                    "by merchants and nobles as headquarters for their " +
                    "guilds and testimonies to their wealth. " +
                    "One of these towers bears the mark of the ‘immortal’ " +
                    "Necromancer that we killed yesterday. Taking " +
                    "a peek at his stuff could give us a clue about the " +
                    "Necromancers’ master plan (if there even is such a " +
                    "thing). He had plenty of guards, but we’re betting " +
                    "on some nice artifacts too!"),
    The_Zombie_Army(FLAG_WOLFBURG, "The Zombie Army",
            "We found another Necromancer’s lair! " +
                    "The good news is: it’s filled with " +
                    "treasure! The bad news is: the Necromancers have " +
                    "spotted us. Even now, their hordes encircle us. But " +
                    "by now, we’re all experienced survivors. We’ve faced " +
                    "worse than just this petty army, right? Zombicide!"),
    A_Coin_For_The_Ferryman(FLAG_WOLFBURG, "A Coin for the Ferryman",
            "Turns out the Necromancers are human " +
                    "after all. They’ve been using the towers " +
                    "to boost their egos and pile up plundered treasure, " +
                    "just like their former owners did. But, the " +
                    "Necromancers have also opened magical gates " +
                    "leading to some hidden place! The next step in their " +
                    "invasion, perhaps? Who knows? Finding these " +
                    "towers and sealing the gates will trap them here " +
                    "in Wulfsburg with us. Then we finish it, once and " +
                    "for all!");

    fun load(): ZQuest {
        return when (this) {
            Tutorial -> ZQuestTutorial()
            Big_Game_Hunting -> ZQuestBigGameHunting()
            The_Black_Book -> ZQuestTheBlackBook()
            The_Abomination -> ZQuestTheAbomination()
            The_Necromancer -> ZQuestTheNecromancer()
	        Thors_Hammer -> ZQuestThorsHammer()
            The_Shepherds -> ZQuestTheShepherds()
            Famine -> ZQuestFamine()
            The_Commandry -> ZQuestTheCommandry()
            In_Caligine_Abditus -> ZQuestInCaligineAbditus()
            Dead_Trail -> ZQuestDeadTrail()
            The_Evil_Temple -> ZQuestTheEvilTemple()
            The_Hell_Hole -> ZQuestTheHellHole()
            Trial_by_Fire -> ZQuestTrialByFire()
            Welcome_to_Wulfsberg -> WolfQuestWelcomeToWulfsburg()
            Know_Your_Enemy -> WolfQuestKnowYourEnemy()
            The_Evil_Twins -> WolfQuestTheEvilTwins()
            The_Ambush -> WolfQuestTheAmbush()
            Immortal -> WolfQuestImmortal()
            Zombie_Court -> WolfZombieCourt()
            Blood_Red -> WolfBloodRed()
            The_Ghost_Door -> WolfTheGhostDoor()
            The_Zombie_Army -> WolfTheZombieArmy()
            A_Coin_For_The_Ferryman -> WolfACoinForTheFerryman()
        }
    }

    val isWolfBurg: Boolean
        get() = 0 != flag and FLAG_WOLFBURG

    companion object {
        @JvmStatic
        fun questsBlackPlague(): List<ZQuests> {
            return values().filter { quest: ZQuests -> 0 != quest.flag and FLAG_BLACK_PLAGUE }
        }

        @JvmStatic
        fun questsWolfsburg(): List<ZQuests> {
            return values().filter { quest: ZQuests -> 0 != quest.flag and FLAG_WOLFBURG }
        }
    }

	override fun getTooltipText(): String = description

	override fun getLabel(): String = name.replace('_', ' ')
}