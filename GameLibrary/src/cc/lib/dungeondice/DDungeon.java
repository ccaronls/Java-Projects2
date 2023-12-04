package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.reflector.Reflector;

public class DDungeon extends Reflector<DDungeon> {

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
        DMG_PLUS1,
        DMG_PLUS2,
        BMG_PLUS3,
        ATTACK_PLUS1,
        ATTACK_PLUS2,
        ATTACK_PLUS3,
        MAGIC_PLUS2,
        HP_PLUS_1,
        HP_PLUS_2,
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
                diceConfig = DiceConfig.ONE_6x6;
                if (getCurPlayer().rollDice()) {
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
                diceConfig = DiceConfig.ONE_6x6;
                if (getCurPlayer().rollDice()) {
                    DEnemy enemy = enemyList.get(0);
                    if (getDieRoll() <= enemy.type.chanceToFight) {
                        state = State.CHOOSE_FIGHT_OR_FLEE;
                    } else {
                        nextPlayer();
                    }
                }
                break;

            case ROLL_DICE_FOR_BLUE_PRIZE:
                diceConfig = DiceConfig.ONE_6x6;
                if (getCurPlayer().rollDice()) {

                }
                break;

            case ROLL_DICE_FOR_RED_PRIZE:
                diceConfig = DiceConfig.ONE_6x6;
                if (getCurPlayer().rollDice()) {

                }
                break;

            case ROLL_DICE_FOR_ROOM: {
                diceConfig = DiceConfig.TWO_6x6;
                if (getCurPlayer().rollDice()) {
                    DEnemy.EnemyType[] t = {
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SPIDER,
                    };
                    enemyList.add(t[die1-1].newEnemy());
                    enemyList.add(t[die2-1].newEnemy());
                }
                break;
            }

            case ROLL_DICE_FOR_LOCKED_ROOM: {
                diceConfig = DiceConfig.THREE_6x6;
                if (getCurPlayer().rollDice()) {
                    DEnemy.EnemyType[] t = {
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.RAT,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SNAKE,
                            DEnemy.EnemyType.SPIDER,
                            DEnemy.EnemyType.SPIDER,
                    };
                    enemyList.add(t[die1-1].newEnemy());
                    enemyList.add(t[die2-1].newEnemy());
                    enemyList.add(t[die3-1].newEnemy());
                }
                break;
            }

            case CHOOSE_FIGHT_OR_FLEE: {
                DMove [] moves = {
                        new DMove(MoveType.ATTACK, 0, 0, null),
                        new DMove(MoveType.FLEE, 0, 0, null)
                };
                DMove move = getCurPlayer().chooseMove(moves);
                if (move != null) {
                    switch (move.type) {
                        case ATTACK: {
                            state = State.ROLL_DICE_FOR_ATTACK;
                            break;
                        }
                        case FLEE:
                            // the enemies get a shot in
                            for (DEnemy e : enemyList) {
                                doAttack(e, getCurPlayer());
                                doAttack(e, getCurPlayer());
                            }

                            if (!checkPlayerDead(getCurPlayer()))
                                getCurPlayer().cellIndex = getCurPlayer().backCellIndex;

                            nextPlayer();
                            break;
                    }
                }
                break;
            }
        }
    }

    private void doAttack(DEntity attacker, DEntity e) {
        // attacker dexterity determines if a hit
        // attacker strength determines max damage
        // e def is amount reduced from damage
        // e att is amount added to damage
        if ((Utils.rand()%6+1) <= attacker.dex) {
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

    protected void onPlayerDead(DPlayer p) {
        log.info("Player %s has died. They return to beginning and lose there ATT and DEF");
    }

    protected void onMiss(DEntity attacker) {
        log.info("%d Missed!", attacker.getName());
    }

    protected void onDamage(DEntity e, int damage) {
        log.info("%s took %d damage", e.getName(), damage);
    }

    private void nextPlayer() {
        curPlayer = (curPlayer + 1) % players.length;
        state = State.ROLL_DICE_TO_ADVANCE;
    }

    protected void rollDice() {
        switch (diceConfig) {
            case THREE_6x6:
                die3 = Utils.rand()%6+1;
            case TWO_6x6:
                die2 = Utils.rand()%6+1;
            case ONE_6x6:
                die1 = Utils.rand()%6+1;
                break;
        }
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
        g.setColor(p.getColor());
        GRectangle rect = board.getCellBoundingRect(p.cellIndex);
        float m = Math.min(rect.w, rect.h);
        rect.scale(m/8, m/8);
        g.setLineWidth(2);
        g.drawCircle(0, -1.5f, 0.5f);
        g.begin();
        g.vertexArray(new float [][] {
                { 0, -1 },
                { 0, .5f },
                { -1, -.5f },
                {  1, -.5f },
                { 0, .5f },
                { -1, 2 },
                { 0, .5f },
                { 1, 2 }
        });
        g.drawLines();
        g.popMatrix();
    }


}
