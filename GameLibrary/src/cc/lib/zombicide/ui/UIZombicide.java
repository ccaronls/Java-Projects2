package cc.lib.zombicide.ui;

import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.ui.IButton;
import cc.lib.utils.Lock;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZAttackType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
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
import cc.lib.zombicide.anims.GroupAnimation;
import cc.lib.zombicide.anims.HoverMessage;
import cc.lib.zombicide.anims.InfernoAnimation;
import cc.lib.zombicide.anims.LightningAnimation;
import cc.lib.zombicide.anims.MagicAnimation;
import cc.lib.zombicide.anims.MakeNoiseAnimation;
import cc.lib.zombicide.anims.MeleeAnimation;
import cc.lib.zombicide.anims.MoveAnimation;
import cc.lib.zombicide.anims.OverlayTextAnimation;
import cc.lib.zombicide.anims.ShieldBlockAnimation;
import cc.lib.zombicide.anims.ShootAnimation;
import cc.lib.zombicide.anims.SlashedAnimation;
import cc.lib.zombicide.anims.SpawnAnimation;
import cc.lib.zombicide.anims.ThrowAnimation;
import cc.lib.zombicide.anims.ZoomAnimation;

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
    protected void onRollDice(Integer[] roll) {
        characterRenderer.addWrappable(new ZDiceWrappable(roll));
    }

    @Override
    protected void onTorchThrown(ZCharacter actor, int zone) {
        Lock animLock = new Lock(1);
        actor.addAnimation(new ThrowAnimation(actor, board, zone, ZIcon.TORCH) {
            @Override
            protected void onDone() {
                super.onDone();
                animLock.release();
            }
        });
        animLock.block();
    }

    @Override
    protected void onDragonBileExploded(int zone) {
        boardRenderer.addPreActor(new InfernoAnimation(board, zone).start());
        Utils.waitNoThrow(this, 1000);
    }

    @Override
    protected void onDoNothing(ZCharacter c) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "Zzzzz", c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);    }

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
        actor.addAnimation(new MoveAnimation(actor, start,end, speed));
        boardRenderer.redraw();
