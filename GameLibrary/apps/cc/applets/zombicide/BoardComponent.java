package cc.applets.zombicide;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.ui.UIRenderer;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZComponent;
import cc.lib.zombicide.ui.UIZombicide;

class BoardComponent extends AWTComponent implements UIZComponent<AWTGraphics> {

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
        UIZBoardRenderer.DEBUG = true;
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
            grabFocus();
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

        Object [][] files = {

            { ZZombieType.Abomination, "zabomination.gif" },
            { ZZombieType.Necromancer, "znecro.gif" },
            { ZZombieType.Walker, "zwalker1.gif" },
            { ZZombieType.Walker, "zwalker2.gif" },
            { ZZombieType.Walker, "zwalker3.gif" },
            { ZZombieType.Walker, "zwalker4.gif" },
            { ZZombieType.Walker, "zwalker5.gif" },
            { ZZombieType.Runner, "zrunner1.gif" },
            { ZZombieType.Runner, "zrunner1.gif" },
            { ZZombieType.Fatty, "zfatty1.gif" },
            { ZZombieType.Fatty, "zfatty2.gif" },
            { ZZombieType.Wolfz, "zwulf1.gif" },
            { ZZombieType.Wolfz, "zwulf2.gif" },
            { ZZombieType.Wolfbomination, "zwolfabom.gif" },

            { ZPlayerName.Clovis, "zchar_clovis.gif" },
            { ZPlayerName.Baldric, "zchar_baldric.gif" },
            { ZPlayerName.Ann, "zchar_ann.gif" },
            { ZPlayerName.Nelly, "zchar_nelly.gif" },
            { ZPlayerName.Samson, "zchar_samson.gif" },
            { ZPlayerName.Silas, "zchar_silas.gif" },
            { ZPlayerName.Tucker, "zchar_tucker.gif" },
            { ZPlayerName.Jain, "zchar_jain.gif" },
            { ZPlayerName.Benson, "zchar_benson.gif" },
            { ZPlayerName.Theo, "zchar_theo.gif" },
            { ZPlayerName.Morrigan, "zchar_morrigan.gif" },
            { ZPlayerName.Karl, "zchar_karl.gif" },
            { ZPlayerName.Ariane, "zchar_ariane.gif" },

            { ZPlayerName.Clovis, "zchar_clovis_outline.gif" },
            { ZPlayerName.Baldric, "zchar_baldric_outline.gif" },
            { ZPlayerName.Ann, "zchar_ann_outline.gif" },
            { ZPlayerName.Nelly, "zchar_nelly_outline.gif" },
            { ZPlayerName.Samson, "zchar_samson_outline.gif" },
            { ZPlayerName.Silas, "zchar_silas_outline.gif" },
            { ZPlayerName.Tucker, "zchar_tucker_outline.gif" },
            { ZPlayerName.Jain, "zchar_jain_outline.gif" },
            { ZPlayerName.Benson, "zchar_benson_outline.gif" },

            { ZPlayerName.Ann.name(), "zcard_ann.gif" },
            { ZPlayerName.Baldric.name(), "zcard_baldric.gif" },
            { ZPlayerName.Clovis.name(), "zcard_clovis.gif" },
            { ZPlayerName.Nelly.name(), "zcard_nelly.gif" },
            { ZPlayerName.Samson.name(), "zcard_samson.gif" },
            { ZPlayerName.Silas.name(), "zcard_silas.gif" },
            { ZPlayerName.Tucker.name(), "zcard_tucker.gif" },
            { ZPlayerName.Jain.name(), "zcard_jain.gif" },
            { ZPlayerName.Benson.name(), "zcard_benson.gif" },
            { ZPlayerName.Theo.name(), "zcard_theo.gif" },
            { ZPlayerName.Morrigan.name(), "zcard_morrigan.gif" },
            { ZPlayerName.Karl.name(), "zcard_karl.gif" },
            { ZPlayerName.Ariane.name(), "zcard_ariane.gif" },

            { ZIcon.DRAGON_BILE, "zdragonbile_icon.gif" },
            { ZIcon.CLAWS, "zclaws1_icon.gif" },
            { ZIcon.CLAWS, "zclaws2_icon.gif" },
            { ZIcon.CLAWS, "zclaws3_icon.gif" },
            { ZIcon.CLAWS, "zclaws4_icon.gif" },
            { ZIcon.CLAWS, "zclaws5_icon.gif" },
            { ZIcon.CLAWS, "zclaws6_icon.gif" },
            { ZIcon.SHIELD, "zshield_icon.gif" },
            { ZIcon.SLIME, "zslime_icon.gif" },
            { ZIcon.TORCH, "ztorch_icon.gif" },
            { ZIcon.ARROW, "zarrow_icon.gif" },
            { ZIcon.SPAWN, "zspawn.gif" },
            { ZIcon.SLASH, "zslash1.gif" },
            { ZIcon.SLASH, "zslash2.gif" },
            { ZIcon.SLASH, "zslash3.gif" },
            { ZIcon.SLASH, "zslash4.gif" },
            { ZIcon.SLASH, "zslash5.gif" },
            { ZIcon.FIREBALL, "zfireball.gif" },
            { ZIcon.GRAVESTONE, "zgravestone.gif" },
            { ZIcon.PADLOCK, "zpadlock2.gif" },
            { ZIcon.SKULL, "zskull.gif"},
        };

