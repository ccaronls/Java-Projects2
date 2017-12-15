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
        final CommandType type;
        int count;
        int nesting=0;

        public Command(CommandType type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    enum CommandType {
        Advance,
        TurnRight,
        TurnLeft,
        LoopStart,
        LoopEnd,
        Jump
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

    @Omit
    private List<Command> program = new ArrayList<>();

    int [][] coins = {
            { 0, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 0 }
    };

    int level=0;
    int posx=0, posy=2;
    Direction dir=Direction.Right;

    @Omit
    private Probot copy;

    @Omit
    private boolean running = false;

    /**
     * Called in separate thread. callbacks made to events should be handled to show on ui
     */
    public final void runProgram() {
        running = true;
        copy = new Probot();
        copy.copyFrom(this);
        int run = runProgram(new int [] { 0 });
        switch (run) {
            case 1:
                onSuccess();
                break;
            case 0:
                onFailed();
            case -1:
                reset();
        }
        running = false;
    }

    public final boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public void add(Command cmd) {
        add(size(), cmd);
    }

    private void updateNesting() {
        int nesting = 0;
        for (Command c : program) {
            c.nesting = nesting;
            switch (c.type) {
                case LoopStart:
                    nesting++;
                    break;
                case LoopEnd:
                    nesting--;
                    break;
            }
        }
    }

    public void add(int index, Command cmd) {
        program.add(index, cmd);
        updateNesting();
    }

    public Command remove(int index) {
        Command cmd = program.remove(index);
        updateNesting();
        return cmd;
    }

    public int size() {
        return program.size();
    }

    public Command get(int index) {
        return program.get(index);
    }

    private int runProgram(int [] linePtr) {
        while (linePtr[0] < program.size()) {
            if (!running)
                return -1;
            onCommand(linePtr[0]);
            Command c = program.get(linePtr[0]);
            switch (c.type) {
                case LoopStart: {
                    int lineStart = ++linePtr[0];
                    for (int i=0; i<c.count; i++) {
                        if (!running)
                            return -1;
                        linePtr[0] = lineStart;
                        int r;
                        if ((r = runProgram(linePtr)) != 1)
                            return r;
                    }
                    break;
                }
                case LoopEnd: {
                    return 1;
                }
                case Advance:
                    if (!advance(1)) {
                        return running ? 0 : -1;
                    }
                    break;
                case TurnRight:
                    turn(1);
                    break;
                case TurnLeft:
                    turn(-1);
                    break;
                case Jump:
                    if (!advance(2)) {
                        return running ? 0 : -1;
                    }
                    break;
            }
            linePtr[0]++;
        }
        for (int i=0; running && i<coins.length; i++) {
            for (int ii=0; running && ii<coins[i].length; ii++) {
                if (coins[i][ii] != 0) {
                    return running ? 0 : -1;
                }
            }
        }
        return running ? 1 : -1;
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
    private boolean advance(int amt) {
        int nx = posx + dir.dx*amt;
        int ny = posy + dir.dy*amt;

        if (nx < 0 || ny < 0 || ny >= coins.length || nx >= coins[ny].length) {
            return false;
        } else if (coins[ny][nx] == 0) {
            return false;
        } else {
            if (amt == 1)
                onAdvanced();
            else
                onJumped();
            posx = nx;
            posy = ny;
            coins[posy][posx] = 0;
        }
        return true;
    }

    private void turn(int d) {
        onTurned(d);
        int nd = dir.ordinal() + d;
        nd += Direction.values().length;
        nd %= Direction.values().length;
        dir = Direction.values()[nd];
    }

    private void reset() {
        if (copy != null) {
            copyFrom(copy);
        }
    }

    void nextLevel() {
        setLevel(++level);
    }

    void setLevel(int level) {
        this.level = level;
        program.clear();
        switch (level) {
            case 0:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 1:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 1, 1, 1, 0, 0, 0},
                        {2, 1, 0, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 2:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 1, 1, 0, 0, 0, 0},
                        {2, 1, 1, 0, 0, 0, 0},
                        {0, 1, 1, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 3:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 1, 0, 0},
                        {0, 0, 0, 0, 1, 0, 0},
                        {0, 1, 1, 1, 1, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 4:
                init(new int[][]{
                        {0, 3, 0, 0, 0, 0, 0},
                        {0, 1, 0, 1, 1, 1, 0},
                        {0, 1, 1, 1, 0, 1, 0},
                        {0, 0, 0, 1, 1, 1, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 5:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 1, 1, 1, 1, 0, 0},
                        {2, 1, 1, 1, 1, 0, 0},
                        {0, 1, 1, 1, 1, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 6:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 1, 1, 1, 1, 1, 0},
                        {0, 5, 0, 0, 0, 0, 0},
                });
                break;
            case 7:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 8:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 9:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 10:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 11:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 12:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 13:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 14:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 15:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 16:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 17:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            case 18:
                init(new int[][]{
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {2, 1, 1, 1, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                        {0, 0, 0, 0, 0, 0, 0},
                });
                break;
            default:
                setLevel(0);
        }
    }

    public boolean isCommandTypeAvailable(Probot.CommandType t) {
        switch (t) {
            case Jump:
                return level > 10;
            case LoopStart:
            case LoopEnd:
                return level > 2;
            case TurnRight:
            case TurnLeft:
                return level > 0;
            case Advance:
            default:
                return true;
        }
    }

    // Overrides to handle important events.

    protected void onCommand(int line) {}

    protected void onFailed() {}

    protected void onAdvanced() {}

    protected void onJumped() {}

    protected void onTurned(int dir) {}

    protected void onSuccess() {}

}
