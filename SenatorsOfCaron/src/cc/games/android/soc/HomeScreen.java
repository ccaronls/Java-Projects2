package cc.games.android.soc;

import cc.games.android.soc.AScreen.GoogleState;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class HomeScreen extends AScreen {

	private ListView lv;
	private ButtonsAdapter ba;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View root = inflater.inflate(R.layout.home, container, false);
		
		lv = (ListView)root.findViewById(R.id.listview);
		ba = new ButtonsAdapter(lv) {
			
			@Override
			protected void onButton(int stringResId) {
				switch (stringResId) {
					case R.string.button_sign_out:
					case R.string.button_signin:
					case R.string.button_start_multiplayer:
					case R.string.button_start_single_player:
				}
			}
		};

		onGooglePlayStateChange(isGooglePlayConnected() ? GoogleState.CONNECTED : GoogleState.DISCONNECTED);
		
		return root;
	}

	protected void onGooglePlayStateChange(GoogleState state) {
		ba.clear();
		switch (state) {
			case CONNECTED:
				ba.addButton(R.string.button_sign_out);
				ba.addButton(R.string.button_start_multiplayer);
				ba.addButton(R.string.button_start_single_player);
				break;
			case DISCONNECTED:
				ba.addButton(R.string.button_signin);
				ba.addButton(R.string.button_start_single_player);
				break;
			case SUSPENDED:
				ba.addButton(R.string.button_sign_out);
				ba.addButton(R.string.button_start_single_player);
				break;
		}
	}

	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

}
