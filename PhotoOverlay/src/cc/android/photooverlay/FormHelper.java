package cc.android.photooverlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.*;
import android.net.Uri;
import android.util.Log;

public class FormHelper extends SQLiteOpenHelper {

	private final static String TAG = "FormHelper";
	public final static int VERSION = 1;
	
	public FormHelper(Context context, DatabaseErrorHandler errorHandler) {
		super(context, "formsDB", null, VERSION, errorHandler);
	}
	public static final String TABLE_FORM = "TForm";
			
	enum Column {
		_id(1, "integer primary key autoincrement"),
		CREATE_DATE(1, "long not null"),
		EDIT_DATE(1, "long not null"),
		COMPANY(1, "text"),
		CUSTOMER(1, "text"),
		LOCATION(1, "text"),
		LAT(1, "double default " + Double.MAX_VALUE),
		LNG(1, "double default " + Double.MAX_VALUE),
		SYSTEM(1, "text"),
		PLAIN(1, "text"),
		DETAIL(1, "text"),
		TYPE(1, "text default " + Form.FormType.Mechanical),
		IMAGE1(1, "text"),
		IMAGE2(1, "text"),
		PASSED(1, "int default 0"),
		COMMENTS(1, "text"),
		INSPECTOR(1, "text");
		
		final int version;
		final String createArgs;
		private int columnIndex = -1;
		
		Column(int version, String createArgs) {
			this.version = version;
			this.createArgs = createArgs;
		}
		
		int getColumnIndex(Cursor c) {
			if (columnIndex < 0)
				columnIndex = c.getColumnIndex(name());
			return columnIndex;
		}
		
		
		void setFormField(Form form, Cursor cursor) {
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
				case DETAIL:
					form.detail = cursor.getString(getColumnIndex(cursor));
					break;
				case EDIT_DATE:
					form.editDate = new Date(cursor.getLong(getColumnIndex(cursor)));
					break;
				case _id:
					form.id = cursor.getInt(getColumnIndex(cursor));
					break;
				case IMAGE1:
					form.imagePath[0] = cursor.getString(getColumnIndex(cursor));
					break;
				case IMAGE2:
					form.imagePath[1] = cursor.getString(getColumnIndex(cursor));
					break;
				case INSPECTOR:
					form.inspector = cursor.getString(getColumnIndex(cursor));
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
				case PLAIN:
					form.plain = cursor.getString(getColumnIndex(cursor));
					break;
				case PASSED:
					form.passed = cursor.getInt(getColumnIndex(cursor)) != 0;
					break;
				case SYSTEM:
					form.system = cursor.getString(getColumnIndex(cursor));
					break;
				case TYPE:
					form.type = Form.FormType.valueOf(cursor.getString(getColumnIndex(cursor)));
					break;
			}
		}
		
		void formToContentValues(Form form, ContentValues values) {
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
				case DETAIL:
					values.put(name(), form.detail);
					break;
				case EDIT_DATE:
					values.put(name(), form.editDate.getTime());
					break;
				case _id:
					if (form.id != null)
						values.put(name(), form.id);
					break;
				case IMAGE1:
					values.put(name(), form.imagePath[0]);
					break;
				case IMAGE2:
					values.put(name(), form.imagePath[1]);
					break;
				case INSPECTOR:
					values.put(name(), form.inspector);
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
				case PLAIN:
					values.put(name(), form.plain);
					break;
				case PASSED:
					values.put(name(), form.passed ? 1 : 0);
					break;
				case SYSTEM:
					values.put(name(), form.system);
					break;
				case TYPE:
					values.put(name(), form.type.name());
					break;
			}
		}
	}
	
	public SQLiteDatabase getDB() {
		return getWritableDatabase();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuffer buf = new StringBuffer("create table ");
		buf.append(TABLE_FORM).append(" ( ");
		for (Column c : Column.values()) {
			if (c.ordinal() > 0)
				buf.append(",");
			buf.append(c.name()).append(" ").append(c.createArgs);
		}
		buf.append(" );");
		Log.i(TAG, "Create DB with CMD: " + buf);
		db.execSQL(buf.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		Log.w(TAG,
		        "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");
		    db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORM);
		    onCreate(db);
	}

	public Cursor listForms(String sortField, boolean ascending, int startIndex, int num) {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " ORDER BY " + sortField + " " + (ascending ? "" : " DESC"), null);
		
		List<Form> forms = getFormsFromCursor(c);
		
		
		return c;
	}
	
	public List<Form> getFormsFromCursor(Cursor cursor) {
		List<Form> forms = new ArrayList<Form>();
		if (cursor.moveToFirst()) {
			do {
				Form form = new Form();
				for (Column c : Column.values()) {
					c.setFormField(form, cursor);
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
		//db.beginTransaction();
		ContentValues values = new ContentValues();
		for (Column c : Column.values())
			c.formToContentValues(form, values);
		if (form.id == null) {
			// add
			long id = db.insert(TABLE_FORM, null, values);
			Log.i(TAG,  "insert returned " + id);
		} else {
			// update
			int result = db.update(TABLE_FORM, values, Column._id.name()+"="+form.id, null);
			Log.i(TAG, "update table returned " + result);
		}
		//db.endTransaction();
	}
	
	public Form getFormById(int id) {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " WHERE _id = ?", new String[] { String.valueOf(id) });
		if (c.moveToFirst()) {
			Form form = new Form();
			for (Column col : Column.values()) {
				col.setFormField(form, c);
			}
			return form;
		}
		return null;
	}
	
	String [] getDistictValuesForColumn(Column column) {
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
		getDB().delete(TABLE_FORM, "_id == ?", new String [] { String.valueOf(id) });
	}
	
	public void reset() {
		// TODO
		try {
			Thread.sleep(4000);
		} catch (Exception e) {
			
		}
	}
}
