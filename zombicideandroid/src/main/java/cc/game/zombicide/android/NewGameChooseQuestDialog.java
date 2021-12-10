package cc.game.zombicide.android;

import android.app.Dialog;
import android.content.DialogInterface;
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
import cc.lib.game.Utils;
import cc.lib.zombicide.ZQuests;

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
        dialog = activity.newDialogBuilder().setTitle("Choose Quest")
                .setView(view).setPositiveButton("Start", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ZQuests q = allQuests.get(pager.getCurrentItem());
                        if (playable.contains(q)) {
                            activity.showNewGameDailogChooseDifficulty(q);
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
        TextView title = content.findViewById(R.id.tv_title);
        TextView body = content.findViewById(R.id.tv_body);
        ImageView lockedOverlay = content.findViewById(R.id.lockedOverlay);

        title.setText(q.getDisplayName());
        body.setText(q.getDescription());

        if (playable.contains(q)) {
            lockedOverlay.setVisibility(View.INVISIBLE);
            content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    activity.showNewGameDailogChooseDifficulty(q);
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
