package cc.android.pacboy

import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import cc.lib.android.BaseRenderer
import cc.lib.android.GL10Graphics
import cc.lib.game.GColor
import cc.lib.game.IVector2D
import cc.lib.game.Maze
import cc.lib.game.Maze.Compass
import cc.lib.game.Utils
import cc.lib.math.Vector2D
import java.util.LinkedList

internal class PacBoyRenderer(parent: GLSurfaceView?) : BaseRenderer(parent), OnTouchListener {
	val TAG = "PacBoy"
	private val STATE_READY = 0 // waiting for use to touch near the start
	private val STATE_PLAYING = 1 // user laying their path
	private val STATE_CHASING = 10 // pacboy shasing the user
	private val STATE_EATEN = 11 // pacboy has caught the user
	private val STATE_SOLVED = 12 // user has solved the maze
	private val STATE_GAME_OVER = 13 // user has no more lives
	private val STATE_INTRO = 100
	private var maze: Maze? = null
	private val path = LinkedList<IVector2D>()
	private var tx = 0f
	private var ty = 0f // touch point
	private var scalex = 0f
	private var scaley = 0f // scale
	private var ix = 0f
	private var iy = 0f // starting cell
	private var ex = 0f
	private var ey = 0f // ending cell
	private val pb = PacBoy()
	var difficulty = 0
		private set
	private var lives = 0
	var score = 0
		private set
	private var state = STATE_READY
	private var frame = 0
	private var startChasePts = 10
	private val solution: List<Compass>? = null
	private fun findClosestPathIndex(x: Float, y: Float, useTooClose: Boolean): Int {
		var closest: IVector2D? = null
		var closestIndex = -1
		var minD = 0f
		var d = 0f
		var tooClose = false
		var tooFar = true
		for (i in path.indices.reversed()) {
			val vv = path[i]
			d = Utils.distSqPointPoint(vv.x, vv.y, x, y)
			if (useTooClose && d <= 0.3f) {
				tooClose = true
				break
			}
			if (d <= 1) {
				tooFar = false
				if (closest == null || d < minD) {
					closest = vv
					closestIndex = i
					minD = d
				}
			}
		}
		if (closest != null && !tooClose && !tooFar) {
			val x0 = closest.x.toInt()
			val y0 = closest.y.toInt()
			val x1 = x.toInt()
			val y1 = y.toInt()
			if (maze!!.isOpen(x0, y0, x1, y1)) {
				return closestIndex
			}
		}
		return -1
	}

