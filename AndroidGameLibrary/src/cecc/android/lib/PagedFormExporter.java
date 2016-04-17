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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Picture;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;

@SuppressWarnings("deprecation")
public abstract class PagedFormExporter extends AsyncTask<Void, String, Object> implements PictureListener, OnCancelListener {
	
	private final String TAG = getClass().getSimpleName();
	
	private final int width = 1600;//R.dimen.paged_webview_width;
	private final int height = width * 22 / 17; // height uses aspect ratio 8.5 x 11
	private final Activity activity; 
	private final SimpleDateFormat stamp = new SimpleDateFormat("MMddyyyy_HHmm", Locale.US);
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

		@Override
		public void onScaleChanged(WebView view, float oldScale, float newScale) {
			Log.d(TAG, "scale changed from " + oldScale + " too " + newScale);
			super.onScaleChanged(view, oldScale, newScale);
		}
    	
    	
	};

	public PagedFormExporter(Activity activity, Signature [] signatures) {
		this.activity = activity;
		this.signatures = signatures;
		wv = new WebView(activity);
		wv.setWebViewClient(webClient);
		wv.setPictureListener(this);
		wv.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
		wv.getSettings().setUseWideViewPort(true);
		wv.getSettings().setSupportZoom(false);
		wv.setAnimation(null);
		wv.setInitialScale(Math.round(getDensity() * 100));
		wv.getSettings().setTextZoom(Math.round(100f / getDensity()));
	}

	protected final void setNumPages(int numPages) {
		this.numPages = numPages;
	}
	
	private float getDensity() {
		DisplayMetrics dm = activity.getResources().getDisplayMetrics();
		return dm.density;
	}
	
	protected final void start() {
		DisplayMetrics dm = activity.getResources().getDisplayMetrics();
		Log.d(TAG, "display metrics density=" + dm.density + " dpi=" + dm.densityDpi + " hgt=" + dm.heightPixels + " scaled=" + dm.scaledDensity + " width=" + dm.widthPixels + " xdpi=" + dm.xdpi + " ydpi=" + dm.ydpi);
		html.setLength(0);
		int h = Math.round(height / getDensity());
		html.append(
				"<html>\n" +
				"<head>\n" +
				"<meta name=\"viewport\" content=\"width=" + width + ", initial-scale=1\">\n" + 
				"<style type=\"text/css\">\n" +
				"body { font-size:36px; height:" + h + "px;}\n" +
				// title
				"h1 { white-space: nowrap; font-size:72px; text-align:center; }\n" +
				"h2 { font-size:60px; }\n" +
				// Comments
				"h3 { font-size:48px; white-space: nowrap; }\n" +
				// Date below the title
				"h4 { font-size:42px; white-space: nowrap; text-align:center; }\n" +
				"td { font-size:48px; white-space: nowrap; text-align:left; vertical-align:top; }\n" +
				// comments body
				"span { width:" + width + "; word-wrap:break-word; }\n" +
				"table { table-layout:auto; }\n" +
				".footer { position: fixed; bottom:0; width:" + width + "px; }\n" +
				"</style>\n" +
				"</head>\n" +
				"<body>\n"
				);
	}
	
	protected final void end() {
		html.append("</body></html>\n");
	}
	
	protected final StringBuffer getHtml() {
		return html;
	}

	private boolean tableMode = false;
	
	protected void beginTable(int width) {
		if (!tableMode) {
			if (width > 0) {
				int w = Math.round((float)this.width * width / (100 * getDensity()));
				html.append("<br/>\n<table style=\"width:").append(w).append("px\">\n");
			} else {
				html.append("<br/>\n<table>\n");
			}
			tableMode = true;
		}
	}
	
	protected void endTable() {
		if (tableMode) {
			html.append("</table>\n");
			tableMode = false;
		}
	}
	
	protected final void entry(String ... pairs) {
		boolean breaky = false;
		for (int i=0; i<pairs.length; i+=2) {
			String label = pairs[i];
			String value = pairs[i+1];
    		if (value != null && !value.isEmpty()) {
    			if (!breaky) {
    				breaky=true;
    				if (tableMode) {
    					html.append("<tr>");
    				} else {
    					html.append("<br/>");
    				}
    			}
    			if (tableMode) {
    				html.append("<td><b>").append(label).append("</b></td><td>").append(value).append("</td>\n");
    			} else {
    				html.append("<b>").append(label).append("</b>").append(value).append("\n");
    			}
    		}
		}
		if (tableMode && breaky) {
			html.append("</tr>");
		}
	}
	
	protected final void header(String title, String date) {
		beginTable(100);
		html.append("<tr><td><h1>").append(title).append("</h1></td></tr>\n");
		html.append("<tr><td><h4>").append(date).append("</h4></td></tr>");
		endTable();
		if (numPages > 1) {
			html.append("</br><pre>Page ").append(curPage).append(" of ").append(numPages).append("</pre>\n");
		}
	}
	
	//protected final void footer(String txt) {
