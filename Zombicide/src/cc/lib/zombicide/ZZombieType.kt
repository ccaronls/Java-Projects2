package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GDimension

@Keep
enum class ZZombieType(@JvmField val category: ZZombieCategory, @JvmField val minDamageToDestroy: Int, @JvmField val expProvided: Int, @JvmField val actionsPerTurn: Int, @JvmField val ignoresArmor: Boolean, @JvmField val rangedPriority: Int, @JvmField val canDoubleSpawn: Boolean, @JvmField val description: String) {
    // TODO: Don't break MVC. Here we make assumptions about assets for zombies.
    Walker(ZZombieCategory.STANDARD, 1, 1, 1, false, 1, true,
            """
                 Once peasants, craftsmen, merchants, or
                 townsfolk, these poor unfortunates were
                 taken unawares. These were everyday people, with
                 their own hopes and dreams, now just zombies all
                 with a singular purpose. We call them Walkers,
                 the dumbest and most numerous of the lot. But
                 never underestimate them. In numbers, they’re
                 very dangerous, and they certainly have numbers.
                 """.trimIndent()),
    Fatty(ZZombieCategory.STANDARD, 2, 1, 1, false, 2, true,
            """
                Fatties are what we call the… well, fat ones.
                But there’s more to that blubber than just
                a dead rich merchant or noble. They ignore pain, just
                like most dead men. You need a strong arm to finish
                these. Or a powerful weapon. Or a wizard. Fire
                works, of course. Use it well to send them to their
                eternal rest.
                """.trimIndent()),
    Runner(ZZombieCategory.STANDARD, 1, 1, 2, false, 4, true,
            """
                Runners are fast. Faster than anything on
                two legs should be. I’ve seen ‘em outrun a
                galloping horse, though only barely. But still, the
                poor cavalryman didn't stand a chance.
                Special Rule: Each runner has 2 Actions per Activation
                """.trimIndent()),
    Abomination(ZZombieCategory.STANDARD, 3, 5, 1, true, 3, false,
            """
                Haven’t seen an Abomination yet?
                Count yourself lucky, neighbor.
                You’ll recognize one as soon as you see
                it. Weapons don’t work. Armor don’t
                work. Running… well, it might work if you’re really
                fast. But these are relentless. Fire is what you need.
                A good hot fire. It’s the only thing that works.
                Special Rules:
                * Wounds inflicted by Abominations can’t be prevented byArmor rolls.
                • A Damage 3 weapon is required to kill an Abomination.
                There is no such weapon in Zombicide: Black Plague’s core box.
                To slay the monster, your team needs to throw Dragon Bile in
                its Zone and ignite it with a Torch, creating a Dragon Fire.
                Samson can also achieve this at Red Level, using a
                Damage 2 Melee weapon in conjunction with his +1 Damage
                """.trimIndent()),
    Necromancer(ZZombieCategory.NECROMANCER, 1, 1, 1, false, 99, false,
            """
                Everyone’s heard the children’s
                stories, of the necromancers that
                live in the woods, that’ll steal little
                children that wander too far. The
                stories worked, and kept most kids close to home.
                No one thought they were real, not in this day and
                age. We don’t know where they came from, or what
                they want... Maybe to just destroy every living thing
                except themselves. They’re immune to the plague
                somehow, but that figures since they control the
                hordes. Kill them on sight, and burn the corpses.
                It’s the only way to be sure.
                """.trimIndent()),
    Wolfz(ZZombieCategory.WOLFSBURG, 1, 1, 3, false, 5, true,
            """
                We now believe the wolves were the first
                signs of the coming horde. They hunt, certainly,
                but nothing will draw them like a fresh kill.
                Scavenging is smart, and numerous wolves can run any
                lone mountain lion off their kill. When they first
                encountered the shambling hordes, straggling in like
                the zombies do, we’re sure the wolves could’nt resist
                such easy prey. But, eating that infected meat… well.
                It changed them.
                """.trimIndent()),
    Wolfbomination(ZZombieCategory.WOLFSBURG, 3, 5, 3, true, 3, false,
            "The Big Bad Wolf exists, and it is a bloodthirsty nightmare. We don’t know yet how the Wolfbominations are created, if they are just alpha males turned zombies or the result of hideous experiments, but there is one thing for sure: if you see one, keep away from it for as long as you can. And save dragon bile for them, whatever comes for you first."),
    GreenTwin(ZZombieCategory.STANDARD, 3, 5, 1, true, 3, false,
            ""),
    BlueTwin(ZZombieCategory.STANDARD, 3, 5, 1, true, 3, false,
            "");

    val scale: Float
        get() {
            when (this) {
                Abomination, Wolfbomination, GreenTwin, BlueTwin -> return 1.8f
            }
            return 1f
        }
    lateinit var imageOptions: IntArray
    lateinit var imageOutlineOptions: IntArray
    lateinit var imageDims: Array<GDimension>
}