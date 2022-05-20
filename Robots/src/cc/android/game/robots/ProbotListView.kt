package cc.android.game.robots

import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cc.lib.game.Utils
import cc.lib.probot.Command
import cc.lib.probot.CommandType
import cc.lib.probot.Probot

/**
 * Created by chriscaron on 12/12/17.
 */
class ProbotListView : ListView, OnDragListener, View.OnClickListener, OnLongClickListener, Runnable {
	
	lateinit var adapter: BaseAdapter
	private var programLineNum = -1
	private var failedLineNum = -1
	private lateinit var probot: Probot
	
	private fun init(c: Context, a: AttributeSet?) {
		setOnDragListener(this)
		adapter = object : BaseAdapter() {
			override fun getCount(): Int {
				return probot.size()
			}

			override fun getItem(position: Int): Any {
				return probot[position]
			}

			override fun getItemId(position: Int): Long {
				return position.toLong()
			}

			override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
				var convertView = convertView?:inflate(context, R.layout.item_command, null)
				convertView.setOnDragListener(this@ProbotListView)
				convertView.setOnLongClickListener(this@ProbotListView)
				convertView.setOnClickListener(this@ProbotListView)
				convertView.tag = position
				val divTop = convertView.findViewById<View>(R.id.divider_top)
				val divBot = convertView.findViewById<View>(R.id.divider_bottom)
				divTop.visibility = GONE
				divBot.visibility = GONE
				if (dragInsertPos >= 0) {
					if (dragInsertPos == position) {
						divTop.visibility = VISIBLE
					} else if (dragInsertPos == position + 1) {
						divBot.visibility = VISIBLE
					}
				}
				val cmd = probot[position]
				val isInLoop = cmd.nesting > 0
				val isLoop = cmd.type == CommandType.LoopEnd || cmd.type == CommandType.LoopStart
				val tvLineNum = convertView.findViewById<View>(R.id.tvLineNum) as TextView
				tvLineNum.text = (position + 1).toString()
				if (position == programLineNum) {
					tvLineNum.setBackgroundColor(Color.GREEN)
				} else if (position == failedLineNum) {
					tvLineNum.setBackgroundColor(Color.RED)
				} else {
					tvLineNum.setBackgroundColor(Color.TRANSPARENT)
				}
				//tvLineNum.setVisibility(View.GONE);
				var v = convertView.findViewById<View>(R.id.ibDelete)
				if (cmd.type == CommandType.LoopEnd) {
					v.visibility = GONE
				} else {
					v.visibility = VISIBLE
					v.tag = position
					v.setOnClickListener(this@ProbotListView)
				}
				val tvLoopCount = convertView.findViewById<View>(R.id.tvLoopCount) as TextView
				v = convertView.findViewById(R.id.ibLoop)
				if (isLoop || isInLoop) {
					v.visibility = GONE
					tvLoopCount.visibility = GONE
				} else {
					if (probot.level.numLoops < 0) {
						// infinite loops
						tvLoopCount.visibility = GONE
					} else {
						tvLoopCount.visibility = VISIBLE
						tvLoopCount.text = probot.level.numLoops.toString()
						v.isEnabled = probot.level.numLoops > 0
					}
					v.tag = position
					v.visibility = VISIBLE
					v.setOnClickListener(this@ProbotListView)
				}
				val iv = convertView.findViewById<View>(R.id.imageView) as ImageView
				if (isLoop) {
					iv.visibility = INVISIBLE
				} else {
					iv.visibility = VISIBLE
					when (cmd.type) {
						CommandType.Advance -> iv.setImageResource(R.drawable.arrow_forward)
						CommandType.TurnRight -> iv.setImageResource(R.drawable.arrow_right)
						CommandType.TurnLeft -> iv.setImageResource(R.drawable.arrow_left)
						CommandType.Jump -> iv.setImageResource(R.drawable.arrow_jump)
						CommandType.UTurn -> iv.setImageResource(R.drawable.uturn)
					}
				}

				//iv = (ImageView) convertView.findViewById(R.id.ivPlay);
				//iv.setVisibility(position == programLineNum ? View.VISIBLE : View.INVISIBLE);
				val bPlus = convertView.findViewById<View>(R.id.ibPlus)
				val bMinus = convertView.findViewById<View>(R.id.ibMinus)
				val tvCount = convertView.findViewById<View>(R.id.tvCount) as TextView
				if (cmd.type == CommandType.LoopStart) {
					bPlus.visibility = VISIBLE
					bPlus.setOnClickListener(this@ProbotListView)
					bPlus.tag = cmd
					bMinus.visibility = VISIBLE
					bMinus.setOnClickListener(this@ProbotListView)
					bMinus.tag = cmd
					tvCount.visibility = VISIBLE
					tvCount.text = cmd.count.toString()
				} else {
					bPlus.visibility = GONE
					bMinus.visibility = GONE
					tvCount.visibility = GONE
				}
				return convertView
			}

			override fun notifyDataSetChanged() {
				super.notifyDataSetChanged()
				(context as ProbotActivity).refresh()
			}
		}
	}

	fun setProbot(p: Probot) {
		probot = p
		setAdapter(adapter)
	}

	constructor(context: Context) : super(context) {
		init(context, null)
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		init(context, attrs)
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		init(context, attrs)
	}

	var dragInsertPos = -1
	var dragMinInsertPos = 0
	var dragMaxInsertPos = 0
	fun getActionStr(action: Int): String {
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

	private var dropped = false
	override fun onDrag(v: View, event: DragEvent): Boolean {
		val dragInsertPosIn = dragInsertPos
		var obj: String? = null
		val state = event.localState as Array<Any>
			?: return false
		val cmd = state[0] as Command
		val originatingLine = state[1] as Int
		if (v is ProbotListView) {
			obj = "ListView"
			when (event.action) {
				DragEvent.ACTION_DROP -> {
					if (cmd != null) {
						if (dragInsertPos < 0) {
							probot.add(cmd)
						} else {
							probot.add(dragInsertPos, cmd)
						}
						adapter.notifyDataSetChanged()
						setSelection(dragInsertPos)
						dragInsertPos = -1
					}
					dropped = true
				}
				DragEvent.ACTION_DRAG_ENTERED -> {
					if (probot.size() > 0) {
						dragInsertPos = Utils.clamp(probot.size(), dragMinInsertPos, dragMaxInsertPos)
						adapter.notifyDataSetChanged()
					}
				}
				DragEvent.ACTION_DRAG_EXITED -> {
					if (dragMinInsertPos < 0) {
						dragInsertPos = -1
					} else if (originatingLine >= 0) {
						dragInsertPos = originatingLine
					}
					adapter.notifyDataSetChanged()
				}
				DragEvent.ACTION_DRAG_STARTED -> dropped = false
				DragEvent.ACTION_DRAG_ENDED -> {
					dragInsertPos = -1
					if (!dropped && originatingLine >= 0) {
						probot.add(originatingLine, cmd)
						adapter.notifyDataSetChanged()
					}
				}
			}
		} else {
			val position = v.tag as Int
			obj = "RowItem[$position]"
			when (event.action) {
				DragEvent.ACTION_DROP -> {
					if (dragInsertPos < 0) {
						probot.add(cmd)
					} else {
						probot.add(dragInsertPos, cmd)
					}
					adapter.notifyDataSetChanged()
					setSelection(dragInsertPos)
					dragInsertPos = -1
					dropped = true
				}
				DragEvent.ACTION_DRAG_EXITED -> {
				}
				DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DRAG_ENTERED -> {
					val mp = (v.height / 2).toFloat()
					val mpd = event.y // + dragging.getHeight();
					val first = firstVisiblePosition
					val last = lastVisiblePosition

					//if (position == first) {
					//    Log.d("PLV", "A");
					//    dragInsertPos = position;
					//} else if (position == last) {
					//    Log.d("PLV", "B");
					//    dragInsertPos = position+1;
					//} else
					dragInsertPos = if (mp > mpd) {
						Log.d("PLV", "C mp=$mp mpd=$mpd")
						position
					} else {
						Log.d("PLV", "D")
						position + 1
					}
					dragInsertPos = Utils.clamp(dragInsertPos, dragMinInsertPos, dragMaxInsertPos)
					adapter.notifyDataSetChanged()
					//              setSelection(dragInsertPos);
					if (dragInsertPosIn != dragInsertPos) {
						val scrollDist = v.height
						val midpt = (last + first + 1) / 2
						val maxSp = 500f
						val speed = Math.round(maxSp - Math.abs(maxSp * 2 * (dragInsertPos - midpt) / (last - first)))
						//0.5f * (dragInsertPos - first - midpt));
						Log.d("PLV", "speed=$speed")
						if (dragInsertPos < midpt) {
							smoothScrollBy(-scrollDist, speed)
						} else {
							smoothScrollBy(scrollDist, speed)
						}
					}
					if (dragInsertPos == 0) smoothScrollToPositionFromTop(0, 0, 0)
					if (dragInsertPos <= first + 1) {
						//    smoothScrollBy(-scrollDist, 500);
					} else if (dragInsertPos >= last - 1) {
						//  smoothScrollBy(scrollDist, 500);
					}
				}
				DragEvent.ACTION_DRAG_STARTED -> {
				}
			}
		}
		//if (dragInsertPos != dragInsertPosIn)
		run { Log.d("List", "v=" + obj + " action=" + getActionStr(event.action) + " pos=" + dragInsertPos + " origLine: " + originatingLine) }
		return true
	}

	fun setProgramLineNum(lineNum: Int) {
		programLineNum = lineNum
		failedLineNum = -1
		post(this)
	}

	fun markFailed() {
		failedLineNum = programLineNum
		programLineNum = -1
		post(this)
	}

	fun notifyDataSetChanged() {
		post(this)
	}

	override fun run() {
		adapter.notifyDataSetChanged()
		if (programLineNum >= 0) {
			setSelection(programLineNum)
		}
	}

	override fun onClick(v: View) {
		if (programLineNum >= 0) return
		when (v.id) {
			R.id.ibLoop -> {
				val position = v.tag as Int
				probot.add(position, Command(CommandType.LoopStart, 1))
				probot.add(position + 2, Command(CommandType.LoopEnd, 0))
			}
			R.id.ibDelete -> {
				var position = v.tag as Int
				val cmd = probot.remove(position)
				if (cmd.type == CommandType.LoopStart) {
					while (probot[position].type != CommandType.LoopEnd) {
						//probot.remove(position);
						position++
					}
					probot.remove(position)
				}
			}
			R.id.ibPlus -> {
				val cmd = v.tag as Command
				if (cmd.count < 5) cmd.count++
			}
			R.id.ibMinus -> {
				val cmd = v.tag as Command
				if (cmd.count > 1) cmd.count--
			}
		}
		adapter.notifyDataSetChanged()
	}

	override fun onLongClick(v: View): Boolean {
		if (probot.size() > 1) {
			val position = v.tag as Int
			val cmd = probot.remove(position)
			startDrag(v, cmd, position)
			adapter.notifyDataSetChanged()
		}
		return true
	}

	fun startDrag(v: View, cmd: Command) {
		startDrag(v, cmd, -1)
	}

	private fun startDrag(v: View, cmd: Command, originatingLine: Int) {
		v.startDrag(ClipData.newPlainText(cmd.type.name, cmd.type.name), DragShadowBuilder(v), arrayOf(
			cmd, originatingLine
		), 0)
		dragMinInsertPos = -1
		dragMaxInsertPos = probot.size()
		if (cmd.type == CommandType.LoopStart) {
			val position = v.tag as Int
			var pos = position
			dragMinInsertPos = pos
			dragMaxInsertPos = dragMinInsertPos
			while (pos > 0) {
				if (probot[pos - 1].type == CommandType.LoopEnd) {
					break
				}
				dragMinInsertPos--
				pos--
			}
			pos = position
			while (pos < probot.size() - 1) {
				if (probot[pos + 1].type == CommandType.LoopEnd) {
					break
				}
				dragMaxInsertPos++
				pos++
			}
			Log.d("ProbotListView", "dragMin=$dragMinInsertPos dragMax=$dragMaxInsertPos pos=$position")
		} else if (cmd.type == CommandType.LoopEnd) {
			val position = v.tag as Int
			// scan forward and back to make sure
			var pos = position
			dragMinInsertPos = pos
			dragMaxInsertPos = dragMinInsertPos
			while (pos > 1) {
				if (probot[pos - 2].type == CommandType.LoopStart) {
					break
				}
				dragMinInsertPos--
				pos--
			}
			pos = position
			while (pos < probot.size()) {
				if (probot[pos].type == CommandType.LoopStart) {
					break
				}
				dragMaxInsertPos++
				pos++
			}
			Log.d("ProbotListView", "insertPos=$dragInsertPos dragMin=$dragMinInsertPos dragMax=$dragMaxInsertPos pos=$position")
		}
	}
}