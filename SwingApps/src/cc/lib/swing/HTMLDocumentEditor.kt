package cc.lib.swing

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import javax.swing.*
import javax.swing.event.UndoableEditEvent
import javax.swing.event.UndoableEditListener
import javax.swing.filechooser.FileFilter
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.DefaultEditorKit.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit.*
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.HTMLEditorKit.InsertHTMLTextAction
import javax.swing.undo.CannotRedoException
import javax.swing.undo.CannotUndoException
import javax.swing.undo.UndoManager

/*	HTMLDocumentEditor.java
*	@author: Charles Bell
*	@version: May 27, 2002	
*/   class HTMLDocumentEditor : JFrame("HTMLDocumentEditor"), ActionListener {
	private var document: HTMLDocument
	private var textPane = JTextPane()
	private val debug = false
	private var currentFile: File? = null

	/** Listener for the edits on the current document.  */
	protected var undoHandler: UndoableEditListener = UndoHandler()

	/** UndoManager that we add edits to.  */
	protected var undo = UndoManager()
	private val undoAction: UndoAction = UndoAction()
	private val redoAction = RedoAction()
	private val cutAction: Action = CutAction()
	private val copyAction: Action = DefaultEditorKit.CopyAction()
	private val pasteAction: Action = PasteAction()
	private val boldAction: Action = BoldAction()
	private val underlineAction: Action = UnderlineAction()
	private val italicAction: Action = ItalicAction()
	private val insertBreakAction: Action = InsertBreakAction()
	private val unorderedListAction = InsertHTMLTextAction("Bullets", "<ul><li> </li></ul>", HTML.Tag.P, HTML.Tag.UL)
	private val bulletAction = InsertHTMLTextAction("Bullets", "<li> </li>", HTML.Tag.UL, HTML.Tag.LI)

	init {
		val editorKit = HTMLEditorKit()
		document = editorKit.createDefaultDocument() as HTMLDocument
		// Force SwingSet to come up in the Cross Platform L&F
		try {
			//UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			// If you want the System L&F instead, comment out the above line and
			// uncomment the following:
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		} catch (exc: Exception) {
			System.err.println("Error loading L&F: $exc")
		}
		init()
	}

	fun init() {
		addWindowListener(FrameListener())
		val menuBar = JMenuBar()
		contentPane.add(menuBar, BorderLayout.NORTH)
		val fileMenu = JMenu("File")
		val editMenu = JMenu("Edit")
		val colorMenu = JMenu("Color")
		val fontMenu = JMenu("Font")
		val styleMenu = JMenu("Style")
		val alignMenu = JMenu("Align")
		val helpMenu = JMenu("Help")
		menuBar.add(fileMenu)
		menuBar.add(editMenu)
		menuBar.add(colorMenu)
		menuBar.add(fontMenu)
		menuBar.add(styleMenu)
		menuBar.add(alignMenu)
		menuBar.add(helpMenu)
		val newItem = JMenuItem("New", ImageIcon("whatsnew-bang.gif"))
		val openItem = JMenuItem("Open", ImageIcon("open.gif"))
		val saveItem = JMenuItem("Save", ImageIcon("save.gif"))
		val saveAsItem = JMenuItem("Save As")
		val exitItem = JMenuItem("Exit", ImageIcon("exit.gif"))
		newItem.addActionListener(this)
		openItem.addActionListener(this)
		saveItem.addActionListener(this)
		saveAsItem.addActionListener(this)
		exitItem.addActionListener(this)
		fileMenu.add(newItem)
		fileMenu.add(openItem)
		fileMenu.add(saveItem)
		fileMenu.add(saveAsItem)
		fileMenu.add(exitItem)
		val undoItem = JMenuItem(undoAction)
		val redoItem = JMenuItem(redoAction)
		val cutItem = JMenuItem(cutAction)
		val copyItem = JMenuItem(copyAction)
		val pasteItem = JMenuItem(pasteAction)
		val clearItem = JMenuItem("Clear")
		val selectAllItem = JMenuItem("Select All")
		val insertBreaKItem = JMenuItem(insertBreakAction)
		val unorderedListItem = JMenuItem(unorderedListAction)
		val bulletItem = JMenuItem(bulletAction)
		cutItem.text = "Cut"
		copyItem.text = "Copy"
		pasteItem.text = "Paste"
		insertBreaKItem.text = "Break"
		cutItem.icon = ImageIcon("cut.gif")
		copyItem.icon = ImageIcon("copy.gif")
		pasteItem.icon = ImageIcon("paste.gif")
		insertBreaKItem.icon = ImageIcon("break.gif")
		unorderedListItem.icon = ImageIcon("bullets.gif")
		clearItem.addActionListener(this)
		selectAllItem.addActionListener(this)
		editMenu.add(undoItem)
		editMenu.add(redoItem)
		editMenu.add(cutItem)
		editMenu.add(copyItem)
		editMenu.add(pasteItem)
		editMenu.add(clearItem)
		editMenu.add(selectAllItem)
		editMenu.add(insertBreaKItem)
		editMenu.add(unorderedListItem)
		editMenu.add(bulletItem)
		val redTextItem = JMenuItem(ForegroundAction("Red", Color.red))
		val orangeTextItem = JMenuItem(ForegroundAction("Orange", Color.orange))
		val yellowTextItem = JMenuItem(ForegroundAction("Yellow", Color.yellow))
		val greenTextItem = JMenuItem(ForegroundAction("Green", Color.green))
		val blueTextItem = JMenuItem(ForegroundAction("Blue", Color.blue))
		val cyanTextItem = JMenuItem(ForegroundAction("Cyan", Color.cyan))
		val magentaTextItem = JMenuItem(ForegroundAction("Magenta", Color.magenta))
		val blackTextItem = JMenuItem(ForegroundAction("Black", Color.black))
		redTextItem.icon = ImageIcon("red.gif")
		orangeTextItem.icon = ImageIcon("orange.gif")
		yellowTextItem.icon = ImageIcon("yellow.gif")
		greenTextItem.icon = ImageIcon("green.gif")
		blueTextItem.icon = ImageIcon("blue.gif")
		cyanTextItem.icon = ImageIcon("cyan.gif")
		magentaTextItem.icon = ImageIcon("magenta.gif")
		blackTextItem.icon = ImageIcon("black.gif")
		colorMenu.add(redTextItem)
		colorMenu.add(orangeTextItem)
		colorMenu.add(yellowTextItem)
		colorMenu.add(greenTextItem)
		colorMenu.add(blueTextItem)
		colorMenu.add(cyanTextItem)
		colorMenu.add(magentaTextItem)
		colorMenu.add(blackTextItem)
		val fontTypeMenu = JMenu("Font Type")
		fontMenu.add(fontTypeMenu)
		val fontTypes = arrayOf("SansSerif", "Serif", "Monospaced", "Dialog", "DialogInput")
		for (i in fontTypes.indices) {
			if (debug) println(fontTypes[i])
			val nextTypeItem = JMenuItem(fontTypes[i])
			nextTypeItem.action = FontFamilyAction(fontTypes[i], fontTypes[i])
			fontTypeMenu.add(nextTypeItem)
		}
		val fontSizeMenu = JMenu("Font Size")
		fontMenu.add(fontSizeMenu)
		val fontSizes = intArrayOf(6, 8, 10, 12, 14, 16, 20, 24, 32, 36, 48, 72)
		for (i in fontSizes.indices) {
			if (debug) println(fontSizes[i])
			val nextSizeItem = JMenuItem(fontSizes[i].toString())
			nextSizeItem.action = FontSizeAction(fontSizes[i].toString(), fontSizes[i])
			fontSizeMenu.add(nextSizeItem)
		}
		val boldMenuItem = JMenuItem(boldAction)
		val underlineMenuItem = JMenuItem(underlineAction)
		val italicMenuItem = JMenuItem(italicAction)
		boldMenuItem.text = "Bold"
		underlineMenuItem.text = "Underline"
		italicMenuItem.text = "Italic"
		boldMenuItem.icon = ImageIcon("bold.gif")
		underlineMenuItem.icon = ImageIcon("underline.gif")
		italicMenuItem.icon = ImageIcon("italic.gif")
		styleMenu.add(boldMenuItem)
		styleMenu.add(underlineMenuItem)
		styleMenu.add(italicMenuItem)
		val subscriptMenuItem = JMenuItem(SubscriptAction())
		val superscriptMenuItem = JMenuItem(SuperscriptAction())
		val strikeThroughMenuItem = JMenuItem(StrikeThroughAction())
		subscriptMenuItem.text = "Subscript"
		superscriptMenuItem.text = "Superscript"
		strikeThroughMenuItem.text = "StrikeThrough"
		subscriptMenuItem.icon = ImageIcon("subscript.gif")
		superscriptMenuItem.icon = ImageIcon("superscript.gif")
		strikeThroughMenuItem.icon = ImageIcon("strikethough.gif")
		styleMenu.add(subscriptMenuItem)
		styleMenu.add(superscriptMenuItem)
		styleMenu.add(strikeThroughMenuItem)
		val leftAlignMenuItem = JMenuItem(AlignmentAction("Left Align", StyleConstants.ALIGN_LEFT))
		val centerMenuItem = JMenuItem(AlignmentAction("Center", StyleConstants.ALIGN_CENTER))
		val rightAlignMenuItem = JMenuItem(AlignmentAction("Right Align", StyleConstants.ALIGN_RIGHT))
		leftAlignMenuItem.text = "Left Align"
		centerMenuItem.text = "Center"
		rightAlignMenuItem.text = "Right Align"
		leftAlignMenuItem.icon = ImageIcon("left.gif")
		centerMenuItem.icon = ImageIcon("center.gif")
		rightAlignMenuItem.icon = ImageIcon("right.gif")
		alignMenu.add(leftAlignMenuItem)
		alignMenu.add(centerMenuItem)
		alignMenu.add(rightAlignMenuItem)
		val helpItem = JMenuItem("Help")
		helpItem.addActionListener(this)
		helpMenu.add(helpItem)
		val shortcutsItem = JMenuItem("Keyboard Shortcuts")
		shortcutsItem.addActionListener(this)
		helpMenu.add(shortcutsItem)
		val aboutItem = JMenuItem("About QuantumHyperSpace")
		aboutItem.addActionListener(this)
		helpMenu.add(aboutItem)
		val editorControlPanel = JPanel()
		//editorControlPanel.setLayout(new GridLayout(3,3));
		editorControlPanel.layout = FlowLayout()

		/* JButtons */
		val cutButton = JButton(cutAction)
		val copyButton = JButton(copyAction)
		val pasteButton = JButton(pasteAction)
		val boldButton = JButton(boldAction)
		val underlineButton = JButton(underlineAction)
		val italicButton = JButton(italicAction)


		//JButton insertButton = new JButton(insertAction);
		//JButton insertBreakButton = new JButton(insertBreakAction);
		//JButton tabButton = new JButton(tabAction);
		cutButton.text = "Cut"
		copyButton.text = "Copy"
		pasteButton.text = "Paste"
		boldButton.text = "Bold"
		underlineButton.text = "Underline"
		italicButton.text = "Italic"

		//insertButton.setText("Insert");
		//insertBreakButton.setText("Insert Break");
		//tabButton.setText("Tab");
		cutButton.icon = ImageIcon("cut.gif")
		copyButton.icon = ImageIcon("copy.gif")
		pasteButton.icon = ImageIcon("paste.gif")
		boldButton.icon = ImageIcon("bold.gif")
		underlineButton.icon = ImageIcon("underline.gif")
		italicButton.icon = ImageIcon("italic.gif")
		editorControlPanel.add(cutButton)
		editorControlPanel.add(copyButton)
		editorControlPanel.add(pasteButton)
		editorControlPanel.add(boldButton)
		editorControlPanel.add(underlineButton)
		editorControlPanel.add(italicButton)


		//editorControlPanel.add(insertButton);
		//editorControlPanel.add(insertBreakButton);
		//editorControlPanel.add(tabButton);
		val subscriptButton = JButton(SubscriptAction())
		val superscriptButton = JButton(SuperscriptAction())
		val strikeThroughButton = JButton(StrikeThroughAction())
		subscriptButton.icon = ImageIcon("subscript.gif")
		superscriptButton.icon = ImageIcon("superscript.gif")
		strikeThroughButton.icon = ImageIcon("strikethough.gif")
		val specialPanel = JPanel()
		specialPanel.layout = FlowLayout()
		specialPanel.add(subscriptButton)
		specialPanel.add(superscriptButton)
		specialPanel.add(strikeThroughButton)

		//JButton leftAlignButton = new JButton(new AlignLeftAction());
		//JButton centerButton = new JButton(new CenterAction());
		//JButton rightAlignButton = new JButton(new AlignRightAction());
		val leftAlignButton = JButton(AlignmentAction("Left Align", StyleConstants.ALIGN_LEFT))
		val centerButton = JButton(AlignmentAction("Center", StyleConstants.ALIGN_CENTER))
		val rightAlignButton = JButton(AlignmentAction("Right Align", StyleConstants.ALIGN_RIGHT))
		val colorButton = JButton(AlignmentAction("Right Align", StyleConstants.ALIGN_RIGHT))
		leftAlignButton.icon = ImageIcon("left.gif")
		centerButton.icon = ImageIcon("center.gif")
		rightAlignButton.icon = ImageIcon("right.gif")
		colorButton.icon = ImageIcon("color.gif")
		leftAlignButton.text = "Left Align"
		centerButton.text = "Center"
		rightAlignButton.text = "Right Align"
		val alignPanel = JPanel()
		alignPanel.layout = FlowLayout()
		alignPanel.add(leftAlignButton)
		alignPanel.add(centerButton)
		alignPanel.add(rightAlignButton)
		document.addUndoableEditListener(undoHandler)
		resetUndoManager()
		textPane = JTextPane(document)
		textPane.contentType = "text/html"
		val scrollPane = JScrollPane(textPane)
		val screenSize = Toolkit.getDefaultToolkit().screenSize
		val scrollPaneSize = Dimension(5 * screenSize.width / 8, 5 * screenSize.height / 8)
		scrollPane.preferredSize = scrollPaneSize
		val toolPanel = JPanel()
		toolPanel.layout = BorderLayout()
		toolPanel.add(editorControlPanel, BorderLayout.NORTH)
		toolPanel.add(specialPanel, BorderLayout.CENTER)
		toolPanel.add(alignPanel, BorderLayout.SOUTH)
		contentPane.add(menuBar, BorderLayout.NORTH)
		//getContentPane().add(toolPanel, BorderLayout.CENTER);	
		contentPane.add(scrollPane, BorderLayout.SOUTH)
		pack()
		setLocationRelativeTo(null)
		startNewDocument()
		show()
	}

	override fun actionPerformed(ae: ActionEvent) {
		val actionCommand = ae.actionCommand
		if (debug) {
			val modifier = ae.modifiers
			val `when` = ae.getWhen()
			val parameter = ae.paramString()
			println("actionCommand: $actionCommand")
			println("modifier: $modifier")
			println("when: $`when`")
			println("parameter: $parameter")
		}
		if (actionCommand.compareTo("New") == 0) {
			startNewDocument()
		} else if (actionCommand.compareTo("Open") == 0) {
			openDocument()
		} else if (actionCommand.compareTo("Save") == 0) {
			saveDocument()
		} else if (actionCommand.compareTo("Save As") == 0) {
			saveDocumentAs()
		} else if (actionCommand.compareTo("Exit") == 0) {
			exit()
		} else if (actionCommand.compareTo("Clear") == 0) {
			clear()
		} else if (actionCommand.compareTo("Select All") == 0) {
			selectAll()
		} else if (actionCommand.compareTo("Help") == 0) {
			help()
		} else if (actionCommand.compareTo("Keyboard Shortcuts") == 0) {
			showShortcuts()
		} else if (actionCommand.compareTo("About QuantumHyperSpace") == 0) {
			aboutQuantumHyperSpace()
		}
	}

	protected fun resetUndoManager() {
		undo.discardAllEdits()
		undoAction.update()
		redoAction.update()
	}

	fun startNewDocument() {
		val oldDoc = textPane.document
		oldDoc?.removeUndoableEditListener(undoHandler)
		val editorKit = HTMLEditorKit()
		document = editorKit.createDefaultDocument() as HTMLDocument
		textPane.document = document
		currentFile = null
		title = "HTMLDocumentEditor"
		textPane.document.addUndoableEditListener(undoHandler)
		resetUndoManager()
	}

	fun openDocument() {
		try {
			val current = File(".")
			val chooser = JFileChooser(current)
			chooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
			chooser.fileFilter = HTMLFileFilter()
			val approval = chooser.showSaveDialog(this)
			if (approval == JFileChooser.APPROVE_OPTION) {
				chooser.selectedFile?.let {
					currentFile = it
					title = it.getName()
					val fr = FileReader(it)
					val oldDoc = textPane.document
					oldDoc?.removeUndoableEditListener(undoHandler)
					val editorKit = HTMLEditorKit()
					document = editorKit.createDefaultDocument() as HTMLDocument
					editorKit.read(fr, document, 0)
					document.addUndoableEditListener(undoHandler)
					textPane.document = document
					resetUndoManager()
				}
			}
		} catch (ble: BadLocationException) {
			System.err.println("BadLocationException: " + ble.message)
		} catch (fnfe: FileNotFoundException) {
			System.err.println("FileNotFoundException: " + fnfe.message)
		} catch (ioe: IOException) {
			System.err.println("IOException: " + ioe.message)
		}
	}

	fun saveDocument() {
		if (currentFile != null) {
			try {
				val fw = FileWriter(currentFile)
				fw.write(textPane.text)
				fw.close()
			} catch (fnfe: FileNotFoundException) {
				System.err.println("FileNotFoundException: " + fnfe.message)
			} catch (ioe: IOException) {
				System.err.println("IOException: " + ioe.message)
			}
		} else {
			saveDocumentAs()
		}
	}

	fun saveDocumentAs() {
		try {
			val current = File(".")
			val chooser = JFileChooser(current)
			chooser.fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
			chooser.fileFilter = HTMLFileFilter()
			val approval = chooser.showSaveDialog(this)
			if (approval == JFileChooser.APPROVE_OPTION) {
				val newFile = chooser.selectedFile
				if (newFile.exists()) {
					val message = """${newFile.absolutePath} already exists. 
Do you want to replace it?"""
					if (JOptionPane.showConfirmDialog(this, message) == JOptionPane.YES_OPTION) {
						currentFile = newFile
						title = currentFile!!.name
						val fw = FileWriter(currentFile)
						fw.write(textPane.text)
						fw.close()
						if (debug) println("Saved " + currentFile!!.absolutePath)
					}
				} else {
					currentFile = File(newFile.absolutePath)
					title = currentFile!!.name
					val fw = FileWriter(currentFile)
					fw.write(textPane.text)
					fw.close()
					if (debug) println("Saved " + currentFile!!.absolutePath)
				}
			}
		} catch (fnfe: FileNotFoundException) {
			System.err.println("FileNotFoundException: " + fnfe.message)
		} catch (ioe: IOException) {
			System.err.println("IOException: " + ioe.message)
		}
	}

	fun exit() {
		val exitMessage = "Are you sure you want to exit?"
		if (JOptionPane.showConfirmDialog(this, exitMessage) == JOptionPane.YES_OPTION) {
			System.exit(0)
		}
	}

	fun clear() {
		startNewDocument()
	}

	fun selectAll() {
		textPane.selectAll()
	}

	fun help() {
		JOptionPane.showMessageDialog(this, """
 	DocumentEditor.java
 	Author: Charles Bell
 	Version: May 25, 2002
 	http://www.quantumhyperspace.com
 	QuantumHyperSpace Programming Services
 	""".trimIndent())
	}

	fun showShortcuts() {
		val shortcuts = """
	       	Navigate in    |  Tab
	       	Navigate out   |  Ctrl+Tab
	       	Navigate out backwards    |  Shift+Ctrl+Tab
	       	Move up/down a line    |  Up/Down Arrown
	       	Move left/right a component or char    |  Left/Right Arrow
	       	Move up/down one vertical block    |  PgUp/PgDn
	       	Move to start/end of line    |  Home/End
	       	Move to previous/next word    |  Ctrl+Left/Right Arrow
	       	Move to start/end of data    |  Ctrl+Home/End
	       	Move left/right one block    |  Ctrl+PgUp/PgDn
	       	Select All    |  Ctrl+A
	       	Extend selection up one line    |  Shift+Up Arrow
	       	Extend selection down one line    |  Shift+Down Arrow
	       	Extend selection to beginning of line    |  Shift+Home
	       	Extend selection to end of line    |  Shift+End
	       	Extend selection to beginning of data    |  Ctrl+Shift+Home
	       	Extend selection to end of data    |  Ctrl+Shift+End
	       	Extend selection left    |  Shift+Right Arrow
	       	Extend selection right    |  Shift+Right Arrow
	       	Extend selection up one vertical block    |  Shift+PgUp
	       	Extend selection down one vertical block    |  Shift+PgDn
	       	Extend selection left one block    |  Ctrl+Shift+PgUp
	       	Extend selection right one block    |  Ctrl+Shift+PgDn
	       	Extend selection left one word    |  Ctrl+Shift+Left Arrow
	       	Extend selection right one word    |  Ctrl+Shift+Right Arrow
	       	
	       	""".trimIndent()
		JOptionPane.showMessageDialog(this, shortcuts)
	}

	fun aboutQuantumHyperSpace() {
		JOptionPane.showMessageDialog(this, """QuantumHyperSpace Programming Services
http://www.quantumhyperspace.com
email: support@quantumhyperspace.com
                     or 
email: charles@quantumhyperspace.com
""",
			"QuantumHyperSpace", JOptionPane.INFORMATION_MESSAGE,
			ImageIcon("quantumhyperspace.gif"))
	}

	internal inner class FrameListener : WindowAdapter() {
		override fun windowClosing(we: WindowEvent) {
			exit()
		}
	}

	internal inner class SubscriptAction : StyledTextAction(StyleConstants.Subscript.toString()) {
		override fun actionPerformed(ae: ActionEvent) {
			val editor = getEditor(ae)
			if (editor != null) {
				val kit = getStyledEditorKit(editor)
				val attr = kit.inputAttributes
				val subscript = if (StyleConstants.isSubscript(attr)) false else true
				val sas = SimpleAttributeSet()
				StyleConstants.setSubscript(sas, subscript)
				setCharacterAttributes(editor, sas, false)
			}
		}
	}

	internal inner class SuperscriptAction : StyledTextAction(StyleConstants.Superscript.toString()) {
		override fun actionPerformed(ae: ActionEvent) {
			val editor = getEditor(ae)
			if (editor != null) {
				val kit = getStyledEditorKit(editor)
				val attr = kit.inputAttributes
				val superscript = if (StyleConstants.isSuperscript(attr)) false else true
				val sas = SimpleAttributeSet()
				StyleConstants.setSuperscript(sas, superscript)
				setCharacterAttributes(editor, sas, false)
			}
		}
	}

	internal inner class StrikeThroughAction : StyledTextAction(StyleConstants.StrikeThrough.toString()) {
		override fun actionPerformed(ae: ActionEvent) {
			val editor = getEditor(ae)
			if (editor != null) {
				val kit = getStyledEditorKit(editor)
				val attr = kit.inputAttributes
				val strikeThrough = if (StyleConstants.isStrikeThrough(attr)) false else true
				val sas = SimpleAttributeSet()
				StyleConstants.setStrikeThrough(sas, strikeThrough)
				setCharacterAttributes(editor, sas, false)
			}
		}
	}

	internal inner class HTMLFileFilter : FileFilter() {
		override fun accept(f: File): Boolean {
			return f.isDirectory || f.name.toLowerCase().indexOf(".htm") > 0
		}

		override fun getDescription(): String {
			return "html"
		}
	}

	internal inner class UndoHandler : UndoableEditListener {
		/**
		 * Messaged when the Document has created an edit, the edit is
		 * added to `undo`, an instance of UndoManager.
		 */
		override fun undoableEditHappened(e: UndoableEditEvent) {
			undo.addEdit(e.edit)
			undoAction.update()
			redoAction.update()
		}
	}

	internal inner class UndoAction : AbstractAction("Undo") {
		init {
			isEnabled = false
		}

		override fun actionPerformed(e: ActionEvent) {
			try {
				undo.undo()
			} catch (ex: CannotUndoException) {
				println("Unable to undo: $ex")
				ex.printStackTrace()
			}
			update()
			redoAction.update()
		}

		fun update() {
			if (undo.canUndo()) {
				isEnabled = true
				putValue(NAME, undo.undoPresentationName)
			} else {
				isEnabled = false
				putValue(NAME, "Undo")
			}
		}
	}

	internal inner class RedoAction : AbstractAction("Redo") {
		init {
			isEnabled = false
		}

		override fun actionPerformed(e: ActionEvent) {
			try {
				undo.redo()
			} catch (ex: CannotRedoException) {
				System.err.println("Unable to redo: $ex")
				ex.printStackTrace()
			}
			update()
			undoAction.update()
		}

		fun update() {
			if (undo.canRedo()) {
				isEnabled = true
				putValue(NAME, undo.redoPresentationName)
			} else {
				isEnabled = false
				putValue(NAME, "Redo")
			}
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val editor = HTMLDocumentEditor()
		}
	}
}