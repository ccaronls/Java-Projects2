package cc.lib.android

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 1/26/23.
 */
open class LinearRecyclerView(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs, 0) {

	interface Listener {
		fun onChanged(recycler: LinearRecyclerView)
	}

	private val listeners = mutableListOf<Listener>()

	val isVertical: Boolean
	var centerPositionIndex: Int = -1

	init {
		var layoutOrientation = VERTICAL
		with(context.obtainStyledAttributes(attrs, R.styleable.LinearRecyclerView)) {
			layoutOrientation = getInt(R.styleable.LinearRecyclerView_layout_orientation, layoutOrientation)
			val topSpacing = getDimension(R.styleable.LinearRecyclerView_item_spacing_top, 0f).roundToInt()
			val bottomSpacing = getDimension(R.styleable.LinearRecyclerView_item_spacing_bottom, 0f).roundToInt()
			val leftSpacing = getDimension(R.styleable.LinearRecyclerView_item_spacing_left, 0f).roundToInt()
			val rightSpacing = getDimension(R.styleable.LinearRecyclerView_item_spacing_right, 0f).roundToInt()
			val omitEndpoints = getBoolean(R.styleable.LinearRecyclerView_item_spacing_omit_endpoints, false)
			if (topSpacing != 0 || bottomSpacing != 0 || leftSpacing != 0 || rightSpacing != 0) {
				addItemDecoration(object : ItemDecoration() {
					override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
						val position = parent.getChildAdapterPosition(view)
						val first = position == 0
						val last = position == state.itemCount - 1

						outRect.set(leftSpacing, topSpacing, rightSpacing, bottomSpacing)

						if (omitEndpoints) {
							when (layoutOrientation) {
								HORIZONTAL -> {
									if (first)
										outRect.left = 0
									if (last)
										outRect.right = 0
								}
								else -> {
									if (first)
										outRect.top = 0
									if (last)
										outRect.bottom = 0
								}
							}
						}
					}
				})
			}
			getResourceId(R.styleable.LinearRecyclerView_divider_drawable, 0).takeIf { it > 0 }?.let { id ->
				context.getDrawable(id)?.let { drawable ->
					addItemDecoration(object : DividerItemDecoration(context, layoutOrientation) {
						override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
							if (omitEndpoints) {
								val position = parent.getChildAdapterPosition(view)
								val last = position == state.itemCount - 1
								if (last) {
									outRect.setEmpty()
									return
								}
							}
							super.getItemOffsets(outRect, view, parent, state)
						}
					}.also {
						it.setDrawable(drawable)
					})
				}
			}
			recycle()
			isVertical = layoutOrientation == VERTICAL
		}

		layoutManager = object : LinearLayoutManager(context, attrs, 0, 0) {
			init {
				orientation = layoutOrientation
			}

			// Fixes Inconsistency crash: See google message board:
			//   https://issuetracker.google.com/issues/211952792?pli=1
			override fun supportsPredictiveItemAnimations(): Boolean = false
		}

	}

	fun firstVisibleIndex(): Int = (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()

	fun lastVisibleIndex(): Int = (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

	fun numVisible(): Int = (lastVisibleIndex() - firstVisibleIndex()).coerceAtLeast(0)

	private val observer = object : AdapterDataObserver() {
		override fun onChanged() {
			post {
				listeners.forEach {
					it.onChanged(this@LinearRecyclerView)
				}
			}
		}
	}

	override fun setAdapter(adapter: Adapter<*>?) {
		this.adapter?.unregisterAdapterDataObserver(observer)
		super.setAdapter(adapter)
		adapter?.registerAdapterDataObserver(observer)
		scrollToCenterPosition()
	}

	override fun onDetachedFromWindow() {
		adapter?.unregisterAdapterDataObserver(observer)
		super.onDetachedFromWindow()
		listeners.clear()
	}

	fun addListener(listener: Listener) {
		listeners.add(listener)
	}

	override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
		super.onLayout(changed, l, t, r, b)
		if (changed) {
			listeners.forEach {
				it.onChanged(this@LinearRecyclerView)
			}
			scrollToCenterPosition()
		}
	}

	fun size(): Int = adapter?.itemCount ?: 0


	/**.
	 * Scroll the visible items to previous page. The page size is determined by the number of visible elements.
	 */
	fun pagePrevious() {
		smoothScrollToPosition((firstVisibleIndex() - numVisible() - 1).coerceAtLeast(0))
	}

	/**
	 * Scroll the visible items to next page. The page size is determined by the number of visible elements
	 */
	fun pageNext() {
		smoothScrollToPosition((lastVisibleIndex() + numVisible() + 1).coerceAtMost(size() - 1))
	}

	fun scrollToCenterPosition() {
		val num = numVisible()
		if (centerPositionIndex >= 0 && num > 0) {
			val first = firstVisibleIndex()
			val last = lastVisibleIndex()
			val mid = (first + last + 1) / 2
			if (centerPositionIndex < mid)
				smoothScrollToPosition((centerPositionIndex - num / 2 - 1).coerceIn(0 until size()))
			else if (centerPositionIndex > mid)
				smoothScrollToPosition((centerPositionIndex + num / 2).coerceIn(0 until size()))
		}
	}

	companion object {
		@JvmStatic
		@BindingAdapter("linearRecyclerViewListener")
		fun addListener(recyclerView: LinearRecyclerView, listener: Listener) {
			recyclerView.addListener(listener)
		}

		@JvmStatic
		@BindingAdapter("centerPosition")
		fun setCenterPosition(recyclerView: LinearRecyclerView, position: Int) {
			recyclerView.centerPositionIndex = position
			recyclerView.scrollToCenterPosition()
		}

	}
}

