package cc.android.test.checkerboard;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.checkerboard.Chess;
import cc.lib.checkerboard.Color;
import cc.lib.checkerboard.PieceType;
import cc.lib.checkerboard.UIGame;
import cc.lib.checkerboard.UIPlayer;

public class ChessTestActivity extends DroidActivity {

    UIGame game = new UIGame() {
        @Override
        public void repaint(long delayMs) {
            if (delayMs > 0)
                getContent().postInvalidateDelayed(delayMs);
            else
                redraw();
        }

        @Override
        protected int getCheckerboardImageId() {
            return 0;
        }

        @Override
        protected int getKingsCourtBoardId() {
            return 0;
        }

        @Override
        public int getPieceImageId(PieceType p, Color color) {
            return 0;
        }
    };

    @Override
    protected void onDraw(DroidGraphics g) {
        g.setTextModePixels(true);
        game.draw(g, 0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        game.setRules(new Chess());
        game.setPlayer(0, new UIPlayer(UIPlayer.Type.AI, 3));
        game.setPlayer(1, new UIPlayer(UIPlayer.Type.AI, 3));
        game.newGame();
        game.startGameThread();
    }

    @Override
    protected void onPause() {
        super.onPause();
        game.stopGameThread();
    }

}
