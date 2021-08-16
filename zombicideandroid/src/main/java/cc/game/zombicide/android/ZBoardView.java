package cc.game.zombicide.android;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

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
        t.imageOptions = ids;
        t.imageDims = new GDimension[ids.length];
        for (int i=0; i<ids.length; i++) {
            t.imageDims[i] = new GDimension(g.getImage(ids[i]));
        }
    }

    @Override
    protected void preDrawInit(DroidGraphics g) {
        UIZBoardRenderer.DEBUG_DRAW_ZONE_INFO = BuildConfig.DEBUG;
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

        initZombieImages(g, ZZombieType.Walker,
                R.drawable.zwalker1,
                R.drawable.zwalker2,
                R.drawable.zwalker3,
                R.drawable.zwalker4,
                R.drawable.zwalker5);

        initZombieImages(g, ZZombieType.Abomination, R.drawable.zabomination);
        initZombieImages(g, ZZombieType.Necromancer, R.drawable.znecro);
        initZombieImages(g, ZZombieType.Runner, R.drawable.zrunner1,R.drawable.zrunner2);
        initZombieImages(g, ZZombieType.Fatty, R.drawable.zfatty1,R.drawable.zfatty2);

        int [][] cells = {
                { 0,0, 56, 84 }, { 56, 0, 131-56, 84 }, { 131, 0, 196-131, 84 },
                { 0,84, 60, 152-84 }, { 60,84,122-60,152-84 }, { 122,84,196-122,152-84 }
        };

        ZIcon.FIRE.imageIds = g.loadImageCells(R.drawable.zfire_icons, cells);
        publishProgress(++progress);

        ZIcon.CLAWS.imageIds = new int[] {
                R.drawable.zclaws1_icon,
                R.drawable.zclaws2_icon,
                R.drawable.zclaws3_icon,
                R.drawable.zclaws4_icon,
                R.drawable.zclaws5_icon,
                R.drawable.zclaws6_icon
        };

        ZIcon.SLASH.imageIds = new int[] {
                R.drawable.zslash1,
                R.drawable.zslash2,
                R.drawable.zslash3,
                R.drawable.zslash4,
                R.drawable.zslash5
        };

        int [] ids = ZIcon.DRAGON_BILE.imageIds = new int[8];
        ids[0] = R.drawable.zdragonbile_icon;
        for (int i=1; i<ids.length; i++) {
            int deg = 45*i;
            ids[i] = g.newRotatedImage(ids[0], deg);
            publishProgress(++progress);
        }

        ids = ZIcon.TORCH.imageIds = new int[8];
        ids[0] = R.drawable.ztorch_icon;
        for (int i=1; i<ids.length; i++) {
            int deg = 45*i;
            ids[i] = g.newRotatedImage(ids[0], deg);
            publishProgress(++progress);
        }

        ZIcon.SHIELD.imageIds = new int[] {
                R.drawable.zshield_icon
        };

        ZIcon.SLIME.imageIds = new int[] {
                R.drawable.zslime_icon
        };

        ZIcon.FIREBALL.imageIds = new int[] {
                R.drawable.zfireball
        };

        ZIcon.GRAVESTONE.imageIds = new int[] {
                R.drawable.zgravestone
        };

        ZIcon.PADLOCK.imageIds = new int[] {
                R.drawable.zpadlock3
        };

        ids = ZIcon.ARROW.imageIds = new int[4];
        ids[ZDir.EAST.ordinal()] = R.drawable.zarrow_icon;
        ids[ZDir.WEST.ordinal()] = g.newRotatedImage(R.drawable.zarrow_icon, 180);
        publishProgress(++progress);
        ids[ZDir.NORTH.ordinal()] = g.newRotatedImage(R.drawable.zarrow_icon, 270);
        publishProgress(++progress);
        ids[ZDir.SOUTH.ordinal()] = g.newRotatedImage(R.drawable.zarrow_icon, 90);
        publishProgress(++progress);

        ids = ZIcon.SPAWN.imageIds = new int[4];
        ids[ZDir.NORTH.ordinal()] = R.drawable.zspawn;
        ids[ZDir.SOUTH.ordinal()] = R.drawable.zspawn;
        ids[ZDir.WEST.ordinal()] = g.newRotatedImage(R.drawable.zspawn, 270);
        publishProgress(++progress);
        ids[ZDir.EAST.ordinal()] = g.newRotatedImage(R.drawable.zspawn, 90);
        publishProgress(++progress);

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
}
