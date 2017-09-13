package cc.android.sebigames.tictactoe;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TTTView extends View {

	private int color;
	private int shape;
	private float insets;
	private Paint p = new Paint();
	
	private void init(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.TTTView);
		shape = a.getInt(R.styleable.TTTView_shape, 0);
		color = a.getColor(R.styleable.TTTView_color, 0);
		p.setStrokeWidth(a.getDimension(R.styleable.TTTView_strokeWidth, 10f));
		insets = a.getDimension(R.styleable.TTTView_insets, 10f);
		a.recycle();
		p.setStyle(Style.STROKE);
	}
	
	public TTTView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public TTTView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public TTTView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (getBackground() != null) {
			getBackground().draw(canvas);
		}
		p.setColor(color);
		float x0 = insets;
		float x1 = getWidth()-insets;
		float y0 = insets;
		float y1 = getHeight()-insets;
		
		switch (shape) {
			case 1: // X
				canvas.drawLine(x0,y0, x1,y1, p);
				canvas.drawLine(x1,y0, x0,y1, p);
				break;
			case 2: // O
				RectF r = new RectF(x0,y0,x1,y1);
				canvas.drawOval(r, p);
				break;
		}
	}

	public final int getColor() {
		return color;
	}

	public final void setColor(int color) {
		this.color = color;
		invalidate();
	}

	public final int getShape() {
		return shape;
	}

	public final void setShape(int shape) {
		this.shape = shape;
		invalidate();
	}
	
}
