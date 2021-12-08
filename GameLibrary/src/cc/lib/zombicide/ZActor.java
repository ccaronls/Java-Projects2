package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.IRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.Vector2D;
import cc.lib.utils.Grid;
import cc.lib.utils.Reflector;
import cc.lib.zombicide.ui.UIZButton;

public abstract class ZActor<E extends Enum<E>> extends Reflector<ZActor<E>> implements UIZButton, IRectangle, IVector2D, IInterpolator<Vector2D> {

    static {
        addAllFields(ZActor.class);
    }

    ZActor(int zone) {
        this.occupiedZone = zone;
    }

    int occupiedZone = -1;
    int priorZone;
    Grid.Pos occupiedCell;
    ZCellQuadrant occupiedQuadrant;
    int actionsLeftThisTurn;
    private GRectangle rect = new GRectangle();

    @Omit
    ZActorAnimation animation = null;

    public GRectangle getRect(ZBoard b) {
        return rect = b.getCell(occupiedCell)
                .getQuadrant(occupiedQuadrant)
                .fit(getDimension())
                .scaledBy(getScale() * b.getCell(getOccupiedCell()).getScale(), Justify.CENTER, Justify.BOTTOM);
    }

    public GRectangle getRect() {
        if (animation != null && animation.getRect() != null)
            return animation.getRect();
        return rect;
    }

    void onBeginRound() {
        actionsLeftThisTurn = getActionsPerTurn();
    }

    protected abstract int getActionsPerTurn();

    public abstract String name();

    protected boolean performAction(ZActionType action, ZGame game) {
        if (isAlive()) {
            actionsLeftThisTurn -= action.costPerTurn();
            Utils.assertTrue(actionsLeftThisTurn >= 0);
        }
        return false;
    }

    public int getActionsLeftThisTurn() {
        return actionsLeftThisTurn;
    }

    public void addExtraAction() {
        actionsLeftThisTurn ++;
    }

    public abstract GDimension drawInfo(AGraphics g, ZGame game, float width, float height);

    public int getOccupiedZone() {
        return occupiedZone;
    }

    public int getPriorZone() {
        return priorZone < 0 ? occupiedZone : priorZone;
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

    public abstract int getOutlineImageId();

    public abstract GDimension getDimension();

    public boolean isInvisible() {
        return false;
    }

    ZCellQuadrant getSpawnQuadrant() {
        return null; // by default dont care where
    }

    public void addAnimation(ZActorAnimation anim) {
        if (animation == null || animation.isDone()) {
            animation = anim;
        } else {
            animation.add(anim);
        }
    }

    long getMoveSpeed() {
        return 1000;
    }

    public boolean isAnimating() {
        return animation != null;// && !animation.isDone();
    }

    public ZAnimation getAnimation() {
        return animation;
    }

    public void drawOrAnimate(AGraphics g) {
        if (animation != null && !animation.isDone()) {
            if (!animation.hidesActor())
                draw(g);

            if (!animation.isStarted())
                animation.start();
            animation.update(g);
        } else {
            animation = null;
            draw(g);
        }
    }

    public void draw(AGraphics g) {
        if (isInvisible()) {
            g.setTransparencyFilter(.5f);
        }
        g.drawImage(getImageId(), getRect());
        g.removeFilter();
    }

    int getPriority() {
        return isAlive() ? 0 : -1;
    }

    @Override
    public String getLabel() {
        return name();
    }

    @Override
    public String getTooltipText() {
        return null;
    }

    void clearActions() {
        actionsLeftThisTurn = 0;
    }

    public boolean isAlive() {
        return true;
    }

    @Override
    public float X() {
        return rect.x;
    }

    @Override
    public float Y() {
        return rect.y;
    }

    @Override
    public float getWidth() {
        return rect.w;
    }

    @Override
    public float getHeight() {
        return rect.h;
    }

    @Override
    public float getX() {
        return rect.getCenter().X();
    }

    @Override
    public float getY() {
        return rect.getCenter().Y();
    }

    @Override
    public Vector2D getAtPosition(float position) {
        return rect.getCenter();
    }

    ZActorPosition getPosition() {
        return new ZActorPosition(occupiedCell, occupiedQuadrant);
    }

    boolean isNoisy() {
        return false;
    }
}
