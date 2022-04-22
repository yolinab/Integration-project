package client;


import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Message {
    private MessageType type;
    private ByteBuffer data;

    public Message(MessageType type){
        this.type = type;
    }

    public Message(MessageType type, ByteBuffer data){
        this.type = type;
        this.data = data;
    }

    public MessageType getType(){
        return type;
    }

    public ByteBuffer getData(){
        return data;
    }

    /**
     * Places the IP of the receiver in the first byte, and the IP of the initial sender
     * in the second byte with an ACK flag.
     *
     * @param receiverIP the IP of the responder
     * @return the ACK message in response to discovery SYN message
     */
    //@requires data.capacity() == 2;
    public Message respondToDiscoverySYN(int receiverIP) {
        ByteBuffer buffer = this.getData();
        byte srcIP = buffer.get(0);
        buffer.clear();
        buffer.put(0, (byte) receiverIP);
        buffer.put(1,(byte)(srcIP + 128));                  //10000000 - ACK
        return new Message(MessageType.DATA_SHORT, buffer);
    }

    /**
     * Decrements the number in the TTL segment in a DataPacket Message by 1.
     *
     * @return the same message, with a TTL equal to the previous TTL - 1;
     */
    public Message decrementTTL() {
        ByteBuffer buffer = this.getData();
        int TTL = buffer.get(3);
        buffer.put(3,(byte) (TTL - 1));
        return new Message(MessageType.DATA,buffer);
    }

    /**
     * Reads the routing table from a received Link State Routing message.
     *
     * @return an array containing the next hops to the destinations
     */
    //@requires data.capacity() == 5;
    public ArrayList<Byte> readReceivedRoutingTable() {
        ByteBuffer buffer = this.getData();
        ArrayList<Byte> neighbours = new ArrayList<>();

        byte neighboursCount = (byte)(buffer.get(1) - 64);  //with removed SYN
        for (int i = 2; i < neighboursCount + 2; i++) {
            neighbours.add(buffer.get(i));
        }
        return neighbours;
    }

}