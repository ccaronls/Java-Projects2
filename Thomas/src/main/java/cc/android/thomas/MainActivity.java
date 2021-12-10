package cc.android.thomas;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import cc.lib.android.CCActivityBase;
import cc.lib.android.DragAndDropAdapter;
import cc.lib.android.VerticalViewPager;
import cc.lib.annotation.Keep;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class MainActivity extends CCActivityBase implements
        NumberPicker.OnValueChangeListener,
        View.OnClickListener,
        Runnable,
        CompoundButton.OnCheckedChangeListener,
        TextToSpeech.OnInitListener,
        ViewPager.OnPageChangeListener {

    final static String TAG = MainActivity.class.getSimpleName();

    final static int STATE_STOPPED = 0;
    final static int STATE_PAUSED = 1;
    final static int STATE_RUNNING = 2;

    NumberPicker np_timer;
    NumberPicker np_stations;
    Button b_start, b_pause, b_options;
    //ToggleButton tb_randomize;
    TextView tv_timer, tv_currentstation;
    TextToSpeech tts = null;
    final String STATIONS_FILE = "stations.txt";
    final String WORKOUTS_FILE = "workouts.txt";
    int setIndex = 0;
    final List<Station> sets = new ArrayList<>();
    List<Workout> workouts;
    int currentWorkout = 0;
    VerticalViewPager pager_workouts;
    PagerAdapter pager_adapter;
    int timeLeftSecs = 0;
    int state = STATE_STOPPED; // stopped, paused, running

    static {
        Reflector.registerClass(StationType.class);
        Reflector.addAllFields(Workout.class);
        Reflector.addAllFields(Station.class);
    }

    @Keep
    public enum StationType {
        Cardio(R.string.station_type_cardio),
        Upper_Body(R.string.station_type_upper_body),
        Core(R.string.station_type_core),
        Lower_Body(R.string.station_type_lower_body);

        StationType(int stringResId) {
            this.stringResId = stringResId;
        }

        final int stringResId;
    }

    public static class Workout extends Reflector<Workout> {

        String name;
        Station [] stations;
        StationType [] ordering;
        int numStations;
        int timerIndex;

        void clearCounts() {
            for (Station st : stations) {
                st.clearCounts();
            }
        }
    }

    public static class Station extends Reflector<Station> implements Parcelable {

        String name;
        StationType type;
        boolean enabled = true;

        long lastDoneLocalSecs;
        long todaySecs;
        long thisWeekSecs;
        long thisMonthSecs;
        long allTimeSecs;

        public Station() {

        }

        public Station(String name, StationType type) {
            this.name = name;
            this.type = type;
        }

        protected Station(Parcel in) {
            name = in.readString();
            type = StationType.values()[in.readInt()];
            enabled = in.readByte() != 0;
        }

        void addSeconds(int seconds) {
            todaySecs += seconds;
            thisWeekSecs += seconds;
            thisMonthSecs += seconds;
            allTimeSecs += seconds;
        }

        void clearCounts() {
            lastDoneLocalSecs = todaySecs = thisWeekSecs = thisMonthSecs = allTimeSecs = 0;
        }

        public static final Creator<Station> CREATOR = new Creator<Station>() {
            @Override
            public Station createFromParcel(Parcel in) {
                return new Station(in);
            }

            @Override
            public Station[] newArray(int size) {
                return new Station[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(name);
            parcel.writeInt(type.ordinal());
            parcel.writeByte((byte) (enabled ? 1 : 0));
        }

        public String getDisplayName(Resources r) {
            return String.format("%s - %s", name, r.getString(type.stringResId));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Station station = (Station) o;
            return name.equals(station.name) &&
                    type == station.type;
        }

        @Override
        public int hashCode() {
            return Utils.hashCode(name, type);
        }
    };

    void loadWorkouts() {
        File file = new File(getFilesDir(), WORKOUTS_FILE);
        try {
            workouts = Reflector.deserializeFromFile(file);
            return;
        } catch (FileNotFoundException e) {
            // ignore
            Log.w(TAG, "File not found: " + file);
        } catch (Exception e) {
            e.getMessage();
            Log.e(TAG, "Failed to load " + WORKOUTS_FILE + " : " + e.getMessage());
        }

        Workout workout = new Workout();
        workout.name = getString(R.string.defaut_workout_name);
        workout.stations = getAllKnownStations();

        workout.ordering = StationType.values();
        workout.numStations = 60;
        workout.timerIndex = 5;

        workouts = new ArrayList(Arrays.asList(workout));
        currentWorkout = 0;
    }

    Station [] getDefaultStations() {
        return new Station[]{
                new Station(getString(R.string.jumping_jacks), StationType.Cardio),
                new Station(getString(R.string.jump_rope), StationType.Cardio),
                new Station(getString(R.string.burpies), StationType.Cardio),
                new Station(getString(R.string.high_knees), StationType.Cardio),

                new Station(getString(R.string.pushups), StationType.Upper_Body),
                new Station(getString(R.string.plank), StationType.Upper_Body),
                new Station(getString(R.string.plank_updowns), StationType.Upper_Body),
                new Station(getString(R.string.curls), StationType.Upper_Body),

                new Station(getString(R.string.bicycles), StationType.Core),
                new Station(getString(R.string.situps), StationType.Core),
                new Station(getString(R.string.boat), StationType.Core),
                new Station(getString(R.string.leg_lifts), StationType.Core),
                new Station(getString(R.string.bridge), StationType.Core),

                new Station(getString(R.string.chair_sit), StationType.Lower_Body),
                new Station(getString(R.string.lunges), StationType.Lower_Body),
                new Station(getString(R.string.squats), StationType.Lower_Body),
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
        Toolbar toolbar = findViewById(R.id.toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        loadWorkouts();

        np_timer = findViewById(R.id.np_period);
        String [] values = new String[30];
        for (int i=0; i<30; i++) {
            int seconds = 10+i*10;
            int minutes = seconds/60;
            seconds -= minutes*60;
            if (minutes > 0) {
                values[i] = getString(R.string.time_format, minutes, seconds);
            } else {
                values[i] = getString(R.string.tts_n_seconds, seconds);
            }
        }
        currentWorkout = getPrefs().getInt("current_workout", 0);
        if (currentWorkout >= workouts.size())
            currentWorkout = 0;
        np_timer.setMinValue(0);
        np_timer.setMaxValue(29);
        np_timer.setValue(workouts.get(currentWorkout).timerIndex);
        np_timer.setDisplayedValues(values);
        np_timer.setOnValueChangedListener(this);
        b_start = findViewById(R.id.b_start);
        b_start.setOnClickListener(this);
        b_pause = findViewById(R.id.b_pause);
        b_pause.setOnClickListener(this);
        b_options = findViewById(R.id.b_options);
        b_options.setOnClickListener(this);
        tv_timer = findViewById(R.id.tv_timer);
        //tb_randomize = findViewById(R.id.tb_random);
        //tb_randomize.setOnCheckedChangeListener(this);
        np_stations = findViewById(R.id.np_stations);
        np_stations.setMinValue(BuildConfig.DEBUG ? 2 : 5);
        np_stations.setMaxValue(60);
        np_stations.setValue(workouts.get(currentWorkout).numStations);
        tv_currentstation = findViewById(R.id.tv_currentstation);
        pager_workouts = findViewById(R.id.pager_workouts);
        pager_workouts.setOffscreenPageLimit(5);
        pager_workouts.setPageMargin(-50);
        pager_workouts.setPageTransformer(false, new DepthPageTransformer());
        pager_workouts.setAdapter(pager_adapter = new PagerAdapter() {

            @Override
            public int getCount() {
                return workouts.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View item = View.inflate(MainActivity.this, R.layout.workout_list_item, null);
                TextView tv = item.findViewById(R.id.text);
                tv.setText(workouts.get(position).name);
                container.addView(item);
                return item;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                return POSITION_NONE;
            }
        });
        pager_workouts.setCurrentItem(currentWorkout, false);
        pager_workouts.setOnPageChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tts = new TextToSpeech(this, this);
        tts.setLanguage(getResources().getConfiguration().locale);
        restoreState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPrefs().edit()
                .putInt("state", state)
                .putInt("setIndex", setIndex).commit();

        pause();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        pause();
        try {
            workouts.get(currentWorkout).numStations = np_stations.getValue();
            workouts.get(currentWorkout).timerIndex = np_timer.getValue();
            Reflector.serializeToFile(workouts, new File(getFilesDir(), WORKOUTS_FILE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreState() {
        int st = getPrefs().getInt("state", STATE_STOPPED);
        switch (st) {
            case STATE_STOPPED:
                stop();
                break;
            case STATE_PAUSED:
            case STATE_RUNNING:
                initWorkout();
                timeLeftSecs = getPrefs().getInt("timeLeftSecs", 0);
                setIndex = getPrefs().getInt("setIndex", 0);
                state = STATE_PAUSED;
                b_pause.setText(R.string.button_resume);
//                tv_timer.post(this);
                run();
                b_start.setText(R.string.button_stop);
                b_pause.setEnabled(true);
                b_options.setEnabled(false);
//                setKeepScreenOn(true);
                pause();
                break;
            default:
                stop();
                break;
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        if (i >= 0 && i < workouts.size()) {
            // save off the current values into the workout
            workouts.get(currentWorkout).timerIndex = np_timer.getValue();
            workouts.get(currentWorkout).numStations = np_stations.getValue();
            // update the np pickers to reflect the newly selected workout
            currentWorkout = i;
            Workout workout = workouts.get(i);
            Log.d(TAG, "Workout set to: " + workout.name);
            np_stations.setValue(workout.numStations);
            np_timer.setValue(workout.timerIndex);
            getPrefs().edit().putInt("current_workout", currentWorkout).apply();
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_pause:
                togglePause();
                break;
            case R.id.b_start:
                toggleStart();
                break;
            case R.id.b_options:
                showOptionsPopup();
                break;
        }
    }

    void showOptionsPopup() {
        newDialogBuilder().setTitle(R.string.popup_title_options)
                .setItems(getResources().getStringArray(R.array.popup_options), (dialogInterface, i) -> {
                    switch (i) {
                        case 0: {
                            Station [] stations = getAllKnownStations();
                            for (Station s : stations) {
                                s.enabled = false;
                            }
                            LinkedHashSet<Station> all = new LinkedHashSet<>();
                            Workout w = workouts.get(currentWorkout);
                            all.addAll(Arrays.asList(w.stations));
                            all.addAll(Arrays.asList(stations));
                            showStationsPopup(all.toArray(new Station[all.size()]));
                            break;
                        }
                        case 1:
                            showSummaryPopup();
                            break;
                        case 2:
                            showOrderingPopup();
                            break;
                        case 3:
                            showSaveWorkoutPopup(true);
                            break;
                        case 4:
                            showSaveWorkoutPopup(false);
                            break;
                        case 5:
                            newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_msg_confirmreset)
                                    .setNegativeButton(R.string.popup_button_cancel, null)
                                    .setPositiveButton(R.string.popup_button_reset, (dialogInterface1, i1) -> {
                                        new File(getFilesDir(), STATIONS_FILE).delete();
                                        new File(getFilesDir(), WORKOUTS_FILE).delete();
                                        loadWorkouts();
                                        pager_adapter.notifyDataSetChanged();
                                    }).show();
                            break;
                        case 6:
                            newDialogBuilder().setTitle(R.string.popup_title_about).setMessage(R.string.popup_about_body).setPositiveButton(R.string.popup_button_ok, null).show();
                            break;
                    }
                }).setPositiveButton(R.string.popup_button_ok, null).show();
    }

    void showSaveWorkoutPopup(boolean createNew) {
        EditText et = new EditText(this);
        et.setHint(R.string.et_hint_workoutname);
        Workout curWorkout = workouts.get(currentWorkout);
        if (!createNew) {
            et.setText(curWorkout.name, TextView.BufferType.EDITABLE);
        }

        newDialogBuilder().setTitle(createNew ? getString(R.string.popup_title_copy_workout, workouts.get(currentWorkout).name) : getString(R.string.popup_title_rename_workout, curWorkout.name)).setView(et)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save, (dialogInterface, i) -> {
                    String name = et.getText().toString();
                    if (name.isEmpty()) {
                        Snackbar.make(tv_currentstation, R.string.toast_err_empty_name, Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Workout w = workouts.get(currentWorkout);
                    if (createNew) {
                        Workout saved = w.deepCopy();
                        workouts.add(saved);
                    }
                    w.name = et.getText().toString();
                    try {
                        Reflector.serializeToFile(workouts, new File(getFilesDir(), WORKOUTS_FILE));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Snackbar.make(tv_currentstation, R.string.toast_err_save_workout, Snackbar.LENGTH_LONG).show();
                    }
                    pager_adapter.notifyDataSetChanged();
                }).show();
    }

    void showOrderingPopup() {
        View v = View.inflate(this, R.layout.ordering_popup, null);
        Button b_cardio = v.findViewById(R.id.b_cardio);
        Button b_upper = v.findViewById(R.id.b_upperbody);
        Button b_core = v.findViewById(R.id.b_core);
        Button b_lower = v.findViewById(R.id.b_lowerbody);
        ListView lv = v.findViewById(R.id.listview);
        DragAndDropAdapter<StationType> adapter = new DragAndDropAdapter<StationType>(lv, workouts.get(currentWorkout).ordering) {
            @Override
            protected void populateItem(StationType cmd, ViewGroup container) {
                TextView tv;
                if (container.getChildCount() > 0) {
                    tv = (TextView)container.getChildAt(0);
                } else {
                    tv = (TextView)View.inflate(MainActivity.this, R.layout.ordering_list_item, null);
                    container.addView(tv);
                }
                tv.setText(cmd.stringResId);
            }

            @Override
            protected String getItemName(StationType item) {
                return item.name().replace('_', ' ');
            }
        };
        lv.setAdapter(adapter);
        adapter.addDraggable(b_cardio, StationType.Cardio);
        adapter.addDraggable(b_upper, StationType.Upper_Body);
        adapter.addDraggable(b_core, StationType.Core);
        adapter.addDraggable(b_lower, StationType.Lower_Body);

        Dialog d = newDialogBuilder()
                .setView(v)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save, (dialogInterface, i) -> {
                    if (adapter.getList().size() == 0) {
                        Snackbar.make(tv_currentstation, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    workouts.get(currentWorkout).ordering = adapter.getList().toArray(new StationType[0]);
                }).create();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (d.getWindow() != null) {
            lp.copyFrom(d.getWindow().getAttributes());
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }
        d.show();
        //d.getWindow().setAttributes(lp);
    }

    Station [] getAllKnownStations() {
        try {
            return Reflector.deserializeFromFile(new File(getFilesDir(), STATIONS_FILE));
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            Snackbar.make(tv_currentstation, R.string.toast_err_load_stations, Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return getDefaultStations();
    }

    void showSummaryPopup() {

        Workout workout = workouts.get(currentWorkout);
        ViewPager pager = new ViewPager(this);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                RecyclerView rv = new RecyclerView(MainActivity.this);
                rv.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                rv.setAdapter(new WorkoutSummaryAdapter(workout, position));
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(MainActivity.this,
                        LinearLayoutManager.VERTICAL);
                rv.addItemDecoration(dividerItemDecoration);
                container.addView(rv);
                return rv;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View)object);
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                return POSITION_NONE;
            }

        });

        newDialogBuilder().setTitle(getString(R.string.popup_title_summary, workout.name))
                .setView(pager).setPositiveButton(R.string.popup_button_ok, null)
                .setNeutralButton(R.string.popup_button_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newDialogBuilder().setTitle(R.string.popup_title_confirm)
                                .setMessage(getString(R.string.popup_msg_confirm_clear, workout.name))
                                .setNeutralButton(R.string.popup_button_cancel, null)
                                .setPositiveButton(R.string.popup_button_clear, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                workout.clearCounts();
                                            }
                                        }).show();
                    }
                }).show();
    }

    static String getTimeString(long secs) {
        if (secs <= 60) {
            return String.format("%d Secs", secs);
        } else if (secs < 60*60) {
            return String.format("%d:%02d", secs/60, secs%60);
        } else {
            long mins = secs/60;
            secs -= mins*60;
            long hours = mins/60;
            mins -= hours * 60;
            return String.format("%d:%02d:%02d", hours, mins, secs);
        }
    }

    void showStationsPopup(final Station [] all) {
        final String [] items = new String[all.length];
        final boolean [] checked = new boolean[all.length];

        for (int i=0; i<all.length; i++) {
            items[i] = all[i].getDisplayName(getResources());
            checked[i] = all[i].enabled;
        }

        newDialogBuilder().setTitle(R.string.popup_title_stations)
                .setMultiChoiceItems(items, checked, (dialogInterface, i, b) -> all[i].enabled = b)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_add_station, (dialogInterface, i) -> showAddStationPopup(all)).setNeutralButton(R.string.popup_button_save, (dialogInterface, i) -> {
                    if (Utils.filter(all, object -> object.enabled).size() == 0) {
                        Snackbar.make(tv_currentstation, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        workouts.get(currentWorkout).stations = all;
                        Reflector.serializeToFile(all, new File(getFilesDir(), STATIONS_FILE));
                    } catch (Exception e) {
                        Snackbar.make(tv_currentstation, getString(R.string.toast_err_savefailed, e.getMessage()), Snackbar.LENGTH_LONG).show();
                    }
                }).show();
    }

    void showAddStationPopup(final Station [] stations) {
        View v = View.inflate(this, R.layout.add_station_popup, null);
        final EditText et_name = v.findViewById(R.id.et_name);
        final NumberPicker np_type = v.findViewById(R.id.np_station_type);
        np_type.setDisplayedValues(Utils.toStringArray(StationType.values()));
        np_type.setMinValue(0);
        np_type.setMaxValue(StationType.values().length-1);

        newDialogBuilder().setTitle(R.string.popup_title_add_station).setView(v).setNegativeButton(R.string.popup_button_cancel, (dialogInterface, i) -> showStationsPopup(stations)).setPositiveButton(R.string.popup_button_add, (dialogInterface, i) -> {
            String name = et_name.getText().toString().trim();
            if (name.isEmpty()) {
                Snackbar.make(tv_currentstation, R.string.toast_err_emptyname, Snackbar.LENGTH_SHORT).show();
                showStationsPopup(stations);
                return;
            }
            for (Station s : stations) {
                if (s.name.equalsIgnoreCase(name)) {
                    Snackbar.make(tv_currentstation, R.string.toast_err_duplicationname, Snackbar.LENGTH_SHORT).show();
                    showStationsPopup(stations);
                    return;
                }
            }
            Station st = new Station(name, StationType.values()[np_type.getValue()]);
            List<Station> l = new ArrayList<>(Arrays.asList(stations));
            l.add(st);
            Station [] newArr = l.toArray(new Station[l.size()]);
            showStationsPopup(newArr);
        }).show();
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {

    }

    void initWorkout() {
        final List<Station> [] workout = new ArrayList[StationType.values().length];
        final int [] count = new int[StationType.values().length];
        Workout w = workouts.get(currentWorkout);
        Station [] all = w.stations;
        for (int i=0; i<workout.length; i++) {
            workout[i] = new ArrayList<>();
        }
        DateTimeZone dtz = DateTimeZone.forTimeZone(TimeZone.getDefault());
        DateTime today = DateTime.now(dtz).withTimeAtStartOfDay();
        for (Station s: all) {
            if (s.enabled) {
                workout[s.type.ordinal()].add(s);
                if (s.lastDoneLocalSecs > 0) {
                    // see if we need to 0 out the day, week, month counters
                    DateTime prev = new DateTime(s.lastDoneLocalSecs*1000, dtz).withTimeAtStartOfDay();
                    if (prev.isBefore(today)) {
                        s.todaySecs=0;
                    }
                    if (prev.isBefore(today.withDayOfWeek(1))) {
                        s.thisWeekSecs=0;
                    }
                    if (prev.isBefore(today.withDayOfMonth(1))) {
                        s.thisMonthSecs=0;
                    }
                }
                s.lastDoneLocalSecs = today.getMillis()/1000;
            }
        }
        for (List<Station> l : workout) {
            Utils.shuffle(l);
        }
        sets.clear();
        // customize ordering here so, for example, we can do like 2 upper and 2 lower
        StationType [] order = workouts.get(currentWorkout).ordering;
        int orderIndex = 0;
        while (sets.size() < np_stations.getValue()) {
            int idx = order[orderIndex++ % order.length].ordinal();
            List<Station> set = workout[idx];
            if (set.size() == 0) {
                continue;
            }
            int c = count[idx]++ % set.size();
            Station s = set.get(c);
            Log.d("SETS", "Added " + s.toString());
            sets.add(s);
        }

        Log.d("SETS", "All sets: " + sets);
        sayNow(sets.get(0).name);
        setIndex = 0;
    }

    void toggleStart() {
        if (state != STATE_STOPPED) {
            stop();
        } else {
            start();
        }
    }

    void togglePause() {
        if (state == STATE_PAUSED) {
            resume();
        } else if (state == STATE_RUNNING) {
            pause();
        }
    }

    void pause() {
        if (state == STATE_RUNNING) {
            getPrefs().edit().putInt("timeLeftSecs", timeLeftSecs).apply();
            state = STATE_PAUSED;
            b_pause.setText(R.string.button_resume);
            tv_timer.removeCallbacks(this);
            setKeepScreenOn(false);
        }
    }

    void resume() {
        if (state != STATE_RUNNING) {
            timeLeftSecs = getPrefs().getInt("timeLeftSecs", 0);
            state = STATE_RUNNING;
            b_pause.setText(R.string.button_pause);
            b_start.setText(R.string.button_stop);
            tv_timer.post(this);
            setKeepScreenOn(true);
        }
    }

    void stop() {
        timeLeftSecs = 0;
        state = STATE_STOPPED;
        b_start.setText(R.string.button_start);
        tv_timer.removeCallbacks(this);
        b_pause.setEnabled(false);
        b_options.setEnabled(true);
        setKeepScreenOn(false);
    }

    void start() {
        initWorkout();
        timeLeftSecs = 0;
        state = STATE_RUNNING;
        b_pause.setText(R.string.button_pause);
        tv_timer.post(this);
        b_start.setText(R.string.button_stop);
        b_pause.setEnabled(true);
        b_options.setEnabled(false);
        setKeepScreenOn(true);
    }

    @Override
    public void run() {

        int stationPeriodSecs = np_timer.getValue()*10 + 10;
        final int numStations = np_stations.getValue();

        if (timeLeftSecs <= 0)
            timeLeftSecs = stationPeriodSecs;
        else
            timeLeftSecs--;

        if (timeLeftSecs > stationPeriodSecs) {
            timeLeftSecs = stationPeriodSecs;
        }

        if (timeLeftSecs == 0) {
            ++setIndex;
        }

        boolean allDone = setIndex >= numStations;
        setIndex = Utils.clamp(setIndex, 0, numStations-1);
        String curSet, nextSet;
        Station station = sets.get(setIndex);
//        station.addSeconds(1);

        if (allDone) {
            curSet = getString(R.string.tts_completed);
            nextSet = "";
        } else if (setIndex == numStations-1) {
            curSet = station.name;
            nextSet = getString(R.string.tts_next_completed);
        } else {
            curSet = station.name;
            nextSet = getString(R.string.tts_next_workout, sets.get(setIndex+1).name);
        }

        int secs = timeLeftSecs;
        switch (secs) {
            case 60: case 30: case 15:
            case 3: case 2: case 1:
                if (stationPeriodSecs > secs)
                    sayNow(secs < 10 ? String.valueOf(secs) : getString(R.string.tts_n_seconds, secs));

            default: {
                if (secs <= 60) {
                    tv_timer.setText(String.valueOf(secs));
                } else {
                    int mins = secs/60;
                    secs -= mins*60;
                    tv_timer.setText(getString(R.string.time_format, mins, secs));
                }
                break;
            }

            case 0:
                station.addSeconds(stationPeriodSecs);
                if (allDone) {
                    sayNow(getString(R.string.tts_alldone));
                    tv_timer.setText(getString(R.string.tts_completed));
                    stop();
                    showSummaryPopup();
                } else {
                    sayNow(curSet);
                    tv_timer.setText(R.string.text_switch);
                }
                break;
        }

        tv_currentstation.setText(getString(R.string.text_n_of_n_stations, (setIndex +1), numStations, curSet, nextSet));

        if (state == STATE_RUNNING)
            tv_timer.postDelayed(this, 1000);
    }

    @Override
    public void onInit(int status) {
        switch (status) {
            case TextToSpeech.SUCCESS:
                //sayNow(getString(R.string.tts_welcome));
                break;

            default:
                Log.e("TTS", "Failed to init.  status=" + status);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }

    void sayNow(String txt) {
        if (tts != null && txt != null) {
            tts.stop();
            tts.speak(txt, TextToSpeech.QUEUE_ADD, null);
        }
    }

}
