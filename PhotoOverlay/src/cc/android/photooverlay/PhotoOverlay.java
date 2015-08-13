package cc.android.photooverlay;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.MutableContextWrapper;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PhotoOverlay extends BaseActivity implements OnClickListener, Callback, LocationListener  {

	final static String TAG = "PhotoOverlay";
	
	
	SurfaceView viewFinder;
	ImageView overlay;
	Camera camera;
    SurfaceHolder surfaceHolder;
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
	Button choosePhoto;
	Button capture;
	SeekBar transparency;
	TextView timeStampLeft, timeStampRight;
	String locationStr = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		viewFinder = (SurfaceView)findViewById(R.id.viewFinder);
		choosePhoto = (Button)findViewById(R.id.buttonPickPhoto);
		capture = (Button)findViewById(R.id.buttonCapture);
		capture.setOnClickListener(this);
		choosePhoto.setOnClickListener(this);
		transparency = (SeekBar)findViewById(R.id.seekBarTransparency);
		transparency.setProgress(getPrefs().getInt("transparency", 50));
		timeStampLeft = (TextView)findViewById(R.id.tvTimeStampLft);
		timeStampRight = (TextView)findViewById(R.id.tvTimeStampRt);
		overlay = (ImageView)findViewById(R.id.overlay);
		overlay.setAlpha(0.01f * transparency.getProgress());
		
		transparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				getPrefs().edit().putInt("transparency", progress).commit();
				overlay.setAlpha(0.01f * progress);
				
			}
		});
		
		surfaceHolder = viewFinder.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setFormat(PixelFormat.RGB_565);
		
/*		googleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
	*/	
        
		// Acquire a reference to the system Location Manager
		locationMgr = (LocationManager) this.getSystemService(LOCATION_SERVICE);
		try{
            camera = Camera.open();
        }catch(RuntimeException e){
        	Toast.makeText(this, "Failed to find a camera", Toast.LENGTH_LONG).show();
        	finish();
        }
		
	}
	
