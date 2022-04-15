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
     * Creates a DIRECT PING Message.
     *
     * @param sourceIp the node that want to know if they are still in range with that destination
     * @param destIP the node the message is directed to
     * @return DIRECT PING Message with a SYN flag
     */
    //@requires data.capacity() == 2;
    public Message makeDirectedPING(int sourceIp, int destIP) {               //first byte is the
        ByteBuffer buffer = this.getData();                                     //second byte is the node that being checked
        buffer.put(0,(byte) sourceIp);
        buffer.put(1,(byte)(64 + destIP));
        return new Message(MessageType.DATA_SHORT,buffer);
    }

    /**
     * Creates a response to a DIRECT PING Message.
     *
     * @return the response to a DIRECT PING Message with an ACK flag
     */
    public Message respondToDirectedPING() {
        ByteBuffer buffer = this.getData();                     //if the initial data is right we just remove the SYN from the second byte of the initial buffer
        byte initByte2 = buffer.get(1);
        buffer.put(0,(byte)(initByte2 - 64));            //the ACK contains our IP as source and the initial sender as the second byte with an ACK flag
        buffer.put(1,(byte) 128);                        // 10000000 - "ACK" for a directed PING
        return new Message(MessageType.DATA_SHORT,buffer);
    }

    /**
     * Reads the list of neighbours from a received Link State Routing message.
     *
     * @return an array containing the nodes in range of the source of that message
     */
    //@requires data.capacity() == 5;
    public ArrayList<Byte> readReceivedRoutingTable() {
        ByteBuffer buffer = this.getData();
        ArrayList<Byte> neighbours = new ArrayList<>();

        byte neighboursCount = (byte)(buffer.get(1) - 64);  //with removed SYN
        for (int i = 0; i < neighboursCount; i++) {
            neighbours.add(buffer.get(i+2));
        }
        return neighbours;
    }

}