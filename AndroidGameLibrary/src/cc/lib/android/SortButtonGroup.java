package cc.lib.android;

import java.util.*;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

/**
 * A variation on a radio group.  Sort buttons have 3 states:
 * 
 * Enabled, Sort Ascending
 * Enabled, Sort Descending
 * Disabled
 * 
 * This class handles these changes and executes a callback.  The callback is designed to be used to create an SQL
 * query statement.  
 * 
 * Example XML:
    <com.fullpower.widget.SortButtonGroup
        android:id="@+id/layoutSortButtons"
        android:layout_width="fill_parent"
        android:layout_height="wrap_content"
        android:checkedButton="@+id/buttonSortByDate"
        android:orientation="horizontal" >

        <com.fullpower.widget.SortButton
            android:id="@+id/buttonSortByDate"
            android:text="Date" 
            app:sortAscending="false"
            app:sortField="SORT_FIELD_DATE"
            />
        <com.fullpower.widget.SortButton
            android:id="@+id/buttonSortByTime"
            android:text="Time" 
            app:sortField="SORT_FIELD_DURATION"
            />
        <com.fullpower.widget.SortButton
            android:id="@+id/buttonSortByName"
            android:text="Name" 
            app:sortField="SORT_FIELD_NAME"
            />
        <com.fullpower.widget.SortButton
            android:id="@+id/buttonSortByDistance"
            android:text="Distance" 
            app:sortField="SORT_FIELD_DISTANCE"
            />
        
    </com.fullpower.widget.SortButtonGroup>
    
 
 * 
 * @author chriscaron
 *
 */
public final class SortButtonGroup extends RadioGroup{

	public static interface OnSortButtonListener {
		
		/**
		 * Executed when a sort button is pressed.  
		 * @param group the parent sort button group of the button that was pressed
		 * @param checkedId the id of the button that was pressed
		 * @param sortField the sort field attached to the sort button (will default to the sort button text if this is not set)
		 * @param ascending sort button 3rd state
		 */
		void sortButtonChanged(SortButtonGroup group, int checkedId, SortButton ... buttonsHistory);
	}
	
	private OnSortButtonListener sortListener;
	private final LinkedList<SortButton> sortHistory = new LinkedList<SortButton>();
	private int maxSortFields = 2;
	
	private void init(Context context, AttributeSet attrs) {
	}

	public SortButtonGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public SortButtonGroup(Context context) {
		super(context);
	}

	public OnSortButtonListener getOnSortButtonListener() {
		return sortListener;
	}

	public void setOnSortButtonListener(OnSortButtonListener sortListener) {
		this.sortListener = sortListener;
	}

	@Override
	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		throw new RuntimeException("dont use OnCheckChangedListener, use OnSortButtonListener");
	}

	// package access since used only by SortButton
	void triggerSort(SortButton button) {
		pushSortHistory(button);
		if (sortListener != null) {
			SortButton [] buttons = sortHistory.toArray(new SortButton[sortHistory.size()]);
			sortListener.sortButtonChanged(this, button.getId(), buttons);
		}
	}
	
	public void setSelectedSortButton(int id, boolean ascending) {
		this.check(id);
		SortButton button = (SortButton)findViewById(id);
		if (button != null) {
			button.setSortAscending(ascending);
		}
	}
	
	private void pushSortHistory(SortButton button) {
		if (button == null)
			return;
		if (sortHistory.size() == 0 || sortHistory.get(0) != button) {
    		sortHistory.addFirst(button);
    		Log.d("sortButtonGroup", "Added: " + button.getSortSQL());
    		while (sortHistory.size() > maxSortFields) {
    			String removed = sortHistory.removeLast().getSortSQL();
    			Log.d("SortButtonGroup", "removing : " + removed);
    		}
		} else {
			Log.d("SortButtonGroup", "Ignored: " + button);
		}
	}

	public void setSelectedSortButton(String sortField, boolean ascending) {
		for (int i=0; i<getChildCount(); i++) {
			View v = getChildAt(i);
			if (v instanceof SortButton) {
				SortButton button = (SortButton)v;
				if (button.getSortField() != null && button.getSortField().equals(sortField)) {
					check(button.getId());
					button.setSortAscending(ascending);
					pushSortHistory(button);
					break;
				}
			}
		}
	}

	SortButton getPreviousButton() {
		return sortHistory.size() > 0 ? sortHistory.getFirst() : null;
	}

	public final void setMaxSortFields(int max) {
		if (max < 1 || max > 32)
			throw new IllegalArgumentException("max must be between [1-32] inclusive");
		this.maxSortFields = max;
	}


}
