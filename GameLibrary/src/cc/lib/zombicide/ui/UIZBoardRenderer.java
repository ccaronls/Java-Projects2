package cc.lib.zombicide.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.IDimension;
import cc.lib.game.IRectangle;
import cc.lib.game.IShape;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.ui.IButton;
import cc.lib.ui.UIRenderer;
import cc.lib.utils.Grid;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZActionType;
import cc.lib.zombicide.ZActor;
import cc.lib.zombicide.ZAnimation;
import cc.lib.zombicide.ZBoard;
import cc.lib.zombicide.ZCell;
import cc.lib.zombicide.ZCellType;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZDoor;
import cc.lib.zombicide.ZEquipment;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZItem;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZSpell;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZWeapon;
import cc.lib.zombicide.ZWeaponStat;
import cc.lib.zombicide.ZZombie;
import cc.lib.zombicide.ZZone;
import cc.lib.zombicide.anims.ZoomAnimation;

public class UIZBoardRenderer<T extends AGraphics> extends UIRenderer {

    final static Logger log = LoggerFactory.getLogger(UIZBoardRenderer.class);

    public static boolean DEBUG_DRAW_ZONE_INFO = false;

    final List<ZAnimation> preActor = new ArrayList<>();
    final List<ZAnimation> postActor = new ArrayList<>();
    final List<ZAnimation> overlayAnimations = new ArrayList<>();
    ZAnimation zoomAnimation = null;
    final Map<IShape, List<IButton>> clickables = new HashMap<>();

    Grid.Pos highlightedCell = null;
    Object highlightedResult = null;
    ZActor highlightedActor = null;
    ZDoor highlightedDoor = null;
    Grid.Pos selectedCell = null;
    IShape highlightedShape = null;
    List<IButton> highlightedMoves = null;
    boolean actorsAnimating = false;
    private Object overlayToDraw = null;
    boolean drawTiles = false;
    MutableVector2D dragOffset = new MutableVector2D(Vector2D.ZERO);
    Vector2D dragStart = Vector2D.ZERO;

    public UIZBoardRenderer(UIZComponent component) {
        super(component);
    }

    UIZombicide getGame() {
        return UIZombicide.getInstance();
    }

    ZBoard getBoard() {
        return getGame().getBoard();
    }

    public boolean isAnimating() {
        return actorsAnimating || postActor.size() > 0 || preActor.size() > 0 || overlayAnimations.size() > 0 || zoomAnimation != null;
    }

    void addPreActor(ZAnimation a) {
        synchronized (preActor) {
            preActor.add(a.start());
        }
        redraw();
    }

    void addPostActor(ZAnimation a) {
        synchronized (postActor) {
            postActor.add(a.start());
        }
        redraw();
    }

    void addOverlay(ZAnimation a) {
        synchronized (overlayAnimations) {
            overlayAnimations.add(a);
        }
        redraw();
    }

    private void addClickable(IShape rect, IButton move) {
        List<IButton> moves = clickables.get(rect);
        if (moves == null) {
            moves = new ArrayList<>();
            clickables.put(rect, moves);
        }
        moves.add(move);
    }

    List<IButton> submenu = null;

    void processSubMenu(ZCharacter cur, List<IButton> options) {
//        submenu = options;
  //      redraw();
    }

