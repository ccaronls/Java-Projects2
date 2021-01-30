package cc.android.thomas;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.lib.android.CCActivityBase;
import cc.lib.android.DragAndDropAdapter;
import cc.lib.android.DroidStopWatch;
import cc.lib.game.Utils;
import cc.lib.utils.Reflector;

public class MainActivity extends CCActivityBase implements NumberPicker.OnValueChangeListener, View.OnClickListener, Runnable, CompoundButton.OnCheckedChangeListener, TextToSpeech.OnInitListener {

    NumberPicker np_timer;
    NumberPicker np_stations;
    Button b_start, b_pause, b_options;
    //ToggleButton tb_randomize;
    TextView tv_timer, tv_currentstation;
    final DroidStopWatch sw = new DroidStopWatch();
    TextToSpeech tts = null;
    final String STATIONS_FILE = "stations.txt";
    final String ORDERING_FILE = "ordering.txt";
    int workoutIndex = 0;
    final List<String> sets = new ArrayList<>();

    static {
        Reflector.registerClass(StationType.class);
    }

    public enum StationType {
        Cardio,
        Upper_Body,
        Core,
        Lower_Body
    };

    public static class Station extends Reflector<Station> implements Parcelable {

        static {
            addAllFields(Station.class);
        }

        String name;
        StationType type;
        boolean enabled = true;

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

        public String toString() {
            return name + " - " + type.toString().replace('_', ' ');
        }
    };

    final Station [] DEFAULT_STATIONS = {
            new Station("Jumping Jacks", StationType.Cardio),
            new Station("Jump Rope", StationType.Cardio),
            new Station("Burpies", StationType.Cardio),
            new Station("High Knees", StationType.Cardio),

            new Station("Push Ups", StationType.Upper_Body),
            new Station("Plank", StationType.Upper_Body),
            new Station("Plank Up Downs", StationType.Upper_Body),
            new Station("Curls", StationType.Upper_Body),

            new Station("Bicycles", StationType.Core),
            new Station("Sit Ups", StationType.Core),
            new Station("Boat", StationType.Core),
            new Station("Leg Lifts", StationType.Core),
            new Station("Bridge", StationType.Core),

            new Station("Chair Sit", StationType.Lower_Body),
            new Station("Lunges", StationType.Lower_Body),
            new Station("Squats", StationType.Lower_Body),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        np_timer = findViewById(R.id.number_picker_period);
        String [] values = new String[30];
        for (int i=0; i<30; i++) {
            int seconds = 10+i*10;
            int minutes = seconds/60;
            seconds -= minutes*60;
            if (minutes > 0) {
                values[i] = String.format("%d:%02d", minutes, seconds);
            } else {
                values[i] = String.format("%d Seconds", seconds);
            }
        }
        np_timer.setMinValue(0);
        np_timer.setMaxValue(29);
        np_timer.setValue(getPrefs().getInt("period", 5));
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
        np_stations = findViewById(R.id.number_picker_stations);
        np_stations.setMinValue(5);
        np_stations.setMaxValue(60);
        np_stations.setValue(getPrefs().getInt("stations", 20));
        tv_currentstation = findViewById(R.id.tv_currentstation);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tts = new TextToSpeech(this, this);
        tts.setLanguage(getResources().getConfiguration().locale);
    }

