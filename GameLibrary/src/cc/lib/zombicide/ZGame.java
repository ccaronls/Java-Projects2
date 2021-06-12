package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        addAllFields(State.class);
    }

    private final static Logger log = LoggerFactory.getLogger(ZGame.class);

    final static int GAME_LOST = 2;
    final static int GAME_WON  = 1;

    public boolean tryGiftEquipment(ZCharacter c, ZEquipment e) {
        if (c.getEquipableSlots(e).size() == 0) {
            return false;
        }

        c.equip(e);
        onEquipmentFound(c, e);
        return true;
    }

    private boolean tryOpenDoor(ZCharacter cur, ZDoor door) {
        if (!door.isJammed())
            return true;

        if (cur.tryOpenDoor(this)) {
            onCharacterOpenedDoor(cur, door);
            return true;
        }
        onCharacterOpenDoorFailed(cur, door);
        return false;
    }

    public static class State extends Reflector<State> {
        final ZState state;
        final ZPlayerName player;

        public  State() {
            this(null, null);
        }

        State(ZState state, ZPlayerName player) {
            this.state = state;
            this.player = player;
        }
    }

    private final Stack<State> stateStack = new Stack<>();
    protected ZBoard board=null;
    private ZUser [] users=null;
    private ZQuest quest=null;
    private int currentUser=0;
    private LinkedList<ZEquipment> searchables = new LinkedList<>();
    private int spawnMultiplier=1;
    private int roundNum=0;
    private int gameOverStatus=0; // 0 == in play, 1, == game won, 2 == game lost
    private String gameFailedReason = "";
    private ZQuests currentQuest;
    private int [] dice = new int [256];
    private ZDiffuculty difficulty = ZDiffuculty.EASY;

    void pushState(ZState state, ZPlayerName player) {
        stateStack.push(new State(state, player));
    }

    public void setDifficulty(ZDiffuculty difficulty) {
        this.difficulty = difficulty;
    }

    public ZDiffuculty getDifficulty() {
        return difficulty;
    }

    private void initGame() {
        initSearchables();
        roundNum = 0;
        gameOverStatus = 0;
        spawnMultiplier = 1;
        initDice();
        setState(ZState.INIT, null);
    }

    private void initDice() {
        int num=6;
        // make sure even distribution of 1-6 numbers otherwise we can ge alot of repeats
        for (int i=0; i<dice.length; i++) {
            dice[i] = num;
            num--;
            if (num==0)
                num=6;
        }
        Utils.shuffle(dice);
    }

    public ZBoard getBoard() {
        return board;
    }

    public void setUsers(ZUser ... users) {
        this.users = users;
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

    protected void initQuest(ZQuest quest) {}

    public void loadQuest(ZQuests quest) {
        this.currentQuest = quest;
        ZQuest prevQuest = this.quest;
        this.quest = quest.load();
        board = this.quest.loadBoard();
        if (prevQuest == null || !prevQuest.getName().equals(this.quest.getName()))
            initQuest(this.quest);
        initGame();
        for (Grid.Iterator<ZCell> it = board.getCellsIterator(); it.hasNext(); ) {
            ZCell cell=it.next();
            if (cell.isCellTypeEmpty())
                continue;
            ZZone zone = board.getZone(cell.zoneIndex);
            switch (cell.environment) {
                case ZCell.ENV_OUTDOORS:
                    zone.setType(ZZoneType.OUTDOORS);
                    break;
                case ZCell.ENV_BUILDING:
                    zone.setType(ZZoneType.BUILDING);
                    break;
                case ZCell.ENV_VAULT:
                    zone.setType(ZZoneType.VAULT);
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
            for (ZCellType type : ZCellType.values()) {
                if (cell.isCellType(type)) {
                    switch (type) {
                        case START:
                            // position all the characters here
                            for (ZUser u : users) {
                                for (ZPlayerName pl : u.characters) {
                                    ZCharacter c = pl.create();
                                    c.occupiedZone = cell.zoneIndex;
                                    board.addActor(c, cell.zoneIndex, null);
                                }
                            }
                            break;
                        case SPAWN_NORTH:
                        case SPAWN_SOUTH:
                        case SPAWN_EAST:
                        case SPAWN_WEST:
                            zone.setSpawnType(ZSpawnType.NORMAL);
                            break;
                        case OBJECTIVE_BLACK:
                        case OBJECTIVE_RED:
                        case OBJECTIVE_BLUE:
                        case OBJECTIVE_GREEN:
                            zone.setObjective(true);
                            break;
                        case VAULT_DOOR_VIOLET:
                            addVaultDoor(cell, zone, it.getPos(), GColor.MAGENTA);
                            break;
                        case VAULT_DOOR_GOLD:
                            addVaultDoor(cell, zone, it.getPos(), GColor.GOLD);
                            break;
                    }
                }
            }
        }
        this.quest.init(this);
    }

    private void addVaultDoor(ZCell cell, ZZone zone, Grid.Pos pos, GColor color) {
        // add a vault door leading to the cell specified by vaultFlag
        Utils.assertTrue (cell.vaultFlag > 0);
        for (Grid.Iterator<ZCell> it2 = board.getCellsIterator(); it2.hasNext(); ) {
            ZCell cell2 = it2.next();
            if (cell == cell2)
                continue;
            if (cell.vaultFlag == cell2.vaultFlag) {
                zone.doors.add(new ZDoor(pos, it2.getPos(), cell.environment == ZCell.ENV_VAULT ? ZDir.ASCEND : ZDir.DESCEND, color));
                break;
            }
        }

    }

    public void spawnZombies(int count, ZZombieType name, int zone) {

        if (count == 0)
            return;

        do {
            if (name == ZZombieType.Necromancer) {
                // when spawning a necromancer, make sure we are spawning from not a spawn spot
                if (getBoard().getZone(zone).isSpawn()) {
                    name = Utils.flipCoin() ? ZZombieType.Fatty : ZZombieType.Runner;
                } else {
                    break;
                }
            }
            count *= spawnMultiplier;
            spawnMultiplier = 1;
        } while (false);

        while (true) {
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
            break;
        }
    }

    private void spawnZombies(ZZombieType type, int count, int zone) {
        for (int i = 0; i < count; i++) {
            ZZombie zombie = new ZZombie(type, zone);
            switch (zombie.type) {
                case Necromancer: {
                    board.setSpawnZone(zone, true);
                    spawnZombies(zone);
                }
            }
            if (board.addActor(zombie, zone, null))
                onZombieSpawned(zombie);
        }
    }

    protected void onZombieSpawned(ZZombie zombie) {
    }

    public ZState getState() {
        if (stateStack.empty())
            pushState(ZState.BEGIN_ROUND, null);
        return stateStack.peek().state;
    }

    private void setState(ZState state, ZPlayerName c) {
        stateStack.clear();
        stateStack.push(new State(state, c));
    }

    protected void onQuestComplete() {
        getCurrentUser().showMessage("Quest Complete");
    }

    boolean isGameSetup() {
        if(board == null)
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

    /**
     *
     * @return true if something changed, false otherwise
     */
    public boolean runGame() {
        log.debug("runGame %s", getState());
        if (!isGameSetup()) {
            log.error("Invalid Game");
            Utils.assertTrue(false);
            return false;
        }

        if (isGameOver())
            return false;

        if (getAllLivingCharacters().size() == 0) {
            gameLost("All Players Killed");
            return true;
        }

        String failedReason;
        if (null != (failedReason=quest.getQuestFailedReason(this))) {
            gameLost(failedReason);
            return true;
        }

        if (quest.getPercentComplete(this) >= 100) {
            gameWon();
            return true;
        }

        final ZUser user = getCurrentUser();

        switch (getState()) {

            case INIT: {
                for (ZCell cell : board.getCells()) {
                    for (ZCellType type : ZCellType.values()) {
                        if (cell.isCellType(type)) {
                            switch (type) {
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
                            }
                        }
                    }
                }
                setState(ZState.BEGIN_ROUND, null);
                return true;
            }

            case BEGIN_ROUND: {
                if (roundNum > 0)
                    setState(ZState.SPAWN, null);
                else
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                roundNum++;
                getCurrentUser().showMessage("Begin Round " + roundNum);
                onStartRound(roundNum);
                currentUser = 0;
                for (ZActor a : board.getAllLiveActors())
                    a.onBeginRound();
                board.resetNoise();
                break;
            }

            case SPAWN: {
                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                ZSkillLevel highestSkill = getHighestSkillLevel();
                for (int zIdx=0; zIdx<board.getNumZones(); zIdx++) {
                    ZZone z = board.getZone(zIdx);
                    if (z.isSpawn()) {
                        spawnZombies(zIdx, highestSkill);
                    }
                }
                setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                break;
            }

            case  PLAYER_STAGE_CHOOSE_CHARACTER: {
                // for each user, they choose each of their characters in any order and have them
                // perform all of their actions
                List<ZCharacter> options = new ArrayList<>();

                // any player who has done a move and is not a tactician must continue to move
                for (ZCharacter c : getAllLivingCharacters()) {
                    if (c.getActionsLeftThisTurn() > 0 && c.getActionsLeftThisTurn() < c.getActionsPerTurn() && !c.hasAvailableSkill(ZSkill.Tactician)) {
                        options.add(c);
                        break;
                    }
                }

                if (options.size() == 0) {
                    options.addAll(Utils.filter(getAllLivingCharacters(), object -> object.getActionsLeftThisTurn() > 0));
                }

                //if (options.size() > 0) {
                    // add characters who have organized and can do so again
                //    options.addAll(Utils.filter(getAllLivingCharacters(), object -> object.getActionsLeftThisTurn() == 0 && object.inventoryThisTurn));
                //}

                ZCharacter currentCharacter = null;
                if (options.size() == 0) {
                    if (++currentUser >= users.length) {
                        currentUser = 0;
                        setState(ZState.ZOMBIE_STAGE, null);
                    }
                    break;
                } else if (options.size() == 1) {
                    currentCharacter = options.get(0);
                } else {
                    currentCharacter = getCurrentUser().chooseCharacter(options);
                }
                if (currentCharacter != null) {
                    pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, currentCharacter.name);
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION: {

                final ZCharacter cur = getCurrentCharacter();

                boolean invOnly = false;
                if (getCurrentCharacter().getActionsLeftThisTurn() <= 0) {
                    if (getCurrentCharacter().inventoryThisTurn) {
                        invOnly = true;
                    } else {
                        getCurrentCharacter().onEndOfTurn(this);
                        setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                        return true;
                    }
                }

                List<ZMove> options = new ArrayList<>();
                options.add(ZMove.newEndTurn());
                if (!invOnly) {
                    //options.add(ZMove.newDoNothing());

                    // determine players available moves
                    for (ZSkill skill : cur.getAvailableSkills()) {
                        skill.addSpecialMoves(this, cur, options);
                    }

                    // add any moves determined by the quest
                    quest.addMoves(this, cur, options);
                }

                // check for organize
                if (cur.getAllEquipment().size() > 0)
                    options.add(ZMove.newInventoryMove());

                boolean zoneCleared = isClearedOfZombies(cur.occupiedZone);

                // check for trade with another character in the same zone (even if they are dead)
                if (zoneCleared && cur.canTrade()) {
                    List<ZCharacter> inZone = Utils.filter((List)board.getActorsInZone(cur.occupiedZone),
                            object -> {
                                if (object instanceof ZCharacter && object != cur && ((ZCharacter)object).canTrade())
                                    return true;
                                return false;
                            });
                    if (inZone.size() > 0) {
                        options.add(ZMove.newTradeMove(inZone));
                    }
                }

                if (!invOnly) {
                    ZZone zone = board.getZone(cur.occupiedZone);

                    // check for search
                    if (zone.isSearchable() && cur.canSearch() && zoneCleared) {
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

                    List<ZSpell> spells = cur.getSpells();
                    if (spells.size() > 0) {
                        options.add(ZMove.newEnchantMove(spells));
                    }

                    if (zone.getType() == ZZoneType.VAULT) {
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
                }

                ZMove move = getCurrentUser().chooseMove(this, cur, options);
                if (move != null) {
                    return performMove(cur, move);
                }

                return false;
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
                    onNewSkillAquired(cur, skill);
                    cur.addSkill(skill);
                    stateStack.pop();
                    return true;
                }
                return false;
            }

            case ZOMBIE_STAGE: {
                // sort them such that filled zones have their actions performed first
                Integer [] zoneArr = new Integer[board.getNumZones()];
                for (int i=0; i<zoneArr.length; i++) {
                    zoneArr[i] = i;
                }
                Arrays.sort(zoneArr, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o0, Integer o1) {
                        ZZone z0 = board.getZone(o0);
                        ZZone z1 = board.getZone(o1);
                        int numZ0 = board.getActorsInZone(o0).size();
                        int numZ1 = board.getActorsInZone(o1).size();
                        int MAX_PER_CELL = ZCellQuadrant.values().length;
                        int numEmptyZ0 = z0.cells.size() * MAX_PER_CELL - numZ0;
                        int numEmptyZ1 = z1.cells.size() * MAX_PER_CELL - numZ1;
                        // order such that zones with fewest empty slots have their zombies move first
                        return Integer.compare(numEmptyZ0, numEmptyZ1);
                    }
                });
                for (int zoneIdx : zoneArr) {
                    List<ZZombie> zombies = board.getZombiesInZone(zoneIdx);
                    for (ZZombie zombie : zombies) {
                        List<ZDir> path = null;
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
                                    playerWounded(victim, ZAttackType.NORMAL, 1, zombie.type.name());
                                }
                            } else {
                                if (path == null) {
                                    if (zombie.getType() == ZZombieType.Necromancer) {
                                        ZZone z = board.getZone(zombie.getOccupiedZone());
                                        if (z.getSpawnType() == ZSpawnType.NORMAL) {
                                            // necromancer is escaping!
                                            zombie.performAction(ZActionType.MOVE, this);
                                            onNecromancerEscaped(zombie);
                                            quest.onNecromancerEscaped(this, zombie);
                                            board.removeActor(zombie);
                                            continue;
                                        }
                                        path = getZombiePathTowardNearestSpawn(zombie);
                                    }
                                    if (path == null)
                                        path = getZombiePathTowardVisibleCharactersOrLoudestZone(zombie);
                                    if (path.isEmpty()) {
                                        // make zombies move around randomly
                                        List<Integer> zones = board.getAccessableZones(zombie.occupiedZone, zombie.getActionsPerTurn(), ZActionType.MOVE);
                                        if (zones.isEmpty()) {
                                            path = Collections.emptyList();
                                        } else {
                                            List<List<ZDir>> paths = board.getShortestPathOptions(zombie.occupiedCell, Utils.randItem(zones));
                                            if (paths.isEmpty()) {
                                                path = Collections.emptyList();
                                            } else {
                                                path = Utils.randItem(paths);
                                            }
                                        }
                                    } else {
                                        onZombiePath(zombie, path);
                                    }

                                }

                                if (path.isEmpty()) {
                                    zombie.performAction(ZActionType.DO_NOTHING, this);
                                } else {
                                    ZDir dir = path.remove(0);
                                    moveActorInDirection(zombie, dir);
                                }
                            }
                        }
                    }
                }
                setState(ZState.BEGIN_ROUND, null);
                return true;
            }

            case PLAYER_ENCHANT_SPEED_MOVE: {
                // compute all empty of zombie zones 1 or 2 units away form xurrent position
                List<Integer> zones = board.getAccessableZones(getCurrentCharacter().getOccupiedZone(), 1, ZActionType.MOVE);
                Set<Integer> all = new HashSet<>();
                for (int zoneIdx : zones) {
                    if (board.getZombiesInZone(zoneIdx).size() > 0)
                        continue;
                    all.add(zoneIdx);
                    List<Integer> next = board.getAccessableZones(zoneIdx, 1, ZActionType.MOVE);
                    for (int zoneIdx2 : next) {
                        if (board.getZombiesInZone(zoneIdx).size() > 0)
                            continue;
                        all.add(zoneIdx2);
                    }
                }
                all.remove(getCurrentCharacter().getOccupiedZone());
                if (all.size() == 0) {
                    stateStack.pop();
                } else {
                    Integer speedMove = getCurrentUser().chooseZoneToWalk(this, getCurrentCharacter(), new ArrayList<>(all));
                    if (speedMove != null) {
                        moveActor(getCurrentCharacter(), speedMove, 200);
                        stateStack.pop();
                        return true;
                    }
                }

                return false;
            }

            case PLAYER_STAGE_CHOOSE_ZONE_TO_REMOVE_SPAWN: {
                List<Integer> zones = new ArrayList<>();
                for (ZZone zone : board.getZones()) {
                    if (zone.isSpawn()) {
                        zones.add(zone.getZoneIndex());
                    }
                }

                if (zones.size() > 0) {
                    ZCharacter cur = getCurrentCharacter();
                    Integer zIdx = user.chooseZoneToRemoveSpawn(this, cur, zones);
                    if (zIdx != null) {
                        board.getZone(zIdx).setSpawnType(ZSpawnType.NONE);
                        onCharacterDestroysSpawn(cur, zIdx);
                        stateStack.pop();
                        return true;
                    }
                }

                return false;
            }

            default:
                throw new cc.lib.utils.GException("Unhandled state: " + getState());
        }

        return false;
    }

    protected void onCharacterDestroysSpawn(ZCharacter c, int zoneIdx) {}

    protected void onCharacterDefends(ZCharacter cur, ZZombie zombie) {
    }

    protected void onNewSkillAquired(ZCharacter c, ZSkill skill) {}

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

    void gameLost(String msg) {
        gameOverStatus = GAME_LOST;
        getCurrentUser().showMessage(("Game Lost " + msg).trim());
        onGameLost();
    }

    void gameWon() {
        gameOverStatus = GAME_WON;
        getCurrentUser().showMessage("Game Won!!!");
        onQuestComplete();
    }

    protected void onGameLost() {

    }

    void playerWounded(ZCharacter victim, ZAttackType attackType, int amount, String reason) {
        victim.woundBar += amount;
        if (victim.isDead()) {
            getCurrentUser().showMessage(victim.name() + " has been killed by a " + reason);
            onCharacterAttacked(victim, attackType, true);
            //removeCharacter(victim);
        } else {
            getCurrentUser().showMessage(victim.name() + " has been wounded by a " + reason);
            onCharacterAttacked(victim, attackType, false);
        }

    }


    protected void onCharacterAttacked(ZCharacter character, ZAttackType attackType, boolean characterPerished) {
    }

    protected void onTorchThrown(ZCharacter c, int zone) {
    }

    protected void onDragonBileThrown(ZCharacter c, int zone) {
        log.info("%s placed dragon bile in zone %d", c.name, zone);
    }

    private void removeCharacter(ZCharacter character) {
        for (ZUser user : users) {
            user.characters.remove(character);
        }
        board.removeActor(character);
    }

    public List<ZDir> getZombiePathTowardNearestSpawn(ZZombie zombie) {
        Map<Integer, List<ZDir>> pathsMap = new HashMap<>();
        Integer shortestPath = null;
        for (ZZone zone : board.getZones()) {
            if (zone.getSpawnType() == ZSpawnType.NORMAL) {
                List<List<ZDir>> paths = board.getShortestPathOptions(zombie.occupiedCell, zone.getZoneIndex());
                if (paths.size() > 0) {
                    pathsMap.put(zone.getZoneIndex(), paths.get(0));
                    if (shortestPath == null || paths.size() < pathsMap.get(shortestPath).size()) {
                        shortestPath = zone.getZoneIndex();
                    }
                }
            }
        }
        if (shortestPath == null)
            return Collections.emptyList();
        return pathsMap.get(shortestPath);
    }

    public List<ZDir> getZombiePathTowardVisibleCharactersOrLoudestZone(ZZombie zombie) {
        // zombie will move toward players it can see first and then noisy areas second
        int maxNoise = 0;
        int targetZone = -1;
        for (ZCharacter c : Utils.filter(getAllLivingCharacters(), object -> !object.isInvisible())) {
            if (board.canSee(zombie.occupiedZone, c.occupiedZone)) {
                int noiseLevel = board.getZone(c.occupiedZone).getNoiseLevel();
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
            for (int zone=0; zone < board.getNumZones(); zone++) {
                int noiseLevel = board.getZone(zone).getNoiseLevel();
                if (noiseLevel > maxNoise) {
                    maxNoise = noiseLevel;
                    targetZone = zone;
                }
            }
            log.info("%s cannot see any players so moving toward loudest sound %d in zone %d.", zombie.name(), maxNoise, targetZone);
        }

        if (targetZone >= 0) {
            List<List<ZDir>> paths =  board.getShortestPathOptions(zombie.occupiedCell, targetZone);
            if (paths.size() > 0) {
                return Utils.randItem(paths);
            }
        }

        return Collections.emptyList();
    }

    protected void onStartRound(int roundNum) {

    }

    protected void onDoNothing(ZCharacter c) {}

    public boolean canUse(ZEquipSlot slot) {
        ZCharacter c = getCurrentCharacter();
        return c != null && c.getSlot(slot) != null;
    }

    public boolean canWalk(ZDir dir) {
        ZCharacter c = getCurrentCharacter();
        return c != null && c.getActionsLeftThisTurn() > 0 && getBoard().canMove(c, dir);
    }


    private boolean useEquipment(ZCharacter c, ZEquipment e) {
        if (e.isMagic()) {
            return performMove(c, ZMove.newMagicAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isMelee()) {
            return performMove(c, ZMove.newMeleeAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isRanged()) {
            return performMove(c, ZMove.newRangedAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isThrowable()) {
            return performMove(c, ZMove.newThrowItemMove(Utils.toList((ZItem)e)));
        }
        return false;
    }

    private boolean performMove(ZCharacter cur, ZMove move) {
        log.debug("performMove:%s", move);
        ZUser user = getCurrentUser();
        switch (move.type) {
            case DO_NOTHING:
                onDoNothing(cur);
                cur.performAction(ZActionType.DO_NOTHING, this);
                return true;
            case END_TURN:
                cur.clearActions();
                stateStack.pop();
                break;
            case SWITCH_ACTIVE_CHARACTER: {
                if (canSwitchActivePlayer()) {
                    int idx = 0;
                    for (ZPlayerName nm : user.characters) {
                        if (nm == cur.getType()) {
                            break;
                        }
                        idx++;
                    }
                    for (int i=(idx+1) % user.characters.size(); i!=idx; i=(i+1)%user.characters.size()) {
                        ZCharacter c = user.characters.get(i).character;
                        if (c.isAlive() && c.getActionsLeftThisTurn() > 0) {
                            stateStack.pop();
                            pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, c.name);
                            return true;
                        }
                    }
                }
                return false;
            }
            case TAKE_OBJECTIVE: {
                cur.performAction(ZActionType.OBJECTIVE, this);
                ZZone zone = board.getZone(cur.occupiedZone);
                zone.setObjective(false);
                for (Grid.Pos pos : zone.cells) {
                    for (ZCellType ct : Arrays.asList(ZCellType.OBJECTIVE_BLACK, ZCellType.OBJECTIVE_BLUE, ZCellType.OBJECTIVE_GREEN, ZCellType.OBJECTIVE_RED))
                        board.getCell(pos).setCellType(ct, false);
                }
                getCurrentUser().showMessage(cur.name() + " Found an OBJECTIVE");
                quest.processObjective(this, cur, move);
                return true;
            }
            case INVENTORY: {
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
                    return false;
                }
                // choose which equipment from the slot to organize
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
                    return false;

                // we have a slot and an equipment from the slot to do something with
                // we can:
                //   dispose, unequip, equip to an empty slot or consume
                List<ZMove> options = new ArrayList<>();
                if (selectedEquipment.canConsume() && cur.getActionsLeftThisTurn() > 0) {
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
                    return performMove(cur, move);
                }
                return false;
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
                        if (other.getEquipableSlots(eq).size() > 0) {
                            options.add(ZMove.newGiveMove(other, eq));
                        }
                    }

                    for (ZEquipment eq : other.getAllEquipment()) {
                        if (cur.getEquipableSlots(eq).size() > 0) {
                            options.add(ZMove.newTakeMove(other, eq));
                        }
                    }

                    move = user.chooseMove(this, cur, options);
                    if (move != null) {
                        return performMove(cur, move);
                    }
                }
                return false;
            case WALK: {
                Integer zone = move.integer;
                if (zone == null)
                    zone = user.chooseZoneToWalk(this, cur, move.list);
                if (zone != null) {
                    moveActor(cur, zone, cur.getMoveSpeed());
                    return true;
                    //cur.performAction(ZActionType.MOVE, this);
                }
                return false;
            }
            case WALK_DIR: {
                moveActorInDirection(cur, move.dir);
                return true;
            }
            case USE_LEFT_HAND: {
                ZEquipment e;
                if ((e=cur.getSlot(ZEquipSlot.LEFT_HAND)) != null) {
                    return useEquipment(cur, e);
                }
                return false;
            }
            case USE_RIGHT_HAND: {
                ZEquipment e;
                if ((e=cur.getSlot(ZEquipSlot.RIGHT_HAND)) != null) {
                    return useEquipment(cur, e);
                }
                return false;
            }
            case MELEE_ATTACK: {
                List<ZWeapon> weapons = move.list;
                ZWeapon weapon = null;
                if (weapons.size() > 1) {
                    weapon = user.chooseWeaponSlot(this, cur, weapons);
                } else {
                    weapon = weapons.get(0);
                }
                if (weapon != null) {
                    Utils.assertTrue(weapon.slot != null);
                    ZWeaponStat stat = cur.getWeaponStat(weapon, ZActionType.MELEE, this, cur.getOccupiedZone());
                    List<ZZombie> zombies = board.getZombiesInZone(cur.occupiedZone);
                    if (zombies.size() > 1)
                        Collections.sort(zombies);//, (o1, o2) -> Integer.compare(o2.type.minDamageToDestroy, o1.type.minDamageToDestroy));
                    while (zombies.size() > 0 && zombies.get(0).type.minDamageToDestroy > stat.damagePerHit) {
                        zombies.remove(0);
                    }
                    int hits = resolveHits(cur, zombies.size(), ZActionType.MELEE, stat.numDice, stat.dieRollToHit, zombies.size()/2-1, zombies.size()/2+1);
                    onAttack(cur, weapon, ZActionType.MELEE, stat.numDice, hits, cur.getOccupiedZone());

                    for (int i=0; i<hits && zombies.size() > 0; i++) {
                        ZZombie z = zombies.remove(0);
                        destroyZombie(z, stat.attackType, cur);
                        addExperience(cur, z.type.expProvided);
                    }
                    if (weapon.isAttackNoisy()) {
                        addNoise(cur.occupiedZone, 1);
                    }
                    cur.performAction( ZActionType.MELEE,this);
                    user.showMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                    return true;
                }
                return false;
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
                Integer zone = move.integer;
                if (slot != null) {
                    ZActionType actionType = move.type.getActionType(slot);
                    ZWeaponStat stat = cur.getWeaponStat(slot, actionType, this, -1);
                    if (zone == null) {
                        List<Integer> zones = new ArrayList<>();
                        for (int range = stat.minRange; range <= stat.maxRange; range++) {
                            zones.addAll(board.getAccessableZones(cur.occupiedZone, range, actionType));
                        }
                        if (zones.size() > 0) {
                            zone = user.chooseZoneForAttack(this, cur, zones);
                        }
                    }
                    if (zone != null) {
                        stat = cur.getWeaponStat(slot, actionType, this, zone);
                        // process a ranged attack
                        if (!slot.isLoaded()) {
                            getCurrentUser().showMessage("CLICK! Weapon not loaded!");
                            onWeaponGoesClick(cur, slot);
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
                            onAttack(cur, slot, actionType, stat.numDice, hits, zone);
                            for (int i=0; i<hits && zombies.size() > 0; i++) {
                                ZZombie zombie = zombies.remove(0);
                                if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                    destroyZombie(zombie, stat.attackType, cur);
                                    addExperience(cur, zombie.type.expProvided);
                                }
                            }

                            user.showMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                            if (cur.canFriendlyFire()) {
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
                                for (int i = 0; i < misses && friendlyFireOptions.size() > 0; i++) {
                                    // friendy fire!
                                    ZCharacter victim = friendlyFireOptions.get(0);
                                    if (playerDefends(victim, ZZombieType.Walker)) {
                                        getCurrentUser().showMessage(victim.name() + " defended thyself from friendly fire!");
                                    } else {
                                        playerWounded(victim, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                                        if (victim.isDualWeilding())
                                            friendlyFireOptions.remove(0);
                                    }
                                }
                            }
                        }
                        cur.performAction(actionType,this);
                        return true;
                    }
                }
                return false;
            }

            case THROW_ITEM: {
                ZItem slot = null;
                List<ZItem> slots = move.list;
                if (slots.size() == 1)
                    slot = slots.get(0);
                else
                    slot = getCurrentUser().chooseItemToThrow(this, cur, slots);
                if (slot != null) {
                    Integer zoneIdx = null;
                    if (move.integer != null) {
                        zoneIdx = move.integer;
                    } else {
                        List<Integer> zones = board.getAccessableZones(cur.occupiedZone, 1, ZActionType.THROW_ITEM);
                        zones.add(cur.occupiedZone);
                        zoneIdx = getCurrentUser().chooseZonetoThrowItem(this, cur, slot, zones);
                    }
                    if (zoneIdx != null) {
                        switch (slot.type) {
                            case DRAGON_BILE:
                                getCurrentUser().showMessage(cur.name() + " threw the drogon Bile!");
                                onDragonBileThrown(cur, zoneIdx);
                                board.getZone(zoneIdx).setDragonBile(true);
                                break;
                            case TORCH: {
                                ZZone zone = board.getZone(zoneIdx);
                                onTorchThrown(cur, zoneIdx);
                                if (!zone.isDragonBile()) {
                                    getCurrentUser().showMessage("Throwing the Torch had no effect");
                                } else {
                                    zone.setDragonBile(false);
                                    int exp = 0;
                                    int num=0;
                                    getCurrentUser().showMessage(cur.name() + " threw the torch exploding the dragon bile!");
                                    for (ZActor a : board.getActorsInZone(zoneIdx)) {
                                        if (a instanceof ZZombie) {
                                            ZZombie z = (ZZombie)a;
                                            exp += z.type.expProvided;
                                            destroyZombie(z, ZAttackType.FIRE, cur);
                                            num++;
                                        } else if (a instanceof ZCharacter) {
                                            // characters caught in the zone get wounded
                                            ZCharacter c = (ZCharacter)a;
                                            playerWounded(c, ZAttackType.FIRE, 4, "Exploding Dragon Bile");
                                        }
                                    }
                                    addExperience(cur, exp);
                                    getCurrentUser().showMessage(String.format("%s Destroyed %d zombies for %d total experience pts!", cur.name(), num, exp));
                                    quest.onDragonBileExploded(cur, zoneIdx);
                                }
                                break;
                            }
                            default:
                                throw new GException("Unhandled case: " + slot.type);
                        }
                        cur.removeEquipment(slot);
                        cur.performAction(ZActionType.THROW_ITEM, this);
                        putBackInSearchables(slot);
                        return true;
                    }

                }
                return false;
            }

            case RELOAD: {
                ZWeapon weapon = (ZWeapon)move.equipment;
                if (cur.isDualWeilding()) {
                    ((ZWeapon)cur.getSlot(ZEquipSlot.LEFT_HAND)).reload();
                    ((ZWeapon)cur.getSlot(ZEquipSlot.RIGHT_HAND)).reload();
                    user.showMessage(getCurrentCharacter().name() + " Reloaded both their " + weapon.getLabel() + "s");
                } else {
                    weapon.reload();
                    user.showMessage(getCurrentCharacter().name() + " Reloaded their " + weapon.getLabel());
                }
                cur.performAction(ZActionType.RELOAD, this);
                return true;
            }
            case OPERATE_DOOR: {
                List<ZDoor> doors = move.list;
                ZDoor door = null;
                if (doors.size() > 1)
                    door = user.chooseDoorToToggle(this, cur, doors);
                else
                    door = doors.get(0);
                if (door != null) {
                    if (door.isClosed(board)) {
                        if (tryOpenDoor(cur, door)) {
                            door.toggle(board);
                            //getCurrentUser().showMessage(currentCharacter.name() + " has opened a " + door.name());
                            onCharacterOpenedDoor(cur, door);
                            // spawn zombies in the newly exposed zone and any adjacent
                            ZDoor otherSide = door.getOtherSide();
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
                    return true;
                }
                return false;
            }
            case SEARCH: {
                // draw from top of the deck
                int numCardsDrawn = 1;
                if (cur.isEquiped(ZItemType.TORCH))
                    numCardsDrawn = 2;
                while (searchables.size() > 0 && numCardsDrawn-- > 0 && cur.canSearch()) {
                    ZEquipment equip = searchables.removeLast();
                    quest.onEquipmentFound(this, equip);
                    if (equip.getType() == ZItemType.AAHHHH) {
                        getCurrentUser().showMessage("Aaaahhhh!!!");
                        onAhhhhhh(cur);
                        // spawn zombie right here right now
                        spawnZombies(1, ZZombieType.Walker, cur.occupiedZone);
                        putBackInSearchables(equip);
                    } else {
                        onEquipmentFound(cur, equip);
                        user.showMessage(cur.name() + " Found a " + equip);
                        cur.equip(equip);
                    }
                }
                cur.performAction(ZActionType.SEARCH, this);
                return true;
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
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            }
            case UNEQUIP:
                cur.removeEquipment(move.equipment, move.fromSlot);
                cur.attachEquipment(move.equipment, ZEquipSlot.BACKPACK);
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            case TAKE:
                move.character.removeEquipment(move.equipment);
                cur.attachEquipment(move.equipment);
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            case GIVE:
                cur.removeEquipment(move.equipment);
                move.character.attachEquipment(move.equipment);
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            case DISPOSE:
                cur.removeEquipment(move.equipment, move.fromSlot);
                cur.performAction(ZActionType.INVENTORY, this);
                putBackInSearchables(move.equipment);
                return true;
            case CONSUME:
                performConsume(cur, move);
                cur.performAction(ZActionType.CONSUME, this);
                return true;
            case PICKUP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToPickup(this, cur, move.list);
                if (equip != null) {
                    quest.pickupItem(cur.occupiedZone, equip);
                    cur.equip(equip);
                    cur.performAction(ZActionType.PICKUP_ITEM, this);
                    return true;
                }
                return false;
            }
            case DROP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToDrop(this, cur, move.list);
                if (equip != null) {
                    quest.dropItem(cur.occupiedZone, equip);
                    cur.removeEquipment(equip);
                    cur.performAction(ZActionType.DROP_ITEM, this);
                    return true;
                }
                return false;
            }
            case MAKE_NOISE: {
                int maxNoise = board.getMaxNoiseLevel();
                addNoise(move.integer, maxNoise+1 - board.getZone(move.integer).getNoiseLevel());
                getCurrentUser().showMessage(cur.name() + " made alot of noise to draw the zombies!");
                cur.performAction(ZActionType.MAKE_NOISE, this);
                return true;
            }
            case SHOVE: {
                Integer targetZone = getCurrentUser().chooseZoneToShove(this, cur, move.list);
                if (targetZone != null) {
                    // shove all zombies in this zone into target zone
                    for (ZZombie z : board.getZombiesInZone(cur.getOccupiedZone())) {
                        GRectangle prev = z.getRect();
                        board.moveActor(z, targetZone);
                        GRectangle next   = board.getCell(z.occupiedCell).getQuadrant(z.occupiedQuadrant);
                        onActorMoved(z, prev, next, 300);
                    }
                    cur.performAction(ZActionType.SHOVE, this);
                    return true;
                }
                return false;
            }
            case ENCHANT: {
                ZSpell spell = null;
                ZCharacter target = null;
                if (move.list.size() == 1 && move.character != null) {
                    spell = (ZSpell)move.list.get(0);
                    target = move.character;
                } else {
                    List<ZSpell> spells = cur.getSpells();
                    spell = getCurrentUser().chooseSpell(this, cur, spells);
                    if (spell != null) {
                        List<ZCharacter> targets = Utils.filter(getAllLivingCharacters(), object -> board.canSee(cur.getOccupiedZone(), object.getOccupiedZone()));
                        target = getCurrentUser().chooseCharacterForSpell(this, cur, spell, targets);
                    }
                }
                if (spell != null && target != null) {
                    spell.type.doEnchant(this, target);//target.availableSkills.add(spell.type.skill);
                    cur.performAction(ZActionType.ENCHANTMENT, this);
                    return true;
                }
                return false;
            }
            case BORN_LEADER: {
                ZCharacter chosen = null;
                if (move.character != null)
                    chosen = move.character;
                else
                    chosen = getCurrentUser().chooseCharacterToBequeathMove(this, cur, move.list);
                if (chosen != null) {
                    if (chosen.getActionsLeftThisTurn() > 0) {
                        chosen.addExtraAction();
                    } else {
                        chosen.addAvailableSkill(ZSkill.Plus1_Action);
                    }
                    cur.performAction(ZActionType.BEQUEATH_MOVE, this);
                    return true;
                }
                return false;
            }
            case BLOODLUST_MELEE:
            case BLOODLUST_MAGIC:
            case BLOODLUST_RANGED:
                return performBloodlust(cur, move);
            default:
                log.error("Unhandled move: %s", move.type);
        }
        return false;
    }

    private boolean performBloodlust(ZCharacter cur, ZMove move) {
        List<ZWeapon> weapons;
        ZActionType action = null;
        switch (move.type) {
            case BLOODLUST_MAGIC:
                weapons = cur.getMagicWeapons();
                action = ZActionType.MAGIC;
                break;
            case BLOODLUST_MELEE:
                weapons = cur.getMeleeWeapons();
                action = ZActionType.MELEE;
                break;
            case BLOODLUST_RANGED:
                weapons = cur.getRangedWeapons();
                break;
            default:
                return false;
        }

        Integer zone;
        if (move.list.size() == 1) {
            zone = (Integer)move.list.get(0);
        } else {
            zone = getCurrentUser().chooseZoneForBloodlust(this, cur, move.list);
        }
        if (zone != null) {
            ZWeapon slot = null;
            if (move.equipment != null) {
                slot = (ZWeapon)move.equipment;
            } else if (weapons.size() > 1) {
                slot = getCurrentUser().chooseWeaponSlot(this, cur, weapons);
            } else if (weapons.size() == 1) {
                slot = weapons.get(0);
            }
            if (slot != null) {
                if (action == null) {
                    action = slot.type.usesArrows ? ZActionType.ARROWS : ZActionType.BOLTS;
                }
                cur.addExtraAction();
                moveActor(cur, zone, cur.getMoveSpeed()/2);
                performAttack(slot, action);
                cur.performAction(action, this);
                cur.removeAvailableSkill(move.skill);
                return true;
            }
        }
        return false;
    }

    protected void onAhhhhhh(ZCharacter c) {}

    protected void onNecromancerEscaped(ZZombie necro) {}

    protected void onEquipmentFound(ZCharacter c, ZEquipment equipment) {}

    ZDir getDirection(int fromZoneIdx, int toZoneIdx) {
        Utils.assertTrue(fromZoneIdx != toZoneIdx);

        ZZone fromZone = board.getZone(fromZoneIdx);
        ZZone toZone   = board.getZone(toZoneIdx);

        if (fromZone.getType() == ZZoneType.VAULT) {
            return ZDir.ASCEND;
        }

        if (toZone.getType() == ZZoneType.VAULT) {
            return ZDir.DESCEND;
        }

        for (Grid.Pos fromPos : fromZone.cells) {
            for (Grid.Pos toPos : toZone.cells) {
                ZDir dir = ZDir.getDirFrom(fromPos, toPos);
                if (dir != null)
                    return dir;
            }
        }
        Utils.assertTrue(false);
        return null;
    }

    private void performAttack(ZWeapon slot, ZActionType actionType) {
        ZUser user = getCurrentUser();
        ZCharacter cur = getCurrentCharacter();
        switch (actionType) {
            case MELEE: {
                ZWeaponStat stat = cur.getWeaponStat(slot, ZActionType.MELEE, this, cur.getOccupiedZone());
                List<ZZombie> zombies = board.getZombiesInZone(cur.occupiedZone);
                if (zombies.size() > 1)
                    Collections.sort(zombies, (o1, o2) -> Integer.compare(o2.type.minDamageToDestroy, o1.type.minDamageToDestroy));
                while (zombies.size() > 0 && zombies.get(0).type.minDamageToDestroy > stat.damagePerHit) {
                    zombies.remove(0);
                }
                int hits = resolveHits(cur, zombies.size(), ZActionType.MELEE, stat.numDice, stat.dieRollToHit, zombies.size() / 2 - 1, zombies.size() / 2 + 1);
                onAttack(cur, slot, ZActionType.MELEE, stat.numDice, hits, cur.getOccupiedZone());

                for (int i = 0; i < hits && zombies.size() > 0; i++) {
                    ZZombie z = zombies.remove(0);
                    addExperience(cur, z.type.expProvided);
                    destroyZombie(z, stat.attackType, cur);
                }
                if (slot.isAttackNoisy()) {
                    addNoise(cur.occupiedZone, 1);
                }
                user.showMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                break;
            }
            case MAGIC:
            case ARROWS:
            case BOLTS: {

                ZWeaponStat stat = cur.getWeaponStat(slot, actionType, this, -1);
                List<Integer> zones = new ArrayList<>();
                for (int range = stat.minRange; range <=stat.maxRange; range++) {
                    zones.addAll(board.getAccessableZones(cur.occupiedZone, range, actionType));
                }
                if (zones.size() > 0) {
                    Integer zone = user.chooseZoneForAttack(this, cur, zones);
                    if (zone != null) {
                        stat = cur.getWeaponStat(slot, actionType, this, zone);
                        // process a ranged attack
                        if (!slot.isLoaded()) {
                            getCurrentUser().showMessage("CLICK! Weapon not loaded!");
                            onWeaponGoesClick(cur, slot);
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
                            onAttack(cur, slot, actionType, stat.numDice, hits, zone);
                            for (int i=0; i<hits && zombies.size() > 0; i++) {
                                ZZombie zombie = zombies.remove(0);
                                if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                    addExperience(cur, zombie.type.expProvided);
                                    destroyZombie(zombie, stat.attackType, cur);
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
                                    getCurrentUser().showMessage(victim.name() + " defended thyself from friendly fire!");
                                } else {
                                    playerWounded(victim, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                                    if (victim.isDualWeilding() || victim.isDead())
                                        friendlyFireOptions.remove(0);
                                }
                            }
                            user.showMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                        }
                    }
                }
                break;
            }
        }

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
                getCurrentUser().showMessage("Rolled " + numSixes + " 6s + 1Die roll each!");
                result = rollDice(numSixes);
            } else {
                break;
            }
        } while (true);
        return hits;
    }

    protected void onWeaponGoesClick(ZCharacter c, ZWeapon weapon) {}

    protected void onCharacterOpenedDoor(ZCharacter cur, ZDoor door) {}

    protected void onCharacterOpenDoorFailed(ZCharacter cur, ZDoor door) {}

    protected void onAttack(ZCharacter attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {}

    public ZSkillLevel getHighestSkillLevel() {
        int highestSkill = 0;
        for (ZUser u : users) {
            for (ZPlayerName c : u.characters) {
                highestSkill = Math.max(highestSkill, c.character.getSkillLevel().ordinal());
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
                putBackInSearchables(item);
                break;
            default:
                throw new cc.lib.utils.GException("Unhandled case: " + item);
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
            pushState(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL, getCurrentCharacter().name);
        }
    }

    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        log.info("%s gained %d experence!", c.name, points);
    }

    Integer [] rollDiceWithRerollOption(int numDice, int dieNumToHit, int minHitsForAutoReroll, int maxHitsForAutoNoReroll) {
        minHitsForAutoReroll = Math.max(minHitsForAutoReroll, 0);
        maxHitsForAutoNoReroll = Math.min(maxHitsForAutoNoReroll, numDice);
        Integer [] dice = rollDice(numDice);
        int hits = 0;
        for (int d : dice) {
            if (d >= dieNumToHit)
                hits++;
        }

        getCurrentUser().showMessage(getCurrentCharacter().getLabel() + " Scored " + hits + " hits");
        //onRollDice(dice);

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
        if (dice == null) {
            initDice();
        }
        Integer [] result = new Integer[num];
        String dieStr = "| ";

        for (int i=0; i<num; i++) {
            result[i] = dice[i];
            dieStr += String.format("%d | ", result[i]);
        }
        for (int i = 0; i< dice.length-num; i++) {
            dice[i] = dice[i+num];
        }
        for (int i = 0; i< num; i++) {
            dice[i+ dice.length-num] = result[i];
        }
        //getCurrentUser().showMessage("Rolled a " + dieStr);//new Table().addRow(result).toString());
        onRollDice(result);
        return result;
    }

    protected void onRollDice(Integer [] roll) {
        log.info("Rolling dice result is: %s", Arrays.toString(roll));
    }

    private void destroyZombie(ZZombie zombie, ZAttackType deathType, ZCharacter killer) {
        killer.onKilledZombie(zombie);
        onZombieDestroyed(killer, deathType, zombie);
        board.removeActor(zombie);
        if (zombie.getType() == ZZombieType.Necromancer) {
            pushState(ZState.PLAYER_STAGE_CHOOSE_ZONE_TO_REMOVE_SPAWN, killer.name);
        }
    }

    protected void onZombieDestroyed(ZCharacter c, ZAttackType deathType, ZZombie zombie) {
        log.info("%s Zombie %s destroyed for %d experience", c.name(), zombie.type.name(), zombie.type.expProvided);
    }

    public List<ZCharacter> getAllCharacters() {
        List<ZCharacter> all = new ArrayList<>();
        for (ZUser user : users) {
            for (ZPlayerName p : user.characters)
               all.add(p.character);
        }
        return all;
    }

    public List<ZCharacter> getAllLivingCharacters() {
        return Utils.filter(getAllCharacters(), object -> object.isAlive());
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        return board.getZombiesInZone(zoneIndex).size() == 0;
    }

    protected void onDoubleSpawn(int multiplier) {
    }

    private void doubleSpawn() {
        spawnMultiplier *= 2;
        getCurrentUser().showMessage("DOUBLE SPAWN!");
        onDoubleSpawn(spawnMultiplier);
    }

    private void extraActivation() {

    }

    private void extraActivation(ZZombieType name) {
        getCurrentUser().showMessage("Extra Activation for " + name);
        for (ZZombie z : Utils.filter(board.getAllZombies(), object -> object.type == name)) {
            z.addExtraAction();
        }
    }

    public void spawnZombies(int zoneIdx) {
        spawnZombies(zoneIdx, getHighestSkillLevel());
    }

    private void spawnZombies(int zoneIdx, ZSkillLevel level) {
        switch (difficulty) {
            case EASY:
                spawnZombiesEasy(zoneIdx, level);
                break;
            case MEDIUM:
                spawnZombiesMedium(zoneIdx, level);
                break;
            case HARD:
                spawnZombiesHard(zoneIdx, level);
                break;
        }
    }

    private void spawnZombiesEasy(int zoneIdx, ZSkillLevel level) {
        switch (level) {
            case BLUE:
                if (Utils.rand() % 3 > 1)
                    spawnZombies(1, ZZombieType.Walker, zoneIdx);
                break;
            case YELLOW:
                spawnZombies(Utils.randRange(1,2), ZZombieType.Walker, zoneIdx);
                break;
            case ORANGE:
                if (Utils.flipCoin()) {
                    spawnZombies(Utils.randRange(2,3), ZZombieType.Walker, zoneIdx);
                } else {
                    spawnZombies(Utils.rand() % 2, ZZombieType.Fatty, zoneIdx);
                }
                break;
            case RED:
                switch (Utils.rand() % 10) {
                    case 0:
                        spawnZombies(1, ZZombieType.Abomination, zoneIdx);
                        break;
                    case 1:
                        spawnZombies(Utils.randRange(2,3), ZZombieType.Runner, zoneIdx);
                        break;
                    case 2:
                    case 3:
                        spawnZombies(Utils.randRange(1,2), ZZombieType.Fatty, zoneIdx);
                        break;
                    case 4:
                    case 5:
                    case 6:
                        spawnZombies(Utils.randRange(3,4), ZZombieType.Walker, zoneIdx);
                        break;
                }
                break;
        }
    }

    private void spawnZombiesMedium(int zoneIdx, ZSkillLevel level) {
        switch (level) {
            case BLUE:
                if (Utils.rand() % 3 > 0)
                    spawnZombies(Utils.randRange(1,2), ZZombieType.Walker, zoneIdx);
                break;
            case YELLOW:
                switch(Utils.rand() % 6) {
                    case 0:
                        spawnZombies(Utils.randRange(1, 2), ZZombieType.Runner, zoneIdx);
                        break;
                    case 1:
                        spawnZombies(1, ZZombieType.Necromancer, zoneIdx);
                        break;
                    default:
                        spawnZombies(Utils.randRange(2, 3), ZZombieType.Walker, zoneIdx);
                        break;
                }
                break;
            case ORANGE:
                if (spawnMultiplier == 1 && Utils.randRange(0,4) == 0) {
                    doubleSpawn();
                } else {
                    if (Utils.flipCoin()) {
                        spawnZombies(Utils.randRange(2, 3), ZZombieType.Walker, zoneIdx);
                    } else {
                        spawnZombies(1, ZZombieType.Fatty, zoneIdx);
                    }
                }
                break;
            case RED:
                switch (Utils.rand() % 10) {
                    case 0:
                        spawnZombies(1, ZZombieType.Abomination, zoneIdx);
                        break;
                    case 1:
                    case 2:
                        spawnZombies(Utils.randRange(2,3), ZZombieType.Runner, zoneIdx);
                        break;
                    case 3:
                    case 4:
                        spawnZombies(Utils.randRange(1,2), ZZombieType.Fatty, zoneIdx);
                        break;
                    case 5:
                    case 6:
                        spawnZombies(Utils.randRange(3,4), ZZombieType.Walker, zoneIdx);
                        break;
                }
                break;
        }
    }

    private void spawnZombiesHard(int zoneIdx, ZSkillLevel level) {
        log.info("Random zombie spawn for zone %d and level %s", zoneIdx, level);
        if (Utils.rand() % (spawnMultiplier*10) == 0) {
            doubleSpawn();
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
        if (stateStack.isEmpty())
            return null;
        ZPlayerName player = stateStack.peek().player;
        if (player == null)
            return null;
        return player.character;
    }

    void moveActor(ZActor actor, int toZone, long speed) {
        int fromZone = actor.getOccupiedZone();
        Grid.Pos fromPos = actor.getOccupiedCell();
        GRectangle fromRect = actor.getRect(board);
        board.moveActor(actor, toZone);
        doMove(actor, fromZone, fromPos, fromRect, speed);
    }

    void moveActorInDirection(ZActor actor, ZDir dir) {
        int fromZone = actor.getOccupiedZone();
        Grid.Pos fromPos = actor.getOccupiedCell();
        GRectangle fromRect = actor.getRect(board);
        Grid.Pos next = board.getAdjacent(fromPos, dir);
        board.moveActor(actor, next);
        doMove(actor, fromZone, fromPos, fromRect, actor.getMoveSpeed());
    }

    private void doMove(ZActor actor, int fromZone, Grid.Pos fromPos, GRectangle fromRect, long speed) {
        int toZone = actor.getOccupiedZone();
        Grid.Pos toPos = actor.getOccupiedCell();
        GRectangle toRect = actor.getRect(board);

        if (board.getZone(fromZone).getType() == ZZoneType.VAULT && board.getZone(toZone).getType() != ZZoneType.VAULT) {
            // ascending the stairs
            GRectangle fromVaultRect = new GRectangle(board.getCell(fromPos)).scale(.2f);
            GRectangle toVaultRect = new GRectangle(board.getCell(toPos)).scale(.5f);
            onActorMoved(actor, fromRect, fromVaultRect, speed/2);
            onActorMoved(actor, toVaultRect, toRect, speed/2);
        } else if (board.getZone(fromZone).getType() != ZZoneType.VAULT && board.getZone(toZone).getType() == ZZoneType.VAULT) {
            // descending the stairs
            GRectangle fromVaultRect = new GRectangle(board.getCell(fromPos)).scale(.2f);
            GRectangle toVaultRect = new GRectangle(board.getCell(toPos)).scale(.5f);
            onActorMoved(actor, fromRect, fromVaultRect, speed/2);
            onActorMoved(actor, toVaultRect, toRect, speed/2);
        } else {
            onActorMoved(actor, fromRect, toRect, speed);
        }
        if (toZone != fromZone) {
            actor.performAction(ZActionType.MOVE, this);
        }
    }

    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end, long speed) {
    }
/*
    public boolean canGoBack() {
        switch (getState()) {
            case PLAYER_STAGE_CHOOSE_ZONE_TO_REMOVE_SPAWN:
            case PLAYER_STAGE_CHOOSE_NEW_SKILL:
                return false;
        }
        return stateStack.size() > 1;
    }

    public void goBack() {
        switch (getState()) {
            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION:
            case PLAYER_STAGE_CHOOSE_CHARACTER:
                stateStack.pop();
        }
    }
*/
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
        searchables.addAll(make(3, ZItemType.DRAGON_BILE));
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
        //searchables.addAll(make(1, ZSpellType.REPULSE));
        searchables.addAll(make(2, ZItemType.SALTED_MEAT));
        searchables.addAll(make(2, ZArmorType.SHIELD));
        searchables.addAll(make(1, ZWeaponType.SHORT_BOW));
        searchables.addAll(make(1, ZWeaponType.SHORT_SWORD));
        searchables.addAll(make(1, ZSpellType.SPEED));
        searchables.addAll(make(2, ZWeaponType.SWORD));
        searchables.addAll(make(4, ZItemType.TORCH));
        searchables.addAll(make(2, ZItemType.WATER));
        quest.processSearchables(searchables);

        Utils.shuffle(searchables);
    }

    public void addNoise(int zoneIdx, int noise) {
        onNoiseAdded(zoneIdx);
        board.getZone(zoneIdx).addNoise(noise);
//        getCurrentUser().showMessage("Noise was made in zone " + zoneIdx);
    }

    protected void onNoiseAdded(int zoneIndex) {
    }

    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {}

    public Table getGameSummaryTable() {
        Table summary = new Table("PLAYER", "KILLS", "STATUS", "EXP", "LEVEL").setNoBorder();
        for (ZCharacter c : getAllCharacters()) {
            summary.addRow(c.name, c.getKillsTable(), c.isDead() ? "KIA" : "Alive", c.dangerBar, c.getSkillLevel());
        }
        String gameStatus;
        switch (gameOverStatus) {
            case GAME_LOST:
                gameStatus = String.format("Failed: %s", gameFailedReason);
                break;
            case GAME_WON:
                gameStatus = String.format("Completed");
                break;
            default:
                gameStatus = String.format("In Progress: %d%% Completed", quest.getPercentComplete(this));
        }
        return new Table(quest.getName())
                .addRow("STATUS: " + gameStatus)
                .addRow(new Table("SUMMARY").addRow(summary));
    }

    private void putBackInSearchables(ZEquipment e) {
        searchables.addFirst(e);
    }

    public boolean canSwitchActivePlayer() {
        ZCharacter cur = getCurrentCharacter();
        if (cur == null)
            return false;
        if (cur.getActionsLeftThisTurn() == cur.getActionsPerTurn())
            return true;
        if (cur.hasAvailableSkill(ZSkill.Tactician))
            return true;
        return false;
    }

    public List<ZEquipment> getAllSearchables() {
        return Collections.unmodifiableList(searchables);
    }

    public void onIronRain(ZCharacter c, int targetZone) {}

    protected void onDoorUnlocked(ZDoor door) {}

    public void unlockDoor(ZDoor door) {
        Utils.assertTrue(board.getDoor(door) == ZWallFlag.LOCKED);
        board.setDoor(door, ZWallFlag.CLOSED);
        onDoorUnlocked(door);
    }

    public void lockDoor(ZDoor door) {
        Utils.assertTrue(board.getDoor(door) != ZWallFlag.LOCKED);
        board.setDoor(door, ZWallFlag.LOCKED);
    }

}
