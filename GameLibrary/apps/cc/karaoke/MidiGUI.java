package cc.karaoke;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Vector;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


public class MidiGUI extends JFrame {
    //private GridLayout mgr = new GridLayout(3,1);
    private BorderLayout mgr = new BorderLayout();

    private PianoPanel pianoPanel;
    private MelodyPanel melodyPanel;

    private AttributedLyricPanel lyric1;
    private AttributedLyricPanel lyric2;
    private AttributedLyricPanel[] lyricLinePanels;
    private int whichLyricPanel = 0;

    private JPanel lyricsPanel = new JPanel();

    private Sequencer sequencer;
    private Sequence sequence;
    private Vector<LyricLine> lyricLines;

    private int lyricLine = -1;

    private boolean inLyricHeader = true;
    private Vector<DurationNote> melodyNotes;

    private Map<Character, String> pinyinMap;

    private int language;

    public MidiGUI(final Sequencer sequencer) {
        this.sequencer = sequencer;
        sequence = sequencer.getSequence();

        // get lyrics and notes from Sequence Info
        lyricLines = SequenceInformation.getLyrics();
        melodyNotes = SequenceInformation.getMelodyNotes();
        language = SequenceInformation.getLanguage();

        pianoPanel = new PianoPanel(sequencer);
        melodyPanel = new MelodyPanel(sequencer);

        pinyinMap = CharsetEncoding.loadPinyinMap();
        lyric1 = new AttributedLyricPanel(pinyinMap);
        lyric2 = new AttributedLyricPanel(pinyinMap);
        lyricLinePanels = new AttributedLyricPanel[]{
                lyric1, lyric2};

        Debug.println("Lyrics ");

        for (LyricLine line : lyricLines) {
            Debug.println(line.line + " " + line.startTick + " " + line.endTick +
                    " num notes " + line.notes.size());
        }

        getContentPane().setLayout(mgr);
	/*
	getContentPane().add(pianoPanel);
	getContentPane().add(melodyPanel);

	getContentPane().add(lyricsPanel);
	*/
        getContentPane().add(pianoPanel, BorderLayout.PAGE_START);
        getContentPane().add(melodyPanel, BorderLayout.CENTER);

        getContentPane().add(lyricsPanel, BorderLayout.PAGE_END);


        lyricsPanel.setLayout(new GridLayout(2, 1));
        lyricsPanel.add(lyric1);
        lyricsPanel.add(lyric2);
        setLanguage(language);

        setText(lyricLinePanels[whichLyricPanel], lyricLines.elementAt(0).line);

        Debug.println("First lyric line: " + lyricLines.elementAt(0).line);
        if (lyricLine < lyricLines.size() - 1) {
            setText(lyricLinePanels[(whichLyricPanel + 1) % 2], lyricLines.elementAt(1).line);
            Debug.println("Second lyric line: " + lyricLines.elementAt(1).line);
        }

        // handle window closing
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                sequencer.stop();
//                Debug.SCRIPT_WRITER.flush();
//                Debug.SCRIPT_WRITER.close();
//                Debug.LYRICS_WRITER.close();
//                System.exit(0);
            }
        });

        // handle resize events
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Debug.printf("Component has resized to width %d, height %d\n",
                        getWidth(), getHeight());
                // force resize of children - especially the middle MelodyPanel
                e.getComponent().validate();
            }

            public void componentShown(ComponentEvent e) {
                Debug.printf("Component is visible with width %d, height %d\n",
                        getWidth(), getHeight());
            }
        });

        setSize(1600, 900);
        setVisible(true);
    }

    public void setLanguage(int lang) {
        lyric1.setLanguage(lang);
        lyric2.setLanguage(lang);
    }


    /**
     * A lyric starts with a header section
     * We have to skip over that, but can pick useful
     * data out of it
     */

    /**
     * header format is
     * \@Llanguage code
     * \@Ttitle
     * \@Tsinger
     */

    static int page = 0;
    static int lastEnd = 0;
    static String lastTxt = "";
    static float lastTimeStamp = 0;

    public void setLyric(String txt, long tickPosition) {
        Debug.println("Setting lyric to " + txt);
        if (inLyricHeader) {
            if (txt.startsWith("@")) {
                Debug.println("Header: " + txt);
                return;
            } else {
                inLyricHeader = false;
            }
        }

        if (page == 0) {
            page++;
            Debug.sprintln("# TIMESTAMP | SINGER | PAGE | START | END");
            Debug.sprintln("# Here we go!\\n3 2 1");
            /*
            -4.00       | 0      | 1    | 0     | 0
-3.00       | 0      | 1    | 12    | 13
-2.00       | 0      | 1    | 15    | 16
             */
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", -4.0, 0, page, 0, 0);
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", -3.0, 0, page, 12, 13);
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", -2.0, 0, page, 14, 15);
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", -1.0, 0, page, 16, 17);
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n",  0.0, 0, 0, 0, 0);
            page++;
        }

        try {
            if ((lyricLine == -1) && (txt.charAt(0) == '\\')) {
                lyricLine = 0;
                colourLyric(lyricLinePanels[whichLyricPanel], txt.substring(1));
                // lyricLinePanels[whichLyricPanel].colourLyric(txt.substring(1));
                return;
            }

            // start of line
            if (txt.equals("\r\n") || (txt.charAt(0) == '/') || (txt.charAt(0) == '\\')) {
                page++;
                lastEnd = 0;
                lastTxt = "";

                if (lyricLine < lyricLines.size() - 1)
                    Debug.println("Setting next lyric line to \"" +
                            lyricLines.elementAt(lyricLine + 1).line + "\"");

                final int thisPanel = whichLyricPanel;
                whichLyricPanel = (whichLyricPanel + 1) % 2;

                Debug.println("Setting new lyric line at tick " +
                        sequencer.getTickPosition());

                lyricLine++;

                // if it's a \ r /, the rest of the txt should be the next  word to
                // be coloured

                if ((txt.charAt(0) == '/') || (txt.charAt(0) == '\\')) {
                    Debug.println("Colouring newline of " + txt);
                    colourLyric(lyricLinePanels[whichLyricPanel], txt.substring(1));
                }

                // Update the current line of text to show the one after next
                // But delay the update until 0.25 seconds after the next line
                // starts playing, to preserve visual continuity
                if (lyricLine + 1 < lyricLines.size()) {
            /*
              long startNextLineTick = lyricLines.elementAt(lyricLine).startTick;
              long delayForTicks = startNextLineTick - sequencer.getTickPosition();
              Debug.println("Next  current "  + startNextLineTick + " " + sequencer.getTickPosition());
              float microSecsPerQNote = sequencer.getTempoInMPQ();
              float delayInMicroSecs = microSecsPerQNote * delayForTicks / 24 + 250000L;
            */

                    final Vector<DurationNote> notes = lyricLines.elementAt(lyricLine).notes;

                    final int nextLineForPanel = lyricLine + 1;

                    if (lyricLines.size() >= nextLineForPanel) {
                        Timer timer = new Timer((int) 1000,
                                new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        if (nextLineForPanel >= lyricLines.size()) {
                                            return;
                                        }
                                        setText(lyricLinePanels[thisPanel], lyricLines.elementAt(nextLineForPanel).line);
                                        //lyricLinePanels[thisPanel].setText(lyricLines.elementAt(nextLineForPanel).line);

                                    }
                                });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        // no more lines
                    }
                }
            } else {
                Debug.println("Playing lyric " + txt);
                colourLyric(lyricLinePanels[whichLyricPanel], txt);
                //lyricLinePanels[whichLyricPanel].colourLyric(txt);
            }
        } finally {

            float timeStamp = (float) ((double) tickPosition / 1000);
            int start = lastEnd;
            if (txt.startsWith("/") || txt.startsWith("\\")) {
                txt = txt.substring(1);
                //if (lastTimeStamp >= 0)
                {
                    // insert a page with nothing highlighted before the new page
                    float midTS = (lastTimeStamp + timeStamp)/2;
                    Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", midTS, 0, page, 0, 0);
                }
            }
            int end = start + txt.length();
            Debug.sprintln("# " + lastTxt + txt);
            Debug.sprintf("%-11.2f | %-6d | %-4d | %-5d | %-3d\n", timeStamp, 0, page, start, end);
            lastTxt += txt;
            lastEnd = end;
            lastTimeStamp = timeStamp;
        }
    }

    /**
     * colour the lyric of a panel.
     * called by one thread, makes changes in GUI thread
     */
    private void colourLyric(final AttributedLyricPanel p, final String txt) {
        SwingUtilities.invokeLater(new Runnable() {
                                       public void run() {
                                           Debug.print("Colouring lyric \"" + txt + "\"");
                                           if (p == lyric1) Debug.println(" on panel 1");
                                           else Debug.println(" on panel 2");
                                           p.colourLyric(txt);
                                       }
                                   }
        );
    }

    /**
     * set the lyric of a panel.
     * called by one thread, makes changes in GUI thread
     */
    private void setText(final AttributedLyricPanel p, final String txt) {
        SwingUtilities.invokeLater(new Runnable() {
                                       public void run() {
                                           Debug.println("Setting text \"" + txt + "\"");
                                           if (p == lyric1) Debug.println(" on panel 1");
                                           else Debug.println(" on panel 2");
                                           p.setText(txt);
                                       }
                                   }
        );
    }

    public void setNote(long timeStamp, int onOff, int note) {
        Debug.printf("Setting note in gui to %d\n", note);

        if (onOff == Constants.MIDI_NOTE_OFF) {
            pianoPanel.drawNoteOff(note);
        } else if (onOff == Constants.MIDI_NOTE_ON) {
            pianoPanel.drawNoteOn(note);
        }
    }
}


