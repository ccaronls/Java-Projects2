package cc.lib.android

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.ToggleButton
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import cc.lib.android.databinding.TabbedViewPagerBinding
import cc.lib.annotation.RuleMeta
import java.lang.reflect.Field

abstract class ConfigDialogBuilder(val context: CCActivityBase) : OnItemSelectedListener {
	enum class ValueType {
		BOOLEAN,
		INTEGER,
		STRING,
		INT_CHOICE,
		STRING_CHOICE,
		ENUM_CHOICE
	}

	abstract class Config<T>(val name: String, val type: ValueType) {
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
	}

	private lateinit var rules: Any
	private var editable = true

	inner class ConfigBool(name: String, val field: Field) : Config<Boolean>(name, ValueType.BOOLEAN) {
		override fun get(): Boolean = field.getBoolean(rules)

		override fun set(value: Boolean) = field.set(rules, value)
	}

	inner class ConfigInt(name: String, val field: Field, override val min: Int, override val max: Int) :
		Config<Int>(name, ValueType.INTEGER) {
		override fun get(): Int = field.getInt(rules)

		override fun set(value: Int) = field.set(rules, value.coerceIn(min, max))
	}

	inner class ConfigString(name: String, val field: Field) : Config<String>(name, ValueType.STRING) {
		override fun get(): String = field.get(rules).toString()

		override fun set(value: String) = field.set(rules, value)
	}

	inner class ConfigChoice<T>(name: String, val field: Field, choices: Array<T>) : Config<T>(name, ValueType.ENUM_CHOICE) {
		override fun get(): T = field.get(rules) as T

		override fun set(value: T) = field.set(rules, value)

		override val choices: Array<T> = choices
	}

