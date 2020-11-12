package cc.lib.zombicide;

import java.util.ArrayList;
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
    List<ZMove> movesDoneThisTurn = new ArrayList<>();
    List<ZSkill> skills = new ArrayList<>();

    ZEquipment [] backpack = new ZEquipment[MAX_BACKPACK_SIZE];
    ZEquipment leftHand, rightHand, body;
    int numBackpackItems = 0;

    public ZCharacter() {
        super(-1);
    }

    @Override
    void prepareTurn() {
        super.prepareTurn();
        movesDoneThisTurn.clear();
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
    protected void performAction(ZActionType action) {
        super.performAction(action);
    }

    @Override
    protected int getActionsPerTurn() {
        int actions = 3;
        if (skills.contains(ZSkill.Plus1_Action))
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
        switch (e.getSlot()) {
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
        switch (e.getSlot()) {
            case HAND:
                if (leftHand == null) {
                    leftHand = e;
                    return ZEquipSlot.HAND;
                }
                if (rightHand == null) {
                    rightHand = e;
                    return ZEquipSlot.HAND;
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

    public void unequip(ZEquipment equip) {
        if (equip == leftHand)
            leftHand = null;
        if (equip == rightHand)
            rightHand = null;
        if (equip == body)
            body = null;
        if (!isBackpackFull())
            backpack[numBackpackItems++] = equip;
    }

    public void dispose(ZEquipment equip) {
        if (leftHand == equip) {
            leftHand = null;
        }
        else if (rightHand == equip)
            rightHand = null;
        else if (body == equip)
            body = null;
        else for (int i=0; i<numBackpackItems; i++)
            if (backpack[i] == equip) {
                backpack[i] = backpack[--numBackpackItems];
                backpack[numBackpackItems] = null;
                break;
            }
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
        String str = name + " (" + name.characterClass + ")\n" + table.toString();
        str += "\nSkills: " + skills;
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
}
