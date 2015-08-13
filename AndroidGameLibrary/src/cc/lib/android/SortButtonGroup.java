package cc.lib.android;

import android.content.Context;
import android.util.AttributeSet;
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
		void sortButtonChanged(SortButtonGroup group, int checkedId, String sortField, boolean ascending);
	}
	
	private OnSortButtonListener sortListener;
	private SortButton previousButton; 
	
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
		if (sortListener != null) {
			sortListener.sortButtonChanged(this, button.getId(), button.getSortField(), button.isSortAscending());
		}
		previousButton = button;
	}
	
	SortButton getPreviousButton() {
		return this.previousButton;
	}
	
	public void setSelectedSortButton(int id, boolean ascending) {
		this.check(id);
		SortButton button = (SortButton)findViewById(id);
		if (button != null) {
			button.setSortAscending(ascending);
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
					previousButton = button;
					break;
				}
			}
		}
	}

}
