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
    private HashMap<Byte,Byte> routingTable;
    private Message routingMessage;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node(String serverIP, int serverPort, int frequency) {
        receivedQueue = new LinkedBlockingQueue<>();
        sendingQueue = new LinkedBlockingQueue<>();
        new Client(serverIP, serverPort, frequency, receivedQueue, sendingQueue);
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
        if (ip == 0) {ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;}
    }

    /**
     * Starts the discovery and routing sequences of a Node.
     */
    public void initialize() {
        mediumIsFree = false;
        PONGsToSend = new LinkedBlockingQueue<>();
        recentlyReceivedPONGs = new LinkedBlockingQueue<>();
        routingTable = new HashMap<>(3);

        new receiveThread(receivedQueue).start();

        new transmitThread(sendingQueue).start();
    }

    /**
     * Placing messages in the sending queue is done ONLY by using this method.
     * By avoiding race conditions, we ensure fair queueing.
     *
     * @param message Message to be sent
     */
    public synchronized void putMessageInSendingQueue(Message message) {
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
            sendRoutingInfo.start();
            while (true) {
                try {
                    Thread.sleep(15000);//TODO - deal with later
                } catch (InterruptedException e) {
                    System.out.println("Failed to send data. " + e);
                    break;
                }
            }
        }
    }

    /**
     * A separate thread for broadcasting PING messages.
     *
     * It is used in the discovery phase to determine the nodes that are in range,
     * and keeps running to adapt to change in network topology.
     */
    public class sendPINGsThread extends Thread {

        private long timeInterval = 8000;
        boolean send;

        @Override
        public void run() {
            while (true) {
                if (mediumIsFree) {
                    sendPING();
                }
                try {
                    Thread.sleep(timeInterval);
                } catch (InterruptedException e) {
                    System.err.println("Failed to send PING " + e);
                    break;
                }
                timeInterval = timeInterval + 100;
                if (timeInterval > 20000) {
                    timeInterval = 3000;
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
        @Override
        public void run() {
            while (true) {
                try {
                    if (mediumIsFree && PONGsToSend.size() > 0) {
                        putMessageInSendingQueue(PONGsToSend.take());
                    }
                    Thread.sleep(timeInterval);
                } catch (InterruptedException e) {
                    System.out.println("Failed to send data. " + e);
                    break;
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
        LinkStateRoutingPacket routingPacket;
        @Override
        public void run() {
            //Before it sends it's first routing table, it waits 10 seconds
            //Updates neighbours about its routing table every 10 seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.err.println("Failed to send routing table.");
            }

            while (true) {
                routingPacket = new LinkStateRoutingPacket(ip, routingTable);
                routingMessage = routingPacket.convertToMessage();
                try {
                    if (mediumIsFree && PONGsToSend.isEmpty()) {
                        putMessageInSendingQueue(routingMessage);
                        Thread.sleep(10000);
                    }
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
//                                System.out.println(getIp() + " received a PONG from " + m.getData().get(0) + " .");
                                routingTable.put(m.getData().get(0),m.getData().get(0));
                                recentlyReceivedPONGs.put(m.getData().get(0));
                            }
                        }
                        case DATA -> {
                            ByteBuffer buffer = m.getData();
                            //------------------- RECEIVING ROUTING TABLE -------------------//
                            if (buffer.get(0) != getIp() && buffer.get(1) >> (byte) 4 == 4) {

                                ArrayList<Byte> senderNeighbours = m.readReceivedRoutingTable();
                                //--- add to our table only the destinations we currently don't have in range with next hop being the sender of the routing table
                                for (Byte senderNeighbour : senderNeighbours) {
                                    if (!getRoutingTable().containsKey(senderNeighbour) && senderNeighbour != getIp()) {
                                        routingTable.put(senderNeighbour, buffer.get(0));
                                    }
                                }
                            }
                            //------------------- RECEIVING A FORWARDED MESSAGE -------------------//
                            if (buffer.get(1) == getIp()) {
                                System.out.println("Message from: " + buffer.get(0) + "...");
                                for (int i = 4; i < buffer.get(2) + 4; i++) {
                                    System.out.print((char) buffer.get(i));
                                }
                                System.out.println();
                            }
                            //------------------- RECEIVING MESSAGE WITH TTL 0 -------------------//
                            else if (buffer.get(1) < 64 && buffer.get(3) == 0){
                                //do nothing / discard packet
                            }
                            //------------------- RECEIVING MESSAGE NOT INTENDED FOR US -------------------//
                            else if (buffer.get(1) < 64 && buffer.get(0) != getIp()){//not a routing packet + not for us
                                System.out.println(getIp() + " forwarding a message toward " + buffer.get(1));
                                System.out.println("TTL = " + buffer.get(3));
                                putMessageInSendingQueue(m.decrementTTL());
                                System.out.println("We set the TTL to: " + m.getData().get(3));
                            }
                        }
                        case HELLO -> {
                            System.out.println("HELLO");
                            mediumIsFree = true;
                        }
                        case FREE -> {
                            mediumIsFree = true;
                        }
                        case BUSY -> {
                            mediumIsFree = false;
                        }
                        case SENDING -> mediumIsFree = false;
                        case DONE_SENDING -> {
                        }
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
            if ((byte)destination == routingTable.get(destination)) {
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

    /**
     * Removes a node from the routing table if it hasn't received a recent PONG from them.
     *
     * A PONG is considered recent if its one of the last five received PONGs in the recentlyReceivedPONGs queue.
     */
    public void updateRoutingTable() {
        for (Byte key: routingTable.keySet()) {
            if (!hasRecentlyReceivedPONG(key)) {
                routingTable.remove(key);
            }
        }
    }
}

