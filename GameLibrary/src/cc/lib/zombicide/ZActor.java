package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;

public abstract class ZActor<E extends Enum<E>> extends Reflector<ZActor<E>> {

    static {
        addAllFields(ZActor.class);
    }

    ZActor(int zone) {
        this.occupiedZone = zone;
    }

    int occupiedZone;
    Grid.Pos occupiedCell;
    ZCellQuadrant occupiedQuadrant;
    private int actionsLeftThisTurn;
    GRectangle rect;

    @Omit
    ZAnimation animation = null;

    void onBeginRound() {
        actionsLeftThisTurn = getActionsPerTurn();
    }

    protected abstract int getActionsPerTurn();

    public abstract String name();

    protected boolean performAction(ZActionType action, ZGame game) {
        actionsLeftThisTurn-=action.costPerTurn();
        return false;
    }

    public int getActionsLeftThisTurn() {
        return actionsLeftThisTurn;
    }

    void extraActivation() {
        assert (actionsLeftThisTurn>0);
        actionsLeftThisTurn++;
    }

    public final GRectangle getRect() {
        return rect;
    }

    public abstract GDimension drawInfo(AGraphics g, ZGame game, int width, int height);

    public int getOccupiedZone() {
        return occupiedZone;
    }

    public Grid.Pos getOccupiedCell() {
        return occupiedCell;
    }

    public ZCellQuadrant getOccupiedQuadrant() {
        return occupiedQuadrant;
    }

    public int getNoise() {
        return 0;
    }
    
    public abstract E getType();

    public float getScale() {
        return 1;
    }

    public abstract int getImageId();

    public boolean isInvisible() {
        return false;
    }

    public void setRect(GRectangle rect) {
        this.rect = rect;
    }

    ZCellQuadrant getSpawnQuadrant() {
        return null; // by default dont care where
    }

    void addAnimation(ZAnimation anim) {
        if (animation == null) {
            animation = anim;
        } else {
            animation.add(anim);
        }
    }

    long getMoveSpeed() {
        return 1000;
    }

    public boolean isAnimating() {
        return animation != null;
    }

    public ZAnimation getAnimation() {
        return animation;
    }

    public void draw(AGraphics g) {
        if (animation != null) {
            if (animation.isDone()) {
                animation = null;
            } else {
                if (!animation.isStarted())
                    animation.start();
                animation.update(g);
                return;
            }
        }
        g.drawImage(getImageId(), rect);
    }

    int getPriority() {
        return 0;
    }
}
