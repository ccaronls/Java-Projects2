package cc.android.game.robots;

import android.content.ClipData;
import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * Created by chriscaron on 12/12/17.
 */

public class ProbotListView extends ListView implements View.OnDragListener, View.OnClickListener, Runnable {

    BaseAdapter adapter = null;
    int programLineNum = -1;
    Probot probot = null;

    private void init(Context c, AttributeSet a) {
        setOnDragListener(this);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return probot.program.size();
            }

            @Override
            public Object getItem(int position) {
                return probot.program.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(getContext(), R.layout.item_command, null);
                }
                final Probot.Command cmd = probot.program.get(position);
                boolean isInLoop = false;
                boolean isLoop = cmd.type == Probot.CommandType.LoopEnd || cmd.type == Probot.CommandType.LoopStart;
                if (!isLoop) {
                    for (int i = position - 1; i >= 0; i--) {
                        Probot.CommandType type = probot.program.get(i).type;
                        if (type == Probot.CommandType.LoopStart) {
                            isInLoop = true;
                            break;
                        } else if (type == Probot.CommandType.LoopEnd) {
                            break;
                        }
                    }
                }

                if (isInLoop) {
                    convertView.setPadding(100, 0, 0, 0);
                } else {
                    convertView.setPadding(0, 0, 0, 0);
                }

                TextView tvLineNum = (TextView) convertView.findViewById(R.id.tvLineNum);
                tvLineNum.setText(String.valueOf(position + 1) + ":");
                tvLineNum.setVisibility(View.GONE);

                View v = convertView.findViewById(R.id.ibDelete);
                if (cmd.type == Probot.CommandType.LoopEnd) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setTag(position);
                    v.setOnClickListener(ProbotListView.this);
                }

                v = convertView.findViewById(R.id.ibLoop);
                if (isLoop || isInLoop) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setTag(position);
                    v.setVisibility(View.VISIBLE);
                    v.setOnClickListener(ProbotListView.this);
                }

                ImageView iv = (ImageView) convertView.findViewById(R.id.imageView);
                if (isLoop) {
                    iv.setVisibility(View.GONE);
                } else {
                    iv.setVisibility(View.VISIBLE);
                    switch (cmd.type) {

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
                }

                iv = (ImageView) convertView.findViewById(R.id.ivPlay);
                iv.setVisibility(position == programLineNum ? View.VISIBLE : View.INVISIBLE);

                View bPlus = convertView.findViewById(R.id.bPlus);
                View bMinus = convertView.findViewById(R.id.bMinus);
                TextView tvCount = (TextView) convertView.findViewById(R.id.tvCount);

                if (cmd.type == Probot.CommandType.LoopStart) {
                    bPlus.setVisibility(View.VISIBLE);
                    bPlus.setOnClickListener(ProbotListView.this);
                    bPlus.setTag(cmd);
                    bMinus.setVisibility(View.VISIBLE);
                    bMinus.setOnClickListener(ProbotListView.this);
                    bMinus.setTag(cmd);
                    tvCount.setVisibility(View.VISIBLE);
                    tvCount.setText(String.valueOf(cmd.count));
                } else {
                    bPlus.setVisibility(View.GONE);
                    bMinus.setVisibility(View.GONE);
                    tvCount.setVisibility(View.GONE);
                }

                return convertView;
            }
        };
    }

    public void setProbot(Probot p) {
        this.probot = p;
        setAdapter(adapter);
    }

    public ProbotListView(Context context) {
        super(context);
        init(context, null);
    }

    public ProbotListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProbotListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DROP: {
                // TODO: Use event x/y to insert into correct position
                Probot.Command cmd = (Probot.Command) event.getLocalState();
                probot.program.add(cmd);
                adapter.notifyDataSetChanged();
                setSelection(probot.program.size() - 1);
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

    public void setProgramLineNum(int lineNum) {
        this.programLineNum = lineNum;
        post(this);
    }

    public void notifyDataSetChanged() {
        post(this);
    }

    @Override
    public void run() {
        adapter.notifyDataSetChanged();
        if (programLineNum >= 0) {
            if (programLineNum < getFirstVisiblePosition() || programLineNum > getLastVisiblePosition()) {
                setSelection(programLineNum);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibLoop: {
                int position = (Integer) v.getTag();
                probot.program.add(position, new Probot.Command(Probot.CommandType.LoopStart, 1));
                probot.program.add(position + 2, new Probot.Command(Probot.CommandType.LoopEnd, 0));
                break;
            }
            case R.id.ibDelete: {
                int position = (Integer) v.getTag();
                Probot.Command cmd = probot.program.remove(position);
                if (cmd.type == Probot.CommandType.LoopStart) {
                    probot.program.remove(position + 1);
                } else if (cmd.type == Probot.CommandType.LoopEnd) {
                    probot.program.remove(position - 2);
                }
                break;
            }

            case R.id.bPlus: {
                Probot.Command cmd = (Probot.Command) v.getTag();
                if (cmd.count < 5)
                    cmd.count++;
                break;
            }
            case R.id.bMinus: {
                Probot.Command cmd = (Probot.Command) v.getTag();
                if (cmd.count > 1)
                    cmd.count--;
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }
}