package out.production.java_framework_integration_project.Packets;

import Packets.Packet;
import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;

public class DataPacket implements Packet {
    private  int sourceIp;
    private  int destIp;
    private int syn;

    private ByteBuffer buffer;

    /*
                   Source                  Destination
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

                   Flag                    SYN
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

                   Payload
            |_|_||_|_||_|_||_|_|      ...
     */

    /**
     *
     * @param sourceIP
     * @param destIp
     * @param syn
     * @param payload
     */
    public DataPacket(int sourceIP, int destIp, int syn, byte[] payload) {
        super();
        //buffer = ByteBuffer.allocate(2);
        buffer.put((byte) (sourceIp));
        buffer.put((byte) (destIp));
        buffer.put((byte) 128);        //flag 10000000
        buffer.put((byte) syn);
        for (Byte b: payload) {
            buffer.put(b);
        }
    }

    public int makeSYN(int syn) {
        return 0;
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    @Override
    public Message convertToMessage() {
        return new Message(MessageType.DATA, buffer);
    }
}
