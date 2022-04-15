package Network;

import Packets.DiscoveryPacket;
import Packets.LinkStateRoutingPacket;
import client.Client;
import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    private int ip;
    private boolean mediumIsFree;
    private BlockingQueue<Message> PONGsToSend;
    private BlockingQueue<Byte> recentlyReceivedPONGs;

    //ROUTING TABLE -> key - destination | value - next hop
    private HashMap<Byte,Byte> neighbours;
    private Message routingMessage;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node(String serverIP, int serverPort, int frequency) {
        receivedQueue = new LinkedBlockingQueue<>();
        sendingQueue = new LinkedBlockingQueue<>();
        new Client(serverIP, serverPort, frequency, receivedQueue, sendingQueue);
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
    }

    /**
     * Starts the discovery and routing sequences of a Node.
     */
    public void initialize() {
        mediumIsFree = false;
        PONGsToSend = new LinkedBlockingQueue<>();
        recentlyReceivedPONGs = new LinkedBlockingQueue<>();
        neighbours = new HashMap<>(3);

        new receiveThread(receivedQueue).start();

        new transmitThread(sendingQueue).start();
    }

    /**
     * Placing messages in the sending queue is done ONLY by using this method.
     * By avoiding race conditions, we ensure fair queueing.
     *
     * @param message Message to be sent
     */
    private synchronized void putMessageInSendingQueue(Message message) {
        try {
            sendingQueue.put(message);
        } catch (InterruptedException e) {
            System.err.println("Failed to put message in sending queue." + e);
        }
    }

    /**
     * Puts a PING Message in the sending queue.
     */
    private synchronized void sendPING() {
        putMessageInSendingQueue(new DiscoveryPacket(ip).convertToMessage());
    }

    //---------------------------------------------- Start of sending threads ----------------------------------------------//
    /**
     * Thread for transmitting Messages.
     */
    private  class transmitThread extends Thread {
        private BlockingQueue<Message> sendingQueue;
        private Thread sendPINGs;
        private Thread sendPONGs;
        private Thread sendRoutingInfo;

        public transmitThread(BlockingQueue<Message> sendingQueue){
            super();
            this.sendingQueue = sendingQueue;
            sendPINGs = new sendPINGsThread();
            sendPONGs = new sendPONGsThread();
            sendRoutingInfo = new sendRoutingInfoThread();
        }

        @Override
        public void run() {
            sendPINGs.start();
            sendPONGs.start();
//            sendRoutingInfo.start();
            while (true) {
                try {
                    if (neighbours.size() == 3) {

                        for (Byte dest: neighbours.keySet()) {
                            System.out.print("Destination:");
                            System.out.println(dest);
                            System.out.println("Next hop:");
                            System.out.println(neighbours.get(dest));
                        }
                    }
                    Thread.sleep(1000);
                    //TODO: routing sequence
                } catch (InterruptedException e) {
                    System.out.println("Failed to send data. " + e);
                    break;
                }
            }
        }
    }

     //TODO: race conditions?
    /**
     * A separate thread for broadcasting PING messages.
     *
     * It is used in the discovery phase to determine the nodes that are in range,
     * and keeps running to adapt to change in network topology.
     */
    public class sendPINGsThread extends Thread {

        private final long timeInterval = 3000;
        // ≈ ??% probability to send
        boolean send;
        //counts how many PINGs a node has sent, so overtime it can decrease the rate it's sending them at
        int counter;

        @Override
        public void run() {
            while (true) {
                send =(new Random().nextInt(8) == 0);

                while (send) {
                    if (mediumIsFree && PONGsToSend.isEmpty()) {//we are still in discovery phase
                        System.out.println(getIp() + " is sending a PING.");
                        sendPING();
                        counter++;
                    }
                    try {
                        Thread.sleep(timeInterval);
                    } catch (InterruptedException e) {
                        System.err.println("Failed to send PING " + e);
                        break;
                    }
                    send = new Random().nextInt(15) == 0;
                }
                try {
                    Thread.sleep(timeInterval);
                } catch (InterruptedException e) {
                    System.err.println("Failed to send PING " + e);
                    break;
                }
            }
        }
    }

    /**
     * A separate thread for responding to PING messages.
     *
     * It is used in the discovery phase to determine the nodes that are in range,
     * and keeps running to adapt to change in network topology.
     */
    public class sendPONGsThread extends Thread {
        private int timeInterval = 500;
        private boolean send;
        @Override
        public void run() {
            while (true) {
                send =(new Random().nextInt(8) == 0);

                while (send) {
                    try {
                        if (mediumIsFree && PONGsToSend.size() > 0) {
                            System.out.println(getIp() + " is sending a PONG.");
                            putMessageInSendingQueue(PONGsToSend.take());
                        }
                        Thread.sleep(timeInterval);
                        send =(new Random().nextInt(10) == 0);
                    } catch (InterruptedException e) {
                        System.out.println("Failed to send data. " + e);
                        break;
                    }
                }
            }
        }
    }

    /**
     * A separate thread for sending a Nodes routing information at a specified time interval.
     *
     * Before it starts it waits 10 seconds so the nodes can first discover their neighbours.
     */
    public class sendRoutingInfoThread extends Thread {

        @Override
        public void run() {
            //Before it sends it's first routing table, it waits 10 seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.err.println("Failed to send routing table.");
            }

            while (true) {
                LinkStateRoutingPacket routingPacket = new LinkStateRoutingPacket(ip, neighbours);
                routingMessage = routingPacket.convertToMessage();
                try {
                    putMessageInSendingQueue(routingMessage);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("Node failed to send routing info " + e);
                }
            }
        }
    }
    //---------------------------------------------- End of sending threads ----------------------------------------------//


    //---------------------------------------------- Start of receive thread ----------------------------------------------//
    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print(  ( bytes.get(i) ) );
            }
        }

        public void run() {
            while (true) {
                try {
                    Message m = receivedQueue.take();
                    MessageType type = m.getType();
                    switch (type) {
                        case DATA_SHORT -> {
                            //------------------- RECEIVING A PING -------------------//    //[sender of PING] + [01000000]
                            if (m.getData().get(1) == 64) {                                 //only if is message is SYN, send a response
                                System.out.println(getIp() + " received a PING. ");
                                printByteBuffer(m.getData(), m.getData().capacity());
                                System.out.println();
                                if (m.getData().get(0) == getIp()) {                        //if the sender has the same ip as us, we change ours
                                    ip =(new Random().nextInt((int) System.currentTimeMillis())) % 64;
                                    System.out.println("Someone has the same IP as us, so we changed ours to: " + ip);
                                    sendPING();
                                } else if(m.getData().get(0) != getIp()) {
                                    PONGsToSend.put(m.respondToDiscoverySYN((byte) getIp()));
                                }
                            }
                            //------------------- RECEIVING A PONG -------------------//
                            else if ((m.getData().get(1))  == (byte) (getIp() + 128)) { //if message is ACK, just add to neighbour's map
                                System.out.println(getIp() + " received a PONG from " + m.getData().get(0) + " .");
                                neighbours.put(m.getData().get(0),m.getData().get(0));
                                recentlyReceivedPONGs.put(m.getData().get(0));
                            }
                        }
                        case DATA -> {
                            System.out.print("DATA: ");
                            printByteBuffer(m.getData(), m.getData().capacity());
                            //------------------- RECEIVING SYN ROUTING TABLE -------------------//

                            if (m.getData().get(1) >> (byte) 4 == 4) {           //if we receive a SYN routing table we first read the routing table

                                ArrayList<Byte> senderNeighbours = m.readReceivedRoutingTable();
                                ArrayList<Byte> ourNeighbours = getNodesInRange();

                                //FIRST --- add only the destinations we currently don't have in range with next hop - the sender of the routing table
                                for (Byte senderNeighbour : senderNeighbours) {
                                    neighbours.putIfAbsent(senderNeighbour, m.getData().get(0));
                                }
                                //SECOND - check if the nodes that are the same as in our routing table, we are still in direct contact with by checking if we have received a PONG from them recently
                                for (Byte ourNeighbour : ourNeighbours) {
                                    for (Byte senderNeighbour : senderNeighbours) {

                                        if (ourNeighbour.equals(senderNeighbour)) {

                                            if (!hasRecentlyReceivedPONG(ourNeighbour)) {            //if we haven't received a PONG from that IP, update it
                                                neighbours.put(ourNeighbour, m.getData().get(0));    //with next hop being the sender of the routing table
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        case HELLO -> {
                            System.out.println("HELLO");
                            mediumIsFree = true;
                        }
                        case FREE -> {
                            System.out.println("FREE");
                            mediumIsFree = true;
                        }
                        case BUSY -> {
                            System.out.println("BUSY");
                            mediumIsFree = false;
                        }
                        case SENDING -> mediumIsFree = false;
                        case DONE_SENDING -> System.out.println("DONE_SENDING");
                        case END -> {
                            System.out.println("END");
                            System.exit(0);
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println("Failed to take from queue: " + e);
                }
            }
        }
    }

    private class receiveDataShortThread extends Thread {

         @Override
        public void run() {

             while (true) {

             }
         }
    }
    //---------------------------------------------- End of receive thread ----------------------------------------------//

    public int getIp() {
        return ip;
    }

    public HashMap<Byte,Byte> getNeighbours() {
        return neighbours;
    }

    /**
     * Extracts from our routing table only the nodes, we are in direct contact with.
     *
     * @return an array containing only the destination nodes from our routing table, where they are the next hop
     */
    public ArrayList<Byte> getNodesInRange() {
        ArrayList<Byte> directNeighbours = new ArrayList<>();
        for (Byte destination: neighbours.keySet()) {
            if ((byte)destination == neighbours.get(destination)) {  //we only add to the list if the key is the same as the value
                directNeighbours.add(destination);
            }
        }
        return directNeighbours;
    }

    /**
     * Checks the last 5 PINGs we have received, if at least one of them is from the specified IP.
     * @param IP to check
     * @return true if we have received a PING from the specified IP in the last 5 PINGs
     */
    public boolean hasRecentlyReceivedPONG(byte IP){
        int i = 0;
        while (i < 5) {
            try {
                if (recentlyReceivedPONGs.take() == IP) {
                    return true;
                }
                i++;
            } catch (InterruptedException e) {
                System.err.println("error");
            }
        }
        return false;
    }
}

