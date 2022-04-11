package Packets;

import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Class for the first type of packet being sent by the nodes in the discovery phase.
 * Message type: DATA_SHORT
 */

//TODO: protocol handling packet loss
public class DiscoveryPacket implements Packet {

    private int sourceIp;
    private ByteBuffer buffer;
//    private ArrayList<Byte> neighbours;

    /*
                 Source           Flag(SYN/ACK)   Receiver puts their IP here
            |_|_||_|_||_|_||_|_|      |_|_|          |_|_||_|_||_|_|
     */

    //@requires sourceIp != 0;
    //@ensures buffer.capacity() == 2;
    public DiscoveryPacket(int sourceIP) {
        super();
        this.sourceIp = sourceIP;
        buffer = ByteBuffer.allocate(2);
//        neighbours = new ArrayList<>(3);
    }

    /**
     * Makes an initial SYN packet containing the IP of the creator,
     * a set SYN flag and an empty placeholder for the receivers IP.
     */
    //@ensures buffer.get(1) == 64;
    public void makeSYN() {
        buffer.put((byte) (sourceIp));      //00000000 for source IP
        buffer.put((byte) 64);              //01000000 for SYN
    }

    /**
     * When a node has received an initial SYN packet, it sends back a response with its IP
     * The ACK flag is automatically set
     *
     * @param ip the IP of the neighbour who sends the ACK
     */
    //@requires ip != 0;
    public void respond(int ip) {
        buffer.put(1, (byte) ((buffer.get(1) + ip) - 64));//add the IP and remove SYN flag
//        neighbours.add((byte)ip);
    }

    /**
     * Returns a ByteBuffer containing the bits the packet was last altered to.
     * discovery / response
     *
     * @return the current state of the packet
     */
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    /**
     * Checks if the ACK flag is set, i.e. this is a response packet.
     *
     * @return true is this packet is a response to a discovery packet
     */
    //@requires buffer != null;
    public boolean isACKed() {
        return buffer.get(1) < 64;
    }


    /**
     * Retrieves the list of all the nodes who responded to the initial discovery message
     * @return the list of direct neighbours
     */
//    public ArrayList<Byte> getNeighbours() {
//        return neighbours;
//    }

    /**
     * Encapsulates the discovery packet in a Message Type
     * @return the discovery packet in a form of a MessageType.DATA_SHORT
     */
    public Message convertToMessage() {
//        return new Message(MessageType.DATA_SHORT,buffer);
        return new Message(MessageType.DATA_SHORT,buffer);
    }

}

//    public static void main(String[] args) {
//        DiscoveryPacket packet = new DiscoveryPacket(63);
//        packet.makeSYN();
//        ByteBuffer resultBuffer = packet.getByteBuffer();
//        System.out.println("SYN packet from 63:");
//        for (int i = 0; i < resultBuffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(resultBuffer.get(i))).replace(' ', '0'));
//        }
//        packet.respond(55);
//        System.out.println("ACK packet from 55:");
//        ByteBuffer responseBuffer = packet.getByteBuffer();
//        for (int i = 0; i < responseBuffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(responseBuffer.get(i))).replace(' ', '0'));
//        }
//    }

