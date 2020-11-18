package cc.lib.zombicide;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.GException;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;
import cc.lib.utils.Table;

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
        setState(ZState.INIT);
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

        final ZUser user = getCurrentUser();

        if (quest.isQuestComplete(this)) {
            gameWon();
            return;
        }

        log.debug("runGame %s", getState());

        switch (getState()) {
            case INIT: {
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
                getCurrentUser().showMessage("Begin Round " + roundNum);
                onStartRound(roundNum++);
                currentUser = 0;
                currentCharacter = null;
                for (ZActor a : (List<ZActor>)board.getAllActors())
                    a.prepareTurn();
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
                currentCharacter = null;
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

                final ZCharacter cur = getCurrentCharacter();

                if (getCurrentCharacter().getActionsLeftThisTurn() <= 0) {
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
                    return;
                }

                List<ZMove> options = new ArrayList<>();
                options.add(ZMove.newDoNothing());

                // determine players available moves

                // add any moves determined by the quest
                quest.addMoves(this, cur, options);

                // check for organize
                if (cur.getAllEquipment().size() > 0)
                    options.add(ZMove.newOrganizeMove());

                // check for trade with another character in the same zone
                List<ZCharacter> inZone = Utils.filter(board.getCharactersInZone(cur.occupiedZone),
                        object -> {
                            if (object == cur)
                                return false;
                            return (object.canTrade() || cur.canTrade());
                        });
                if (inZone.size() > 0) {
                    options.add(ZMove.newTradeMove(inZone));
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
                    for (ZEquipSlot slot : slots) {
                        if (!((ZWeapon)cur.getSlot(slot)).isLoaded()) {
                            options.add(ZMove.newReloadMove(slot));
                        }
                    }
                }

                slots = cur.getMagicWeapons();
                if (slots.size() > 0) {
                    options.add(ZMove.newMagicAttackMove(slots));
                }

                slots = cur.getThrowableItems();
                if (slots.size() > 0) {
                    options.add(ZMove.newThrowItemMove(slots));
                }

                if (zone.type == ZZoneType.VAULT) {
                    List<ZEquipment> takables = new ArrayList<>();
                    for (ZEquipment e : quest.getVaultItems()) {
                        if (cur.canEquip(e) || !cur.isBackpackFull()) {
                            takables.add(e);
                        }
                    }
                    if (takables.size() > 0) {
                        options.add(ZMove.newPickupItemMove(takables));
                    }

                    List<ZEquipment> items = cur.getAllEquipment();
                    if (items.size() > 0) {
                        options.add(ZMove.newDropItemMove(items));
                    }

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

            case PLAYER_STAGE_CHOOSE_NEW_SKILL: {
                final ZCharacter cur = getCurrentCharacter();
                ZSkill skill = null;
                List<ZSkill> options = Arrays.asList(getCurrentCharacter().name.getSkillOptions(cur.getSkillLevel()));
                if (options.size() == 1) {
                    skill = options.get(0);
                } else {
                    skill = getCurrentUser().chooseNewSkill(this, cur, options);
                }

                if (skill != null) {
                    cur.allSkills.add(skill);
                    cur.availableSkills.add(skill);
                    stateStack.pop();
                }
                break;
            }

            case ZOMBIE_STAGE: {
                // any existing zombies take their actions
                for (int zoneIdx=0; zoneIdx < board.zones.size(); zoneIdx++) {
                    List<ZZombie> zombies = board.getZombiesInZone(zoneIdx);
                    for (ZZombie zombie : zombies) {
                        while (zombie.getActionsLeftThisTurn() > 0) {
                            List<ZCharacter> victims = board.getCharactersInZone(zombie.occupiedZone);
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
                                if (playerDefends(victim, zombie)) {
                                    getCurrentUser().showMessage(victim.name() + " defends against " + zombie.name());
                                    onCharacterDefends(victim, zombie);
                                } else
                                    victim.woundBar++;
                                if (victim.isDead()) {
                                    removeCharacter(victim);
                                    getCurrentUser().showMessage(victim.name() + " has been killed by a " + zombie.name());
                                    onCharacterPerished(victim, zombie);
                                    if (quest.isAllMustLive()) {
                                        gameLost();
                                    }
                                } else {
                                    getCurrentUser().showMessage(victim.name() + " has been wounded by a " + zombie.name());
                                    onCharacterWounded(victim, zombie);
                                }
                            } else {
                                moveZombieToTowardVisibleCharactersOrLoudestZone(zombie);
                            }
                        }
                    }
                }
                setState(ZState.BEGIN_ROUND);
                break;
            }

            default:
                throw new AssertionError("Unhandled state: " + getState());
        }
    }

    protected void onCharacterDefends(ZCharacter cur, ZZombie zombie) {

    }

    boolean playerDefends(ZCharacter cur, ZZombie zombie) {
        for (ZArmor armor : cur.getArmor()) {
            int rating = armor.getRating(zombie.type);
            if (rating > 0) {
                Integer [] dice = rollDice(1);
                if (dice[0] >= rating)
                    return true;
            }
        }
        return false;
    }

    void gameLost() {
        gameOverStatus = GAME_LOST;
        getCurrentUser().showMessage("Game Lost");
        onGameLost();
    }

    void gameWon() {
        gameOverStatus = GAME_WON;
        getCurrentUser().showMessage("Game Won!!!");
        onQuestComplete();
    }

    protected void onGameLost() {

    }

    protected void onCharacterPerished(ZCharacter character, ZZombie attacker) {

    }

    protected void onCharacterWounded(ZCharacter character, ZZombie attacker) {

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

        if (targetZone >= 0)
            log.info("%s can see players with noise level %d in zone %d and walking toward it.", zombie.name(), maxNoise, targetZone);
        else {
            // move to noisiest zone
            for (int zone=0; zone < board.zones.size(); zone++) {
                int noiseLevel = board.zones.get(zone).noiseLevel;
                if (noiseLevel > maxNoise) {
                    maxNoise = noiseLevel;
                    targetZone = zone;
                }
            }
            log.info("%s cannot see any players so moving toward loudest sound %d in zone %d.", zombie.name(), maxNoise, targetZone);
        }

        if (targetZone >= 0) {
            Collection<ZDir> paths = board.getShortestPathOptions(zombie.occupiedCell, targetZone);
            if (paths.size() > 0) {
                ZDir dir = Utils.randItem(paths); // TODO: Use Dir class instead of int
                log.debug("$s moving in direction %s", zombie.name(), dir);
                //board.moveActorInDirection(zombie, dir);
                moveActorInDirection(zombie, dir);
                return;
            }
        }

        zombie.performAction(ZActionType.DO_NOTHING, this);
    }

    protected void onStartRound(int roundNum) {

    }

    private void performMove(ZCharacter cur, ZMove move) {
        ZUser user = getCurrentUser();
        switch (move.type) {
            case DO_NOTHING:
                cur.performAction(ZActionType.DO_NOTHING, this);
                break;
            case OBJECTIVE: {
                quest.processObjective(this, cur, move);
                cur.performAction(ZActionType.OBJECTIVE, this);
                board.getZone(cur.occupiedZone).objective = false;
                break;
            }
            case ORGANNIZE: {
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
                ZEquipSlot selectedSlot = getCurrentUser().chooseSlotToOrganize(this, cur, slots);
                if (selectedSlot == null) {
                    //stateStack.push(ZState.PLAYER_STAGE_ORGANIZE_CHOOSE_ITEM);
                    break;
                }
                // choose which equipment form the slot to organize
                ZEquipment selectedEquipment = null;
                switch (selectedSlot) {
                    case BACKPACK:
                        if (cur.getNumBackpackItems() > 1) {
                            // add
                            selectedEquipment = getCurrentUser().chooseEquipment(this, cur, cur.getBackpack());
                        } else {
                            selectedEquipment = cur.getBackpackItem(0);
                        }
                        break;
                    case BODY:
                        selectedEquipment = cur.body;
                        break;
                    case LEFT_HAND:
                        selectedEquipment = cur.leftHand;
                        break;
                    case RIGHT_HAND:
                        selectedEquipment = cur.rightHand;
                        break;
                }

                if (selectedEquipment == null)
                    break;

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
                move = user.chooseMove(this, cur, options);
                if (move != null) {
                    performMove(cur, move);
                }
                break;
            }
            case TRADE:
                ZCharacter other = null;
                if (move.list.size() == 1) {
                    other = (ZCharacter)move.list.get(0);
                } else {
                    other = user.chooseTradeCharacter(this, cur, move.list);
                }
                if (other != null) {
                    List<ZMove> options = new ArrayList<>();
                    // we can take if our backpack is not full or give if their packpack is not full
                    if (!other.isBackpackFull()) {
                        for (ZEquipment e : cur.getBackpack()) {
                            options.add(ZMove.newGiveMove(other, e));
                        }
                    }

                    if (!cur.isBackpackFull()) {
                        for (ZEquipment e : other.getBackpack()) {
                            options.add(ZMove.newTakeMove(other, e));
                        }
                    }

                    move = user.chooseMove(this, cur, options);
                    if (move != null) {
                        performMove(cur, move);
                    }
                }
                break;
            case WALK: {
                Integer zone = user.chooseZoneToWalk(this, cur, move.list);
                if (zone != null) {
                    moveActor(cur, zone);
                    cur.performAction(ZActionType.MOVE, this);
                }
                break;
            }
            case WALK_DIR: {
                moveActorInDirection(cur, move.dir);
                break;
            }
            case MELEE_ATTACK: {
                List<ZEquipSlot> weapons = move.list;
                ZEquipSlot slot = null;
                if (weapons.size() > 1) {
                    slot = user.chooseWeaponSlot(this, cur, weapons);
                } else {
                    slot = weapons.get(0);
                }
                if (slot != null) {
                    ZWeaponStat stat = cur.getWeaponStat(slot, ZActionType.MELEE, this);
                    List<ZZombie> zombies = board.getZombiesInZone(cur.occupiedZone);
                    if (zombies.size() > 1)
                        Collections.sort(zombies, (o1, o2) -> Integer.compare(o2.type.minDamageToDestroy, o1.type.minDamageToDestroy));
                    while (zombies.size() > 0 && zombies.get(0).type.minDamageToDestroy > stat.damagePerHit) {
                        zombies.remove(0);
                    }
                    Integer [] result = rollDice(stat.numDice);
                    int hits = 0;
                    for (int i=0; i<result.length && zombies.size() > 0; i++) {
                        if (result[i] >= stat.dieRollToHit) {
                            ZZombie z = zombies.remove(0);
                            addExperience(cur, z.type.expProvided);
                            board.removeActor(z);
                            onZombieDestroyed(z);
                            hits ++;
                        }
                    }
                    cur.performAction( ZActionType.MELEE,this);
                    user.showMessage(currentCharacter.name() + " Scored " + hits + " hits");
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
                    slot = user.chooseWeaponSlot(this, cur, weapons);
                } else {
                    slot = weapons.get(0);
                }
                if (slot != null) {
                    ZWeaponStat stat = cur.getWeaponStat(slot, actionType, this);
                    List<Integer> zones = new ArrayList<>();
                    for (int range = stat.minRange; range <=stat.maxRange; range++) {
                        zones.addAll(board.getAccessableZones(cur.occupiedZone, range));
                    }
                    if (zones.size() > 0) {
                        Integer zone = user.chooseZoneForAttack(this, cur, zones);
                        if (zone != null) {
                            // process a ranged attack
                            ZWeapon weapon = (ZWeapon)cur.getSlot(slot);
                            if (!weapon.isLoaded()) {
                                getCurrentUser().showMessage("CLICK! Weapon not loaded!");
                                onWeaponGoesClick(slot);
                            } else {
                                weapon.fireWeapon();
                                List<ZZombie> zombies = board.getZombiesInZone(zone);
                                if (zombies.size() > 1)
                                    Collections.sort(zombies, (o1, o2) -> Integer.compare(o1.type.rangedPriority, o2.type.rangedPriority));
                                Integer[] result = rollDice(stat.numDice);
                                int hits = 0;
                                for (int i = 0; i < result.length && zombies.size() > 0; i++) {
                                    if (result[i] >= stat.dieRollToHit) {
                                        ZZombie zombie = zombies.remove(0);
                                        if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                            addExperience(cur, zombie.type.expProvided);
                                            board.removeActor(zombie);
                                            onZombieDestroyed(zombie);
                                            hits++;
                                        }
                                    }
                                }
                                user.showMessage(currentCharacter.name() + " Scored " + hits + " hits");
                            }
                            cur.performAction(actionType,this);
                        }
                    }
                }
                break;
            }

            case THROW_ITEM: {
                ZEquipSlot slot = null;
                List<ZEquipSlot> slots = move.list;
                if (slots.size() == 1)
                    slot = slots.get(0);
                else
                    slot = getCurrentUser().chooseItemToThrow(this, cur, slots);
                if (slot != null) {
                    ZItem toThrow = cur.getSlot(slot);
                    List<Integer> zones = board.getAccessableZones(cur.occupiedZone, 1);
                    zones.add(cur.occupiedZone);
                    Integer zoneIdx = getCurrentUser().chooseZonetoThrowItem(this, cur, toThrow, zones);
                    if (zoneIdx != null) {
                        switch (toThrow.type) {
                            case DRAGON_BILE:
                                board.getZone(zoneIdx).dragonBile = true;
                                getCurrentUser().showMessage(cur.name() + " threw the drogon Bile!");
                                onDragonBilePlaced(cur, zoneIdx);
                                break;
                            case TORCH: {
                                ZZone zone = board.getZone(zoneIdx);
                                if (!zone.dragonBile) {
                                    getCurrentUser().showMessage("Throwing the Torch had no effect");
                                } else {
                                    zone.dragonBile = false;
                                    int exp = 0;
                                    int num=0;
                                    for (ZZombie z : board.getZombiesInZone(zoneIdx)) {
                                        exp += z.type.expProvided;
                                        board.removeActor(z);
                                        num++;
                                        onZombieDestroyed(z);
                                    }
                                    getCurrentUser().showMessage(cur.name() + " threw the torch exploding the dragon bile and destroying " + num + " zombies for " + exp + " total experience pts!");
                                }
                                break;
                            }
                            default:
                                throw new GException("Unhandled case: " + toThrow.type);
                        }
                        cur.removeEquipment(toThrow, slot);
                        cur.performAction(ZActionType.THROW_ITEM, this);
                    }

                }
                break;
            }

            case RELOAD: {
                ZWeapon weapon = cur.getSlot(move.fromSlot);
                ((ZWeapon)cur.getSlot(move.fromSlot)).reload();
                user.showMessage(currentCharacter.name() + " Reloaded their " + weapon.type);
                cur.performAction(ZActionType.RELOAD, this);
                break;
            }
            case TOGGLE_DOOR: {
                List<ZDoor> doors = move.list;
                ZDoor door = user.chooseDoorToToggle(this, cur, doors);
                if (door != null) {
                    if (door.isClosed(board)) {
                        if (!door.isJammed() || cur.tryOpenDoor(this)) {
                            door.toggle(board);
                            //getCurrentUser().showMessage(currentCharacter.name() + " has opened a " + door.name());
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
                        cur.performAction(ZActionType.OPEN_DOOR, this);
                    } else {
                        cur.performAction(ZActionType.CLOSE_DOOR, this);
                        door.toggle(board);
                    }
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
                        spawnZombies(1, ZZombieType.WALKERS, cur.occupiedZone);
                        searchables.addFirst(equip);
                    } else {
                        user.showMessage(cur.name() + " Found a " + equip);
                        cur.equip(equip);
                    }
                    cur.performAction(ZActionType.SEARCH, this);
                }
                break;
            }
            case EQUIP: {
                ZEquipment prev = cur.getSlot(move.toSlot);
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment, move.fromSlot);
                }
                cur.attachEquipment(move.equipment, move.toSlot);
                if (prev != null && !cur.isBackpackFull()) {
                    cur.attachEquipment(prev, ZEquipSlot.BACKPACK);
                }
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            }
            case UNEQUIP:
                cur.removeEquipment(move.equipment, move.fromSlot);
                cur.attachEquipment(move.equipment, ZEquipSlot.BACKPACK);
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            case TAKE:
                move.character.removeEquipment(move.equipment, ZEquipSlot.BACKPACK);
                cur.attachEquipment(move.equipment, ZEquipSlot.BACKPACK);
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            case GIVE:
                cur.removeEquipment(move.equipment, ZEquipSlot.BACKPACK);
                move.character.attachEquipment(move.equipment, ZEquipSlot.BACKPACK);
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            case DISPOSE:
                cur.removeEquipment(move.equipment, move.fromSlot);
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            case CONSUME:
                performConsume(cur, move);
                cur.performAction(ZActionType.CONSUME, this);
                break;
            case PICKUP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToPickup(this, cur, move.list);
                if (equip != null) {
                    quest.getVaultItems().remove(equip);
                    cur.equip(equip);
                    cur.performAction(ZActionType.PICKUP_ITEM, this);
                }
                break;
            }
            case DROP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToDrop(this, cur, move.list);
                if (equip != null) {
                    quest.getVaultItems().add(equip);
                    cur.removeEquipment(equip);
                    cur.performAction(ZActionType.DROP_ITEM, this);
                }
                break;
            }
            default:
                log.error("Unhandled move: %s", move.type);
        }
    }

    protected void onWeaponGoesClick(ZEquipSlot slot) {

    }

    protected void onDoorOpened(ZDoor door) {

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
        switch (item.getType()) {
            case WATER:
            case APPLES:
            case SALTED_MEAT:
                c.removeEquipment(item, slot);
                addExperience(c, 1);
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
        onCharacterGainedExperience(c, pts);
        if (c.getSkillLevel() != sl) {
            getCurrentUser().showMessage(c.name() + " has gained the " + c.getSkillLevel() + " skill level");
            stateStack.push(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL);
        }
    }

    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        log.info("%s gained %d experence!", c.name, points);
    }

    Integer [] rollDice(int num) {
        Integer [] result = new Integer[num];
        for (int i=0; i<num; i++) {
            result[i] = Utils.randRange(1,6);
        }
        getCurrentUser().showMessage("Rolled a " + new Table().addRow(result).toString());
        onRollDice(result);
        return result;
    }

    protected void onRollDice(Integer [] roll) {
        log.info("Rolling dice result is: %s", Arrays.toString(roll));
    }

    protected void onZombieDestroyed(ZZombie zombie) {
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
    }

    private void spawnZombies(int zoneIdx, ZSkillLevel level) {
        log.info("Random zombie spawn for zone %d and level %s", zoneIdx, level);
        if (!doubleSpawn) {
            if (Utils.rand() % 10 == 0) {
                doubleSpawn = true;
                getCurrentUser().showMessage("DOUBLE SPAWN!");
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
            case YELLOW:
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

    void moveActorInDirection(ZActor actor, ZDir dir) {
        Grid.Pos pos = actor.occupiedCell;
        Grid.Pos next = dir.getAdjacent(pos);
        int curZone = actor.occupiedZone;
        int nxtZone = board.getCell(next).zoneIndex;
        board.removeActor(actor);
        board.addActorToCell(actor, next);
        if (nxtZone != curZone) {
            actor.performAction(ZActionType.MOVE, this);
        }
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
        searchables.addAll(make(2, ZArmorType.CHAINMAIL));
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
        getCurrentUser().showMessage("Noise was made in zone " + zoneIdx);
        onNoiseAdded(zoneIdx);
    }

    protected void onNoiseAdded(int zoneIndex) {
    }

}
