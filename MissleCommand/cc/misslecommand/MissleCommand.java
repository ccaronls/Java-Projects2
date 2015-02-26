package cc.misslecommand;

import java.awt.event.KeyEvent;

import cc.lib.swing.AWTGraphics;
import cc.lib.swing.EZFrame;
import cc.lib.swing.ImageMgr;
import cc.lib.swing.KeyboardAnimationApplet;
import cc.lib.game.*;

public class MissleCommand extends KeyboardAnimationApplet {

	
	// --------------------------------------------------------------
	// MAIN - DEBUGGING ENABLED
	// In Applet mode, debugging is off by default
	// --------------------------------------------------------------
	
	public static void main(String [] args) {
		Utils.DEBUG_ENABLED = true;
		EZFrame frame = new EZFrame("Missle Command DEBUG MODE");
		MissleCommand mc = new MissleCommand();
		frame.add(mc);
		frame.centerToScreen(820, 620);
		mc.init();
		mc.start();
	}
	
	
	// --------------------------------------------------------------
	// INHERITED METHODS
	// --------------------------------------------------------------

	public void doInitialization() {
		Utils.initTable(enemyMissles, Missle.class);
		Utils.initTable(playerMissles, Missle.class);
		Utils.initTable(enemyExplosions, Explosion.class);
		Utils.initTable(playerExplosions, Explosion.class);
		Utils.initTable(cities, City.class);
	}
	
	public void initGraphics(AGraphics g) {
	    initColors(g);
		initGameStateGetReady(getScreenWidth(), getScreenHeight());
	}
	
	@Override
	public void drawFrame(AWTGraphics g) {

	    if (colors == null) {
	        initGraphics(g);
	    }
	    
		final int width = getScreenWidth();
		final int height = getScreenHeight();
		
		g.setColor(getSkyColor());
		g.drawFilledRect(0,0,width, height);
		switch (gameState) {
		case GAME_STATE_GET_READY:
			drawLand(g);
			drawCities(g);			
			if (getFrameNumber() > 30)
				initGameStatePlay();
			g.setColor(getTextColor());			
			g.drawJustifiedString(width/2,height/2, Justify.CENTER, Justify.CENTER, 
					"GET READY!\n" + "Level " + currentLevel);
			break;
			
		case GAME_STATE_PLAY:
			checkPlayerInput();
			this.drawMissles(g);
			this.drawExplosions(g);
			drawLand(g);
			drawCities(g);
			break;
			
		case GAME_STATE_LEVEL_OVER:
			drawLand(g);
			drawSummary(g);
			if (getFrameNumber() > 100)
				initNextLevel();
			break;
			
		case GAME_STATE_GAME_OVER:
			break;
			
		default:
			//initGameStateGetReady();
			break;
		}
		if (Utils.DEBUG_ENABLED) {
			g.setColor(this.getEnemyMissleColor());
			fillCircle(g, getMouseX(), this.getLandHeight(width, getMouseX()), 3);			
		}
	}
	
	private boolean qPressed = false;
	public void keyPressed(KeyEvent ev) {
		// make so 2 consecutive Q presses quit the game
		if (ev.getKeyCode() == KeyEvent.VK_Q) {
			if (qPressed)
				System.exit(0);
			else {
				qPressed = true;
				return;
			}
		}
		qPressed = false;

		// look for:
		// R restart level
		// L launch wave
		// A,S,D fire missle
		if (ev.getKeyCode() == KeyEvent.VK_R) {}
			//this.initGameStateGetReady(g, getScreenWidth(), getScreenHeight());
		else if (ev.getKeyCode() == KeyEvent.VK_L)
			this.startMissleWave();
		
		int [] cityKeys = { KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D };
		
		for (int i=0; i<cityKeys.length; i++) {
			if (ev.getKeyCode() == cityKeys[i] && getMissleCity(i).numMissles > 0) {
				this.startPlayerMissle(this.getMissleCity(i));
			}
		}
		
	}
	
	public void onDimensionsChanged(AGraphics g, int width, int height) {
		initCities(width, false);
	}
	
	// --------------------------------------------------------------
	// METHODS
	// --------------------------------------------------------------
	
