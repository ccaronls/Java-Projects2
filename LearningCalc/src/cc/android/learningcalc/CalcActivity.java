package cc.android.learningcalc;

import java.math.BigInteger;

import cc.lib.android.GLColor;
import cc.lib.game.Utils;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.TextView;

public class CalcActivity extends Activity implements OnClickListener, OnInitListener {

	static final int TRIGGER_NUMBERS_ANIM = -100;
	static final int TRIGGER_OPERATIONS_ANIM = -101;
	static final int TRIGGER_QUIZ = -102;
	static final int TRIGGER_QUIZ_PULSE = -103;
	static final int TRIGGER_SAY_REPEAT = -104;
	
	static int QUIZ_SCORE_STEP = 10;

	private Quiz quiz = null;
	private int score = 0;
	private int nextQuizScore = QUIZ_SCORE_STEP;
	private BigInteger num1, num2;
	private Op op;
	private EditText et;
	private TextToSpeech tts = null;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case TRIGGER_NUMBERS_ANIM:
					triggerPulseAnimNumbersAnim();
					sendEmptyMessageDelayed(TRIGGER_NUMBERS_ANIM, Utils.randRange(8, 16)*1000);
					break;
				case TRIGGER_OPERATIONS_ANIM:
					triggerPulseAnimOperationsAnim();
					sendEmptyMessageDelayed(TRIGGER_OPERATIONS_ANIM, Utils.randRange(8, 16)*1000);
					break;
				case TRIGGER_QUIZ:
					//say("I'm the Quiz! I the Quiz and nobody beats me!");
					say("pop quiz hot shot");
					// say("quiz time")
					quiz = Quiz.generate(CalcActivity.this, score);
					sendEmptyMessage(TRIGGER_QUIZ_PULSE);
					break;
				case TRIGGER_QUIZ_PULSE: {
					if (quiz != null) {
						et.setText(Html.fromHtml(quiz.getHtml()));
						sendEmptyMessageDelayed(TRIGGER_QUIZ_PULSE, 30);
					}
					break;
				}
				case TRIGGER_SAY_REPEAT: {
					say(msg.obj.toString(), true);
					sendMessageDelayed(obtainMessage(TRIGGER_SAY_REPEAT, msg.obj), 5*1000);
					break;
				}
				default:
					findViewById(msg.what).startAnimation(newPulseAnimation(2000, 1, 800, null));

			}
		}
		
	};

	int buttons[] = {
			R.id.button1,
			R.id.button2,
			R.id.button3,
			R.id.button_plus,

			R.id.button4,
			R.id.button5,
			R.id.button6,
			R.id.button_minus,

			R.id.button7,
			R.id.button8,
			R.id.button9,
			R.id.button_mult,

			R.id.button_c,
			R.id.button0,
			R.id.button_eq,
			R.id.button_div,
	};

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.calc);
		et = (EditText)findViewById(R.id.editText1);
		for (int i : buttons) {
			findViewById(i).setOnClickListener(this);
		}
		
		reset();
		if (b != null) {
			et.setText(b.getString("text", ""));
			clearEquation();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("text", et.getText().toString());
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if (tts != null) {
			tts.shutdown();
			tts = null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		if (audio == null) {
			return false;
		}
		
		int type = AudioManager.STREAM_MUSIC;
		int vol = audio.getStreamVolume(type);
		int max = audio.getStreamMaxVolume(type);
		int step = max/10;
		
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				vol = Math.min(max, vol+step);
				audio.setStreamVolume(type, vol, AudioManager.FLAG_SHOW_UI);
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				vol = Math.max(0, vol-step);
				audio.setStreamVolume(type, vol, AudioManager.FLAG_SHOW_UI);
				break;
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				audio.setStreamVolume(type, 0, AudioManager.FLAG_SHOW_UI);
				break;
			default:
				return super.onKeyDown(keyCode, event);
		}
		say("Hello");
		return true; // we handled the event
	}

	void say(String txt) {
		say(txt, false);
	}
	
	void say(String txt, boolean queue) {
		if (tts != null) {
			if (!queue)
				tts.stop();
			tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		tts = new TextToSpeech(this, this);
		tts.setLanguage(getResources().getConfiguration().locale);
		handler.sendEmptyMessage(TRIGGER_NUMBERS_ANIM);
		handler.sendEmptyMessage(TRIGGER_OPERATIONS_ANIM);
	}
	
	void triggerPulseAnimNumbersAnim() {
		handler.sendEmptyMessageDelayed(R.id.button1, 200);
		handler.sendEmptyMessageDelayed(R.id.button2, 200);
		handler.sendEmptyMessageDelayed(R.id.button3, 200);
		handler.sendEmptyMessageDelayed(R.id.button4, 500);
		handler.sendEmptyMessageDelayed(R.id.button5, 500);
		handler.sendEmptyMessageDelayed(R.id.button6, 500);
		handler.sendEmptyMessageDelayed(R.id.button7, 800);
		handler.sendEmptyMessageDelayed(R.id.button8, 800);
		handler.sendEmptyMessageDelayed(R.id.button9, 800);
		handler.sendEmptyMessageDelayed(R.id.button0, 1100);
	}
	void triggerPulseAnimOperationsAnim() {
		handler.sendEmptyMessageDelayed(R.id.button_plus, 2500);
		handler.sendEmptyMessageDelayed(R.id.button_minus, 2500);
		handler.sendEmptyMessageDelayed(R.id.button_mult, 2500);
		handler.sendEmptyMessageDelayed(R.id.button_div, 2500);
		
	}
	
	void endQuiz() {
		quiz = null;
		handler.removeCallbacksAndMessages(null);
		et.setText("");
	}
	
	@Override
	public void onClick(View v) {
		
		if (quiz != null) {
			switch (quiz.test(v.getId())) {
				case GOOD:
					say("great job.");
					score ++;
					nextQuizScore += QUIZ_SCORE_STEP;
					endQuiz();
					break;
				case KEEP_GOING:
					say("Keep going");
					break;
				case TRY_AGAIN:
					say("Try again");
					break;
				case WRONG:
					say("no");
					score-=3;
					endQuiz();
					break;
			}
			return;
		}
		
		switch (v.getId()) {
			case R.id.button1:
				tryAddNumber(1); break;
			case R.id.button2:
				tryAddNumber(2); break;
			case R.id.button3:
				tryAddNumber(3); break;
			case R.id.button_plus:
				trySetOp(Op.ADD); break;

			case R.id.button4:
				tryAddNumber(4); break;
			case R.id.button5:
				tryAddNumber(5); break;
			case R.id.button6:
				tryAddNumber(6); break;
			case R.id.button_minus:
				trySetOp(Op.SUB); break;

			case R.id.button7:
				tryAddNumber(7); break;
			case R.id.button8:
				tryAddNumber(8); break;
			case R.id.button9:
				tryAddNumber(9); break;
			case R.id.button_mult:
				trySetOp(Op.MULT); break;
				
			case R.id.button_c:
				say("clear");
				reset();
				clearEquation();
				break;
			case R.id.button0:
				tryAddNumber(0); break;
			case R.id.button_eq:
				trySolve(); break;
			case R.id.button_div:
				trySetOp(Op.DIV); break;

		}
		
		if (score > 0) {
			((TextView)findViewById(R.id.tvScore)).setText("" + score);
		}

	}
	
	void tryAddNumber(int num) {
		if (op == null) {
			num1 = addNumber(num, num1);
		} else {
			num2 = addNumber(num, num2);
		}
		resetEquation();
	}
	
	BigInteger addNumber(int num, BigInteger target) {
		if (target == null) {
			target = BigInteger.valueOf(num);
		} else {
			target = target.multiply(BigInteger.TEN);
			target = target.add(BigInteger.valueOf(num));
		}
		say(target.toString());
		return target;
	}
	
	void trySetOp(Op op) {
		say(op.tts());
		if (num1 != null) {
			this.op = op;
			resetEquation();
		}
	}
	
	void clearEquation() {
		String line = et.getText().toString();
		int endl = line.indexOf('\n');
		if (endl > 0) {
			line = line.substring(endl);
			et.setText(line);
		} else {
			et.setText("");
		}
	}
	
	void resetEquation() {
		String text = et.getText().toString();
		int endl = text.indexOf('\n');
		if (endl >= 0) {
			text = text.substring(endl);
		} else {
			text = "";
		}
		String line = "";
		if (num1 != null) {
			line += num1.toString();
		}
		if (op != null) {
			line += " ";
			line += op.sign(this);
		}
		if (num2 != null) {
			line += " ";
			line += num2.toString();
		}
		if (line.length() > 0) {
			et.setText(line + text);
		}
	}
	
	void trySolve() {
		if (num2 != null) {
			BigInteger n = op.solve(num1, num2);
			if (n != null) {
				say("equals " + n.toString());
				score += op.ordinal() + 1;
				if (score >= nextQuizScore) {
					handler.sendEmptyMessageDelayed(TRIGGER_QUIZ, 1000);
				}
				reset();
				append(n.toString());
				num1 = n;
			} else {
				reset();
				append("NAN");
				say("Not a number");
			}
		}
	}
	
	void append(String s) {
		if (s == null || s.trim().length() == 0)
			return;
		String line = et.getText().toString();
		et.setText(s + "\n" + line);
	}
	
	void reset() {
		num1=num2=null;
		op = null;
	}
	
	enum Op {
		ADD,SUB,MULT,DIV;
		
		int solve(int a, int b) {
			switch (this) {
				case ADD:
					return a+b;
				case DIV:
					return a/b;
				case MULT:
					return a*b;
				case SUB:
					return a-b;
			}
			return 0;
		}
		
		BigInteger solve(BigInteger a, BigInteger b) {
			try {
    			switch (this) {
    				case ADD:
    					return a.add(b);
    				case DIV:
    					return a.divide(b);
    				case MULT:
    					return a.multiply(b);
    				case SUB:
    					return a.subtract(b);
    				
    			}
			} catch (ArithmeticException e) {
			}
			return null;
		}
		
		String sign(Context c) {
			switch (this) {
				case ADD:
					return "+";
				case DIV:
					return c.getString(R.string.div_symbol);
				case MULT:
					return "X";
				case SUB:
					return "-";
			}
			return "?";
		}
		
		String tts() {
			switch (this) {
				case ADD:
					return "plus";
				case DIV:
					return "divided by";
				case MULT:
					return "times";
				case SUB:
					return "minus";
			}
			return "um";
		}
	}
	
	@Override
	public void onInit(int status) {
		switch (status) {
			case TextToSpeech.SUCCESS:
				say("Hi Sebastian!");
				break;
				
			default:
				Log.e("TTS", "Failed to init.  status=" + status);
		}
		
	}
	
	Animation newPulseAnimation(long delay, int repeat, long duration, AnimationListener listener) {
		ScaleAnimation anim = new ScaleAnimation(1f, 1.1f, 1f, 1.1f);
		anim.setDuration(duration);
		anim.setInterpolator(new LinearInterpolator());
		anim.setRepeatCount(repeat);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setStartTime(AnimationUtils.currentAnimationTimeMillis() + delay);
		anim.setAnimationListener(listener);
		return anim;
	}
	/*
	void generateQuiz() {
		handler.removeCallbacksAndMessages(null);
		if (score < 20) {
			// (1-3) + (1-3) = ?
			int n1 = Utils.randRange(1, 3);
			int n2 = Utils.randRange(1, 3);
			int s  = n1+n2;
			handler.sendMessage(handler.obtainMessage(TRIGGER_SAY_REPEAT, "" + n1 + " plus " + n2 + " equals"));
			this.quiz = new Quiz("<b>QUIZ TIME<br/>" + n1 + " + " + n2 + " = <font color=\"#%s\">?</font>", s);
		} else if (score < 30) {
			// (2-5) + (2-5) = ?
		} else if (score < 40) {
			// (2-4) - (1-3) = ?
		} else if (score < 50) {
			// (3-6) - (2-6) = ?
		} else if (score < 60) {
			// (2-8) +/- (2-8) = ?
		} else if (score < 70) {
			// (1-3) * (1-3) = ?
		} else if (score < 80) {
			// (2-5) * (2-5) = ?
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
	}*/
}
