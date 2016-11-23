import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Socket {
    //FINAL VARIABLES
    private static final int MAX_DATA_SIZE = 1012;
    private static final int TIMER = 500;
    //RECEIVE
    private byte[] receive_buffer;
    private int receiveSize;
    private int windowSize;
    private int remoteSeq;
    private int expectedSeqNum;
    //SEND
    private byte[] send_buffer;
    private int sendSize;
    private int localSeq;
    private int waitingForAck;
    //OTHER
    private DatagramSocket socket;
    private InetAddress destAddress;
    private int destPort;
    boolean debug;

    public LLP_Socket(boolean debug) throws IllegalArgumentException {
       this(-1, null, debug);
    }


    public LLP_Socket(int localPort, boolean debug) throws IllegalArgumentException {
       this(localPort, null, debug);
    }
    public LLP_Socket(int localPort, InetAddress address, boolean debug) {
        this(localPort, address, null, debug);
    }

    public LLP_Socket(int localPort, InetAddress localAddress, SocketAddress remoteAddress, boolean debug) {
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
            if (localPort != -1 && localAddress == null) {
                socket.bind(new InetSocketAddress(localPort));
            } else if (localPort != -1 && localAddress != null){
                socket.bind(new InetSocketAddress(localAddress, localPort));
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to create a socket");
            System.exit(-1); //TODO: shouldn't we try to make socket again?
        }
        if (remoteAddress != null) {
            isSuccessful = false;
            while (!isSuccessful) {
                try {
                    socket.connect(remoteAddress);
                    isSuccessful = true;
                } catch (SocketException e) {
                    System.out.println("Failed to connect. Retrying...");

                }
            }
        }
        this.debug = debug;
        send_buffer = new byte[windowSize];
        receive_buffer = new byte[windowSize];
        sendSize = 0;
        receiveSize = 0;
        windowSize = 50; // default window size, unless initialized by the application
        localSeq = 0;
        waitingForAck = 0;
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
        //TODO: checksum?
        LLP_Packet synPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize);
        synPacketLLP.setSYNFlag(true);
        byte[] synData = synPacketLLP.toArray();

        DatagramPacket synPacket = new DatagramPacket(synData, synData.length, address, port);

        while (timeoutRcv("SYN_ACK", synPacket)) {
            ensureSend(synPacket);

            // Receive SYN-ACK & initialize remote Seq Num
            printDebug("WAITING FOR SYN-ACK FROM SERVER");
            // TODO: Check sequence number etc. ; window allocation done around here
        }
        //TODO: get remote sequence number

        // Send ACK
        LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize); // TODO: compute these values instead of hardcoded
        ackPacketLLP.setACKFlag(true);
        byte[] ackData = ackPacketLLP.toArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        do {
            System.out.println("SENDING ACK TO SERVER");
            ensureSend(ackPacket);
        } while (!timeoutRcv("SYN_ACK", synPacket));
        // TODO: update seq numbers

        setDestAddress(address);
        setDestPort(port);
        socket.connect(address, port);
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

        printDebug("WAITING FOR SYN FROM CLIENT");
        // TODO: Check that packet has SYN, otherwise need to switch states / wait;
        DatagramPacket receiveSYN = ensureRcvdFlag("SYN", null);
        if (receiveSYN == null) {
            printDebug("SYN is null");
        }
        LLP_Packet receiveSYNLLP = LLP_Packet.parsePacket(receiveSYN.getData());
        remoteSeq = receiveSYNLLP.getSeqNum();

        LLP_Packet synAckPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize); // TODO: compute window size
        synAckPacketLLP.setACKFlag(true);
        synAckPacketLLP.setSYNFlag(true);
        byte[] sendSYNACKData = synAckPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendSYNACKData, sendSYNACKData.length,
                receiveSYN.getAddress(), receiveSYN.getPort());
        while (timeoutRcv("ACK", receiveSYN)) {
            printDebug("SEND A SYN-ACK TO CLIENT");
            // Send SYN-ACK
            ensureSend(sendPacket);

            // Receive ACK
            printDebug("Receive ACK from Client.");
        }
        System.out.println("CONNECTION ACCEPTED");
        LLP_Socket retSocket = new LLP_Socket(socket.getLocalPort(), socket.getLocalAddress(), receiveSYN.getSocketAddress(), debug);
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
        printDebug("SENDING FIN");

        LLP_Packet finPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize);
        finPacketLLP.setFINFlag(true);
        byte[] finArray = finPacketLLP.toArray();

        DatagramPacket finPacket = new DatagramPacket(finArray, finArray.length, socket.getRemoteSocketAddress());
        ensureSend(finPacket);

        // Receive either FIN or ACK
        printDebug("FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.");

        ensureRcv(recvPacket);

        byte[] trimmedRcvData = new byte[recvPacket.getLength()]; // I think getLength() returns actual packet size received
        System.arraycopy(receiveData, 0, trimmedRcvData, 0, trimmedRcvData.length);
        LLP_Packet recvPacktLLP = LLP_Packet.parsePacket(trimmedRcvData);

        System.out.println("finpacket address:" + finPacket.getSocketAddress());
        if (recvPacktLLP.getACKFlag() == 1) {
            //FIN-WAIT-2
            printDebug("ACK received. Waiting for FIN.");
            ensureRcvdFlag("FIN", finPacket);
            printDebug("Received FIN. Sending ACK.");

            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            ensureSend(ackPacket);
        } else if (recvPacktLLP.getFINFlag() == 1) {
            printDebug("FIN received. Sending ACK");
            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, windowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            ensureSend(ackPacket);

            printDebug("Closing State. Waiting for ACK");

            ensureRcvdFlag("ACK", finPacket);
        }
        //TODO: Timed-Wait or no?
        printDebug("Timed-Wait State.");
        socket.close();
    }

    public boolean isSendBufferFull() {
        return send_buffer.length == sendSize + 1;
    }

    public byte[] receive(int maxSize) {
        // TODO: discuss whether the client should be able to specify a receive size

        byte[] rawReceiveData = new byte[maxSize];
        DatagramPacket receivePacket = new DatagramPacket(rawReceiveData, rawReceiveData.length);
        if (!ensureRcv(receivePacket)) {
            return "timeout".getBytes();
        }

        // create new byte array without the empty unused buffer
        byte[] receiveData = new byte[receivePacket.getLength()];
        System.arraycopy(rawReceiveData, 0, receiveData, 0, receiveData.length);
        LLP_Packet receivedLLP = LLP_Packet.parsePacket(receiveData);

        System.out.println("Received: " + new String(receiveData));
        if (whichFlag(receivedLLP).equals("FIN")) {
            recvdClose(receivePacket);
            //TODO: Return something else?
            return null;
        }

        System.out.println("REMOTE SEQ " + this.expectedSeqNum);
        if (!receivedLLP.isValidChecksum() || receivedLLP.getSeqNum() != this.expectedSeqNum) {
            System.out.println("CHECKSUM " + receivedLLP.isValidChecksum());
            System.out.println("PACKET DISCARDED: SEQ EXPECTED: " + this.expectedSeqNum + " RECEIVED: " + receivedLLP.getSeqNum());

            LLP_Packet ackPacket = new LLP_Packet(this.localSeq, this.expectedSeqNum, 0, windowSize);
            ackPacket.setACKFlag(true);
            byte[] sendData = ackPacket.toArray();

            DatagramPacket ack = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
            ensureSend(ack);
            return new byte[0]; // TODO: Returning empty array means client will have to check if null is returned -- is this what we want?
        } else {
            printDebug("RECEIVED DATA: EXPECTED SEQ: " + this.expectedSeqNum + "RECEIVED SEQ " + receivedLLP.getSeqNum());

            LLP_Packet ackPacket = new LLP_Packet(this.localSeq, ++this.expectedSeqNum, 0, windowSize);
            ackPacket.setACKFlag(true);
            byte[] sendData = ackPacket.toArray();

            DatagramPacket ack = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
            ensureSend(ack);
            String reStr = new String(receivedLLP.getData());

            printDebug("reStr: " + reStr);
            return receivedLLP.getData();
        }
    }

    public void send(byte[] data) throws SocketException {
        // Data to be sent
        byte[] fileBuff = data;

        // Seq num of last ACKed packet

        // Sequence number of the last packet
        int startSeqNum = this.localSeq;
        int lastNumPackets = ((int) Math.ceil((double) fileBuff.length / MAX_DATA_SIZE));
        System.out.println("LOCAL SEQ NUM " + this.localSeq);
        // Seq Num of Last Sent Packet
//        this.localSeq = 0;


        // Map to store unAcknowledged packets
        Map<Integer, DatagramPacket> sentPackets = new HashMap<>();

        // While window not full and there is more data to send:
        while(true) {
            while (this.localSeq - waitingForAck < windowSize && this.localSeq < lastNumPackets+startSeqNum) {

                // Store packet data to send
                byte[] sendPacketBytes = null;
                int startPos = (this.localSeq-startSeqNum)*MAX_DATA_SIZE;
                if (startPos + MAX_DATA_SIZE > fileBuff.length) {
                    sendPacketBytes = Arrays.copyOfRange(fileBuff, startPos, fileBuff.length); // ensure last packet has no nulls
                } else {
                    sendPacketBytes = Arrays.copyOfRange(fileBuff, startPos, startPos + MAX_DATA_SIZE);
                }

                LLP_Packet sendPacketLLP = new LLP_Packet(this.localSeq, ++remoteSeq, 0, windowSize);
                sendPacketLLP.setData(sendPacketBytes);
                byte[] sendData = sendPacketLLP.toArray();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destAddress, destPort);

                ensureSend(sendPacket);
                sentPackets.put(this.localSeq, sendPacket);

                System.out.println("SENT DATA " + this.localSeq);

                this.localSeq++;
                // TODO: Accept ACKs for sent packets

                // TODO: Have timer
            }

            // Receive Acks
            byte[] ackBytes = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length);

            if (timeoutRcv("", ackPacket)) {
                System.out.println("RE TRANSMISSION");

                // Re-send Packets
                for (int i = waitingForAck; i < this.localSeq; i++) {
                    DatagramPacket resendPacket = sentPackets.get(i);
                    System.out.println("i: " + i + "resendPacket: " + resendPacket);
                    ensureSend(resendPacket);
                }
            } else {
                printDebug("packet received: " + new String(ackPacket.getData()));
                LLP_Packet ackLLP = LLP_Packet.parsePacket(ackPacket.getData());
                String flag = whichFlag(ackLLP);
                if (flag.equals("ACK")) {
                    System.out.println("RECEIVED ACK: " + ackLLP.getAckNum());
                    System.out.println("LAST SEQ NUM " + lastNumPackets);
                    waitingForAck = Math.max(waitingForAck, ackLLP.getAckNum());
                    // exit loop if last packet
                    if (ackLLP.getAckNum() == lastNumPackets + startSeqNum) {
                        System.out.println("EXITING");
                        return;
                    }
                } else if (flag.equals("FIN")){
                    System.out.println("hello");
                    throw new SocketException("Server closed.");
                    // TODO: Handle this case
                }
            }

        }

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
    public void closeServer() {
        socket.close();
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
    private boolean ensureRcv (DatagramPacket recvPckt) {
        boolean isSuccessful = false;
        while (!isSuccessful) {
            try {
                socket.receive(recvPckt);
                isSuccessful = true;
                return isSuccessful;
            } catch (SocketTimeoutException e) {
                return false;
            } catch (IOException e) {
                System.out.println("Failed to receive. Retrying...");
            }
        }
        return isSuccessful;
    }
    /**
     * Right side of State diagram when trying to close
     * Called when either side receives FIN from the other end point
     * */
    private void recvdClose(DatagramPacket receivedPacket) {
        //Received FIN. Sending ACK
        printDebug("Received FIN. Trying to send ACK");

        LLP_Packet sendPacketLLP = new LLP_Packet(localSeq, remoteSeq, 0, windowSize);
        sendPacketLLP.setACKFlag(1);
        byte[] sendData = sendPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getRemoteSocketAddress());
        ensureSend(sendPacket);

        printDebug("Successfully sent ACK. In CLOSE-WAIT state.");
        printDebug("Sending FIN");

        sendPacketLLP = new LLP_Packet(localSeq, remoteSeq, 0, windowSize);
        sendPacketLLP.setFINFlag(1);
        sendData = sendPacketLLP.toArray();
        sendPacket = new DatagramPacket(sendData, sendData.length, socket.getRemoteSocketAddress());
        ensureSend(sendPacket);

        printDebug("Successfully sent FIN. In LAST-ACK state.");

        ensureRcvdFlag("ACK", receivedPacket);

        printDebug("Received ACK for FIN. Closing...");
        socket.close();
    }

    private String whichFlag(LLP_Packet recvPacketLLP) {
        //In assumption that multiple flags won't be set in the same packet except syn/ack
        if (recvPacketLLP.getACKFlag() == 1 && recvPacketLLP.getSYNFlag() == 1) {
            return "SYN_ACK";
        } else if (recvPacketLLP.getSYNFlag() == 1) {
            return "SYN";
        } else if (recvPacketLLP.getACKFlag() == 1){
            return "ACK";
        }

        if (recvPacketLLP.getFINFlag() == 1) {
            return "FIN";
        } else {
            //TODO: returning empty string for now
            return "";
        }
    }
    private void printDebug(String statement) {
        if (debug) {
            System.out.println(statement);
        }
    }

    private DatagramPacket ensureRcvdFlag(String flag, DatagramPacket remotePacket) {
        boolean isFlag = false;
        boolean isFromRightClient = false;
        byte[] trimmedRcvData;
        LLP_Packet recvPacketLLP;
        byte[] receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(receiveData, receiveData.length);

        if (remotePacket == null) {
            isFromRightClient = true;
        }

        while (!isFlag || !isFromRightClient) {
            printDebug("Did not receive " + flag + ". Waiting...");
            boolean noTimeout = ensureRcv(recvPacket);

            trimmedRcvData = new byte[recvPacket.getLength()];
            System.arraycopy(receiveData, 0, trimmedRcvData, 0, trimmedRcvData.length);
            recvPacketLLP = LLP_Packet.parsePacket(trimmedRcvData);
            if (!noTimeout) {
                return null;
            }
            if (whichFlag(recvPacketLLP).equals(flag)) {
                isFlag = true;
            }
            if (!isFromRightClient && remotePacket.getSocketAddress().equals(recvPacket.getSocketAddress())) {
                isFromRightClient = true;
            }
        }

        return recvPacket;
    }
    /**
    * Used to set timeout and unset timeout when receiving a packet
    * @param flag if flag is empty string, eitherPacket is receivePacket
     * otherwise, eitherPacket is remotePacket
    * @param eitherPacket packet that can be either receivePacket or remotePacket
     * */
    private boolean timeoutRcv(String flag, DatagramPacket eitherPacket) {
        boolean isSuccessful = false;
        boolean isTimedout;
        while (!isSuccessful) {
            try {
                socket.setSoTimeout(TIMER);
                isSuccessful = true;
            } catch (SocketException e) {
                printDebug("Failed to set timeout. Retrying...");
            }
        }
        if (flag.equals("")) {
            isTimedout = !ensureRcv(eitherPacket);
        } else {
            isTimedout = ensureRcvdFlag(flag, eitherPacket) == null;
        }
        isSuccessful = false;
        while (!isSuccessful) {
            try {
                socket.setSoTimeout(0);
                isSuccessful = true;
            } catch (SocketException e) {
                printDebug("Failed to set timeout. Retrying...");
            }
        }
        return isTimedout;

    }

    //TODO: DELETE METHODS BELOW
    public boolean isClosed() {
        return socket.isClosed();
    }

    public void setTimeout(boolean on) {
        int timeout = on? TIMER+1000 : 0;
        try {
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            printDebug("Failed to set timeout");
        }
    }
}

