package cc.android.scorekeeper;


import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import cc.lib.android.CCActivityBase;
import cc.lib.game.Utils;

public class ScoreKeeper extends CCActivityBase {

    final int MAX_POINTS = 50;

    final int TL = 0;
    final int TR = 1;
    final int BL = 2;
    final int BR = 3;

    enum Cell {
        WATER(R.color.blue_fore, R.color.blue_bk, R.drawable.water_icon),
        FIRE(R.color.red_fore, R.color.red_bk, R.drawable.fire_icon),
        TREE(R.color.green_fore, R.color.green_bk, R.drawable.tree_icon),
        SKULL(R.color.black_fore, R.color.black_bk, R.drawable.skull_icon),
        SUN(R.color.white_fore, R.color.white_bk, R.drawable.sun_icon);

        Cell(int foreColor, int backColor, int iconResource) {
            this.foreColor = foreColor;
            this.backColor = backColor;
            this.iconResource = iconResource;
        }

        final int foreColor;
        final int backColor;
        final int iconResource;
    }

    final int [] points = new int[4];
    final Cell [] cells = new Cell[4];

    final ViewGroup [] vg = new ViewGroup[4];
    final ViewPager [] vp = new ViewPager[4];
    final ImageButton [] ibRemove = new ImageButton[4];
    final ImageButton [] ibToggle = new ImageButton[4];
    ImageButton ibAdd;
    ViewGroup topRow, bottomRow;

    final boolean [] visible = new boolean[4];

    class ItemPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

        final int index;
        ItemPagerAdapter(int index) {
            this.index = index;
        }

        @Override
        public int getCount() {
            return MAX_POINTS+1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = View.inflate(ScoreKeeper.this, R.layout.tv_points, null);
            TextView tv = (TextView)v.findViewById(R.id.tvScore);
            ImageView iv = (ImageView)v.findViewById(R.id.ivIcon);
            tv.setText(String.valueOf(position));
            tv.setTextColor(getResources().getColor(cells[index].foreColor));
            v.setBackgroundColor(getResources().getColor(cells[index].backColor));
            iv.setImageResource(cells[index].iconResource);
            iv.setColorFilter(getResources().getColor(cells[index].foreColor), PorterDuff.Mode.MULTIPLY);
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            points[index] = position;
        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scorekeeper_layout);

        vg[TL] = (ViewGroup)findViewById(R.id.vgTL);
        vg[TR] = (ViewGroup)findViewById(R.id.vgTR);
        vg[BL] = (ViewGroup)findViewById(R.id.vgBL);
        vg[BR] = (ViewGroup)findViewById(R.id.vgBR);

        vp[TL] = (ViewPager)findViewById(R.id.vpTL);
        vp[TR] = (ViewPager)findViewById(R.id.vpTR);
        vp[BL] = (ViewPager)findViewById(R.id.vpBL);
        vp[BR] = (ViewPager)findViewById(R.id.vpBR);

        ibRemove[TL] = (ImageButton)findViewById(R.id.ibRemoveTL);
        ibRemove[TR] = (ImageButton)findViewById(R.id.ibRemoveTR);
        ibRemove[BL] = (ImageButton)findViewById(R.id.ibRemoveBL);
        ibRemove[BR] = (ImageButton)findViewById(R.id.ibRemoveBR);

        ibToggle[TL] = (ImageButton)findViewById(R.id.ibToggleTL);
        ibToggle[TR] = (ImageButton)findViewById(R.id.ibToggleTR);
        ibToggle[BL] = (ImageButton)findViewById(R.id.ibToggleBL);
        ibToggle[BR] = (ImageButton)findViewById(R.id.ibToggleBR);

        ImageButton [] ibPlus5 = new ImageButton[4];
        ibPlus5[TL] = (ImageButton)findViewById(R.id.ibAddTL);
        ibPlus5[TR] = (ImageButton)findViewById(R.id.ibAddTR);
        ibPlus5[BL] = (ImageButton)findViewById(R.id.ibAddBL);
        ibPlus5[BR] = (ImageButton)findViewById(R.id.ibAddBR);

