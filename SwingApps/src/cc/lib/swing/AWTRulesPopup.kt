package cc.lib.swing

import cc.lib.annotation.RuleMeta
import cc.lib.reflector.Reflector
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Container
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.io.File
import java.lang.reflect.Field
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSpinner
import javax.swing.JToggleButton
import javax.swing.ScrollPaneConstants
import javax.swing.SpinnerNumberModel

/**
 * Created by Chris Caron on 2/9/24.
 */
open class AWTRulesPopup<T : Reflector<T>>(
	val frame: AWTFrame,
	val rulesOriginal: T,
	val rulesSaveFile: File
) {

	val popupFrame = AWTFrame()
	val view = JPanel()
	val panel = JScrollPane()
	val rules: Reflector<T> = rulesOriginal.deepCopy()

	init {
		panel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		//panel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.preferredSize = Dimension(frame.width, frame.height)
		panel.viewport.add(view)
		view.layout = GridBagLayout()
	}

	val cons = GridBagConstraints().apply {
		fill = GridBagConstraints.BOTH
		anchor = GridBagConstraints.WEST
	}
	val componentFields = HashMap<JComponent, Field>()
	val numCols = 10

	fun show(title: String, editable: Boolean) {
		try {
			val metaMap: Map<Field, RuleMeta> = rules.javaClass.declaredFields.map { field ->
				field.annotations.firstOrNull { it.annotationClass == RuleMeta::class }?.let {
					Pair(field, it as RuleMeta).also {
						field.isAccessible = true
					}
				}
			}.filterNotNull().sortedBy {
				it.second.order
			}.toMap()

			val fieldsMap = mutableMapOf<String, MutableList<Field>>()

			metaMap.forEach { (field, meta) ->
				fieldsMap.getOrPut(meta.variation) {
					mutableListOf()
				}.add(field)
			}
			fieldsMap.forEach { (variation, list) ->
				cons.gridx = 0
				cons.fill = GridBagConstraints.HORIZONTAL
				cons.gridwidth = numCols
				view.add(JLabel(variation), cons)
				cons.gridy++
				view.add(JSeparator(), cons)
				cons.gridy++
				cons.fill = GridBagConstraints.NONE

				list.forEach { f ->
					val ruleVar = metaMap[f]!!
					cons.gridx = 0
					cons.gridwidth = 1
					if (f.type == Boolean::class.javaPrimitiveType) {
						if (editable) {
							val button = JCheckBox("", f.getBoolean(rules))
							view.add(button, cons)
							componentFields[button] = f
						} else {
							view.add(
								JLabel(if (f.getBoolean(rules)) "Enabled" else "Disabled"),
								cons
							)
						}
					} else if (f.type == Int::class.javaPrimitiveType) {
						if (editable) {
							val spinner = JSpinner(
								SpinnerNumberModel(
									f.getInt(rules),
									ruleVar.minValue,
									ruleVar.maxValue,
									1
								)
							)
							view.add(spinner, cons)
							componentFields[spinner] = f
						} else {
							view.add(JLabel("" + f.getInt(rules)), cons)
						}
					} else {
						System.err.println("Don't know how to handle field type:" + f.type)
					}
					cons.gridx = 1
					cons.gridwidth = numCols - 1
					val txt =
						"<html><div WIDTH=900>${ruleVar.description}</div></html>"
					val label = JLabel(txt)
					view.add(label, cons)
					cons.gridy++
				}
			}
			val buttons = arrayOf(
				object : AWTPopupButton("View\nDefaults") {
					override fun doAction(): Boolean {
						Thread {
							AWTRulesPopup(frame, rulesOriginal, rulesSaveFile).show(
								title,
								true
							)
						}.start()
						return false
					}
				},
				object : AWTPopupButton("Save\nAs Default") {
					override fun doAction(): Boolean {
						try {
							for (c in componentFields.keys) {
								val f = componentFields[c]
								if (c is JToggleButton) {
									val value = c.isSelected
									f!!.setBoolean(rules, value)
								} else if (c is JSpinner) {
									val value = c.value as Int
									f!!.setInt(rules, value)
								}
							}
							rules.saveToFile(rulesSaveFile.absoluteFile)
							rulesOriginal.copyFrom(rules)
						} catch (e: Exception) {
							e.printStackTrace()
						}
						return true
					}
				},
				object : AWTPopupButton("Keep") {
					override fun doAction(): Boolean {
						try {
							// TODO: fix cut-paste code
							for (c in componentFields.keys) {
								val f = componentFields[c]
								if (c is JToggleButton) {
									val value = c.isSelected
									f!!.setBoolean(rules, value)
								} else if (c is JSpinner) {
									val value = c.value as Int
									f!!.setInt(rules, value)
								}
							}
							rules.copyFrom(rules)
						} catch (e: Exception) {
							e.printStackTrace()
						}
						return true
					}
				},
				object : AWTPopupButton("Cancel") {
					override fun doAction(): Boolean {
						onCancelled()
						return super.doAction()
					}
				})

			popupFrame.title = title
			val container = JPanel()
			container.border = BorderFactory.createLineBorder(Color.BLACK, 3)
			container.layout = BorderLayout()
			container.add(JLabel(title), BorderLayout.NORTH)
			container.add(view, BorderLayout.CENTER)
			val buttonsContainer = Container()
			container.add(buttonsContainer, BorderLayout.SOUTH)
			buttonsContainer.layout = GridLayout(1, 0)
			buttons.forEach {
				buttonsContainer.add(it)
			}
			popupFrame.contentPane = container
			popupFrame.showAsPopup(frame)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	open fun onCancelled() {
		popupFrame.closePopup()
	}
}