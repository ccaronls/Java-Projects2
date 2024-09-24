package cc.applets.probot

import cc.lib.game.AGraphics
import cc.lib.game.Utils
import cc.lib.probot.Command
import cc.lib.probot.CommandType
import cc.lib.probot.Level
import cc.lib.probot.UIProbot
import cc.lib.reflector.Reflector
import cc.lib.swing.AWTButton
import cc.lib.swing.AWTComponent
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTGraphics
import cc.lib.swing.AWTLabel
import cc.lib.swing.AWTPanel
import cc.lib.utils.FileUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileNotFoundException
import java.util.Arrays
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.KeyStroke
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.HTMLEditorKit.InsertHTMLTextAction

class AWTProbot internal constructor() : AWTComponent() {
	val txtEditor = JEditorPane()

	lateinit var frame: AWTFrame
	lateinit var run: JButton
	lateinit var pause: JButton
	lateinit var stop: JButton
	lateinit var clear: JButton
	lateinit var quit: JButton
	lateinit var next: JButton
	lateinit var previous: JButton
	lateinit var help: JButton
	val currentLevelNum: JLabel = AWTLabel("-", 1, 14f, true)
	val currentLevelTitle: JLabel = AWTLabel("-", 1, 16f, true)
	val levels: MutableList<Level> = ArrayList()
	lateinit var faces: IntArray

	val probot: UIProbot = object : UIProbot() {

		override fun repaint() {
			this@AWTProbot.repaint()
		}

		override fun setProgramLine(line: Int) {
			val html = templateHTML
			val htmlFirst = html.substring(0, html.indexOf("<ol>") + 4)
			val htmlLast = html.substring(html.indexOf("</ol>"))
			var htmlBody = ""
			val program: Array<String> = getProgram(true)
			var index = 0
			for (cmd in program) {
				htmlBody += if (index++ == line) {
					"<li class=\"active\">$cmd</li>\n"
				} else {
					"<li class=\"good\">$cmd</li>\n"
				}
			}
			txtEditor.text = htmlFirst + htmlBody + htmlLast
		}

		override fun onSuccess() {
			super.onSuccess()
			Thread.sleep(3000)
			val curLevel = frame.getIntProperty("curLevel", 0)
			initLevel(curLevel + 1)
		}

		override fun onFailed() {
			Thread.sleep( 3000)
			super.onFailed()
			run.isEnabled = true
			pause.isEnabled = false
		}

		override fun getFaceImageIds(g: AGraphics): IntArray {
			return faces
		}
	}

	override fun init(g: AWTGraphics) {
		super.init(g)
		faces = intArrayOf(
			g.loadImage("guy_smile1.png"),
			g.loadImage("guy_smile2.png"),
			g.loadImage("guy_smile3.png")
		)
	}

	override fun paint(g: AWTGraphics) {
		probot.paint(g, mouseX, mouseY)
	}

	val templateHTML: String
		get() = try {
			FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_template.html")).replace(">[\n\t ]+<".toRegex(), "><")
		} catch (e: Exception) {
			throw RuntimeException(e)
		}

	fun getProgram(includeEmptyLines: Boolean): Array<String> {
		val txt = txtEditor.text.replace("[\n ]+".toRegex(), " ")
		val ol = Pattern.compile("<ol.*</ol>")
		val li = Pattern.compile("<li[^<]*</li>")
		val olm = ol.matcher(txt)
		if (!olm.find()) {
			throw RuntimeException("WTF!")
		}
		val m = li.matcher(olm.group())
		val lines: MutableList<String> = ArrayList()
		while (m.find()) {
			val group = m.group()
			val cmdStr = group.replace("<[^>]+>".toRegex(), "").trim { it <= ' ' }.toLowerCase()
			if (!includeEmptyLines && cmdStr.length == 0) continue
			lines.add(cmdStr)
		}
		return lines.toTypedArray()
	}

