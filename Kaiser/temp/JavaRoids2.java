package cc.jroids;

import cc.lib.swing.AWTFrame;
import cc.lib.swing.AWTKeyboardAnimationApplet;
import cc.lib.swing.Renderer;
import cc.lib.game.Justify;
import cc.lib.game.Polygon2D;
import cc.lib.game.Utils;

import java.awt.event.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

public class JavaRoids2 extends AWTKeyboardAnimationApplet
{
	static final long serialVersionUID = 0;

	public static void main(String [] args) {
		Utils.DEBUG_ENABLED = true;
		AWTFrame frame = new AWTFrame("JavaRoids Debug Mode");
		AWTKeyboardAnimationApplet app = new JavaRoids2();
		frame.add(app);
		frame.centerToScreen(640, 640);
		app.init();
		app.start();
	}	
	
	// General purpose shape	
	class Obj
	{
		float		x,y;			// position
		float		vx,vy;			// velocity
		float		radius;			// radius
		float		angle;			// heading
		float		spin;			// ang speed (for rocks) 
		int			type;			// type
		int			start_frame;	// the frame this object was added on (for projectiles)
		int			shape_index;	// index to the shape array for rendering
	};
	

	//////////////////////////////////////////////////////
	// CONSTANTS
	
	final int		TYPE_UNUSED				= 0;
	final int		TYPE_STAR_NEAR			= 1;
	final int		TYPE_STAR_FAR			= 2;
	final int		TYPE_STAR_DISTANT		= 3;
	final int		TYPE_PLAYER_MISSLE		= 4;
	final int		TYPE_ROCK				= 5;
	
	final int		MAX_OBJECTS				= 256;
	
	// GAME STATES
	
	final int		STATE_INTRO				= 0;
	final int		STATE_START				= 1;
	final int		STATE_PLAY				= 2;
	final int		STATE_EXPLODING			= 3;
	final int		STATE_GAME_OVER			= 4;
	
	// BUTTONS That respond to press/release events
	//   Not like Hyperspace which is a typed event
	
	final int		SHAPE_PLAYER			= 0;
	final int		SHAPE_ROCK_I			= 1;
	final int		SHAPE_ROCK_II			= 2;
	final int		SHAPE_ROCK_III			= 3;
	final int		SHAPE_ROCK_IV			= 4;
	final int		SHAPE_LARGE_SAUCER		= 5;
	final int		SHAPE_SMALL_SAUCER		= 6;
	final int		SHAPE_THRUST			= 7;
	final int		SHAPE_THRUST1			= 8;
	final int		SHAPE_THRUST2			= 9;
	final int		SHAPE_THRUST3			= 10;
	final int		NUM_SHAPES				= 11; // MUST BE LAST!!!
	
	final int		NUM_THRUST_SHAPES		= 4;
	
	final Color		BUTTON_OUTLINE_COLOR	= Color.RED;
	final Color		BUTTON_FILL_COLOR		= Color.DARK_GRAY;
	final Color		BUTTON_TEXT_COLOR		= Color.BLUE;
	final Color		BUTTON_H_OUTLINE_COLOR	= Color.YELLOW;
	final Color		BUTTON_H_TEXT_COLOR		= Color.YELLOW;
	
	final float		PLAYER_FRICTION			= 0.95f;
	final Color		PLAYER_COLOR			= Color.GREEN;
	final float		PLAYER_RADIUS			= 15.0f;
	final int		PLAYER_ROTATE_SPEED		= 8;
	final float		PLAYER_THRUST_POWER		= 0.25f;
	final int		PLAYER_SHOOT_RATE		= 10;
	final int		PLAYER_BUTTON_RIGHT		= 1;
	final int		PLAYER_BUTTON_LEFT		= 2;
	final int		PLAYER_BUTTON_THRUST	= 4;
	final int		PLAYER_BUTTON_SHOOT		= 8;
	final int		PLAYER_BUTTON_SHIELD	= 16;

