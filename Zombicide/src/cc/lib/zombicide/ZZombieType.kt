package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GDimension

const val PRIORITY_SURVIVOR = 1
const val PRIORITY_DRAGON = 2
const val PRIORITY_SWARMS = 3
const val PRIORITY_WALKER = 4
const val PRIORITY_FATTY = 5
const val PRIORITY_RUNNER = 6
const val PRIORITY_WOLFZ = 7
const val PRIORITY_NECROMANCER = 7


@Keep
enum class ZZombieType(
	val category: ZZombieCategory,
	val damagePerHit: Int,
	val minDamageToDestroy: Int,
	val expProvided: Int,
	val actionsPerTurn: Int,
	val ignoresArmor: Boolean,
	val targetingPriority: Int,
	val canDoubleSpawn: Boolean,
	val description: String
) {
	// TODO: Don't break MVC. Here we make assumptions about assets for zombies.
	Walker(
		ZZombieCategory.STANDARD, 1, 1, 1, 1, false, PRIORITY_WALKER, true,
		"""
                 Once peasants, craftsmen, merchants, or
                 townsfolk, these poor unfortunates were
                 taken unawares. These were everyday people, with
                 their own hopes and dreams, now just zombies all
                 with a singular purpose. We call them Walkers,
                 the dumbest and most numerous of the lot. But
                 never underestimate them. In numbers, they’re
                 very dangerous, and they certainly have numbers.
                 """.trimIndent()
	),
	Fatty(
		ZZombieCategory.STANDARD, 1, 2, 1, 1, false, PRIORITY_FATTY, true,
		"""
                Fatties are what we call the… well, fat ones.
                But there’s more to that blubber than just
                a dead rich merchant or noble. They ignore pain, just
                like most dead men. You need a strong arm to finish
                these. Or a powerful weapon. Or a wizard. Fire
                works, of course. Use it well to send them to their
                eternal rest.
                """.trimIndent()
	),
	Runner(
		ZZombieCategory.STANDARD, 1, 1, 1, 2, false, PRIORITY_WALKER, true,
		"""
                Runners are fast. Faster than anything on
                two legs should be. I’ve seen ‘em outrun a
                galloping horse, though only barely. But still, the
                poor cavalryman didn't stand a chance.
                Special Rule: Each runner has 2 Actions per Activation
                """.trimIndent()
	),
	Abomination(
		ZZombieCategory.STANDARD, 1, 3, 5, 1, true, PRIORITY_FATTY, false,
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
                """.trimIndent()
	),
	Necromancer(
		ZZombieCategory.NECROMANCER, 1, 1, 1, 1, false, PRIORITY_NECROMANCER, false,
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
                """.trimIndent()
	),
	Wolfz(
		ZZombieCategory.WOLFSBURG, 1, 1, 1, 3, false, PRIORITY_WOLFZ, true,
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
                """.trimIndent()
	),
	Wolfbomination(
		ZZombieCategory.WOLFSBURG, 1, 3, 5, 3, true, PRIORITY_FATTY, false,
		"The Big Bad Wolf exists, and it is a bloodthirsty nightmare. We don’t know yet how the Wolfbominations are created, if they are just alpha males turned zombies or the result of hideous experiments, but there is one thing for sure: if you see one, keep away from it for as long as you can. And save dragon bile for them, whatever comes for you first."
	),
	GreenTwin(
		ZZombieCategory.STANDARD, 1, 3, 5, 1, true, PRIORITY_FATTY, false,
		""
	),
	BlueTwin(
		ZZombieCategory.STANDARD, 1, 3, 5, 1, true, PRIORITY_FATTY, false,
		""
	),
	OrcWalker(
		ZZombieCategory.GREEN_HOARD, 2, 1, 1, 1, false, PRIORITY_WALKER, true,
		"""
				Orc Walkers used to be the rank-and-file
				warriors of the Orcish warbands. The
				zombie plague took their cunning, but none
				of their strength. Each of them is as strong as two
				grown men. Moreover, there are a lot of them still
				roaming around fields, houses, and ruins.
				""".trimIndent()
	),
	OrcFatty(
		ZZombieCategory.GREEN_HOARD, 3, 2, 1, 1, false, PRIORITY_FATTY, true,
		"""
				Orc Fatties used to be leaders, shock troops,
				enforcers and… well, plain brutes. Pay extra
				attention to them: they are strong, even by
				Orc standards, meaning they can punch a hole right
				through your chest. If you have to approach one or go
				into close-quarters combat in an uncharted building,
				make sure you wear armor and carry a four-leaf clover.
				""".trimIndent()
	),
	OrcRunner(
		ZZombieCategory.GREEN_HOARD, 1, 1, 1, 2, false, PRIORITY_RUNNER, true,
		"""
				Orc Runners are Goblins, the Orcs’ smaller
				and faster cousins. They were already
				troublesome in life, and infection made it
				worse. They are fast, very numerous, and really
				unpredictable. Like our own human Runners, they seem
				to have kept some of their survival instinct and hide
				behind their beefier cousins to avoid retaliation.
				""".trimIndent()
	),
	OrcAbomination(
		ZZombieCategory.GREEN_HOARD, 3, 3, 5, 1, true, PRIORITY_FATTY, false,
		"""
				We are not yet certain of the Orc
				Abomination’s origin, but we know
				what defines it best: raw power. It
				is strong enough to rip an armored soldier apart in
				a moment’s notice and impervious to anything but
				extreme damage. Spot them as early as you can in
				the wandering Orcish hordes, then plan two things:
				a way to destroy them and an escape route.
				""".trimIndent()
	),
	OrcNecromancer(
		ZZombieCategory.NECROMANCER, 2, 1, 1, 1, false, PRIORITY_NECROMANCER, false,
		"""
				Evil knows no bounds, color, or
				frontier. As the zombie plague spread,
				we soon observed that some figures
				among the Orcish hordes didn’t turn
				into feral, mindless, killing machines. They were the
				ones keeping still with gloomy smiles, as their own people
				were being corrupted. These dark creatures were the
				necromancers. They caused all of this. The zombie
				horde is their weapon to spread chaos and seize power.
				They flee upon being discovered in order to spread
				the disease somewhere else. Make them your priority
				target, and kill them whenever possible.
				""".trimIndent()
	),
	NecromanticDragon(
		ZZombieCategory.NECRO_DRAGON, 2, 2, 1, 1, true, PRIORITY_DRAGON, false,
		"""
				The zombie invasion is running rampant
				across the land, and dragons themselves
				have woken from their slumber to get rid of the
				problem in their own way: by direct and open
				combat. After all, an army is no match for such a
				magnificent creature of fangs and flames, right?
				Wrong! Many dragons battled the zombie hordes
				head on, without any plan, and were defeated. Raised
				from the dead as necromantic dragons, the beasts
				kept most of their former instinct and willpower.
				""".trimIndent()
	),
	Ratz(
		ZZombieCategory.ALL, 1, 1, 1, 3, false, PRIORITY_SWARMS, false,
		"""
				From the point of view of many,
				rats are pests to be killed at first
				sight. They eat our crops and soil our
				houses. On second thought, however,
				the destinies of rat and man seem linked.
				Both species are survivors. We accused
				rats of being responsible for the black
				plague, the zombie disease, but it seems
				they carry the burden in the same way we
				do. They are vulnerable and can be turned
				into zombies. We call these the Ratz.
				Ratz are as numerous as their brethren,
				of course, but their fear of man has been
				replaced with a hunger for human flesh.
				For an unknown reason so far, they act
				like a giant pack. As soon as some of
				them find a fresh food source, all ratz
				in the vicinity gather for the feast. The
				more there are, the more frantic these
				critters become, and you know how agile
				and fast a rat can be!
				""".trimIndent()
	) {
		override fun isBlockedBy(wallType: ZWallFlag): Boolean = when (wallType) {
			ZWallFlag.WALL,
			ZWallFlag.CLOSED,
			ZWallFlag.LEDGE,
			ZWallFlag.LOCKED -> true

			ZWallFlag.RAMPART,
			ZWallFlag.HEDGE,
			ZWallFlag.NONE,
			ZWallFlag.OPEN -> false
		}
	},
	Crowz(
		ZZombieCategory.MURDER_CROWS, 1, 1, 1, 3, false, PRIORITY_SWARMS, false,
		"""
				We used to live along with crows, the smartest scavengers
				you can find in cities and wild alike. Some among us even
				considered them as useful, in a way, but never told it loud
				enough for the priests to hear. “Devil’s brood”, they said.
				The holy men were right: unfortunately, crows were not smart
				enough to keep from eating infected flesh, and turned into 
				flying pests we now call crowz. The vicious predators find
				strength in murders, ignoring mundane obstacles to reach their
				next meal!

				Being scavengers, crows ate things they should have avoided
				and turned infected too, like zombies. These murders of crows
				are really nasty small flying pests, so small they can enter
				everywhere, so fast they’re on you in seconds and quickly rip
				you apart.
				""".trimIndent()
	) {
		override fun isBlockedBy(wallType: ZWallFlag): Boolean = false
	},

	SpectralWalker(
		ZZombieCategory.SPECTRAL, 1, 1, 1, 1, false, PRIORITY_WALKER, false,
		"""
				Imbued with dark energies, Spectral Walkers may look like 
				angry ghosts, but they are still zombies, and may rip survivors 
				apart in the same easy manner!

				Mundane weapons like arrows and blades have no effect on them. 
				Only magic, fire, or ballista bolts may take them down. Keep a 
				combat spell handy, or face near-invincible foes!
				""".trimIndent()
	) {
		override fun isDamagedBy(weaponType: ZWeaponType): Boolean =
			weaponType.weaponClass != ZWeaponClass.NORMAL
	},
	RatKing(
		ZZombieCategory.NECROMANCER, 1, 1, 3, 1, false, PRIORITY_NECROMANCER, false,
		"""
				Everyone knows that the zombie
				hordes emerged from the wilderness
				at the black plague’s outset. Yet, they
				didn’t strike just one town or city, but
				many regions across the kingdoms.
				The best (remaining) wizards and
				scholars believe that its creation was
				an act of raw necromantic power, but
				its propagation resembles a malignant
				disease. As such, many, many creatures
				can be carriers, and the Rat King’s gift
				with rats makes him a natural harbinger
				of this virulent doom. He’s kept safe and
				secure by Tobias, his enormous pet rat
				who watches his back, night and day.
				""".trimIndent()
	),
	SwampTroll(
		ZZombieCategory.ALL, 2, 3, 5, 1, true, PRIORITY_FATTY, false,
		"""
				Waterholes are not a welcome
				sight by any stretch of the
				imagination. It’s bad enough that the
				stagnant, smelly flooded streets hinder
				our progress. But worse yet is that the
				murky waters conceal whatever might be
				hiding under the surface. Stories say trolls
				like to hide under bridges, but a Swamp
				Troll hides underwater, always ready
				to strike at careless survivors who wade
				into these watery traps! Even the bravest
				knight would wet himself when surprised
				by this monstrosity (if he weren’t already
				soaking wet from the waterhole).				
				""".trimIndent()
	)
	;

	val scale: Float
		get() = when (this) {
			Abomination, Wolfbomination, GreenTwin, BlueTwin, OrcAbomination -> 1.8f
			else -> 1f
		}

	open fun isBlockedBy(wallType: ZWallFlag): Boolean = when (wallType) {
		ZWallFlag.WALL,
		ZWallFlag.CLOSED,
		ZWallFlag.LOCKED,
		ZWallFlag.RAMPART,
		ZWallFlag.LEDGE,
		ZWallFlag.HEDGE -> true

		ZWallFlag.NONE,
		ZWallFlag.OPEN -> false
	}

	open fun isDamagedBy(weaponType: ZWeaponType): Boolean = true

	val isNecromancer: Boolean
		get() = targetingPriority == PRIORITY_NECROMANCER

	lateinit var imageOptions: IntArray
	lateinit var imageOutlineOptions: IntArray
	lateinit var imageDims: Array<GDimension>
}