package cc.android.checkerboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cc.lib.android.DroidUtils;
import cc.lib.checkers.*;
import cc.lib.game.AAnimation;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.Bezier;
import cc.lib.math.Vector2D;

/**
 * This class renders the game and handles touch events on the board.
 * 
 * @author chriscaron
 *
 */
public class CheckerboardView extends RelativeLayout implements View.OnClickListener, Runnable {

    private final Paint pFill = new Paint();
    private final Paint pStroke = new Paint();
    private final Paint pText = new Paint();
    private final RectF rf = new RectF();
    private final Paint glowYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowRed = new Paint(Paint.ANTI_ALIAS_FLAG);
    boolean drawDebugInfo = false;
    private TextView bNear, bFar, bStart;

    private CheckerboardActivity activity;
    private ACheckboardGame game = null;
    private float boardPadding = 0;

    public CheckerboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CheckerboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public final void setGame(ACheckboardGame game) {
        this.game = game;
        reset();
        removeCallbacks(this);
        if (game != null && game instanceof Chess && ((Chess)game).getTimerLength() > 0) {
            bFar.setVisibility(View.VISIBLE);
            bNear.setVisibility(View.VISIBLE);
            bStart.setVisibility(View.VISIBLE);
            bFar.setEnabled(false);
            bNear.setEnabled(false);
            bNear.setText(timerText(((Chess)game).getTimerNear()));
            bFar.setText(timerText(((Chess)game).getTimerFar()));
        } else {
            bFar.setVisibility(View.GONE);
            bNear.setVisibility(View.GONE);
            bStart.setVisibility(View.GONE);
        }
        invalidate();
    }

    public final ACheckboardGame getGame() {
        return this.game;
    }

    private void init(Context context, AttributeSet attrs) {
        activity = (CheckerboardActivity)context;
        pFill.setStyle(Paint.Style.FILL);
        pStroke.setStyle(Paint.Style.STROKE);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckerboardView);
        pStroke.setStrokeWidth(a.getDimension(R.styleable.CheckerboardView_outlineThickness, 5));
        a.recycle();
        pText.setColor(Color.WHITE);
        pText.setTextSize(getResources().getDisplayMetrics().density * 12);
        pText.setTypeface(Typeface.DEFAULT_BOLD);
        ColorFilter filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        BlurMaskFilter blur = new BlurMaskFilter(15, BlurMaskFilter.Blur.INNER);

        glowRed.setColorFilter(filter);
        glowRed.setMaskFilter(blur);

        filter = new PorterDuffColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
        //BlurMaskFilter blur = new BlurMaskFilter(300, BlurMaskFilter.Blur.OUTER);

        glowYellow.setColorFilter(filter);
        glowRed.setMaskFilter(blur);

        if (isInEditMode()) {
            game = new Chess();
            game.newGame();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        (bFar = (TextView)findViewById(R.id.buttonFar)).setOnClickListener(this);
        (bNear = (TextView)findViewById(R.id.buttonNear)).setOnClickListener(this);
        (bStart = (TextView)findViewById(R.id.buttonStart)).setOnClickListener(this);
        setGame(game);
    }

    private long downTime = 0;
    private int touchRank, touchColumn;
    private float dragX, dragY;
    private Piece dragging = null, tapped = null;
    private float cellDim, pcRad;
    private Move highlightMove = null;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (game == null || animations.size() > 0)
            return false;

        float touchX = event.getX();
        float touchY = event.getY();

        int dim = Math.min(getWidth(), getHeight());
        dim -= boardPadding*2;
        int curRank = (int)((touchY-boardPadding) * game.RANKS / dim);
        int curColumn = (int)((touchX-boardPadding) * game.COLUMNS / dim);