    void processMoveOptions(ZCharacter cur, List<ZMove> options) {
        clickables.clear();
        if (true)
            return;
        for (ZMove move : options) {
            switch (move.type) {
                case DO_NOTHING:
                    addClickable(cur.getRect(), move);
                    break;
                case INVENTORY:
                    //addClickable(cur.getRect(), move);
                    break;
                case TRADE:
                    for (ZCharacter c : (List<ZCharacter>)move.list)
                        addClickable(c.getRect(), new ZMove(move, c));
                    break;
                case WALK:
                    for (Integer zoneIdx : (List<Integer>)move.list)
                        addClickable(getBoard().getZone(zoneIdx), new ZMove(move, zoneIdx, zoneIdx));
                    break;
                case WALK_DIR:
                    break;
                case MELEE_ATTACK:
                case RANGED_ATTACK:
                case MAGIC_ATTACK:
                    for (ZWeapon w : (List<ZWeapon>)move.list) {
                        ZActionType actionType = move.type.getActionType(w);
                        ZWeaponStat stat = cur.getWeaponStat(w, actionType, getGame(), -1);
                        if (stat.getMinRange() == 0) {
                            addClickable(cur.getRect(), new ZMove(move, w, cur.getOccupiedZone()));
                        }
                        for (int i=1; i<stat.getMaxRange(); i++) {
                            for (int zoneIdx : getBoard().getAccessableZones(cur.getOccupiedZone(), i, actionType)) {
                                addClickable(getBoard().getZone(zoneIdx), new ZMove(move, w, zoneIdx));
                            }
                        }
                    }
                    break;
                case THROW_ITEM: {
                    List<Integer> zones = getBoard().getAccessableZones(cur.getOccupiedZone(), 1, ZActionType.THROW_ITEM);
                    zones.add(cur.getOccupiedZone());
                    for (ZItem item : (List<ZItem>) move.list) {
                        for (Integer zoneIdx : zones) {
                            addClickable(cur.getRect(), new ZMove(move, item, zoneIdx));
                        }
                    }
                    break;
                }
                case RELOAD:
                    addClickable(cur.getRect(), move);
                    break;
                case OPERATE_DOOR:
                    for (ZDoor door : (List<ZDoor>)move.list) {
                        addClickable(door.getRect(getBoard()).grow(.1f), new ZMove(move, door));
                    }
                    break;
                case SEARCH:
                case CONSUME:
                case EQUIP:
                case UNEQUIP:
                case GIVE:
                case TAKE:
                case DISPOSE:
                    addClickable(cur.getRect(), move);
                    break;
                case TAKE_OBJECTIVE:
                    addClickable(getBoard().getZone(cur.getOccupiedZone()), move);
                    break;
                case DROP_ITEM:
                case PICKUP_ITEM:
                    for (ZEquipment e : (List<ZEquipment>)move.list) {
                        addClickable(cur.getRect(), new ZMove(move, e));
                    }
                    break;
                case MAKE_NOISE:
                    addClickable(cur.getRect(), move);
                    break;
                case SHOVE:
                    for (int zoneIdx : (List<Integer>)move.list) {
                        addClickable(getBoard().getZone(zoneIdx), new ZMove(move, zoneIdx));
                    }
                    break;
                case REROLL:
                    break;
                case KEEP_ROLL:
                    break;
                case ENCHANT: {
                    List<ZCharacter> targets = Utils.filter(getGame().getBoard().getAllCharacters(), object -> object.isAlive() && getBoard().canSee(cur.getOccupiedZone(), object.getOccupiedZone()));
                    for (ZSpell spell : (List<ZSpell>)move.list) {
                        for (ZCharacter c : targets) {
                            addClickable(c.getRect(), new ZMove(move, spell, c.getPlayerName()));
                        }
                    }
                    break;
                }
                case BORN_LEADER:
                    for (ZCharacter c : (List<ZCharacter>)move.list) {
                        addClickable(c.getRect(), new ZMove(move, c.getPlayerName(), c.getPlayerName()));
                    }
                    break;
                case BLOODLUST_MELEE:
                    for (ZWeapon w : cur.getMeleeWeapons()) {
                        for (int zoneIdx : (List<Integer>)move.list) {
                            addClickable(getBoard().getZone(zoneIdx), new ZMove(move, zoneIdx, w));
                        }
                    }
                    break;
                case BLOODLUST_RANGED:
                    for (ZWeapon w : cur.getRangedWeapons()) {
                        for (int zoneIdx : (List<Integer>)move.list) {
                            addClickable(getBoard().getZone(zoneIdx), new ZMove(move, zoneIdx, w));
                        }
                    }
                    break;
                case BLOODLUST_MAGIC:
                    for (ZWeapon w : cur.getMagicWeapons()) {
                        for (int zoneIdx : (List<Integer>)move.list) {
                            addClickable(getBoard().getZone(zoneIdx), new ZMove(move, zoneIdx, w));
                        }
                    }
                    break;
                default:
                    log.warn("Unhandled case: %s", move.type);
            }
        }
    }

    synchronized void drawAnimations(List<ZAnimation> anims, AGraphics g) {
        synchronized (anims) {
            Iterator<ZAnimation> it = anims.iterator();
            while (it.hasNext()) {
                ZAnimation a = it.next();
                if (a.isDone()) {
                    it.remove();
                } else {
                    if (!a.isStarted())
                        a.start();
                    a.update(g);
                }
            }
        }
    }

    public ZActor drawActors(AGraphics g, UIZombicide game, float mx, float my, boolean drawAnimating) {
        ZActor picked = null;
        float distFromCenter = 0;
        actorsAnimating = false;
        ZBoard b = game.getBoard();

        Set<ZActor> options = Collections.emptySet();

        switch (game.getUiMode()) {
            case PICK_ZOMBIE:
            case PICK_CHARACTER: {
                options = new HashSet<>(game.getOptions());
                /*
                g.setColor(GColor.YELLOW);
                for (ZActor a : (List<ZActor>) game.getOptions()) {
                    a.getRect(game.getBoard()).drawOutlined(g, 1);
                }
                if (game.getOptions().contains(highlightedActor)) {
                    highlightedResult = highlightedActor;
                }*/
                break;
            }
        }

        for (ZActor a : b.getAllLiveActors()) {
            if (a.isAnimating() && !drawAnimating)
                continue;
            AImage img = g.getImage(a.getImageId());
            if (img != null) {
                GRectangle rect = a.getRect(b);
                // draw box under character of the color of the user who is owner
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
                if (a.isAnimating()) {
                    a.drawOrAnimate(g);
                    actorsAnimating = true;
                } else {
                    GColor outline = null;
                    if (game.getCurrentCharacter() != null && a == game.getCurrentCharacter().getCharacter()) {
                        outline = GColor.GREEN;
                    } else if (a == picked)
                        outline = GColor.CYAN;
                    else if (options.contains(a))
                        outline = GColor.WHITE;
                    drawActor((T)g, a, outline);
                }
                g.removeFilter();
            }
        }
        return picked;
    }