//		html.append("<p><div id=\"footer\">").append(txt).append("</div></p>\n");
	//}
	
	protected String getFooter() { return ""; }
	
	protected abstract void genPage(int pageNum, Signature [] signatures);
	
	private void makePage() throws Exception {
		Log.d(TAG, "makePage begein: state=" + state);
		File htmlFile = new File(activity.getCacheDir(), "htmlFile.html");
		FileUtils.stringToFile(html.toString(), htmlFile);
		final String url = "file:///" + activity.getCacheDir().getAbsolutePath() + "/" + htmlFile.getName();
		state = 0;
		publishProgress(url);
		synchronized (this) {
			for (int i=0; i<10 && state < 3; i++) {
				Log.d(TAG, "makePage waiting for state to settle");
				wait(2000);
			}
		}
		Log.d(TAG, "makePage state changed to " + state);
		if (state != 3)
			throw new Exception();
		htmlFile.delete();
	}
	
	/**
     * Draw the view into a bitmap.
     */
    private Bitmap getViewBitmap(View v) {
    	
		int specWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
		int specHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
		wv.measure(specWidth, specHeight);
		wv.layout(0, 0, width, height);

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		wv.draw(canvas);
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		p.setTextAlign(Align.CENTER);
		p.setTextSize(24);
		canvas.drawText(getFooter(), width/2, height-p.getTextSize()-5, p);

        Log.d(TAG, "Bitmap dim out= "+ bitmap.getWidth() + "x" + bitmap.getHeight());
        
        return bitmap;
    }
    
	@Override
	public final void onNewPicture(WebView view, Picture picture) {
		Log.d(TAG, "onNewPicture state=" + state);
		if (state != 2)
			return;
		long t = System.currentTimeMillis();
		Log.d(TAG, "Generating page image");
		try {
			Bitmap bm = getViewBitmap(wv);
			Log.d(TAG, "bitmap dim = " + bm.getWidth() + " x " + bm.getHeight());
			File file = null;
			if (numPages > 1)
				file = new File(activity.getCacheDir(), "page" + curPage + ".jpg");
			else
				file = new File(activity.getCacheDir(), "Report_" + stamp.format(new Date()) + ".jpg");
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
			state = 3;
		} catch (Exception e) {
			Log.e(TAG, "onNewPicture Error: " + e.getMessage());
			state = 4; // error
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
	protected final void onPostExecute(Object result) {
		Log.d(TAG, "onPostExecute result=" + result);
		dialog.dismiss();
		if (result != null) {
			if (result instanceof File)
				onEmailAttachmentReady((File)result);
			else if (result instanceof Exception)
				onError((Exception)result);
			else
				onError(new Exception("Unknown result: " + result.getClass().getSimpleName()));
		} else {
			onError(new Exception("null result"));
		}
	}
	
	protected abstract void onEmailAttachmentReady(File attachment);

	protected abstract void onError(Exception e);
	
	@Override
	protected final void onProgressUpdate(String... values) {
		Log.d(TAG, "onProgressUpdate: " + values[0]);
		wv.loadUrl(values[0]);
	}

	@Override
	protected final Object doInBackground(Void... params) {
		try {
    		Log.d(TAG, "Starting to make " + numPages);
			for (int i=0; i<numPages; i++) {
				html.setLength(0);
				Log.d(TAG, "genPage " + i);
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
    			file = new File(activity.getCacheDir(),"CompletedReport_" + stamp.format(new Date()) + ".zip");
    			FileUtils.zipFiles(file, pages);
			} else {
				file = pages.get(0);
			}
			
			return file;
		} catch (Exception e) {
			e.printStackTrace();
			return e; // Error
		}
	
	}

	@Override
	public final void onCancel(DialogInterface dialog) {
		this.cancel(true);
		dialog.dismiss();
	}
	
	protected final Activity getActivity() {
		return activity;
	}
	
	protected final int getWidth() {
		return width;
	}
	
	protected final int getHeight() {
		return height;
	}
}
