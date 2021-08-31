package cc.lib.zombicide.ui;

import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
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
import cc.lib.zombicide.ZActorPosition;
import cc.lib.zombicide.ZAttackType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZMoveType;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZQuest;
import cc.lib.zombicide.ZSkill;
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZombieCategory;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.anims.AscendingAngelDeathAnimation;
import cc.lib.zombicide.anims.DeathAnimation;
import cc.lib.zombicide.anims.DeathStrikeAnimation;
import cc.lib.zombicide.anims.EarthquakeAnimation;
import cc.lib.zombicide.anims.ElectrocutionAnimation;
import cc.lib.zombicide.anims.EmptyAnimation;
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
        PICK_SPAWN,
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

    public Integer pickSpawn(String message, List<ZSpawnArea> areas) {
        synchronized (this) {
            options = areas;
            uiMode = UIMode.PICK_SPAWN;
            boardRenderer.redraw();
            setBoardMessage(message);
        }
        ZSpawnArea area = waitForUser(ZSpawnArea.class);
        if (area == null)
            return null;
        return areas.indexOf(area);
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
        List<ZMove> moves = options == null ? Collections.emptyList() : (List)Utils.filter(options, option -> option instanceof ZMove);
        List<ZMoveType> types = Utils.map(moves, option->option.type);
        if (types.contains(ZMoveType.WALK) && canWalk(dir)) {
            ZCharacter cur = getCurrentCharacter().getCharacter();
            if (cur != null) {
                if (getBoard().canMove(cur, dir)) {
                    setResult(ZMove.newWalkDirMove(dir));
                }
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
        boardRenderer.setOverlay(new OverlayTextAnimation(getQuest().getName(), boardRenderer.getNumOverlayTextAnimations()) {
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
    protected void onEquipmentThrown(ZPlayerName actor, ZIcon icon, int zone) {
        super.onEquipmentThrown(actor, icon, zone);
        Lock animLock = new Lock(1);
        if (actor.getCharacter().getOccupiedZone() != zone) {
            actor.getCharacter().addAnimation(new ThrowAnimation(actor.getCharacter(), board, zone, icon) {
                @Override
                protected void onDone() {
                    super.onDone();
                    animLock.release();
                }
            });
            boardRenderer.redraw();
            animLock.block();
        }
    }

    @Override
    protected void onRollDice(Integer[] roll) {
        super.onRollDice(roll);
        characterRenderer.addWrappable(new ZDiceWrappable(roll));
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
            boardRenderer.addOverlay(new OverlayTextAnimation("A B O M I N A T I O N ! !", boardRenderer.getNumOverlayTextAnimations()));
            Utils.waitNoThrow(this, 500);
        }
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterDefends(ZPlayerName cur, ZActorPosition attackerPosition) {
        super.onCharacterDefends(cur, attackerPosition);
        ZActor actor = board.getActor(attackerPosition);
        actor.addAnimation(new EmptyAnimation(actor) {
            @Override
            protected void onDone() {
                super.onDone();
                cur.getCharacter().addAnimation(new ShieldBlockAnimation(cur.getCharacter()));
            }
        });
        boardRenderer.redraw();
    }

    @Override
    protected void onCharacterAttacked(ZPlayerName character, ZActorPosition attackerPosition, ZAttackType attackType, boolean perished) {
        super.onCharacterAttacked(character, attackerPosition, attackType, perished);
        ZActor attacker = board.getActor(attackerPosition);
        switch (attackType) {
            case ELECTROCUTION:
                attacker.addAnimation(new EmptyAnimation(attacker) {
                    @Override
                    protected void onDone() {
                        super.onDone();
                        character.getCharacter().addAnimation(new ElectrocutionAnimation(character.getCharacter()));
                    }
                });
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
                attacker.addAnimation(new EmptyAnimation(attacker) {
                    @Override
                    protected void onDone() {
                        super.onDone();
                        character.getCharacter().addAnimation(new SlashedAnimation(character.getCharacter()));
                    }
                });
        }
        if (perished) {
            character.getCharacter().addAnimation(new AscendingAngelDeathAnimation(character.getCharacter()));
            // at the end of the 'ascending angel' grow a tombstone
            character.getCharacter().addAnimation(new ZActorAnimation(character.getCharacter(), 2000) {
                @Override
                protected void draw(AGraphics g, float position, float dt) {
                    AImage img = g.getImage(ZIcon.GRAVESTONE.imageIds[0]);
                    GRectangle rect = new GRectangle(actor.getRect().fit(img));
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
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "AHHHHHH!", c.getCharacter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onEquipmentFound(ZPlayerName c, ZEquipment equipment) {
        super.onEquipmentFound(c, equipment);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "+" + equipment.getLabel(), c.getCharacter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onCharacterGainedExperience(ZPlayerName c, int points) {
        super.onCharacterGainedExperience(c, points);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("+%d EXP", points), c.getCharacter()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onGameLost() {
        super.onGameLost();
        boardRenderer.addOverlay(new OverlayTextAnimation("Y O U   L O S T", boardRenderer.getNumOverlayTextAnimations()) {
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
        boardRenderer.addOverlay(new OverlayTextAnimation("C O M P L E T E D", 0) {
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
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("DOUBLE SPAWN X %d", multiplier), boardRenderer.getNumOverlayTextAnimations()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onNewSkillAquired(ZPlayerName c, ZSkill skill) {
        super.onNewSkillAquired(c, skill);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Acquired", skill.getLabel()), c.getCharacter()));
        characterRenderer.addMessage(String.format("%s has aquired the %s skill", c.getLabel(), skill.getLabel()));
    }

    @Override
    protected void onExtraActivation(ZZombieCategory category) {
        super.onExtraActivation(category);
        boardRenderer.addOverlay(new OverlayTextAnimation(String.format("EXTRA ACTIVATION %s", category), boardRenderer.getNumOverlayTextAnimations()));
        Utils.waitNoThrow(this, 500);
    }

    @Override
    protected void onSkillKill(ZPlayerName c, ZSkill skill, ZZombie z, ZAttackType attackType) {
        super.onSkillKill(c, skill, z, attackType);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Kill!!", skill.getLabel()), z));
    }

    @Override
    protected void onRollSixApplied(ZPlayerName c, ZSkill skill) {
        super.onRollSixApplied(c, skill);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("Roll Six!! %s", skill.getLabel()), c.getCharacter()));
    }

    @Override
    protected void onWeaponReloaded(ZPlayerName c, ZWeapon w) {
        super.onWeaponReloaded(c, w);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("%s Reloaded", w.getLabel()), c.getCharacter()));
    }

    @Override
    protected void onNoiseAdded(int zoneIndex) {
        Lock lock = new Lock(1);
        super.onNoiseAdded(zoneIndex);
        ZZone zone = board.getZone(zoneIndex);
        boardRenderer.addPreActor(new MakeNoiseAnimation(zone.getCenter()) {
            @Override
            protected void onDone() {
                super.onDone();
                lock.release();
            }
        });
        boardRenderer.redraw();
        lock.block();
    }

    @Override
    protected void onWeaponGoesClick(ZPlayerName c, ZWeapon weapon) {
        super.onWeaponGoesClick(c, weapon);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "CLICK", c.getCharacter()));
    }

    @Override
    protected void onAttack(ZPlayerName attacker, ZWeapon weapon, ZActionType actionType, int numDice, int numHits, int targetZone) {
        super.onAttack(attacker, weapon, actionType, numDice, numHits, targetZone);
        if (actionType.isMelee()) {
            switch (weapon.getType()) {
                case EARTHQUAKE_HAMMER: {
                    Lock animLock = new Lock(numDice);
                    float currentZoom = boardRenderer.getZoomPercent();
                    List<ZZombie> inZone = board.getZombiesInZone(targetZone);
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
                        GroupAnimation g = new GroupAnimation(attacker.getCharacter()) {
                            @Override
                            protected void onDone() {
                                super.onDone();
                            }
                        };
                        for (ZZombie z : inZone) {
                            g.addAnimation(0, new EarthquakeAnimation(z, attacker.getCharacter(), 300));
                        }
                        attacker.getCharacter().addAnimation(g);
                    }
                    boardRenderer.redraw();
                    animLock.block();
                    boardRenderer.animateZoomTo(currentZoom);
                    break;
                }

                default: {
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
                    }
                    boardRenderer.redraw();
                    animLock.block();
                    boardRenderer.animateZoomTo(currentZoom);
                }
            }
        } else if (actionType.isRanged()) {

            GroupAnimation group = new GroupAnimation(attacker.getCharacter());
            Lock animLock = new Lock(numDice);
            int delay = 100;
            for (int i=0; i<numDice; i++) {
                group.addAnimation(delay, new ShootAnimation(attacker.getCharacter(), board,300, targetZone, ZIcon.ARROW) {

                    @Override
                    protected void onDone() {
                        animLock.release();
                    }

                });
                delay += 100;
            }
            attacker.getCharacter().addAnimation(group);
            boardRenderer.redraw();
            animLock.block();
        } else if (weapon.isMagic()) {

            switch (weapon.getType()) {
                case DEATH_STRIKE: {
                    Lock animLock = new Lock(1);
                    GRectangle zoneRect = board.getZone(targetZone).getRectangle();
                    GRectangle targetRect = zoneRect.scaledBy(.5f);//.moveBy(0, -1);
                    attacker.getCharacter().addAnimation(new DeathStrikeAnimation(attacker.getCharacter(), targetRect, numDice) {
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
                case MANA_BLAST:
                case DISINTEGRATE: {
                    // TODO: Disintegrate should look meaner than mana blast
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
                    Lock animLock = new Lock(numDice);
                    int delay = 0;
                    for (int i = 0; i < numDice; i++) {
                        Vector2D end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(0.3f));
                        group.addAnimation(delay, new FireballAnimation(attacker.getCharacter(), end) {
                            @Override
                            protected void onDone() {
                                animLock.release();
                            }
                        });
                        delay+=150;
                    }
                    attacker.getCharacter().addAnimation(group);
                    boardRenderer.redraw();
                    animLock.block();
                    break;
                }
                case INFERNO: {
                    Lock lock = new Lock(1);
                    boardRenderer.addPreActor(new InfernoAnimation(board, targetZone) {
                        @Override
                        protected void onDone() {
                            super.onDone();
                            lock.release();
                        }
                    });
                    boardRenderer.redraw();
                    lock.block();
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
    protected void onCharacterHealed(ZPlayerName c, int amt) {
        super.onCharacterHealed(c, amt);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, String.format("+%d wounds healed",amt), c.getCharacter()));
    }

    @Override
    protected void onCharacterDestroysSpawn(ZPlayerName c, int zoneIdx) {
        super.onCharacterDestroysSpawn(c, zoneIdx);
    }

    @Override
    protected void onCharacterOpenDoorFailed(ZPlayerName cur, ZDoor door) {
        super.onCharacterOpenDoorFailed(cur, door);
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "Open Failed", door.getRect(board).getCenter()));
    }

    @Override
    public void onIronRain(ZPlayerName c, int targetZone) {
        super.onIronRain(c, targetZone);
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
        boardRenderer.addPostActor(new HoverMessage(boardRenderer, "BONUS ACTION " + action.getLabel(), pl.getCharacter()));
    }
}