	City getMissleCity(int num) {
		switch (num) {
		case 0: return cities[0];
		case 1: return cities[2];
		case 2: return cities[5];
		default: Utils.unhandledCase(num);
		}
		return null;
	}
	
	void checkPlayerInput() {
		City mc = null;
		for (int i=0; i<3; i++) {
			if (getMouseButtonClicked(i) && getMissleCity(i).numMissles > 0) {
				mc = getMissleCity(i);
			}
		}
		if (mc != null) {
			startPlayerMissle(mc);
		}
	}
	
	// init 
	void initGameStateGetReady(int width, int height) {
		gameState = GAME_STATE_GET_READY;
		setFrameNumber(0);
		initColors();
		initLand(height);
		initCities(width, true);
	}
	
	void initImages(AGraphics g) {
		int cityId = g.loadImage("city.gif", g.BLACK);
		//Image src = images.getSourceImage(cityId);
		cityImageIds[0] = g.newSubImage(cityId, 0, 0, 64, 64);
		cityImageIds[1] = g.newSubImage(cityId, 64, 0, 64, 64);
		cityImageIds[2] = g.newSubImage(cityId, 0, 64, 64, 64);
		cityImageIds[3] = g.newSubImage(cityId, 64, 64, 64, 64);
		for (int i=0; i<4; i++) {
		    //cityImageIds[i] = g.getImage(cityImageIds[i], getCityRadius(), getCityRadius());
		    cityImageIds[i] = g.newTransformedImage(cityImageIds[i], new ImageColorFilter(g.WHITE, getCityColor(), 0));
		}
	}
	
	void initGameStatePlay() {
		Utils.println("initGameStatePlay");
		gameState = GAME_STATE_PLAY;
		
	}

	void initColors() {
		Utils.shuffle(colors);
	}
	
	void initLand(int screenHeight) {
		for (int i=0; i<landYFactor.length; i++) {
			landYFactor[i] = Utils.randFloat(1);
			Utils.print("[%f]", landYFactor[i]);
		}
		Utils.println();
	}
	
	int getStartNumMissles() {
		return 10;
	}
	
	void initCities(int screenWidth, boolean resetMissles) {
		if (cities[0] == null)
			return;
		final int dx = screenWidth / (cities.length);
		int x = dx/2;
		for (int i=0; i<cities.length; i++) {
			cities[i].x = x + Utils.randRange(-dx/4, dx/4);
			cities[i].y = this.getLandHeight(screenWidth, cities[i].x);
			if (resetMissles)
				cities[i].numMissles = 0;
			x += dx;
		}	
		if (resetMissles) {
			for (int i=0; i<3; i++) {
				getMissleCity(i).numMissles = getStartNumMissles();
			}
		}
	}
	
	void startPlayerMissle(City mc) {
		assert(mc != null && mc.numMissles > 0);
		mc.numMissles --;
		this.addMissle(playerMissles, mc.x, mc.y, this.getMouseX(), getMouseY(), getPlayerMissleSpeed());
	}
	
	void startMissleWave() {
		int numStartMissles = Utils.randRange(5+currentLevel, 10+currentLevel*2);
		for (int i=0; i<numStartMissles; i++) {
			int sx = Utils.randRange(10, getScreenWidth()-10);
			int sy = 0;
			int ex = Utils.randRange(10, getScreenWidth()-10);
			int ey = getLandHeight(getScreenWidth(), ex);
			this.addMissle(enemyMissles, sx, sy, ex, ey, getRandomEnemyMissleSpeed());
		}
	}
	
	void initNextLevel() {
		initLand(getScreenHeight());
		initCities(getScreenWidth(), true);
	}
	
	float getLandY(int index) {
		float height = getScreenHeight();
		float minLandY = height-(height*0.1f);
		float maxLandY = height-(height*0.25f);
		return minLandY + (maxLandY-minLandY)*landYFactor[index];
	}
	
	int getLandHeight(float width, float x) {
		
		
		// return
		final float dx = width/(landYFactor.length-1);
		int i=1;
		for ( ; i<this.landYFactor.length-1; i++) {
			if (x <= dx)
				break;
			x -= dx;
		}
		int first = i-1;
		int last  = i;
		float y0 = getLandY(first);
		float y1 = getLandY(last);
		
		float m  = (y1-y0)/dx; // slope
		int landHeight = Math.round(y0 + m*x);
		return landHeight;
	}

