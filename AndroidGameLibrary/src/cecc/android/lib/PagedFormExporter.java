package cecc.android.lib;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import cc.lib.utils.FileUtils;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

@SuppressWarnings("deprecation")
public abstract class PagedFormExporter extends AsyncTask<Void, String, File> implements PictureListener, OnCancelListener {
	
	private final String TAG = getClass().getSimpleName();
	
	private final int width = 1600;
	private final int height = 1600*22/17;
	protected final Activity activity; 
//	private final SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM d, yyyy", Locale.getDefault());
	protected final StringBuffer html = new StringBuffer();
	private int numPages;
	private final List<File> pages = new ArrayList<File>();
	private final WebView wv;
	private final Signature [] signatures;
	private int state = 0;
	private int curPage = 1;
	
	public static class Signature {
		public final File signatureFile;
		public final String fullName;
		public final Date date;
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

	public PagedFormExporter(Activity activity, Signature [] signatures) {
		this.activity = activity;
		this.signatures = signatures;
		wv = new WebView(activity);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		wv.setLayoutParams(params);
		wv.layout(0, 0, width, height);
		wv.setWebViewClient(webClient);
		wv.setPictureListener(this);
	}

	protected final void setNumPages(int numPages) {
		this.numPages = numPages;
	}
	
	protected final void start() {
		html.setLength(0);
		html.append("<html><head></head><body>\n");
	}
	
	protected final void end() {
		html.append("</body></html>\n");
	}
	
	protected final StringBuffer getHtml() {
		return html;
	}

	protected final void entry(String ... pairs) {
		boolean breaky = false;
		for (int i=0; i<pairs.length; i+=2) {
			String label = pairs[i];
			String value = pairs[i+1];
    		if (value != null && !value.isEmpty()) {
    			if (!breaky) {
    				breaky=true;
    				html.append("<br/>");
    			}
    			html.append("<b>").append(label).append("</b>").append(value).append("\n");
    		}
		}
	}
	
	protected final void header(String title) {
		html.append("<h1>").append(title).append("</h1>");
		if (numPages > 1) {
			html.append("</br>Page ").append(curPage).append(" of ").append(numPages);
		}
	}
	
	protected abstract void genPage(int pageNum, Signature [] signatures);
	
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
	public final void onNewPicture(WebView view, Picture picture) {
		if (state != 2)
			return;
		state = 3;
		long t = System.currentTimeMillis();
		Log.d(TAG, "Generating page image");
		try {
			Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bm);
			view.draw(canvas);
			File file = new File(activity.getCacheDir(), "page" + curPage + ".jpg");
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
	protected final void onPreExecute() {
		dialog = ProgressDialog.show(activity, "Generating...", "", true, true, this);
	}
	
	public final int getNumPages() {
		return this.numPages;
	}

	@Override
	protected final void onPostExecute(File result) {
		dialog.dismiss();
		if (result != null) {
    		//EmailHelper.sendEmail(activity, result, null, activity.getString(R.string.emailSubjectSignedPTCform), activity.getString(R.string.email_body_signed_form));
			onEmailAttachmentReady(result);
		}
	}
	
	protected abstract void onEmailAttachmentReady(File attachment);

	@Override
	protected final void onProgressUpdate(String... values) {
		state = 0;
		wv.loadUrl(values[0]);
	}

	@Override
	protected final File doInBackground(Void... params) {
		try {
    		
			for (int i=0; i<numPages; i++) {
				html.setLength(0);
				genPage(i, signatures);
				if (isCancelled())
					return null;
				makePage();
				if (isCancelled())
					return null;
				curPage++;
			}
			
			File file = null;
			if (pages.size() > 1) {
				SimpleDateFormat stamp = new SimpleDateFormat("mmddyyyy_HHmm", Locale.US);
    			file = new File(activity.getCacheDir(),"CompletedReport_" + stamp.format(new Date()) + ".zip");
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
	public final void onCancel(DialogInterface dialog) {
		this.cancel(true);
		dialog.dismiss();
	}
}
