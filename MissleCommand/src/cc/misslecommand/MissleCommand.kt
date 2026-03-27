package cc.misslecommand

import cc.lib.game.AGraphics
import cc.lib.game.GColor
import cc.lib.game.ImageColorFilter
import cc.lib.game.Justify
import cc.lib.game.Utils

abstract class MissleCommand {
	// --------------------------------------------------------------
	// INHERITED METHODS
	// --------------------------------------------------------------
	abstract val screenWidth: Int

	abstract fun getScreenHeight(): Int
	abstract fun getPointerX(): Int
	abstract fun getPointerY(): Int
	abstract fun getFrameNumber(): Int
	abstract fun checkPlayerInput()
	abstract fun setFrameNumber(frameNum: Int)
	fun doInitialization() {
		Utils.initTable(enemyMissles, Missle::class.java)
		Utils.initTable(playerMissles, Missle::class.java)
		Utils.initTable(enemyExplosions, Explosion::class.java)
		Utils.initTable(playerExplosions, Explosion::class.java)
		Utils.initTable(cities, City::class.java)
	}

	fun initGraphics(g: AGraphics) {
		initColors(g)
		initImages(g)
		initGameStateGetReady(screenWidth, getScreenHeight())
	}

	fun drawFrame(g: AGraphics) {
		if (!::colors.isInitialized) {
			initGraphics(g)
		}
		val width = screenWidth
		val height = getScreenHeight()
		g.color = getSkyColor()
		g.drawFilledRect(0, 0, width, height)
		when (gameState) {
			GAME_STATE_GET_READY -> {
				drawLand(g)
				drawCities(g)
				if (getFrameNumber() > 30) initGameStatePlay()
				g.color = getTextColor()
				g.drawJustifiedString(width / 2, height / 2, Justify.CENTER, Justify.CENTER,
					"GET READY!\nLevel $currentLevel")
			}

			GAME_STATE_PLAY -> {
				checkPlayerInput()
				drawMissles(g)
				drawExplosions(g)
				drawLand(g)
				drawCities(g)
			}

			GAME_STATE_LEVEL_OVER -> {
				drawLand(g)
				drawSummary(g)
				if (getFrameNumber() > 100) initNextLevel()
			}

			GAME_STATE_GAME_OVER -> {}
			else -> {}
		}
		if (Utils.isDebugEnabled()) {
			g.color = getEnemyMissleColor()
			fillCircle(g, getPointerX().toFloat(), getLandHeight(width.toFloat(), getPointerX().toFloat()).toFloat(), 3f)
		}
	}

	fun onDimensionsChanged(g: AGraphics?, width: Int, height: Int) {
		initCities(width, false)
	}

	// --------------------------------------------------------------
	// METHODS
	// --------------------------------------------------------------
	fun getMissleCity(num: Int): City? {
		when (num) {
			0 -> return cities[0]
			1 -> return cities[2]
			2 -> return cities[5]
			else -> Utils.unhandledCase(num)
		}
		return null
	}

	// init
	fun initGameStateGetReady(width: Int, height: Int) {
		gameState = GAME_STATE_GET_READY
		setFrameNumber(0)
		initColors()
		initLand(height)
		initCities(width, true)
	}

	fun initImages(g: AGraphics) {
		val cityId = g.loadImage("city.gif", GColor.BLACK)
		//Image src = images.getSourceImage(cityId);
		cityImageIds[0] = g.newSubImage(cityId, 0, 0, 64, 64)
		cityImageIds[1] = g.newSubImage(cityId, 64, 0, 64, 64)
		cityImageIds[2] = g.newSubImage(cityId, 0, 64, 64, 64)
		cityImageIds[3] = g.newSubImage(cityId, 64, 64, 64, 64)
		for (i in 0..3) {
			//cityImageIds[i] = g.getImage(cityImageIds[i], getCityRadius(), getCityRadius());
			cityImageIds[i] = g.newTransformedImage(cityImageIds[i], ImageColorFilter(GColor.WHITE, getCityColor(), 0))
		}
	}

	fun initGameStatePlay() {
		Utils.println("initGameStatePlay")
		gameState = GAME_STATE_PLAY
	}

	fun initColors() {
		Utils.shuffle(colors)
	}

	fun initLand(screenHeight: Int) {
		for (i in landYFactor.indices) {
			landYFactor[i] = Utils.randFloat(1f)
			Utils.print("[%f]", landYFactor[i])
		}
		Utils.println()
	}

	fun getStartNumMissles(): Int {
		return 10
	}

