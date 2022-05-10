package cc.game.zombicide.android;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cc.game.zombicide.android.databinding.SaveGameDialogBinding;
import cc.lib.game.Utils;
import cc.lib.utils.Pair;

/**
 * Created by Chris Caron on 8/17/21.
 */
public class SaveGameDialog extends BaseAdapter implements View.OnClickListener {

    final ZombicideActivity activity;
    final int maxSaves;
    final List<Pair<String,String>> list = new ArrayList<>();
    final Dialog dialog;
    SaveGameDialogBinding sb;

    void updateSaves() {
        list.clear();
        list.addAll(Utils.toList(activity.getSaves()));
        sb.bSave.setEnabled(list.size() < maxSaves);
        while (list.size() < maxSaves) {
            list.add(new Pair("EMPTY", null));
        }
    }

    public SaveGameDialog(ZombicideActivity activity, int maxSaves) {
        this.activity = activity;
        this.maxSaves = maxSaves;
        SaveGameDialogBinding sb = SaveGameDialogBinding.inflate(activity.getLayoutInflater());
        sb.bSave.setOnClickListener(this);
        sb.bCancel.setOnClickListener(this);
        updateSaves();

        sb.listView.setScrollContainer(false);
        sb.listView.setAdapter(this);


        dialog = activity.newDialogBuilder().setTitle("Save Game")
                .setView(sb.getRoot()).show();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null ? convertView : View.inflate(activity, R.layout.save_game_dialog_item, null);

        TextView tv_text = view.findViewById(R.id.tv_text);
        View b_delete = view.findViewById(R.id.b_delete);

        Pair<String,String> item = list.get(position);

        b_delete.setTag(position);
        b_delete.setOnClickListener(this);

        b_delete.setVisibility(item.second == null ? View.GONE : View.VISIBLE);
        tv_text.setText(item.first);

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_delete: {
                int position = (Integer)v.getTag();
                String name = list.get(position).first;
                activity.deleteSave(name);
                updateSaves();
                notifyDataSetChanged();
                sb.bSave.setEnabled(true);
                break;
            }
            case R.id.b_save: {
                activity.saveGame();
                dialog.dismiss();
            }
            case R.id.b_cancel:
                dialog.dismiss();
                break;
        }
    }
}