//	GoogleApiClient googleApiClient;
	LocationManager locationMgr = null;
	SimpleDateFormat timeStampFMT = new SimpleDateFormat("EEE,MMM d,yyyy\nh:mm a");
	Timer tsTimer;
	
	Runnable updateTimeStampRunner = new Runnable() {
		public void run() {
			if (getPrefs().getBoolean("timeStampEnabled", true)) {
    			boolean onRight = getPrefs().getBoolean("timeStampRight", false);
    			
    			String txt = timeStampFMT.format(new Date()) + "\n" + locationStr;
    			String extra = getPrefs().getString("location", "");
    			if (!extra.isEmpty())
    				txt += "\n" + extra;
    			if (onRight) {
    				timeStampRight.setText(txt);
    				timeStampLeft.setText("");
    			} else {
    				timeStampLeft.setText(txt);
    				timeStampRight.setText("");
    			}
			} else {
				timeStampLeft.setText("");
				timeStampRight.setText("");
			}
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		String file = getPrefs().getString("filePath", null);
		if (file != null) {
			int orientation = getPrefs().getInt("orientation", 0);
			Bitmap bitmap = BitmapFactory.decodeFile(file);
			setOverlay(bitmap, orientation);
		}
		synchronized (this) {
    		tsTimer = new Timer();
    		tsTimer.scheduleAtFixedRate(new TimerTask() {
				
    			@Override
    			public void run() {
    				runOnUiThread(updateTimeStampRunner);
    			}
    			
    		}, 0, 30*1000);
		}
		if (locationMgr != null) {
			locationMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 10, this, Looper.getMainLooper());
		}
		if (camera != null && surfaceReady) {
			camera.startPreview();
		}
		Log.i(TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
    	Log.i(TAG, "onPause");
    	if (camera != null) {
            camera.stopPreview();
    	}
    	synchronized (this) {
        	if (tsTimer != null) {
        		tsTimer.cancel();
        		tsTimer.purge();
        		tsTimer = null;
        	}
    	}
    	if (locationMgr != null) {
    		locationMgr.removeUpdates(this);
    	}
	}

	class TakePhotoTask extends AsyncTask<Void, Void, String> implements Camera.ShutterCallback, Camera.PictureCallback {

		ProgressDialog spinner = new ProgressDialog(getActivity());
		byte [] data = null;
		
		@Override
		protected void onPreExecute() {
			spinner.show();
			camera.takePicture(this, null, this);
		}

		@Override
		protected void onPostExecute(String result) {
			spinner.dismiss();
			if (camera != null)
				camera.startPreview();
			Toast.makeText(getActivity(), result, Toast.LENGTH_LONG).show();
		}

		@Override
		protected String doInBackground(Void... params) {
			
			Bitmap mutable = null;
			SimpleDateFormat fmt = new SimpleDateFormat("mmddyyyy_HHmmss");
			String fileName = String.format("GUAGE_%s.jpg", fmt.format(new Date()));
			try {
				synchronized (this) {
					wait();
				}
    			Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
    			Matrix m = new Matrix();
    			if (isPortrait()) {
    				m.postRotate(90);
    			}
    			Bitmap bm2 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
    			if (bm != bm2)
    				bm.recycle();
    			mutable=  bm2.copy(Config.RGB_565, true);
    			bm2.recycle();
    			Canvas c = new Canvas(mutable);
    			
    			
    			TextView timeStamp = timeStampLeft;
    			boolean onRight = getPrefs().getBoolean("timeStampRight", false);
    			if (onRight) {
    				timeStamp = timeStampRight;
    			}
    			float device = (float)timeStamp.getHeight() / viewFinder.getHeight();
    			float pic = (float)timeStamp.getHeight() / c.getHeight();
    			float scale = device/pic;
    			if (onRight) {
    				c.translate(mutable.getWidth()-timeStamp.getWidth()*scale, 0);
    			}
    			c.scale(scale, scale);
    			timeStamp.draw(c);
    			
    			File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    			Log.i(TAG, "dest folder is " + dir);
    			if (!dir.isDirectory()) {
    				if (!dir.mkdirs())
    					throw new Exception("Failed to create " + dir);
    			}
    			File dest = new File(dir, fileName);
    			FileOutputStream out = new FileOutputStream(dest);
    			try {
    				if (!mutable.compress(CompressFormat.JPEG, 90, out))
    					throw new Exception("Failed to create JPEG");
    				// trigger the media mount to force a re-scan for new images to be seen in the gallery viewers
    				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
    				{
				        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				        Uri contentUri = Uri.fromFile(dest);
				        mediaScanIntent.setData(contentUri);
				        getActivity().sendBroadcast(mediaScanIntent);
    				}
    				else
    				{
				        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    				}
    			} finally {
    				out.close();
    			}
			} catch (Exception e) {
				return e.getMessage();
			} finally {
				if (mutable != null)
					mutable.recycle();
			}
			
			return String.format("%s (%d Kb)", fileName, data.length / 1000);
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			this.data = data;
			synchronized (this) {
				notify();
			}
		}

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.buttonPickPhoto: {
				startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI) , 0);
				break;
			}
			case R.id.buttonCapture:
				if (camera != null) {
					new TakePhotoTask().execute();
				}
				break;
		}
	}
	
	void setOverlay(Bitmap bitmap, int orientation) {
		Log.i(TAG, "setOverlay orientation=" + orientation);
		try {
    		Matrix matrix = new Matrix();
    		if (bitmap.getWidth() > 2048 || bitmap.getHeight() > 2048) {
    			float scale = 2048f / (float)Math.max(bitmap.getWidth(), bitmap.getHeight());
    			matrix.postScale(scale, scale);
    		}
    		//if (isPortrait())
    		//	orientation += 90;
    		matrix.postRotate(orientation);
    		Bitmap newBitmap;
            newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (newBitmap != null && newBitmap != bitmap) {
            	bitmap.recycle();
                bitmap = newBitmap;
            }
            overlay.setImageBitmap(bitmap);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult code=" + requestCode + " result=" + resultCode + " data=" + data);
		switch (requestCode) {
			case 0: {
				if (data == null)
					break;
				Uri image = data.getData();
                if (image != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image);
                        final Uri imageUri = data.getData();

                        String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION };
                        Cursor cursor = getContentResolver().query(imageUri, columns, null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(columns[0]);
                            int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
                            String filePath = cursor.getString(columnIndex);
                            int orientation = cursor.getInt(orientationColumnIndex);
                            Log.i(TAG, "got image orientation "+orientation);
                            Editor edit = getPrefs().edit();
                            edit.putString("filePath", filePath);
                            edit.putInt("orientation", orientation);
                            edit.commit();
                            setOverlay(bitmap, orientation);
                        }

                    } catch (Exception e) {
                            e.printStackTrace();
                    }
                }
				break;
			}
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.i(TAG, "onDestroy");
    	if (camera != null) {
    		camera.release();
    		camera = null;
    	}
    }
    
    boolean surfaceReady = false;
    
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (camera != null) {
			camera.stopPreview();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "surfacechanged format=" + format + " dim=" + width + "x" + height + " camera=" + camera);
		/*if (camera == null) {
    		try{
                camera = Camera.open();
            }catch(RuntimeException e){
                Log.e(TAG, "init_camera: " + e);
                return;
            }
		} else {
			camera.stopPreview();
		}*/
        Camera.Parameters param = camera.getParameters();
        processCameraParametersFromPreferences(param, width, height);
        param.setVideoStabilization(true); // tODO : Preference?
        if (isPortrait()) {
        	camera.setDisplayOrientation(90);
        	param.setRotation(90);
        } else {
        	camera.setDisplayOrientation(0);
        	param.setRotation(0);
        }

        try {
            camera.setParameters(param);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            //camera.takePicture(shutter, raw, jpeg)
            surfaceReady = true;
        } catch (Throwable e) {
            Log.e(TAG, "init_camera: " + e);
            // clear data
            getPrefs().edit().clear().commit();
        }		
	}
	
	Camera.Size getSizeClosestTo(int width, int height, Collection<Camera.Size> options_) {
		
		List<Camera.Size>options = new ArrayList<Camera.Size>();
		options.addAll(options_);
		// we want the largest size that is the closest aspect ratio.  
		Collections.sort(options, new Comparator<Camera.Size>() {

			@Override
			public int compare(Size lhs, Size rhs) {
				int lhsArea = lhs.width * lhs.height;
				int rhsArea = rhs.width * rhs.height;
				return rhsArea-lhsArea; // descending order
			}
			
		});
		
		if (isPortrait()) {
			int t = width;
			width = height;
			height = t;
		}
		Camera.Size best = null;
		final float actualAspect = (float)width / height;
        float bestDelta = 0;
        for (Camera.Size size : options) {
        	float aspect = (float)size.width / size.height;
        	float delta = Math.abs(aspect - actualAspect);
        	if (best == null || delta < bestDelta) {
        		bestDelta = delta;
        		best = size;
        	} 
        }
        return best;
	}
	
	private void processCameraParametersFromPreferences(Camera.Parameters param, int width, int height) {
        HashMap<Integer, String> allFormats = new HashMap<Integer, String>();
    	allFormats.put(ImageFormat.JPEG, "JPEG");
    	allFormats.put(ImageFormat.NV16, "NV16");
    	allFormats.put(ImageFormat.NV21, "NV21");
    	allFormats.put(ImageFormat.RGB_565, "RGB565");
    	allFormats.put(ImageFormat.YUV_420_888, "YUV 420 888");
    	allFormats.put(ImageFormat.YUY2, "YUV2");
    	allFormats.put(ImageFormat.YV12, "YV12");
        final int VERSION = 1; // Increment when we add more params

        if (getPrefs().getInt("cameraParams", 0) != VERSION) {
            LinkedHashSet<String> previewSizesStr = new LinkedHashSet<String>();
            List<Camera.Size> previewSizes = param.getSupportedPreviewSizes();
            for (Camera.Size size : previewSizes) {
            	previewSizesStr.add(sizeToString(size));
            }
            LinkedHashSet<String> pictureSizesStr = new LinkedHashSet<String>();
            List<Camera.Size> pictureSizes = param.getSupportedPictureSizes();
            for (Camera.Size size : pictureSizes) {
            	pictureSizesStr.add(sizeToString(size));
            }

        	LinkedHashSet<String> pictureFormatsStr = new LinkedHashSet<String>();
        	
        	for (int iFormat : param.getSupportedPictureFormats()) {
        		pictureFormatsStr.add(allFormats.get(iFormat));
        	}
            getPrefs().edit()
            	.putStringSet("previewSizes", previewSizesStr)
            	.putStringSet("pictureSizes", pictureSizesStr)
            	.putStringSet("antiBanding", new LinkedHashSet<String>(param.getSupportedAntibanding()))
            	.putStringSet("colorEffects", new LinkedHashSet<String>(param.getSupportedColorEffects()))
            	.putStringSet("flashModes", new LinkedHashSet<String>(param.getSupportedFlashModes()))
            	.putStringSet("focusModes", new LinkedHashSet<String>(param.getSupportedFocusModes()))
            	.putStringSet("sceneModes", new LinkedHashSet<String>(param.getSupportedSceneModes()))
            	.putStringSet("whiteBalance", new LinkedHashSet<String>(param.getSupportedWhiteBalance()))
            	.putStringSet("pictureFormats", pictureFormatsStr)
            	.putInt("cameraParams", VERSION)
            	.commit();
        }        	
        //modify parameter
//        param.setPreviewFrameRate(20);
//        param.setPictureFormat(format);
        LinkedHashSet<Camera.Size> intersectionSizes = new LinkedHashSet<Camera.Size>();
        intersectionSizes.addAll(param.getSupportedPictureSizes());
        intersectionSizes.retainAll(param.getSupportedPreviewSizes());
        Log.i(TAG, "Intersection count of preview and picture sizes is " + intersectionSizes.size());
        
        String value = getPrefs().getString("previewSizesValue", null);
        if (value != null) {
        	try {
        		int [] wh = stringToSize(value);
                param.setPreviewSize(wh[0], wh[1]);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        } else {
            Camera.Size size = getSizeClosestTo(width, height, intersectionSizes.size() > 0 ? intersectionSizes : param.getSupportedPreviewSizes());
        	getPrefs().edit().putString("previewSizesValue", sizeToString(size)).commit();
            param.setPreviewSize(size.width, size.height);
        }
        value = getPrefs().getString("pictureSizesValue", null);
        if (value != null) {
        	try {
        		int [] wh = stringToSize(value);
                param.setPictureSize(wh[0], wh[1]);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        } else {
            Camera.Size size = getSizeClosestTo(width, height, intersectionSizes.size() > 0 ? intersectionSizes : param.getSupportedPictureSizes());
        	getPrefs().edit().putString("pictureSizesValue", sizeToString(size)).commit();
            param.setPictureSize(size.width, size.height);
        }
        Log.i(TAG, "orientation=" + getResources().getConfiguration().orientation);

        value = getPrefs().getString("antiBandingValue", null);
        if (value != null)
        	param.setAntibanding(value);
        else
        	getPrefs().edit().putString("antiBandingValue", param.getAntibanding()).commit();
        value = getPrefs().getString("colorEffectsValue", null);
        if(value != null)
        	param.setColorEffect(value);
        else
        	getPrefs().edit().putString("colorEffectsValue", param.getColorEffect()).commit();
        	
        value = getPrefs().getString("flashModesValue", null);
        if (value != null)
        	param.setFlashMode(value);
        else
        	getPrefs().edit().putString("flashModesValue", param.getFlashMode()).commit();
        value = getPrefs().getString("focusModesValue", null);
        if (value != null)
        	param.setFocusMode(value);
        else
        	getPrefs().edit().putString("focusModesValue", param.getFocusMode()).commit();
        //value = getPrefs().getString("sceneModesValues", null);
        //if (value != null)
        //	param.setSceneMode(value);
        value = getPrefs().getString("whiteBalanceValue", null);
        if (value != null)
        	param.setWhiteBalance(value);
        else
        	getPrefs().edit().putString("whiteBalanceValue", param.getWhiteBalance()).commit();
        value = getPrefs().getString("pictureFormatsValue", null);
        int pictureFormat = param.getSupportedPictureFormats().get(0);
        if (value != null) {
        	for (Map.Entry<Integer,String> e : allFormats.entrySet()) {
        		if (e.getValue().equals(value)) {
        			pictureFormat = e.getKey();
        			break;
        		}
        	}
        } else {
        	getPrefs().edit().putString("pictureFormatsValue", allFormats.get(pictureFormat)).commit();
        }
        param.setPictureFormat(pictureFormat);

	}

	private String sizeToString(Camera.Size size) {
		return size.width + " X " + size.height;
	}
	
	private int [] stringToSize(String size) {
		String [] parts = size.split("[ Xx]+");
		return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
	}
	
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceReady = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Settings");
		menu.add("Time Stamp");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals("Settings")) {
			startActivity(new Intent(this, Settings.class));
		} else if (item.getTitle().equals("Time Stamp")) {
			final String [] items = {
				getPrefs().getBoolean("timeStampEnabled", true) ? "Disable" : "Enable",
				"Custom Location",
				"Clean Custom\nLocation",
				"Toggle Screen Position",
			};
			newDialogBuilder().setTitle("Time Stamp")
				.setItems(items, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case 0: { // enable / disable
								boolean isEnabled = getPrefs().getBoolean("timeStampEnabled", true);
								getPrefs().edit().putBoolean("timeStampEnabled", !isEnabled).commit();
								runOnUiThread(updateTimeStampRunner);
								break;
							}
							
							case 1: { // extra timestamp text
								final Set<String> knownLocations = getPrefs().getStringSet("knownLocations", new HashSet<String>());
								List<String> matches = new ArrayList<String>(knownLocations);
								final AutoCompleteTextView et = new AutoCompleteTextView(getActivity());
								et.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.list_item_known_locations, R.id.textViewLocation, matches));
								
//								final EditText et = new EditText(getActivity());
								et.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
								
								newDialogBuilder().setTitle("Extra Text")
								.setView(et)
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										String txt = et.getText().toString().trim();
										if (txt.length() > 3) {
											knownLocations.add(txt);
											getPrefs().edit().putStringSet("knownLocations", knownLocations).commit();
										}
										getPrefs().edit().putString("location", txt).commit();
										runOnUiThread(updateTimeStampRunner);
									}
								}).setNegativeButton("Cancel", null)
								.show();
								break;
							}
							
							case 2: {
								getPrefs().edit().remove("location").commit();
								runOnUiThread(updateTimeStampRunner);
								break;
							}
							
							case 3: {
								boolean onRight = getPrefs().getBoolean("timeStampRight", false);
								getPrefs().edit().putBoolean("timeStampRight", !onRight).commit();
								runOnUiThread(updateTimeStampRunner);
								break;
							}
						}
					}
				})
				.setNegativeButton("Cancel", null)
//				.setPositiveButton("Ok", null)
				.show();
		}
		return true;
	}

	@Override
	public void onLocationChanged(Location location) {
		locationStr = String.format("%.3f, %.3f", location.getLatitude(), location.getLongitude());
		runOnUiThread(updateTimeStampRunner);
		getAddressFromLocation(location);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
		
	}

	public void getAddressFromLocation(final Location loc) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
				try {
					List<Address> addressList = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
					if (addressList != null && addressList.size() > 0) {
						Address address = addressList.get(0);
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
							if (i>0)
								sb.append("\n");
							sb.append(address.getAddressLine(i));
						}
						//sb.append(address.getLocality()).append("\n");
						//sb.append(address.getPostalCode()).append("\n");
						//sb.append(address.getCountryName());
						locationStr = sb.toString();
						runOnUiThread(updateTimeStampRunner);
					}
				} catch (Exception e) {
					Log.e(TAG, "Unable connect to Geocoder", e);
				} 
			}
		};
		thread.start();
	}
	
}
