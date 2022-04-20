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
    private final int TTL = 6;

    /*
                   Source                  Destination
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

              Length of payload                TTL
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|

                   Payload
            |_|_||_|_||_|_||_|_|      ...
     */

    /**
     * Creates a packet intended for use of actual sending of text messages.
     *
     * @param sourceIP the address of the sender
     * @param destIp the intended destination of the message
     * @param lenght of the payload
     * @param payload the actual text data in the form of a byte array
     */
    public DataPacket(int sourceIP, int destIp, int lenght, byte[] payload) {
        super();
        buffer = ByteBuffer.allocate(payload.length + headerLenght);
        buffer.put((byte) (sourceIP));
        buffer.put((byte) (destIp));
        buffer.put((byte) lenght);
        buffer.put((byte) TTL);
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