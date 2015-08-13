package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.view.View;

/**
 * See SortButtonGroup for details on how to use.
 * 
 * @author chriscaron
 *
 */
public final class SortButton extends RadioButton implements View.OnClickListener {

	private void init(Context context, AttributeSet attrs) {
		TypedArray arr = context.obtainStyledAttributes( attrs, R.styleable.SortButton);
		sortAscending = arr.getBoolean(R.styleable.SortButton_sortAscending, false);
		sortField = arr.getString(R.styleable.SortButton_sortField);
		arr.recycle();
		super.setOnClickListener(this);
	}
	
	private final static int [] SORT_ASCENDING = { R.attr.sortAscending };
	
	private String sortField;
	private boolean sortAscending;
	
	public SortButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public SortButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public SortButton(Context context) {
		super(context);
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public void setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
		invalidate();
	}

	public String getSortField() {
		if (sortField == null)
			return getText().toString();
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int [] state = super.onCreateDrawableState(extraSpace+1);
		if (isSortAscending())
			mergeDrawableStates(state, SORT_ASCENDING);
		return state;
	}

	@Override
	public void onClick(View v) {
		SortButtonGroup parent = ((SortButtonGroup)getParent());
		if (parent.getPreviousButton() == this) {
			sortAscending = !sortAscending;
		} else if (parent.getPreviousButton() != null){
			sortAscending = parent.getPreviousButton().isSortAscending();
		}
		parent.triggerSort(this);
	}

	@Override
	protected void onAttachedToWindow() {
		if (!(getParent() instanceof SortButtonGroup)) {
			throw new RuntimeException("SortButton can only be a child of SortButtonGroup");
		}
	}
	
	@Override
	public void setOnClickListener(View.OnClickListener listener) {
		throw new RuntimeException("Cannot override onClickListener of SortButton, use SortButtonGroup.OnSortButtonListener instead");
	}
}
