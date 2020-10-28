package cc.audio.stream.server;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.DecoderConfig;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.adts.ADTSDemultiplexer;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioServer {

    public static void main(String [] args) throws Exception {

        File aacFile = new File("resources/surfin_usa.aac");

        int port = 31110;
        DatagramSocket socket = new DatagramSocket();
        InetAddress listener = InetAddress.getByName("localhost");

        try (InputStream in = new FileInputStream(aacFile)) {
            final ADTSDemultiplexer adts = new ADTSDemultiplexer(in);
            Decoder dec = new Decoder(adts.getDecoderSpecificInfo());

            final SampleBuffer buf = new SampleBuffer();
            byte[] b;
            while (true) {
                b = adts.readNextFrame();
                dec.decodeFrame(b, buf);
                DecoderConfig config = dec.getConfig();

                DatagramPacket packet = new DatagramPacket(b, 0, b.length, listener, port);
                System.out.println("Send " + b.length + " bytes frame");
                socket.send(packet);
            }
        } catch (EOFException e) {
            // file read completely
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.close();
    }

}
