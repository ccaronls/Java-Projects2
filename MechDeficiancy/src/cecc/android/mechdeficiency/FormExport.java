package cecc.android.mechdeficiency;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import cc.lib.android.EmailHelper;
import cc.lib.utils.FileUtils;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Picture;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

@SuppressWarnings("deprecation")
public class FormExport extends AsyncTask<Void, String, File> implements PictureListener, OnCancelListener {
	
	private final String TAG = getClass().getSimpleName();
	
	private final int width = 1600;
	private final int height = 1600*22/17;
	private final BaseActivity activity; 
	private final SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM d, yyyy", Locale.getDefault());
	private final StringBuffer html = new StringBuffer();
	private int numPages = 0;
	private final Form form;
	private final List<File> pages = new ArrayList<File>();
	private final WebView wv;
	private final Signature [] signatures;
	private int state = 0;
	
	public static class Signature {
		final File signatureFile;
		final String fullName;
		final Date date;
		public Signature(File signatureFile, String fullName, Date date) {
			super();
			this.signatureFile = signatureFile;
			this.fullName = fullName;
			this.date = date;
		}
		
	}
	
	private final WebViewClient webClient = new WebViewClient() {
		
    	@Override
    	public void onPageStarted(WebView view, String url, Bitmap favicon) {
    		if (state == 0)
    			state = 1;
    	}
    
    	@Override
    	public void onPageFinished(WebView view, String url) {
    		if (state == 1)
    			state = 2;
    	}
	};

	FormExport(BaseActivity activity, Form form, Signature [] signatures) {
		this.activity = activity;
		this.form = form;
		this.signatures = signatures;
		wv = new WebView(activity);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		wv.setLayoutParams(params);
		wv.layout(0, 0, width, height);
		wv.setWebViewClient(webClient);
		wv.setPictureListener(this);
	}

	private void start() {
		html.setLength(0);
		html.append("<html><head></head><body>\n");
	}
	
	private void end() {
		html.append("</body></html>\n");
	}

	private void entry(String ... pairs) {
		boolean breaky = false;
		for (int i=0; i<pairs.length; i+=2) {
			String label = pairs[i];
			String value = pairs[i+1];
    		if (value != null) {
    			if (!breaky) {
    				breaky=true;
    				html.append("<br/>");
    			}
    			html.append("<b>").append(label).append("</b>").append(value).append("\n");
    		}
		}
	}
	
	private void header(String title) {
		html.append("<h1>").append(title).append("</h1>");
	}
	
	private void mainPage() {
		start();
		header("Mechanical Deficiency Report");
		if (numPages > 1) {
			html.append("</br>Page 1 of ").append(numPages);
		}
		entry("Date:", fmt.format(form.editDate));
		entry("Cusomter:", form.customer);
		entry("Company:", form.company, "Project:", form.project);
		entry("Representative:", form.representative);
		entry("Plan:", form.plan, "Type:", form.type);
		html.append("<br/><table width=\"100%\"><tr>");
		html.append("<td>").append(form.fix ? "Fix Needed" : "Fix Not Needed").append("</td>\n");
		html.append("<td>").append(form.fixed ? "Fix Completed" : "Fix Not Completed").append("</td>\n");
		html.append("</tr></table>\n");
		html.append("<br/><table width=\"100%\">\n");
		html.append("<tr>\n");
		for (int i=0; i<2; i++) {
			html.append("<tr>");
    		for (int ii=0; ii<3; ii++) {
    			html.append("<td>\n");
    			int index = (i*3) + ii;
    			Image image = form.getImageForIndex(index);
    			if (image != null) {
    				Uri uri = Uri.fromFile(new File(activity.getImagesPath(), image.path));
    				html.append("<img width=\"170\" height=\"170\" src=\"").append(uri.toString()).append("\">\n");
    			} else {
    				html.append("<img width=\"170\" height=\"170\" src=\"\" alt=\"No Image\"/>\n");
    			}
    			html.append("</td>\n");
    		}
    		html.append("</tr><tr>\n");
    		for (int ii=0; ii<3; ii++) {
    			html.append("<td>\n");
    			int index = (i*3) + ii;
    			Image image = form.getImageForIndex(index);
    			if (image != null && image.data != null) {
    				html.append(image.data);
    			}    			
    			html.append("</td>\n");
    		}
    		html.append("</tr>\n");
		}
		html.append("</table>\n");
		html.append("<br/><h3>Comments</h3>\n").append(form.comments).append("\n");
		end();
		Log.d("HTML Page 1", html.toString());
	}

