package cc.lib.android

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import cc.lib.utils.StopWatch

class DroidStopWatch(
	startTime: Long = 0,
	pauseTime: Long = 0,
	curTime: Long = 0,
	deltaTime: Long = 0,
	lastCaptureTime: Long = 0
) : StopWatch(startTime, pauseTime, curTime, deltaTime, lastCaptureTime), Parcelable {
	constructor(reader: Parcel) : this(
		startTime = reader.readLong(),
		pauseTime = reader.readLong(),
		curTime = reader.readLong(),
		deltaTime = reader.readLong(),
		lastCaptureTime = reader.readLong()
	)

	override fun describeContents(): Int {
		return 0
	}

	override fun writeToParcel(dest: Parcel, flags: Int) {
		dest.writeLong(startTime)
		dest.writeLong(pauseTime)
		dest.writeLong(curTime)
		dest.writeLong(deltaTime)
		dest.writeLong(lastCaptureTime)
	}

	override val clockMiliseconds: Long
		protected get() = SystemClock.uptimeMillis()

	companion object {
		@JvmField
		val CREATOR: Parcelable.Creator<DroidStopWatch> = object : Parcelable.Creator<DroidStopWatch> {
			override fun createFromParcel(`in`: Parcel): DroidStopWatch? {
				return DroidStopWatch(`in`)
			}

			override fun newArray(size: Int): Array<DroidStopWatch?> {
				return arrayOfNulls(size)
			}
		}
	}
}
