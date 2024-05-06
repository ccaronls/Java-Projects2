package cc.lib.zombicide

import cc.lib.annotation.Keep

@Keep
enum class ZAttackType(val description: String) {
	FAMILIAR("Mauled"),
	FIRE("Burned"),
	ELECTROCUTION("Electrocuted"),
	DISINTEGRATION("Disintegrated"),
	BLADE("Impaled"),
	CRUSH("Crushed"),
	RANGED_ARROWS("Sniped"),
	RANGED_BOLTS("Sniped"),
	RANGED_THROW(""),
	EARTHQUAKE("Earthquake"),
	MENTAL_STRIKE("Scanned"),
	DRAGON_FIRE("Incinerated"),
	FREEZE("Frozen"),
	FRIENDLY_FIRE("Friendly Fire"),
}