package cc.android.game.robots;

import java.util.ArrayList;
import java.util.List;

import cc.lib.utils.Reflector;

/**
 * Created by chriscaron on 12/7/17.
 *
 * This class contains the bizness logic for a game to teach programming.
 * The idea is to program a robot to collect coins.
 *
 * The player sets up commands to move the robot like: advance, turn right/left ect.
 *
 * The robot must advance to a coin or the level fails
 *
 */
public class Probot extends Reflector<Probot> {

    static {
        addAllFields(Probot.class);
    }

    public static class Command {
        CommandType type;
        int count;
        List<Command> list;
    }

    enum CommandType {
        Advance,
        TurnRight,
        TurnLeft,
        Loop
    }

    enum Direction {
        Right(1, 0),
        Down(0, 1),
        Left(-1, 0),
        Up(0, -1),
        ;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        final int dx, dy;
    }

    List<Command> program = new ArrayList<>();

    int [][] coins = {
            { 0, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 0 }
    };
    int posx=0, posy=2;
    Direction dir=Direction.Right;

    @Omit
    Probot copy;

    /**
     * Called in separate thread. callbacks made to events should be handled to show on ui
     */
    public final void runProgram() {
        copy = new Probot();
        copy.copyFrom(this);
        if (runProgramList(program, 0)) {
            onSuccess();
        } else {
            onFailed();
            reset();
        }
    }

    private boolean runProgramList(List<Command> program, final int startLine) {
        int line = startLine;
        for (Command c : program) {
            onCommand(line);
            switch (c.type) {
                case Loop: {
                    for (int i=0; i<c.count; i++) {
                        if (!runProgramList(c.list, line))
                            return false;
                    }
                    break;
                }
                case Advance:
                    if (!advance()) {
                        return false;
                    }
                    break;
                case TurnRight:
                    turn(1);
                    break;
                case TurnLeft:
                    turn(-1);
                    break;
            }
            line++;
        }
        for (int i=0; i<coins.length; i++) {
            for (int ii=0; ii<coins[i].length; ii++) {
                if (coins[i][ii] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public final void init(int [][] matrix) {
        coins = matrix;
        program.clear();
        for (int i=0; i<coins.length; i++) {
            for (int ii=0; ii<coins[i].length; ii++) {
                if (coins[i][ii] > 1) {
                    posx = ii;
                    posy = i;
                    dir = Direction.values()[coins[i][ii]-2];
                    coins[i][ii] = 0;
                    return;
                }
            }
        }
        throw new AssertionError();
    }

    // return false if failed
    private boolean advance() {
        int nx = posx + dir.dx;
        int ny = posy + dir.dy;

        if (nx < 0 || ny < 0 || ny >= coins.length || nx >= coins[ny].length) {
            return false;
        } else if (coins[ny][nx] == 0) {
            return false;
        } else {
            onAdvanced();
            posx = nx;
            posy = ny;
            coins[posy][posx] = 0;
        }
        return true;
    }

    private void turn(int d) {
        int nd = dir.ordinal() + d;
        nd += Direction.values().length;
        nd %= Direction.values().length;
        dir = Direction.values()[nd];
        onTurned();
    }

    private void reset() {
        if (copy != null) {
            copyFrom(copy);
        }
    }

    protected void onCommand(int line) {}

    protected void onFailed() {}

    protected void onAdvanced() {}

    protected void onTurned() {}

    protected void onSuccess() {}

}