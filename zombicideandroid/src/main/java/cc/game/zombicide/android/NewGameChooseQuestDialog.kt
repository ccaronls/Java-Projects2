package cc.game.zombicide.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import cc.lib.game.GRectangle;
import cc.lib.game.Utils;
import cc.lib.zombicide.ZQuests;
import cc.lib.zombicide.ZTile;

/**
 * Created by Chris Caron on 8/22/21.
 */
class NewGameChooseQuestDialog extends PagerAdapter {

    static final String TAG = NewGameChooseQuestDialog.class.getSimpleName();

    final List<ZQuests> allQuests;
    final ZombicideActivity activity;
    final Set<ZQuests> playable;
    int firstPage = 0;
    final Dialog dialog;

    NewGameChooseQuestDialog(ZombicideActivity activity, List<ZQuests> quests, Set<ZQuests> playable) {
        this.activity = activity;
        this.allQuests = quests;
        this.playable = playable;
        for (ZQuests q : allQuests) {
            if (!playable.contains(q)) {
                playable.add(q);
                break;
            }
            firstPage = Utils.clamp(firstPage + 1, 0, allQuests.size() - 1);
        }
        View view = View.inflate(activity, R.layout.viewpager_dialog, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        pager.setAdapter(this);
        pager.setCurrentItem(firstPage);
        dialog = activity.newDialogBuilder().setTitle(R.string.popup_title_choose_quest)
                .setView(view).setPositiveButton(R.string.popup_button_next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZQuests q = allQuests.get(pager.getCurrentItem());
                        if (playable.contains(q)) {
                            activity.showNewGameDialogChooseDifficulty(q);
                        } else {
                            Toast.makeText(activity, "Quest Locked", Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.popup_button_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.showNewGameDialog();
                    }
                }).show();


    }

    @Override
    public int getCount() {
        return allQuests.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ZQuests q = allQuests.get(position);
        View content = View.inflate(activity, R.layout.choose_quest_dialog_item, null);
        ZTile[] tiles = q.load().getTiles();
        Bitmap bm = null;
        if (tiles != null && tiles.length > 0) {
            GRectangle rect = new GRectangle();
            float imageDim = 64;
            for (ZTile tile : tiles) {
                rect.addEq(tile.quadrant);
            }
            rect.scaleDimension(imageDim);
            bm = Bitmap.createBitmap(Math.round(rect.getWidth()), Math.round(rect.getHeight()), Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(bm);
            c.scale(imageDim, imageDim);

            for (ZTile tile : tiles) {
                try {
                    Bitmap t = BitmapFactory.decodeStream(container.getContext().getAssets().open("ztile_" + tile.id + ".png"));
                    c.save();
                    c.translate(tile.quadrant.x + 1.5f, tile.quadrant.y + 1.5f);
                    c.rotate(tile.orientation);
                    c.translate(-1.5f, -1.5f);
                    Rect src = new Rect(0, 0, t.getWidth(), t.getHeight());
                    Rect dst = new Rect(0, 0, 3, 3);
                    c.drawBitmap(t, src, dst, null);
                    t.recycle();
                    c.restore();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        TextView tv_body;
        ImageView iv_board;

        if (bm == null || (bm.getWidth() > bm.getHeight())) {
            // use horz
            tv_body = content.findViewById(R.id.tv_body_horz);
            iv_board = content.findViewById(R.id.iv_board_horz);
        } else {
            // use vert
            tv_body = content.findViewById(R.id.tv_body_vert);
            iv_board = content.findViewById(R.id.iv_board_vert);
        }

        TextView title = content.findViewById(R.id.tv_title);
        ImageView lockedOverlay = content.findViewById(R.id.lockedOverlay);

        title.setText(q.getDisplayName());
        tv_body.setText(q.getDescription());
        if (bm != null)
            iv_board.setImageBitmap(bm);

        if (playable.contains(q)) {
            lockedOverlay.setVisibility(View.INVISIBLE);
            content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    activity.showChooseGameModeDialog(q);
                }
            });
        } else {
            lockedOverlay.setVisibility(View.VISIBLE);
            content.setOnClickListener(null);
        }
        container.addView(content);
        return content;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
