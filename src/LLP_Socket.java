import java.io.IOException;
import java.net.*;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Socket {
    private static final int MAX_WINDOW_SIZE = 1024;
    private LLP_Packet[] send_buffer;
    private LLP_Packet[] receive_buffer;
    private int sendSize;
    private int receiveSize;
    private int windowSize;
    private DatagramSocket socket;
    private int localSN;
    private int remoteSN;


    public LLP_Socket() throws IllegalArgumentException {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        send_buffer = new LLP_Packet[MAX_WINDOW_SIZE];
        receive_buffer = new LLP_Packet[MAX_WINDOW_SIZE];
        sendSize = 0;
        receiveSize = 0;
        windowSize = 50; // default window size, unless initialized by the application
        localSN = 0;
    }

    public void connect(InetAddress address, int port) {
        //send syn
        socket.connect(address, port);
        byte[] header = createHeader();
        socket.send(new DatagramPacket())
        //receive syn/ack and initialize remote SN
        //send ack
    }

    public LLP_Socket accept() {
        // not in udp, we must implement
        // receive syn and initialize remote sequence number
        // send syn/ack
        // receive ack
        return null;
    }

    public void bind(SocketAddress address) {
        try {
            socket.bind(address);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        socket.close();
    }

    public boolean isSendBufferFull() {
        return send_buffer.length == sendSize + 1;
    }

    public LLP_Packet receive(int readSize) {
        // checksum (corrupt?)
        //timeout (lost?)
        //out of order received pckt ignored
        // successful?
        // store the packet without the header
        //return received backets
        return null;
    }

    public void send(LLP_Packet packet) {
        // LLP header
        //
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public byte[] createHeader() {

    }
}
