import java.io.IOException;
import java.net.*;
import java.io.Closeable;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Socket {
    private static final int MAX_WINDOW_SIZE = 1024;
    private LLP_Packet[] send_buffer;
    private LLP_Packet[] receive_buffer;
    private int windowSize;
    private DatagramSocket socket;


    public LLP_Socket(String type) throws IllegalArgumentException {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        send_buffer = new LLP_Packet[MAX_WINDOW_SIZE];
        receive_buffer = new LLP_Packet[MAX_WINDOW_SIZE];
        windowSize = 50; // default window size, unless initialized by the application
    }

    public void connect(InetAddress address, int port) {

    }

    public LLP_Socket accept() {
        // not in udp, we must implement
        return null;
    }

    public void bind(SocketAddress address) {

    }

    public void close() {

    }

    public boolean isBufferFull() {
        return false;
    }

    public void receive(LLP_Packet packet) {


    }

    public void send(LLP_Packet packet) {

    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }
}
