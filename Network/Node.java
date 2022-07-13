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
    private ArrayList<Byte> recentlyReceived;
    public BlockingQueue<Message> forwardingMessages;
    private String receivedMessage;
    private ArrayList<Byte> ourNeighbours;

    //ROUTING TABLE -> key - destination | value - next hop
    private ArrayList<Byte> routingTable;
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
     * Starts the discovery and routing sequences of a Node.(if transmit thread is started)
     */
    public void initialize() {
        mediumIsFree = false;
        PONGsToSend = new LinkedBlockingQueue<>();
        recentlyReceived = new ArrayList<>();
        routingTable = new ArrayList<>();
        receivedMessage = "";
        ourNeighbours = new ArrayList<>();
        forwardingMessages = new LinkedBlockingQueue<>();

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
     * Thread for starting the initialisation sequence of a Node.
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
            if (mediumIsFree && canSend(8)) {
                //System.out.println(getIp() + " is sending a PING ");
                sendPING();
            }
            while (true) {
                try {

                    if (mediumIsFree && !forwardingMessages.isEmpty()) {
                        if (canSend(15)) {
                            putMessageInSendingQueue(forwardingMessages.take());
                        }
                        Thread.sleep(500);
                    }

//                    routingPacket = new LinkStateRoutingPacket(ip, cloneArrayList(ourNeighbours));
//                    routingMessage = routingPacket.convertToMessage();
//                    if (mediumIsFree && canSendPing(100000)) {
//                        System.out.println(getIp() + " is sending a Routing Message ");
//                        putMessageInSendingQueue(routingMessage);
//                        Thread.sleep(5000);
//                    }

                } catch (InterruptedException e) {
                    System.out.println("Node failed to send routing info " + e);
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

        private long timeInterval = 1000;

        @Override
        public void run() {
            while (true) {
                if (mediumIsFree && canSend(8)) {
                    System.out.println(getIp() + " is sending a PING ");
                    sendPING();
                }
                try {
                    Thread.sleep(timeInterval);
                } catch (InterruptedException e) {
                    System.err.println("Failed to send PING " + e);
                    break;
                }
                timeInterval = timeInterval + 1000;
                if (timeInterval > 10000) {
                    timeInterval = 5000;
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
        private int timeInterval = 5000;
        @Override
        public void run() {
            while (true) {
                try {
                    if (mediumIsFree && PONGsToSend.size() > 0 && canSend(8)) {
                        System.out.println(getIp() + " is sending a PONG ");
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

    //---------------------------------------------- End of sending threads ----------------------------------------------//


    //---------------------------------------------- Start of receive thread ----------------------------------------------//
    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }

        public void run() {
            while (true) {
                try {
                    Message m = receivedQueue.take();
                    MessageType type = m.getType();
                    switch (type) {
                        case DATA_SHORT -> {
                            if (!(m.getData().get(0) == getIp())) {
                                recentlyReceived.add(m.getData().get(0));                          //keep track of all the IPs we recently
                                if (!routingTable.contains(m.getData().get(0))) {                  //received a message from
                                    routingTable.add(m.getData().get(0));
                                }
                            }
                            //------------------- RECEIVING A PING -------------------//    //[sender of PING] + [01000000]
                            if (m.getData().get(1) == 64) {                                 //only if is message is SYN, send a response
                                if (m.getData().get(0) == getIp()) {                        //if the sender has the same ip as us, we change ours
                                    System.out.println(getIp() + " received a PING from " + m.getData().get(0));
                                    ip =(new Random().nextInt((int) System.currentTimeMillis())) % 64;
                                    System.out.println("Someone has the same IP as us, so we changed ours to: " + ip);
                                    sendPING();
                                } else if(m.getData().get(0) != getIp()) {
                                    PONGsToSend.put(m.respondToDiscoverySYN((byte) getIp()));
                                }
                            }
                            //------------------- RECEIVING A PONG -------------------//
                            else if ((m.getData().get(1))  == (byte) (getIp() + 128)) { //if message is ACK, just add to neighbour's map
                                System.out.println(getIp() + " received a PONG from " + m.getData().get(0));
                            }
                        }
                        case DATA -> {
                            if (!(m.getData().get(0) == getIp())) {
                                recentlyReceived.add(m.getData().get(0));
                                if (!routingTable.contains(m.getData().get(0))) {
                                    routingTable.add(m.getData().get(0));
                                }
                            }
                            ByteBuffer buffer = m.getData();
                            //------------------- RECEIVING ROUTING TABLE -------------------//
                            if (buffer.get(0) != getIp() && buffer.get(1) >> (byte) 4 == 4) {
                                ArrayList<Byte> senderNeighbours = m.readReceivedRoutingTable();
                                //--- add to our table only the destinations we currently don't have in range with next hop being the sender of the routing table
                                for (Byte senderNeighbour : senderNeighbours) {
                                    if (!routingTable.contains(senderNeighbour) && senderNeighbour != getIp()) {
                                        routingTable.add(senderNeighbour);
                                    }
                                }
                                for (Byte key: cloneArrayList(ourNeighbours)) {
                                    if (!hasRecentlyReceivedPONG(key)) {
                                        routingTable.remove(key);
                                        ourNeighbours.remove(key);
                                    }
                                }
                            }
                            //------------------- RECEIVING A FORWARDED MESSAGE -------------------//
                            if (buffer.get(1) == getIp()) {

                                int seq = m.getData().get(4) >> 4 ;
                                int sw = m.getData().get(4) - (((byte) seq) << 4);

                                String payload = "";

                                for (int i = 5; i < buffer.get(2) + 5; i++) {               //build the single message
                                    if (buffer.get(i) != (byte) 0) {
                                        payload = payload + ((char) buffer.get(i));
                                    }
                                }
                                if (sw == 1) {              //if sending window is 1 print the single message
                                    System.out.println("Message from: " + buffer.get(0) + "...");
                                    System.out.println(payload + "\n");
                                }else {
                                    if (seq == sw) {
                                        receivedMessage = receivedMessage.concat(payload);
                                        System.out.println("Message from: " + buffer.get(0) + "...");
                                        System.out.println(receivedMessage + "\n");            //the message is formed
                                        receivedMessage = "";          //clear the String for the next message
                                    } else if (seq < sw){              //append the new message to form the whole text
                                        receivedMessage = receivedMessage.concat(payload);
                                    }
                                }
                            }
                            //------------------- RECEIVING MESSAGE WITH TTL 0 -------------------//
                            else if (buffer.get(1) < 64 && buffer.get(3) == 0){
                                //do nothing / discard packet
                            }
                            //------------------- RECEIVING MESSAGE NOT INTENDED FOR US -------------------//
                            else if (buffer.get(1) < 64 && buffer.get(0) != getIp()){//not a routing packet + not for us
//                                System.out.println(getIp() + " forwarding a message toward " + buffer.get(1));
//                                System.out.println("TTL = " + buffer.get(3));
//                                System.out.println("We set the TTL to: " + m.getData().get(3));
                                forwardingMessages.put(m.decrementTTL(buffer));
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

    public ArrayList<Byte> getRoutingTable() {
        return routingTable;
    }

    public synchronized boolean hasRecentlyReceivedPONG(byte IP){
        int i = 1;
        while (i < 10) {
            if (i <= recentlyReceived.size() && recentlyReceived.get(recentlyReceived.size() - i).equals(IP)) {
                return true;
            }
            i++;
        }
        if (recentlyReceived.size() == 10) {
            recentlyReceived.clear();
        }
        return false;
    }

    /**
     * For every method call, returns true with a probability of 16%.
     *
     * @return true if a newly generated random number between 0 and 5 is equal to zero
     */
    private boolean canSend(int n) {
        return new Random().nextInt(n)==5;
    }

    public synchronized ArrayList<Byte> cloneArrayList(ArrayList<Byte> rt){
        rt.clone();
        return rt;
    }
}