        long dt = (int)(SystemClock.uptimeMillis() - downTime);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = SystemClock.uptimeMillis();
                touchRank = curRank;
                touchColumn = curColumn;
                break;
            case MotionEvent.ACTION_MOVE:
                if (dt > 200 && game.isOnBoard(touchRank, touchColumn)) {
                    dragX = touchX - boardPadding;
                    dragY = touchY - boardPadding;
                    dragging = game.getPiece(touchRank, touchColumn);
                    if (dragging.getNumMoves() == 0)
                        dragging = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (dt < 500 && curColumn == touchColumn && curRank == touchRank) {
                    onTap(curRank, curColumn);
                } else if (dragging != null) {
                    onDragEnd(curRank, curColumn);
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

    private class GlowAnim extends AAnimation<Canvas> {
        final Paint p;
        final int start, end;

        GlowAnim(Paint p, int start, int end) {
            super(3000, -1, true);
            this.p = p;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void draw(Canvas g, float position, float dt) {
            int color = Utils.interpolateColor(start, end, position);
            p.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            invalidate();
        }
    }

    private abstract class MoveAnim extends AAnimation<Canvas> {
        final Runnable whenDone;
        final float sx;
        final float sy;
        final float ex;
        final float ey;
        final int playerNum;
        final PieceType pType;

        MoveAnim(long duration, int [] start, int [] end, int playerNum, PieceType pType, Runnable whenDone, Piece ... toHide) {
            //super(duration, 0);
            this(duration,
                    cellDim * start[1] + cellDim / 2,
                    cellDim * start[0] + cellDim / 2,// == 0 ? 0f : (float)getHeight(),
                    cellDim * end[1] + cellDim / 2,
                    cellDim * end[0] + cellDim / 2,
                    playerNum, pType, whenDone, toHide);
        }

        MoveAnim(long duration, float sx, float sy, float ex, float ey, int playerNum, PieceType pType, Runnable whenDone, Piece ... toHide) {
            super(duration, 0);
            this.sx = sx;
            this.sy = sy;
            this.ex = ex;
            this.ey = ey;
            this.playerNum = playerNum;
            this.pType = pType;
            this.whenDone = whenDone;
            hidden.addAll(Arrays.asList(toHide));
        }

        MoveAnim(long duration, Move move, Runnable whenDone, Piece ... toHide) {
            this(duration, move.getStart(), move.hasEnd() ? move.getEnd() : move.getStart(), move.getPlayerNum(), move.getStartType(), whenDone, toHide);
        }

        @Override
        protected final void onDone() {
//            animations.remove(this);
            if (whenDone != null) {
                whenDone.run();
            }
        }

    }

    class StackAnim extends MoveAnim {

        public StackAnim(int [] pos, int playerNum, PieceType pt, Runnable whenDone) {
            super(1200, cellDim * pos[1] + cellDim/2,
                    pos[1] == 0 ? 0 : getHeight(),
                    cellDim * pos[1] + cellDim/2,
                    cellDim * pos[0] + cellDim/2, playerNum, pt, whenDone);//, game.getPiece(pos));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {

            float scale = 1 + (1-position);
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            g.save();
            g.translate(x, y);
            g.scale(scale, scale);
            drawPieceAt(g, new Piece(playerNum, pType), 0f, 0f, OUTLINE_NONE, false);
            g.restore();
        }

    }

    boolean shouldDrawUpsideDown(int playerNum) {
        return (game instanceof Chess) && activity.isMultiPlayer() && (playerNum < 0 || game.getTurn() == playerNum);
    }

    class SlideAnim extends MoveAnim {

        public SlideAnim(Move move, Runnable whenDone) {
            this(move.getStart(), move.getEnd(), move.getPlayerNum(), move.getStartType(), whenDone);
        }

        public SlideAnim(int[] start, int []end, int playerNum, PieceType pt, Runnable whenDone) {
            super(800, start, end, playerNum, pt, whenDone, game.getPiece(start));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = new Piece(playerNum, pType);
            drawPieceAt(g, p, x, y, OUTLINE_NONE, shouldDrawUpsideDown(ACheckboardGame.FAR));
        }
    }

    class JumpAnim extends MoveAnim {

        final Bezier curve;
        boolean upsidedown;

        private IVector2D[] computeJumpPoints(int playerNum) {

            upsidedown = shouldDrawUpsideDown(playerNum);
            float midx1 = sx + ((ex-sx) / 3);
            float midx2 = sx + ((ex-sx) * 2 / 3);
            float midy1 = sy + ((ey-sy) / 3);
            float midy2 = sy + ((ey-sy) * 2 / 3);
            float dist = cellDim * -1;//getDir(playerNum);
            IVector2D [] v = {
                    new Vector2D(sx, sy),
                    new Vector2D(midx1, midy1+dist),
                    new Vector2D(midx2, midy2+dist),
                    new Vector2D(ex, ey),
            };
            return v;
        }

        JumpAnim setUpsideDown(boolean upsideDown) {
            this.upsidedown = upsideDown;
            return this;
        }

        public JumpAnim(int [] start, int [] end, int playerNum, PieceType pt, Runnable whenDone) {
            super(1200, start, end, playerNum, pt, whenDone, game.getPiece(start));
            curve = new Bezier(computeJumpPoints(playerNum));
        }

        public JumpAnim(float sx, float sy, float ex, float ey, int playerNum, PieceType pt, Runnable whenDone, Piece ... hiders) {
            super(1200, sx, sy, ex, ey, playerNum, pt, whenDone, hiders);
            curve = new Bezier(computeJumpPoints(playerNum));
        }

        public JumpAnim(Move move, Runnable whenDone) {
            super(1200, move, whenDone, game.getPiece(move.getStart()));
            curve = new Bezier(computeJumpPoints(playerNum));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            Vector2D v = curve.getPointAt(position);
            Piece p = new Piece(playerNum, pType);
            drawPieceAt(g, p, v.X(), v.Y(), OUTLINE_NONE, upsidedown);
        }

    };

    // TODO: Another way would be to pull up a picker from a (long?) touch but how to cancel?
    private void onTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.getNumMoves() == 0)
                tapped = null;
        } else {
            Piece swappy = null;
            for (Move m : tapped.getMoves()) {
                switch (m.getMoveType()) {
                    case END:
                        continue;
                    case CASTLE:
                    case SLIDE:
                    case FLYING_JUMP:
                    case JUMP:
                        if (m.getEnd()[0]==curRank && m.getEnd()[1] == curCol) {
                            animateAndExecuteMove(m);
                            tapped = null;
                            return;
                        }
                        break;
                    case SWAP: {
                        if (m.getStart()[0]==curRank && m.getStart()[1]==curCol) {
                            Piece p = game.getPiece(curRank, curCol);
                            swappy = p;
                            if (p.getType() == PieceType.PAWN_TOSWAP) {
                                p.setType(m.getEndType());
                            } else if (p.getType() == m.getEndType()) {
                                p.setType(PieceType.PAWN_TOSWAP); // this makes so next iteration will
                            }
                        }
                        break;
                    }
                    case STACK:
                        if (m.getStart()[0]==curRank && m.getStart()[1]==curCol) {
                            animateAndExecuteMove(m);
                            tapped = null;
                            return;
                        }
                        break;
                }
            }
            if (swappy != null) {
                if (swappy.getType() == PieceType.PAWN_TOSWAP) {
                    swappy.setType(swappy.getMovesIterator().next().getEndType());
                }
            } else {
                Piece p = game.getPiece(curRank, curCol);
                if (p.getNumMoves() > 0) {
                    tapped = p;
                } else {
                    tapped = null;
                }
            }
            if (Build.VERSION.SDK_INT >= 15)
                callOnClick();

            if (taps == 0) {
                postDelayed(new Runnable() {
                    public void run() {
                        taps = 0;
                    }
                }, 2000);
            }

            if (++taps == 5) {
                drawDebugInfo = !drawDebugInfo;
            }
        }
    }

