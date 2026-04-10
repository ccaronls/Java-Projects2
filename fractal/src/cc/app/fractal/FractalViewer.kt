package cc.app.fractal

import cc.app.fractal.AFractal.Custom
import cc.app.fractal.AFractal.Julia
import cc.app.fractal.AFractal.Mandelbrot
import cc.app.fractal.FractalComponent.FractalListener
import cc.app.fractal.evaluator.AEvaluator
import cc.app.fractal.evaluator.Evaluator
import cc.app.fractal.evaluator.TokenMgrError
import cc.lib.logger.LoggerFactory
import cc.lib.logger.LoggerFactory.LogLevel
import cc.lib.math.ComplexNumber
import cc.lib.swing.AWTFrame
import cc.lib.swing.AWTPanel
import cc.lib.utils.FileUtils
import java.awt.BorderLayout
import java.awt.Button
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Vector
import javax.swing.AbstractButton
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JRadioButton
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.filechooser.FileNameExtensionFilter

class FractalViewer internal constructor() : AWTFrame(), FractalListener, ActionListener {
	val DEFAULT_ZOOM = 2

	enum class Action(val label: String) {
		UNDO("Undo"),
		REDO("Redo"),
		ZOOMIN("Zoom in"),
		ZOOMOUT("Zoom out"),
		SAVE("Save"),
		CENTER("Center"),
		SET_SCALE(""),
		INVERT_COLORS("Invert"),
		BRIGHTEN_COLORS("Brighten"),
		DARKEN_COLORS("Darken"),
		ROTATE_COLORS("Rotate"),
		MANDELBROT_SET("Mandelbrot Set"),
		JULIA_SET("Julia Set"),
		ANIM_CONSTANT_START("Start"),
		ANIM_CONSTANT_END("End"),
		CUSTOM_SET_EXPRESSION("Zi="),
		CUSTOM_SET("Custom"),
		CUSTOM_SET_CONSTANT("C="),
		ANIMATE("Animate"),
		CANCEL("Cancel"),
		SHOW_WATERMARK("Water Mark"),
		MAKE_MOVIE("Make Movie"),
		POSITION("Position"),
		RENDER("Render")

	}

	var leftButtons = JPanel()
	var rightButtons = JPanel()
	var fractalComponent: FractalComponent
	var progressBar = JProgressBar(0, 100)
	var formulaExpression: JComboBox<*>
	var animationNumFrames: JComboBox<*>
	var constantExpression: JTextField
	var undoButton: AbstractButton
	var redoButton: AbstractButton
	var saveButton: AbstractButton? = null
	var animateButton: AbstractButton
	var zoomInButton: AbstractButton
	var zoomOutButton: AbstractButton
	var cancelButton: AbstractButton
	var showWatermarkButton: AbstractButton
	var animStartField: JTextField
	var animEndField: JTextField
	var animation: AnimateThread
	var zoomLeft: JTextField
	var zoomRight: JTextField
	var zoomTop: JTextField
	var zoomBottom: JTextField

	//    final HashMap<String, ComplexNumber> vars = new HashMap<String, ComplexNumber>();
	val evaluator: AEvaluator = Evaluator()
	var formulas = Vector<String>()
	val SETTINGS by lazy {
		FileUtils.getOrCreateSettingsDirectory(javaClass).also {
			log.info("Settings dir: $it")
		}
	}
	val FORMULAS_FILE by lazy {
		File(SETTINGS, "formulas.txt").also {
			log.info("Formulas file: $it")
		}
	}
	val ANIMS_DIR by lazy {
		File(SETTINGS, "/anims").also {
			if (!it.isDirectory) {
				if (!it.mkdir())
					throw RuntimeException("Failed to create ANIMS_DIR: $it")
			}
		}.also {
			log.info("Anims dir: $it")
		}
	}
	val MOVIES_DIR by lazy {
		File(System.getProperty("user.home") + "/Documents/Movies").also {
			if (!it.isDirectory) {
				if (!it.mkdir()) {
					throw RuntimeException("Failed to create MOVIES_DIR: $it")
				}
			}
		}.also {
			log.info("Movies Dir: $it")
		}
	}
	val FFMPEG: String by lazy {
		getStringProperty("ffmpeg", "/usr/local/bin/ffmpeg")
	}

