package cc.android.thomas

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cc.android.thomas.MainActivity.Workout
import cc.android.thomas.databinding.WorkoutSummaryListItemBinding
import cc.lib.game.Utils
import java.util.*

data class Info(val str: String, val time: String)

class WorkoutSummaryAdapter internal constructor(context: Context, workout: Workout, which: Int) : RecyclerView.Adapter<WorkoutSummaryAdapter.Holder>() {
    val text: MutableList<Info> = ArrayList()
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): Holder {
        return Holder(WorkoutSummaryListItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false))
    }

    override fun onBindViewHolder(holder: Holder, i: Int) {
        holder.set(text[i].str, text[i].time)
    }

    override fun getItemCount(): Int {
        return text.size
    }

    class Holder(var binding: WorkoutSummaryListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        operator fun set(name: String, time: String) {
            binding.tvStationName.text = name
            binding.tvStationTime.text = time
        }
    }

    init {
        text.add(Info(context.getString(R.string.workout_column_station), context.resources.getStringArray(R.array.workout_column_names)[which]))
        workout.stations.forEach { s->
            if (s.enabled) {
                var secs: Long = when (which) {
                    0 -> s.todaySecs
                    1 -> s.thisWeekSecs
                    2 -> s.thisMonthSecs
                    else -> s.allTimeSecs
                }
                if (secs > 0)
                    text.add(Info(s.name, MainActivity.getTimeString(secs)))
            }
        }
    }
}