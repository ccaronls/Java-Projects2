package cc.game.android.yahtzee;

import cc.game.yahtzee.core.Yahtzee;
import cc.game.yahtzee.core.YahtzeeSlot;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class YahtzeeSlotAdapter extends BaseAdapter {

	final Yahtzee yahtzee;
	final YahtzeeActivity activity;
	
	YahtzeeSlotAdapter(YahtzeeActivity context, Yahtzee yahtzee) {
		this.activity = context;
		this.yahtzee = yahtzee;
	}
	
	@Override
	public int getCount() {
		return yahtzee.getAllSlots().size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			view = View.inflate(activity, R.layout.yahtzeeslotlistitem, null);
			view.setOnClickListener(activity);
		}
		TextView tvName = (TextView)view.findViewById(R.id.textViewSlotName);
		TextView tvPts  = (TextView)view.findViewById(R.id.textViewSlotPoints);
		ImageView ivUnavail = (ImageView)view.findViewById(R.id.imageViewUnavailable);
		YahtzeeSlot slot = yahtzee.getAllSlots().get(position);
		view.setTag(slot);
		boolean used = yahtzee.isSlotUsed(slot);
		view.setEnabled(!used); 
		tvName.setText(slot.getNiceName());
		tvPts.setText("" + (used ? yahtzee.getSlotScore(slot) : slot.getScore(yahtzee)));
		ivUnavail.setVisibility(used ? View.VISIBLE : View.INVISIBLE);
		return view;
	}

}
