package cc.game.android.risk;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;

import cc.lib.game.Utils;
import cc.lib.risk.Army;
import cc.lib.utils.Lock;

/**
 * Created by Chris Caron on 9/15/21.
 */
class DiceDialog implements Runnable, DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private final RiskActivity context;
    private final Army attacker, defender;
    private final int [] attackingDice, defendingDice;
    private final boolean [] result;
    private final Lock lock;
    private Dialog dialog;
    private TextView [] text;

    public DiceDialog(RiskActivity context, Army attacker, Army defender, int[] attackingDice, int[] defendingDice, boolean[] result) {
        this.lock = new Lock();
        this.context = context;
        this.attacker = attacker;
        this.defender = defender;
        this.attackingDice = attackingDice;
        this.defendingDice = defendingDice;
        this.result = result;
        context.runOnUiThread(this);
        Utils.waitNoThrow(this, 2000);
        if (!dialog.isShowing())
            return;
        lock.block();
        if (!dialog.isShowing())
            return;
        context.runOnUiThread(() -> {
            showResult();
        });
        if (dialog.isShowing()) {
            dialog.setCanceledOnTouchOutside(true);
            Utils.waitNoThrow(this, 6000);
        }
        context.runOnUiThread(() -> dialog.dismiss());
    }

    public void run() {
        View view = View.inflate(context, R.layout.dice_dialog, null);

        TextView tv_attacker = view.findViewById(R.id.tv_attacker);
        TextView tv_defender = view.findViewById(R.id.tv_defender);

        tv_attacker.setText(Utils.toPrettyString(attacker));
        tv_defender.setText(Utils.toPrettyString(defender));

        DiceView [] red = {
                view.findViewById(R.id.red1),
                view.findViewById(R.id.red2),
                view.findViewById(R.id.red3)
        };

        DiceView [] white = {
                view.findViewById(R.id.white1),
                view.findViewById(R.id.white2)
        };

        for (int i=0; i<red.length; i++) {
            if (i >= attackingDice.length) {
                red[i].setVisibility(View.INVISIBLE);
            } else {
                red[i].rollDice(attackingDice[i], lock);
            }
        }

        for (int i=0; i<white.length; i++) {
            if (i >= defendingDice.length)
                white[i].setVisibility(View.INVISIBLE);
            else
                white[i].rollDice(defendingDice[i], lock);
        }

        text = new TextView[] {
                view.findViewById(R.id.text1),
                view.findViewById(R.id.text2),
        };

        dialog = context.newDialogBuilder().setView(view)
                .setNegativeButton("Pause", this).show();
        dialog.setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        lock.releaseAll();
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        context.stopGameThread();
    }

    void dismiss() {
        dialog.dismiss();
    }

    void showResult() {
        for (int i=0; i<result.length; i++) {
            text[i].setText(result[i] ? R.string.arrow_left : R.string.arrow_right);
        }
    }
}