	final int		PLAYER_MISSLE_DURATION	= 100;
	final float		PLAYER_MISSLE_SPEED		= 10.0f;
	final Color		PLAYER_MISSLE_COLOR		= Color.YELLOW;
	
	final int		NUM_STARS				= 32;
	final Color		COLOR_STAR_NEAR			= Color.WHITE;
	final Color		COLOR_STAR_FAR			= Color.GRAY;
	final Color		COLOR_STAR_DISTANT		= Color.DARK_GRAY;
	
	final int		ROCK_RADIUS_LARGE		= 40;
	final int		ROCK_RADIUS_MEDIUM		= 20;
	final int		ROCK_RADIUS_SMALL		= 10;
	
	//////////////////////////////////////////////////////
	// GAME VARS
	
	int			game_state			= STATE_INTRO;
	
	// dimension of the world, player stays in middle 
	// the world wraps
	int			world_w				= 2000;
	int			world_h				= 2000;
	
	int			screen_x 			= 0;
	int			screen_y 			= 0;
	
	int			mouse_x				= 0;
	int			mouse_y				= 0;
	int			mouse_dx			= 0;
	int			mouse_dy			= 0;
	
	float		player_x 			= world_w / 2;
	float		player_y			= world_h / 2;
	float		player_dx			= 0;
	float		player_dy			= 0;
	float		player_angle		= 0;
	int			player_missle_next_fire_frame; // used to space out missles
	
	int			player_button_flag	= 0;
	
	Obj [] 		objects				= new Obj[MAX_OBJECTS];
	int			num_objects			= 0;
	
	Polygon2D []shapes				= new Polygon2D[NUM_SHAPES];
	
	Renderer	renderer;
	
	/////////////////////////////////////////////////////////////////
	// OVERRIDDEN FUNCTIONS - Called when window is resized
	
	//---------------------------------------------------------------
	protected void 		setDimension(int width, int height) {
	}

	//---------------------------------------------------------------
	float [] makePoint(float x, float y) {
		float [] pt = new float[2];
		pt[0] = x;
		pt[1] = y;
		return pt;
	}

	//---------------------------------------------------------------
	Polygon2D makePlayerShape() {
		final float [][] player_pts = new float[4][];
		player_pts[0] = makePoint(3,0);
		player_pts[1] = makePoint(-2,2);
		player_pts[2] = makePoint(-1,0);
		player_pts[3] = makePoint(-2,-2);
		return new Polygon2D (player_pts, PLAYER_COLOR, PLAYER_RADIUS);
	}
	
	//---------------------------------------------------------------
	// randomly generate a 'rock'
	Polygon2D makeRockShape(int num_pts) {
		
		//ArrayList angles = new ArrayList(num_pts);
		//for (int i=0; i<num_pts; i++)
		//	angles.add(i, new Integer(Utils.randRange(0,359)));
		//Collections.sort(angles);
		
		int range = Math.round(360f / num_pts);
		
		int [] angles = new int[num_pts];
		for (int i=0; i<num_pts; i++)
			angles[i] = range*i + Utils.rand() % range;
		Arrays.sort(angles);		
		
		float [][] pts = new float[num_pts][];
		for (int i=0; i<num_pts; i++)
		{
			float angle = Math.round(angles[i] * Utils.DEG_TO_RAD);
			
			float x = (float)Math.cos(angle);
			float y = (float)Math.sin(angle);
			float len = 1 + Utils.randFloat(2);
			pts[i] = makePoint(x*len, y*len);
		}
		int r = Utils.randRange(0,50);
		int g = 255-Utils.randRange(0,50);
		int b = Utils.randRange(0,50);
		Color color = new Color(r,g,b);
		return new Polygon2D(pts, color, 1);
	}
	
