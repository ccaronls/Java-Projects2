package cc.game.zombicide.android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Created by Chris Caron on 12/13/21.
 */
public class ActivityViewModel extends ViewModel {

    public MutableLiveData<Boolean> consoleVisible = new MutableLiveData(true);

    public MutableLiveData<Boolean> loading = new MutableLiveData(false);

    public MutableLiveData<Boolean> playing = new MutableLiveData(false);

    // TODO: Move this to ViewModel
    public static class ButtonAdapter extends BaseAdapter {

        ButtonAdapter() {
            this.buttons = new ArrayList<>();
        }

        private final List<View> buttons;

        public void update(List<View> buttons) {
            this.buttons.clear();
            this.buttons.addAll(buttons);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return buttons.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return buttons.get(position);
        }
    }

    public final ButtonAdapter listAdapter = new ButtonAdapter();


}
