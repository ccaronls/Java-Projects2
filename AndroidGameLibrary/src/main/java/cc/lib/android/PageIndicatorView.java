package cc.lib.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.ViewPager;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

/**
 * This view will draw a series of dots, one of which will be highlighted.  This is typically
 * used in conjunction with a ViewPager to show which page you are on.
 *
 * Can be added as a pageChangeListener to a view pager to automatically track changes.
 */
public class PageIndicatorView extends View implements ViewPager.OnPageChangeListener {

	private int pageCount = 10;
	private float spacingFraction = 0.5f; // spacing as a fraction of dimension
	private int pageColorOn;
	private int pageColorOff;
	private float dim;
	private int currentPage = 0;
	private boolean computeDim = true;
	private Paint paint = new Paint();
	private int viewPagerId = 0;
	
	private void init(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.PageIndicatorView);
		dim = a.getDimension(R.styleable.PageIndicatorView_dotDimension, 0);
		if (dim > 0) {
			computeDim = false;
		}
		pageCount = a.getInt(R.styleable.PageIndicatorView_pageCount, pageCount);
		spacingFraction = a.getFloat(R.styleable.PageIndicatorView_pagePadding, spacingFraction);
		pageColorOn = a.getColor(R.styleable.PageIndicatorView_pageColorOn, Color.WHITE);
		int defaultOffColor = Color.argb(128, Color.red(pageColorOn), Color.green(pageColorOn), Color.blue(pageColorOn));
		pageColorOff = a.getColor(R.styleable.PageIndicatorView_pageColorOff, defaultOffColor);
		viewPagerId = a.getResourceId(R.styleable.PageIndicatorView_viewPagerId, 0);
		a.recycle();
	}

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (viewPagerId != 0) {
            ViewGroup parent = (ViewGroup)getParent();
            while (parent != null) {
                ViewPager pgr = parent.findViewById(viewPagerId);
                if (pgr != null) {
                    startTrackingPageAdapter(pgr);
                    return;
                }
                parent = (ViewGroup)parent.getParent();
            }
        }
    }



    public PageIndicatorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public PageIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public PageIndicatorView(Context context) {
		super(context);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//ADTLog.log(this,  "---------------------- onDraw");
		// just draw the white lines
		float spacing = dim*spacingFraction;
		float cx = getWidth() / 2 - (dim*pageCount)/2 - (spacing*(pageCount-1))/2 + dim/2;
		float cy = getHeight() / 2;
		paint.setColor(pageColorOff);
		paint.setStyle(Style.FILL);
		for (int i=0; i<currentPage; i++) {
			canvas.drawCircle(cx, cy, dim/2, paint);
			cx += dim + spacing;
		}
		paint.setColor(pageColorOn);
		canvas.drawCircle(cx, cy, dim/2, paint);
		cx += dim + spacing;
		paint.setColor(pageColorOff);
		for (int i=currentPage+1; i<pageCount; i++) {
			canvas.drawCircle(cx, cy, dim/2, paint);
			cx += dim + spacing;
		}
	}
	
	private float getWidthFromDim() {
		return Math.round(dim*pageCount+(dim*spacingFraction*(pageCount-1)));
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

	    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	    float widthSize = MeasureSpec.getSize(widthMeasureSpec);
	    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	    float heightSize = MeasureSpec.getSize(heightMeasureSpec);

	    float width=0;
	    float height=0;

	    final float maxDim = dim > 0 ? dim : Math.round(widthSize/(pageCount+(spacingFraction*(pageCount-1))));

	    switch (widthMode) {
	    case EXACTLY:
	    	// dim is the best fit given the width
	    	width = widthSize;
	    	switch (heightMode) {
		    case EXACTLY:
		    	height = heightSize;
		    	if (computeDim)
		    		dim = Math.min(height,  maxDim);
		    	break;
		    case AT_MOST:
		    case UNSPECIFIED:
		    	height = Math.min(heightSize, maxDim);
		    	if (computeDim)
		    		dim = Math.min(height, maxDim);
		    	break;
	    	}
	    	break;
	    case UNSPECIFIED:
	    case AT_MOST:
	    	switch (heightMode) {
		    case EXACTLY:
		    	height = heightSize;
		    	if (computeDim)
		    		dim = Math.min(maxDim,  height);
		    	break;
		    case AT_MOST:
		    	height = Math.min(heightSize, maxDim);
		    	if (computeDim)
		    		dim = Math.min(height, maxDim);
		    	break;
		    case UNSPECIFIED:
		    	height = maxDim;
		    	if (computeDim)
		    		dim = maxDim;
		    	break;
	    	}
	    	width = Math.min(widthSize, getWidthFromDim());
	    }
	    setMeasuredDimension(Math.round(width), Math.round(height));
	}

	public final int getPageCount() {
		return pageCount;
	}

	public final void setPageCount(int pageCount) {
        if (this.pageCount != pageCount) {
            this.pageCount = Math.max(pageCount, 1);
            setVisibility(pageCount < 2 ? View.INVISIBLE : View.VISIBLE);
            postInvalidate();
        }
	}

	public final int getCurrentPage() {
		return currentPage;
	}

	public final void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
		postInvalidate();
	}

	private ViewPager pager = null;

    private void trySetCount() {
        if (pager != null && pager.getAdapter() != null) {
            setPageCount(pager.getAdapter().getCount());
        }
    }

    public void startTrackingPageAdapter(ViewPager pager) {
        pager.addOnPageChangeListener(this);
        this.pager = pager;
        trySetCount();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        trySetCount();
        setCurrentPage(position);
    }

    @Override
    public void onPageSelected(int position) {
        trySetCount();
        setCurrentPage(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        trySetCount();
    }
}
