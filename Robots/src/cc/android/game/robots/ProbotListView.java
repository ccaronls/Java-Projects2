package cc.android.game.robots;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import cc.lib.game.Utils;

/**
 * Created by chriscaron on 12/12/17.
 */

public class ProbotListView extends ListView implements View.OnDragListener, View.OnClickListener, View.OnLongClickListener, Runnable {

    BaseAdapter adapter = null;
    int programLineNum = -1;
    int failedLineNum = -1;
    Probot probot = null;

    private void init(Context c, AttributeSet a) {
        setOnDragListener(this);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return probot.size();
            }

            @Override
            public Object getItem(int position) {
                return probot.get(position);
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
                convertView.setOnDragListener(ProbotListView.this);
                convertView.setOnLongClickListener(ProbotListView.this);
                convertView.setOnClickListener(ProbotListView.this);
                convertView.setTag(position);

                View divTop = convertView.findViewById(R.id.divider_top);
                View divBot = convertView.findViewById(R.id.divider_bottom);

                divTop.setVisibility(View.GONE);
                divBot.setVisibility(View.GONE);

                if (dragInsertPos >= 0) {
                    if (dragInsertPos == position) {
                        divTop.setVisibility(View.VISIBLE);
                    } else if (dragInsertPos == position+1) {
                        divBot.setVisibility(View.VISIBLE);
                    }
                }

                final Probot.Command cmd = probot.get(position);
                boolean isInLoop = cmd.nesting > 0;
                boolean isLoop = cmd.type == Probot.CommandType.LoopEnd || cmd.type == Probot.CommandType.LoopStart;

                View vIndent = convertView.findViewById(R.id.vIndent);
                float indent = getResources().getDimension(R.dimen.item_command_indent_width);
                ViewGroup.LayoutParams lp = vIndent.getLayoutParams();
                lp.width = Math.round(indent * cmd.nesting);
//                vIndent.setLayoutParams(Math.round(indent * cmd.nesting), 0, 0, 0);
                vIndent.setLayoutParams(lp);


                TextView tvLineNum = (TextView) convertView.findViewById(R.id.tvLineNum);
                tvLineNum.setText(String.valueOf(position + 1));
                if (position == programLineNum) {
                    tvLineNum.setBackgroundColor(Color.GREEN);
                } else if (position == failedLineNum) {
                    tvLineNum.setBackgroundColor(Color.RED);
                } else {
                    tvLineNum.setBackgroundColor(Color.TRANSPARENT);
                }
                //tvLineNum.setVisibility(View.GONE);

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
                    iv.setVisibility(View.INVISIBLE);
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
                        case Jump:
                            iv.setImageResource(R.drawable.arrow_jump);
                            break;
                    }
                }

                //iv = (ImageView) convertView.findViewById(R.id.ivPlay);
                //iv.setVisibility(position == programLineNum ? View.VISIBLE : View.INVISIBLE);

                View bPlus = convertView.findViewById(R.id.ibPlus);
                View bMinus = convertView.findViewById(R.id.ibMinus);
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

    int dragInsertPos = -1;
    int dragMinInsertPos = 0;
    int dragMaxInsertPos = 0;

    String getActionStr(int action) {
        switch (action) {
            case DragEvent.ACTION_DRAG_ENDED:
                return "Ended";
            case DragEvent.ACTION_DRAG_STARTED:
                return "Started";
            case DragEvent.ACTION_DRAG_ENTERED:
                return "Entered";
            case DragEvent.ACTION_DRAG_EXITED:
                return "Exited";
            case DragEvent.ACTION_DRAG_LOCATION:
                return "Location";
            case DragEvent.ACTION_DROP:
                return "Drop";
        }
        return "???";
    }

    private boolean dropped = false;

    @Override
    public boolean onDrag(View v, DragEvent event) {
        int dragInsertPosIn = dragInsertPos;
        String obj = null;
        Object [] state = (Object []) event.getLocalState();
        if (state == null)
            return false;
        Probot.Command cmd = (Probot.Command)state[0];
        int originatingLine = (Integer)state[1];
        if (v instanceof ProbotListView) {
            obj = "ListView";
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP: {
                    if (cmd != null) {
                        if (dragInsertPos < 0) {
                            probot.add(cmd);
                        } else {
                            probot.add(dragInsertPos, cmd);
                        }
                        adapter.notifyDataSetChanged();
                        setSelection(dragInsertPos);
                        dragInsertPos = -1;
                    }
                    dropped = true;
                    break;
                }
                //case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_ENTERED: {
                    if (probot.size() > 0) {
                        dragInsertPos = Utils.clamp(probot.size(), dragMinInsertPos, dragMaxInsertPos);
                        adapter.notifyDataSetChanged();
                    }
                    break;
                }
                case DragEvent.ACTION_DRAG_EXITED:
                    if (dragMinInsertPos < 0) {
                        dragInsertPos = -1;
                    } else if (originatingLine >= 0) {
                        dragInsertPos = originatingLine;
                    }
                    adapter.notifyDataSetChanged();
                    break;
                case DragEvent.ACTION_DRAG_STARTED:
                    dropped = false;
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    dragInsertPos = -1;
                    if (!dropped && originatingLine >= 0) {
                        probot.add(originatingLine, cmd);
                        adapter.notifyDataSetChanged();
                    }
                    break;
            }
        } else {
            int position = (Integer)v.getTag();
            obj = "RowItem[" + position + "]";
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP: {
                    if (dragInsertPos < 0) {
                        probot.add(cmd);
                    } else {
                        probot.add(dragInsertPos, cmd);
                    }
                    adapter.notifyDataSetChanged();
                    setSelection(dragInsertPos);
                    dragInsertPos = -1;
                    dropped = true;
                    break;
                }
                case DragEvent.ACTION_DRAG_EXITED:
                    // TODO: Do I need this?
                    //dragInsertPos = -1;
                    //adapter.notifyDataSetChanged();
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_ENTERED: {
                    float mp = //v.getY() +
                            v.getHeight()/2;
                    float mpd = event.getY();// + dragging.getHeight();
                    int first = getFirstVisiblePosition();
                    int last = getLastVisiblePosition();

                    if (position == first) {
                        Log.d("PLV", "A");
                        dragInsertPos = position;
                    } else if (position == last) {
                        Log.d("PLV", "B");
                        dragInsertPos = position+1;
                    } else if (mp > mpd) {
                        Log.d("PLV", "C mp=" + mp + " mpd=" + mpd);
                        dragInsertPos = position;
                    } else {
                        Log.d("PLV", "D");
                        dragInsertPos = position+1;
                    }
                    dragInsertPos = Utils.clamp(dragInsertPos, dragMinInsertPos, dragMaxInsertPos);
                    adapter.notifyDataSetChanged();
      //              setSelection(dragInsertPos);
                    if (dragInsertPosIn != dragInsertPos){
                        int scrollDist = v.getHeight();
                        int midpt =(last + first + 1) / 2;
                        float maxSp = 500f;
                        int speed = Math.round(maxSp - Math.abs(maxSp * 2 * (dragInsertPos-midpt)/((last-first))));
                                //0.5f * (dragInsertPos - first - midpt));
                        Log.d("PLV", "speed=" + speed);
                        if (dragInsertPos < midpt) {
                            smoothScrollBy(-scrollDist, speed);
                        } else {
                            smoothScrollBy(scrollDist, speed);
                        }
                    }

                    if (dragInsertPos == 0)
                        smoothScrollToPositionFromTop(0, 0, 0);

                    if (dragInsertPos <= first+1) {
                    //    smoothScrollBy(-scrollDist, 500);
                    } else if (dragInsertPos >= last-1) {
                      //  smoothScrollBy(scrollDist, 500);
                    }
        //            setSelection(dragInsertPos);
                    break;
                }
                case DragEvent.ACTION_DRAG_STARTED:

            }
        }
        //if (dragInsertPos != dragInsertPosIn)
        {
            Log.d("List", "v=" + obj + " action=" + getActionStr(event.getAction()) + " pos=" + dragInsertPos + " origLine: " + originatingLine);

        }
        return true;
    }

    public void setProgramLineNum(int lineNum) {
        this.programLineNum = lineNum;
        this.failedLineNum = -1;
        post(this);
    }

    public void markFailed() {
        this.failedLineNum = programLineNum;
        programLineNum = -1;
        post(this);
    }

    public void notifyDataSetChanged() {
        post(this);
    }

    @Override
    public void run() {
        adapter.notifyDataSetChanged();
        if (programLineNum >= 0) {
            setSelection(programLineNum);
        }
    }

    @Override
    public void onClick(View v) {
        if (programLineNum >= 0)
            return;
        switch (v.getId()) {
            case R.id.ibLoop: {
                int position = (Integer) v.getTag();
                probot.add(position, new Probot.Command(Probot.CommandType.LoopStart, 1));
                probot.add(position + 2, new Probot.Command(Probot.CommandType.LoopEnd, 0));
                break;
            }
            case R.id.ibDelete: {
                int position = (Integer) v.getTag();
                Probot.Command cmd = probot.remove(position);
                if (cmd.type == Probot.CommandType.LoopStart) {
                    while (probot.get(position).type != Probot.CommandType.LoopEnd) {
                        //probot.remove(position);
                        position++;
                    }
                    probot.remove(position);
                }
                break;
            }

            case R.id.ibPlus: {
                Probot.Command cmd = (Probot.Command) v.getTag();
                if (cmd.count < 5)
                    cmd.count++;
                break;
            }
            case R.id.ibMinus: {
                Probot.Command cmd = (Probot.Command) v.getTag();
                if (cmd.count > 1)
                    cmd.count--;
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View v) {
        if (probot.size() > 1) {
            int position = (Integer) v.getTag();
            Probot.Command cmd = probot.remove(position);
            startDrag(v, cmd, position);
            adapter.notifyDataSetChanged();
        }
        return true;
    }

    public final void startDrag(View v, Probot.Command cmd) {
        startDrag(v, cmd, -1);
    }

    private final void startDrag(View v, Probot.Command cmd, int originatingLine) {
        v.startDrag(ClipData.newPlainText(cmd.type.name(), cmd.type.name()), new View.DragShadowBuilder(v),
                new Object[] {
                    cmd, originatingLine
                }, 0);
        dragMinInsertPos = -1;
        dragMaxInsertPos = probot.size();
        if (cmd.type == Probot.CommandType.LoopStart) {
            final int position = (Integer)v.getTag();
            int pos = position;
            dragMaxInsertPos = dragMinInsertPos = pos;
            while (pos > 0) {
                if (probot.get(pos-1).type == Probot.CommandType.LoopEnd) {
                    break;
                }
                dragMinInsertPos--;
                pos--;
            }
            pos = position;
            while (pos < probot.size()-1) {
                if (probot.get(pos+1).type == Probot.CommandType.LoopEnd) {
                    break;
                }
                dragMaxInsertPos++;
                pos++;
            }
            Log.d("ProbotListView", "dragMin=" + dragMinInsertPos + " dragMax=" + dragMaxInsertPos + " pos=" + position);
        } else if (cmd.type == Probot.CommandType.LoopEnd) {
            final int position = (Integer)v.getTag();
            // scan forward and back to make sure
            int pos = position;
            dragMaxInsertPos = dragMinInsertPos = pos;
            while (pos > 1) {
                if (probot.get(pos-2).type == Probot.CommandType.LoopStart) {
                    break;
                }
                dragMinInsertPos--;
                pos--;
            }
            pos = position;
            while (pos < probot.size()) {
                if (probot.get(pos).type == Probot.CommandType.LoopStart) {
                    break;
                }
                dragMaxInsertPos++;
                pos++;
            }
            Log.d("ProbotListView", "insertPos=" + dragInsertPos + " dragMin=" + dragMinInsertPos + " dragMax=" + dragMaxInsertPos + " pos=" + position);
        }
    }
}