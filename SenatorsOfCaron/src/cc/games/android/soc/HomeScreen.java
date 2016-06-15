package cc.games.android.soc;

import cc.games.android.soc.AScreen.GoogleState;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HomeScreen extends AScreen {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	private View si, so, mp;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View root = inflater.inflate(R.layout.home, container, false);
		
		si = root.findViewById(R.id.buttonSignin); si.setOnClickListener(this);
		so = root.findViewById(R.id.buttonSignOut); so.setOnClickListener(this);
		mp = root.findViewById(R.id.buttonMultiplayer); mp.setOnClickListener(this);
		root.findViewById(R.id.buttonSinglePlayer).setOnClickListener(this);
		
		return root;
	}

	protected void onGooglePlayStateChange(GoogleState state) {
		switch (state) {
			case CONNECTED:
				si.setVisibility(View.GONE);
				so.setVisibility(View.VISIBLE);
				mp.setEnabled(true);
				break;
			case DISCONNECTED:
				si.setVisibility(View.VISIBLE);
				so.setVisibility(View.GONE);
				mp.setEnabled(false);
				break;
			case SUSPENDED:
				si.setVisibility(View.GONE);
				so.setVisibility(View.GONE);
				mp.setEnabled(false);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonMultiplayer:
			case R.id.buttonSignin:
			case R.id.buttonSignOut:
			case R.id.buttonSinglePlayer:
		}
	}

}
