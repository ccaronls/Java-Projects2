package cc.lib.dungeondice;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.IGame;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class DDungeon extends Reflector<DDungeon> implements IGame<DMove> {

    static {
        addAllFields(DDungeon.class);
    }

    enum State {
        INIT,
        ROLL_TO_MOVE,
        ADVANCE,
        LAND_ON_RED,
        LAND_ON_BLUE,
        LAND_ON_GREEN,
        LAND_ON_BROWN,
        LAND_ON_BLACK,
        LAND_ON_ROOM,
        LAND_ON_KEY_ROOM,
        ROLL_TO_FIGHT_ENEMY,
        ROLL_FOR_ENEMY_GP,
        ROLL_PLAYER_HITS,
        ROLL_PLAYER_DAMAGE,
        ROLL_ENEMY_HITS,
        ROLL_ENEMY_DAMAGE,
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
    State state = State.INIT;
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

    }

    public final void newGame(int numPlayers) {

    }

    @Override
    public void executeMove(DMove move) {

    }

    @Override
    public DMove undo() {
        return null;
    }

    @Override
    public Iterable<DMove> getMoves() {
        return null;
    }

    @Override
    public final int getTurn() {
        return curPlayer;
    }

    public final DPlayer getCurPlayer() {
        return players[curPlayer];
    }

    public void run() {
        switch (state) {
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
                break;
        }
    }

    private void nextPlayer() {
        curPlayer = (curPlayer + 1) % players.length;
        state = State.ROLL_TO_MOVE;
    }

    private void processMove(DMove move) {

    }

    protected void rollDice() {

    }
}