    @Override
    protected void onResume() {
        super.onResume();
        b_start.setText("Start");
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
        getPrefs().edit()
                .putInt("stations", np_stations.getValue())
                .putInt("period", np_timer.getValue())
        .apply();
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
                //showStationsPopup(getAllKnownStations());
                showOptionsPopup();
                break;
        }
    }

    void showOptionsPopup() {
        newDialogBuilder().setTitle("OPTIONS")
                .setItems(Utils.toArray("STATIONS", "ORDERING", "RESET"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                showStationsPopup(getAllKnownStations());
                                break;
                            case 1:
                                showOrderingPopup();
                                break;
                            case 2:
                                newDialogBuilder().setTitle("CONFIRM").setMessage("This will reset ordering to default and remove all your added stations. Are you sure?")
                                        .setNegativeButton("Cancel", null)
                                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                new File(getFilesDir(), STATIONS_FILE).delete();
                                                new File(getFilesDir(), ORDERING_FILE).delete();
                                            }
                                        }).show();
                        }
                    }
                }).setPositiveButton("OK", null).show();
    }

    void showOrderingPopup() {
        View v = View.inflate(this, R.layout.ordering_popup, null);
        Button b_cardio = v.findViewById(R.id.b_cardio);
        Button b_upper = v.findViewById(R.id.b_upperbody);
        Button b_core = v.findViewById(R.id.b_core);
        Button b_lower = v.findViewById(R.id.b_lowerbody);
        ListView lv = v.findViewById(R.id.listview);
        DragAndDropAdapter<StationType> adapter = new DragAndDropAdapter<StationType>(lv, getOrdering()) {
            @Override
            protected void populateItem(StationType cmd, ViewGroup container) {
                TextView tv;
                if (container.getChildCount() > 0) {
                    tv = (TextView)container.getChildAt(0);
                } else {
                    tv = new TextView(MainActivity.this);
                    container.addView(tv);
                }
                tv.setText(cmd.name());
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

        Dialog d = newDialogBuilder().setTitle("Customize Ordering").setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (adapter.getList().size() == 0) {
                            Toast.makeText(MainActivity.this, "List cannot be empty", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            Log.d("ORDERING", adapter.getList().toString());
                            Reflector.serializeToFile(adapter.getList(), new File(getFilesDir(), ORDERING_FILE));
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }).create();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
//        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        d.show();
        //d.getWindow().setAttributes(lp);
    }

    List<StationType> getOrdering() {
        try {
            return Reflector.deserializeFromFile(new File(getFilesDir(), ORDERING_FILE));
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            Toast.makeText(this, "Error loading orderings", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return Arrays.asList(StationType.values());
    }

    Station [] getAllKnownStations() {
        try {
            return Reflector.deserializeFromFile(new File(getFilesDir(), STATIONS_FILE));
        } catch (FileNotFoundException e) {
            // ignore
        } catch (Exception e) {
            Toast.makeText(this, "Error loading stations", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return DEFAULT_STATIONS;
    }

    void showStationsPopup(final Station [] all) {
        final String [] items = new String[all.length];
        final boolean [] checked = new boolean[all.length];

        for (int i=0; i<all.length; i++) {
            items[i] = all[i].toString();
            checked[i] = all[i].enabled;
        }

        newDialogBuilder().setTitle("Stations").setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                all[i].enabled = b;
            }
        }).setNegativeButton("Cancel", null)
        .setPositiveButton("Add Station", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showAddStationPopup(all);
            }
        }).setNeutralButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (Utils.filterItems(new Utils.Filter<Station>() {
                    @Override
                    public boolean keep(Station object) {
                        return object.enabled;
                    }
                }, all).size() == 0) {
                    Toast.makeText(MainActivity.this, "Cannot save Empty list", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    Reflector.serializeToFile(all, new File(getFilesDir(), STATIONS_FILE));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
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

        newDialogBuilder().setTitle("Add Station").setView(v).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showStationsPopup(stations);
            }
        }).setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = et_name.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Empty Station Name", Toast.LENGTH_SHORT).show();
                    showStationsPopup(stations);
                    return;
                }
                for (Station s : stations) {
                    if (s.name.equalsIgnoreCase(name)) {
                        Toast.makeText(MainActivity.this, "Duplicate Station Name", Toast.LENGTH_SHORT).show();
                        showStationsPopup(stations);
                        return;
                    }
                }
                Station st = new Station(name, StationType.values()[np_type.getValue()]);
                List<Station> l = new ArrayList<>();
                l.addAll(Arrays.asList(stations));
                l.add(st);
                Station [] newArr = l.toArray(new Station[l.size()]);
                showStationsPopup(newArr);
            }
        }).show();
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {

    }

    void initWorkout() {
        final List<Station> [] workout = new ArrayList[StationType.values().length];
        final int [] count = new int[StationType.values().length];
        Station [] all = getAllKnownStations();
        for (int i=0; i<workout.length; i++) {
            workout[i] = new ArrayList<>();
        }
        for (Station s: all) {
            if (s.enabled)
                workout[s.type.ordinal()].add(s);
        }
        for (List l : workout) {
            Utils.shuffle(l);
        }
        sets.clear();
        //int index = 0;
        // customize ordering here so, for example, we can do like 2 upper and 2 lower
        List<StationType> order = getOrdering();
        int orderIndex = 0;
        while (sets.size() < np_stations.getValue()) {
            //int idx = (index++ % workout.length);
            int idx = order.get(orderIndex++ % order.size()).ordinal();
            List<Station> set = workout[idx];
            if (set.size() == 0) {
                continue;
            }
            int c = count[idx]++ % set.size();
            Station s = set.get(c);
            Log.d("SETS", "Added " + s.toString());
            sets.add(s.name);
        }

        Log.d("SETS", "All sets: " + sets);
        sayNow(sets.get(0));
        workoutIndex = 0;
    }

    String getSet(int index) {
        index = index%sets.size();
        return sets.get(index);
    }

    void toggleStart() {
        if (sw.isStarted()) {
            stop();
        } else {
            start();
        }
    }

    void togglePause() {
        if (sw.isPaused()) {
            resume();
        } else {
            pause();
        }
    }

    void pause() {
        sw.pause();
        b_pause.setText("RESUME");
        tv_timer.removeCallbacks(this);
    }

    void resume() {
        sw.unpause();
        b_pause.setText("PAUSE");
        tv_timer.post(this);
    }

    void stop() {
        sw.stop();
        b_start.setText("START");
        tv_timer.removeCallbacks(this);
        b_pause.setEnabled(false);
        b_options.setEnabled(true);
    }

    void start() {
        initWorkout();
        sw.start();
        b_pause.setText("PAUSE");
        tv_timer.post(this);
        b_start.setText("STOP");
        b_pause.setEnabled(true);
        b_options.setEnabled(false);
    }

    @Override
    public void run() {

        sw.capture();

        int elapsedTimeSecs = (int)(sw.getTime()/1000);
        int stationPeriod = np_timer.getValue()*10 + 10;
        final int numStations = np_stations.getValue()-1;

        int timeLeftSecs = stationPeriod - (elapsedTimeSecs % stationPeriod);
        if (timeLeftSecs == 1)
            workoutIndex = Math.min(++workoutIndex, numStations);

        boolean lastSet = workoutIndex >= numStations;

        String curSet = getSet(workoutIndex);
        String nextSet = workoutIndex >= numStations ? "Completed" : getSet(workoutIndex+1);

        int secs = timeLeftSecs-1;
        switch (secs) {
            case 30: case 15:
            case 3: case 2: case 1:
                sayNow(secs < 10 ? String.valueOf(secs) : String.format("%d seconds", secs));

            default: {
                if (secs <= 60) {
                    tv_timer.setText(String.valueOf(secs));
                } else {
                    int mins = secs/60;
                    secs -= mins*60;
                    tv_timer.setText(String.format("%d:%02d", mins, secs));
                }
                break;
            }

            case 0:
                if (lastSet) {
                    sayNow("All Done");
                    tv_timer.setText("COMPLETED");
                    stop();
                } else {
                    sayNow(curSet);
                    tv_timer.setText("SWITCH");
                }
                break;
        }

        tv_currentstation.setText(String.format("%d of %d Stations\n%s\nNext Up:%s", (workoutIndex+1), numStations+1, curSet, nextSet));

        if (sw.isStarted() && !sw.isPaused())
            tv_timer.postDelayed(this, 1000);
    }

    @Override
    public void onInit(int status) {
        switch (status) {
            case TextToSpeech.SUCCESS:
                sayNow("Welcome to Thomas's workout");
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
