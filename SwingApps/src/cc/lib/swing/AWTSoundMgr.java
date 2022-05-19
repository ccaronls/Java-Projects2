package cc.lib.swing;

import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

public class AWTSoundMgr {

	private AWTSoundMgr() {}
	
    /**
     * 
     * @param file_name
     * @return
     */
    public static int loadAudio(String file_name)
    {
        try {
            // From file
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file_name));
        
            // At present, ALAW and ULAW encodings must be converted
            // to PCM_SIGNED before it can be played
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(),
                        format.getSampleSizeInBits()*2,
                        format.getChannels(),
                        format.getFrameSize()*2,
                        format.getFrameRate(),
                        true);        // big endian
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
        
            // Create the clip
            DataLine.Info info = new DataLine.Info(
                Clip.class, stream.getFormat(), ((int)stream.getFrameLength()*format.getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
        
            // This method does not return until the audio file is completely loaded
            clip.open(stream);
        
            sounds.add(clip);
            return sounds.size()-1;
        } catch (Exception e) {
        	System.err.println("EXCEPTION " + e + " caught loading sounds " + file_name);
        } 
        
        return -1;
    }
    
    /**
     * 
     * @param id
     */
    public static void playSound(int id) {
    	if (id<0)
    		return;
    	Clip clip = (Clip)sounds.get(id);
    	if (clip != null)
    		clip.start();
    }
    
    private static ArrayList<Clip> sounds = new ArrayList<Clip>(32);
}
