package cc.lib.probot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
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
public class Probot extends Reflector<Probot> implements Comparator<Integer> {

    static {
        addAllFields(Probot.class);
    }

    public final static GColor [] manColors = {
            GColor.YELLOW,
            GColor.GREEN,
            GColor.ORANGE,
            GColor.PINK
    };

    @Omit
    private List<Command> program = new ArrayList<>();

    public Level level = new Level();
    int levelNum = 0;

    // the lazer matrix is same size as the coins. Each elem is a bit flag of LAZER_N/S/E/W values

    public int [][] lazer = {};

    public final static int LAZER_EAST  = 1<<0;
    public final static int LAZER_SOUTH = 1<<1;
    public final static int LAZER_WEST  = 1<<2;
    public final static int LAZER_NORTH = 1<<3;

    public final List<Guy> guys = new ArrayList<>();

    @Omit
    private Probot copy;

    @Omit
    private boolean running = false;

    /**
     * Called in separate thread. callbacks made to events should be handled to show on ui
     */
    public final void runProgram() {
        Utils.assertFalse(running);
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

    /**
     *
     * @param types
     * @return
     */
    public int getCommandCount(CommandType ... types) {
        int count = 0;
        for (Command c : program) {
            if (Utils.linearSearch(types, c.type) >= 0) {
                count++;
            }
        }
        return count;
    }

    /**
     *
     * @return
     */
    public final boolean isRunning() {
        return running;
    }

    /**
     *
     */
    public void stop() {
        running = false;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     *
     * @param cmd
     */
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

    /**
     *
     * @param index
     * @param cmd
     */
    public void add(int index, Command cmd) {
        program.add(index, cmd);
        updateNesting();
    }

    /**
     *
     * @param index
     * @return
     */
    public Command remove(int index) {
        Command cmd = program.remove(index);
        updateNesting();
        return cmd;
    }

    public void clear() {
        program.clear();
        updateNesting();
    }

    /**
     *
     * @return
     */
    public int getLevelNum() {
        return levelNum;
    }

    /**
     *
     * @return
     */
    public int size() {
        return program.size();
    }

    /**
     *
     * @param index
     * @return
     */
    public Command get(int index) {
        return program.get(index);
    }

    /**
     *
     * @return
     */
    public final int getMaxGuys() {
        return manColors.length;
    }

    /**
     *
     * @return
     */
    public final int getNumGuys() {
        return guys.size();
    }

    /**
     *
     * @return
     */
    public final Iterable<Guy> getGuys() {
        return guys;
    }

    private int runProgram(int [] linePtr) {
        while (linePtr[0] < program.size()) {
            if (!running)
                return -1;
            final Command c = program.get(linePtr[0]);
            onCommand(linePtr[0]);
            for (Guy guy : guys) {
                switch (c.type) {

                    case Advance:
                        advance(guy, 1, true);
                        break;
                    case TurnRight:
                        onTurned(guy, 1);
                        break;
                    case TurnLeft:
                        onTurned(guy, -1);
                        break;
                    case UTurn:
                        onTurned(guy, 2);
                        break;
                    case LoopStart:
                        break;
                    case LoopEnd:
                        break;
                    case Jump:
                        advance(guy, 2, true);
                        break;
                    case IfThen:
                        break;
                    case IfElse:
                        break;
                    case IfEnd:
                        break;
                }
            }
            onCommand(linePtr[0]);
            for (Guy guy : guys) {
                switch (c.type) {
                    case LoopStart: {
                        int lineStart = ++linePtr[0];
                        for (int i = 0; i < c.count; i++) {
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
                        if (!advance(guy, 1, false)) {
                            return running ? 0 : -1;
                        }
                        break;
                    case TurnRight:
                        turn(guy, 1);
                        break;
                    case TurnLeft:
                        turn(guy, -1);
                        break;
                    case UTurn:
                        turn(guy, 2);
                        break;
                    case Jump:
                        if (!advance(guy, 2, false)) {
                            return running ? 0 : -1;
                        }
                        break;
                }
            }
            linePtr[0]++;
        }
        for (int i=0; running && i<level.coins.length; i++) {
            for (int ii=0; running && ii<level.coins[i].length; ii++) {
                if (level.coins[i][ii] == Type.DD) {
                    onDotsLeftUneaten();
                    return running ? 0 : -1;
                }
            }
        }
        return running ? 1 : -1;
    }

    private final void init(Level level) {
        this.level = level;
        initLazers();
        program.clear();
        guys.clear();
    }

    public void start() {
        for (int i=0; i<level.coins.length; i++) {
            for (int ii=0; ii<level.coins[i].length; ii++) {
                switch (level.coins[i][ii]) {
                    case EM:
                        break;
                    case DD:
                        break;
                    case SE:
                    case SS:
                    case SW:
                    case SN:
                        guys.add(new Guy(ii, i, level.coins[i][ii].direction, manColors[guys.size()]));
                        level.coins[i][ii] = Type.EM;
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
    private boolean advance(Guy guy, int amt, boolean useCB) {
        int nx = guy.posx + guy.dir.dx*amt;
        int ny = guy.posy + guy.dir.dy*amt;

        if (nx < 0 || ny < 0 || ny >= level.coins.length || nx >= level.coins[ny].length) {
            if (useCB)
                onAdvanceFailed(guy);
            return false;
        } else if (!canMoveToPos(ny, nx)) {
            if (useCB)
                onAdvanceFailed(guy);
            return false;
        } else if (lazer[ny][nx] != 0) {
            if (useCB)
                onLazered(guy,false); // walking into a lazer
            return false;
        } else {
            if (useCB) {
                if (amt == 1) {
                    onAdvanced(guy);
                } else {
                    onJumped(guy);
                }
                int [][] lazersCopy = Reflector.deepCopy(lazer);
                testLazers(ny, nx);
                if (lazer[ny][nx] != 0) {
                    onLazered(guy, true); // lazer activated on guy
                }
                lazer = Reflector.deepCopy(lazersCopy);
            } else {
                guy.posx = nx;
                guy.posy = ny;
                switch (level.coins[guy.posy][guy.posx]) {
                    case DD:
                        level.coins[guy.posy][guy.posx] = Type.EM;
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
                if (lazer[ny][nx] != 0) {
                    onLazered(guy, true); // lazer activated on guy
                    return false;
                }
            }
        }
        return true;
    }

    private void testLazers(int row, int col) {
        switch (level.coins[row][col]) {
            case DD:
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
                lazer[i][x] |= LAZER_SOUTH;
                break; // cannot lazer past another lazer
            }
            lazer[i][x] |= LAZER_NORTH | LAZER_SOUTH;
        }

        for (int i=y+1; i<lazer.length; i++) {
            if (lazer[i][x] != 0) {
                lazer[i][x] |= LAZER_NORTH;
                break; // cannot lazer past another lazer
            }
            lazer[i][x] |= LAZER_NORTH | LAZER_SOUTH;
        }
    }

    /*
    INTERNAL USE ONLY
     */
    @Override
    public final int compare(Integer i1, Integer i2) {
        boolean o1 = level.lazers[i1];
        boolean o2 = level.lazers[i2];
        if (o1 && !o2)
            return -1;
        if (!o1 && o2)
            return 1;
        return 0;
    }

    private void initLazers() {
        Arrays.sort(lazerOrdering, this);
        lazer = new int[level.coins.length][level.coins[0].length];
        for (int laz : lazerOrdering) {
            if (!level.lazers[laz])
                continue;
            for (int i=0; i<level.coins.length; i++) {
                for (int ii=0; ii<level.coins[0].length; ii++) {
                    switch (level.coins[i][ii]) {
                        case LH0:
                            if (laz == 0) {
                                initHorzLazer(i, ii);
                            }
                            break;
                        case LV0:
                            if (laz == 0) {
                                initVertLazer(i, ii);
                            }
                            break;
                        case LH1:
                            if (laz == 1) {
                                initHorzLazer(i, ii);
                            }
                            break;
                        case LV1:
                            if (laz == 1) {
                                initVertLazer(i, ii);
                            }
                            break;
                        case LH2:
                            if (laz == 2) {
                                initHorzLazer(i, ii);
                            }
                            break;
                        case LV2:
                            if (laz == 2) {
                                initVertLazer(i, ii);
                            }
                            break;
                    }
                }
            }
        }
    }

    // ordering the lazer initialilzation makes possible for any lazer to block any other depending on whose state has changed
    Integer [] lazerOrdering = { 0, 1, 2 };

    private void toggleLazers(int ... nums) {
        for (int n : nums) {
            level.lazers[n] = !level.lazers[n];
        }
        initLazers();
    }

    public void setLazerEnabled(int num, boolean on) {
        System.out.println("lazerOrdering num = " + num + " ordering: " + Arrays.toString(lazerOrdering));
        level.lazers[num] = on;
        initLazers();
        System.out.println("lazerOrdering = " + Arrays.toString(lazerOrdering));
    }

    private boolean canMoveToPos(int y, int x) {
        switch (level.coins[y][x]) {
            case DD:
            case LB0:
            case LB1:
            case LB2:
            case LB:
                return true;
        }
        return false;
    }

    private void turn(Guy guy, int d) {
        int nd = guy.dir.ordinal() + d;
        nd += Direction.values().length;
        nd %= Direction.values().length;
        guy.dir = Direction.values()[nd];
    }

    public void reset() {
        stop();
        if (copy != null) {
            copyFrom(copy);
        }
    }

    public void setLevel(int num, Level level) {
        this.levelNum = num;
        program.clear();
        Arrays.sort(lazerOrdering);
        init(level.deepCopy());
    }

    /**
     * Return -1 for infinte available.
     * Otherwise a number >= 0 of num available.
     *
     * @param t
     * @return
     */
    public int getCommandTypeNumAvaialable(CommandType t) {
        switch (t) {
            case Jump:
                return level.numJumps < 0 ? -1 : level.numJumps - getCommandCount(t);
            case LoopStart:
                return level.numLoops < 0 ? -1 : level.numLoops - getCommandCount(CommandType.LoopStart);
            case TurnLeft:
            case TurnRight:
            case UTurn:
                return level.numTurns < 0 ? -1 : level.numTurns - getCommandCount(CommandType.TurnLeft, CommandType.TurnRight, CommandType.UTurn);
        }
        return -1;
    }

    public boolean isCommandTypeVisible(CommandType t) {
        switch (t) {
            case Jump:
                return level.numJumps != 0;
            case LoopStart:
            case LoopEnd:
                return level.numLoops != 0;
            case TurnRight:
            case TurnLeft:
            case UTurn:
                return level.numTurns != 0;
            case Advance:
            default:
                return true;
        }
    }

    protected float getStrokeWidth() {
        return 10;
    }

    // begin rendering
    public void draw(AGraphics g, int width, int height) {

        float lineWidth = getStrokeWidth();

        g.clearScreen(GColor.BLACK);
        g.setColor(GColor.RED);
        g.drawRect(0, 0, width, height, lineWidth);

        Level l = level;
        if (l.coins == null || l.coins.length == 0 || l.coins[0].length == 0)
            return;

        int cols = l.coins[0].length;
        int rows = l.coins.length;

        // get cell width/height
        int cw = width / cols;
        int ch = height / rows;
        float radius = Math.round(0.2f * Math.min(cw, ch));

        int curColor = 0;
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                final int x = ii*cw + cw/2;
                final int y = i*ch + ch/2;
                final Type t = l.coins[i][ii];
                switch (t) {
                    case EM:
                        break;
                    case DD:
                        g.setColor(GColor.WHITE);
                        g.drawFilledCircle(x, y, radius);
                        break;
                    case SE:
                    case SS:
                    case SW:
                    case SN:
                        if (curColor < getMaxGuys())
                            drawGuy(g, new Guy(x, y, Direction.values()[t.ordinal()-2], manColors[curColor++]), radius);
                        else
                            l.coins[i][ii] = Type.EM;
                        break;
                    case LH0:
                        drawLazer(g, x, y, radius, true, GColor.RED);
                        break;
                    case LV0:
                        drawLazer(g, x, y, radius, false, GColor.RED);
                        break;
                    case LB0:
                        drawButton(g, x, y, radius, GColor.RED, level.lazers[0]);
                        break;
                    case LH1:
                        drawLazer(g, x, y, radius, true, GColor.BLUE);
                        break;
                    case LV1:
                        drawLazer(g, x, y, radius, false, GColor.BLUE);
                        break;
                    case LB1:
                        drawButton(g, x, y, radius, GColor.BLUE, level.lazers[1]);
                        break;
                    case LH2:
                        drawLazer(g, x, y, radius, true, GColor.GREEN);
                        break;
                    case LV2:
                        drawLazer(g, x, y, radius, false, GColor.GREEN);
                        break;
                    case LB2:
                        drawButton(g, x, y, radius, GColor.GREEN, level.lazers[2]);
                        break;
                    case LB:
                        // toggle all button
                        g.setColor(GColor.RED);
                        g.drawFilledCircle(x, y, radius*3/2);
                        g.setColor(GColor.GREEN);
                        g.drawCircle(x, y, radius);
                        g.setColor(GColor.BLUE);
                        g.drawCircle(x, y, radius*2/3);
                        break;
                }
            }
        }

        // draw guys
        for (Guy guy : guys) {
            drawGuy(g, guy, radius);
        }

        // draw lazers
        g.setColor(GColor.RED);
        for (int i=0; i<rows; i++) {
            for (int ii=0; ii<cols; ii++) {
                int cx = ii*cw + cw/2;
                int cy = i*ch + ch/2;
                int left = ii*cw;
                int right = left + cw;
                int top = i*ch;
                int bottom = top + ch;

                if (0 != (lazer[i][ii] & Probot.LAZER_WEST)) {
                    g.drawLine(left, cy, cx, cy, lineWidth);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_EAST)) {
                    g.drawLine(cx, cy, right, cy, lineWidth);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_NORTH)) {
                    g.drawLine(cx, top, cx, cy, lineWidth);
                }

                if (0 != (lazer[i][ii] & Probot.LAZER_SOUTH)) {
                    g.drawLine(cx, cy, cx, bottom, lineWidth);
                }

            }
        }
    }

    void drawLazer(AGraphics g, int cx, int cy, float rad, boolean horz, GColor color) {
        g.pushMatrix();
        g.translate(cx, cy);
        if (!horz) {
            g.rotate(90);
        }
        g.setColor(GColor.GRAY);
        float radius = rad*3/2;
        g.drawFilledCircle(0, 0, radius);
        g.setColor(color);
        g.begin();
        g.vertex(-radius, 0);
        g.vertex(0, -radius/2);
        g.vertex(radius, 0);
        g.vertex(0, radius/2);
        g.drawTriangleFan();
        g.popMatrix();
    }

    void drawButton(AGraphics g, int cx, int cy, float radius, GColor color, boolean on) {
        g.setColor(GColor.GRAY);
        g.drawFilledCircle(cx, cy, radius);
        g.setColor(color);
        //g.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
        //c.drawCircle(cx, cy, radius/2, p);
        if (on) {
            g.drawFilledCircle(cx, cy, radius/2);
        } else {
            g.drawCircle(cx, cy, radius/2);
        }
    }

    public void drawGuy(AGraphics g, Guy guy, float radius) {
        g.setColor(guy.color);
        final int x = guy.posx;
        final int y = guy.posy;
        g.drawFilledCircle(x, y, radius);
        g.setColor(GColor.BLACK);
        switch (guy.dir) {
            case Right:
                g.drawLine(x, y, x+radius, y, getStrokeWidth());
                break;
            case Down:
                g.drawLine(x, y, x, y+radius, getStrokeWidth());
                break;
            case Left:
                g.drawLine(x, y, x-radius, y, getStrokeWidth());
                break;
            case Up:
                g.drawLine(x, y, x, y-radius, getStrokeWidth());
                break;
        }
    }

    // Overrides to handle important events.

    protected void onCommand(int line) {}

    /**
     * Called at the end of program if some FAILED action occured.
     */
    protected void onFailed() {}

    /**
     * Guy a walked off board or onto an unavailable square
     * @param guy
     */
    protected void onAdvanceFailed(Guy guy) {}

    /**
     * A guy has advanced
     * @param guy
     */
    protected void onAdvanced(Guy guy) {}

    /**
     * A guy has jumped
     * @param guy
     */
    protected void onJumped(Guy guy) {}

    /**
     * A Guy has execute a right, left or U turn
     * @param guy
     * @param dir
     */
    protected void onTurned(Guy guy, int dir) {}

    /**
     * Called at end of program if no FAILED events occured.
     */
    protected void onSuccess() {}

    /**
     * Guy has walked into a lazer or a lazer activated on them. FAILED.
     * @param guy
     * @param instantaneous
     */
    protected void onLazered(Guy guy, boolean instantaneous) {}

    /**
     * The program has finished but not all dots eaten. FAILED.
     */
    protected void onDotsLeftUneaten() {}
}
