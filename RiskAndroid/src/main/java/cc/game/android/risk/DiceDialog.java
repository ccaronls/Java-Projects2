package cc.game.android.risk;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import cc.lib.game.Utils;
import cc.lib.risk.Army;
import cc.lib.utils.Lock;

/**
 * Created by Chris Caron on 9/15/21.
 */
class DiceDialog {

    final RiskActivity context;
    final Army attacker, defender;
    final int [] attackingDice, defendingDice;
    final boolean [] result;
    final Lock lock;
    final Dialog dialog;
    final TextView [] text;

    public DiceDialog(Lock lock, RiskActivity context, Army attacker, Army defender, int[] attackingDice, int[] defendingDice, boolean[] result) {
        this.lock = lock;
        this.context = context;
        this.attacker = attacker;
        this.defender = defender;
        this.attackingDice = attackingDice;
        this.defendingDice = defendingDice;
        this.result = result;
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

        dialog = context.newDialogBuilder().setView(view).show();
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
