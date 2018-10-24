package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.utils.Reflector;

public class DDungeon extends Reflector<DDungeon> {

    public interface Listener {
        void onPrize(Prize p);

        void onEnemyDead(DEnemy e);

        void onPlayerDead(DPlayer p);

        void onMiss(DEntity attacker);

        void onDamage(DEntity e, int damage);
    }


    private final static Logger log = LoggerFactory.getLogger(DDungeon.class);

    static {
        addAllFields(DDungeon.class);
    }

    enum State {
        INIT,
        ROLL_DICE_TO_ADVANCE,
        ADVANCE,
        ROLL_DICE_FOR_RED_PRIZE,
        ROLL_DICE_FOR_BLUE_PRIZE,
        ROLL_DICE_FOR_FIGHT,
        ROLL_DICE_FOR_ROOM,
        ROLL_DICE_FOR_LOCKED_ROOM,
        CHOOSE_FIGHT_OR_FLEE,
        ROLL_DICE_FOR_ATTACK,
    }

    enum Prize {
        KEY,
        DEFENSE_PLUS1,
        DEFENSE_PLUS2,
        DEFENSE_PLUS3,
        ATTACK_PLUS1,
        ATTACK_PLUS2,
        ATTACK_PLUS3,
        MAGIC_PLUS2,
        HP_PLUS_1,
        HP_PLUS_2;

        void apply(DPlayer p) {
            switch (this) {

                case KEY:
                    p.key = true;
                    break;
                case DEFENSE_PLUS1:
                    p.defense+=1;
                    break;
                case DEFENSE_PLUS2:
                    p.defense+=2;
                    break;
                case DEFENSE_PLUS3:
                    p.defense+=3;
                    break;
                case ATTACK_PLUS1:
                    p.attack+=1;
                    break;
                case ATTACK_PLUS2:
                    p.attack+=2;
                    break;
                case ATTACK_PLUS3:
                    p.attack+=3;
                    break;
                case MAGIC_PLUS2:
                    ;
                    break;
                case HP_PLUS_1:
                    p.hp+=1;
                    break;
                case HP_PLUS_2:
                    p.hp+=1;
                    break;
            }
        }
    }

    enum DiceConfig {
        ONE_6x6,
        TWO_6x6,
        THREE_6x6
    }

    DBoard board;
    DPlayer [] players;
    int numPlayers;
    int curPlayer;
    int curEnemy;
    State state = null;
    int die1, die2, die3;
    DiceConfig diceConfig = DiceConfig.ONE_6x6;
    final List<DEnemy> enemyList = new ArrayList<>();
    @Omit
    HashSet<Listener> listeners = new HashSet<>();

    public int getDieRoll() {
        switch (diceConfig) {
            case ONE_6x6:
                return die1;
            case TWO_6x6:
                return die1 + die2;
            case THREE_6x6:
                return die1 + die2 + die3;
            default:
                Utils.unhandledCase(diceConfig);
        }
        return 0;
    }

    public final void setPlayer(int index, DPlayer player) {
        if (state != State.INIT)
            throw new RuntimeException("Cannot modify players while in game");
        if (index < 0 || index >= numPlayers)
            throw new IndexOutOfBoundsException();
        players[index] = player;
        player.playerNum = index;
    }

    public final void addPlayer(DPlayer player) {
        if (state != State.INIT)
            throw new RuntimeException("Cannot modify players while in game");
        if (player == null)
            throw new NullPointerException();
        players[numPlayers] = player;
        player.playerNum = numPlayers++;
    }

    public final int getTurn() {
        return curPlayer;
    }

    public final DPlayer getCurPlayer() {
        return players[curPlayer];
    }

    public void newGame() {
        if (players.length < 1) {
            throw new RuntimeException("Must have at least 1 player");
        }
        state = State.INIT;
        curPlayer = 0;
        die1 = die2 = die3 = 0;
        enemyList.clear();

    }