	AColor getSkyColor() 			{ return colors[0]; }	
	AColor getLandColor() 			{ return colors[1]; }	
	AColor getEnemyMissleColor() 	{ return colors[2]; }	
	AColor getPlayerMissleColor() 	{ return colors[3]; }	
	AColor getExplosionColor() 		{ return colors[4]; }	
	AColor getCityColor() 			{ return colors[5]; }	
	AColor getMissleHeadColor() 		{ return colors[6]; }	
	AColor getTextColor() 			{ return colors[7]; }
	
	boolean drawMissle(AGraphics g, Missle m, AColor color, int missleSpeed) {
		
		float dx = m.ex - m.sx;
		float dy = m.ey - m.sy;
		float dist = (float)Math.sqrt(dx*dx + dy*dy);
		
		if (dist<1)
			return false;
		
		double distInv = 1.0/dist;
		
		dx *= distInv;
		dy *= distInv;
		
		int frames = this.getFrameNumber() - m.startFrame;
		
		int len = frames*missleSpeed;
		
		g.setColor(color);
		m.nx = m.sx + Math.round(dx * len);
		m.ny = m.sy + Math.round(dy * len);
		
		g.drawLine(m.sx, m.sy, m.nx, m.ny, 2);
		g.setColor(getMissleHeadColor());
		
		this.fillCircle(g, m.nx, m.ny, 2);
		
		if (len >= dist) {
			return true;
		}
		return false;
	}
	
	void startPlayerExplosion(int x, int y) {
		for (int i=0; i<playerExplosions.length; i++) {
			if (playerExplosions[i].startFrame < 0) {
				playerExplosions[i].startFrame = getFrameNumber();
				playerExplosions[i].x = x;
				playerExplosions[i].y = y;
				break;
			}
		}
	}
	
	void startEnemyExplosion(int x, int y) {
		for (int i=0; i<enemyExplosions.length; i++) {
			if (enemyExplosions[i].startFrame < 0) {
				enemyExplosions[i].startFrame = getFrameNumber();
				enemyExplosions[i].x = x;
				enemyExplosions[i].y = y;
				break;
			}
		}
	}
	
	int getNumCitiesLeft() {
		int num = 0;
		for (int i=0; i<cities.length; i++)
			if (cities[i].numMissles>=0)
				num++;
		return num;				
	}
	
	//int getEnemyMissleSpeed() {
	//	return currentLevel < 10 ? currentLevel : 10;
	//}
	
	int getRandomEnemyMissleSpeed() {
		return Utils.randRange(currentLevel, currentLevel+2);
	}
	
	int getPlayerMissleSpeed() {
		return 10;
	}
	
	int getPointsPerCity() {
		return 500;
	}
	
	int getPointsPerMissle() {
		return 10;
	}
	
	void drawSummary(AGraphics g) {
		int numCities = 0;
		int numMissles = 0;
		for (int i=0; i<cities.length; i++) {
			if (cities[i].numMissles<0)
				continue;
			numCities ++;
			numMissles += cities[i].numMissles;
		}
		
		int cityPoints = numCities * getPointsPerCity();
		int misslePoints = numMissles * getPointsPerMissle();
		int totalPoints  = cityPoints + misslePoints;
		
		String summary = "Level " + currentLevel + " Complete"
					   + "\n\nCities X " + numCities + " Bonus: " + cityPoints
					   + "\nMissles X " + numMissles + " Bonus: " + misslePoints
					   + "\n\nTOTAL " + totalPoints;
		
		g.setColor(getTextColor());
		g.drawJustifiedString( getScreenWidth()/2, getScreenHeight()/2, Justify.CENTER, Justify.CENTER, summary);
	}
	
	void drawPlayerMissles(AGraphics g) {
		for (int i=0; i<playerMissles.length; i++) {
			if (playerMissles[i].startFrame < 0)
				continue;
			if (drawMissle(g, playerMissles[i], getPlayerMissleColor(), getPlayerMissleSpeed())) {
				playerMissles[i].startFrame = -1; // mark frame as not used
				startPlayerExplosion(playerMissles[i].ex, playerMissles[i].ey);
			}
		}
	}
	
