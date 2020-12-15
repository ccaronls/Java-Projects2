package cc.lib.zombicide.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Bezier;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZone;

public abstract class UIZombicide extends ZGame implements ZTiles {

    static Logger log = LoggerFactory.getLogger(UIZombicide.class);

    public enum UIMode {
        NONE,
        PICK_CHARACTER,
        PICK_ZONE,
        PICK_DOOR,
        PICK_MENU,
        PICK_ZOMBIE
    }

    List<ZAnimation> preActor = new ArrayList<>();
    List<ZAnimation> postActor = new ArrayList<>();
    List<ZAnimation> overlayAnimations = new ArrayList<>();
    boolean gameRunning = false;
    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;
    Grid.Pos selectedCell = null;
    boolean actorsAnimating = false;
    private Object overlayToDraw = null;
    UIMode uiMode = UIMode.NONE;
    String message = null;
    List options = Collections.emptyList();
    Object monitor = new Object();
    Object result = null;
    boolean drawTiles = false;
    final UIZCharacterRenderer characterRenderer;

    private static UIZombicide instance;

    public static UIZombicide getInstance() {
        assert(instance != null);
        return instance;
    }

    public UIZombicide(UIComponent characterComponent) {
        assert(instance == null);
        instance = this;
        characterRenderer = new UIZCharacterRenderer(characterComponent);
    }

    public UIMode getUiMode() {
        return uiMode;
    }

    public List getOptions() {
        return options;
    }

    static class DeathAnimation extends ZActorAnimation {

        DeathAnimation(ZActor a) {
            super(a, 2000);
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            GRectangle rect = new GRectangle(actor.getRect());
            rect.y += rect.h*position;
            rect.h *= (1f-position);
            float dx = rect.w*position;
            rect.w += dx;
            rect.x -= dx/2;
            g.drawImage(actor.getImageId(), rect);
        }
    }

    static class HoverMessage extends ZAnimation {

        private final String msg;
        private final IVector2D center;

        HoverMessage(String msg, IVector2D center) {
            super(1500);
            this.msg = msg;
            this.center = center;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            Vector2D v = new Vector2D(center);
            v = v.sub(0, position);
            g.setColor(GColor.YELLOW.withAlpha(1f-position));
            g.drawJustifiedString(v, Justify.LEFT, Justify.BOTTOM, msg);
        }
    }

    public boolean isAnimating() {
        return actorsAnimating || postActor.size() > 0 || preActor.size() > 0 || overlayAnimations.size() > 0;
    }

    private void addPreActor(ZAnimation a) {
        synchronized (preActor) {
            preActor.add(a.start());
        }
        redraw();
    }

    private void addPostActor(ZAnimation a) {
        synchronized (postActor) {
            postActor.add(a.start());
        }
        redraw();
    }

    private void addOverlay(ZAnimation a) {
        synchronized (overlayAnimations) {
            overlayAnimations.add(a.start());
        }
        redraw();
    }

    private synchronized void drawAnimations(List<ZAnimation> anims, AGraphics g) {
        synchronized (anims) {
            Iterator<ZAnimation> it = anims.iterator();
            while (it.hasNext()) {
                ZAnimation a = it.next();
                if (a.isDone()) {
                    it.remove();
                } else {
                    a.update(g);
                }
            }
        }
    }

