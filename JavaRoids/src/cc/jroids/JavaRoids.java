package cc.jroids;

import java.util.Arrays;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.Justify;
import cc.lib.game.Polygon2D;
import cc.lib.game.Utils;
import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class JavaRoids
{
	public abstract int getFrameNumber();
	public abstract int getScreenWidth();
	public abstract int getScreenHeight();

	// General purpose shape
	private static class Obj
	{
		final MutableVector2D position = new MutableVector2D();
		final MutableVector2D velocity = new MutableVector2D();
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
	final int		TYPE_PLAYER_MISSLE		= 1;
	final int		TYPE_ROCK				= 2;
	
	final int		MAX_OBJECTS				= 256;
	
	// GAME STATES
	
	final int		STATE_INTRO				= 0;
	final int		STATE_START				= 1;
	final int		STATE_PLAY				= 2;
	final int		STATE_EXPLODING			= 3;
	final int		STATE_GAME_OVER			= 4;
	
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

	// BUTTONS That respond to press/release events
	//   Not like Hyperspace which is a typed event

	final float		PLAYER_FRICTION			= 0.95f;
	final float		PLAYER_RADIUS			= 15.0f;
	final int		PLAYER_ROTATE_SPEED		= 8;
	final float		PLAYER_THRUST_POWER		= 0.25f;
	final int		PLAYER_SHOOT_RATE		= 10;
	public static final int		PLAYER_BUTTON_RIGHT		= 1;
    public static final int		PLAYER_BUTTON_LEFT		= 2;
    public static final int		PLAYER_BUTTON_THRUST	= 4;
    public static final int		PLAYER_BUTTON_SHOOT		= 8;
    public static final int		PLAYER_BUTTON_SHIELD	= 16;

	final int		PLAYER_MISSLE_DURATION	= 100;
	final float		PLAYER_MISSLE_SPEED		= 10.0f;
	
	final int		NUM_STARS				= 32;
	
	final int		ROCK_RADIUS_LARGE		= 40;
	final int		ROCK_RADIUS_MEDIUM		= 20;
	final int		ROCK_RADIUS_SMALL		= 10;
	
	//////////////////////////////////////////////////////
	// GAME VARS
	
	int			game_state			= STATE_PLAY;
	
	// dimension of the world, player stays in middle 
	// the world wraps
	int			world_w				= 2000;
	int			world_h				= 2000;
	
	//int			screen_x 			= 0;
	//int			screen_y 			= 0;
	
	//int			mouse_x				= 0;
	//int			mouse_y				= 0;
	//int			mouse_dx			= 0;
	//int			mouse_dy			= 0;

	public final MutableVector2D	player_p = new MutableVector2D();
	public final MutableVector2D	player_v = new MutableVector2D();
	public final MutableVector2D   screen_p = new MutableVector2D();
	float		player_angle		= 0;
	int			player_missle_next_fire_frame; // used to space out missles
	
	int			player_button_flag	= 0; // bitflag indicates which button player is holding down
	
	Obj [] 		objects				= new Obj[MAX_OBJECTS];
	int			num_objects			= 0;
	
	Polygon2D []shapes= null;
	
	final int STAR_NEAR = 0;
	final int STAR_FAR  = 1;
	final int STAR_DISTANT = 2;
	final int NUM_STAR_TYPES = 3; // MUST BE LAST!
	
	final float STAR_RADIUS_MIN = 0.5f;
	final float STAR_RADIUS_MAX = 2f;

	final MutableVector2D [] star_p = new MutableVector2D[NUM_STARS];
	final int [] star_type = new int[NUM_STARS];
	final GColor [] star_color = new GColor[NUM_STAR_TYPES];

    GColor	BUTTON_OUTLINE_COLOR;
    GColor	BUTTON_FILL_COLOR;
    GColor	BUTTON_TEXT_COLOR;
    GColor	BUTTON_H_OUTLINE_COLOR;
    GColor	BUTTON_H_TEXT_COLOR;
    GColor  PLAYER_COLOR;
    GColor  PLAYER_MISSLE_COLOR;
	
    void initColors(AGraphics g) {
        BUTTON_OUTLINE_COLOR    = GColor.RED;
        BUTTON_FILL_COLOR       = GColor.DARK_GRAY;
        BUTTON_TEXT_COLOR       = GColor.BLUE;
        BUTTON_H_OUTLINE_COLOR  = GColor.YELLOW;
        BUTTON_H_TEXT_COLOR     = GColor.YELLOW;
        PLAYER_COLOR            = GColor.GREEN;
        PLAYER_MISSLE_COLOR     = GColor.YELLOW;
        GColor w = GColor.WHITE;
        for (int i=0; i<NUM_STAR_TYPES; i++) {
        	star_color[i] = w;
        	w = w.darkened(1.0f/NUM_STAR_TYPES);
        }
    }	

    public void pressButton(int button) {
        player_button_flag |= button;
    }

    public void releaseButton(int button) {
        player_button_flag &= ~button;

        if (button == PLAYER_BUTTON_SHOOT) {
            player_missle_next_fire_frame = 0;
        }
    }

	/////////////////////////////////////////////////////////////////
	// OVERRIDDEN FUNCTIONS - Called when window is resized
	
	//---------------------------------------------------------------
	

	//---------------------------------------------------------------
	Polygon2D makePlayerShape() {
		Vector2D [] pts = {
				new Vector2D(3,0),
				new Vector2D(-2,2),
				new Vector2D(-1,0),
				new Vector2D(-2,-2)
		};
		
		return new Polygon2D (pts, PLAYER_COLOR, PLAYER_RADIUS);
	}
	
	//---------------------------------------------------------------
	// randomly generate a 'rock'
	Polygon2D makeRockShape(AGraphics g, int num_pts) {
		
		//ArrayList angles = new ArrayList(num_pts);
		//for (int i=0; i<num_pts; i++)
		//	angles.add(i, new Integer(Utils.randRange(0,359)));
		//Collections.sort(angles);
		
		int range = Math.round(360f / num_pts);
		
		int [] angles = new int[num_pts];
		for (int i=0; i<num_pts; i++)
			angles[i] = range*i + Utils.rand() % range;
		Arrays.sort(angles);		
		
		Vector2D [] pts = new Vector2D[num_pts];
		for (int i=0; i<num_pts; i++)
		{
			float angle = Math.round(angles[i] * CMath.DEG_TO_RAD);
			
			float x = (float)Math.cos(angle);
			float y = (float)Math.sin(angle);
			float len = 1 + Utils.randFloat(2);
			pts[i] = new Vector2D(x*len, y*len);
		}
		int red = Utils.randRange(0,50);
		int grn = 255-Utils.randRange(0,50);
		int blu = Utils.randRange(0,50);
		GColor color = new GColor(red,grn,blu);
		return new Polygon2D(pts, color, 1);
	}
	
	Polygon2D makeRandomThrustShape(AGraphics g)
	{
		Vector2D [] pts = {
				new Vector2D(-2, 0),
				new Vector2D(-5 + Utils.randRange(-1,1), -2 + Utils.randRange(0,1)),
				new Vector2D(-11+Utils.randRange(-2,-2),  0 + Utils.randRange(-2,2)),
				new Vector2D(-5 + Utils.randRange(-1,1),  2 + Utils.randRange(0,1))
		};
		Polygon2D p = new Polygon2D(pts, GColor.RED, PLAYER_RADIUS);
		p.translate(-PLAYER_RADIUS*3/4, 0);		
		return p;
	}
	
	//---------------------------------------------------------------
	void initShapes(AGraphics g) {
	    if (shapes != null)
	        return;
        shapes = new Polygon2D[NUM_SHAPES];
		shapes[SHAPE_PLAYER] 	= makePlayerShape();
		shapes[SHAPE_ROCK_I] 	= makeRockShape(g, 5);
		shapes[SHAPE_ROCK_II] 	= makeRockShape(g, 7);
		shapes[SHAPE_ROCK_III] 	= makeRockShape(g, 7);
		shapes[SHAPE_ROCK_IV] 	= makeRockShape(g, 9);
		for (int i=0; i<NUM_THRUST_SHAPES; i++)
			shapes[SHAPE_THRUST+i] = makeRandomThrustShape(g);
	}
	
	//---------------------------------------------------------------
	// called once at beginning af app
	// do all allocation here, NOT during game
	public void doInitialization() {
		// alloc all objects
        // allocate all the objects
        for (int i=0; i<MAX_OBJECTS; i++)
            objects[i] = new Obj();
        // Init Stars
	}
	
	private void initStars(AGraphics g) {
		int sw = g.getViewportWidth();
		int sh = g.getViewportHeight();
		for (int i=0; i<NUM_STARS; i++) {
			star_p[i] = new MutableVector2D(Utils.randRange(-sw/2, sw/2), Utils.randRange(-sh/2,  sh/2));
			star_type[i] = Utils.rand() % NUM_STAR_TYPES;
		}
	}
	
	public void initGraphics(AGraphics g) {
        initColors(g);
		initShapes(g);
		initStars(g);
        addRocks(64);
	}
	
	//---------------------------------------------------------------
	// called once per frame
	public void drawFrame(AGraphics g) {

	    if (this.PLAYER_COLOR == null) {
	        initGraphics(g);
	    }

	    // clear screen
		g.clearScreen(GColor.BLACK);
		
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
		//mouse_dx = mouse_dy = 0;
	}
	
	// FUNCTIONS
	
	//---------------------------------------------------------------
	void drawStart(AGraphics g) {
		
	}
	
	//---------------------------------------------------------------
	void drawIntro(AGraphics g) {
		
	}
	
	//---------------------------------------------------------------
	void doPlayerThrust() {
		Vector2D dv = Vector2D.newTemp(PLAYER_THRUST_POWER, 0).rotate(player_angle);
		player_v.addEq(dv);
	}
	
	//---------------------------------------------------------------
	boolean isButtonDown(int button) {
		if ((player_button_flag & button) != 0)
			return true;
		return false;
	}

	//---------------------------------------------------------------
	void addPlayerMissle() {
		Obj obj = this.findUnusedObject();
		if (obj != null)
		{
            obj.position.assign(Vector2D.newTemp().rotate(player_angle).scaledBy(PLAYER_RADIUS - 3)).addEq(player_p);
            obj.velocity.assign(Vector2D.newTemp(PLAYER_MISSLE_SPEED, 0).rotateEq(player_angle)).addEq(player_v);
            obj.angle = player_angle;
            obj.type = TYPE_PLAYER_MISSLE;
            obj.start_frame = this.getFrameNumber();
        }
		player_missle_next_fire_frame = getFrameNumber() + PLAYER_SHOOT_RATE;
	}
	
	//---------------------------------------------------------------
	void doPlayerMissle() {
		if (this.getFrameNumber() >= player_missle_next_fire_frame)
			addPlayerMissle();
	}
	
	//---------------------------------------------------------------
	void updatePlayer() {
		if (isButtonDown(PLAYER_BUTTON_RIGHT))
			player_angle -= PLAYER_ROTATE_SPEED;
		if (isButtonDown(PLAYER_BUTTON_LEFT))
			player_angle += PLAYER_ROTATE_SPEED;
		if (isButtonDown(PLAYER_BUTTON_THRUST))
			doPlayerThrust();
		if (isButtonDown(PLAYER_BUTTON_SHOOT))
			doPlayerMissle();	
		player_p.addEq(player_v);
		wrapPosition(player_p);
		player_p.sub(Vector2D.newTemp(getScreenWidth()/2, getScreenHeight()/2), screen_p);
	}
	
	int wrapPosition(MutableVector2D p) {
		int changed = 0;
		if (p.X() < 0) {
			p.setX(p.X() + world_w);
			changed = 1;
		} else if (p.X() > world_w) {
			p.setX(p.X() - world_w);
			changed = 1;
		}
		if (p.Y() < 0) {
			p.setY(p.Y() + world_h);
			changed |= 2;
		} else if (p.Y() > world_h) {
			p.setY(p.Y() - world_h);
			changed |= 2;
		}
		return changed;
	}
	
	//---------------------------------------------------------------
	void updateStar(int index, Vector2D dv) {
		final int screen_w = getScreenWidth();
		final int screen_h = getScreenHeight();

		MutableVector2D star = star_p[index];
		star.addEq(dv);
		if (star.X() < -screen_w/2) {
			star.setX(star.X() + screen_w);
			star_type[index] = Utils.rand() % NUM_STAR_TYPES;
		} else if (star.X() > screen_w/2) {
			star.setX(star.X() - screen_w);
			star_type[index] = Utils.rand() % NUM_STAR_TYPES;
		}
		
		if (star.Y() < -screen_h/2) {
			star.setY(star.Y() + screen_h);
			star_type[index] = Utils.rand() % NUM_STAR_TYPES;
		} else if (star.Y() > screen_h/2) {
			star.setY(star.Y() - screen_h);
			star_type[index] = Utils.rand() % NUM_STAR_TYPES;
		}
	}
	
	//---------------------------------------------------------------
	void drawPlayer(AGraphics g, int thrust) {
	    g.pushMatrix();
	    g.setIdentity();
		//translateToPlayerPos(g);
	    //g.translate(player_p.scale(-1));
		g.rotate(player_angle);
		//g.scale(PLAYER_RADIUS, PLAYER_RADIUS);
		shapes[SHAPE_PLAYER].draw(g);
		if (thrust>=0)
			shapes[thrust].draw(g);
		g.popMatrix();
	}
	
	//---------------------------------------------------------------
	void drawRock(AGraphics g, Obj obj) {
		MutableVector2D temp = Vector2D.newTemp();
		if (isOnScreen(obj, temp)) {
			g.pushMatrix();
			g.translate(temp);
			g.pushMatrix();
			g.rotate(obj.angle);
			g.scale(obj.radius, obj.radius);
			shapes[obj.shape_index].draw(g);
			g.popMatrix();
			g.setColor(GColor.WHITE);
			//g.drawString(obj.position.toString(), 0, 0); // debug
			g.popMatrix();
		}
	}
	
	Vector2D getWorldDelta(Vector2D A, Vector2D B) {
		float w2= world_w/2;
		float h2= world_h/2;
		MutableVector2D D = Vector2D.newTemp(B.sub(A));
		if (D.X() > w2) {
			D.setX(D.X() - world_w);
		} else if (D.X() < -w2) {
			D.setX(D.X() + world_w);
		}
		
		if (D.Y() > h2) {
			D.setY(D.Y() - world_h);
		} else if (D.Y() < -h2) {
			D.setY(D.Y() + world_h);
		}
		
		return D;
	}
	
	boolean isOnScreen(Obj obj, MutableVector2D screenP) {
		float sw = getScreenWidth();
		float sh = getScreenHeight();
		Vector2D dv = getWorldDelta(player_p, obj.position);
		if (Math.abs(dv.X()) - obj.radius > sw/2)
			return false;
		if (Math.abs(dv.Y()) - obj.radius > sh/2)
			return false;
//		Vector2D S = player_p.sub(Vector2D.newTemp(getScreenWidth()/2, getScreenHeight()/2));
		//screenP.set(dv.sub(S));
		dv.sub(screen_p, screenP);
		return true;
	}
	
	//---------------------------------------------------------------
	void freeObject(Obj obj) {
		obj.type = TYPE_UNUSED;
	}

	//---------------------------------------------------------------
	void updatePlayerMissle(Obj obj) {
		if (this.getFrameNumber() - obj.start_frame > PLAYER_MISSLE_DURATION)
		{
			freeObject(obj);
		}
		else
		{
			obj.position.addEq(obj.velocity);
			wrapPosition(obj.position);
		}
	}
	
	//---------------------------------------------------------------
	void doRockExplosion()
	{
		
	}
	
	//---------------------------------------------------------------
	void doRockCollision(Obj obj) {
		freeObject(obj);
		if (obj.radius <= ROCK_RADIUS_SMALL)
		{
			doRockExplosion();
		}
		else if (obj.radius <= ROCK_RADIUS_MEDIUM)
		{
			int x = Math.round(obj.position.X());
			int y = Math.round(obj.position.Y());
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-5,5), ROCK_RADIUS_SMALL);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-5,5), ROCK_RADIUS_SMALL);
		}
		else
		{
			int x = Math.round(obj.position.X());
			int y = Math.round(obj.position.Y());
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
			addRock(x+Utils.randRange(-10,10), y+Utils.randRange(-10,10), ROCK_RADIUS_MEDIUM);
		}
	}
	
	//---------------------------------------------------------------
	void doCollisionDetection()
	{
		for (int i=0; i<num_objects; i++)
		{
			Obj obj = objects[i];
			
			if (obj.type == TYPE_PLAYER_MISSLE)
			{
				for (int j=0; j<num_objects; j++)
				{
					if (i==j)
						continue;
					
					Obj obj2 = objects[j];
					
					if (obj2.type == TYPE_ROCK)
					{
						int px = Math.round(obj.position.X());
						int py = Math.round(obj.position.Y());
						int cx = Math.round(obj2.position.X());
						int cy = Math.round(obj2.position.Y());
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
	void updateRock(Obj obj) {
		obj.position.addEq(obj.velocity);
		wrapPosition(obj.position);
		obj.angle += obj.spin;
	}

	//---------------------------------------------------------------
	void drawPixel(AGraphics g, float x, float y) {
		g.drawRect(x,y,1,1);
	}

	//---------------------------------------------------------------
	void drawPlayerMissle(AGraphics g, Obj obj) {
		g.setColor(PLAYER_MISSLE_COLOR);
		MutableVector2D P = Vector2D.newTemp();
		if (isOnScreen(obj, P)) {
			drawPixel(g,P.X(), P.Y());
		}
	}
	
	//---------------------------------------------------------------
	void updateObjects() {
		for (int i=0; i<num_objects; i++)
		{
			Obj obj = objects[i];
			
			switch (obj.type) {
			case TYPE_PLAYER_MISSLE:
				updatePlayerMissle(obj);
				break;
				
			case TYPE_ROCK:
				updateRock(obj);
				break;
			}
		}
	}
	
	void updateStars() {
		for (int i=0; i<NUM_STARS; i++) {
			switch (star_type[i]){
			case STAR_NEAR:
				updateStar(i, player_v.scaledBy(-1));
				break;
			case STAR_FAR:
				updateStar(i, player_v.scaledBy(-0.5f));
				break;
			case STAR_DISTANT:
				updateStar(i, player_v.scaledBy(-0.2f));
				break;
			}
		}
	}

	void translateToPlayerPos(AGraphics g) {
		g.translate(player_p.sub(Vector2D.newTemp(getScreenWidth()/2, getScreenHeight()/2)));
	}
	
	//---------------------------------------------------------------
	void drawObjects(AGraphics g) {
		g.pushMatrix();
		g.setIdentity();
		translateToPlayerPos(g);
		for (int i=0; i<num_objects; i++) {
			Obj obj = objects[i];
			switch (obj.type) {
			case TYPE_PLAYER_MISSLE:
				drawPlayerMissle(g,obj);
				break;
				
			case TYPE_ROCK:
				drawRock(g, obj);
				break;
			}
		}
		g.setColor(GColor.WHITE);
		g.drawRect(0, 0, world_w, world_h);
		g.popMatrix();
	}

	void drawStars(AGraphics g) {
		for (int i=0; i<NUM_STARS; i++) {
			g.setColor(star_color[star_type[i]]);
			float t = (float)star_type[i]/NUM_STAR_TYPES;
			float radius = STAR_RADIUS_MIN + (STAR_RADIUS_MAX-STAR_RADIUS_MIN) * (1-t);
			g.drawFilledCircle(star_p[i].X(), star_p[i].Y(), radius);
		}		
	}
	
	//---------------------------------------------------------------
	void drawPlay(AGraphics g) {
		updateObjects();
		updatePlayer();
		updateStars();
		doCollisionDetection();
		drawStars(g);
		drawObjects(g);
		int thrust = -1;
		if (isButtonDown(PLAYER_BUTTON_THRUST))
			thrust = SHAPE_THRUST + Utils.rand()%NUM_THRUST_SHAPES;
		drawPlayer(g, thrust);
		drawDebug(g);
	}
	
	void drawDebug(AGraphics g) {
		if (!Utils.isDebugEnabled())
			return;
		
		final int cx = getScreenWidth()/2;
		final int cy = getScreenHeight()/2;
		
/*
		int angle = getFrameNumber() % 360;
		Vector2D v = Vector2D.newTemp(100,0).rotateEq(angle);
		g.setColor(g.WHITE);
		g.drawLine(0, 0, v.X(), v.Y());
*/
		String str = String.format("P: %s\nV: %s\nA: %d", 
				player_p,
				player_v, 
				Math.round(player_angle));
		
		// draw the player's position
		g.setColor(GColor.CYAN);
		g.drawJustifiedString(cx, cy, Justify.RIGHT, Justify.TOP, str);

	}
	
	//---------------------------------------------------------------
	void drawExploding(AGraphics g) {
		
	}
	
	//---------------------------------------------------------------
	void drawGameOver(AGraphics g) {
	}

	//---------------------------------------------------------------
	// return a ptr to an unused object, or null of none are available
	Obj findUnusedObject() {
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
	void addRocks(int num_rocks) {
		for (int i=0; i<num_rocks; i++)
		{
			int x = Utils.randRange(0,world_w);
			int y = Utils.randRange(0,world_h);
			addRock(x,y,ROCK_RADIUS_LARGE);
		}
	}
	
	//---------------------------------------------------------------
	void addRock(int x, int y, int radius) {
		Obj obj = findUnusedObject();
		if (obj == null)
			return;
        obj.position.assign(x, y);
        obj.angle = Utils.randRange(0, 360);
		obj.spin		= Utils.randRange(2,6);
		obj.radius		= radius;
		obj.start_frame = getFrameNumber();
		obj.type		= TYPE_ROCK;
		float velocity	= Utils.randFloat(5) + 1;
        obj.velocity.assign(velocity, 0).rotateEq(obj.angle);
        obj.shape_index = Utils.randRange(SHAPE_ROCK_I, SHAPE_ROCK_IV);
	}	
	

}