	@Synchronized
	override fun onTouch(v: View, event: MotionEvent): Boolean {
		if (maze == null || graphics == null) return false
		tx = event.x
		ty = event.y
		tx /= scalex
		ty /= scaley
		val x = tx
		val y = ty
		if (x > 0 && y > 0 && x < maze!!.width && y < maze!!.height) {
			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					v.performClick()
					synchronized(path) {
						if (path.size > 0) {
							// find the point we are closest too and remove all in front
							val index = findClosestPathIndex(x, y, false)
							if (index > 0) {
								while (path.size > 0 && index < path.size) path.removeLast()
							}
							repaint()
						}
					}
				}

				MotionEvent.ACTION_MOVE -> {
					if (state < STATE_EATEN) {
						val d = 0f
						if (path.size == 0) {
							//if ((d = Utils.distSqPointPoint(x, y, ix, iy)) < 0.5) {
							if (maze!!.startX == x.toInt() && maze!!.startY == y.toInt()) {
								addPath(x, y)
								setState(STATE_PLAYING)
								repaint()
							}
							Log.d("Dist", "d=$d")
						} else {
							/*
        					//IVector2D top = path.get(path.size()-1);
        					IVector2D closest = null;
        					float minD = 0;
        					boolean tooClose = false;
        					boolean tooFar = true;
        					for (int i=path.size()-1; i>=0; i--) {
        						IVector2D vv = path.get(i);
        						d=Utils.distSqPointPoint(vv.getX(), vv.getY(), x, y);
        						if (d<= 0.3f) {
        							tooClose = true;
        							break;
        						}
        						if (d<=1) {
        							tooFar = false;
        							if (closest == null || d < minD) {
        								closest = vv;
        								minD = d;
        							} 
        						}
        					}
        					
        					if (closest != null && !tooClose && !tooFar) {
        						int x0 = (int)closest.getX();
        						int y0 = (int)closest.getY();
        						int x1 = (int)x;
        						int y1 = (int)y;
        						
        						if (maze.isOpen(x0,y0,x1,y1)) {
            						addPath(x,y);
            						//if ((d=Utils.distSqPointPoint(ex, ey, x, y)) < 1) {
                					if (maze.getEndX() == (int)x && maze.getEndY() == (int)y) {
                						setState(STATE_SOLVED);
                					}
                					repaint();
                					break;
        						}
    
        					}*/
							val index = findClosestPathIndex(x, y, true)
							if (index >= 0) {
								addPath(x, y)
								//if ((d=Utils.distSqPointPoint(ex, ey, x, y)) < 1) {
								if (maze!!.endX == x.toInt() && maze!!.endY == y.toInt()) {
									if (difficulty <= DIFFICULTY_NO_CHASE) {
										loadNextLevel()
									} else {
										setState(STATE_SOLVED)
									}
								}
								repaint()
							}
						}
					}
				}
			}
		}
		return true
	}

	private fun addPath(x: Float, y: Float) {
		synchronized(path) { path.addLast(Vector2D(x, y)) }
		if (state == STATE_PLAYING && difficulty > DIFFICULTY_NO_CHASE && path.size < 10) {
			setState(STATE_CHASING)
		}
	}

	override fun drawFrame(g: GL10Graphics) {
		if (maze != null) {
			scalex = (g.viewportWidth / maze!!.width).toFloat()
			scaley = (g.viewportHeight / maze!!.height).toFloat()
		}
		g.pushMatrix()
		g.scale(scalex, scaley)
		when (state) {
			STATE_READY -> {
				targetFPS = 30
				drawReady(g)
			}

			STATE_PLAYING -> {
				targetFPS = 0
				drawPlaying(g)
			}

			STATE_CHASING -> {
				targetFPS = 20
				drawChasing(g)
			}

			STATE_EATEN -> {
				targetFPS = 20
				drawEaten(g)
			}

			STATE_SOLVED -> drawSolved(g)
			STATE_GAME_OVER -> drawGameOver(g)
			STATE_INTRO -> {
				targetFPS = 20
				drawIntro(g)
			}
		}
		g.popMatrix()
		frame++
		if (difficulty > DIFFICULTY_NO_CHASE) {
			g.color = GColor.RED
			var x = 15f
			val y = (g.viewportHeight - 15).toFloat()
			for (i in 0 until lives) {
				g.drawFilledCircle(x, y, 10f)
				x += 30f
			}
		}
	}

	fun addToScore(pts: Int) {
		score += pts
	}

	fun drawMaze(g: GL10Graphics) {
		g.clearScreen(GColor.YELLOW)
		g.color = GColor.BLUE
		maze!!.draw(g, 5f)
	}

	private fun drawDots(g: GL10Graphics, size: Float) {
		g.begin()
		var index = 0
		synchronized(path) {
			for (v in path) {
				g.vertex(v)
				if (index++ % 64 == 0) {
					g.drawPoints(size)
					g.begin()
				}
			}
		}
		g.drawPoints(size)
		g.end()
	}

	private fun drawPath(g: GL10Graphics, path: List<Compass>) {
		var sx = maze!!.startX
		var sy = maze!!.startY
		g.begin()
		g.color = GColor.MAGENTA
		g.vertex(sx.toFloat(), sy.toFloat())
		for (c in path) {
			sx += c.dx
			sy += c.dy
			g.vertex(sx.toFloat(), sy.toFloat())
		}
		g.drawLineStrip(5f)
	}

	var pulse = floatArrayOf(0f, 1f, 2f, 3f, 2f, 1f, 0f)
	fun drawReady(g: GL10Graphics) {
		drawMaze(g)
		g.color = GColor.GREEN
		g.drawFilledCircle(ix, iy, 0.3f + 0.01f * pulse[frame % pulse.size])
		g.color = GColor.RED
		g.drawFilledCircle(ex, ey, 0.3f)
	}

	fun drawPlaying(g: GL10Graphics) {
		drawMaze(g)
		g.color = GColor.GREEN
		g.drawFilledCircle(ix, iy, 0.3f)
		g.color = GColor.RED
		g.drawFilledCircle(ex, ey, 0.3f + 0.01f * pulse[frame % pulse.size])
		drawDots(g, 8f)
		//drawPath(g, solution);
	}

	private fun drawChasing(g: GL10Graphics) {
		drawPlaying(g)
		g.color = GColor.BLACK
		pb.draw(g)
		if (path.size > 0) {
			synchronized(path) {
				if (pb.moveTo(path.first)) {
					path.removeFirst()
				}
			}
		} else {
			setState(STATE_EATEN)
		}
	}

	fun setState(newState: Int) {
		if (state == newState) return
		state = newState
		when (newState) {
			STATE_READY -> path.clear()
			STATE_PLAYING -> {}
			STATE_CHASING -> {
				pb.pos.assign(maze!!.startX, maze!!.startY)
				pb.reset()
			}

			STATE_EATEN -> frame = 0
			STATE_SOLVED -> {
				Log.d(TAG, "Solved")
				targetFPS = 5
			}

			STATE_GAME_OVER -> {
				state = STATE_GAME_OVER
				frame = 0
				targetFPS = 20
			}
		}
	}

	private fun drawEaten(g: GL10Graphics) {
		val colors = arrayOf(GColor.BLUE, GColor.YELLOW)
		val i0 = frame / 5 % 2
		val i1 = (i0 + 1) % 2
		g.clearScreen(colors[i0])
		g.color = colors[i1]
		val scale = 0.01f * frame
		//g.translate(-scale/2, -scale/2);
		maze!!.draw(g, 5f)
		pb.radius = 0.5f + scale
		pb.degrees += scale * 20
		pb.draw(g)
		if (frame > 100) {
			if (--lives <= 0) {
				setState(STATE_GAME_OVER)
			} else {
				setState(STATE_READY)
			}
		}
	}

	private fun drawFace(g: GL10Graphics, x: Float, y: Float, r: Float, tongue: Boolean) {
		g.color = GColor.BLACK
		g.drawFilledCircle(x, y, r)
		g.color = GColor.YELLOW
		g.begin()
		g.vertex(x - r / 3, y - r / 3)
		g.vertex(x + r / 3, y - r / 3)
		g.drawPoints(8f)
		g.end()
		if (tongue) {
			// draw a tongue
			g.color = GColor.RED
			g.drawFilledRect(x - r / 6, y + r / 4, r / 3, r / 3)
			g.drawFilledCircle(x, y + r / 4 + r / 3, r / 6)
		} else {
			g.begin()
			g.vertex(x - r / 3, y)
			g.vertex(x + r / 3, y + r / 3)
			g.drawLines(4f)
			g.end()
		}
	}

	private fun drawCleanup(g: GL10Graphics) {
		drawPlaying(g)
		g.color = GColor.BLACK
		pb.draw(g)
		if (path.size > 0) {
			synchronized(path) {
				if (pb.moveTo(path.first)) {
					path.removeFirst()
				}
			}
		} else {
			loadNextLevel()
		}
	}

	private fun loadNextLevel() {
		if (0 == difficulty % DIFFICULTY_INCREASE_MAZE_SIZE_MOD) {
			val width = maze!!.width + 2
			val height = maze!!.height + 1
			maze!!.resize(width, height)
		}
		if (DIFFICULTY_NO_CHASE > DIFFICULTY_NO_CHASE && difficulty % DIFFICULTY_INCREASE_PACBOY_MAX_SPEED_MOD == 0) {
			pb.maxSpeed += 0.1f
		}
		newMaze()
	}

	private fun drawSolved(g: GL10Graphics) {
		drawPlaying(g)
		g.color = GColor.RED
		g.drawFilledCircle(ex, ey, 0.3f)
		g.color = GColor.ORANGE
		drawDots(g, 10f)
		val x = pb.pos.x
		val y = pb.pos.y
		val r = pb.radius
		g.color = GColor.BLACK
		g.drawFilledCircle(x, y, r)
		g.color = GColor.YELLOW
		g.begin()
		g.vertex(x - r / 3, y - r / 3)
		g.vertex(x + r / 3, y - r / 3)
		g.drawPoints(8f)
		g.end()
		drawFace(g, x, y, r, path.size <= 10)
		if (path.size == 0) {
			loadNextLevel()
		} else {
			synchronized(path) {
				score += path.size
				path.removeFirst()
				if (path.size % 3 == 0) {
					targetFPS = targetFPS + 1
				}
			}
		}
	}

	fun drawGameOver(g: GL10Graphics) {
		drawPlaying(g)
		val r = (maze!!.height / 4).toFloat()
		val x = (maze!!.width / 2).toFloat()
		var y = maze!!.height + r * 2
		val step = maze!!.height * 0.02f
		y -= step * frame
		if (y < maze!!.height / 2) {
			y = (maze!!.height / 2).toFloat()
			drawFace(g, x, y, r, true)
			targetFPS = 0
		} else {
			drawFace(g, x, y, r, false)
		}
	}

	fun drawIntro(g: GL10Graphics) {
		val wid = g.viewportWidth.toFloat()
		val hgt = g.viewportHeight.toFloat()
		if (frame == 0) {
			pb.radius = hgt / 6
			pb.pos.assign(-pb.radius, hgt / 2)
		}
		val secondsToCenter = 3
		val framesToCenter = secondsToCenter * targetFPS
		val speed = (wid / 2 + pb.radius) / framesToCenter
		g.clearScreen(GColor.YELLOW)
		g.color = GColor.BLACK
		if (frame >= framesToCenter && frame < framesToCenter + targetFPS * 3) {
			if (frame < framesToCenter + targetFPS) {
				drawFace(g, pb.pos.x, pb.pos.y, pb.radius, false)
			} else if (frame < framesToCenter + targetFPS * 2) {
				drawFace(g, pb.pos.x, pb.pos.y, pb.radius, true)
			} else {
				drawFace(g, pb.pos.x, pb.pos.y, pb.radius, false)
			}
		} else {
			pb.pos.addEq(speed, 0)
			pb.draw(g)
		}
	}

	override fun init(g: GL10Graphics) {
		g.ortho()
		setDrawFPS(false)
		if (maze != null) {
			scalex = (graphics.viewportWidth / maze!!.width).toFloat()
			scaley = (graphics.viewportHeight / maze!!.height).toFloat()
		}
	}

	fun newMaze(width: Int, height: Int, difficulty: Int) {
		frame = 0
		this.difficulty = difficulty
		lives = 3
		score = 0
		if (difficulty == 0) {
			startChasePts = 20
		}
		Log.d(TAG, "difficulty = $difficulty")
		maze = Maze(width, height)
		newMaze()
	}

	fun newMaze() {
		Log.d(TAG, "newMaze")
		if (1 == difficulty % 2) {
			maze!!.generateDFS()
		} else {
			maze!!.generateBFS()
		}
		if (difficulty > 5) {
			maze!!.setStartEndToLongestPath()
		} else {
			maze!!.setStart(0, Utils.rand() % maze!!.height)
			maze!!.setEnd(maze!!.width - 1, Utils.rand() % maze!!.height)
		}
		path.clear()
		state = STATE_READY
		ix = 0.5f + maze!!.startX
		iy = 0.5f + maze!!.startY
		ex = 0.5f + maze!!.endX
		ey = 0.5f + maze!!.endY
		if (startChasePts > 5) startChasePts--
		difficulty++
		pb.reset()
		//solution = maze.findSolution();
	}

	fun setupIntro() {
		state = STATE_INTRO
		pb.reset()
		scaley = 1f
		scalex = scaley
		frame = 0
	}

	companion object {
		const val DIFFICULTY_NO_CHASE = 9
		const val DIFFICULTY_INCREASE_MAZE_SIZE_MOD = 5
		const val DIFFICULTY_INCREASE_PACBOY_MAX_SPEED_MOD = 3
	}
}