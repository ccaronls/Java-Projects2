package cc.applets.zombicide;

import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.ui.UIRenderer;
import cc.lib.utils.Table;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZComponent;
import cc.lib.zombicide.ui.UIZombicide;

class BoardComponent extends AWTComponent implements UIZComponent<AWTGraphics>, WindowListener {

    final Logger log = LoggerFactory.getLogger(getClass());

    BoardComponent() {
        setPreferredSize(250, 250);
    }

    @Override
    protected void init(AWTGraphics g) {
        setMouseEnabled(true);
        new Thread() {
            @Override
            public void run() {
                loadImages(g);
            }
        }.start();
    }

    @Override
    public void setRenderer(UIRenderer r) {
        renderer = (UIZBoardRenderer)r;
    }

    int numImagesLoaded=0;
    int totalImagesToLoad=1000;
    UIZBoardRenderer renderer = null;

    @Override
    protected float getInitProgress() {
        float progress = (float)numImagesLoaded / totalImagesToLoad;
        if (progress >= 1 && loadedTiles.length > 0) {
            progress = (float)numTilesLoaded / (loadedTiles.length+1);
        }
        return progress;
    }

    @Override
    protected void onDimensionChanged(AWTGraphics g, int width, int height) {
        //GDimension cellDim = getGame().getBoard().initCellRects(g, width-5, height-5);
       // int newWidth = (int)cellDim.width * getGame().getBoard().getColumns();
       // int newHeight = (int)cellDim.height * getGame().getBoard().getRows();
       // setPreferredSize(newWidth, newHeight);
    }

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        g.clearScreen();
        if (renderer != null) {
            renderer.draw(g, mouseX, mouseY);
        }
    }


    @Override
    protected void onFocusGained() {
        //renderer.setOverlay(null);
    }

    @Override
    protected void onClick() {
        renderer.onClick();
    }

    @Override
    protected void onDragStarted(int x, int y) {
        renderer.onDragStart(x, y);
    }

    @Override
    protected void onDragStopped() {
        renderer.onDragEnd();
    }

    @Override
    protected void onDrag(int x, int y) {
        renderer.onDragMove(x, y);
    }

    void loadImages(AWTGraphics g) {

        g.addSearchPath("zombicideandroid/src/main/res/drawable");
        Object [][] files = {

            { ZZombieType.Abomination, "zabomination.png" },
                { ZZombieType.Abomination, "zabomination_outline.png" },
            { ZZombieType.GreenTwin, "zgreentwin.png" },
                { ZZombieType.GreenTwin, "zabomination_outline.png" },
            { ZZombieType.BlueTwin, "zbluetwin.png" },
                { ZZombieType.BlueTwin, "zabomination_outline.png" },
            { ZZombieType.Necromancer, "znecro.png" },
                { ZZombieType.Necromancer, "znecro_outline.png" },
            { ZZombieType.Walker, "zwalker1.png" },
                { ZZombieType.Walker, "zwalker1_outline.png" },
            { ZZombieType.Walker, "zwalker2.png" },
                { ZZombieType.Walker, "zwalker2_outline.png" },
            { ZZombieType.Walker, "zwalker3.png" },
                { ZZombieType.Walker, "zwalker3_outline.png" },
            { ZZombieType.Walker, "zwalker4.png" },
                { ZZombieType.Walker, "zwalker4_outline.png" },
            { ZZombieType.Walker, "zwalker5.png" },
                { ZZombieType.Walker, "zwalker5_outline.png" },
            { ZZombieType.Runner, "zrunner1.png" },
                { ZZombieType.Runner, "zrunner1_outline.png" },
            { ZZombieType.Runner, "zrunner2.png" },
                { ZZombieType.Runner, "zrunner2_outline.png" },
            { ZZombieType.Fatty, "zfatty1.png" },
                { ZZombieType.Fatty, "zfatty1_outline.png" },
            { ZZombieType.Fatty, "zfatty2.png" },
                { ZZombieType.Fatty, "zfatty2_outline.png" },
            { ZZombieType.Wolfz, "zwulf1.png" },
                { ZZombieType.Wolfz, "zwulf1_outline.png" },
            { ZZombieType.Wolfz, "zwulf2.png" },
                { ZZombieType.Wolfz, "zwulf2_outline.png" },
            { ZZombieType.Wolfbomination, "zwolfabom.png" },
                { ZZombieType.Wolfbomination, "zwolfabom_outline.png" },

            { ZPlayerName.Clovis, "zchar_clovis.png" },
            { ZPlayerName.Baldric, "zchar_baldric.png" },
            { ZPlayerName.Ann, "zchar_ann.png" },
            { ZPlayerName.Nelly, "zchar_nelly.png" },
            { ZPlayerName.Samson, "zchar_samson.png" },
            { ZPlayerName.Silas, "zchar_silas.png" },
            { ZPlayerName.Tucker, "zchar_tucker.png" },
            { ZPlayerName.Jain, "zchar_jain.png" },
            { ZPlayerName.Benson, "zchar_benson.png" },
            { ZPlayerName.Theo, "zchar_theo.png" },
            { ZPlayerName.Morrigan, "zchar_morrigan.png" },
            { ZPlayerName.Karl, "zchar_karl.png" },
            { ZPlayerName.Ariane, "zchar_ariane.png" },

            { ZPlayerName.Clovis, "zchar_clovis_outline.png" },
            { ZPlayerName.Baldric, "zchar_baldric_outline.png" },
            { ZPlayerName.Ann, "zchar_ann_outline.png" },
            { ZPlayerName.Nelly, "zchar_nelly_outline.png" },
            { ZPlayerName.Samson, "zchar_samson_outline.png" },
            { ZPlayerName.Silas, "zchar_silas_outline.png" },
            { ZPlayerName.Tucker, "zchar_tucker_outline.png" },
            { ZPlayerName.Jain, "zchar_jain_outline.png" },
            { ZPlayerName.Benson, "zchar_benson_outline.png" },
            { ZPlayerName.Theo, "zchar_theo_outline.png" },
            { ZPlayerName.Morrigan, "zchar_morrigan_outline.png" },
            { ZPlayerName.Karl, "zchar_karl_outline.png" },
            { ZPlayerName.Ariane, "zchar_ariane_outline.png" },

            { ZPlayerName.Ann.name(), "zcard_ann.png" },
            { ZPlayerName.Baldric.name(), "zcard_baldric.png" },
            { ZPlayerName.Clovis.name(), "zcard_clovis.png" },
            { ZPlayerName.Nelly.name(), "zcard_nelly.png" },
            { ZPlayerName.Samson.name(), "zcard_samson.png" },
            { ZPlayerName.Silas.name(), "zcard_silas.png" },
            { ZPlayerName.Tucker.name(), "zcard_tucker.png" },
            { ZPlayerName.Jain.name(), "zcard_jain.png" },
            { ZPlayerName.Benson.name(), "zcard_benson.png" },
            { ZPlayerName.Theo.name(), "zcard_theo.png" },
            { ZPlayerName.Morrigan.name(), "zcard_morrigan.png" },
            { ZPlayerName.Karl.name(), "zcard_karl.png" },
            { ZPlayerName.Ariane.name(), "zcard_ariane.png" },

            { ZIcon.DRAGON_BILE, "zdragonbile_icon.png" },
            { ZIcon.CLAWS, "zclaws1_icon.png" },
            { ZIcon.CLAWS, "zclaws2_icon.png" },
            { ZIcon.CLAWS, "zclaws3_icon.png" },
            { ZIcon.CLAWS, "zclaws4_icon.png" },
            { ZIcon.CLAWS, "zclaws5_icon.png" },
            { ZIcon.CLAWS, "zclaws6_icon.png" },
            { ZIcon.SHIELD, "zshield_icon.png" },
            { ZIcon.SLIME, "zslime_icon.png" },
            { ZIcon.TORCH, "ztorch_icon.png" },
            { ZIcon.ARROW, "zarrow_icon.png" },
            { ZIcon.SPAWN_RED, "zspawn_red.png" },
            { ZIcon.SPAWN_BLUE, "zspawn_blue.png" },
            { ZIcon.SPAWN_GREEN, "zspawn_green.png" },
            { ZIcon.SLASH, "zslash1.png" },
            { ZIcon.SLASH, "zslash2.png" },
            { ZIcon.SLASH, "zslash3.png" },
            { ZIcon.SLASH, "zslash4.png" },
            { ZIcon.SLASH, "zslash5.png" },
            { ZIcon.FIREBALL, "zfireball.png" },
            { ZIcon.GRAVESTONE, "zgravestone.png" },
            { ZIcon.PADLOCK, "zpadlock3.png" },
            { ZIcon.SKULL, "zskull.png"},
            { ZIcon.DAGGER, "zdagger_icon.png" },
            { ZIcon.SWORD, "zsword_icon.png" },
            { ZIcon.MJOLNIR, "zmjolnir.png" },
            { ZIcon.BLACKBOOK, "zblack_book.png" }
        };

        Map<Object, List<Integer>> objectToImageMap = new HashMap<>();

        totalImagesToLoad = files.length + ZIcon.values().length;
        for (Object [] entry : files) {
            Object key = entry[0];
            String file = (String)entry[1];
            int id = g.loadImage(file, null, 1);
            if (id >= 0) {
                if (!objectToImageMap.containsKey(key)) {
                    objectToImageMap.put(key, new ArrayList<>());
                }
                objectToImageMap.get(key).add(id);
            }
            numImagesLoaded++;
            repaint();
        }

        for (ZZombieType type : ZZombieType.values()) {
//            type.imageOptions = Utils.toIntArray(objectToImageMap.get(type));
  //          type.imageDims = new GDimension[type.imageOptions.length];
            List<Integer> ids = objectToImageMap.get(type);
            type.imageOptions = new int[ids.size()/2];
            type.imageOutlineOptions = new int[type.imageOptions.length];
            type.imageDims = new GDimension[type.imageOptions.length];
            int idx=0;
            for (int i=0; i<ids.size(); i+=2) {
                type.imageOptions[idx] = ids.get(i);
                type.imageOutlineOptions[idx] = ids.get(i+1);
                type.imageDims[idx] = new GDimension(g.getImage(type.imageOptions[idx]));
                idx++;
            }
        }

        for (ZPlayerName pl : ZPlayerName.values()) {
            pl.imageId = objectToImageMap.get(pl).get(0);
            pl.outlineImageId = objectToImageMap.get(pl).get(1);
            pl.imageDim = new GDimension(g.getImage(pl.imageId));
            pl.cardImageId = objectToImageMap.get(pl.name()).get(0);
        }

        // Icons that 'spin'
        for (ZIcon icon : Utils.toArray(ZIcon.DRAGON_BILE, ZIcon.TORCH, ZIcon.SWORD, ZIcon.DAGGER)) {
            int [] ids = new int[8];
            ids[0] = objectToImageMap.get(icon).get(0);
            for (int i=1; i<ids.length; i++) {
                int deg = 45*i;
                ids[i] = g.createRotatedImage(ids[0], deg);
                numImagesLoaded++;
                repaint();
            }
            icon.imageIds = ids;
        }

        // Icons that shoot
        for (ZIcon icon : Utils.toArray(ZIcon.ARROW, ZIcon.MJOLNIR)) {
            int [] ids = new int[4];
            int eastId = objectToImageMap.get(icon).get(0);
            ids[ZDir.EAST.ordinal()] = eastId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(eastId, 180);
            ids[ZDir.NORTH.ordinal()] = g.createRotatedImage(eastId, 270);
            ids[ZDir.SOUTH.ordinal()] = g.createRotatedImage(eastId, 90);
            icon.imageIds = ids;
            numImagesLoaded++;
            repaint();
        }

        for (ZIcon icon : Utils.toArray(ZIcon.SPAWN_RED, ZIcon.SPAWN_GREEN, ZIcon.SPAWN_BLUE))
        {
            int [] ids = new int[4];
            int northId = objectToImageMap.get(icon).get(0);
            ids[ZDir.NORTH.ordinal()] = northId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(northId, 270);
            ids[ZDir.EAST.ordinal()] = g.createRotatedImage(northId, 90);
            ids[ZDir.SOUTH.ordinal()] = northId;
            icon.imageIds = ids;
            numImagesLoaded++;
            repaint();
        }

        // Icons that have a single id variation
        for (ZIcon icon : Utils.toArray(ZIcon.CLAWS, ZIcon.SHIELD, ZIcon.SLIME, ZIcon.SLASH, ZIcon.FIREBALL, ZIcon.GRAVESTONE, ZIcon.PADLOCK, ZIcon.SKULL, ZIcon.BLACKBOOK)) {
            icon.imageIds = Utils.toIntArray(objectToImageMap.get(icon));
            numImagesLoaded++;
            repaint();
        }

        {
            ZIcon icon = ZIcon.FIRE;
            int [][] cells = {
                    { 0,0, 56, 84 }, { 56, 0, 131-56, 84 }, { 131, 0, 196-131, 84 },
                    { 0,84, 60, 152-84 }, { 60,84,122-60,152-84 }, { 122,84,196-122,152-84 }
            };
            icon.imageIds  = g.loadImageCells("zfire_icons.png", cells);
            numImagesLoaded++;
            repaint();
        }

        log.debug("Images: " + objectToImageMap);
        numImagesLoaded = totalImagesToLoad;
        ZombicideApplet.instance.onAllImagesLoaded();
        renderer.setDrawTiles(ZombicideApplet.instance.getStringProperty("tiles", "no").equals("yes"));
        renderer.setDrawDebugText(ZombicideApplet.instance.getStringProperty("debugText", "no").equals("yes"));
        renderer.setDrawRangedAccessibility(ZombicideApplet.instance.getStringProperty("rangedAccessibility", "no").equals("yes"));
        renderer.setDrawTowersHighlighted(ZombicideApplet.instance.getStringProperty("drawTowersHighlighted", "no").equals("yes"));
        renderer.setDrawZombiePaths(ZombicideApplet.instance.getStringProperty("drawZombiePaths", "no").equals("yes"));
        renderer.setMiniMapMode(ZombicideApplet.instance.getIntProperty("miniMapMode", 0));
        repaint();
    }

    int [] loadedTiles = new int[0];
    int numTilesLoaded = 0;

    @Override
    public void loadTiles(AWTGraphics g, ZTile[] tiles) {
        numTilesLoaded = 0;
        g.addSearchPath("zombicideandroid/assets");
        new Thread() {
            public void run() {
                for (int t : loadedTiles) {
                    g.deleteImage(t);
                }
                loadedTiles = new int[tiles.length];
                for (int i=0; i<loadedTiles.length; i++) {
                    loadedTiles[i] = g.loadImage("ztile_" + tiles[i].id + ".png", tiles[i].orientation);
                    numTilesLoaded ++;
                    repaint();
//                    Utils.waitNoThrow(this, 500);
                }
                renderer.onTilesLoaded(loadedTiles);
                numTilesLoaded++;
                repaint();
            }
        }.start();
        repaint();
    }

    synchronized void initKeysPresses(List options) {
        keyMap.clear();
        for (Iterator it = options.iterator(); it.hasNext(); ) {
            Object obj = it.next();
            if (!(obj instanceof ZMove))
                continue;
            ZMove move = (ZMove)obj;
            switch (move.getType()) {
                case WALK_DIR: {
                    switch (ZDir.values()[move.getInteger()]) {
                        case NORTH:
                            keyMap.put(KeyEvent.VK_UP, move);
                            break;
                        case SOUTH:
                            keyMap.put(KeyEvent.VK_DOWN, move);
                            break;
                        case EAST:
                            keyMap.put(KeyEvent.VK_RIGHT, move);
                            break;
                        case WEST:
                            keyMap.put(KeyEvent.VK_LEFT, move);
                            break;
                        case DESCEND:
                        case ASCEND:
                            keyMap.put(KeyEvent.VK_SLASH, move);
                            break;
                    }
                    it.remove();
                    break;
                }

                case SWITCH_ACTIVE_CHARACTER:
                    keyMap.put(KeyEvent.VK_SPACE, move);
                    it.remove();
                    break;
            }
        }
    }

    Map<Integer, ZMove> keyMap = new HashMap<>();

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        UIZombicide game = UIZombicide.getInstance();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_SLASH: {
                ZMove move = keyMap.get(e.getKeyCode());
                if (move != null) {
                    game.setResult(move);
                    keyMap.clear();
                }
                break;
            }
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
                // zoom in
                renderer.animateZoomTo(renderer.getZoomPercent() + 0.25f);
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_UNDERSCORE:
                // zoom out
                renderer.animateZoomTo(renderer.getZoomPercent() - 0.25f);
                break;

            case KeyEvent.VK_T:
                ZombicideApplet.instance.setStringProperty("tiles", renderer.toggleDrawTiles() ? "yes" : "no");
                break;

            case KeyEvent.VK_D:
                ZombicideApplet.instance.setStringProperty("debugText", renderer.toggleDrawDebugText() ? "yes" : "no");
                break;

            case KeyEvent.VK_R:
                ZombicideApplet.instance.setStringProperty("rangedAccessibility", renderer.toggleDrawRangedAccessibility() ? "yes" : "no");
                break;

            case KeyEvent.VK_P:
                ZombicideApplet.instance.setStringProperty("drawZombiePaths", renderer.toggleDrawZoombiePaths() ? "yes" : "no");
                break;

            case KeyEvent.VK_H:
                ZombicideApplet.instance.setStringProperty("drawTowersHighlighted", renderer.toggleDrawTowersHighlighted() ? "yes" : "no");
                break;

            case KeyEvent.VK_M:
                ZombicideApplet.instance.setIntProperty("miniMapMode", renderer.toggleDrawMinimap());
                break;

            case KeyEvent.VK_BACK_QUOTE:
                ZombicideApplet.instance.boardComp.renderer.setOverlay(new Table()
                    .addRow("+ / -", "Zoom in / out", String.format("%s%%", Math.round(100f*renderer.getZoomPercent())))
                    .addRow("T", "Toggle draw Tiles", renderer.getDrawTiles())
                    .addRow("D", "Toggle draw Debug text", renderer.getDrawDebugText())
                    .addRow("R", "Toggle show Ranged Accessibility", renderer.getDrawRangedAccessibility())
                    .addRow("P", "Toggle draw zombie paths", renderer.getDrawZombiePaths())
                    .addRow("H", "Toggle draw towers highlighted", renderer.getDrawTowersHighlighted())
                    .addRow("M", "Toggle Minimap mode", renderer.getMiniMapMode())
                );
                break;
            default:
                System.err.println("Got code " + e.getKeyCode());
        }
        repaint();
    }

    @Override
    public synchronized void keyReleased(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_BACK_QUOTE:
                renderer.setOverlay(null);
                repaint();
                break;
        }
    }

    @Override
    protected void onMouseWheel(int rotation) {
        if (renderer != null)
            renderer.scroll(0, -5f * rotation);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {
        log.debug("grabFocus");
        requestFocusInWindow();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}