package cc.android.learningcalc;

import android.graphics.Color;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class AnimatingSpannableString extends SpannableString implements Runnable {

	final int repeats;
	private long startTime;
	private float scale;
	final int periodMsecs;
	final boolean pulse;
	final TextView parent;
	static int [] colors = {
		Color.BLACK,
		Color.BLUE,
		Color.CYAN,
	};
	
	/*
		Color.RED,
		Color.BLUE,
		Color.YELLOW,
		Color.GREEN,
		Color.MAGENTA,
		Color.CYAN
	};//*/

	public AnimatingSpannableString(final TextView view, int periodMsecs, int repeats, boolean pulse) {
		super(view.getText());
		parent = view;
		this.periodMsecs = periodMsecs;
		this.repeats = repeats;
		this.pulse = pulse;
		startTime = AnimationUtils.currentAnimationTimeMillis();
		initSpan();
		view.post(this);
	}
	
	protected void initSpan() {
		setSpan(new CharacterStyle() {
			
			@Override
			public void updateDrawState(TextPaint tp) {
				int maxRadius = Math.min(parent.getWidth(), parent.getHeight());
				int minRadius = Math.max(1,  maxRadius/2);

				float radius = scale * (maxRadius - minRadius);
				if (radius > 0) {
					int cx = parent.getWidth() / 2;
					int cy = parent.getHeight() / 2;
					tp.setShader(new RadialGradient(cx, cy, radius, colors, null, Shader.TileMode.MIRROR));
					
					tp.setTextScaleX(1.0f + scale/2);
				}	
			}
		}, 0, length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
	
	public AnimatingSpannableString(final TextView view) {
		this(view, 1000, -1, false);
	}

	@Override 
	public void run() {
		float dt = (float)(AnimationUtils.currentAnimationTimeMillis() - startTime);
		int count = 0;
		while (dt > periodMsecs) {
			dt -= periodMsecs;
			if (++count >= repeats) {
				parent.setText(parent.getText().toString());
				return;
			}
		}
		
		if (pulse) {
			if (dt < periodMsecs/2) {
				scale = dt*2/periodMsecs;
			} else {
				scale = ((dt-periodMsecs/2)*2)/periodMsecs;
			}
		} else {
			scale = dt/periodMsecs;
		}
		parent.invalidate();
		parent.postDelayed(this, 20);
	}
	
}
