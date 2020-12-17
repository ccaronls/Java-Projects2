package cc.game.zombicide.android;

import android.content.Context;
import android.util.AttributeSet;

import cc.lib.android.DroidGraphics;
import cc.lib.android.UIComponentView;
import cc.lib.game.GDimension;
import cc.lib.zombicide.ZIcon;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZTiles;
import cc.lib.zombicide.ZZombieType;

public class ZBoardView extends UIComponentView implements ZTiles<DroidGraphics> {

    public ZBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context, AttributeSet attrs) {

    }

    int progress = 0;
    int numImages = 100;

    void initZombieImages(DroidGraphics g, ZZombieType t, int ... ids) {
        t.imageOptions = ids;
        t.imageDims = new GDimension[ids.length];
        for (int i=0; i<ids.length; i++) {
            t.imageDims[i] = new GDimension(g.getImage(ids[i]));
        }
    }

    void initCharacter(DroidGraphics g, ZPlayerName pl, int cardImageId, int charImageId) {
        pl.imageId = charImageId;
        pl.cardImageId = cardImageId;
        pl.imageDim = new GDimension(g.getImage(charImageId));
    }

    int [] tiles = new int[0];

    private void deleteTiles(DroidGraphics g) {
        for (int t : tiles) {
            if (t > 0)
                g.deleteImage(t);
        }
        tiles = new int[0];
    }

    @Override
    public int[] loadTiles(DroidGraphics g, String[] names, int[] orientations) {
        deleteTiles(g);
        try {
            tiles = new int[names.length];
            for (int i=0; i<tiles.length; i++) {
                int id = g.loadImage(names[i]);
                if (id < 0)
                    throw new Exception("Failed to load " + names[i]);
                tiles[i] = g.newRotatedImage(id, orientations[i]);
                g.deleteImage(id);
            }
            return tiles;
        } catch (Exception e) {
            deleteTiles(g);
            e.printStackTrace();
        }
        return tiles;
    }

    @Override
    protected void loadAssets(DroidGraphics g) {

        initCharacter(g, ZPlayerName.Baldric, R.drawable.zcard_baldric, R.drawable.zchar_baldric);
        initCharacter(g, ZPlayerName.Benson, R.drawable.zcard_benson, R.drawable.zchar_benson);
        initCharacter(g, ZPlayerName.Jain, R.drawable.zcard_jain, R.drawable.zchar_jain);
        initCharacter(g, ZPlayerName.Tucker, R.drawable.zcard_tucker, R.drawable.zchar_tucker);
        initCharacter(g, ZPlayerName.Silas, R.drawable.zcard_silas, R.drawable.zchar_silas);
        initCharacter(g, ZPlayerName.Samson, R.drawable.zcard_samson, R.drawable.zchar_samson);
        initCharacter(g, ZPlayerName.Nelly, R.drawable.zcard_nelly, R.drawable.zchar_nelly);
        initCharacter(g, ZPlayerName.Ann, R.drawable.zcard_ann, R.drawable.zchar_ann);
        initCharacter(g, ZPlayerName.Clovis, R.drawable.zcard_clovis, R.drawable.zchar_clovis);

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

        ZIcon.SPAWN.imageIds = new int[] {
                R.drawable.zspawn,
                g.newRotatedImage(R.drawable.zspawn, 90),
                g.newRotatedImage(R.drawable.zspawn, 270),
                R.drawable.zspawn,
        };
        publishProgress(++progress);

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

        publishProgress(++progress);

        ZIcon.DRAGON_BILE.imageIds = new int[] {
                R.drawable.zdragonbile_icon
        };

        ZIcon.SHIELD.imageIds = new int[] {
                R.drawable.zshield_icon
        };

        publishProgress(numImages);
    }

    void publishProgress(int p) {
        progress = p;
        postInvalidate();
    }

    @Override
    protected float getProgress() {
        return (float)progress / numImages;
    }
}
