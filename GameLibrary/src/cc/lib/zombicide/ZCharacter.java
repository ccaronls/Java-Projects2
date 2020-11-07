package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public class ZCharacter extends ZActor {
    ZPlayerName name;
    int woundBar;
    int dangerBar;
    int skillLevel;
    int occupiedZone;
    List<ZMove> movesDoneThisTurn = new ArrayList<>();

    List<ZEquipment> backpack = new ArrayList<>();
    ZEquipment leftHand, rightHand, body;

    @Override
    void prepareTurn() {
        super.prepareTurn();
        movesDoneThisTurn.clear();
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

}
