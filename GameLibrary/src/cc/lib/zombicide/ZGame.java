package cc.lib.zombicide;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public class ZGame extends Reflector<ZGame>  {

    static {
        addAllFields(ZGame.class);
    }

    private final static Logger log = LoggerFactory.getLogger(ZGame.class);

    final static int GAME_LOST = 2;
    final static int GAME_WON  = 1;

    private final Stack<ZState> stateStack = new Stack<>();
    public ZBoard board=null;
    @Omit
    ZUser [] users=null;
    ZQuest quest=null;
    int currentUser=0;
    @Omit
    ZCharacter currentCharacter = null;
    @Omit
    ZEquipSlot selectedSlot = null;
    @Omit
    ZEquipment selectedEquipment = null;
    LinkedList<ZEquipment> searchables = new LinkedList<>();
    boolean doubleSpawn=false;
    int roundNum=0;
    int gameOverStatus=0; // 0 == in play, 1, == game won, 2 == game lost

    @Override
    protected synchronized void deserialize(BufferedReader _in) throws Exception {
        super.deserialize(_in);
        for (ZUser user : users) {
            user.characters.clear();
        }
        for (ZCharacter c : board.getAllCharacters()) {
            users[c.userIndex].addCharacter(c);
        }
        // TODO: Figure out better way to avoid this?
        setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
    }

    public void setUsers(ZUser ... users) {
        this.users = users;
    }

    public void setQuest(ZQuest quest) {
        this.quest = quest;
    }

    public boolean isGameOver() {
        return gameOverStatus != 0;
    }

    public boolean isGameWon() {
        return gameOverStatus == GAME_WON;
    }

    public boolean isGameLost() {
        return gameOverStatus == GAME_LOST;
    }

    public void loadQuest(ZQuests quest) {
        this.quest = quest.load();
        board = this.quest.loadBoard();
        //for (ZCell cell : board.getCells()) {
        for (Grid.Iterator<ZCell> it = board.grid.iterator(); it.hasNext(); ) {
            ZCell cell=it.next();
            if (cell.cellType == ZCellType.EMPTY)
                continue;
            ZZone zone = board.zones.get(cell.zoneIndex);
            switch (cell.environment) {
                case ZCell.ENV_OUTDOORS:
                    zone.type = ZZoneType.OUTDOORS;
                    break;
                case ZCell.ENV_BUILDING:
                    zone.type = ZZoneType.BUILDING;
                    break;
                case ZCell.ENV_VAULT:
                    zone.type = ZZoneType.VAULT;
                    break;
            }
            // add doors for the zone
            for (ZDir dir : ZDir.values()) {
                switch (cell.getWallFlag(dir)) {
                    case LOCKED:
                    case CLOSED:
                    case OPEN:
                        zone.doors.add(new ZCellDoor(it.getPos(), dir));
                }
            }
            switch (cell.cellType) {
                case START:
                    // position all the characters here
                    for (ZCharacter c : getAllCharacters()) {
                        c.occupiedZone = cell.zoneIndex;
                        board.addActor(c, cell.zoneIndex);
                    }
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
                case SPAWN:
                    zone.isSpawn = true;
                    break;
                case OBJECTIVE:
                    zone.objective = true;
                    break;
                case VAULT_DOOR: {
                    // add a vault door leading to the cell specified by vaultFlag
                    assert(cell.vaultFlag > 0);
                    for (Grid.Iterator<ZCell> it2 = board.grid.iterator(); it2.hasNext(); ) {
                        ZCell cell2 = it2.next();
                        if (cell == cell2)
                            continue;
                        if (cell.vaultFlag == cell2.vaultFlag) {
                            zone.doors.add(new ZVaultDoor(it.getPos(), it2.getPos(), cell.environment == ZCell.ENV_VAULT ? ZDir.NORTH : ZDir.SOUTH));
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private void spawnZombies(int count, ZZombieType [] options, int zone) {
        if (doubleSpawn) {
            count *= 2;
            doubleSpawn = false;
        }
        for (int i=0; i<count; i++) {
            ZZombie zombie = new ZZombie(Utils.randItem(options), zone);
            board.addActor(zombie, zone);
            onZombieSpawned(zombie);
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

        if (isGameOver())
            return;

        final ZCharacter cur = getCurrentCharacter();
        final ZUser user = getCurrentUser();

        if (quest.isQuestComplete(this)) {
            gameWon();
            return;
        }

        log.debug("runGame %s", getState());

        switch (getState()) {
            case INIT: {
                users[0].prepareTurn();
                initSearchables();
                setState(ZState.BEGIN_ROUND);
                roundNum = 0;
                break;
            }

            case BEGIN_ROUND: {
                if (roundNum > 0)
                    setState(ZState.SPAWN);
                else
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
                onStartRound(roundNum++);
                currentUser = 0;
                currentCharacter = null;
                board.resetNoise();
                break;
            }

            case SPAWN: {
                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                ZSkillLevel highestSkill = getHighestSkillLevel();
                for (int zIdx=0; zIdx<board.zones.size(); zIdx++) {
                    ZZone z = board.getZone(zIdx);
                    if (z.isSpawn) {
                        spawnZombies(zIdx, highestSkill);
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
                } else if (options.size() == 1) {
                    currentCharacter = options.get(0);
                } else {
                    currentCharacter = getCurrentUser().chooseCharacter(options);
                }
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
                if (cur.getNumBackpackItems() > 0)
                    options.add(ZMove.newOrganizeMove());

                // check for trade with another character in the same zone
                if (cur.canTrade()) {
                    List<ZCharacter> tradeOptions = new ArrayList<>();
                    for (ZCharacter c : getAllCharacters()) {
                        if (c == cur)
                            continue;
                        if (c.occupiedZone == cur.occupiedZone && c.canTrade()) {
                            tradeOptions.add(c);
                        }
                    }
                    if (tradeOptions.size() > 0)
                        options.add(ZMove.newTradeMove(tradeOptions));
                }

                ZZone zone = board.zones.get(cur.occupiedZone);

                // check for search
                if (zone.isSearchable() && cur.canSearch() && isClearedOfZombies(cur.occupiedZone)) {
                    options.add(ZMove.newSearchMove(cur.occupiedZone));
                }

                // check for move up, down, right, left
                List<Integer> accessableZones = board.getAccessableZones(cur.occupiedZone, 1);
                if (accessableZones.size() > 0)
                    options.add(ZMove.newWalkMove(accessableZones));


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

                List<ZDoor> doors = new ArrayList<>();
                for (ZDoor door : zone.doors) {
                    if (door.isJammed() && !cur.canUnjamDoor())
                        continue;
                    if (!door.isClosed(board) && !door.canBeClosed(cur))
                        continue;
                    doors.add(door);
                }

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
                    slots.add(ZEquipSlot.LEFT_HAND);
                if (cur.rightHand != null)
                    slots.add(ZEquipSlot.RIGHT_HAND);
                if (cur.body != null)
                    slots.add(ZEquipSlot.BODY);
                if (cur.getNumBackpackItems() > 0)
                    slots.add(ZEquipSlot.BACKPACK);
                selectedSlot = getCurrentUser().chooseSlotToOrganize(this, cur, slots);
                if (selectedSlot != null) {
                    stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ITEM);
                }
                break;
            }

            case PLAYER_STAGE_ORGANIZE_CHOOSE_ITEM: {
                // choose which equipment form the slot to organize
                switch (selectedSlot) {
                    case BACKPACK:
                        if (cur.getNumBackpackItems() > 1) {
                            // add
                            selectedEquipment = getCurrentUser().chooseEquipment(this, cur, cur.getBackpack());
                            if (selectedEquipment != null) {
                                stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                            }
                        } else {
                            selectedEquipment = cur.getBackpackItem(0);
                            stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        }
                        break;
                    case BODY:
                        selectedEquipment = cur.body;
                        stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        break;
                    case LEFT_HAND:
                        selectedEquipment = cur.leftHand;
                        stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ACTION);
                        break;
                    case RIGHT_HAND:
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
                    case RIGHT_HAND:
                    case LEFT_HAND:
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
                    while (stateStack.peek() != ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_SLOT)
                        stateStack.pop();
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

            case ZOMBIE_STAGE: {
                // any existing zombies take their actions
                for (ZZombie zombie : board.getAllZombies()) {
                    zombie.prepareTurn();
                }
                for (int zoneIdx=0; zoneIdx < board.zones.size(); zoneIdx++) {
                    List<ZZombie> zombies = board.getZombiesInZone(zoneIdx);
                    List<ZCharacter> victims = board.getCharactersInZone(zoneIdx);
                    for (ZZombie zombie : zombies) {
                        while (zombie.getActionsLeftThisTurn() > 0) {
                            if (victims.size() > 1) {
                                Collections.sort(victims, (o1, o2) -> {
                                    int v0 = o1.woundBar + o1.getArmorRating(zombie.type);
                                    int v1 = o2.woundBar + o2.getArmorRating(zombie.type);
                                    return Integer.compare(v1, v0);
                                });
                            }
                            if (victims.size() > 0) {
                                ZCharacter victim = victims.get(0);
                                zombie.performAction(ZActionType.MELEE, this);
                                if (playerDefends(cur, zombie)) {
                                    onCharacterDefends(cur, zombie);
                                }
                                victim.woundBar++;
                                if (victim.isDead()) {
                                    removeCharacter(victim);
                                    onCharacterPerished(victim, zombie);
                                    if (quest.isAllMustLive()) {
                                        gameLost();
                                    }
                                } else {
                                    onCharacterWounded(victim, zombie);
                                }
                            } else {
                                moveZombieToTowardVisibleCharactersOrLoudestZone(zombie);
                                zombie.performAction(ZActionType.MOVE, this);
                            }
                        }
                    }
                }
                setState(ZState.SPAWN);
                break;
            }

            default:
                throw new AssertionError("Unhandled state: " + getState());
        }
    }

    protected void onCharacterDefends(ZCharacter cur, ZZombie zombie) {
        getCurrentUser().showMessage(cur.name() + " defends against " + zombie.name());
    }

    boolean playerDefends(ZCharacter cur, ZZombie zombie) {
        for (ZArmor armor : cur.getArmor()) {
            int rating = armor.getRating(zombie.type);
            if (rating > 0) {
                int [] dice = rollDice(1);
                if (dice[0] >= rating)
                    return true;
            }
        }
        return false;
    }

    void gameLost() {
        gameOverStatus = GAME_LOST;
        onGameLost();
    }

    void gameWon() {
        gameOverStatus = GAME_WON;
        onQuestComplete();
    }

    protected void onGameLost() {
        getCurrentUser().showMessage("Game Lost");
    }

    protected void onCharacterPerished(ZCharacter character, ZZombie attacker) {
        getCurrentUser().showMessage(character.name() + " has been killed by a " + attacker.name());
    }

    protected void onCharacterWounded(ZCharacter character, ZZombie attacker) {
        getCurrentUser().showMessage(character.name() + " has been wounded by a " + attacker.name());
    }

    protected void onDragonBilePlaced(ZCharacter c, int zone) {
        log.info("%s placed dragon bile in zone %d", c.name, zone);
    }

    private void removeCharacter(ZCharacter character) {
        for (ZUser user : users) {
            user.characters.remove(character);
        }
        board.removeActor(character);
    }

    private void moveZombieToTowardVisibleCharactersOrLoudestZone(ZZombie zombie) {
        // zombie will move toward players it can see first and then noisy areas second
        int maxNoise = 0;
        int targetZone = -1;
        for (ZCharacter c : getAllCharacters()) {
            if (board.canSee(zombie.occupiedZone, c.occupiedZone)) {
                int noiseLevel = board.getZone(c.occupiedZone).noiseLevel;
                if (maxNoise < noiseLevel) {
                    targetZone = c.occupiedZone;
                    maxNoise = noiseLevel;
                }
            }
        }

        if (targetZone < 0) {
            // move to noisiest zone
            for (int zone=0; zone < board.zones.size(); zone++) {
                int noiseLevel = board.zones.get(zone).noiseLevel;
                if (noiseLevel > maxNoise) {
                    maxNoise = noiseLevel;
                    targetZone = zone;
                }
            }
        }

        if (targetZone >= 0) {
            List<ZDir> paths = board.getShortestPathOptions(zombie.occupiedCell, targetZone);
            if (paths.size() > 0) {
                ZDir dir = Utils.randItem(paths); // TODO: Use Dir class instead of int
                board.moveActorInDirection(zombie, dir);
            }
        }
    }

    protected void onStartRound(int roundNum) {
        getCurrentUser().showMessage("Begin Round " + roundNum);
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
                c.performAction(ZActionType.OBJECTIVE, this);
                board.getZone(c.occupiedZone).objective = false;
                endAction();
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
                                        addExperience(c, zombie.type.expProvided);
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
                    if (door.isClosed(board)) {
                        if (!door.isJammed() || c.performAction(ZActionType.OPEN_DOOR, this)) {
                            door.toggle(board);
                            onDoorOpened(door);
                            // spawn zombies in the newly exposed zone and any adjacent
                            ZDoor otherSide = door.getOtherSide(board);
                            if (board.getZone(board.getCell(otherSide.getCellPos()).zoneIndex).canSpawn()) {
                                ZSkillLevel highest = getHighestSkillLevel();
                                HashSet<Integer> spawnZones = new HashSet<>();
                                board.getUndiscoveredIndoorZones(otherSide.getCellPos(), spawnZones);
                                log.debug("Zombie spawn zones: " + spawnZones);
                                for (int zone : spawnZones) {
                                    spawnZombies(zone, highest);
                                }
                            }
                        }
                    } else {
                        c.performAction(ZActionType.CLOSE_DOOR, this);
                        door.toggle(board);
                    }
                    //c.performAction(ZActionType.TOGGLE_DOOR, this);
                    endAction();
                }
                break;
            }
            case SEARCH: {
                // draw from top of the deck
                if (searchables.size() > 0) {
                    ZEquipment equip = searchables.removeLast();
                    if (equip.getType() == ZItemType.AAHHHH) {
                        getCurrentUser().showMessage("Aaaahhhh!!!");
                        // spawn zombie right here right now
                        spawnZombies(1, ZZombieType.WALKERS, currentCharacter.occupiedZone);
                        searchables.addFirst(equip);
                    } else {
                        user.showMessage(currentCharacter.name() + " Found a " + equip);
                        currentCharacter.equip(equip);
                    }
                    currentCharacter.performAction(ZActionType.SEARCH, this);
                    endAction();
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
                endAction();
                break;
            default:
                log.error("Unhandled move: %s", move.type);
        }
    }

    protected void onDoorOpened(ZDoor door) {
        log.info(currentCharacter.name() + " has opened a " + door.name());
    }

    public ZSkillLevel getHighestSkillLevel() {
        int highestSkill = 0;
        for (ZUser u : users) {
            for (ZCharacter c : u.characters) {
                highestSkill = Math.max(highestSkill, c.getSkillLevel().ordinal());
            }
        }
        return ZSkillLevel.values()[highestSkill];
    }

    private void performConsume(ZCharacter c, ZMove move) {
        ZItem item  = (ZItem)move.equipment;
        ZEquipSlot slot = move.fromSlot;
        switch ((ZItemType)item.getType()) {
            case DRAGON_BILE: {
                Integer zone = getCurrentUser().chooseZoneForBile(this, c, move.list);
                if (zone != null) {
                    c.removeEquipment(item, selectedSlot);
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
                    c.removeEquipment(ZItemType.TORCH, selectedSlot);
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
        onRollDice(result);
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

    protected void onDoubleSpawn() {
        getCurrentUser().showMessage("DOUBLE SPAWN!");
    }

    private void spawnZombies(int zoneIdx, ZSkillLevel level) {
        log.info("Random zombie spawn for zone %d and level %s", zoneIdx, level);
        if (!doubleSpawn) {
            if (Utils.rand() % 10 == 0) {
                doubleSpawn = true;
                onDoubleSpawn();
                return;
            }
        }

        // STANDARD INVASION
        //   RED = Fatty x 2
        //   ORANGE = Walker x 5
        //   YELLOW = Runner x 2
        //   BLUE = 0

        switch (level) {
            case BLUE:
                switch (Utils.rand() % 3) {
                    case 2:
                        spawnZombies(1, ZZombieType.WALKERS, zoneIdx);
                        break;
                }
                break;
            case YELOW:
                spawnZombies(Utils.randRange(2,3), ZZombieType.WALKERS, zoneIdx);
                break;
            case ORANGE:
                switch (Utils.rand() % 3) {
                    case 0:
                        spawnZombies(Utils.randRange(3,4), ZZombieType.WALKERS, zoneIdx);
                        break;
                    case 1:
                        spawnZombies(Utils.randRange(1,2), ZZombieType.FATTIES, zoneIdx);
                        break;
                    case 2:
                        spawnZombies(1, ZZombieType.RUNNERS, zoneIdx);
                        break;
                }
                break;
            case RED:
                switch (Utils.rand() % 4) {
                    case 0:
                        spawnZombies(5, ZZombieType.WALKERS, zoneIdx);
                        break;
                    case 1:
                        spawnZombies(2, ZZombieType.FATTIES, zoneIdx);
                        break;
                    case 2:
                        spawnZombies(2, ZZombieType.RUNNERS, zoneIdx);
                        break;
                    case 3:
                        spawnZombies(1, ZZombieType.ABOMINATIONS, zoneIdx);
                        break;
                }
                break;
        }
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

    List<ZEquipment> make(int count, Enum e) {
        List<ZEquipment> list = new ArrayList<>();
        if (e instanceof ZWeaponType) {
            for (int i=0; i<count; i++)
                list.add(new ZWeapon((ZWeaponType)e));
        } else if (e instanceof ZArmorType) {
            for (int i=0; i<count; i++)
                list.add(new ZArmor((ZArmorType)e));
        } else if (e instanceof ZItemType) {
            for (int i=0; i<count; i++)
                list.add(new ZItem((ZItemType)e));
        }
        return list;
    }

    void initSearchables() {
        searchables.clear();
        searchables.addAll(make(4, ZItemType.AAHHHH));
        searchables.addAll(make(2, ZItemType.APPLES));
        searchables.addAll(make(2, ZWeaponType.AXE));
        searchables.addAll(make(2, ZArmorType.CHAIN));
        searchables.addAll(make(2, ZWeaponType.CROSSBOW));
        searchables.addAll(make(4, ZWeaponType.DAGGER));
        searchables.addAll(make(2, ZWeaponType.DEATH_STRIKE));
        //searchables.addAll(make(4, ZItemType.DRAGON_BILE));
        searchables.addAll(make(4, ZWeaponType.FIREBALL));
        searchables.addAll(make(2, ZWeaponType.GREAT_SWORD));
        searchables.addAll(make(1, ZWeaponType.HAMMER));
        searchables.addAll(make(2, ZWeaponType.HAND_CROSSBOW));
        searchables.addAll(make(1, ZSpellType.HEALING));
        searchables.addAll(make(1, ZWeaponType.INFERNO));
        searchables.addAll(make(1, ZSpellType.INVISIBILITY));
        searchables.addAll(make(2, ZArmorType.LEATHER));
        searchables.addAll(make(2, ZWeaponType.LIGHTNING_BOLT));
        searchables.addAll(make(2, ZWeaponType.LONG_BOW));
        searchables.addAll(make(1, ZWeaponType.MANA_BLAST));
//        searchables.addAll(make(1, ZWeaponType.ORCISH_CROSSBOW));
        searchables.addAll(make(1, ZArmorType.PLATE));
        searchables.addAll(make(3, ZItemType.PLENTY_OF_ARROWS));
        searchables.addAll(make(3, ZItemType.PLENTY_OF_BOLTS));
        searchables.addAll(make(2, ZWeaponType.REPEATING_CROSSBOW));
        searchables.addAll(make(1, ZSpellType.REPULSE));
        searchables.addAll(make(2, ZItemType.SALTED_MEAT));
        searchables.addAll(make(2, ZArmorType.SHIELD));
        searchables.addAll(make(1, ZWeaponType.SHORT_BOW));
        searchables.addAll(make(1, ZWeaponType.SHORT_SWORD));
        searchables.addAll(make(1, ZSpellType.SPEED));
        searchables.addAll(make(2, ZWeaponType.SWORD));
//        searchables.addAll(make(4, ZItemType.TORCH));
        searchables.addAll(make(2, ZItemType.WATER));
        Utils.shuffle(searchables);
    }

    public void addNoise(int zoneIdx, int noise) {
        board.getZone(zoneIdx).noiseLevel += noise;
        onNoiseAdded(zoneIdx);
    }

    protected void onNoiseAdded(int zoneIndex) {
        getCurrentUser().showMessage("Noise was made in zone " + zoneIndex);
    }

}
