package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public class ZCharacter extends ZActor implements Table.Model {

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
                    return false;
                organizedThisTurn = true;
                break;
            case OPEN_DOOR: {
                super.performAction(action, game);
                for (ZWeapon w : getWeapons()) {
                    if (w.canOpenDoor()) {
                        if (w.openDoorsIsNoisy)
                            game.addNoise(occupiedZone, 1);
                        if (w.meleeStats.dieRollToOpenDoor>1) {
                            int [] die = game.rollDice(1);
                            if (die[0] > w.meleeStats.dieRollToOpenDoor) {
                                return false;
                            }
                        }
                        game.getCurrentUser().showMessage(name() + " Used their " + w + " to break open the door");
                        return true;
                    }
                }
                return false;
            }
        }
        return super.performAction(action, game);
    }

    @Override
    protected int getActionsPerTurn() {
        int actions = 3;
        if (availableSkills.contains(ZSkill.Plus1_Action))
            actions++;
        return actions;
    }

    public boolean canOpenDoor() {
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
        return Utils.filterItems(object -> object instanceof ZWeapon, leftHand, rightHand, body);
    }

    /**
     * Return any armor and / or shileds player has
     * @return
     */
    public List<ZArmor> getArmor() {
        return Utils.filterItems(object -> object instanceof ZArmor, leftHand, rightHand, body);
    }

    /**
     *
     * @return
     */
    public boolean isDualWeilding() {
        List<ZWeapon> weapons = Utils.filterItems(object -> object instanceof ZWeapon, leftHand, rightHand);
        if (weapons.size() != 2)
            return false;
        if (!weapons.get(0).equals(weapons.get(1)))
            return false;
        if (weapons.get(0).canTwoHand)
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
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e) >= 0) {
            return true;
        }

        return false;
    }

    public ZEquipSlot equip(ZEquipment e) {
        switch (e.getSlotType()) {
            case HAND:
                if (leftHand == null) {
                    leftHand = e;
                    return ZEquipSlot.LHAND;
                }
                if (rightHand == null) {
                    rightHand = e;
                    return ZEquipSlot.RHAND;
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


    public void removeEquipment(ZEquipment equipment, ZEquipSlot slot) {
        switch (slot) {
            case BODY:
                body = null;
                break;
            case BACKPACK: {
                int idx = backpack.indexOf(equipment);
                assert(idx >= 0);
                backpack.remove(idx);
                break;
            }
            case LHAND:
                leftHand = null;
                break;
            case RHAND:
                rightHand = null;
                break;
        }
    }

    public ZEquipment attachEquipment(ZEquipment equipment, ZEquipSlot slot) {
        ZEquipment prev = null;
        switch (slot) {
            case RHAND:
                prev = rightHand;
                rightHand = equipment;
                break;
            case LHAND:
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

    public String getDebugString() {
        ZSkillLevel sl = getSkillLevel();
        int ptsToNxt = sl.getPtsToNextLevel(dangerBar);
        String [] header = { "Left Hand", "Body", "Right Hand", "Backpack" };
        Object [][] data = {
                { leftHand, body, rightHand, backpack.size() > 0 ? backpack.get(0) : null },
                { null, null, null, backpack.size() > 1 ? backpack.get(1) : null  },
                { "Wounds", woundBar, null, backpack.size() > 2 ? backpack.get(2) : null  },
                { "Danger", dangerBar, null, backpack.size() > 3 ? backpack.get(3) : null  },
                { "Skill", sl + (ptsToNxt > 0 ? (" (" + ptsToNxt + ")") : ""), null, backpack.size() > 4 ? backpack.get(4) : null  }
        };
        Table table = new Table(header, data, this);
        String str = name + " (" + name.characterClass + ") Actions Left:" + getActionsLeftThisTurn() + "\n" + table.toString();
        str += "\nSkills: ";
        String delim = "";
        for (ZSkill skill : allSkills) {
            str += delim + skill.name();
            if (!availableSkills.contains(skill)) {
                str += "(Used)";
            }
            delim = ", ";
        }
        return str;
    }

    @Override
    public int getTextAlignment(int row, int col) {
        if (col == 1 && row > 1) {
            return 2;
        }
        return 0;
    }

    public boolean isBackpackFull() {
        return backpack.size() == MAX_BACKPACK_SIZE;
    }

    @Override
    public void drawInfo(AGraphics g, int width, int height) {
        g.drawString(getDebugString(), 0, 0);
    }

    public boolean canTrade() {
        return leftHand != null || rightHand != null || body != null || backpack.size() > 0;
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
                options.add(ZEquipSlot.LHAND);
                options.add(ZEquipSlot.RHAND);
                if (Utils.linearSearch(name.alternateBodySlots, equip) >= 0) {
                    options.add(ZEquipSlot.BODY);
                }
                break;
        }
        return options;
    }

    public List<ZEquipSlot> getMeleeWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isMelee())
            slots.add(ZEquipSlot.LHAND);
        if (rightHand != null && rightHand.isMelee())
            slots.add(ZEquipSlot.RHAND);
        if (body != null && body.isMelee())
            slots.add(ZEquipSlot.BODY);
        return slots;
    }

    public List<ZEquipSlot> getRangedWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isRanged() && isLoaded(leftHand))
            slots.add(ZEquipSlot.LHAND);
        if (rightHand != null && rightHand.isRanged() && isLoaded(rightHand))
            slots.add(ZEquipSlot.RHAND);
        if (body != null && body.isRanged() && isLoaded(body))
            slots.add(ZEquipSlot.BODY);
        return slots;
    }

    public List<ZEquipSlot> getMagicWeapons() {
        List<ZEquipSlot> slots = new ArrayList<>();
        if (leftHand != null && leftHand.isMagic())
            slots.add(ZEquipSlot.LHAND);
        if (rightHand != null && rightHand.isMagic())
            slots.add(ZEquipSlot.RHAND);
        if (body != null && body.isMagic())
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
        ZWeapon weapon = (ZWeapon)getSlot(slot);
        ZWeaponStat stat = null;
        switch (attackType) {
            case MELEE:
                stat = weapon.meleeStats.copy();
                break;
            case RANGED:
                stat = weapon.rangedStats.copy();
                break;
            case MAGIC:
                stat = weapon.magicStats.copy();
                break;
        }
        for (ZSkill skill : availableSkills) {
            skill.modifyStat(stat, attackType, this, game);
        }
        return stat;
    }

    public ZEquipment getSlot(ZEquipSlot slot) {
        switch (slot) {
            case BODY:
                return body;
            case RHAND:
                return rightHand;
            case LHAND:
                return leftHand;
        }
        return null;
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
        if (getArmor().contains(ZArmor.SHIELD_OF_AGES))
            return true;
        if (availableSkills.contains(ZSkill.Shove))
            return true;
        return false;
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
