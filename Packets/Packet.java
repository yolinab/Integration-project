package Packets;

public interface Packet {

    enum PacketType {
        DISCOVERY,
        LSP,
        TCP,
        IP
    }
}
