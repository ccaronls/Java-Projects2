package cc.game.zombicide.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
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
 * Created by Chris Caron on 7/21/21.
 */
public abstract class CharacterChooserDialog extends RecyclerView.Adapter<CharacterChooserDialog.Holder> implements View.OnClickListener, View.OnLongClickListener, Runnable {

    final static String TAG = CharacterChooserDialog.class.getSimpleName();

    final List<Assignee> selectedPlayers;
    final ZombicideActivity activity;
    final int maxPlayers;
    int numSelected = 0;
    final Dialog dialog;

    CharacterChooserDialog(ZombicideActivity activity, List<Assignee> selectedPlayers, int maxPlayers) {
        this.activity = activity;
        this.selectedPlayers = selectedPlayers;
        this.maxPlayers = maxPlayers;
        View view = View.inflate(activity, R.layout.assign_dialog_p2p, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        view.findViewById(R.id.bStart).setOnClickListener(this);
        view.findViewById(R.id.bDisconnect).setOnClickListener(this);
        recyclerView.setAdapter(this);
//        recyclerView.setLayoutManager(new GridLayoutManager(activity, 2, LinearLayoutManager.VERTICAL, false));

        updateSelected();
        dialog = activity.newDialogBuilder().setTitle("ASSIGN")
                .setView(view).show();
    }

    protected abstract void onAssigneeChecked(Assignee a);

    protected abstract void onStart();

    protected void onDisconnect() {
        activity.p2pShutdown();
        dialog.dismiss();

    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.assign_dialog_item, parent, false), this);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        Assignee a = selectedPlayers.get(position);
        holder.bind(a, this);
    }

    @Override
    public int getItemCount() {
        return selectedPlayers.size();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bStart: {
                if (numSelected <= 0) {
                    Toast.makeText(activity, "Please select at least one character", Toast.LENGTH_LONG).show();
                } else {
                    dialog.dismiss();
                    onStart();
                }
                return;
            }
            case R.id.bDisconnect: {
                if (activity.isP2PConnected()) {
                    activity.newDialogBuilder().setTitle("Confirm")
                            .setMessage("Are you sure you want to cancel P2P game?")
                            .setNegativeButton(R.string.popup_button_no, null)
                            .setPositiveButton(R.string.popup_button_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onDisconnect();
                                }
                            }).show();
                } else {
                    dialog.dismiss();
                }
                return;
            }
        }

        Assignee a = (Assignee)v.getTag();
        if (a == null)
            return;

        if (!a.checked && numSelected >= maxPlayers) {
            Toast.makeText(activity, "Can only have " + maxPlayers + " at a time", Toast.LENGTH_LONG).show();
            return;
        }

        a.checked = !a.checked;
        onAssigneeChecked(a);
        updateSelected();
        notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getTag() != null && v.getTag() instanceof Assignee) {
            Assignee a = (Assignee)v.getTag();
            ImageView iv = new ImageView(activity);
            iv.setImageResource(a.name.cardImageId);
            activity.newDialogBuilder().setTitle(a.name.getLabel())
                    .setView(iv).setNegativeButton(R.string.popup_button_cancel, null).show();
        }
        return true;
    }

    public void run() {
        notifyDataSetChanged();
    }

    public synchronized void postNotifyUpdateAssignee(Assignee a) {
        int idx = selectedPlayers.indexOf(a);
        if (idx >= 0) {
            Assignee c = selectedPlayers.get(idx);
            c.copyFrom(a);
            c.isAssingedToMe = a.isAssingedToMe;
        }
        activity.runOnUiThread(this);
    }

    private void updateSelected() {
        numSelected = 0;
        for (Assignee aa : selectedPlayers) {
            if (aa.isAssingedToMe)
                numSelected++;
        }
    }

    public static class Holder extends RecyclerView.ViewHolder {

        Assignee assignee;
        final ImageView image;
        final CheckBox checkbox;
        final View lockedOverlay;
        final TextView lockedReason;
        final TextView assignedPlayer; // MP Only

        void bind(Assignee a, CharacterChooserDialog cl) {
            this.assignee = a;

            assignedPlayer.setText(a.userName);
            if (a.color >= 0) {
                assignedPlayer.setTextColor(ZUser.USER_COLORS[a.color].toARGB());
                assignedPlayer.setText(a.userName);
            } else {
                assignedPlayer.setTextColor(Color.WHITE);
                assignedPlayer.setText(R.string.p2p_name_unassigned);
            }
            checkbox.setChecked(a.checked);
            checkbox.setClickable(false);
            image.setOnLongClickListener(cl);
            if (!a.isUnlocked()) {
                lockedOverlay.setVisibility(View.VISIBLE);
                lockedReason.setVisibility(View.VISIBLE);
                lockedReason.setText(a.lock.unlockMessage);
                checkbox.setEnabled(false);
                image.setOnClickListener(null);
            } else {
                lockedOverlay.setVisibility(View.INVISIBLE);
                checkbox.setEnabled(true);
                lockedReason.setVisibility(View.GONE);
                image.setOnClickListener(cl);
            }

            image.setImageResource(a.name.cardImageId);
            image.setTag(assignee);
        }

        public Holder(View itemView, View.OnClickListener listener) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            image.setOnClickListener(listener);
            checkbox = itemView.findViewById(R.id.checkbox);
            checkbox.setClickable(false);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
            lockedReason = itemView.findViewById(R.id.tvLockedReason);
            assignedPlayer = itemView.findViewById(R.id.tvP2PName);
        }
    }

}
