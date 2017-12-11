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

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    ProbotView pv;
    ListView lv;
    View actionForward;
    View actionRight;
    View actionLeft;
    BaseAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.probotview);
        pv = (ProbotView)findViewById(R.id.probotView);
        lv = (ListView)findViewById(R.id.lvProgram);
        actionForward = findViewById(R.id.ivArrowForward);
        actionRight = findViewById(R.id.ivArrowRight);
        actionLeft = findViewById(R.id.ivArrowLeft);

        actionForward.setTag(Probot.Command.Advance);
        actionRight.setTag(Probot.Command.TurnRight);
        actionLeft.setTag(Probot.Command.TurnLeft);

        findViewById(R.id.ibPlay).setOnClickListener(this);
        findViewById(R.id.ibClear).setOnClickListener(this);

        actionForward.setOnTouchListener(this);
        actionRight.setOnTouchListener(this);
        actionLeft.setOnTouchListener(this);

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return pv.probot.program.size();
            }

            @Override
            public Object getItem(int position) {
                return pv.probot.program.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(ProbotActivity.this, R.layout.item_command, null);
                }
                TextView tvLineNum = (TextView)convertView.findViewById(R.id.tvLineNum);
                tvLineNum.setText(String.valueOf(position+1) + ":");

                convertView.findViewById(R.id.ibDelete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pv.probot.program.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });

                ImageView iv = (ImageView)convertView.findViewById(R.id.imageView);
                switch (pv.probot.program.get(position)) {

                    case Advance:
                        iv.setImageResource(R.drawable.arrow_forward);
                        break;
                    case TurnRight:
                        iv.setImageResource(R.drawable.arrow_right);
                        break;
                    case TurnLeft:
                        iv.setImageResource(R.drawable.arrow_left);
                        break;
                }
                return convertView;
            }
        };

        lv.setAdapter(adapter);

        lv.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DROP: {
                        // TODO: Use event x/y to insert into correct position
                        Probot.Command cmd = (Probot.Command) event.getLocalState();
                        pv.probot.program.add(cmd);
                        adapter.notifyDataSetChanged();
                    }
                    //case DragEvent.ACTION_DRAG_STARTED:
                    //case DragEvent.ACTION_DRAG_ENDED:
                    case DragEvent.ACTION_DRAG_EXITED:
                        // drag has moved outside, so remove the entery we added
                    case DragEvent.ACTION_DRAG_ENTERED:
                        // drag has moved inside, so position the item at the bottom
                    case DragEvent.ACTION_DRAG_STARTED:
                    case DragEvent.ACTION_DRAG_LOCATION:
                        // drag has moved within, so update the items position in the list
                        return true;

                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Probot.Command cmd = (Probot.Command) v.getTag();
        v.startDrag(ClipData.newPlainText(cmd.name(), cmd.name()), new View.DragShadowBuilder(v), cmd, 0);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibClear:
                pv.probot.program.clear();
                adapter.notifyDataSetChanged();
                break;
            case R.id.ibPlay:
                new Thread() {
                    public void run() {
                        pv.probot.runProgram();
                    }
                }.start();
                break;
        }
    }
}
