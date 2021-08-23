package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GDimension;

@Keep
public enum ZZombieType {
    // TODO: Don't break MVC. Here we make assumptions about assets for zombies.
    Walker(ZZombieCategory.STANDARD, 1, 1, 1, false, 1, true,
            "Once peasants, craftsmen, merchants, or\n" +
            "townsfolk, these poor unfortunates were\n" +
            "taken unawares. These were everyday people, with\n" +
            "their own hopes and dreams, now just zombies all\n" +
            "with a singular purpose. We call them Walkers,\n" +
            "the dumbest and most numerous of the lot. But\n" +
            "never underestimate them. In numbers, they’re\n" +
            "very dangerous, and they certainly have numbers."),
    Fatty(ZZombieCategory.STANDARD,2, 1, 1, false, 2, true,
    "Fatties are what we call the… well, fat ones.\n" +
            "But there’s more to that blubber than just\n" +
            "a dead rich merchant or noble. They ignore pain, just\n" +
            "like most dead men. You need a strong arm to finish\n" +
            "these. Or a powerful weapon. Or a wizard. Fire\n" +
            "works, of course. Use it well to send them to their\n" +
            "eternal rest."),
    Runner( ZZombieCategory.STANDARD,1, 1, 2, false, 3, true,
    "Runners are fast. Faster than anything on\n" +
            "two legs should be. I’ve seen ‘em outrun a\n" +
            "galloping horse, though only barely. But still, the\n" +
            "poor cavalryman didn't stand a chance.\n" +
            "Special Rule: Each runner has 2 Actions per Activation"),
    Abomination( ZZombieCategory.STANDARD,3, 5, 1, true, 2, false,
    "Haven’t seen an Abomination yet?\n" +
            "Count yourself lucky, neighbor.\n" +
            "You’ll recognize one as soon as you see\n" +
            "it. Weapons don’t work. Armor don’t\n" +
            "work. Running… well, it might work if you’re really\n" +
            "fast. But these are relentless. Fire is what you need.\n" +
            "A good hot fire. It’s the only thing that works.\n" +
            "Special Rules:\n* Wounds inflicted by Abominations can’t be prevented byArmor rolls.\n" +
            "• A Damage 3 weapon is required to kill an Abomination.\n" +
            "There is no such weapon in Zombicide: Black Plague’s core box.\n" +
            "To slay the monster, your team needs to throw Dragon Bile in\n" +
            "its Zone and ignite it with a Torch, creating a Dragon Fire.\n" +
            "Samson can also achieve this at Red Level, using a\n" +
            "Damage 2 Melee weapon in conjunction with his +1 Damage"),
    Necromancer( ZZombieCategory.NECROMANCER,1, 1, 1, false, 5, false,
    "Everyone’s heard the children’s\n" +
            "stories, of the necromancers that\n" +
            "live in the woods, that’ll steal little\n" +
            "children that wander too far. The\n" +
            "stories worked, and kept most kids close to home.\n" +
            "No one thought they were real, not in this day and\n" +
            "age. We don’t know where they came from, or what\n" +
            "they want... Maybe to just destroy every living thing\n" +
            "except themselves. They’re immune to the plague\n" +
            "somehow, but that figures since they control the\n" +
            "hordes. Kill them on sight, and burn the corpses.\n" +
            "It’s the only way to be sure."),
    Wolfz(ZZombieCategory.WOLFSBURG,1, 1, 3, false, 4, true,
  "We now believe the wolves were the first\n" +
             "signs of the coming horde. They hunt, certainly,\n" +
             "but nothing will draw them like a fresh kill.\n" +
             "Scavenging is smart, and numerous wolves can run any\n" +
             "lone mountain lion off their kill. When they first\n" +
             "encountered the shambling hordes, straggling in like\n" +
             "the zombies do, we’re sure the wolves couldn’t resist\n" +
             "such easy prey. But, eating that infected meat… well.\n" +
             "It changed them."),
    Wolfbomination(ZZombieCategory.WOLFSBURG, 3, 5, 3, true, 2, false,
            "The Big Bad Wolf exists, and it is a bloodthirsty nightmare. We don’t know yet how the Wolfbominations are created, if they are just alpha males turned zombies or the result of hideous experiments, but there is one thing for sure: if you see one, keep away from it for as long as you can. And save dragon bile for them, whatever comes for you first.");

    ZZombieType(ZZombieCategory category, int minDamageToDestroy, int expProvided, int actionsPerTurn, boolean ignoresArmor, int attackPriority, boolean canDoubleSpawn, String description) {
        this.category = category;
        this.minDamageToDestroy = minDamageToDestroy;
        this.expProvided = expProvided;
        this.actionsPerTurn = actionsPerTurn;
        this.ignoresArmor = ignoresArmor;
        this.attackPriority = attackPriority;
        this.canDoubleSpawn = canDoubleSpawn;
        this.description = description;
    }

    final ZZombieCategory category;
    final int minDamageToDestroy;
    final int expProvided;
    final int actionsPerTurn;
    final boolean ignoresArmor;
    final int attackPriority;
    final boolean canDoubleSpawn;
    final String description;

    public float getScale() {
        switch (this) {
            case Abomination:
                return 1.8f;
        }
        return 1;
    }

    public int [] imageOptions=null;
    public int [] imageOutlineOptions=null;
    public GDimension [] imageDims=null;

    public String getDescription() {
        return description;
    }
}
