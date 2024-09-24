package cc.lib.zombicide

import cc.lib.annotation.Keep
import cc.lib.ui.IButton

@Keep
enum class ZSpellType(val skill: ZSkill, val description: String) : ZEquipmentType, IButton {
    HEALING(ZSkill.Healing, "Once per turn targeted survivor recovers a wound point") {
        override suspend fun doEnchant(game: ZGame, target: ZCharacter) {
	        target.heal(game, 1)
        }
    },
	INVISIBILITY(ZSkill.Invisible, "Once per turn targeted survivor benefits from rotten skill effects until end of round. Making noise or attacking nullify the effect"),  //IRONCLAD_ZOMBIE_WOLFZ(ZSkill.Ironclad, "Once per turn targeted survivor benefits from effect until end of round."),

	//REPULSE("once per turn select a target zone survivor can see and destination zone at range 1 from it - sharing an open path - and 1 zone farther from the survivor. Zombies standing in the target zone are pushed into the destination zone."),
	SPEED(ZSkill.Speed, "Once per turn select a survivor in a zone without zombies. He may immediately perform a free move action up to 2 zones.") {
		override suspend fun doEnchant(game: ZGame, target: ZCharacter) {
			game.pushState(ZGame.State(ZState.PLAYER_ENCHANT_SPEED_MOVE, target.type))
		}
	},
	//TRANSMUTATION("Once per turn the survivor discards an equipment of their choice from inventory. Draw an equipment card. He can re-organize inventory for free. Not a search action. Ahhhh! card played as usual"),

	//DRAGON_AURA(ZSkill.Dragon_Aura, "Once per turn targeted survivor gains +4 armor until end of round."),
	HELL_GOAT(ZSkill.Shove, "Once per turn the targeted survivor gains the shove skill until end of round"),

	;

	override fun create(): ZSpell {
		return ZSpell(this)
	}

    override fun isActionType(type: ZActionType): Boolean {
        return type === ZActionType.ENCHANTMENT
    }

	override fun getTooltipText(): String? {
		return description
	}

	open suspend fun doEnchant(game: ZGame, target: ZCharacter) {
		target.addAvailableSkill(skill)
	}

	override val equipmentClass: ZEquipmentClass
		get() = ZEquipmentClass.ENCHANTMENT

	open val usesPerTurn = 1
}