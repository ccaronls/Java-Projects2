package com.example.sholodex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {

    final static int IMAGE_CAPURTE_DIM = 512;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabEdit;
    private TextView tvTabs;

    DBHelper helper = null;

    public final DBHelper getFormHelper() {
        if (helper == null || !helper.getDB().isOpen())
            helper = new DBHelper(this);
        return helper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        fabAdd = findViewById(R.id.fabAdd);
        fabEdit = findViewById(R.id.fabEdit);

        fabAdd.setOnClickListener(this);
        fabEdit.setOnClickListener(this);
        tvTabs = findViewById(R.id.tvTabs);

        mViewPager.addOnPageChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabAdd: {
                IndexCard ic = new IndexCard();
                showEditIndexCarddialog(ic, true);
                break;
            }
            case R.id.fabEdit: {
                IndexCard ic = getFormHelper().getFormFromCursor(cursor);
                showEditIndexCarddialog(ic, false);
                break;
            }
        }
    }

    void showEditIndexCarddialog(final IndexCard ic, final boolean newCard) {
        final View v = View.inflate(this, R.layout.form_edit_dialog, null);
        new AlertDialog.Builder(this).setTitle(newCard ? "New Index Card" : "Edit Index Card")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(newCard ? "Add" : "Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText etFN = v.findViewById(R.id.etFirstName);
                        EditText etPH = v.findViewById(R.id.etPhoneNum);
                        EditText etAD = v.findViewById(R.id.etAddress);
                        EditText etEM = v.findViewById(R.id.etEmail);

                        ic.address = etAD.getText().toString();
                        ic.email = etEM.getText().toString();
                        ic.firstName = etFN.getText().toString();
                        ic.phoneNumber = etPH.getText().toString();

                        if (ic.firstName.trim().length() > 0
                                || ic.phoneNumber.trim().length() > 0
                                || ic.email.trim().length() > 0
                                || ic.address.trim().length() > 0) {
                            getFormHelper().addOrUpdateForm(ic);
                            if (newCard) {
                                refreshCursor();
                                cursor.moveToLast(); // move the cursor to most recently added card
                            }
                            mSectionsPagerAdapter.notifyDataSetChanged();
                            fabEdit.setVisibility(View.VISIBLE);
                        } else {
                            new AlertDialog.Builder(MainActivity.this).setTitle("Error").setMessage("Index card cannot be empty").setNegativeButton("Ok", null).show();
                        }
                    }
                }).show();

    }

    Cursor cursor = null;

    @Override
    protected void onResume() {
        super.onResume();
        refreshCursor();
        if (getFormHelper().getFormCount() == 0) {
            fabEdit.setVisibility(View.GONE);
        } else {
            fabEdit.setVisibility(View.VISIBLE);
        }
    }

    void updateTabs(int position) {
        if (cursor != null) {
            tvTabs.setText(String.format("%d of %d cards", position, getFormHelper().getFormCount()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    void refreshCursor() {
        int pos = -1;
        if (cursor != null) {
            pos = cursor.getPosition();
            cursor.close();
        }
        cursor = getFormHelper().listAllForms();
        if (pos >= 0) {
            cursor.moveToPosition(pos);
        } else {
            cursor.moveToFirst();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        updateTabs(position+1);
    }

    @Override
    public void onPageSelected(int position) {
        updateTabs(position+1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        Bitmap photo = null;
        IndexCard ic;
        ImageView ivPhoto;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(IndexCard card) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putParcelable("IndexCard", card);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onClick(View view) {
            ((MainActivity)getActivity()).startTakePictureActivity(ic.id);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            ivPhoto = rootView.findViewById(R.id.ivPhoto);
            TextView tvFirstName = rootView.findViewById(R.id.tvFirstName);
            TextView tvPhoneNum = rootView.findViewById(R.id.tvPhoneNum);
            TextView tvEmail = rootView.findViewById(R.id.tvEmail);
            TextView tvAddress = rootView.findViewById(R.id.tvAddress);

            ic = getArguments().getParcelable("IndexCard");

            ivPhoto.setOnClickListener(this);
            tvFirstName.setText(ic.firstName);
            tvPhoneNum.setText(ic.phoneNumber);
            tvEmail.setText(ic.email);
            tvAddress.setText(ic.address);

            loadPhoto();

            return rootView;
        }

        private void loadPhoto() {

            if (ic.imagePath != null && ic.imagePath.trim().length() > 0) {
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        return BitmapFactory.decodeFile(new File(getActivity().getFilesDir(), ic.imagePath).getPath());
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (photo != null) {
                            photo.recycle();
                        }
                        photo = bitmap;
                        ivPhoto.setImageBitmap(photo);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (photo != null) {
                photo.recycle();
                photo = null;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            ic = ((MainActivity)getActivity()).getFormHelper().getFormById(ic.id);
            loadPhoto();
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            cursor.moveToPosition(position);
            IndexCard ic = getFormHelper().getFormFromCursor(cursor);
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(ic);
        }

        @Override
        public int getCount() {
            return getFormHelper().getFormCount();
        }

    }

    public final void startTakePictureActivity(int resultIndex) {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, resultIndex);
    }

    protected void onPictureTaken(Bitmap bitmap, File bitmapFile, int resultIndex) {
        IndexCard ic = getFormHelper().getFormById(resultIndex);
        bitmap.recycle();
        if (ic != null) {
            ic.imagePath = bitmapFile.getName();
            getFormHelper().addOrUpdateForm(ic);
            mSectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int resultIndex, int resultCode, Intent data) {
        Bitmap bitmap = null;
        int orientation = 0;

        if (resultCode == Activity.RESULT_OK && data != null)
        {
            Uri image = data.getData();
            if (image != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), image);
                    final Uri imageUri = data.getData();

                    String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION };
                    Cursor cursor = getContentResolver().query(imageUri, columns, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        //int fileColumnIndex = cursor.getColumnIndex(columns[0]);
                        int orientationColumnIndex = cursor.getColumnIndex(columns[1]);
                        //String filePath = cursor.getString(fileColumnIndex);
                        orientation = cursor.getInt(orientationColumnIndex);
                        //log.debug("got image orientation "+orientation);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bitmap != null) {
                //if (isPremiumEnabled(false))
                bitmap = ThumbnailUtils.extractThumbnail(bitmap, IMAGE_CAPURTE_DIM, IMAGE_CAPURTE_DIM);
                //else
                //	bitmap = ThumbnailUtils.extractThumbnail(bitmap, 64, 64);
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case 90:
                    case 180:
                    case 270:
                        matrix.postRotate(orientation);
                        break;
                }

                Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                if (newBitmap != null) {
                    bitmap.recycle();
                    bitmap = newBitmap;
                }

                // watermark
                //if (isPremiumEnabled(false))

                try {
                    File destFile = File.createTempFile("photo", ".jpg", getFilesDir());
                    FileOutputStream out = new FileOutputStream(destFile);
                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        onPictureTaken(bitmap, destFile, resultIndex);

                    } finally {
                        out.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //bitmap.recycle();
            }
        }
    }
}
