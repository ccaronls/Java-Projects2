package cc.lib.zombicide.ui;

import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
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
import cc.lib.ui.IButton;
import cc.lib.utils.Grid;
import cc.lib.utils.Lock;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZone;

public abstract class UIZombicide extends ZGame {

    static Logger log = LoggerFactory.getLogger(UIZombicide.class);

    public enum UIMode {
        NONE,
        PICK_CHARACTER,
        PICK_ZONE,
        PICK_DOOR,
        PICK_MENU,
        PICK_ZOMBIE
    }

    private boolean gameRunning = false;
    private UIMode uiMode = UIMode.NONE;
    private String message;
    private List options = Collections.emptyList();
    private Object monitor = new Object();
    private Object result = null;
    public final UIZCharacterRenderer characterRenderer;
    public final UIZBoardRenderer boardRenderer;

    private static UIZombicide instance;

    public static UIZombicide getInstance() {
        Utils.assertTrue(instance != null);
        return instance;
    }

    public UIZombicide(UIZCharacterRenderer characterRenderer, UIZBoardRenderer boardRenderer) {
        Utils.assertTrue(instance == null);
        instance = this;
        this.characterRenderer = characterRenderer;
        this.boardRenderer = boardRenderer;
    }

    public UIMode getUiMode() {
        return uiMode;
    }

