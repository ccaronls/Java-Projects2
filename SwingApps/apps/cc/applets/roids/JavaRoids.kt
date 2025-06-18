package cc.applets.roids

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.Justify
import cc.lib.game.Polygon2D
import cc.lib.game.Utils
import cc.lib.math.CMath
import cc.lib.math.MutableVector2D
import cc.lib.math.Vector2D
import cc.lib.math.Vector2D.Companion.newTemp
import java.util.Arrays

abstract class JavaRoids {
	abstract val frameNumber: Int
	abstract val screenWidth: Int
	abstract val screenHeight: Int

	// General purpose shape
	class Obj {
		val position = MutableVector2D()
		val velocity = MutableVector2D()
		var radius = 0f // radius
		var angle = 0f // heading
		var spin = 0f // ang speed (for rocks) 
		var type = 0 // type
		var start_frame = 0 // the frame this object was added on (for projectiles)
		var shape_index = 0 // index to the shape array for rendering
	}

	//////////////////////////////////////////////////////
	// CONSTANTS
	val TYPE_UNUSED = 0
	val TYPE_PLAYER_MISSLE = 1
	val TYPE_ROCK = 2
	val MAX_OBJECTS = 256

	// GAME STATES
	val STATE_INTRO = 0
	val STATE_START = 1
	val STATE_PLAY = 2
	val STATE_EXPLODING = 3
	val STATE_GAME_OVER = 4
	val SHAPE_PLAYER = 0
	val SHAPE_ROCK_I = 1
	val SHAPE_ROCK_II = 2
	val SHAPE_ROCK_III = 3
	val SHAPE_ROCK_IV = 4
	val SHAPE_LARGE_SAUCER = 5
	val SHAPE_SMALL_SAUCER = 6
	val SHAPE_THRUST = 7
	val SHAPE_THRUST1 = 8
	val SHAPE_THRUST2 = 9
	val SHAPE_THRUST3 = 10
	val NUM_SHAPES = 11 // MUST BE LAST!!!
	val NUM_THRUST_SHAPES = 4

	// BUTTONS That respond to press/release events
	//   Not like Hyperspace which is a typed event
	val PLAYER_FRICTION = 0.95f
	val PLAYER_RADIUS = 15.0f
	val PLAYER_ROTATE_SPEED = 8
	val PLAYER_THRUST_POWER = 0.25f
	val PLAYER_SHOOT_RATE = 10
	val PLAYER_MISSLE_DURATION = 100
	val PLAYER_MISSLE_SPEED = 10.0f
	val NUM_STARS = 32
	val ROCK_RADIUS_LARGE = 40
	val ROCK_RADIUS_MEDIUM = 20
	val ROCK_RADIUS_SMALL = 10

	//////////////////////////////////////////////////////
	// GAME VARS
	var game_state = STATE_PLAY

	// dimension of the world, player stays in middle 
	// the world wraps
	var world_w = 2000
	var world_h = 2000

