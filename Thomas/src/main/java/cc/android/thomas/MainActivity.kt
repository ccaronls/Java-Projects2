package cc.android.thomas

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.NumberPicker.OnValueChangeListener
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import cc.android.thomas.databinding.*
import cc.lib.android.CCActivityBase
import cc.lib.android.DragAndDropAdapter
import cc.lib.annotation.Keep
import cc.lib.game.Utils
import cc.lib.utils.Reflector
import cc.lib.utils.prettify
import com.google.android.material.snackbar.Snackbar
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class MainActivity : CCActivityBase(), OnValueChangeListener, View.OnClickListener, Runnable, CompoundButton.OnCheckedChangeListener, OnInitListener, ViewPager.OnPageChangeListener {
    lateinit var binding: ActivityMainBinding
    var tts: TextToSpeech? = null
    var setIndex = 0
    val sets: MutableList<Station> = ArrayList()
    lateinit var workouts: MutableList<Workout>
    var currentWorkout = 0
    lateinit var pager_adapter: PagerAdapter
    var timeLeftSecs = 0
    var state = STATE_STOPPED // stopped, paused, running

    companion object {
        val TAG = MainActivity::class.java.simpleName
        val STATIONS_FILE = "stations.txt"
        val WORKOUTS_FILE = "workouts.txt"
        const val STATE_STOPPED = 0
        const val STATE_PAUSED = 1
        const val STATE_RUNNING = 2
        fun getTimeString(secs: Long): String {
            var secs = secs
            return if (secs <= 60) {
                String.format("%d Secs", secs)
            } else if (secs < 60 * 60) {
                String.format("%d:%02d", secs / 60, secs % 60)
            } else {
                var mins = secs / 60
                secs -= mins * 60
                val hours = mins / 60
                mins -= hours * 60
                String.format("%d:%02d:%02d", hours, mins, secs)
            }
        }

        init {
            Reflector.registerClass(StationType::class.java)
            Reflector.addAllFields(Workout::class.java)
            Reflector.addAllFields(Station::class.java)
        }
    }

    @Keep
    enum class StationType(val stringResId: Int) {
        Cardio(R.string.station_type_cardio),
        Upper_Body(R.string.station_type_upper_body),
        Core(R.string.station_type_core),
        Lower_Body(R.string.station_type_lower_body);
    }

    class Workout : Reflector<Workout>() {
        var name: String = ""
        lateinit var stations: Array<Station>
        lateinit var ordering: Array<StationType>
        var numStations = 0
        var timerIndex = 0
        fun clearCounts() {
            stations.forEach { st ->
                st.clearCounts()
            }
        }
    }

    @Parcelize
    data class Station(var name: String = "", var type: StationType = StationType.Core, var enabled:Boolean = true) : Reflector<Station>(), Parcelable {

        @IgnoredOnParcel var lastDoneLocalSecs: Long = 0
        @IgnoredOnParcel var todaySecs: Long = 0
        @IgnoredOnParcel var thisWeekSecs: Long = 0
        @IgnoredOnParcel var thisMonthSecs: Long = 0
        @IgnoredOnParcel var allTimeSecs: Long = 0

        fun addSeconds(seconds: Int) {
            todaySecs += seconds.toLong()
            thisWeekSecs += seconds.toLong()
            thisMonthSecs += seconds.toLong()
            allTimeSecs += seconds.toLong()
        }

        fun clearCounts() {
            allTimeSecs = 0
            thisMonthSecs = allTimeSecs
            thisWeekSecs = thisMonthSecs
            todaySecs = thisWeekSecs
            lastDoneLocalSecs = todaySecs
        }

        fun getDisplayName(r: Resources): String {
            return String.format("%s - %s", name, r.getString(type.stringResId))
        }

        override fun equals(o: Any?): Boolean {
            if (this === o)
                return true
            if (o == null)
                return false
            if (o is Station) {
                return o.name == name && o.type == type
            }
            return false
        }

        override fun hashCode(): Int {
            return Utils.hashCode(name, type)
        }

    }

    fun loadWorkouts() {
        val file = File(filesDir, WORKOUTS_FILE)
        try {
            workouts = Reflector.deserializeFromFile(file)
            //            Log.d(TAG, "Loaded:\n" + workouts.toString());
            return
        } catch (e: FileNotFoundException) {
            // ignore
            Log.w(TAG, "File not found: $file")
        } catch (e: Exception) {
            e.message
            Log.e(TAG, "Failed to load " + WORKOUTS_FILE + " : " + e.message)
        }
        val workout = Workout()
        workout.name = getString(R.string.defaut_workout_name)
        workout.stations = allKnownStations
        workout.ordering = StationType.values()
        workout.numStations = 60
        workout.timerIndex = 5
        workouts = ArrayList<Workout>(Arrays.asList(workout))
        currentWorkout = 0
    }

    val defaultStations: Array<Station>
        get() = arrayOf(
                Station(getString(R.string.jumping_jacks), StationType.Cardio),
                Station(getString(R.string.jump_rope), StationType.Cardio),
                Station(getString(R.string.burpies), StationType.Cardio),
                Station(getString(R.string.high_knees), StationType.Cardio),
                Station(getString(R.string.pushups), StationType.Upper_Body),
                Station(getString(R.string.plank), StationType.Upper_Body),
                Station(getString(R.string.plank_updowns), StationType.Upper_Body),
                Station(getString(R.string.curls), StationType.Upper_Body),
                Station(getString(R.string.bicycles), StationType.Core),
                Station(getString(R.string.situps), StationType.Core),
                Station(getString(R.string.boat), StationType.Core),
                Station(getString(R.string.leg_lifts), StationType.Core),
                Station(getString(R.string.bridge), StationType.Core),
                Station(getString(R.string.chair_sit), StationType.Lower_Body),
                Station(getString(R.string.lunges), StationType.Lower_Body),
                Station(getString(R.string.squats), StationType.Lower_Body))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        loadWorkouts()
        val values = arrayOfNulls<String>(30)
        for (i in 0..29) {
            var seconds = 10 + i * 10
            val minutes = seconds / 60
            seconds -= minutes * 60
            if (minutes > 0) {
                values[i] = getString(R.string.time_format, minutes, seconds)
            } else {
                values[i] = getString(R.string.tts_n_seconds, seconds)
            }
        }
        currentWorkout = prefs.getInt("current_workout", 0)
        if (currentWorkout >= workouts.size) currentWorkout = 0
        binding.npPeriod.minValue = 0
        binding.npPeriod.maxValue = 29
        binding.npPeriod.value = workouts[currentWorkout].timerIndex
        binding.npPeriod.displayedValues = values
        binding.npPeriod.setOnValueChangedListener(this)
        binding.bStart.setOnClickListener(this)
        binding.bPause.setOnClickListener(this)
        binding.bOptions.setOnClickListener(this)
        binding.npStations.setOnValueChangedListener(this)
        binding.npStations.minValue = if (BuildConfig.DEBUG) 2 else 5
        binding.npStations.maxValue = 60
        binding.npStations.value = workouts[currentWorkout].numStations
        binding.pagerWorkouts.offscreenPageLimit = 5
        binding.pagerWorkouts.pageMargin = -50
        binding.pagerWorkouts.setPageTransformer(false, DepthPageTransformer())
        binding.pagerWorkouts.adapter = object : PagerAdapter() {
            override fun getCount(): Int {
                return workouts.size
            }

            override fun isViewFromObject(view: View, o: Any): Boolean {
                return view === o
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val lb = WorkoutListItemBinding.inflate(layoutInflater)
                lb.text.text = workouts[position].name
                container.addView(lb.root)
                return lb.root
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }

            override fun getItemPosition(`object`: Any): Int {
                return POSITION_NONE
            }
        }.also { pager_adapter = it }
        binding.pagerWorkouts.setCurrentItem(currentWorkout, false)
        binding.pagerWorkouts.setOnPageChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        tts = TextToSpeech(this, this)
        tts?.language = resources.configuration.locale
        restoreState()
    }

    override fun onPause() {
        super.onPause()
        prefs.edit()
                .putInt("state", state)
                .putInt("setIndex", setIndex).apply()
        pause()
        tts?.let {
            it.stop()
            it.shutdown()
            tts = null
        }
        pause()
        workouts[currentWorkout].numStations = binding.npStations.value
        workouts[currentWorkout].timerIndex = binding.npPeriod.value
        saveWorkouts()
    }

    fun saveWorkouts() {
        try {
            Reflector.serializeToFile<Any>(workouts, File(filesDir, WORKOUTS_FILE))
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.tvCurrentstation, R.string.toast_err_save_workout, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun restoreState() {
        val st = prefs.getInt("state", STATE_STOPPED)
        when (st) {
            STATE_STOPPED -> stop()
            STATE_PAUSED, STATE_RUNNING -> {
                initWorkout()
                timeLeftSecs = prefs.getInt("timeLeftSecs", 0)
                setIndex = prefs.getInt("setIndex", 0)
                state = STATE_PAUSED
                binding.bPause.setText(R.string.button_resume)
                //                binding.tvTimer.post(this);
                run()
                binding.bStart.setText(R.string.button_stop)
                binding.bPause.isEnabled = true
                binding.bOptions.isEnabled = false
                //                setKeepScreenOn(true);
                pause()
            }
            else                        -> stop()
        }
    }

    override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
    override fun onPageSelected(i: Int) {
        if (i >= 0 && i < workouts.size) {
            // save off the current values into the workout
            workouts[currentWorkout].timerIndex = binding.npPeriod.value
            workouts[currentWorkout].numStations = binding.npStations.value
            // update the np pickers to reflect the newly selected workout
            currentWorkout = i
            val workout = workouts[i]
            Log.d(TAG, "Workout set to: " + workout.name)
            binding.npStations.value = workout.numStations
            binding.npPeriod.value = workout.timerIndex
            prefs.edit().putInt("current_workout", currentWorkout).apply()
        }
    }

    override fun onPageScrollStateChanged(i: Int) {}
    override fun onClick(view: View) {
        when (view.id) {
            R.id.b_pause -> togglePause()
            R.id.b_start -> toggleStart()
            R.id.b_options -> showOptionsPopup()
        }
    }

    fun showOptionsPopup() {
        newDialogBuilder().setTitle(R.string.popup_title_options)
                .setItems(resources.getStringArray(R.array.popup_options)) { dialogInterface: DialogInterface?, i: Int ->
                    when (i) {
                        0 -> {
                            val stations = allKnownStations
                            for (s in stations) {
                                s.enabled = false
                            }
                            val all = LinkedHashSet<Station>()
                            val w = workouts[currentWorkout]
                            all.addAll(Arrays.asList(*w.stations))
                            all.addAll(Arrays.asList(*stations))
                            showStationsPopup(all.toTypedArray())
                        }
                        1 -> showSummaryPopup()
                        2 -> showOrderingPopup()
                        3 -> showSaveWorkoutPopup(true)
                        4 -> showSaveWorkoutPopup(false)
                        5 -> newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_msg_confirmreset)
                                .setNegativeButton(R.string.popup_button_cancel, null)
                                .setPositiveButton(R.string.popup_button_reset) { dialogInterface1: DialogInterface?, i1: Int ->
                                    File(filesDir, STATIONS_FILE).delete()
                                    File(filesDir, WORKOUTS_FILE).delete()
                                    loadWorkouts()
                                    pager_adapter.notifyDataSetChanged()
                                }.show()
                        6 -> newDialogBuilder().setTitle(R.string.popup_title_about).setMessage(R.string.popup_about_body).setPositiveButton(R.string.popup_button_ok, null).show()
                    }
                }.setPositiveButton(R.string.popup_button_ok, null).show()
    }

    fun showSaveWorkoutPopup(createNew: Boolean) {
        val et = EditText(this)
        et.setHint(R.string.et_hint_workoutname)
        val curWorkout = workouts[currentWorkout]
        if (!createNew) {
            et.setText(curWorkout.name, TextView.BufferType.EDITABLE)
        }
        newDialogBuilder().setTitle(if (createNew) getString(R.string.popup_title_copy_workout, workouts[currentWorkout].name) else getString(R.string.popup_title_rename_workout, curWorkout.name)).setView(et)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save) { dialogInterface: DialogInterface?, i: Int ->
                    val name = et.text.toString()
                    if (name.isEmpty()) {
                        Snackbar.make(binding.tvCurrentstation, R.string.toast_err_empty_name, Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val w = workouts[currentWorkout]
                    if (createNew) {
                        val saved = w.deepCopy()
                        workouts.add(saved)
                    }
                    w.name = et.text.toString()
                    saveWorkouts()
                    pager_adapter.notifyDataSetChanged()
                }.show()
    }

    fun showOrderingPopup() {
        val ob = OrderingPopupBinding.inflate(layoutInflater)
        val adapter: DragAndDropAdapter<StationType> = object : DragAndDropAdapter<StationType>(ob.listview, *workouts[currentWorkout].ordering) {
            override fun populateItem(cmd: StationType, container: ViewGroup, lineNum: Int) {
                val tv: TextView
                if (container.childCount > 0) {
                    tv = container.getChildAt(0) as TextView
                } else {
                    tv = View.inflate(this@MainActivity, R.layout.ordering_list_item, null) as TextView
                    container.addView(tv)
                }
                tv.setText(cmd.stringResId)
            }

            override fun getItemName(item: StationType): String {
                return prettify(item.name)
            }
        }
        adapter.addDraggable(ob.bCardio, StationType.Cardio)
        adapter.addDraggable(ob.bUpperbody, StationType.Upper_Body)
        adapter.addDraggable(ob.bCore, StationType.Core)
        adapter.addDraggable(ob.bLowerbody, StationType.Lower_Body)
        val d: Dialog = newDialogBuilder()
                .setView(ob.root)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save) { _, _ ->
                    if (adapter.list.size == 0) {
                        Snackbar.make(binding.tvCurrentstation, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    workouts[currentWorkout].ordering = adapter.list.toTypedArray()
                }.create()
        val lp = WindowManager.LayoutParams()
        d.window?.let {
            lp.copyFrom(it.attributes)
            //        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        d.show()
    }

    // ignore
    val allKnownStations: Array<Station>
        get() {
            try {
                return Reflector.deserializeFromFile(File(filesDir, STATIONS_FILE))
            } catch (e: FileNotFoundException) {
                // ignore
            } catch (e: Exception) {
                Snackbar.make(binding.tvCurrentstation, R.string.toast_err_load_stations, Snackbar.LENGTH_LONG).show()
                e.printStackTrace()
            }
            return defaultStations
        }

    fun showSummaryPopup() {
        val wb = WorkoutSummaryBinding.inflate(layoutInflater)
        val workout = workouts[currentWorkout]
        wb.viewPager.adapter = object : PagerAdapter() {
            override fun getCount(): Int {
                return 4
            }

            override fun isViewFromObject(view: View, o: Any): Boolean {
                return view === o
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val rv = RecyclerView(this@MainActivity)
                rv.layoutManager = LinearLayoutManager(this@MainActivity)
                rv.adapter = WorkoutSummaryAdapter(container.context, workout, position)
                val dividerItemDecoration = DividerItemDecoration(this@MainActivity,
                        LinearLayoutManager.VERTICAL)
                rv.addItemDecoration(dividerItemDecoration)
                container.addView(rv)
                return rv
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container.removeView(`object` as View)
            }

            override fun getItemPosition(`object`: Any): Int {
                return POSITION_NONE
            }
        }
        newDialogBuilder().setTitle(getString(R.string.popup_title_summary, workout.name))
                .setView(wb.root).setPositiveButton(R.string.popup_button_ok, null)
                .setNeutralButton(R.string.popup_button_clear) { dialog, which ->
                    newDialogBuilder().setTitle(R.string.popup_title_confirm)
                            .setMessage(getString(R.string.popup_msg_confirm_clear, workout.name))
                            .setNeutralButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_clear) { dialog, which ->
                                run {
                                    workout.clearCounts()
                                    Reflector.serializeToFile<Any>(workouts, File(filesDir, WORKOUTS_FILE))
                                }
                            }.show()
                }.show()
    }

    fun showStationsPopup(all: Array<Station>) {
        val items = arrayOfNulls<String>(all.size)
        val checked = BooleanArray(all.size)
        for (i in all.indices) {
            items[i] = all[i].getDisplayName(resources)
            checked[i] = all[i].enabled
        }
        newDialogBuilder().setTitle(R.string.popup_title_stations)
                .setMultiChoiceItems(items, checked) { dialogInterface: DialogInterface?, i: Int, b: Boolean -> all[i].enabled = b }
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_add_station) { dialogInterface: DialogInterface?, i: Int -> showAddStationPopup(all) }.setNeutralButton(R.string.popup_button_save) { dialogInterface: DialogInterface?, i: Int ->
                    if (Utils.filter(all) { `object`: Station -> `object`.enabled }.size == 0) {
                        Snackbar.make(binding.tvCurrentstation, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show()
                        return@setNeutralButton
                    }
                    try {
                        workouts[currentWorkout].stations = all
                        Reflector.serializeToFile<Any>(all, File(filesDir, STATIONS_FILE))
                    } catch (e: Exception) {
                        Snackbar.make(binding.tvCurrentstation, getString(R.string.toast_err_savefailed, e.message), Snackbar.LENGTH_LONG).show()
                    }
                }.show()
    }

    fun showAddStationPopup(stations: Array<Station>) {
        val ab = AddStationPopupBinding.inflate(LayoutInflater.from(this))
        ab.npStationType.displayedValues = Utils.toStringArray(StationType.values())
        ab.npStationType.minValue = 0
        ab.npStationType.maxValue = StationType.values().size - 1
        newDialogBuilder().setTitle(R.string.popup_title_add_station).setView(ab.root).setNegativeButton(R.string.popup_button_cancel) { dialogInterface: DialogInterface?, i: Int -> showStationsPopup(stations) }.setPositiveButton(R.string.popup_button_add) { dialogInterface: DialogInterface?, i: Int ->
            val name = ab.etName.text.toString().trim { it <= ' ' }
            if (name.isEmpty()) {
                Snackbar.make(binding.tvCurrentstation, R.string.toast_err_emptyname, Snackbar.LENGTH_SHORT).show()
                showStationsPopup(stations)
                return@setPositiveButton
            }
            for (s in stations) {
                if (s.name.equals(name, ignoreCase = true)) {
                    Snackbar.make(binding.tvCurrentstation, R.string.toast_err_duplicationname, Snackbar.LENGTH_SHORT).show()
                    showStationsPopup(stations)
                    return@setPositiveButton
                }
            }
            val st = Station(name, StationType.values()[ab.npStationType.value])
            val l: MutableList<Station> = ArrayList(Arrays.asList(*stations))
            l.add(st)
            val newArr = l.toTypedArray()
            showStationsPopup(newArr)
        }.show()
    }

    override fun onValueChange(numberPicker: NumberPicker, previousValue: Int, newValue: Int) {
        when (numberPicker) {
            binding.npPeriod -> workouts[currentWorkout].timerIndex = newValue
            binding.npStations -> workouts[currentWorkout].numStations = newValue
        }
        saveWorkouts()
    }

    fun initWorkout() {
        val workout: Array<MutableList<Station>> = Array(StationType.values().size) { ArrayList<Station>() }
        val count = IntArray(StationType.values().size)
        val w = workouts[currentWorkout]
        val all = w.stations
        val dtz = DateTimeZone.forTimeZone(TimeZone.getDefault())
        val today = DateTime.now(dtz).withTimeAtStartOfDay()
        for (s in all) {
            if (s.enabled) {
                workout[s.type.ordinal].add(s)
                if (s.lastDoneLocalSecs > 0) {
                    // see if we need to 0 out the day, week, month counters
                    val prev = DateTime(s.lastDoneLocalSecs * 1000, dtz).withTimeAtStartOfDay()
                    if (prev.isBefore(today)) {
                        s.todaySecs = 0
                    }
                    if (prev.isBefore(today.withDayOfWeek(1))) {
                        s.thisWeekSecs = 0
                    }
                    if (prev.isBefore(today.withDayOfMonth(1))) {
                        s.thisMonthSecs = 0
                    }
                }
                s.lastDoneLocalSecs = today.millis / 1000
            }
        }
        for (l in workout) {
            Utils.shuffle(l)
        }
        sets.clear()
        // customize ordering here so, for example, we can do like 2 upper and 2 lower
        val order = workouts[currentWorkout].ordering
        var orderIndex = 0
        while (sets.size < binding.npStations.value) {
            val idx = order[orderIndex++ % order.size].ordinal
            val set: List<Station> = workout[idx]
            if (set.isEmpty()) {
                continue
            }
            val c = count[idx]++ % set.size
            val s = set[c]
            Log.d("SETS", "Added $s")
            sets.add(s)
        }
        Log.d("SETS", "All sets: $sets")
        sayNow(sets[0].name)
        setIndex = 0
    }

    fun toggleStart() {
        if (state != STATE_STOPPED) {
            stop()
        } else {
            start()
        }
    }

    fun togglePause() {
        if (state == STATE_PAUSED) {
            resume()
        } else if (state == STATE_RUNNING) {
            pause()
        }
    }

    fun pause() {
        if (state == STATE_RUNNING) {
            prefs.edit().putInt("timeLeftSecs", timeLeftSecs).apply()
            state = STATE_PAUSED
            binding.bPause.setText(R.string.button_resume)
            binding.tvTimer.removeCallbacks(this)
            setKeepScreenOn(false)
        }
    }

    fun resume() {
        if (state != STATE_RUNNING) {
            timeLeftSecs = prefs.getInt("timeLeftSecs", 0)
            state = STATE_RUNNING
            binding.bPause.setText(R.string.button_pause)
            binding.bStart.setText(R.string.button_stop)
            binding.tvTimer.post(this)
            setKeepScreenOn(true)
        }
    }

    fun stop() {
        timeLeftSecs = 0
        state = STATE_STOPPED
        binding.bStart.setText(R.string.button_start)
        binding.tvTimer.removeCallbacks(this)
        binding.bPause.isEnabled = false
        binding.bOptions.isEnabled = true
        setKeepScreenOn(false)
    }

    fun start() {
        initWorkout()
        timeLeftSecs = 0
        state = STATE_RUNNING
        binding.bPause.setText(R.string.button_pause)
        binding.tvTimer.post(this)
        binding.bStart.setText(R.string.button_stop)
        binding.bPause.isEnabled = true
        binding.bOptions.isEnabled = false
        setKeepScreenOn(true)
    }

    override fun run() {
        val stationPeriodSecs = binding.npPeriod.value * 10 + 10
        val numStations = binding.npStations.value
        if (timeLeftSecs <= 0) timeLeftSecs = stationPeriodSecs else timeLeftSecs--
        if (timeLeftSecs > stationPeriodSecs) {
            timeLeftSecs = stationPeriodSecs
        }
        if (timeLeftSecs == 0) {
            sets[setIndex++].addSeconds(stationPeriodSecs)
            saveWorkouts()
        }
        val allDone = setIndex >= numStations
        setIndex = Utils.clamp(setIndex, 0, numStations - 1)
        val curSet: String
        val nextSet: String
        val station = sets[setIndex]
        if (allDone) {
            curSet = getString(R.string.tts_completed)
            nextSet = ""
        } else if (setIndex == numStations - 1) {
            curSet = station.name
            nextSet = getString(R.string.tts_next_completed)
        } else {
            curSet = station.name
            nextSet = getString(R.string.tts_next_workout, sets[setIndex + 1].name)
        }
        var secs = timeLeftSecs
        when (secs) {
            60, 30, 15, 3, 2, 1 -> {
                if (stationPeriodSecs > secs) sayNow(if (secs < 10) secs.toString() else getString(R.string.tts_n_seconds, secs))
                run {
                    if (secs <= 60) {
                        binding.tvTimer.text = secs.toString()
                    } else {
                        val mins = secs / 60
                        secs -= mins * 60
                        binding.tvTimer.text = getString(R.string.time_format, mins, secs)
                    }
                }
            }
            0 -> {
                if (allDone) {
                    sayNow(getString(R.string.tts_alldone))
                    binding.tvTimer.text = getString(R.string.tts_completed)
                    stop()
                    showSummaryPopup()
                } else {
                    sayNow(curSet)
                    binding.tvTimer.setText(R.string.text_switch)
                }
            }
            else                -> {
                if (secs <= 60) {
                    binding.tvTimer.text = secs.toString()
                } else {
                    val mins = secs / 60
                    secs -= mins * 60
                    binding.tvTimer.text = getString(R.string.time_format, mins, secs)
                }
            }
        }
        binding.tvCurrentstation.text = getString(R.string.text_n_of_n_stations, setIndex + 1, numStations, curSet, nextSet)
        if (state == STATE_RUNNING) binding.tvTimer.postDelayed(this, 1000)
    }

    override fun onInit(status: Int) {
        when (status) {
            TextToSpeech.SUCCESS -> {
            }
            else                 -> Log.e("TTS", "Failed to init.  status=$status")
        }
    }

    override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {}
    fun sayNow(txt: String) {
        tts?.let {
            it.stop()
            it.speak(txt, TextToSpeech.QUEUE_ADD, null)
        }
    }
}