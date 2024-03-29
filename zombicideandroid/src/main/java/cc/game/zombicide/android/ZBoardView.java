package cc.game.zombicide.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;

import cc.lib.android.DroidGraphics;
import cc.lib.android.UIComponentView;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZDir;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTile;
import cc.lib.zombicide.ZZombieType;
import cc.lib.zombicide.ui.UIZBoardRenderer;
import cc.lib.zombicide.ui.UIZComponent;

public class ZBoardView extends UIComponentView implements UIZComponent<DroidGraphics> {

    public ZBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    int progress = 0;
    int numImages = 21;

    void initZombieImages(DroidGraphics g, ZZombieType t, int ... ids) {
        t.imageOptions = new int[ids.length/2];
        t.imageOutlineOptions = new int[ids.length/2];
        for (int i=0, ii=0; i<ids.length; i+=2, ii++) {
            t.imageOptions[ii] = ids[i];
            t.imageOutlineOptions[ii] = ids[i+1];
        }

        t.imageDims = new GDimension[ids.length/2];
        for (int i=0; i<t.imageDims.length; i++) {
            t.imageDims[i] = new GDimension(g.getImage(ids[i*2]));
        }
    }

    @Override
    protected void preDrawInit(DroidGraphics g) {
        g.setTextModePixels(true);
        g.setTextHeight(getResources().getDimension(R.dimen.board_view_text_size));
        g.setLineThicknessModePixels(false);
        //g.getPaint().setStrokeWidth(getResources().getDimension(R.dimen.default_line_width));
        super.preDrawInit(g);
    }

    void initCharacter(DroidGraphics g, ZPlayerName pl, int cardImageId, int charImageId, int outlineImageId) {
        pl.imageId = charImageId;
        pl.cardImageId = cardImageId;
        pl.imageDim = new GDimension(g.getImage(charImageId));
        pl.outlineImageId = outlineImageId;
    }

    int [] tileIds = new int[0];

    private void deleteTiles(DroidGraphics g) {
        for (int t : tileIds) {
            if (t > 0)
                g.deleteImage(t);
        }
        tileIds = new int[0];
    }

    @Override
    public void loadTiles(DroidGraphics g, ZTile[] tiles) {
        progress = 0;
        numImages = tiles.length;
        UIZBoardRenderer renderer = (UIZBoardRenderer)getRenderer();
        deleteTiles(g);
        try {
            tileIds = new int[tiles.length];
            for (int i=0; i<tiles.length; i++) {
                int id = g.loadImage("ztile_" + tiles[i].id + ".png");
                if (id < 0)
                    throw new Exception("Failed to load " + tiles[i].id);
                if (tiles[i].orientation == 0) {
                    tileIds[i] = id;
                    continue;
                }

                tileIds[i] = g.newRotatedImage(id, tiles[i].orientation);
                g.deleteImage(id);
            }
            renderer.onTilesLoaded(tileIds);
            renderer.onLoaded();
            redraw();
        } catch (Exception e) {
            deleteTiles(g);
            e.printStackTrace();
        }
    }

