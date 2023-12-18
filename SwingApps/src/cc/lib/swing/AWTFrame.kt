package cc.lib.swing

import cc.lib.game.GColor
import cc.lib.game.Utils
import cc.lib.logger.LoggerFactory
import java.awt.*
import java.awt.event.*
import java.io.*
import java.util.*
import javax.swing.*
import javax.swing.event.MenuEvent
import javax.swing.event.MenuKeyEvent
import javax.swing.event.MenuKeyListener
import javax.swing.event.MenuListener
import javax.swing.filechooser.FileFilter

open class AWTFrame : JFrame, WindowListener, ComponentListener, MenuListener, MenuKeyListener {
	val log = LoggerFactory.getLogger(javaClass)
	private var propertiesFile: File? = null
	private val properties = Properties()

	constructor(content: Container?) : this() {
		contentPane = content
	}

	constructor() : super() {
		addWindowListener(this)
		addComponentListener(this)
		//setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	constructor(label: String?) : super(label) {
		addWindowListener(this)
		addComponentListener(this)
	}

	constructor(label: String?, width: Int, height: Int) : super(label) {
		this.setSize(width, height)
		addWindowListener(this)
		addComponentListener(this)
	}

	fun listScreens() {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val gs = ge.screenDevices
		for (i in gs.indices) {
			println(String.format("Screen %2d %10s %10b", i, gs[i].iDstring, gs[i].isFullScreenSupported))
		}
	}

	fun showAsPopup(parent: JFrame) {
		isUndecorated = true
		parent.isEnabled = false
		minimumSize = Dimension(160, 120)
		pack()
		val x = parent.x + parent.width / 2 - width / 2
		val y = parent.y + parent.height / 2 - height / 2
		setLocation(x, y)
		isResizable = false
		isVisible = true
		isAlwaysOnTop = true
		this.parent = parent
	}

	private var parent: JFrame? = null
	fun closePopup() {
//		synchronized(this) { this.notify() }
		isVisible = false
		parent!!.isEnabled = true
		parent!!.isVisible = true
	}

	fun showFullscreenOnScreen(screen: Int) {
		val ge = GraphicsEnvironment
			.getLocalGraphicsEnvironment()
		val gs = ge.screenDevices
		if (screen > -1 && screen < gs.size) {
			gs[screen].fullScreenWindow = this
		} else if (gs.size > 0) {
			gs[0].fullScreenWindow = this
		} else {
			throw RuntimeException("No Screens Found")
		}
	}

	fun showOnScreen(screen: Int) {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val gd = ge.screenDevices
		if (screen > -1 && screen < gd.size) {
			setLocation(gd[screen].defaultConfiguration.bounds.x, y)
		} else if (gd.size > 0) {
			setLocation(gd[0].defaultConfiguration.bounds.x, y)
		} else {
			throw RuntimeException("No Screens Found")
		}
	}

	protected open fun onWindowClosing() {}
	protected open fun onWindowResized(w: Int, h: Int) {}

