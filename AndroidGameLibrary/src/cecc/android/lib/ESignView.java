package cecc.android.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ESignView extends View {

	private void init(Context context, AttributeSet attrs) {
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.BLACK);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeWidth(10);
	}
	
	
	public ESignView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	public ESignView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
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
				path.lineTo(event.getX()+1, event.getY()); // this makes sure that taps with no MOVE still produce something
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
		canvas.drawPath(path, paint);
	}

	public void clearSignature() {
		path.reset();
		invalidate();
	}
	
	
}
