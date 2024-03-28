package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.game.GDimension

/**
 * Created by Chris Caron on 2/15/24.
 */
@Keep
enum class ZFamiliarType(val weaponType: ZWeaponType, val skills: Array<ZSkill>) : ZEquipmentType {
	GOG(ZWeaponType.FAMILIAR_HOUND, arrayOf(ZSkill.Low_profile, ZSkill.Search, ZSkill.Slippery)),
	MAGOG(ZWeaponType.FAMILIAR_HOUND, arrayOf(ZSkill.Low_profile, ZSkill.Search, ZSkill.Slippery)),
	SETH(
		ZWeaponType.FAMILIAR_FLYING_CAT,
		arrayOf(ZSkill.Birds_eye_view, ZSkill.Low_profile, ZSkill.Search, ZSkill.Slippery)
	),
	MANADIS(
		ZWeaponType.FAMILIAR_FLYING_CAT,
		arrayOf(ZSkill.Birds_eye_view, ZSkill.Low_profile, ZSkill.Search, ZSkill.Slippery)
	),
	NUCIFER(ZWeaponType.FAMILIAR_WOLF, arrayOf(ZSkill.Low_profile, ZSkill.Slippery)),
	VATAN(ZWeaponType.FAMILIAR_WOLF, arrayOf(ZSkill.Low_profile, ZSkill.Slippery));

	var imageId = -1
	var cardImageId = -1
	var outlineImageId = -1
	var imageDim: GDimension = GDimension.EMPTY

	override fun create(): ZFamiliarLink = ZFamiliarLink(this)

	override val equipmentClass = ZEquipmentClass.FAMILIAR

	override fun isActionType(type: ZActionType): Boolean {
		TODO("Not yet implemented")
	}

	override fun getTooltipText(): String? = skills.joinToString("\n") {
		it.getLabel()
	}
}