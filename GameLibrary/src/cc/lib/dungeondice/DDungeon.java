package cc.lib.dungeondice;

import cc.lib.game.IGame;
import cc.lib.utils.Reflector;

public class DDungeon extends Reflector<DDungeon> implements IGame<DMove> {

    DBoard board;
    DPlayer [] players;
    int numPlayers;
    int curPlayer;
    State state;

    enum State {
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

    enum RoomPrize {
        KEY,
        DMG_PLUS1,
        DMG_PLUS2,
        ATTACK_PLUS1,
        ATTACK_PLUS2,
        MAGIC_PLUS2
    }

    enum EnemyType {
        RAT,
        SNAKE,
        SPIDER,
    }

    public void newGame(int numPlayers) {

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
    public int getTurn() {
        return curPlayer;
    }

    public void run() {
        switch (state) {

            case ADVANCE:
                DMove [] moves = board.findMoves(players[curPlayer]);
                players[curPlayer].chooseMove(moves);
                break;
            case LAND_ON_RED:
                break;
            case LAND_ON_BLUE:
                break;
            case LAND_ON_GREEN:
                break;
            case LAND_ON_BROWN:
                break;
            case LAND_ON_BLACK:
                break;
            case LAND_ON_ROOM:
                break;
            case LAND_ON_KEY_ROOM:
                break;
            case ROLL_TO_FIGHT_ENEMY:
                break;
            case ROLL_FOR_ENEMY_GP:
                break;
            case ROLL_PLAYER_HITS:
                break;
            case ROLL_PLAYER_DAMAGE:
                break;
            case ROLL_ENEMY_HITS:
                break;
            case ROLL_ENEMY_DAMAGE:
                break;
        }
    }
}
