package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZIcon {
    DRAGON_BILE,
    CLAWS,
    SHIELD,
    SLIME,
    TORCH,
    FIRE,
    ARROW,
    SPAWN_RED,
    SPAWN_BLUE,
    SPAWN_GREEN,
    SLASH,
    FIREBALL,
    GRAVESTONE,
    PADLOCK,
    SKULL,
    DAGGER,
    SWORD,
    MJOLNIR,
    BLACKBOOK;

    lateinit var imageIds: IntArray
}