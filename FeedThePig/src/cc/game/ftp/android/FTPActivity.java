package cc.game.ftp.android;

import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

public class FTPActivity extends DroidActivity {

    @Override
    protected void onDraw(DroidGraphics g) {
		final int w = g.getViewportWidth();
		final int h = g.getViewportHeight()/2;
		switch (state) {
			case COIN_DROP:
				break;
			case FEEDING:
				break;
			case CHOOSE_TYPE: {
				int [] counts = { Type.PENNY.num, Type.NICKEL.num, Type.DIME.num, Type.QUARTER.num };
				wanted = Type.values()[Utils.chooseRandomFromSet(counts)];
				state = State.FEEDING;
				break;
			}
			case INIT:
				initCoins(w,h);
				state = State.CHOOSE_TYPE;
				break;
		}
		g.clearScreen(GColor.WHITE);
		g.drawImage(bank, 0, h, w, h);
		if (wanted != null) {
			int r = Math.round(wanted.radius);
			g.drawImage(bubble, 5, h, r*4+20, r*4+20);
			g.drawImage(wanted.id, 5+10+r, h+r-20,r*2, r*2);
		}
		for (Coin c : coins) {
			int x = Math.round(c.x - c.t.image.getWidth()/2);
			int y = Math.round(c.y - c.t.image.getHeight()/2);
			//g.setColor(g.TRANSPARENT);
			g.drawImage(c.t.id, x, y, c.t.image.getWidth(), c.t.image.getHeight());
			//AColor old = g.getColor();
			//g.setColor(g.GREEN);
			//g.drawCircle(c.x, c.y, c.t.radius);
			//g.setColor(old);
		}
		g.drawImage(bankBottom, 0, h, w, h);
		//g.setColor(g.WHITE);
		
	}

	@Override
	protected void onInit(DroidGraphics g) {
		int w = g.getViewportWidth();
		int h = g.getViewportHeight();
		
		Type.PENNY.setImage(g.getImage(R.drawable.penny), R.drawable.penny);
		Type.NICKEL.setImage(g.getImage(R.drawable.nickel), R.drawable.nickel);
		Type.DIME.setImage(g.getImage(R.drawable.dime), R.drawable.dime);
		Type.QUARTER.setImage(g.getImage(R.drawable.quarter), R.drawable.quarter);
		bank = R.drawable.piggybank;
		bankBottom = R.drawable.piggybankbottom;
		bubble = R.drawable.bubble;
		g.ortho(0,w,0,h);

//		getRenderer().setTargetFPS(20);
//		getRenderer().setDrawFPS(false);
	}
	
    @Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		String action = "?";
		if (state == State.FEEDING) {
    		switch (event.getAction()) {
    			case MotionEvent.ACTION_DOWN: {
    				action = "DOWN"; 
    				pickCoin(event.getX(), event.getY());
    				break;
    			}
    			case MotionEvent.ACTION_UP:
    				action = "UP"; 
    				picked = null;
    				break;
    				
    			case MotionEvent.ACTION_MOVE:
    				action = "MOVE"; 
    				if (picked == null) {
    					pickCoin(event.getX(), event.getY());
    				} else {
    					picked.x = Math.round(event.getX());
    					picked.y = Math.round(event.getY());
    					processCoinMove(picked);
    				}
    				break;
    			default:
    				Log.w(TAG, "Unhanded action " + event.getAction());
    		}
    		
		}
		lastX = event.getX();
		lastY = event.getY(); 
		Log.d("FTP", "x=" + x + " y=" + y + " action=" + action + " picked=" + picked);
		
		
		return super.onTouchEvent(event);
	}

    enum State {
    	INIT,
    	COIN_DROP,
    	CHOOSE_TYPE,
    	FEEDING,
    }
    
    enum Type {
    	PENNY,
    	NICKEL,
    	DIME,
    	QUARTER;
    	
    	void setImage(AImage i, int id) {
    		this.image = i;
    		this.radius = Math.min(i.getHeight(), i.getWidth()) / 2;
    		this.id = id;
    	}
    	
    	float radius;
    	AImage image;
    	int id;
    	int num = 0;
    }
    
    static class Coin {
    	float x, y;
    	Type t;
    	
    	public String toString() {
    		return t.name() + " x=" + x + " y=" + y;
    	}
    }
    
    private void initCoins(int w, int h) {
    	int num = Utils.rand() % 10 + 10;
    	for (int i=0; i<num; i++) {
    		Coin c = new Coin();
    		c.x = 10 + Utils.rand() % (w-20);
    		c.y = 10 + Utils.rand() % (h-10);
    		c.t = Utils.randItem(Type.values());
    		c.t.num ++;
    		coins.add(c);
    	}
    }
    
    private void pickCoin(float tx, float ty) {
    	picked = null;
    	for (Coin c : coins) {
    		if (Utils.fastLen(tx-c.x + c.t.radius, ty-c.y+c.t.radius) < c.t.radius*2) {
    			picked = c;
    		}
    	}
    }
    
    private void processCoinMove(Coin c) {
        DroidGraphics g = getGraphics();
        if (g == null)
            return;
    	final int h = g.getViewportHeight() / 2;
    	
    	final float x0 = g.getViewportWidth() * SLOT_LEFT;
    	final float x1 = g.getViewportWidth() * SLOT_RIGHT;
    	final float y0 = h + h*SLOT_LEFT_BOTTOM;
    	final float y1 = h + h*SLOT_RIGHT_BOTTOM;
    	
    	if (c.x < x0) {
    		if (c.y + c.t.radius > y0) {
    			c.y = y0 - c.t.radius;
    		}
    	} else if (c.x < x1) {
    		final float r2 = c.t.radius * c.t.radius;
    		final float d2 = Utils.distSqPointLine(c.x, c.y, x0, y0, x1, y1);

    		if (d2 < r2 || c.y > y0) {
    			if (c.t == wanted) {
        			c.t.num--;
        			coins.remove(c);
        			picked = null;
        			wanted = null;
        			if (coins.size() == 0) {
        				state = State.INIT;
        			} else {
        				state = State.CHOOSE_TYPE;
        			}
    			} else {
    				picked = null;
    				c.y = h;
    			}
    		}
    		
    	} else {
    		if (c.y + c.t.radius > y1) {
    			c.y = y1 - c.t.radius;
    		}
    	}
    }
    
    int bank = 0;
    int bankBottom = 0;
    int bubble = 0;
    State state = State.INIT;
    ArrayList<Coin> coins = new ArrayList<Coin>();
    final static String TAG = "FTP";
    Coin picked = null;
    float lastX, lastY;
    Type wanted = null;
    
    final float SLOT_LEFT_BOTTOM = 50f/128;
    final float SLOT_RIGHT_BOTTOM = 37f/128;
    final float SLOT_LEFT = 40f/128;
    final float SLOT_RIGHT = 90f/128;
}