	void startMissleSpread(int x, int y) {
		int numMissles = Utils.randRange(2, 2+currentLevel);
		for (int i=0; i<numMissles; i++) {
			int ex = Utils.rand() % getScreenWidth();
			int ey = this.getLandHeight(getScreenWidth(), ex);
			this.addMissle(enemyMissles, x, y, ex, ey, getRandomEnemyMissleSpeed()+2);
		}
	}
	
	void drawEnemyMissles(AGraphics g) {
		for (int i=0; i<enemyMissles.length; i++) {
			Missle m = enemyMissles[i];
			if (m.startFrame < 0)
				continue;
			if (drawMissle(g, m, getEnemyMissleColor(), m.speed)) {
				m.startFrame = -1; // mark frame as not used
				
				if (m.ny < getScreenHeight()*3/4) {				
					startEnemyExplosion(enemyMissles[i].ex, enemyMissles[i].ey);
				} else {
					// spawn missles from here
					startMissleSpread(m.nx, m.ny);
				}
			}
		}
	}
	
	void drawMissles(AGraphics g) {
		drawPlayerMissles(g);
		drawEnemyMissles(g);
	}
	
	void fillCircle(AGraphics g, float x0, float y0, float radius) {
		int x = Math.round(x0 - radius);
		int y = Math.round(y0 - radius);
		int wh = Math.round(radius*2);
		g.drawFilledOval(x, y, wh, wh);
	}
	
	float getExplosionNumFrames() {
		return 60;
	}
	
	float getExplosionMaxRadius() {
		return 50;
	}

	// return true when missle is active
	boolean drawExplosion(AGraphics g, Explosion e) {
		final float numExplosionFrames = getExplosionNumFrames();
		final float maxExplosionRadius = getExplosionMaxRadius();

		int frames = this.getFrameNumber()-e.startFrame;
		if (frames > numExplosionFrames)
			return false;
		
		if (frames < numExplosionFrames*0.5f) {
			e.innerRadius = 0;
			e.outerRadius = maxExplosionRadius * 
					(this.getFrameNumber() - e.startFrame) / (numExplosionFrames/2);
		} else {
			e.innerRadius = maxExplosionRadius * 
					((this.getFrameNumber()-numExplosionFrames/2)  
					- e.startFrame) / (numExplosionFrames*0.5f);
			e.outerRadius = maxExplosionRadius;
		}
		
		g.setColor(getExplosionColor());
		fillCircle(g, e.x, e.y, e.outerRadius);
		g.setColor(getSkyColor());
		fillCircle(g, e.x, e.y, e.innerRadius);
		
		return true;
	}
	
	void drawPlayerExplosions(AGraphics g) {
	
		for (int i=0; i<playerExplosions.length; i++) {
			Explosion e = playerExplosions[i];
			if (e.startFrame < 0)
				continue;
			if (!drawExplosion(g, e)) {
				e.startFrame = -1;
			} else {
				collisionScanEnemyMissle(e);
			}
		}
	}
	
	// 
	void collisionScanEnemyMissle(Explosion e) {
		for (int i=0; i<enemyMissles.length; i++) {
			Missle m = enemyMissles[i];
			if (m.startFrame < 0) {
				continue;
			}
			int hx = m.nx;
			int hy = m.ny;
			if (Utils.isPointInsideCircle(hx, hy, e.x, e.y, Math.round(e.outerRadius))) {
				m.startFrame = -1;
				this.startPlayerExplosion(hx, hy);
			}
		}
	}
	
	void drawEnemyExplosions(AGraphics g) {
		for (int i=0; i<enemyExplosions.length; i++) {
			Explosion e = enemyExplosions[i];
			if (e.startFrame < 0)
				continue;
			if (!drawExplosion(g, e)) {
				e.startFrame = -1;
			} else {
				collisionScanCity(e);
			}
		}
	}
	
	void collisionScanCity(Explosion e) {
		for (int i=0; i<cities.length; i++) {
			City c = cities[i];
			if (c.numMissles < 0)
				continue;
			if (Utils.isCirclesOverlapping(c.x, c.y, CITY_RADIUS, e.x, e.y, Math.round(e.outerRadius))) {
				this.startPlayerExplosion(c.x, c.y);
				c.numMissles = -1;
			}
		}
	}
	