    int taps = 0;

    public void animateAndExecuteMove(final Move m) {
        highlightMove = null;
        animateMoveAndThen(m, new Runnable() {
            public void run() {
                game.executeMove(m);
            }
        });
    }

    private void startDrawGameAnimation() {
        animations.add(new AAnimation<Canvas>(2000, 10, true) {

            Paint p = new Paint();

            @Override
            protected void draw(Canvas g, float position, float dt) {

                int startColor = Color.RED;
                int endColor = Color.CYAN;

                float minSize = cellDim / 3;
                float maxSize = cellDim * 2 / 3;

                p.setColor(Utils.interpolateColor(startColor, endColor, position));
                p.setTextSize(minSize + (maxSize - minSize) * position);

                float dim = Math.min(getWidth(), getHeight());

                DroidUtils.drawJustifiedTextCanvas(g, "DRAW GAME", dim / 2, dim / 2, DroidUtils.JUSTIY_CENTER, DroidUtils.JUSTIY_CENTER, p);

            }
        }.start());
        postInvalidate();//invalidate();
    }

    public void startEndgameAnimation() {

        int[] pos;
        if (game.isForfeited()) {
            pos = game.findPiecePosition(game.getTurn(), PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE, PieceType.UNCHECKED_KING, PieceType.UNCHECKED_KING_IDLE);
        } else {
            pos = game.findPiecePosition(game.getTurn(), PieceType.CHECKED_KING, PieceType.CHECKED_KING_IDLE);
        }
        if (pos == null) {
            // then we are in a draw game.
            startDrawGameAnimation();
            return;
        }

        hidden.add(game.getPiece(pos[0], pos[1]));
        final int drawable = game.getPlayerColor(game.getTurn()) == ACheckboardGame.Color.BLACK ? R.drawable.bk_king : R.drawable.wt_king;

        animations.add(new CheckmateAnim(pos, drawable, false).start());

        //animations.add(new GlowAnim(glowRed, Color.RED, Color.argb(0, 0xff, 0, 0)).start());
    }

    private class CheckmateAnim extends AAnimation<Canvas>{

        final int [] pos;
        final int drawable;

        CheckmateAnim(int [] pos, int d, boolean neverEnds) {
            super(2000, neverEnds ? -1 : 0);
            this.pos = pos;
            this.drawable = d;
        }

        @Override
        protected void draw(Canvas g, float position, float dt) {

            float cx = pos[1] * cellDim + cellDim / 2;
            float cy = pos[0] * cellDim + cellDim / 2;

            BitmapDrawable d = (BitmapDrawable)getResources().getDrawable(drawable);
            float [] dim = getBitmapRect(d, 1);

            boolean upsidedown = shouldDrawUpsideDown(ACheckboardGame.FAR);
            g.save();
            g.translate(cx, cy);
            float ty = dim[1]/2 * (upsidedown ? -1 : 1);
            g.translate(dim[0]/2, ty);
            g.rotate(90f * position * (upsidedown ? -1:1));
            g.translate(-dim[0]/2, -ty);
            drawBitmap(g, 0, 0, d, 1, OUTLINE_NONE, upsidedown);
            g.restore();

        }

