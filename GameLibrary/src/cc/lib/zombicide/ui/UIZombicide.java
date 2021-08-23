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
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZombieCategory;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.anims.AscendingAngelDeathAnimation;
import cc.lib.zombicide.anims.DeathAnimation;
import cc.lib.zombicide.anims.DeathStrikeAnimation;
import cc.lib.zombicide.anims.EarthquakeAnimation;
import cc.lib.zombicide.anims.ElectrocutionAnimation;
import cc.lib.zombicide.anims.FireballAnimation;
import cc.lib.zombicide.anims.GroupAnimation;
import cc.lib.zombicide.anims.HoverMessage;
import cc.lib.zombicide.anims.InfernoAnimation;
import cc.lib.zombicide.anims.LightningAnimation2;
import cc.lib.zombicide.anims.MagicOrbAnimation;
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
import cc.lib.zombicide.p2p.ZGameMP;

public abstract class UIZombicide extends ZGameMP {

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
    private String boardMessage;
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

    public String getBoardMessage() {
        return boardMessage;
    }

    public List getOptions() {
        return options;
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

    @Override
    protected void setBoardMessage(String msg) {
        super.setBoardMessage(msg);
        this.boardMessage = msg;
        boardRenderer.redraw();
    }

    public ZPlayerName pickCharacter(String message, List<ZPlayerName> characters) {
        synchronized (this) {
            options = Utils.map(characters, (in)-> in.getCharacter());
            uiMode = UIMode.PICK_CHARACTER;
            setBoardMessage(message);
        }
        ZCharacter ch = waitForUser(ZCharacter.class);
        if (ch != null) {
            return ch.getPlayerName();
        }
        return null;
    }

    public Integer pickZone(String message, List<Integer> zones) {
        synchronized (this) {
            options = zones;
            uiMode = UIMode.PICK_ZONE;
            boardRenderer.redraw();
            setBoardMessage(message);
        }
        return waitForUser(Integer.class);
    }

    public <T> T pickMenu(ZPlayerName name, String message, Class expectedType, List<T> moves) {
        synchronized (this) {
            options = moves;
            uiMode = UIMode.PICK_MENU;
            if (expectedType.equals(ZMove.class))
                boardRenderer.processMoveOptions(name.getCharacter(), (List<ZMove>)moves);
            else
                boardRenderer.processSubMenu(name.getCharacter(), (List<IButton>)moves);
            setBoardMessage(message);
        }
        return (T) waitForUser(expectedType);
    }

    public ZDoor pickDoor(String message, List<ZDoor> doors) {
        synchronized (this) {
            options = doors;
            uiMode = UIMode.PICK_DOOR;
            boardRenderer.redraw();
            setBoardMessage(message);
        }
        return waitForUser(ZDoor.class);
    }

    public void tryWalk(ZDir dir) {
        ZCharacter cur = getCurrentCharacter().getCharacter();
        if (cur != null) {
            if (getBoard().canMove(cur, dir)) {
                setResult(ZMove.newWalkDirMove(dir));
            }
        }
    }

    @Override
    public void addLogMessage(String msg) {
        super.addLogMessage(msg);
        addPlayerComponentMessage(msg);
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
        } else {
            boardRenderer.animateZoomTo(0);
        }
        boardRenderer.redraw();
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
    protected void initQuest(ZQuest quest) {
        boardRenderer.clearTiles();
        showQuestTitleOverlay();
    }


    @Override
    protected void onDragonBileThrown(ZPlayerName actor, int zone) {
        super.onDragonBileThrown(actor, zone);
        if (actor.getCharacter().getOccupiedZone() != zone) {
            actor.getCharacter().addAnimation(new ThrowAnimation(actor.getCharacter(), board, zone, ZIcon.DRAGON_BILE));
            waitForAnimationToComplete(1000);
        }
    }

    @Override
    protected void onRollDice(Integer[] roll) {
        super.onRollDice(roll);
        characterRenderer.addWrappable(new ZDiceWrappable(roll));
    }

    @Override
    protected void onTorchThrown(ZPlayerName actor, int zone) {
        super.onTorchThrown(actor, zone);
        Lock animLock = new Lock(1);
        actor.getCharacter().addAnimation(new ThrowAnimation(actor.getCharacter(), board, zone, ZIcon.TORCH) {
            @Override
            protected void onDone() {
                super.onDone();
                animLock.release();
            }
        });
        boardRenderer.redraw();
        animLock.block();
    }

    @Override
    protected void onDragonBileExploded(int zone) {
        super.onDragonBileExploded(zone);
        boardRenderer.addPreActor(new InfernoAnimation(board, zone).start());
        Utils.waitNoThrow(this, 1000);
    }

    @Override
    protected void onZombieDestroyed(ZPlayerName c, ZAttackType deathType, ZZombie zombie) {
        super.onZombieDestroyed(c, deathType, zombie);
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
                boardRenderer.redraw();
                lock.block();
            case FIRE:
            case DISINTEGRATION:
            case BLADE:
            case CRUSH:
            case RANGED_ARROWS:
            case RANGED_BOLTS:
            case RANGED_THROW:
            case EARTHQUAKE:
            case MENTAL_STRIKE:
            case NORMAL:
            default:
                boardRenderer.addPreActor(new DeathAnimation(zombie));
        }
    }