	fun initCities(screenWidth: Int, resetMissles: Boolean) {
		if (cities[0] == null) return
		val dx = screenWidth / cities.size
		var x = dx / 2
		for (i in cities.indices) {
			cities[i]!!.x = x + Utils.randRange(-dx / 4, dx / 4)
			cities[i]!!.y = getLandHeight(screenWidth.toFloat(), cities[i]!!.x.toFloat())
			if (resetMissles) cities[i]!!.numMissles = 0
			x += dx
		}
		if (resetMissles) {
			for (i in 0..2) {
				getMissleCity(i)!!.numMissles = getStartNumMissles()
			}
		}
	}

	fun startPlayerMissle(mc: City?) {
		assert(mc != null && mc.numMissles > 0)
		mc!!.numMissles--
		addMissle(playerMissles, mc.x, mc.y, getPointerX(), getPointerY(), getPlayerMissleSpeed())
	}

	fun startMissleWave() {
		val numStartMissles = Utils.randRange(5 + currentLevel, 10 + currentLevel * 2)
		for (i in 0 until numStartMissles) {
			val sx = Utils.randRange(10, screenWidth - 10)
			val sy = 0
			val ex = Utils.randRange(10, screenWidth - 10)
			val ey = getLandHeight(screenWidth.toFloat(), ex.toFloat())
			addMissle(enemyMissles, sx, sy, ex, ey, getRandomEnemyMissleSpeed())
		}
	}

	fun initNextLevel() {
		initLand(getScreenHeight())
		initCities(screenWidth, true)
	}

	fun getLandY(index: Int): Float {
		val height = getScreenHeight().toFloat()
		val minLandY = height - height * 0.1f
		val maxLandY = height - height * 0.25f
		return minLandY + (maxLandY - minLandY) * landYFactor[index]
	}

	fun getLandHeight(width: Float, x: Float): Int {


		// return
		var x = x
		val dx = width / (landYFactor.size - 1)
		var i = 1
		while (i < landYFactor.size - 1) {
			if (x <= dx) break
			x -= dx
			i++
		}
		val first = i - 1
		val last = i
		val y0 = getLandY(first)
		val y1 = getLandY(last)
		val m = (y1 - y0) / dx // slope
		return Math.round(y0 + m * x)
	}

	fun getSkyColor(): GColor {
		return colors!![0]
	}

	fun getLandColor(): GColor {
		return colors!![1]
	}

	fun getEnemyMissleColor(): GColor {
		return colors!![2]
	}

	fun getPlayerMissleColor(): GColor {
		return colors!![3]
	}

	fun getExplosionColor(): GColor {
		return colors!![4]
	}

	fun getCityColor(): GColor {
		return colors!![5]
	}

	fun getMissleHeadColor(): GColor {
		return colors!![6]
	}

	fun getTextColor(): GColor {
		return colors!![7]
	}

	fun drawMissle(g: AGraphics, m: Missle?, color: GColor?, missleSpeed: Int): Boolean {
		var dx = (m!!.ex - m.sx).toFloat()
		var dy = (m.ey - m.sy).toFloat()
		val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
		if (dist < 1) return false
		val distInv = 1.0 / dist
		dx *= distInv.toFloat()
		dy *= distInv.toFloat()
		val frames = getFrameNumber() - m.startFrame
		val len = frames * missleSpeed
		g.color = color!!
		m.nx = m.sx + Math.round(dx * len)
		m.ny = m.sy + Math.round(dy * len)
		g.drawLine(m.sx, m.sy, m.nx, m.ny, 2)
		g.color = getMissleHeadColor()
		fillCircle(g, m.nx.toFloat(), m.ny.toFloat(), 2f)
		return if (len >= dist) {
			true
		} else false
	}

	fun startPlayerExplosion(x: Int, y: Int) {
		for (i in playerExplosions.indices) {
			if (playerExplosions[i]!!.startFrame < 0) {
				playerExplosions[i]!!.startFrame = getFrameNumber()
				playerExplosions[i]!!.x = x
				playerExplosions[i]!!.y = y
				break
			}
		}
	}

	fun startEnemyExplosion(x: Int, y: Int) {
		for (i in enemyExplosions.indices) {
			if (enemyExplosions[i]!!.startFrame < 0) {
				enemyExplosions[i]!!.startFrame = getFrameNumber()
				enemyExplosions[i]!!.x = x
				enemyExplosions[i]!!.y = y
				break
			}
		}
	}

	fun getNumCitiesLeft(): Int {
		var num = 0
		for (i in cities.indices) if (cities[i]!!.numMissles >= 0) num++
		return num
	}

	//int getEnemyMissleSpeed() {
	//	return currentLevel < 10 ? currentLevel : 10;
	//}
	fun getRandomEnemyMissleSpeed(): Int {
		return Utils.randRange(currentLevel, currentLevel + 2)
	}

	fun getPlayerMissleSpeed(): Int {
		return 10
	}

	fun getPointsPerCity(): Int {
		return 500
	}

	fun getPointsPerMissle(): Int {
		return 10
	}