	//int			screen_x 			= 0;
	//int			screen_y 			= 0;
	//int			mouse_x				= 0;
	//int			mouse_y				= 0;
	//int			mouse_dx			= 0;
	//int			mouse_dy			= 0;
	val player_p = MutableVector2D()
	val player_v = MutableVector2D()
	val screen_p = MutableVector2D()
	var player_angle = 0f
	var player_missle_next_fire_frame = 0 // used to space out missles
	var player_button_flag = 0 // bitflag indicates which button player is holding down
	var objects = arrayOfNulls<Obj>(MAX_OBJECTS)
	var num_objects = 0
	var shapes: Array<Polygon2D?>? = null
	val STAR_NEAR = 0
	val STAR_FAR = 1
	val STAR_DISTANT = 2
	val NUM_STAR_TYPES = 3 // MUST BE LAST!
	val STAR_RADIUS_MIN = 0.5f
	val STAR_RADIUS_MAX = 2f
	val star_p = Array(NUM_STARS) { MutableVector2D() }
	val star_type = IntArray(NUM_STARS)
	val star_color = Array<GColor>(NUM_STAR_TYPES) { GColor.WHITE.copyOf() }
	var BUTTON_OUTLINE_COLOR: GColor? = null
	var BUTTON_FILL_COLOR: GColor? = null
	var BUTTON_TEXT_COLOR: GColor? = null
	var BUTTON_H_OUTLINE_COLOR: GColor? = null
	var BUTTON_H_TEXT_COLOR: GColor? = null
	var PLAYER_COLOR: GColor? = null
	var PLAYER_MISSLE_COLOR: GColor? = null
	fun initColors(g: AGraphics?) {
		BUTTON_OUTLINE_COLOR = GColor.RED
		BUTTON_FILL_COLOR = GColor.DARK_GRAY
		BUTTON_TEXT_COLOR = GColor.BLUE
		BUTTON_H_OUTLINE_COLOR = GColor.YELLOW
		BUTTON_H_TEXT_COLOR = GColor.YELLOW
		PLAYER_COLOR = GColor.GREEN
		PLAYER_MISSLE_COLOR = GColor.YELLOW
		var w = GColor.WHITE
		for (i in 0 until NUM_STAR_TYPES) {
			star_color[i] = w
			w = w.darkened(1.0f / NUM_STAR_TYPES)
		}
	}

	fun pressButton(button: Int) {
		player_button_flag = player_button_flag or button
	}

	fun releaseButton(button: Int) {
		player_button_flag = player_button_flag and button.inv()
		if (button == PLAYER_BUTTON_SHOOT) {
			player_missle_next_fire_frame = 0
		}
	}

	/////////////////////////////////////////////////////////////////
	// OVERRIDDEN FUNCTIONS - Called when window is resized
	//---------------------------------------------------------------
	//---------------------------------------------------------------
	fun makePlayerShape(): Polygon2D {
		val pts = arrayOf(
			Vector2D(3, 0),
			Vector2D(-2, 2),
			Vector2D(-1, 0),
			Vector2D(-2, -2)
		)
		return Polygon2D(pts, PLAYER_COLOR!!, PLAYER_RADIUS)
	}

	//---------------------------------------------------------------
	// randomly generate a 'rock'
	fun makeRockShape(g: AGraphics?, num_pts: Int): Polygon2D {

		//ArrayList angles = new ArrayList(num_pts);
		//for (int i=0; i<num_pts; i++)
		//	angles.add(i, new Integer(Utils.randRange(0,359)));
		//Collections.sort(angles);
		val range = Math.round(360f / num_pts)
		val angles = IntArray(num_pts)
		for (i in 0 until num_pts) angles[i] = range * i + Utils.rand() % range
		Arrays.sort(angles)
		val pts = Array(num_pts) {
			val angle = Math.round(angles[it] * CMath.DEG_TO_RAD).toFloat()
			val x = Math.cos(angle.toDouble()).toFloat()
			val y = Math.sin(angle.toDouble()).toFloat()
			val len = 1 + Utils.randFloat(2f)
			Vector2D(x * len, y * len)
		}
		val red = Utils.randRange(0, 50)
		val grn = 255 - Utils.randRange(0, 50)
		val blu = Utils.randRange(0, 50)
		val color = GColor(red, grn, blu)
		return Polygon2D(pts, color, 1f)
	}

	fun makeRandomThrustShape(g: AGraphics?): Polygon2D {
		val pts = arrayOf(
			Vector2D(-2, 0),
			Vector2D(-5 + Utils.randRange(-1, 1), -2 + Utils.randRange(0, 1)),
			Vector2D(-11 + Utils.randRange(-2, -2), 0 + Utils.randRange(-2, 2)),
			Vector2D(-5 + Utils.randRange(-1, 1), 2 + Utils.randRange(0, 1))
		)
		val p = Polygon2D(pts, GColor.RED, PLAYER_RADIUS)
		p.translate(-PLAYER_RADIUS * 3 / 4, 0f)
		return p
	}

