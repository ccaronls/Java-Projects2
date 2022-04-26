package cc.lib.zombicide.quests;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZombieType;

/**
 * Created by Chris Caron on 8/27/21.
 */
public class WolfQuestImmortal extends ZQuest {

    static {
        addAllFields(WolfQuestImmortal.class);
    }

    List<ZSpawnArea> immortals = new ArrayList<>();
    int numStartImmortals = 0;

    public WolfQuestImmortal() {
        super(ZQuests.Immortal);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                {"z0:i:re", "z1:t3", "z1:t3:re", "z2:i:ods:we", "z3", "z4:blspe"},
                {"z5:t2:rn", "z1:t3:rs", "z1:t3:re:rs", "z6:i:ws:de", "z7", "z8:i:dw:wn:ods:red"},
                {"z5:t2:rs", "z9:t1:rs", "z10", "z11", "z12", "z13:i:ww"},

                {"z14:spw", "z15", "z16", "z17:t2:rw:rn", "z18:t3:rn", "z18:t3:rn"},
                {"z19:spw", "z20:v:wn:vd1:ws:we:ww", "z21:st", "z22:t1:rw", "z18:t3:rw:rs", "z18:t3:rs"},
                {"z23:spw", "z24", "z25", "z26", "z27", "z28"},

                {"z29:i:dn:we:ods", "z30", "z31:i:dn:de:ods:ww:red", "z32:i:dn", "z32:i:wn:we", "z33"},
                {"z34:i:ws:ode", "z35:i:ods:ode:wn", "z36:i:we:ods", "z32:i", "z32:i:red:we", "z40"},
                {"z37:i:vd1", "z37:i:we", "z38:i:de", "z32:i", "z32:i:we", "z39:blsps"}
        };

        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        ZSpawnArea area;
        switch (cmd) {
            case "blsps":
                setSpawnArea(grid.get(pos), area = new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.SOUTH, true, false, true));
                immortals.add(area);
                break;
            case "blspe":
                setSpawnArea(grid.get(pos), area = new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.EAST, true, false, true));
                immortals.add(area);
                break;

            default:
                super.loadCmd(grid, pos, cmd);
        }
    }

    @Override
    public ZTile[] getTiles() {

        return new ZTile[] {
                new ZTile("11R", 0, ZTile.getQuadrant(0, 0)),
                new ZTile("3V", 90, ZTile.getQuadrant(0, 3)),

                new ZTile("6R", 0, ZTile.getQuadrant(3, 0)),
                new ZTile("10V", 180, ZTile.getQuadrant(3, 3)),

                new ZTile("8V", 270, ZTile.getQuadrant(6, 0)),
                new ZTile("1V", 180, ZTile.getQuadrant(6, 3))
        };
    }

    @Override
    public void init(ZGame game) {
        numStartImmortals = immortals.size();
    }

    @Override
    public void onZombieSpawned(ZGame game, ZZombie zombie, int zone) {
        int idx = Utils.searchIndex(immortals, sp -> game.getBoard().getCell(sp.getCellPos()).getZoneIndex());
        if (idx >= 0) {
            switch (zombie.getType()) {
                case Necromancer:
                    ZSpawnArea area = immortals.remove(idx);
                    area.setIcon(ZIcon.SPAWN_GREEN);
                    area.setCanBeRemovedFromBoard(true);
                    area.setEscapableForNecromancers(false);
                    area.setCanSpawnNecromancers(false);
                    game.spawnZombies(idx);
                    return;
            }
        }
        super.onZombieSpawned(game, zombie, zone);
    }

    int getNumNecrosOnBoard(ZGame game) {
        return Utils.count(game.getBoard().getAllZombies(), z -> z.getType() == ZZombieType.Necromancer);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int total = getNumStartObjectives() + numStartImmortals + getNumNecrosOnBoard(game);
        int completed = getNumFoundObjectives() + (numStartImmortals - immortals.size());
        return completed*100 / total;
    }


    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                        .addRow("1.", "Collect all Objectives. Each objective gives a vault item.", String.format("%d of %d", getNumFoundObjectives(), getNumStartObjectives()))
                        .addRow("2.", "Purge the EVIL by removing the GREEN spawn areas. BLUE spawn areas become GREEN once a Necromancer is spawned.", String.format("%d of %d", numStartImmortals-immortals.size(), numStartImmortals))
                        .addRow("3.", "Random Vault weapon hidden in the Vault.", getNumFoundVaultItems() > 0)
                );
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        game.giftRandomVaultArtifact(c);
    }
}
