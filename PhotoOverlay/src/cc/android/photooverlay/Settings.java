package cc.android.photooverlay;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Settings extends BaseActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
	}
	
	public void showConfig(View v) {
		final String config = (String)v.getTag();
		Set<String> items = getPrefs().getStringSet(config, new HashSet<String>());
		AlertDialog.Builder builder = newDialogBuilder();
		builder.setTitle(((Button)v).getText());
		final String [] itemsArr = items.toArray(new String[items.size()]);
		String curValue = getPrefs().getString(config + "Value", "");
		int checkedItem = -1;
		for (int i=0; i<itemsArr.length; i++) {
			if (itemsArr[i].equals(curValue)) {
				checkedItem = i;
				break;
			}
		}
		builder.setSingleChoiceItems(itemsArr, checkedItem, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getPrefs().edit().putString(config + "Value", itemsArr[which]).commit();
			}
		});
		builder.setNegativeButton("Clear", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getPrefs().edit().remove(config + "Value").commit();
			}
		});
		builder.setPositiveButton("Ok", null);
		builder.show();
	}
	
}
