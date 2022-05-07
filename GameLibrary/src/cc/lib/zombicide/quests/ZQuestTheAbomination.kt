package cc.lib.zombicide.quests;

import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZEquipmentType;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWeaponType;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTheAbomination extends ZQuest {

    static {
        addAllFields(ZQuestTheAbomination.class);
    }

    public ZQuestTheAbomination() {
        super(ZQuests.The_Abomination);
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:red:st:ode:ds", "z1",        "z2",               "z3", "z4:i:red:st:odw:ds" },
                { "z5","z6",                    "z7:abom",          "z8", "z9" },
                { "z10:spw","z11",                  "z12",               "z13", "z14:spe" },
                { "z15","z16",         "z17:t3:rn:re:rw",             "z18", "z19" },
                { "z20:i:red:st:dn:ode","z21",       "z22",           "z23", "z24:i:red:st:dn:odw" }
        };

        return load(map);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Abomination) > 0 ? 100 : 0;
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[0];
    }

    @Override
    public void init(ZGame game) {

    }

    @Override
    protected int getObjectiveExperience(int zoneIdx, int nthFound) {
        return 20;
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (getVaultItemsRemaining().size() > 0) {
            game.giftEquipment(c, getVaultItemsRemaining().remove(0));
        }
    }

    @Override
    public List<ZEquipmentType> getAllVaultOptions() {
        return Utils.toList(ZItemType.DRAGON_BILE, ZItemType.DRAGON_BILE, ZWeaponType.CHAOS_LONGBOW, ZWeaponType.VAMPIRE_CROSSBOW, ZWeaponType.INFERNO, ZWeaponType.ORCISH_CROSSBOW, ZWeaponType.BASTARD_SWORD, ZWeaponType.EARTHQUAKE_HAMMER);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
            .addRow(new Table().setNoBorder()
                .addRow("Kill the Abomination")
            );
    }
}