    public final Iterable<DPlayer> getPlayers() {
        return new Iterable<DPlayer>() {
            @Override
            public Iterator<DPlayer> iterator() {
                return new Iterator<DPlayer>() {
                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < numPlayers;
                    }

                    @Override
                    public DPlayer next() {
                        return players[index++];
                    }
                };
            }
        };
    }

    public final void runGame() {
        switch (state) {
            case INIT:
                curPlayer = 0;
                curEnemy = -1;
                enemyList.clear();
                for (DPlayer p : getPlayers()) {
                    p.cellIndex = board.getStartCellIndex();
                }
                break;

            case ROLL_DICE_TO_ADVANCE: {
                if (rollDice(DiceConfig.ONE_6x6)) {
                    state = State.ADVANCE;
                }
                break;
            }

            case ADVANCE: {
                DMove[] moves = board.findMoves(getCurPlayer(), getDieRoll());
                DMove move = getCurPlayer().chooseMove(moves);
                if (move != null) {
                    getCurPlayer().backCellIndex = getCurPlayer().cellIndex;
                    getCurPlayer().cellIndex = move.index;
                    DCell cell = board.getCell(move.index);
                    switch (cell.getCellType()) {

                        case EMPTY:
                            log.warn("Cell %d is EMPTY", move.index);
                        case WHITE:
                        case START:
                            nextPlayer();
                            break;
                        case RED:
                            state = State.ROLL_DICE_FOR_RED_PRIZE;
                            break;
                        case GREEN:
                            enemyList.add(DEnemy.EnemyType.SNAKE.newEnemy());
                            state = State.ROLL_DICE_FOR_FIGHT;
                            break;
                        case BLUE:
                            state = State.ROLL_DICE_FOR_BLUE_PRIZE;
                            break;
                        case BROWN:
                            enemyList.add(DEnemy.EnemyType.RAT.newEnemy());
                            state = State.ROLL_DICE_FOR_FIGHT;
                            break;
                        case BLACK:
                            enemyList.add(DEnemy.EnemyType.SPIDER.newEnemy());
                            state = State.ROLL_DICE_FOR_FIGHT;
                            break;
                        case ROOM:
                            state = State.ROLL_DICE_FOR_ROOM;
                            break;
                        case LOCKED_ROOM:
                            state = State.ROLL_DICE_FOR_LOCKED_ROOM;
                            break;
                    }
                }
                break;
            }

            case ROLL_DICE_FOR_FIGHT:
                if (rollDice(DiceConfig.ONE_6x6)) {
                    DEnemy enemy = enemyList.get(0);
                    if (getDieRoll() <= enemy.type.chanceToFight) {
                        state = State.CHOOSE_FIGHT_OR_FLEE;
                    } else {
                        nextPlayer();
                    }
                }
                break;

            case ROLL_DICE_FOR_BLUE_PRIZE:
                if (rollDice(DiceConfig.ONE_6x6)) {
                    Prize [] prizes = {
                            Prize.DEFENSE_PLUS3,
                            Prize.DEFENSE_PLUS2,
                            Prize.DEFENSE_PLUS2,
                            Prize.DEFENSE_PLUS1,
                            Prize.DEFENSE_PLUS1,
                            Prize.DEFENSE_PLUS1,
                    };
                    onPrize(prizes[getDieRoll()]);
                    prizes[getDieRoll()].apply(getCurPlayer());
                }
                break;

            case ROLL_DICE_FOR_RED_PRIZE:
                if (rollDice(DiceConfig.ONE_6x6)) {
                    Prize [] prizes = {
                            Prize.ATTACK_PLUS3,
                            Prize.ATTACK_PLUS2,
                            Prize.ATTACK_PLUS2,
                            Prize.ATTACK_PLUS1,
                            Prize.ATTACK_PLUS1,
                            Prize.ATTACK_PLUS1,
                    };
                    onPrize(prizes[getDieRoll()]);
                    prizes[getDieRoll()].apply(getCurPlayer());
                }
                break;

            case ROLL_DICE_FOR_ROOM: {
                if (rollDice(DiceConfig.TWO_6x6)) {
                    DEnemy.EnemyType[] t = {
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SPIDER,
                    };
                    enemyList.clear();
                    enemyList.add(t[die1-1].newEnemy());
                    enemyList.add(t[die2-1].newEnemy());
                    state = State.CHOOSE_FIGHT_OR_FLEE;
                }
                break;
            }

            case ROLL_DICE_FOR_LOCKED_ROOM: {
                if (rollDice(DiceConfig.THREE_6x6)) {
                    DEnemy.EnemyType[] t = {
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SPIDER,
                            DEnemy.EnemyType.SPIDER,
                    };
                    enemyList.clear();
                    enemyList.add(t[die1-1].newEnemy());
                    enemyList.add(t[die2-1].newEnemy());
                    enemyList.add(t[die3-1].newEnemy());
                    state = State.CHOOSE_FIGHT_OR_FLEE;
                }
                break;
            }

            case CHOOSE_FIGHT_OR_FLEE: {
                DMove [] moves = new DMove[enemyList.size()+1];
                for (int i=0; i<enemyList.size(); i++) {
                    moves[i] = new DMove(MoveType.ATTACK, getCurPlayer().playerNum, i, null);
                }
                moves[enemyList.size()] = new DMove(MoveType.FLEE, getCurPlayer().playerNum, 0, null);
                DMove move = getCurPlayer().chooseMove(moves);
                if (move != null) {
                    switch (move.type) {
                        case ATTACK: {
                            state = State.ROLL_DICE_FOR_ATTACK;
                            curEnemy = move.index;
                            break;
                        }
                        case FLEE: {
                            // the enemies get a shot in
                            for (DEnemy e : enemyList) {
                                doAttack(Utils.rand()%6+1, e, getCurPlayer());
                            }

                            if (!checkPlayerDead(getCurPlayer()))
                                getCurPlayer().cellIndex = getCurPlayer().backCellIndex;

                            enemyList.clear();
                            nextPlayer();
                            break;
                        }
                    }
                }
                break;
            }

            case ROLL_DICE_FOR_ATTACK: {
                if (rollDice(DiceConfig.ONE_6x6)) {
                    {
                        DEnemy e = enemyList.get(curEnemy);
                        doAttack(getDieRoll(), getCurPlayer(), e);
                        if (e.hp <= 0) {
                            onEnemyDead(e);
                            enemyList.remove(curEnemy);
                            curEnemy = -1;
                        }
                    }
                    if (enemyList.size() > 0) {
                        for (DEnemy e : enemyList) {
                            doAttack(Utils.rand()%6+1, e, getCurPlayer());
                        }

                        if (checkPlayerDead(getCurPlayer())) {
                            state = State.ROLL_DICE_TO_ADVANCE;
                            nextPlayer();
                        } else {
                            state = State.CHOOSE_FIGHT_OR_FLEE;
                        }
                    }
                }
                break;
            }
        }
    }

    private void doAttack(int die, DEntity attacker, DEntity e) {
        if (e.hp <= 0)
            return;

        // attacker dexterity determines if a hit
        // attacker strength determines max damage
        // e def is amount reduced from damage
        // e att is amount added to damage
        if (die <= attacker.dex) {
            int damage = Utils.rand()%attacker.str+1+attacker.attack-e.defense;
            if (damage > 0) {
                onDamage(e, damage);
                e.hp -= damage;
            } else {
                onMiss(attacker);
            }
        } else {
            onMiss(attacker);
        }
    }

    private boolean checkPlayerDead(DPlayer p) {
        if (p.hp <= 0) {
            onPlayerDead(p);
            p.cellIndex = board.getStartCellIndex();
            p.attack = p.defense = 0;
            return true;
        }
        return false;
    }

    private void onPrize(Prize p) {
        log.info("Player %s gets %s", getCurPlayer().getName(), p.name());
        for (Listener l : listeners) {
            l.onPrize(p);
        }
    }

    private void onEnemyDead(DEnemy e) {
        log.info("Enemy %s destroyed.", e.getName());
        for (Listener l : listeners) {
            l.onEnemyDead(e);
        }
    }

    private void onPlayerDead(DPlayer p) {
        log.info("Player %s has died. They return to beginning and lose there ATT and DEF", p.getName());
        for (Listener l : listeners) {
            l.onPlayerDead(p);
        }
    }

    private void onMiss(DEntity attacker) {
        log.info("%s Missed!", attacker.getName());
        for (Listener l : listeners) {
            l.onMiss(attacker);
        }
    }

    private void onDamage(DEntity e, int damage) {
        log.info("%s took %d damage", e.getName(), damage);
        for (Listener l : listeners) {
            l.onDamage(e, damage);
        }
    }

    private void nextPlayer() {
        curPlayer = (curPlayer + 1) % players.length;
        state = State.ROLL_DICE_TO_ADVANCE;
    }

    protected boolean rollDice(DiceConfig config) {
        diceConfig = config;
        if (getCurPlayer().rollDice()) {
            switch (diceConfig) {
                case THREE_6x6:
                    die3 = Utils.rand() % 6 + 1;
                case TWO_6x6:
                    die2 = Utils.rand() % 6 + 1;
                case ONE_6x6:
                    die1 = Utils.rand() % 6 + 1;
                    break;
            }
            return true;
        }
        return false;
    }

    public void draw(APGraphics g) {
        board.drawCells(g, 1);
        for (int i=0; i<numPlayers; i++) {
            drawPlayer(g, players[i]);
        }
    }

    protected void drawPlayer(AGraphics g, DPlayer p) {
        DCell cell = board.getCell(p.cellIndex);
        g.pushMatrix();
        g.translate(cell);
        GRectangle rect = board.getCellBoundingRect(p.cellIndex);
        float m = Math.min(rect.w, rect.h);
        rect.scale(m/8, m/8);
        p.draw(g, 1);
        g.popMatrix();
    }


}
