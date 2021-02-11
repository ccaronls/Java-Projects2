package cc.android.thomas;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.workout_summary_list_item, viewGroup, false);
        return new Holder(view);
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

        TextView stationName, stationTime;

        public Holder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.tv_station_name);
            stationTime = itemView.findViewById(R.id.tv_station_time);
        }

        void set(String name, String time) {
            stationName.setText(name);
            stationTime.setText(time);
        }

        void set(int stringResId, int stringArrayIndex) {
            stationName.setText(stringResId);
            stationTime.setText(stationTime.getResources().getStringArray(R.array.workout_column_names)[stringArrayIndex]);
        }
    }
}
