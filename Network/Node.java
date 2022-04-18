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
    private final int ip;
    private boolean mediumIsFree;
    private Message discoveryMessage;
    private BlockingQueue<Message> PONGsToSend;
    private ArrayList<Byte> recentlyReceived;
    private BlockingQueue<Message> routingMessagesToSend;

    //ROUTING TABLE -> key - destination | value - next hop
    private HashMap<Byte,Byte> routingTable;
    private Message routingMessage;

    public BlockingQueue<Message> receivedDataQueue;
    public BlockingQueue<Message> receivedShortDataQueue;
    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node(String serverIP, int serverPort, int frequency) {
        receivedQueue = new LinkedBlockingQueue<>();
        sendingQueue = new LinkedBlockingQueue<>();
        new Client(serverIP, serverPort, frequency, receivedQueue, sendingQueue);
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
    }

    /**
     * Starts the discovery and routing sequences of a node.
     */
    public void initialize() {
        mediumIsFree = false;
        PONGsToSend = new LinkedBlockingQueue<>();
        recentlyReceived = new ArrayList<>();
        routingTable = new HashMap<>(3);
        routingMessagesToSend = new LinkedBlockingQueue<>();
        new receiveThread(receivedQueue).start();

        DiscoveryPacket discoveryPacket = new DiscoveryPacket(ip);
        discoveryMessage = discoveryPacket.convertToMessage();

        new transmitThread(sendingQueue).start();
    }

    /**
     * Placing messages in the sending queue is done ONLY by using this method.
     * By avoiding race conditions, we ensure fair queueing.
     *
     * @param msgToPutInSendingQueue message to be sent
     */
    private synchronized void putMessageInSendingQueue(Message msgToPutInSendingQueue) {
        try {
            sendingQueue.put(msgToPutInSendingQueue);
        } catch (InterruptedException e) {
            System.err.println("Failed to put message in sending queue." + e);
        }
    }

    /**
     * Puts a PING message in the sending queue.
     */
    private synchronized void sendPING() {
        putMessageInSendingQueue(discoveryMessage);
    }

    //---------------------------------------------- Start of sending threads ----------------------------------------------//
    /**
     * Thread for transmitting messages.
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
            sendRoutingInfo.start();
            while (true) {
                try {
                    Thread.sleep(10000);
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
        // ≈ 20% probability to send
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
     * A separate thread for sending a nodes routing information at a specified time interval.
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
            if (routingMessagesToSend.size() == 0) {
                LinkStateRoutingPacket routingPacket = new LinkStateRoutingPacket(getIp(), routingTable);
                routingMessage = routingPacket.convertToMessage();
                try {
                    routingMessagesToSend.put(routingMessage);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (true) {
                try {
                    putMessageInSendingQueue(routingMessagesToSend.take());
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
            receivedDataQueue = new LinkedBlockingQueue<>();
            receivedShortDataQueue = new LinkedBlockingQueue<>();
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print(  ( bytes.get(i) ) );
//                System.out.println(String.format("%8s",Integer.toBinaryString(bytes.get(i))).replace(' ','0'));
            }
        }

        public void run() {
            while (true) {
                try {
                    Message m = receivedQueue.take();
                    MessageType type = m.getType();
                    switch (type) {
                        case DATA_SHORT -> {
                            receivedShortDataQueue.put(m);
                            //Whatever we receive means the sender is our neighbour
                            //We put the sender in the recently received list
                            if (!(m.getData().get(0) == getIp())) {
                                recentlyReceived.add(m.getData().get(0));
                            }
                            //------------------- RECEIVING A PING -------------------//    //[sender of PING] + [01000000]
                            if (m.getData().get(1) == 64) {                                 //only if is message is SYN, send a response
                                System.out.println(getIp() + " received a PING. ");
                                printByteBuffer(m.getData(), m.getData().capacity());
                                System.out.println();
                                PONGsToSend.put(m.respondToDiscoverySYN((byte) getIp()));   //send a response through sending thread
                            }
                            //------------------- RECEIVING A PONG -------------------//    //[the node that ACKed our SYN] + [00000000]
                            else if ((m.getData().get(1))  == (byte) (getIp() + 128)) {                          //if message is ACK, just add to neighbour's map
                                System.out.println(getIp() + " received a PONG from " + m.getData().get(0) + " .");
                                routingTable.put(m.getData().get(0),m.getData().get(0));
                            }
                        }
                        case DATA -> {
                            System.out.print("DATA: ");
                            printByteBuffer(m.getData(), m.getData().capacity());
//                            for (Byte dest: neighbours.keySet()) {
//                                System.out.println(dest);
//                            }
                            receivedDataQueue.put(m);
                            //------------------- RECEIVING A ROUTING MESSAGE -------------------//

                            //When we receive a DATA message we check the flag to see if corresponds to a routing message
                            if (m.getData().get(1) >> (byte) 4 == 4) {
                                //FIRST --- we add the sender to our routing table and to the recently received list
                                routingTable.put(m.getData().get(0),m.getData().get(0));
                                recentlyReceived.add(m.getData().get(0));

                                ArrayList<Byte> senderNeighbours = m.readReceivedRoutingTable();
                                ArrayList<Byte> ourNeighbours = getNodesInRange();

                                //SECOND --- add only the destinations we currently don't have in range
                                //with next hop - the sender of the routing table
                                for (Byte senderNeighbour : senderNeighbours) {
                                    if (senderNeighbour!=getIp() && !routingTable.containsKey(senderNeighbour)) {
                                        routingTable.put(senderNeighbour, m.getData().get(0));
                                    }
                                }

                                //THIRD --- if the topology has changed we remove the old info, so it can update anew
                                for (Byte ourNeighbour : ourNeighbours) {
                                    //if that IP is not our neighbour anymore, remove it
                                    if (!hasRecentlyReceivedPONG(ourNeighbour)) {
                                        //routingTable.put(ourNeighbour, m.getData().get(0));
                                        routingTable.remove(ourNeighbour);
                                        //if we used that IP for a next hop as well, remove it
                                        if (routingTable.containsValue(ourNeighbour)) {
                                            //routingTable.put(findKey(routingTable, ourNeighbour), m.getData().get(0));
                                            routingTable.remove((findKey(routingTable, ourNeighbour)), ourNeighbour);
                                        }
                                    }
                                }

                                //controlling the list's size to not exceed 20
                                //removing the excessive  information in the front
                                while (recentlyReceived.size() > 20) {
                                    recentlyReceived.remove(0);
                                }

                                for (int j = 1; j < recentlyReceived.size(); j++) {
                                    System.out.println("Recently received PONG " + recentlyReceived.get(recentlyReceived.size() - j) + " with INDEX " + ( recentlyReceived.size() - j));
                                }

                                for (Byte b: routingTable.keySet()) {
                                    System.out.println("\nDestination: " + b);
                                    System.out.println("Next Hop: " + routingTable.get(b));
                                }
                            }
                            //with the updated routing table we create a new routing message, we put it in the queue
                            LinkStateRoutingPacket LSP1 = new LinkStateRoutingPacket(getIp(), routingTable);
                            routingMessagesToSend.put(LSP1.convertToMessage());
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
    //---------------------------------------------- End of receive thread ----------------------------------------------//

    public int getIp() {
        return ip;
    }

    public HashMap<Byte,Byte> getRoutingTable() {
        return routingTable;
    }

    /**
     * Extracts from our routing table only the nodes, we are in direct contact with.
     *
     * @return an array containing only the destination nodes from our routing table, where they are the next hop
     */
    public ArrayList<Byte> getNodesInRange() {
        ArrayList<Byte> directNeighbours = new ArrayList<>();
        for (Byte destination: routingTable.keySet()) {
            if ((byte)destination == routingTable.get(destination)) {  //we only add to the list if the key is the same as the value
                directNeighbours.add(destination);
            }
        }
        return directNeighbours;
    }

    /**
     * Finds the key of a given HashMap, knowing the corresponding value
     * @param map from which we need a key
     * @param value to check
     * @return the key mapped to this value
     */
    public Byte findKey(Map<Byte, Byte> map, Byte value) {
        Byte result = null;
        for ( Byte key : map.keySet() ) {
            if (map.get(key).equals(value)) {
                result = key;
            }
        }
        return result;
    }

    /**
     * Checks the last 20 PINGs we have received, if at least one of them is from the specified IP.
     * @param IP to check
     * @return true if we have received a PING from the specified IP in the last 20 PINGs
     */
    public boolean hasRecentlyReceivedPONG(byte IP){
        int i = 1;
        while (i < 20) {
            if (i <= recentlyReceived.size() && recentlyReceived.get(recentlyReceived.size() - i).equals(IP)) {
                return true;
            } i++;
        }
        return false;
    }
}


