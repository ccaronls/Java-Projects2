package cc.android.learningcalc;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import cc.lib.game.Utils;

public class Quiz {
	private final int [] buttons = new int[10];
	private int numButtons = 0;
	private Spannable question;
	private int qmarkStart, qmarkEnd, answerStart;
	private final long start = System.currentTimeMillis();
	private int attemptsLeft = 3;
	
	Quiz() {}

	Quiz(String question, int solution, int spanStart, int spanLen) {
		this(question, solution);
		qmarkStart = answerStart = spanStart;
		qmarkEnd = qmarkStart + spanLen;
	}

	Quiz(String question, int solution) {
		while (solution > 0) {
			buttons[numButtons++] = solution % 10;
			solution = solution/10;
		}
		if (numButtons == 0) {
			buttons[numButtons++] = 0;
		}
		this.question = new SpannableString(question);
		for ( ; qmarkStart<question.length(); qmarkStart++) {
			if (question.charAt(qmarkStart) == '?') {
				break;
			}
		}
		qmarkEnd = qmarkStart + numButtons;
		answerStart = qmarkStart;
	}
	
	Spannable getSpanned() {
		long dt = System.currentTimeMillis() - start;
		long pulseSpeed = 3000;
		float scale = 0;
		while (dt > pulseSpeed) {
			dt -= pulseSpeed;
		}
		if (dt < pulseSpeed / 2) {
			// forward
			scale = (float)(dt*2)/pulseSpeed;
		} else {
			// reverse
			scale = 1.0f - (float)((dt-pulseSpeed/2)*2)/pulseSpeed;
		}

		/*
		int red = 255;
		int grn = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		int blu = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		
		int c = Color.argb(255, red, grn, blu);
		*/
		//String s = String.format(html, Integer.toHexString(c));
		//System.out.println("scale = " + scale + " HTML=" + s);
		//return s;
		
		int c = Color.argb(Math.round(255*scale), 255, 0, 0);
		
		question.setSpan(new ForegroundColorSpan(c), qmarkStart, qmarkEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		return question;
	}
	
	static final int numbers[] = {
			R.id.button0,
			R.id.button1,
			R.id.button2,
			R.id.button3,
			R.id.button4,
			R.id.button5,
			R.id.button6,
			R.id.button7,
			R.id.button8,
			R.id.button9,
	};

	static final int operations[] = {
			R.id.button_plus,
			R.id.button_minus,
			R.id.button_mult,
			R.id.button_div,
	};
	
	enum TestResult {
		GOOD,
		WRONG,
		KEEP_GOING,
		TRY_AGAIN
	}
	
	TestResult test(int buttonId) {
		int button = buttons[numButtons-1];
		if (buttonId == numbers[button]) {
			numButtons--;
			if (numButtons <= 0) {
				return TestResult.GOOD;
			}
			//html = html.replaceFirst("[\\?]", String.valueOf(button));
			qmarkStart++;
			question.setSpan(new ForegroundColorSpan(Color.GREEN), answerStart, qmarkStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			return TestResult.KEEP_GOING; // more
		} else if (--attemptsLeft > 0) {
			return TestResult.TRY_AGAIN;
		}
		return TestResult.WRONG; // fail
	}
	

}