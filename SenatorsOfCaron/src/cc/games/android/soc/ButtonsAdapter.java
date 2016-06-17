package cc.games.android.soc;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public abstract class ButtonsAdapter extends BaseAdapter implements OnItemClickListener {

	private final List<Integer> ids = new ArrayList<>();
	private final Context context;
	
	ButtonsAdapter(ListView lv) {
		this.context = lv.getContext();
		lv.setOnItemClickListener(this);
	}
	
	ButtonsAdapter addButton(int stringResId) {
		ids.add(stringResId);
		notifyDataSetChanged();
		return this;
	}
	
	@Override
	public int getCount() {
		return ids.size();
	}

	@Override
	public Object getItem(int position) {
		return ids.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		
		if (view == null) {
			view = View.inflate(context, R.layout.list_item_button, null);	
		}
		
		TextView tv = (TextView)view.findViewById(R.id.tvButtonText);
		tv.setText(ids.get(position));
		
		return view;
	}

	@Override
	public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		onButton(ids.get(position));
	}
	
	public void clear() {
		ids.clear();
		notifyDataSetInvalidated();
	}

	protected abstract void onButton(int stringResId);
}