    @Override
    protected void loadAssets(DroidGraphics g) {

        initCharacter(g, ZPlayerName.Baldric, R.drawable.zcard_baldric, R.drawable.zchar_baldric, R.drawable.zchar_baldric_outline);
        initCharacter(g, ZPlayerName.Benson, R.drawable.zcard_benson, R.drawable.zchar_benson, R.drawable.zchar_benson_outline);
        initCharacter(g, ZPlayerName.Jain, R.drawable.zcard_jain, R.drawable.zchar_jain, R.drawable.zchar_jain_outline);
        initCharacter(g, ZPlayerName.Tucker, R.drawable.zcard_tucker, R.drawable.zchar_tucker, R.drawable.zchar_tucker_outline);
        initCharacter(g, ZPlayerName.Silas, R.drawable.zcard_silas, R.drawable.zchar_silas, R.drawable.zchar_silas_outline);
        initCharacter(g, ZPlayerName.Samson, R.drawable.zcard_samson, R.drawable.zchar_samson, R.drawable.zchar_samson_outline);
        initCharacter(g, ZPlayerName.Nelly, R.drawable.zcard_nelly, R.drawable.zchar_nelly, R.drawable.zchar_nelly_outline);
        initCharacter(g, ZPlayerName.Ann, R.drawable.zcard_ann, R.drawable.zchar_ann, R.drawable.zchar_ann_outline);
        initCharacter(g, ZPlayerName.Clovis, R.drawable.zcard_clovis, R.drawable.zchar_clovis, R.drawable.zchar_clovis_outline);
        initCharacter(g, ZPlayerName.Karl, R.drawable.zcard_karl, R.drawable.zchar_karl, R.drawable.zchar_karl_outline);
        initCharacter(g, ZPlayerName.Ariane, R.drawable.zcard_ariane, R.drawable.zchar_ariane, R.drawable.zchar_ariane_outline);
        initCharacter(g, ZPlayerName.Morrigan, R.drawable.zcard_morrigan, R.drawable.zchar_morrigan, R.drawable.zchar_morrigan_outline);
        initCharacter(g, ZPlayerName.Theo, R.drawable.zcard_theo, R.drawable.zchar_theo, R.drawable.zchar_theo_outline);

        initZombieImages(g, ZZombieType.Walker,
                R.drawable.zwalker1, R.drawable.zwalker1_outline,
                R.drawable.zwalker2, R.drawable.zwalker2_outline,
                R.drawable.zwalker3, R.drawable.zwalker3_outline,
                R.drawable.zwalker4, R.drawable.zwalker4_outline,
                R.drawable.zwalker5, R.drawable.zwalker5_outline);

        initZombieImages(g, ZZombieType.Abomination, R.drawable.zabomination, R.drawable.zabomination_outline);
        initZombieImages(g, ZZombieType.Necromancer, R.drawable.znecro, R.drawable.znecro_outline);
        initZombieImages(g, ZZombieType.Runner, R.drawable.zrunner1, R.drawable.zrunner1_outline, R.drawable.zrunner2, R.drawable.zrunner2_outline);
        initZombieImages(g, ZZombieType.Fatty, R.drawable.zfatty1, R.drawable.zfatty1_outline, R.drawable.zfatty2, R.drawable.zfatty2_outline);
        initZombieImages(g, ZZombieType.Wolfz, R.drawable.zwulf1, R.drawable.zwulf1_outline, R.drawable.zwulf2, R.drawable.zwulf2_outline);
        initZombieImages(g, ZZombieType.Wolfbomination, R.drawable.zwolfabom, R.drawable.zwolfabom_outline);
        initZombieImages(g, ZZombieType.Wolfz, R.drawable.zwulf1, R.drawable.zwulf1_outline, R.drawable.zwulf2, R.drawable.zwulf2_outline);

        // Fire images are extracted from larger main image
        int[][] cells = {
                {0, 0, 56, 84}, {56, 0, 131 - 56, 84}, {131, 0, 196 - 131, 84},
                {0, 84, 60, 152 - 84}, {60, 84, 122 - 60, 152 - 84}, {122, 84, 196 - 122, 152 - 84}
        };

        numImages = ZIcon.values().length;
        ZIcon.FIRE.imageIds = g.loadImageCells(R.drawable.zfire_icons, cells);
        publishProgress(++progress);

        ZIcon.CLAWS.imageIds = new int[]{
                R.drawable.zclaws1_icon,
                R.drawable.zclaws2_icon,
                R.drawable.zclaws3_icon,
                R.drawable.zclaws4_icon,
                R.drawable.zclaws5_icon,
                R.drawable.zclaws6_icon
        };
        publishProgress(++progress);

        ZIcon.SLASH.imageIds = new int[]{
                R.drawable.zslash1,
                R.drawable.zslash2,
                R.drawable.zslash3,
                R.drawable.zslash4,
                R.drawable.zslash5
        };
        publishProgress(++progress);

        // Icons that spin around
        for (Pair<ZIcon, Integer> pair : Utils.toArray(
                new Pair(ZIcon.DRAGON_BILE, R.drawable.zdragonbile_icon),
                new Pair(ZIcon.TORCH, R.drawable.ztorch_icon),
                new Pair(ZIcon.DAGGER, R.drawable.zdagger_icon),
                new Pair(ZIcon.SWORD, R.drawable.zsword_icon))) {
            int[] ids = pair.first.imageIds = new int[8];
            ids[0] = pair.second;
            for (int i = 1; i < ids.length; i++) {
                int deg = 45 * i;
                ids[i] = g.newRotatedImage(ids[0], deg);
            }
            publishProgress(++progress);
        }

        // Icons that only have a single image
        for (Pair<ZIcon, Integer> pair : Utils.toArray(
                new Pair(ZIcon.SHIELD, R.drawable.zshield_icon),
                new Pair(ZIcon.SLIME, R.drawable.zslime_icon),
                new Pair(ZIcon.FIREBALL, R.drawable.zfireball),
                new Pair(ZIcon.GRAVESTONE, R.drawable.zgravestone),
                new Pair(ZIcon.PADLOCK, R.drawable.zpadlock3),
                new Pair(ZIcon.SKULL, R.drawable.zskull)
        )) {
            pair.first.imageIds = new int[]{pair.second};
            publishProgress(++progress);
        }

        // Directional, like projectiles
        for (Pair<ZIcon, Integer> pair : Utils.toArray(
                new Pair(ZIcon.ARROW, R.drawable.zarrow_icon)
        )) {
            int[] ids =pair.first.imageIds = new int[4];
            ids[ZDir.NORTH.ordinal()] = g.newRotatedImage(pair.second, 270);
            ids[ZDir.SOUTH.ordinal()] = g.newRotatedImage(pair.second, 90);
            ids[ZDir.EAST.ordinal()] = pair.second;
            ids[ZDir.WEST.ordinal()] = g.newRotatedImage(pair.second, 180);
            publishProgress(++progress);
        }

        for (Pair<ZIcon, Integer> pair : Utils.toArray(
                new Pair(ZIcon.SPAWN_RED, R.drawable.zspawn_red),
                new Pair(ZIcon.SPAWN_BLUE, R.drawable.zspawn_blue),
                new Pair(ZIcon.SPAWN_GREEN, R.drawable.zspawn_green))) {
            int [] ids = pair.first.imageIds = new int[4];
            ids[ZDir.NORTH.ordinal()] = pair.second;
            ids[ZDir.SOUTH.ordinal()] = pair.second;
            ids[ZDir.WEST.ordinal()] = g.newRotatedImage(pair.second, 270);
            ids[ZDir.EAST.ordinal()] = g.newRotatedImage(pair.second, 90);
            publishProgress(++progress);
        }

        Log.i("IMAGES", "Loaded total of " + progress);
        publishProgress(numImages);
    }

    void publishProgress(int p) {
        progress = p;
        postInvalidate();
        Utils.waitNoThrow(this, 100);
    }

    @Override
    protected float getProgress() {
        return (float)progress / numImages;
    }

    @Override
    protected void onLoading() {
        ((UIZBoardRenderer)getRenderer()).onLoading();
    }

    @Override
    protected void onLoaded() {
        ((UIZBoardRenderer)getRenderer()).onLoaded();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
/*        synchronized (this) {
            notify(); // notify activity we have rendered a frame
        }*/
    }
}
