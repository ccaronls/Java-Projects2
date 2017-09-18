package cc.android.checkerboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import java.io.File;
import java.util.List;

import cc.lib.android.DroidUtils;
import cc.lib.android.GLColor;
import cc.lib.game.Utils;
import cc.lib.net.FormElem;
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

        if (game == null)
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

    private void doTap(int curRank, int curCol) {
        if (tapped == null) {
            tapped = game.getPiece(curRank, curCol);
            if (tapped.moves.size() == 0)
                tapped = null;
        } else {
            for (Move m : tapped.moves) {
                if (m.endRank==curRank && m.endCol == curCol) {
                    // TODO: start anims here
                    game.executeMove(m);
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
                //game.loadFromFile(saveFile);
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