	init {
		/*
        vars.put("E", new ComplexNumber(Math.E, 0));
        vars.put("PI", new ComplexNumber(Math.PI, 0));
        vars.put("A", new ComplexNumber());
        vars.put("B", new ComplexNumber());
        vars.put("C", new ComplexNumber());
        */
		//val settings = FileUtils.getOrCreateSettingsDirectory(javaClass)
		setPropertiesFile(File(SETTINGS, "fractal.properties"))
		LoggerFactory.logLevel = getEnumProperty("log_level", LogLevel.INFO)
		//FORMULAS_FILE = File(settings, "formulas.txt")
		listScreens()
		addWindowListener(this)
		loadFormulas()
		leftButtons.layout = GridLayout(0, 2)
		rightButtons.layout = GridLayout(15, 1)
		add(leftButtons, BorderLayout.WEST)
		add(rightButtons, BorderLayout.EAST)
		undoButton = addButton(Action.UNDO, leftButtons, false)
		redoButton = addButton(Action.REDO, leftButtons, false)
		zoomInButton = addButton(Action.ZOOMIN, leftButtons)
		zoomOutButton = addButton(Action.ZOOMOUT, leftButtons)
		addButton(Action.SAVE, leftButtons)
		addButton(Action.CENTER, leftButtons)
		var mv1: JPanel = AWTPanel(0, 1)
		zoomLeft = addTextField(Action.POSITION, "LEFT", mv1, getStringProperty(PROP_ZOOM_LEFT, "-2"))
		zoomRight = addTextField(Action.POSITION, "RIGHT", mv1, getStringProperty(PROP_ZOOM_RIGHT, "2"))
		leftButtons.add(mv1)
		mv1 = AWTPanel(0, 1)
		zoomTop = addTextField(Action.POSITION, "TOP", mv1, getStringProperty(PROP_ZOOM_TOP, "-2"))
		zoomBottom = addTextField(Action.POSITION, "BOTTOM", mv1, getStringProperty(PROP_ZOOM_BOTTOM, "2"))
		leftButtons.add(mv1)
		var group = ButtonGroup()
		val fractalSetString = getStringProperty(PROP_FRACTAL_SET, Action.MANDELBROT_SET.name)
		var fractalSet = Action.MANDELBROT_SET
		try {
			fractalSet = Action.valueOf(fractalSetString)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		addRadioButton(Action.MANDELBROT_SET, leftButtons, group, fractalSet == Action.MANDELBROT_SET)
		leftButtons.add(JLabel("Z(i+1) = Zi^2 + Z0"))
		addRadioButton(Action.JULIA_SET, leftButtons, group, fractalSet == Action.JULIA_SET)
		leftButtons.add(JLabel("Z(i+1) = Zi^2 - C"))
		addRadioButton(Action.CUSTOM_SET, leftButtons, group, fractalSet == Action.CUSTOM_SET)
		leftButtons.add(JPanel()) // empty space
		val combo = JComboBox(formulas)
		combo.isEditable = true
		combo.addActionListener(this)
		combo.maximumSize = Dimension(100, combo.maximumSize.height)
		combo.actionCommand = Action.CUSTOM_SET_EXPRESSION.name
		formulaExpression = combo
		formulaExpression.isEnabled = false
		var panel = JPanel()
		panel.add(JLabel(Action.CUSTOM_SET_EXPRESSION.label))
		panel.add(formulaExpression)
		leftButtons.add(panel)

		//addRadioButton(Action.CUSTOM_SET_JULIA_MODE, leftButtons, group, fractalSet == Action.CUSTOM_SET_JULIA_MODE);
		constantExpression =
			addTextField(Action.CUSTOM_SET_CONSTANT, leftButtons, getStringProperty(PROP_CONSTANT_EXPRESSION, "[0.5,0.5]"))
		constantExpression.isEnabled = false
		animStartField =
			addTextField(Action.ANIM_CONSTANT_START, leftButtons, getStringProperty(PROP_ANIM_START_FIELD, "[0,0]"))
		animStartField.isEnabled = false
		animEndField = addTextField(Action.ANIM_CONSTANT_END, leftButtons, getStringProperty(PROP_ANIM_END_FIELD, "[1,1]"))
		animEndField.isEnabled = false
		animateButton = addButton(Action.ANIMATE, leftButtons, false)
		cancelButton = addButton(Action.CANCEL, leftButtons)
		animationNumFrames = JComboBox(frameOptions)
		animationNumFrames.selectedItem = getStringProperty(PROP_ANIM_NUM_FRAMES, frameOptions[0])
		panel = JPanel()
		panel.add(JLabel("Animation num frames"))
		panel.add(animationNumFrames)
		leftButtons.add(panel)
		showWatermarkButton = addButton(JToggleButton(Action.SHOW_WATERMARK.label), Action.SHOW_WATERMARK, leftButtons, null)
		showWatermarkButton.isSelected = getBooleanProperty(PROP_SHOW_WATERMARK_BOOLEAN, false)
		val scaleString = getStringProperty(PROP_COLOR_SCALE, ColorTable.Scale.RAINBOW_SCALE.name)
		val scale = ColorTable.Scale.valueOf(scaleString)
		val colorTable = ColorTable(scale)
		group = ButtonGroup()
		for (s in ColorTable.Scale.entries.toTypedArray()) {
			addRadioButton(Action.SET_SCALE, s.name, rightButtons, group, s == scale)
		}
		addButton(Action.INVERT_COLORS, rightButtons)
		addButton(Action.BRIGHTEN_COLORS, rightButtons)
		addButton(Action.DARKEN_COLORS, rightButtons)
		//addButton(Action.ROTATE_COLORS, rightButtons);
		run {
			val b = Button("Rotate")
			b.addMouseListener(object : MouseButtonListener() {
				override fun doAction() {
					fractalComponent.colorTable.rotateColors()
					fractalComponent.startNewFractal(true)
				}
			})
			rightButtons.add(b, null)
		}
		addButton(Action.MAKE_MOVIE, rightButtons)
		addButton(Action.RENDER, rightButtons)
		fractalComponent = FractalComponent(colorTable, 2)
		fractalComponent.setShowWatermark(showWatermarkButton.isSelected)
		try {
			when (fractalSet) {
				Action.JULIA_SET -> {
					evaluator.parse(constantExpression.text)
					val constant = evaluator.evaluate()
					fractalComponent.setFractal(constant, Julia())
					constantExpression.isEnabled = true
					animStartField.isEnabled = true
					animEndField.isEnabled = true
					animateButton.isEnabled = true
				}

				Action.CUSTOM_SET -> {
					var constant: ComplexNumber? = null
					constant = try {
						evaluator.parse(constantExpression.text)
						evaluator.evaluate()
					} catch (e: TokenMgrError) {
						e.printStackTrace()
						ComplexNumber()
					}
					//ComplexNumber constant = evaluator.evaluate();
					fractalComponent.setFractal(constant, Custom(expressionText))
					constantExpression.isEnabled = true
					formulaExpression.isEnabled = true
					animStartField.isEnabled = true
					animEndField.isEnabled = true
					animateButton.isEnabled = true
				}

				Action.MANDELBROT_SET -> fractalComponent.setFractal(ComplexNumber(), Mandelbrot())
				else -> fractalComponent.setFractal(ComplexNumber(), Mandelbrot())
			}
		} catch (e: Exception) {
			fractalComponent.setFractal(ComplexNumber(), Mandelbrot())
			e.printStackTrace()
		}
		val zoomLeft = getDoubleProperty(PROP_ZOOM_LEFT, -DEFAULT_ZOOM.toDouble())
		val zoomRight = getDoubleProperty(PROP_ZOOM_RIGHT, DEFAULT_ZOOM.toDouble())
		val zoomTop = getDoubleProperty(PROP_ZOOM_TOP, DEFAULT_ZOOM.toDouble())
		val zoomBottom = getDoubleProperty(PROP_ZOOM_BOTTOM, DEFAULT_ZOOM.toDouble())
		fractalComponent.zoomRect(zoomLeft, zoomRight, zoomTop, zoomBottom)
		add(fractalComponent)
		animation = AnimateThread(fractalComponent, this, constantExpression)
		fractalComponent.fractalListener = this
		val progressLayout = JPanel(BorderLayout())
		addButton(Action.CANCEL, progressLayout, BorderLayout.EAST)
		progressBar.value = 0
		progressBar.isStringPainted = true
		progressLayout.add(progressBar)
		add(progressLayout, BorderLayout.SOUTH)
		addMenuBarMenu("Viewer", "About", "Help")
		if (!restoreFromProperties()) {
			centerToScreen(640, 480)
		}
	}

	override fun onMenuItemSelected(menu: String, subMenu: String) {
		when (menu) {
			"Viewer" -> when (subMenu) {
				"About" -> FileUtils.inputStreamToString(FileUtils.openFileOrResource("about.txt")).also {
					showMessageDialog("About", it)
				}

				"Help" -> FileUtils.inputStreamToString(FileUtils.openFileOrResource("help.txt")).also {
					showMessageDialog("Help", it)
				}
			}
		}
	}

	override fun onProgress(progress: Int) {
		progressBar.value = progress
		progressBar.repaint()
//		synchronized(this) { notifyAll() }
	}

	override fun onDone() {
		undoButton.isEnabled = fractalComponent.canUndo()
		redoButton.isEnabled = fractalComponent.canRedo()
		setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.text)
	}