    protected void drawActor(T g, ZActor actor, GColor outline) {
        if (outline != null) {
            if (actor.getOutlineImageId() > 0) {
                g.pushMatrix();
                g.setTint(GColor.WHITE, outline);
                g.drawImage(actor.getOutlineImageId(), actor.getRect());
                g.removeTint();
                g.popMatrix();
            } else {
                g.setColor(outline);
                g.drawRect(actor.getRect(), 1);
            }
        }
        actor.draw(g);
    }

    public int pickZone(AGraphics g, int mouseX, int mouseY) {
        for (ZCell cell : getBoard().getCells()) {
            if (cell.contains(mouseX, mouseY)) {
                return cell.getZoneIndex();
            }
        }
        return -1;
    }

    public ZDoor pickDoor(AGraphics g, List<ZDoor> doors, float mouseX, float mouseY) {
        ZDoor picked = null;
        for (ZDoor door : doors) {
            GRectangle doorRect = door.getRect(getBoard()).grow(.1f);
            if (doorRect.contains(mouseX, mouseY)) {
                g.setColor(GColor.RED);
                picked = door;
                // highlight the other side if this is a vault
                g.drawRect(door.getOtherSide().getRect(getBoard()).grow(.1f), 2);
            } else {
                g.setColor(GColor.YELLOW);
            }
            g.drawRect(doorRect, 2);
        }
        return picked;
    }

    public void drawZoneOutline(AGraphics g, ZBoard board, int zoneIndex) {
        ZZone zone = board.getZone(zoneIndex);
        for (Grid.Pos cellPos : zone.getCells()) {
            g.drawRect(board.getCell(cellPos), 2);
        }
    }

    /**
     * return zone highlighted by mouseX, mouseY
     *
     * @param g
     * @param mouseX
     * @param mouseY
     * @return
     */
    public int drawZones(AGraphics g, float mouseX, float mouseY) {
        final ZBoard board = getBoard();
        int result = -1;
        for (int i=0; i<getBoard().getNumZones(); i++) {
            ZZone zone = board.getZone(i);
            for (Grid.Pos pos : zone.getCells()) {
                ZCell cell = board.getCell(pos);
                if (cell.isCellTypeEmpty())
                    continue;
                switch (zone.getType()) {
                    case VAULT:
                        g.setColor(GColor.BROWN);
                        g.drawFilledRect(cell);
                        break;
                }
                drawCellWalls(g, pos, 1);
                if (cell.contains(mouseX, mouseY)) {
                    result = i;
                    g.setColor(GColor.RED.withAlpha(32));
                    g.drawFilledRect(cell);
                }
                String text = "";
                for (ZCellType type : ZCellType.values()) {
                    if (cell.isCellType(type)) {
                        switch (type) {
                            case OBJECTIVE_BLACK:
                            case OBJECTIVE_BLUE:
                            case OBJECTIVE_GREEN:
                            case OBJECTIVE_RED:
                                if (zone.isObjective()) {
                                    // draw a big red X om the center of the cell
                                    GRectangle redX = new GRectangle(cell).scaledBy(.25f, .25f);
                                    g.setColor(type.getColor());
                                    g.drawLine(redX.getTopLeft(), redX.getBottomRight(), 10);
                                    g.drawLine(redX.getTopRight(), redX.getBottomLeft(), 10);
                                }
                                break;
                            case EXIT:
                                text += "EXIT";
                                break;
                            case SPAWN_NORTH:
                                if (zone.isSpawn()) {
                                    drawSpawn(g, cell, ZDir.NORTH);
                                }
                                break;
                            case SPAWN_SOUTH:
                                if (zone.isSpawn()) {
                                    drawSpawn(g, cell, ZDir.SOUTH);
                                }
                                break;
                            case SPAWN_EAST:
                                if (zone.isSpawn()) {
                                    drawSpawn(g, cell, ZDir.EAST);
                                }
                                break;
                            case SPAWN_WEST:
                                if (zone.isSpawn()) {
                                    drawSpawn(g, cell, ZDir.WEST);
                                }
                                break;
                        }
                    }
                }
                if (zone.isDragonBile()) {
                    g.drawImage(ZIcon.SLIME.imageIds[0], cell);
                }
                if (text.length() > 0) {
                    g.setColor(GColor.YELLOW);
                    g.drawJustifiedStringOnBackground(cell.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 2);
                }
                if (getGame().getUiMode() == UIZombicide.UIMode.PICK_ZONE && !getGame().getOptions().contains(i)) {
                    g.setColor(GColor.TRANSLUSCENT_BLACK);
                    g.drawFilledRect(cell);
                }
            }
            if (zone.getNoiseLevel() > 0) {
                g.setColor(GColor.BLACK);
                g.drawJustifiedString(zone.getCenter(), Justify.CENTER, Justify.CENTER, String.valueOf(zone.getNoiseLevel()));
            }
        }
        ZZone maxNoise = board.getMaxNoiseLevelZone();
        if (maxNoise != null) {
            GColor color = new GColor(GColor.BLACK);
            float radius = 0.5f;
            float dr = radius / 5;
            radius = dr;
            for (int i=0; i<5; i++) {
                g.setColor(color);
                g.drawCircle(maxNoise.getCenter(), radius, 0);
                color = color.lightened(.1f);
                radius += dr;
            }
        }
        return result;
    }

