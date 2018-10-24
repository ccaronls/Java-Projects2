package cc.lib.dungeondice;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.ui.UIAnimation;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public class UIGameRenderer extends UIRenderer implements DDungeon.Listener {

    DDungeon game;
    static int ratId, snakeId, spiderId; // image assets
    UIAnimation animation=null;

    public UIGameRenderer(UIComponent component) {
        super(component);
    }

    public void initAssets(int ratId, int snakeId, int spiderId) {
        this.ratId=ratId;
        this.snakeId=snakeId;
        this.spiderId=spiderId;
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (game == null || game.board == null)
            return;
        game.board.drawCells(g, 1);
        for (int i=0; i<game.numPlayers; i++) {
            drawPlayer(g, game.players[i]);
        }
        if (game.enemyList.size() > 0) {
            // overlay the battle
            g.pushMatrix();
            float dim = Math.min(g.getViewportWidth(), g.getViewportHeight())*2/3;
            g.translate(g.getViewportWidth()/2, g.getViewportHeight()/2);
            g.translate(-dim/2, -dim/2);
            drawBattle(g, dim);
            g.popMatrix();
        }
    }

    private void drawBattle(APGraphics g, float dim) {
        // draw enemies on top and

        g.setColor(GColor.LIGHT_GRAY);
        g.drawFilledRect(0, 0, dim, dim);

        float mid = dim/2;
        float imgDim = mid*2/3;
        float n = game.enemyList.size();

        if (imgDim *n + (imgDim /4*(n-1)) > dim) {
            imgDim = 4 * dim / (5 * n - 1);
        }
        float spacing = imgDim /4;
        float dw = imgDim *n + spacing*(n-1);
        g.pushMatrix();
        g.translate(dim/2 - dw/2, dim/2-dw/2);
        for (DEnemy d : game.enemyList) {
            d.draw(g, imgDim);
            g.translate(dw+spacing, 0);
        }

        if (animation != null) {
            animation.update(g);
            if (animation.isDone()) {
                animation=null;
            }
        }

        g.popMatrix();
    }

    protected void drawPlayer(AGraphics g, DPlayer p) {
        DCell cell = game.board.getCell(p.cellIndex);
        g.pushMatrix();
        g.translate(cell);
        g.setColor(p.getColor());
        GRectangle rect = game.board.getCellBoundingRect(p.cellIndex);
        float m = Math.min(rect.w, rect.h);
        rect.scale(m/8, m/8);
        g.setLineWidth(2);
        g.drawCircle(0, -1.5f, 0.5f);
        g.begin();
        g.vertexArray(new float [][] {
                {  0f, -1.0f },
                {  0f,  0.5f },
                { -1f, -0.5f },
                {  1f, -0.5f },
                {  0f,  0.5f },
                { -1f,  2.0f },
                {  0f,  0.5f },
                {  1f,  2.0f }
        });
        g.drawLines();
        g.popMatrix();
    }

    @Override
    public void onPrize(DDungeon.Prize p) {

    }

    @Override
    public void onEnemyDead(DEnemy e) {
        e.animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {

            }
        }.start();

    }

    @Override
    public void onPlayerDead(DPlayer p) {

    }

    @Override
    public void onMiss(DEntity attacker) {

    }

    @Override
    public void onDamage(DEntity e, int damage) {

    }

    private void repaintAndWait() {
        getComponent().redraw();
        try {
            synchronized (animation) {
                animation.wait(animation.getDuration()+10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
