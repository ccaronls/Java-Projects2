package cc.game.zombicide.android;

import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import cc.lib.zombicide.ZUser;

/**
 * Created by Chris Caron on 7/26/21.
 *
 * Use CharacterChooserDialog
 */
@Deprecated
abstract class PlayerChooserDialog extends PagerAdapter implements View.OnClickListener, Runnable {

    final List<Assignee> selectedPlayers;
    final ZombicideActivity activity;
    final int maxPlayers;
    int numSelected = 0;

    PlayerChooserDialog(ZombicideActivity activity, List<Assignee> selectedPlayers, int maxPlayers) {
        this.activity = activity;
        this.maxPlayers = maxPlayers;
        this.selectedPlayers = selectedPlayers;
        View view = View.inflate(activity, R.layout.viewpager_dialog, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        pager.setAdapter(this);
        activity.newDialogBuilder().setTitle("Choose Players")
                .setView(view)
                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.p2pShutdown();
                    }
                }).setPositiveButton(R.string.popup_button_start, null).show();

    }

    @Override
    public final int getCount() {
        return selectedPlayers.size();
    }

    @Override
    public final boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public final synchronized Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(activity).inflate(R.layout.assign_dialog_item, null);
        Assignee assignee = selectedPlayers.get(position);
        update(view, assignee);
        container.addView(view);
        view.setTag(assignee);
        return view;
    }

    public void update(View view, Assignee a) {
        ImageView image = view.findViewById(R.id.image);
        CheckBox checkbox = view.findViewById(R.id.checkbox);
        ViewGroup lockedOverlay = view.findViewById(R.id.lockedOverlay);
        TextView tvHowToUnlock = view.findViewById(R.id.tvLockedReason);
        TextView tvP2PName = view.findViewById(R.id.tvP2PName);

        tvP2PName.setText(a.userName);
        if (a.color >= 0) {
            tvP2PName.setTextColor(ZUser.USER_COLORS[a.color].toARGB());
            tvP2PName.setText(a.userName);
        } else {
            tvP2PName.setTextColor(Color.WHITE);
            tvP2PName.setText(R.string.p2p_name_unassigned);
        }
        checkbox.setChecked(a.checked);
        checkbox.setClickable(false);
        if (!a.isUnlocked()) {
            lockedOverlay.setVisibility(View.VISIBLE);
            tvHowToUnlock.setVisibility(View.VISIBLE);
            tvHowToUnlock.setText(a.lock.unlockMessage);
            checkbox.setEnabled(false);
            view.setOnClickListener(null);
        } else {
            lockedOverlay.setVisibility(View.INVISIBLE);
            checkbox.setEnabled(true);
            tvHowToUnlock.setVisibility(View.GONE);
            view.setOnClickListener(this);
        }

        image.setImageResource(a.name.cardImageId);

    }

    protected abstract void onAssigneeChecked(Assignee assignee);

    @Override
    public final void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View)object);
    }

    @Override
    public synchronized final void onClick(View v) {
        Assignee a = (Assignee)v.getTag();
        if (a == null)
            return;

        if (!a.checked && numSelected >= maxPlayers) {
            Toast.makeText(activity, "Can only have " + maxPlayers + " at a time", Toast.LENGTH_LONG).show();
            return;
        }

        a.checked = !a.checked;
        onAssigneeChecked(a);
    }

    public void run() {
        notifyDataSetChanged();
    }

    public synchronized void postNotifiyUpdateAssignee(Assignee a) {
        int idx = selectedPlayers.indexOf(a);
        if (idx >= 0) {
            Assignee c = selectedPlayers.get(idx);
            c.copyFrom(a);
            c.isAssingedToMe = a.isAssingedToMe;
        }
        activity.runOnUiThread(this);
    }

}
