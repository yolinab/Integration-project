package Packets;

import client.Message;

import java.nio.ByteBuffer;

public interface Packet {

    ByteBuffer getByteBuffer();
    Message convertToMessage();

    enum PacketType {
        DISCOVERY,
        LSP,
        TCP,
        IP
    }
}
