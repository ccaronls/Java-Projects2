package cc.lib.android;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

/**
 * Created by chriscaron on 3/12/18.
 */

public abstract class ArrayListAdapter<T> extends BaseAdapter {

    final ArrayList<T> list;
    final int itemId;
    final Context ctxt;

    public ArrayListAdapter(Context ctxt, ArrayList<T> list, int itemLayoutId) {
        this.ctxt = ctxt;
        this.list = list;
        this.itemId = itemLayoutId;
    }

    @Override
    public final int getCount() {
        return list.size();
    }

    @Override
    public final Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public final long getItemId(int position) {
        return position;
    }

    @Override
    public final View getView(int position, View v, ViewGroup parent) {
        if (v == null) {
            v = View.inflate(ctxt, itemId, null);
        }
        initItem(v, position, list.get(position));
        return v;
    }

    protected abstract void initItem(View v, int position, T item);
}
