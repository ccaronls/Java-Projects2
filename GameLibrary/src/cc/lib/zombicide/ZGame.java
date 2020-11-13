package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;

public class ZGame {

    private final static Logger log = LoggerFactory.getLogger(ZGame.class);

    private Stack<ZState> stateStack = new Stack<>();

    public ZBoard board;

    ZUser [] users;
    ZQuest quest;
    int currentUser;
    ZCharacter currentCharacter = null;
    ZEquipSlot selectedSlot = null;
    ZEquipment selectedEquipment = null;
    LinkedList<ZEquipment> searchables = new LinkedList<>();

    public void setUsers(ZUser ... users) {
        this.users = users;
    }

    public void setQuest(ZQuest quest) {
        this.quest = quest;
    }

    public void loadQuest(ZQuests quest) {
        this.quest = quest.load();
        board = this.quest.loadBoard();
        for (ZCell cell : board.getCells()) {
            switch (cell.cellType) {

                case NONE:
                    break;
                case VAULT:
                    break;
                case OBJECTIVE:
                    break;
                case SPAWN:
                    break;
                case START:
                    // position all the characters here
                    for (ZCharacter c : getAllCharacters()) {
                        c.occupiedZone = cell.zoneIndex;
                        board.addActor(c, cell.zoneIndex);
                    }
                    break;
                case EXIT:
                    break;
                case WALKER:
                    spawnZombies(1, ZZombieType.WALKERS, cell.zoneIndex);
                    break;
                case RUNNER:
                    spawnZombies(1, ZZombieType.RUNNERS, cell.zoneIndex);
                    break;
                case FATTY:
                    spawnZombies(1, ZZombieType.FATTIES, cell.zoneIndex);
                    break;
                case NECRO:
                    spawnZombies(1, ZZombieType.NECROMANCERS, cell.zoneIndex);
                    break;
            }
        }
    }

    private void spawnZombies(int count, ZZombieType [] options, int zone) {
        for (int i=0; i<count; i++) {
            ZZombie zombie = new ZZombie(Utils.randItem(options), zone);
            board.addActor(zombie, zone);
        }
    }