    public String getMessage() {
        return message;
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

    class HoverMessage extends ZAnimation {

        private final String msg;
        private final IVector2D center;

        HoverMessage(String msg, IVector2D center) {
            super(3000);
            this.msg = msg;
            this.center = center;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            float zoom = board.getZoom()+1;
            Vector2D v = new Vector2D(center);
            v = v.sub(0, position/zoom);
            float t = g.getTextHeight();
            g.setTextHeight(20);
            g.setColor(GColor.YELLOW.withAlpha(1f-position));
            g.drawJustifiedString(v, Justify.LEFT, Justify.BOTTOM, msg);
            g.setTextHeight(t);
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
                        curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(), .5f);
                    }
                    int idx = Math.round(position * (ZIcon.DRAGON_BILE.imageIds.length-1));
                    int id = ZIcon.DRAGON_BILE.imageIds[idx];

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
                        curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(), .5f);
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
                    boardRenderer.addPostActor(new ZAnimation(2000) {
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
        boardRenderer.addPreActor(new DeathAnimation(zombie));
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
        boardRenderer.redraw();
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
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterDefends(ZCharacter cur, ZZombie zombie) {
        boardRenderer.addPostActor(new ZAnimation(1000) {
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
    }

    private void waitForAnimationToComplete(long duration) {
        boardRenderer.redraw();
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
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterWounded(ZCharacter character) {
        boardRenderer.addPostActor(new ZAnimation(1000) {

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
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        boardRenderer.addPostActor(new HoverMessage(String.format("+%d EXP", points), c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        boardRenderer.addOverlay(new ZAnimation(2000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                int cx = g.getViewportWidth()/2;
                int cy = g.getViewportHeight()/2;

                float minHeight = 32;
                float maxHeight = 48;

                GColor color = GColor.GREEN.withAlpha(1f-position);
                float curHeight = g.getTextHeight();
                g.setTextHeight(minHeight + (maxHeight-minHeight)*position);
                g.drawJustifiedString(cx, cy, Justify.CENTER, Justify.CENTER, String.format("DOUBLE SPAWN X %d", multiplier));
                g.setTextHeight(curHeight);
            }
        });
    }

    @Override
    protected void onNewSkillAquired(ZCharacter c, ZSkill skill) {
        boardRenderer.addPostActor(new HoverMessage(String.format("%s Aquired", skill.getLabel()), c.getRect().getCenter()));
        characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.getLabel(), skill.getLabel()));
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        boardRenderer.addPreActor(new ZAnimation(1000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                Grid.Pos pos = zone.getCells().iterator().next();
                GRectangle rect = new GRectangle(board.getCell(pos)).setCenter(zone.getCenter());
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
                    g.drawCircle(rect.getCenter(), radius, 0);
                }
            }
        });
        waitForAnimationToComplete(1000);
    }

    @Override
    protected void onWeaponGoesClick(ZCharacter c, ZWeapon weapon) {
        boardRenderer.addPostActor(new HoverMessage("CLICK", c.getRect().getCenter()));
    }

    @Override
    protected void onAttack(ZCharacter attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {

        if (actionType == ZActionType.MELEE) {

            Lock animLock = new Lock(numDice);

            for (int i=0; i<numDice; i++) {
                int id = Utils.randItem(ZIcon.SLASH.imageIds);
                GRectangle rect = attacker.getRect(board).scaledBy(1.3f).movedBy(Utils.randFloatX(.1f), Utils.randFloatX(.1f));

                boardRenderer.addPostActor(new ZAnimation(400) {
                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        AImage img = g.getImage(id);
                        g.setTransparencyFilter(1f-position);
                        g.drawImage(id, rect.fit(img));
                        g.removeFilter();
                    }

                    @Override
                    protected void onDone() {
                        animLock.release();
                    }
                });
                Utils.waitNoThrow(this, 100);
            }



        } else if (attacker.getOccupiedZone() != targetZone && actionType.isRanged()) {

            //ZZone start = getBoard().getZone(attacker.getOccupiedZone());
            ZZone end   = getBoard().getZone(targetZone);
            Vector2D start = attacker.getRect().getCenter();
            Vector2D dv = end.getCenter().sub(start);
            ZDir dir = ZDir.getFromVector(dv);
            Lock animLock = new Lock(numDice);

            for (int i=0; i<numDice; i++) {

                Vector2D path = end.getCenter().addEq(Utils.randFloatX(.3f), Utils.randFloatX(.3f)).sub(start);
                long dur = Math.round(path.mag() * 500);
                boardRenderer.addPostActor(new ZAnimation(dur) {

                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        int id = ZIcon.ARROW.imageIds[dir.ordinal()];
                        AImage img = g.getImage(id);
                        GRectangle rect = attacker.getRect(board).scaledBy(.5f).fit(img);
                        Vector2D pos = start.add(path.scaledBy(position));
                        rect.setCenter(pos);
                        g.drawImage(id, rect);
                    }

                    @Override
                    protected void onDone() {
                        animLock.release();
                    }
                });
                Utils.waitNoThrow(this, 200);
            }
            animLock.block();
        }
    }

    public void addPlayerComponentMessage(String message) {
        characterRenderer.addMessage(message);
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
                boardRenderer.redraw();
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
            boardRenderer.redraw();
        }
        return waitForUser(ZCharacter.class);
    }

    public Integer pickZone(String message, List<Integer> zones) {
        synchronized (this) {
            this.message = message;
            options = zones;
            uiMode = UIMode.PICK_ZONE;
            boardRenderer.redraw();
        }
        return waitForUser(Integer.class);
    }

    public <T> T pickMenu(String message, Class expectedType, List<T> moves) {
        synchronized (this) {
            this.message = message;
            options = moves;
            uiMode = UIMode.PICK_MENU;
            if (expectedType.equals(ZMove.class))
                boardRenderer.processMoveOptions(getCurrentCharacter(), (List<ZMove>)moves);
            else
                boardRenderer.processSubMenu(getCurrentCharacter(), (List<IButton>)moves);
        }
        return (T) waitForUser(expectedType);
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        synchronized (this) {
            this.message = message;
            options = doors;
            uiMode = UIMode.PICK_DOOR;
            boardRenderer.redraw();
        }
        return waitForUser(ZDoor.class);
    }

    public void tryWalk(ZDir dir) {
        ZCharacter cur = getCurrentCharacter();
        if (cur != null) {
            if (getBoard().canMove(cur, dir)) {
                setResult(ZMove.newWalkDirMove(dir));
            }
        }
    }

    @Override
    protected void initQuest(ZQuest quest) {
        boardRenderer.clearTiles();
    }
}
