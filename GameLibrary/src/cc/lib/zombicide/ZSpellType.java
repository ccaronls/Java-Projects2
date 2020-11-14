package cc.lib.zombicide;

public enum  ZSpellType implements ZEquipmentType<ZEnchantment> {

    HEALING, // once per targeted survivor recovers a wound point
    INVISIBILITY, // once per turn targeted survivor benefits from rotten skill effects until end of round.
    // Making noise or attacking nullifys the effect
    IRONCLAD_ZOMBIE_WOLFZ, // Once per turn targeted survivor benefits from effect until end of round
    REPULSE, // once per turn select a target zone survivor can see and destination zone at range 1
    // from it - sharing an open path - and 1 zone farther from the survivor. Zombies
    // standing in the target zone are pushed into the destination zone.
    SPEED, // Once per turn select a survivor in a zone without zombies. He may emmidiately perform
    // a free move action up to 2 zones.
    TRANSMUTATION, // Once per turn the survivor discards an equipment of their choice from inventory
    // Draw an equipment card. He re re-organize inventory for free. Not a search action.
    // Ahhhh! card played as usual
    DRAGON_AURA, // Once per turn targeted survivor gains +4 armor until end of round.
    HELL_GOAT, // Once per turn the targeted survivor gains the shove skill until end of round

    ;

    @Override
    public ZEnchantment create() {
        return new ZEnchantment(this);
    }
}
