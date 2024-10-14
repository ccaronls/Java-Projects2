package cc.lib.android

import android.content.ClipData
import android.os.SystemClock
import android.util.Log
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.DragShadowBuilder
import android.view.View.OnDragListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import java.util.Arrays

/**
 * Adapter to support drag and drop of list items within the list.
 * Also supports ability to drop external items into the list as droppable positions.
 *
 * Example:
 *
 * Say we have a view with ListView and some buttons like this:
 *
 * +-----------------------------+
 * |  +-----------------------+  |
 * |  |                       |  |
 * |  |       ListView        |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  |                       |  |
 * |  +-----------------------+  |
 * |                             |
 * |  +-------+-------+-------+  |
 * |  |   A   |   B   |   C   |  |
 * |  +-------+-------+-------+  |
 * +-----------------------------+
 *
 * enum MyEnum {
 * A,B,C
 * }
 *
 * ListView listView;
 * Button buttonA,buttonB,buttonC;
 *
 * DragAndDropAdapter<MyEnum> adapter = new DrapAndDropAdapter<MyEnum>(listView) {
 * void populateItem(MyEnum e, ViewGroup container) {
 * TextView tv = new TextView(context);
 * tv.setText(e.name());
 * container.removeAllViews();
 * container.add(tv);
 * }
 *
 * String getItemName(MyEnum e) {
 * return e.name();
 * }
 * });
 *
 * adapter.addDraggable(buttonA, MyEnum.A);
 * adapter.addDraggable(buttonB, MyEnum.B);
 * adapter.addDraggable(buttonC, MyEnum.C);
 *
 * list.setAdapter(adapter);
 *
 * List<MyEnum> list = adapter.getList();
 *
 * @param <T>
</T></MyEnum></MyEnum></MyEnum> */
abstract class DragAndDropAdapter<T> @JvmOverloads constructor(
	private val listView: ListView, // TODO: Use dataset observer instead
	private val list: MutableList<T> = ArrayList()
) : BaseAdapter(), OnDragListener, View.OnLongClickListener, OnTouchListener {
	private var dragInsertPos = -1
	private var dragMinInsertPos = 0
	private var dragMaxInsertPos = 0
	private var dropped = false

	/**
	 * Populate a list item. Add views to the container. A non-empty container
	 * can have its views reused as appropriate.
	 *
	 * @param data
	 * @param container
	 */
	protected abstract fun populateItem(data: T, container: ViewGroup?, position: Int)

	/**
	 *
	 *
	 * @param item
	 * @return
	 */
	protected abstract fun getItemName(item: T): String

	constructor(listView: ListView, vararg items: T) : this(listView, ArrayList<T>(Arrays.asList<T>(*items)))

	override fun notifyDataSetChanged() {
		super.notifyDataSetChanged()
	}

	fun getList(): List<T> {
		return list
	}

	override fun getCount(): Int {
		val s = list.size
		return list.size
	}

	override fun getItem(position: Int): T {
		return list[position]
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
		var convertView = convertView
		if (convertView == null) {
			convertView = View.inflate(listView.context, R.layout.draganddrop_list_item, null)
		}
		convertView.setOnDragListener(this)
		convertView.setOnLongClickListener(this)
		convertView.tag = position
		val divTop = convertView.findViewById<View>(R.id.divider_top)
		val divBot = convertView.findViewById<View>(R.id.divider_bottom)
		divTop.visibility = View.GONE
		divBot.visibility = View.GONE
		if (dragInsertPos >= 0) {
			if (dragInsertPos == position) {
				divTop.visibility = View.VISIBLE
			} else if (dragInsertPos == position + 1) {
				divBot.visibility = View.VISIBLE
			}
		}
		val data = list[position]
		val tvLineNum = convertView.findViewById<TextView>(R.id.tvLineNum)
		tvLineNum.text = (position + 1).toString()
		onLineNumTextViewUpdated(position, tvLineNum)
		val v = convertView.findViewById<View>(R.id.ibDelete)
		v.visibility = View.VISIBLE
		v.tag = position
		v.setOnClickListener { v1: View ->
			val position1 = v1.tag as Int
			val data1 = list.removeAt(position1)
			notifyDataSetChanged()
		}
		onDeleteButtonUpdated(position, v)
		val container = convertView.findViewById<ViewGroup>(R.id.container)
		//container.removeAllViews();
		populateItem(data, container, position)
		return convertView
	}

	fun onLineNumTextViewUpdated(position: Int, textView: TextView?) {}
	fun onDeleteButtonUpdated(position: Int, button: View?) {}
	override fun onDrag(v: View, event: DragEvent): Boolean {
		val dragInsertPosIn = dragInsertPos
		val state = event.localState as Array<Any> ?: return false
		val data = state[0] as T
		val originatingLine = state[1] as Int
		val position = v.tag as Int
		val obj = "RowItem[$position]"
		when (event.action) {
			DragEvent.ACTION_DROP -> {
				Log.d(TAG, "dragInsertPos=$dragInsertPos")
				if (dragInsertPos < 0 || list.size == 0) {
					list.add(data)
				} else {
					list.add(dragInsertPos, data)
				}
				notifyDataSetChanged()
				listView.setSelection(dragInsertPos)
				dragInsertPos = -1
				dropped = true
			}

			DragEvent.ACTION_DRAG_EXITED -> {}
			DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DRAG_ENTERED -> {
				val mp = (v.height / 2).toFloat()
				val mpd = event.y // + dragging.getHeight();
				val first = listView.firstVisiblePosition
				val last = listView.lastVisiblePosition

				//if (position == first) {
				//    Log.d("PLV", "A");
				//    dragInsertPos = position;
				//} else if (position == last) {
				//    Log.d("PLV", "B");
				//    dragInsertPos = position+1;
				//} else
				dragInsertPos = if (mp > mpd) {
					Log.d(TAG, "C mp=$mp mpd=$mpd")
					position
				} else {
					Log.d(TAG, "D")
					position + 1
				}
				dragInsertPos = dragInsertPos.coerceIn(dragMinInsertPos, dragMaxInsertPos)
				notifyDataSetChanged()
				//listView.setSelection(dragInsertPos);
				if (dragInsertPosIn != dragInsertPos) {
					val scrollDist = v.height
					val midpt = (last + first + 1) / 2
					val maxSp = 500f
					val speed = Math.round(maxSp - Math.abs(maxSp * 2 * (dragInsertPos - midpt) / (last - first)))
					//0.5f * (dragInsertPos - first - midpt));
					Log.d(TAG, "speed=$speed")
					if (dragInsertPos < midpt) {
						listView.smoothScrollBy(-scrollDist, speed)
					} else {
						listView.smoothScrollBy(scrollDist, speed)
					}
				}
				if (dragInsertPos == 0) listView.smoothScrollToPositionFromTop(0, 0, 0)
				if (dragInsertPos <= first + 1) {
					//listView.smoothScrollBy(-scrollDist, 500);
				} else if (dragInsertPos >= last - 1) {
					//  smoothScrollBy(scrollDist, 500);
				}
			}

			DragEvent.ACTION_DRAG_STARTED -> dropped = false
			DragEvent.ACTION_DRAG_ENDED -> if (!dropped) {
				// put the item back in
				if (originatingLine >= 0) list.add(originatingLine, data) else list.add(data)
				notifyDataSetChanged()
				dropped = true
			}
		}
		//if (dragInsertPos != dragInsertPosIn)
		run {
			Log.d(
				TAG,
				"v=" + obj + " action=" + getActionStr(event.action) + " pos=" + dragInsertPos + " origLine: " + originatingLine
			)
		}
		return true
	}

	override fun onLongClick(v: View): Boolean {
		if (list.size > 1) {
			listView.transcriptMode = ListView.TRANSCRIPT_MODE_DISABLED
			val position = v.tag as Int
			val data = list.removeAt(position)
			startDrag(v, data, position)
			notifyDataSetChanged()
		}
		return true
	}

	private fun startDrag(v: View, data: T, originatingLine: Int = -1) {
		val name = getItemName(data)
		v.startDrag(
			ClipData.newPlainText(name, name), DragShadowBuilder(v), arrayOf(
				data, originatingLine
			), 0
		)
		dragMinInsertPos = -1
		dragMaxInsertPos = list.size
	}

	private var touchView: View? = null

	/**
	 *
	 * @param listView
	 */
	init {
		listView.adapter = this
	}

	override fun onTouch(v: View, event: MotionEvent): Boolean {
		val type = v.tag as T
		when (event.action) {
			MotionEvent.ACTION_UP -> if (touchView === v) {
				listView.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
				list.add(type)
				listView.setSelection(list.size - 1)
				notifyDataSetChanged()
			}

			MotionEvent.ACTION_MOVE -> {
				val delta = SystemClock.uptimeMillis() - event.downTime
				//Log.d(TAG, "delta -= " + delta);
				if (list.size > 0 && delta > 500) {
					startDrag(v, type)
					touchView = null
				}
			}

			MotionEvent.ACTION_DOWN -> {
				listView.transcriptMode = ListView.TRANSCRIPT_MODE_DISABLED
				touchView = v
			}
		}
		return true
	}

	fun addDraggable(v: View, type: T) {
		v.tag = type
		v.setOnTouchListener(this)
	}

	companion object {
		val TAG = DragAndDropAdapter::class.java.simpleName
		private fun getActionStr(action: Int): String {
			when (action) {
				DragEvent.ACTION_DRAG_ENDED -> return "Ended"
				DragEvent.ACTION_DRAG_STARTED -> return "Started"
				DragEvent.ACTION_DRAG_ENTERED -> return "Entered"
				DragEvent.ACTION_DRAG_EXITED -> return "Exited"
				DragEvent.ACTION_DRAG_LOCATION -> return "Location"
				DragEvent.ACTION_DROP -> return "Drop"
			}
			return "???"
		}
	}
}
