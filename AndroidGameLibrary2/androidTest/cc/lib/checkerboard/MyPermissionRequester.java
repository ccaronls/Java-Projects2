package cc.lib.checkerboard;

import android.app.UiAutomation;
import android.os.Build;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

public class MyPermissionRequester {

    public static final String TAG = MyPermissionRequester.class.getSimpleName();

    public static void request(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiAutomation auto = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            String cmd = "pm grant " + InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + " %1$s";
            String cmdTest = "pm grant " + InstrumentationRegistry.getInstrumentation().getContext().getPackageName() + " %1$s";
            String currCmd;
            for (String perm : permissions) {
                execute(String.format(cmd, perm), auto);
                execute(String.format(cmdTest, perm), auto);
            }
        }
    }

    private static void execute(String currCmd, UiAutomation auto){
        Log.d(TAG, "exec cmd: " + currCmd);
        auto.executeShellCommand(currCmd);
    }
}