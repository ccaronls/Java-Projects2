package cc.android.test.dice;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import cc.android.test.R;

/**
 * Created by Chris Caron on 8/20/21.
 */
public class DiceActivity extends Activity implements View.OnClickListener {

    ListView lv;

    static class DiceEntry {
        int curColor = 0;
        int dieNum = 6;
        int maxDieNums = 0;
        long delay = 0;
        boolean rolling = false;
    }

    List<DiceEntry> entries = new ArrayList() {{
        add(new DiceEntry());
    }};

    class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return entries.size();
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
            dv.setEntry(entries.get(position));

            view.findViewById(R.id.bColor).setOnClickListener(v -> dv.toggleColor());

            view.findViewById(R.id.bPipsAdd).setOnClickListener(v -> dv.addPips());

            view.findViewById(R.id.bPipsMinus).setOnClickListener(v -> dv.removePips());

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
                if (entries.size() < 20) {
                    entries.add(new DiceEntry());
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.bRemoveDice:
                if (entries.size() > 1) {
                    entries.remove(entries.size()-1);
                    adapter.notifyDataSetChanged();
                }
                break;
        }
    }
}
