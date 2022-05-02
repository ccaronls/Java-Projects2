package cc.android.thomas;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cc.android.thomas.databinding.WorkoutSummaryListItemBinding;
import cc.lib.game.Utils;

public class WorkoutSummaryAdapter extends RecyclerView.Adapter<WorkoutSummaryAdapter.Holder> {

    final List<Object[]> text = new ArrayList<>();

    WorkoutSummaryAdapter(MainActivity.Workout workout, int which) {
        text.add(Utils.toArray(R.string.workout_column_station, which));
        for (MainActivity.Station s : workout.stations) {
            if (s.enabled) {
                long secs = 0;
                switch (which) {
                    case 0: // today
                        secs = s.todaySecs;
                        break;
                    case 1: // this week
                        secs = s.thisWeekSecs;
                        break;
                    case 2: // this month
                        secs = s.thisMonthSecs;
                        break;
                    default: // all time
                        secs = s.allTimeSecs;
                        break;
                }
                if (secs > 0)
                    text.add(Utils.toArray(s.name, MainActivity.getTimeString(secs)));
            }
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new Holder(WorkoutSummaryListItemBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int i) {
        Object [] o = text.get(i);
        if (o instanceof String[]) {
            holder.set((String)o[0], (String)o[1]);
        } else if (o instanceof Integer[]) {
            holder.set((Integer)o[0], (Integer)o[1]);
        }
    }

    @Override
    public int getItemCount() {
        return text.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {

        WorkoutSummaryListItemBinding binding;

        public Holder(@NonNull WorkoutSummaryListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void set(String name, String time) {
            binding.tvStationName.setText(name);
            binding.tvStationTime.setText(time);
        }

        void set(int stringResId, int stringArrayIndex) {
            binding.tvStationName.setText(stringResId);
            binding.tvStationTime.setText(binding.getRoot().getResources().getStringArray(R.array.workout_column_names)[stringArrayIndex]);
        }
    }
}