	@Synchronized
	protected fun saveToFile() {
		if (propertiesFile != null) {
			val p = getProperties()
			p.setProperty("gui.x", x.toString())
			p.setProperty("gui.y", y.toString())
			p.setProperty("gui.w", width.toString())
			p.setProperty("gui.h", height.toString())
			try {
				val out: OutputStream = FileOutputStream(propertiesFile)
				try {
					p.store(out, "")
				} finally {
					out.close()
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	fun saveProperties() {
		propertiesFile?.let { file ->
			try {
				FileOutputStream(file).use {
					properties.store(it, "")
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	@Synchronized
	fun setProperties(props: Properties) {
		properties.clear()
		properties.putAll(props)
		saveProperties()
	}

	@Synchronized
	fun setProperty(name: String, value: Any?) {
		if (value == null) properties.remove(name) else properties.setProperty(name, value.toString())
		saveProperties()
	}

	fun setPropertiesFile(file: File) {
		propertiesFile = file
		try {
			val reader: InputStream = FileInputStream(propertiesFile)
			try {
				properties.load(reader)
				val x = properties.getProperty("gui.x").toInt()
				val y = properties.getProperty("gui.y").toInt()
				val w = properties.getProperty("gui.w").toInt()
				val h = properties.getProperty("gui.h").toInt()
				setBounds(x, y, w, h)
				this.isVisible = true
			} finally {
				reader.close()
			}
		} catch (e: FileNotFoundException) {
			System.err.println("File Not Found: $propertiesFile")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun loadFromFile(propertiesFile: File): Boolean {
		setPropertiesFile(propertiesFile)
		return restoreFromProperties()
	}

	fun restoreFromProperties(): Boolean {
		val p = getProperties()
		try {
			val x = p.getProperty("gui.x").toInt()
			val y = p.getProperty("gui.y").toInt()
			val w = p.getProperty("gui.w").toInt()
			val h = p.getProperty("gui.h").toInt()
			setBounds(x, y, w, h)
			this.isVisible = true
			return true
		} catch (e: NumberFormatException) {
		} catch (e: NullPointerException) {
			// ignore
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	fun addMenuBarMenu(menuName: String?, vararg menuItems: String?) {
		if (menuItemActionListener == null) {
			menuItemActionListener = ActionListener { e ->
				log.debug("actionPerformed: $e")
				val ev = e as ActionEvent
				val cmd = ev.actionCommand
				//JMenuItem source = (JMenuItem)ev.getSource();
				try {
					onMenuItemSelected(selectedMenu!!.text, cmd)
				} catch (ee: Exception) {
					ee.printStackTrace()
				}
				//log.debug("actionPerformed: cmd=" + cmd + " source=" + source);
				//JMenuItem item = (JMenuItem)source;
			}
		}
		addMenuBarMenu(menuName, menuItemActionListener, *menuItems)
	}

	private var menuItemActionListener: ActionListener? = null
	fun addMenuBarMenu(menuName: String?, listener: ActionListener?, vararg menuItems: String?) {
		var bar = jMenuBar
		if (bar == null) {
			bar = JMenuBar()
			jMenuBar = bar
		}
		val menu = JMenu(menuName)
		bar.add(menu)
		menu.addMenuListener(this)
		for (item in menuItems) {
			if (item == null) menu.addSeparator() else {
				val i = menu.add(item)
				//i.addMenuKeyListener(this);
				i.addActionListener(listener)
			}
		}
	}

	override fun windowOpened(ev: WindowEvent) {
		log.debug("windowOpened")
	}

	override fun windowClosed(ev: WindowEvent) {
		log.debug("windowClosed")
	}

	override fun windowClosing(ev: WindowEvent) {
		saveToFile()
		onWindowClosing()
		System.exit(0)
	}

	override fun windowIconified(ev: WindowEvent) {
		log.debug("windowIconified")
	}

	override fun windowDeiconified(ev: WindowEvent) {
		log.debug("windowDeiconified")
	}

	override fun windowActivated(ev: WindowEvent) {
		log.debug("windowActivated")
	}

	override fun windowDeactivated(ev: WindowEvent) {
		log.debug("windowDeactivated")
	}

	fun centerToScreen() {
		pack()
		finalizeFrame(width, height, 1, 1, 0)
	}

	fun centerToScreen(width: Int, height: Int) {
		this.setSize(width, height)
		finalizeFrame(getWidth(), getHeight(), 1, 1, 0)
	}

	fun finalizeToBounds(x: Int, y: Int, w: Int, h: Int) {
		pack()
		setBounds(x, y, w, h)
		isVisible = true
	}

	fun finalizeToPosition(x: Int, y: Int) {
		pack()
		setBounds(x, y, width, height)
		isVisible = true
	}

	fun finalizeToBounds(rect: Rectangle?) {
		pack()
		bounds = rect
		isVisible = true
	}

	fun fullscreenMode() {
		//pack();
		isUndecorated = true
		extendedState = MAXIMIZED_BOTH
		isVisible = true
		pack()
	}

	/*
	 * 
	 * @param hJust 0==LEFT, 1==CENTER, 2==RIGHT
	 * @param vJust 0==TOP, 1==CENTER, 2==RIGHT
	 * @param padding border padding
	 */
	private fun finalizeFrame(w: Int, h: Int, hJust: Int, vJust: Int, padding: Int) {
		val dim = Toolkit.getDefaultToolkit().screenSize
		//int w = getWidth();
		//int h = getHeight();
		val x: Int
		val y: Int
		x = when (hJust) {
			0 -> padding
			1 -> dim.width / 2 - w / 2
			else -> dim.width - w - padding
		}
		y = when (vJust) {
			0 -> padding
			1 -> dim.height / 2 - h / 2
			else -> dim.height - h - padding
		}
		this.setBounds(x, y, w, h)
		this.isVisible = true
	}

	override fun componentHidden(arg0: ComponentEvent) {}
	override fun componentMoved(arg0: ComponentEvent) {
		saveToFile()
	}

	override fun componentResized(arg0: ComponentEvent) {
		saveToFile()
		onWindowResized(width, height)
	}

	override fun componentShown(arg0: ComponentEvent) {}
	private var selectedMenu: JMenu? = null
	override fun menuSelected(e: MenuEvent) {
		log.debug("menuSelected: $e")
		selectedMenu = e.source as JMenu
	}

	override fun menuDeselected(e: MenuEvent) {
		log.debug("menuDeselected: $e")
	}

	override fun menuCanceled(e: MenuEvent) {
		log.debug("menuCancelled: $e")
	}

	override fun menuKeyPressed(e: MenuKeyEvent) {
		log.debug("menuKeyPressed: $e")
	}

	override fun menuKeyReleased(e: MenuKeyEvent) {
		log.debug("menuKeyReleased: $e")
	}

	override fun menuKeyTyped(e: MenuKeyEvent) {
		log.debug("menuKeyTyped: $e")
	}

	protected open fun onMenuItemSelected(menu: String, subMenu: String) {
		log.warn("Unhandled onMenuItemSelected: menu=$menu item=$subMenu")
	}

	fun add(comp: AWTComponent?) {
		super.add(comp)
	}

	override fun repaint() {
		super.repaint()
	}

	override fun validate() {
		clearContainersBackgrounds(contentPane)
		super.validate()
	}

	override fun setEnabled(enabled: Boolean) {
		super.setEnabled(enabled)
		super.setFocusable(enabled)
		super.setFocusableWindowState(enabled)
		if (enabled) {
			isVisible = true
		}
	}

	override fun setVisible(visible: Boolean) {
		super.setVisible(visible)
	}

	var background: GColor
		get() = with(super.getBackground()) {
			GColor(red, green, blue, alpha)
		}
		set(c) {
			val container = contentPane
			container.background = AWTUtils.toColor(c)
			clearContainersBackgrounds(container)
		}

	private fun clearContainersBackgrounds(c: Container) {
		for (comp in c.components) {
			comp.background = null
			if (comp is Container) {
				clearContainersBackgrounds(comp)
			}
		}
	}

	/**
	 * Load properties from file
	 *
	 * @return
	 */
	@Synchronized
	fun getProperties(): Properties = properties

	/**
	 *
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	fun getBooleanProperty(name: String, defaultValue: Boolean): Boolean {
		return try {
			val prop = getProperties().getProperty(name) ?: return defaultValue
			java.lang.Boolean.parseBoolean(prop)
		} catch (e: Exception) {
			defaultValue
		}
	}

	/**
	 *
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	fun getIntProperty(name: String, defaultValue: Int): Int {
		val value = getProperties().getProperty(name)
		try {
			if (value != null) return value.toInt()
		} catch (e: Exception) {
			log.error("Cannot convert value '$value' to integer")
		}
		return defaultValue
	}

	/**
	 *
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	fun getDoubleProperty(name: String, defaultValue: Double): Double {
		val value = getProperties().getProperty(name)
		try {
			if (value != null) return value.toDouble()
		} catch (e: Exception) {
			log.error("Cannot convert value '$value' to double")
		}
		return defaultValue
	}

	/**
	 *
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	fun getFloatProperty(name: String, defaultValue: Float): Float {
		val value = getProperties().getProperty(name)
		try {
			if (value != null) return value.toFloat()
		} catch (e: Exception) {
			log.error("Cannot convert value '$value' to float")
		}
		return defaultValue
	}

	/**
	 *
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	fun getStringProperty(name: String, defaultValue: String): String {
		val value = getProperties().getProperty(name)
		return value ?: defaultValue
	}

	inline fun <reified T : Enum<T>> getEnumProperty(name: String, defaultValue: T): T {
		val value = getProperties().getProperty(name) ?: return defaultValue
		try {
			return enumValueOf(value)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return defaultValue
	}

	inline fun <reified T : Enum<T>> getEnumListProperty(propertyName: String, defaultList: List<T>): List<T> {
		val value = getStringProperty(propertyName, "")
		if (value.isEmpty())
			return defaultList
		val list: MutableList<T> = ArrayList()
		val parts = value.split("[,]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		for (S in parts) {
			val s = S.trim { it <= ' ' }
			if (s.isEmpty()) continue
			try {
				list.add(enumValueOf(value))
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		return list
	}

	fun <T : Enum<T>> getEnumListProperty(propertyName: String, className: Class<T>, defaultList: List<T>): List<T> {
		val value = getStringProperty(propertyName, "")
		if (value.isEmpty())
			return defaultList
		val list: MutableList<T> = ArrayList()
		val parts = value.split("[,]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		for (S in parts) {
			val s = S.trim { it <= ' ' }
			if (s.isEmpty()) continue
			className.enumConstants.firstOrNull {
				it.name == s
			}?.let {
				list.add(it)
			}
		}
		return list
	}


	fun <T : Enum<T>> setEnumListProperty(property: String, items: Collection<T>) {
		setProperty(property, Utils.trimEnclosure(items.toString()))
	}
	/**
	 *
	 * @return
	 */
	/**
	 *
	 * @param dir
	 */
	var workingDir: File
		get() {
			val p = getProperties()
			val dir = p.getProperty("workingDir")
			return if (dir != null) File(dir) else File(".")
		}
		set(dir) {
			val p = getProperties()
			p.setProperty("workingDir", dir.absolutePath)
			setProperties(p)
		}

	/**
	 *
	 * @param title
	 * @param extension
	 * @return
	 */
	fun showFileOpenChooser(title: String?, extension: String?, description: String?): File? {
		var extension = extension
		val chooser = JFileChooser()
		chooser.currentDirectory = workingDir
		chooser.dialogTitle = title
		chooser.fileSelectionMode = JFileChooser.FILES_ONLY
		if (extension != null) {
			if (!extension.startsWith(".")) extension = ".$extension"
			chooser.fileFilter = getExtensionFilter(extension, description, true)
		}
		val result = chooser.showOpenDialog(this)
		if (result == JFileChooser.APPROVE_OPTION) {
			val file = chooser.selectedFile
			workingDir = file.parentFile
			var fileName = file.absolutePath
			if (extension != null && !fileName.endsWith(extension)) fileName += extension
			return File(fileName)
		}
		return null
	}

	/**
	 *
	 * @param title
	 * @param extension
	 * @param selectedFile
	 * @return
	 */
	fun showFileSaveChooser(title: String?, extension: String?, description: String?, selectedFile: File?): File? {
		var extension = extension
		val chooser = JFileChooser()
		chooser.selectedFile = selectedFile
		chooser.currentDirectory = workingDir
		chooser.dialogTitle = title
		chooser.fileSelectionMode = JFileChooser.FILES_ONLY
		if (extension != null) {
			if (!extension.startsWith(".")) extension = ".$extension"
			chooser.fileFilter = getExtensionFilter(extension, description, true)
		}
		val result = chooser.showSaveDialog(this)
		if (result == JFileChooser.APPROVE_OPTION) {
			val file = chooser.selectedFile
			workingDir = file.parentFile
			var fileName = file.name
			if (extension != null && !fileName.endsWith(extension)) fileName += extension
			return File(file.parent, fileName)
		}
		return null
	}

	enum class MessageIconType(val type: Int) {
		PLAIN(JOptionPane.PLAIN_MESSAGE),
		QUESTION(JOptionPane.QUESTION_MESSAGE),
		INFO(JOptionPane.INFORMATION_MESSAGE),
		WARNING(JOptionPane.WARNING_MESSAGE),
		ERROR(JOptionPane.ERROR_MESSAGE);
	}
	/**
	 *
	 * @param title
	 * @param message
	 * @param icon
	 */
	/**
	 *
	 * @param title
	 * @param message
	 */
	@JvmOverloads
	fun showMessageDialog(title: String?, message: String, icon: MessageIconType = MessageIconType.PLAIN) {
		var message = message
		if (message.length > 32) {
			message = Utils.wrapTextWithNewlines(message, 64)
		}
		JOptionPane.showMessageDialog(this, message, title, icon.type)
	}

	fun showMessageDialogWithHTMLContent(titleStr: String?, html: String?) {
		val dialog = AWTFrame()
		val content = JPanel()
		content.layout = BorderLayout()
		val txt = JTextPane()
		txt.isEditable = false
		txt.contentType = "text/html"
		txt.text = html
		content.add(txt, BorderLayout.CENTER)
		val title: JLabel = AWTLabel(titleStr, 1, 16f, true)
		content.add(title, BorderLayout.NORTH)
		val close: JButton = object : AWTButton("Close") {
			override fun onAction() {
				dialog.closePopup()
			}
		}
		content.add(close, BorderLayout.SOUTH)
		dialog.add(content, BorderLayout.CENTER)
		dialog.showAsPopup(this)
		//        dialog.pack();
		//      dialog.setVisible(true);
	}

	/**
	 * Show drop down menu with options
	 *
	 * @param title
	 * @param message
	 * @param items
	 * @return index of the chosen item or -1 if cancelled
	 */
	fun showItemChooserDialog(title: String?, message: String?, selectedItem: String?, vararg items: String?): Int {
		return JOptionPane.showOptionDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, items, selectedItem
			?: items[0])
	}

	/**
	 * Show a dialog with yes/no buttons. Return true if yes was pressed.
	 *
	 * @param title
	 * @param message
	 * @return
	 */
	fun showYesNoDialog(title: String?, message: String?): Boolean {
		val n = JOptionPane.showConfirmDialog(
			this,
			message,
			title,
			JOptionPane.YES_NO_OPTION)
		return n == JOptionPane.YES_OPTION
	}

	interface OnListItemChoosen {
		fun itemChoose(index: Int)
		fun cancelled() {}
	}

	/**
	 * Show chooser with list of items
	 *
	 * @param title
	 * @param items
	 * @return
	 */
	fun showListChooserDialog(itemListener: OnListItemChoosen, title: String?, vararg items: String) {
		val popup = AWTFrame()
		val listener = ActionListener { e: ActionEvent ->
			val index = Utils.linearSearch(items, e.actionCommand)
			if (index >= 0) itemListener.itemChoose(index) else itemListener.cancelled()
			popup.closePopup()
		}
		val frame = AWTPanel(BorderLayout())
		val list = AWTPanel(0, 1)
		for (item in items) {
			list.add(AWTButton(item, listener))
		}
		if (title != null) {
			frame.add(AWTLabel(title, 1, 20f, true), BorderLayout.NORTH)
		}
		frame.add(list, BorderLayout.CENTER)
		frame.add(AWTButton("CANCEL", listener), BorderLayout.SOUTH)
		popup.contentPane = frame
		popup.showAsPopup(this)
	}

	companion object {
		const val serialVersionUID: Long = 20002
		fun getExtensionFilter(ext: String, description: String?, acceptDirectories: Boolean): FileFilter {
			return object : FileFilter() {
				override fun accept(file: File): Boolean {
					return if (file.isDirectory && acceptDirectories) true else file.name.endsWith(ext!!)
				}

				override fun getDescription(): String {
					return description ?: "unknown"
				}
			}
		}
	}
}