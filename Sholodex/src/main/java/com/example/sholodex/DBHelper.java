package com.example.sholodex;

import java.util.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

	private final static String TAG = "FormHelper";
	public final static int VERSION = 1;
	
	public DBHelper(Context context) {
		super(context, "sholodex.db", null, VERSION);
	}
	public static final String TABLE_FORM = "TCard";
			
	enum Column {
		_id(1, "integer primary key autoincrement"),
        FIRST_NAME(1, "text"),
        PHONE_NUMBER(1, "text"),
        EMAIL(1, "text"),
        ADDRESS(1, "text"),
        IMAGE_PATH(1, "text")
		;
		
		final int version;
		final String createArgs;
		private int columnIndex = -1;
		
		Column(int version, String createArgs) {
			this.version = version;
			this.createArgs = createArgs;
		}
		
		static void clearColumnCache() {
			for (Column c : values()) {
				c.columnIndex = -1;
			}
		}
		
		int getColumnIndex(Cursor c) {
			if (columnIndex < 0)
				columnIndex = c.getColumnIndex(name());
			return columnIndex;
		}
		
		
		void setFormField(IndexCard form, Cursor cursor) {
			switch (this) {
                case _id:
                    form.id = cursor.getInt(getColumnIndex(cursor));
                    break;
                case FIRST_NAME:
                    form.firstName = cursor.getString(getColumnIndex(cursor));
                    break;
                case PHONE_NUMBER:
                    form.phoneNumber = cursor.getString(getColumnIndex(cursor));
                    break;
                case EMAIL:
                    form.email= cursor.getString(getColumnIndex(cursor));
                    break;
                case ADDRESS:
                    form.address = cursor.getString(getColumnIndex(cursor));
                    break;
                case IMAGE_PATH:
                    form.imagePath = cursor.getString(getColumnIndex(cursor));
                    break;
            }
		}
		
		void formToContentValues(IndexCard form, ContentValues values) {
			switch (this) {
                case _id:
                    if (form.id != null)
                        values.put(name(), form.id);
                    break;
                case FIRST_NAME:
                    values.put(name(), form.firstName);
                    break;
                case PHONE_NUMBER:
                    values.put(name(), form.phoneNumber);
                    break;
                case EMAIL:
                    values.put(name(), form.email);
                    break;
                case ADDRESS:
                    values.put(name(), form.address);
                    break;
                case IMAGE_PATH:
                    values.put(name(), form.imagePath);
                    break;
            }
		}
	}
	
	public SQLiteDatabase getDB() {
		return getWritableDatabase();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Column.clearColumnCache();
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

		Column.clearColumnCache();
		Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
		switch (oldVersion) {
			
			default:
				// default is to nuke
        		Log.w(TAG, "Dont know how to update, so destroy all old data");
        		db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORM);
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

	public Cursor listForms(String sortField, boolean ascending, int startIndex, int num) {
		return getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " ORDER BY " + sortField + " " + (ascending ? "" : " DESC"), null);
	}

	public Cursor listAllForms() {
	    return getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM, null);
    }

	public IndexCard getFormFromCursor(Cursor cursor) {
        IndexCard form = new IndexCard();
        for (Column c : Column.values()) {
            c.setFormField(form, cursor);
        }
        return form;
    }

	public List<IndexCard> getFormsFromCursor(Cursor cursor) {
		List<IndexCard> forms = new ArrayList<IndexCard>();
		if (cursor.moveToFirst()) {
			do {
				IndexCard form = new IndexCard();
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
	
	public void addOrUpdateForm(IndexCard form) {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		try {
    		ContentValues values = new ContentValues();
    		for (Column c : Column.values())
    			c.formToContentValues(form, values);
    		if (form.id == null) {
    			// add
    			long id = db.insert(TABLE_FORM, null, values);
    			Log.i(TAG,  "insert returned " + id);
    			form.id = (int)id;
    		} else {
    			// update
    			int result = db.update(TABLE_FORM, values, Column._id.name()+"="+form.id, null);
    			Log.i(TAG, "update table returned " + result);
    		}
    		db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
	}
	
	public IndexCard getFormById(int id) {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_FORM + " WHERE _id = ?", new String[] { String.valueOf(id) });
		if (c.moveToFirst()) {
			IndexCard form = new IndexCard();
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
	
}
