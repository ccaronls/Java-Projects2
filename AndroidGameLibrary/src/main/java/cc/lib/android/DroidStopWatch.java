package cc.lib.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import cc.lib.utils.StopWatch;

public class DroidStopWatch extends StopWatch implements Parcelable {

    public DroidStopWatch() {
    }

    private DroidStopWatch(Parcel in) {
        startTime = in.readLong();
        pauseTime = in.readLong();
        curTime = in.readLong();
        deltaTime = in.readLong();
        lastCaptureTime = in.readLong();
    }

    public static final Creator<DroidStopWatch> CREATOR = new Creator<DroidStopWatch>() {
        @Override
        public DroidStopWatch createFromParcel(Parcel in) {
            return new DroidStopWatch(in);
        }

        @Override
        public DroidStopWatch[] newArray(int size) {
            return new DroidStopWatch[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startTime);
        dest.writeLong(pauseTime);
        dest.writeLong(curTime);
        dest.writeLong(deltaTime);
        dest.writeLong(lastCaptureTime);
    }

    @Override
    protected long getClockMiliseconds() {
        return SystemClock.uptimeMillis();
    }
}
