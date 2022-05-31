package cc.android.game.probot

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cc.lib.android.DragAndDropAdapter
import cc.lib.probot.Command
import cc.lib.probot.CommandType
import cc.lib.probot.Probot

class ProbotAdapter(val probot: Probot, val listView: ListView) : DragAndDropAdapter<Command>(listView, probot), Runnable, View.OnClickListener {

	var programLineNum = 0
		private set
	var failedLineNum = -1
		private set

	fun setProgramLineNum(lineNum: Int) {
		programLineNum = lineNum
		failedLineNum = -1
		postRefresh()
	}

	fun markFailed() {
		failedLineNum = programLineNum
		programLineNum = -1
		postRefresh()
	}

	fun postRefresh() {
		listView.post(this)
	}

	override fun onLineNumTextViewUpdated(position: Int, tvLineNum: TextView) {
		if (position == programLineNum) {
			tvLineNum.setBackgroundColor(Color.GREEN)
		} else if (position == failedLineNum) {
			tvLineNum.setBackgroundColor(Color.RED)
		} else {
			tvLineNum.setBackgroundColor(Color.TRANSPARENT)
		}
	}

	override fun onDeleteButtonUpdated(position: Int, v: View) {
		val cmd = probot[position]
		if (cmd.type == CommandType.LoopEnd) {
			v.visibility = View.GONE
		} else {
			v.visibility = View.VISIBLE
			v.tag = position
			v.setOnClickListener(this)
		}
	}

	override fun populateItem(data: Command, container: ViewGroup, position: Int) {
		val convertView: ViewGroup = if (container.childCount > 0) container.getChildAt(0) as ViewGroup else (View.inflate(container.context, R.layout.item_command, null) as ViewGroup).also {
			container.addView(it)
		}

		val cmd = data
		val isInLoop = cmd.nesting > 0
		val isLoop = cmd.type == CommandType.LoopEnd || cmd.type == CommandType.LoopStart
		val tvLoopCount = convertView.findViewById<View>(R.id.tvLoopCount) as TextView
		val v = convertView.findViewById<View>(R.id.ibLoop)
		if (isLoop || isInLoop) {
			v.visibility = ListView.GONE
			tvLoopCount.visibility = ListView.GONE
		} else {
			if (probot.level.numLoops < 0) {
				// infinite loops
				tvLoopCount.visibility = ListView.GONE
			} else {
				tvLoopCount.visibility = ListView.VISIBLE
				tvLoopCount.text = probot.level.numLoops.toString()
				v.isEnabled = probot.level.numLoops > 0
			}
			v.tag = position
			v.visibility = ListView.VISIBLE
			v.setOnClickListener(this)
		}
		val iv = convertView.findViewById<View>(R.id.imageView) as ImageView
		if (isLoop) {
			iv.visibility = ListView.INVISIBLE
		} else {
			iv.visibility = ListView.VISIBLE
			when (cmd.type) {
				CommandType.Advance -> iv.setImageResource(R.drawable.arrow_forward)
				CommandType.TurnRight -> iv.setImageResource(R.drawable.arrow_right)
				CommandType.TurnLeft -> iv.setImageResource(R.drawable.arrow_left)
				CommandType.Jump -> iv.setImageResource(R.drawable.arrow_jump)
				CommandType.UTurn -> iv.setImageResource(R.drawable.uturn)
			}
		}

		val bPlus = convertView.findViewById<View>(R.id.ibPlus)
		val bMinus = convertView.findViewById<View>(R.id.ibMinus)
		val tvCount = convertView.findViewById<View>(R.id.tvCount) as TextView
		if (cmd.type == CommandType.LoopStart) {
			bPlus.visibility = ListView.VISIBLE
			bPlus.setOnClickListener(this)
			bPlus.tag = cmd
			bMinus.visibility = ListView.VISIBLE
			bMinus.setOnClickListener(this)
			bMinus.tag = cmd
			tvCount.visibility = ListView.VISIBLE
			tvCount.text = cmd.count.toString()
		} else {
			bPlus.visibility = ListView.GONE
			bMinus.visibility = ListView.GONE
			tvCount.visibility = ListView.GONE
		}
	}

	override fun getItemName(item: Command): String {
		return item.type.name
	}

	override fun run() {
		notifyDataSetChanged()
		if (programLineNum >= 0) {
			listView.setSelection(programLineNum)
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
				val cmd = probot.removeAt(position)
				if (cmd.type == CommandType.LoopStart) {
					while (probot[position].type != CommandType.LoopEnd) {
						//probot.remove(position);
						position++
					}
					probot.removeAt(position)
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
		notifyDataSetChanged()
	}
}