	Polygon2D makeRandomThrustShape()
	{
		float [][] pts = new float[4][];
		pts[0] = this.makePoint(-2, 0);
		pts[1] = makePoint(-5 + Utils.randRange(-1,1), -2 + Utils.randRange(0,1));
		pts[2] = makePoint(-11+Utils.randRange(-2,-2),  0 + Utils.randRange(-2,2));
		pts[3] = makePoint(-5 + Utils.randRange(-1,1),  2 + Utils.randRange(0,1));
		Polygon2D p = new Polygon2D(pts, Color.RED, PLAYER_RADIUS);
		p.translate(-PLAYER_RADIUS*3/4, 0);		
		return p;
	}
	
	//---------------------------------------------------------------
	void initShapes() {
		shapes[SHAPE_PLAYER] 	= makePlayerShape();
		shapes[SHAPE_ROCK_I] 	= makeRockShape(5);
		shapes[SHAPE_ROCK_II] 	= makeRockShape(7);
		shapes[SHAPE_ROCK_III] 	= makeRockShape(7);
		shapes[SHAPE_ROCK_IV] 	= makeRockShape(9);
		for (int i=0; i<NUM_THRUST_SHAPES; i++)
			shapes[SHAPE_THRUST+i] = makeRandomThrustShape();
	}
	
	//---------------------------------------------------------------
	// called once at beginning af app
	// do all allocation here, NOT during game
	protected void		doInitialization() {
		// alloc all objects
		renderer = new Renderer(this);
		this.setBackground(Color.BLACK);
		
		// allocate all the objects
		for (int i=0; i<MAX_OBJECTS; i++)
			objects[i] = new Obj();
		
		// Init shapes
		initShapes();
		
		// Init Stars
		for (int i=0; i<NUM_STARS; i++)
			addStar();
		
		renderer.makeIdentity();
		renderer.setOrtho(-320, 320, -320, 320);
		addRocks(64);
		
	}
	
	//---------------------------------------------------------------
	// called once per frame
	protected void 		drawFrame(Graphics g) {
		
		final int width = getScreenWidth();
		final int height = getScreenHeight();
		
        // clear screen
        g.setColor(Color.black);
        g.fillRect(0,0,width,height);
		
		switch (game_state) {
		case STATE_INTRO:
			drawIntro(g);
			break;
		case STATE_START:
			drawStart(g);
			break;
		case STATE_PLAY:
			drawPlay(g);
			break;
		case STATE_EXPLODING:
			drawExploding(g);
			break;
		case STATE_GAME_OVER:
			drawGameOver(g);
			break;
		}
		
		// reset the mouse deltas at the end of the frame
		mouse_dx = mouse_dy = 0;
	}
	
	// FUNCTIONS
	
	//---------------------------------------------------------------
	void		drawStart(Graphics g) {
	}
	
	//---------------------------------------------------------------
	void		drawIntro(Graphics g) {
		this.drawPlayer(g, this.getFrameNumber()%4, 320, 320, this.getFrameNumber() % 360);
	}
	
	//---------------------------------------------------------------
	void		doPlayerThrust() {
		float angle = Math.round(player_angle * Utils.DEG_TO_RAD);
		float dx = (float)Math.cos(angle);
		float dy = (float)Math.sin(angle);
		player_dx += dx * PLAYER_THRUST_POWER;
		player_dy += dy * PLAYER_THRUST_POWER;
	}
	
	//---------------------------------------------------------------
	boolean		isButtonDown(int button) {
		if ((player_button_flag & button) != 0)
			return true;
		return false;
	}

	//---------------------------------------------------------------
	void		addPlayerMissle() {
		Obj obj = this.findUnusedObject();
		if (obj != null)
		{
			float angle = Math.round(player_angle * Utils.DEG_TO_RAD);
			obj.x = player_x + (float)Math.cos(angle) * (PLAYER_RADIUS-3);
			obj.y = player_y + (float)Math.sin(angle) * (PLAYER_RADIUS-3);
			obj.angle = player_angle;
			obj.type = TYPE_PLAYER_MISSLE;
			obj.start_frame = this.getFrameNumber();
		}
		player_missle_next_fire_frame = getFrameNumber() + PLAYER_SHOOT_RATE;
	}
	
