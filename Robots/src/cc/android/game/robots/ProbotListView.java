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

public class ProbotListView extends ListView implements View.OnDragListener, Runnable {

    BaseAdapter adapter = null;
    int programLineNum = -1;
    Probot probot=null;

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
                TextView tvLineNum = (TextView)convertView.findViewById(R.id.tvLineNum);
                tvLineNum.setText(String.valueOf(position+1) + ":");

                convertView.findViewById(R.id.ibDelete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        probot.program.remove(position);
                        adapter.notifyDataSetChanged();
                    }
                });

                ImageView iv = (ImageView)convertView.findViewById(R.id.imageView);
                switch (probot.program.get(position).type) {

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

                iv = (ImageView)convertView.findViewById(R.id.ivPlay);
                iv.setVisibility(position == programLineNum ? View.VISIBLE : View.INVISIBLE);

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
}

    /*
    LinearLayout
} implements View.OnDragListener, View.OnLongClickListener {

    private List<Probot.Command> list = Collections.emptyList();

    private void init(Context c, AttributeSet a) {
        setOrientation(LinearLayout.VERTICAL);
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

    public void setList(List<Probot.Command> list) {
        this.list = list;
        removeAllViews();
        for (Probot.Command c : list) {
            View v = View.inflate(getContext(), R.layout.item_command, null);
            // populate the view
            v.setOnLongClickListener(this);
            v.setOnDragListener(this);
            v.setTag(c);
        }
    }



    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_LOCATION: {
                // if the midpoint y of v is in the upper half of this view, then insert above
                // otherwise insert below
                float mp = v.getY() + v.getHeight()/2; // mp of view being dragged into
                float mpd = event.getY() + dragged.getHeight();
                Probot.Command cmd = (Probot.Command)v.getTag();
                int index = list.indexOf(cmd);
                if (mpd < mp) {

                } else {

                }
                break;
            }

            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DROP:
        }

        return true;
    }

    private View dragged = null; // when not null then we are re-arranging the list

    @Override
    public boolean onLongClick(View v) {
        Probot.Command cmd = (Probot.Command)v.getTag();
        v.startDrag(ClipData.newPlainText(cmd.type.name(), cmd.type.name()), new View.DragShadowBuilder(v), v, 0);
        dragged = v;
        return true;
    }


}
*/