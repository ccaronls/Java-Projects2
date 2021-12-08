package cc.game.android.risk;

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import cc.lib.game.Utils;
import cc.lib.risk.Army;
import cc.lib.risk.RiskPlayer;

/**
 * Created by Chris Caron on 9/17/21.
 */
public class PlayerChooserDialog extends BaseAdapter implements DialogInterface.OnClickListener {

    final RiskActivity context;

    static class PL {
        boolean checked;
        boolean robot;
        Army army;

        PL(Army army) {
            this.army = army;
        }
    }

    List<PL> players = new ArrayList<>();

    PlayerChooserDialog(RiskActivity context) {
        this.context = context;
        for (Army army : Army.values()) {
            if (army != Army.NEUTRAL)
                players.add(new PL(army));
        }
        ListView lv = new ListView(context);
        lv.setAdapter(this);
        context.newDialogBuilder().setTitle("Choose Players")
                .setView(lv)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_ok, this).show();
    }

    @Override
    public int getCount() {
        return players.size();
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
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = View.inflate(context, R.layout.player_list_item, null);
        }

        CheckBox cb = view.findViewById(R.id.check_box);
        ToggleButton tb = view.findViewById(R.id.toggle_button);

        cb.setOnCheckedChangeListener(null);
        tb.setOnCheckedChangeListener(null);

        PL pl = players.get(position);
        cb.setText(pl.army.name());
        cb.setChecked(pl.checked);
        tb.setChecked(pl.robot);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> pl.checked = !pl.checked);
        tb.setOnCheckedChangeListener((buttonView, isChecked) -> pl.robot = !pl.robot);

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (Utils.count(players, pl->pl.checked) < 2) {
            Toast.makeText(context, "Not enough players", Toast.LENGTH_LONG).show();
            return;
        }

        List<RiskPlayer> p = new ArrayList<>();
        for (PL pl : players) {
            if (pl.checked) {
                if (pl.robot) {
                    p.add(new RiskPlayer(pl.army));
                } else {
                    p.add(new UIRiskPlayer(pl.army));
                }
            }
        }

        context.startGame(p);
    }
}