	//---------------------------------------------------------------
	void		doPlayerMissle() {
		if (this.getFrameNumber() >= player_missle_next_fire_frame)
			addPlayerMissle();
	}
	
	//---------------------------------------------------------------
	void		updatePlayer() {
		final int screen_w = getScreenWidth();
		final int screen_h = getScreenHeight();
		if (isButtonDown(PLAYER_BUTTON_RIGHT))
			player_angle += PLAYER_ROTATE_SPEED;
		if (isButtonDown(PLAYER_BUTTON_LEFT))
			player_angle -= PLAYER_ROTATE_SPEED;
		if (isButtonDown(PLAYER_BUTTON_THRUST))
			doPlayerThrust();
		if (isButtonDown(PLAYER_BUTTON_SHOOT))
			doPlayerMissle();				
		player_x += player_dx;
		player_y += player_dy;
		if (player_x < 0)
			player_x += world_w;
		else if (player_x > world_w)
			player_x -= world_w;
		if (player_y < 0)
			player_y += world_h;
		else if (player_y > world_h)
			player_y -= world_h;
		screen_x = Math.round(player_x) - screen_w/2;
		screen_y = Math.round(player_y) - screen_h/2;
	}
	
	//---------------------------------------------------------------
	void		updateStar(Obj obj, float dx, float dy) {
		final int screen_w = getScreenWidth();
		final int screen_h = getScreenHeight();

		obj.x += dx;
		obj.y += dy;
		if (obj.x < 0)
		{
			// wrap the star ABOUT THE SCREEN and
			obj.x += screen_w;
			obj.y = Utils.randRange(0, screen_h);
			obj.type = Utils.randRange(TYPE_STAR_NEAR, TYPE_STAR_DISTANT);
		}
		else if (obj.x > screen_w) {
			obj.x -= screen_w;
			obj.y = Utils.randRange(0, screen_h);
			obj.type = Utils.randRange(TYPE_STAR_NEAR, TYPE_STAR_DISTANT);
		}
		
		if (obj.y < 0)
		{
			obj.y += screen_h;
			obj.x = Utils.randRange(0, screen_w);
			obj.type = Utils.randRange(TYPE_STAR_NEAR, TYPE_STAR_DISTANT);
		}
		else if (obj.y > screen_h)
		{
			obj.y -= screen_h;
			obj.x = Utils.randRange(0, screen_w);
			obj.type = Utils.randRange(TYPE_STAR_NEAR, TYPE_STAR_DISTANT);
		}
	}
	
	//---------------------------------------------------------------
	void		drawStar(Graphics g, int x, int y, Color color) {
		g.setColor(color);
		drawPixel(g,x,y);
	}
	
	//---------------------------------------------------------------
	void		drawPlayer(Graphics g, int thrust, float x, float y, float angle) {
		renderer.pushMatrix();
        renderer.translate(x, y);
		renderer.rotate(angle);
        renderer.translate(-x, -y);
		shapes[SHAPE_PLAYER].draw(renderer, g);
		if (thrust>=0)
			shapes[thrust].draw(renderer, g);
		renderer.popMatrix();
	}
	
	//---------------------------------------------------------------
	void		drawRock(Graphics g, Obj obj) {
		float dx = this.worldToScreenX(obj.x);
		float dy = worldToScreenY(obj.y);
		renderer.pushMatrix();
		renderer.translate(dx,dy);
		renderer.rotate(obj.angle);
		renderer.scale(obj.radius);
		shapes[obj.shape_index].draw(renderer, g);
		renderer.popMatrix();
	}
	
	//---------------------------------------------------------------
	void		freeObject(Obj obj) {
		obj.type = TYPE_UNUSED;
	}

