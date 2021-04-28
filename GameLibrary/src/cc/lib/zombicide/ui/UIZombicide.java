package cc.lib.zombicide.ui;

import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.ui.IButton;
import cc.lib.utils.Lock;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZAttackType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
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
import cc.lib.zombicide.anims.MakeNoiseAnimation;
import cc.lib.zombicide.anims.MeleeAnimation;
import cc.lib.zombicide.anims.OverlayTextAnimation;
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
    protected void onAhhhhhh(ZCharacter c) {
        boardRenderer.addPostActor(new HoverMessage(board, "AHHHHHH!", c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onEquipmentFound(ZCharacter c, ZEquipment equipment) {
        boardRenderer.addPostActor(new HoverMessage(board, "+" + equipment.getLabel(), c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        boardRenderer.addPostActor(new HoverMessage(board, String.format("+%d EXP", points), c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onGameLost() {
        boardRenderer.addOverlay(new OverlayTextAnimation("Y O U   L O S T"));
        waitForAnimationToComplete(1000);
        boardRenderer.setOverlay(getGameSummaryTable());
    }

    @Override
    protected void onQuestComplete() {
        boardRenderer.addOverlay(new OverlayTextAnimation("C O M P L E T E D"));
        waitForAnimationToComplete(1000);
        boardRenderer.setOverlay(getGameSummaryTable());
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("DOUBLE SPAWN X %d", multiplier)));
    }

    @Override
    protected void onNewSkillAquired(ZCharacter c, ZSkill skill) {
        boardRenderer.addPostActor(new HoverMessage(board, String.format("%s Acquired", skill.getLabel()), c.getRect().getCenter()));
        characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.getLabel(), skill.getLabel()));
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        boardRenderer.addPreActor(new MakeNoiseAnimation(zone.getCenter()));

        /*
        new ZAnimation(1000) {
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
        });*/
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
                attacker.addAnimation(new ShootAnimation(attacker, board,300, targetZone, ZIcon.ARROW) {

                    @Override
                    protected void onDone() {
                        super.onDone();
                        animLock.release();
                    }

                });
                Utils.waitNoThrow(this, 100);
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
