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
public class DataPacket implements Packet {

    private ByteBuffer buffer;
    private final int headerLenght = 4;

    /*
                   Source                  Destination
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

                    Size                       ?
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

                   Payload
            |_|_||_|_||_|_||_|_|      ...
     */

    /**
     * Creates a packet intended for use of actual sending of text messages.
     *
     * @param sourceIP the address of the sender
     * @param destIp the intended destination of the message
     * @param sw sending window
     * @param seq the sequence number of the packet
     * @param payload the actual text data in the form of a byte array
     */
    public DataPacket(int sourceIP, int destIp, int sw, int seq, byte[] payload) {
        super();
        buffer = ByteBuffer.allocate(payload.length + headerLenght);
        buffer.put((byte) (sourceIP));
        buffer.put((byte) (destIp));
        buffer.put((byte) sw);
        buffer.put((byte) seq);
        for (Byte b: payload) {
            buffer.put(b);
        }
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