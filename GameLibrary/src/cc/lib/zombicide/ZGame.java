package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class ZGame {

    private final static Logger log = LoggerFactory.getLogger(ZGame.class);

    private Stack<ZState> stateStack = new Stack<>();

    public ZBoard board;

    ZUser [] users;
    ZQuest quest;
    List<ZZombie> zombies = new ArrayList<>();
    int currentUser;
    int currentCharacter;
    int costToOrganize;

    public void setUsers(ZUser ... users) {
        this.users = users;
    }

    public void setQuest(ZQuest quest) {
        this.quest = quest;
    }

    public ZState getState() {
        if (stateStack.empty())
            stateStack.push(ZState.INIT);
        return stateStack.peek();
    }

    private void setState(ZState state) {
        stateStack.clear();
        stateStack.push(state);
    }

    public void runGame() {
        switch (getState()) {
            case INIT: {
                users[0].prepareTurn();
                setState(ZState.BEGIN_ROUND);
                break;
            }

            case BEGIN_ROUND: {
                setState(ZState.SPAWN);
                currentUser = 0;
                currentCharacter = 0;
                break;
            }

            case SPAWN: {
                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                int highestSkill = 0;
                for (ZUser u : users) {
                    for (ZCharacter c : u.characters) {
                        highestSkill = Math.max(highestSkill, c.getSkillLevel().ordinal());
                    }
                }
                for (ZZone z : board.getZones()) {
                    if (z.isSpawn) {
                        spawnZombies(z, highestSkill);
                    }
                }
                setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
                break;
            }

            case  PLAYER_STAGE_CHOOSE_CHARACTER: {
                // for each user, they choose each of their characters in any order and have them
                // perform all of their actions
                ZUser user = getCurrentUser();
                List<Integer> options = new ArrayList<>();
                for (int i = 0; i<user.characters.size(); i++) {
                    ZCharacter c = user.characters.get(i);
                    if (c.actionsLeftThisTurn > 0) {
                        options.add(i);
                    }
                }

                if (options.size() == 0) {
                    if (++currentUser >= users.length) {
                        currentUser = 0;
                        setState(ZState.ZOMBIE_STAGE);
                    }
                    getCurrentUser().prepareTurn();
                    break;
                }

                currentCharacter = -1;
                Integer choice = getCurrentUser().chooseCharacter(options);
                if (choice != null) {
                    currentCharacter = choice;
                    stateStack.push(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION);
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION: {
                List<ZMove> options = new ArrayList<>();
                options.add(ZMove.newDoNothing());
                options.add(ZMove.newGoBack(ZState.PLAYER_STAGE_CHOOSE_CHARACTER));
                // determine players available moves
                ZCharacter cur = getCurrentCharacter();
                // check for organize
                for (ZEquipment e : cur.backpack) {
                    if (e.getSlot() != ZEquipSlot.BACKPACK) {
                        options.add(ZMove.newOrganizeMove());
                        break;
                    }
                }

                int zoneIndex = cur.occupiedZone;
                ZZone zone = board.getZone(zoneIndex);

                // check for move up, down, right, left
                List<Integer> accessableZones = board.getAccessableZones(zoneIndex, 1);
                for (int  z : accessableZones) {
                    options.add(ZMove.newWalkMove(z));
                }

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

                for (ZWeapon w : getCurrentCharacter().getWeapons()) {
                    addAttackOptions(zoneIndex, w.getMeleeStats(cur), w, options, true);
                    //if (w.loaded)
                    //    addAttackOptions(zoneIndex, w.type.getRangedStats(cur), w, options, false);
                    //else
                        options.add(ZMove.newReloadMove(w));
                    addAttackOptions(zoneIndex, w.getMagicStats(cur), w, options, false);
                    if (cur.isDualWeilding())
                        break;
                }

                // check for enchanting self or another

                // check for eating / backpack

                // check for open check /

                ZMove move = getCurrentUser().chooseMove(this, cur, options);
                if (move != null) {
                    performMove(cur, move);
                }

                break;
            }

            case PLAYER_STAGE_ORGANIZE: {
                ZCharacter cur = getCurrentCharacter();
                List<ZMove> options = new ArrayList<>();
                boolean backpackFull = cur.isBackpackFull();
                if (cur.leftHand != null) {
                    if (!backpackFull)
                        options.add(ZMove.newUnequipMove(cur.leftHand));
                    options.add(ZMove.newDisposeMove(cur.leftHand));
                }

                if (cur.rightHand != null) {
                    if (!backpackFull)
                        options.add(ZMove.newUnequipMove(cur.rightHand));
                    options.add(ZMove.newDisposeMove(cur.rightHand));
                }

                if (cur.body != null) {
                    if (!backpackFull)
                        options.add(ZMove.newUnequipMove(cur.body));
                    options.add(ZMove.newDisposeMove(cur.body));
                }

                for (int i=0; i<cur.numBackpackItems; i++) {
                    ZEquipment equip = cur.backpack[i];
                    switch (equip.getSlot()) {
                        case BODY:
                        case HAND:
                            options.add(ZMove.newEquipMove(equip));
                            break;
                    }
                    options.add(ZMove.newDisposeMove(equip));
                    if (equip.canConsume())
                        options.add(ZMove.newConsumeMove(equip));
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_NEW_SKILL: {
                ZCharacter cur = getCurrentCharacter();
                ZSkill skill = null;
                switch (cur.getSkillLevel()) {
                    case BLUE:
                        skill = getCurrentUser().chooseNewSkill(this, cur, Arrays.asList(cur.name.blueSkillOptions));
                        break;
                    case YELOW:
                        skill = getCurrentUser().chooseNewSkill(this, cur, Arrays.asList(cur.name.blueSkillOptions));
                        break;
                    case ORANGE:
                        skill = getCurrentUser().chooseNewSkill(this, cur, Arrays.asList(cur.name.blueSkillOptions));
                        break;
                    case RED:
                        skill = getCurrentUser().chooseNewSkill(this, cur, Arrays.asList(cur.name.blueSkillOptions));
                        break;
                }
                if (skill != null) {
                    cur.skills.add(skill);
                    endAction();
                }

                break;
            }

            case PLAYER_STAGE_CHOOSE_ZONE_FOR_BILE: {
                ZCharacter cur = getCurrentCharacter();
                List<Integer> accessable = board.getAccessableZones(cur.occupiedZone, 1);
                if (accessable.size() == 1) {
                    cur.dispose(ZItem.DRAGON_BILE);
                    board.zones.get(accessable.get(0)).dragonBile = true;
                    endAction();
                } else if (accessable.size() > 1) {
                    Integer zone = getCurrentUser().chooseZoneForBile(this, cur, accessable);
                    if (zone != null) {
                        cur.dispose(ZItem.DRAGON_BILE);
                        board.zones.get(zone).dragonBile = true;
                        endAction();
                    }
                }


                break;
            }

            default:
                throw new AssertionError("Unhandled state: " + getState());
        }
    }

    protected void onDragonBilePlaced(ZCharacter c, int zone) {
        log.info("%s placed dragon bile in zone %d", c.name, zone);
    }

    private void endAction() {
        if (getCurrentCharacter().actionsLeftThisTurn > 0) {
            setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION);
        } else {
            costToOrganize = 1;
            setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
        }
    }

    private void performMove(ZCharacter c, ZMove move) {
        switch (move.type) {
            case DO_NOTHING:
                c.actionsLeftThisTurn--;
                break;
            case GO_BACK:
                if (stateStack.size() > 1) {
                    stateStack.pop();
                }
                break;
            case ORGANNIZE: {
                stateStack.push(ZState.PLAYER_STAGE_ORGANIZE);
                break;
            }
            case TRADE:
                break;
            case WALK:
                break;
            case MELEE_ATTACK:
                break;
            case RANGED_ATTACK:
                break;
            case RELOAD:
                break;
            case OPEN_DOOR:
                break;
            case SEARCH:
                break;
            case DROP_DRAGON_BILE:
                break;
            case IGNITE:
                break;

            case EQUIP:
                c.equip(move.equip);
                c.actionsLeftThisTurn -= costToOrganize;
                costToOrganize = 0;
                break;
            case UNEQUIP:
                c.unequip(move.equip);
                c.actionsLeftThisTurn -= costToOrganize;
                costToOrganize = 0;
                break;
            case DISPOSE:
                c.dispose(move.equip);
                c.actionsLeftThisTurn -= costToOrganize;
                costToOrganize = 0;
                break;
            case CONSUME:
                performConsume(c, (ZItem)move.equip);
                //c.equip(move.equip);
                costToOrganize = 0;
                break;
        }
    }

    private void performConsume(ZCharacter c, ZItem consumable) {
        switch (consumable) {
            case DRAGON_BILE:
                stateStack.push(ZState.PLAYER_STAGE_CHOOSE_ZONE_FOR_BILE);
                break;
            case TORCH: {
                performTorch(c);
                break;
            }
            case WATER:
            case APPLES:
            case SALTED_MEAT:
                c.dispose(consumable);
                addExperience(c, 1);
                break;
            default:
                throw new AssertionError("Unhandled case: " + consumable);
        }
    }

    private void addExperience(ZCharacter c, int pts) {
        if (pts <= 0)
            return;
        ZSkillLevel sl = c.getSkillLevel();
        c.dangerBar += pts;
        if (c.getSkillLevel() != sl) {
            stateStack.push(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL);
        } else {
            onCharacterGainedExperience(c, pts);
        }
    }

    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        log.info("%s gained %d experence!", c.name, points);
    }

    private void performTorch(ZCharacter c) {
        List<Integer> zones = board.getAccessableZones(c.occupiedZone, 1);
        Iterator<Integer> it = zones.iterator();
        while (it.hasNext()) {
            if (!board.zones.get(it.next()).dragonBile) {
                it.remove();
            }
        }
        if (zones.size() == 1) {
            // light the bile!
            resolveDragonBile(c, zones.get(0));
        } else {
            stateStack.push(ZState.PLAYER_STAGE_CHOOSE_ZONE_TO_IGNITE);
        }
    }

    private void resolveDragonBile(ZCharacter c, int zone) {
        c.actionsLeftThisTurn-=1;
        c.dispose(ZItem.TORCH);
        List<Integer> torched = getZombiesInZone(zone);
        int exp = 0;
        board.zones.get(zone).dragonBile = false;
        for (int z : torched) {
            ZZombie zombie = zombies.get(z);
            onZombieDestroyed(zombie);
            zombies.remove(zombie);
            exp += zombie.type.expProvided;
        }
        addExperience(c, exp);
    }

    protected void onZombieDestroyed(ZZombie zombie) {
        log.info("Zombie %s destroyed for %d experience", zombie.type.name(), zombie.type.expProvided);
    }

    private Iterable<ZCharacter> getAllCharacters() {
        if (users.length == 1)
            return users[0].characters;
        List<ZCharacter> all = new ArrayList<>();
        for (ZUser user : users) {
            all.addAll(user.characters);
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
            if (zombies.get(i).occupiedZone == zoneIndex)
                zz.add(i);
        }
        return zz;
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        for (ZZombie z : zombies) {
            if (z.occupiedZone == zoneIndex)
                return false;
        }
        return true;
    }

    private void spawnZombies(ZZone z, int highestSkill) {
        //throw new RuntimeException("Not implemented");
    }

    ZUser getCurrentUser() {
        return users[currentUser];
    }

    ZCharacter getCurrentCharacter() {
        return getCurrentUser().characters.get(currentCharacter);
    }

}
