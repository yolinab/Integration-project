package out.production.java_framework_integration_project.Packets;

import Packets.Packet;
import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;

public class AckPacket implements Packet {
    ByteBuffer buffer;

    public AckPacket(byte ack) {
        buffer = ByteBuffer.allocate(2);
        buffer.put(ack);
        buffer.put((byte) 1);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    @Override
    public Message convertToMessage() {
        return new Message(MessageType.DATA_SHORT,buffer);
    }
}