        @Override
        public boolean isDone() {
            return isReverse();
        }
    }

    private class CaptureAnim extends JumpAnim {
        CaptureAnim(int sx, int sy, int playerNum, PieceType type, Runnable whenDone) {
            super(cellDim * sx + cellDim / 2,
                    cellDim * sy + cellDim / 2,
                    playerNum == ACheckboardGame.NEAR ? nextFarCaptureX : nextNearCaptureX,
                    playerNum == ACheckboardGame.NEAR ? nextFarCaptureY : nextNearCaptureY,
                    playerNum, type, whenDone);
            setUpsideDown(shouldDrawUpsideDown(-1) && playerNum == ACheckboardGame.NEAR);
        }
    }

    private void startCapturedAnimation(Move m, Runnable whenDone) {
        //startCapturedAnimation(m, whenDone, false);
        startCapturedAnimation(m.getCaptured(), game.getPiece(m.getCaptured()), whenDone);
    }

    public void startCapturedAnimation(int [] pos, Piece p, Runnable whenDone) {
        animations.add(new CaptureAnim(pos[1], pos[0], p.getPlayerNum(), p.getType(), whenDone).start());
        this.hidden.add(p);
    }

    private void startCapturedAnimation(Move m, Runnable whenDone, boolean reverse) {

        JumpAnim a = new CaptureAnim(m.getCaptured()[1], m.getCaptured()[0], game.getOpponent(m.getPlayerNum()), m.getCapturedType(), whenDone);
        animations.add(a);
        if (reverse)
            a.startReverse();
        else
            a.start();
        this.hidden.add(game.getPiece(m.getCaptured()));
    }

    public void animateMoveAndThen(Move m, Runnable onDone) {
        switch (m.getMoveType()) {
            case CASTLE:
                animations.add(new JumpAnim(m.getCastleRookStart(), m.getCastleRookEnd(), m.getPlayerNum(), PieceType.ROOK, onDone).start());
                animations.add(new SlideAnim(m, null).start());
                break;
            case SLIDE:
                if (m.hasCaptured()) {
                    startCapturedAnimation(m, onDone);
                    onDone = null;
                }
                animations.add(new SlideAnim(m, onDone).start());
                break;
            case FLYING_JUMP:
            case JUMP:
                // TODO: Flying jump sometimes capture
                if (m.hasCaptured() && !game.isCaptureAtEndEnabled()) {
                    startCapturedAnimation(m, null);
                }
                animations.add(new JumpAnim(m, onDone).start());
                break;
            case STACK:
                animations.add(new StackAnim(m.getStart(), m.getPlayerNum(), PieceType.CHECKER, onDone).start());
                break;
            case SWAP:
            default:
                if (onDone != null) {
                    onDone.run();
                    invalidate();
                }
                break;
        }
        invalidate();
    }

    private void onDragEnd(int curRank, int curCol) {
        for (final Move m : dragging.getMoves()) {
            if (m.hasEnd() && m.getEnd()[0] == curRank && m.getEnd()[1] == curCol) {
                switch (m.getMoveType()) {
                    case SLIDE:
                        break;
                    case FLYING_JUMP:
                    case JUMP: {
                        if (m.hasCaptured() && !game.isCaptureAtEndEnabled()) {
                            startCapturedAnimation(m, null);
                        }
                        break;
                    }
                    case CASTLE:
                        animations.add(new JumpAnim(m.getCastleRookStart(), m.getCastleRookEnd(), m.getPlayerNum(), PieceType.ROOK, null).start());
                        break;
                    case END:
                        continue;
                }
                game.executeMove(m);
                break;
            }
        }
    }

    private final Set<Piece> hidden = new HashSet<>();

    // track next place a captured piece will go so the animation
    // looks good.
    private float nextNearCaptureX = 0;
    private float nextNearCaptureY = 0;
    private float nextFarCaptureX = 0;
    private float nextFarCaptureY = 0;

