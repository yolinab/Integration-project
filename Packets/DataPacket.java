package Packets;

import client.Message;
import client.MessageType;
import java.nio.ByteBuffer;

/**
 * Class for the packet containing the actual text exchanged between nodes.
 * Header:  4 bytes
 * Payload: 28 bytes
 *
 * Message type: DATA
 */
public class DataPacket{

    private ByteBuffer buffer;
    private final int headerLenght = 5;
    private final int TTL = 2;

    /*
                   Source                     Destination
            |_|_||_|_||_|_||_|_|          |_|_||_|_||_|_||_|_|

              Length of payload               TTL
            |_|_||_|_||_|_||_|_|          |_|_||_||_|_|_||_|_|

               SEQ          SW                Payload
            |_|_||_|_|    |_|_||_|_|      |_|_||_|_||_|_||_|_|

                      ...
     */

    /**
     * Creates a packet intended for use of actual sending of text messages.
     * @param sourceIP the address of the sender
     * @param destIp the intended destination of the message
     * @param payload the actual text data in the form of a byte array
     */
    public DataPacket(int sourceIP, int destIp, int seq, int sw, byte[] payload) {
        super();
        buffer = ByteBuffer.allocate(payload.length + headerLenght);
        buffer.put((byte) (sourceIP));
        buffer.put((byte) (destIp));
        buffer.put((byte) payload.length);
        buffer.put((byte) TTL);
        buffer.put((byte) ((byte) (seq << 4) + (byte) (sw)));
        for (Byte b: payload) {
            buffer.put(b);
        }
    }

    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    public Message convertToMessage() {
        return new Message(MessageType.DATA, buffer);
    }
}