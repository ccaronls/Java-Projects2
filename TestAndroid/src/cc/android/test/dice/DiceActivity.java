package cc.android.test.dice;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import cc.android.test.R;

/**
 * Created by Chris Caron on 8/20/21.
 */
public class DiceActivity extends Activity implements View.OnClickListener {

    ListView lv;
    int numDice = 1;

    class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return numDice;
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
            View view = convertView == null ? View.inflate(DiceActivity.this, R.layout.dice_list_item, null) : convertView;

            DiceView dv = view.findViewById(R.id.diceView);
            view.findViewById(R.id.bRoll).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dv.rollDice();
                }
            });

            view.findViewById(R.id.bColor).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dv.toggleColor();
                }
            });

            return view;
        }
    }

    Adapter adapter = new Adapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dice_activity);
        lv = findViewById(R.id.diceList);
        findViewById(R.id.bAddDice).setOnClickListener(this);
        findViewById(R.id.bRemoveDice).setOnClickListener(this);
        lv.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bAddDice:
                if (numDice < 20) {
                    numDice++;
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.bRemoveDice:
                if (numDice > 1) {
                    numDice--;
                    adapter.notifyDataSetChanged();
                }
                break;
        }
    }
}
