package cc.lib.checkerboard;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import cc.lib.mp.android.WifiP2pHelper;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MultiplayerInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        //assertEquals("cc.game.zombicide.android", appContext.getPackageName());
    }

    @Test
    public void testWiFiP2PMgr() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        WifiP2pHelper helper = new WifiP2pHelper(appContext);
        helper.p2pInitialize();
        helper.discoverPeers();
        Thread.sleep(5000);
        helper.destroy();
    }

}