	fun compileProgram(): Boolean {
		val program = getProgram(false)
		var success = true
		val html = templateHTML
		val htmlFirst = html.substring(0, html.indexOf("<ol>") + 4)
		val htmlLast = html.substring(html.indexOf("</body>"))
		var htmlList = ""
		probot.clear()
		val errors: MutableList<String> = ArrayList()
		try {
			var nesting = 0
			var lineNum = 0
			var maxNum = -1
			var cmdCnt = 0
			for (cmdStr in program) {
				lineNum++
				val cmd = cmdStr.split("[ ]+".toRegex()).toTypedArray()
				var color = "good"
				when (cmd[0]) {
					"c", "chomp" -> {
						probot.add(Command(CommandType.Advance, 1))
						maxNum = probot.getCommandTypeMaxAvailable(CommandType.Advance)
						cmdCnt = probot.getCommandCount(CommandType.Advance)
					}
					"r", "right" -> {
						probot.add(Command(CommandType.TurnRight, 1))
						maxNum = probot.getCommandTypeMaxAvailable(CommandType.TurnRight)
						cmdCnt = probot.getCommandCount(CommandType.TurnRight)
					}
					"l", "left" -> {
						probot.add(Command(CommandType.TurnLeft, 1))
						maxNum = probot.getCommandTypeMaxAvailable(CommandType.TurnLeft)
						cmdCnt = probot.getCommandCount(CommandType.TurnLeft)
					}
					"u", "uturn" -> {
						probot.add(Command(CommandType.UTurn, 1))
						maxNum = probot.getCommandTypeMaxAvailable(CommandType.UTurn)
						cmdCnt = probot.getCommandCount(CommandType.UTurn)
					}
					"j", "jump" -> {
						probot.add(Command(CommandType.Jump, 2))
						maxNum = probot.getCommandTypeMaxAvailable(CommandType.Jump)
						cmdCnt = probot.getCommandCount(CommandType.Jump)
					}
					"loopend", "done", "end", "e" -> if (nesting-- > 0) {
						probot.add(Command(CommandType.LoopEnd, 1))
					} else {
						color = "bad"
						success = false
						errors.add("[$lineNum] Cannot end loop")
					}
					"b", "begin", "repeat", "loop" -> try {
						probot.add(Command(CommandType.LoopStart, cmd[1].toInt()))
						nesting++
					} catch (e: Exception) {
						errors.add("[" + lineNum + "] " + cmd[1] + " is not a number")
						color = "bad"
						success = false
					}
					else                           -> {
						color = "bad"
						success = false
						errors.add("[" + lineNum + "] " + cmd[0] + " is not a command")
					}
				}
				if (maxNum >= 0) {
					if (cmdCnt > maxNum) {
						color = "bad"
						success = false
						if (maxNum == 0) {
							errors.add("[" + lineNum + "] " + cmd[0] + " cannot be used")
						} else {
							errors.add("[" + lineNum + "] " + cmd[0] + " can only be used " + maxNum + " times")
						}
					} else {
						//cmdStr += " (" + cmdCnt + " of " + maxNum + ")";
					}
				}
				htmlList += "<li class=\"$color\">$cmdStr</li>"
			}
			if (nesting > 0) {
				success = false
				errors.add("[$lineNum] Loop not closed")
			}
		} finally {
			htmlList += "</ol>"
			if (errors.size > 0) {
				htmlList += "<ul>"
				for (err in errors) {
					htmlList += "<li class=\"err\">$err</li>"
				}
				htmlList += "</ul>"
			}
			txtEditor.text = htmlFirst + htmlList + htmlLast
		}
		return success
	}

