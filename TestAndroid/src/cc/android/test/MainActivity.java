package cc.android.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import cc.android.test.checkerboard.ChessTestActivity;
import cc.android.test.circle.CircleActivity;
import cc.android.test.dice.DiceActivity;
import cc.android.test.lightning.LightningTestActivity;
import cc.android.test.p2p.WifiTest2;
import cc.android.test.strbounds.StringBoundsActivity;
import cc.lib.utils.Pair;

/**
 * Created by Chris Caron on 3/23/22.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ListView lv = findViewById(R.id.listview);

        List<Pair<String, Intent>> list = new ArrayList<Pair<String,Intent>>() {{
            add(new Pair("Dice", new Intent(MainActivity.this, DiceActivity.class)));
            add(new Pair("Lightning", new Intent(MainActivity.this, LightningTestActivity.class)));
            add(new Pair("Circle", new Intent(MainActivity.this, CircleActivity.class)));
            add(new Pair("Chess", new Intent(MainActivity.this, ChessTestActivity.class)));
            add(new Pair("WifiTest2", new Intent(MainActivity.this, WifiTest2.class)));
            add(new Pair("StringBoundsActivity", new Intent(MainActivity.this, StringBoundsActivity.class)));
        }};

        lv.setAdapter(new BaseAdapter() {
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
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    view = new Button(MainActivity.this);
                }

                Pair<String,Intent> pair = list.get(position);
                Button b = (Button)view;
                b.setText(pair.first);
                b.setOnClickListener(v -> startActivity(pair.second));

                return view;
            }
        });
    }
}
