package cc.lib.zombicide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    public static boolean DEBUG = false;

    static {
        addAllFields(ZGame.class);
        addAllFields(State.class);
    }

    private final static Logger log = LoggerFactory.getLogger(ZGame.class);

    final static int GAME_LOST = 2;
    final static int GAME_WON  = 1;

    public static class State extends Reflector<State> {
        final ZState state;
        final ZPlayerName player;
        final ZEquipment equipment;
        final ZSkillLevel skillLevel;

        public  State() {
            this(null, null, null, null);
        }

        State(ZState state, ZPlayerName player, ZEquipment e, ZSkillLevel sk) {
            this.state = state;
            this.player = player;
            this.equipment = e;
            this.skillLevel = sk;
        }

    }

    private final Stack<State> stateStack = new Stack<>();
    protected ZBoard board=new ZBoard();
    @Omit
    private final List<ZUser> users = new ArrayList<>();
    private ZQuest quest=null;
    private int currentUser=0;
    private int startUser=0;
    private LinkedList<ZEquipment> searchables = new LinkedList<>();
    private int spawnMultiplier=1;
    private int roundNum=0;
    private int gameOverStatus=0; // 0 == in play, 1, == game won, 2 == game lost
    private ZQuests currentQuest;
    private int [] dice = null;
    private ZDifficulty difficulty = ZDifficulty.EASY;

    void pushState(ZState state, ZPlayerName player) {
        pushState(state, player, null);
    }

    void pushState(ZState state, ZPlayerName player, ZEquipment e) {
        pushState(state, player, e, null);
    }

    void pushState(ZState state, ZPlayerName player, ZEquipment e, ZSkillLevel skill) {
        pushState(new State(state, player, e, skill));
    }

    void pushState(State state) {
        ZPlayerName oldPlayer = getCurrentCharacter();
        if (state.player != oldPlayer)
            onCurrentCharacterUpdated(oldPlayer, state.player);
        stateStack.push(state);
    }

    void popState() {
        ZPlayerName curPlayer = getCurrentCharacter();
        stateStack.pop();
        if (curPlayer != getCurrentCharacter())
            onCurrentCharacterUpdated(curPlayer, getCurrentCharacter());

    }

    protected void onCurrentUserUpdated(ZUser user) {}

    protected void onCurrentCharacterUpdated(ZPlayerName priorPlayer, ZPlayerName player) {}

    public void setDifficulty(ZDifficulty difficulty) {
        this.difficulty = difficulty;
        dice = null;
    }

    public ZDifficulty getDifficulty() {
        return difficulty;
    }

    private void initGame() {
        initSearchables();
        roundNum = 0;
        gameOverStatus = 0;
        spawnMultiplier = 1;
        dice = initDice(difficulty);
        setState(ZState.INIT, null);
    }

    static int [] initDice(ZDifficulty difficulty) {
        int countPerNum;
        int step;

        // skew the odds in our favor for easier difficulties
        switch (difficulty) {
            case EASY:
                countPerNum = 20;
                step = -1;
                break;
            case MEDIUM:
                countPerNum = 30;
                step = -1;
                break;
            default:
                countPerNum = 30;
                step = 0;
        }

        int len = 0;
        int num = 6;
        int cnt = countPerNum;
        while (num > 0) {
            len += cnt;
            cnt += step;
            num --;
        }

        int [] dice = new int[len];

        cnt = countPerNum;
        num = 6;
        int idx=0;

        while (num > 0) {
            for (int i=0; i<cnt; i++)
                dice[idx++] = num;
            cnt += step;
            num--;
        }

        Utils.assertTrue(idx == dice.length);
        Utils.shuffle(dice);
        return dice;
    }

    public void clearCharacters() {
        if (board != null) {
            board.removeCharacters();
        }
    }

    public void clearUsersCharacters() {
        for (ZUser u : users) {
            u.clearCharacters();
        }
    }

    public ZBoard getBoard() {
        return board;
    }

    public Iterable<ZUser> getUsers() {
        return users;
    }

    public void setUsers(ZUser ... users) {
        this.users.clear();
        this.users.addAll(Arrays.asList(users));
    }

    public boolean isGameOver() {
        return gameOverStatus != 0;
    }

    public int getRoundNum() {
        return roundNum;
    }

    public boolean isGameWon() {
        return gameOverStatus == GAME_WON;
    }

    public boolean isGameLost() {
        return gameOverStatus == GAME_LOST;
    }

    public void reload() {
        if (currentQuest != null)
            loadQuest(currentQuest);
    }

    public int getNumKills(ZZombieType type) {
        int num=0;
        for (ZCharacter c : board.getAllCharacters()) {
            num += c.getKills(type);
        }
        return num;
    }

    public void addUser(ZUser user) {
        if (!users.contains(user))
            users.add(user);
    }

    public void removeUser(ZUser user) {
        users.remove(user);
        if (currentUser >= users.size())
            currentUser = 0;
        if (startUser >= users.size())
            startUser = 0;
    }

    protected void initQuest(ZQuest quest) {}

    public ZCharacter addCharacter(ZPlayerName pl) {
        ZCell cell = Utils.randItem(board.getCellsOfType(ZCellType.START));
        ZCharacter c = pl.create();
        c.occupiedZone = cell.zoneIndex;
        board.addActor(c, cell.zoneIndex, null);
        return c;
    }

    public void removeCharacter(ZPlayerName nm) {
        board.removeActor(nm.character);
    }

    public void loadQuest(ZQuests quest) {
        this.currentQuest = quest;
        ZQuest prevQuest = this.quest;
        this.quest = quest.load();
        board = this.quest.loadBoard();
        if (board == null)
            throw new NullPointerException("Null board loaded for quest: " + quest);
        if (prevQuest == null || !prevQuest.getName().equals(this.quest.getName()))
            initQuest(this.quest);
        initGame();
        List<ZCell> startCells = new ArrayList<>();
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
                case ZCell.ENV_TOWER:
                    zone.setType(ZZoneType.TOWER);
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
                            startCells.add(cell);
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
        // position all the characters here
        board.removeCharacters();
        int curCellIndex = 0;
        for (ZUser u : users) {
            for (ZPlayerName pl : u.getCharacters()) {
                ZCharacter c = pl.create();
                c.setColor(u.getColor());
                ZCell cell = startCells.get(curCellIndex);
                curCellIndex = (curCellIndex+1) % startCells.size();
                c.occupiedZone = cell.zoneIndex;
                board.addActor(c, cell.zoneIndex, null);
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

        while (true) {
            final ZZombieType _name = name;
            int numOnBoard = Utils.count(board.getAllZombies(), object -> object.type == _name);
            log.debug("Num %s on board is %d and trying to spawn %d more", name, numOnBoard, count);
            if (numOnBoard + count > quest.getMaxNumZombiesOfType(name)) {
                switch (name) {
                    case Necromancer:
                        name = ZZombieType.Abomination;
                        continue;
                    case Abomination:
                        name = ZZombieType.Fatty;
                        count *= 2;
                        continue;
                    case Fatty:
                    case Runner:
                        name = ZZombieType.Walker;
                        count *= 2;
                        continue;
                }
            }
            spawnZombiesInternal(name, count, zone);
            break;
        }
    }

    private void spawnZombiesInternal(ZZombieType type, int count, int zone) {
        log.debug("spawn zombies %s X %d in zone %d", type, count, zone);
        if (count > 0 && type.canDoubleSpawn && spawnMultiplier > 1) {
            log.debug("**** Spawn multiplier applied %d", spawnMultiplier);
            addLogMessage("Spawn Multiplier X " + spawnMultiplier + " Applied");
            count *= spawnMultiplier;
            spawnMultiplier = 1;
        }
        int numCurrent = Utils.count(board.getAllZombies(), object -> object.type == type);
        int max = quest.getMaxNumZombiesOfType(type);

        log.debug("Num current %ss = %d with a max of %d", type, numCurrent, max);

        for (int i = 0; i < count; i++) {
            if (numCurrent >= max)
                break;
            numCurrent++;
            ZZombie zombie = new ZZombie(type, zone);
            quest.onZombieSpawned(this, zombie, zone);
            /*
            // This part moved to quest since we want custom logic
            switch (zombie.type) {
                case Necromancer: {
                    board.setSpawnZone(zone, ZIcon.SPAWN_GREEN, false, false, true);
                    spawnZombies(zone);
                }
            }*/
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

    public ZEquipment getStateEquipment() {
        if (stateStack.isEmpty())
            throw new GException("Invalid state");
        if (stateStack.peek().equipment == null)
            throw new GException("null equipment in state");
        return stateStack.peek().equipment;
    }

    private void setState(ZState state, ZPlayerName c) {
        stateStack.clear();
        pushState(state, c);
    }

    protected void onQuestComplete() {
        addLogMessage("Quest Complete");
    }

    boolean isGameSetup() {
        if(board == null)
            return false;
        if (users.size() == 0 || getCurrentUser() == null)
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

    void addTradeOptions(ZCharacter ch, List<ZMove> options) {
        if (ch.canTrade()) {
            // check for trade with another character in the same zone (even if they are dead)
            if (isClearedOfZombies(ch.occupiedZone)) {
                List<ZPlayerName> tradablePlayers = Utils.map(Utils.filter(board.getCharactersInZone(ch.occupiedZone),
                        object -> object != ch), (zChar) -> zChar.getPlayerName());
                for (Iterator<ZPlayerName> it = tradablePlayers.iterator(); it.hasNext(); ) {
                    ZPlayerName pl = it.next();
                    if (!pl.getCharacter().canTrade() && !ch.canTrade()) {
                        it.remove();
                    }
                }
                if (tradablePlayers.size() > 0)
                    options.add(ZMove.newTradeMove(tradablePlayers));

            }
        }
    }

    void addWalkOptions(ZCharacter ch, List<ZMove> options) {
        for (ZDir dir : ZDir.values()) {
            if (getBoard().canMove(ch, dir)) {
                options.add(ZMove.newWalkDirMove(dir));
            }
        }
        List<Integer> accessibleZones = board.getAccessableZones(ch.occupiedZone, 1, 1, ZActionType.MOVE);
        if (accessibleZones.size() > 0)
            options.add(ZMove.newWalkMove(accessibleZones));
    }

    boolean isHandMoveAvailable(ZCharacter ch, ZEquipSlot slot, List<ZMoveType> types) {
        for (ZActionType ac : Utils.toArray(ZActionType.THROW_ITEM, ZActionType.RANGED, ZActionType.MELEE, ZActionType.MAGIC, ZActionType.ENCHANTMENT)) {
            ZEquipment equip = ch.getSlot(slot);
            if (equip != null && equip.getType().isActionType(ac)) {
                switch (ac) {
                    case THROW_ITEM:
                        if (types.contains(ZMoveType.THROW_ITEM))
                            return true;
                        break;
                    case RANGED:
                        if (types.contains(ZMoveType.RANGED_ATTACK))
                            return true;
                        break;
                    case MELEE:
                        if (types.contains(ZMoveType.MELEE_ATTACK))
                            return true;
                        break;
                    case MAGIC:
                        if (types.contains(ZMoveType.MAGIC_ATTACK))
                            return true;
                        break;
                    case ENCHANTMENT:
                        if (types.contains(ZMoveType.ENCHANT))
                            return true;
                        break;
                }
            }
        }
        return false;
    }

    void addHandOptions(ZCharacter ch, List<ZMove> options) {
        List<ZMoveType> types = Utils.map(options, move -> move.type);
        if (isHandMoveAvailable(ch, ZEquipSlot.LEFT_HAND, types)) {
            options.add(ZMove.newUseLeftHand());
        }
        if (isHandMoveAvailable(ch, ZEquipSlot.RIGHT_HAND, types)) {
            options.add(ZMove.newUseRightHand());
        }
    }

    private void removeDeadZombies() {
        for (ZActor z : Utils.filter(board.getAllActors(), a -> a instanceof ZZombie)) {
            if (!z.isAlive() && !z.isAnimating()) {
                board.removeActor(z);
            }
        }
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
            return false;
        }

        String failedReason;
        if (null != (failedReason=quest.getQuestFailedReason(this))) {
            gameLost(failedReason);
            return false;
        }

        if (quest.getPercentComplete(this) >= 100) {
            gameWon();
            return false;
        }

        final ZUser user = getCurrentUser();

        removeDeadZombies();

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
                onCurrentUserUpdated(getCurrentUser());
                return true;
            }

            case BEGIN_ROUND: {
                if (roundNum > 0)
                    setState(ZState.SPAWN, null);
                else
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                roundNum++;
                addLogMessage("Begin Round " + roundNum);
                onStartRound(roundNum);
                if (currentUser != startUser) {
                    currentUser = startUser;
                    onCurrentUserUpdated(getCurrentUser());
                }
                for (ZActor a : board.getAllActors())
                    a.onBeginRound();
                board.resetNoise();
                break;
            }

            case SPAWN: {
                // search cells and randomly decide on spawning depending on the
                // highest skill level of any remaining players
                ZSkillLevel highestSkill = getHighestSkillLevel();
                for (int zIdx=0; zIdx<board.getNumZones(); zIdx++) {
                    if (board.isZoneSpawnable(zIdx)) {
                        spawnZombies(zIdx, highestSkill);
                    }
                }
                setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                break;
            }

            case  PLAYER_STAGE_CHOOSE_CHARACTER: {
                // for each user, they choose each of their characters in any order and have them
                // perform all of their actions
                List<ZPlayerName> options = new ArrayList<>();

                // any player who has done a move and is not a tactician must continue to move
                for (ZPlayerName nm : getCurrentUserCharacters()) {
                    ZCharacter c = nm.getCharacter();
                    if (c.getActionsLeftThisTurn() > 0 && c.getActionsLeftThisTurn() < c.getActionsPerTurn() && !c.hasAvailableSkill(ZSkill.Tactician)) {
                        options.add(nm);
                        break;
                    }
                }

                if (options.size() == 0) {
                    options.addAll(Utils.filter(getCurrentUserCharacters(), object -> object.character.getActionsLeftThisTurn() > 0));
                }

                //if (options.size() > 0) {
                    // add characters who have organized and can do so again
                //    options.addAll(Utils.filter(getAllLivingCharacters(), object -> object.getActionsLeftThisTurn() == 0 && object.inventoryThisTurn));
                //}

                ZPlayerName currentCharacter = null;
                if (options.size() == 0) {
                    if (users.size() > 1) {
                        currentUser = (currentUser + 1) % users.size();
                        onCurrentUserUpdated(getCurrentUser());
                    }
                    if (currentUser == startUser) {
                        startUser = (startUser + 1) % users.size();
                        setState(ZState.ZOMBIE_STAGE, null);
                    }
                    break;
                } else if (options.size() == 1) {
                    currentCharacter = options.get(0);
                } else {
                    currentCharacter = getCurrentUser().chooseCharacter(options);
                }
                if (currentCharacter != null) {
                    pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, currentCharacter);
                }
                break;
            }

            case PLAYER_STAGE_CHOOSE_CHARACTER_ACTION: {

                final ZPlayerName cur = getCurrentCharacter();
                final ZCharacter ch = cur.getCharacter();

                int actionsLeft = ch.getActionsLeftThisTurn();
                LinkedList<ZMove> options = new LinkedList<>();

                // determine players available moves
                for (ZSkill skill : ch.getAvailableSkills()) {
                    skill.addSpecialMoves(this, ch, options);
                }

                if (actionsLeft > 0) {

                    // check for organize
                    if (ch.getAllEquipment().size() > 0)
                        options.add(ZMove.newInventoryMove());

                    boolean zoneCleared = isClearedOfZombies(ch.occupiedZone);

                    // check for trade with another character in the same zone (even if they are dead)
                    if (zoneCleared) {
                        addTradeOptions(ch, options);
                    }

                    // add any moves determined by the quest
                    quest.addMoves(this, ch, options);

                    ZZone zone = board.getZone(ch.occupiedZone);

                    // check for search
                    if (zoneCleared && ch.canSearch(zone)) {
                        options.add(ZMove.newSearchMove(ch.occupiedZone));
                    }

                    options.add(ZMove.newMakeNoiseMove(ch.occupiedZone));

                    // check for move up, down, right, left
                    addWalkOptions(ch, options);

                    {
                        List<ZWeapon> melee = ch.getMeleeWeapons();
                        if (melee.size() > 0) {
                            options.add(ZMove.newMeleeAttackMove(melee));
                        }
                    }

                    {
                        List<ZWeapon> ranged = ch.getRangedWeapons();
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
                        List<ZWeapon> magic = ch.getMagicWeapons();
                        if (magic.size() > 0) {
                            options.add(ZMove.newMagicAttackMove(magic));
                        }
                    }

                    {
                        List<ZEquipment> items = ch.getThrowableEquipment();
                        if (items.size() > 0) {
                            options.add(ZMove.newThrowEquipmentMove(items));
                        }
                    }

                    List<ZSpell> spells = ch.getSpells();
                    if (spells.size() > 0) {
                        options.add(ZMove.newEnchantMove(spells));
                    }

                    if (zone.getType() == ZZoneType.VAULT) {
                        List<ZEquipment> takables = quest.getVaultItems(ch.occupiedZone);
                        if (takables.size() > 0) {
                            options.add(ZMove.newPickupItemMove(takables));
                        }

                        List<ZEquipment> items = ch.getAllEquipment();
                        if (items.size() > 0) {
                            options.add(ZMove.newDropItemMove(items));
                        }
                    }

                    List<ZDoor> doors = new ArrayList<>();
                    for (ZDoor door : zone.doors) {
                        if (door.isJammed() && !ch.canUnjamDoor())
                            continue;
                        if (!door.isClosed(board) && !door.canBeClosed(ch))
                            continue;
                        if (door.isLocked(board))
                            continue;
                        doors.add(door);
                    }

                    if (doors.size() > 0) {
                        options.add(ZMove.newToggleDoor(doors));
                    }
                }

                if (options.size() == 0) {
                    ch.onEndOfTurn(this);
                    setState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER, null);
                    return false;
                }

                options.addFirst(ZMove.newEndTurn());

                addHandOptions(ch, options);
                if (canSwitchActivePlayer())
                    options.add(ZMove.newSwitchActiveCharacter());

                ZMove move = getCurrentUser().chooseMoveInternal(cur, options);
                return performMove(ch, move);
            }

            case PLAYER_STAGE_CHOOSE_NEW_SKILL: {
                final ZPlayerName cur = getCurrentCharacter();
                final ZCharacter ch = cur.getCharacter();
                ZSkill skill;
                List<ZSkill> options = getCurrentCharacter().getCharacter().getRemainingSkillsForLevel(stateStack.peek().skillLevel.getColor().ordinal());
                log.debug("Skill options for " + stateStack.peek().skillLevel + " : " + options);
                if (options.size() == 0) {
                    stateStack.pop();
                    return false;
                }
                if (options.size() == 1) {
                    skill = options.get(0);
                } else {
                    skill = getCurrentUser().chooseNewSkill(cur, options);
                }

                if (skill != null) {
                    log.debug("New Skill Chosen: " + skill);
                    onNewSkillAquired(cur, skill);
                    ch.addSkill(skill);
                    options.remove(skill);
                    popState();
                    return true;
                }
                return false;
            }

            case PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT: {
                final ZPlayerName cur = getCurrentCharacter();
                final ZEquipment equip = getStateEquipment();
                List<ZMove> options = new ArrayList<ZMove>() {{
                   add(ZMove.newKeepMove(equip));
                   add(ZMove.newDisposeEquipmentMove(equip));
                }};

                if (cur.getCharacter().getActionsLeftThisTurn() > 0 && equip.isConsumable()) {
                    options.add(ZMove.newConsumeMove(equip, null));
                }
                ZMove move = getCurrentUser().chooseMoveInternal(cur, options);
                // need to pop first since performMove might push TODO: Consider remove?
                popState();
                if (!performMove(cur.getCharacter(), move)) {
                    pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur, equip);
                    return false;
                }
                return true;
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
                            List<ZCharacter> victims = Utils.filter(board.getCharactersInZone(zombie.occupiedZone), object -> !object.isInvisible() && object.isAlive());
                            if (victims.size() > 1) {
                                Collections.sort(victims, new WoundingComparator(zombie.type));
                            }
                            if (victims.size() > 0) {
                                ZCharacter victim = victims.get(0);
                                zombie.performAction(ZActionType.MELEE, this);
                                if (playerDefends(victim, zombie.type)) {
                                    addLogMessage(victim.name() + " defends against " + zombie.name());
                                    onCharacterDefends(victim.getPlayerName(), zombie.getPosition());
                                } else {
                                    playerWounded(victim, zombie, ZAttackType.NORMAL, 1, zombie.type.name());
                                }
                            } else {
                                if (path == null) {
                                    if (zombie.getType() == ZZombieType.Necromancer) {
                                        if (isZoneEscapableForNecromancers(zombie.getOccupiedZone())) {
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
                                        List<Integer> zones = board.getAccessableZones(zombie.occupiedZone, 1, zombie.getActionsPerTurn(), ZActionType.MOVE);
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
                                    zombie.performAction(ZActionType.NOTHING, this);
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
                // compute all empty of zombie zones 1 or 2 units away form current position
                List<Integer> zones = board.getAccessableZones(getCurrentCharacter().getCharacter().getOccupiedZone(), 1, 2, ZActionType.MOVE);
                if (zones.size() == 0) {
                    popState();
                } else {
                    Integer speedMove = getCurrentUser().chooseZoneToWalk(getCurrentCharacter(), zones);
                    if (speedMove != null) {
                        moveActor(getCurrentCharacter().getCharacter(), speedMove, 200, null);
                        popState();
                        return true;
                    }
                }

                return false;
            }

            case PLAYER_STAGE_CHOOSE_SPAWN_AREA_TO_REMOVE: {
                List<ZSpawnArea> areas = new ArrayList<>();
                for (ZCell cell : board.getCells()) {
                    areas.addAll(Utils.filter(cell.getSpawnAreas(), a -> a.getRect() != null && a.isCanBeRemovedFromBoard()));
                }

                if (areas.size() > 0) {
                    ZPlayerName cur = getCurrentCharacter();
                    Integer zIdx = user.chooseSpawnAreaToRemove(cur, areas);
                    if (zIdx != null) {
                        board.removeSpawn(areas.get(zIdx));
                        onCharacterDestroysSpawn(cur, zIdx);
                        popState();
                        return true;
                    }
                } else {
                    popState();
                }

                return false;
            }

            default:
                throw new cc.lib.utils.GException("Unhandled state: " + getState());
        }

        return false;
    }

    protected void onCharacterDestroysSpawn(ZPlayerName c, int zoneIdx) {}

    protected void onCharacterDefends(ZPlayerName cur, ZActorPosition attackerPosition) {
    }

    protected void onNewSkillAquired(ZPlayerName c, ZSkill skill) {}

    private boolean playerDefends(ZCharacter cur, ZZombieType type) {
        for (int rating : cur.getArmorRatings(type)) {
            addLogMessage("Defensive roll");
            Integer [] dice = rollDice(1);
            if (dice[0] >= rating)
                return true;
        }
        return false;
    }

    void gameLost(String msg) {
        gameOverStatus = GAME_LOST;
        addLogMessage(("Game Lost " + msg).trim());
        onGameLost();
    }

    void gameWon() {
        gameOverStatus = GAME_WON;
        addLogMessage("Game Won!!!");
        onQuestComplete();
    }

    protected void onGameLost() {

    }

    void playerWounded(ZCharacter victim, ZActor attacker, ZAttackType attackType, int amount, String reason) {
        victim.wound(amount);
        if (victim.isDead()) {
            victim.clearActions();
            addLogMessage(victim.name() + " has been killed by a " + reason);
            onCharacterAttacked(victim.getPlayerName(), attacker.getPosition(), attackType, true);
            //removeCharacter(victim);
        } else {
            addLogMessage(victim.name() + " has been wounded by a " + reason);
            onCharacterAttacked(victim.getPlayerName(), attacker.getPosition(), attackType, false);
        }

    }


    protected void onCharacterAttacked(ZPlayerName character, ZActorPosition attackerPosition, ZAttackType attackType, boolean characterPerished) {
    }

    protected void onEquipmentThrown(ZPlayerName c, ZIcon icon, int zone) {
    }

    boolean isZoneEscapableForNecromancers(int zoneIdx) {
        ZZone zone = board.getZone(zoneIdx);
        for (Grid.Pos pos : zone.getCells()) {
            for (ZSpawnArea area : board.getCell(pos).getSpawnAreas()) {
                if (area.isEscapableForNecromancers())
                    return true;
            }
        }
        return false;
    }

    boolean canZoneSpawnNecromancers(int zoneIdx) {
        ZZone zone = board.getZone(zoneIdx);
        for (Grid.Pos pos : zone.getCells()) {
            for (ZSpawnArea area : board.getCell(pos).getSpawnAreas()) {
                if (area.isCanSpawnNecromancers())
                    return true;
            }
        }
        return false;
    }

    public List<ZDir> getZombiePathTowardNearestSpawn(ZZombie zombie) {
        Map<Integer, List<ZDir>> pathsMap = new HashMap<>();
        Integer shortestPath = null;
        for (ZZone zone : Utils.filter(board.getZones(), z -> isZoneEscapableForNecromancers(z.getZoneIndex()))) {
            List<List<ZDir>> paths = board.getShortestPathOptions(zombie.occupiedCell, zone.getZoneIndex());
            if (paths.size() > 0) {
                pathsMap.put(zone.getZoneIndex(), paths.get(0));
                if (shortestPath == null || paths.size() < pathsMap.get(shortestPath).size()) {
                    shortestPath = zone.getZoneIndex();
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
        for (ZCharacter c : Utils.filter(board.getAllCharacters(), object -> !object.isInvisible() && object.isAlive())) {
            if (board.canSee(zombie.occupiedZone, c.occupiedZone)) {
                int noiseLevel = board.getZone(c.occupiedZone).getNoiseLevel();
                if (maxNoise < noiseLevel) {
                    targetZone = c.occupiedZone;
                    maxNoise = noiseLevel;
                }
            }
        }

        if (targetZone < 0) {
            // move to noisiest zone
            for (int zone=0; zone < board.getNumZones(); zone++) {
                int noiseLevel = board.getZone(zone).getNoiseLevel();
                if (noiseLevel > maxNoise) {
                    maxNoise = noiseLevel;
                    targetZone = zone;
                }
            }
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

    private boolean useEquipment(ZCharacter c, ZEquipment e) {
        if (e.isMagic()) {
            return performMove(c, ZMove.newMagicAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isMelee()) {
            return performMove(c, ZMove.newMeleeAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isRanged()) {
            return performMove(c, ZMove.newRangedAttackMove(Utils.toList((ZWeapon)e)));
        } else if (e.isThrowable()) {
            return performMove(c, ZMove.newThrowEquipmentMove(Utils.toList((ZItem)e)));
        }
        return false;
    }

    private boolean performMove(ZCharacter cur, ZMove move) {
        if (move == null)
            return false;
        log.debug("performMove:%s", move);
        ZUser user = getCurrentUser();
        switch (move.type) {
            case END_TURN:
                cur.clearActions();
                cur.onEndOfTurn(this);
                popState();
                break;
            case SWITCH_ACTIVE_CHARACTER: {
                if (canSwitchActivePlayer()) {
                    int idx = 0;
                    for (ZPlayerName nm : user.getCharacters()) {
                        if (nm == cur.getType()) {
                            break;
                        }
                        idx++;
                    }
                    for (int i=(idx+1) % user.getCharacters().size(); i!=idx; i=(i+1)%user.getCharacters().size()) {
                        ZCharacter c = user.getCharacters().get(i).character;
                        if (c.isAlive() && c.getActionsLeftThisTurn() > 0) {
                            popState();
                            pushState(ZState.PLAYER_STAGE_CHOOSE_CHARACTER_ACTION, c.getPlayerName());
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
                addLogMessage(cur.name() + " Found an OBJECTIVE");
                quest.processObjective(this, cur, move);
                return true;
            }
            case INVENTORY: {
                // give options of which slot to organize
                List<ZEquipSlot> slots = new ArrayList<>();
                if (cur.getLeftHand() != null)
                    slots.add(ZEquipSlot.LEFT_HAND);
                if (cur.getRightHand() != null)
                    slots.add(ZEquipSlot.RIGHT_HAND);
                if (cur.getBody() != null)
                    slots.add(ZEquipSlot.BODY);
                if (cur.getNumBackpackItems() > 0)
                    slots.add(ZEquipSlot.BACKPACK);
                ZEquipSlot selectedSlot = getCurrentUser().chooseSlotToOrganize(cur.getPlayerName(), slots);
                if (selectedSlot == null) {
                    return false;
                }
                // choose which equipment from the slot to organize
                ZEquipment selectedEquipment = null;
                switch (selectedSlot) {
                    case BACKPACK:
                        if (cur.getNumBackpackItems() > 1) {
                            // add
                            selectedEquipment = getCurrentUser().chooseEquipmentInternal(cur.getPlayerName(), cur.getBackpack());
                        } else {
                            selectedEquipment = cur.getBackpackItem(0);
                        }
                        break;
                    case BODY:
                        selectedEquipment = cur.getBody();
                        break;
                    case LEFT_HAND:
                        selectedEquipment = cur.getLeftHand();
                        break;
                    case RIGHT_HAND:
                        selectedEquipment = cur.getRightHand();
                        break;
                }

                if (selectedEquipment == null)
                    return false;

                // we have a slot and an equipment from the slot to do something with
                // we can:
                //   dispose, unequip, equip to an empty slot or consume
                List<ZMove> options = new ArrayList<>();
                if (selectedEquipment.isConsumable() && cur.getActionsLeftThisTurn() > 0) {
                    options.add(ZMove.newConsumeMove(selectedEquipment, selectedSlot));
                }
                switch (selectedSlot) {
                    case BACKPACK:
                        if (selectedEquipment.isEquippable(cur)) {
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
                move = user.chooseMoveInternal(cur.getPlayerName(), options);
                if (move != null) {
                    return performMove(cur, move);
                }
                return false;
            }
            case TRADE:
                ZPlayerName other;
                if (move.list.size() == 1) {
                    other = (ZPlayerName) move.list.get(0);
                } else {
                    other = user.chooseTradeCharacter(cur.getPlayerName(), move.list);
                }
                if (other != null) {
                    List<ZMove> options = new ArrayList<>();
                    // we can take if our backpack is not full or give if their backpack is not full
                    for (ZEquipment eq : cur.getAllEquipment()) {
                        if (other.character.getEquipableSlots(eq).size() > 0) {
                            options.add(ZMove.newGiveMove(other, eq));
                        }
                    }

                    for (ZEquipment eq : other.character.getAllEquipment()) {
                        if (cur.getEquipableSlots(eq).size() > 0) {
                            options.add(ZMove.newTakeMove(other, eq));
                        }
                    }

                    move = user.chooseMoveInternal(cur.getPlayerName(), options);
                    if (move != null) {
                        return performMove(cur, move);
                    }
                }
                return false;
            case WALK: {
                Integer zone = move.integer;
                if (zone == null)
                    zone = user.chooseZoneToWalk(cur.getPlayerName(), move.list);
                if (zone != null) {
                    moveActor(cur, zone, cur.getMoveSpeed(), ZActionType.MOVE);
                    return true;
                    //cur.performAction(ZActionType.MOVE, this);
                }
                return false;
            }
            case JUMP: {
                Integer zone = move.integer;
                if (zone == null)
                    zone = user.chooseZoneToWalk(cur.getPlayerName(), move.list);
                if (zone != null) {
                    moveActor(cur, zone, cur.getMoveSpeed()/2, null);
                    cur.removeAvailableSkill(ZSkill.Jump);
                    return true;
                    //cur.performAction(ZActionType.MOVE, this);
                }
                return false;
            }
            case IGNITE: {
                Integer zoneToIgnite = move.integer;
                if (zoneToIgnite == null) {
                    List<Integer> ignitableZones = move.list;
                    if (ignitableZones.size() == 1) {
                        zoneToIgnite = ignitableZones.get(0);
                    }
                }
                if (zoneToIgnite == null) {
                    zoneToIgnite = user.chooseZoneToIgnite(cur.getPlayerName(), move.list);
                }
                if (zoneToIgnite != null) {
                    performDragonFire(cur, zoneToIgnite);
                    return true;
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
                    weapon = user.chooseWeaponSlotInternal(cur.getPlayerName(), weapons);
                } else {
                    weapon = weapons.get(0);
                }
                if (weapon != null) {
                    ZWeaponStat stat = cur.getWeaponStat(weapon, ZActionType.MELEE, this, cur.getOccupiedZone());
                    if (performAttack(weapon, stat, cur.getOccupiedZone())) {
                        cur.performAction( ZActionType.MELEE,this);
                        return true;
                    }
                }
                return false;
            }
            case MAGIC_ATTACK: {
                return performRangedOrMagicAttack(cur, move.list, move.integer, ZActionType.MAGIC);
            }

            case RANGED_ATTACK: {
                return performRangedOrMagicAttack(cur, move.list, move.integer, ZActionType.RANGED);
            }

            case THROW_ITEM: {
                ZEquipment slot = null;
                List<ZEquipment> slots = move.list;
                if (slots.size() == 1)
                    slot = slots.get(0);
                else
                    slot = getCurrentUser().chooseEquipmentToThrowInternal(cur.getPlayerName(), slots);
                if (slot != null) {
                    Integer zoneIdx = null;
                    if (move.integer != null) {
                        zoneIdx = move.integer;
                    } else {
                        List<Integer> zones = board.getAccessableZones(cur.occupiedZone, slot.getType().getThrowMinRange(), slot.getType().getThrowMaxRange(), ZActionType.THROW_ITEM);
                        zones.add(cur.occupiedZone);
                        zoneIdx = getCurrentUser().chooseZoneToThrowEquipment(cur.getPlayerName(), slot, zones);
                    }
                    if (zoneIdx != null) {
                        slot.getType().onThrown(this, cur, zoneIdx);
                        /*
                        switch (slot.type) {
                            case DRAGON_BILE:
                                addLogMessage(cur.name() + " threw the dragon Bile!");
                                onDragonBileThrown(cur.getPlayerName(), zoneIdx);
                                board.getZone(zoneIdx).setDragonBile(true);
                                break;
                            case TORCH: {
                                ZZone zone = board.getZone(zoneIdx);
                                onTorchThrown(cur.getPlayerName(), zoneIdx);
                                if (!zone.isDragonBile()) {
                                    addLogMessage("Throwing the Torch had no effect");
                                } else {
                                    performDragonFire(cur, zone.getZoneIndex());
                                }
                                break;
                            }
                            default:
                                throw new GException("Unhandled case: " + slot.type);
                        }*/
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
                if (cur.isDualWielding()) {
                    ((ZWeapon)cur.getSlot(ZEquipSlot.LEFT_HAND)).reload();
                    ((ZWeapon)cur.getSlot(ZEquipSlot.RIGHT_HAND)).reload();
                    addLogMessage(getCurrentCharacter().name() + " Reloaded both their " + weapon.getLabel() + "s");
                } else {
                    weapon.reload();
                    addLogMessage(getCurrentCharacter().name() + " Reloaded their " + weapon.getLabel());
                }
                cur.performAction(ZActionType.RELOAD, this);
                return true;
            }
            case OPERATE_DOOR: {
                List<ZDoor> doors = move.list;
                ZDoor door = null;
                if (doors.size() > 1)
                    door = user.chooseDoorToToggleInternal(cur.getPlayerName(), doors);
                else
                    door = doors.get(0);
                if (door != null) {
                    if (door.isClosed(board)) {
                        if (tryOpenDoor(cur, door)) {
                            door.toggle(board);
                            //showMessage(currentCharacter.name() + " has opened a " + door.name());
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
                if (cur.isEquipped(ZItemType.TORCH))
                    numCardsDrawn = 2;
                while (searchables.size() > 0 && numCardsDrawn-- > 0) {
                    ZEquipment equip = searchables.removeLast();
                    if (equip.getType() == ZItemType.AAHHHH) {
                        addLogMessage("Aaaahhhh!!!");
                        onAhhhhhh(cur.getPlayerName());
                        // spawn zombie right here right now
                        //spawnZombies(1, ZZombieType.Walker, cur.occupiedZone);
                        spawnZombies(cur.occupiedZone);
                        putBackInSearchables(equip);
                    } else {
                        onEquipmentFound(cur.getPlayerName(), equip);
                        quest.onEquipmentFound(this, equip);
                        pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur.getPlayerName(), equip);
                        if (equip.isDualWieldCapable() && cur.hasAvailableSkill(ZSkill.Matching_set)) {
                            onBonusAction(cur.getPlayerName(), ZSkill.Matching_set);
                            pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, cur.getPlayerName(), equip.getType().create());
                        }
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
                move.character.character.removeEquipment(move.equipment);
                cur.attachEquipment(move.equipment);
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            case GIVE:
                cur.removeEquipment(move.equipment);
                move.character.character.attachEquipment(move.equipment);
                cur.performAction(ZActionType.INVENTORY, this);
                return true;
            case DISPOSE:
                if (move.fromSlot != null) {
                    cur.removeEquipment(move.equipment, move.fromSlot);
                    cur.performAction(ZActionType.INVENTORY, this);
                }
                putBackInSearchables(move.equipment);
                return true;
            case KEEP: {
                ZEquipment equip = move.equipment;
                ZEquipSlot slot = cur.getEmptyEquipSlotForOrNull(equip);
                if (slot == null) {
                    // need to make room
                    List<ZMove> options = new ArrayList<>();
                    for (ZEquipment e : cur.getBackpack()) {
                        options.add(ZMove.newDisposeMove(e, ZEquipSlot.BACKPACK));
                    }

                    switch (equip.getSlotType()) {
                        case BODY:
                            options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.BODY), ZEquipSlot.BODY));
                            break;
                        case HAND:
                            options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.LEFT_HAND), ZEquipSlot.LEFT_HAND));
                            options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.RIGHT_HAND), ZEquipSlot.RIGHT_HAND));
                            if (cur.canEquipBody(equip)) {
                                options.add(ZMove.newDisposeMove(cur.getSlot(ZEquipSlot.BODY), ZEquipSlot.BODY));
                            }
                            break;
                    }

                    move = getCurrentUser().chooseMoveInternal(cur.getPlayerName(), options);
                    if (move == null)
                        return false;

                    cur.removeEquipment(move.equipment, move.fromSlot);
                    putBackInSearchables(move.equipment);
                    slot = move.fromSlot;
                }
                cur.attachEquipment(equip, slot);
                return true;
            }
            case CONSUME: {
                ZItem item  = (ZItem)move.equipment;
                ZEquipSlot slot = move.fromSlot;
                switch (item.getType()) {
                    case WATER:
                    case APPLES:
                    case SALTED_MEAT:
                        addExperience(cur, item.getType().getExpWhenConsumed());
                        if (slot != null) {
                            cur.removeEquipment(item, slot);
                        }
                        cur.performAction(ZActionType.CONSUME, this);
                        putBackInSearchables(item);
                        break;
                    default:
                        throw new cc.lib.utils.GException("Unhandled case: " + item);
                }
                return true;
            }
            case PICKUP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToPickupInternal(cur.getPlayerName(), move.list);
                if (equip != null) {
                    if (cur.tryEquip(equip) == null) {
                        ZMove keep = ZMove.newKeepMove(equip);
                        if (!performMove(cur, keep))
                            return false;
                    }
                    quest.pickupItem(cur.occupiedZone, equip);
                    cur.performAction(ZActionType.PICKUP_ITEM, this);
                    return true;
                }
                return false;
            }
            case DROP_ITEM: {
                ZEquipment equip = getCurrentUser().chooseItemToDropInternal(cur.getPlayerName(), move.list);
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
                addLogMessage(cur.name() + " made alot of noise to draw the zombies!");
                cur.performAction(ZActionType.MAKE_NOISE, this);
                return true;
            }
            case SHOVE: {
                Integer targetZone = getCurrentUser().chooseZoneToShove(cur.getPlayerName(), move.list);
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
                ZPlayerName target = null;
                if (move.list.size() == 1 && move.character != null) {
                    spell = (ZSpell)move.list.get(0);
                    target = move.character;
                } else {
                    List<ZSpell> spells = cur.getSpells();
                    spell = getCurrentUser().chooseSpell(cur.getPlayerName(), spells);
                    if (spell != null) {
                        List<ZPlayerName> targets = Utils.filter(getAllLivingCharacters(), object -> board.canSee(cur.getOccupiedZone(), object.character.getOccupiedZone()));
                        target = getCurrentUser().chooseCharacterForSpell(cur.getPlayerName(), spell, targets);
                    }
                }
                if (spell != null && target != null) {
                    spell.type.doEnchant(this, target.character);//target.availableSkills.add(spell.type.skill);
                    cur.performAction(ZActionType.ENCHANTMENT, this);
                    return true;
                }
                return false;
            }
            case BORN_LEADER: {
                ZPlayerName chosen = null;
                if (move.character != null)
                    chosen = move.character;
                else
                    chosen = getCurrentUser().chooseCharacterToBequeathMove(cur.getPlayerName(), move.list);
                if (chosen != null) {
                    if (chosen.character.getActionsLeftThisTurn() > 0) {
                        chosen.character.addExtraAction();
                    } else {
                        chosen.character.addAvailableSkill(ZSkill.Plus1_Action);
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
                action = ZActionType.RANGED;
                break;
            default:
                return false;
        }

        Integer zone;
        if (move.list.size() == 1) {
            zone = (Integer)move.list.get(0);
        } else {
            zone = getCurrentUser().chooseZoneForBloodlust(cur.getPlayerName(), move.list);
        }
        if (zone != null) {
            ZWeapon weapon = null;
            if (move.equipment != null) {
                weapon = (ZWeapon)move.equipment;
            } else if (weapons.size() > 1) {
                weapon = getCurrentUser().chooseWeaponSlotInternal(cur.getPlayerName(), weapons);
            } else if (weapons.size() == 1) {
                weapon = weapons.get(0);
            }
            if (weapon != null) {
                cur.addExtraAction();
                moveActor(cur, zone, cur.getMoveSpeed()/2, ZActionType.MOVE);
                performAttack(weapon, weapon.getStatForAction(action), zone);
                cur.performAction(action, this);
                cur.removeAvailableSkill(move.skill);
                return true;
            }
        }
        return false;
    }

    protected void onAhhhhhh(ZPlayerName c) {}

    protected void onNecromancerEscaped(ZZombie necro) {}

    protected void onEquipmentFound(ZPlayerName c, ZEquipment equipment) {}

    protected void onCharacterHealed(ZPlayerName c, int amt) {}

    protected void onSkillKill(ZPlayerName c, ZSkill skill, ZZombie z, ZAttackType attackType) {}

    protected void onRollSixApplied(ZPlayerName c, ZSkill skill) {}

    protected void onWeaponReloaded(ZPlayerName c, ZWeapon w) {}
/*
    static List<ZZombie> filterZombiesForMelee(List<ZZombie> list, int weaponDamage) {
        List<ZZombie> zombies = Utils.filter(list, object -> object.type.minDamageToDestroy <= weaponDamage);
        if (zombies.size() > 1)
            Collections.sort(zombies);
        return zombies;
    }

    static List<ZZombie> filterZombiesForRanged(List<ZZombie> zombies, int weaponDamage) {
        if (zombies.size() > 1)
            Collections.sort(zombies);
        // find the first zombie whom we cannot destroy and remove that one and all after since ranged priority
        int numHittable = 0;
        for (ZZombie z : zombies) {
            if (z.type.minDamageToDestroy <= weaponDamage)
                numHittable++;
        }
//        log.debug("There are %d hittable zombies", numHittable);
        while (zombies.size() > numHittable)
            zombies.remove(zombies.size() - 1);
        return zombies;
    }

    static List<ZZombie> filterZombiesForMarksman(List<ZZombie> zombies, int weaponDamage) {
// marksman zombie sorting works differently
        zombies = Utils.filter(zombies, object -> object.type.minDamageToDestroy <= weaponDamage);
        Collections.reverse(zombies);
        return zombies;
    }
*/
    private boolean performRangedOrMagicAttack(ZCharacter cur, List<ZWeapon> weapons, Integer zoneIdx, ZActionType actionType) {
        // rules same for both kinda
        ZUser user = getCurrentUser();
        ZWeapon weapon = null;
        if (weapons.size() > 1) {
            weapon = user.chooseWeaponSlotInternal(cur.getPlayerName(), weapons);
        } else {
            weapon = weapons.get(0);
        }
        if (weapon != null) {
            if (zoneIdx == null) {
                ZWeaponStat stat = cur.getWeaponStat(weapon, actionType, this, -1);
                List<Integer> zones = board.getAccessableZones(cur.occupiedZone, stat.minRange, stat.maxRange, actionType);
                if (zones.size() == 0)
                    return false;
                zoneIdx = user.chooseZoneForAttack(cur.getPlayerName(), zones);
            }
            if (zoneIdx != null) {
                ZWeaponStat stat = cur.getWeaponStat(weapon, actionType, this, zoneIdx);
                if (performAttack(weapon, stat, zoneIdx)) {
                    cur.performAction(actionType, this);
                    return true;
                }
            }
        }
        return false;
    }

    public final static int ACTOR_POS_DATA_DEFENDED = 0;
    public final static int ACTOR_POS_DATA_DAMAGED = 1;

    private boolean performAttack(ZWeapon weapon, ZWeaponStat stat, int zoneIdx) {
        Utils.assertTrue(stat != null);
        ZUser user = getCurrentUser();
        ZCharacter cur = getCurrentCharacter().getCharacter();
        ZActionType actionType = stat.actionType;
        switch (actionType) {
            case MELEE: {
                List<ZZombie> zombies = board.getZombiesInZone(cur.occupiedZone);
                Collections.sort(zombies, new MarksmanComparator(stat.damagePerHit));
                int hits = resolveHits(cur, zombies.size(), weapon.type, stat, zombies.size() / 2 - 1, zombies.size() / 2 + 1);
                // when we attack with melee there will be some misses and some hits that are defended
                List<ZActorPosition> zombiesHit = new ArrayList<>();
                List<ZZombie> zombiesDestroyed = new ArrayList<>();
                Iterator<ZZombie> it = zombies.iterator();
                while (it.hasNext()) {
                    if (hits <= 0)
                        break;
                    ZZombie z = it.next();
                    ZActorPosition pos = z.getPosition();
                    zombiesHit.add(pos);
                    if (z.getType().minDamageToDestroy <= stat.damagePerHit) {
                        zombiesDestroyed.add(z);
                        it.remove();
                        hits--;
                        pos.setData(ACTOR_POS_DATA_DAMAGED);
                    } else {
                        pos.setData(ACTOR_POS_DATA_DEFENDED);
                    }
                }
                onAttack(cur.getPlayerName(), weapon, actionType, stat.numDice, zombiesHit, cur.getOccupiedZone());
                for (ZZombie z : zombiesDestroyed) {
                    addExperience(cur, z.type.expProvided);
                    destroyZombie(z, stat.attackType, cur);
                }
                if (weapon.isAttackNoisy()) {
                    addNoise(cur.occupiedZone, 1);
                }
                addLogMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                for (ZSkill skill : Utils.mergeLists(cur.getAvailableSkills(), weapon.getType().getSkillsWhenUsed())) {
                    skill.onAttack(this, cur, actionType, stat, cur.getOccupiedZone(), hits, zombiesDestroyed);
                }
                if (hits > 0)
                    checkForHitAndRun(cur);
                return true;
            }
            case MAGIC:
            case RANGED: {
                //ZWeaponStat stat = cur.getWeaponStat(weapon, actionType, this, zoneIdx);
                // process a ranged attack
                if (!weapon.isLoaded()) {
                    addLogMessage("CLICK! Weapon not loaded!");
                    onWeaponGoesClick(cur.getPlayerName(), weapon);
                } else {
                    weapon.fireWeapon(this, cur, stat);
                    ZZone zone = board.getZone(zoneIdx);
                    if (weapon.getType().isFire() && zone.isDragonBile()) {
                        performDragonFire(cur, zone.getZoneIndex());
                    } else {
                        List<ZZombie> zombies = board.getZombiesInZone(zoneIdx);
                        if (zombies.size() > 1) {
                            switch (actionType) {
                                case RANGED:
                                    if (cur.hasAvailableSkill(ZSkill.Marksman)) {
                                        Collections.sort(zombies, new MarksmanComparator(stat.damagePerHit));
                                        break;
                                    }
                                    Collections.sort(zombies, new RangedComparator());
                            }
                            log.debug("Ranged Priority:" + Utils.map(zombies, z -> z.getType()));
                        }

                        int hits = resolveHits(cur, zombies.size(), weapon.type, stat, zombies.size() / 2 - 1, zombies.size() / 2 + 1);
                        int hitsMade = 0;
                        List<ZZombie> zombiesDestroyed = new ArrayList<>();
                        List<ZActorPosition> actorsHit = new ArrayList<>();
                        for (int i = 0; i < hits && zombies.size() > 0; i++) {
                            ZZombie zombie = zombies.remove(0);
                            ZActorPosition pos = zombie.getPosition();
                            actorsHit.add(pos);
                            if (zombie.type.minDamageToDestroy <= stat.damagePerHit) {
                                zombiesDestroyed.add(zombie);
                                hitsMade++;
                                pos.setData(ACTOR_POS_DATA_DAMAGED);
                            } else {
                                pos.setData(ACTOR_POS_DATA_DEFENDED);
                            }
                        }
                        // pre-process friendly fire actors here
                        List<ZCharacter> friendsHit = new ArrayList<>();
                        if (cur.canFriendlyFire()) {
                            int misses = stat.numDice - hitsMade;
                            List<ZCharacter> friendlyFireOptions = Utils.filter(board.getCharactersInZone(zoneIdx), object -> object != cur && object.canReceiveFriendlyFire());
                            for (int i = 0; i < misses && friendlyFireOptions.size() > 0; i++) {
                                if (friendlyFireOptions.size() > 1) {
                                    // sort them in same way we would sort zombie attacks
                                    Collections.sort(friendlyFireOptions, new WoundingComparator(ZZombieType.Walker));
                                }
                                // friendy fire!
                                ZCharacter victim = friendlyFireOptions.get(0);
                                if (playerDefends(victim, ZZombieType.Walker)) {
                                    addLogMessage(victim.name() + " defended thyself from friendly fire!");
                                    //onCharacterDefends(victim.getPlayerName(), cur.getPosition());
                                    actorsHit.add(victim.getPosition().setData(ACTOR_POS_DATA_DEFENDED));
                                } else {
                                    friendsHit.add(victim);
                                    actorsHit.add(victim.getPosition().setData(ACTOR_POS_DATA_DAMAGED));
                                    //playerWounded(victim, cur, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                                    //if (victim.isDead())
                                    if (victim.getWoundBar() + stat.damagePerHit >= ZCharacter.MAX_WOUNDS) {
                                        // killed em
                                        friendlyFireOptions.remove(0);
                                    }
                                }
                            }
                        }
                        onAttack(cur.getPlayerName(), weapon, actionType, stat.numDice, actorsHit, zoneIdx);
                        for (ZZombie zombie : zombiesDestroyed) {
                            destroyZombie(zombie, stat.attackType, cur);
                            addExperience(cur, zombie.type.expProvided);
                        }
                        for (ZSkill skill : Utils.mergeLists(cur.getAvailableSkills(), weapon.getType().getSkillsWhenUsed())) {
                            skill.onAttack(this, cur, actionType, stat, zoneIdx, hits, zombiesDestroyed);
                        }
                        for (ZCharacter victim : friendsHit) {
                            playerWounded(victim, cur, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                        }
                        /*
                        if (cur.canFriendlyFire()) {
                            int misses = stat.numDice - hitsMade;
                            List<ZCharacter> friendlyFireOptions = Utils.filter(board.getCharactersInZone(zoneIdx), object -> object != cur && object.canReceiveFriendlyFire());
                            for (int i = 0; i < misses && friendlyFireOptions.size() > 0; i++) {
                                if (friendlyFireOptions.size() > 1) {
                                    // sort them in same way we would sort zombie attacks
                                    Collections.sort(friendlyFireOptions, new WoundingComparator(ZZombieType.Walker));
                                }
                                // friendy fire!
                                ZCharacter victim = friendlyFireOptions.get(0);
                                if (playerDefends(victim, ZZombieType.Walker)) {
                                    addLogMessage(victim.name() + " defended thyself from friendly fire!");
                                    onCharacterDefends(victim.getPlayerName(), cur.getPosition());
                                } else {
                                    playerWounded(victim, cur, stat.getAttackType(), stat.damagePerHit, "Friendly Fire!");
                                    if (victim.isDead())
                                        friendlyFireOptions.remove(0);
                                }
                            }
                        }*/
                        addLogMessage(getCurrentCharacter().name() + " Scored " + hits + " hits");
                        if (hits > 0)
                            checkForHitAndRun(cur);
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    void performSkillKill(ZCharacter c, ZSkill skill, ZZombie z, ZAttackType at) {
        onSkillKill(c.getPlayerName(), skill, z, at);
        addExperience(c, z.type.expProvided);
        destroyZombie(z, at, c);
    }

    static class WoundingComparator implements Comparator<ZCharacter> {
        final ZZombieType zType;
        WoundingComparator(ZZombieType zType) {
            this.zType = zType;
        }

        @Override
        public int compare(ZCharacter o1, ZCharacter o2) {
            int v0 = o1.getArmorRating(zType) - o1.getWoundBar();
            int v1 = o2.getArmorRating(zType) - o2.getWoundBar();
            return Integer.compare(v1, v0);
        }
    }

    static class RangedComparator implements Comparator<ZZombie> {
        @Override
        public int compare(ZZombie o1, ZZombie o2) {
            return Integer.compare(o1.type.attackPriority, o2.type.attackPriority);
        }
    }

    // Marksman have reverse of Ranged expect Zombies with minDamage above are prioritized last
    static class MarksmanComparator implements Comparator<ZZombie> {
        final int attackDamage;

        MarksmanComparator(int attackDamage) {
            this.attackDamage = attackDamage;
        }

        @Override
        public int compare(ZZombie o1, ZZombie o2) {
            int o1Value = o1.type.minDamageToDestroy > attackDamage ? 0 : o1.type.attackPriority;
            int o2Value = o2.type.minDamageToDestroy > attackDamage ? 0 : o2.type.attackPriority;
            return Integer.compare(o2Value, o1Value); // descending order
        }
    }


    private int resolveHits(ZCharacter cur, int maxHits, ZWeaponType type, ZWeaponStat stat, int minHitsForAutoReroll, int maxHitsForAutoNoReroll) {
        Integer [] result;
        if (cur.canReroll(stat.attackType)) {
            result = rollDiceWithRerollOption(stat.numDice, stat.dieRollToHit, minHitsForAutoReroll, maxHitsForAutoNoReroll);
        } else {
            result = rollDice(stat.numDice);
        }
        int hits = 0;
        //boolean isRoll6Plus1Die = cur.isRoll6Plus1Die(type, stat.actionType);
        boolean keepGoing;
        do {
            keepGoing = false;
            for (int i=0; i<result.length; i++) {
                if (result[i] >= stat.dieRollToHit) {
                    hits ++;
                }
            }
            int numSixes = Utils.count(result, object -> object == 6);
            if (numSixes > 0) {
                for (ZSkill skill : Utils.mergeLists(cur.getAvailableSkills(), type.getSkillsWhenUsed())) {
                    if (skill.onSixRolled(this, cur, stat) && hits < maxHits) {
                        result = rollDice(numSixes);
                        keepGoing = true;
                    }
                }
            } else {
                break;
            }
            if (keepGoing) {
                addLogMessage("Rolled " + numSixes + " 6s + 1Die roll each!");
            }
        } while (keepGoing);
        return hits;
    }

    protected void onWeaponGoesClick(ZPlayerName c, ZWeapon weapon) {}

    protected void onCharacterOpenedDoor(ZPlayerName cur, ZDoor door) {}

    protected void onCharacterOpenDoorFailed(ZPlayerName cur, ZDoor door) {}

    /**
     * Prior to processing damages and kills this iu
     * @param attacker
     * @param weapon
     * @param actionType
     * @param numDice
     * @param actorsHit
     * @param targetZone
     */
    protected void onAttack(ZPlayerName attacker, ZWeapon weapon, ZActionType actionType, int numDice, List<ZActorPosition> actorsHit, int targetZone) {}

    protected void onBonusAction(ZPlayerName pl, ZSkill action) {}

    private void checkForHitAndRun(ZCharacter cur) {
        if (cur.hasAvailableSkill(ZSkill.Hit_and_run) && board.getNumZombiesInZone(cur.getOccupiedZone()) == 0) {
            cur.addAvailableSkill(ZSkill.Plus1_free_Move_Action);
            addLogMessage(cur.getLabel() + " used Hit and Run for a free move action");
            onBonusAction(cur.getPlayerName(), ZSkill.Plus1_free_Move_Action);
        }
    }

    public ZSkillLevel getHighestSkillLevel() {
        ZSkillLevel best = new ZSkillLevel(ZColor.BLUE);
        for (ZUser u : users) {
            for (ZPlayerName c : u.getCharacters()) {
                ZSkillLevel lvl = c.character.getSkillLevel();
                if (best.compareTo(lvl) < 0) {
                    best = lvl;
                }
            }
        }
        return best;
    }

    public void addExperience(ZCharacter c, int pts) {
        if (pts <= 0)
            return;
        ZSkillLevel sl = c.getSkillLevel();
        c.addExperience(pts);
        onCharacterGainedExperience(c.getPlayerName(), pts);
        // make so a user can level up multiple times in a single level up
        // need to push state in reverse order so that the lowest new level choices are first
        List<State> states = new ArrayList<>();
        while (!sl.equals(c.getSkillLevel())) {
            sl = sl.nextLevel();
            addLogMessage(c.name() + " has gained the " + sl.toString() + " skill level");
            states.add(new State(ZState.PLAYER_STAGE_CHOOSE_NEW_SKILL, c.getPlayerName(), null,  sl));
        }
        Collections.reverse(states);
        for (State s: states)
            pushState(s);
    }

    protected void onCharacterGainedExperience(ZPlayerName c, int points) {
        log.info("%s gained %d experence!", c, points);
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

        addLogMessage(getCurrentCharacter().getLabel() + " Scored " + hits + " hits");
        //onRollDice(dice);

        if (hits >= maxHitsForAutoNoReroll) {
            return dice;
        }

        if (hits > minHitsForAutoReroll) {
            ZMove plentyOMove = getCurrentUser().chooseMoveInternal(getCurrentCharacter(),
                    Arrays.asList(ZMove.newReRollMove(), ZMove.newKeepRollMove()));

            if (plentyOMove != null) {
                if (plentyOMove.type == ZMoveType.KEEP_ROLL)
                    return dice;
            }
        }

        addLogMessage("Bonus roll dice!");
        return rollDice(numDice);
    }

    Integer [] rollDice(int num) {
        if (dice == null) {
            dice = initDice(difficulty);
        }
        Integer [] result = new Integer[num];
        String dieStrEnd = "+";
        String dieStrMid = "|";
        for (int i=0; i<num; i++) {
            result[i] = dice[i];
            dieStrMid += String.format(" %d |", result[i]);
            dieStrEnd += "---+";
        }
        for (int i = 0; i< dice.length-num; i++) {
            dice[i] = dice[i+num];
        }
        for (int i = 0; i< num; i++) {
            dice[i+ dice.length-num] = result[i];
        }
        //showMessage("Rolled a " + dieStr);//new Table().addRow(result).toString());
        log.info("Rolled a " + dieStrEnd);
        log.info("Rolled a " + dieStrMid);
        log.info("Rolled a " + dieStrEnd);
        onRollDice(result);
        return result;
    }

    protected void onRollDice(Integer [] roll) {
        log.info("Rolling dice result is: %s", Arrays.toString(roll));
    }

    void destroyZombie(ZZombie zombie, ZAttackType deathType, ZCharacter killer) {
        killer.onKilledZombie(zombie);
        onZombieDestroyed(killer.getPlayerName(), deathType, zombie.getPosition());
        zombie.destroyed = true;//board.removeActor(zombie);
        if (zombie.getType() == ZZombieType.Necromancer) {
            pushState(ZState.PLAYER_STAGE_CHOOSE_SPAWN_AREA_TO_REMOVE, killer.getPlayerName());
        }
    }

    protected void onZombieDestroyed(ZPlayerName c, ZAttackType deathType, ZActorPosition zombiePos) {
        ZZombie zombie = board.getActor(zombiePos);
        log.info("%s Zombie %s destroyed for %d experience", c.name(), zombie.type.name(), zombie.type.expProvided);
    }

    public List<ZPlayerName> getAllCharacters() {
        return Utils.map(board.getAllCharacters(), (in)-> in.getPlayerName());
    }

    public List<ZPlayerName> getAllLivingCharacters() {
        return Utils.filter(getAllCharacters(), object -> object.character.isAlive());
    }

    public List<ZPlayerName> getCurrentUserCharacters() {
        List<ZPlayerName> list = new ArrayList<>();
        for (ZPlayerName nm : getCurrentUser().getCharacters()) {
            if (nm.character.isAlive())
                list.add(nm);
        }
        return list;
    }

    private boolean isClearedOfZombies(int zoneIndex) {
        return board.getNumZombiesInZone(zoneIndex) == 0;
    }

    protected void onDoubleSpawn(int multiplier) {
    }

    private void doubleSpawn() {
        spawnMultiplier *= 2;
        addLogMessage("DOUBLE SPAWN!");
        onDoubleSpawn(spawnMultiplier);
    }

    private void extraActivation(ZZombieCategory category) {
        onExtraActivation(category);
        addLogMessage("EXTRA ACTIVATION!");
        for (ZZombie z : board.getAllZombies()) {
            if (z.type.category == category)
                z.addExtraAction();
        }
    }

    protected void onExtraActivation(ZZombieCategory category) {}

    public void spawnZombies(int zoneIdx) {
        spawnZombies(zoneIdx, getHighestSkillLevel());
    }

    private void spawnZombies(int zoneIdx, ZSkillLevel level) {
        //ZSpawnArea spawnType = quest.getSpawnType(this, board.getZone(zoneIdx));
        ZSpawnCard card = ZSpawnCard.drawSpawnCard(quest.isWolfBurg(), canZoneSpawnNecromancers(zoneIdx), difficulty);
        log.debug("Draw spawn card: " + card);
        ZSpawnCard.Action action = card.getAction(level.getDifficultyColor());
        switch (action.action) {
            case NOTHING_IN_SIGHT:
                break;
            case SPAWN:
                spawnZombiesInternal(action.type, action.count, zoneIdx);
                break;
            case DOUBLE_SPAWN:
                doubleSpawn();
                break;
            case EXTRA_ACTIVATION_STANDARD:
                extraActivation(ZZombieCategory.STANDARD);
                break;
            case EXTRA_ACTIVATION_NECROMANCER:
                extraActivation(ZZombieCategory.NECROMANCER);
                break;
            case EXTRA_ACTIVATION_WOLFSBURG:
                extraActivation(ZZombieCategory.WOLFSBURG);
                break;
        }
    }

    public ZUser getCurrentUser() {
        return users.get(currentUser);
    }

    public ZPlayerName getCurrentCharacter() {
        if (stateStack.isEmpty())
            return null;
        return stateStack.peek().player;
    }

    protected void moveActor(ZActor actor, int toZone, long speed, ZActionType actionType) {
        int fromZone = actor.getOccupiedZone();
        Grid.Pos fromPos = actor.getOccupiedCell();
        GRectangle fromRect = actor.getRect(board);
        board.moveActor(actor, toZone);
        doMove(actor, fromZone, fromPos, fromRect, speed, actionType);
    }

    protected void moveActorInDirection(ZActor actor, ZDir dir) {
        int fromZone = actor.getOccupiedZone();
        Grid.Pos fromPos = actor.getOccupiedCell();
        GRectangle fromRect = actor.getRect(board);
        Grid.Pos next = board.getAdjacent(fromPos, dir);
        board.moveActor(actor, next);
        doMove(actor, fromZone, fromPos, fromRect, actor.getMoveSpeed(), ZActionType.MOVE);
    }

    public void moveActorInDirectionDebug(ZActor actor, ZDir dir) {
        int fromZone = actor.getOccupiedZone();
        Grid.Pos fromPos = actor.getOccupiedCell();
        //GRectangle fromRect = actor.getRect(board);
        Grid.Pos next = board.getAdjacent(fromPos, dir);
        board.moveActor(actor, next);
        //doMove(actor, fromZone, fromPos, fromRect, actor.getMoveSpeed());
    }

    private void doMove(ZActor actor, int fromZone, Grid.Pos fromPos, GRectangle fromRect, long speed, ZActionType actionType) {
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
        if (toZone != fromZone && actionType != null) {
            actor.performAction(ZActionType.MOVE, this);
        }
    }

    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end, long speed) {
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
        searchables.addAll(make(2, ZWeaponType.DAGGER));
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
        searchables.addAll(make(2, ZWeaponType.SHORT_SWORD));
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
//        showMessage("Noise was made in zone " + zoneIdx);
    }

    protected void onNoiseAdded(int zoneIndex) {
    }

    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {}

    public Table getGameSummaryTable() {
        Table summary = new Table("PLAYER", "KILLS", "STATUS", "EXP", "LEVEL").setNoBorder();
        for (ZCharacter c : board.getAllCharacters()) {
            summary.addRow(c.getPlayerName(), c.getKillsTable(), c.isDead() ? "KIA" : "Alive", c.getDangerBar(), c.getSkillLevel());
        }
        String gameStatus;
        switch (gameOverStatus) {
            case GAME_LOST:
                gameStatus = quest.getQuestFailedReason(this);
                break;
            case GAME_WON:
                gameStatus = String.format("Completed");
                break;
            default:
                gameStatus = String.format("In Progress: %d%% Completed", Utils.clamp(quest.getPercentComplete(this), 0, 100));
        }
        return new Table(quest.getName())
                .addRow("STATUS: " + gameStatus)
                .addRow(new Table("SUMMARY").addRow(summary));
    }

    void putBackInSearchables(ZEquipment e) {
        searchables.addFirst(e);
    }

    private boolean canSwitchActivePlayer() {
        ZPlayerName cur = getCurrentCharacter();
        if (cur == null)
            return false;
        if (cur.character.getActionsLeftThisTurn() == cur.character.getActionsPerTurn())
            return true;
        if (cur.character.hasAvailableSkill(ZSkill.Tactician))
            return true;
        return false;
    }

    public List<ZEquipment> getAllSearchables() {
        return Collections.unmodifiableList(searchables);
    }

    public void onIronRain(ZPlayerName c, int targetZone) {}

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

    public void addLogMessage(String msg) {
        log.info(msg);
    }

    void performDragonFire(ZCharacter cur, int zoneIdx) {
        onDragonBileExploded(zoneIdx);
        board.getZone(zoneIdx).setDragonBile(false);
        int exp = 0;
        int num=0;
        addLogMessage(cur.name() + " ignited the dragon bile!");
        for (ZActor a : Utils.filter(board.getActorsInZone(zoneIdx), a -> a.isAlive())) {
            if (a instanceof ZZombie) {
                ZZombie z = (ZZombie)a;
                exp += z.type.expProvided;
                destroyZombie(z, ZAttackType.FIRE, cur);
                num++;
            } else if (a instanceof ZCharacter) {
                // characters caught in the zone get wounded
                ZCharacter c = (ZCharacter)a;
                playerWounded(c, cur, ZAttackType.FIRE, 4, "Exploding Dragon Bile");
            }
        }
        if (cur.isAlive()) {
            addExperience(cur, exp);
            addLogMessage(String.format("%s Destroyed %d zombies for %d total experience pts!", cur.name(), num, exp));
        } else {
            addLogMessage(String.format("%s Destroyed %d zombies and themselves in the process!", cur.name(), num));
        }
        quest.onDragonBileExploded(cur, zoneIdx);

    }

    public void giftEquipment(ZCharacter c, ZEquipment e) {
        onEquipmentFound(c.getPlayerName(), e);
        quest.onEquipmentFound(this, e);
        pushState(ZState.PLAYER_STAGE_CHOOSE_KEEP_EQUIPMENT, c.getPlayerName(), e);
    }

    private boolean tryOpenDoor(ZCharacter cur, ZDoor door) {
        if (!door.isJammed())
            return true;

        if (cur.tryOpenDoor(this)) {
            onCharacterOpenedDoor(cur.getPlayerName(), door);
            return true;
        }
        onCharacterOpenDoorFailed(cur.getPlayerName(), door);
        return false;
    }



    protected void onDragonBileExploded(int zoneIdx) {}
}
