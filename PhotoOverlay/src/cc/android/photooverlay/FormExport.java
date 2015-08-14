package cc.android.photooverlay;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.widget.ImageView;
import android.widget.TextView;

public class FormExport extends BaseActivity implements ImageGetter {
	
	public final static String INTENT_FORM = "FORM";
	
	private void replace(StringBuffer buffer, String toReplace, String with) {
		int idx = buffer.indexOf(toReplace);
		if (idx >= 0) {
			if (with == null || with.trim().length() == 0)
				with = "???";
			buffer.replace(idx, idx+toReplace.length(), with);
		}
	}
	
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.formexport);
		Form form = (Form)getIntent().getParcelableExtra(INTENT_FORM);
		
		try {
			InputStream in = getAssets().open("export.html");
			try {
				byte [] buffer = new byte[in.available()];
				in.read(buffer);
				
				
				StringBuffer buf = new StringBuffer(new String(buffer));
				replace(buf, "%%CUSTOMER%%", form.customer);
				replace(buf, "%%INSPECTOR%%", form.inspector);
				replace(buf, "%%LOCATION%%", form.location);
				replace(buf, "%%DATE%%", DateFormat.getDateInstance(DateFormat.SHORT).format(form.editDate));
				replace(buf, "%%SYSTEM%%", form.system);
				replace(buf, "%%STATUS%%", form.passed ? "PASSED" : "FAILED");
				replace(buf, "%%PLAIN%%", form.plain);
				replace(buf, "%%DETAIL%%", form.detail);
				replace(buf, "%%TYPE%%", form.type.name());
				replace(buf, "%%IMAGE1%%", form.imagePath[0]);
				replace(buf, "%%IMAGE2%%", form.imagePath[1]);
				replace(buf, "%%COMMENTS%%", form.comments);
				
				((TextView)findViewById(R.id.tvHtmlForm)).setText(Html.fromHtml(buf.toString(), this, null));
				
			} finally {
				in.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
 
	}

	@Override
	public Drawable getDrawable(String source) {
		File image = new File(getImagesPath(), source);
		Drawable empty = null;
		LevelListDrawable d = new LevelListDrawable();
		if (image.exists()) {
			empty = Drawable.createFromPath(image.getPath());
		} else {
			empty =getResources().getDrawable(R.drawable.ic_launcher);
		}
        d.addLevel(0, 0, empty);
        d.setBounds(0, 0, empty.getIntrinsicWidth(), empty.getIntrinsicHeight());
        return d;
	}

}