    @Override
    protected void onActorMoved(ZActor actor, GRectangle start, GRectangle end, long speed) {
        super.onActorMoved(actor, start, end, speed);
        actor.addAnimation(new MoveAnimation(actor, start,end, speed));
        boardRenderer.redraw();
    }

    @Override
    protected void onZombieSpawned(ZZombie zombie) {
        super.onZombieSpawned(zombie);
        zombie.addAnimation(new SpawnAnimation(zombie, board));
        if (zombie.getType() == ZZombieType.Abomination) {
            boardRenderer.addOverlay(new OverlayTextAnimation("A B O M I N A T I O N ! !"));
        }
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterDefends(ZPlayerName cur) {
        super.onCharacterDefends(cur);
        cur.getCharacter().addAnimation(new ShieldBlockAnimation(cur.getCharacter()));
        boardRenderer.redraw();
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
    protected void onCharacterAttacked(ZPlayerName character, ZAttackType attackType, boolean perished) {
        super.onCharacterAttacked(character, attackType, perished);
        switch (attackType) {
            case ELECTROCUTION:
                character.getCharacter().addAnimation(new ElectrocutionAnimation(character.getCharacter()));
                break;
            case NORMAL:
            case FIRE:
            case DISINTEGRATION:
            case BLADE:
            case CRUSH:
            case RANGED_ARROWS:
            case RANGED_BOLTS:
            case RANGED_THROW:
            case EARTHQUAKE:
            case MENTAL_STRIKE:
            default:
                character.getCharacter().addAnimation(new SlashedAnimation(character.getCharacter()));
        }
        if (perished) {
            character.getCharacter().addAnimation(new AscendingAngelDeathAnimation(character.getCharacter()));
            // at the end of the 'ascending angel' grow a tombstone
            character.getCharacter().addAnimation(new ZActorAnimation(character.getCharacter(), 2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    GRectangle rect = new GRectangle(actor.getRect());
                    rect.y += rect.h*(1f-position);
                    rect.h *= position;
                    g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect);
                }
            });

        }
        boardRenderer.redraw();
    }

