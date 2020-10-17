package cc.android.learningcalc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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

import java.math.BigInteger;

import cc.lib.android.CCActivityBase;
import cc.lib.game.Utils;

public class CalcActivity extends CCActivityBase implements OnClickListener, OnInitListener {

	static final int TRIGGER_NUMBERS_ANIM 		= -100;
	static final int TRIGGER_OPERATIONS_ANIM 	= -101;
	static final int TRIGGER_QUIZ 				= -102;
	static final int TRIGGER_QUIZ_PULSE 		= -103;
	static final int TRIGGER_SAY_REPEAT 		= -104;
	static final int TRIGGER_INACTIVITY 		= -105;
	
	static int QUIZ_SCORE_STEP = 10;

	private Quiz quiz = null;
	private int score = 0;
	private int nextQuizScore = QUIZ_SCORE_STEP;
	private BigInteger num1, num2;
	private Op op;
	private EditText et;
	private TextView tvScore;
	private TextToSpeech tts = null;
	private Spannable quizGoodET;
	private String quizGoodSay;
	
	private class MyHandler extends Handler {
	
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
				    switch (Utils.rand() % 3) {
                        case 0:
                            sayNow("I'm the Quiz! I'm the Quiz and nobody beats me!");
                            break;
                        case 1:
                            sayNow("pop quiz hot shot");
                            break;
                        case 2:
                            sayNow("quiz time");
                            break;
                    }
					generate();
					handler.sendEmptyMessage(TRIGGER_QUIZ_PULSE);
					break;
				case TRIGGER_QUIZ_PULSE: {
					if (quiz != null) {
						et.setText(quiz.getSpanned());//Html.fromHtml(quiz.getHtml()));
						handler.sendEmptyMessageDelayed(TRIGGER_QUIZ_PULSE, 30);
					}
					break;
				}
				case TRIGGER_SAY_REPEAT: {
					sayQ(msg.obj.toString());
					sendMessageDelayed(obtainMessage(TRIGGER_SAY_REPEAT, msg.obj), 5*1000);
					break;
				}
				case TRIGGER_INACTIVITY: {
					int sol = Utils.randRange(-100, -10);//Utils.randRange(10, 99*score);
					sayNow("Can you type the number " + sol);
					quizGoodSay = "Great job!";
					quiz = new Quiz(String.format("%d", sol), sol, 0, String.valueOf(sol).length());
					handler.sendEmptyMessage(TRIGGER_QUIZ_PULSE);
					break;
				}
				default:
					findViewById(msg.what).startAnimation(newPulseAnimation(2000, 1, 800, null));

			}
		}
		
	};
	
	private Handler handler = new MyHandler();

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
		tvScore = (TextView)findViewById(R.id.tvScore);
		for (int i : buttons) {
			TextView button = (TextView)findViewById(i);
			button.setOnClickListener(this);
			//button.setText(new AnimatingSpannableString(button));
		}
		
		reset();
		if (b != null) {
			et.setText(b.getString("text", ""));
			clearEquation();
		}
		
		findViewById(R.id.buttonInfo).setOnClickListener(this);
        tts = new TextToSpeech(this, this);
        tts.setLanguage(getResources().getConfiguration().locale);
	}

    @Override
    public void onResume() {
        super.onResume();
        handler.sendEmptyMessage(TRIGGER_NUMBERS_ANIM);
        handler.sendEmptyMessage(TRIGGER_OPERATIONS_ANIM);
    }

    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("text", et.getText().toString());
	}

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
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
		sayNow("Hello");
		return true; // we handled the event
	}

	void sayNow(String txt) {
		if (tts != null && txt != null) {
			tts.stop();
			tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	void sayQ(String txt) {
		if (tts != null && txt != null) {
			tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
		}
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
		handler.sendEmptyMessageDelayed(TRIGGER_INACTIVITY, 20*1000);
		
		et.setText("");
	}
	
	@Override
	public void onClick(View v) {
		
		if (v.getId() == R.id.buttonInfo) {
			showInfoDialog();
			return;
		}
		
		if (v instanceof TextView) {
			TextView tv = (TextView)v;
			tv.setText(new AnimatingSpannableString(tv, 1000, 1, true));
		}
		
		if (quiz != null) {
			switch (quiz.test(v.getId())) {
				case KEEP_GOING:
					sayNow("Keep going");
					break;
				case TRY_AGAIN:
					sayNow("Try again");
					break;
                case GOOD:
                    et.setText(quizGoodET);
                    sayNow(quizGoodSay);
                    incrementScore(1);
                    nextQuizScore += QUIZ_SCORE_STEP;
                    endQuiz();
                    break;
				case WRONG:
					sayNow("incorrect");
					incrementScore(-3);
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
			    if (num1 != null)
				    trySetOp(Op.SUB);
			    else if (et.getText().toString().trim().isEmpty()) {
                    sayNow("negative");
                    et.setText("-");
                }
			    break;

			case R.id.button7:
				tryAddNumber(7); break;
			case R.id.button8:
				tryAddNumber(8); break;
			case R.id.button9:
				tryAddNumber(9); break;
			case R.id.button_mult:
				trySetOp(Op.MULT); break;
				
			case R.id.button_c:
				sayNow("clear");
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
		

	}
	
	void tryAddNumber(int num) {
		if (op == null) {
            if (clearOnNumber) {
                num1 = BigInteger.valueOf(0);
                clearOnNumber = false;
            }
            if (et.getText().toString().startsWith("-") && et.getText().toString().length() == 1)
                num = -num;
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
		sayNow(target.toString());
		return target;
	}
	
	void trySetOp(Op op) {
		sayNow(op.tts());
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

	boolean clearOnNumber = false;
	
	void trySolve() {
		if (num2 != null) {
			BigInteger n = op.solve(num1, num2);
			if (n != null) {
				sayNow("equals " + n.toString());
				incrementScore(op.ordinal() + 1);
				reset();
				append(n.toString());
				num1 = n;
			} else {
				reset();
				append("NAN");
				sayNow("Not a number");
			}
			clearOnNumber = true;
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

	String getGreetings() {
	    String greetings = getPrefs().getString("GREETINGS", null);
	    if (greetings == null) {
	        return getString(R.string.default_greeting);
        }
	    return greetings;
    }

	@Override
	public void onInit(int status) {
		switch (status) {
			case TextToSpeech.SUCCESS:
				sayNow(getGreetings());
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
	
	void generateBasic(int n1, int n2, Op op) {
		int s  = op.solve(n1, n2);
		String neg = "";
		if (s < 0) {
			neg="-";
			s = -s;
		}
		sayQ(String.format("%s %s %s equals what", n1, op.tts(), n2));
		quiz = new Quiz(String.format("QUIZ TIME\n%d %s %d = %s%s", n1, op.sign(this), n2, neg, Utils.getRepeatingChars('?', String.valueOf(s).length())), s);
		quizGoodSay = String.format("%s %s %s equals %s. Great job.", n1, op.sign(this), n2, op.solve(n1, n2));
		quizGoodET = new SpannableString(String.format("%s %s %s = %s", n1, op.sign(this), n2, s));
		quizGoodET.setSpan(new ForegroundColorSpan(Color.GREEN), 0, quizGoodET.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
	
	void generate() {
		if (score < 20) {
			generateBasic(Utils.randRange(1, 3), Utils.randRange(1, 3), Op.ADD);
		} else if (score < 30) {
			// (2-5) + (2-5) = ?
			generateBasic(Utils.randRange(2,5), Utils.randRange(2,5), Op.ADD);
		} else if (score < 40) {
			// (2-4) - (1-3) = ?
			generateBasic(Utils.randRange(3,12), Utils.randRange(5,20), Op.ADD);
		} else if (score < 50) {
			// (3-6) - (2-6) = ?
			generateBasic(Utils.randRange(3,5), Utils.randRange(1,3), Op.SUB);
		} else if (score < 60) {
			// (2-8) +/- (2-8) = ?
            generateBasic(Utils.randRange(5,10), Utils.randRange(3,5), Op.SUB);
		} else if (score < 70) {
			// (1-3) * (1-3) = ?
            generateBasic(Utils.randRange(10,20), Utils.randRange(5,9), Op.SUB);
		} else if (score < 80) {
			// (2-5) * (2-5) = ?
            generateBasic(Utils.randRange(5,20), Utils.randRange(5,20), Utils.flipCoin() ? Op.SUB : Op.ADD);
		} else if (score < 90) {
            generateBasic(Utils.randRange(10,100), Utils.randRange(10,100), Utils.flipCoin() ? Op.SUB : Op.ADD);
			// ? + (1-3) = (3-5)
		} else if (score < 100) {
            generateBasic(Utils.randRange(1,100), Utils.randRange(1,100), Utils.flipCoin() ? Op.SUB : Op.ADD);
			// (1-3) + ? = (3-5)
		} else if (score < 110) {
			// (3-5) - ? = (1-5)
		} else if (score < 120) {
            generateBasic(Utils.randRange(2,5), Utils.randRange(2,5), Op.MULT);
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
            generateBasic(Utils.randRange(0,1000), Utils.randRange(0,1000), Utils.randItem(Op.values()));
        }
        et.setText(quiz.getSpanned());
	}
	
	private void incrementScore(int amt) {
		//TextView tv = ((TextView)findViewById(R.id.tvScore)).setText("" + score);
		score = Math.max(0, score += amt);
		if (score >= nextQuizScore) {
			handler.sendEmptyMessageDelayed(TRIGGER_QUIZ, 1000);
		} else if (score < nextQuizScore-QUIZ_SCORE_STEP){
			nextQuizScore -= QUIZ_SCORE_STEP;
		}
		
		if (score > 0) {
			tvScore.setText(String.format("%d (%d)", score, nextQuizScore));
		} else {
			tvScore.setText("");
		}
	}

	boolean isQuizEnabled() {
	    return getPrefs().getBoolean("QUIZ_ENABLED", true);
    }

    void setQuizEnabled(boolean enabled) {
	    getPrefs().edit().putBoolean("QUIZ_ENABLED", enabled).apply();
    }

	void showInfoDialog() {
        newDialogBuilder().setItems(R.array.options_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        newDialogBuilder().setMessage(getString(R.string.about_msg, getAppVersionFromManifest()))
                                .setNeutralButton(R.string.cancel, null).show();
                        break;

                    case 1:
                        final View v = View.inflate(CalcActivity.this, R.layout.edit_greetings_view, null);
                        final EditText et = (EditText)v.findViewById(R.id.edit_greetings);
                        et.setText(getPrefs().getString("GREETINGS", ""), TextView.BufferType.NORMAL);
                        et.selectAll();
                        newDialogBuilder().setView(v)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String text = et.getText().toString();
                                        if (text.isEmpty()) {
                                            getPrefs().edit().remove("GREETINGS").apply();
                                        } else {
                                            getPrefs().edit().putString("GREETINGS", text).apply();
                                        }
                                    }
                                }).show();
                        break;
                    case 2:
                        newDialogBuilder().setMessage(R.string.quiz_dialog_msg)
                                .setNegativeButton(R.string.disable, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setQuizEnabled(false);
                                    }
                                }).setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setQuizEnabled(true);
                                    }
                                }).show();
                        break;
                }
            }
        }).show();
		
	}

}
