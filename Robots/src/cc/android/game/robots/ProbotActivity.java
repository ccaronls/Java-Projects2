package cc.android.game.robots;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import cc.lib.android.CCActivityBase;

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotActivity extends CCActivityBase implements View.OnClickListener, View.OnTouchListener {

    ProbotView pv;
    ProbotListView lv;
    View actionForward;
    View actionRight;
    View actionLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.probotview);
        pv = (ProbotView)findViewById(R.id.probotView);
        lv = (ProbotListView)findViewById(R.id.lvProgram);
        lv.setProbot(pv.probot);
        actionForward = findViewById(R.id.ivArrowForward);
        actionRight = findViewById(R.id.ivArrowRight);
        actionLeft = findViewById(R.id.ivArrowLeft);

        actionForward.setTag(Probot.CommandType.Advance);
        actionRight.setTag(Probot.CommandType.TurnRight);
        actionLeft.setTag(Probot.CommandType.TurnLeft);

        findViewById(R.id.ibPlay).setOnClickListener(this);
        findViewById(R.id.ibClear).setOnClickListener(this);

        actionForward.setOnTouchListener(this);
        actionRight.setOnTouchListener(this);
        actionLeft.setOnTouchListener(this);

        int level = getPrefs().getInt("Level", 0);

        pv.setLevel(level);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Probot.CommandType type = (Probot.CommandType) v.getTag();
        Probot.Command cmd = new Probot.Command(type, 0);
        v.startDrag(ClipData.newPlainText(cmd.type.name(), cmd.type.name()), new View.DragShadowBuilder(v), cmd, 0);
        return true;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.ibClear:
                pv.probot.program.clear();
                lv.notifyDataSetChanged();
                break;
            case R.id.ibPlay:
                if (pv.probot.program.size() > 0) {
                    v.setEnabled(false);
                    new Thread() {
                        public void run() {
                            pv.probot.runProgram();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    v.setEnabled(true);
                                }
                            });
                        }
                    }.start();
                }
                break;
        }
    }
}
