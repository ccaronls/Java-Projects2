package cc.lib.checkerboard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class CheckerboardInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        //assertEquals("cc.game.zombicide.android", appContext.getPackageName());
    }


    @Test
    public void testChess() {

        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String [] cmds = { "adb shell pm grant " + appContext.getPackageName() + " android.permission.READ_EXTERNAL_STORAGE",
                    "adb shell pm grant " + appContext.getPackageName() + " android.permission.WRITE_EXTERNAL_STORAGE" };

            for (String cmd : cmds) {
                Log.i("CHESS", "Shell CMD: " + cmd);
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd);
            }
        }

        int grantResult = ActivityCompat.checkSelfPermission(InstrumentationRegistry.getTargetContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Assert.assertEquals(PackageManager.PERMISSION_GRANTED, grantResult);
        grantResult = ActivityCompat.checkSelfPermission(InstrumentationRegistry.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Assert.assertEquals(PackageManager.PERMISSION_GRANTED, grantResult);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "chess.trace");
        file.mkdirs();

//        File file = new File(Environment.getExternalStorageDirectory(), "chess.trace");

        Log.i("CHESS", "Writing trace file to: " + file);

        Game game = new Game();
        game.setRules(new Chess());
        game.setPlayer(0, new AIPlayer());
        game.setPlayer(1, new AIPlayer());
        game.newGame();
        while (!game.isGameOver()) {
            Log.i("CHESS", "run game begin");
            Debug.startMethodTracing(file.getPath());
            game.runGame();
            Debug.stopMethodTracing();
            Log.i("CHESS", "run game end");
        }
    }

    @Test
    public void testChess2() {
        Game game = new Game();
        game.setRules(new Chess());
        game.setPlayer(0, new AIPlayer());
        game.setPlayer(1, new AIPlayer());
        game.newGame();
        List<Move> moves = game.getRules().computeMoves(game);
        for (Move m : moves) {
            Log.i("CHESS2", m.toString());
        }

    }

}