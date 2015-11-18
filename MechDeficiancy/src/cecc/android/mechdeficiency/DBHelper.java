package cecc.android.mechdeficiency;

import java.util.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

	private final static String TAG = "DBHelper";
	public final static int VERSION = 1;
	
	public DBHelper(Context context) {
		super(context, "formsDB", null, VERSION);
	}
	public static final String TABLE_FORM = "TForm";
	public static final String TABLE_IMAGE = "TImage";
			
	static interface IColumn<T> {
		int getColumnIndex(Cursor c);
		void setField(T t, Cursor cursor);
		void toContentValues(T t, ContentValues values);
		String getCreateArgs();
		int getVersion();
		int ordinal();
		String name();
	}
	
	enum ImageColumn implements IColumn<Image> {
		FORM(1, "integer not null"),
		IDX(1, "integer not null"),
		PATH(1, "text not null"),
		DATA(1, "text"),
		;
		
		final int version;
		final String createArgs;
		private int columnIndex = -1;
		
		ImageColumn(int version, String createArgs) {
			this.version = version;
			this.createArgs = createArgs;
		}
		
		static void clearColumnCache() {
			for (ImageColumn c : values()) {
				c.columnIndex = -1;
			}
		}
		
		@Override
		public int getColumnIndex(Cursor c) {
			if (columnIndex < 0)
				columnIndex = c.getColumnIndex(name());
			return columnIndex;
		}
		
		
		@Override
		public void setField(Image image, Cursor cursor) {
			switch (this) {
				case FORM:
					image.formId = cursor.getLong(getColumnIndex(cursor));
					break;
				case IDX:
					image.index = cursor.getInt(getColumnIndex(cursor));
					break;
				case DATA:
					image.data = cursor.getString(getColumnIndex(cursor));
					break;
				case PATH:
					image.path = cursor.getString(getColumnIndex(cursor));
					break;
			}
		}
		
		@Override
		public void toContentValues(Image image, ContentValues values) {
			switch (this) {
				case FORM:
					values.put(name(), image.formId);
					break;
				case DATA:
					values.put(name(), image.data);
					break;
				case IDX:
					values.put(name(), image.index);
					break;
				case PATH:
					values.put(name(), image.path);
					break;
			}
		}

		@Override
		public String getCreateArgs() {
			return createArgs;
		}

		@Override
		public int getVersion() {
			return version;
		}
	}
	
	
	
	enum FormColumn implements IColumn<Form> {
		_id(1, "integer primary key autoincrement"),
		CREATE_DATE(1, "long not null"),
		EDIT_DATE(1, "long not null"),
		COMPANY(1, "text"),
		CUSTOMER(1, "text"),
		LOCATION(1, "text"),
		LAT(1, "double default " + Double.MAX_VALUE),
		LNG(1, "double default " + Double.MAX_VALUE),
		PLAN(1, "text"),
		TYPE(1, "text"),
		FIX(1, "int default 0"),
		FIXED(1, "int default 0"),
		COMMENTS(1, "text"),
		REPRESENTATIVE(1, "text"),
		PROJECT(1, "text")
		;
		
		final int version;
		final String createArgs;
		private int columnIndex = -1;
		
		FormColumn(int version, String createArgs) {
			this.version = version;
			this.createArgs = createArgs;
		}
		
		static void clearColumnCache() {
			for (FormColumn c : values()) {
				c.columnIndex = -1;
			}
		}
		
		@Override
		public int getColumnIndex(Cursor c) {
			if (columnIndex < 0)
				columnIndex = c.getColumnIndex(name());
			return columnIndex;
		}
		
		
		@Override
		public void setField(Form form, Cursor cursor) {
			switch (this) {
				case COMMENTS:
					form.comments = cursor.getString(getColumnIndex(cursor));
					break;
				case COMPANY:
					form.company = cursor.getString(getColumnIndex(cursor));
					break;
				case CREATE_DATE:
					form.createDate= new Date(cursor.getLong(getColumnIndex(cursor)));
					break;
				case CUSTOMER:
					form.customer = cursor.getString(getColumnIndex(cursor));
					break;
				case EDIT_DATE:
					form.editDate = new Date(cursor.getLong(getColumnIndex(cursor)));
					break;
				case _id:
					form.id = cursor.getLong(getColumnIndex(cursor));
					break;
				case REPRESENTATIVE:
					form.representative = cursor.getString(getColumnIndex(cursor));
					break;
				case LAT:
					form.latitude = cursor.getDouble(getColumnIndex(cursor));
					break;
				case LNG:
					form.longitude = cursor.getDouble(getColumnIndex(cursor));
					break;
				case LOCATION:
					form.location = cursor.getString(getColumnIndex(cursor));
					break;
				case PLAN:
					form.plan = cursor.getString(getColumnIndex(cursor));
					break;
				case FIX:
					form.fix = cursor.getInt(getColumnIndex(cursor)) != 0;
					break;
				case FIXED:
					form.fixed = cursor.getInt(getColumnIndex(cursor)) != 0;
					break;
				case TYPE:
					form.type = cursor.getString(getColumnIndex(cursor));
					break;
				case PROJECT:
					form.project = cursor.getString(getColumnIndex(cursor));
					break;
			}
		}
		
		@Override
		public void toContentValues(Form form, ContentValues values) {
			switch (this) {
				case COMMENTS:
					values.put(name(), form.comments);
					break;
				case COMPANY:
					values.put(name(), form.company);
					break;
				case CREATE_DATE:
					values.put(name(), form.createDate.getTime());
					break;
				case CUSTOMER:
					values.put(name(), form.customer);
					break;
				case EDIT_DATE:
					values.put(name(), form.editDate.getTime());
					break;
				case _id:
					if (form.id != null)
						values.put(name(), form.id);
					break;
				case REPRESENTATIVE:
					values.put(name(), form.representative);
					break;
				case LAT:
					values.put(name(), form.latitude);
					break;
				case LNG:
					values.put(name(), form.longitude);
					break;
				case LOCATION:
					values.put(name(), form.location);
					break;
				case PLAN:
					values.put(name(), form.plan);
					break;
				case FIX:
					values.put(name(), form.fix ? 1 : 0);
					break;
				case FIXED:
					values.put(name(), form.fixed ? 1 : 0);
					break;
				case TYPE:
					values.put(name(), form.type);
					break;
				case PROJECT:
					values.put(name(), form.project);
					break;
			}
		}

		@Override
		public String getCreateArgs() {
			return createArgs;
		}

		@Override
		public int getVersion() {
			return version;
		}

	}
	
	public SQLiteDatabase getDB() {
		return getWritableDatabase();
	}
	
	private <T> void createTable(SQLiteDatabase db, String name, IColumn<T> [] values) {
		StringBuffer buf = new StringBuffer("create table ");
		buf.append(name).append(" ( ");
		for (IColumn<T> e : values) {
			if (e.ordinal() > 0)
				buf.append(",");
			buf.append(e.name()).append(" ").append(e.getCreateArgs());
		}
		buf.append(" );");
		Log.i(TAG, "Create DB with CMD: " + buf);
		db.execSQL(buf.toString());
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		FormColumn.clearColumnCache();
		ImageColumn.clearColumnCache();
		createTable(db, TABLE_FORM, FormColumn.values());
		createTable(db, TABLE_IMAGE, ImageColumn.values());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		FormColumn.clearColumnCache();
		ImageColumn.clearColumnCache();
		Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
		switch (oldVersion) {
			
			default:
				// default is to nuke
        		Log.w(TAG, "Dont know how to update, so destroy all old data");
        		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORM);
        		db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
        		onCreate(db);
		}
	}
	/*
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Column.clearColumnCache();
		Log.w(TAG, "Downgrade: Dont know how to update from " + oldVersion + " to " + newVersion + ", so destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORM);
		onCreate(db);
	}*/

	/**
	 * Return a list of forms.  NOTE in the interest of performance the forms will not have their images attached.  To get a form with all images use getFormById or
	 * call getImagesForForm 
	 * @param sortField
	 * @param ascending
	 * @param startIndex
	 * @param num
	 * @return
	 */
	public Cursor listForms(String sortField, boolean ascending) {
		return getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " ORDER BY " + sortField + " " + (ascending ? "" : " DESC"), null);
	}
	
	public List<Form> getFormsFromCursor(Cursor cursor) {
		List<Form> forms = new ArrayList<Form>();
		if (cursor.moveToFirst()) {
			do {
				Form form = new Form();
				for (FormColumn c : FormColumn.values()) {
					c.setField(form, cursor);
				}
				forms.add(form);
			} while (cursor.moveToNext());
		}
		return forms;
	}
	
	public int getFormCount() {
		Cursor cursor = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM, null);
		return cursor.getCount();
	}
	
	public void addOrUpdateForm(Form form) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
    		ContentValues values = new ContentValues();
    		for (FormColumn c : FormColumn.values())
    			c.toContentValues(form, values);
    		if (form.id == null) {
    			// add
    			long id = db.insert(TABLE_FORM, null, values);
    			Log.i(TAG,  "insert returned " + id);
    			form.id = id;
    		} else {
    			// update
    			int result = db.update(TABLE_FORM, values, FormColumn._id.name()+"="+form.id, null);
    			Log.i(TAG, "update table returned " + result);
        		db.delete(TABLE_IMAGE, ImageColumn.FORM.name() + "=?", new String [] { String.valueOf(form.id) });
    		}
    		for (Image image : form.images) {
    			image.formId = form.id;
        		values.clear();
        		for (ImageColumn c: ImageColumn.values()) {
        			c.toContentValues(image, values);
        		}
    			db.insert(TABLE_IMAGE, null, values);
    		}
    		db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
	}
	
	public Form getFormById(int id) {
		Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " WHERE _id = ?", new String[] { String.valueOf(id) });
		if (c.moveToFirst()) {
			Form form = new Form();
			for (FormColumn col : FormColumn.values()) {
				col.setField(form, c);
			}
			loadFormImages(form);
			return form;
		}
		return null;
	}
	
	public void loadFormImages(Form form) {
		Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_IMAGE + " WHERE " + ImageColumn.FORM.name() + " = ?", new String[] { String.valueOf(form.id) });
		Log.d(TAG, "loaded " + c.getCount() + " images for form " + form.id);
		if (c.moveToFirst()) {
			do {
    			Image image = new Image();
    			for (ImageColumn col : ImageColumn.values()) {
    				col.setField(image, c);
    			}
    			Log.d(TAG, "loaded image: " + image);
    			form.images.add(image);
			} while (c.moveToNext());
		}
	}
	
	String [] getDistictValuesForColumn(FormColumn column) {
		Cursor c = getWritableDatabase().rawQuery("SELECT DISTINCT " + column.name() + " from " + TABLE_FORM, null);
        String [] choices = new String[c.getCount()];
        int index = 0;
        if (c.moveToFirst()) {
        	do {
        		choices[index++] = c.getString(0);
        	} while (c.moveToNext());
        }
        return choices;
	}
	
	public void deleteForm(int id) {
		getDB().delete(TABLE_IMAGE, ImageColumn.FORM.name() + " == ?", new String [] { String.valueOf(id) });
		getDB().delete(TABLE_FORM, "_id == ?", new String [] { String.valueOf(id) });
	}

	public Object getImageByPath(String name) {
		Cursor c = getDB().rawQuery("SELECT * FROM " + TABLE_IMAGE + " WHERE PATH = ?", new String[] { name });
		if (c.moveToFirst()) {
    		Image image = new Image();
    		for (ImageColumn col : ImageColumn.values()) {
    			col.setField(image, c);
    		}
    		return image;
		}
		return null;
	}
	
}