//        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onZombieSpawned(ZZombie zombie) {
        zombie.addAnimation(new SpawnAnimation(zombie, board));
    }

    @Override
    protected void onCharacterDefends(ZCharacter cur) {
        cur.addAnimation(new ShieldBlockAnimation(cur));
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
            // at the end of the 'ascending angel' grow a tombstone
            character.addAnimation(new ZActorAnimation(character, 2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    GRectangle rect = new GRectangle(actor.getRect());
                    rect.y += rect.h*(1f-position);
                    rect.h *= position;
                    g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect);
                }
            });

        }
    }

    @Override
    protected void onAhhhhhh(ZCharacter c) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "AHHHHHH!", c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onEquipmentFound(ZCharacter c, ZEquipment equipment) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "+" + equipment.getLabel(), c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onCharacterGainedExperience(ZCharacter c, int points) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("+%d EXP", points), c.getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onGameLost() {
        boardRenderer.setOverlay(new OverlayTextAnimation("Y O U   L O S T") {
            @Override
            protected void onDone() {
                super.onDone();
                boardRenderer.setOverlay(getGameSummaryTable());
            }
        });
    }

    @Override
    protected void onQuestComplete() {
        boardRenderer.setOverlay(new OverlayTextAnimation("C O M P L E T E D") {
            @Override
            protected void onDone() {
                super.onDone();
                boardRenderer.setOverlay(getGameSummaryTable());
            }
        });
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("DOUBLE SPAWN X %d", multiplier)));
    }

    @Override
    protected void onNewSkillAquired(ZCharacter c, ZSkill skill) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Acquired", skill.getLabel()), c.getRect().getCenter()));
        characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.getLabel(), skill.getLabel()));
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        boardRenderer.addPreActor(new MakeNoiseAnimation(zone.getCenter()));
        waitForAnimationToComplete(1000);
    }

    @Override
    protected void onWeaponGoesClick(ZCharacter c, ZWeapon weapon) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "CLICK", c.getRect().getCenter()));
    }

    @Override
    protected void onAttack(ZCharacter attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {

        if (actionType == ZActionType.MELEE) {

            //GroupAnimation group = new GroupAnimation(attacker);
            //attacker.addAnimation(group);
            Lock animLock = new Lock(numDice);
            float currentZoom = boardRenderer.getZoomPercent();
            if (currentZoom < 1)
                attacker.addAnimation(new ZoomAnimation(attacker, boardRenderer, 1));
            for (int i=0; i<numDice; i++) {
                attacker.addAnimation(new MeleeAnimation(attacker, board) {
                    @Override
                    protected void onDone() {
                        super.onDone();
                        animLock.release();
                    }
                });
                //Utils.waitNoThrow(this, 100);
            }
            boardRenderer.redraw();
            animLock.block();
            boardRenderer.animateZoomTo(currentZoom);
        } else if (actionType.isRanged()) {

            GroupAnimation group = new GroupAnimation(attacker);
            attacker.addAnimation(group);
            Lock animLock = new Lock(numDice);
            for (int i=0; i<numDice; i++) {
                group.addAnimation(new ShootAnimation(attacker, board,300, targetZone, ZIcon.ARROW) {

                    @Override
                    protected void onDone() {
                        animLock.release();
                    }

                });
                Utils.waitNoThrow(this, 100);
            }
            boardRenderer.redraw();
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
                    GroupAnimation group = new GroupAnimation(attacker);
                    attacker.addAnimation(group);
                    Lock animLock = new Lock(numDice);
                    for (int i = 0; i < numDice; i++) {
                        Vector2D end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(0.3f));
                        group.addAnimation(new FireballAnimation(attacker, end) {
                            @Override
                            protected void onDone() {
                                animLock.release();
                            }
                        });
                        Utils.waitNoThrow(this, 150);
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
                    boardRenderer.redraw();
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
        if (cur != null && cur.getActionsLeftThisTurn() > 0) {
            if (getBoard().canMove(cur, dir)) {
                setResult(ZMove.newWalkDirMove(dir));
            }
        }
    }

    public void trySwitchActivePlayer() {
        boardRenderer.dragOffset.zero();
        if (getCurrentCharacter() == null) {
            for (ZPlayerName nm : getCurrentUser().getCharacters()) {
                if (nm.getCharacter().isAlive() && nm.getCharacter().getActionsLeftThisTurn() > 0) {
                    setResult(nm.getCharacter());
                    break;
                }
            }
        } else if (canSwitchActivePlayer()) {
            setResult(ZMove.newSwitchActiveCharacter());
        }
        boardRenderer.redraw();
    }

    @Override
    protected void initQuest(ZQuest quest) {
        boardRenderer.clearTiles();
        showQuestTitleOverlay();
    }

    @Override
    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {

        /*
        final Vector2D start = zombie.getRect().getCenter();
        boardRenderer.addPostActor(new ZAnimation(1000) {

            @Override
            protected void draw(AGraphics g, float position, float dt) {
                GColor pathColor = GColor.YELLOW.withAlpha(1f-position);
                g.setColor(pathColor);
                g.begin();
                g.vertex(start);
                MutableVector2D next = new MutableVector2D(start);
                for (ZDir dir : path) {
                    next.addEq(dir.dx, dir.dy);
                    g.vertex(next);
                }
                g.drawLineStrip(3);
            }
        });*/

    }

    @Override
    protected void onCharacterOpenedDoor(ZCharacter cur, ZDoor door) {

    }

    @Override
    protected void onCharacterOpenDoorFailed(ZCharacter cur, ZDoor door) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "Open Failed", door.getRect(board).getCenter()));
    }

    public void showObjectivesOverlay() {
        boardRenderer.setOverlay(getQuest().getObjectivesOverlay(this));
    }

    public void showQuestTitleOverlay() {
        boardRenderer.setOverlay(new OverlayTextAnimation(getQuest().getName()) {
            @Override
            protected void onDone() {
                super.onDone();
                showObjectivesOverlay();
            }
        });
    }

    public void showSummaryOverlay() {
        boardRenderer.setOverlay(getGameSummaryTable());
    }

    @Override
    public void onIronRain(ZCharacter c, int targetZone) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "LET IT RAIN!!", getBoard().getZone(targetZone).getCenter()));
    }

    @Override
    protected void onDoorUnlocked(ZDoor door) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "DOOR UNLOCKED", door.getRect(getBoard()).getCenter()));
    }
}
