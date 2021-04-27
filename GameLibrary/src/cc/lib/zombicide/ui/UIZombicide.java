package cc.lib.zombicide.ui;

import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
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
import cc.lib.zombicide.ZAttackType;
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
import cc.lib.zombicide.anims.AscendingAngelDeathAnimation;
import cc.lib.zombicide.anims.DeathAnimation;
import cc.lib.zombicide.anims.EarthquakeAnimation;
import cc.lib.zombicide.anims.ElectrocutionAnimation;
import cc.lib.zombicide.anims.FireballAnimation;
import cc.lib.zombicide.anims.HoverMessage;
import cc.lib.zombicide.anims.InfernoAnimation;
import cc.lib.zombicide.anims.LightningAnimation;
import cc.lib.zombicide.anims.MagicAnimation;
import cc.lib.zombicide.anims.MeleeAnimation;
import cc.lib.zombicide.anims.ShootAnimation;
import cc.lib.zombicide.anims.SlashedAnimation;
import cc.lib.zombicide.anims.ThrowAnimation;

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
/*
    class ThrowAnimation extends ZActorAnimation {

        final int zone;
        final ZIcon icon;
        Bezier curve=null;

        ThrowAnimation(ZActor actor, int targetZone, ZIcon icon) {
            super(actor, 1000);
            this.zone = targetZone;
            this.icon = icon;
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            if (curve == null) {
                curve = Bezier.build(actor.getRect(board).getCenter(), board.getZone(zone).getCenter(), .5f);
            }
            int idx = Math.round(position * (icon.imageIds.length-1));
            int id = icon.imageIds[idx];

            AImage img = g.getImage(id);
            GRectangle rect = actor.getRect(board).scaledBy(.5f).fit(img);
            rect.setCenter(curve.getPointAt(position));
            g.drawImage(id, rect);
        }

        @Override
        protected boolean hidesActor() {
            return false;
        }
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

    class InfernoAnimation extends ZAnimation {

        InfernoAnimation(int zoneIndex) {
            super(2000);
            this.zone = zoneIndex;
        }

        final int zone;
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
    }

    class FireballAnimation extends ZActorAnimation {

        final Vector2D path;
        final Vector2D start;
        final GRectangle rect;

        FireballAnimation(ZActor actor, Vector2D end) {
            super(actor, 500);
            this.rect = actor.getRect(board).scaledBy(.5f);
            this.start = rect.getCenter();
            path = end.sub(start);
            setDuration(Math.round(path.mag()* 700));
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            int id = Utils.randItem(ZIcon.FIREBALL.imageIds);
            AImage img = g.getImage(id);
            //GRectangle rect = attacker.getRect(board).scaledBy(.5f).fit(img);
            Vector2D pos = start.add(path.scaledBy(position));
            GRectangle r = rect.fit(img).setCenter(pos);
            g.drawImage(id, r);
        }
    }

    class LightningAnimation extends ZActorAnimation {

        Vector2D dv;
        final float mag;
        final Vector2D start, end;
        final LinkedList<Vector2D>[] sections;// = new LinkedList<>();
        final float sectionLen;
        final int numSections;
        int curRepeat = -1;

        public LightningAnimation(ZActor actor, int targetZone, int strands) {
            super(actor, 150, 3);
            final int sections = 4;
            start = actor.getRect(board).getCenter();
            end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(.3f));
            dv = end.sub(start);
            mag = dv.mag();
            dv = dv.scaledBy(1.0f / mag);
            numSections = sections;
            sectionLen = mag / (numSections+1);
            this.sections = new LinkedList[strands];
            for (int i=0; i<strands; i++) {
                this.sections[i] = new LinkedList<>();
            }
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            int r = getRepeat();
            if (r != curRepeat) {
                curRepeat = r;
                for (List l : sections) {
                    l.clear();
                    l.add(start);
                }
            }

            float randLenFactor = .8f;
            float randAngFactor = 30;

            if (position <= .52f) {

                int sec = Utils.clamp(Math.round(position * 2 * (numSections + 1))+1, 1, numSections+1);

                for (LinkedList<Vector2D> l : sections) {
                    while (sec > l.size()) {
                        float m = sectionLen * (l.size() + 1);
                        MutableVector2D n = l.getFirst().add(dv.scaledBy(randLenFactor * m));
                        //n.addEq(Vector2D.newRandom(sectionLen / (maxRandomFactor/sec)));
                        n.addEq(dv.rotate(Utils.randFloatX(randAngFactor)).scaledBy((1f-randLenFactor) * m));
                        l.add(n);
                    }
                }
            } else {
                for (LinkedList<Vector2D> l : sections) {
                    int sec = (numSections + 1) - Math.round((position - .5f) * 2 * (numSections + 1));
                    if (sec < 1)
                        sec = 1;
                    while (sec < l.size()) {
                        l.removeFirst();
                    }
                }
            }

            g.setColor(GColor.WHITE);
            g.setLineWidth(2);
            for (LinkedList<Vector2D> l : sections) {
                g.begin();
                for (Vector2D v : l) {
                    g.vertex(v);
                }
                g.drawLineStrip();
                g.end();
            }
        }

        @Override
        protected boolean hidesActor() {
            return false;
        }
    }

    class MeleeAnimation extends ZActorAnimation {

        final int id;
        final GRectangle rect;

        public MeleeAnimation(ZActor actor) {
            super(actor, 400);
            id = Utils.randItem(ZIcon.SLASH.imageIds);
            rect = actor.getRect(board).scaledBy(1.3f).movedBy(Vector2D.newRandom(.1f));
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            AImage img = g.getImage(id);
            g.setTransparencyFilter(1f - position);
            g.drawImage(id, rect.fit(img));
            g.removeFilter();
        }

        @Override
        protected boolean hidesActor() {
            return false;
        }
    }

    class ShootAnimation extends ZActorAnimation {

        final ZIcon icon;
        final ZDir dir;
        final GRectangle rect;
        final Vector2D start, path;

        public ShootAnimation(ZActor actor, long duration, int targetZone, ZIcon icon) {
            super(actor, duration);
            this.icon = icon;
            ZZone end   = getBoard().getZone(targetZone);
            rect = actor.getRect(board).scaledBy(.5f);
            start = rect.getCenter();
            Vector2D dv = end.getCenter().sub(start);
            dir = ZDir.getFromVector(dv);
            path = end.getCenter().addEq(Vector2D.newRandom(.3f)).sub(start);
            setDuration(Math.round(path.mag() * duration));
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            int id = ZIcon.ARROW.imageIds[dir.ordinal()];
            AImage img = g.getImage(id);
            Vector2D pos = start.add(path.scaledBy(position));
            g.drawImage(id, rect.fit(img).setCenter(pos));
        }
    }

    class MagicAnimation extends ZActorAnimation {

        Vector2D start, end, dv;
        final int numArcs;
        final float startAngle;
        final float sweepAngle;
        final float radius;

        MagicAnimation(ZActor actor, int targetZone) {
            this(actor, actor.getRect(board).getCenter(), board.getZone(targetZone).getCenter(), 5, 20);
        }

        MagicAnimation(ZActor actor, Vector2D start, Vector2D end, int numArcs, float sweepAngle) {
            super(actor, 1000);
            this.start = start;
            this.end = end;
            this.numArcs = numArcs;
            this.sweepAngle = sweepAngle;
            this.radius = end.sub(start).mag();
            this.startAngle = end.sub(start).angleOf() - sweepAngle / 2;
            this.dv = end.sub(start).scaledBy(1f / numArcs);
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.setColor(GColor.WHITE);
            g.setLineWidth(3);
            float radiusStep = radius / numArcs;
            if (position <= .5f) {
                // draw the arcs emitting from the start
                int numArcsToDraw = Math.round(position * 2 * numArcs);
                g.drawFilledCircle(start, 1);
                float r = radiusStep;
                for (int i=0; i<numArcsToDraw; i++) {
                    g.drawArc(start, r, startAngle, sweepAngle);
                    r += radiusStep;
                }
                g.drawArc(start, position*2*radius, startAngle, sweepAngle);
            } else {
                // draw the arcs backward from end
                int numArcsToDraw = Math.round(2 * (1f - position) * numArcs);
                float r = numArcs*radiusStep;
                for (int i=0; i<numArcsToDraw; i++) {
                    g.drawArc(start, r, startAngle, sweepAngle);
                    r -= radiusStep;
                }
                g.drawArc(start, (position-.5f)*2*radius, startAngle, sweepAngle);
            }
        }
    }

    class EarthquakeAnimation extends ZActorAnimation {
        public EarthquakeAnimation(ZActor actor) {
            super(actor, 2000);
        }

        @Override
        protected void draw(AGraphics g, float position, float dt) {
            g.pushMatrix();
            g.translate(Vector2D.newRandom(position/8));
            g.drawImage(actor.getImageId(), actor.getRect());
            g.popMatrix();
        }

    }*/

    @Override
    protected void onDragonBileThrown(ZCharacter actor, int zone) {
        if (actor.getOccupiedZone() != zone) {
            actor.addAnimation(new ThrowAnimation(actor, board, zone, ZIcon.DRAGON_BILE));
            waitForAnimationToComplete(1000);
        }
    }

    @Override
    protected void onTorchThrown(ZCharacter actor, int zone) {
        if (actor.getOccupiedZone() != zone) {
            actor.addAnimation(new ThrowAnimation(actor, board, zone, ZIcon.TORCH) {

                @Override
                protected void onDone() {
                    super.onDone();
                    boardRenderer.addPostActor(new InfernoAnimation(board, zone));
                }
            });
            waitForAnimationToComplete(1000);
        }
    }

    @Override
    protected void onZombieDestroyed(ZCharacter c, ZAttackType deathType, ZZombie zombie) {
        Lock lock = new Lock();
        switch (deathType) {

            case ELECTROCUTION:
                lock.acquire();
                zombie.addAnimation(new ElectrocutionAnimation(zombie) {
                    @Override
                    protected void onDone() {
                        super.onDone();
                        lock.release();
                    }
                });
                lock.block();
            case FIRE:
            case DISINTEGRATION:
            case BLADE:
            case CRUSH:
            case ARROW:
            case EARTHQUAKE:
            case MENTAL_STRIKE:
            case NORMAL:
            default:
                boardRenderer.addPreActor(new DeathAnimation(zombie));
        }
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
    protected void onCharacterAttacked(ZCharacter character, ZAttackType attackType, boolean perished) {
        switch (attackType) {
            case ELECTROCUTION:
                character.addAnimation(new ElectrocutionAnimation(character));
                break;
            case NORMAL:
            case FIRE:
            case DISINTEGRATION:
            case BLADE:
            case CRUSH:
            case ARROW:
            case EARTHQUAKE:
            case MENTAL_STRIKE:
            default:
                character.addAnimation(new SlashedAnimation(character));
        }
        if (perished) {
            character.addAnimation(new AscendingAngelDeathAnimation(character));
        }
    }


    @Override
    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        boardRenderer.addPostActor(new HoverMessage(board, String.format("+%d EXP", points), c.getRect().getCenter()));
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
        boardRenderer.addPostActor(new HoverMessage(board, String.format("%s Aquired", skill.getLabel()), c.getRect().getCenter()));
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
        boardRenderer.addPostActor(new HoverMessage(board, "CLICK", c.getRect().getCenter()));
    }

    @Override
    protected void onAttack(ZCharacter attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {

        if (actionType == ZActionType.MELEE) {

            Lock animLock = new Lock(numDice);
            for (int i=0; i<numDice; i++) {
                attacker.addAnimation(new MeleeAnimation(attacker, board) {
                    @Override
                    protected void onDone() {
                        super.onDone();
                        animLock.release();
                    }
                });
                Utils.waitNoThrow(this, 100);
            }
            animLock.block();

        } else if (actionType.isRanged()) {

            Lock animLock = new Lock(numDice);
            for (int i=0; i<numDice; i++) {
                attacker.addAnimation(new ShootAnimation(attacker, board,500, targetZone, ZIcon.ARROW) {

                    @Override
                    protected void onDone() {
                        super.onDone();
                        animLock.release();
                    }

                });
                Utils.waitNoThrow(this, 200);
            }
            animLock.block();
        } else if (weapon.isMagic()) {

            switch (weapon.getType()) {
                case DEATH_STRIKE:
                case MANA_BLAST:
                case DISINTEGRATE: {
                    Lock animLock = new Lock(1);
                    attacker.addAnimation(new MagicAnimation(attacker, board, targetZone) {
                        @Override
                        protected void onDone() {
                            super.onDone();
                            animLock.release();
                        }
                    });
                    animLock.block();
                    break;
                }
                case FIREBALL: {
                    Lock animLock = new Lock(numDice);
                    for (int i = 0; i < numDice; i++) {

                        Vector2D end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(0.3f));
                        attacker.addAnimation(new FireballAnimation(attacker, board, end) {
                            @Override
                            protected void onDone() {
                                super.onDone();
                                animLock.release();
                            }
                        });
                        Utils.waitNoThrow(this, 200);
                    }
                    animLock.block();
                    break;
                }
                case INFERNO: {
                    boardRenderer.addPreActor(new InfernoAnimation(board, targetZone));
                    waitForAnimationToComplete(1000);
                    break;
                }
                case LIGHTNING_BOLT: {
                    Lock animLock = new Lock(1);
                    attacker.addAnimation(new LightningAnimation(attacker, board, targetZone, numDice) {
                        @Override
                        protected void onDone() {
                            animLock.release();
                        }
                    });
                    animLock.block();
                    break;
                }
                case EARTHQUAKE: {
                    Lock animLock = new Lock();
                    for (ZZombie z : board.getZombiesInZone(targetZone)) {
                        animLock.acquire();
                        z.addAnimation(new EarthquakeAnimation(z) {
                            @Override
                            protected void onDone() {
                                super.onDone();
                                animLock.release();
                            }
                        });
                    }
                    animLock.block();
                    break;
                }
            }
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