    void drawSpawn(AGraphics g, IRectangle rect, ZDir dir) {
        int id = ZIcon.SPAWN.imageIds[dir.ordinal()];
        AImage img = g.getImage(id);
        g.drawImage(id, new GRectangle(rect).scale(.8f).fit(img, dir.horz, dir.vert));
    }

    public void drawCellWalls(AGraphics g, Grid.Pos cellPos, float scale) {
        final ZBoard board = getBoard();
        ZCell cell = board.getCell(cellPos);
        g.pushMatrix();
        Vector2D center = cell.getCenter();
        g.translate(center);
        Vector2D v0 = cell.getTopLeft().subEq(center);
        Vector2D v1 = cell.getTopRight().subEq(center);
        g.scale(scale);

        final GColor doorColor = GColor.ORANGE;

        for (ZDir dir : ZDir.getCompassValues()) {
            Vector2D dv = v1.sub(v0).scaleEq(.33f);
            Vector2D dv0 = v0.add(dv);
            Vector2D dv1 = dv0.add(dv);

            g.pushMatrix();
            g.rotate(dir.rotation);
            g.setColor(GColor.BLACK);
            switch (cell.getWallFlag(dir)) {
                case WALL:
                    g.drawLine(v0, v1, 3);
                    break;
                case OPEN: {
                    g.drawLine(v0, dv0, 3);
                    g.drawLine(dv1, v1, 3);
                    if (dir == ZDir.SOUTH || dir == ZDir.WEST) {
                        g.setColor(doorColor);
                        g.drawLine(dv0, (dv0.add(dv.scaledBy(.5f).rotate(145))), 4);
                        g.drawLine(dv1, (dv1.sub(dv.scaledBy(.5f).rotate(-145))), 4);
                    }
                    break;
                }
                case LOCKED: {
                    if (dir == ZDir.SOUTH || dir == ZDir.WEST) {
                        g.drawLine(v0, v1, 3);
                        ZDoor door = getBoard().findDoor(cellPos, dir);
                        g.setColor(door.getLockedColor());
                        GRectangle padlock = new GRectangle(0, 0, .2f, .2f).withCenter(dv1.add(dv0).scaleEq(.5f));
                        //GRectangle rect = door.getRect(board).withDimension()scale(.5f);
                        AImage img = g.getImage(ZIcon.PADLOCK.imageIds[0]);
                        g.drawImage(ZIcon.PADLOCK.imageIds[0], padlock.fit(img));
                        g.drawLine(dv0, dv1, 4);
                    }
                    break;
                }
                case CLOSED:
                    if (dir == ZDir.SOUTH || dir == ZDir.WEST) {
                        g.drawLine(v0, v1, 3);
                        g.setColor(doorColor);
                        g.drawLine(dv0, dv1, 4);
                    }
                    break;
            }
            g.popMatrix();
        }
        g.popMatrix();

        for (ZDir dir : ZDir.getElevationValues()) {
            switch (cell.getWallFlag(dir)) {
                case LOCKED: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    g.setColor(door.getLockedColor());
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.RED);
                    vaultRect.drawOutlined(g, 2);
//                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "LOCKED");
                    GRectangle rect = door.getRect(board).scale(.7f);
                    AImage img = g.getImage(ZIcon.PADLOCK.imageIds[0]);
                    g.drawImage(ZIcon.PADLOCK.imageIds[0], rect.fit(img));
                    break;
                }
                case CLOSED: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    g.setColor(door.lockedColor);
                    vaultRect.drawFilled(g);
                    g.setColor(GColor.YELLOW);
                    vaultRect.drawOutlined(g, 2);
//                    g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "VAULT");
                    break;
                }
                case OPEN: {
                    ZDoor door = board.findDoor(cellPos, dir);
                    g.setColor(GColor.BLACK);
                    GRectangle vaultRect = door.getRect(board);
                    vaultRect.drawFilled(g);
                    // draw the 'lid' opened
                    g.begin();
                    g.vertex(vaultRect.getTopRight());
                    g.vertex(vaultRect.getTopLeft());
                    float dh = vaultRect.h/3;
                    float dw = vaultRect.w/5;
                    if (dir == ZDir.ASCEND) {
                        // open 'up'
                        g.moveTo(-dw, -dh);
                        g.moveTo(vaultRect.w+dw*2, 0);
                    } else {
                        // open 'down
                        g.moveTo(dw, dh);
                        g.moveTo(vaultRect.w-dw*2, 0);
                    }
                    g.setColor(door.lockedColor);
                    g.drawTriangleFan();
                    g.setColor(GColor.YELLOW);
                    g.end();
                    vaultRect.drawOutlined(g, 2);
                    g.drawLineLoop(2);
                }
            }
        }
    }

    public Grid.Pos pickCell(AGraphics g, float mouseX, float mouseY) {
        Grid.Iterator<ZCell> it = getBoard().getCellsIterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            if (cell.contains(mouseX, mouseY)) {
                return it.getPos();
            }
        }
        return null;
    }


    public Grid.Pos drawNoTiles(AGraphics g, float mouseX, float mouseY, boolean debugText) {
        g.clearScreen();
        Grid.Pos returnCell = null;

        Grid.Iterator<ZCell> it = getBoard().getCellsIterator();
        while (it.hasNext()) {
            ZCell cell = it.next();
            if (cell.isCellTypeEmpty())
                continue;
            switch (cell.getEnvironment()) {
                case ZCell.ENV_BUILDING:
                    g.setColor(GColor.DARK_GRAY); break;
                case ZCell.ENV_OUTDOORS:
                    g.setColor(GColor.LIGHT_GRAY); break;
                case ZCell.ENV_VAULT:
                    g.setColor(GColor.BROWN); break;
            }
            g.drawFilledRect(cell);
            //drawCellWalls(g, it.getPos(), .97f);
            String text = "Zone " + cell.getZoneIndex();
            for (ZCellType type : ZCellType.values()) {
                if (cell.isCellType(type)) {
                    switch (type) {
                        case NONE:
                        case VAULT_DOOR_VIOLET:
                        case VAULT_DOOR_GOLD:
                            break;
                        case OBJECTIVE_RED:
                        case OBJECTIVE_BLUE:
                        case OBJECTIVE_GREEN:
                        case OBJECTIVE_BLACK:
                            text += "\n" + type.name().substring(10);
                            break;
                        case SPAWN_NORTH:
                        case SPAWN_SOUTH:
                        case SPAWN_EAST:
                        case SPAWN_WEST:
                            text += "\nSPAWN";
                            break;
                        default:
                            text += "\n" + type;
                    }
                }
            }
            /*
            if (cell.rect.contains(mouseX, mouseY)) {
                List<Integer> accessible = getAccessableZones(cell.zoneIndex, 1, ZActionType.MOVE);
                text = "1 Unit away:\n" + accessible;
                returnCell = it.getPos();//new int[] { col, row };
                List<Integer> accessible2 = getAccessableZones(cell.zoneIndex, 2, ZActionType.MAGIC);
                text += "\n2 Units away:\n" + accessible2;
            }*/
            g.setColor(GColor.CYAN);
            for (ZActor a : cell.getOccupant()) {
                if (a != null) {
                    text += "\n" + a.name();
                }
            }
            if (cell.getVaultFlag() > 0) {
                text += "\nvaultFlag " + cell.getVaultFlag();
            }
            if (debugText)
                g.drawJustifiedStringOnBackground(cell.getCenter(), Justify.CENTER, Justify.CENTER, text, GColor.TRANSLUSCENT_BLACK, 10, 3);

        }
        return returnCell;
    }

    IButton pickButtons(AGraphics g, Vector2D pos, List<IButton> moves, int mouseX, int mouseY) {
        float height = moves.size() * g.getTextHeight() * 2;
        MutableVector2D top = pos.sub(0, height/2);
        if (top.Y() < 0) {
            top.setY(0);
        } else if (top.Y() + height > g.getViewportHeight()) {
            top.setY(g.getViewportHeight() - height);
        }
        // draw buttons to the right is pos id on left side and to the left if user on the right side
        IButton move = null;
        float buttonHeight = g.getTextHeight()*2;
        float buttonWidth = 0;
        float padding = g.getTextHeight()/2;
        for (IButton m : moves) {
            buttonWidth = Math.max(0, g.getTextWidth(m.getLabel()));
        }
        buttonWidth += padding * 2;
        if (top.getX()+buttonWidth > g.getViewportWidth()) {
            top.setX(g.getViewportWidth()-buttonWidth);
        }
        GRectangle button = new GRectangle(top, buttonWidth, buttonHeight);
        for (IButton m : moves) {
            if (button.contains(mouseX, mouseY)) {
                g.setColor(GColor.RED);
                move = m;
            } else {
                g.setColor(GColor.YELLOW);
            }
            button.drawRounded(g, buttonHeight/4);
            g.drawJustifiedString(button.x+padding, button.y + buttonHeight/2, Justify.LEFT, Justify.CENTER, m.getLabel());
            button.y += buttonHeight;
        }
        return move;
    }

    IButton pickMove(APGraphics g, IVector2D mouse, int screenMouseX, int screenMouseY) {

        IButton picked = null;
        if (submenu != null) {
            highlightedMoves = submenu;
        } else {
            for (Map.Entry<IShape, List<IButton>> entry : clickables.entrySet()) {
                IShape shape = entry.getKey();
                if (shape.contains(mouse.getX(), mouse.getY())) {
                    highlightedShape = shape;
                    highlightedMoves = entry.getValue();
                } else {
                    g.setColor(GColor.YELLOW.withAlpha(32));
                    entry.getKey().drawFilled(g);
                }
            }
        }

        if (highlightedShape != null) {
            g.setColor(GColor.RED);
            g.setLineWidth(2);
            highlightedShape.drawOutlined(g);
            MutableVector2D cntr = highlightedShape.getCenter();
            g.transform(cntr);
            g.pushMatrix();
            g.setIdentity();
            g.ortho();
            picked = pickButtons(g, cntr, highlightedMoves, screenMouseX, screenMouseY);
            g.popMatrix();
        }


        return picked;
    }

    public IVector2D getBoardCenter() {
        ZGame game = getGame();
        if (game.getCurrentCharacter() != null)
            return game.getCurrentCharacter().getCharacter().getRect().getCenter();//game.getBoard().getZone(game.getCurrentCharacter().getOccupiedZone()).getCenter();
        List<ZCharacter> chars = game.getBoard().getAllCharacters();
        if (chars.size() == 0)
            return new GRectangle(getBoard()).getCenter();
        GRectangle rect = null;
        for (ZCharacter c : chars) {
            if (rect == null)
                rect = new GRectangle(c.getRect());
            else
                rect.addEq(c.getRect());
        }
        return rect.getCenter();
    }

    ZTile [] tiles = null;
    int [] tileIds = new int[0];

    public void clearTiles() {
        tiles = null;
        tileIds = new int[0];
    }

    public void onTilesLoaded(int [] ids) {
        tileIds = ids;
    }

    public void onLoading() {}

    public void onLoaded() {}

    float zoomPercent = 0;

    public float getZoomPercent() {
        return zoomPercent;
    }

    public void setZoomPercent(float zoomPercent) {
        this.zoomPercent = zoomPercent;
    }

    public void animateZoomTo(float targetZoomPercent) {
        targetZoomPercent = Utils.clamp(targetZoomPercent, 0, 1);
        if (zoomPercent != targetZoomPercent) {
            zoomAnimation = new ZoomAnimation(this, targetZoomPercent).start();
            redraw();
        }
    }

    private GRectangle zoomedRect = null;

    public GRectangle getZoomedRect() {
        return zoomedRect;
    }

    public GRectangle getZoomedRectangle(AGraphics g, IVector2D center) {
        GDimension dim = getBoard().getDimension();
        float aspect = dim.getAspect();
        GDimension zoomed;
        GDimension viewport = g.getViewport();
        // produce a value between 0-1 where
        //   0 means zoomAmt is 0 and
        //   1 is zoomAmt results in a rect with min side of 3

        float zoomAmtMin = Math.min(dim.getWidth(), dim.getHeight());
        float zoomAmtMax = 3;
        float zoomAmt = (zoomAmtMin - zoomAmtMax) * zoomPercent;
        float newW = dim.width - zoomAmt * aspect;
        float newH = dim.height - zoomAmt;
        float vAspect = viewport.getAspect();

        if (vAspect > aspect) {
            zoomed = new GDimension(newW, newH * aspect / vAspect);
        } else {
            zoomed = new GDimension(newW * vAspect / aspect, newH);
        }

        GRectangle rect = new GRectangle(zoomed).withCenter(center);
        /*
        if (rect.x >= 0) {
            rect.x = 0;
        } else if (rect.x + rect.w < dim.width) {
            rect.x = dim.width - rect.w;
        }
        if (rect.y >= 0) {
            rect.y = 0;
        } else if (rect.y + rect.h < dim.height) {
            rect.y = dim.height - rect.h;
        }*/
        rect.x = Utils.clamp(rect.x, 0, dim.width-rect.w);
        rect.y = Utils.clamp(rect.y, 0, dim.height-rect.h);
        return zoomedRect = rect;
    }

    void drawQuestLabel(AGraphics g) {
        g.setColor(GColor.BLACK);
        if (getGame().getQuest()!=null) {
            float height = g.getTextHeight();
            g.setTextHeight(24);//setFont(bigFont);
            g.setTextStyles(AGraphics.TextStyle.BOLD, AGraphics.TextStyle.ITALIC);
            g.drawJustifiedString(10, getHeight()-10-g.getTextHeight(), Justify.LEFT, Justify.BOTTOM, getGame().getQuest().getName());
            g.setTextHeight(height);
            g.setTextStyles(AGraphics.TextStyle.NORMAL);
        }
    }

    private void drawNoBoard(AGraphics g) {
        g.clearScreen(GColor.WHITE);
        Vector2D cntr = new Vector2D(g.getViewportWidth()/2, g.getViewportHeight()/2);
        GDimension minDim = new GDimension(g.getViewportWidth()/4, g.getViewportHeight()/4);
        GDimension maxDim = new GDimension(g.getViewportWidth()/2, g.getViewportHeight()/2);
        GRectangle rect = new GRectangle().withDimension(minDim.interpolateTo(maxDim, 1)).withCenter(cntr);
        //g.setColor(GColor.RED);
        //rect.drawOutlined(g, 5);
        AImage img = g.getImage(ZIcon.GRAVESTONE.imageIds[0]);
        g.drawImage(ZIcon.GRAVESTONE.imageIds[0], rect.fit(img));

    }

    @Override
    public void draw(APGraphics g, int mouseX, int mouseY) {
        //log.debug("mouseX=" + mouseX + " mouseY=" + mouseY);
        UIZombicide game = getGame();
        ZBoard board = getBoard();
        if (board == null) {
            drawNoBoard(g);
            return;
        }
        highlightedActor = null;
        highlightedCell = null;
        highlightedResult = null;
        highlightedDoor = null;

        g.setIdentity();

        IVector2D center = getBoardCenter();
        GRectangle rect = getZoomedRectangle(g, center);
        MutableVector2D dragViewport = dragOffset.scaledBy(-rect.getWidth() / g.getViewportWidth(), -rect.getHeight() / g.getViewportHeight());

        //log.debug("dragViewport = " + dragViewport);// + " topL = " + topL + "  bottomR = "+ bottomR);

        IDimension boardRect = board.getDimension();
        rect.moveBy(dragViewport);
        if (rect.x < 0) {
            rect.x = 0;
        } else if (rect.x + rect.w > boardRect.getWidth()) {
            rect.x = boardRect.getWidth() - rect.w;
        }

        if (rect.y < 0) {
            rect.y = 0;
        } else if (rect.y + rect.h > boardRect.getHeight()) {
            rect.y = boardRect.getHeight() - rect.h;
        }

        //log.debug("Rect = " + rect);

        g.ortho(rect);

        Vector2D mouse = g.screenToViewport(mouseX, mouseY);

        if (drawTiles && tiles == null) {
            tiles = getGame().getQuest().getTiles(getBoard());
            onLoading();
            ((UIZComponent)getComponent()).loadTiles(g, tiles);
            return;
        }

        Grid.Pos cellPos = drawNoTiles(g, mouseX, mouseY, DEBUG_DRAW_ZONE_INFO);
        if (drawTiles) {
            for (int i = 0; i < tileIds.length; i++) {
                g.drawImage(tileIds[i], tiles[i].quadrant);
            }
        }

        int highlightedZone = drawZones(g, mouse.X(), mouse.Y());
        game.getQuest().drawQuest(game, g);
        boolean drawAnimating = game.isGameOver();

        drawAnimations(preActor, g);

        highlightedActor = //board.drawActors(g, getMouseX(), getMouseY());
                drawActors(g, game, mouse.X(), mouse.Y(), drawAnimating || overlayToDraw == null);

        if (DEBUG_DRAW_ZONE_INFO) {
            if (highlightedActor instanceof ZZombie) {
                List<ZDir> path = null;
                ZZombie z = (ZZombie)highlightedActor;
                switch (z.getType()) {
                    case Walker:
                    case Fatty:
                    case Runner:
                    case Abomination:
                        path = game.getZombiePathTowardVisibleCharactersOrLoudestZone(z);
                        break;
                    case Necromancer:
                        path = game.getZombiePathTowardNearestSpawn(z);
                        break;
                }
                g.begin();
                g.setColor(GColor.YELLOW)   ;
                final Vector2D start = z.getRect().getCenter();
                g.vertex(start);
                MutableVector2D next = new MutableVector2D(start);
                for (ZDir dir : path) {
                    next.addEq(dir.dx, dir.dy);
                    g.vertex(next);
                }
                g.drawLineStrip(3);
            }
        }

        drawAnimations(postActor, g);

        drawQuestLabel(g);

        switch (game.getUiMode()) {
            case PICK_ZOMBIE:
            case PICK_CHARACTER: {
                //g.setColor(GColor.YELLOW);
                //for (ZActor a : (List<ZActor>)game.getOptions()) {
                //    a.getRect(board).drawOutlined(g, 1);
                //}
                if (game.getOptions().contains(highlightedActor)) {
                    highlightedResult = highlightedActor;
                }
                break;
            }
            case PICK_ZONE: {
                if (highlightedZone >= 0 && game.getOptions().contains(highlightedZone)) {
                    highlightedResult = highlightedZone;
                    g.setColor(GColor.YELLOW);
                    drawZoneOutline(g, board, highlightedZone);
                } else if (cellPos != null) {
                    ZCell cell = board.getCell(cellPos);
                    for (int i = 0; i < game.getOptions().size(); i++) {
                        if (cell.getZoneIndex() == (Integer)game.getOptions().get(i)) {
                            highlightedCell = cellPos;
                            highlightedResult = cell.getZoneIndex();
                            break;
                        }
                    }
                }
                break;
            }
            case PICK_DOOR: {
                highlightedResult = pickDoor(g, (List<ZDoor>)game.getOptions(), mouse.X(), mouse.Y());
                break;
            }
            case PICK_MENU: {
                highlightedResult = pickMove(g, mouse, mouseX, mouseY);
                break;
            }
        }
        if (highlightedCell != null) {
            ZCell cell = board.getCell(highlightedCell);
            g.setColor(GColor.RED.withAlpha(32));
            drawZoneOutline(g, board, cell.getZoneIndex());
        }

        g.pushMatrix();
        g.setIdentity();
        g.ortho();
        g.setColor(GColor.WHITE);
        g.drawJustifiedStringOnBackground(10, getHeight()-10, Justify.LEFT, Justify.BOTTOM, game.getBoardMessage(), GColor.TRANSLUSCENT_BLACK, getBorderThickness());
        g.popMatrix();
        game.characterRenderer.redraw();
        drawAnimations(overlayAnimations, g);

        //if (game.isGameOver() && overlayToDraw == null && !isAnimating()) {
        //    setOverlay(game.getGameSummaryTable());
        //}

        drawOverlay(g);

        if (zoomAnimation != null) {
            if (zoomAnimation.isDone()) {
                zoomAnimation = null;
            } else {
                zoomAnimation.update(g);
            }
        }

        if (isAnimating())
            redraw();
    }

    protected void drawOverlay(AGraphics g) {
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
                g.pushMatrix();
                g.setIdentity();
                g.ortho();
                g.setColor(GColor.YELLOW);
                IVector2D cntr = new Vector2D(getWidth()/2, getHeight()/3);
                Table t = (Table)overlayToDraw;
                GDimension d = t.getDimension(g);
                t.draw(g, cntr, Justify.CENTER, Justify.CENTER);
                g.popMatrix();
                /*
                g.setColor(GColor.RED);
                GRectangle r = new GRectangle(d).withCenter(cntr);
                g.drawRect(r);
                g.drawRect(0, 0, g.getViewportWidth(), g.getViewportHeight());
                */
            } else if (overlayToDraw instanceof String) {
                g.setColor(GColor.WHITE);
                g.drawWrapStringOnBackground(getWidth()/2, getHeight()/2, getWidth()/2, Justify.CENTER, Justify.CENTER, (String)overlayToDraw, GColor.TRANSLUSCENT_BLACK, 10);
            } else if (overlayToDraw instanceof ZAnimation) {
                ZAnimation a = (ZAnimation)overlayToDraw;
                if (!a.isStarted()) {
                    a.start();
                }
                if (!a.isDone()) {
                    a.update(g);
                    redraw();
                }
            } else if (overlayToDraw instanceof AImage) {

            }
        }
    }

    public float getBorderThickness() {
        return 5;
    }

    public void setOverlay(Object obj) {
        if (obj == null) {
            overlayToDraw = null;
        } else if (obj instanceof Table) {
            overlayToDraw = obj;
        } else if (obj instanceof ZPlayerName){
            overlayToDraw = ((ZPlayerName)obj).cardImageId;
        } else if (obj instanceof AImage) {
            overlayToDraw = obj;
        } else if (obj instanceof ZAnimation) {
            overlayToDraw = obj;
        } else {
            overlayToDraw = obj.toString();
        }
        redraw();
    }

    @Override
    public void onClick() {
        if (getGame().isGameRunning()) {
            overlayToDraw = null;
            submenu = null;
            getGame().setResult(highlightedResult);
        } else {
            if (highlightedDoor != null) {
                highlightedDoor.toggle(getBoard());
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

    public boolean isDrawTiles() {
        return drawTiles;
    }

    @Override
    public void onDragStart(float x, float y) {
        dragStart = new Vector2D(x, y);
    }

    @Override
    public void onDragEnd() {
        //dragOffset = Vector2D.ZERO;
        redraw();
    }

    @Override
    public void onDragMove(float x, float y) {
        Vector2D v = new Vector2D(x, y);
        Vector2D dv = v.sub(dragStart);
        dragOffset.addEq(dv);
        dragStart = v;
        redraw();
    }

    @Override
    public void redraw() {
        log.debug("!!!!!Redraw!!!!!");
        super.redraw();
    }
}
