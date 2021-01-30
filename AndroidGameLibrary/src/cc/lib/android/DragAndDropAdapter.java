package cc.lib.android;

import android.content.ClipData;
import android.os.SystemClock;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.game.Utils;

public abstract class DragAndDropAdapter<T> extends BaseAdapter implements View.OnDragListener, View.OnLongClickListener, View.OnTouchListener {

    final static String TAG = DragAndDropAdapter.class.getSimpleName();

    private final ListView dndLv;
    private final List<T> list = new ArrayList<>();
    private int dragInsertPos = -1;
    private int dragMinInsertPos = 0;
    private int dragMaxInsertPos = 0;
    private boolean dropped = false;

    public DragAndDropAdapter(ListView dndLv) {
        this.dndLv = dndLv;

    }

    public DragAndDropAdapter(ListView dndLv, List<T> items) {
        this(dndLv);
        list.addAll(items);
    }

    public DragAndDropAdapter(ListView dndLv, T ... items) {
        this(dndLv);
        list.addAll(Arrays.asList(items));
    }

    public final List<T> getList() {
        return list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(dndLv.getContext(), R.layout.draganddrop_list_item, null);
        }
        convertView.setOnDragListener(this);
        convertView.setOnLongClickListener(this);
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

        final T cmd = list.get(position);

        TextView tvLineNum = (TextView) convertView.findViewById(R.id.tvLineNum);
        tvLineNum.setText(String.valueOf(position + 1));

        View v = convertView.findViewById(R.id.ibDelete);
        v.setVisibility(View.VISIBLE);
        v.setTag(position);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                T cmd = list.remove(position);
                notifyDataSetChanged();
            }
        });


        ViewGroup container = (ViewGroup)convertView.findViewById(R.id.container);
        //container.removeAllViews();
        populateItem(cmd, container);

        return convertView;
    }

    protected abstract void populateItem(T cmd, ViewGroup container);

    @Override
    public boolean onDrag(View v, DragEvent event) {
        int dragInsertPosIn = dragInsertPos;
        String obj = null;
        Object [] state = (Object []) event.getLocalState();
        if (state == null)
            return false;
        T cmd = (T)state[0];
        int originatingLine = (Integer)state[1];
        if (v.equals(dndLv)) {
            obj = "ListView";
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP: {
                    if (cmd != null) {
                        if (dragInsertPos < 0) {
                            list.add(cmd);
                        } else {
                            list.add(dragInsertPos, cmd);
                        }
                        notifyDataSetChanged();
                        dndLv.setSelection(dragInsertPos);
                        dragInsertPos = -1;
                    }
                    dropped = true;
                    break;
                }
                //case DragEvent.ACTION_DRAG_LOCATION:
                case DragEvent.ACTION_DRAG_ENTERED: {
                    if (list.size() > 0) {
                        dragInsertPos = Utils.clamp(list.size(), dragMinInsertPos, dragMaxInsertPos);
                        notifyDataSetChanged();
                    }
                    break;
                }
                case DragEvent.ACTION_DRAG_EXITED:
                    if (dragMinInsertPos < 0) {
                        dragInsertPos = -1;
                    } else if (originatingLine >= 0) {
                        dragInsertPos = originatingLine;
                    }
                    notifyDataSetChanged();
                    break;
                case DragEvent.ACTION_DRAG_STARTED:
                    dropped = false;
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    dragInsertPos = -1;
                    if (!dropped && originatingLine >= 0) {
                        list.add(originatingLine, cmd);
                        notifyDataSetChanged();
                    }
                    break;
            }
        } else {
            int position = (Integer)v.getTag();
            obj = "RowItem[" + position + "]";
            switch (event.getAction()) {
                case DragEvent.ACTION_DROP: {
                    if (dragInsertPos < 0 || list.size() == 0) {
                        list.add(cmd);
                    } else {
                        list.add(dragInsertPos, cmd);
                    }
                    notifyDataSetChanged();
                    dndLv.setSelection(dragInsertPos);
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
                    float mp = v.getHeight()/2;
                    float mpd = event.getY();// + dragging.getHeight();
                    int first = dndLv.getFirstVisiblePosition();
                    int last = dndLv.getLastVisiblePosition();

                    //if (position == first) {
                    //    Log.d("PLV", "A");
                    //    dragInsertPos = position;
                    //} else if (position == last) {
                    //    Log.d("PLV", "B");
                    //    dragInsertPos = position+1;
                    //} else
                    if (mp > mpd) {
                        Log.d(TAG, "C mp=" + mp + " mpd=" + mpd);
                        dragInsertPos = position;
                    } else {
                        Log.d(TAG, "D");
                        dragInsertPos = position+1;
                    }
                    dragInsertPos = Utils.clamp(dragInsertPos, dragMinInsertPos, dragMaxInsertPos);
                    notifyDataSetChanged();
                    //              setSelection(dragInsertPos);
                    if (dragInsertPosIn != dragInsertPos){
                        int scrollDist = v.getHeight();
                        int midpt =(last + first + 1) / 2;
                        float maxSp = 500f;
                        int speed = Math.round(maxSp - Math.abs(maxSp * 2 * (dragInsertPos-midpt)/((last-first))));
                        //0.5f * (dragInsertPos - first - midpt));
                        Log.d(TAG, "speed=" + speed);
                        if (dragInsertPos < midpt) {
                            dndLv.smoothScrollBy(-scrollDist, speed);
                        } else {
                            dndLv.smoothScrollBy(scrollDist, speed);
                        }
                    }

                    if (dragInsertPos == 0)
                        dndLv.smoothScrollToPositionFromTop(0, 0, 0);

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
            Log.d(TAG, "v=" + obj + " action=" + getActionStr(event.getAction()) + " pos=" + dragInsertPos + " origLine: " + originatingLine);

        }
        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        if (list.size() > 1) {
            int position = (Integer) v.getTag();
            T cmd = list.remove(position);
            startDrag(v, cmd, position);
            notifyDataSetChanged();
        }
        return true;
    }

    public final void startDrag(View v, T cmd) {
        startDrag(v, cmd, -1);
    }

    protected abstract String getItemName(T item);

    private final void startDrag(View v, T cmd, int originatingLine) {
        String name = getItemName(cmd);
        v.startDrag(ClipData.newPlainText(name, name), new View.DragShadowBuilder(v),
                new Object[] {
                        cmd, originatingLine
                }, 0);
        dragMinInsertPos = -1;
        dragMaxInsertPos = list.size();
    }

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

    private View touchView;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        T type = (T)v.getTag();
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (touchView == v) {
                    list.add(type);
                    notifyDataSetChanged();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (list.size() > 0 && (event.getDownTime() - SystemClock.uptimeMillis()) > 500) {
                    startDrag(v, type);
                    touchView = null;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                touchView = v;
                break;
        }

        return true;
    }

    public void addDraggable(View v, T type) {
        v.setTag(type);
        v.setOnTouchListener(this);
    }
}
