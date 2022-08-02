package cc.lib.probot

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.utils.Lock
import cc.lib.utils.Reflector
import java.util.*

interface Observer {
	fun onChanged()
}

class ObservableArrayList<T>(initalCapacity: Int = 0) : ArrayList<T>(initalCapacity) {

	var observer: Observer? = null

	override fun add(element: T): Boolean {
		return super.add(element).also { if (it) observer?.onChanged() }
	}

	override fun add(index: Int, element: T) {
		super.add(index, element).also {  observer?.onChanged() }
	}

	override fun removeAt(index: Int): T {
		return super.removeAt(index).also { observer?.onChanged() }
	}

	override fun clear() {
		super.clear()
		observer?.onChanged()
	}

}

/**
 * Created by chriscaron on 12/7/17.
 *
 * This class contains the bizness logic for a game to teach programming.
 * The idea is to program a robot to collect coins.
 *
 * The player sets up commands to move the robot like: advance, turn right/left ect.
 *
 * The robot must advance to a coin or the level fails
 *
 */
open class Probot(private val program: ObservableArrayList<Command> = ObservableArrayList()) : Reflector<Probot>(), Comparator<Int>, Observer, MutableList<Command> by program {

	init {
		program.observer = this
	}

	companion object {
		val manColors = arrayOf(
			GColor.YELLOW,
			GColor.GREEN,
			GColor.ORANGE,
			GColor.PINK
		)
		const val LAZER_EAST = 1 shl 0
		const val LAZER_SOUTH = 1 shl 1
		const val LAZER_WEST = 1 shl 2
		const val LAZER_NORTH = 1 shl 3

		init {
			addAllFields(Probot::class.java)
		}
	}

	@JvmField
    var level = Level()

	@JvmField
	@Omit
	val lock = Lock()

	/**
	 *
	 * @return
	 */
	var levelNum = 0

	// the lazer matrix is same size as the coins. Each elem is a bit flag of LAZER_N/S/E/W values
    @JvmField
    var lazer = arrayOf<IntArray>()
	@JvmField
    val guys: MutableList<Guy> = ArrayList()

	@Omit
	private var copy: Probot? = null

	/**
	 *
	 * @return
	 */
	@Omit
	var isRunning = false
		private set

	/**
	 * Called in separate thread. callbacks made to events should be handled to show on ui
	 */
	fun runProgram() {
		assert(!isRunning)
		isRunning = true
		copy = Probot()
		copy!!.copyFrom(this)
		val run = runProgram(intArrayOf(0))
		when (run) {
			1 -> onSuccess()
			0 -> {
				onFailed()
				reset()
			}
			-1 -> reset()
		}
		isRunning = false
	}

	/**
	 *
	 * @param types
	 * @return
	 */
	fun getCommandCount(vararg types: CommandType): Int {
		return program.count { cmd ->
			types.firstOrNull { it==cmd.type } != null
		}
	}

	/**
	 *
	 */
	open fun stop() {
		isRunning = false
		lock.releaseAll()
	}

	override fun onChanged() {
		var nesting = 0
		for (c in program) {
			c.nesting = nesting
			when (c.type) {
				CommandType.LoopStart -> nesting++
				CommandType.LoopEnd -> nesting--
			}
		}
	}

	/**
	 *
	 * @return
	 */
	fun getMaxGuys(): Int {
		return manColors.size
	}

	/**
	 *
	 * @return
	 */
	fun getNumGuys(): Int {
		return guys.size
	}

	/**
	 *
	 * @return
	 */
	fun getGuys(): Iterable<Guy> {
		return guys
	}

