package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SquareLinearLayout extends LinearLayout {

	public enum DominantSide {
		HEIGHT_MATCHES_WIDTH,
		WIDTH_MATCHES_HEIGHT
	}
	
	private DominantSide dominantSide = DominantSide.HEIGHT_MATCHES_WIDTH;
	
	private void init(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.SquareLinearLayout );
		dominantSide = DominantSide.values()[a.getInt(R.styleable.SquareLinearLayout_dominantSide, dominantSide.ordinal())];
        a.recycle();
	}
	
	public SquareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public SquareLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public SquareLinearLayout(Context context) {
		super(context);
	}

	@Override 
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    int width = MeasureSpec.getSize(widthMeasureSpec);
	    int height = MeasureSpec.getSize(heightMeasureSpec);
	    int size = 0;
	    switch (dominantSide) {
			case HEIGHT_MATCHES_WIDTH:
				size = width;
				break;
			case WIDTH_MATCHES_HEIGHT:
				size = height;
				break;
	    }
	    super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
	            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));	    
	}
}
