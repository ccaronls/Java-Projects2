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
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSkillLevel;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZSpawnCard;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombie;

/**
 * Created by Chris Caron on 9/1/21.
 */
public class WolfTheZombieArmy extends ZQuest {

    static {
        addAllFields(WolfTheZombieArmy.class);
    }

    int blueObjZone = -1;
    int greenObjZone = -1;
    int startDeckSize=0;

    List<ZSpawnCard> spawnDeck = new ArrayList<>();

    final static int MAX_SPAWN_ZONES = 6;

    public WolfTheZombieArmy() {
        super(ZQuests.The_Zombie_Army);
    }

    @Override
    public ZBoard loadBoard() {
        String [][] map = {
                { "z0", "z1", "z2:st",                          "z10:i:ww","z10:i:xspn","z10:i"   },
                { "z3:xspw", "z4:i:wn:ws:we:ww", "z5",           "z10:i:dw:ws", "z10:i:ws","z10:i:ws"  },
                { "z6","z7","z8",                               "z11","z12","z13"  },

                { "z20:i:odn:ws:xspw","z20:i:wn:we:ws","z21",    "z30:t1:rn","z31:t2:rn:re","z32:t3:rn:red"   },
                { "z22","z23","z24",                             "z33:t3:rw:rn:red","z34:t3:red","z35:t3:red"   },
                { "z25:i:wn:xspw","z25:i:dn:we","z26",            "z36:t3:rw:red","z37:t3:rw:red:odn:odw:ode","z38:t3:red"    }
        };


        return load(map);
    }

    @Override
    protected void loadCmd(Grid<ZCell> grid, Grid.Pos pos, String cmd) {
        switch (cmd) {
            case "xspn":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.NORTH, true, true, false));
                break;
            case "xsps":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.SOUTH, true, true, false));
                break;
            case "xspe":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.EAST, true, true, false));
                break;
            case "xspw":
                setSpawnArea(grid.get(pos), new ZSpawnArea(pos, ZIcon.SPAWN_BLUE, ZDir.WEST, true, true, false));
                break;
            default:
                super.loadCmd(grid, pos, cmd);

        }
    }

    @Override
    public ZTile[] getTiles(ZBoard board) {
        return new ZTile[] {
                new ZTile("6R", 0, ZTile.getQuadrant(0, 0, board)),
                new ZTile("1V", 270, ZTile.getQuadrant(0, 3, board)),

                new ZTile("9V", 180, ZTile.getQuadrant(3, 0, board)),
                new ZTile("10R", 90, ZTile.getQuadrant(3, 3, board))
        };
    }

    @Override
    public void init(ZGame game) {
        while (blueObjZone == greenObjZone) {
            blueObjZone = Utils.randItem(getRedObjectives());
            greenObjZone = Utils.randItem(getRedObjectives());
        }
        switch (game.getDifficulty()) {
            default:
            case EASY:
                startDeckSize = 20; break;
            case MEDIUM:
                startDeckSize = 30; break;
            case HARD:
                startDeckSize = 40; break;
        }
        for (int i=0; i<startDeckSize; i++)
            spawnDeck.add(ZSpawnCard.drawSpawnCard(isWolfBurg(), true, game.getDifficulty()));
    }

    @Override
    public ZSpawnCard drawSpawnCard(ZGame game, int targetZone, ZSkillLevel dangerLevel) {
        if (spawnDeck.isEmpty())
            return null;
        return spawnDeck.remove(spawnDeck.size()-1);
    }

    @Override
    public int getPercentComplete(ZGame game) {
        int numZombies = game.getBoard().getAllZombies().size();
        int total = 4 * startDeckSize + numZombies;
        int completed = 4 * (startDeckSize-spawnDeck.size());
        return completed * 100 / total;
    }

    @Override
    public void processObjective(ZGame game, ZCharacter c) {
        super.processObjective(game, c);
        if (c.getOccupiedZone() == greenObjZone) {
            greenObjZone = -1;
            game.chooseVaultItem();
        } else if (c.getOccupiedZone() == blueObjZone) {
            blueObjZone = -1;
            game.chooseVaultItem();
        } else {
            game.chooseEquipmentFromSearchables();
            c.addAvailableSkill(ZSkill.Inventory);
        }
    }

    int getNumSpawnZones(ZGame game) {
        return Utils.sumInt(game.getBoard().getCells(), c -> c.getSpawnAreas().size());
    }

    @Override
    public Table getObjectivesOverlay(ZGame game) {
        return new Table(getName())
                .addRow(new Table().setNoBorder()
                    .addRow("1.", "Destroy the Zombie Army. All Spawn cards must be depleted and all zombies destroyed in order to complete the quest", String.format("%d of %d cards left", startDeckSize-spawnDeck.size(), startDeckSize))
                    .addRow("2.", "Find the GREEN objective hidden among the RED objetives. Choose a Vault item of your choice", greenObjZone < 0)
                    .addRow("3.", "Find the BLUE objective hidden among the RED objetives. Choose a Vault item of your choice", blueObjZone < 0)
                    .addRow("4.", "Taking a RED objectives allows survivor to take and item from deck and reorganize their inventory for free" )
                    .addRow("5.", "Spawn areas cannot be removed after killing a Necromancer, even those created by Necromancers. Max of " + MAX_SPAWN_ZONES + " spawn areas on the board", String.format("%d of %d", getNumSpawnZones(game), MAX_SPAWN_ZONES))
                );
    }

    @Override
    public void onZombieSpawned(ZGame game, ZZombie zombie, int zone) {
        switch (zombie.getType()) {
            case Necromancer: {
                if (getNumSpawnZones(game) < MAX_SPAWN_ZONES) {
                    game.getBoard().setSpawnZone(zone, ZIcon.SPAWN_GREEN, false, false, false);
                    game.spawnZombies(zone);
                }
                break;
            }
        }

    }
}
