package cc.karaoke;

/*
 * KaraokePlayer.java
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import cc.lib.utils.FileUtils;

public class KaraokePlayer {


    public static void main(String[] args) throws Exception {
        try {
            if (args.length != 1) {
                System.err.println("KaraokePlayer: usage: " +
                        "KaraokePlayer <midifile>");
                System.exit(1);
            }
            String strFilename = args[0];
            File source = new File(strFilename);
            if (!source.exists()) {
                System.err.println("Unknown file '" + source + "' not found in directory: " + new File(".").getAbsolutePath());
                System.exit(1);
            }

            File assetsDirectory = new File("/Users/chriscaron/Documents/fisker/project-f1-android/app/src/main/assets");
            if (!assetsDirectory.isDirectory()) {
                System.err.println("Cannot find directory: " + assetsDirectory);
                Debug.SCRIPT_WRITER = new PrintWriter(System.out);
                Debug.LYRICS_WRITER = new PrintWriter(System.out);
            } else {
                String fileName = FileUtils.stripExtension(source.getName());
                Debug.SCRIPT_WRITER = new PrintWriter(new FileWriter(new File(assetsDirectory, fileName + "_script.txt")));
                Debug.LYRICS_WRITER = new PrintWriter(new FileWriter(new File(assetsDirectory, fileName + "_lyrics.json")));
            }
            MidiPlayer midiPlayer = new MidiPlayer();
            midiPlayer.playMidiFile(strFilename);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}



