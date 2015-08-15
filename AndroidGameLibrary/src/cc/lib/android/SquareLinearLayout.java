package cc.lib.android;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLinearLayout extends LinearLayout {

	public SquareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SquareLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareLinearLayout(Context context) {
		super(context);
	}

	@Override 
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    int width = MeasureSpec.getSize(widthMeasureSpec);
	    int height = MeasureSpec.getSize(heightMeasureSpec);
	    int size = width > height ? width : height;
	    setMeasuredDimension(size, size);
	}
}
