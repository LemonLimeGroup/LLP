import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Socket {
    private static final int MAX_WINDOW_SIZE = 20480;
    private static final int MAX_DATA_SIZE = 1024;
    private byte[] send_buffer;
    private byte[] receive_buffer;
    private int sendSize;
    private int receiveSize;
    private int windowSize;
    private DatagramSocket socket;
    private DatagramSocket connectionSocket; // TODO: Might be necessary for accept() to work properly
    private int localSeq;
    private int remoteSeq;
    private InetAddress destAddress;
    private int destPort;

    public LLP_Socket() throws IllegalArgumentException {
       this(-1, null);
    }


    public LLP_Socket(int localPort) throws IllegalArgumentException {
       this(localPort, null);
    }

    public LLP_Socket(int localPort, InetAddress address) {
        boolean isSuccessful = false;
        try {
            socket = new DatagramSocket(null);
            while (!isSuccessful) {
                try {
                    socket.setReuseAddress(true);
                    isSuccessful = true;
                } catch (SocketException e) {
                    System.out.println("Failed to make address reusable. Retrying...");
                }
            }
            if (localPort != -1 && address == null) {
                socket.bind(new InetSocketAddress(localPort));
            } else if (localPort != -1 && address != null){
                socket.bind(new InetSocketAddress(address, localPort));
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to create a socket");
            return;
        }

        send_buffer = new byte[MAX_WINDOW_SIZE];
        receive_buffer = new byte[MAX_WINDOW_SIZE];
        sendSize = 0;
        receiveSize = 0;
        windowSize = 50; // default window size, unless initialized by the application
        localSeq = 0;
    }

    /**
     * Connect to remote address at specified InetAddress.
     * Initiates a three-way handshake with teh server.
     * Local and remote sequence numbers initialized.
     *
     * @param address Address of remote host.
     * @param port Port of remote host.
     */
    public void connect(InetAddress address, int port) {
        // Send SYN
        System.out.println("SENDING SYN TO SERVER");
        LLP_Packet synPacketLLP = new LLP_Packet(localSeq, 0, 0, windowSize);
        synPacketLLP.setSYNFlag(true);
        byte[] synData = synPacketLLP.toArray();

        DatagramPacket synPacket = new DatagramPacket(synData, synData.length, address, port);
        ensureSend(synPacket);

        // Receive SYN-ACK & initialize remote Seq Num
        System.out.println("WAITING FOR SYN-ACK FROM SERVER");
        byte[] receiveData = new byte[MAX_DATA_SIZE]; // based on max data size
        DatagramPacket recvSynAckPacket = new DatagramPacket(receiveData, receiveData.length);
        ensureRcv(recvSynAckPacket); // TODO: Check sequence number etc. ; window allocation done around here

        byte[] synAckArray = recvSynAckPacket.getData();
        //TODO: get remote sequence numbere
        // Send ACK
        System.out.println("SENDING ACK TO SERVER");
        LLP_Packet ackPacketLLP = new LLP_Packet(1, 1, 0, windowSize); // TODO: compute these values instead of hardcoded
        ackPacketLLP.setACKFlag(true);
        byte[] ackData = ackPacketLLP.toArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        ensureSend(ackPacket);
        // TODO: update seq numbers

        setDestAddress(address);
        setDestPort(port);
        socket.connect(address, port); // TODO: probably should be used somewhere
        System.out.println("CONNECTION ESTABLISHED");
        // Connection established completed for client-side
    }

    /**
     * Accepts a new connection.
     * Handles server-side semantics of the three-way handshake.
     *
     * @return socket
     */
    public LLP_Socket accept() {
        byte[] receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket receiveSYN = new DatagramPacket(receiveData, receiveData.length);
        // Receive SYN
        System.out.println("WAITING FOR SYN FROM CLIENT");
        // TODO: Check that packet has SYN, otherwise need to switch states / wait;
        ensureRcv(receiveSYN);

        System.out.println("SEND A SYN-ACK TO CLIENT");
        // Send SYN-ACK
        LLP_Packet synAckPacketLLP = new LLP_Packet(0, 1, 0, windowSize); // TODO: compute window size
        synAckPacketLLP.setACKFlag(true);
        synAckPacketLLP.setSYNFlag(true);
        byte[] sendSYNACKData = synAckPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendSYNACKData, sendSYNACKData.length,
                receiveSYN.getAddress(), receiveSYN.getPort());
        ensureSend(sendPacket);

        // Receive ACK
        receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket receiveACK = new DatagramPacket(receiveData, receiveData.length);
        ensureRcv(receiveACK);
        System.out.println("CONNECTION ACCEPTED");
        LLP_Socket retSocket = new LLP_Socket(socket.getLocalPort());
        // TODO: parse ACK and Seq Number
        return retSocket; // TODO: Not sure what to return -- tcp normally returns connection socket
    }

    // TODO: This might be done with DatagramSocket()
    public void bind(SocketAddress address) {
        try {
            socket.bind(address);
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to bind");
            System.exit(-1);
        }
    }

    public void close() {
        byte[] receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(receiveData, receiveData.length);
        // Send FIN
        System.out.println("SENDING FIN");
        LLP_Packet finPacketLLP = new LLP_Packet(localSeq, remoteSeq+1, 0, windowSize);
        finPacketLLP.setFINFlag(true);
        byte[] finArray = finPacketLLP.toArray();

        DatagramPacket finPacket = new DatagramPacket(finArray, finArray.length, socket.getRemoteSocketAddress());
        ensureSend(finPacket);

        // Receive either FIN or ACK
        System.out.println("FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.");
        ensureRcv(recvPacket);

        byte[] trimmedRcvData = new byte[recvPacket.getLength()]; // I think getLength() returns actual packet size received
        System.arraycopy(receiveData, 0, trimmedRcvData, 0, trimmedRcvData.length);
        LLP_Packet recvPacktLLP = LLP_Packet.parsePacket(trimmedRcvData);

        if (recvPacktLLP.getACKFlag() == 1) {
            //TODO FIN-WAIT-2
            System.out.println("ACK received. Waiting for FIN.");
            receiveData = new byte[MAX_DATA_SIZE];
            recvPacket = new DatagramPacket(receiveData, receiveData.length);
            ensureRcv(recvPacket);

            //TODO? Check if FIN
            System.out.println("Received FIN. Sending ACK.");
            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, remoteSeq+1, 0, windowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            ensureSend(ackPacket);
        } else if (recvPacktLLP.getFINFlag() == 1) {
            System.out.println("FIN received. Sending ACK");
            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, remoteSeq+1, 0, windowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            ensureSend(ackPacket);

            System.out.println("Closing State. Waiting for ACK");
            receiveData = new byte[MAX_DATA_SIZE]; // based on max data size
            recvPacket = new DatagramPacket(receiveData, receiveData.length);

            ensureRcv(recvPacket);

            //TODO? Check if it is ACK
        }
        //TODO: Timed-Wait or no?
        System.out.println("Timed-Wait State.");
        socket.close();
    }

    public boolean isSendBufferFull() {
        return send_buffer.length == sendSize + 1;
    }

    public byte[] receive(int maxSize) {
        // checksum (corrupt?)
        // timeout (lost?)
        // out of order received pckt ignored
        // successful?
        // store the packet without the header
        // return received packets
        byte[] rawReceiveData = new byte[maxSize];
        DatagramPacket receivePacket = new DatagramPacket(rawReceiveData, rawReceiveData.length);
        ensureRcv(receivePacket);

        // create new byte array without the empty unused buffer
        byte[] receiveData = new byte[receivePacket.getLength()];
        System.arraycopy(rawReceiveData, 0, receiveData, 0, receiveData.length);
        LLP_Packet receivedLLP = LLP_Packet.parsePacket(receiveData);

        System.out.println("RECEIVED DATA");
        return receivedLLP.getData();
    }

    public void send(byte[] data) {
        // LLP header
        LLP_Packet sendPacketLLP = new LLP_Packet(localSeq, ++remoteSeq, 0, windowSize); // TODO: compute window size & seq numbers
        sendPacketLLP.setData(data);
        byte[] sendData = sendPacketLLP.toArray();
        System.out.println(destAddress);
        System.out.println(socket.getRemoteSocketAddress());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
        ensureSend(sendPacket);
        System.out.println("SENT DATA");
    }

    public byte[] getInputStream() {
        return receive_buffer;
    }

    public byte[] getOutputStream() {
        return send_buffer;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public void setDestAddress(InetAddress address) {
        this.destAddress = address;
    }

    public void setDestPort(int port) {
        this.destPort = port;
    }

    //HELPER METHODS
    private void ensureSend(DatagramPacket sendPckt) {
        boolean isSuccessful = false;
        while (!isSuccessful) {
            try {
                socket.send(sendPckt);
                isSuccessful = true;
            } catch (IOException e) {
                System.out.println("Failed to send. Retrying...");
            }
        }
    }
    private void ensureRcv(DatagramPacket recvPckt) {
        boolean isSuccessful = false;
        while (!isSuccessful) {
            try {
                socket.receive(recvPckt);
                isSuccessful = true;
            } catch (IOException e) {
                System.out.println("Failed to receive. Retrying...");
            }
        }
    }
}

