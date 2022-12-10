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
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import cc.android.thomas.databinding.*
import cc.lib.android.CCActivityBase
import cc.lib.android.DragAndDropAdapter
import cc.lib.android.EmailHelper
import cc.lib.annotation.Keep
import cc.lib.game.Utils
import cc.lib.utils.Reflector
import cc.lib.utils.prettify
import cc.lib.utils.weakReference
import com.google.android.material.snackbar.Snackbar
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.File
import java.io.FileNotFoundException
import java.lang.RuntimeException
import java.util.*

enum class WorkoutState {
	STOPPED,
	PAUSED,
	RUNNING
}

class ThomasViewModel(_context : MainActivity) : ViewModel() {
	val activity by weakReference(_context)

	val started = MutableLiveData(false)
	val numStations = MutableLiveData(0)
	val setIndex = MutableLiveData(0)
	val curSet = MutableLiveData("")
	val nextSet = MutableLiveData("")
	val timerText = MutableLiveData(_context.getString(R.string.text_timer))

	val timeLeftSecs = MutableLiveData(0)
	val state = MutableLiveData(WorkoutState.STOPPED)

	val npTimePeriod = MutableLiveData(0)
	val npStations = MutableLiveData(0)
	val npCircuits = MutableLiveData(0)

	val currentWorkout = MutableLiveData(0)

	val npPeriodFormatter = object : NumberPicker.Formatter {
		override fun format(value: Int): String {
			var seconds = 10 + value * 10
			val minutes = seconds / 60
			seconds -= minutes * 60
			return if (minutes > 0) {
				activity?.getString(R.string.time_format, minutes, seconds)?:""
			} else {
				activity?.getString(R.string.tts_n_seconds, seconds)?:""
			}
		}
	}
}

