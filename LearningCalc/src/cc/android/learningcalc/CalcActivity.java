package cc.android.learningcalc;

import java.math.BigInteger;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class CalcActivity extends Activity implements OnClickListener, OnInitListener {

	private BigInteger num1, num2;
	private Op op;
	private State state;
	private EditText et;
	private TextToSpeech tts = null;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.calc);
		et = (EditText)findViewById(R.id.editText1);
		findViewById(R.id.button1).setOnClickListener(this);
		findViewById(R.id.button2).setOnClickListener(this);
		findViewById(R.id.button3).setOnClickListener(this);
		findViewById(R.id.button_plus).setOnClickListener(this);

		findViewById(R.id.button4).setOnClickListener(this);
		findViewById(R.id.button5).setOnClickListener(this);
		findViewById(R.id.button6).setOnClickListener(this);
		findViewById(R.id.button_minus).setOnClickListener(this);

		findViewById(R.id.button7).setOnClickListener(this);
		findViewById(R.id.button8).setOnClickListener(this);
		findViewById(R.id.button9).setOnClickListener(this);
		findViewById(R.id.button_mult).setOnClickListener(this);

		findViewById(R.id.button_c).setOnClickListener(this);
		findViewById(R.id.button0).setOnClickListener(this);
		findViewById(R.id.button_eq).setOnClickListener(this);
		findViewById(R.id.button_div).setOnClickListener(this);
		
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
		if (tts != null) {
			tts.stop();
			tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		tts = new TextToSpeech(this, this);
		tts.setLanguage(getResources().getConfiguration().locale);
	}
	
	@Override
	public void onClick(View v) {
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
	}
	
	void tryAddNumber(int num) {
		switch (state) {
			case NUM1:
				num1 = addNumber(num, num1);
				resetEquation();
				break;
			case NUM2:
				num2 = addNumber(num, num2);
				resetEquation();
				break;
		}
	}
	
	BigInteger addNumber(int num, BigInteger target) {
		if (target == null) {
			target = BigInteger.valueOf(num);
		} else {
			target = target.multiply(BigInteger.TEN);
			target = target.add(BigInteger.valueOf(num));
		}
//		if (target.compareTo(BigInteger.valueOf(10000)) < 0)
		say(target.toString());
		return target;
	}
	
	void trySetOp(Op op) {
		say(op.tts());
		if (state == State.NUM1 && num1 != null) {
			this.op = op;
			resetEquation();
			state = State.NUM2;
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
		if (state == State.NUM2 && num2 != null) {
			BigInteger n = op.solve(num1, num2);
			if (n != null) {
				say("equals " + n.toString());
				reset();
				append(n.toString());
				num1 = n;
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
		state = State.NUM1;
	}
	
	enum Op {
		ADD,SUB,MULT,DIV;
		
		BigInteger solve(BigInteger a, BigInteger b) {
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
	
	enum State {
		NUM1,
		NUM2,
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
	
}