	fun drawSummary(g: AGraphics) {
		var numCities = 0
		var numMissles = 0
		for (i in cities.indices) {
			if (cities[i]!!.numMissles < 0) continue
			numCities++
			numMissles += cities[i]!!.numMissles
		}
		val cityPoints = numCities * getPointsPerCity()
		val misslePoints = numMissles * getPointsPerMissle()
		val totalPoints = cityPoints + misslePoints
		val summary = """Level $currentLevel Complete

Cities X $numCities Bonus: $cityPoints
Missles X $numMissles Bonus: $misslePoints

TOTAL $totalPoints"""
		g.color = getTextColor()
		g.drawJustifiedString(screenWidth / 2, getScreenHeight() / 2, Justify.CENTER, Justify.CENTER, summary)
	}

	fun drawPlayerMissles(g: AGraphics) {
		for (i in playerMissles.indices) {
			if (playerMissles[i]!!.startFrame < 0) continue
			if (drawMissle(g, playerMissles[i], getPlayerMissleColor(), getPlayerMissleSpeed())) {
				playerMissles[i]!!.startFrame = -1 // mark frame as not used
				startPlayerExplosion(playerMissles[i]!!.ex, playerMissles[i]!!.ey)
			}
		}
	}

	fun startMissleSpread(x: Int, y: Int) {
		val numMissles = Utils.randRange(2, 2 + currentLevel)
		for (i in 0 until numMissles) {
			val ex = Utils.rand() % screenWidth
			val ey = getLandHeight(screenWidth.toFloat(), ex.toFloat())
			addMissle(enemyMissles, x, y, ex, ey, getRandomEnemyMissleSpeed() + 2)
		}
	}

	fun drawEnemyMissles(g: AGraphics) {
		for (i in enemyMissles.indices) {
			val m = enemyMissles[i]
			if (m!!.startFrame < 0) continue
			if (drawMissle(g, m, getEnemyMissleColor(), m.speed)) {
				m.startFrame = -1 // mark frame as not used
				if (m.ny < getScreenHeight() * 3 / 4) {
					startEnemyExplosion(enemyMissles[i]!!.ex, enemyMissles[i]!!.ey)
				} else {
					// spawn missles from here
					startMissleSpread(m.nx, m.ny)
				}
			}
		}
	}

	fun drawMissles(g: AGraphics) {
		drawPlayerMissles(g)
		drawEnemyMissles(g)
	}

	fun fillCircle(g: AGraphics, x0: Float, y0: Float, radius: Float) {
		val x = Math.round(x0 - radius)
		val y = Math.round(y0 - radius)
		val wh = Math.round(radius * 2)
		g.drawFilledOval(x, y, wh, wh)
	}

	fun getExplosionNumFrames(): Float {
		return 60f
	}

	fun getExplosionMaxRadius(): Float {
		return 50f
	}

	// return true when missle is active
	fun drawExplosion(g: AGraphics, e: Explosion?): Boolean {
		val numExplosionFrames = getExplosionNumFrames()
		val maxExplosionRadius = getExplosionMaxRadius()
		val frames = getFrameNumber() - e!!.startFrame
		if (frames > numExplosionFrames) return false
		if (frames < numExplosionFrames * 0.5f) {
			e.innerRadius = 0f
			e.outerRadius = maxExplosionRadius *
				(getFrameNumber() - e.startFrame) / (numExplosionFrames / 2)
		} else {
			e.innerRadius = maxExplosionRadius *
				(getFrameNumber() - numExplosionFrames / 2
					- e.startFrame) / (numExplosionFrames * 0.5f)
			e.outerRadius = maxExplosionRadius
		}
		g.color = getExplosionColor()
		fillCircle(g, e.x.toFloat(), e.y.toFloat(), e.outerRadius)
		g.color = getSkyColor()
		fillCircle(g, e.x.toFloat(), e.y.toFloat(), e.innerRadius)
		return true
	}

	fun drawPlayerExplosions(g: AGraphics) {
		for (i in playerExplosions.indices) {
			val e = playerExplosions[i]
			if (e!!.startFrame < 0) continue
			if (!drawExplosion(g, e)) {
				e.startFrame = -1
			} else {
				collisionScanEnemyMissle(e)
			}
		}
	}

	// 
	fun collisionScanEnemyMissle(e: Explosion?) {
		for (i in enemyMissles.indices) {
			val m = enemyMissles[i]
			if (m!!.startFrame < 0) {
				continue
			}
			val hx = m.nx
			val hy = m.ny
			if (Utils.isPointInsideCircle(hx, hy, e!!.x, e.y, Math.round(e.outerRadius))) {
				m.startFrame = -1
				startPlayerExplosion(hx, hy)
			}
		}
	}