	//---------------------------------------------------------------
	fun initShapes(g: AGraphics?) {
		if (shapes != null) return
		shapes = arrayOfNulls(NUM_SHAPES)
		shapes!![SHAPE_PLAYER] = makePlayerShape()
		shapes!![SHAPE_ROCK_I] = makeRockShape(g, 5)
		shapes!![SHAPE_ROCK_II] = makeRockShape(g, 7)
		shapes!![SHAPE_ROCK_III] = makeRockShape(g, 7)
		shapes!![SHAPE_ROCK_IV] = makeRockShape(g, 9)
		for (i in 0 until NUM_THRUST_SHAPES) shapes!![SHAPE_THRUST + i] = makeRandomThrustShape(g)
	}

	//---------------------------------------------------------------
	// called once at beginning af app
	// do all allocation here, NOT during game
	fun doInitialization() {
		// alloc all objects
		// allocate all the objects
		for (i in 0 until MAX_OBJECTS) objects[i] = Obj()
		// Init Stars
	}

	private fun initStars(g: AGraphics) {
		val sw = g.viewportWidth
		val sh = g.viewportHeight
		for (i in 0 until NUM_STARS) {
			star_p[i] = MutableVector2D(Utils.randRange(-sw / 2, sw / 2), Utils.randRange(-sh / 2, sh / 2))
			star_type[i] = Utils.rand() % NUM_STAR_TYPES
		}
	}

	fun initGraphics(g: AGraphics) {
		initColors(g)
		initShapes(g)
		initStars(g)
		addRocks(64)
	}

	//---------------------------------------------------------------
	// called once per frame
	fun drawFrame(g: AGraphics) {
		if (PLAYER_COLOR == null) {
			initGraphics(g)
		}

		// clear screen
		g.clearScreen(GColor.BLACK)
		when (game_state) {
			STATE_INTRO -> drawIntro(g)
			STATE_START -> drawStart(g)
			STATE_PLAY -> drawPlay(g)
			STATE_EXPLODING -> drawExploding(g)
			STATE_GAME_OVER -> drawGameOver(g)
		}

		// reset the mouse deltas at the end of the frame
		//mouse_dx = mouse_dy = 0;
	}

	// FUNCTIONS
	//---------------------------------------------------------------
	fun drawStart(g: AGraphics?) {}

	//---------------------------------------------------------------
	fun drawIntro(g: AGraphics?) {}

	//---------------------------------------------------------------
	fun doPlayerThrust() {
		val dv: Vector2D = newTemp(PLAYER_THRUST_POWER, 0).rotate(player_angle)
		player_v.addEq(dv)
	}

	//---------------------------------------------------------------
	fun isButtonDown(button: Int): Boolean {
		return if (player_button_flag and button != 0) true else false
	}

	//---------------------------------------------------------------
	fun addPlayerMissle() {
		val obj = findUnusedObject()
		if (obj != null) {
			obj.position.assign(newTemp(1, 0).rotate(player_angle).scaledBy(PLAYER_RADIUS - 3)).addEq(player_p)
			obj.velocity.assign(newTemp(PLAYER_MISSLE_SPEED, 0).rotateEq(player_angle)).addEq(player_v)
			obj.angle = player_angle
			obj.type = TYPE_PLAYER_MISSLE
			obj.start_frame = frameNumber
		}
		player_missle_next_fire_frame = frameNumber + PLAYER_SHOOT_RATE
	}

	//---------------------------------------------------------------
	fun doPlayerMissle() {
		if (frameNumber >= player_missle_next_fire_frame) addPlayerMissle()
	}

	//---------------------------------------------------------------
	fun updatePlayer() {
		if (isButtonDown(PLAYER_BUTTON_RIGHT)) player_angle -= PLAYER_ROTATE_SPEED.toFloat()
		if (isButtonDown(PLAYER_BUTTON_LEFT)) player_angle += PLAYER_ROTATE_SPEED.toFloat()
		if (isButtonDown(PLAYER_BUTTON_THRUST)) doPlayerThrust()
		if (isButtonDown(PLAYER_BUTTON_SHOOT)) doPlayerMissle()
		player_p.addEq(player_v)
		wrapPosition(player_p)
		player_p.sub(newTemp(screenWidth / 2, screenHeight / 2), screen_p)
	}

