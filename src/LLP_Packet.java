import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Sally on 11/12/16.
 */
public class LLP_Packet {

    private int sequenceNum;
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

    public LLP_Packet(int sequenceNum, int ackNum, int checksum, int windowSize) {
        this(sequenceNum, ackNum, 0, checksum, windowSize, false, false, false, false);
    }

    public LLP_Packet(int sequenceNum, int ackNum, int dataOffset, int checksum, int windowSize,
                      boolean ACK, boolean RST, boolean SYN, boolean FIN) {
        this.sequenceNum = sequenceNum;
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
        buff.putInt(sequenceNum);
        buff.putInt(ackNum);

        String bitString = "";
        bitString += String.format("%2s", Integer.toBinaryString(dataOffset)).replace(' ', '0');
        bitString += String.format("%16s", Integer.toBinaryString(checksum)).replace(' ', '0');
        bitString += Integer.toBinaryString(ACK);
        bitString += Integer.toBinaryString(RST);
        bitString += Integer.toBinaryString(SYN);
        bitString += Integer.toBinaryString(FIN);
        bitString += String.format("%10s", Integer.toBinaryString(windowSize)).replace(' ', '0');

        int i = Integer.parseInt(bitString, 2);
        buff.putInt(i);

        byte[] newHeader = buff.array();
        return newHeader;
    }

    public byte[] getHeader() {
        return createHeader();
    }

    public static LLP_Packet parsePacket(byte[] rawPacket) {
        ByteBuffer wrapped = ByteBuffer.wrap(rawPacket); // big-endian by default
        LLP_Packet packet = new LLP_Packet();

        // Parse Data
        packet.setSequenceNum(wrapped.getInt()); // 32-bit seq num
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

    public void setSequenceNum(int sequenceNum) {
        this.sequenceNum = sequenceNum;
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
        LLP_Packet test = new LLP_Packet(456,40,50,50);
        byte[] bytes = test.getHeader();
        LLP_Packet testParsedPacket = LLP_Packet.parsePacket(bytes);
        System.out.println("Hello World!"); // Display the string.
    }
}
