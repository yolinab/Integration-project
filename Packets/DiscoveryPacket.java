package Packets;

import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Class for the first type of packet being sent by the nodes in the discovery phase.
 *
 * Message type: DATA_SHORT
 */

//TODO: protocol handling packet loss
public class DiscoveryPacket implements Packet {

    private final int sourceIp;
    private ByteBuffer buffer;

    /*
                 Source           Flag(SYN/ACK)   Receiver puts their IP here
            |_|_||_|_||_|_||_|_|      |_|_|          |_|_||_|_||_|_|
     */

    /**
     * Makes an initial SYN packet containing the IP of the creator,
     * a set SYN flag and an empty placeholder for the receivers IP.
     */
    //@requires sourceIp != 0;
    //@ensures buffer.capacity() == 2 && buffer.get(1) == 64;
    public DiscoveryPacket(int sourceIP) {
        super();
        this.sourceIp = sourceIP;
        buffer = ByteBuffer.allocate(2);
        buffer.put((byte) (sourceIp));      //00000000 for source IP
        buffer.put((byte) 64);              //01000000 for SYN
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
     * Encapsulates the discovery packet in a Message Type
     * @return the discovery packet in a form of a MessageType.DATA_SHORT
     */
    public Message convertToMessage() {
        return new Message(MessageType.DATA_SHORT,buffer);
    }
}

