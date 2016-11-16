import java.io.IOException;
import java.net.*;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Socket {
    private static final int MAX_WINDOW_SIZE = 20480;
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
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to create a socket");
            System.exit(-1);
        }
        send_buffer = new byte[MAX_WINDOW_SIZE];
        receive_buffer = new byte[MAX_WINDOW_SIZE];
        sendSize = 0;
        receiveSize = 0;
        windowSize = 50; // default window size, unless initialized by the application
        localSeq = 0;
    }


    public LLP_Socket(int localPort) throws IllegalArgumentException {
        try {
            socket = new DatagramSocket(localPort);
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to create a socket");
            System.exit(-1);
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
        try {
            socket.send(synPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Receive SYN-ACK & initialize remote Seq Num
        System.out.println("WAITING FOR SYN-ACK FROM SERVER");
        byte[] receiveData = new byte[1024]; // based on max data size
        DatagramPacket recvSynAckPacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            socket.receive(recvSynAckPacket); // TODO: Check sequence number etc. ; window allocation done around here
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] synAckArray = recvSynAckPacket.getData();
        //TODO: get remote sequence number
        // Send ACK
        System.out.println("SENDING ACK TO SERVER");
        LLP_Packet ackPacketLLP = new LLP_Packet(1, 1, 0, windowSize); // TODO: compute these values instead of hardcoded
        ackPacketLLP.setACKFlag(true);
        byte[] ackData = ackPacketLLP.toArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        try {
            socket.send(ackPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Receive SYN
        System.out.println("WAITING FOR SYN FROM CLIENT");
        byte[] receiveData = new byte[1024];
        // TODO: Check that packet has SYN, otherwise need to switch states / wait
        DatagramPacket receiveSYN = new DatagramPacket(receiveData, receiveData.length);
        boolean isSuccessful = false;
        while (!isSuccessful) {
            try {
                //TODO: timeout?
                socket.receive(receiveSYN);
                isSuccessful = true;
            } catch (IOException e) {
                System.out.println("Did not receive packet. Trying again");
            } // TODO: Initialize remote sequence number

        }

        System.out.println("SEND A SYN-ACK TO CLIENT");
        // Send SYN-ACK
        LLP_Packet synAckPacketLLP = new LLP_Packet(0, 1, 0, windowSize); // TODO: compute window size
        synAckPacketLLP.setACKFlag(true);
        synAckPacketLLP.setSYNFlag(true);
        byte[] sendSYNACKData = synAckPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendSYNACKData, sendSYNACKData.length,
                receiveSYN.getAddress(), receiveSYN.getPort());
        isSuccessful = false;
        while(!isSuccessful) {
            try {
                socket.send(sendPacket);
                isSuccessful = true;
            } catch (IOException e) {
                System.out.println("Sending packet wasn't successful. Trying again.");
            }

        }

        // Receive ACK
        byte[] receiveAckData = new byte[1024];
        DatagramPacket receiveACK = new DatagramPacket(receiveAckData, receiveAckData.length);
        isSuccessful = false;
        while (!isSuccessful) {
            try {
                socket.receive(receiveACK);
                isSuccessful = true;
            } catch (IOException e) {
                System.out.println("Receiving ACK failed. Trying again")
            }
        }
        System.out.println("CONNECTION ACCEPTED");
        // TODO: parse ACK and Seq Number
        return null; // TODO: Not sure what to return -- tcp normally returns connection socket
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
        try {
            socket.receive(receivePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}