	fun wrapPosition(p: MutableVector2D): Int {
		var changed = 0
		if (p.x < 0) {
			p.setX(p.x + world_w)
			changed = 1
		} else if (p.x > world_w) {
			p.setX(p.x - world_w)
			changed = 1
		}
		if (p.y < 0) {
			p.setY(p.y + world_h)
			changed = changed or 2
		} else if (p.y > world_h) {
			p.setY(p.y - world_h)
			changed = changed or 2
		}
		return changed
	}

	//---------------------------------------------------------------
	fun updateStar(index: Int, dv: Vector2D?) {
		val screen_w = screenWidth
		val screen_h = screenHeight
		val star = star_p[index]
		star!!.addEq(dv!!)
		if (star.x < -screen_w / 2) {
			star.setX(star.x + screen_w)
			star_type[index] = Utils.rand() % NUM_STAR_TYPES
		} else if (star.x > screen_w / 2) {
			star.setX(star.x - screen_w)
			star_type[index] = Utils.rand() % NUM_STAR_TYPES
		}
		if (star.y < -screen_h / 2) {
			star.setY(star.y + screen_h)
			star_type[index] = Utils.rand() % NUM_STAR_TYPES
		} else if (star.y > screen_h / 2) {
			star.setY(star.y - screen_h)
			star_type[index] = Utils.rand() % NUM_STAR_TYPES
		}
	}

	//---------------------------------------------------------------
	fun drawPlayer(g: AGraphics, thrust: Int) {
		g.pushMatrix()
		g.setIdentity()
		//translateToPlayerPos(g);
		//g.translate(player_p.scale(-1));
		g.rotate(player_angle)
		//g.scale(PLAYER_RADIUS, PLAYER_RADIUS);
		shapes!![SHAPE_PLAYER]!!.draw(g)
		if (thrust >= 0) shapes!![thrust]!!.draw(g)
		g.popMatrix()
	}

	//---------------------------------------------------------------
	fun drawRock(g: AGraphics, obj: Obj?) {
		val temp: MutableVector2D = newTemp()
		if (isOnScreen(obj, temp)) {
			g.pushMatrix()
			g.translate(temp)
			g.pushMatrix()
			g.rotate(obj!!.angle)
			g.scale(obj.radius, obj.radius)
			shapes!![obj.shape_index]!!.draw(g)
			g.popMatrix()
			g.color = GColor.WHITE
			//g.drawString(obj.position.toString(), 0, 0); // debug
			g.popMatrix()
		}
	}

	fun getWorldDelta(A: Vector2D?, B: Vector2D): Vector2D {
		val w2 = (world_w / 2).toFloat()
		val h2 = (world_h / 2).toFloat()
		val D = newTemp(B.sub(A!!))
		if (D.x > w2) {
			D.setX(D.x - world_w)
		} else if (D.x < -w2) {
			D.setX(D.x + world_w)
		}
		if (D.y > h2) {
			D.setY(D.y - world_h)
		} else if (D.y < -h2) {
			D.setY(D.y + world_h)
		}
		return D
	}

	fun isOnScreen(obj: Obj?, screenP: MutableVector2D?): Boolean {
		val sw = screenWidth.toFloat()
		val sh = screenHeight.toFloat()
		val dv = getWorldDelta(player_p, obj!!.position)
		if (Math.abs(dv.x) - obj.radius > sw / 2) return false
		if (Math.abs(dv.y) - obj.radius > sh / 2) return false
		//		Vector2D S = player_p.sub(Vector2D.newTemp(getScreenWidth()/2, getScreenHeight()/2));
		//screenP.set(dv.sub(S));
		dv.sub(screen_p, screenP!!)
		return true
	}

	//---------------------------------------------------------------
	fun freeObject(obj: Obj?) {
		obj!!.type = TYPE_UNUSED
	}

