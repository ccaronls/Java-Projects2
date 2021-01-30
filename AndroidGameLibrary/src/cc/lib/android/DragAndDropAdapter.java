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

/**
 * Adapter to support drag and drop of list items within the list.
 * Also supports ability to drop external items into the list as droppable positions.
 *
 * Example:
 *
 * Say we have a view with ListView and some buttons like this:
 *
 *    +-----------------------------+
 *    |  +-----------------------+  |
 *    |  |                       |  |
 *    |  |       ListView        |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  |                       |  |
 *    |  +-----------------------+  |
 *    |                             |
 *    |  +-------+-------+-------+  |
 *    |  |   A   |   B   |   C   |  |
 *    |  +-------+-------+-------+  |
 *    +-----------------------------+
 *
 *    enum MyEnum {
 *        A,B,C
 *    }
 *
 *    ListView listView;
 *    Button buttonA,buttonB,buttonC;
 *
 *    DragAndDropAdapter<MyEnum> adapter = new DrapAndDropAdapter<MyEnum>(listView) {
 *        void populateItem(MyEnum e, ViewGroup container) {
 *            TextView tv = new TextView(context);
 *            tv.setText(e.name());
 *            container.removeAllViews();
 *            container.add(tv);
 *        }
 *
 *        String getItemName(MyEnum e) {
 *            return e.name();
 *        }
 *    });
 *
 *    adapter.addDraggable(buttonA, MyEnum.A);
 *    adapter.addDraggable(buttonB, MyEnum.B);
 *    adapter.addDraggable(buttonC, MyEnum.C);
 *
 *    list.setAdapter(adapter);
 *
 *    List<MyEnum> list = adapter.getList();
 *
 * @param <T>
 */
public abstract class DragAndDropAdapter<T> extends BaseAdapter implements View.OnDragListener, View.OnLongClickListener, View.OnTouchListener {

    final static String TAG = DragAndDropAdapter.class.getSimpleName();

    private final ListView listView;
    private final List<T> list = new ArrayList<>();
    private int dragInsertPos = -1;
    private int dragMinInsertPos = 0;
    private int dragMaxInsertPos = 0;
    private boolean dropped = false;

    /**
     * Populate a list item. Add views to the container. A non-empty container
     * can have its views reused as appropriate.
     *
     * @param data
     * @param container
     */
    protected abstract void populateItem(T data, ViewGroup container);

    /**
     *
     *
     * @param item
     * @return
     */
    protected abstract String getItemName(T item);

    /**
     *
     * @param listView
     */
    public DragAndDropAdapter(ListView listView) {
        this.listView = listView;
//        listView.setStackFromBottom(true);
    }

    public DragAndDropAdapter(ListView listView, List<T> items) {
        this(listView);
        list.addAll(items);
    }

    public DragAndDropAdapter(ListView listView, T ... items) {
        this(listView);
        list.addAll(Arrays.asList(items));
    }

    public final List<T> getList() {
        return list;
    }

    @Override
    public final int getCount() {
        return list.size();
    }

    @Override
    public T getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public final View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(listView.getContext(), R.layout.draganddrop_list_item, null);
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

        final T data = list.get(position);

        TextView tvLineNum = convertView.findViewById(R.id.tvLineNum);
        tvLineNum.setText(String.valueOf(position + 1));

        View v = convertView.findViewById(R.id.ibDelete);
        v.setVisibility(View.VISIBLE);
        v.setTag(position);
        v.setOnClickListener(v1 -> {
            int position1 = (Integer) v1.getTag();
            T data1 = list.remove(position1);
            notifyDataSetChanged();
        });


        ViewGroup container = convertView.findViewById(R.id.container);
        //container.removeAllViews();
        populateItem(data, container);

        return convertView;
    }

    @Override
    public final boolean onDrag(View v, DragEvent event) {
        final int dragInsertPosIn = dragInsertPos;
        final Object [] state = (Object []) event.getLocalState();
        if (state == null)
            return false;
        final T data = (T)state[0];
        final int originatingLine = (Integer)state[1];
        final int position = (Integer)v.getTag();
        final String obj = "RowItem[" + position + "]";
        switch (event.getAction()) {
            case DragEvent.ACTION_DROP: {
                Log.d(TAG, "dragInsertPos=" + dragInsertPos);
                if (dragInsertPos < 0 || list.size() == 0) {
                    list.add(data);
                } else {
                    list.add(dragInsertPos, data);
                }
                notifyDataSetChanged();
                listView.setSelection(dragInsertPos);
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
                int first = listView.getFirstVisiblePosition();
                int last = listView.getLastVisiblePosition();

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
                //listView.setSelection(dragInsertPos);
                if (dragInsertPosIn != dragInsertPos){
                    int scrollDist = v.getHeight();
                    int midpt =(last + first + 1) / 2;
                    float maxSp = 500f;
                    int speed = Math.round(maxSp - Math.abs(maxSp * 2 * (dragInsertPos-midpt)/((last-first))));
                    //0.5f * (dragInsertPos - first - midpt));
                    Log.d(TAG, "speed=" + speed);
                    if (dragInsertPos < midpt) {
                        listView.smoothScrollBy(-scrollDist, speed);
                    } else {
                        listView.smoothScrollBy(scrollDist, speed);
                    }
                }

                if (dragInsertPos == 0)
                    listView.smoothScrollToPositionFromTop(0, 0, 0);

                if (dragInsertPos <= first+1) {
                    //listView.smoothScrollBy(-scrollDist, 500);
                } else if (dragInsertPos >= last-1) {
                    //  smoothScrollBy(scrollDist, 500);
                }
                //            setSelection(dragInsertPos);
                break;
            }
            case DragEvent.ACTION_DRAG_STARTED:
                dropped = false;
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                if (!dropped) {
                    // put the item back in
                    list.add(originatingLine, data);
                    notifyDataSetChanged();
                    dropped = true;
                }
                break;
        }
        //if (dragInsertPos != dragInsertPosIn)
        {
           Log.d(TAG, "v=" + obj + " action=" + getActionStr(event.getAction()) + " pos=" + dragInsertPos + " origLine: " + originatingLine);

        }
        return true;
    }

    @Override
    public final boolean onLongClick(View v) {
        if (list.size() > 1) {
            listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
            int position = (Integer) v.getTag();
            T data = list.remove(position);
            startDrag(v, data, position);
            notifyDataSetChanged();
        }
        return true;
    }

    private void startDrag(View v, T data) {
        startDrag(v, data, -1);
    }

    private void startDrag(View v, T data, int originatingLine) {
        String name = getItemName(data);
        v.startDrag(ClipData.newPlainText(name, name), new View.DragShadowBuilder(v),
                new Object[] {
                        data, originatingLine
                }, 0);
        dragMinInsertPos = -1;
        dragMaxInsertPos = list.size();
    }

    private static String getActionStr(int action) {
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
    public final boolean onTouch(View v, MotionEvent event) {
        T type = (T)v.getTag();
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (touchView == v) {
                    listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    list.add(type);
                    listView.setSelection(list.size()-1);
                    notifyDataSetChanged();
                }
                break;
            case MotionEvent.ACTION_MOVE: {
                long delta = SystemClock.uptimeMillis() - event.getDownTime();
                //Log.d(TAG, "delta -= " + delta);
                if (list.size() > 0 && delta > 500) {
                    startDrag(v, type);
                    touchView = null;
                }
                break;
            }
            case MotionEvent.ACTION_DOWN:
                listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
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