	fun drawEnemyExplosions(g: AGraphics) {
		for (i in enemyExplosions.indices) {
			val e = enemyExplosions[i]
			if (e!!.startFrame < 0) continue
			if (!drawExplosion(g, e)) {
				e.startFrame = -1
			} else {
				collisionScanCity(e)
			}
		}
	}

	fun collisionScanCity(e: Explosion?) {
		for (i in cities.indices) {
			val c = cities[i]
			if (c!!.numMissles < 0) continue
			if (Utils.isCirclesOverlapping(c.x.toFloat(), c.y.toFloat(), CITY_RADIUS.toFloat(), e!!.x.toFloat(), e.y.toFloat(), Math.round(e.outerRadius).toFloat())) {
				startPlayerExplosion(c.x, c.y)
				c.numMissles = -1
			}
		}
	}

	fun drawExplosions(g: AGraphics) {
		drawPlayerExplosions(g)
		drawEnemyExplosions(g)
	}

	fun drawLand(g: AGraphics) {
		val height = getScreenHeight() - 1
		g.color = getLandColor()
		val xStep = screenWidth / (landYFactor.size - 1)
		var x = 0
		for (i in 0 until landYFactor.size - 1) {
			g.begin()
			g.vertex(x, height)
			g.vertex(x, Math.round(getLandY(i)))
			g.vertex(x + xStep, Math.round(getLandY(i + 1)))
			g.vertex(x + xStep, height)
			g.drawTriangleFan()
			x += xStep
		}
	}

	fun getCityRadius(): Int {
		return 32
	}

	fun drawCities(g: AGraphics) {
		for (i in cities.indices) {
			if (cities[i]!!.numMissles < 0) continue
			val x = cities[i]!!.x - getCityRadius() / 2
			val y = cities[i]!!.y - getCityRadius() / 2
			val imageNum = i % cityImageIds.size
			if (cityImageIds[imageNum] > 0) g.drawImage(cityImageIds[imageNum], x, y, getCityRadius(), getCityRadius())
			if (cities[i]!!.numMissles > 0) {
				g.color = getTextColor()
				g.drawJustifiedString(cities[i]!!.x, getScreenHeight() - 20, Justify.CENTER, Justify.CENTER, cities[i]!!.numMissles.toString())
			}
		}
	}

	fun addMissle(array: Array<Missle?>, sx: Int, sy: Int, ex: Int, ey: Int, speed: Int) {
		for (i in array.indices) {
			if (array[i]!!.startFrame < 0) {
				array[i]!!.startFrame = getFrameNumber()
				array[i]!!.sx = sx
				array[i]!!.sy = sy
				array[i]!!.ex = ex
				array[i]!!.ey = ey
				array[i]!!.speed = speed
				return
			}
		}
		System.err.println("cant add any more missles too [$array]")
	}

	// --------------------------------------------------------------
	// CLASSES
	// --------------------------------------------------------------
	class Missle {
		var sx = 0
		var sy = 0 // start xy
		var ex = 0
		var ey = 0 // end xy
		var nx = 0
		var ny = 0 // next x,y (position of the head)
		var startFrame = -1 // frame this missle was spawned
		var speed = 0
	}

	class Explosion {
		var x = 0
		var y = 0
		var startFrame = -1
		var innerRadius = 0f
		var outerRadius = 0f
	}

	class City {
		var x = 0
		var y = 0
		var numMissles = 0
	}

	// --------------------------------------------------------------
	// CONSTANTS
	// --------------------------------------------------------------
	val GAME_STATE_GET_READY = 0
	val GAME_STATE_PLAY = 1
	val GAME_STATE_GAME_OVER = 2
	val GAME_STATE_LEVEL_OVER = 3
	val CITY_RADIUS = 10

	// --------------------------------------------------------------
	// TABLES
	// --------------------------------------------------------------
	val playerMissles = arrayOfNulls<Missle>(256)
	val enemyMissles = arrayOfNulls<Missle>(256)
	val playerExplosions = arrayOfNulls<Explosion>(64)
	val enemyExplosions = arrayOfNulls<Explosion>(64)
	val cities = arrayOfNulls<City>(6)
	val landYFactor = FloatArray(cities.size + 1)
	val cityImageIds = IntArray(4)
	var nextWaveFrame = 0
	var numWavesLeft = 0
	lateinit var colors: Array<GColor>
	fun initColors(g: AGraphics?) {
		colors = arrayOf(
			GColor.RED,
			GColor.BLACK,
			GColor.BLUE,
			GColor.GRAY,
			GColor.GREEN,
			GColor.DARK_GRAY,
			GColor.CYAN,
			GColor.MAGENTA,
			GColor.YELLOW,
			GColor.ORANGE,
			GColor.WHITE
		)
	}

	// --------------------------------------------------------------
	// GLOBALS
	// --------------------------------------------------------------
	var gameState = GAME_STATE_GET_READY
	var currentLevel = 1
}
