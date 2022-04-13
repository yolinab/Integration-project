package Packets;

import client.Message;
import client.MessageType;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Class for the second type of messages being sent by the nodes in the routing phase.
 *
 * When a node receives a LinkStatePacket from another node, it looks through the list of the neighbours of that sender,
 * and if it doesn't contain a certain IP in its routing table,
 * it adds it as a destination, and the sender of that packet as the next hop.
 * MessageType: DATA
 */

public class LinkStatePacket implements Packet{

    private final int sourceIp;
    private ByteBuffer buffer;
    /*
                 Source             Flag(SYN/ACK)    Number of neighbours
            |_|_||_|_||_|_||_|_|      |_|_||_|_|          |_|_||_|_|
                Placeholders for direct neighbours
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|
            |_|_||_|_||_|_||_|_|
     */
    /**
     * Makes an initial SYN packet containing the IP of the creator,
     * a set SYN flag(4 bits), number of neighbors(4 bits) and 3 bytes for the list of neighbours.
     */
    //@requires nodesInRange != null && nodesInRange.size() < 4;
    public LinkStatePacket(int sourceIp, HashMap<Byte,Byte> nodesInRange) {
        super();
        this.sourceIp = sourceIp;
        buffer = ByteBuffer.allocate(5);
        buffer.put((byte) sourceIp);
        buffer.put((byte) (64 + nodesInRange.size()));               //SYN flag 01000000 + size
        for (Byte dest: nodesInRange.keySet()) {
            buffer.put(dest);
        }
    }

    /**
     * Returns a ByteBuffer containing the bits the packet was last altered to.
     * SYN / ACK
     *
     * @return the current state of the packet
     */
    //@ensures buffer.capacity() == 5;
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    /**
     * Encapsulates the routing packet in a Message Type
     *
     * @return the discovery packet in a form of a MessageType. DATA
     */
    //@ensures \result.getData().capacity() == 5;
    public Message convertToMessage() {
        return new Message(MessageType.DATA,buffer);
    }

}