	//---------------------------------------------------------------
	void		updatePlayerMissle(Obj obj) {
		//if (!isOnScreen(Math.round(obj.x), Math.round(obj.y)))
		//{
		//	freeObject(obj);
		//}
		//else 
		if (this.getFrameNumber() - obj.start_frame > PLAYER_MISSLE_DURATION)
		{
			freeObject(obj);
		}
		else
		{
			float angle = Math.round(obj.angle * Utils.DEG_TO_RAD);
			
			float dx = (float)Math.cos(angle) * PLAYER_MISSLE_SPEED;
			float dy = (float)Math.sin(angle) * PLAYER_MISSLE_SPEED;
			
			obj.x += dx;
			obj.y += dy;
			
			obj.x %= world_w;
			obj.y %= world_h;
		}
	}
	
	//---------------------------------------------------------------
	void		doRockExplosion()
	{
		
	}
	
	//---------------------------------------------------------------
	void		doRockCollision(Obj obj) {
		freeObject(obj);
		if (obj.radius <= ROCK_RADIUS_SMALL)
		{
			doRockExplosion();
		}
		else if (obj.radius <= ROCK_RADIUS_MEDIUM)
		{
			int x = Math.round(obj.x);
			int y = Math.round(obj.y);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-5,5), ROCK_RADIUS_SMALL);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-5,5), ROCK_RADIUS_SMALL);
		}
		else
		{
			int x = Math.round(obj.x);
			int y = Math.round(obj.y);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
		}
	}
	
	//---------------------------------------------------------------
	void		doCollisionDetection()
	{
		for (int i=NUM_STARS; i<num_objects; i++)
		{
			Obj obj = objects[i];
			
			if (obj.type == TYPE_PLAYER_MISSLE)
			{
				for (int j=NUM_STARS; j<num_objects; j++)
				{
					if (i==j)
						continue;
					
					Obj obj2 = objects[j];
					
					if (obj2.type == TYPE_ROCK)
					{
						int px = Math.round(obj.x);
						int py = Math.round(obj.y);
						int cx = Math.round(obj2.x);
						int cy = Math.round(obj2.y);
						int rad = Math.round(obj2.radius);
						
						if (Utils.isPointInsideRect(px,py,cx-rad,cy-rad,rad*2,rad*2))
						{
							doRockCollision(obj2);
							freeObject(obj);
							break;
						}
					}
				}
			}			
		}
	}
	
	//---------------------------------------------------------------
