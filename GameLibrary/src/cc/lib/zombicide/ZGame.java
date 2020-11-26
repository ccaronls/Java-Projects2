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

import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
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
    int spawnMultiplier=1;
    int roundNum=0;
    int gameOverStatus=0; // 0 == in play, 1, == game won, 2 == game lost
    ZQuests currentQuest;

    private void initGame() {
        initSearchables();
        for (ZCharacter c : getAllCharacters())
            c.reset();
        roundNum = 0;
        gameOverStatus = 0;
        spawnMultiplier = 1;
        setState(ZState.BEGIN_ROUND);
    }

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

    public void reload() {
        loadQuest(currentQuest);
    }

    public int getNumKills(ZZombieType type) {
        int num=0;
        for (ZCharacter c : getAllCharacters()) {
            num += c.kills[type.ordinal()];
        }
        return num;
    }

    public void loadQuest(ZQuests quest) {
        this.currentQuest = quest;
        this.quest = quest.load();
        board = this.quest.loadBoard();
        initGame();
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
            for (ZDir dir : ZDir.getCompassValues()) {
                switch (cell.getWallFlag(dir)) {
                    case CLOSED:
                    case OPEN: {
                        Grid.Pos pos = it.getPos();
                        Grid.Pos next = board.getAdjacent(pos, dir);
                        zone.doors.add(new ZDoor(pos, next, dir, GColor.RED));
                    }
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
                    spawnZombies(1, ZZombieType.Walker, cell.zoneIndex);
                    break;
                case RUNNER:
                    spawnZombies(1, ZZombieType.Runner, cell.zoneIndex);
                    break;
                case FATTY:
                    spawnZombies(1, ZZombieType.Fatty, cell.zoneIndex);
                    break;
                case NECRO:
                    spawnZombies(1, ZZombieType.Necromancer, cell.zoneIndex);
                    break;
                case ABOMINATION:
                    spawnZombies(1, ZZombieType.Abomination, cell.zoneIndex);
                    break;
                case SPAWN:
                    zone.isSpawn = true;
                    break;
                case OBJECTIVE_BLACK:
                case OBJECTIVE_RED:
                case OBJECTIVE_BLUE:
                case OBJECTIVE_GREEN:
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
                            zone.doors.add(new ZDoor(it.getPos(), it2.getPos(), cell.environment == ZCell.ENV_VAULT ? ZDir.ASCEND : ZDir.DESCEND, GColor.RED));
                            break;
                        }
                    }
                    break;
                }
            }
        }
        this.quest.init(this);
    }

    public void spawnZombies(int count, ZZombieType name, int zone) {

        count *= spawnMultiplier;
        spawnMultiplier = 1;

        do {
            final ZZombieType _name = name;
            int numOnBoard = Utils.filter(board.getAllZombies(), object -> object.type == _name).size();
            log.debug("Num %s on board is %d and trying to spawn %d more", name, numOnBoard, count);
            if (numOnBoard + count > quest.getMaxNumZombiesOfType(name)) {
                switch (name) {
                    case Necromancer:
                        name = ZZombieType.Abomination;
                        continue;
                    case Abomination:
                        name = ZZombieType.Fatty;
                        continue;
                    case Fatty:
                    case Runner:
                        name = ZZombieType.Walker;
                        continue;
                }
            }
            spawnZombies(name, count, zone);
        } while (false);
    }

    private void spawnZombies(ZZombieType type, int count, int zone) {
        for (int i = 0; i < count; i++) {
            ZZombie zombie = new ZZombie(type, zone);
            switch (zombie.type) {
                case Necromancer:
                    board.setSpawnZone(zone, true);
            }
            board.addActor(zombie, zone);
            onZombieSpawned(zombie);
        }
    }

    protected void onZombieSpawned(ZZombie zombie) {
        log.info("A %s Zombie has Spawned at zone %d", zombie.type, zombie.occupiedZone);
    }

    public ZState getState() {
        if (stateStack.empty())
            stateStack.push(ZState.BEGIN_ROUND);
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
        log.debug("runGame %s", getState());
        if (!isGameSetup()) {
            log.error("Invalid Game");
            assert(false);
            return;
        }

        if (isGameOver())
            return;

        if (getAllLivingCharacters().size() == 0) {
            gameLost();
            return;
        }

        if (quest.isQuestFailed(this)) {
            gameLost();
            return;
        }

        if (quest.isQuestComplete(this)) {
            gameWon();
            return;
        }

        final ZUser user = getCurrentUser();

        switch (getState()) {
            case BEGIN_ROUND: {
                if (roundNum > 0)
                    setState(ZState.SPAWN);
                else
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER);
                getCurrentUser().showMessage("Begin Round " + roundNum);
                onStartRound(roundNum++);
                currentUser = 0;
                currentCharacter = null;
                for (ZActor a : board.getAllActors())
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
                    if (!c.isDead() && c.getActionsLeftThisTurn() > 0) {
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
                for (ZSkill skill : cur.availableSkills) {
                    skill.addSpecialMoves(this, cur, options);
                }

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

                options.add(ZMove.newMakeNoiseMove(cur.occupiedZone));

                // check for move up, down, right, left
                List<Integer> accessableZones = board.getAccessableZones(cur.occupiedZone, 1, ZActionType.MOVE);
                if (accessableZones.size() > 0)
                    options.add(ZMove.newWalkMove(accessableZones));

                {
                    List<ZWeapon> melee = cur.getMeleeWeapons();
                    if (melee.size() > 0) {
                        options.add(ZMove.newMeleeAttackMove(melee));
                    }
                }

                {
                    List<ZWeapon> ranged = cur.getRangedWeapons();
                    if (ranged.size() > 0) {
                        options.add(ZMove.newRangedAttackMove(ranged));
                        for (ZWeapon slot : ranged) {
                            if (!slot.isLoaded()) {
                                options.add(ZMove.newReloadMove(slot));
                                if (cur.isDualWeilding())
                                    break;
                            }
                        }
                    }
                }

                {
                    List<ZWeapon> magic = cur.getMagicWeapons();
                    if (magic.size() > 0) {
                        options.add(ZMove.newMagicAttackMove(magic));
                    }
                }

                {
                    List<ZItem> items = cur.getThrowableItems();
                    if (items.size() > 0) {
                        options.add(ZMove.newThrowItemMove(items));
                    }
                }

                if (zone.type == ZZoneType.VAULT) {
                    List<ZEquipment> takables = new ArrayList<>();
                    for (ZEquipment e : quest.getVaultItems(cur.occupiedZone)) {
                        if (cur.getEquipableSlots(e).size() > 0) {
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
                    if (door.isLocked(board))
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
                ZSkill skill;
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
                                    int v0 = o1.getArmorRating(zombie.type) - o1.woundBar;
                                    int v1 = o2.getArmorRating(zombie.type) - o2.woundBar;
                                    return Integer.compare(v1, v0);
                                });
                            }
                            if (victims.size() > 0) {
                                ZCharacter victim = victims.get(0);
                                zombie.performAction(ZActionType.MELEE, this);
                                if (playerDefends(victim, zombie.type)) {
                                    getCurrentUser().showMessage(victim.name() + " defends against " + zombie.name());
                                    onCharacterDefends(victim, zombie);
                                } else {
                                    playerWounded(victim, 1, zombie.type.name());
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

    private boolean playerDefends(ZCharacter cur, ZZombieType type) {
        for (ZArmor armor : cur.getArmorForDefense()) {
            int rating = 6 - armor.getRating(type);
            if (rating > 0) {
                Integer [] dice = cur.canReroll(ZActionType.DEFEND) ?
                        rollDiceWithRerollOption(1, rating, 0, 1) :
                        rollDice(1);
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

    void playerWounded(ZCharacter victim, int amount, String reason) {
        victim.woundBar += amount;
        if (victim.isDead()) {
            removeCharacter(victim);
            getCurrentUser().showMessage(victim.name() + " has been killed by a " + reason);
            onCharacterPerished(victim);
        } else {
            getCurrentUser().showMessage(victim.name() + " has been wounded by a " + reason);
            onCharacterWounded(victim);
        }

    }


    protected void onCharacterPerished(ZCharacter character) {

    }

    protected void onCharacterWounded(ZCharacter character) {

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
                log.debug("%s moving in direction %s", zombie.name(), dir);
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
        log.debug("performMove:%s", move);
        ZUser user = getCurrentUser();
        switch (move.type) {
            case DO_NOTHING:
                cur.performAction(ZActionType.DO_NOTHING, this);
                break;
            case OBJECTIVE: {
                getCurrentUser().showMessage(cur.name() + " Found an OBJECTIVE");
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
                ZCharacter other;
                if (move.list.size() == 1) {
                    other = (ZCharacter)move.list.get(0);
                } else {
                    other = user.chooseTradeCharacter(this, cur, move.list);
                }
                if (other != null) {
                    List<ZMove> options = new ArrayList<>();
                    // we can take if our backpack is not full or give if their packpack is not full
                    for (ZEquipment eq : cur.getAllEquipment()) {
                        if (other.getEquipableSlots(eq) != null) {
                            options.add(ZMove.newGiveMove(other, eq));
                        }
                    }

                    for (ZEquipment eq : other.getAllEquipment()) {
                        if (cur.getEquipableSlots(eq) != null) {
                            options.add(ZMove.newTakeMove(other, eq));
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
                List<ZWeapon> weapons = move.list;
                ZWeapon slot = null;
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
                    int hits = resolveHits(cur, zombies.size(), ZActionType.MELEE, stat.numDice, stat.dieRollToHit, zombies.size()/2-1, zombies.size()/2+1);

                    for (int i=0; i<hits && zombies.size() > 0; i++) {
                        ZZombie z = zombies.remove(0);
                        addExperience(cur, z.type.expProvided);
                        destroyZombie(z, cur);
                    }
                    if (slot.isAttackNoisy()) {
                        addNoise(cur.occupiedZone, 1);
                    }
                    cur.performAction( ZActionType.MELEE,this);
                    user.showMessage(currentCharacter.name() + " Scored " + hits + " hits");
                }
                break;
            }
            case MAGIC_ATTACK:
            case RANGED_ATTACK: {
                // rules same for both kinda
                List<ZWeapon> weapons = move.list;
                ZWeapon slot = null;
                if (weapons.size() > 1) {
                    slot = user.chooseWeaponSlot(this, cur, weapons);
                } else {
                    slot = weapons.get(0);
                }
                ZActionType actionType = null;
                switch (move.type) {
                    case MAGIC_ATTACK:
                        actionType = ZActionType.MAGIC;
                        break;
                    case RANGED_ATTACK:
                        if (slot.type.usesArrows)
                            actionType = ZActionType.RANGED_ARROWS;
                        else if (slot.type.usesBolts)
                            actionType = ZActionType.RANGED_BOLTS;
                        else
                            assert (false);
                        break;
                    default:
                        assert (false);
                        return;
                }

                if (slot != null) {
                    ZWeaponStat stat = cur.getWeaponStat(slot, actionType, this);
                    List<Integer> zones = new ArrayList<>();
                    for (int range = stat.minRange; range <=stat.maxRange; range++) {
                        zones.addAll(board.getAccessableZones(cur.occupiedZone, range, actionType));
                    }
                    if (zones.size() > 0) {
                        Integer zone = user.chooseZoneForAttack(this, cur, zones);
                        if (zone != null) {
                            // process a ranged attack
                            if (!slot.isLoaded()) {
                                getCurrentUser().showMessage("CLICK! Weapon not loaded!");
                                onWeaponGoesClick(slot);
                            } else {
                                slot.fireWeapon();
                                List<ZZombie> zombies = board.getZombiesInZone(zone);
                                if (zombies.size() > 1)
                                    Collections.sort(zombies, (o1, o2) -> Integer.compare(o1.type.rangedPriority, o2.type.rangedPriority));
                                // find the first zombie whom we cannot destory and remove that one and all after since ranged priority
                                {
                                    int numHittable = 0;
                                    for (ZZombie z : zombies) {
                                        if (z.type.minDamageToDestroy <= stat.damagePerHit)
                                            numHittable++;
                                    }
                                    log.debug("There are %d hittable zombies", numHittable);
                                    while (zombies.size() > numHittable)
                                        zombies.remove(zombies.size() - 1);
                                }

                                int hits = resolveHits(cur, zombies.size(), actionType, stat.numDice, stat.dieRollToHit, zombies.size()/2-1, zombies.size()/2+1);
                                for (int i=0; i<hits; i++) {
                                    ZZombie zombie = zombies.remove(0);
                                    if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                        addExperience(cur, zombie.type.expProvided);
                                        destroyZombie(zombie, cur);
                                    }
                                }

                                int misses = stat.numDice - hits;
                                List<ZCharacter> friendlyFireOptions = Utils.filter(board.getCharactersInZone(zone), object -> object != cur);
                                if (friendlyFireOptions.size() > 1) {
                                    // sort them in same way we would sort zombie attacks
                                    Collections.sort(friendlyFireOptions, (o1, o2) -> {
                                        int v0 = o1.getArmorRating(ZZombieType.Walker) - o1.woundBar;
                                        int v1 = o2.getArmorRating(ZZombieType.Walker) - o2.woundBar;
                                        return Integer.compare(v1, v0);
                                    });
                                }
                                for (int i=0; i<misses && friendlyFireOptions.size() > 0; i++) {
                                    // friendy fire!
                                    ZCharacter victim = friendlyFireOptions.get(0);
                                    if (playerDefends(victim, ZZombieType.Walker)) {
                                        getCurrentUser().showMessage(victim.name() + " defended themself from friendly fire!");
                                    } else {
                                        playerWounded(victim, stat.damagePerHit, "Freindly Fire!");
                                        if (victim.isDualWeilding())
                                            friendlyFireOptions.remove(0);
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
                ZItem slot = null;
                List<ZItem> slots = move.list;
                if (slots.size() == 1)
                    slot = slots.get(0);
                else
                    slot = getCurrentUser().chooseItemToThrow(this, cur, slots);
                if (slot != null) {
                    List<Integer> zones = board.getAccessableZones(cur.occupiedZone, 1, ZActionType.THROW_ITEM);
                    zones.add(cur.occupiedZone);
                    Integer zoneIdx = getCurrentUser().chooseZonetoThrowItem(this, cur, slot, zones);
                    if (zoneIdx != null) {
                        switch (slot.type) {
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
                                    getCurrentUser().showMessage(cur.name() + " threw the torch exploding the dragon bile!");
                                    for (ZActor a : board.getActorsInZone(zoneIdx)) {
                                        if (a instanceof ZZombie) {
                                            ZZombie z = (ZZombie)a;
                                            exp += z.type.expProvided;
                                            destroyZombie(z, cur);
                                            num++;
                                        } else if (a instanceof ZCharacter) {
                                            // characters caught in the zone get wounded
                                            ZCharacter c = (ZCharacter)a;
                                            playerWounded(c, 4, "Exploding Dragon Bile");
                                        }
                                    }
                                    addExperience(cur, exp);
                                    getCurrentUser().showMessage(cur.name() + "Destroyed " + num + " zombies for " + exp + " total experience pts!");
                                }
                                break;
                            }
                            default:
                                throw new GException("Unhandled case: " + slot.type);
                        }
                        cur.removeEquipment(slot);//, slot);
                        cur.performAction(ZActionType.THROW_ITEM, this);
                    }

                }
                break;
            }

            case RELOAD: {
                ZWeapon weapon = (ZWeapon)move.equipment;
                if (cur.isDualWeilding()) {
                    ((ZWeapon)cur.getSlot(ZEquipSlot.LEFT_HAND)).reload();
                    ((ZWeapon)cur.getSlot(ZEquipSlot.RIGHT_HAND)).reload();
                    user.showMessage(currentCharacter.name() + " Reloaded both their " + weapon.type + "s");
                } else {
                    weapon.reload();
                    user.showMessage(currentCharacter.name() + " Reloaded their " + weapon.type);
                }
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
                            if (board.getZone(board.getCell(otherSide.getCellPosStart()).zoneIndex).canSpawn()) {
                                ZSkillLevel highest = getHighestSkillLevel();
                                HashSet<Integer> spawnZones = new HashSet<>();
                                board.getUndiscoveredIndoorZones(otherSide.getCellPosStart(), spawnZones);
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
                    quest.onEquipmentFound(this, equip);
                    if (equip.getType() == ZItemType.AAHHHH) {
                        getCurrentUser().showMessage("Aaaahhhh!!!");
                        // spawn zombie right here right now
                        spawnZombies(1, ZZombieType.Walker, cur.occupiedZone);
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
                move.character.removeEquipment(move.equipment);
                cur.attachEquipment(move.equipment);
                cur.performAction(ZActionType.ORGANIZE, this);
                break;
            case GIVE:
                cur.removeEquipment(move.equipment);
                move.character.attachEquipment(move.equipment);
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
                    quest.pickupItem(cur.occupiedZone, equip);
                    cur.equip(equip);
                    cur.performAction(ZActionType.PICKUP_ITEM, this);
                }
                break;
            }
            case DROP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToDrop(this, cur, move.list);
                if (equip != null) {
                    quest.dropItem(cur.occupiedZone, equip);
                    cur.removeEquipment(equip);
                    cur.performAction(ZActionType.DROP_ITEM, this);
                }
                break;
            }
            case MAKE_NOISE: {
                int maxNoise = board.getMaxNoiseLevelZone().noiseLevel;
                addNoise(move.integer, maxNoise+1 - board.getZone(move.integer).noiseLevel);
                getCurrentUser().showMessage(cur.name() + " made alot of noise to draw the zombies!");
                cur.performAction(ZActionType.MAKE_NOISE, this);
                break;
            }
            case SHOVE: {
                Integer targetZone = getCurrentUser().chooseZoneToShove(this, cur, move.list);
                if (targetZone != null) {
                    // shove all zombies in this zone into target zone
                    for (ZZombie z : board.getZombiesInZone(cur.getOccupiedZone())) {
                        board.removeActor(z);
                        board.addActor(z, targetZone);
                    }
                    cur.performAction(ZActionType.SHOVE, this);
                }
                break;
            }
            default:
                log.error("Unhandled move: %s", move.type);
        }
    }

    ZDir getDirection(int fromZoneIdx, int toZoneIdx) {
        assert(fromZoneIdx != toZoneIdx);

        ZZone fromZone = board.getZone(fromZoneIdx);
        ZZone toZone   = board.getZone(toZoneIdx);

        if (fromZone.type == ZZoneType.VAULT) {
            return ZDir.ASCEND;
        }

        if (toZone.type == ZZoneType.VAULT) {
            return ZDir.DESCEND;
        }

        for (Grid.Pos fromPos : fromZone.cells) {
            for (Grid.Pos toPos : toZone.cells) {
                ZDir dir = ZDir.getDirFrom(fromPos, toPos);
                if (dir != null)
                    return dir;
            }
        }
        assert(false);
        return null;
    }

    private int resolveHits(ZCharacter cur, int maxHits, ZActionType actionType, int numDice, int dieRollToHit, int minHitsForAutoReroll, int maxHitsForAutoNoReroll) {
        Integer [] result;
        if (cur.canReroll(actionType)) {
            result = rollDiceWithRerollOption(numDice, dieRollToHit, minHitsForAutoReroll, maxHitsForAutoNoReroll);
        } else {
            result = rollDice(numDice);
        }
        int hits = 0;
        boolean isRoll6Plus1Die = cur.isRoll6Plus1Die(actionType);
        do {
            for (int i=0; i<result.length; i++) {
                if (result[i] >= dieRollToHit) {
                    hits ++;
                }
            }
            // look for Roll6Plus1
            if (!isRoll6Plus1Die || hits >= maxHits)
                break;
            int numSixes = Utils.filterItems(object -> object == 6, result).size();
            if (numSixes > 0) {
                getCurrentUser().showMessage("Rolled " + numSixes + " 6s + 1Die roll eash!");
                result = rollDice(numSixes);
            } else {
                break;
            }
        } while (true);
        return hits;
    }

    protected void onWeaponGoesClick(ZWeapon weapon) {

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
                addExperience(c, item.getType().getExpWhenConsumed());
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

    Integer [] rollDiceWithRerollOption(int numDice, int dieNumToHit, int minHitsForAutoReroll, int maxHitsForAutoNoReroll) {
        Integer [] dice = rollDice(numDice);
        int hits = 0;
        for (int d : dice) {
            if (d >= dieNumToHit)
                hits++;
        }

        if (hits >= maxHitsForAutoNoReroll) {
            return dice;
        }

        if (hits > minHitsForAutoReroll) {
            ZMove plentyOMove = getCurrentUser().chooseMove(this, getCurrentCharacter(),
                    Arrays.asList(ZMove.newReRollMove(), ZMove.newKeepRollMove()));

            if (plentyOMove != null) {
                if (plentyOMove.type == ZMoveType.KEEP_ROLL)
                    return dice;
            }
        }

        getCurrentUser().showMessage("Bonus roll dice!");
        return rollDice(numDice);
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

    private void destroyZombie(ZZombie zombie, ZCharacter killer) {
        board.removeActor(zombie);
        killer.onKilledZombie(zombie);
        onZombieDestroyed(killer, zombie);
    }

    protected void onZombieDestroyed(ZCharacter c, ZZombie zombie) {
        log.info("%s Zombie %s destroyed for %d experience", c.name(), zombie.type.name(), zombie.type.expProvided);
    }

    public List<ZCharacter> getAllCharacters() {
        if (users.length == 1)
            return new ArrayList<>(users[0].characters);
        List<ZCharacter> all = new ArrayList<>();
        for (ZUser user : users) {
            all.addAll(user.characters);
        }
        return all;
    }

    public List<ZCharacter> getAllLivingCharacters() {
        return Utils.filter(getAllCharacters(), object -> !object.isDead());
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        return board.getZombiesInZone(zoneIndex).size() == 0;
    }

    protected void onDoubleSpawn() {
    }

    private void extraActivation(ZZombieType name) {
        getCurrentUser().showMessage("Extra Activation for " + name);
        for (ZZombie z : Utils.filter(board.getAllZombies(), object -> object.type == name)) {
            z.extraActivation();
        }
    }

    public void spawnZombies(int zoneIdx) {
        spawnZombies(zoneIdx, getHighestSkillLevel());
    }

    private void spawnZombies(int zoneIdx, ZSkillLevel level) {
        log.info("Random zombie spawn for zone %d and level %s", zoneIdx, level);
        if (Utils.rand() % (spawnMultiplier*10) == 0) {
            spawnMultiplier *= 2;
            getCurrentUser().showMessage("DOUBLE SPAWN!");
            onDoubleSpawn();
            return;
        }
        do {
            switch (Utils.chooseRandomFromSet(2, 1, 1, 4, 3, 2)) {
                case 0: // extra activation walker
                    switch (level) {
                        case BLUE:
                            break; // do nothing
                        default:
                            // Extra Activation Walker
                            if (Utils.filter(board.getAllZombies(), object -> object.type == ZZombieType.Walker).size() == 0)
                                continue;
                            extraActivation(ZZombieType.Walker);
                    }
                    break;
                case 1: // extra activation runner
                    switch (level) {
                        case BLUE:
                            break; // do nothing
                        default: // Extra Activation Runner x 1 (Except Blue)
                            if (Utils.filter(board.getAllZombies(), object -> object.type == ZZombieType.Runner).size() == 0)
                                continue;
                            extraActivation(ZZombieType.Runner);
                    }
                    break;
                case 2: // extra activation fatty
                    switch (level) {
                        case BLUE:
                            break; // do nothing
                        default: // Extra Activation Fatty x 1 (Except Blue)
                            if (Utils.filter(board.getAllZombies(), object -> object.type == ZZombieType.Fatty).size() == 0)
                                continue;
                            extraActivation(ZZombieType.Fatty);
                    }
                    break;
                case 3: // standard zombie invasion 1
                    /*
                  Standard Zombie Invasion 1 x 2
                  Red - Abomination x 1
                  Orange - Runner x 2
                  Yellow - Walker x 3
                  Blue - Walker x 1
                     */
                    switch (level) {
                        case RED:
                            spawnZombies(1, ZZombieType.Abomination, zoneIdx);
                            break;
                        case ORANGE:
                            spawnZombies(2, ZZombieType.Runner, zoneIdx);
                            break;
                        case YELLOW:
                            spawnZombies(3, ZZombieType.Walker, zoneIdx);
                            break;
                        case BLUE:
                            spawnZombies(1, ZZombieType.Walker, zoneIdx);
                            break;
                    }
                    break;
                case 4: // standard zombie invasion 2
                    /*
                  Standard Zombie Invasion 2 x 2
                  Red - Fatty x 2
                  Orange - Walker x 5
                  Yellow - Runner x 2
                  Blue - Nothing
                     */
                    switch (level) {
                        case RED:
                            spawnZombies(2, ZZombieType.Fatty, zoneIdx);
                            break;
                        case ORANGE:
                            spawnZombies(5, ZZombieType.Walker, zoneIdx);
                            break;
                        case YELLOW:
                            spawnZombies(2, ZZombieType.Runner, zoneIdx);
                            break;
                        case BLUE:
                    }
                    break;
                case 5: // standard zombie invasion 3
                    /*
                  Standard Zombie Invasion 3 x 2
                  Red - Walker x 6
                  Orange - Abomination x 1
                  Yellow - Fatty x 1
                  Blue - Walker x 2
                     */
                    switch (level) {
                        case RED:
                            spawnZombies(6, ZZombieType.Walker, zoneIdx);
                            break;
                        case ORANGE:
                            spawnZombies(1, ZZombieType.Abomination, zoneIdx);
                            break;
                        case YELLOW:
                            spawnZombies(1, ZZombieType.Fatty, zoneIdx);
                            break;
                        case BLUE:
                            spawnZombies(2, ZZombieType.Walker, zoneIdx);
                            break;
                    }
                    break;
            }
        } while (false);
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
        Grid.Pos next = board.getAdjacent(pos, dir);
        int curZone = actor.occupiedZone;
        int nxtZone = board.getCell(next).zoneIndex;
        GRectangle start = board.getCell(pos).getQuadrant(actor.occupiedQuadrant);
        board.removeActor(actor);
        board.addActorToCell(actor, next);
        GRectangle end   = board.getCell(next).getQuadrant(actor.occupiedQuadrant);
        onActorMoved(actor, start, end);
        if (nxtZone != curZone) {
            actor.performAction(ZActionType.MOVE, this);
        }
    }

    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end) {

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
        searchables.addAll(make(4, ZItemType.DRAGON_BILE));
        searchables.addAll(make(4, ZWeaponType.FIREBALL));
        searchables.addAll(make(2, ZWeaponType.GREAT_SWORD));
        searchables.addAll(make(1, ZWeaponType.HAMMER));
        searchables.addAll(make(2, ZWeaponType.HAND_CROSSBOW));
        searchables.addAll(make(1, ZSpellType.HEALING));
//        searchables.addAll(make(1, ZWeaponType.INFERNO));
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
        searchables.addAll(make(4, ZItemType.TORCH));
        searchables.addAll(make(2, ZItemType.WATER));
        Utils.shuffle(searchables);
    }

    public void addNoise(int zoneIdx, int noise) {
        board.getZone(zoneIdx).noiseLevel += noise;
//        getCurrentUser().showMessage("Noise was made in zone " + zoneIdx);
        onNoiseAdded(zoneIdx);
    }

    protected void onNoiseAdded(int zoneIndex) {
    }

    public Table getGameSummaryTable() {
        Table summary = new Table("PLAYER", "KILLS", "STATUS", "EXP").setNoBorder();
        for (ZCharacter c : getAllCharacters()) {
            summary.addRow(c.name, c.getKillsTable(), c.isDead() ? "KIA" : "Alive", c.dangerBar);
        }
        return new Table(quest.getName())
                .addRow("STATUS:" +( gameOverStatus == 1 ? "COMPLETED" : gameOverStatus == 2 ? "FAILED" : "IN PROGRESS"))
                .addRow(new Table("SUMMARY").addRow(summary));
    }
}