	private void signaturesPage(Signature [] signatures, int offset, int page) {
		start();
		header("Mechanical Deficiency Report");
		if (numPages > 1) {
			html.append("</br>Page ").append(page).append(" of ").append(numPages);
		}
		entry("Date:", fmt.format(form.editDate));
		entry("Cusomter:", form.customer);
		entry("Company:", form.company, "Project:", form.project);
		entry("Representative:", form.representative);
		entry("Plan:", form.plan, "Type:", form.type);
		int count = 0;
		for (int i=offset; i<signatures.length; i++) {
			if (count ++ == 3)
				break;
			Signature s = signatures[i];
			html.append("</br><table width=\"100%\"><tr>\n");
			html.append("<td width=\"50%\">");
			html.append(s.fullName == null || s.fullName.isEmpty() ? "Not Specified" : s.fullName).append("</td>\n");
			html.append("<td align=\"right\" width=\"50%\">").append(fmt.format(s.date)).append("</td>\n");
			html.append("</tr></table>");
//			html.append("</br>").append(s.fullName).append("     ").append(fmt.format(s.date));
			Uri uri = Uri.fromFile(s.signatureFile);
			html.append("</br><img width=\"512\" src=\"").append(uri.toString()).append("\">\n");
		}
		end();
		Log.d("HTML Page " + page, html.toString());
	}
	
	private void makePage() throws Exception {
		File htmlFile = new File(activity.getCacheDir(), "htmlFile.html");
		FileUtils.stringToFile(html.toString(), htmlFile);
		final String url = "file:///" + activity.getCacheDir().getAbsolutePath() + "/" + htmlFile.getName();
		//done = false;
		publishProgress(url);
		synchronized (this) {
			wait(2000);
		}
		htmlFile.delete();
	}
	
	@Override
	public void onNewPicture(WebView view, Picture picture) {
		if (state != 2)
			return;
		state = 3;
		long t = System.currentTimeMillis();
		Log.d(TAG, "Generating page image");
		try {
			Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bm);
			view.draw(canvas);
			File file = File.createTempFile("form", ".jpg", activity.getCacheDir());
			OutputStream out = new FileOutputStream(file);
			try {
				bm.compress(CompressFormat.JPEG, 90, out);
			} finally {
				bm.recycle();
				out.close();
			}	
			pages.add(file);
			long dt = System.currentTimeMillis() - t;
			Log.d(TAG, "generated file in " + dt/1000 + " seconds: " + file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		synchronized (this) {
			notify();
		}
	}
	
	private ProgressDialog dialog;

	@Override
	protected void onPreExecute() {
		dialog = ProgressDialog.show(activity, "Generating...", "", true, true, this);
	}

	@Override
	protected void onPostExecute(File result) {
		dialog.dismiss();
		if (result != null) {
    		EmailHelper.sendEmail(activity, result, null, activity.getString(R.string.emailSubjectSignedPTCform), activity.getString(R.string.email_body_signed_form));
		}
	}

	@Override
	protected void onProgressUpdate(String... values) {
		state = 0;
		wv.loadUrl(values[0]);
	}

	@Override
	protected File doInBackground(Void... params) {
		try {
    		numPages = 1;
    		if (signatures.length > 0) {
    			numPages += signatures.length / 3 + 1;
    		}
    		
    		mainPage();
    		if (isCancelled())
    			return null;
    		makePage();
    		if (isCancelled())
    			return null;
    		
    		File file = null;
    		
    		int curPage = 2;
    		if (signatures.length > 0) {
    			for (int i=0; i<signatures.length; i+=3) {
    				html.setLength(0);
    				signaturesPage(signatures, i, curPage++);
    				if (isCancelled())
    					return null;
    				makePage();
    				if (isCancelled())
    					return null;
    			}
    			SimpleDateFormat stamp = new SimpleDateFormat("mmddyyyy_HHmm", Locale.US);
    			file = new File(activity.getCacheDir(),"MechDefReport_" + stamp.format(new Date()) + ".zip");
    			FileUtils.zipFiles(file, pages);
    		} else {
    			file = pages.get(0);
    		}
    		
    		return file;
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Error
		}
	
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		this.cancel(true);
		dialog.dismiss();
	}
}
