import java.nio.ByteBuffer;

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
    private byte[] header;

    public LLP_Packet(int sequenceNum, int ackNum, int dataOffset, int checksum,
                      int ACK, int RST, int SYN, int FIN, int windowSize) {
        this.sequenceNum = sequenceNum;
        this.ackNum = ackNum;
        this.dataOffset = dataOffset;
        this.checksum = checksum;
        this.ACK = ACK;
        this.RST = RST;
        this.SYN = SYN;
        this.FIN = FIN;
        this.windowSize = windowSize;
        this.header = createHeader();
    }


    public byte[] createHeader() {
        byte[] newHeader = new byte[96];
        ByteBuffer temp = ByteBuffer.allocate(4).putInt(sequenceNum);
        newHeader = temp.array();
        return newHeader;
    }

    public byte[] getHeader() {
        return header;
    }
}