	private fun runProgram(linePtr: IntArray): Int {
		while (linePtr[0] < program.size) {
			if (!isRunning) return -1
			val c = program[linePtr[0]]
			onCommand(linePtr[0])
			for (guy in guys) {
				when (c.type) {
					CommandType.Advance -> advance(guy, 1, true)
					CommandType.TurnRight -> onTurned(guy, 1)
					CommandType.TurnLeft -> onTurned(guy, -1)
					CommandType.UTurn -> onTurned(guy, 2)
					CommandType.LoopStart -> {
					}
					CommandType.LoopEnd -> {
					}
					CommandType.Jump -> advance(guy, 2, true)
					CommandType.IfThen -> {
					}
					CommandType.IfElse -> {
					}
					CommandType.IfEnd -> {
					}
				}
			}
			onCommand(linePtr[0])
			for (guy in guys) {
				when (c.type) {
					CommandType.LoopStart -> {
						val lineStart = ++linePtr[0]
						var i = 0
						while (i < c.count) {
							if (!isRunning) return -1
							linePtr[0] = lineStart
							var r: Int
							if (runProgram(linePtr).also { r = it } != 1) return r
							i++
						}
					}
					CommandType.LoopEnd -> {
						return 1
					}
					CommandType.Advance -> if (!advance(guy, 1, false)) {
						return if (isRunning) 0 else -1
					}
					CommandType.TurnRight -> turn(guy, 1)
					CommandType.TurnLeft -> turn(guy, -1)
					CommandType.UTurn -> turn(guy, 2)
					CommandType.Jump -> if (!advance(guy, 2, false)) {
						return if (isRunning) 0 else -1
					}
				}
			}
			linePtr[0]++
		}
		var i = 0
		while (isRunning && i < level.coins.size) {
			var ii = 0
			while (isRunning && ii < level.coins[i].size) {
				if (level.coins[i][ii] == Type.DD) {
					onDotsLeftUneaten()
					return if (isRunning) 0 else -1
				}
				ii++
			}
			i++
		}
		return if (isRunning) 1 else -1
	}

	private fun init(level: Level) {
		this.level = level
		initLazers()
		program.clear()
		guys.clear()
	}

	fun start() {
		for (i in level.coins.indices) {
			for (ii in 0 until level.coins[i].size) {
				when (level.coins[i][ii]) {
					Type.EM -> {
					}
					Type.DD -> {
					}
					Type.SE, Type.SS, Type.SW, Type.SN -> {
						guys.add(Guy(ii, i, level.coins[i][ii].direction, manColors[guys.size]))
						level.coins[i][ii] = Type.EM
					}
					Type.LH0 -> {
					}
					Type.LV0 -> {
					}
					Type.LB0 -> {
					}
					Type.LH1 -> {
					}
					Type.LV1 -> {
					}
					Type.LB1 -> {
					}
					Type.LH2 -> {
					}
					Type.LV2 -> {
					}
					Type.LB2 -> {
					}
					Type.LB -> {
					}
				}
			}
		}
	}

	// return false if failed
	private fun advance(guy: Guy, amt: Int, useCB: Boolean): Boolean {
		val nx = guy.posx + guy.dir.dx * amt
		val ny = guy.posy + guy.dir.dy * amt
		if (!canMoveToPos(ny, nx)) {
			if (useCB) onAdvanceFailed(guy)
			return false
		} else if (lazer[ny][nx] != 0) {
			if (useCB) onLazered(guy, amt) // walking into a lazer
			return false
		} else {
			if (useCB) {
				if (amt == 1) {
					onAdvanced(guy)
				} else {
					onJumped(guy)
				}
			} else {
				guy.posx = nx
				guy.posy = ny
				when (level.coins[guy.posy][guy.posx]) {
					Type.DD -> level.coins[guy.posy][guy.posx] = Type.EM
					Type.LB0 -> toggleLazers(0)
					Type.LB1 -> toggleLazers(1)
					Type.LB2 -> toggleLazers(2)
					Type.LB -> toggleLazers(0, 1, 2)
				}
				if (lazer[ny][nx] != 0) {
					onLazered(guy, 0) // lazer activated on guy
					return false
				}
			}
		}
		return true
	}

	private fun testLazers(row: Int, col: Int) {
		when (level.coins[row][col]) {
			Type.DD -> {
			}
			Type.LB0 -> toggleLazers(0)
			Type.LB1 -> toggleLazers(1)
			Type.LB2 -> toggleLazers(2)
			Type.LB -> toggleLazers(0, 1, 2)
		}
	}