	//---------------------------------------------------------------
	fun updatePlayerMissle(obj: Obj?) {
		if (frameNumber - obj!!.start_frame > PLAYER_MISSLE_DURATION) {
			freeObject(obj)
		} else {
			obj.position.addEq(obj.velocity)
			wrapPosition(obj.position)
		}
	}

	//---------------------------------------------------------------
	fun doRockExplosion() {}

	//---------------------------------------------------------------
	fun doRockCollision(obj: Obj?) {
		freeObject(obj)
		if (obj!!.radius <= ROCK_RADIUS_SMALL) {
			doRockExplosion()
		} else if (obj.radius <= ROCK_RADIUS_MEDIUM) {
			val x = Math.round(obj.position.x).toInt()
			val y = Math.round(obj.position.y).toInt()
			addRock(x + Utils.randRange(-10, 10), y + Utils.randRange(-5, 5), ROCK_RADIUS_SMALL)
			addRock(x + Utils.randRange(-10, 10), y + Utils.randRange(-5, 5), ROCK_RADIUS_SMALL)
		} else {
			val x = Math.round(obj.position.x).toInt()
			val y = Math.round(obj.position.y).toInt()
			addRock(x + Utils.randRange(-10, 10), y + Utils.randRange(-10, 10), ROCK_RADIUS_MEDIUM)
			addRock(x + Utils.randRange(-10, 10), y + Utils.randRange(-10, 10), ROCK_RADIUS_MEDIUM)
			addRock(x + Utils.randRange(-10, 10), y + Utils.randRange(-10, 10), ROCK_RADIUS_MEDIUM)
		}
	}

	//---------------------------------------------------------------
	fun doCollisionDetection() {
		for (i in 0 until num_objects) {
			val obj = objects[i]
			if (obj!!.type == TYPE_PLAYER_MISSLE) {
				for (j in 0 until num_objects) {
					if (i == j) continue
					val obj2 = objects[j]
					if (obj2!!.type == TYPE_ROCK) {
						val px = Math.round(obj.position.x).toInt()
						val py = Math.round(obj.position.y).toInt()
						val cx = Math.round(obj2.position.x).toInt()
						val cy = Math.round(obj2.position.y).toInt()
						val rad = Math.round(obj2.radius)
						if (Utils.isPointInsideRect(px.toFloat(), py.toFloat(), (cx - rad).toFloat(), (cy - rad).toFloat(), (rad * 2).toFloat(), (rad * 2).toFloat())) {
							doRockCollision(obj2)
							freeObject(obj)
							break
						}
					}
				}
			}
		}
	}

	//---------------------------------------------------------------
	fun updateRock(obj: Obj?) {
		obj!!.position.addEq(obj.velocity)
		wrapPosition(obj.position)
		obj.angle += obj.spin
	}

	//---------------------------------------------------------------
	fun drawPixel(g: AGraphics, x: Float, y: Float) {
		g.drawRect(x, y, 1f, 1f)
	}

	//---------------------------------------------------------------
	fun drawPlayerMissle(g: AGraphics, obj: Obj?) {
		g.color = PLAYER_MISSLE_COLOR
		val P: MutableVector2D = newTemp()
		if (isOnScreen(obj, P)) {
			drawPixel(g, P.x, P.y)
		}
	}

	//---------------------------------------------------------------
	fun updateObjects() {
		for (i in 0 until num_objects) {
			val obj = objects[i]
			when (obj!!.type) {
				TYPE_PLAYER_MISSLE -> updatePlayerMissle(obj)
				TYPE_ROCK -> updateRock(obj)
			}
		}
	}

	fun updateStars() {
		for (i in 0 until NUM_STARS) {
			when (star_type[i]) {
				STAR_NEAR -> updateStar(i, player_v.scaledBy(-1))
				STAR_FAR -> updateStar(i, player_v.scaledBy(-0.5f))
				STAR_DISTANT -> updateStar(i, player_v.scaledBy(-0.2f))
			}
		}
	}

	fun translateToPlayerPos(g: AGraphics) {
		g.translate(player_p.sub(newTemp(screenWidth / 2, screenHeight / 2)))
	}

