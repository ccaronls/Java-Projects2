package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GDimension
import cc.lib.utils.Table
import cc.lib.utils.prettify
import cc.lib.utils.wrap


@Keep
enum class ZSiegeTypeEngineType(val costPerAction: Int, vararg types: ZWeaponType) {
	CATAPULT(3, ZWeaponType.SCATTERSHOT, ZWeaponType.GRAPESHOT, ZWeaponType.BOULDER),
	BALLISTA(2, ZWeaponType.BOLT)
	;

	val types = listOf(*types)
	var imageId = -1
	var imageOutlineId = -1
	var dimension = GDimension()
}

/**
 * Created by Chris Caron on 12/28/23.
 */
class ZSiegeEngine(
	startZone: Int = -1,
	override val type: ZSiegeTypeEngineType = ZSiegeTypeEngineType.BALLISTA
) : ZActor(startZone) {

	companion object {
		init {
			addAllFields(ZSiegeEngine::class.java)
		}
	}

	override val actionsPerTurn: Int
		get() = 0

	override fun name(): String = type.prettify()

	override val imageId: Int
		get() = type.imageId

	override val outlineImageId: Int
		get() = type.imageOutlineId

	override val isSiegeEngine: Boolean
		get() = true

	override fun getDimension() = type.dimension

	override fun getSpawnQuadrant(): ZCellQuadrant = ZCellQuadrant.CENTER

	override fun makeId(): String = when (type) {
		ZSiegeTypeEngineType.CATAPULT -> type.name
		else -> super.makeId()
	}

	override val moveSpeed: Long
		get() = 2000L

	override val scale: Float
		get() = 2f

	override fun isBlockedBy(wallType: ZWallFlag): Boolean = !wallType.catapultCrossable

	fun getInfo(game: ZGame): Table = Table().setNoBorder().also { table ->
		table.addColumnNoHeader(
			listOf(
				"Type",
				"Damage",
				"Hit %",
				"Range",
				"Doors",
				"Reloads"
			)
		)
		type.types.forEach { weaponType ->

			weaponType.create().getStatForAction(ZActionType.CATAPULT_FIRE)?.let { stats ->
				val doorInfo: String = if (stats.dieRollToOpenDoor > 0) {
					String.format(
						"%s %d%%",
						if (weaponType.openDoorsIsNoisy) "noisy" else "quiet",
						stats.dieRollToOpenDoorPercent
					)
				} else {
					"no"
				}
				table.addColumnNoHeader(
					listOf(
						weaponType.name,
						String.format(
							"%d %s",
							stats.damagePerHit,
							if (weaponType.attackIsNoisy) " loud" else " quiet"
						),
						String.format(
							"%d%% x %d",
							(7 - stats.dieRollToHit) * 100 / 6,
							stats.numDice
						),
						stats.rangeString,
						doorInfo,
						"no"
					)
				)
				if (weaponType.specialInfo != null) {
					table.addRow("*${weaponType.specialInfo}".wrap(32))
				} else {
					val skills = weaponType.skillsWhileEquipped
					if (skills.isNotEmpty()) {
						table.addRow("Equipped", skills)
					}
				}
			}
		}
	}

	override fun getMoveOptions(name: ZPlayerName, game: ZGame): List<ZMove> = when (type) {
		ZSiegeTypeEngineType.CATAPULT -> {
			// for firing a catapult, find all zones with at least 1 zombie of any type or a destructable spawn zone
			val board = game.board
			board.zones.filter { board.isZoneTargetForCatapult(it) }.map {
				it.zoneIndex
			}.toSet().filter {
				board.getDistanceBetweenZones(it, occupiedZone) >= 1
			}.takeIf { it.isNotEmpty() }?.let {
				listOf(
					ZMove.newFireCatapultScatterShot(name, it),
					ZMove.newFireCatapultGrapeShot(name, it),
					ZMove.newFireCatapultBoulder(name, it)
				)
			} ?: emptyList()
		}

		ZSiegeTypeEngineType.BALLISTA -> {
			ZDir.compassValues.filter {
				game.board.getCell(occupiedCell).getWallFlag(it)
					.openedForAction(ZActionType.BALLISTA_FIRE)
			}.takeIf { it.isNotEmpty() }?.let {
				listOf(ZMove.newFireBallistaBolt(name, it))
			} ?: emptyList()
		}
	}
}