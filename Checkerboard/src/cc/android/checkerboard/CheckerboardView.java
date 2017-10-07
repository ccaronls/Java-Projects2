package cc.android.checkerboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.android.DroidUtils;
import cc.lib.game.AAnimation;
import cc.lib.game.IVector2D;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;

/**
 * This handles events form system like user input, pause, resume etc.
 * 
 * @author chriscaron
 *
 */
public class CheckerboardView extends View implements View.OnClickListener {

    private final Paint pFill = new Paint();
    private final Paint pStroke = new Paint();
    private final Paint pText = new Paint();
    private final RectF rf = new RectF();

    private Checkers game = null;

    public CheckerboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CheckerboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public final void setGame(Checkers game) {
        this.game = game;
        invalidate();
    }

    public final Checkers getGame() {
        return this.game;
    }

    private void init(Context context, AttributeSet attrs) {
        pFill.setStyle(Paint.Style.FILL);
        pStroke.setStyle(Paint.Style.STROKE);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckerboardView);
        pStroke.setStrokeWidth(a.getDimension(R.styleable.CheckerboardView_outlineThickness, 5));
        a.recycle();
        pText.setColor(Color.WHITE);
        pText.setTextSize(getResources().getDisplayMetrics().density * 12);
        pText.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private long downTime = 0;
    private int touchRank, touchColumn;
    private float dragX, dragY;
    private Piece dragging = null, tapped = null;
    private float cellW, cellH, pcRad;
    private Move highlightMove = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (game == null || animations.size() > 0)
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
                if (dt > 200 && game.isOnBoard(touchRank, touchColumn)) {
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

    private List<AAnimation<Canvas>> animations = new ArrayList<>();

    private abstract class MoveAnim extends AAnimation<Canvas> {
        final Move move;
        final List<Piece> hiders;
        final Runnable whenDone;

        MoveAnim(long duration, Move move, Runnable whenDone, Piece ... toHide) {
            super(duration, 0);
            this.move = move;
            this.whenDone = whenDone;
            this.hiders = Arrays.asList(toHide);
            hidden.addAll(hiders);
        }

        @Override
        protected final void onDone() {
            animations.remove(this);
            hidden.removeAll(hiders);
            if (whenDone != null) {
                whenDone.run();
            }
        }

    }

    class StackAnim extends MoveAnim {
        final int rank, col, stacks, playerNum;

        public StackAnim(Move move, Runnable whenDone, int playerNum, int rank, int col, int stacks) {
            super(1600, move, whenDone, game.getPiece(rank, col));
            this.rank = rank;
            this.col = col;
            this.stacks = stacks;
            this.playerNum = playerNum;
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            float sx = cellW * col + cellW/2;
            float sy = rank == 0 ? 0 : getHeight();//0;//cellH * move.startRank + cellH/2;
            float ex = cellW * col + cellW/2;
            float ey = cellH * rank + cellH/2;
            float scale = 1 + (1-position);
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = game.getPiece(rank, col);
            if (p.stacks - stacks > 0) {
                drawChecker(g, sx, sy, pcRad, p.stacks - stacks, playerNum, getPcColor(playerNum), 0);
            }
            g.save();
            g.translate(x, y);
            g.scale(scale, scale);
            drawChecker(g, 0, 0, pcRad, stacks, playerNum, getPcColor(playerNum), 0);
            g.restore();
        }

    }

    class SlideAnim extends MoveAnim {

        public SlideAnim(Move move, Runnable whenDone) {
            super(800, move, whenDone, game.getPiece(move.startRank, move.startCol));
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
            drawChecker(g, x, y, pcRad, p.stacks, move.playerNum, getPcColor(move.playerNum), 0);
        }
    }

    class JumpAnim extends MoveAnim {

        final Bezier curve;

        private IVector2D[] computeJumpPoints(Move move) {

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

        public JumpAnim(Move move, Runnable whenDone) {
            super(1200, move, whenDone, game.getPiece(move.startRank, move.startCol));
            curve = new Bezier(computeJumpPoints(move));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            Vector2D v = curve.getPointAt(position);
            Piece p = game.getPiece(move.startRank, move.startCol);
            drawChecker(g, v.Xi(), v.Yi(), pcRad, p.stacks, p.playerNum, getPcColor(p.playerNum), 0);
        }

    };
    private void doTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.moves.size() == 0)
                tapped = null;
        } else {
            for (Move m : tapped.moves) {
                if (m.endRank==curRank && m.endCol == curCol && m.type != Checkers.MoveType.END) {
                    animateAndExecuteMove(m);
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

    public void animateAndExecuteMove(final Move m) {
        animateMoveAndThen(m, new Runnable() {
            public void run() {
                game.executeMove(m);
            }
        });
    }

    public void animateMoveAndThen(Move m, Runnable onDone) {
        switch (m.type) {
            case SLIDE:
                animations.add(new SlideAnim(m, onDone).start());
                break;
            case JUMP_CAPTURE: {
                Piece capture = game.getPiece(m.captureRank, m.captureCol);
                animations.add(new StackAnim(m, null, capture.playerNum, m.captureRank, m.captureCol, capture.stacks).startReverse());
            }
            case JUMP:
                animations.add(new JumpAnim(m, onDone).start());
                break;
            case STACK:
                animations.add(new StackAnim(m, onDone, m.playerNum, m.startRank, m.startCol, 1).start());
                break;
            default:
                if (onDone != null) {
                    post(onDone);
                    postInvalidate();
                }
                break;
        }
        invalidate();
    }

    private void doDragEnd(int curRank, int curCol) {
        for (Move m : dragging.moves) {
            if (m.endRank == curRank && m.endCol == curCol) {
                switch (m.type) {
                    case JUMP_CAPTURE:
                        Piece captured = game.getPiece(m.captureRank, m.captureCol);
                        animations.add(new StackAnim(m, null, captured.playerNum, m.captureRank, m.captureCol, captured.stacks).startReverse());
                        break;
                }
                game.executeMove(m);
                break;
            }
        }
        dragging = null;
    }

    private final Set<Piece> hidden = new HashSet<>();

    @Override
    protected void onDraw(Canvas canvas) {

        float width = getWidth();
        float height = getHeight();

        int COLUMNS=8;
        int RANKS=8;

        if (game != null) {
            COLUMNS=game.COLUMNS;
            RANKS =game.RANKS;
        }

        cellW = width / COLUMNS;
        cellH = height / RANKS;
        pcRad = Math.min(cellW/3, cellH/3);

        pFill.setColor(Color.BLACK);
        canvas.drawRect(0f, 0f, width, height, pFill);

        pFill.setColor(DroidUtils.ORANGE);

        for (int i=0; i<COLUMNS; i++) {
            for (int ii=i%2; ii<RANKS; ii+=2) {
                float x = i*cellW;
                float y = ii*cellH;
                canvas.drawRect(x, y, x+cellW, y+cellH, pFill);
            }
        }

        for (int i=0; i<COLUMNS; i++) {
            String txt = String.valueOf(i);
            canvas.drawText(txt, 0, txt.length(), 5+i*cellW, cellH - 5, pText);
        }

        for (int i=1; i<RANKS; i++) {
            String txt = String.valueOf(i);
            canvas.drawText(txt, 0, txt.length(), 5, cellH-5+i*cellH, pText);
        }

        if (game == null)
            return;

        ArrayList<AAnimation<Canvas>> anims = new ArrayList<>(animations);

        if (anims.size() == 0) {
            hidden.clear();
        }

        Piece mainPc = null;
        int numMvblePcs = 0;
        for (int i=0; i<COLUMNS; i++) {
            for (int ii=0; ii<RANKS; ii++) {
                if (dragging != null && touchColumn == i && touchRank == ii)
                    continue;
                Piece pc = game.getPiece(ii, i);
                if (pc != null && pc.stacks > 0 && !hidden.contains(pc)) {
                    boolean outline = dragging == null && tapped == null && pc.moves.size() > 0;
                    drawChecker(canvas, pc, ii, i, outline);
                    if (pc.moves.size() > 0) {
                        numMvblePcs++;
                        if (numMvblePcs == 1) {
                            mainPc = pc;
                        } else {
                            mainPc = null;
                        }
                    }
                }
            }
        }

        if (anims.size() > 0) {
            for (AAnimation<Canvas> a : anims)
                a.update(canvas);
            invalidate();
            return;
        }

        if (highlightMove != null) {
            float sx = highlightMove.startCol*cellW;
            float sy = highlightMove.startRank*cellH;
            float ex = highlightMove.endCol*cellW;
            float ey = highlightMove.endRank*cellH;
            switch (highlightMove.type) {
                case JUMP:
                case JUMP_CAPTURE:
                case SLIDE:
                    pStroke.setColor(Color.RED);
                    canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                    pStroke.setColor(Color.GREEN);
                    canvas.drawRect(ex, ey, ex+cellW, ey+cellH, pStroke);
                    break;
                case END:
                    pStroke.setColor(Color.YELLOW);
                    canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                    break;
                case STACK:
                    pStroke.setColor(Color.BLUE);
                    canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                    break;
            }
        } else if (mainPc != null) {
            tapped = mainPc;
        }

        if (tapped != null) {
            for (Move m : new ArrayList<>(tapped.moves)) {
                Log.d("CB", "Tapped move: " + m);
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

    }

    int DIR_BLACK = 1;
    int DIR_RED   = -1;

    int getDir(int playerNum) {
        switch (playerNum) {
            case 0:
                return DIR_BLACK;
            case 1:
                return DIR_RED;
        }
        return 0;
    }

    int [] playerColors = {
            Color.rgb(64, 64, 64),//BLACK,
            Color.RED,//rgb(64, 64, 255)
    };

    int getPcColor(int playerNum) {
        if (playerNum >= 0)
            return playerColors[playerNum];
        return 0;
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
            drawChecker(g, x, y, pcRad, pc.stacks, pc.playerNum, getPcColor(pc.playerNum), outlined ? Color.YELLOW : 0);
            y -= pcRad/4;
        }
    }

    void drawChecker(Canvas g, float x, float y, float rad, int stacks, int playerNum, int color, int outlineColor) {
        if (stacks <= 0)
            return;
        int dark = DroidUtils.darken(color, 0.5f);
        pFill.setColor(dark);
        final int step = (int)(rad/20 * -1);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonEndTurn) {
            game.endTurn();
        } else if (v.getId() == R.id.buttonNewGame) {
            game.newGame();
        }
        this.dragging = this.tapped = null;
        invalidate();
    }

    public final void undo() {
        if (animations.size() > 0)
            return;
        Move m = game.undo();
        if (m != null) {
            switch (m.type) {
                case JUMP_CAPTURE:
                    animations.add(new StackAnim(m, null, m.captured.playerNum, m.captureRank, m.captureCol, m.captured.stacks).start());
                case JUMP:
                    animations.add(new JumpAnim(m, null).startReverse());
                    break;
                case SLIDE:
                    animations.add(new SlideAnim(m, null).startReverse());
                    break;
                case STACK:
                    animations.add(new StackAnim(m, null, m.playerNum, m.startRank, m.startCol, 1).startReverse());
                    break;
            }
        }
        tapped = null;
        dragging = null;
        invalidate();
    }

    public boolean isEndTurnButtonAvailable() {
        Piece lock = game.getLocked();
        if (lock != null) {
            if (lock.playerNum != game.getCurPlayerNum())
                throw new AssertionError();
            for (Move m : lock.moves) {
                if (m.type == Checkers.MoveType.END) {
                    return true;
                }
            }
        }
        return false;
    }

    public void highlightMove(Move m) {
        this.highlightMove = m;
        this.tapped = null;
    }
}
