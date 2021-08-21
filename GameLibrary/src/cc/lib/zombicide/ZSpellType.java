package cc.lib.zombicide;

import cc.lib.annotation.Keep;
import cc.lib.ui.IButton;

@Keep
public enum  ZSpellType implements ZEquipmentType<ZSpell>, IButton {

    // TODO: Do we really need spell type?

    HEALING(ZSkill.Healing,"Once per turn targeted survivor recovers a wound point"),
    INVISIBILITY(ZSkill.Invisible,"Once per turn targeted survivor benefits from rotten skill effects until end of round. Making noise or attacking nullify the effect"),
    //IRONCLAD_ZOMBIE_WOLFZ(ZSkill.Ironclad, "Once per turn targeted survivor benefits from effect until end of round."),
    //REPULSE("once per turn select a target zone survivor can see and destination zone at range 1 from it - sharing an open path - and 1 zone farther from the survivor. Zombies standing in the target zone are pushed into the destination zone."),
    SPEED(ZSkill.Speed, "Once per turn select a survivor in a zone without zombies. He may immediately perform a free move action up to 2 zones.") {
        @Override
        public void doEnchant(ZGame game, ZCharacter target) {
            game.pushState(ZState.PLAYER_ENCHANT_SPEED_MOVE, target.getPlayerName());
        }
    },
    //TRANSMUTATION("Once per turn the survivor discards an equipment of their choice from inventory. Draw an equipment card. He can re-organize inventory for free. Not a search action. Ahhhh! card played as usual"),
    //DRAGON_AURA(ZSkill.Dragon_Aura, "Once per turn targeted survivor gains +4 armor until end of round."),
    HELL_GOAT(ZSkill.Shove,"Once per turn the targeted survivor gains the shove skill until end of round"),
    ;

    ZSpellType(ZSkill skill, String description) {
        this.skill = skill;
        this.description = description;
    }

    final ZSkill skill;
    final String description;

    @Override
    public ZSpell create() {
        return new ZSpell(this);
    }

    @Override
    public ZActionType getActionType() {
        return ZActionType.ENCHANTMENT;
    }

    @Override
    public String getTooltipText() {
        return description;
    }

    public void doEnchant(ZGame game, ZCharacter target) {
        target.addAvailableSkill(skill);
    }
}
