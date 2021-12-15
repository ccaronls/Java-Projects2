package cc.game.zombicide.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import cc.game.zombicide.android.databinding.AssignDialogItemBinding;
import cc.game.zombicide.android.databinding.AssignDialogP2pBinding;
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
        AssignDialogP2pBinding ab = AssignDialogP2pBinding.inflate(activity.getLayoutInflater());
        ab.bStart.setOnClickListener(this);
        ab.bDisconnect.setOnClickListener(this);
        ab.recyclerView.setAdapter(this);

        updateSelected();
        dialog = activity.newDialogBuilder().setTitle("ASSIGN")
                .setView(ab.getRoot()).show();
    }

    protected abstract void onAssigneeChecked(Assignee a, boolean checked);

    protected abstract void onStart();

    protected void onDisconnect() {
        activity.p2pShutdown();
        dialog.dismiss();

    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        AssignDialogItemBinding ib = AssignDialogItemBinding.inflate(LayoutInflater.from(activity), parent, false);
        return new Holder(ib, this);
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

        onAssigneeChecked(a, !a.checked);
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
        final AssignDialogItemBinding ib;

        void bind(Assignee a, CharacterChooserDialog cl) {
            this.assignee = a;

            ib.tvP2PName.setText(a.userName);
            if (a.color >= 0) {
                ib.tvP2PName.setTextColor(ZUser.USER_COLORS[a.color].toARGB());
                ib.tvP2PName.setText(a.userName);
            } else {
                ib.tvP2PName.setTextColor(Color.WHITE);
                ib.tvP2PName.setText(R.string.p2p_name_unassigned);
            }
            ib.checkbox.setChecked(a.checked);
            ib.checkbox.setClickable(false);
            ib.image.setOnLongClickListener(cl);
            if (!a.isUnlocked()) {
                ib.lockedOverlay.setVisibility(View.VISIBLE);
                ib.tvLockedReason.setVisibility(View.VISIBLE);
                ib.tvLockedReason.setText(a.lock.unlockMessageId);
                ib.checkbox.setEnabled(false);
                ib.image.setOnClickListener(null);
            } else {
                ib.lockedOverlay.setVisibility(View.INVISIBLE);
                ib.checkbox.setEnabled(true);
                ib.tvLockedReason.setVisibility(View.GONE);
                ib.image.setOnClickListener(cl);
            }

            ib.image.setImageResource(a.name.cardImageId);
            ib.image.setTag(assignee);
        }

        public Holder(AssignDialogItemBinding ib, View.OnClickListener listener) {
            super(ib.getRoot());
            this.ib = ib;
        }
    }

}
