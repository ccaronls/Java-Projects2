package cc.applets.zombicide;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.swing.AWTComponent;
import cc.lib.swing.AWTGraphics;
import cc.lib.zombicide.ZCharacter;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZGame;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZMove;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ui.UIZombicide;

class BoardComponent extends AWTComponent implements ZTiles {

    final Logger log = LoggerFactory.getLogger(getClass());

    BoardComponent() {
        setPreferredSize(250, 250);
    }

    UIZombicide getGame() {
        return UIZombicide.getInstance();
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

    int numImagesLoaded=0;
    int totalImagesToLoad=1000;

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / totalImagesToLoad;
    }

    @Override
    protected void onDimensionChanged(AWTGraphics g, int width, int height) {
        GDimension cellDim = getGame().board.initCellRects(g, width-5, height-5);
        int newWidth = (int)cellDim.width * getGame().board.getColumns();
        int newHeight = (int)cellDim.height * getGame().board.getRows();
        setPreferredSize(newWidth, newHeight);
    }

    @Override
    protected synchronized void paint(AWTGraphics g, int mouseX, int mouseY) {
        UIZombicide game = getGame();
        if (game != null) {
            game.draw(g, mouseX, mouseY);
            if (game.isAnimating())
                repaint();
        }
    }


    @Override
    protected void onFocusGained() {
        getGame().setOverlay(null);
    }

    @Override
    protected void onMousePressed(int mouseX, int mouseY) {
        getGame().onTap();
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
            { ZPlayerName.Clovis, "zchar_clovis.gif" },
            { ZPlayerName.Baldric, "zchar_baldric.gif" },
            { ZPlayerName.Ann, "zchar_ann.gif" },
            { ZPlayerName.Nelly, "zchar_nelly.gif" },
            { ZPlayerName.Samson, "zchar_samson.gif" },
            { ZPlayerName.Silas, "zchar_silas.gif" },
            { ZPlayerName.Tucker, "zchar_tucker.gif" },
            { ZPlayerName.Jain, "zchar_jain.gif" },
            { ZPlayerName.Benson, "zchar_benson.gif" },

            { ZPlayerName.Ann.name(), "zcard_ann.gif" },
            { ZPlayerName.Baldric.name(), "zcard_baldric.gif" },
            { ZPlayerName.Clovis.name(), "zcard_clovis.gif" },
            { ZPlayerName.Nelly.name(), "zcard_nelly.gif" },
            { ZPlayerName.Samson.name(), "zcard_samson.gif" },
            { ZPlayerName.Silas.name(), "zcard_silas.gif" },
            { ZPlayerName.Tucker.name(), "zcard_tucker.gif" },
            { ZPlayerName.Jain.name(), "zcard_jain.gif" },
            { ZPlayerName.Benson.name(), "zcard_benson.gif" },

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
        };

        Map<Object, List<Integer>> objectToImageMap = new HashMap<>();

        totalImagesToLoad = files.length;
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
            }
            icon.imageIds = ids;
        }

        {
            ZIcon icon = ZIcon.ARROW;
            int [] ids = new int[4];
            int eastId = objectToImageMap.get(icon).get(0);
            ids[ZDir.EAST.ordinal()] = eastId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(eastId, 180);
            ids[ZDir.NORTH.ordinal()] = g.createRotatedImage(eastId, 270);
            ids[ZDir.SOUTH.ordinal()] = g.createRotatedImage(eastId, 90);
            icon.imageIds = ids;
        }

        {
            ZIcon icon = ZIcon.SPAWN;
            int [] ids = new int[4];
            int northId = objectToImageMap.get(icon).get(0);
            ids[ZDir.NORTH.ordinal()] = northId;
            ids[ZDir.WEST.ordinal()] = g.createRotatedImage(northId, 270);
            ids[ZDir.EAST.ordinal()] = g.createRotatedImage(northId, 90);
            ids[ZDir.SOUTH.ordinal()] = g.createRotatedImage(northId, 1800);
            icon.imageIds = ids;
        }

        {
            ZIcon.CLAWS.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.CLAWS));
            ZIcon.SHIELD.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SHIELD));
            ZIcon.SLIME.imageIds = Utils.toIntArray(objectToImageMap.get(ZIcon.SLIME));
        }

        {
            ZIcon icon = ZIcon.FIRE;
            int [][] cells = {
                    { 0,0, 56, 84 }, { 56, 0, 131-56, 84 }, { 131, 0, 196-131, 84 },
                    { 0,84, 60, 152-84 }, { 60,84,122-60,152-84 }, { 122,84,196-122,152-84 }
            };
            icon.imageIds  = g.loadImageCells("zfire_icons.gif", cells);
        }

        log.debug("Images: " + objectToImageMap);
        numImagesLoaded = totalImagesToLoad;
        ZombicideApplet.instance.onAllImagesLoaded();
        repaint();
    }

    int [] tiles = new int[0];

    @Override
    public int[] loadTiles(AGraphics _g, String[] names, int[] orientations) {
        AWTGraphics g = (AWTGraphics)_g;
        for (int t : tiles) {
            g.deleteImage(t);
        }

        tiles = new int[names.length];

        for (int i=0; i<names.length; i++) {
            tiles[i] = g.loadImage("ztile_" + names[i] + ".png", orientations[i]);
        }
        return tiles;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        ZGame game = getGame();
        ZCharacter cur = game.getCurrentCharacter();
        if (cur != null) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (game.board.canMove(cur, ZDir.WEST)) {
                        UIZombicide.getInstance().setResult(ZMove.newWalkDirMove(ZDir.WEST));
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (game.board.canMove(cur, ZDir.EAST)) {
                        UIZombicide.getInstance().setResult(ZMove.newWalkDirMove(ZDir.EAST));
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (game.board.canMove(cur, ZDir.NORTH)) {
                        UIZombicide.getInstance().setResult(ZMove.newWalkDirMove(ZDir.NORTH));
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (game.board.canMove(cur, ZDir.SOUTH)) {
                        UIZombicide.getInstance().setResult(ZMove.newWalkDirMove(ZDir.SOUTH));
                    }
                    break;
            }
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_T:
                getGame().toggleDrawTiles();
                break;
        }
        repaint();
    }
}