import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Packet {

    private int seqNum;
    private int ackNum;
    private int dataOffset;
    private int checksum;
    private int ACK;
    private int RST;
    private int SYN;
    private int FIN;
    private int windowSize;
    private byte[] data;


    public LLP_Packet() {
        this(0, 0, 0, 0, 0, false, false, false, false);
    }

    public LLP_Packet(int seqNum, int ackNum, int checksum, int windowSize) {
        this(seqNum, ackNum, 0, checksum, windowSize, false, false, false, false);
    }

    public LLP_Packet(int seqNum, int ackNum, int dataOffset, int checksum, int windowSize,
                      boolean ACK, boolean RST, boolean SYN, boolean FIN) {
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.dataOffset = dataOffset;
        this.checksum = checksum;
        this.ACK = ACK ? 1 : 0;
        this.RST = RST ? 1 : 0;
        this.SYN = SYN ? 1 : 0;
        this.FIN = FIN ? 1 : 0;
        this.windowSize = windowSize;
        this.data = null;
    }

    public byte[] createHeader() {
        ByteBuffer buff = ByteBuffer.allocate(12);

        String seqString = String.format("%32s", Integer.toBinaryString(seqNum)).replace(' ', '0');
        String ackString = String.format("%32s", Integer.toBinaryString(ackNum)).replace(' ', '0');

        buff.putInt(Integer.parseInt(seqString, 2));
        buff.putInt(Integer.parseInt(ackString, 2));

        String bitString = "";
        bitString += String.format("%2s", Integer.toBinaryString(dataOffset)).replace(' ', '0');
        bitString += createChecksum();
        bitString += Integer.toBinaryString(ACK);
        bitString += Integer.toBinaryString(RST);
        bitString += Integer.toBinaryString(SYN);
        bitString += Integer.toBinaryString(FIN);
        bitString += String.format("%10s", Integer.toBinaryString(windowSize)).replace(' ', '0');

        int i = Integer.parseInt(bitString, 2);
        buff.putInt(i);

        return buff.array();
    }

    /**
     * Computes the checksum for this LLP_Packet and updates the global var for checksum.
     * Also returns the checksum in string representation.
     *
     * Helper function for createHeader()
     *
     * @return String containing the 16-bit checksum for this packet
     */
    public String createChecksum() {
        // Lots of computations already done in createHeader() are repeated here; could potentially optimize

        // Combine the header into one 80-bit string to segment later
        String bitString = "";
        bitString += String.format("%32s", Integer.toBinaryString(seqNum)).replace(' ', '0');
        bitString += String.format("%32s", Integer.toBinaryString(ackNum)).replace(' ', '0');
        bitString += String.format("%2s", Integer.toBinaryString(dataOffset)).replace(' ', '0');
        bitString += Integer.toBinaryString(ACK);
        bitString += Integer.toBinaryString(RST);
        bitString += Integer.toBinaryString(SYN);
        bitString += Integer.toBinaryString(FIN);
        bitString += String.format("%10s", Integer.toBinaryString(windowSize)).replace(' ', '0');

        if (bitString.length() != 80) {
            System.out.println("WARNING: BITSTRING NOT EXPECTED LENGTH. Found: " + bitString.length() + " Expected: " + 80);
        }

        if (getData() != null) {
            for (int i = 0; i < data.length; i += 1) {
                bitString += String.format("%8s", Integer.toBinaryString((int) data[i])).replace(' ', '0');
            }
            if (data.length % 2 != 0) {
                bitString += "00000000";
            }
        }

        if (bitString.length() % 16 != 0) {
            System.out.println("WARNING: BITSTRING NOT A MULTIPLE OF 0");
        }

        // Add the 16-bit chunks together
        int checksum = 0;
        for (int i = 0; i <= bitString.length() - 16; i += 16) {
            checksum += Integer.parseInt(bitString.substring(i, i + 16), 2);
        }

        String checksumString = Integer.toBinaryString(checksum).replace(' ', '0');

        // Take care of the carry(s)
        while (checksumString.length() > 16) {
            int carry = Integer.parseInt(checksumString.substring(0, checksumString.length() - 16), 2);
            int rest = Integer. parseInt(checksumString.substring(checksumString.length() - 16, checksumString.length()), 2);
            checksum = carry + rest;
            checksumString = Integer.toBinaryString(checksum);
        }

        // One's Complement Computations
        int result = Integer.parseInt(checksumString, 2);
        int flipped = ~result;
        checksumString = Integer.toBinaryString(flipped); // 32-bits
        checksumString = checksumString.substring(checksumString.length() - 16, checksumString.length()); // Truncate to 16-bits

        this.checksum = Integer.parseInt(checksumString, 2);
        return checksumString;
    }

    /**
     * Computes the checksum for this LLP_Packet and updates the global var for checksum.
     * Also returns the checksum in string representation.
     *
     * Helper function for createHeader()
     *
     * @return String containing the 16-bit checksum for this packet
     */
    public boolean isValidChecksum() {
        // Lots of computations already done in createHeader() are repeated here; could potentially optimize

        // Combine the header into one 80-bit string to segment later
        String bitString = "";
        bitString += String.format("%32s", Integer.toBinaryString(seqNum)).replace(' ', '0');
        bitString += String.format("%32s", Integer.toBinaryString(ackNum)).replace(' ', '0');
        bitString += String.format("%2s", Integer.toBinaryString(dataOffset)).replace(' ', '0');
        bitString += Integer.toBinaryString(ACK);
        bitString += Integer.toBinaryString(RST);
        bitString += Integer.toBinaryString(SYN);
        bitString += Integer.toBinaryString(FIN);
        bitString += String.format("%10s", Integer.toBinaryString(windowSize)).replace(' ', '0');

        if (getData() != null) {
            for (int i = 0; i < data.length; i += 1) {
                bitString += String.format("%8s", Integer.toBinaryString((int) data[i])).replace(' ', '0');
            }
            if (data.length % 2 != 0) {
                bitString += "00000000";
            }
        }

        // Include the checksum
        bitString += String.format("%16s", Integer.toBinaryString(checksum)).replace(' ', '0');

        if (bitString.length() % 16 != 0) {
            System.out.println("WARNING: BITSTRING NOT A MULTIPLE OF 16. Found: " + bitString.length());
        }

        // Add the 16-bit chunks together
        int checksum = 0;
        for (int i = 0; i <= bitString.length() - 16; i += 16) {
            checksum += Integer.parseInt(bitString.substring(i, i + 16), 2);
        }

        String checksumString = Integer.toBinaryString(checksum).replace(' ', '0');

        // Take care of the carry(s)
        while (checksumString.length() > 16) {
            int carry = Integer.parseInt(checksumString.substring(0, checksumString.length() - 16), 2);
            int rest = Integer. parseInt(checksumString.substring(checksumString.length() - 16, checksumString.length()), 2);
            checksum = carry + rest;
            checksumString = Integer.toBinaryString(checksum);
        }

        // One's Complement Computations
        int additionResult = Integer.parseInt(checksumString, 2);

        int flipped = ~additionResult;
        String temp = Integer.toBinaryString(flipped);

        int finalResult = Integer.parseInt(temp.substring(temp.length() - 16, temp.length()), 2);

        return finalResult == 0;
    }

    public byte[] getHeader() {
        return createHeader();
    }

    public static LLP_Packet parsePacket(byte[] rawPacket) {
        ByteBuffer wrapped = ByteBuffer.wrap(rawPacket); // big-endian by default
        LLP_Packet packet = new LLP_Packet();

        // Parse Data
        packet.setSeqNum(wrapped.getInt()); // 32-bit seq num
        packet.setAckNum(wrapped.getInt()); // 32-bit  ack num

        int rest = wrapped.getInt();
        String binRest = String.format("%32s", Integer.toBinaryString(rest)).replace(' ', '0');

        packet.setChecksum(Integer.parseInt(binRest.substring(2, 18), 2));  // index by bits
        packet.setACKFlag(Integer.parseInt(binRest.substring(18, 19), 2));
        packet.setRSTFlag(Integer.parseInt(binRest.substring(19, 20), 2));
        packet.setSYNFlag(Integer.parseInt(binRest.substring(20, 21), 2));
        packet.setFINFlag(Integer.parseInt(binRest.substring(21, 22), 2));
        packet.setWindowSize(Integer.parseInt(binRest.substring(22, 32), 2));

        packet.setData(Arrays.copyOfRange(rawPacket, 12, rawPacket.length)); // index by bytes

        return packet;
    }
    //TODO? I'm going to create getters for the ones I need... but if you see better way, do change it :)
    public int getACKFlag() {
        return ACK;
    }
    public int getFINFlag() {
        return FIN;
    }
    public int getSeqNum() {
        return seqNum;
    }
    public int getAckNum() {
        return ackNum;
    }
    public int getWindowSize() {
        return windowSize;
    }

    public int getSYNFlag() {
        return SYN;
    }

    public void setACKFlag(boolean enabled) {
        this.ACK = enabled ? 1 : 0;
    }

    public void setACKFlag(int flag) {
        this.ACK = flag;
    }

    public void setRSTFlag(boolean enabled) {
        this.RST = enabled ? 1 : 0;
    }

    public void setRSTFlag(int flag) {
        this.RST = flag;
    }

    public void setSYNFlag(boolean enabled) {
        this.SYN = enabled ? 1 : 0;
    }

    public void setSYNFlag(int flag) {
        this.SYN = flag;
    }

    public void setFINFlag(boolean enabled) {
        this.FIN = enabled ? 1: 0;
    }

    public void setFINFlag(int flag) {
        this.FIN = flag;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public byte[] toArray() {
        byte[] header = getHeader();
        byte[] data = getData();
        if (data == null) {
            return header;
        } else {
            byte[] combinedHeaderAndData = new byte[header.length + data.length];
            System.arraycopy(header, 0, combinedHeaderAndData, 0, header.length);
            System.arraycopy(data, 0, combinedHeaderAndData, header.length, data.length);
            return combinedHeaderAndData;
        }
    }

    //DELETE
    public static void main(String[] args) {
        LLP_Packet test = new LLP_Packet(2,13241,0,234);
        byte[] bytes = test.getHeader();
        System.out.println("ORIGINAL PACKET: WINDOW " + test.getWindowSize()
                + " ACK " + test.getAckNum()
                + " SEQ " + test.getSeqNum());

        LLP_Packet testParsedPacket = LLP_Packet.parsePacket(bytes);
        System.out.println("RECEIVED PACKET: WINDOW " + testParsedPacket.getWindowSize()
                + " ACK " + testParsedPacket.getAckNum()
                + " SEQ " + testParsedPacket.getSeqNum());


        System.out.println("IS VALID CHECKSUM: " + test.isValidChecksum());
        System.out.println("Hello World!"); // Display the string.
    }
}