	fun initLevel(levelNum: Int) {
		var levelNum = levelNum
		if (levelNum > levels.size - 1) levelNum = 0
		val level = levels[levelNum]
		var maxLevel = frame.getIntProperty("maxLevel", -1)
		if (maxLevel < levelNum) {
			maxLevel = levelNum
			frame.setProperty("maxLevel", maxLevel)
			if (!Utils.isEmpty(level.info)) {
				Thread {
					Thread.sleep(2000)
					frame.showMessageDialog(level.label, level.info)
				}.start()
			}
		}
		currentLevelTitle.text = level.label
		probot.setLevel(levelNum + 1, levels[levelNum])
		frame.setProperty("curLevel", levelNum)
		probot.clear()
		probot.start()
		txtEditor.text = templateHTML
		previous.isEnabled = levelNum > 0
		next.isEnabled = levelNum < maxLevel
		currentLevelNum.text = "  " + (levelNum + 1) + "  "
		run.isEnabled = true
		pause.isEnabled = false
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			Utils.setDebugEnabled()
			try {
				AWTProbot()
			} catch (t: Throwable) {
				t.printStackTrace()
			}
		}
	}

	init {
		val settingsDir = FileUtils.getOrCreateSettingsDirectory(javaClass)
		val propertiesFile = File(settingsDir, "awtprobot.properties")
		levels.addAll(Reflector.deserializeFromInputStream(FileUtils.openFileOrResource("awtprobot_levels.txt")))
		setMouseEnabled(false)
		txtEditor.isEditable = true
		txtEditor.border = BorderFactory.createLineBorder(Color(0, 0, 0, 0), 5)
		txtEditor.contentType = "text/html"
		val scrollPane = JScrollPane(txtEditor)
		val iMap = txtEditor.inputMap
		val aMap = txtEditor.actionMap
		iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter")
		aMap.put("enter", object : InsertHTMLTextAction("Bullets", "<li></li>", HTML.Tag.OL, HTML.Tag.LI) {
			override fun actionPerformed(ae: ActionEvent) {

				// *****************************************************************************************
				// Annoying Hack: for some reason the editor adds 2 list items, so remove the extra one made
				// *****************************************************************************************


				// get the position of the cursor
				val lines = getProgram(true)
				val cursor = txtEditor.caretPosition
				var lineNum = 0
				var c = cursor
				while (lineNum < lines.size) {
					c -= Math.max(1, lines[lineNum].length)
					if (c <= 0) break
					lineNum++
				}
				super.actionPerformed(ae)
				try {
					FileUtils.stringToFile(txtEditor.text, File("/tmp/x.html"))
				} catch (e: Exception) {
					e.printStackTrace()
				}
				var html = txtEditor.text
				println("Cursor is on lineNum: $lineNum")
				//cursor -= lines.length; // position includes newlines which we strip off
				println("cursor=$cursor")
				println("program=" + Arrays.toString(lines))
				// convert the cursor pos inrendered coords to a char in the html string.
				// For example, a cursor pos of 0 maps to Everything up to the end of the first <li>
				html = html.replace("[\n\t ]+".toRegex(), " ")
				val left = html.lastIndexOf("<li>") + 4
				val right = html.lastIndexOf("</li>") + 5
				println(html)
				println(Utils.getRepeatingChars(' ', left - 1) + "^" + Utils.getRepeatingChars(' ', right - left - 1) + "^")
				html = html.substring(0, left) + html.substring(right)
				//txtEditor.setText(html);
			}
		})
		val doc = txtEditor.document as HTMLDocument
		val kit = txtEditor.editorKit as HTMLEditorKit
		kit.defaultCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
		clear = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("clear_x.png"))) {
			override fun onAction() {
				txtEditor.text = templateHTML
				txtEditor.grabFocus()
			}
		}
		run = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("play_triangle.png"))) {
			override fun onAction() {
				if (probot.isRunning) {
					probot.isPaused = false
					isEnabled = false
					pause.isEnabled = true
				} else if (compileProgram()) {
					isEnabled = false
					pause.isEnabled = true
					stop.isEnabled = true
					probot.startProgramThread()
				}
			}
		}
		pause = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("pause_bars.png"))) {
			override fun onAction() {
				if (probot.isRunning) {
					probot.isPaused = true
					run.setEnabled(true)
					isEnabled = false
				}
			}
		}
		stop = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("stop_square.png"))) {
			override fun onAction() {
				probot.stop()
				run.setEnabled(true)
				pause.setEnabled(false)
				isEnabled = false
			}
		}
		quit = object : AWTButton("QUIT") {
			override fun onAction() {
				System.exit(0)
			}
		}
		next = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("forward_arrow.png"))) {
			override fun onAction() {
				if (!probot.isRunning) initLevel(frame.getIntProperty("curLevel", 0) + 1)
			}
		}
		previous = object : AWTButton(ImageIO.read(FileUtils.openFileOrResource("back_arrow.png"))) {
			override fun onAction() {
				if (!probot.isRunning) initLevel(frame.getIntProperty("curLevel", 0) - 1)
			}
		}
		help = object : AWTButton("HELP") {
			override fun onAction() {
				try {
					frame.showMessageDialogWithHTMLContent("Commands", FileUtils.inputStreamToString(FileUtils.openFileOrResource("pr_help.html")))
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		val topButtons = AWTPanel(previous, currentLevelNum, next, help)
		val bottomButtons = AWTPanel(run, pause, stop, clear)
		val left = AWTPanel(BorderLayout())
		left.add(scrollPane, BorderLayout.CENTER)
		left.add(topButtons, BorderLayout.NORTH)
		left.add(bottomButtons, BorderLayout.SOUTH)
		frame = object : AWTFrame("Probot") {
			override fun onWindowResized(w: Int, h: Int) {
				super.onWindowResized(w, h)
				//txtEditor.grabFocus();
				txtEditor.grabFocus()
			}

			override fun onWindowClosing() {
				try {
					FileUtils.stringToFile(txtEditor.text, File(settingsDir, "program.html"))
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		val cntr = AWTPanel(BorderLayout())
		cntr.addTop(currentLevelTitle)
		cntr.add(this, BorderLayout.CENTER)
		frame.add(cntr, BorderLayout.CENTER)
		frame.add(left, BorderLayout.WEST)
		//File lbLevelsFile = FileUtilsnew File(lbSettingsDir, "levels_backup.txt");
		frame.setPropertiesFile(propertiesFile)
		val curLevel = frame.getIntProperty("curLevel", 0)
		initLevel(curLevel)
		var html: String? = null
		try {
			html = FileUtils.fileToString(File(settingsDir, "program.html"))
		} catch (e: FileNotFoundException) {
			// dont care
		} catch (e: Exception) {
			e.printStackTrace()
		}
		if (html == null) html = templateHTML
		txtEditor.text = html
		frame.centerToScreen(640, 480)
		txtEditor.grabFocus()
	}
}