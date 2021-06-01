package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.game.GDimension;

@Keep
public enum ZZombieType {
    // TODO: Don't break MVC. Here we make assumptions about assets for zombies.
    Walker(1, 1, 1, false, 1,
            "Once peasants, craftsmen, merchants, or\n" +
            "townsfolk, these poor unfortunates were\n" +
            "taken unawares. These were everyday people, with\n" +
            "their own hopes and dreams, now just zombies all\n" +
            "with a singular purpose. We call them Walkers,\n" +
            "the dumbest and most numerous of the lot. But\n" +
            "never underestimate them. In numbers, they’re\n" +
            "very dangerous, and they certainly have numbers."),
    Fatty(2, 1, 1, false, 2,
    "Fatties are what we call the… well, fat ones.\n" +
            "But there’s more to that blubber than just\n" +
            "a dead rich merchant or noble. They ignore pain, just\n" +
            "like most dead men. You need a strong arm to finish\n" +
            "these. Or a powerful weapon. Or a wizard. Fire\n" +
            "works, of course. Use it well to send them to their\n" +
            "eternal rest."),
    Runner( 1, 1, 2, false, 3,
    "Runners are fast. Faster than anything on\n" +
            "two legs should be. I’ve seen ‘em outrun a\n" +
            "galloping horse, though only barely. But still, the\n" +
            "poor cavalryman didn’t stand a chance.\n" +
            "Special Rule: Each runner has 2 Actions per Activation"),
    Abomination( 3, 5, 1, true, 2,
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
    Necromancer( 1, 1, 0, false, 4,
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
            "It’s the only way to be sure.");

    ZZombieType(int minDamageToDestroy, int expProvided, int actionsPerTurn, boolean ignoresArmor, int rangedPriority, String description) {
        this.minDamageToDestroy = minDamageToDestroy;
        this.expProvided = expProvided;
        this.actionsPerTurn = actionsPerTurn;
        this.ignoresArmor = ignoresArmor;
        this.rangedPriority = rangedPriority;
        this.description = description;
    }

    final int minDamageToDestroy;
    final int expProvided;
    final int actionsPerTurn;
    final boolean ignoresArmor;
    final int rangedPriority;
    final String description;

    public float getScale() {
        switch (this) {
            case Abomination:
                return 1.8f;
        }
        return 1;
    }

    public int [] imageOptions=null;
    public GDimension [] imageDims=null;

    public String getDescription() {
        return description;
    }
}
