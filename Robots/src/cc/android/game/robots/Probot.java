package cc.android.game.robots;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
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

    public final static int EM  = 0;  // EMPTY
    public final static int DD  = 1;  // DOT
    public final static int SE  = 2;  // Start facing east
    public final static int SS  = 3;  // Start facing south
    public final static int SW  = 4;  // Start facing west
    public final static int SN  = 5;  // Start facing north
    public final static int LH0 = 6;  // horz lazer on by default
    public final static int LV0 = 7;  // vert lazer on by default
    public final static int LB0 = 8;  // lazer0 toggle
    public final static int LH1 = 9;  // horz lazer on by default
    public final static int LV1 = 10; // vert lazer on by default
    public final static int LB1 = 11; // lazer1 toggle
    public final static int LH2 = 12; // horz lazer on by default
    public final static int LV2 = 13; // vert lazer on by default
    public final static int LB2 = 14; // lazer2 toggle
    public final static int LB  = 15; // universal lazer toggle

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
        UTurn,
        LoopStart,
        LoopEnd,
        Jump,
        IfThen,
        IfElse,
        IfEnd
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
            {  0,  0,  0,  0,  0, 0 },
            {  0, DD, DD, DD, DD, 0 },
            { SE, DD, DD, DD, DD, 0 },
            {  0, DD, DD, DD, DD, 0 },
            {  0,  0,  0,  0,  0, 0 }
    };

    // the lazer matrix is same size as the coins. Eash elem is a bit flag of LAZER_N/S/E/W values

    int [][] lazer = {};

    public final static int LAZER_EAST  = 1<<0;
    public final static int LAZER_SOUTH = 1<<1;
    public final static int LAZER_WEST  = 1<<2;
    public final static int LAZER_NORTH = 1<<3;

    int level=0;
    int posx=0, posy=2;
    Direction dir=Direction.Right;
    boolean [] lazerEnabled = new boolean[3];

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
                case UTurn:
                    turn(2);
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
                if (coins[i][ii] == DD) {
                    onDotsRemaining();
                    return running ? 0 : -1;
                }
            }
        }
        return running ? 1 : -1;
    }

    public final void init(int [][] matrix) {
        init(matrix, true, true, false);
    }

    public final void init(int [][] matrix, boolean ... lazerEnabled) {
        coins = matrix;
        Utils.setElems(this.lazerEnabled, lazerEnabled);
        initLazers();
        program.clear();
        for (int i=0; i<coins.length; i++) {
            for (int ii=0; ii<coins[i].length; ii++) {
                switch (coins[i][ii]) {
                    case EM:
                        break;
                    case DD:
                        break;
                    case SE:
                    case SS:
                    case SW:
                    case SN:
                        posx = ii;
                        posy = i;
                        dir = Direction.values()[coins[i][ii]-2];
                        coins[i][ii] = 0;
                        break;
                    case LH0:
                        break;
                    case LV0:
                        break;
                    case LB0:
                        break;
                    case LH1:
                        break;
                    case LV1:
                        break;
                    case LB1:
                        break;
                    case LH2:
                        break;
                    case LV2:
                        break;
                    case LB2:
                        break;
                    case LB:
                        break;
                }
            }
        }
    }

    // return false if failed
    private boolean advance(int amt) {
        int nx = posx + dir.dx*amt;
        int ny = posy + dir.dy*amt;

        if (nx < 0 || ny < 0 || ny >= coins.length || nx >= coins[ny].length) {
            onAdvanceFailed();
            return false;
        } else if (!canMoveToPos(ny, nx)) {
            onAdvanceFailed();
            return false;
        } else if (lazer[ny][nx] != 0) {
            onLazered();
            return false;
        } else {
            if (amt == 1) {
                onAdvanced();
            } else {
                onJumped();
            }
            posx = nx;
            posy = ny;
            switch (coins[posy][posx]) {
                case DD:
                    coins[posy][posx] = EM;
                    break;
                case LB0:
                    toggleLazers(0);
                    break;
                case LB1:
                    toggleLazers(1);
                    break;
                case LB2:
                    toggleLazers(2);
                    break;
                case LB:
                    toggleLazers(0, 1, 2);
                    break;
            }
            //coins[posy][posx] = 0;
        }
        return true;
    }

    private void initHorzLazer(int y, int x) {
        for (int i=x-1; i>=0; i--) {
            if (lazer[y][i] != 0) {
                lazer[y][i] |= LAZER_EAST;
                break; // cannot lazer past another lazer
            }
            lazer[y][i] |= LAZER_WEST | LAZER_EAST;
        }

        for (int i=x+1; i<lazer[0].length; i++) {
            if (lazer[y][i] != 0) {
                lazer[y][i] |= LAZER_WEST;
                break; // cannot lazer past another lazer
            }
            lazer[y][i] |= LAZER_WEST | LAZER_EAST;
        }
    }

    private void initVertLazer(int y, int x) {
        for (int i=y-1; i>=0; i--) {
            if (lazer[i][x] != 0) {
                lazer[i][x] |= LAZER_NORTH;
                break; // cannot lazer past another lazer
            }
            lazer[i][x] |= LAZER_NORTH | LAZER_SOUTH;
        }

        for (int i=y+1; i<lazer.length; i++) {
            if (lazer[i][x] != 0) {
                lazer[i][x] |= LAZER_SOUTH;
                break; // cannot lazer past another lazer
            }
            lazer[i][x] |= LAZER_NORTH | LAZER_SOUTH;
        }
    }

    private void initLazers() {
        lazer = new int[coins.length][coins[0].length];
        for (int i=0; i<lazer.length; i++) {
            for (int ii=0; ii<lazer[i].length; ii++) {
                for (int iii=0; iii<lazerOrdering.length; iii++) {
                    switch (lazerOrdering[iii]) {
                        case 0:
                            switch (coins[i][ii]) {
                                case LH0:
                                    if (lazerEnabled[0]) {
                                        initHorzLazer(i, ii);
                                    }
                                    break;
                                case LV0:
                                    if (lazerEnabled[0]) {
                                        initVertLazer(i, ii);
                                    }
                                    break;
                            }
                            break;
                        case 1:
                            switch (coins[i][ii]) {
                                case LH1:
                                    if (lazerEnabled[1]) {
                                        initHorzLazer(i, ii);
                                    }
                                    break;
                                case LV1:
                                    if (lazerEnabled[1]) {
                                        initVertLazer(i, ii);
                                    }
                                    break;
                            }
                            break;
                        case 2:
                            switch (coins[i][ii]) {
                                case LH2:
                                    if (lazerEnabled[2]) {
                                        initHorzLazer(i, ii);
                                    }
                                    break;
                                case LV2:
                                    if (lazerEnabled[2]) {
                                        initVertLazer(i, ii);
                                    }
                                    break;
                            }


                    }
                }
            }
        }
    }

    // ordering the lazer initialilzation makes possible for any lazer to block any other depending on whose state has changed
    int [] lazerOrdering = { 0, 1, 2 };

    private void toggleLazers(int ... nums) {
        for (int n : nums) {
            lazerEnabled[n] = !lazerEnabled[n];
        }
        initLazers();
    }

    private void toggleLazer(int num) {
        lazerEnabled[num] = !lazerEnabled[num];
        // adjust the ordering so that 'num' is made last. This makes it possible for any lazer to block another

        for (int i=0; i<lazerOrdering.length-1; i++) {
            if (lazerOrdering[i] == num) {
                lazerOrdering[i] = lazerOrdering[i+1];
                lazerOrdering[i+1] = num;
            }
        }

        initLazers();
    }

    private boolean canMoveToPos(int y, int x) {
        switch (coins[y][x]) {
            case DD:
            case LB0:
            case LB1:
            case LB2:
            case LB:
                return true;
        }
        return false;
    }

    private void moveToPos(int y, int x) {
        switch (coins[y][x]) {
            case EM:
                break;
            case DD:
                break;
            case SE:
                break;
            case SS:
                break;
            case SW:
                break;
            case SN:
                break;
            case LH0:
                break;
            case LV0:
                break;
            case LB0:
                break;
            case LH1:
                break;
            case LV1:
                break;
            case LB1:
                break;
            case LH2:
                break;
            case LV2:
                break;
            case LB2:
                break;
            case LB:
                break;
        }
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
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 1:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, EM, EM, EM },
                        { SE, DD, EM, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 2:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, EM, EM, EM, EM },
                        { SE, DD, DD, EM, EM, EM, EM },
                        { EM, DD, DD, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 3:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, DD, EM, EM },
                        { EM, EM, EM, EM, DD, EM, EM },
                        { EM, DD, DD, DD, DD, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 4:
                init(new int[][] {
                        { EM, SS,LV0, EM, EM, EM, EM },
                        { EM, DD, EM, DD, DD, DD,LH1 },
                        { EM,LB0, DD, LB, EM, LB, EM },
                        { EM, EM, EM, DD, DD, DD,LH0 },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 5:
                init(new int[][] {
                        {  EM, EM,LV1, EM, EM, EM, EM },
                        {  LH0,LB1,DD ,DD, DD, EM, EM },
                        {  SE, LB0,DD ,LB2,DD, EM, EM },
                        {  LH0,LB2,DD ,DD, DD, EM, EM },
                        {  EM, EM,LV2, EM, EM, EM, EM },
                });
                break;
            case 6:
                init(new int[][]{
                        {EM, EM,  LV2, EM,  LV2, EM,  EM},
                        {EM, LB0, LB1, LB2, LB1, LB0, EM},
                        {LH0,LB1, LB2, LB0, LB2, LB1, EM},
                        {LH1,LB2, LB0, DD,  LB0, LB2, EM},
                        {LH0,LB1, LB2, LB0, LB2, LB1, EM},
                        {SE, LB0, LB1, LB2, LB1, LB0, EM},
                        {EM, EM,  EM,  EM,  EM,  EM,  EM}

                }, false, false, false);

                /*init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, EM },
                        { LH0,DD, DD, DD, DD, DD, EM },
                        { EM, DD, DD, DD, DD, DD, EM },
                        { EM, SN, LV0,EM, EM, EM, EM },
                });*/
                break;
            case 7:
                /*
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, DD, DD, EM },
                        { EM, EM, EM, DD, DD, DD, EM },
                        { EM, EM, DD, DD, DD, DD, EM },
                        { EM, DD, DD, DD, DD, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });*/
                init(new int[][] {
                        { EM, EM, LV0,EM, EM, EM, LV0,EM, EM },
                        { EM, EM, EM, EM, DD, EM, EM, EM, EM },
                        { EM, EM, EM, EM, LB2,EM, EM, EM, LH2 },
                        { LH1,EM, EM, EM, LB0,EM, EM, EM, EM },
                        { EM, DD, LB1,LB0,SE, LB, LB2,DD, EM },
                        { EM, EM, EM, EM, LB2,EM, EM, EM, LH1 },
                        { EM, EM, EM, EM, LB1,EM, EM, EM, EM },
                        { EM, EM, EM, EM, DD, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, LV2,EM, EM }
                }, false, false, false);
                break;
            case 8:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, EM, EM, EM, EM, DD, EM },
                        { EM, DD, DD, DD, DD, EM, DD, EM },
                        { EM, EM, EM, EM, EM, EM, DD, EM },
                        { SE, DD, DD, DD, DD, DD, DD, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 9:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, EM, EM, EM, EM, DD, EM },
                        { EM, DD, EM, DD, DD, DD, DD, EM },
                        { EM, DD, EM, DD, EM, EM, DD, EM },
                        { SE, DD, EM, DD, DD, DD, DD, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 10:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, EM, DD, DD, EM, DD, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, DD, DD, EM, EM, DD, EM },
                        { EM, DD, EM, DD, DD, DD, DD, EM },
                        { EM, SN, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 11:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, EM, DD, DD, EM, DD, EM },
                        { EM, DD, EM, EM, EM, DD, DD, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, EM, EM, DD, DD, EM, DD, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, DD, DD, EM, EM, EM, EM },
                        { EM, EM, EM, SE, EM, EM, EM, EM },
                });
                break;
            case 12:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 13:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 14:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 15:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 16:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 17:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 18:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { SE, DD, DD, DD, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 19:
                init(new int[][] {
                        { EM, SS, EM, EM, EM, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, EM, EM, DD, DD, EM, EM, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, DD, EM, DD, DD, EM, DD, EM },
                        { EM, DD, DD, DD, DD, DD, DD, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            case 20:
                init(new int[][] {
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { SE, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                        { EM, EM, EM, EM, EM, EM, EM, EM, EM },
                });
                break;
            default:
                setLevel(0);
        }
    }

    public boolean isCommandTypeAvailable(CommandType t) {
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

    protected void onAdvanceFailed() {}

    protected void onAdvanced() {}

    protected void onJumped() {}

    protected void onTurned(int dir) {}

    protected void onSuccess() {}

    protected void onLazered() {}

    protected void onDotsRemaining() {}
}