	inner class ConfigListAdapter(val entries: List<Config<*>>) : BaseAdapter() {

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
			return editable
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			var view = convertView ?: View.inflate(context, R.layout.configelem, null)
			val tv = view.findViewById<View>(R.id.textView1) as TextView
			val sb = view.findViewById<View>(R.id.seekBar1) as SeekBar
			val et = view.findViewById<View>(R.id.editText1) as EditText
			val sbt = view.findViewById<View>(R.id.textView2) as TextView
			val tb = view.findViewById<View>(R.id.toggleButton1) as ToggleButton
			val cb = view.findViewById<View>(R.id.choiceButton1) as Button
			tv.text = entries[position].name
			sb.visibility = View.GONE
			et.visibility = View.GONE
			sbt.visibility = View.GONE
			tb.visibility = View.GONE
			when (entries[position].type) {
				ValueType.INTEGER -> {
					val config = entries[position] as Config<Int>
					val value = config.get()
					//Main.HorizontalSlider slider = new HorizontalSlider(Main.this);
					sb.min = config.min
					sb.max = config.max
					sb.progress = value
					sbt.text = "$value"
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
							sbt.text = "$progress"
						}
					})
					view.setOnClickListener {
						val seekBar = SeekBar(context).also { seekBar ->
							seekBar.min = config.min
							seekBar.max = config.max
							seekBar.progress = config.get()
						}
						val dialog = context
							.newDialogBuilder()
							.setTitle("${config.name} : ${seekBar.progress}")
							.setView(seekBar)
							.setNegativeButton(R.string.popup_button_cancel, null)
							.setPositiveButton(R.string.popup_button_apply) { _, _ ->
								config.set(seekBar.progress)
								notifyDataSetChanged()
							}.show()
						seekBar.setOnKeyListener { _, keyCode, event ->
							val down = event.action == KeyEvent.ACTION_DOWN
							when (keyCode) {
								KeyEvent.KEYCODE_DPAD_RIGHT -> if (down) seekBar.progress = seekBar.progress + 1
								KeyEvent.KEYCODE_DPAD_LEFT -> if (down) seekBar.progress = seekBar.progress - 1
								else -> return@setOnKeyListener false
							}
							dialog.setTitle("${config.name} : ${seekBar.progress}")
							true
						}
					}
				}

				ValueType.BOOLEAN -> {
					val config = entries[position] as Config<Boolean>
					val value = config.get()
					tb.isChecked = value
					tb.visibility = View.VISIBLE
					tb.setOnClickListener { config.set(tb.isChecked) }
					view.setOnClickListener {
						tb.performClick()
					}
				}

				ValueType.STRING -> {
					val config = entries[position] as Config<String>
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
					val config = entries[position] as Config<Int>
					cb.visibility = View.VISIBLE
					cb.text = config.get().toString()
					cb.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue.toString()
					}
				}

				ValueType.STRING_CHOICE -> {
					val config = entries[position] as Config<String>
					cb.visibility = View.VISIBLE
					cb.text = config.get()
					cb.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue
					}
				}

				ValueType.ENUM_CHOICE -> {
					val config = entries[position] as Config<Enum<*>>
					cb.visibility = View.VISIBLE
					cb.text = config.get().name
					view.setOnClickListener {
						val newValue = increment(config.get(), *config.choices)
						config.set(newValue)
						cb.text = newValue.toString()
					}
				}
			}
			return view
		}
	}

	private fun fieldToConfig(field: Field, meta: RuleMeta): Config<*> {
		return when (field.type) {
			Boolean::class.javaPrimitiveType -> {
				ConfigBool(meta.description, field)
			}

			Int::class.javaPrimitiveType -> {
				ConfigInt(meta.description, field, meta.minValue, meta.maxValue)
			}

			String::class.javaPrimitiveType -> {
				ConfigString(meta.description, field)
			}

			else -> {
				if (field.type.isEnum) {
					val choices = field.type.enumConstants as Array<Enum<*>>
					ConfigChoice(meta.description, field, choices)
				} else throw UnsupportedOperationException("${field.type}")
			}
		}
	}

	private fun build(rules: Any): List<Pair<String, List<Config<*>>>> {
		val fieldsMap = mutableMapOf<String, MutableList<Config<*>>>()

		val metaMap: Map<Field, RuleMeta> = rules.javaClass.declaredFields.map { field ->
			field.annotations.firstOrNull { it.annotationClass == RuleMeta::class }?.let {
				Pair(field, it as RuleMeta).also {
					field.isAccessible = true
				}
			}
		}.filterNotNull().sortedBy {
			it.second.order
		}.toMap()

		metaMap.forEach { (field, meta) ->
			fieldsMap.getOrPut(meta.variation) {
				mutableListOf()
			}.add(fieldToConfig(field, meta))
		}

		return fieldsMap.toList()
	}

	fun show(rules: Any, editable: Boolean, title: String? = null) {

		this.rules = rules
		this.editable = editable
		val map = build(rules)
		val v = if (map.size > 1) {

			val binding = TabbedViewPagerBinding.inflate(context.layoutInflater)
			binding.viewPager.adapter = object : PagerAdapter() {
				override fun getCount(): Int = map.size

				override fun isViewFromObject(view: View, obj: Any) = view === obj

				override fun instantiateItem(container: ViewGroup, position: Int): Any {
					return ListView(context).also {
						it.adapter = ConfigListAdapter(map[position].second)
						it.layoutParams = ViewPager.LayoutParams()
						it.setOnItemClickListener { _, view, _, _ ->
							view.performClick()
						}
						it.onItemSelectedListener = this@ConfigDialogBuilder
						it.isClickable = true
						container.addView(it)
					}
				}

				override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
					container.removeView(obj as View)
				}

				override fun getPageTitle(position: Int) = map[position].first
			}
			binding.tabLayout.setupWithViewPager(binding.viewPager)
			binding.root

		} else if (map.isNotEmpty()) {
			// single list view
			ListView(context).also {
				it.adapter = ConfigListAdapter(map[0].second)
			}
		} else {
			return
		}

		context.newDialogBuilder()
			.setView(v)
			.setNegativeButton(R.string.popup_button_cancel, null)
			.setPositiveButton(R.string.popup_button_apply) { _, _ ->
				onApplyRules()
			}.also {
				if (title != null) {
					it.setTitle(title)
				}
			}
			.show()
	}

	override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
		view.requestFocus()
	}

	override fun onNothingSelected(parent: AdapterView<*>?) {
		TODO("Not yet implemented")
	}

	abstract fun onApplyRules()
}
