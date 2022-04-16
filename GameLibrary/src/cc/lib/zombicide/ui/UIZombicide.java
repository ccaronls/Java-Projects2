package cc.lib.zombicide.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IInterpolator;
import cc.lib.game.IRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.Vector2D;
import cc.lib.utils.Lock;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZActorAnimation;
import cc.lib.zombicide.ZActorPosition;
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
import cc.lib.zombicide.ZSpawnArea;
import cc.lib.zombicide.ZUser;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZombieCategory;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.anims.AscendingAngelDeathAnimation;
import cc.lib.zombicide.anims.DeathAnimation;
import cc.lib.zombicide.anims.DeathStrikeAnimation;
import cc.lib.zombicide.anims.DeflectionAnimation;
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
import cc.lib.zombicide.anims.StaticAnimation;
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

    public abstract ZUser getThisUser();

    public void refresh() {
        boardRenderer.redraw();
        characterRenderer.redraw();
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

    public void undo() {}

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
        boardRenderer.setOverlay(null);
        this.result = result;
        synchronized (monitor) {
            monitor.notify();
        }
        refresh();
    }

    @Override
    public void setCurrentUser(String name) {
        super.setCurrentUser(name);
        setBoardMessage(name + "'s Turn");
    }

    protected void setBoardMessage(String msg) {
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
                boardRenderer.processMoveOptions(name.getCharacter(), moves);
            else
                boardRenderer.processSubMenu(name.getCharacter(), moves);
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

    @Override
    public void addLogMessage(String msg) {
        super.addLogMessage(msg);
        addPlayerComponentMessage(msg);
    }

    public void showObjectivesOverlay() {
        boardRenderer.setOverlay(getQuest().getObjectivesOverlay(this));
    }

    public <T extends ZEquipment> void showEquipmentOverlay(ZPlayerName player, List<T> list) {
        Table table = new Table(new Table.Model() {
            @Override
            public int getMaxCharsPerLine() {
                return 32;
            }
        });
        for (ZEquipment t : list) {
            table.addColumnNoHeaderVarArg(t.getCardInfo(player.getCharacter(), this));
        }
        boardRenderer.setOverlay(table);

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
    }


    @Override
    protected void onEquipmentThrown(ZPlayerName actor, ZIcon icon, int zone) {
        super.onEquipmentThrown(actor, icon, zone);
        Lock animLock = new Lock(1);
        if (actor.getCharacter().getOccupiedZone() != zone) {
            actor.getCharacter().addAnimation(new ThrowAnimation(actor.getCharacter(), board.getZone(zone).getCenter(), icon) {
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
        List<IRectangle> rects = Utils.map(board.getZone(zone).getCells(), pos -> board.getCell(pos));
        boardRenderer.addPreActor(new InfernoAnimation(rects).start());
        Utils.waitNoThrow(this, 1000);
    }

    @Override
    protected void onZombieDestroyed(ZPlayerName c, ZAttackType deathType, ZActorPosition pos) {
        super.onZombieDestroyed(c, deathType, pos);
        ZZombie zombie = board.getActor(pos);
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
                zombie.addAnimation(new DeathAnimation(zombie));
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
        actor.addAnimation(new ShieldBlockAnimation(cur.getCharacter()));
        boardRenderer.redraw();
    }

    @Override
    protected void onCurrentCharacterUpdated(ZPlayerName priorPlayer, ZPlayerName player) {
        super.onCurrentCharacterUpdated(priorPlayer, player);
        if (priorPlayer != null) {
            Lock animLock = new Lock(1);
            // Add an animation to end of any existing animations to block until all are completed
            priorPlayer.getCharacter().addAnimation(new EmptyAnimation(priorPlayer.getCharacter()) {
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
    protected void onCharacterAttacked(ZPlayerName character, ZActorPosition attackerPosition, ZAttackType attackType, boolean perished) {
        super.onCharacterAttacked(character, attackerPosition, attackType, perished);
        ZActor attacker = board.getActor(attackerPosition);
        switch (attackType) {
            case ELECTROCUTION:
                attacker.addAnimation(new ElectrocutionAnimation(character.getCharacter()));
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
                attacker.addAnimation(new SlashedAnimation(character.getCharacter()));
        }
        if (perished) {
            attacker.addAnimation(new AscendingAngelDeathAnimation(character.getCharacter()));
            // at the end of the 'ascending angel' grow a tombstone
            attacker.addAnimation(new ZActorAnimation(character.getCharacter(), 2000) {
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
    protected void onEquipmentFound(ZPlayerName c, List<ZEquipment> equipment) {
        super.onEquipmentFound(c, equipment);
        if (getThisUser().getPlayers().contains(c)) {
            Table info = new Table().setModel(new Table.Model() {
                @Override
                public float getCornerRadius() {
                    return 20;
                }

                @Override
                public GColor getBackgroundColor() {
                    return GColor.TRANSLUSCENT_BLACK;
                }
            });
            info.addRowList(Utils.map(equipment, e -> e.getCardInfo(c.getCharacter(), this)));
            boardRenderer.setOverlay(info);
        } else {
            for (ZEquipment e : equipment) {
                boardRenderer.addPostActor(new HoverMessage(boardRenderer, "+" + e.getLabel(), c.getCharacter()));
                Utils.waitNoThrow(this, 500);
            }
        }
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
        boardRenderer.waitForAnimations();
        boardRenderer.addOverlay(new OverlayTextAnimation("Y O U   L O S T", boardRenderer.getNumOverlayTextAnimations()) {
            @Override
            protected void onDone() {
                super.onDone();
                showSummaryOverlay();
            }
        });
    }

    @Override
    protected void onQuestComplete() {
        super.onQuestComplete();
        boardRenderer.waitForAnimations();
        boardRenderer.addOverlay(new OverlayTextAnimation("C O M P L E T E D", 0) {
            @Override
            protected void onDone() {
                super.onDone();
                showSummaryOverlay();
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
    protected void onBeginRound(int roundNum) {
        super.onBeginRound(roundNum);
        boardRenderer.waitForAnimations();
        if (roundNum == 0)
            showQuestTitleOverlay();
    }

    @Override
    protected void onAttack(ZPlayerName _attacker, ZWeapon weapon, ZActionType actionType, int numDice, List<ZActorPosition> hits, int targetZone) {
        super.onAttack(_attacker, weapon, actionType, numDice, hits, targetZone);
        ZCharacter attacker = _attacker.getCharacter();
        if (actionType.isMelee()) {
            switch (weapon.getType()) {
                case EARTHQUAKE_HAMMER: {
                    Lock animLock = new Lock(numDice);
                    float currentZoom = boardRenderer.getZoomPercent();
                    if (currentZoom < 1)
                        attacker.addAnimation(new ZoomAnimation(attacker, boardRenderer, 1));
                    for (int i = 0; i < numDice; i++) {
                        attacker.addAnimation(new MeleeAnimation(attacker, board) {
                            @Override
                            protected void onDone() {
                                super.onDone();
                                animLock.release();
                            }
                        });
                        GroupAnimation g = new GroupAnimation(attacker) {
                            @Override
                            protected void onDone() {
                                super.onDone();
                            }
                        };
                        for (ZActorPosition pos : hits) {
                            ZActor z = board.getActor(pos);
                            if (pos.getData() == ZGame.ACTOR_POS_DATA_DAMAGED)
                                g.addAnimation(0, new EarthquakeAnimation(z, attacker, 300));
                            else
                                g.addAnimation(0, new ShieldBlockAnimation(z));
                        }
                        attacker.addAnimation(g);
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
                        attacker.addAnimation(new ZoomAnimation(attacker, boardRenderer, 1));
                    for (int i = 0; i < numDice; i++) {
                        if (i < hits.size()) {
                            ZActorPosition pos = hits.get(i);
                            ZActor victim = board.getActor(pos);
                            Utils.assertTrue(victim != attacker);
                            attacker.addAnimation(new MeleeAnimation(attacker, board) {
                                @Override
                                protected void onDone() {
                                    super.onDone();
                                    if (pos.getData() == ZGame.ACTOR_POS_DATA_DEFENDED) {
                                        victim.addAnimation(new ShieldBlockAnimation(victim));
                                    } else {
                                        victim.addAnimation(new SlashedAnimation(victim));
                                    }
                                    animLock.release();
                                }
                            });
                        } else {
                            attacker.addAnimation(new MeleeAnimation(attacker, board) {
                                @Override
                                protected void onDone() {
                                    super.onDone();
                                    animLock.release();
                                    boardRenderer.addPostActor(new HoverMessage(boardRenderer, "MISS!!", attacker));
                                }
                            });
                        }
                    }
                    boardRenderer.redraw();
                    animLock.block();
                    boardRenderer.animateZoomTo(currentZoom);
                }
            }
        } else if (actionType.isRanged()) {

            switch (weapon.getType()) {
                case DAGGER: {
                    GroupAnimation group = new GroupAnimation(attacker);
                    Lock animLock = new Lock(numDice);
                    int delay = 200;
                    for (int i = 0; i < numDice; i++) {
                        if (i < hits.size()) {
                            ZActorPosition pos = hits.get(i);
                            ZActor victim = board.getActor(pos);
                            group.addAnimation(delay, new ThrowAnimation(attacker, victim, ZIcon.DAGGER, .1f, 400) {

                                @Override
                                protected void onDone() {
                                    if (pos.getData() == ZGame.ACTOR_POS_DATA_DEFENDED) {
                                        victim.addAnimation(new GroupAnimation(victim)
                                                .addAnimation(new ShieldBlockAnimation(victim))
                                                .addAnimation(new DeflectionAnimation(victim, Utils.randItem(ZIcon.DAGGER.imageIds), getRect(), getDir().getOpposite()))
                                        );
                                    } else {
                                        victim.addAnimation(new SlashedAnimation(victim));
                                    }
                                    animLock.release();
                                }

                            });
                        } else {
                            IVector2D center = board.getZone(targetZone).getRectangle().getRandomPointInside();
                            group.addAnimation(delay, new ThrowAnimation(attacker, center, ZIcon.DAGGER, .1f, 400) {

                                @Override
                                protected void onDone() {
                                    boardRenderer.addPostActor(new HoverMessage(boardRenderer, "MISS!!", attacker));
                                    animLock.release();
                                }

                            });

                        }
                        delay += 200;
                    }
                    attacker.addAnimation(group);
                    boardRenderer.redraw();
                    animLock.block();
                    break;
                }
                default: {
                    GroupAnimation group = new GroupAnimation(attacker);
                    Lock animLock = new Lock(numDice);
                    int delay = 0;
                    for (int i = 0; i < numDice; i++) {
                        if (i < hits.size()) {
                            ZActorPosition pos = hits.get(i);
                            ZActor victim = board.getActor(pos);
                            group.addAnimation(delay, new ShootAnimation(attacker, 300, victim, ZIcon.ARROW) {

                                @Override
                                protected void onDone() {
                                    int arrowId = ZIcon.ARROW.imageIds[getDir().ordinal()];
                                    if (pos.getData() == ZGame.ACTOR_POS_DATA_DEFENDED) {
                                        victim.addAnimation(new GroupAnimation(victim)
                                                .addAnimation(new ShieldBlockAnimation(victim))
                                                .addAnimation(new DeflectionAnimation(victim, arrowId, getRect(), getDir().getOpposite()))
                                        );
                                    } else {
                                        victim.addAnimation(new StaticAnimation(victim, 800, arrowId, getRect(), true));
                                    }
                                    animLock.release();
                                }

                            });
                        } else {
                            IVector2D center = board.getZone(targetZone).getRectangle().getRandomPointInside();
                            group.addAnimation(delay, new ShootAnimation(attacker, 300, center, ZIcon.ARROW) {

                                @Override
                                protected void onDone() {
                                    boardRenderer.addPostActor(new HoverMessage(boardRenderer, "MISS!!", attacker));
                                    animLock.release();
                                }

                            });
                        }
                        delay += 100;
                    }
                    attacker.addAnimation(group);
                    boardRenderer.redraw();
                    animLock.block();
                }
            }

        } else if (weapon.isMagic()) {

            switch (weapon.getType()) {
                case DEATH_STRIKE: {
                    Lock animLock = new Lock(1);
                    GRectangle zoneRect = board.getZone(targetZone).getRectangle();
                    GRectangle targetRect = zoneRect.scaledBy(.5f);//.moveBy(0, -1);
                    attacker.addAnimation(new DeathStrikeAnimation(attacker, targetRect, numDice) {
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
                    attacker.addAnimation(new MagicOrbAnimation(attacker, board.getZone(targetZone).getCenter()) {
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
                    GroupAnimation group = new GroupAnimation(attacker);
                    Lock animLock = new Lock(numDice);
                    int delay = 0;
                    for (int i = 0; i < numDice; i++) {
                        if (hits.size() > 0) {
                            ZActorPosition pos = hits.remove(0);
                            ZActor victim = board.getActor(pos);
                            group.addAnimation(delay, new FireballAnimation(attacker, victim) {
                                @Override
                                protected void onDone() {
                                    if (pos.getData() == ZGame.ACTOR_POS_DATA_DEFENDED) {
                                        victim.addAnimation(new ShieldBlockAnimation(victim));
                                    } else {
                                        boardRenderer.addPostActor(new InfernoAnimation(victim.getRect()));
                                    }
                                    animLock.release();
                                }
                            });
                        } else {
                            Vector2D end = board.getZone(targetZone).getCenter().add(Vector2D.newRandom(0.3f));
                            group.addAnimation(delay, new FireballAnimation(attacker, end) {
                                @Override
                                protected void onDone() {
                                    boardRenderer.addPostActor(new HoverMessage(boardRenderer, "MISS!!", attacker));
                                    animLock.release();
                                }
                            });
                        }
                        delay+=150;
                    }
                    attacker.addAnimation(group);
                    boardRenderer.redraw();
                    animLock.block();
                    break;
                }
                case INFERNO: {
                    Lock lock = new Lock(1);
                    List<IRectangle> rects = Utils.map(board.getZone(targetZone).getCells(), pos -> board.getCell(pos));
                    boardRenderer.addPreActor(new InfernoAnimation(rects) {
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
                    List<IInterpolator<Vector2D>> targets = new ArrayList<>();
                    for (int i=0; i<numDice*2; i++) {
                        if (i < hits.size()) {
                            targets.add(board.getActor(hits.get(i)));
                        } else {
                            GRectangle rect = board.getZone(targetZone).getRectangle().scaledBy(.5f);
                            targets.add(Vector2D.getLinearInterpolator(rect.getRandomPointInside(), rect.getRandomPointInside()));
                        }
                    }
                    attacker.addAnimation(new LightningAnimation2(attacker, targets) {
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