/*	void		wrapPosition(Obj obj) {
		if (obj.x < 0)
			obj.x += world_w;
		else if (obj.x > world_w)
			obj.x -= world_w;
		
		if (obj.y < 0)
			obj.y += world_h;
		else if (obj.y > world_h)
			obj.y -= world_h;
	}
	
	//---------------------------------------------------------------
	// Return an x coordinate relative to the screen
	// defined with the origin at the center of the screen
/*	int			worldToScreenX(float x) {
		int X = Math.round(x) - screen_x;
		if (screen_x > world_w-screen_w)
		{
			// the screen is overlapping with the world
		}
	}*/
	
	//---------------------------------------------------------------
	void		updateRock(Obj obj) {
		float dx = obj.vx;
		float dy = obj.vy;
		obj.x += dx;
		obj.y += dy;
		obj.x %= world_w;
		obj.y %= world_h;
		obj.angle += obj.spin;
		//wrapPosition(obj);
	}

	//---------------------------------------------------------------
	void		drawPixel(Graphics g, int x, int y) {
		g.drawRect(x,y,1,1);
	}

	//---------------------------------------------------------------
	void		drawPlayerMissle(Graphics g, int x, int y) {
		g.setColor(PLAYER_MISSLE_COLOR);
		x = worldToScreenX(x);
		y = worldToScreenY(y);
		drawPixel(g,x,y);
	}
	
	//---------------------------------------------------------------
	int		worldToScreenX(float x) {
		float dx = x - player_x;
		if (dx < -world_w/2)
			dx += world_w;
		else if (dx > world_w/2)
			dx -= world_w;
		
		return Math.round(player_x + dx);
	}
	
	//---------------------------------------------------------------
	int		worldToScreenY(float y) {
		float dy = y - player_y;
		if (dy < -world_h/2)
			dy += world_h;
		else if (dy > world_h/2)
			dy -= world_h;
		
		return Math.round(player_x + dy);
	}

	//---------------------------------------------------------------
	void		updateObjects() {
		for (int i=0; i<num_objects; i++)
		{
			Obj obj = objects[i];
			
			switch (obj.type) {
			case TYPE_STAR_NEAR:
				updateStar(obj, -player_dx, -player_dy);
				break;
				
			case TYPE_STAR_FAR:
				updateStar(obj, -player_dx/2, -player_dy/2);
				break;
				
			case TYPE_STAR_DISTANT:
				updateStar(obj, -player_dx/3, -player_dy/3);
				break;
				
			case TYPE_PLAYER_MISSLE:
				updatePlayerMissle(obj);
				break;
				
			case TYPE_ROCK:
				updateRock(obj);
				break;
			}
		}
	}
	
	//---------------------------------------------------------------
	void		drawObjects(Graphics g) {
		for (int i=0; i<num_objects; i++) {
			Obj obj = objects[i];
			int x = Math.round(obj.x);
			int y = Math.round(obj.y);
			switch (obj.type) {
			case TYPE_STAR_NEAR:
				drawStar(g,x,y,COLOR_STAR_NEAR);
				break;
				
			case TYPE_STAR_FAR:
				drawStar(g,x,y,COLOR_STAR_FAR);
				break;
			
			case TYPE_STAR_DISTANT:
				drawStar(g,x,y,COLOR_STAR_DISTANT);
				break;
				
			case TYPE_PLAYER_MISSLE:
				drawPlayerMissle(g,x,y);
				break;
				
			case TYPE_ROCK:
				//if (isOnScreen(x,y))
					drawRock(g, obj);
			}
		}
	}
	
	//---------------------------------------------------------------
	void		drawPlay(Graphics g) {
		updateObjects();
		updatePlayer();
		doCollisionDetection();
		drawObjects(g);
		int thrust = -1;
		if (isButtonDown(PLAYER_BUTTON_THRUST))
			thrust = SHAPE_THRUST + Utils.rand()%NUM_THRUST_SHAPES;
		drawPlayer(g, thrust, player_x, player_y, player_angle);
		drawDebug(g);
	}
	
	void drawDebug(Graphics g) {
		if (!Utils.DEBUG_ENABLED)
			return;
		
		final int cx = getScreenWidth()/2;
		final int cy = getScreenHeight()/2;
		
		g.setColor(Color.CYAN);
		
		String text = "[" + (int)this.player_x + ", " + (int)player_y + "]";
		
		// draw the player's position
		Utils.drawJustifiedString(g, cx, cy, Justify.RIGHT, Justify.TOP, text);
	}
	
	//---------------------------------------------------------------
	void		drawExploding(Graphics g) {
		
	}
	
	//---------------------------------------------------------------
	void		drawGameOver(Graphics g) {
	}
	
	//---------------------------------------------------------------
	void		drawButton(Graphics g, int x, int y, int w, int h, String text) {
		if (isMouseInRect(x,y,w,h))
		{

		}
		else
		{
			g.fillRect(x,y,w,h);
		}
	}
	
	//---------------------------------------------------------------
	boolean		isMouseInRect(int x, int y, int w, int h) {
		return Utils.isPointInsideRect(mouse_x, mouse_y, x, y, w, h);
	}
	
	/*
	//---------------------------------------------------------------
	boolean		isOnScreen(int x, int y) {
		// need to take into account that the world wraps
		if (screen_x > world_w-screen_w)
		{
			int dx = world_w - screen_x;
			if (x < screen_x && x > screen_w-dx)
				return false;
		}
		else
		{
			if (x < screen_x || x > screen_x+screen_w)
				return false;
		}
		
		if (screen_y > world_h-screen_h)
		{
			int dy = world_h - screen_y;
			if (y < screen_y && y > screen_h-dy)
				return false;
		}
		else
		{
			if (y < screen_y || y > screen_y+screen_h)
				return false;
		}
		return true;
	}
	*/
	
	//---------------------------------------------------------------
	// return a ptr to an unused object, or null of none are available
	Obj			findUnusedObject() {
		for (int i=0; i<num_objects; i++)
		{
			if (objects[i].type == TYPE_UNUSED)
				return objects[i];
		}
		if (num_objects < MAX_OBJECTS)
			return objects[num_objects++];
		return null;
	}
	
	//---------------------------------------------------------------
	void		addRocks(int num_rocks) {
		for (int i=0; i<num_rocks; i++)
		{
			int x = Utils.randRange(0,world_w);
			int y = Utils.randRange(0,world_h);
			addRock(x,y,ROCK_RADIUS_LARGE);
		}
	}
	
	//---------------------------------------------------------------
	// add a star
	void		addStar() {
		int width = getScreenWidth();
		int height = getScreenHeight();
		
		Utils.println("addStart width[" + width + "] height [" + height + "]");
		Obj obj = findUnusedObject();
		if (obj == null)
			return;
		obj.x = Utils.randRange(0,width);
		obj.y = Utils.randRange(0,height);
		obj.type = Utils.randRange(TYPE_STAR_NEAR, TYPE_STAR_DISTANT);
	}
	
	//---------------------------------------------------------------
	void		addRock(int x, int y, int radius) {
		Obj obj = findUnusedObject();
		if (obj == null)
			return;
		obj.x			= x;
		obj.y			= y;
		obj.angle		= Utils.randRange(0,360);
		obj.spin		= Utils.randRange(2,6); //1+ROCK_RADIUS_LARGE*2-radius);
		obj.radius		= radius;
		obj.start_frame = getFrameNumber();
		obj.type		= TYPE_ROCK;
		float velocity	= Utils.randFloat(5) + 1;
		float angle		= Math.round(obj.angle * Utils.DEG_TO_RAD);
		obj.vx			= (float)Math.cos(angle) * velocity;
		obj.vy			= (float)Math.sin(angle) * velocity;
		obj.shape_index	= Utils.randRange(SHAPE_ROCK_I, SHAPE_ROCK_IV);
	}	
	
	//---------------------------------------------------------------
	public void keyTyped(KeyEvent e) {
		int key=e.getKeyCode();
		switch (key){
		case KeyEvent.VK_SPACE:
			//hyperspace();
			break;
		}
	}
	
	//---------------------------------------------------------------
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		
		switch (key) {
		case KeyEvent.VK_LEFT:
			player_button_flag |= PLAYER_BUTTON_LEFT;
			break;
			
		case KeyEvent.VK_RIGHT:
			player_button_flag |= PLAYER_BUTTON_RIGHT;
			break;
			
		case KeyEvent.VK_UP:
			player_button_flag |= PLAYER_BUTTON_THRUST;
			break;
			
		case KeyEvent.VK_DOWN:
			break;
			
		case KeyEvent.VK_Z:
			player_button_flag |= PLAYER_BUTTON_SHOOT;
			break;
			
		case KeyEvent.VK_SHIFT:
			player_button_flag |= PLAYER_BUTTON_SHIELD;
			break;
			
		case KeyEvent.VK_SPACE:
			break;
			
		case KeyEvent.VK_ALT:
			break;
		}
	}

	
	//---------------------------------------------------------------
	public void keyReleased(KeyEvent e) {
		int key = e.getKeyCode();
		
		switch (key) {
		case KeyEvent.VK_LEFT:
			player_button_flag &= ~PLAYER_BUTTON_LEFT;
			break;
			
		case KeyEvent.VK_RIGHT:
			player_button_flag &= ~PLAYER_BUTTON_RIGHT;
			break;
			
		case KeyEvent.VK_UP:
			player_button_flag &= ~PLAYER_BUTTON_THRUST;
			break;
			
		case KeyEvent.VK_DOWN:
			break;
			
		case KeyEvent.VK_Z:
			addPlayerMissle();
			player_button_flag &= ~PLAYER_BUTTON_SHOOT;
			break;
			
		case KeyEvent.VK_SHIFT:
			player_button_flag &= ~PLAYER_BUTTON_SHIELD;
			break;
			
		case KeyEvent.VK_SPACE:
			break;
			
		case KeyEvent.VK_ALT:
			break;
		}
	}
	
	//---------------------------------------------------------------
	public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		mouse_dx = mouse_x - x;
		mouse_dy = mouse_y - y;
		mouse_x = x;
		mouse_y = y;
	}
	
	//---------------------------------------------------------------
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}

	//---------------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		int button=e.getButton();
		switch (button) {
		case MouseEvent.BUTTON1:
			switch (game_state) {
			case STATE_INTRO:
				break;
				
			case STATE_PLAY:
				break;
			}
			break;
			
		case MouseEvent.BUTTON2:
			break;
			
		case MouseEvent.BUTTON3:
			if (game_state == STATE_PLAY)
			{
			
			}
			break;
		}
	}

	//---------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (game_state != STATE_PLAY)
			return;
		
		int button=e.getButton();
		switch (button) {
		case MouseEvent.BUTTON1:
			break;
			
		case MouseEvent.BUTTON2:
			break;
			
		case MouseEvent.BUTTON3:
			break;
		}
	}

	boolean isInQuadrant(int x, int y, int qx, int qy, int qx2, int qy2, int radius, int [] quadXY) {
	    int cx = (qx+qx2)/2;
	    int cy = (qy+qy2)/2;
	    
	    int qw = qx2-qx;
	    int qh = qy2-qy;
	    //if (x >= qx && y >= qy && x <= qx2 && y <= qy2) {
	    int dx = Math.abs(x - cx);
	    int dy = Math.abs(y - cy);
	    if (dx + radius < qw/2 && dy + radius < qh/2) {
    	    quadXY[0] = x - qx;
    	    quadXY[1] = y - qy;
    	    return true;
	    }
	    return false;
	}
	
	boolean isOnScreen(int x, int y, int radius, int [] screenXY) {

	    int screen_w = getScreenWidth();
	    int screen_h = getScreenHeight();
	    
	    if (screen_x + screen_w > world_w) {
	        int dx = world_w - (screen_x + screen_w);
	        if (screen_y + screen_h > world_h) {
	            // 4 quadrants
	            int dy = world_h - (screen_y + screen_h);
	            return isInQuadrant(x, y, screen_x, screen_y, screen_x+dx, screen_y+dy, radius, screenXY) ||
	                   isInQuadrant(x, y, 0, screen_y, screen_w-dx, screen_y+dy, radius, screenXY) ||
	                   isInQuadrant(x, y, screen_x, 0, screen_x+dx, screen_h-dy, radius, screenXY) ||
	                   isInQuadrant(x, y, 0, 0, screen_w-dx, screen_h-dy, radius, screenXY);
	        } else {
	            // 2 quadrants
	            return isInQuadrant(x, y, screen_x, screen_y, screen_x+dx, screen_y+screen_h, radius, screenXY) ||
	                   isInQuadrant(x, y, 0       , screen_y, screen_w-dx, screen_y+screen_h, radius, screenXY);
	        }
	        
	    } else if (screen_y + screen_h > world_h) {
	        // 2 quadrants
	        int dy = world_h - (screen_y + screen_h);
	        return isInQuadrant(x, y, screen_x, screen_y, screen_x+screen_w, screen_y+dy, radius, screenXY) ||
	               isInQuadrant(x, y, screen_x, 0       , screen_x+screen_w, screen_h-dy, radius, screenXY);
	    } else {
	        // 1 quadrant
	        return isInQuadrant(x, y, screen_x, screen_y, screen_x+screen_w, screen_y+screen_h, radius, screenXY);
	    }
	    
	    
	}
}