	void drawExplosions(AGraphics g) {
		drawPlayerExplosions(g);
		drawEnemyExplosions(g);
	}
	
	void drawLand(AGraphics g) {
		int height = getScreenHeight()-1;
		g.setColor(this.getLandColor());
		final int xStep = getScreenWidth() / (landYFactor.length-1);
		int x = 0;

		for (int i=0; i<landYFactor.length-1; i++) {
		    g.begin();
		    g.vertex(x, height);
		    g.vertex(x, Math.round(getLandY(i)));
		    g.vertex(x+xStep, Math.round(getLandY(i+1)));
		    g.vertex(x+xStep, height);
		    g.drawTriangleFan();
			x+=xStep;
		}
	}

	int getCityRadius() {
		return 32;
	}
	
	void drawCities(AGraphics g) {
		for (int i=0; i<cities.length; i++) {
			if (cities[i].numMissles < 0)
				continue;
			int x = cities[i].x - getCityRadius()/2;
			int y = cities[i].y - getCityRadius()/2;
			int imageNum = i % cityImageIds.length;
			if (cityImageIds[imageNum] > 0)
				g.drawImage(cityImageIds[imageNum], x, y, this.getCityRadius(), getCityRadius());
			if (cities[i].numMissles > 0) {
				g.setColor(getTextColor());
				g.drawJustifiedString( cities[i].x, getScreenHeight()-20, Justify.CENTER, Justify.CENTER,String.valueOf(cities[i].numMissles));
			}
		}
	}
	
	void addMissle(Missle [] array, int sx, int sy, int ex, int ey, int speed) {
		for (int i=0; i<array.length; i++) {
			if (array[i].startFrame < 0) {
				array[i].startFrame = getFrameNumber();
				array[i].sx = sx;
				array[i].sy = sy;
				array[i].ex = ex;
				array[i].ey = ey;
				array[i].speed = speed;
				return;
			}
		}
		System.err.println("cant add any more missles too [" + array + "]");
	}
	
	// --------------------------------------------------------------
	// CLASSES
	// --------------------------------------------------------------

	public static class Missle {
		int sx, sy; // start xy
		int ex, ey; // end xy
		int nx, ny; // next x,y (position of the head)
		int startFrame=-1; // frame this missle was spawned
		int speed;
	};
	
	public static class Explosion {
		int x, y;
		int startFrame=-1;
		float innerRadius, outerRadius;
	};
	
	public static class City {
		int x, y;
		int numMissles=0;
	};
	
	// --------------------------------------------------------------
	// CONSTANTS
	// --------------------------------------------------------------
	
	final int GAME_STATE_GET_READY 		= 0;
	final int GAME_STATE_PLAY 			= 1;
	final int GAME_STATE_GAME_OVER 		= 2;
	final int GAME_STATE_LEVEL_OVER 	= 3;

	final int CITY_RADIUS 				= 10; 

	// --------------------------------------------------------------
	// TABLES
	// --------------------------------------------------------------
	
	final Missle [] 	playerMissles 		= new Missle[256];
	final Missle [] 	enemyMissles  		= new Missle[256];
	
	final Explosion [] 	playerExplosions 	= new Explosion[64];
	final Explosion [] 	enemyExplosions 	= new Explosion[64];
	
	final City [] 		cities 				= new City[6];
	
	final float [] landYFactor				= new float[cities.length+1];
	final int [] 	cityImageIds 			= new int[4];

	int nextWaveFrame = 0;
	int numWavesLeft = 0;
	
	AColor colors[];
	
	void initColors(AGraphics g) {
	    colors = new AColor[] {
    		g.RED,
    		g.BLACK,
    		g.BLUE,
    		g.GRAY,
    		g.GREEN,
    		g.DARK_GRAY,
    		g.CYAN,
    		g.MAGENTA,
    		g.YELLOW,
    		g.ORANGE,
    		g.WHITE
	    };
	};
	
	// --------------------------------------------------------------
	// GLOBALS
	// --------------------------------------------------------------
	int gameState 							= GAME_STATE_GET_READY;
	int currentLevel 						= 1;
}