	private fun initHorzLazer(y: Int, x: Int) {
		for (i in x - 1 downTo 0) {
			if (lazer[y][i] != 0) {
				lazer[y][i] = lazer[y][i] or LAZER_EAST
				break // cannot lazer past another lazer
			}
			lazer[y][i] = lazer[y][i] or (LAZER_WEST or LAZER_EAST)
		}
		for (i in x + 1 until lazer[0].size) {
			if (lazer[y][i] != 0) {
				lazer[y][i] = lazer[y][i] or LAZER_WEST
				break // cannot lazer past another lazer
			}
			lazer[y][i] = lazer[y][i] or (LAZER_WEST or LAZER_EAST)
		}
	}

	private fun initVertLazer(y: Int, x: Int) {
		for (i in y - 1 downTo 0) {
			if (lazer[i][x] != 0) {
				lazer[i][x] = lazer[i][x] or LAZER_SOUTH
				break // cannot lazer past another lazer
			}
			lazer[i][x] = lazer[i][x] or (LAZER_NORTH or LAZER_SOUTH)
		}
		for (i in y + 1 until lazer.size) {
			if (lazer[i][x] != 0) {
				lazer[i][x] = lazer[i][x] or LAZER_NORTH
				break // cannot lazer past another lazer
			}
			lazer[i][x] = lazer[i][x] or (LAZER_NORTH or LAZER_SOUTH)
		}
	}

	/*
    INTERNAL USE ONLY
     */
	override fun compare(i1: Int, i2: Int): Int {
		val o1 = level.lazers[i1]
		val o2 = level.lazers[i2]
		if (o1 && !o2) return -1
		return if (!o1 && o2) 1 else 0
	}

	private fun initLazers() {
		Arrays.sort(lazerOrdering, this)
		lazer = Array(level.coins.size) { IntArray(level.coins[0].size) }
		for (laz in lazerOrdering) {
			if (!level.lazers[laz]) continue
			for (i in level.coins.indices) {
				for (ii in 0 until level.coins[0].size) {
					when (level.coins[i][ii]) {
						Type.LH0 -> if (laz == 0) {
							initHorzLazer(i, ii)
						}
						Type.LV0 -> if (laz == 0) {
							initVertLazer(i, ii)
						}
						Type.LH1 -> if (laz == 1) {
							initHorzLazer(i, ii)
						}
						Type.LV1 -> if (laz == 1) {
							initVertLazer(i, ii)
						}
						Type.LH2 -> if (laz == 2) {
							initHorzLazer(i, ii)
						}
						Type.LV2 -> if (laz == 2) {
							initVertLazer(i, ii)
						}
					}
				}
			}
		}
	}

	// ordering the lazer initialilzation makes possible for any lazer to block any other depending on whose state has changed
	var lazerOrdering = arrayOf(0, 1, 2)
	private fun toggleLazers(vararg nums: Int) {
		for (n in nums) {
			level.lazers[n] = !level.lazers[n]
		}
		initLazers()
	}

	fun setLazerEnabled(num: Int, on: Boolean) {
		println("lazerOrdering num = " + num + " ordering: " + Arrays.toString(lazerOrdering))
		level.lazers[num] = on
		initLazers()
		println("lazerOrdering = " + Arrays.toString(lazerOrdering))
	}

	private fun canMoveToPos(y: Int, x: Int): Boolean {
		if (x < 0 || y < 0 || y >= level.coins.size || x >= level.coins[y].size) return false
		when (level.coins[y][x]) {
			Type.DD, Type.LB0, Type.LB1, Type.LB2, Type.LB -> return true
		}
		return false
	}

	private fun turn(guy: Guy, d: Int) {
		var nd = guy.dir.ordinal + d
		nd += Direction.values().size
		nd %= Direction.values().size
		guy.dir = Direction.values()[nd]
	}

	fun reset() {
		stop()
		if (copy != null) {
			copyFrom(copy)
		}
	}

	open fun setLevel(num: Int, level: Level) {
		levelNum = num
		program.clear()
		Arrays.sort(lazerOrdering)
		init(level.deepCopy())
	}