    protected void onZombieSpawned(ZZombie zombie) {
        log.info("A %s Zombie has Spawned at zone %d", zombie.type, zombie.occupiedZone);
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

    protected void onQuestComplete() {
        getCurrentUser().showMessage("Quest Complete");
    }

    boolean isGameSetup() {
        if(board == null || board.grid == null)
            return false;
        if (users == null || users.length == 0 || getCurrentUser() == null)
            return false;
        if (getAllCharacters().size() == 0)
            return false;
        if (quest == null)
            return false;
        return true;
    }

    public ZQuest getQuest() {
        return quest;
    }

    public void runGame() {

        if (!isGameSetup()) {
            log.error("Invalid Game");
            assert(false);
            return;
        }

        final ZCharacter cur = getCurrentCharacter();
        final ZUser user = getCurrentUser();

        if (quest.isQuestComplete(this)) {
            onQuestComplete();
            return;
        }

        switch (getState()) {
            case INIT: {
                users[0].prepareTurn();
                initSearchables();
                setState(ZState.BEGIN_ROUND);
                break;
            }

            case BEGIN_ROUND: {
                setState(ZState.SPAWN);
                currentUser = 0;
                currentCharacter = null;
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
                List<ZCharacter> options = new ArrayList<>();
                for (int i = 0; i<user.characters.size(); i++) {
                    ZCharacter c = user.characters.get(i);
                    if (c.getActionsLeftThisTurn() > 0) {
                        options.add(c);
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

                currentCharacter = getCurrentUser().chooseCharacter(options);
                if (currentCharacter != null) {
                    stateStack.push(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION);
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION: {
                List<ZMove> options = new ArrayList<>();
                options.add(ZMove.newDoNothing());

                // determine players available moves

                // add any moves determined by the quest
                quest.addMoves(this, cur, options);

                // check for organize
                if (cur.numBackpackItems > 0)
                    options.add(ZMove.newOrganizeMove());

                // check for move up, down, right, left
                List<Integer> accessableZones = board.getAccessableZones(cur.occupiedZone, 1);
                if (accessableZones.size() > 0)
                    options.add(ZMove.newWalkMove(accessableZones));

                // check for trade with another character in the same zone
                if (cur.canTrade()) {
                    List<ZCharacter> tradeOptions = new ArrayList<>();
                    for (ZCharacter c : getAllCharacters()) {
                        if (c == cur)
                            continue;
                        if (c.occupiedZone == cur.occupiedZone) {
                            tradeOptions.add(c);
                        }
                    }
                    if (tradeOptions.size() > 0)
                        options.add(ZMove.newTradeMove(tradeOptions));
                }

                // check for search
                if (board.zones.get(cur.occupiedZone).searchable && cur.canSearch() && isClearedOfZombies(cur.occupiedZone)) {
                    options.add(ZMove.newSearchMove(cur.occupiedZone));
                }

                List<ZEquipSlot> slots = cur.getMeleeWeapons();
                if (slots.size() > 0) {
                    options.add(ZMove.newMeleeAttackMove(slots));
                }

                slots = cur.getRangedWeapons();
                if (slots.size() > 0) {
                    options.add(ZMove.newRangedAttackMove(slots));
                }

                slots = cur.getMagicWeapons();
                if (slots.size() > 0) {
                    options.add(ZMove.newMagicAttackMove(slots));
                }

                // check for open check /
                List<ZDoor> doors = board.getDoorsForZone(cur.occupiedZone, ZWallFlag.CLOSED, ZWallFlag.OPEN);
                if (doors.size() > 0) {
                    options.add(ZMove.newToggleDoor(doors));
                }

                ZMove move = getCurrentUser().chooseMove(this, cur, options);
                if (move != null) {
                    performMove(cur, move);
                }

                break;
            }

            case PLAYER_STAGE_ORGANIZE_CHOOSE_SLOT: {
                // give options of which slot to organize
                List<ZEquipSlot> slots = new ArrayList<>();
                if (cur.leftHand != null)
                    slots.add(ZEquipSlot.LHAND);
                if (cur.rightHand != null)
                    slots.add(ZEquipSlot.RHAND);
                if (cur.body != null)
                    slots.add(ZEquipSlot.BODY);
                if (cur.numBackpackItems > 0)
                    slots.add(ZEquipSlot.BACKPACK);
                selectedSlot = getCurrentUser().chooseSlotToOrganize(this, cur, slots);
                if (selectedSlot != null) {
                    stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                }
                break;
            }

            case PLAYER_STAGE_ORGANIZE_CHOOSE_ITEM: {
                // choose which equipment form the slot to organize
                switch (selectedSlot) {
                    case BACKPACK:
                        if (cur.numBackpackItems > 1) {
                            // add
                            selectedEquipment = getCurrentUser().chooseEquipment(this, cur, Utils.toList(0, cur.numBackpackItems, cur.backpack));
                            if (selectedEquipment != null) {
                                stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                            }
                        } else {
                            selectedEquipment = cur.backpack[0];
                            stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        }
                        break;
                    case BODY:
                        selectedEquipment = cur.body;
                        stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        break;
                    case LHAND:
                        selectedEquipment = cur.leftHand;
                        stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        break;
                    case RHAND:
                        selectedEquipment = cur.rightHand;
                        stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        break;
                }

                break;
            }

            case PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION: {
                // we have a slot and an equipment from the slot to do something with
                // we can:
                //   dispose, unequip, equip to an empty slot or consume
                List<ZMove> options = new ArrayList<>();
                if (selectedEquipment.canConsume()) {
                    options.add(ZMove.newConsumeMove(selectedEquipment, selectedSlot));
                }
                switch (selectedSlot) {
                    case BACKPACK:
                        if (selectedEquipment.canEquip()) {
                            for (ZEquipSlot slot : cur.getEquipableSlots(selectedEquipment)) {
                                options.add(ZMove.newEquipMove(selectedEquipment, selectedSlot, slot));
                            }
                        }
                        break;
                    case RHAND:
                    case LHAND:
                    case BODY: {
                        if (!cur.isBackpackFull()) {
                            options.add(ZMove.newUnequipMove(selectedEquipment, selectedSlot));
                        }
                    }
                }
                options.add(ZMove.newDisposeMove(selectedEquipment, selectedSlot));
                ZMove move = user.chooseMove(this, cur, options);
                if (move != null) {
                    performMove(cur, move);
                    endAction();
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_NEW_SKILL: {
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
                    cur.allSkills.add(skill);
                    cur.availableSkills.add(skill);
                    endAction();
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
        if (getCurrentCharacter().getActionsLeftThisTurn() > 0) {
            setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION);
        } else {
            setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
        }
    }

    private void performMove(ZCharacter c, ZMove move) {
        ZUser user = getCurrentUser();
        switch (move.type) {
            case DO_NOTHING:
                c.performAction(ZActionType.DO_NOTHING, this);
                endAction();
                break;
            case OBJECTIVE: {
                quest.processObjective(this, c, move);
                break;
            }
            case ORGANNIZE: {
                stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_SLOT);
                break;
            }
            case TRADE:
                ZCharacter other = user.chooseTradeCharacter(this, c, move.list);
                if (other != null) {
                    // TODO
                    endAction();
                }
                break;
            case WALK: {
                Integer zone = user.chooseZoneToWalk(this, c, move.list);
                if (zone != null) {
                    moveActor(c, zone);
                    c.performAction(ZActionType.MOVE, this);
                    endAction();
                }
                break;
            }
            case MELEE_ATTACK: {
                List<ZEquipSlot> weapons = move.list;
                ZEquipSlot slot = null;
                if (weapons.size() > 1) {
                    slot = user.chooseWeaponSlot(this, c, weapons);
                } else {
                    slot = weapons.get(0);
                }
                if (slot != null) {
                    ZWeaponStat stat = c.getWeaponStat(slot, ZActionType.MELEE, this);
                    List<ZZombie> zombies = board.getZombiesInZone(c.occupiedZone);
                    if (zombies.size() > 1)
                        Collections.sort(zombies, (o1, o2) -> Integer.compare(o2.type.minDamageToDestroy, o1.type.minDamageToDestroy));
                    while (zombies.size() > 0 && zombies.get(0).type.minDamageToDestroy > stat.damagePerHit) {
                        zombies.remove(0);
                    }
                    int [] result = rollDice(stat.numDice);
                    int hits = 0;
                    for (int i=0; i<result.length && zombies.size() > 0; i++) {
                        if (result[i] >= stat.dieRollToHit) {
                            ZZombie z = zombies.remove(0);
                            board.removeActor(z);
                            onZombieDestroyed(z);
                            hits ++;
                        }
                    }
                    c.performAction( ZActionType.MELEE,this);
                    user.showMessage(currentCharacter.name() + " Scored " + hits + " hits");
                    endAction();
                }
                break;
            }
            case MAGIC_ATTACK:
            case RANGED_ATTACK: {
                // rules same for both kinda
                ZActionType actionType = move.type == ZMoveType.RANGED_ATTACK ? ZActionType.RANGED : ZActionType.MAGIC;
                List<ZEquipSlot> weapons = move.list;
                ZEquipSlot slot = null;
                if (weapons.size() > 1) {
                    slot = user.chooseWeaponSlot(this, c, weapons);
                } else {
                    slot = weapons.get(0);
                }
                if (slot != null) {
                    ZWeaponStat stat = c.getWeaponStat(slot, actionType, this);
                    List<Integer> zones = new ArrayList<>();
                    for (int range = stat.minRange; range <=stat.maxRange; range++) {
                        zones.addAll(board.getAccessableZones(c.occupiedZone, range));
                    }
                    if (zones.size() > 0) {
                        Integer zone = user.chooseZoneForAttack(this, c, zones);
                        if (zone != null) {
                            // process a ranged attack
                            List<ZZombie> zombies = board.getZombiesInZone(zone);
                            if (zombies.size() > 1)
                                Collections.sort(zombies, (o1, o2) -> Integer.compare(o1.type.rangedPriority, o2.type.rangedPriority));
                            int [] result = rollDice(stat.numDice);
                            int hits = 0;
                            for (int i=0; i<result.length && zombies.size() > 0; i++) {
                                if (result[i] >= stat.dieRollToHit) {
                                    ZZombie zombie = zombies.get(0);
                                    if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                        board.removeActor(zombie);
                                        onZombieDestroyed(zombie);
                                        hits++;
                                    }
                                }
                            }
                            c.performAction(actionType,this);
                            user.showMessage(currentCharacter.name() + " Scored " + hits + " hits");
                            endAction();
                        }
                    }
                }
                break;
            }
            case RELOAD:
                break;
            case TOGGLE_DOOR: {
                List<ZDoor> doors = move.list;
                ZDoor door = user.chooseDoorToToggle(this, c, doors);
                if (door != null) {
                    board.toggleDoor(door);
                    c.performAction(ZActionType.TOGGLE_DOOR, this);
                    endAction();
                }
                break;
            }
            case SEARCH: {
                // draw from top of the deck
                if (searchables.size() > 0) {
                    ZEquipment equip = searchables.removeLast();
                    if (equip == ZItem.AAHHHH) {
                        // spawn zombie right here right now
                        spawnZombies(1, ZZombieType.WALKERS, currentCharacter.occupiedZone);
                        searchables.addFirst(equip);
                    } else {
                        user.showMessage(currentCharacter.name() + " Found a " + equip.name());
                        currentCharacter.equip(equip);
                    }
                }
                break;
            }
            case EQUIP:
                if (move.fromSlot != null) {
                    c.removeEquipment(move.equipment, move.fromSlot);
                }
                c.attachEquipment(move.equipment, move.toSlot);
                c.performAction(ZActionType.ORGANIZE, this);
                endAction();
                break;
            case UNEQUIP:
                c.removeEquipment(move.equipment, move.fromSlot);
                c.attachEquipment(move.equipment, ZEquipSlot.BACKPACK);
                c.performAction(ZActionType.ORGANIZE, this);
                endAction();
                break;
            case DISPOSE:
                c.removeEquipment(move.equipment, move.fromSlot);
                c.performAction(ZActionType.ORGANIZE, this);
                endAction();
                break;
            case CONSUME:
                performConsume(c, move);
                c.performAction(ZActionType.CONSUME, this);
                break;
            default:
                log.error("Unhandled move: %s", move.type);
        }
    }

    private void performConsume(ZCharacter c, ZMove move) {
        ZItem item  = (ZItem)move.equipment;
        ZEquipSlot slot = move.fromSlot;
        switch (item) {
            case DRAGON_BILE: {
                Integer zone = getCurrentUser().chooseZoneForBile(this, c, move.list);
                if (zone != null) {
                    c.removeEquipment(ZItem.DRAGON_BILE, selectedSlot);
                    board.zones.get(zone).dragonBile = true;
                    c.performAction(ZActionType.CONSUME, this);
                    endAction();
                }
                break;
            }
            case TORCH: {
                Integer zone = getCurrentUser().chooseZoneToIgnite(this, c, move.list);
                if (zone != null) {
                    List<ZZombie> torched = board.getZombiesInZone(zone);
                    int exp = 0;
                    board.zones.get(zone).dragonBile = false;
                    for (ZZombie zombie : torched) {
                        onZombieDestroyed(zombie);
                        exp += zombie.type.expProvided;
                    }
                    addExperience(c, exp);
                    c.removeEquipment(ZItem.TORCH, selectedSlot);
                    board.zones.get(zone).dragonBile = false;
                    c.performAction(ZActionType.CONSUME, this);
                    endAction();
                }
                break;
            }
            case WATER:
            case APPLES:
            case SALTED_MEAT:
                c.removeEquipment(item, slot);
                addExperience(c, 1);
                endAction();
                break;
            default:
                throw new AssertionError("Unhandled case: " + item);
        }
    }

    public void addExperience(ZCharacter c, int pts) {
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

    int [] rollDice(int num) {
        int [] result = new int[num];
        for (int i=0; i<num; i++) {
            result[i] = Utils.randRange(1,6);
        }
        return result;
    }

    protected void onRollDice(int [] roll) {
        log.info("Rolling dice result is: %s", Arrays.toString(roll));
    }
/*
    List<Integer> getZonesForDragonBile(ZCharacter c) {
        List<Integer> accessable = board.getAccessableZones(c.occupiedZone, 1);
        Iterator<Integer> it = accessable.iterator();
        while (it.hasNext()) {
            if (board.zones.get(it.next()).dragonBile) {
                it.remove();
            }
        }
        return accessable;
    }

    List<ZCharacter> getTradableCharacters(ZCharacter cur) {
        List<ZCharacter> tradeOptions = new ArrayList<>();
        for (ZCharacter c : getAllCharacters()) {
            if (c == cur)
                continue;
            if (c.occupiedZone == cur.occupiedZone) {
                tradeOptions.add(c);
            }
        }
        return tradeOptions;
    }

    List<Integer> getTorchableZones(ZCharacter c) {
        List<Integer> zones = board.getAccessableZones(c.occupiedZone, 1);
        Iterator<Integer> it = zones.iterator();
        while (it.hasNext()) {
            if (!board.zones.get(it.next()).dragonBile) {
                it.remove();
            }
        }
        return zones;
    }
*/
    protected void onZombieDestroyed(ZZombie zombie) {
        getCurrentUser().showMessage("Zombie " + zombie.type + " destroyed");
        log.info("Zombie %s destroyed for %d experience", zombie.type.name(), zombie.type.expProvided);
    }

    public List<ZCharacter> getAllCharacters() {
        if (users.length == 1)
            return users[0].characters;
        List<ZCharacter> all = new ArrayList<>();
        for (ZUser user : users) {
            all.addAll(user.characters);
        }
        return all;
    }

    private List<ZZombie> getZombiesInRange(int zoneIndex, int distance) {
        if (distance == 0) {
            return board.getZombiesInZone(zoneIndex);
        }

        List<ZZombie> list = new ArrayList<>();
        List<Integer> zones = board.getAccessableZones(zoneIndex, distance);
        for (int z : zones) {
            list.addAll(board.getZombiesInZone(z));
        }
        return list;
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        return board.getZombiesInZone(zoneIndex).size() == 0;
    }

    private void spawnZombies(ZZone z, int highestSkill) {
        //throw new RuntimeException("Not implemented");
    }

    public ZUser getCurrentUser() {
        return users[currentUser];
    }

    public ZCharacter getCurrentCharacter() {
        return currentCharacter;
    }

    void moveActor(ZActor actor, int toZone) {
        board.removeActor(actor);
        board.addActor(actor, toZone);
    }

    public boolean canGoBack() {
        return stateStack.size() > 1;
    }

    public void goBack() {
        stateStack.pop();
    }

    void initSearchables() {
        searchables.clear();
        searchables.addAll(Arrays.asList(
                ZItem.AAHHHH, ZItem.AAHHHH, ZItem.AAHHHH, ZItem.AAHHHH,
                ZItem.APPLES, ZItem.APPLES,
                ZWeapon.AXE, ZWeapon.AXE,
                ZArmor.CHAIN, ZArmor.CHAIN,
                ZWeapon.CROSSBOW, ZWeapon.CROSSBOW,
                ZWeapon.DAGGER, ZWeapon.DAGGER, ZWeapon.DAGGER, ZWeapon.DAGGER,
                ZWeapon.DEATH_STRIKE, ZWeapon.DEATH_STRIKE,
                ZItem.DRAGON_BILE, ZItem.DRAGON_BILE, ZItem.DRAGON_BILE, ZItem.DRAGON_BILE,
                ZWeapon.FIREBALL, ZWeapon.FIREBALL,
                ZWeapon.GREAT_SWORD, ZWeapon.GREAT_SWORD,
                ZWeapon.HAMMER,
                ZWeapon.HAND_CROSSBOW, ZWeapon.HAND_CROSSBOW,
                ZEnchantment.HEALING,
                ZWeapon.INFERNO,
                ZEnchantment.INVISIBILITY,
                ZArmor.LEATHER, ZArmor.LEATHER,
                ZWeapon.LIGHTNING_BOLT, ZWeapon.LIGHTNING_BOLT,
                ZWeapon.LONG_BOW, ZWeapon.LONG_BOW,
                ZWeapon.MANA_BLAST,
                ZWeapon.ORCISH_CROSSBOW,
                ZArmor.PLATE,
                ZItem.PLENTY_OF_ARROWS,ZItem.PLENTY_OF_ARROWS, ZItem.PLENTY_OF_ARROWS,
                ZItem.PLENTY_OF_BOLTS,ZItem.PLENTY_OF_BOLTS,ZItem.PLENTY_OF_BOLTS,
                ZWeapon.REPEATING_CROSSBOW, ZWeapon.REPEATING_CROSSBOW,
                ZEnchantment.REPULSE,
                ZItem.SALTED_MEAT, ZItem.SALTED_MEAT,
                ZArmor.SHIELD, ZArmor.SHIELD,
                ZWeapon.SHORT_BOW,
                ZWeapon.SHORT_SWORD,
                ZEnchantment.SPEED,
                ZWeapon.SWORD, ZWeapon.SWORD,
                ZItem.TORCH, ZItem.TORCH, ZItem.TORCH, ZItem.TORCH,
                ZItem.WATER, ZItem.WATER
                ));
        Utils.shuffle(searchables);
    }
}
