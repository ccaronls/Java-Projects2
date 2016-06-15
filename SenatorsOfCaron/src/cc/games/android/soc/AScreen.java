package cc.games.android.soc;

import com.google.android.gms.common.api.GoogleApiClient;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

public abstract class AScreen extends Fragment implements OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
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
	
	public final boolean isGooglePlayConnected() {
		return false;
	}
	
	final GoogleApiClient getGoogleAPI() {
		return null;
	}
	
	enum GoogleState {
		CONNECTED,SUSPENDED,DISCONNECTED
	}
	
	protected void onGooglePlayStateChange(GoogleState state) {}
}