	/**
	 * Return -1 for infinte available.
	 * Otherwise a number >= 0 of num available.
	 *
	 * @param t
	 * @return
	 */
	fun getCommandTypeNumAvaialable(t: CommandType): Int {
		when (t) {
			CommandType.Jump -> return if (level.numJumps < 0) -1 else level.numJumps - getCommandCount(t)
			CommandType.LoopStart -> return if (level.numLoops < 0) -1 else level.numLoops - getCommandCount(CommandType.LoopStart)
			CommandType.TurnLeft, CommandType.TurnRight, CommandType.UTurn -> return if (level.numTurns < 0) -1 else level.numTurns - getCommandCount(CommandType.TurnLeft, CommandType.TurnRight, CommandType.UTurn)
		}
		return -1
	}

	/**
	 *
	 * @param t
	 * @return
	 */
	fun getCommandTypeMaxAvailable(t: CommandType): Int {
		when (t) {
			CommandType.Jump -> return if (level.numJumps < 0) -1 else level.numJumps
			CommandType.LoopStart -> return if (level.numLoops < 0) -1 else level.numLoops
			CommandType.TurnLeft, CommandType.TurnRight, CommandType.UTurn -> return if (level.numTurns < 0) -1 else level.numTurns
		}
		return -1
	}

	fun isCommandTypeVisible(t: CommandType): Boolean {
		return when (t) {
			CommandType.Jump -> level.numJumps != 0
			CommandType.LoopStart, CommandType.LoopEnd -> level.numLoops != 0
			CommandType.TurnRight, CommandType.TurnLeft, CommandType.UTurn -> level.numTurns != 0
			CommandType.Advance -> true
			else -> true
		}
	}

	protected open fun getStrokeWidth(): Float {
		return 10f
	}

	// begin rendering
	fun draw(g: AGraphics, width: Int, height: Int) {
		val lineWidth = getStrokeWidth()
		g.clearScreen(GColor.BLACK)
		g.color = GColor.RED
		g.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lineWidth)
		val l = level
		if (l.coins.isEmpty() || l.coins[0].isEmpty()) return
		val cols: Int = l.coins[0].size
		val rows = l.coins.size

