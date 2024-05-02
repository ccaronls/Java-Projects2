package cc.lib.android

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.ToggleButton
import cc.lib.annotation.RuleMeta
import cc.lib.kreflector.Reflector
import java.lang.reflect.Field

class ConfigDialogBuilder<Rules : Reflector<Rules>>(val context: CCActivityBase) {
	enum class ValueType {
		BOOLEAN,
		INTEGER,
		STRING,
		INT_CHOICE,
		STRING_CHOICE,
		ENUM_CHOICE
	}

	abstract class Config<T>(val type: ValueType) {
		abstract fun get(): T
		abstract fun set(value: T)
		open val min: Int
			get() = 0
		open val max: Int
			get() = 100
		open val choices: Array<T>
			get() {
				TODO()
			}
		open val isEnabled: Boolean
			get() = true
	}

	var values: MutableMap<String, Config<*>> = LinkedHashMap()
	fun addConfig(name: String, config: Config<*>) {
		values[name] = config
	}

	fun constructAdapter(): BaseAdapter {
		return ConfigListAdapter()
	}

	internal inner class ConfigListAdapter : BaseAdapter() {
		var entries: Array<Map.Entry<String, Config<*>>> =
			values.entries.toTypedArray<Map.Entry<String, Config<*>>>()

		override fun getCount(): Int {
			return entries.size
		}

		override fun getItem(position: Int): Any {
			return entries[position]
		}

		override fun getItemId(position: Int): Long {
			return position.toLong()
		}

		@SafeVarargs
		private fun <T> increment(currentValue: T, vararg options: T): T {
			var index = 0
			while (index < options.size) {
				if (currentValue === options[index]) break
				index++
			}
			index = (index + 1) % options.size
			return options[index]
		}

		override fun isEnabled(position: Int): Boolean {
			return entries[position].value.isEnabled
		}

		override fun getView(position: Int, view: View, parent: ViewGroup): View {
			var view = view
			if (view == null) view = View.inflate(context, R.layout.configelem, null)
			val tv = view.findViewById<View>(R.id.textView1) as TextView
			val sb = view.findViewById<View>(R.id.seekBar1) as SeekBar
			val et = view.findViewById<View>(R.id.editText1) as EditText
			val sbt = view.findViewById<View>(R.id.textView2) as TextView
			val tb = view.findViewById<View>(R.id.toggleButton1) as ToggleButton
			val cb = view.findViewById<View>(R.id.choiceButton1) as Button
			val (key) = entries[position]
			tv.text = key
			sb.visibility = View.GONE
			et.visibility = View.GONE
			sbt.visibility = View.GONE
			tb.visibility = View.GONE
			when (entries[position].value.type) {
				ValueType.INTEGER -> {
					val config = entries[position].value as Config<Int>
					val value = config.get()
					//Main.HorizontalSlider slider = new HorizontalSlider(Main.this);
					sb.max = config.max
					sb.progress = value
					sbt.text = "" + value
					sb.visibility = View.VISIBLE
					sbt.visibility = View.VISIBLE
					sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
						override fun onStopTrackingTouch(seekBar: SeekBar) {
							config.set(seekBar.progress)
						}

						override fun onStartTrackingTouch(seekBar: SeekBar) {}
						override fun onProgressChanged(
							seekBar: SeekBar,
							progress: Int,
							fromUser: Boolean
						) {
							sbt.text = "" + progress
						}
					})
				}

				ValueType.BOOLEAN -> {
					val config = entries[position].value as Config<Boolean>
					val value = config.get()
					tb.isChecked = value
					tb.visibility = View.VISIBLE
					tb.setOnClickListener { config.set(tb.isChecked) }
				}

				ValueType.STRING -> {
					val config = entries[position].value as Config<String>
					et.visibility = View.VISIBLE
					et.setText(config.get())
					et.setOnEditorActionListener(OnEditorActionListener { arg0, arg1, enterKey ->
						if (enterKey != null) {
							config.set(tv.text.toString())
							return@OnEditorActionListener true
						}
						false
					})
				}

				ValueType.INT_CHOICE -> {
					val config = entries[position].value as Config<Int>
					cb.visibility = View.VISIBLE
					cb.text = config.get().toString()
					cb.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue.toString()
					}
				}

				ValueType.STRING_CHOICE -> {
					val config = entries[position].value as Config<String>
					cb.visibility = View.VISIBLE
					cb.text = config.get()
					cb.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue
					}
				}

				ValueType.ENUM_CHOICE -> {
					val config = entries[position].value as Config<Enum<*>>
					cb.visibility = View.VISIBLE
					cb.text = config.get().name
					cb.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue.toString()
					}
				}
			}
			return view
		}
	}

	fun build(rules: R) {
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

		} catch (e: Exception) {


		}
	}

	fun show() {
		val v = ListView(context)
		v.adapter = constructAdapter()
		context.newDialogBuilder()
			.setView(v)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.show()
	}
}
