package cc.lib.zombicide.p2p;

import java.util.List;

import cc.lib.net.ClientConnection;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipSlot;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZItem;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZSpell;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;

/**
 * Created by Chris Caron on 7/17/21.
 */
public class ZUserMP extends ZUser {

    final ClientConnection connection;

    public static final String USER_ID = "ZUser";

    public ZUserMP(ClientConnection conn) {
        this.connection = conn;
    }

    @Override
    public ZPlayerName chooseCharacter(List<ZPlayerName> options) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, options);
        }
        return null;
    }

    @Override
    public ZMove chooseMove(ZPlayerName cur, List<ZMove> options) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, options);
        }
        return null;
    }

    @Override
    public ZSkill chooseNewSkill(ZPlayerName character, List<ZSkill> skillOptions) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, character, skillOptions);
        }
        return null;
    }

    @Override
    public ZEquipSlot chooseSlotToOrganize(ZPlayerName cur, List<ZEquipSlot> slots) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, slots);
        }
        return null;
    }

    @Override
    public ZEquipment chooseEquipment(ZPlayerName cur, List<ZEquipment> equipOptions) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, equipOptions);
        }
        return null;
    }

    @Override
    public ZEquipSlot chooseSlotForEquip(ZPlayerName cur, List<ZEquipSlot> equipableSlots) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, equipableSlots);
        }

        return null;
    }

    @Override
    public Integer chooseZoneToWalk(ZPlayerName cur, List<Integer> zones) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, zones);
        }
        return null;
    }

    @Override
    public ZDoor chooseDoorToToggle(ZPlayerName cur, List<ZDoor> doors) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, doors);
        }

        return null;
    }

    @Override
    public ZWeapon chooseWeaponSlot(ZPlayerName c, List<ZWeapon> weapons) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, c, weapons);
        }
        return null;
    }

    @Override
    public ZPlayerName chooseTradeCharacter(ZPlayerName c, List<ZPlayerName> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, c, list);
        }
        return null;
    }

    @Override
    public Integer chooseZoneForAttack(ZPlayerName c, List<Integer> zones) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, c, zones);
        }
        return null;
    }

    @Override
    public ZEquipment chooseItemToPickup(ZPlayerName cur, List<ZEquipment> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }

        return null;
    }

    @Override
    public ZEquipment chooseItemToDrop(ZPlayerName cur, List<ZEquipment> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }
        return null;
    }

    @Override
    public ZItem chooseItemToThrow(ZPlayerName cur, List<ZItem> slots) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, slots);
        }

        return null;
    }

    @Override
    public Integer chooseZoneToThrowItem(ZPlayerName cur, ZItem toThrow, List<Integer> zones) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, toThrow, zones);
        }

        return null;
    }

    @Override
    public Integer chooseZoneToShove(ZPlayerName cur, List<Integer> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }

        return null;
    }

    @Override
    public ZSpell chooseSpell(ZPlayerName cur, List<ZSpell> spells) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, spells);
        }
        return null;
    }

    @Override
    public ZPlayerName chooseCharacterForSpell(ZPlayerName cur, ZSpell spell, List<ZPlayerName> targets) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, spell, targets);
        }
        return null;
    }

    @Override
    public ZPlayerName chooseCharacterToBequeathMove(ZPlayerName cur, List<ZPlayerName> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }
        return null;
    }

    @Override
    public Integer chooseZoneForBloodlust(ZPlayerName cur, List<Integer> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }

        return null;
    }

    @Override
    public Integer chooseSpawnAreaToRemove(ZPlayerName cur, List<ZSpawnArea> list) {
        if (connection != null) {
            return connection.executeDerivedOnRemote(USER_ID, true, cur, list);
        }

        return null;
    }

}