    @Override
    protected void onDraw(Canvas canvas) {

        float width = getWidth();
        float height = getHeight();

        final int COLUMNS = game == null ? 8 : game.COLUMNS;
        final int RANKS = game == null ? 8 : game.RANKS;
        final float dim = Math.min(width, height);

        cellDim = Math.round(dim / COLUMNS);
        pcRad = Math.min(cellDim / 3, cellDim / 3);

        if (getBackground() != null) {
            getBackground().setBounds(0, 0, Math.round(width), Math.round(height));
            getBackground().draw(canvas);
        } else {
            pFill.setColor(Color.GREEN);
            canvas.drawRect(0f, 0f, width, height, pFill);
        }

        if (game == null)
            drawCheckerboard(canvas, width, height, dim, RANKS, COLUMNS);
        else {
            switch (game.getBoardType()) {
                case DAMA:
                    drawDamaboard(canvas, width, height, dim, RANKS, COLUMNS);
                    break;
                default:
                    drawCheckerboard(canvas, width, height, dim, RANKS, COLUMNS);
            }

            drawCapturedPieces(canvas, width, height, dim);
            drawBoardPieces(canvas, width, height, dim, RANKS, COLUMNS);
        }
    }

    private void drawDamaboard(Canvas canvas, float width, float height, float dim, int RANKS, int COLUMNS) {
        boardPadding = 0;
        pFill.setColor(getResources().getColor(R.color.dama_board));
        canvas.drawRect(0, 0, dim, dim, pFill);
        pStroke.setColor(Color.BLACK);
        pStroke.setStrokeWidth(getResources().getDimension(R.dimen.dama_line));
        for (int i=0; i<=RANKS; i++) {
            canvas.drawLine(i*cellDim, 0, i*cellDim, dim, pStroke);
        }
        for (int i=0; i<=COLUMNS; i++) {
            canvas.drawLine(0, i*cellDim, dim, i*cellDim, pStroke);
        }
    }

    private void drawCheckerboard(Canvas canvas, float width, float height, float dim, int RANKS, int COLUMNS) {

        int iDim = Math.round(dim);
        // draw simple checkerboard
        if (COLUMNS != 8 || RANKS != 8) {
            boardPadding = 0;
            pFill.setColor(Color.GRAY);
            canvas.drawRect(0f, 0f, dim, dim, pFill);

            pFill.setColor(DroidUtils.ORANGE);

            for (int i = 0; i < COLUMNS; i++) {
                for (int ii = i % 2; ii < RANKS; ii += 2) {
                    float x = i * cellDim;
                    float y = ii * cellDim;
                    canvas.drawRect(x, y, x + cellDim, y + cellDim, pFill);
                }
            }
        } else {
            // Draw cb from bitmap resource
            Drawable board = getResources().getDrawable(R.drawable.wood_checkerboard_8x8);
            board.setBounds(0, 0, iDim, iDim);
            board.draw(canvas);
            float ratio = 25f / 548;
            boardPadding = dim * ratio;
        }
    }

