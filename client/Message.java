package client;


import java.nio.ByteBuffer;

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
     * Removes the SYN flag from a discovery message and places the IP in the second byte
     * @param receiverIP the IP of the responder
     * @return the ACK message in response to discovery SYN message
     */
    //@requires this.getData().capacity() == 2;
    public Message respondToDiscoverySYN(byte receiverIP) {
        ByteBuffer buffer = this.getData();
        buffer.put(1, (byte) ((buffer.get(1) + receiverIP) - 64));//add the IP and remove SYN flag
        return new Message(MessageType.DATA_SHORT, buffer);
    }

//    public Message respondToRoutingSYN(){
//        return
//    }

}