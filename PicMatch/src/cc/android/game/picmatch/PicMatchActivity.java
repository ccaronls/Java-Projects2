package cc.android.game.picmatch;

import java.io.InputStream;
import java.util.*;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

public class PicMatchActivity extends Activity implements OnTouchListener {

	final String TAG = "PickMatch";
	ImageView [] images;
	ImageButton [] choices;
	ImageView qmark;
	ImageView overlay;
	final Random rand = new Random();
	ImageButton current = null;
	float startX, startY;
	float touchX, touchY;
	
	final int [] happy = {
		R.drawable.happy0,
		R.drawable.happy1,
		R.drawable.happy2,
	};
	
	final int [] sad = {
			R.drawable.sad0,
			R.drawable.sad1,
			R.drawable.sad2,
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.picmatch);
		images = new ImageView[] { 
				(ImageView)findViewById(R.id.iv1),
				(ImageView)findViewById(R.id.iv2),
				(ImageView)findViewById(R.id.iv3),
				(ImageView)findViewById(R.id.iv4),
		};
		choices = new ImageButton[] { 
				(ImageButton)findViewById(R.id.ivChoice1),
				(ImageButton)findViewById(R.id.ivChoice2),
				(ImageButton)findViewById(R.id.ivChoice3),
				(ImageButton)findViewById(R.id.ivChoice4),
		};
		qmark = images[0];
		overlay = (ImageView)findViewById(R.id.overlay);
		
		for (View v : choices) {
			v.setOnTouchListener(this);
		}
		
		try {
			parseAssets();
			generateLevel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		currentMT = MatchType.valueOf(item.getTitle().toString());
		generateLevel();
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		for (MatchType mt : MatchType.values()) {
			menu.add(mt.name());
		}
		return true;
	}

	MatchType currentMT = MatchType.SIMILAR;

	HashMap<String, List<String>> categories = new HashMap<String, List<String>>();
	HashMap<String, List<String>> names = new HashMap<String, List<String>>();
	HashMap<String, List<String>> colors = new HashMap<String, List<String>>();
	HashMap<String, List<String>> counts = new HashMap<String, List<String>>();

	private void addToMap(Map<String, List<String>> map, String key, String value) {
		if (!map.containsKey(key))
			map.put(key, new ArrayList<String>());
		map.get(key).add(value);
	}
	
	private void parseAssets() throws Exception {
		for (String fileName : getAssets().list("")) {
			String [] parts = fileName.split("[_]+");
			if (parts.length != 5) {
				Log.w(TAG, "Skipping asset '" + fileName + "'");
				continue;
			}

			long len = getAssets().openFd(fileName).getLength();
			if (len > 1024*1024) {
				Log.w(TAG, "Skipping too large asset " + fileName + " (" + len + ") bytes");
				continue;
			}
			
			Map [] maps = {
					categories, names, colors, counts
			};
			
			for (int i=0; i<4; i++) {
				if (!parts[i].equals("none"))
					addToMap(maps[i], parts[i], fileName);
			}
		}
		
		Log.i(TAG, "Categories=" + dumpMapInfo(categories));
		Log.i(TAG, "Names=" + dumpMapInfo(names));
		Log.i(TAG, "Colors=" + dumpMapInfo(colors));
		Log.i(TAG, "Counts=" + dumpMapInfo(counts));
	}
	
	private String dumpMapInfo(Map<String, List<String>> map) {
		StringBuffer buf = new StringBuffer();
		for (String key : map.keySet()) {
			buf.append(key).append("(").append(map.get(key).size()).append("), ");
		}
		return buf.toString();
	}
	
	// ordered on complexity
	enum MatchType {
		SIMILAR,      // match pictures that are similar, e.g. dogs
		CATEGORY,     // match pictures of the same category, e.g. animals
		COLOR,        // match pictures of objects of the same color
		COUNT,        // match pictures with the same number of elements
//		SERIES,       // choose picture that fits into the series, e.g. baby, child, teen, adult
//		CUSTOM,       // Provide specific arrangement of pictures
	}

	Animation newAlphaAnim() {
		AlphaAnimation blinkanimation= new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
		blinkanimation.setDuration(800); // duration - half a second
		blinkanimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
		blinkanimation.setRepeatCount(-1); // Repeat animation infinitely
		blinkanimation.setRepeatMode(Animation.REVERSE);	
		return blinkanimation;
	}
	
