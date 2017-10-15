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
public class CheckerboardView extends View implements View.OnClickListener {

    private final Paint pFill = new Paint();
    private final Paint pStroke = new Paint();
    private final Paint pText = new Paint();
    private final RectF rf = new RectF();
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        ColorFilter filter = new PorterDuffColorFilter(0xFFFF0000, PorterDuff.Mode.SRC_IN);
        BlurMaskFilter blur = new BlurMaskFilter(300, BlurMaskFilter.Blur.OUTER);

        glow.setColorFilter(filter);
        glow.setMaskFilter(blur);

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
        final int rank, col, playerNum;
        final PieceType type;

        public StackAnim(Move move, Runnable whenDone, int playerNum, int rank, int col, PieceType pt) {
            super(1600, move, whenDone, game.getPiece(rank, col));
            this.rank = rank;
            this.col = col;
            this.playerNum = playerNum;
            this.type = pt;
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
            //Piece p = game.getPiece(rank, col);
            //drawPiece(g, p, rank, col, false);
            //if (p.type == PieceType.KING)
            //    drawChecker(g, sx, sy, pcRad, 1, playerNum, getPcColor(playerNum), 0);
            g.save();
            g.translate(x, y);
            g.scale(scale, scale);
            //drawChecker(g, 0, 0, pcRad, stacks, playerNum, getPcColor(playerNum), 0);
            drawPieceAt(g, new Piece(playerNum, type), 0f, 0f, false);
            g.restore();
        }

    }

    class SlideAnim extends MoveAnim {

        public SlideAnim(Move move, Runnable whenDone) {
            super(800, move, whenDone, game.getPiece(move.getStart()));//.startRank, move.startCol));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            float sx = cellW * move.getStart()[1] + cellW/2;
            float sy = cellH * move.getStart()[0] + cellH/2;
            float ex = cellW * move.getEnd()[1] + cellW/2;
            float ey = cellH * move.getEnd()[0] + + cellH/2;
            int x = Math.round(sx + (ex-sx) * position);
            int y = Math.round(sy + (ey-sy) * position);
            Piece p = game.getPiece(move.getStart());//.startRank, move.startCol);
            drawPieceAt(g, p, x, y, false);
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
            float dist = cellH * getDir(playerNum);
            IVector2D [] v = {
                    new Vector2D(sx, sy),
                    new Vector2D(midx1, midy1+dist),
                    new Vector2D(midx2, midy2+dist),
                    new Vector2D(ex, ey),
            };
            return v;
        }

        public JumpAnim(int [] start, int [] end, int playerNum, Runnable whenDone) {
            super(1200, null, whenDone, game.getPiece(start));
            curve = new Bezier(computeJumpPoints(start, end, playerNum));
        }

        public JumpAnim(Move move, Runnable whenDone) {
            super(1200, move, whenDone, game.getPiece(move.getStart()));//.startRank, move.startCol));
            curve = new Bezier(computeJumpPoints(move.getStart(), move.getEnd(), move.playerNum));
        }

        @Override
        public void draw(Canvas g, float position, float dt) {
            Vector2D v = curve.getPointAt(position);
            Piece p = game.getPiece(move.getStart());//.startRank, move.startCol);
            drawPieceAt(g, p, v.X(), v.Y(), false);
        }

    };

    // TODO: Another way would be to pull up a picker from a (long?) touch but how to cancel?
    private void onTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.moves.size() == 0)
                tapped = null;
        } else if (tapped.type == PieceType.PAWN_TOSWAP) {
            Piece p = game.getPiece(curRank, curCol);
            for (int i=0; i<p.moves.size(); i++) {
                Move m = p.moves.get(i);
                if (m.nextType == p.type) {
                    int ii = (i+1)%p.moves.size();
                    p.type = p.moves.get(ii).nextType;
                    return;
                }
            }
            p.type = p.moves.get(0).nextType;
        } else {
            for (Move m : tapped.moves) {
                if (m.type == MoveType.END)
                    continue; // ignore since the ui button will handle
                if (m.squares.length > 1 && m.getEnd()[0]==curRank && m.getEnd()[1] == curCol) {
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
            case CASTLE: {
                animations.add(new JumpAnim(m.getCastleRookStart(), m.getCastleRookEnd(), m.playerNum, null));
            }

            case SLIDE:
                animations.add(new SlideAnim(m, onDone).start());
                break;
            case JUMP:
                if (m.captured != null)
                    animations.add(new StackAnim(m, null, m.captured.playerNum, m.getCaptured()[0], m.getCaptured()[1], m.captured.type).startReverse());
                animations.add(new JumpAnim(m, onDone).start());
                break;
            case STACK:
                animations.add(new StackAnim(m, onDone, m.playerNum, m.getStart()[0], m.getStart()[1], PieceType.CHECKER).start());
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
            if (m.squares.length > 1 && m.getEnd()[0] == curRank && m.getEnd()[1] == curCol) {
                switch (m.type) {
                    case SLIDE:
                    case JUMP: {
                        if (m.captured != null)
                            animations.add(new StackAnim(m, null, m.captured.playerNum, m.getCaptured()[0], m.getCaptured()[1], m.captured.type).startReverse());
                        break;
                    }
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
                if (pc.type != PieceType.EMPTY && !hidden.contains(pc)) {
                    boolean outline = dragging == null && tapped == null && pc.moves.size() > 0;
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

        if (anims.size() > 0) {
            for (AAnimation<Canvas> a : anims)
                a.update(canvas);
            invalidate();
            return;
        }

        if (highlightMove != null) {
            float sx = highlightMove.getStart()[1]*cellW;
            float sy = highlightMove.getStart()[0]*cellH;
            float ex = highlightMove.getEnd()[1]*cellW;
            float ey = highlightMove.getEnd()[0]*cellH;
            switch (highlightMove.type) {
                case JUMP:
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
        } else if (mainPc != null && mainPc.playerNum == game.getTurn()) {
            tapped = mainPc;
        }

        if (tapped != null && tapped.playerNum == game.getTurn()) {
            for (Move m : new ArrayList<>(tapped.moves)) {
                Log.d("CB", "Tapped move: " + m);
                float sx = m.getStart()[1]*cellW;
                float sy = m.getStart()[0]*cellH;
                float ex=0, ey=0;
                switch (m.type) {
                    case END:
                        break;
                    case CASTLE:
                        pStroke.setColor(Color.YELLOW);
                        ex = m.getCastleRookStart()[1] * cellW;
                        ey = m.getCastleRookStart()[0] * cellH;
                        canvas.drawRect(ex, ey, ex + cellW, ey + cellH, pStroke);
                        ex = m.getCastleRookStart()[1] * cellW;
                        ey = m.getCastleRookStart()[0] * cellH;
                        canvas.drawRect(ex, ey, ex + cellW, ey + cellH, pStroke);

                    case JUMP:
                    case SLIDE:
                        ex = m.getEnd()[1] * cellW;
                        ey = m.getEnd()[0] * cellH;
                        pStroke.setColor(Color.GREEN);
                        canvas.drawRect(sx, sy, sx + cellW, sy + cellH, pStroke);
                        pStroke.setColor(Color.YELLOW);
                        canvas.drawRect(ex, ey, ex + cellW, ey + cellH, pStroke);
                        break;
                    case SWAP:
                    case STACK:
                        pStroke.setColor(Color.GREEN);
                        drawDisk(canvas, pStroke, sx + cellW / 2, sy + cellH / 2, pcRad);
                        break;
                }
            }
        }

        if (dragging != null) {
            drawPieceAt(canvas, dragging, dragX, dragY, false);
            for (Move m : dragging.moves) {
                float sx = m.getStart()[1]*cellW;
                float sy = m.getStart()[0]*cellH;
                switch (m.type) {
                    case END:
                        break;
                    case JUMP:
                    case SLIDE:
                        pStroke.setColor(Color.YELLOW);
                        float ex = m.getEnd()[1]*cellW;
                        float ey = m.getEnd()[0]*cellH;
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

    String [] names = {
            "BLACK",
            "RED",
    };

    int getPcColor(int playerNum) {
        if (playerNum >= 0)
            return playerColors[playerNum];
        return 0;
    }

    String getPcColorName(int playerNum) {
        int c = getPcColor(playerNum);
        int index=0;
        for (int pc : playerColors) {
            if (pc == c)
                return names[index];
            index++;
        }
        throw new AssertionError();
    }

    boolean isSquareBlack(int rank, int col) {
        if (rank % 2 == 0) {
            return col % 2 == 1;
        }
        return col % 2 == 0;
    }

    void drawPiece(Canvas g, Piece pc, int rank, int col, boolean outlined) {
        float cx = col * cellW + cellW / 2;
        float cy = rank * cellH + cellH / 2;
        drawPieceAt(g, pc, cx, cy, outlined);
        if (isSquareBlack(rank, col))
            g.drawText(pc.type.name(), cellW * col, cellH * rank + cellH - 5, pText);
        else
            g.drawText(pc.type.name(), cellW * col, cellH * rank + pText.getTextSize() + 5, pText);
    }

    void drawPieceAt(Canvas g, Piece pc, float cx, float cy, boolean outlined) {

        Drawable d = null;
        switch (pc.type) {
            case PAWN:
            case PAWN_IDLE:
            case PAWN_ENPASSANT:
            case PAWN_TOSWAP:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_pawn);
                else
                    d = getResources().getDrawable(R.drawable.wt_pawn);
                break;
            case BISHOP:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_bishop);
                else
                    d = getResources().getDrawable(R.drawable.wt_bishop);
                break;
            case KNIGHT:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_knight);
                else
                    d = getResources().getDrawable(R.drawable.wt_knight);
                break;
            case ROOK_IDLE:
            case ROOK:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_rook);
                else
                    d = getResources().getDrawable(R.drawable.wt_rook);
                break;
            case QUEEN:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_queen);
                else
                    d = getResources().getDrawable(R.drawable.wt_queen);
                break;
            case CHECKED_KING:
            case CHECKED_KING_IDLE:
            case UNCHECKED_KING:
            case UNCHECKED_KING_IDLE:
                if (pc.playerNum == ACheckboardGame.BLACK)
                    d = getResources().getDrawable(R.drawable.bk_king);
                else
                    d = getResources().getDrawable(R.drawable.wt_king);
                break;
            case KING:
                drawChecker(g, cx, cy, pcRad, getPcColor(pc.playerNum), 0);
                cy -= pcRad/4;
                // fall through
            case CHECKER:
                drawChecker(g, cx, cy, pcRad, getPcColor(pc.playerNum), outlined ? Color.YELLOW : 0);
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
            if (outlined) {
                g.save();
                g.scale(1.2f, 1.1f, cx, cy);
                g.drawBitmap(bd.getBitmap(), null, dest, glow);
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

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonEndTurn) {
            Move move = (Move)v.getTag();
            if (move == null)
                throw new AssertionError();
            game.executeMove(move);
        } else if (v.getId() == R.id.buttonNewGame) {
            game.newGame();
        }
        this.dragging = this.tapped = null;
        invalidate();
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
                        animations.add(new StackAnim(m, null, m.captured.playerNum, m.getCaptured()[0], m.getCaptured()[1], m.captured.type).start());
                    animations.add(new JumpAnim(m, null).startReverse());
                    break;
                case SLIDE:
                    animations.add(new SlideAnim(m, null).startReverse());
                    break;
                case STACK:
                    animations.add(new StackAnim(m, null, m.playerNum, m.getStart()[0], m.getStart()[1], PieceType.CHECKER).startReverse());
                    break;
            }
        }
        tapped = null;
        dragging = null;
        invalidate();
    }
/*
    public boolean isEndTurnButtonAvailable() {
        Piece lock = game.getLocked();
        if (lock != null) {
            if (lock.playerNum != game.getTurn())
                throw new AssertionError();
            for (Move m : lock.moves) {
                if (m.type == MoveType.END) {
                    return true;
                }
            }
        }
        return false;
    }
*/
    public void highlightMove(Move m) {
        this.highlightMove = m;
        this.tapped = null;
    }
}
