package cc.android.learningcalc;

import android.content.Context;
import cc.android.learningcalc.CalcActivity.Op;
import cc.lib.game.Utils;

public class Quiz {
	private final int [] buttons = new int[10];
	private int numButtons = 0;
	private String html;
	private final long start = System.currentTimeMillis();
	private int attemptsLeft = 3;
	private String testGoodText = null;
	
	Quiz(String html, int solution) {
		while (solution > 0) {
			buttons[numButtons++] = solution % 10;
			solution = solution/10;
		}
		if (numButtons == 0) {
			buttons[numButtons++] = 0;
		}
		this.html = html;
	}
	
	String getHtml() {
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

		int red = 255;
		int grn = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		int blu = (int)Utils.clamp(scale * 0 + (1f-scale)*255, 0, 255);
		
		int c = red<<16 | grn<<8 | blu;
		String s = String.format(html, Integer.toHexString(c));
		//System.out.println("scale = " + scale + " HTML=" + s);
		return s;
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
			html = html.replaceFirst("[\\?]", String.valueOf(button));
			return TestResult.KEEP_GOING; // more
		} else if (--attemptsLeft > 0) {
			return TestResult.TRY_AGAIN;
		}
		return TestResult.WRONG; // fail
	}
	
	String getTestText() {
		return testGoodText;
	}
	
	String getTestGoodText() {
		return null;
	}
	
	static Quiz generateBasic(Context c, int n1, int n2, Op op) {
		int s  = op.solve(n1, n2);
		String neg = "";
		if (s < 0) {
			neg="-";
			s = -s;
		}
//		handler.sendMessage(handler.obtainMessage(TRIGGER_SAY_REPEAT, "" + n1 + " plus " + n2 + " equals"));
		Quiz quiz = new Quiz(String.format("<b>QUIZ TIME<br/>%d %s %d = <font color=\"#%%s\">%s%s</font>", n1, op.sign(c), n2, neg, Utils.getRepeatingChars('?', String.valueOf(s).length())), s);
		//quiz.testGoodText = String.format("%d %s %d = <font color=\"#%%s\">%s%s</font>", n1, op.tts(), n2, neg, Utils.getRepeatingChars('?', String.valueOf(s).length()));
		return quiz;
	}
	
	static Quiz generate(Context c, int score) {
		if (score < 20) {
			// (1-3) + (1-3) = ?
			//int n1 = Utils.randRange(1, 3);
			//int n2 = Utils.randRange(1, 3);
			//int s  = n1+n2;
//			handler.sendMessage(handler.obtainMessage(TRIGGER_SAY_REPEAT, "" + n1 + " plus " + n2 + " equals"));
			//Quiz quiz = new Quiz("<b>QUIZ TIME<br/>" + n1 + " + " + n2 + " = <font color=\"#%s\">?</font>", s);
			//quiz.testGoodText = "" + n1 + " plus " + n2 + " equals " + s;
			return generateBasic(c, Utils.randRange(1, 3), Utils.randRange(1, 3), Op.ADD);
		} else if (score < 30) {
			// (2-5) + (2-5) = ?
			return generateBasic(c, Utils.randRange(2,5), Utils.randRange(2,5), Op.ADD);
		} else if (score < 40) {
			// (2-4) - (1-3) = ?
			return generateBasic(c, Utils.randRange(3,4), Utils.randRange(1,2), Op.SUB);
		} else if (score < 50) {
			// (3-6) - (2-6) = ?
			return generateBasic(c, Utils.randRange(3,6), Utils.randRange(2,6), Op.SUB);
		} else if (score < 60) {
			// (2-8) +/- (2-8) = ?
			return generateBasic(c, Utils.randRange(2,8), Utils.randRange(2,8), Utils.flipCoin() ? Op.SUB : Op.ADD);
		} else if (score < 70) {
			// (1-3) * (1-3) = ?
			return generateBasic(c, Utils.randRange(1,3), Utils.randRange(1,3), Op.MULT);
		} else if (score < 80) {
			// (2-5) * (2-5) = ?
			return generateBasic(c, Utils.randRange(2,5), Utils.randRange(2,5), Op.MULT);
		} else if (score < 90) {
			// ? + (1-3) = (3-5)
		} else if (score < 100) {
			// (1-3) + ? = (3-5)
		} else if (score < 110) {
			// (3-5) - ? = (1-5)
		} else if (score < 120) {
			// (1-3) ? (1-3) = (1-9)
		} else if (score < 130) {
			// (3-5) / (1-3) = ?
		} else if (score < 140) {
			// (1-9)? + (1-9) = (2-18)
		} else if (score < 150) {
			// ?(0-9) + (1-9) = (1-108)
		} else if (score < 160) {
			// ?(0-9) ? (1-9) = (1-108)
		} else {
			
		}
		return generateBasic(c, Utils.randRange(5, 10), Utils.randRange(5, 10), Utils.randItem(Op.values()));
	}
}