        Map<Object, List<Integer>> objectToImageMap = new HashMap<>();

        totalImagesToLoad = files.length + 30;
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
            type.imageOptions = Utils.toIntArray(objectToImageMap.get(type));
            type.imageDims = new GDimension[type.imageOptions.length];
            int idx=0;
            for (int id : type.imageOptions) {
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

        {
            ZIcon icon = ZIcon.DRAGON_BILE;
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

        {
            ZIcon icon = ZIcon.TORCH;
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

        {
            ZIcon icon = ZIcon.ARROW;
            int [] ids = new int[4];
            int eastId = objectToImageMap.get(icon).get(0);
            ids[ZDir.EAST.ordinal()] = eastId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(eastId, 180);
            numImagesLoaded++;
            repaint();
            ids[ZDir.NORTH.ordinal()] = g.createRotatedImage(eastId, 270);
            numImagesLoaded++;
            repaint();
            ids[ZDir.SOUTH.ordinal()] = g.createRotatedImage(eastId, 90);
            numImagesLoaded++;
            repaint();
            icon.imageIds = ids;
        }

        {
            ZIcon icon = ZIcon.SPAWN;
            int [] ids = new int[4];
            int northId = objectToImageMap.get(icon).get(0);
            ids[ZDir.NORTH.ordinal()] = northId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(northId, 270);
            numImagesLoaded++;
            repaint();
            ids[ZDir.EAST.ordinal()] = g.createRotatedImage(northId, 90);
            numImagesLoaded++;
            repaint();
            ids[ZDir.SOUTH.ordinal()] = northId;
            icon.imageIds = ids;
        }

        {
            ZIcon.CLAWS.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.CLAWS));
            ZIcon.SHIELD.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SHIELD));
            ZIcon.SLIME.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SLIME));
            ZIcon.SLASH.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SLASH));
            ZIcon.FIREBALL.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.FIREBALL));
            ZIcon.GRAVESTONE.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.GRAVESTONE));
            ZIcon.PADLOCK.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.PADLOCK));
            ZIcon.SKULL.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SKULL));
        }

        {
            ZIcon icon = ZIcon.FIRE;
            int [][] cells = {
                    { 0,0, 56, 84 }, { 56, 0, 131-56, 84 }, { 131, 0, 196-131, 84 },
                    { 0,84, 60, 152-84 }, { 60,84,122-60,152-84 }, { 122,84,196-122,152-84 }
            };
            icon.imageIds  = g.loadImageCells("zfire_icons.gif", cells);
            numImagesLoaded++;
            repaint();

        }

        log.debug("Images: " + objectToImageMap);
        numImagesLoaded = totalImagesToLoad;
        ZombicideApplet.instance.onAllImagesLoaded();
        renderer.setDrawTiles(ZombicideApplet.instance.getStringProperty("tiles", "no").equals("yes"));
        repaint();
    }

    int [] loadedTiles = new int[0];
    int numTilesLoaded = 0;

    @Override
    public void loadTiles(AWTGraphics g, ZTile[] tiles) {
        numTilesLoaded = 0;
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

    @Override
    public void keyPressed(KeyEvent e) {
        UIZombicide game = UIZombicide.getInstance();
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                game.tryWalk(ZDir.WEST);
                break;
            case KeyEvent.VK_RIGHT:
                game.tryWalk(ZDir.EAST);
                break;
            case KeyEvent.VK_UP:
                game.tryWalk(ZDir.NORTH);
                break;
            case KeyEvent.VK_DOWN:
                game.tryWalk(ZDir.SOUTH);
                break;
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
            case KeyEvent.VK_SPACE:
                // toggle active player
                game.trySwitchActivePlayer();
                break;
            case KeyEvent.VK_SLASH:
                if (game.getBoard().canMove(game.getCurrentCharacter().getCharacter(), ZDir.DESCEND)) {
                    game.setResult(ZMove.newWalkDirMove(ZDir.DESCEND));
                } else if (game.getBoard().canMove(game.getCurrentCharacter().getCharacter(), ZDir.ASCEND)) {
                    game.setResult(ZMove.newWalkDirMove(ZDir.ASCEND));
                }
                break;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_T:
                renderer.toggleDrawTiles();
                ZombicideApplet.instance.setStringProperty("tiles", renderer.isDrawTiles() ? "yes" : "no");
                break;
        }
        repaint();
    }
}