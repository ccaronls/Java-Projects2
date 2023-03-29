package cc.android.thomas

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cc.android.thomas.databinding.WorkoutSummaryListItemBinding
import cc.lib.utils.toHMS
import cc.lib.utils.weakReference
import java.util.*

data class Info(val str: String, val time: String)

class WorkoutSummaryAdapter internal constructor(val _context: Context, stations: Collection<MainActivity.Station>, which: Int) : RecyclerView.Adapter<WorkoutSummaryAdapter.Holder>() {
    val text: MutableList<Info> = ArrayList()
	val context by weakReference(_context)

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): Holder {
        return Holder(WorkoutSummaryListItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false))
    }

    override fun onBindViewHolder(holder: Holder, i: Int) {
        holder.set(text[i].str, text[i].time)
    }

    override fun getItemCount(): Int {
        return text.size
    }

	fun getTimeString(secs: Long): String = when (secs) {
		in 0..60 -> context?.getString(R.string.text_n_secs, secs)?:secs.toString()
		in 60..(60 * 60) -> String.format("%d:%02d", secs / 60, secs % 60)
		else -> secs.toHMS().let {
			String.format("%d:%02d:%02d", it[0], it[1], it[2])
		}
	}

	class Holder(var binding: WorkoutSummaryListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        operator fun set(name: String, time: String) {
            binding.tvStationName.text = name
            binding.tvStationTime.text = time
        }
    }

    init {
        text.add(Info(_context.getString(R.string.workout_column_station), _context.resources.getStringArray(R.array.workout_column_names)[which]))
        stations.forEach { s->
            when (which) {
                0 -> s.todaySecs
                1 -> s.thisWeekSecs
                2 -> s.thisMonthSecs
                else -> s.allTimeSecs
            }.also { secs ->
	            if (secs > 0)
		            text.add(Info(s.name, getTimeString(secs)))
            }
        }
    }
}