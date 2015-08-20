package cc.android.photooverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ESignView extends View {

	public ESignView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ESignView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ESignView(Context context) {
		super(context);
	}

	private final Path path = new Path(); 
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				path.moveTo(event.getX(), event.getY());
				break;
				
			case MotionEvent.ACTION_MOVE:
				path.lineTo(event.getX(), event.getY());
				break;
				
			case MotionEvent.ACTION_UP:
				path.lineTo(event.getX(), event.getY());
				break;
		}
		
		invalidate();
		return true;
	}

	private final Paint paint = new Paint();
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.BLACK);
		paint.setStrokeWidth(10);
		canvas.drawPath(path, paint);
	}

	public void clearSignature() {
		path.reset();
		invalidate();
	}
	
	
}