class MainActivity : CCActivityBase(),
	Runnable,
	OnInitListener,
	ViewPager.OnPageChangeListener {
    lateinit var binding: ActivityMainBinding
    var tts: TextToSpeech? = null
    val sets: MutableList<Station> = ArrayList()
    lateinit var workouts: MutableList<Workout>
    lateinit var pager_adapter: PagerAdapter
	val vm : ThomasViewModel by viewModels {
		object : ViewModelProvider.NewInstanceFactory() {
			override fun <T : ViewModel> create(modelClass: Class<T>): T  = ThomasViewModel(this@MainActivity) as T
		}
	}
	val curWorkout : Workout
		get() = workouts[(vm.currentWorkout.value?:0).coerceIn(0, workouts.size)]

    companion object {
        val TAG : String = MainActivity::class.java.simpleName
        const val STATIONS_FILE = "stations.txt"
        const val WORKOUTS_FILE = "workouts.txt"
        fun getTimeString(_secs: Long): String {
            var secs = _secs
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
        Lower_Body(R.string.station_type_lower_body),
        Stretch(R.string.station_type_stretch);
    }

    class Workout : Reflector<Workout>() {
        var name: String = ""
        lateinit var stations: Array<Station>
        lateinit var ordering: Array<StationType>
        var numStations = 0
        var timerIndex = 0
	    var numCircuits = 1
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

        override fun equals(other: Any?): Boolean {
            if (this === other)
                return true
            if (other == null)
                return false
            if (other is Station) {
                return other.name == name && other.type == type
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
        } catch (e: Exception) {
            e.message
            Log.e(TAG, "Failed to load " + WORKOUTS_FILE + " : " + e.message)
	        if (BuildConfig.DEBUG)
	        	throw RuntimeException(e)
        }
        val workout = Workout()
        workout.name = getString(R.string.defaut_workout_name)
        workout.stations = allKnownStations
        workout.ordering = StationType.values()
        workout.numStations = 60
        workout.timerIndex = 5
        workouts = ArrayList<Workout>(Arrays.asList(workout))
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

    override fun onCreate(bundle: Bundle?) {
	    super.onCreate(bundle)
	    binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
	    binding.lifecycleOwner = this
	    binding.viewModel = vm
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
	    vm.currentWorkout.value = prefs.getInt("current_workout", 0)
	    //binding.npPeriod.minValue = 0
	    //binding.npPeriod.maxValue = 29
	    //binding.npPeriod.value = w.timerIndex
	    //binding.npPeriod.displayedValues = values
	    //binding.npPeriod.setOnValueChangedListener(this)
	    //binding.npStations.setOnValueChangedListener(this)
	    //binding.npStations.minValue = if (BuildConfig.DEBUG) 2 else 5
	    //binding.npStations.maxValue = 60
	    //binding.npStations.value = w.numStations
	    //binding.npCircuits.minValue = 1
	    //binding.npCircuits.maxValue = 10
	    //binding.npCircuits.value = w.numCircuits
	    //binding.npCircuits.setOnValueChangedListener(this)
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
	    binding.pagerWorkouts.setCurrentItem(vm.currentWorkout.value?:0, false)
	    binding.pagerWorkouts.setOnPageChangeListener(this)
    }

	override fun onStart() {
		super.onStart()
		vm.state.observe(this) {
			Log.d(TAG, "setKeepScreenOn $it")
			setKeepScreenOn(it == WorkoutState.RUNNING)
		}
		listOf(
			Pair(vm.npStations, Workout::numStations),
			Pair(vm.npCircuits, Workout::numCircuits),
			Pair(vm.npTimePeriod, Workout::timerIndex)
		).forEach { pair ->
			pair.first.value = pair.second.get(curWorkout)
			pair.first.observe(this) {
				with (curWorkout) {
					if (pair.second.get(this) != it) {
						pair.second.set(this, it)
						saveWorkouts()
					}
				}
			}
		}
		vm.currentWorkout.observe(this) {
			with (curWorkout) {
				vm.npStations.value = numStations
				vm.npCircuits.value = numCircuits
				vm.npTimePeriod.value = timerIndex
			}
		}
    }

    override fun onResume() {
        super.onResume()
        tts = TextToSpeech(this, this)
        tts?.language = preferredLocale
        restoreState()
	    updateWorkoutsSummary()
    }

    override fun onPause() {
        super.onPause()
        prefs.edit()
            .putInt("state", vm.state.value?.ordinal?:0)
            .putInt("setIndex", vm.setIndex.value?:0)
	        .apply()
        pause()
        tts?.let {
            it.stop()
            it.shutdown()
            tts = null
        }
        pause()
        saveWorkouts()
    }

    fun saveWorkouts() {
        try {
            Reflector.serializeToFile<Any>(workouts, File(filesDir, WORKOUTS_FILE))
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.vCenter, R.string.toast_err_save_workout, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun restoreState() {
        val st = WorkoutState.values()[prefs.getInt("state", WorkoutState.STOPPED.ordinal)]
        when (st) {
            WorkoutState.STOPPED -> stop()
            WorkoutState.PAUSED,
            WorkoutState.RUNNING -> {
                initWorkout()
                vm.timeLeftSecs.value = prefs.getInt("timeLeftSecs", 0)
                vm.setIndex.value = prefs.getInt("setIndex", 0)
                vm.state.value = WorkoutState.PAUSED
                binding.bPause.setText(R.string.button_resume)
                //                binding.tvTimer.post(this);
                run()
                binding.bStart.setText(R.string.button_stop)
                binding.bPause.isEnabled = true
                binding.bOptions.isEnabled = false
                pause()
            }
        }
    }

	private fun updateWorkoutsSummary() {
		val dtz = DateTimeZone.forTimeZone(TimeZone.getDefault())
		val today = DateTime.now(dtz).withTimeAtStartOfDay()
		workouts.forEach { w ->
			val all = w.stations
			for (s in all) {
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
			}
		}
		saveWorkouts()
	}

    override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
    override fun onPageSelected(i: Int) {
        if (i >= 0 && i < workouts.size) {
            vm.currentWorkout.value = i
            prefs.edit().putInt("current_workout", i.coerceIn(0, workouts.size)).apply()
        }
    }

    override fun onPageScrollStateChanged(i: Int) {}

    fun showOptionsPopup() {
        newDialogBuilder().setTitle(R.string.popup_title_options)
                .setItems(resources.getStringArray(R.array.popup_options)) { _, i: Int ->
                    when (i) {
                        0 -> {
                            val stations = allKnownStations
                            for (s in stations) {
                                s.enabled = false
                            }
                            val all = LinkedHashSet<Station>()
                            all.addAll(Arrays.asList(*curWorkout.stations))
                            all.addAll(Arrays.asList(*stations))
                            showStationsPopup(all.toTypedArray())
                        }
                        1 -> showSummaryPopup()
                        2 -> showOrderingPopup()
                        3 -> showSaveWorkoutPopup(true)
                        4 -> showSaveWorkoutPopup(false)
                        5 -> newDialogBuilder().setTitle(R.string.popup_title_confirm).setMessage(R.string.popup_msg_confirmreset)
                                .setNegativeButton(R.string.popup_button_cancel, null)
                                .setPositiveButton(R.string.popup_button_reset) { _, _ ->
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
        if (!createNew) {
            et.setText(curWorkout.name, TextView.BufferType.EDITABLE)
        }
        newDialogBuilder().setTitle(if (createNew) getString(R.string.popup_title_copy_workout, curWorkout.name) else getString(R.string.popup_title_rename_workout, curWorkout.name)).setView(et)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save) { dialogInterface: DialogInterface?, i: Int ->
                    val name = et.text.toString()
                    if (name.isEmpty()) {
                        Snackbar.make(binding.vCenter, R.string.toast_err_empty_name, Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val w = curWorkout
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
        val adapter: DragAndDropAdapter<StationType> = object : DragAndDropAdapter<StationType>(ob.listview, *curWorkout.ordering) {
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
	    with (ob.vgButtons) {
		    removeAllViews()
	    	StationType.values().forEach { station ->
	    		ItemOrderingButtonBinding.inflate(layoutInflater).let {
	    			it.button.text = getString(station.stringResId)
				    addView(it.root)
				    adapter.addDraggable(it.button, station)
			    }
		    }
	    }
        val d: Dialog = newDialogBuilder()
                .setView(ob.root)
                .setNegativeButton(R.string.popup_button_cancel, null)
                .setPositiveButton(R.string.popup_button_save) { _, _ ->
                    if (adapter.list.size == 0) {
                        Snackbar.make(binding.vCenter, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    curWorkout.ordering = adapter.list.toTypedArray()
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
                Snackbar.make(binding.vCenter, R.string.toast_err_load_stations, Snackbar.LENGTH_LONG).show()
                e.printStackTrace()
            }
            return defaultStations
        }

    fun showSummaryPopup() {
        val wb = WorkoutSummaryBinding.inflate(layoutInflater)
	    val stations = mutableMapOf<String, Station>()
	    workouts.forEach { w ->
		    w.stations.forEach { s ->
			    stations[s.name]?.let { station ->
				    station.allTimeSecs += s.allTimeSecs
				    station.thisMonthSecs += s.thisMonthSecs
				    station.thisWeekSecs += s.thisWeekSecs
				    station.todaySecs += s.todaySecs
				    station.lastDoneLocalSecs = minOf(station.lastDoneLocalSecs, s.lastDoneLocalSecs)
			    }?:s.deepCopy().also {
				    stations[s.name] = it
			    }
		    }
	    }
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
                rv.adapter = WorkoutSummaryAdapter(container.context, stations.values, position)
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
	    val workout = curWorkout
        newDialogBuilder().setTitle(getString(R.string.popup_title_summary, workout.name))
                .setView(wb.root).setPositiveButton(R.string.popup_button_ok, null)
                .setNeutralButton(R.string.popup_button_clear) { _,_ ->
                    newDialogBuilder().setTitle(R.string.popup_title_confirm)
                            .setMessage(getString(R.string.popup_msg_confirm_clear, workout.name))
                            .setNeutralButton(R.string.popup_button_cancel, null)
                            .setPositiveButton(R.string.popup_button_clear) { _,_ ->
                                run {
                                    workouts.forEach { it.clearCounts() }
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
                        Snackbar.make(binding.vCenter, R.string.toast_err_emptylist, Snackbar.LENGTH_LONG).show()
                        return@setNeutralButton
                    }
                    try {
                        curWorkout.stations = all
                        Reflector.serializeToFile<Any>(all, File(filesDir, STATIONS_FILE))
                    } catch (e: Exception) {
                        Snackbar.make(binding.vCenter, getString(R.string.toast_err_savefailed, e.message), Snackbar.LENGTH_LONG).show()
                    }
                }.show()
    }

    fun showAddStationPopup(stations: Array<Station>) {
        val ab = AddStationPopupBinding.inflate(LayoutInflater.from(this))
        ab.npStationType.displayedValues = Utils.toStringArray(StationType.values())
        ab.npStationType.minValue = 0
        ab.npStationType.maxValue = StationType.values().size - 1
        newDialogBuilder().setTitle(R.string.popup_title_add_station).setView(ab.root).setNegativeButton(R.string.popup_button_cancel) { _, _ -> showStationsPopup(stations) }.setPositiveButton(R.string.popup_button_add) { dialogInterface: DialogInterface?, i: Int ->
            val name = ab.etName.text.toString().trim { it <= ' ' }
            if (name.isEmpty()) {
                Snackbar.make(binding.vCenter, R.string.toast_err_emptyname, Snackbar.LENGTH_SHORT).show()
                showStationsPopup(stations)
                return@setPositiveButton
            }
            for (s in stations) {
                if (s.name.equals(name, ignoreCase = true)) {
                    Snackbar.make(binding.vCenter, R.string.toast_err_duplicationname, Snackbar.LENGTH_SHORT).show()
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

    fun initWorkout() {
        val workout: Array<MutableList<Station>> = Array(StationType.values().size) { ArrayList<Station>() }
        val count = IntArray(StationType.values().size)
        val w = curWorkout
	    val all = w.stations
	    val dtz = DateTimeZone.forTimeZone(TimeZone.getDefault())
	    val today = DateTime.now(dtz).withTimeAtStartOfDay()
	    for (s in all) {
		    if (s.enabled) {
			    workout[s.type.ordinal].add(s)
			    s.lastDoneLocalSecs = today.millis / 1000
		    }
	    }
        for (l in workout) {
            Utils.shuffle(l)
        }
        sets.clear()
	    val subSets = mutableListOf<Station>()
        // customize ordering here so, for example, we can do like 2 upper and 2 lower
        val order = w.ordering
        var orderIndex = 0
        while (subSets.size < binding.npStations.value) {
            val idx = order[orderIndex++ % order.size].ordinal
            val set: List<Station> = workout[idx]
            if (set.isEmpty()) {
                continue
            }
            val c = count[idx]++ % set.size
            val s = set[c]
            Log.d("SETS", "Added $s")
            subSets.add(s)
        }
	    for (i in 0 until w.numCircuits) {
	    	sets.addAll(subSets)
	    }
        Log.d("SETS", "All sets: $sets")
        sayNow(sets[0].name)
        vm.setIndex.value = 0
    }

    fun toggleStart() {
	    when (vm.state.value) {
	    	WorkoutState.STOPPED -> start()
		    else -> stop()
	    }
    }

    fun togglePause() {
	    when (vm.state.value) {
	    	WorkoutState.PAUSED -> resume()
		    WorkoutState.RUNNING -> pause()
	    }
    }

    fun pause() {
        if (vm.state.value == WorkoutState.RUNNING) {
            prefs.edit().putInt("timeLeftSecs", vm.timeLeftSecs.value!!).apply()
            vm.state.value = WorkoutState.PAUSED
            binding.vCenter.removeCallbacks(this)
        }
    }

    fun resume() {
        if (vm.state.value != WorkoutState.RUNNING) {
            vm.timeLeftSecs.value = prefs.getInt("timeLeftSecs", 0)
            vm.state.value = WorkoutState.RUNNING
            binding.vCenter.post(this)
        }
    }

    fun stop() {
        vm.timeLeftSecs.value = 0
        vm.state.value = WorkoutState.STOPPED
        binding.vCenter.removeCallbacks(this)
    }

    fun start() {
        initWorkout()
        vm.timeLeftSecs.value = 0
        vm.state.value = WorkoutState.RUNNING
        //binding.bPause.setText(R.string.button_pause)
        binding.vCenter.post(this)
        //binding.bStart.setText(R.string.button_stop)
        //binding.bPause.isEnabled = true
        //binding.bOptions.isEnabled = false
        //setKeepScreenOn(true)
    }

    override fun run() {
	    vm.started.value = true
        val stationPeriodSecs = binding.npPeriod.value * 10 + 10
        val numStations = binding.npStations.value
	    vm.numStations.value = numStations
        if (vm.timeLeftSecs.value!! <= 0)
        	vm.timeLeftSecs.value = stationPeriodSecs
        else
        	with (vm.timeLeftSecs) { value = value?.dec() }
        if (vm.timeLeftSecs.value!! > stationPeriodSecs) {
            vm.timeLeftSecs.value = stationPeriodSecs
        }
	    if (sets.isEmpty()) {
		    sayNow(getString(R.string.tts_alldone))
		    vm.timerText.value = getString(R.string.text_completed)
		    stop()
		    showSummaryPopup()
		    vm.curSet.value = getString(R.string.text_completed)
		    vm.nextSet.value = ""
		    return
	    }
	    val station = sets.first()

		if (sets.size > 1) {
	        vm.curSet.value = station.name
	        vm.nextSet.value = sets[1].name
        } else {
	        vm.curSet.value = station.name
	        vm.nextSet.value = getString(R.string.text_completed)
        }
        var secs = vm.timeLeftSecs.value!!
        when (secs) {
            60, 30, 15, 3, 2, 1 -> {
                if (stationPeriodSecs > secs)
                	sayNow(if (secs < 10) secs.toString() else getString(R.string.tts_n_seconds, secs))
                run {
                    if (secs <= 60) {
                        vm.timerText.value = secs.toString()
                    } else {
                        val mins = secs / 60
                        secs -= mins * 60
	                    vm.timerText.value = getString(R.string.time_format, mins, secs)
                    }
                }
            }
            0 -> {
                sayNow(vm.nextSet.value?:"")
                vm.timerText.value = getString(R.string.text_switch)
	            station.addSeconds(stationPeriodSecs)
	            saveWorkouts()
	            with (vm.setIndex) {
		            value = value?.inc()?.coerceIn(0 until numStations)
	            }
	            sets.removeFirst()
            }
            else -> {
                if (secs <= 60) {
	                vm.timerText.value = secs.toString()
                } else {
                    val mins = secs / 60
                    secs -= mins * 60
	                vm.timerText.value = getString(R.string.time_format, mins, secs)
                }
            }
        }
        //binding.tvCurrentstation.text = getString(R.string.text_n_of_n_stations, setIndex + 1, numStations, curSet, nextSet)
        if (vm.state.value == WorkoutState.RUNNING)
        	binding.vCenter.postDelayed(this, 1000)

    }

    override fun onInit(status: Int) {
        when (status) {
            TextToSpeech.SUCCESS -> {
            }
            else                 -> Log.e("TTS", "Failed to init.  status=$status")
        }
    }

    fun sayNow(txt: String) {
        tts?.let {
            it.stop()
	        it.speak(txt, TextToSpeech.QUEUE_ADD, null)
        }
    }

	fun emailWorkouts() {
		if (BuildConfig.DEBUG) {
			File(filesDir, WORKOUTS_FILE).copyTo(File(cacheDir, WORKOUTS_FILE)).also {
				EmailHelper.sendEmail(this, it, "ccaronls@yahoo.com", "WORKOUTS", "Workouts for today")
				Toast.makeText(this, "Email Sent", Toast.LENGTH_LONG).show();
			}
		}
	}
}