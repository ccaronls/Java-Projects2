package cc.lib.zombicide.quests;

import java.util.Arrays;
import java.util.List;

import cc.lib.game.GColor;
import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZItemType;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;

public class ZQuestTheEvilTemple extends ZQuest {

    static {
        addAllFields(ZQuestTheEvilTemple.class);
    }

    int blueObjZone = -1;
    int greenObjZone = -1;
    int violetVaultZone = -1;
    int goldVaultZone = -1;
    ZDoor goldVaultDoor, violetVaultDoor;

    public ZQuestTheEvilTemple() {
        super(ZQuests.The_Evil_Temple);
    }

    @Override
    public ZTile[] getTiles() {
        return new ZTile[] {
                new ZTile("8R", 180, ZTile.getQuadrant(0, 0)),
                new ZTile("9V", 180, ZTile.getQuadrant(0, 3)),
                new ZTile("1V", 270, ZTile.getQuadrant(0, 6)),
                new ZTile("5V", 180, ZTile.getQuadrant(3, 0)),
                new ZTile("4R", 90, ZTile.getQuadrant(3, 3)),
                new ZTile("2R", 90, ZTile.getQuadrant(3, 6)),
        };
    }

    @Override
    public ZBoard loadBoard() {

        String [][] map = {
                { "z0:i:ds",        "z0:i:ws:ode",  "z1:i:red:ws:ode",      "z2:i:ws",          "z2:i:ws:de",       "z3:spn",           "z4:i:dw",          "z4:i:lvd2",        "z4:i" },
                { "z5:spw",         "z6",           "z7",                   "z8",               "z9",               "z10",              "z4:i:ww:ws",       "z4:i:abom:ws",          "z4:i:ws" },
                { "z11:i:dn:ods:lgvd1:we", "z12",   "z13:i:wn:ww:ods:ode",  "z14:i:wn:ws",      "z14:i:wn:we:ws",   "z15",              "z16",              "z17",              "z18:st" },
                { "z19:i:ww:ws:we", "z44",          "z45:i:ww:ws:ode",      "z20:i:red:ws",     "z20:i:ws:ode",     "z21:i:dn:ws:ode",  "z22:i:dn:ws:ode",  "z23:i:wn:ws:we",   "z24" },
                { "z25",            "z26",          "z27",                  "z28",              "z29",              "z30",              "z31",              "z32",              "z33" },
                { "z34:i:wn:ws:ode", "z35:i:dn:ws", "z35:i:wn:ws:ode",      "z36:i:wn:ws:red:ode", "z37:i:wn:ws",   "z37:i:wn:ws:de",   "z38:sps:ws",       "z39:i:ww:wn:ws:red:ode", "z40:i:dn:ws" },
                { "",                "",            "",                     "",                  "",                "z41:v:ww:gvd1",    "z41:v:we",         "z42:v:vd2",        "z42:v" }
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "lvd2":
                violetVaultZone = grid.get(pos).getZoneIndex();
                violetVaultDoor = new ZDoor(pos, ZDir.DESCEND, GColor.MAGENTA);
                super.loadCmd(grid, pos, "vd2");
                break;
            case "lgvd1":
                goldVaultZone = grid.get(pos).getZoneIndex();
                goldVaultDoor = new ZDoor(pos, ZDir.DESCEND, GColor.GOLD);
                super.loadCmd(grid, pos, "gvd1");
                break;
            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public int getPercentComplete(ZGame game) {
        return game.getNumKills(ZZombieType.Abomination) > 0 ? 100 : 0;
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == greenObjZone) {
            game.addLogMessage(c.name() + " has found the GREEN objective and opened the GOLD vault");
            game.unlockDoor(goldVaultDoor);
            greenObjZone = -1;
        } else if (c.getOccupiedZone() == blueObjZone) {
            game.addLogMessage(c.name() + " has found the BLUE objective and opened the VIOLET vault");
            game.unlockDoor(violetVaultDoor);
            blueObjZone = -1;
        }
    }

    @Override
    public List<ZEquipment> getInitVaultItems(int vaultZone) {
        if (vaultZone == goldVaultZone) {
            return Arrays.asList(ZItemType.TORCH.create(), ZItemType.DRAGON_BILE.create());
        } else if (vaultZone == violetVaultZone) {
            List<ZEquipment> all = getVaultItemsRemaining();
            Utils.shuffle(all);
            while (all.size() > 2)
                all.remove(0);
            return all;
        } else {
            return super.getInitVaultItems(vaultZone);
        }
    }

    @Override
    public void init(ZGame game) {
        while (blueObjZone == greenObjZone) {
            blueObjZone = Utils.randItem(getRedObjectives());
            greenObjZone = Utils.randItem(getRedObjectives());
        }
        game.lockDoor(goldVaultDoor);
        game.lockDoor(violetVaultDoor);
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Collect all objectives.", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                    .addRow("2.", "Unlock GOLD vault. Key hidden among RED objectives.", greenObjZone==-1)
                    .addRow("3.", "Unlock VIOLET vault. Key hidden among RED objectives.", blueObjZone==-1)
                    .addRow("4.", "Kill the Abomination.", game.getNumKills(ZZombieType.Abomination) > 0)
                );
    }
}
