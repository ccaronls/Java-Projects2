package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.List;

public class ZGame {

    public enum State {
        INIT,
        BEGIN_ROUND, // reset enchantments
        SPAWN,
        PLAYER_STAGE_CHOOSE_CHARACTER,
        PLAYER_STAGE_CHOOSE_CHARACTER_ACTION,
        ZOMBIE_STAGE,
    }

    private State state = State.INIT;

    private ZBoard board;

    private List<ZUser> users = new ArrayList<>();
    private List<ZZombie> zombies = new ArrayList<>();
    int currentUser;
    int currentCharacter;
    int currentAction;

    public void runGame() {
        switch (state) {
            case INIT: {
                // Choose Quest
                // Choose Map
                // Position doors and objectives as determined by the quest
                // choose characters for each user
                users.get(0).prepareTurn();
                break;
            }

            case SPAWN: {
                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                int highestSkill = 0;
                for (ZUser u : users) {
                    for (ZCharacter c : u.charactoers) {
                        highestSkill = Math.max(highestSkill, c.skillLevel);
                    }
                }
                for (ZZone z : board.getZones()) {
                    if (z.isSpawn) {
                        spawnZombies(z, highestSkill);
                    }
                }
                break;
            }

            case  PLAYER_STAGE_CHOOSE_CHARACTER: {
                // for each user, they choose each of their characters in any order and have them
                // perform all of their actions
                ZUser user = getCurrentUser();
                List<Integer> options = new ArrayList<>();
                for (int i = 0; i<user.charactoers.size(); i++) {
                    ZCharacter c = user.charactoers.get(i);
                    if (c.actionsLeftThisTurn > 0) {
                        options.add(i);
                    }
                }

                if (options.size() == 0) {
                    if (++currentUser >= users.size()) {
                        currentUser = 0;
                        state = State.ZOMBIE_STAGE;
                    }
                    getCurrentUser().prepareTurn();
                    break;
                }

                currentCharacter = -1;
                currentCharacter = users.get(currentUser).chooseCharacter(options);
                if (currentCharacter >= 0) {
                    state = State.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION;
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION: {
                List<ZMove> options = new ArrayList<>();
                options.add(ZMove.newDoNothing());
                options.add(ZMove.newGoBack(State.PLAYER_STAGE_CHOOSE_CHARACTER));
                // determine players available moves
                ZCharacter cur = getCurrentCharacter();
                // check for organize
                for (ZEquipment e : cur.backpack) {
                    if (e.getSlot() != ZEquipSlot.BACKPACK) {
                        options.add(ZMove.newOrganizeMove());
                        break;
                    }
                }

                int zoneIndex = cur.zoneIndex;
                ZZone zone = board.getZone(zoneIndex);

                // check for trade with another character in the same zone
                for (ZCharacter c : getAllCharacters()) {
                    if (c == cur)
                        continue;
                    if (c.occupiedZone == zoneIndex) {
                        options.add(ZMove.newTradeMove(c));
                    }
                }

                // check for search
                if (!cur.movesDoneThisTurn.contains(ZMoveType.SEARCH) && isClearedOfZombies(zoneIndex)) {
                    options.add(ZMove.newSearchMove(zoneIndex));
                }


                // check for walking / open doors
                for (int zIndex : board.getAccessableZones(zoneIndex, 1)) {
                    options.add(ZMove.newWalkMove(zIndex));
                }
                /*
                for (ZWall wall : board.getZoneWalls(zone)) {
                    if (!wall.hasDoor )
                        continue;
                    int adjIndex = board.getAdjacentZoneIndex(zoneIndex, wall);
                    assert(adjIndex >= 0);
                    if (wall.doorOpen) {
                        options.add(ZMove.newWalkMove(adjIndex));
                    } else {
                        if (cur.canOpenDoor())
                            options.add(ZMove.newOpenDoor(adjIndex));
                    }
                }*/

                for (ZWeapon w : getCurrentCharacter().getWeapons()) {
                    addAttackOptions(zoneIndex, w.getMeleeStats(cur), w, options, true);
                    addAttackOptions(zoneIndex, w.getRangedStats(cur), w, options, false);
                    addAttackOptions(zoneIndex, w.getMagicStats(cur), w, options, false);
                    if (cur.isDualWeilding())
                        break;
                }

                // check for enchanting self or another

                // check for eating / backpack

                // check for open check /

                break;
            }

        }
    }

    private Iterable<ZCharacter> getAllCharacters() {
        if (users.size() == 1)
            return users.get(0).charactoers;
        List<ZCharacter> all = new ArrayList<>();
        for (ZUser user : users) {
            all.addAll(user.charactoers);
        }
        return all;
    }

    private void addAttackOptions(int zoneIndex, ZWeaponStat stats, ZWeapon weapon, List<ZMove> options, boolean melee) {
        if (stats != null) {
            for (int i=stats.minRange; i<= stats.maxRange; i++) {
                for (int zombieIndex : getZombiesInRange(zoneIndex, i)) {
                    ZZombie zombie = zombies.get(zombieIndex);
                    if (zombie.minDamage <= stats.damagePerHit) {
                        if (melee)
                            options.add(ZMove.newMeleeAttackMove(zombieIndex, weapon));
                        else
                            options.add(ZMove.newRangedAttackMove(zombieIndex, weapon));
                    }
                }
            }
        }

    }

    private List<Integer> getZombiesInRange(int zoneIndex, int distance) {
        if (distance == 0) {
            return getZombiesInZone(zoneIndex);
        }

        List<Integer> list = new ArrayList<>();
        ZZone zone = board.getZone(zoneIndex);

        List<Integer> zones = board.getAccessableZones(zoneIndex, distance);
        for (int z : zones) {
            list.addAll(getZombiesInZone(z));
        }
        return list;
    }

    private List<Integer> getZombiesInZone(int zoneIndex) {
        List<Integer> zz = new ArrayList<>();
        for (int i=0; i<zombies.size(); i++) {
            if (zombies.get(i).zoneIndex == zoneIndex)
                zz.add(i);
        }
        return zz;
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        for (ZZombie z : zombies) {
            if (z.zoneIndex == zoneIndex)
                return false;
        }
        return true;
    }

    private void spawnZombies(ZZone z, int highestSkill) {
        throw new RuntimeException("Not implemented");
    }

    ZUser getCurrentUser() {
        return users.get(currentUser);
    }

    ZCharacter getCurrentCharacter() {
        return getCurrentUser().charactoers.get(currentCharacter);
    }

}