	fun onAnimationDone(animateThread: AnimateThread) {
		setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.text)
		animateButton.text = Action.ANIMATE.label
		animateButton.isEnabled = true
//		if (!animateThread.isCancelled) {
//			makeMovie()
//		}
	}

	fun makeMovie() {
		val movieName = "fractalAnim" + SimpleDateFormat("yyyyMMddHHmm").format(Date()) + ".mp4"
		val movieFile = File(MOVIES_DIR, movieName)
		val cmd =
			FFMPEG + " -y -r 30 -i " + ANIMS_DIR.absolutePath + "/anim%03d.png -vcodec libx264 -crf 10  -pix_fmt rgb24 " + movieFile
		//        String cmd = "/usr/local/bin/ffmpeg -y -r 30 -i " + ANIMS_DIR.getAbsolutePath() + "/anim%03d.png -pix_fmt rgb24 " + movieFile;
		log.info("Making movie with CMD: $cmd")
		try {
			val p = Runtime.getRuntime().exec(cmd)
			if (p.waitFor() == 0) {
				Runtime.getRuntime().exec("/usr/bin/open " + movieFile.absolutePath)
			} else {
				FileUtils.inputStreamToString(p.errorStream).also {
					log.error(it)
					showError(it)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			showError("Failed to make movie\n${e.message}")
		}
	}

	fun showError(e: Exception) {
		JOptionPane.showMessageDialog(this, "ERROR:" + e.javaClass.simpleName + " " + e.message, "ERROR", JOptionPane.ERROR_MESSAGE)
	}

	fun showError(msg: String) {
		JOptionPane.showMessageDialog(this, "ERROR:$msg", "ERROR", JOptionPane.ERROR_MESSAGE)
	}

	fun addButton(action: Action, panel: JPanel, enabled: Boolean): AbstractButton {
		val b = addButton(JButton(action.label), action, panel, null)
		b.isEnabled = enabled
		return b
	}

	fun addButton(action: Action, panel: JPanel): AbstractButton {
		return addButton(JButton(action.label), action, panel, null)
	}

	@JvmOverloads
	fun addRadioButton(action: Action, panel: JPanel, group: ButtonGroup, chosen: Boolean = false) {
		addRadioButton(action, action.label, panel, group, chosen)
	}

	fun addRadioButton(action: Action, label: String?, panel: JPanel, group: ButtonGroup, chosen: Boolean) {
		val button = JRadioButton(label)
		group.add(button)
		button.isSelected = chosen
		addButton(button, action, panel, null)
	}

	fun addButton(action: Action, panel: JPanel, extra: Any?): AbstractButton {
		return addButton(JButton(action.label), action, panel, null)
	}

	fun addButton(button: AbstractButton, action: Action, panel: JPanel, extra: Any?): AbstractButton {
		button.actionCommand = action.name
		button.addActionListener(this)
		if (extra != null) panel.add(button, extra) else panel.add(button)
		return button
	}

	fun addTextField(fAction: Action, panel: JPanel, defaultText: String?): JTextField {
		return addTextField(fAction, fAction.label, panel, defaultText)
	}

	fun addTextField(fAction: Action, label: String?, panel: JPanel, defaultText: String?): JTextField {
		val input = JTextField(10)
		val container = JPanel()
		input.text = defaultText
		input.setActionCommand(fAction.name)
		input.addActionListener(this)
		container.add(JLabel(label))
		container.add(input)
		panel.add(container)
		return input
	}

	val expressionText: String
		get() {
			val select = formulaExpression.selectedItem ?: return formulas[0]
			return select as String
		}

	fun saveSettings() {
		val i = fractalComponent.lastFractalImage
		setProperty(PROP_ZOOM_LEFT, i.left.toString())
		setProperty(PROP_ZOOM_RIGHT, i.right.toString())
		setProperty(PROP_ZOOM_TOP, i.top.toString())
		setProperty(PROP_ZOOM_BOTTOM, i.bottom.toString())
		setProperty(PROP_ANIM_START_FIELD, animStartField.text)
		setProperty(PROP_ANIM_END_FIELD, animEndField.text)
		setProperty(PROP_ANIM_NUM_FRAMES, animationNumFrames.selectedItem.toString())
		setProperty(PROP_CONSTANT_EXPRESSION, constantExpression.text)
	}

	override fun actionPerformed(ac: ActionEvent) {
		try {
			var command = ac.actionCommand
			log.verbose("Process command: $command")
			if (command == "comboBoxEdited") command = Action.CUSTOM_SET_EXPRESSION.name
			val a = Action.valueOf(command)
			when (a) {
				Action.UNDO -> {
					fractalComponent.undo()
					onDone()
				}

				Action.REDO -> {
					fractalComponent.redo()
					onDone()
				}

				Action.ZOOMIN -> fractalComponent.zoom(0.5)
				Action.ZOOMOUT -> fractalComponent.zoom(2.0)
				Action.CENTER -> fractalComponent.zoomRect(-DEFAULT_ZOOM.toDouble(), DEFAULT_ZOOM.toDouble(), DEFAULT_ZOOM.toDouble(), -DEFAULT_ZOOM.toDouble())
				Action.POSITION -> {
					val left = zoomLeft.text.toDouble()
					val right = zoomRight.text.toDouble()
					val top = zoomTop.text.toDouble()
					val bottom = zoomBottom.text.toDouble()
					fractalComponent.zoomRect(left, right, top, bottom)
				}

				Action.SAVE -> {
					val chooser = JFileChooser()
					chooser.dialogTitle = "Save PNG File"
					val filter = FileNameExtensionFilter(
						"Images", "png", "gif", "jpg", "jpeg")
					chooser.fileFilter = filter
					chooser.currentDirectory = File(getStringProperty(PROP_CURRENT_DIR, ""))
					val returnVal = chooser.showSaveDialog(this)
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						var fileName = chooser.selectedFile.name
						var format = "png"
						val dot = fileName.lastIndexOf('.')
						if (dot < 0) {
							fileName += ".$format"
						} else {
							format = fileName.substring(dot + 1).lowercase(Locale.getDefault())
						}
						val file = File(chooser.currentDirectory, fileName)
						if (!file.exists() || JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "File Exists.  Overwrite?")) {
							setProperty(PROP_CURRENT_DIR, chooser.currentDirectory.absolutePath)
							fractalComponent.saveImage(file, format)
						}
					}
				}

				Action.ANIMATE -> {
					if (animation.isAnimating) {
						if (animation.isPaused) {
							animateButton.text = "Pause"
							animation.resume()
						} else {
							animateButton.text = "Resume"
							animation.pause()
						}
					} else {
						saveSettings()
						evaluator.parse(animStartField.text)
						val start = ComplexNumber(evaluator.evaluate())
						evaluator.parse(animEndField.text)
						val end = ComplexNumber(evaluator.evaluate())
						animateButton.text = "Pause"
						ANIMS_DIR.takeIf { it.isDirectory }?.let {
							// purge directory
							val files = it.listFiles()
							for (f in files) {
								f.delete()
							}
							val numFrames = animationNumFrames.selectedItem.toString().toInt()
							log.debug("Num frames = numFrames")
							animation.startAnimation(it, start, end, numFrames)
						} ?: showError("Anims dir is not a directory or cannot be written to: $ANIMS_DIR")
					}
				}

				Action.SET_SCALE -> {
					val scale = ColorTable.Scale.valueOf((ac.source as AbstractButton).text)
					fractalComponent.colorTable.scale = scale
					fractalComponent.startNewFractal(true)
					setProperty(PROP_COLOR_SCALE, scale.name)
				}

				Action.MANDELBROT_SET -> {
					constantExpression.isEnabled = false
					formulaExpression.isEnabled = false
					animStartField.isEnabled = false
					animEndField.isEnabled = false
					animateButton.isEnabled = false
					fractalComponent.setFractal(ComplexNumber(), Mandelbrot())
					fractalComponent.reset(true)
					fractalComponent.startNewFractal(false)
					setProperty(PROP_FRACTAL_SET, a.name)
				}

				Action.JULIA_SET -> {
					constantExpression.isEnabled = true
					formulaExpression.isEnabled = false
					animStartField.isEnabled = true
					animEndField.isEnabled = true
					animateButton.isEnabled = true
					evaluator.parse(constantExpression.text)
					fractalComponent.setFractal(evaluator.evaluate(), Julia())
					fractalComponent.reset(true)
					fractalComponent.startNewFractal(false)
					setProperty(PROP_FRACTAL_SET, a.name)
				}

				Action.CUSTOM_SET -> {
					constantExpression.isEnabled = true
					formulaExpression.isEnabled = true
					animStartField.isEnabled = true
					animEndField.isEnabled = true
					animateButton.isEnabled = true
					evaluator.parse(constantExpression.text)
					val c = evaluator.evaluate()
					fractalComponent.setFractal(c, Custom(expressionText))
					fractalComponent.reset(true)
					fractalComponent.startNewFractal(false)
					setProperty(PROP_FRACTAL_SET, a.name)
				}

				Action.CUSTOM_SET_EXPRESSION -> {
					evaluator.parse(constantExpression.text)
					fractalComponent.setFractal(evaluator.evaluate(), Custom(expressionText))
					fractalComponent.reset(true)
					fractalComponent.startNewFractal(false)
					addFormula(expressionText)
				}

				Action.ANIM_CONSTANT_START -> {
					val field = ac.source as JTextField
					//AFractal frac= fractalComponent.getFractal();
					evaluator.parse(field.text)
					fractalComponent.constant = evaluator.evaluate()
					setProperty(PROP_ANIM_START_FIELD, field.text)
					fractalComponent.reset(false)
					fractalComponent.startNewFractal(false)
				}

				Action.ANIM_CONSTANT_END -> {
					val field = ac.source as JTextField
					evaluator.parse(field.text)
					fractalComponent.constant = evaluator.evaluate()
					setProperty(PROP_ANIM_END_FIELD, field.text)
					fractalComponent.reset(false)
					fractalComponent.startNewFractal(false)
				}

				Action.CUSTOM_SET_CONSTANT -> {
					val field = ac.source as JTextField
					evaluator.parse(field.text)
					fractalComponent.constant = evaluator.evaluate()
					setProperty(PROP_CONSTANT_EXPRESSION, field.text)
					fractalComponent.reset(false)
					fractalComponent.startNewFractal(false)
				}

				Action.INVERT_COLORS -> {
					fractalComponent.colorTable.invertColors()
					fractalComponent.startNewFractal(true)
				}

				Action.BRIGHTEN_COLORS -> {
					fractalComponent.colorTable.brightenColors()
					fractalComponent.startNewFractal(true)
				}

				Action.DARKEN_COLORS -> {
					fractalComponent.colorTable.darkenColors()
					fractalComponent.startNewFractal(true)
				}

				Action.ROTATE_COLORS -> {}
				Action.CANCEL -> animation.stop()
				Action.SHOW_WATERMARK -> {
					val selected = (ac.source as AbstractButton).isSelected
					fractalComponent.setShowWatermark(selected)
					setProperty(PROP_SHOW_WATERMARK_BOOLEAN, selected.toString())
				}

				Action.MAKE_MOVIE -> {
					makeMovie()
				}

				Action.RENDER -> {
				}
			}
		} catch (e: TokenMgrError) {
			showError(e.javaClass.simpleName + ":" + e.message)
		} catch (e: Exception) {
			e.printStackTrace()
			showError(e.javaClass.simpleName + ":" + e.message)
		}
	}

	fun loadFormulas() {
		try {
			val file = FORMULAS_FILE
			if (file.exists()) {
				log.info("Loading file " + file.absolutePath)
				BufferedReader(InputStreamReader(FileInputStream(file))).use {
					while (true) {
						val formula = it.readLine() ?: break
						if (!formulas.contains(formula)) formulas.add(formula)
					}
				}
				log.info("Loaded " + formulas.size + " formulas")
			} else {
				formulas.add("Z^2 + Z0")
				formulas.add("Z^2 - C")
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun addFormula(formula: String) {
		formulas.remove(formula)
		formulas.add(0, formula)
		var out: PrintWriter? = null
		try {
			out = PrintWriter(FileWriter(FORMULAS_FILE))
			for (f in formulas) {
				out.println(f)
			}
		} catch (e: Exception) {
			this.showError(e)
			e.printStackTrace()
		} finally {
			if (out != null) {
				try {
					out.close()
				} catch (e: Exception) {
				}
			}
		}
	}

	public override fun onWindowClosing() {
		animation.stop()
	}

	companion object {
		const val PROP_CURRENT_DIR = "CURRENT_DIRECTORY"
		const val PROP_ANIM_START_FIELD = "ANIM_START_FIELD"
		const val PROP_ANIM_END_FIELD = "ANIM_END_FIELD"
		const val PROP_CONSTANT_EXPRESSION = "CONSTANT_EXPRESSION"
		const val PROP_COLOR_SCALE = "COLOR_SCALE"
		const val PROP_ZOOM_LEFT = "ZOOM_LEFT"
		const val PROP_ZOOM_RIGHT = "ZOOM_RIGHT"
		const val PROP_ZOOM_TOP = "ZOOM_TOP"
		const val PROP_ZOOM_BOTTOM = "ZOOM_BOTTOM"
		const val PROP_FRACTAL_SET = "FRACTAL_SET"
		const val PROP_ANIM_NUM_FRAMES = "ANIM_NUM_FRAMES"
		const val PROP_SHOW_WATERMARK_BOOLEAN = "SHOW_WATERMARK"
		val frameOptions = arrayOf("10", "25", "50", "100", "150", "200", "250", "300", "400", "500")

		@JvmStatic
		fun main(args: Array<String>) {
			FractalViewer()
		}
	}
}
