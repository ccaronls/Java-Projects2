package cc.android.checkerboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
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
public class CheckerboardView extends View {

    private final Paint pFill = new Paint();
    private final Paint pStroke = new Paint();
    private final Paint pText = new Paint();
    private final RectF rf = new RectF();
    private final Paint glowYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowRed = new Paint(Paint.ANTI_ALIAS_FLAG);
    boolean drawDebugInfo = false;

    private ACheckboardGame game = null;

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
        invalidate();
    }

    public final ACheckboardGame getGame() {
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
        ColorFilter filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        BlurMaskFilter blur = new BlurMaskFilter(300, BlurMaskFilter.Blur.OUTER);

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

    private abstract class MoveAnim extends AAnimation<Canvas> {
        final Runnable whenDone;
        final int [] start;
        final int [] end;
        final int playerNum;
        final PieceType pType;

        MoveAnim(long duration, int [] start, int [] end, int playerNum, PieceType pType, Runnable whenDone, Piece ... toHide) {
            super(duration, 0);
            this.start = start;
            this.end = end;
            this.playerNum = playerNum;
            this.pType = pType;
            this.whenDone = whenDone;
            hidden.addAll(Arrays.asList(toHide));
        }

        MoveAnim(long duration, Move move, Runnable whenDone, Piece ... toHide) {
            this(duration, move.getStart(), move.hasEnd() ? move.getEnd() : move.getStart(), move.playerNum, game.getPiece(move.getStart()).type, whenDone, toHide);
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
            super(1200, pos, null, playerNum, pt, whenDone, game.getPiece(pos));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            int rank = start[0];
            int col = start[1];
            float sx = cellW * col + cellW/2;
            float sy = rank == 0 ? 0 : getHeight();//0;//cellH * move.startRank + cellH/2;
            float ex = cellW * col + cellW/2;
            float ey = cellH * rank + cellH/2;
            float scale = 1 + (1-position);
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            g.save();
            g.translate(x, y);
            g.scale(scale, scale);
            drawPieceAt(g, new Piece(playerNum, pType), 0f, 0f, OUTLINE_NONE);
            g.restore();
        }

    }

    class SlideAnim extends MoveAnim {

        public SlideAnim(Move move, Runnable whenDone) {
            this(move.getStart(), move.getEnd(), move.playerNum, game.getPiece(move.getStart()).type, whenDone);
        }

        public SlideAnim(int[] start, int []end, int playerNum, PieceType pt, Runnable whenDone) {
            super(800, start, end, playerNum, pt, whenDone, game.getPiece(start));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            float sx = cellW * start[1] + cellW/2;
            float sy = cellH * start[0] + cellH/2;
            float ex = cellW * end[1] + cellW/2;
            float ey = cellH * end[0] + cellH/2;
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = new Piece(playerNum, pType);
            drawPieceAt(g, p, x, y, OUTLINE_NONE);
        }
    }

    class JumpAnim extends MoveAnim {

        final Bezier curve;

        private IVector2D[] computeJumpPoints(int [] start, int [] end, int playerNum) {

            float sx = cellW * start[1] + cellW/2;
            float sy = cellW * start[0] + cellH/2;
            float ex = cellH * end[1]   + cellW/2;
            float ey = cellH * end[0]   + cellH/2;

            float midx1 = sx + ((ex-sx) / 3);
            float midx2 = sx + ((ex-sx) * 2 / 3);
            float midy1 = sy + ((ey-sy) / 3);
            float midy2 = sy + ((ey-sy) * 2 / 3);
            float dist = cellH * -1;//getDir(playerNum);
            IVector2D [] v = {
                    new Vector2D(sx, sy),
                    new Vector2D(midx1, midy1+dist),
                    new Vector2D(midx2, midy2+dist),
                    new Vector2D(ex, ey),
            };
            return v;
        }

        public JumpAnim(int [] start, int [] end, int playerNum, PieceType pt, Runnable whenDone) {
            super(1200, start, end, playerNum, pt, whenDone, game.getPiece(start));
            curve = new Bezier(computeJumpPoints(start, end, playerNum));
        }

        public JumpAnim(Move move, Runnable whenDone) {
            super(1200, move, whenDone, game.getPiece(move.getStart()));
            curve = new Bezier(computeJumpPoints(start, end, playerNum));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            Vector2D v = curve.getPointAt(position);
            Piece p = new Piece(playerNum, pType);
            drawPieceAt(g, p, v.X(), v.Y(), OUTLINE_NONE);
        }

    };

    // TODO: Another way would be to pull up a picker from a (long?) touch but how to cancel?
    private void onTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.moves.size() == 0)
                tapped = null;
        } else {
            Piece swappy = null;
            for (Move m : tapped.moves) {
                switch (m.type) {
                    case END:
                        continue;
                    case CASTLE:
                    case SLIDE:
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
                            if (p.type == PieceType.PAWN_TOSWAP) {
                                p.type = m.nextType;
                            } else if (p.type == m.nextType) {
                                p.type = PieceType.PAWN_TOSWAP; // this makes so next iteration will
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
                if (swappy.type == PieceType.PAWN_TOSWAP) {
                    swappy.type = swappy.moves.get(0).nextType;
                }
            } else {
                Piece p = game.getPiece(curRank, curCol);
                if (p.moves.size() > 0) {
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
        animateMoveAndThen(m, new Runnable() {
            public void run() {
                game.executeMove(m);
            }
        });
    }

    public void animateMoveAndThen(Move m, Runnable onDone) {
        switch (m.type) {
            case CASTLE:
                animations.add(new JumpAnim(m.getCastleRookStart(), m.getCastleRookEnd(), m.playerNum, PieceType.ROOK, onDone).start());
                animations.add(new SlideAnim(m, null).start());
                break;
            case SLIDE:
                animations.add(new SlideAnim(m, onDone).start());
                break;
            case JUMP:
                if (m.captured != null)
                    animations.add(new StackAnim(m.getCaptured(), m.captured.playerNum, m.captured.type, onDone).startReverse());
                animations.add(new JumpAnim(m, onDone).start());
                break;
            case STACK:
                animations.add(new StackAnim(m.getStart(), m.playerNum, PieceType.CHECKER, onDone).start());
                break;
            case SWAP:
            default:
                if (onDone != null) {
                    post(onDone);
                    postInvalidate();
                }
                break;
        }
        invalidate();
    }

    private void onDragEnd(int curRank, int curCol) {
        for (Move m : dragging.moves) {
            if (m.hasEnd() && m.getEnd()[0] == curRank && m.getEnd()[1] == curCol) {
                switch (m.type) {
                    case SLIDE:
                    case JUMP: {
                        if (m.captured != null)
                            animations.add(new StackAnim(m.getCaptured(), m.captured.playerNum, m.captured.type, null).startReverse());
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

        pFill.setColor(Color.GRAY);
        canvas.drawRect(0f, 0f, width, height, pFill);

        pFill.setColor(DroidUtils.ORANGE);

        for (int i=0; i<COLUMNS; i++) {
            for (int ii=i%2; ii<RANKS; ii+=2) {
                float x = i*cellW;
                float y = ii*cellH;
                canvas.drawRect(x, y, x+cellW, y+cellH, pFill);
            }
        }

        if (drawDebugInfo) {
            for (int i = 0; i < COLUMNS; i++) {
                String txt = String.valueOf(i);
                canvas.drawText(txt, 0, txt.length(), 5 + i * cellW, cellH - 5, pText);
            }

            for (int i = 1; i < RANKS; i++) {
                String txt = String.valueOf(i);
                canvas.drawText(txt, 0, txt.length(), 5, cellH - 5 + i * cellH, pText);
            }
        }
        if (game == null)
            return;

        if (tapped != null && tapped.playerNum != game.getTurn())
            tapped = null;

        Piece mainPc = null;
        int numMvblePcs = 0;
        for (int i=0; i<COLUMNS; i++) {
            for (int ii=0; ii<RANKS; ii++) {
                if (dragging != null && touchColumn == i && touchRank == ii)
                    continue;
                Piece pc = game.getPiece(ii, i);
                if (pc.type != PieceType.EMPTY && !hidden.contains(pc)) {
                    int outline = OUTLINE_NONE;
                    if (dragging == null && tapped == null && pc.moves.size() > 0) {
                        outline = OUTLINE_YELLOW;
                    }
                    drawPiece(canvas, pc, ii, i, outline);
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
        } else if (mainPc != null && mainPc.playerNum == game.getTurn()) {
            tapped = mainPc;
        }

        if (tapped != null && tapped.playerNum == game.getTurn()) {
            for (Move m : new ArrayList<>(tapped.moves)) {
                Log.d("CB", "Tapped move: " + m);
                highlightMove(canvas, m);
            }
        }

        if (dragging != null) {
            drawPieceAt(canvas, dragging, dragX, dragY, OUTLINE_NONE);
            for (Move m : dragging.moves) {
                highlightMove(canvas, m);
            }
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
        float sx = m.getStart()[1]*cellW;
        float sy = m.getStart()[0]*cellH;
        float cx, cy;
        switch (m.type) {
            case CASTLE:
                pStroke.setColor(Color.WHITE);
                cx = m.getCastleRookStart()[1]*cellW;
                cy = m.getCastleRookStart()[0]*cellH;
                canvas.drawRect(cx, cy, cx+cellW, cy+cellH, pStroke);
                cx = m.getCastleRookEnd()[1]*cellW;
                cy = m.getCastleRookEnd()[0]*cellH;
                canvas.drawRect(cx, cy, cx+cellW, cy+cellH, pStroke);

            case JUMP:
            case SLIDE:
                pStroke.setColor(Color.GREEN);
                canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                pStroke.setColor(Color.YELLOW);
                cx = m.getEnd()[1]*cellW;
                cy = m.getEnd()[0]*cellH;
                canvas.drawRect(cx, cy, cx+cellW, cy+cellH, pStroke);
                break;
            case SWAP:
            case STACK:
                pStroke.setColor(Color.BLUE);
                canvas.drawRect(sx, sy, sx+cellW, sy+cellH, pStroke);
                break;
        }
        if (m.hasEnd()) {
            cx = m.getEnd()[1]*cellW;
            cy = m.getEnd()[0]*cellH + cellH/2;
            if (drawDebugInfo) {
                canvas.drawText(m.nextType == null ? "null" : m.nextType.name(), cx, cy, pText);
                if (m.captured != null) {
                    cy += pText.getTextSize();
                    canvas.drawText(m.captured.type.name() + " " + m.getCaptured()[0] + "x" + m.getCaptured()[1], cx, cy, pText);
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

    void drawPiece(Canvas g, Piece pc, int rank, int col, int outline) {
        float cx = col * cellW + cellW / 2;
        float cy = rank * cellH + cellH / 2;
        drawPieceAt(g, pc, cx, cy, outline);
        if (drawDebugInfo) {
            if (isSquareBlack(rank, col))
                g.drawText(pc.type.name(), cellW * col, cellH * rank + cellH - 5, pText);
            else
                g.drawText(pc.type.name(), cellW * col, cellH * rank + pText.getTextSize() + 5, pText);
        }
    }

    void drawPieceAt(Canvas g, Piece pc, float cx, float cy, int outline) {

        Drawable d = null;
        boolean isBlack = game.getPlayerColor(pc.playerNum) == ACheckboardGame.Color.BLACK;
        switch (pc.type) {
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_pawn);
                else
                    d = getResources().getDrawable(R.drawable.wt_pawn);
                break;
            case BISHOP:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_bishop);
                else
                    d = getResources().getDrawable(R.drawable.wt_bishop);
                break;
            case KNIGHT:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_knight);
                else
                    d = getResources().getDrawable(R.drawable.wt_knight);
                break;
            case ROOK_IDLE:
            case ROOK:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_rook);
                else
                    d = getResources().getDrawable(R.drawable.wt_rook);
                break;
            case QUEEN:
                if (isBlack)
                    d = getResources().getDrawable(R.drawable.bk_queen);
                else
                    d = getResources().getDrawable(R.drawable.wt_queen);
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
            case KING:
                drawChecker(g, cx, cy, pcRad, getPcColor(pc.playerNum), 0);
                cy -= pcRad/4;
                // fall through
            case CHECKER:
                drawChecker(g, cx, cy, pcRad, getPcColor(pc.playerNum), outline);
                break;
        }
        if (d != null) {
            // do aspect fit of bitmap onto cell
            BitmapDrawable bd = (BitmapDrawable)d;
            float dw = d.getIntrinsicHeight();
            float dh = d.getIntrinsicWidth();
            float aspect = dw/dh;
            float cellAspect = cellW/cellH;

            float w,h;
            if (aspect > cellAspect) {
                // the image is 'tall' so compute actual height is cellHeight and width is the aspect of that
                h = cellH;
                w = cellH/aspect;
             } else {
                w = cellW;
                h = cellW*aspect;
            }

            Rect dest = new Rect(Math.round(cx - w / 2), Math.round(cy - h / 2), Math.round(cx + w / 2), Math.round(cy + h / 2));
            if (outline != OUTLINE_NONE) {
                g.save();
                g.scale(1.2f, 1.1f, cx, cy);
                g.drawBitmap(bd.getBitmap(), null, dest, outline == OUTLINE_RED ? glowRed : glowYellow);
                g.restore();
            }
            d.setBounds(dest);
            d.draw(g);
        }
    }

    void drawChecker(Canvas g, float x, float y, float rad, int color, int outlineColor) {
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
        for (AAnimation<Canvas> a : animations) {
            a.stop();
        }
        animations.clear();
        Move m = game.undo();
        if (m != null) {
            switch (m.type) {
                case JUMP:
                    if (m.captured != null)
                        animations.add(new StackAnim(m.getCaptured(), m.captured.playerNum, m.captured.type, null).start());
                    animations.add(new JumpAnim(m, null).startReverse());
                    break;
                case SLIDE:
                    animations.add(new SlideAnim(m, null).startReverse());
                    break;
                case STACK:
                    animations.add(new StackAnim(m.getStart(), m.playerNum, PieceType.CHECKER, null).startReverse());
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

    public void highlightMove(Move m) {
        this.highlightMove = m;
        this.tapped = null;
    }
}
