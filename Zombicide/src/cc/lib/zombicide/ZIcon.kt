package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZIcon {
	DRAGON_BILE,
	CLAWS,
	SHIELD,
	SLIME,
	RUBBLE,
	TORCH,
	FIRE,
	ARROW, // all ranged
	BOLT, // ballista
	SPAWN_RED,
	SPAWN_BLUE,
	SPAWN_GREEN,
	SPAWN_NECRO,
	SLASH,
	FIREBALL,
	GRAVESTONE,
	PADLOCK,
	NOISE,
	SKULL,
	DAGGER,
	SPEAR,
	BOULDER, // catapult
	SWORD,
    MJOLNIR,
    BLACKBOOK;

    lateinit var imageIds: IntArray
}