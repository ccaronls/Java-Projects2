package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class DDungeon extends Reflector<DDungeon> {

    static {
        addAllFields(DDungeon.class);
    }

    enum State {
        INIT,
        ADVANCE
    }

    enum Prize {
        KEY,
        DMG_PLUS1,
        DMG_PLUS2,
        BMG_PLUS3,
        ATTACK_PLUS1,
        ATTACK_PLUS2,
        ATTACK_PLUS3,
        MAGIC_PLUS2
    }

    enum DiceConfig {
        ONE_6x6,
        TWO_6x6,
    }

    DBoard board;
    DPlayer [] players;
    int numPlayers;
    int curPlayer;
    int curEnemy;
    State state = null;
    int die1, die2;
    DiceConfig diceConfig = DiceConfig.ONE_6x6;
    final List<DEnemy> enemyList = new ArrayList<>();

    public int getDieRoll() {
        switch (diceConfig) {
            case ONE_6x6:
                return die1;
            case TWO_6x6:
                return die1 + die2;
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
        die1 = die2 = 0;
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
                /*
            case ROLL_TO_MOVE: {
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                break;
            }
            case ADVANCE: {
                DMove[] moves = board.findMoves(getCurPlayer(), getDieRoll());
                DMove move = getCurPlayer().chooseMove(moves);
                processMove(move);
                break;
            }
            case LAND_ON_RED:
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                break;
            case LAND_ON_BLUE:
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                break;
            case LAND_ON_GREEN:
                enemyList.clear();
                enemyList.add(DEnemy.EnemyType.SNAKE.newEnemy());
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                break;
            case LAND_ON_BROWN:
                enemyList.clear();
                enemyList.add(DEnemy.EnemyType.RAT.newEnemy());
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                break;
            case LAND_ON_BLACK:
                enemyList.clear();
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                if (getDieRoll() <= DEnemy.EnemyType.SPIDER.chanceToFight) {
                    enemyList.add(DEnemy.EnemyType.SPIDER.newEnemy());
                    state = State.ROLL_PLAYER_HITS;
                } else {
                    nextPlayer();
                }
                break;
            case LAND_ON_ROOM:
                diceConfig = DiceConfig.TWO_6x6;
                rollDice();
                break;
            case LAND_ON_KEY_ROOM:
                break;
            case ROLL_FOR_ENEMY_GP:
                break;
            case ROLL_PLAYER_HITS:
                diceConfig = DiceConfig.ONE_6x6;
                rollDice();
                if (getDieRoll() <= getCurPlayer().dex) {
                    state = State.ROLL_PLAYER_DAMAGE;
                } else {
                    state = State.ROLL_ENEMY_HITS;
                }
                break;
            case ROLL_PLAYER_DAMAGE:
                break;
            case ROLL_ENEMY_HITS: {
                curEnemy = Utils.rand() % enemyList.size();

                break;
            }
            case ROLL_ENEMY_DAMAGE:
                break;*/
        }
    }

    private void nextPlayer() {
        curPlayer = (curPlayer + 1) % players.length;
        //state = State.ROLL_TO_MOVE;
    }

    private void processMove(DMove move) {

    }

    protected void rollDice() {

    }
}