    private void drawCapturedPieces(Canvas canvas, float width, float height, float dim) {
        final int iDim = Math.round(dim);
        final List<Piece> captured = game.getCapturedPieces();
        final float padding = cellDim / 8;
        nextNearCaptureY = height - iDim / 2 + cellDim / 2 + padding;
        nextFarCaptureY = cellDim / 2 + padding;
        float xnearpacing = padding;
        float xfarspacing = padding;
        float sxnear = iDim + cellDim / 2;
        float sxfar = sxnear;
        nextNearCaptureX = sxnear;
        nextFarCaptureX = nextNearCaptureX;
        final float maxX = width - cellDim/4;
        for (Piece p : captured) {
            boolean upsidedown = shouldDrawUpsideDown(-1) && p.getPlayerNum() == ACheckboardGame.NEAR;
            switch (p.getPlayerNum()) {
                case ACheckboardGame.FAR:
                    drawPieceAt(canvas, p, nextNearCaptureX, nextNearCaptureY, OUTLINE_NONE, upsidedown);
                    nextNearCaptureX += padding*2;
                    if (nextNearCaptureX > maxX) {
                        sxnear += xnearpacing;
                        xnearpacing *= -1;
                        nextNearCaptureX = sxnear;
                    }
                    nextNearCaptureY += padding;
                    break;
                case ACheckboardGame.NEAR:
                    drawPieceAt(canvas, p, nextFarCaptureX, nextFarCaptureY, OUTLINE_NONE, upsidedown);
                    nextFarCaptureX += padding*2;
                    if (nextFarCaptureX > maxX) {
                        sxfar += xfarspacing;
                        xfarspacing *= -1;
                        nextFarCaptureX = sxfar;
                    }
                    nextFarCaptureY += padding;
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonFar:
            case R.id.buttonNear:
                bNear.setEnabled(false);
                bFar.setEnabled(false);
                game.executeMove((Move)view.getTag());
                break;
            case R.id.buttonStart:
                bStart.setVisibility(View.GONE);
                switch (game.getTurn()) {
                    case ACheckboardGame.FAR:
                        bFar.setEnabled(false);
                        bNear.setEnabled(false);
                        break;
                    case ACheckboardGame.NEAR:
                        bFar.setEnabled(false);
                        bNear.setEnabled(false);
                        break;
                }
                postDelayed(this, 200);
                break;
        }
    }

    String timerText(int seconds) {
        int mins = seconds/60;
        seconds -= mins*60;
        return String.format("%d:%02d", mins, seconds);
    }

    @Override
    public void run() {
        if (game == null)
            return;
        if (!(game instanceof Chess))
            return;
        Chess chess = (Chess)game;
        chess.timerTick(SystemClock.uptimeMillis());
        bFar.setText(timerText(chess.getTimerFar()));
        bNear.setText(timerText(chess.getTimerNear()));
        Move m;
        if (game.computeMoves() == 1 && (m=game.getMoves().iterator().next()).getMoveType() == MoveType.END) {
            switch (chess.getTurn()) {
                case ACheckboardGame.FAR:
                    bFar.setEnabled(true);
                    bFar.setTag(m);
                    break;
                case ACheckboardGame.NEAR:
                    bNear.setEnabled(true);
                    bNear.setTag(m);
                    break;
            }
        } else {
            bNear.setEnabled(false);
            bFar.setEnabled(false);
        }
        if (chess.isTimerExpired()) {
            chess.forfeit();
        } else {
            postDelayed(this, 200);
        }
        invalidate();
    }

    private void drawBoardPieces(Canvas canvas, float width, float height, float dim, int RANKS, int COLUMNS) {
        // the dawable is 530x530. The wood trim is 30px
        canvas.save();
        //canvas.translate(0, 4);
        try {
            canvas.translate(boardPadding, boardPadding);//boardPadding*0.1f);
            final float ddim = dim - boardPadding * 2;

            // debug outline the playable part of the
            //canvas.drawRect(0, 0, dim, dim, pStroke);

            cellDim = ddim / COLUMNS;
            pcRad = Math.min(cellDim / 3, cellDim / 3);

            if (drawDebugInfo) {
                for (int i = 0; i < COLUMNS; i++) {
                    String txt = String.valueOf(i);
                    canvas.drawText(txt, 0, txt.length(), 5 + i * cellDim, cellDim - 5, pText);
                }

                for (int i = 1; i < RANKS; i++) {
                    String txt = String.valueOf(i);
                    canvas.drawText(txt, 0, txt.length(), 5, cellDim - 5 + i * cellDim, pText);
                }
            }

            if (tapped != null && tapped.getPlayerNum() != game.getTurn())
                tapped = null;

            // Draw the pieces and see if there is only one possible piece that can be moved.
            // If so, then that piece is automatically in the 'tapped' state
            Piece mainPc = null; // non-null if only movable piece
            int numMvblePcs = 0;
            for (int i = 0; i < COLUMNS; i++) {
                for (int ii = 0; ii < RANKS; ii++) {
                    if (dragging != null && touchColumn == i && touchRank == ii)
                        continue;
                    Piece pc = game.getPiece(ii, i);
                    if (pc.getType() != PieceType.EMPTY && !hidden.contains(pc)) {
                        int outline = OUTLINE_NONE;
                        if (dragging == null && tapped == null && pc.getNumMoves() > 0) {
                            outline = OUTLINE_YELLOW;
                        } else if (pc.isCaptured()) {
                            outline = OUTLINE_RED;
                        }
                        drawPiece(canvas, pc, ii, i, outline, shouldDrawUpsideDown(ACheckboardGame.FAR));
                        if (pc.getNumMoves() > 0) {
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

            ArrayList<AAnimation<Canvas>> anims = new ArrayList<>(animations);

            if (anims.size() > 0) {
                boolean allDone = true;
                for (AAnimation<Canvas> a : anims) {
                    if (!a.update(canvas))
                        allDone = false;
                }
                invalidate();
                if (!allDone)
                    return;
            }

            animations.clear();
            hidden.clear();

            if (highlightMove != null) {
                highlightMove(canvas, highlightMove);
            } else if (mainPc != null && mainPc.getPlayerNum() == game.getTurn()) {
                tapped = mainPc;
            }

            if (tapped != null && tapped.getPlayerNum() == game.getTurn()) {
                for (Move m : tapped.getMoves()) {
                    Log.d("CB", "Tapped move: " + m);
                    highlightMove(canvas, m);
                }
            }

            if (dragging != null) {
                drawPieceAt(canvas, dragging, dragX, dragY, OUTLINE_NONE, shouldDrawUpsideDown(ACheckboardGame.FAR));
                for (Move m : dragging.getMoves()) {
                    highlightMove(canvas, m);
                }
            }
        } finally {
            canvas.restore();
        }
    }

    int getPcColor(int playerNum) {
        switch (game.getPlayerColor(playerNum)) {
            case BLACK:
                return Color.rgb(64, 64, 64);
            case RED:
                return Color.RED;
            case WHITE:
                return Color.WHITE;
        }
        return 0;
    }

    private void highlightMove(Canvas canvas, Move m) {
        if (m.getStart() == null)
            return;
        float sx = m.getStart()[1]*cellDim;
        float sy = m.getStart()[0]*cellDim;
        float cx, cy;
        switch (m.getMoveType()) {
            case CASTLE:
                pStroke.setColor(Color.WHITE);
                cx = m.getCastleRookStart()[1]*cellDim;
                cy = m.getCastleRookStart()[0]*cellDim;
                canvas.drawRect(cx, cy, cx+cellDim, cy+cellDim, pStroke);
                cx = m.getCastleRookEnd()[1]*cellDim;
                cy = m.getCastleRookEnd()[0]*cellDim;
                canvas.drawRect(cx, cy, cx+cellDim, cy+cellDim, pStroke);

            case FLYING_JUMP:
            case JUMP:
            case SLIDE:
                pStroke.setColor(Color.GREEN);
                canvas.drawRect(sx, sy, sx+cellDim, sy+cellDim, pStroke);
                pStroke.setColor(Color.YELLOW);
                cx = m.getEnd()[1]*cellDim;
                cy = m.getEnd()[0]*cellDim;
                canvas.drawRect(cx, cy, cx+cellDim, cy+cellDim, pStroke);
                break;
            case SWAP:
            case STACK:
                pStroke.setColor(Color.BLUE);
                canvas.drawRect(sx, sy, sx+cellDim, sy+cellDim, pStroke);
                break;
        }
        if (m.hasEnd()) {
            cx = m.getEnd()[1]*cellDim;
            cy = m.getEnd()[0]*cellDim + cellDim/2;
            if (drawDebugInfo) {
                canvas.drawText(!m.hasEnd() ? "null" : m.getEndType().name(), cx, cy, pText);
                if (m.getCaptured() != null) {
                    cy += pText.getTextSize();
                    canvas.drawText(m.getCapturedType().name() + " " + m.getCaptured()[0] + "x" + m.getCaptured()[1], cx, cy, pText);
                }
            }
        }
    }

    boolean isSquareBlack(int rank, int col) {
        if (rank % 2 == 0) {
            return col % 2 == 1;
        }
        return col % 2 == 0;
    }

    final static int OUTLINE_NONE = 0;
    final static int OUTLINE_RED = Color.RED;
    final static int OUTLINE_YELLOW = Color.YELLOW;

    void drawPiece(Canvas g, Piece pc, int rank, int col, int outline, boolean upsidedown) {
        float cx = col * cellDim + cellDim / 2;
        float cy = rank * cellDim + cellDim / 2;
        drawPieceAt(g, pc, cx, cy, outline, upsidedown);
        if (drawDebugInfo) {
            if (isSquareBlack(rank, col))
                g.drawText(pc.getType().name(), cellDim * col, cellDim * rank + cellDim - 5, pText);
            else
                g.drawText(pc.getType().name(), cellDim * col, cellDim * rank + pText.getTextSize() + 5, pText);
        }
    }

    /*
    Draw piece centered at cx, cy
     */
    void drawPieceAt(Canvas g, Piece pc, float cx, float cy, int outline, boolean upsidedown) {

        Drawable d = null;
        boolean isBlack = game.getPlayerColor(pc.getPlayerNum()) == ACheckboardGame.Color.BLACK;
        float heightPercent = 1; // adjust height for pieces. so pawn is shortest and king is tallest and the rest reasonably inbetween
        switch (pc.getType()) {
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_pawn);
                else
                    d = getResources().getDrawable(R.drawable.wt_pawn);
                heightPercent = 0.7f;
                break;
            case BISHOP:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_bishop);
                else
                    d = getResources().getDrawable(R.drawable.wt_bishop);
                heightPercent = 0.8f;
                break;
            case KNIGHT:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_knight);
                else
                    d = getResources().getDrawable(R.drawable.wt_knight);
                heightPercent = 0.8f;
                break;
            case ROOK_IDLE:
            case ROOK:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_rook);
                else
                    d = getResources().getDrawable(R.drawable.wt_rook);
                heightPercent = 0.8f;
                break;
            case QUEEN:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_queen);
                else
                    d = getResources().getDrawable(R.drawable.wt_queen);
                heightPercent = 0.9f;
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
                outline = OUTLINE_RED;
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_king);
                else
                    d = getResources().getDrawable(R.drawable.wt_king);
                break;
            case DAMA_KING:
            case FLYING_KING:
            case KING:
                drawChecker(g, cx, cy, pcRad, pc.getPlayerNum(), 0);
                cy -= pcRad/4;
                // fall through
            case DAMA_MAN:
            case CHECKER:
                drawChecker(g, cx, cy, pcRad, pc.getPlayerNum(), outline);
                break;
        }
        if (d != null) {
            drawBitmap(g, cx, cy, d, heightPercent, outline, upsidedown);
        }
    }

    private float [] getBitmapRect(BitmapDrawable d, float heightPercent) {
        float dw = d.getIntrinsicHeight();
        float dh = d.getIntrinsicWidth();
        float aspect = dw/dh;
        float cellAspect = cellDim/cellDim;

        float padding = getPadding();

        float w,h;
        if (aspect > cellAspect) {
            // the image is 'tall' so compute actual height is cellHeight and width is the aspect of that
            h = heightPercent * (cellDim - padding*2);
            w = h/aspect;
        } else {
            // the image is 'fat'
            h = heightPercent * (cellDim - padding*2);
            w = h * aspect;
        }

        return new float [] { w,h }; //w), Math.round(h));
        //float sy = cy + cellDim/2 - padding;
        //Rect dest = new Rect(Math.round(cx - w / 2), Math.round(sy - h), Math.round(cx + w / 2), Math.round(sy));
    }

    private float getPadding() {
        return cellDim/20;
    }

    public static Bitmap flip(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    private void drawBitmap(Canvas g, float cx, float cy, Drawable d, float heightPercent, int outline, boolean upsidedown) {

        BitmapDrawable bd = (BitmapDrawable)d;
        float [] dim = getBitmapRect(bd, heightPercent);
        g.save();
        g.translate(cx, cy);
        float w = dim[0];
        float h = dim[1];
        Rect dest = null;
        dest = new Rect(Math.round(- w / 2), Math.round(- h / 2), Math.round(w / 2), Math.round(h / 2));
        if (upsidedown)
            g.rotate(180);
        if (outline != OUTLINE_NONE) {
            g.save();
            g.scale(1.2f, 1.1f);
            g.drawBitmap(bd.getBitmap(), null, dest, outline == OUTLINE_RED ? glowRed : glowYellow);
            g.restore();
        }
        d.setBounds(dest);
        d.draw(g);
        g.restore();
    }

    void drawChecker(Canvas g, float x, float y, float rad, int playerNum, int outlineColor) {
        Drawable d;
        if (game.getPlayerColor(playerNum) == ACheckboardGame.Color.BLACK) {
           d = getResources().getDrawable(R.drawable.blk_checker);
        } else {
            d = getResources().getDrawable(R.drawable.red_checker);
        }

        drawBitmap(g, x, y, d, 0.75f, outlineColor, false);


        //Drawable d = getResources().getDrawable(color == )

    }
    void drawChecker_old(Canvas g, float x, float y, float rad, int color, int outlineColor) {
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

    /**
     * Draw disk centered at cx, cy with given radius
     * @param c
     * @param p
     * @param cx
     * @param cy
     * @param rad
     */
    void drawDisk(Canvas c, Paint p, float cx, float cy, float rad) {
        rf.set(cx-rad, cy-rad, cx+rad, cy+rad);
        c.drawOval(rf, p);
    }

    public final void undo() {
        CheckmateAnim cm = null;
        for (AAnimation<Canvas> a : animations) {
            if (a instanceof CheckmateAnim)
                cm = (CheckmateAnim)a;
            a.stop();
        }
        animations.clear();

        // remember the captured state so we know to run reverse animation...whew!
        boolean [][] tmpB = new boolean [game.RANKS][game.COLUMNS];
        for (int r=0; r<game.RANKS; r++) {
            for (int c=0; c<game.COLUMNS; c++) {
                tmpB[r][c] = game.getPiece(r, c).isCaptured();
            }
        }

        final Move m = game.undo();

        if (cm != null) {
            animations.add(new CheckmateAnim(cm.pos, cm.drawable, false).startReverse());
        }

        if (m != null) {
            if (m.hasCaptured() && !tmpB[m.getCaptured()[0]][m.getCaptured()[1]]) {
                // start un-capture animation on next frame so the nextNear/FarCapturedX/Y are reflect updated state
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startCapturedAnimation(m, null, true);
                    }
                }, 1);
            }
            switch (m.getMoveType()) {
                case FLYING_JUMP:
                case JUMP:
                    animations.add(new JumpAnim(m, null).startReverse());
                    break;
                case SLIDE:
                    animations.add(new SlideAnim(m, null).startReverse());
                    break;
                case STACK:
                    animations.add(new StackAnim(m.getStart(), m.getPlayerNum(), PieceType.CHECKER, null).startReverse());
                    break;
                case CASTLE:
                    animations.add(new JumpAnim(m, null).startReverse());
                    animations.add(new SlideAnim(m.getCastleRookStart(), m.getCastleRookEnd(), m.getPlayerNum(), PieceType.ROOK, null).startReverse());
                    break;
            }
        }
        tapped = null;
        dragging = null;
        invalidate();
    }

    public void reset() {
        animations.clear();
        hidden.clear();
        highlightMove(null);
    }

    public void highlightMove(Move m) {
        this.highlightMove = m;
        this.tapped = null;
    }
}