        topRow = (ViewGroup)findViewById(R.id.vgTopRow);
        bottomRow = (ViewGroup)findViewById(R.id.vgBottomRow);

        for (int i=0; i<4; i++) {
            final int index = i;
            ItemPagerAdapter ap = new ItemPagerAdapter(i);
            vp[i].setAdapter(ap);
            vp[i].setOnPageChangeListener(ap);
            ibRemove[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    vg[index].setVisibility(View.GONE);
                    visible[index] = false;
                    ibAdd.setVisibility(View.VISIBLE);
                    updateTopBottomRow();
                }
            });
            ibToggle[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Cell cell = (Cell)vg[index].getTag();
                    Cell nxtCell = Utils.incrementValue(cell, Cell.values());
                    vg[index].setTag(cells[index] = nxtCell);
                    vp[index].setAdapter(new ItemPagerAdapter(index));
                    vp[index].setCurrentItem(points[index]);
                }
            });
            ibPlus5[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int p = points[index];
                    p = Utils.clamp(5*((p+5)/5), 0, MAX_POINTS);
                    points[index] = p;
                    vp[index].setCurrentItem(p, true);
                }
            });
        }


        ibAdd = (ImageButton)findViewById(R.id.ibAdd);
        ibAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i=0; i<visible.length; i++) {
                    if (!visible[i]) {
                        vg[i].setVisibility(View.VISIBLE);
                        visible[i] = true;
                        if (i == visible.length-1)
                            ibAdd.setVisibility(View.GONE);
                        break;
                    }
                }
                updateTopBottomRow();
            }
        });

    }

    private void updateTopBottomRow() {
        boolean topVisible = visible[TL] || visible[TR];
        boolean bottomVisible = visible[BL] || visible[BR];

        topRow.setVisibility(topVisible ? View.VISIBLE : View.GONE);
        bottomRow.setVisibility(bottomVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences p = getPrefs();
        points[TL] = p.getInt("pointsTL", 20);
        points[TR] = p.getInt("pointsTR", 20);
        points[BL] = p.getInt("pointsBL", 20);
        points[BR] = p.getInt("pointsBR", 20);

        visible[TL] = p.getBoolean("visibleTL", true);
        visible[TR] = p.getBoolean("visibleTR", true);
        visible[BL] = p.getBoolean("visibleBL", true);
        visible[BR] = p.getBoolean("visibleBR", true);

        vg[TL].setTag(cells[TL] = Cell.valueOf(p.getString("cellTL", Cell.FIRE.name())));
        vg[TR].setTag(cells[TR] = Cell.valueOf(p.getString("cellTR", Cell.WATER.name())));
        vg[BL].setTag(cells[BL] = Cell.valueOf(p.getString("cellBL", Cell.TREE.name())));
        vg[BR].setTag(cells[BR] = Cell.valueOf(p.getString("cellBR", Cell.SKULL.name())));

        ibAdd.setVisibility(View.GONE);
        for (int i=0; i<4; i++) {
            vg[i].setVisibility(visible[i] ? View.VISIBLE : View.GONE);
            vp[i].setCurrentItem(points[i]);
            if (!visible[i])
                ibAdd.setVisibility(View.VISIBLE);
        }
        updateTopBottomRow();
    }

    @Override
    protected void onPause() {
        getPrefs().edit()
                .putInt("pointsTL", points[TL])
                .putInt("pointsTR", points[TR])
                .putInt("pointsBL", points[BL])
                .putInt("pointsBR", points[BR])
                .putBoolean("visibleTL", visible[TL])
                .putBoolean("visibleTR", visible[TR])
                .putBoolean("visibleBL", visible[BL])
                .putBoolean("visibleBR", visible[BR])
                .putString("cellTL", cells[TL].name())
                .putString("cellTR", cells[TR].name())
                .putString("cellBL", cells[BL].name())
                .putString("cellBR", cells[BR].name())
                .apply();
        super.onPause();
    }
}
