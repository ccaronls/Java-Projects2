package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public final class ZCharacter extends ZActor {

    static {
        addAllFields(ZCharacter.class);
    }

    public final static int MAX_BACKPACK_SIZE = 5;
    public final static int MAX_WOUNDS = 4;

    ZPlayerName name;
    int woundBar;
    int dangerBar;
    boolean organizedThisTurn=false;
    private List<ZActionType> actionsDoneThisTurn = new ArrayList<>();
    final List<ZSkill> allSkills = new ArrayList<>();
    final List<ZSkill> availableSkills = new ArrayList<>();

    private final List<ZEquipment> backpack = new ArrayList<>();
    ZEquipment leftHand, rightHand, body;
    int userIndex=0;

    public ZCharacter() {
        super(-1);
    }

    @Override
    void prepareTurn() {
        super.prepareTurn();
        actionsDoneThisTurn.clear();
        availableSkills.clear();
        availableSkills.addAll(allSkills);
        organizedThisTurn = false;
    }

    @Override
    protected int getImageId() {
        return name.imageId;
    }

    @Override
    public String name() {
        return name.name();
    }

    @Override
    protected boolean performAction(ZActionType action, ZGame game) {
        for (ZSkill skill : availableSkills) {
            if (skill.modifyActionsRemaining(this, action, game)) {
                availableSkills.remove(skill);
                return true;
            }
        }
        switch (action) {
            case ORGANIZE:
                if (organizedThisTurn)
                    return true;
                organizedThisTurn = true;
                break;
        }
        if (action.oncePerTurn()) {
            actionsDoneThisTurn.add(action);
        }
        return super.performAction(action, game);
    }

    boolean tryOpenDoor(ZGame game) {
        List<ZWeapon> weapons = getWeapons();
        // order the weapons so that the best choice for door is in the front
        Collections.sort(weapons, new Comparator<ZWeapon>() {
            @Override
            public int compare(ZWeapon o1, ZWeapon o2) {
                // first order by % to open
                int v1 = o1.getOpenDoorValue();
                int v2 = o2.getOpenDoorValue();
                return Integer.compare(v2, v1);
            }
        });
        for (ZWeapon w : weapons) {
            if (w.canOpenDoor()) {
                if (w.type.openDoorsIsNoisy)
                    game.addNoise(occupiedZone, 1);
                if (w.type.meleeStats.dieRollToOpenDoor>1) {
                    Integer [] die = game.rollDice(1);
                    if (die[0] < w.type.meleeStats.dieRollToOpenDoor) {
                        game.getCurrentUser().showMessage(name() + " Failed to open the door with their " + w);
                        return false;
                    }
                }
                game.getCurrentUser().showMessage(name() + " Used their " + w + " to break open the door");
                return true;
            }
        }
        return false;
    }

    @Override
    protected int getActionsPerTurn() {
        int actions = 3;
        if (availableSkills.contains(ZSkill.Plus1_Action))
            actions++;
        return actions;
    }

    public boolean canUnjamDoor() {
        if (leftHand != null && leftHand.canOpenDoor())
            return true;
        if (rightHand != null && rightHand.canOpenDoor())
            return true;
        return false;
    }

    /**
     *
     * @return
     */
    public List<ZWeapon> getWeapons() {
        return (List)Utils.filterItems(object -> object instanceof ZWeapon, leftHand, rightHand, body);
    }

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    public List<ZArmor> getArmor() {
        return (List)Utils.filterItems(object -> object instanceof ZArmor, leftHand, rightHand, body);
    }

    /**
     *
     * @return
     */
    public boolean isDualWeilding() {
        List<ZWeapon> weapons = getWeapons();
        if (weapons.size() != 2)
            return false;
        if (!weapons.get(0).equals(weapons.get(1)))
            return false;
        if (weapons.get(0).type.canTwoHand)
            return true;
        if (weapons.get(0).isMelee() && availableSkills.contains(ZSkill.Swordmaster))
            return true;
        return false;
    }

    public int getNumBackpackItems() {
        return backpack.size();
    }

    public ZEquipment getBackpackItem(int index) {
        return backpack.get(index);
    }

    public List<ZEquipment> getBackpack() {
        return Collections.unmodifiableList(backpack);
    }

    public boolean canEquip(ZEquipment e) {
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    return true;
                }
                if (rightHand == null) {
                    return true;
                }
                break;
            case BODY:
                if (body == null) {
                    return true;
                }
                break;
            case BACKPACK:
                if (backpack.size() < MAX_BACKPACK_SIZE) {
                    return true;
                }
                break;
        }
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e.getType()) >= 0) {
            return true;
        }

        return false;
    }

    public ZEquipSlot equip(ZEquipment e) {
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    leftHand = e;
                    return ZEquipSlot.LEFT_HAND;
                }
                if (rightHand == null) {
                    rightHand = e;
                    return ZEquipSlot.RIGHT_HAND;
                }
                break;
            case BODY:
                if (body == null) {
                    body = e;
                    return ZEquipSlot.BODY;
                }
                break;
        }
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e) >= 0) {
            body = e;
            return ZEquipSlot.BODY;
        }

        if (backpack.size() < MAX_BACKPACK_SIZE) {
            backpack.add(e);
            return ZEquipSlot.BACKPACK;
        }

        assert(false);
        return null;
    }

    public ZEquipment removeEquipment(Object type, ZEquipSlot slot) {
        ZEquipment removed = null;
        switch (slot) {
            case BODY:
                removed = body;
                body = null;
                break;
            case BACKPACK: {
                int idx = backpack.indexOf(type);
                assert(idx >= 0);
                removed = backpack.remove(idx);
                break;
            }
            case LEFT_HAND:
                removed = leftHand;
                leftHand = null;
                break;
            case RIGHT_HAND:
                removed = rightHand;
                rightHand = null;
                break;
        }
        return removed;
    }

    public ZEquipment attachEquipment(ZEquipment equipment, ZEquipSlot slot) {
        ZEquipment prev = null;
        switch (slot) {
            case RIGHT_HAND:
                prev = rightHand;
                rightHand = equipment;
                break;
            case LEFT_HAND:
                prev = leftHand;
                leftHand = equipment;
                break;
            case BODY:
                prev = body;
                body = equipment;
                break;
            case BACKPACK:
                backpack.add(equipment);
                break;
        }
        return prev;
    }

    public ZSkillLevel getSkillLevel() {
        return ZSkillLevel.getLevel(dangerBar);
    }

    public String getDebugString(ZGame game) {

        /*

        Left Hand    |   Body    |    Right Hand    |    Stats    |     Skills
        -----------------------------------------------------------------------

        <left hand card? | <body card> | <right hand card> | <stats card> | <skills[0]>


         */

        Table info = new Table().setNoBorder().setPadding(0);

        for (ZEquipSlot slot : ZEquipSlot.values()) {
            info.addColumn(slot.name(), Arrays.asList(getSlotInfo(slot, game)));
        }

        Table stats = new Table().setNoBorder().setPadding(0);
        stats.addRow("Wounds", woundBar);
        ZSkillLevel sl = getSkillLevel();
        int ptsToNxt = sl.getPtsToNextLevel(dangerBar);
        stats.addRow("Skill", sl);
        stats.addRow("Exp", dangerBar);
        stats.addRow("Next level", ptsToNxt);
        stats.addRow("Dual\nWielding", isDualWeilding());

        info.addColumn("STATS", Arrays.asList(stats.toString()));
        Table skills = new Table().setNoBorder().addColumnNoHeader(availableSkills);
        info.addColumn("Skills", skills);

        return String.format("%s (%s) moves: %d/%d Body:%s Actions:%s\n%s",
                name.name(), name.characterClass,
                getActionsLeftThisTurn(), getActionsPerTurn(),
                Arrays.toString(name.alternateBodySlots),
                actionsDoneThisTurn,
                info.toString());
    }

    public boolean isBackpackFull() {
        return backpack.size() == MAX_BACKPACK_SIZE;
    }

    @Override
    public GDimension drawInfo(AGraphics g, ZGame game, int width, int height) {
        return g.drawString(getDebugString(game), 0, 0);
    }

    public boolean canTrade() {
        return backpack.size() > 0;
    }

    public boolean canSearch() {
        return !isBackpackFull() && !actionsDoneThisTurn.contains(ZActionType.SEARCH);
    }

    public List<ZEquipSlot> getEquipableSlots(ZEquipment equip) {
        List<ZEquipSlot> options = new ArrayList<>();
        switch (equip.getSlotType()) {
            case BODY:
                return Arrays.asList(ZEquipSlot.BODY);
            case HAND:
                options.add(ZEquipSlot.LEFT_HAND);
                options.add(ZEquipSlot.RIGHT_HAND);
                if (Utils.linearSearch(name.alternateBodySlots, equip.getType()) >= 0) {
                    options.add(ZEquipSlot.BODY);
                }
                break;
        }
        return options;
    }

    public List<ZEquipSlot> getMeleeWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (isDualWeilding()) {
            slots.add(ZEquipSlot.LEFT_HAND);
        } else {
            if (leftHand != null && leftHand.isMelee())
                slots.add(ZEquipSlot.LEFT_HAND);
            if (rightHand != null && rightHand.isMelee())
                slots.add(ZEquipSlot.RIGHT_HAND);
        }
        if (body != null && body.isMelee())
            slots.add(ZEquipSlot.BODY);
        return slots;
    }

    public List<ZEquipSlot> getRangedWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (isDualWeilding()) {
            slots.add(ZEquipSlot.LEFT_HAND);
        } else {
            if (leftHand != null && leftHand.isRanged() && isLoaded(leftHand))
                slots.add(ZEquipSlot.LEFT_HAND);
            if (rightHand != null && rightHand.isRanged() && isLoaded(rightHand))
                slots.add(ZEquipSlot.RIGHT_HAND);
        }
        if (body != null && body.isRanged() && isLoaded(body))
            slots.add(ZEquipSlot.BODY);
        return slots;
    }

    public List<ZEquipSlot> getMagicWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (isDualWeilding()) {
            slots.add(ZEquipSlot.LEFT_HAND);
        } else {
            if (leftHand != null && leftHand.isMagic())
                slots.add(ZEquipSlot.LEFT_HAND);
            if (rightHand != null && rightHand.isMagic())
                slots.add(ZEquipSlot.RIGHT_HAND);
        }
        if (body != null && body.isMagic())
            slots.add(ZEquipSlot.BODY);
        return slots;
    }

    public List<ZEquipSlot> getThrowableItems() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isThrowable())
            slots.add(ZEquipSlot.LEFT_HAND);
        if (rightHand != null && rightHand.isThrowable())
            slots.add(ZEquipSlot.RIGHT_HAND);
        if (body != null && body.isThrowable())
            slots.add(ZEquipSlot.BODY);
        return slots;
    }


    public boolean canReload() {
        return false;
    }

    public boolean isLoaded(ZEquipment weapon) {
        return true;
    }

    public ZWeaponStat getWeaponStat(ZEquipSlot slot, ZActionType attackType, ZGame game) {
        return getWeaponStat(getSlot(slot), attackType, game);
    }

    public <T extends ZEquipment> T getSlot(ZEquipSlot slot) {
        switch (slot) {
            case BODY:
                return (T)body;
            case RIGHT_HAND:
                return (T)rightHand;
            case LEFT_HAND:
                return (T)leftHand;
        }
        return null;
    }

    public ZWeaponStat getWeaponStat(ZWeapon weapon, ZActionType attackType, ZGame game) {
        ZWeaponStat stat = null;
        switch (attackType) {
            case MELEE:
                if (!weapon.isMelee())
                    return null;
                stat = weapon.type.meleeStats.copy();
                break;
            case RANGED:
                if (!weapon.isRanged())
                    return null;
                stat = weapon.type.rangedStats.copy();
                break;
            case MAGIC:
                if (!weapon.isMagic())
                    return null;
                stat = weapon.type.magicStats.copy();
                break;
            default:
                return null;
        }
        if (isDualWeilding()) {
            stat.numDice*=2;
        }
        for (ZSkill skill : availableSkills) {
            skill.modifyStat(stat, attackType, this, game);
        }
        return stat;
    }

    /**
     *
     * @param type
     * @return
     */
    public int getArmorRating(ZZombieType type) {
        int rating = 0;
        for (ZArmor armor : getArmor()) {
            rating += armor.getRating(type);
        }
        return rating;
    }

    /**
     *
     * @return
     */
    public boolean isDead() {
        return woundBar >= MAX_WOUNDS;
    }

    @Override
    public int getNoise() {
        return 1;
    }

    /**
     *
     * @return
     */
    public boolean canShove() {
        if (getArmor().contains(ZArmorType.SHIELD_OF_AGES))
            return true;
        if (availableSkills.contains(ZSkill.Shove))
            return true;
        return false;
    }

    String getSlotInfo(ZEquipSlot slot, ZGame game) {
        switch (slot) {
            case RIGHT_HAND:
                if (rightHand == null)
                    return null;
                return rightHand.getCardString(this, game);
            case LEFT_HAND:
                if (leftHand == null)
                    return null;
                return leftHand.getCardString(this, game);
            case BODY:
                if (body == null) {
                    return null;
                }
                return body.getCardString(this, game);
            case BACKPACK: {
                Table table = new Table().setNoBorder();
                for (ZEquipment e : backpack) {
                    table.addRow(e);
                }
                return table.toString();
            }
        }
        return null;
    }

    public List<ZEquipment> getAllEquipment() {
        List<ZEquipment> all = Utils.filterItems(object -> true, leftHand, body, rightHand);
        all.addAll(backpack);
        return all;
    }

    public void removeEquipment(ZEquipment equip) {
        if (leftHand == equip) {
            leftHand = null;
        } else if (rightHand == null) {
            rightHand = null;
        } else if (body == equip) {
            body = null;
        } else {
            backpack.remove(equip);
        }
    }

    /*
    public boolean canEnchant() {
        if (leftHand != null && leftHand.isEnchantment() && !)
            return true;
        if (rightHand != null && rightHand.isEnchantment())
            return true;
        if (body != null && body.isEnchantment())
            return true;

    }*/
}
