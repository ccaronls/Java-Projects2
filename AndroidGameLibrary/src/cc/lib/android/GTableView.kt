package cc.lib.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import cc.lib.game.GColor
import cc.lib.game.GDimension
import cc.lib.utils.Table

/**
 * Created by Chris Caron on 12/3/23.
 */
class GTableView(context: Context, attrs: AttributeSet? = null) :
	AppCompatTextView(context, attrs) {

	private var table: Table? = null
	private var needResize = true

	private var G: DroidGraphics? = null

	init {
		with(context.obtainStyledAttributes(attrs, R.styleable.GTableView)) {
			recycle()
		}

		if (isInEditMode) {
			table = Table()
				.addColumn("Header 1", "I am cell 1", "I am cell 2")
				.addColumn("Header 2", "I am cell 3", "I am cell 4")
		}
	}

	override fun onDraw(canvas: Canvas?) {
		val g = G ?: object : DroidGraphics(context, canvas, width, height, paint, paint) {
			override fun getBackgroundColor(): GColor = when (background) {
				is ColorDrawable -> GColor.fromARGB((background as ColorDrawable).color)
				else -> GColor.TRANSPARENT
			}

		}.also {
			G = it
		}

		if (needResize) {
			table?.let {
				it.reMeasure(g)
				it.fit(g, GDimension(width.toFloat(), height.toFloat()))
				needResize = false
			}
		}
		table?.draw(g)
	}

	fun setTable(table: Table?) {
		this.table = table
		needResize = true
		postInvalidate()
	}

	override fun setText(text: CharSequence?, type: BufferType?) {
		super.setText(text, type)
		table = Table().addRow(text.toString())
	}

	override fun setTextColor(color: Int) {
		super.setTextColor(color)
		paint.color = color
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		needResize = true
		G = null
	}
}

@BindingAdapter("table")
fun GTableView.setTableBA(newTable: Table?) {
	setTable(newTable)
	postInvalidate()
}