	void generateLevel() {
		
		final List<String> validKeys = new ArrayList<String>();
		final List<String> allKeys  = new ArrayList<String>();

		Map<String, List<String>> assets = null;
		
		switch (currentMT) {
			case CATEGORY:
				assets = categories;
				break;
			case COLOR:
				assets = colors;
				break;
			case COUNT:
				assets = counts;
				break;
			case SIMILAR:
				assets = names;
				break;
		}
		
		// make sure at least one set has at least 4 items
		for (String key : assets.keySet()) {
			allKeys.add(key);
			if (assets.get(key).size()>3) {
				validKeys.add(key);
			}
		}

		if (validKeys.size()<1 || allKeys.size() < 2) {
			Log.e(TAG, "Insufficient assets");
			Log.d(TAG, "assets=" + assets);
			Log.d(TAG, "validKeys=" + validKeys);
			Log.d(TAG, "allKeys=" + allKeys);
			return;
		}
		
		String key = validKeys.get(rand.nextInt(validKeys.size()));
		
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

		shuffle(images);
		qmark = images[images.length-1];
		for (int i=0; i<images.length-1; i++) {
			images[i].setImageBitmap(getBitmapFromAssets(arr2[i]));
			images[i].setAnimation(null);
		}
		
		shuffle(choices);
		for (int i=0; i<3; i++) {
			choices[i].setImageBitmap(getBitmapFromAssets(arr[i]));
			choices[i].setTag(null);
			choices[i].setPressed(false);
		}
		choices[3].setImageBitmap(getBitmapFromAssets(arr2[3]));
		choices[3].setTag(arr2[3]);
		
		qmark.setImageResource(R.drawable.qmark2);
		qmark.setAnimation(newAlphaAnim());
	}
	
	public Bitmap getBitmapFromAssets(String fileName) {
		Log.i(TAG, "decoding " + fileName);
		Bitmap bm = null;
		try {
			InputStream in = getAssets().open(fileName);
			try {
				bm = BitmapFactory.decodeStream(in);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "Problem decoding " + fileName);
			e.printStackTrace();
		} 
		return bm;
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

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				if (current != null) {
    				current.setPressed(false);
    				current.setX(startX);
    				current.setY(startY);
    				current = null;
				}
				break;
			case MotionEvent.ACTION_DOWN:
				if (current == null) {
					overlay.setVisibility(View.GONE);
    				current = (ImageButton)v;
    				current.setPressed(true);
    				touchX = event.getX();
    				touchY = event.getY();
    				startX = v.getX();
    				startY = v.getY();
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (current != null) {
					float dx = event.getX() - touchX;
					float dy = event.getY() - touchY;
					current.setX(current.getX() + dx);
					current.setY(current.getY() + dy);
					if (isViewsOverlapping(current, qmark)) {
						//AlertDialog.Builder b = new AlertDialog.Builder(this);
						//View dv = View.inflate(this, R.layout.dialog, null);
						//b.setView(dv);
						overlay.setVisibility(View.VISIBLE);
						final boolean newLvl = current.getTag() != null;
						if (current.getTag() != null) {
							//iv.setImageBitmap(getBitmapFromAssets("faces/happy" + rand.nextInt(3) + ".jpg"));
							overlay.setImageResource(happy[rand.nextInt(happy.length)]);
							qmark.setImageBitmap(getBitmapFromAssets((String)current.getTag()));
						} else {
							//iv.setImageBitmap(getBitmapFromAssets("faces/sad" + rand.nextInt(3) + ".jpg"));
							overlay.setImageResource(sad[rand.nextInt(sad.length)]);
						}
//						final AlertDialog d = b.show();
						v.postDelayed(new Runnable() {
							public void run() {
								if (newLvl)
									generateLevel();
								//d.dismiss();
								overlay.setVisibility(View.GONE);
							}
						}, 2000);
						current.setX(startX);
						current.setY(startY);
						current.setPressed(false);
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
		float ax = locA[0] + a.getWidth()/2;
		float ay = locA[1] + a.getHeight()/2;
		float bx = locB[0] + b.getWidth()/2;
		float by = locB[1] + b.getHeight()/2;
		float minWidth = Math.max(a.getWidth(), b.getWidth())/3;
		float minHeight = Math.max(a.getHeight(), b.getHeight())/3;
		float dx = Math.abs(ax-bx);
		float dy = Math.abs(ay-by);
		//Log.d(TAG, "dx=" + dx + " dy=" + dy + " minW=" + minWidth + " minHeight=" + minHeight);
		return dx <= minWidth && dy <= minHeight;
	}
	
}
