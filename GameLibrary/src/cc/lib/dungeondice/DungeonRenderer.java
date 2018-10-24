package cc.lib.dungeondice;

import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.ui.UIAnimation;
import cc.lib.ui.UIComponent;
import cc.lib.ui.UIRenderer;

public class DungeonRenderer extends UIRenderer implements DDungeon.Listener {

    DDungeon dungeon;
    UIAnimation animation;

    public DungeonRenderer(UIComponent component) {
        super(component);
    }

    @Override
    public void draw(APGraphics g, int px, int py) {
        if (dungeon == null)
            return;
        dungeon.draw(g);
        if (animation != null) {
            if (animation.isDone()) {
                animation = null;
            } else {
                animation.update(g);
                getComponent().redraw();
            }
        }
    }

    @Override
    public void onPrize(final DDungeon.Prize p) {
        animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.GREEN);
                g.setTextHeight(40);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER,
                        String.format("Player %s gets %s", dungeon.getCurPlayer().getName(), p.name()));
            }
        }.start();
        repaintAndWait();
    }

    @Override
    public void onEnemyDead(final DEnemy e) {
        animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.GREEN);
                g.setTextHeight(40);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER,
                        String.format("Enemy %s is destroyed", e.getName()));
            }
        }.start();
        repaintAndWait();

    }

    @Override
    public void onPlayerDead(final DPlayer p) {
        animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.GREEN);
                g.setTextHeight(40);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER,
                        String.format("Player %s has died", p.getName()));
            }
        }.start();
        repaintAndWait();

    }

    @Override
    public void onMiss(final DEntity attacker) {
        animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.GREEN);
                g.setTextHeight(40);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER,
                        String.format("%s missed", attacker.getName()));
            }
        }.start();
        repaintAndWait();

    }

    @Override
    public void onDamage(final DEntity e, final int damage) {
        animation = new UIAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.setColor(GColor.GREEN);
                g.setTextHeight(40);
                g.drawJustifiedString(g.getViewportWidth()/2, g.getViewportHeight()/2, Justify.CENTER,
                        String.format("%s takes %d damage", e.getName(), damage));
            }
        }.start();
        repaintAndWait();

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
