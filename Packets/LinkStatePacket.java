package Packets;

import client.Message;
import client.MessageType;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Class for the second type of messages being sent by the nodes in the routing phase.
 *
 * When a node receives a LinkStatePacket from another node, it looks through the list of the neighbours of that sender,
 * and if it doesn't contain that key in its routing table(hashmap),
 * it adds it as a key, and the sender of that packet is the corresponding value.
 * MessageType: DATA
 */

public class LinkStatePacket implements Packet{

    private int sourceIp;
    private ByteBuffer buffer;
    /*
                 Source             Flag(SYN/ACK)    Number of neighbours
            |_|_||_|_||_|_||_|_|      |_|_||_|_|          |_|_||_|_|
                Placeholders for direct neighbours
            |_|_||_|_||_|_||_|_|      |_|_||_|_||_|_||_|_|
            |_|_||_|_||_|_||_|_|
     */

    //@ensures buffer.capacity() == 5;
    public LinkStatePacket(int sourceIp) {
        super();
        this.sourceIp = sourceIp;
        buffer = ByteBuffer.allocate(5);
    }

    /**
     * Makes an initial SYN packet containing the IP of the creator,
     * a set SYN flag(4 bits), number of neighbors(4 bits) and 3 bytes for the list of neighbours .
     */
    //@requires directNeighbours != null && directNeighbours.size() > 0;
    public void makeSYN(HashMap<Byte,Byte> directNeighbours) {
        buffer.put((byte) sourceIp);
        buffer.put((byte) (64 + directNeighbours.size()));               //SYN flag 01000000 + size
        for (Byte dest: directNeighbours.keySet()) {
            buffer.put(dest);
        }
    }

    /**
     * Returns a ByteBuffer containing the bits the packet was last altered to.
     * SYN / ACK
     *
     * @return the current state of the packet
     */
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    /**
     * Encapsulates the routing packet in a Message Type
     *
     * @return the discovery packet in a form of a MessageType. DATA
     */
    public Message convertToMessage() {
        return new Message(MessageType.DATA,buffer);
    }

    //For debugging
    public static void main(String[] args) {

        //--------
        HashMap<Byte,Byte> neighbours = new HashMap<>();
        neighbours.put((byte)1,(byte)1);                                //00000001
        neighbours.put((byte)2,(byte)2);                                //00000010
        neighbours.put((byte)3,(byte)3);                                //00000011

        LinkStatePacket packet = new LinkStatePacket(63);       //00111111
        packet.makeSYN(neighbours);                                     //01000000 + 00000011

        ByteBuffer bytes = packet.getByteBuffer();

        System.out.println("Bytes:");
        for (int i = 0; i<bytes.capacity(); i++) {
            System.out.println(String.format("%8s", Integer.toBinaryString(bytes.get(i))).replace(' ', '0'));
        }
    }
}