    @Override
    protected void onDragonBileThrown(ZCharacter actor, int zone) {
        if (actor.getOccupiedZone() != zone) {
            actor.addAnimation(new ZActorAnimation(actor, 1000) {

                Bezier curve;

                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    if (curve == null) {
                        curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(board), .5f);
                    }
                    int idx = Math.round(position * (ZIcon.DRAGON_BILE.imageIds.length-1));
                    int id = ZIcon.DRAGON_BILE.imageIds[idx];//((int)angle)%ZIcon.DRAGON_BILE.imageIds.length];

                    AImage img = g.getImage(id);
                    GRectangle rect = actor.getRect(board).scaledBy(.5f).fit(img);
                    rect.setCenter(curve.getPointAt(position));
                    g.drawImage(id, rect);
                    g.drawImage(actor.getImageId(), actor.getRect(board));
                }

            });
            waitForAnimationToComplete(1000);
        }
    }

    @Override
    protected void onTorchThrown(ZCharacter actor, int zone) {
        if (actor.getOccupiedZone() != zone) {
            actor.addAnimation(new ZActorAnimation(actor, 1000) {

                Bezier curve;

                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    if (curve == null) {
                        curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(board), .5f);
                    }
                    int idx = Math.round(position * (ZIcon.TORCH.imageIds.length-1));
                    int id = ZIcon.TORCH.imageIds[idx];//((int)angle)%ZIcon.DRAGON_BILE.imageIds.length];

                    AImage img = g.getImage(id);
                    GRectangle rect = actor.getRect(board).scaledBy(.5f).fit(img);
                    rect.setCenter(curve.getPointAt(position));
                    g.drawImage(id, rect);
                    g.drawImage(actor.getImageId(), actor.getRect(board));
                }

                @Override
                protected void onDone() {
                    addPostActor(new ZAnimation(2000) {
                        float index = 0;

                        @Override
                        protected void draw(AGraphics g, float position, float dt) {
                            for (Grid.Pos pos : board.getZone(zone).getCells()) {
                                IRectangle rect = board.getCell(pos);
                                int idx = ((int)index) % ZIcon.FIRE.imageIds.length;
                                g.drawImage(ZIcon.FIRE.imageIds[idx], rect);
                                index += .2f;
                            }
                        }
                    });
                }
            });
            waitForAnimationToComplete(1000);
        }
    }

    @Override
    protected void onZombieDestroyed(ZCharacter c, ZZombie zombie) {
        //zombie.addAnimation(new DeathAnimation(zombie));
        addPreActor(new DeathAnimation(zombie));
    }

    @Override
    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end, long speed) {
        actor.addAnimation(new ZActorAnimation(actor, speed) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                MutableVector2D dv0 = end.getTopLeft().sub(start.getTopLeft());
                MutableVector2D dv1 = end.getBottomRight().sub(start.getBottomRight());

                Vector2D topLeft = start.getTopLeft().add(dv0.scaledBy(position));
                Vector2D bottomRight = start.getBottomRight().add(dv1.scaledBy(position));

                GRectangle r = new GRectangle(topLeft, bottomRight);

                g.drawImage(actor.getImageId(), r);
            }
        });
        redraw();
    }

    @Override
    protected void onZombieSpawned(ZZombie zombie) {
        GRectangle rect = new GRectangle(zombie.getRect(board));
        zombie.addAnimation(new ZActorAnimation(zombie, 1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GRectangle dest = new GRectangle(rect);
                dest.y += (dest.h) * (1f - position);
                dest.h *= position;
                g.drawImage(zombie.getImageId(), dest);
            }
        });
        redraw();
    }

    @Override
    protected void onCharacterDefends(ZCharacter cur, ZZombie zombie) {
        addPostActor(new ZAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                int id = ZIcon.SHIELD.imageIds[0];
                AImage img = g.getImage(id);
                GRectangle rect = cur.getRect().fit(img);
                g.setTransparencyFilter(1f-position);
                g.drawImage(id, rect);
                g.removeFilter();
            }
        });
        redraw();
    }

    private void waitForAnimationToComplete(long duration) {
        redraw();
        synchronized (this) {
            try {
                wait(duration);
            } catch (Exception e) {}
        }
    }

    @Override
    protected void onCharacterPerished(ZCharacter character) {
        character.addAnimation(new DeathAnimation(character) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                super.draw(g, position, dt);
                GRectangle rect = new GRectangle(actor.getRect(board));
                rect.y -= rect.h * 3 * position;
                g.setTransparencyFilter(1f-position);
                g.drawImage(actor.getImageId(), rect);
                g.removeFilter();
            }
        });
        redraw();
    }

    @Override
    protected void onCharacterWounded(ZCharacter character) {
        addPostActor(new ZAnimation(1000) {

            int claws = Utils.randItem(ZIcon.CLAWS.imageIds);
            GRectangle rect;

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                if (rect == null) {
                    AImage img = g.getImage(claws);
                    rect = character.getRect(board).fit(img);
                }
                if (!isRunning())
                    return;
                g.setTransparencyFilter(1f-position);
                g.drawImage(claws, rect);
                g.removeFilter();
            }
        });
        redraw();
    }

    @Override
    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        addPostActor(new HoverMessage(String.format("+%d EXP", points), c.getRect().getCenter()));
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        addOverlay(new ZAnimation(2000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                int cx = g.getViewportWidth()/2;
                int cy = g.getViewportHeight()/2;

                float minHeight = 32;
                float maxHeight = 48;

                GColor color = GColor.GREEN.withAlpha(1f-position);
                int curHeight = g.getTextHeight();
                g.setTextHeight(minHeight + (maxHeight-minHeight)*position);
                g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.CENTER, String.format("DOUBLE SPAWN X %d", multiplier));
                g.setTextHeight(curHeight);
            }
        });
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        addPreActor(new ZAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                Grid.Pos pos = zone.getCells().iterator().next();
                GRectangle rect = new GRectangle(board.getCell(pos)).setCenter(zone.getCenter(board));
                final float RADIUS = rect.getRadius();
                final int numCircles = 3;
                float r = RADIUS * position;
                float steps = numCircles+1;
                float r2 = ((float)((int)(steps*position))) / steps;
                g.setColor(GColor.BLACK);
                g.drawCircle(rect.getCenter(), r, 3);
                if (r2 > 0) {
                    float radius = r2*RADIUS;
                    float delta = (r-radius)*steps / RADIUS;
                    float alpha = 1 - delta;
                    //log.debug("alpha = %d", Math.round(alpha*100));
                    g.setColor(GColor.BLACK.withAlpha(alpha));
                    g.drawCircle(rect.getCenter(), radius, 1);
                }
            }
        });
        waitForAnimationToComplete(1000);
    }

    @Override
    protected void onWeaponGoesClick(ZCharacter c, ZWeapon weapon) {
        addPostActor(new HoverMessage("CLICK", c.getRect().getCenter()));
    }

    protected abstract void redraw();

    protected abstract float getWidth();

    protected abstract float getHeight();

    public void addPlayerComponentMessage(String message) {
        characterRenderer.addMessage(message);
    }

    public ZActor drawActors(AGraphics g, ZBoard b, float mx, float my, boolean drawAnimating) {
        ZActor picked = null;
        float distFromCenter = 0;
        actorsAnimating = false;
        for (ZActor a : b.getAllLiveActors()) {
            if (a.isAnimating() && !drawAnimating)
                continue;
            AImage img = g.getImage(a.getImageId());
            if (img != null) {
                GRectangle rect = a.getRect(b);
                if (rect.contains(mx, my)) {
                    float dist = rect.getCenter().sub(mx, my).magSquared();
                    if (picked == null || !(picked instanceof ZCharacter) || dist < distFromCenter) {
                        picked = a;
                        distFromCenter = dist;
                    }
                }
                if (a.isInvisible()) {
                    g.setTransparencyFilter(.5f);
                }
                a.draw(g);
                if (a.isAnimating()) {
                    actorsAnimating = true;
                }
                g.removeFilter();
            }
        }
        return picked;
    }

    public void draw(AGraphics g, int _mouseX, int _mouseY) {
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        g.pushMatrix();
        g.translate(getWidth()/2,0);// - dim.width/2, 0);

        GDimension dim = board.getDimension();
        g.scale(getHeight() / dim.height);
        g.translate(-dim.width/2, 0);

        Vector2D mouse = g.screenToViewport(_mouseX, _mouseY);
        //log.debug("mouse %d,%d -> %s", _mouseX, _mouseY, mouse);

        final int OUTLINE = 2;

        //final Grid.Pos cellPos = board.drawDebug(g, mouse.X(), mouse.Y());

        if (isGameRunning() || isGameOver()) {
            Grid.Pos cellPos = null;
            if (drawTiles) {
                getQuest().drawTiles(g, board, this);
                cellPos = board.pickCell(g, mouse.X(), mouse.Y());
            } else {
                cellPos = board.drawDebug(g, mouse.X(), mouse.Y());
            }
            int highlightedZone = board.drawZones(g, mouse.X(), mouse.Y());
            boolean drawAnimating = isGameOver();

            drawAnimations(preActor, g);

            highlightedActor = //board.drawActors(g, getMouseX(), getMouseY());
                    drawActors(g, board, mouse.X(), mouse.Y(), drawAnimating || overlayToDraw == null);

            drawAnimations(postActor, g);

            //if (drawZoneAnimations(g))
            //    repaint();

            if (getCurrentCharacter() != null) {
//                if (highlightedActor == getGame().getCurrentCharacter())
                //                  highlightedActor = null; // prevent highlighting an already selected actor
                g.setColor(GColor.GREEN);
                g.drawRect(getCurrentCharacter().getRect(board).scale(1.02f), OUTLINE);
            }

            g.setColor(GColor.BLACK);
            if (getQuest()!=null) {
                int height = g.getTextHeight();
                g.setTextHeight(24);//setFont(bigFont);
                g.setTextStyles(AGraphics.TextStyle.BOLD, AGraphics.TextStyle.ITALIC);
                g.drawJustifiedString(10, getHeight()-10-g.getTextHeight(), Justify.LEFT, Justify.BOTTOM, getQuest().getName());
                g.setTextHeight(height);
                g.setTextStyles(AGraphics.TextStyle.NORMAL);
            }
            //g.setFont(smallFont);
            g.drawJustifiedString(10, getHeight()-10, Justify.LEFT, Justify.BOTTOM, message);
            switch (uiMode) {
                case PICK_ZOMBIE:
                case PICK_CHARACTER: {
                    g.setColor(GColor.YELLOW);
                    for (ZActor a : (List<ZActor>)options) {
                        a.getRect(board).drawOutlined(g, 1);
                    }
                    if (options.contains(highlightedActor)) {
                        highlightedResult = highlightedActor;
                    }
                    break;
                }
                case PICK_ZONE: {
                    if (highlightedZone >= 0 && options.contains(highlightedZone)) {
                        highlightedResult = highlightedZone;
                        g.setColor(GColor.YELLOW);
                        board.drawZoneOutline(g, highlightedZone);
                    } else if (cellPos != null) {
                        ZCell cell = board.getCell(cellPos);
                        for (int i = 0; i < options.size(); i++) {
                            if (cell.getZoneIndex() == (Integer)options.get(i)) {
                                highlightedCell = cellPos;
                                highlightedResult = cell.getZoneIndex();
                                break;
                            }
                        }
                    }
                    break;
                }
                case PICK_DOOR: {
                    highlightedResult = board.pickDoor(g, (List<ZDoor>)options, mouse.X(), mouse.Y());
                    break;
                }
            }
            if (highlightedCell != null) {
                ZCell cell = board.getCell(highlightedCell);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
            }
            if (highlightedActor != null) {
                g.setColor(GColor.RED);
                g.drawRect(highlightedActor.getRect(board).scale(1.01f), OUTLINE);
            }

            characterRenderer.redraw();


        } else {

            Grid.Pos cellPos = board.drawDebug(g, mouse.X(), mouse.Y());
            if (drawTiles) {
                getQuest().drawTiles(g, board, this);
            }

            if (cellPos != null) {
                highlightedCell = cellPos;
                ZCell cell = board.getCell(cellPos);
                g.setColor(GColor.RED.withAlpha(32));
                board.drawZoneOutline(g, cell.getZoneIndex());
                g.setColor(GColor.RED);
                g.drawRect(cell);

                List<ZDoor> doors = board.getZone(cell.getZoneIndex()).getDoors();
                highlightedDoor = board.pickDoor(g, doors, mouse.X(), mouse.Y());

                if (selectedCell != null) {
                    ZCell selected = board.getCell(selectedCell);
                    g.setColor(GColor.MAGENTA);
                    selected.drawOutlined(g, 4);
                    ZCell highlighted = board.getCell(highlightedCell);
                    //Collection<ZDir> dirs = board.getShortestPathOptions(selectedCell, highlighted.getZoneIndex());
                    List<List<ZDir>> paths = board.getShortestPathOptions(selectedCell, highlighted.getZoneIndex());
                    GColor [] colors = new GColor[] { GColor.CYAN, GColor.MAGENTA, GColor.PINK, GColor.ORANGE };
                    int colorIndex = 0;
                    for (List<ZDir> path : paths) {
                        g.setColor(colors[colorIndex]);
                        colorIndex = (colorIndex+1) & colors.length;
                        Grid.Pos cur = selectedCell;
                        g.begin();
                        g.vertex(board.getCell(cur).getCenter());
                        for (ZDir dir : path) {
                            cur = board.getAdjacent(cur, dir);///dir.getAdjacent(cur);
                            g.vertex(board.getCell(cur).getCenter());
                        }
                        g.drawLineStrip(3);
                    }


                    g.setColor(GColor.CYAN);
//                    g.drawJustifiedStringOnBackground(mouseX, mouseY, Justify.CENTER, Justify.BOTTOM, dirs.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                } else {
                    g.setColor(GColor.CYAN);
                    g.drawJustifiedStringOnBackground(mouse.X(), mouse.Y(), Justify.CENTER, Justify.BOTTOM, cellPos.toString(), GColor.TRANSLUSCENT_BLACK, 10, 10);
                }
            }

        }

        g.popMatrix();
        drawAnimations(overlayAnimations, g);

        if (isGameOver() && overlayToDraw == null && !isAnimating()) {
            setOverlay(getGameSummaryTable());
        }

        // overlay
        if (overlayToDraw != null) {
            if (overlayToDraw instanceof Integer) {
                int id = ((Integer) overlayToDraw);
                if (id >= 0) {
                    AImage img = g.getImage(id);
                    GRectangle rect = new GRectangle(0, 0, getWidth(), getHeight());
                    rect.scale(.9f, .9f);
                    rect = rect.fit(img, Justify.LEFT, Justify.CENTER);
                    g.drawImage(id, rect);
                }
            } else if (overlayToDraw instanceof Table) {
                g.setColor(GColor.YELLOW);
                dim = ((Table)overlayToDraw).getDimension(g);
                g.pushMatrix();
                g.translate(getWidth()/2, getHeight()/2);
                g.translate(-dim.width/2, -dim.height/2);
                ((Table)overlayToDraw).draw(g);
                g.popMatrix();
            }
        }

    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public synchronized void stopGameThread() {
        gameRunning = false;
    }

    public synchronized void startGameThread() {
        if (gameRunning)
            return;

        characterRenderer.clearMessages();
        gameRunning = true;
        new Thread(()-> {
            try {
                redraw();
                while (gameRunning && !isGameOver()) {
                    runGame();
                }
            } catch (Exception e) {
                log.error(e.getClass().getSimpleName() + " " + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    log.error(st.toString());
                }
                e.printStackTrace();
            }
            gameRunning = false;
            log.debug("Game thread stopped");

        }).start();
    }

    public void setOverlay(Object obj) {
        if (obj == null) {
            overlayToDraw = null;
        } else if (obj instanceof Table) {
            overlayToDraw = obj;
        } else if (obj instanceof ZPlayerName){
            overlayToDraw = ((ZPlayerName)obj).cardImageId;
        }
        redraw();
    }

    public <T> T waitForUser(Class<T> expectedType) {
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (Exception e) {

            }
        }
        uiMode = UIMode.NONE;
        if (result != null && expectedType.isAssignableFrom(result.getClass()))
            return (T)result;
        return null;
    }

    public void setResult(Object result) {
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public ZCharacter pickCharacter(String message, List<ZCharacter> characters) {
        synchronized (this) {
            this.message = message;
            options = characters;
            uiMode = UIMode.PICK_CHARACTER;
            redraw();
        }
        return waitForUser(ZCharacter.class);
    }

    public Integer pickZone(String message, List<Integer> zones) {
        synchronized (this) {
            this.message = message;
            options = zones;
            uiMode = UIMode.PICK_ZONE;
            redraw();
        }
        return waitForUser(Integer.class);
    }

    public <T> T pickMenu(String message, Class expectedType, List<T> moves) {
        synchronized (this) {
            this.message = message;
            options = moves;
            uiMode = UIMode.PICK_MENU;
        }
        return (T) waitForUser(expectedType);
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        synchronized (this) {
            this.message = message;
            options = doors;
            uiMode = UIMode.PICK_DOOR;
            redraw();
        }
        return waitForUser(ZDoor.class);
    }

    public void onTap() {
        if (isGameRunning()) {
            setResult(highlightedResult);
        } else {
            if (highlightedDoor != null) {
                highlightedDoor.toggle(board);
            } else if (highlightedCell != null) {
                if (highlightedCell.equals(selectedCell)) {
                    selectedCell = null;
                } else {
                    selectedCell = highlightedCell;
                }
            }
        }
    }

    public void setDrawTiles(boolean draw) {
        this.drawTiles = draw;
    }

    public void toggleDrawTiles() {
        this.drawTiles = !this.drawTiles;
    }

    public ZActor getHighlightedActor() {
        return highlightedActor;
    }
}