	//---------------------------------------------------------------
	fun drawObjects(g: AGraphics) {
		g.pushMatrix()
		g.setIdentity()
		translateToPlayerPos(g)
		for (i in 0 until num_objects) {
			val obj = objects[i]
			when (obj!!.type) {
				TYPE_PLAYER_MISSLE -> drawPlayerMissle(g, obj)
				TYPE_ROCK -> drawRock(g, obj)
			}
		}
		g.color = GColor.WHITE
		g.drawRect(0f, 0f, world_w.toFloat(), world_h.toFloat())
		g.popMatrix()
	}

	fun drawStars(g: AGraphics) {
		for (i in 0 until NUM_STARS) {
			g.color = star_color[star_type[i]]
			val t = star_type[i].toFloat() / NUM_STAR_TYPES
			val radius = STAR_RADIUS_MIN + (STAR_RADIUS_MAX - STAR_RADIUS_MIN) * (1 - t)
			g.drawFilledCircle(star_p[i].x, star_p[i].y, radius)
		}
	}

	//---------------------------------------------------------------
	fun drawPlay(g: AGraphics) {
		updateObjects()
		updatePlayer()
		updateStars()
		doCollisionDetection()
		drawStars(g)
		drawObjects(g)
		var thrust = -1
		if (isButtonDown(PLAYER_BUTTON_THRUST)) thrust = SHAPE_THRUST + Utils.rand() % NUM_THRUST_SHAPES
		drawPlayer(g, thrust)
		drawDebug(g)
	}

	fun drawDebug(g: AGraphics) {
		if (!Utils.isDebugEnabled()) return
		val cx = screenWidth / 2
		val cy = screenHeight / 2

		/*
		int angle = getFrameNumber() % 360;
		Vector2D v = Vector2D.newTemp(100,0).rotateEq(angle);
		g.setColor(g.WHITE);
		g.drawLine(0, 0, v.x, v.y);
*/
		val str = String.format("P: %s\nV: %s\nA: %d",
			player_p,
			player_v,
			Math.round(player_angle))

		// draw the player's position
		g.color = GColor.CYAN
		g.drawJustifiedString(cx, cy, Justify.RIGHT, Justify.TOP, str)
	}

	//---------------------------------------------------------------
	fun drawExploding(g: AGraphics?) {}

	//---------------------------------------------------------------
	fun drawGameOver(g: AGraphics?) {}

	//---------------------------------------------------------------
	// return a ptr to an unused object, or null of none are available
	fun findUnusedObject(): Obj? {
		for (i in 0 until num_objects) {
			if (objects[i]!!.type == TYPE_UNUSED) return objects[i]
		}
		return if (num_objects < MAX_OBJECTS) objects[num_objects++] else null
	}

	//---------------------------------------------------------------
	fun addRocks(num_rocks: Int) {
		for (i in 0 until num_rocks) {
			val x = Utils.randRange(0, world_w)
			val y = Utils.randRange(0, world_h)
			addRock(x, y, ROCK_RADIUS_LARGE)
		}
	}

	//---------------------------------------------------------------
	fun addRock(x: Int, y: Int, radius: Int) {
		val obj = findUnusedObject() ?: return
		obj.position.assign(x, y)
		obj.angle = Utils.randRange(0, 360).toFloat()
		obj.spin = Utils.randRange(2, 6).toFloat()
		obj.radius = radius.toFloat()
		obj.start_frame = frameNumber
		obj.type = TYPE_ROCK
		val velocity = Utils.randFloat(5f) + 1
		obj.velocity.assign(velocity, 0).rotateEq(obj.angle)
		obj.shape_index = Utils.randRange(SHAPE_ROCK_I, SHAPE_ROCK_IV)
	}

	companion object {
		const val PLAYER_BUTTON_RIGHT = 1
		const val PLAYER_BUTTON_LEFT = 2
		const val PLAYER_BUTTON_THRUST = 4
		const val PLAYER_BUTTON_SHOOT = 8
		const val PLAYER_BUTTON_SHIELD = 16
	}
}
