package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.utils.prettify

@Keep
enum class ZActionType {
	NOTHING,
	MOVE {
		override val isMovement = true
	},
	SEARCH {
		override val oncePerTurn = true
	},
	OPEN_DOOR,
	CLOSE_DOOR,
	BARRICADE_DOOR {
		override val breaksInvisibility = true
		override val costPerTurn = 3
	},
	MELEE {
		override val breaksInvisibility = true
		override val isMelee = true
	},
	RANGED {
		override val isProjectile = true
		override val isRanged = true
	},
	MAGIC {
		override val breaksInvisibility = true
		override val isProjectile = true
		override val isMagic = true
	},
	THROW_ITEM {
		override val breaksInvisibility = true
		override val isProjectile = true
		override val isRanged = true
	},
	ENCHANTMENT {
		override val oncePerTurn = true
	},
	ACTIVATE,
	INVENTORY,
	CONSUME,
	OBJECTIVE,
	RELOAD,
	DROP_ITEM,
	PICKUP_ITEM,
	MAKE_NOISE {
		override val breaksInvisibility = true
	},
	SHOVE {
		override val breaksInvisibility = true
		override val oncePerTurn = true
	},
	DEFEND,
	BEQUEATH_MOVE,
	CLOSE_PORTAL {
		override val costPerTurn = 3
	},
	CATAPULT_MOVE {
		override val costPerTurn = 3
		override val isMovement = true
	},
	CATAPULT_FIRE {
		override val costPerTurn = 3
		override val isProjectile = true
	},
	CLIMB {
		override val costPerTurn = 2
		override val isMovement = true
	},
	BALLISTA_MOVE {
		override val costPerTurn = 2
		override val isMovement = true
	},
	BALLISTA_FIRE {
		override val costPerTurn = 2
		override val isProjectile = true
	}


	;


	open val oncePerTurn: Boolean = false
	open val costPerTurn = 1
	open val breaksInvisibility: Boolean = false
	open val isRanged: Boolean = false
	open val isMagic: Boolean = false
	open val isProjectile: Boolean = false
	open val isMelee: Boolean = false
	open val isMovement: Boolean = false
	val label: String
		get() = name.prettify()
}