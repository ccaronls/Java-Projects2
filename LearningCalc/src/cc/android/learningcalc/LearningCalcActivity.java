package cc.android.learningcalc;

import java.math.BigInteger;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class LearningCalcActivity extends Activity implements OnClickListener {

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
				if (num1 == null)
					num1 = BigInteger.valueOf(num);
				else {
					num1 = num1.multiply(BigInteger.TEN);
					num1 = num1.add(BigInteger.valueOf(num));
				}
				resetEquation();
				break;
			case NUM2:
				if (num2 == null) {
					num2 = BigInteger.valueOf(num);
				} else {
					num2 = num2.multiply(BigInteger.TEN);
					num2 = num2.add(BigInteger.valueOf(num));
				}
				resetEquation();
				break;
		}
	}
	
	void trySetOp(Op op) {
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
			line += op.sign();
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
	
	BigInteger num1, num2;
	Op op;
	State state;
	EditText et;
	
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
		
		String sign() {
			switch (this) {
				case ADD:
					return "+";
				case DIV:
					return "/";
				case MULT:
					return "X";
				case SUB:
					return "-";
			}
			return "?";
		}
	}
	
	enum State {
		NUM1,
		NUM2,
	}
	
}
