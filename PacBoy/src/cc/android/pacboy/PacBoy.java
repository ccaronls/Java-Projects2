package cc.android.pacboy;

import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Utils;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public class PacBoy {

	final float [] seperation = { 0, 15, 30, 45, 60, 75, 90, 75, 60, 45, 30, 15, 0 };
	
	final MutableVector2D pos;
	float degrees;
	float targetDegrees;
	float speed;
	int frame;
	float radius = 0.5f;
	float maxSpeed = 0.15f;
	
	PacBoy() {
		this.pos = new MutableVector2D();
		reset();
	}
	
	void reset() {
		speed = 0.01f;
		frame = 0;
		degrees = 0;
		radius = 0.5f;
	}
	
	void draw(AGraphics g) {
		g.pushMatrix();
		g.translate(pos);
		g.rotate(degrees);
		float sep = seperation[frame++ % seperation.length];
		MutableVector2D v = new MutableVector2D(radius, 0);
		float ang = sep/2;
		float step = 15;
		v.rotateEq(ang);
		g.begin();
		g.vertex(0,0);
		while (ang <= 360-sep/2) {
			g.vertex(v);
			v.rotateEq(step);
			ang += step;
		}
		g.drawTriangleFan();
		g.end();
		g.popMatrix();
	}
	
	boolean moveTo(IVector2D v) {
		MutableVector2D dv = Vector2D.newTemp(v);
		dv.subEq(pos);
		degrees = dv.angleOf();
		/* TODO: work in progress
		float dd = targetDegrees - degrees;
		float angSpeed = 500 * speed;
		if (dd > 180) {
			degrees -= angSpeed;
		} else if (dd < -180) {
			degrees += angSpeed;
		} else if (dd < 0) {
			degrees -= angSpeed;
		} else {
			degrees += angSpeed;
		}
		if (degrees < 0) {
			degrees += 360;
		} else if (degrees > 360) {
			degrees -= 360;
		}*/
		float d = dv.mag();
		if (d < speed) {
			pos.assign(v);
            speed = Utils.clamp(speed + 0.01f, 0, maxSpeed);
			return true;
		}
		dv.scaleEq(speed).scaleEq(1.0f / d);
		pos.addEq(dv);
		return false;
	}
	
}
