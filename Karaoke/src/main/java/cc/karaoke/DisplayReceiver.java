package cc.karaoke;

/**
 * DisplayReceiver
 * <p>
 * Acts as a Midi receiver to the default Java Midi sequencer.
 * It collects Midi events and Midi meta messages from the sequencer.
 * these are handed to a UI object for display.
 * <p>
 * The current UI object is a MidiGUI but could be replaced.
 */

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;

public class DisplayReceiver implements Receiver,
        MetaEventListener {
    private MidiGUI gui;
    private Sequencer sequencer;
    private int melodyChannel = SequenceInformation.getMelodyChannel();

    public DisplayReceiver(Sequencer sequencer) {
        this.sequencer = sequencer;
        gui = new MidiGUI(sequencer);
    }

    public void close() {
        Debug.println("Closing");
    }

    /**
     * Called by a Transmitter to receive events
     * as a Receiver
     */
    public void send(MidiMessage msg, long timeStamp) {
        // Note on/off messages come from the midi player
        // but not meta messages

        if (msg instanceof ShortMessage) {
            ShortMessage smsg = (ShortMessage) msg;


            String strMessage = "Channel " + smsg.getChannel() + " ";

            switch (smsg.getCommand()) {
                case Constants.MIDI_NOTE_OFF:
                    strMessage += "note Off [" + smsg.getData1() + "] " +
                            getKeyName(smsg.getData1()) + " " + timeStamp;
                    break;

                case Constants.MIDI_NOTE_ON:
                    strMessage += "note On [" + smsg.getData1() + "] " +
                            getKeyName(smsg.getData1()) + " " + timeStamp;
                    break;
            }
            Debug.println(strMessage);
            if (smsg.getChannel() == melodyChannel) {
                gui.setNote(timeStamp, smsg.getCommand(), smsg.getData1());
            }

        }
    }


    public void meta(MetaMessage msg) {
        Debug.println("Reciever got a meta message");
        if (((MetaMessage) msg).getType() == Constants.MIDI_TEXT_TYPE) {
            setLyric((MetaMessage) msg);
        } else if (((MetaMessage) msg).getType() == Constants.MIDI_END_OF_TRACK) {
            Debug.SCRIPT_WRITER.flush();
            Debug.SCRIPT_WRITER.close();
            Debug.LYRICS_WRITER.close();
            System.exit(0);
        }
    }

    public void setLyric(MetaMessage message) {
        byte[] data = message.getData();
        String str = new String(data);
        Debug.println("Lyric +\"" + str + "\" at " + sequencer.getTickPosition());
        gui.setLyric(str, sequencer.getMicrosecondPosition()/1000);

    }

    private static String[] keyNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static String getKeyName(int keyNumber) {
        if (keyNumber > 127) {
            return "illegal value";
        } else {
            int note = keyNumber % 12;
            int octave = keyNumber / 12;
            return keyNames[note] + (octave - 1);
        }
    }

}
