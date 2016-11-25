import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sally, Yami on 11/12/16.
 */
public class LLP_Socket {
    //FINAL VARIABLES
    private static final int MAX_DATA_SIZE = 1012;
    private static final int TIMER = 500;
    //RECEIVE
    private int myWindowSize;
    private int expectedSeqNum;
    //SEND
    private int rcvWindowSize;
    private int localSeq;
    private int waitingForAck;
    //OTHER
    private DatagramSocket socket;
    private InetAddress destAddress;
    private int destPort;
    private boolean debug;
    private boolean isTyringToClose = false;

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
            } else if (localPort != -1){
                socket.bind(new InetSocketAddress(localAddress, localPort));
            }
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Failed to create a socket");
            System.exit(-1);
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
        myWindowSize = 1; // default window size, unless initialized by the application
        localSeq = 0;
        waitingForAck = 0;
        this.rcvWindowSize = 1;
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
        printDebug("SENDING SYN TO SERVER");
        LLP_Packet synPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, myWindowSize);
        synPacketLLP.setSYNFlag(true);
        byte[] synData = synPacketLLP.toArray();

        DatagramPacket synPacket = new DatagramPacket(synData, synData.length, address, port);

         do {
            ensureSend(synPacket);

            // Receive SYN-ACK & initialize remote Seq Num
            printDebug("WAITING FOR SYN-ACK FROM SERVER");
        } while (timeoutRcv("SYN_ACK", synPacket, 0));

        // Send ACK
        LLP_Packet ackPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, myWindowSize);
        ackPacketLLP.setACKFlag(true);
        byte[] ackData = ackPacketLLP.toArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        do {
            printDebug("SENDING ACK TO SERVER");
            ensureSend(ackPacket);
        } while (!timeoutRcv("SYN_ACK", synPacket, 100));

        LLP_Packet synReceive = LLP_Packet.parsePacket(synPacket.getData());

        // Set the receive window
        setRcvWindowSize(synReceive.getWindowSize());

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
        DatagramPacket receiveSYN = ensureRcvdFlag("SYN", null);
        LLP_Packet receiveSYNLLP = LLP_Packet.parsePacket(receiveSYN.getData());

        // Set window size for other end
        setRcvWindowSize(receiveSYNLLP.getWindowSize());

        LLP_Packet synAckPacketLLP = new LLP_Packet(localSeq, expectedSeqNum, 0, myWindowSize);
        synAckPacketLLP.setACKFlag(true);
        synAckPacketLLP.setSYNFlag(true);
        byte[] sendSYNACKData = synAckPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendSYNACKData, sendSYNACKData.length,
                receiveSYN.getAddress(), receiveSYN.getPort());
        byte[] receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(receiveData, receiveData.length);
        boolean isNotReceived = false;
        do {
            printDebug("SEND A SYN-ACK TO CLIENT");
            // Send SYN-ACK
            ensureSend(sendPacket);

            // Receive ACK
            printDebug("Receive ACK from Client.");
            isNotReceived = timeoutRcv("", recvPacket, 0);
            LLP_Packet receivedLLP = LLP_Packet.parsePacket(receiveData);
            if (!isNotReceived && whichFlag(receivedLLP).equals("SYN")) {
                isNotReceived = true;
            } else if (!isNotReceived && whichFlag(receivedLLP).equals("ACK")) {
                isNotReceived = false;
            }

        } while (isNotReceived);
        System.out.println("CONNECTION ACCEPTED");
        LLP_Socket retSocket = new LLP_Socket(socket.getLocalPort(), socket.getLocalAddress(), receiveSYN.getSocketAddress(), debug);
        return retSocket;
    }

    public void close() {
        isTyringToClose = true;
        byte[] receiveData = new byte[MAX_DATA_SIZE];
        DatagramPacket recvPacket = new DatagramPacket(receiveData, receiveData.length);
        // Send FIN
        printDebug("SENDING FIN");

        LLP_Packet finPacketLLP = new LLP_Packet(localSeq++, expectedSeqNum, 0, myWindowSize);
        finPacketLLP.setFINFlag(true);
        byte[] finArray = finPacketLLP.toArray();

        DatagramPacket finPacket = new DatagramPacket(finArray, finArray.length, socket.getRemoteSocketAddress());
         do {
            ensureSend(finPacket);

            // Receive either FIN or ACK
            printDebug("FIN-WAIT-1 STATE. WAITING FOR FIN OR ACK.");
        } while (timeoutRcv("", recvPacket, 0));
        expectedSeqNum++;
        byte[] trimmedRcvData = new byte[recvPacket.getLength()];
        System.arraycopy(receiveData, 0, trimmedRcvData, 0, trimmedRcvData.length);
        LLP_Packet recvPacktLLP = LLP_Packet.parsePacket(trimmedRcvData);

        if (whichFlag(recvPacktLLP).equals("ACK")) {
            //FIN-WAIT-2
            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq++, expectedSeqNum, 0, myWindowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            do {
                printDebug("ACK received. Waiting for FIN.");
            } while (timeoutRcv("FIN", finPacket, 0));
            printDebug("Received FIN. Sending ACK.");
            do {
                ensureSend(ackPacket);
                //If ack gets lost then it will resend ack
            } while (!timeoutRcv("FIN", finPacket, 100) || !timeoutRcv("ACK", finPacket, 100));
            expectedSeqNum++;
        } else if (whichFlag(recvPacktLLP).equals("FIN")) {
            printDebug("FIN received. Sending ACK");
            LLP_Packet ackPacketLLP = new LLP_Packet(localSeq++, expectedSeqNum, 0, myWindowSize);
            ackPacketLLP.setACKFlag(true);
            byte[] ackArray = ackPacketLLP.toArray();
            DatagramPacket ackPacket = new DatagramPacket(ackArray, finArray.length, socket.getRemoteSocketAddress());
            do {
                ensureSend(ackPacket);
                printDebug("Closing State. Waiting for ACK");
            } while (timeoutRcv("ACK", finPacket, 0));
            expectedSeqNum++;
        }
        printDebug("Timed-Wait State.");
        socket.close();
        printDebug("CLOSED");
    }

    public byte[] receive(int maxSize) {

        byte[] rawReceiveData = new byte[maxSize];
        DatagramPacket receivePacket = new DatagramPacket(rawReceiveData, rawReceiveData.length);
        if (!ensureRcv(receivePacket)) {
            return "timeout".getBytes();
        }

        // create new byte array without the empty unused buffer
        byte[] receiveData = new byte[receivePacket.getLength()];
        System.arraycopy(rawReceiveData, 0, receiveData, 0, receiveData.length);
        LLP_Packet receivedLLP = LLP_Packet.parsePacket(receiveData);

        printDebug("Received: " + new String(receiveData));
        // Update window size
        setRcvWindowSize(receivedLLP.getWindowSize());

        if (whichFlag(receivedLLP).equals("FIN")) {
            recvdClose(receivePacket);
            return null;
        }


        if (!receivedLLP.isValidChecksum() || receivedLLP.getSeqNum() != this.expectedSeqNum) {
            printDebug("PACKET DISCARDED: SEQ EXPECTED: " + this.expectedSeqNum + " RECEIVED: " + receivedLLP.getSeqNum());

            LLP_Packet ackPacket = new LLP_Packet(this.localSeq, this.expectedSeqNum, 0, myWindowSize);
            ackPacket.setACKFlag(true);
            byte[] sendData = ackPacket.toArray();

            DatagramPacket ack = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
            ensureSend(ack);
            return new byte[0];
        } else {
            if (isTyringToClose) {
                return null;
            }
            System.out.println("RECEIVED SEQ " + this.expectedSeqNum);
            printDebug("RECEIVED DATA: EXPECTED SEQ: " + this.expectedSeqNum + " RECEIVED SEQ " + receivedLLP.getSeqNum());

            LLP_Packet ackPacket = new LLP_Packet(this.localSeq, ++this.expectedSeqNum, 0, myWindowSize);
            ackPacket.setACKFlag(true);
            byte[] sendData = ackPacket.toArray();

            DatagramPacket ack = new DatagramPacket(sendData, sendData.length, destAddress, destPort);
            ensureSend(ack);
            String reStr = new String(receivedLLP.getData());

            printDebug(reStr);
            return receivedLLP.getData();
        }
    }

    public void send(byte[] data) throws SocketException {
        // Data to be sent
        byte[] fileBuff = data;

        // Sequence number of the last packet
        int startSeqNum = this.localSeq;
        int lastNumPackets = ((int) Math.ceil((double) fileBuff.length / MAX_DATA_SIZE));
        System.out.println("=== PACKETS TO BE SENT: " + lastNumPackets + " ===");

        // Map to store unAcknowledged packets
        Map<Integer, DatagramPacket> sentPackets = new HashMap<>();

        // While window not full and there is more data to send:
        while(true) {
            while (this.localSeq - waitingForAck < rcvWindowSize && this.localSeq < lastNumPackets+startSeqNum) {

                // Store packet data to send
                byte[] sendPacketBytes = null;
                int startPos = (this.localSeq-startSeqNum)*MAX_DATA_SIZE;
                if (startPos + MAX_DATA_SIZE > fileBuff.length) {
                    sendPacketBytes = Arrays.copyOfRange(fileBuff, startPos, fileBuff.length); // ensure last packet has no nulls
                } else {
                    sendPacketBytes = Arrays.copyOfRange(fileBuff, startPos, startPos + MAX_DATA_SIZE);
                }

                LLP_Packet sendPacketLLP = new LLP_Packet(this.localSeq, expectedSeqNum, 0, myWindowSize);
                sendPacketLLP.setData(sendPacketBytes);
                byte[] sendData = sendPacketLLP.toArray();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, destAddress, destPort);

                ensureSend(sendPacket);
                sentPackets.put(this.localSeq, sendPacket);

                printDebug("SENT DATA " + this.localSeq);

                this.localSeq++;
            }

            // Receive Acks
            byte[] ackBytes = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length);

            if (timeoutRcv("", ackPacket, 0)) {
                printDebug("Re-transmitting Packets...");

                // Re-send Packets
                for (int i = waitingForAck; i < this.localSeq; i++) {
                    DatagramPacket resendPacket = sentPackets.get(i);
                    printDebug("Resending packet Seq: " + i);
                    ensureSend(resendPacket);
                }
            } else {
                printDebug("packet received: " + new String(ackPacket.getData()));
                LLP_Packet ackLLP = LLP_Packet.parsePacket(ackPacket.getData());
                String flag = whichFlag(ackLLP);
                if (flag.equals("ACK")) {
                    printDebug("RECEIVED ACK FOR SEQ NUM: " + ackLLP.getAckNum());
                    printDebug("SEQ NUM OF LAST PACKET: " + lastNumPackets);
                    waitingForAck = Math.max(waitingForAck, ackLLP.getAckNum());
                    // exit loop if last packet
                    if (ackLLP.getAckNum() == lastNumPackets + startSeqNum) {
                        System.out.println("=== SENDING COMPLETE ===");
                        return;
                    }
                } else if (flag.equals("FIN")){
                    printDebug("FIN RECEIVED.");
                    this.localSeq--;
                    recvdClose(ackPacket);
                    throw new SocketException("Server closed.");
                } else {
                    // Lost ACK
                    if (ackLLP.getAckNum() > lastNumPackets + startSeqNum) {
                        System.out.println("=== SENDING COMPLETE ===");
                        return;
                    }
                }
            }

        }
    }

    public void setMyWindowSize(int myWindowSize) {
        this.myWindowSize = myWindowSize;
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
                printDebug("Failed to send. Retrying...");
            }
        }
    }
    private boolean ensureRcv (DatagramPacket recvPckt) {
        while (true) {
            try {
                socket.receive(recvPckt);
                System.out.println(new String(recvPckt.getData()));
                LLP_Packet recvPktLLP = LLP_Packet.parsePacket(recvPckt.getData());
                if (recvPktLLP.getSeqNum() != expectedSeqNum) {
                    System.out.println("packet seq num: " + recvPktLLP.getSeqNum() + " Ack num: " + expectedSeqNum);
                }
                if (recvPktLLP.isValidChecksum() && recvPktLLP.getSeqNum() == expectedSeqNum) {
                    if (recvPktLLP.getFINFlag() == 1) {
                        System.out.println("packet seq num: " + recvPktLLP.getSeqNum() + " Ack num: " + expectedSeqNum);
                    }
                    return true;
                }
            } catch (SocketTimeoutException e) {
                printDebug("Timeout");
                return false;
            } catch (IOException e) {
                printDebug("Failed to receive. Retrying...");
            } catch (BufferUnderflowException e) {
                //similar to timeout
                return false;
            }
        }
    }
    /**
     * Right side of State diagram when trying to close
     * Called when either side receives FIN from the other end point
     * */
    private void recvdClose(DatagramPacket receivedPacket) {
        //Received FIN. Sending ACK
        printDebug("Received FIN. Trying to send ACK");
        expectedSeqNum++;
        LLP_Packet ackPacketLLP = new LLP_Packet(localSeq++, expectedSeqNum, 0, myWindowSize);
        ackPacketLLP.setACKFlag(1);
        byte[] ackData = ackPacketLLP.toArray();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, socket.getRemoteSocketAddress());

        LLP_Packet sendPacketLLP = new LLP_Packet(localSeq++, expectedSeqNum, 0, myWindowSize);
        sendPacketLLP.setFINFlag(1);
        byte[] sendData = sendPacketLLP.toArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getRemoteSocketAddress());
        do {
            printDebug("Sending ACK. In CLOSE-WAIT state.");
            ensureSend(ackPacket);
            printDebug("Sending FIN");
            ensureSend(sendPacket);
            printDebug("Successfully sent FIN. In LAST-ACK state.");
        } while (timeoutRcv("ACK", receivedPacket, 0));
        expectedSeqNum++;

        printDebug("Received ACK for FIN. Closing...");
        socket.close();
        printDebug("CLOSED");
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
            if (!noTimeout) {
                return null;
            }

            trimmedRcvData = new byte[recvPacket.getLength()];
            System.arraycopy(receiveData, 0, trimmedRcvData, 0, trimmedRcvData.length);
            recvPacketLLP = LLP_Packet.parsePacket(trimmedRcvData);
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
    private boolean timeoutRcv(String flag, DatagramPacket eitherPacket, int extraTime) {
        boolean isSuccessful = false;
        boolean isTimedout;
        while (!isSuccessful) {
            try {
                socket.setSoTimeout(TIMER+extraTime);
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

    public void setRcvWindowSize(int rcvWindowSize) {
        printDebug("RECEIVE WINDOW SIZE SET TO: " + rcvWindowSize);
        this.rcvWindowSize = rcvWindowSize;
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
