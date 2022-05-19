package cc.audio.stream.server;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class AudioClient {

    public static void main(String [] args) throws Exception {

        int port = 31110;
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(30000);
        InetAddress listener = InetAddress.getByName("localhost");
        //MP4Container container = new MP4Container(new DatagramInputReader(socket));



        AudioInputStream stream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(new DatagramInputReader(socket)));

        new AudioPlayer().play(stream);
    }

    static class AudioPlayer implements LineListener {
        boolean playCompleted = false;

        void play(AudioInputStream stream) throws Exception {
            AudioFormat format = stream.getFormat();
            System.out.println("Format: " + format);

            DataLine.Info info = new DataLine.Info(Clip.class, format);

            Clip audioClip = (Clip) AudioSystem.getLine(info);

            audioClip.addLineListener(this);

            audioClip.open(stream);

            audioClip.start();

            while (!playCompleted) {
                // wait for the playback completes
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }

            audioClip.close();
        }

        @Override
        public void update(LineEvent event) {
            LineEvent.Type type = event.getType();

            if (type == LineEvent.Type.START) {
                System.out.println("Playback started.");

            } else if (type == LineEvent.Type.STOP) {
                playCompleted = true;
                System.out.println("Playback completed.");
            }
        }
    }

    static class DatagramInputReader extends InputStream {

        byte [] buffer = new byte[1024];
        DatagramPacket packet;
        DatagramSocket socket;
        int numBytes = 0;

        DatagramInputReader(DatagramSocket socket) {
            this.socket = socket;
            packet = new DatagramPacket(buffer, buffer.length);
        }

        @Override
        public int read() throws IOException {
            if (numBytes == 0) {
                socket.receive(packet);
                numBytes = packet.getLength();
                System.out.println("Read " + numBytes + " of data");
                if (numBytes <= 0)
                    throw new EOFException("No more data");
            }
            return buffer[--numBytes];
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public long skip(long n) throws IOException {
            return super.skip(n);
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
        }
    }
}
