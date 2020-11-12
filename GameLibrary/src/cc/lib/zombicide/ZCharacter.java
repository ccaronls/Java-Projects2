package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.utils.Table;

public class ZCharacter extends ZActor implements Table.Model {

    static {
        addAllFields(ZCharacter.class);
    }

    public final static int MAX_BACKPACK_SIZE = 5;

    ZPlayerName name;
    int woundBar;
    int dangerBar;
    private List<ZActionType> actionsDoneThisTurn = new ArrayList<>();
    final List<ZSkill> allSkills = new ArrayList<>();
    final List<ZSkill> availableSkills = new ArrayList<>();

    ZEquipment [] backpack = new ZEquipment[MAX_BACKPACK_SIZE];
    ZEquipment leftHand, rightHand, body;
    int numBackpackItems = 0;

    public ZCharacter() {
        super(-1);
    }

    @Override
    void prepareTurn() {
        super.prepareTurn();
        actionsDoneThisTurn.clear();
        availableSkills.clear();
        availableSkills.addAll(allSkills);
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
    protected void performAction(ZActionType action, ZGame game) {
        for (ZSkill skill : availableSkills) {
            if (skill.modifyActionsRemaining(this, action, game)) {
                availableSkills.remove(skill);
                return;
            }
        }
        super.performAction(action, game);
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

    public ZWeapon[] getWeapons() {
        ZWeapon left = leftHand == null ? null : (leftHand instanceof ZWeapon ? (ZWeapon)leftHand : null);
        ZWeapon right = rightHand == null ? null : (rightHand instanceof ZWeapon ? (ZWeapon)rightHand : null);
        if (left == null && right == null)
            return new ZWeapon[0];
        if (left == null)
            return new ZWeapon[] { right };
        if (right == null)
            return new ZWeapon[] { left };
        return new ZWeapon[] { left, right };
    }

    public boolean isDualWeilding() {
        ZWeapon [] weapons = getWeapons();
        return weapons.length == 2 && weapons[0] == weapons[1] && weapons[0].canTwoHand;
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
                if (numBackpackItems < backpack.length) {
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
            case BACKPACK:
                if (numBackpackItems < backpack.length) {
                    backpack[numBackpackItems++] = e;
                    return ZEquipSlot.BACKPACK;
                }
                break;
        }
        if (body == null && Utils.linearSearch(name.alternateBodySlots, e) >= 0) {
            body = e;
            return ZEquipSlot.BODY;
        }

        return null;
    }


    public void removeEquipment(ZEquipment equipment, ZEquipSlot slot) {
        switch (slot) {
            case BODY:
                body = null;
                break;
            case BACKPACK: {
                int idx = Utils.linearSearch(backpack, equipment, numBackpackItems);
                assert(idx >= 0);
                backpack[idx] = backpack[--numBackpackItems];
                backpack[numBackpackItems] = null;
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
                backpack[numBackpackItems++] = equipment;
                break;
        }
        return prev;
    }

    public ZSkillLevel getSkillLevel() {
        if (dangerBar < 7)
            return ZSkillLevel.BLUE;
        if (dangerBar < 19)
            return ZSkillLevel.YELOW;
        if (dangerBar < 42)
            return ZSkillLevel.ORANGE;
        return ZSkillLevel.RED;
    }

    public String getDebugString() {
        String [] header = { "Left Hand", "Body", "Right Hand", "Backpack" };
        Object [][] data = {
                { leftHand, body, rightHand, backpack[0]},
                { null, null, null, backpack[1] },
                { "Wounds", woundBar, null, backpack[2] },
                { "Danger", dangerBar, null, backpack[3] },
                { "Skill", getSkillLevel(), null, backpack[4] }
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
        return numBackpackItems == backpack.length;
    }

    @Override
    public void drawInfo(AGraphics g, int width, int height) {
        g.drawString(getDebugString(), 0, 0);
    }

    public boolean canTrade() {
        return leftHand != null && rightHand != null && body != null && numBackpackItems != 0;
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
