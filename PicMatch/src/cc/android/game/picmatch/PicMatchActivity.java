package cc.android.game.picmatch;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class PicMatchActivity extends Activity implements OnTouchListener {

	final String TAG = "PickMatch";
	ImageView [] images;
	ImageView [] choices;
	ImageView qmark;
	final HashMap<String, List<String>> assets = new HashMap<String, List<String>>();
	final Random rand = new Random();
	final List<String> validKeys = new ArrayList<String>();
	final List<String> allKeys  = new ArrayList<String>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picmatch);
		images = new ImageView[] { 
				(ImageView)findViewById(R.id.iv1),
				(ImageView)findViewById(R.id.iv2),
				(ImageView)findViewById(R.id.iv3),
		};
		choices = new ImageView[] { 
				(ImageView)findViewById(R.id.ivChoice1),
				(ImageView)findViewById(R.id.ivChoice2),
				(ImageView)findViewById(R.id.ivChoice3),
				(ImageView)findViewById(R.id.ivChoice4),
		};
		qmark = (ImageView)findViewById(R.id.ivQmark);
		
		for (View v : choices) {
			v.setOnTouchListener(this);
		}

		Pattern pattern = Pattern.compile("[a-z]+[1-9][0-9]*\\.[a-zA-Z]{3,4}");
		Pattern pattern2 = Pattern.compile("[a-z]+");
		try {
    		for (String file : getAssets().list("")) {
    			if (pattern.matcher(file).matches()) {
    				Log.i(TAG, "Matched file: " + file);
    				Matcher m = pattern2.matcher(file);
    				if (m.find()) {
    					String prefix = m.group();
    					Log.i(TAG, "Parsed prefix '" + prefix + "'");
    					if (!assets.containsKey(prefix)) {
    						assets.put(prefix,  new LinkedList<String>());
    					}
    					assets.get(prefix).add(file);
    				}
    			}
    		}
    		
    		// make sure at least one set has at least 4 items
    		for (String key : assets.keySet()) {
    			allKeys.add(key);
    			if (assets.get(key).size()>3) {
    				validKeys.add(key);
    			}
    		}

    		Log.d(TAG, "assets=" + assets);
    		Log.d(TAG, "validKeys=" + validKeys);
    		Log.d(TAG, "allKeys=" + allKeys);

    		if (validKeys.size()<1 || allKeys.size() < 2)
    			throw new Exception("Insufficient assets");

    		generateLevel();
    		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	void generateLevel() {
		
		int r= rand.nextInt(validKeys.size());
		
		String key = validKeys.get(r);
		
		List<String> allChoices = new ArrayList<String>();
		for (String s : allKeys) {
			if (s.equals(key))
				continue;
			allChoices.addAll(assets.get(s));
		}
		
		String [] arr = allChoices.toArray(new String[allChoices.size()]);
		shuffle(arr);
		
		List<String> sources = assets.get(key);
		String [] arr2 = sources.toArray(new String[sources.size()]);
		shuffle(arr2);
		
		for (int i=0; i<images.length; i++)
			images[i].setImageBitmap(getBitmapFromAssets(arr2[i]));
		
		shuffle(choices);
		for (int i=0; i<3; i++) {
			choices[i].setImageBitmap(getBitmapFromAssets(arr[i]));
			choices[i].setTag(null);
		}
		choices[3].setImageBitmap(getBitmapFromAssets(arr2[3]));
		choices[3].setTag(arr2[3]);
		
		qmark.setImageResource(R.drawable.qmark);
	}
	
	public Bitmap getBitmapFromAssets(String fileName) {
		try {
			return BitmapFactory.decodeStream(getAssets().open(fileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	<T> void shuffle(T [] items) {
		for (int i=0; i<items.length * 10; i++) {
			int i0 = rand.nextInt(items.length);
			int i1 = rand.nextInt(items.length);
			T t = items[i0];
			items[i0] = items[i1];
			items[i1] = t;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	View current = null;
	float startX, startY;
	float touchX, touchY;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				if (current != null) {
    				current.setX(startX);
    				current.setY(startY);
    				current = null;
				}
				break;
			case MotionEvent.ACTION_DOWN:
				current = v;
				touchX = event.getX();
				touchY = event.getY();
				startX = v.getX();
				startY = v.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (current != null) {
					float dx = event.getX() - touchX;
					float dy = event.getY() - touchY;
					current.setX(current.getX() + dx);
					current.setY(current.getY() + dy);
					if (isViewsOverlapping(current, qmark)) {
						AlertDialog.Builder b = new AlertDialog.Builder(this);
						View dv = View.inflate(this, R.layout.dialog, null);
						b.setView(dv);
						ImageView iv = (ImageView)dv.findViewById(R.id.iv1);
						final boolean newLvl = current.getTag() != null;
						if (current.getTag() != null) {
							iv.setImageBitmap(getBitmapFromAssets("faces/happy" + rand.nextInt(3) + ".jpg"));
							qmark.setImageBitmap(getBitmapFromAssets((String)current.getTag()));
						} else {
							iv.setImageBitmap(getBitmapFromAssets("faces/sad" + rand.nextInt(3) + ".jpg"));
						}
						final AlertDialog d = b.show();
						v.postDelayed(new Runnable() {
							public void run() {
								if (newLvl)
									generateLevel();
								d.dismiss();
							}
						}, 2000);
						current.setX(startX);
						current.setY(startY);
						current = null;
					}
				}
				break;
		}
		return true;
	}

	boolean isViewsOverlapping(View a, View b) {
		int [] locA = new int[2];
		int [] locB = new int[2];
		a.getLocationOnScreen(locA);
		b.getLocationOnScreen(locB);
		float ax = locA[0] + a.getWidth();
		float ay = locA[1] + a.getHeight()/2;
		float bx = locB[0] + b.getWidth()/2;
		float by = locB[1] + b.getHeight()/2;
		float minWidth = (a.getWidth() + b.getWidth())/4;
		float minHeight = (a.getHeight() + b.getHeight())/4;
		float dx = Math.abs(ax-bx);
		float dy = Math.abs(ay-by);
		//Log.d(TAG, "dx=" + dx + " dy=" + dy + " minW=" + minWidth + " minHeight=" + minHeight);
		return dx <= minWidth && dy <= minHeight;
	}
	
}
