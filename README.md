Final project for Network systems module.

A fully distributed multi-hop ad-hoc chat application that communicates through wireless sound waves using the emulator: 

http://netsys.ewi.utwente.nl/integrationproject/ - on frequency 5304 Hz

Convergence of the network:

Upon initialization, each node creates its own IP address(6 bits) based on the time (in milliseconds) it joined the network. 
A link-state routing mechanism is implemented for the discovery of the nodes. A round-robit approach was used for medium access control in the discovery phase and a protocol based on ALOHA for the actual messages.

Sending messages:

Communication on the transport layer is connection-oriented, i.e., TCP like mechanism. To establish a connection, the sender initiates a handshake, sending a control message (data short) to the receiver, with a sequence number and requesting the port number the receiver has reserved for communication with them. The receiver sends back an ACK of the SYN, its own SYN, and the requested port number. When the sender finally sends an ACK of the receivers SYN the “connection” is established.

Packet fragmentation:

Packets are fragmented at the sender, and we will include a flag “done sending” at the transport layer header indicating the last packet of the fragment. The receiver will simply parse them one by one in order of their sequence numbers.

Retransmissions:

The problem of packet loss is taken care of by retransimtting(broadasting) every packet that is not addressed to us. In the case of medium loss, a TTL of 3 for packets works best.

Queueing:

Two main queues are used. One main thread for transmitting packets and one for receiving. Both use FIFO scheduling.




