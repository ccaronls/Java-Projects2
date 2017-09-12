package cc.android.sebigames.calc;

import java.math.BigInteger;

import cc.android.sebigames.R;
import android.content.Context;

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