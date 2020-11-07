package cc.lib.zombicide;

public enum ZSkill {
    Ironclad, /*[Zombie type] – The Survivor ignores all Wounds
    coming from Zombies of the specified type (such as “Walker”,
    “Runner”, etc.).*/
    Iron_hide, /* The Survivor can make Armor rolls with a 5+ Armor
    value, even when he does not wear an armor on his Body slot.
    Wearing an armor, the Survivor adds 1 to the result of each die
    he rolls for Armor rolls. The maximum result is always 6.*/
    Iron_rain, /* – When resolving a Ranged Action, the Survivor may
    substitute the Dice number of the Ranged weapon(s) he uses
    with the number of Zombies standing in the targeted Zone.
    Skills affecting the dice value, like +1 die: Ranged, still apply.
    Is that all you’ve got? – You can use this Skill any time the
    Survivor is about to get Wounds. Discard one Equipment
    card in your Survivor’s inventory for each Wound he’s about
    to receive. Negate a Wound per discarded Equipment card.*/
    Jump, /* The Survivor can use this Skill once during each Activation.
    The Survivor spends one Action: He moves two Zones
    into a Zone to which he has Line of Sight. Movement related
    Skills (like +1 Zone per Move Action or Slippery) are ignored,
    but Movement penalties (like having Zombies in the starting
    Zone) apply. Ignore everything in the intervening Zone.*/
    Lifesaver, /* – The Survivor can use this Skill, for free, once during
    each of his Turns. Select a Zone containing at least one Zombie
    at Range 1 from your Survivor. Choose Survivors in the selected
    Zone to be dragged to your Survivor’s Zone without penalty.
    This is not a Move Action. A Survivor can decline the rescue and
    stay in the selected Zone if his controller chooses. Both Zones
    need to share a clear path. A Survivor can’t cross closed doors or
    walls, and can’t be extracted into or out of a Vault.*/
    Lock_it_down, /*– At the cost of one Action, the Survivor can
    close an open door in his Zone. Opening or destroying it
    again later does not trigger a new Zombie Spawn.*/
    Loud, /* – Once during each of his Turns, the Survivor can make a
    huge amount of noise! Until this Survivor’s next Turn, the Zone
    he used this Skill in is considered to have the highest number of
    Noise tokens on the entire board. If different Survivors have this
    Skill, only the last one who used it applies the effects.*/
    Low_profile, /* – The Survivor can’t get hit by Survivors’ Magic
    and Ranged Actions. Ignore him when casting a Combat spell
    or shooting in the Zone he stands in. Game effects that kill
    everything in the targeted Zone, like Dragon Fire, still kill
    him, though.*/
    Lucky, /* – The Survivor can re-roll once all the dice for each
    Action (or Armor roll) he takes. The new result takes the place
    of the previous one. This Skill stacks with the effects of other
    Skills and Equipment that allows re-rolls.*/
    Mana_rain, /* – When resolving a Magic Action, the Survivor
    may substitute the Dice number of the Combat spell(s) he uses
    with the number of Zombies standing in the targeted Zone.
    Skills affecting the dice value, like +1 die: Magic, still apply.*/
    Marksman, /* – The Survivor may freely choose the targets of
    all his Magic and Ranged Actions. Misses don’t hit Survivors.
    Matching set! – When a Survivor performs a Search Action
    and draws an Equipment card with the Dual symbol, he can
    immediately take a second card of the same type from the
    Equipment deck. Shuffle the deck afterward.*/
    Point_blank, /* – The Survivor can resolve Ranged and Magic
    Actions in his own Zone, no matter the minimum Range.
    When resolving a Magic or Ranged Action at Range 0, the
    Survivor freely chooses the targets and can kill any type of
    Zombies. His Combat spells and Ranged weapons still need
    to inflict enough Damage to kill his targets. Misses don’t hit
    Survivors.*/
    Reaper_Combat, /* – Use this Skill when assigning hits while
    resolving a Combat Action (Melee, Ranged or Magic). One
    of these hits can freely kill an additional identical Zombie
    in the same Zone. Only a single additional Zombie can be
    killed per Action when using this Skill. The Survivor gains
    the experience for the additional Zombie.*/
    Reaper_Magic, /* – Use this Skill when assigning hits while
    resolving a Magic Action. One of these hits can freely kill an
    additional identical Zombie in the same Zone. Only a single
    additional Zombie can be killed per Action when using this Skill.
    The Survivor gains the experience for the additional Zombie.*/
    Reaper_Melee, /* – Use this Skill when assigning hits while
    resolving a Melee Action. One of these hits can freely kill an
    additional identical Zombie in the same Zone. Only a single
    additional Zombie can be killed per Action when using this Skill.
    The Survivor gains the experience for the additional Zombie.*/
    Reaper_Ranged, /* – Use this Skill when assigning hits while
    resolving a Ranged Action. One of these hits can freely kill an
    additional identical Zombie in the same Zone. Only a single
    additional Zombie can be killed per Action when using this Skill.
    The Survivor gains the experience for the additional Zombie.*/
    Regeneration, /* – At the end of each Game Round, remove all
    Wounds the Survivor received. Regeneration doesn’t work if
    the Survivor has been eliminated.*/
    Roll_6_plus1_die_Combat, /*– You may roll an additional die for
    each “6” rolled on any Combat Action (Melee, Ranged or
    Magic). Keep on rolling additional dice as long as you keep
    getting “6”. Game effects that allow re-rolls (the Plenty Of
    Arrows Equipment card, for example) must be used before
    rolling any additional dice for this Skill.*/
    Roll_6_plus1_die_Magic, /* – You may roll an additional die for each
    “6” rolled on a Magic Action. Keep on rolling additional dice
    as long as you keep getting “6”. Game effects that allow rerolls
    must be used before rolling any additional dice for this
    Skill.*/
    Roll_6_plus1_die_Melee, /* – You may roll an additional die for each
    “6” rolled on a Melee Action. Keep on rolling additional dice
    as long as you keep getting “6”. Game effects that allow rerolls must
    be used before rolling any additional dice for this
    Skill.*/
    Roll_6_plus1_dies_Ranged, /* – You may roll an additional die for
    each “6” rolled on a Ranged Action. Keep on rolling additional
    dice as long as you keep getting “6”. Game effects that allow
    re-rolls (the Plenty Of Arrows Equipment card, for example)
    must be used before rolling any additional dice for this Skill.*/
    Rotten, /* – At the end of his Turn, if the Survivor
    has not resolved a Combat Action (Melee, Ranged or Magic) and not
    produced a Noise token, place a Rotten token next to his base. As
    long as he has this token, he is totally ignored
    by all Zombies and is not considered a Noise token. Zombies
    don’t attack him and will even walk past him. The Survivor
    loses his Rotten token if he resolves any kind of Combat
    Action (Melee, Ranged or Magic) or makes noise. Even with
    the Rotten token, the Survivor still has to spend extra Actions
    to move out of a Zone crowded with Zombies.*/
    Scavenger, /* – The Survivor can Search in any Zone. This includes
    street Zones, Vault Zones, etc.*/
    Search_plus1_card, /* – Draw an extra card when Searching with
    the Survivor.*/
    Shove, /* – The Survivor can use this Skill, for free, once during
    each of his Turns. Select a Zone at Range 1 from your
    Survivor. All Zombies standing in your Survivor’s Zone are
    pushed to the selected Zone. This is not a Movement. Both
    Zones need to share a clear path. A Zombie can’t cross closed
    doors, ramparts (see the Wulfsburg expansion) or walls, but
    can be shoved in or out of a Vault.*/
    Slippery, /* – The Survivor does not spend extra Actions when
    he performs a Move Action out of a Zone containing Zombies. Entering
    a Zone containing Zombies ends the Survivor’s
    Move Action.*/
    Spellbook, /* – All Combat spells and Enchantments in the Survivor’s
    Inventory are considered equipped in Hand. With this
    Skill, a Survivor could effectively be considered as having
    several Combat spells and Enchantments cards equipped in
    Hand. For obvious reasons, he can only use two identical dual
    Combat Spells at any given time. Choose any combination of
    two before resolving Actions or rolls involving the Survivor.*/
    Spellcaster, /* – The Survivor has one extra free Action. This Action
    may only be used for a Magic Action or an Enchantment Action.*/
    Sprint, /* – The Survivor can use this Skill once during each
    of his Turns. Spend one Move Action with the Survivor: He
    may move two or three Zones instead of one. Entering a Zone
    containing Zombies ends the Survivor’s Move Action.*/
    Super_strength, /* – Consider the Damage value of Melee weapons used by the Survivor to be 3. */
    Starts_with_a, /* [Equipment] – The Survivor begins the game
    with the indicated Equipment; its card is automatically assigned to him during Setup.*/
    Steady_hand, /* – The Survivor can ignore other Survivors of his
    choosing when missing with a Magic or Ranged Action. The
    Skill does not apply to game effects killing everything in the
    targeted Zone (such as a Dragon Fire, for example).*/
    Swordmaster, /* – The Survivor treats all Melee weapons as if
    they had the Dual symbol.*/
    Tactician, /* – The Survivor’s Turn can be resolved anytime
    during the Players’ Phase, before or after any other Survivor’s
    Turn. If several Survivors benefit from this Skill at the same
    time, choose their Turn order.*/
    Taunt, /* – The Survivor can use this Skill, for free, once during
    each of his Turns. Select a Zone your Survivor can see. All
    Zombies standing in the selected Zone immediately gain an
    extra Activation: They try to reach the taunting Survivor by
    any means available. Taunted Zombies ignore all other Survivors.
    They do not attack them and cross the Zone they stand
    in if needed to reach the taunting Survivor.*/
    Tough, /* – The Survivor ignores the first Wound he receives
    from a single Zombie every Zombies’ Phase.*/
    Trick_shot, /* – When the Survivor is equipped with Dual Combat
    spells or Ranged weapons, he can aim at different Zones
    with each spell/weapon in the same Action.*/
    Zombie_link, /* – The Survivor plays an extra Turn each time an
    Extra Activation card is drawn from the Zombie pile. He plays
    before the extra-activated Zombies. If several Survivors benefit
    from this Skill at the same time, choose their Turn order.*/
}
