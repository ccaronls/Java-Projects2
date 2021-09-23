package cc.android.checkerboard;

import java.io.File;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.checkerboard.Color;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.UIGame;

public class CheckerboardActivity2 extends DroidActivity {

    private File saveFile=null;

    private final UIGame game = new UIGame() {
        @Override
        public void repaint() {
            redraw();
        }

        @Override
        protected int getCheckerboardImageId() {
            return R.drawable.wood_checkerboard_8x8;
        }

        @Override
        protected int getKingsCourtBoardId() {
            return R.drawable.kings_court_board_8x8;
        }

        @Override
        public int getPieceImageId(PieceType p, Color color) {
            switch (p) {

                case PAWN:
                case PAWN_IDLE:
                case PAWN_ENPASSANT:
                case PAWN_TOSWAP:
                    if (color == Color.BLACK)
                        return R.drawable.bk_pawn;
                    else
                        return R.drawable.wt_pawn;
                case BISHOP:
                    if (color == Color.BLACK)
                        return R.drawable.bk_bishop;
                    else
                        return R.drawable.wt_bishop;
                case KNIGHT:
                    if (color == Color.BLACK)
                        return R.drawable.bk_knight;
                    else
                        return R.drawable.wt_knight;
                case ROOK:
                case ROOK_IDLE:
                case DRAGON: // TEMP until icon available
                case DRAGON_IDLE:
                    if (color == Color.BLACK)
                        return R.drawable.bk_rook;
                    else
                        return R.drawable.wt_rook;
                case QUEEN:
                    if (color == Color.BLACK)
                        return R.drawable.bk_queen;
                    else
                        return R.drawable.wt_queen;
                case CHECKED_KING:
                case CHECKED_KING_IDLE:
                case UNCHECKED_KING:
                case UNCHECKED_KING_IDLE:
                    if (color == Color.BLACK)
                        return R.drawable.bk_king;
                    else
                        return R.drawable.wt_king;
                case KING:
                case FLYING_KING:
                case CHECKER:
                case DAMA_MAN:
                case DAMA_KING:
                case CHIP_4WAY:
                    if (color == Color.BLACK)
                        return R.drawable.blk_checker;
                    else
                        return R.drawable.red_checker;
            }
            return 0;
        }
    };

    int tx=-1, ty=-1;
    boolean dragging = false;

    @Override
    protected void onDraw(DroidGraphics g) {
        game.draw(g, tx, ty);
    }

    @Override
    protected void onTouchDown(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        redraw();
    }

    @Override
    protected void onTouchUp(float x, float y) {
        if (dragging) {
            //monopoly.stopDrag();
            dragging = false;
        }
        tx = -1;//Math.round(x);
        ty = -1;//Math.round(y);
        redraw();
    }

    @Override
    protected void onDrag(float x, float y) {
        if (!dragging) {
            //monopoly.startDrag();
            dragging = true;
        }
        tx = Math.round(x);
        ty = Math.round(y);
        redraw();
    }

    @Override
    protected void onTap(float x, float y) {
        tx = Math.round(x);
        ty = Math.round(y);
        /*
        redraw();
        getContent().postDelayed(new Runnable() {
            public void run() {
                tx = ty = -1;
                monopoly.onClick();
            }
        }, 100);*/
        game.doClick();
    }

}
