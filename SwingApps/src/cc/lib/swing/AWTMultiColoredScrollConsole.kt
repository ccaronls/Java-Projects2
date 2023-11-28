package cc.lib.swing

import java.util.*
import javax.swing.*

/**
 * This class allows for a console swing component where each console line
 * can be highlighted in its own color by prepending the line with a color code ala bash.
 *
 *
 * @author ccaron
 * TODO: delete
class AWTMultiColoredScrollConsole @JvmOverloads constructor(private val backgroundColor: Color? = null) : JList<AWTMultiColoredScrollConsole.Entry>(), ListCellRenderer<AWTMultiColoredScrollConsole.Entry> {
class Entry internal constructor(val color: Color, val text: String)

private val lines = LinkedList<Entry>()
var maxRecordLines = 150
var textHeight = 16
var visibleLines = 8
private var listener: ListDataListener? = null
private val model: ListModel<Entry> = object : ListModel<Entry> {
override fun getElementAt(index: Int): Entry {
synchronized(lines) { return lines[index] }
}

override fun addListDataListener(l: ListDataListener) {
Utils.assertTrue(listener == null)
listener = l
}

override fun removeListDataListener(l: ListDataListener) {
Utils.assertTrue(listener === l)
listener = null
}

override fun getSize(): Int {
synchronized(lines) { return lines.size }
}
}

init {
this.isOpaque = true
super.setVisibleRowCount(visibleLines)
super.setCellRenderer(this)
super.setModel(model)
}

@Synchronized
fun addText(color: Color, text: String?) {
val width = bounds.width
val scrollBarSize = (UIManager.get("ScrollBar.width") as Int).toInt()
fixedCellWidth = width
//		System.out.println("width = " + width);
val l = AWTUtils.generateWrappedLines(graphics, text, width - scrollBarSize - 5)
for (i in l.indices.reversed()) lines.addFirst(Entry(color, l[i]))
while (lines.size > maxRecordLines) {
lines.removeLast()
if (listener != null) {
listener!!.contentsChanged(ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, lines.size - 1, lines.size - 1))
}
}
if (listener != null) {
listener!!.contentsChanged(ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, 0))
}
super.revalidate()
}

/**
 * Clear the console of all the lines of text
*/
fun clear() {
lines.clear()
}

override fun getListCellRendererComponent(list: JList<out Entry>,
value: Entry, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
val label = JLabel()
label.foreground = value.color
label.text = value.text
return label
}

@Synchronized
public override fun paintComponent(g: Graphics) {
if (backgroundColor != null) {
val rect = bounds
g.color = backgroundColor
g.fillRect(rect.x, rect.y, rect.width, rect.height)
}
super.paintComponent(g)
}
}*/