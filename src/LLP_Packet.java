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

    public LLP_Packet(int sequenceNum, int ackNum, int checksum, int windowSize) {
        this.sequenceNum = sequenceNum;
        this.ackNum = ackNum;
        this.checksum = checksum;
        this.windowSize = windowSize;
        this.dataOffset = 0; // Setting this value breaks the packet parsing; need to fix
        this.ACK = 0;
        this.RST = 0;
        this.SYN = 0;
        this.FIN = 0;
        this.data = null;
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

    public void parseHeader() {
        ByteBuffer wrapped = ByteBuffer.wrap(createHeader()); // big-endian by default
        this.sequenceNum = wrapped.getInt();
        this.ackNum = wrapped.getInt();

        int rest = wrapped.getInt();
        String binRest = String.format("%32s", Integer.toBinaryString(rest)).replace(' ', '0');

        this.checksum = Integer.parseInt(binRest.substring(2, 18), 2);

        this.ACK = Integer.parseInt(binRest.substring(18, 19), 2);
        this.RST = Integer.parseInt(binRest.substring(19, 20), 2);
        this.SYN = Integer.parseInt(binRest.substring(20, 21), 2);
        this.FIN = Integer.parseInt(binRest.substring(21, 22), 2);
        this.windowSize = Integer.parseInt(binRest.substring(22, 32), 2);
    }

    public void setACKFlag(boolean enabled) {
        this.ACK = enabled ? 1 : 0;
    }

    public void setRSTFlag(boolean enabled) {
        this.RST = enabled ? 1 : 0;
    }

    public void setSYNFlag(boolean enabled) {
        this.SYN = enabled ? 1 : 0;
    }

    public void setFINFlag(boolean enabled) {
        this.FIN = enabled ? 1: 0;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    public byte[] createPacket() {
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

    public static void main(String[] args) {
        LLP_Packet test = new LLP_Packet(456,40,50,50);
        byte[] bytes = test.getHeader();
        test.parseHeader();
        System.out.println("Hello World!"); // Display the string.
    }
}