    @Override
    protected void onAhhhhhh(ZPlayerName c) {
        super.onAhhhhhh(c);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "AHHHHHH!", c.getCharacter().getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onEquipmentFound(ZPlayerName c, ZEquipment equipment) {
        super.onEquipmentFound(c, equipment);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "+" + equipment.getLabel(), c.getCharacter().getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onCharacterGainedExperience(ZPlayerName c, int points) {
        super.onCharacterGainedExperience(c, points);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("+%d EXP", points), c.getCharacter().getRect().getCenter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onGameLost() {
        super.onGameLost();
        boardRenderer.addOverlay(new OverlayTextAnimation("Y O U   L O S T") {
            @Override
            protected void onDone() {
                super.onDone();
                boardRenderer.setOverlay(getGameSummaryTable());
            }
        });
    }

    @Override
    protected void onQuestComplete() {
        super.onQuestComplete();
        boardRenderer.addOverlay(new OverlayTextAnimation("C O M P L E T E D") {
            @Override
            protected void onDone() {
                super.onDone();
                boardRenderer.setOverlay(getGameSummaryTable());
            }
        });
    }

    @Override
    protected void onDoubleSpawn(int multiplier) {
        super.onDoubleSpawn(multiplier);
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("DOUBLE SPAWN X %d", multiplier)));
    }

    @Override
    protected void onNewSkillAquired(ZPlayerName c, ZSkill skill) {
        super.onNewSkillAquired(c, skill);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Acquired", skill.getLabel()), c.getCharacter().getRect().getCenter()));
        characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.getLabel(), skill.getLabel()));
    }

    @Override
    protected void onExtraActivation(ZZombieCategory category) {
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("EXTRA ACTIVATION %s", category)));
    }

    @Override
    protected void onReaperKill(ZPlayerName c, ZZombie z, ZWeapon w, ZActionType at) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "Reaper Kill!!", z.getRect().getCenter()));
    }

    @Override
    protected void onWeaponReloaded(ZPlayerName c, ZWeapon w) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Reloaded", w.getLabel()), c.getCharacter().getRect().getCenter()));
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        super.onNoiseAdded(zoneIndex);
        ZZone zone = board.getZone(zoneIndex);
        boardRenderer.addPreActor(new MakeNoiseAnimation(zone.getCenter()));
        waitForAnimationToComplete(1000);
    }

    @Override
    protected void onWeaponGoesClick(ZPlayerName c, ZWeapon weapon) {
        super.onWeaponGoesClick(c, weapon);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "CLICK", c.getCharacter().getRect().getCenter()));
    }

    @Override
    protected void onAttack(ZPlayerName attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {
        super.onAttack(attacker, weapon, actionType, numDice, numHits, targetZone);
        if (actionType == ZActionType.MELEE) {

            //GroupAnimation group = new GroupAnimation(attacker);
            //attacker.addAnimation(group);
            Lock animLock = new Lock(numDice);
            float currentZoom = boardRenderer.getZoomPercent();
            if (currentZoom < 1)
                attacker.getCharacter().addAnimation(new ZoomAnimation(attacker.getCharacter(), boardRenderer, 1));
            for (int i=0; i<numDice; i++) {
                attacker.getCharacter().addAnimation(new MeleeAnimation(attacker.getCharacter(), board) {
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

            GroupAnimation group = new GroupAnimation(attacker.getCharacter());
            attacker.getCharacter().addAnimation(group);
            Lock animLock = new Lock(numDice);
            for (int i=0; i<numDice; i++) {
                group.addAnimation(new ShootAnimation(attacker.getCharacter(), board,300, targetZone, ZIcon.ARROW) {

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
                case DEATH_STRIKE: {
                    Lock animLock = new Lock(numDice);
                    for (int i=0; i<numDice; i++) {
                        GRectangle zoneRect = board.getZone(targetZone).getRectangle();
                        GRectangle targetRect = zoneRect.scaledBy(.5f);//.moveBy(0, -1);
                        attacker.getCharacter().addAnimation(new DeathStrikeAnimation(attacker.getCharacter(), targetRect) {
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
                case MANA_BLAST:
                case DISINTEGRATE: {
                    Lock animLock = new Lock(1);
                    attacker.getCharacter().addAnimation(new MagicOrbAnimation(attacker.getCharacter(), board.getZone(targetZone).getCenter()) {
                        @Override
                        protected void onDone() {
                            super.onDone();
                            animLock.release();
                        }
                    });
                    boardRenderer.redraw();
                    animLock.block();
                    break;
                }
                case FIREBALL: {
                    GroupAnimation group = new GroupAnimation(attacker.getCharacter());
                    attacker.getCharacter().addAnimation(group);
                    Lock animLock = new Lock(numDice);
                    for (int i = 0; i < numDice; i++) {
                        Vector2D end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(0.3f));
                        group.addAnimation(new FireballAnimation(attacker.getCharacter(), end) {
                            @Override
                            protected void onDone() {
                                animLock.release();
                            }
                        });
                        Utils.waitNoThrow(this, 150);
                    }
                    boardRenderer.redraw();
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
                    attacker.getCharacter().addAnimation(new LightningAnimation2(attacker.getCharacter(), board.getZone(targetZone).getRectangle().scaledBy(.5f), numDice) {
                        @Override
                        protected void onDone() {
                            animLock.release();
                        }
                    });
                    boardRenderer.redraw();
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

    @Override
    protected void onZombiePath(ZZombie zombie, List<ZDir> path) {
        super.onZombiePath(zombie, path);
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
    protected void onCharacterOpenedDoor(ZPlayerName cur, ZDoor door) {
        super.onCharacterOpenedDoor(cur, door);
    }

    @Override
    protected void onCharacterOpenDoorFailed(ZPlayerName cur, ZDoor door) {
        super.onCharacterOpenDoorFailed(cur, door);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "Open Failed", door.getRect(board).getCenter()));
    }

    @Override
    public void onIronRain(ZPlayerName c, int targetZone) {
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "LET IT RAIN!!", getBoard().getZone(targetZone).getCenter()));
    }

    @Override
    protected void onDoorUnlocked(ZDoor door) {
        super.onDoorUnlocked(door);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "DOOR UNLOCKED", door.getRect(getBoard()).getCenter()));
    }

    @Override
    protected void onBonusAction(ZPlayerName pl, ZSkill action) {
        super.onBonusAction(pl, action);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "BONUS ACTION " + action.getLabel(), pl.getCharacter().getRect().getCenter()));
    }
}
