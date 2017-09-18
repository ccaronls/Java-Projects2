package cc.android.checkerboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;

import cc.lib.android.DroidUtils;
import cc.lib.game.AAnimation;
import cc.lib.game.IVector2D;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

/**
 * This handles events form system like user input, pause, resume etc.
 * 
 * @author chriscaron
 *
 */
public class CheckerboardView extends View implements View.OnClickListener {

    private final Paint pFill = new Paint();
    private final Paint pStroke = new Paint();
    private final RectF rf = new RectF();
    private Checkers game = new Checkers();

    public CheckerboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CheckerboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        pFill.setStyle(Paint.Style.FILL);
        pStroke.setStyle(Paint.Style.STROKE);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckerboardView);
        pStroke.setStrokeWidth(a.getDimension(R.styleable.CheckerboardView_outlineThickness, 5));
        a.recycle();
        game.newGame();
    }

    long downTime = 0;
    int touchRank, touchColumn;
    float dragX, dragY;
    Piece dragging = null, tapped = null;
    float cellW, cellH, pcRad;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (game == null || animation != null)
            return false;

        float touchX = event.getX();
        float touchY = event.getY();

        int curRank = (int)(touchY * 8 / getHeight());
        int curColumn = (int)(touchX * 8 / getWidth());


        long dt = (int)(SystemClock.uptimeMillis() - downTime);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                touchRank = curRank;
                touchColumn = curColumn;
                break;
            case MotionEvent.ACTION_MOVE:
                if (tapped == null && game.isOnBoard(touchRank, touchColumn)) {
                    dragX = touchX;
                    dragY = touchY;
                    dragging = game.getPiece(touchRank, touchColumn);
                    if (dragging.moves.size() == 0)
                        dragging = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dt < 500 && curColumn == touchColumn && curRank == touchRank) {
                    doTap(curRank, curColumn);
                } else if (dragging != null) {
                    doDragEnd(curRank, curColumn);
                    tapped = null;
                }
                dragging = null;
                break;

            default:
                return false;

        }
        invalidate();
        return true;
    }

    private AAnimation<Canvas> animation = null;

    private abstract class BaseAnim extends AAnimation<Canvas> {
        final Move move;
        final int stacks;
        final int playerNum;

        BaseAnim(long duration, int maxRepeats, Move move, int playerNum) {
            super(duration, maxRepeats);
            this.move = move;
            this.playerNum = playerNum;
            this.stacks = game.getPiece(move.startRank, move.startCol).stacks;
        }

        @Override
        protected final void onDone() {
            game.getPiece(move.startRank, move.startCol).stacks = stacks;
            game.executeMove(move);
            animation = null;
        }
    }

    IVector2D[] computeJumpPoints(Move move) {

        float sx = cellW * move.startCol + cellW/2;
        float sy = cellW * move.startRank + cellH/2;
        float ex = cellH * move.endCol + cellW/2;
        float ey = cellH * move.endRank + cellH/2;

        float midx1 = sx + ((ex-sx) / 3);
        float midx2 = sx + ((ex-sx) * 2 / 3);
        float midy1 = sy + ((ey-sy) / 3);
        float midy2 = sy + ((ey-sy) * 2 / 3);
        float dist = cellH * getDir(move.playerNum);
        IVector2D [] v = {
                new Vector2D(sx, sy),
                new Vector2D(midx1, midy1+dist),
                new Vector2D(midx2, midy2+dist),
                new Vector2D(ex, ey),
        };
        return v;
    }

    class StackAnim extends BaseAnim {
        public StackAnim(Move move, int playerNum) {
            super(2000, 0, move, playerNum);
        }

        @Override
        protected void onStarted() {
            //Piece p = CheckerboardActivity.game.getPiece(move.startRank, move.startCol);
            //p.stacks = 0;
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            float sx = cellW * move.startCol + cellW/2;
            float sy = move.startRank == 0 ? 0 : getHeight();//0;//cellH * move.startRank + cellH/2;
            float ex = cellW * move.startCol + cellW/2;
            float ey = cellH * move.startRank + cellH/2;
            float scale = 1 + (1-position);
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = game.getPiece(move.startRank, move.startCol);
            g.save();
            g.translate(x, y);
            g.scale(scale, scale);
            drawChecker(g, 0, 0, pcRad, stacks, playerNum, getPcColor(p), 0);
            g.restore();
        }

    }

    class SlideAnim extends BaseAnim {

        public SlideAnim(Move move, int playerNum) {
            super(1000, 0, move, playerNum);
        }

        @Override
        protected void onStarted() {
            Piece p = game.getPiece(move.startRank, move.startCol);
            p.stacks = 0;
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            float sx = cellW * move.startCol + cellW/2;
            float sy = cellH * move.startRank + cellH/2;
            float ex = cellW * move.endCol + cellW/2;
            float ey = cellH * move.endRank + cellH/2;
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = game.getPiece(move.startRank, move.startCol);
            drawChecker(g, x, y, pcRad, stacks, playerNum, getPcColor(p), 0);
        }
    }

    class JumpAnim extends BaseAnim {

        final Bezier curve;

        public JumpAnim(Move move, int playerNum) {
            super(2000, 0, move, playerNum);
            curve = new Bezier(computeJumpPoints(move));
        }

        @Override
        protected void onStarted() {
            Piece p = game.getPiece(move.startRank, move.startCol);
            p.stacks = 0;
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            Vector2D v = curve.getPointAt(position);
            Piece p = game.getPiece(move.startRank, move.startCol);
            drawChecker(g, v.Xi(), v.Yi(), pcRad, stacks, playerNum, getPcColor(p), 0);
        }

    };
    private void doTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.moves.size() == 0)
                tapped = null;
        } else {
            for (Move m : tapped.moves) {
                if (m.endRank==curRank && m.endCol == curCol) {
                    switch (m.type) {
                        case SLIDE:
                            animation = new SlideAnim(m, game.getCurPlayerNum()).start();
                            break;
                        case JUMP:
                        case JUMP_CAPTURE:
                            animation = new JumpAnim(m, game.getCurPlayerNum()).start();
                            break;
                        case STACK:
                            animation = new StackAnim(m, game.getCurPlayerNum()).start();
                            break;
                        default:
                            game.executeMove(m);
                            break;
                    }
                    tapped = null;
                    return;
                }
            }
            Piece p = game.getPiece(curRank, curCol);
            if (p.moves.size() > 0) {
                tapped = p;
            } else {
                tapped = null;
            }
        }
    }

    private void doDragEnd(int curRank, int curCol) {
        for (Move m : dragging.moves) {
            if (m.endRank == curRank && m.endCol == curCol) {
                game.executeMove(m);
                break;
            }
        }
        dragging = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float width = getWidth();
        float height = getHeight();

        cellW = width / game.COLUMNS;
        cellH = height / game.RANKS;
        pcRad = Math.min(cellW/3, cellH/3);

        pFill.setColor(Color.DKGRAY);
        canvas.drawRect(0f, 0f, width, height, pFill);

        pFill.setColor(DroidUtils.ORANGE);

        for (int i=0; i<game.COLUMNS; i++) {
            for (int ii=i%2; ii<game.RANKS; ii+=2) {
                float x = i*cellW;
                float y = ii*cellH;
                canvas.drawRect(x, y, x+cellW, y+cellH, pFill);
            }
        }

        if (game == null)
            return;

        int numMvs = 0;
        Move mainMv = null;
        for (int i=0; i<game.COLUMNS; i++) {
            for (int ii=0; ii<game.RANKS; ii++) {
                if (dragging != null && touchColumn == i && touchRank == ii)
                    continue;
                Piece pc = game.getPiece(ii, i);
                if (pc != null && pc.stacks > 0) {
                    boolean outline = dragging == null && tapped == null && pc.moves.size() > 0;
                    drawChecker(canvas, pc, ii, i, outline);
                    if (pc.moves.size() > 0) {
                        if (pc.moves.size() == 1)
                            mainMv = pc.moves.get(0);
                        numMvs += pc.moves.size();
                    }
                }
            }
        }

        if (animation != null) {
            animation.update(canvas);
            invalidate();
            return;
        }

        if (numMvs == 1 && mainMv != null && mainMv.type == Checkers.MoveType.STACK) {
            tapped = game.getPiece(mainMv.startRank, mainMv.startCol);
            dragging = null;
        }

        if (dragging != null) {
            drawChecker(canvas, dragging, dragX, dragY, false);
            for (Move m : dragging.moves) {
                float sx = m.startCol*cellW;
                float sy = m.startRank*cellH;
                float ex = m.endCol*cellW;
                float ey = m.endRank*cellH;
                switch (m.type) {
                    case END:
                        break;
                    case JUMP:
                    case JUMP_CAPTURE:
                    case SLIDE:
                        //pStroke.setColor(Color.GREEN);
                        //canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                        pStroke.setColor(Color.YELLOW);
                        canvas.drawRect(ex, ey, ex+cellW, ey+cellH, pStroke);
                        break;
                    case STACK:
                        pStroke.setColor(Color.GREEN);
                        drawDisk(canvas, pStroke, sx+cellW/2, sy+cellH/2, pcRad);
                        break;
                }
            }
        }

        if (tapped != null) {
            for (Move m : tapped.moves) {
                float sx = m.startCol*cellW;
                float sy = m.startRank*cellH;
                float ex = m.endCol*cellW;
                float ey = m.endRank*cellH;
                switch (m.type) {
                    case END:
                        break;
                    case JUMP:
                    case JUMP_CAPTURE:
                    case SLIDE:
                        pStroke.setColor(Color.GREEN);
                        canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                        pStroke.setColor(Color.YELLOW);
                        canvas.drawRect(ex, ey, ex+cellW, ey+cellH, pStroke);
                        break;
                    case STACK:
                        pStroke.setColor(Color.GREEN);
                        drawDisk(canvas, pStroke, sx+cellW/2, sy+cellH/2, pcRad);
                }
            }
        }
    }

    int DIR_BLACK = -1;
    int DIR_RED   = 1;

    int getDir(int playerNum) {
        switch (playerNum) {
            case 0:
                return DIR_BLACK;
            case 1:
                return DIR_RED;
        }
        return 0;
    }

    int getPcColor(Piece p) {
        if (p.playerNum == 0)
            return Color.RED;
        return Color.BLUE;
    }

    void drawChecker(Canvas g, Piece pc, int rank, int col, boolean outlined) {
        float cx = col*cellW + cellW/2;
        float cy = rank*cellH + cellH/2;
        drawChecker(g, pc, cx, cy, outlined);
    }

    void drawChecker(Canvas g, Piece pc, float x, float y, boolean outlined) {
        if (pc.stacks == 0)
            return;
        for (int i=0; i<pc.stacks; i++) {
            drawChecker(g, x, y, pcRad, pc.stacks, pc.playerNum, getPcColor(pc), outlined ? Color.YELLOW : 0);
            y -= pcRad/4;
        }
    }

    void drawChecker(Canvas g, float x, float y, float rad, int stacks, int playerNum, int color, int outlineColor) {
        if (stacks <= 0)
            return;
        int dark = DroidUtils.darken(color, 0.5f);
        pFill.setColor(dark);
        final int step = (int)(rad/20 * getDir(playerNum));
        final int num = 5;
        for (int i=0; i<num; i++) {
            drawDisk(g, pFill, x/*+i*step*/, y+i*step, rad);
        }
        pFill.setColor(color);
        drawDisk(g, pFill, x/*+num*step*/, y+num*step, rad);
        if (outlineColor != 0) {
            pStroke.setColor(outlineColor);
            drawDisk(g, pStroke, x, y+num*step, rad);
        }
    }

    void drawDisk(Canvas c, Paint p, float x, float y, float rad) {
        rf.set(x-rad, y-rad, x+rad, y+rad);
        c.drawOval(rf, p);
    }

    void pause(File saveFile) {
        try {
            game.saveToFile(saveFile);
            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void resume(File saveFile) {
        try {
            if (saveFile.exists()) {
                game.loadFromFile(saveFile);
                FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
            }
        } catch (Reflector.VersionTooOldException e) {
            saveFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonEndTurn) {
            game.endTurn();
        } else if (v.getId() == R.id.buttonNewGame) {
            game.newGame();
        }
        invalidate();
    }

}