		// get cell width/height
		val cw = width / cols
		val ch = height / rows
		val radius = Math.round(0.2f * Math.min(cw, ch)).toFloat()
		var curColor = 0
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val x = ii * cw + cw / 2
				val y = i * ch + ch / 2
				val t = l.coins[i][ii]
				when (t) {
					Type.EM -> {
					}
					Type.DD -> {
						g.color = GColor.WHITE
						g.drawFilledCircle(x.toFloat(), y.toFloat(), radius)
					}
					Type.SE, Type.SS, Type.SW, Type.SN -> if (curColor < getMaxGuys()) drawGuy(g, Guy(x, y, Direction.values()[t.ordinal - 2], manColors[curColor++]), radius) else l.coins[i][ii] = Type.EM
					Type.LH0 -> drawLazer(g, x, y, radius, true, GColor.RED)
					Type.LV0 -> drawLazer(g, x, y, radius, false, GColor.RED)
					Type.LB0 -> drawButton(g, x, y, radius, GColor.RED, level.lazers[0])
					Type.LH1 -> drawLazer(g, x, y, radius, true, GColor.BLUE)
					Type.LV1 -> drawLazer(g, x, y, radius, false, GColor.BLUE)
					Type.LB1 -> drawButton(g, x, y, radius, GColor.BLUE, level.lazers[1])
					Type.LH2 -> drawLazer(g, x, y, radius, true, GColor.GREEN)
					Type.LV2 -> drawLazer(g, x, y, radius, false, GColor.GREEN)
					Type.LB2 -> drawButton(g, x, y, radius, GColor.GREEN, level.lazers[2])
					Type.LB -> {
						// toggle all button
						g.color = GColor.RED
						g.drawFilledCircle(x.toFloat(), y.toFloat(), radius * 3 / 2)
						g.color = GColor.GREEN
						g.drawCircle(x.toFloat(), y.toFloat(), radius)
						g.color = GColor.BLUE
						g.drawCircle(x.toFloat(), y.toFloat(), radius * 2 / 3)
					}
				}
			}
		}

		// draw guys
		for (guy in guys) {
			drawGuy(g, guy, radius)
		}

		// draw lazers
		g.color = GColor.RED
		for (i in 0 until rows) {
			for (ii in 0 until cols) {
				val cx = ii * cw + cw / 2
				val cy = i * ch + ch / 2
				val left = ii * cw
				val right = left + cw
				val top = i * ch
				val bottom = top + ch
				if (0 != lazer[i][ii] and LAZER_WEST) {
					g.drawLine(left.toFloat(), cy.toFloat(), cx.toFloat(), cy.toFloat(), lineWidth)
				}
				if (0 != lazer[i][ii] and LAZER_EAST) {
					g.drawLine(cx.toFloat(), cy.toFloat(), right.toFloat(), cy.toFloat(), lineWidth)
				}
				if (0 != lazer[i][ii] and LAZER_NORTH) {
					g.drawLine(cx.toFloat(), top.toFloat(), cx.toFloat(), cy.toFloat(), lineWidth)
				}
				if (0 != lazer[i][ii] and LAZER_SOUTH) {
					g.drawLine(cx.toFloat(), cy.toFloat(), cx.toFloat(), bottom.toFloat(), lineWidth)
				}
			}
		}
	}

	fun drawLazer(g: AGraphics, cx: Int, cy: Int, rad: Float, horz: Boolean, color: GColor?) {
		g.pushMatrix()
		g.translate(cx.toFloat(), cy.toFloat())
		if (!horz) {
			g.rotate(90f)
		}
		g.color = GColor.GRAY
		val radius = rad * 3 / 2
		g.drawFilledCircle(0f, 0f, radius)
		g.color = color
		g.begin()
		g.vertex(-radius, 0f)
		g.vertex(0f, -radius / 2)
		g.vertex(radius, 0f)
		g.vertex(0f, radius / 2)
		g.drawTriangleFan()
		g.popMatrix()
	}

	fun drawButton(g: AGraphics, cx: Int, cy: Int, radius: Float, color: GColor, on: Boolean) {
		g.color = GColor.GRAY
		g.drawFilledCircle(cx.toFloat(), cy.toFloat(), radius)
		g.color = color
		//g.setStyle(on ? Paint.Style.FILL : Paint.Style.STROKE);
		//c.drawCircle(cx, cy, radius/2, p);
		if (on) {
			g.drawFilledCircle(cx.toFloat(), cy.toFloat(), radius / 2)
		} else {
			g.drawCircle(cx.toFloat(), cy.toFloat(), radius / 2)
		}
	}

	fun drawGuy(g: AGraphics, guy: Guy, radius: Float) {
		g.color = guy.color
		val x = guy.posx
		val y = guy.posy
		g.drawFilledCircle(x.toFloat(), y.toFloat(), radius)
		g.color = GColor.BLACK
		when (guy.dir) {
			Direction.Right -> g.drawLine(x.toFloat(), y.toFloat(), x + radius, y.toFloat(), getStrokeWidth())
			Direction.Down -> g.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), y + radius, getStrokeWidth())
			Direction.Left -> g.drawLine(x.toFloat(), y.toFloat(), x - radius, y.toFloat(), getStrokeWidth())
			Direction.Up -> g.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), y - radius, getStrokeWidth())
		}
	}

	// Overrides to handle important events.
	open fun onCommand(line: Int) {}

	/**
	 * Called at the end of program if some FAILED action occured.
	 */
	open fun onFailed() {}

	/**
	 * Guy a walked off board or onto an unavailable square
	 * @param guy
	 */
	open fun onAdvanceFailed(guy: Guy) {}

	/**
	 * A guy has advanced
	 * @param guy
	 */
	open fun onAdvanced(guy: Guy) {}

	/**
	 * A guy has jumped
	 * @param guy
	 */
	open fun onJumped(guy: Guy) {}

	/**
	 * A Guy has execute a right, left or U turn
	 * @param guy
	 * @param dir
	 */
	open fun onTurned(guy: Guy, dir: Int) {}

	/**
	 * Called at end of program if no FAILED events occured.
	 */
	open fun onSuccess() {}

	/**
	 * Guy has walked into a lazer or a lazer activated on them. FAILED.
	 * @param guy
	 * @param type 0 == instantaneous, 1 == walked into, 2 == jumped into
	 */
	open fun onLazered(guy: Guy, type: Int) {}

	/**
	 * The program has finished but not all dots eaten. FAILED.
	 */
	open fun onDotsLeftUneaten() {}
}