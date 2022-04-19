package Network;

import Packets.DiscoveryPacket;
import Packets.LinkStateRoutingPacket;
import client.Client;
import client.Message;
import client.MessageType;
import out.production.java_framework_integration_project.Packets.AckPacket;
import out.production.java_framework_integration_project.Packets.DataPacket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    private final int ip;
    private boolean mediumIsFree;
    boolean inputting;

    private Message discoveryMessage;
    private BlockingQueue<Message> PONGsToSend;
    private ArrayList<Byte> recentlyReceived;
    private ArrayList<Byte> receivedACKs;
    private ArrayList<Byte> ourNeighbours;
    private BlockingQueue<Message> routingMessagesToSend;

    //ROUTING TABLE -> key - destination | value - next hop
    private HashMap<Byte,Byte> routingTable;
    private Message routingMessage;

    public BlockingQueue<Message> receivedDataQueue;
    public BlockingQueue<Message> receivedShortDataQueue;
    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;
    private BlockingQueue<Message> ackToSend;
    private BlockingQueue<Message> dataMessagesToSend;
    private int syn;
    private  long startInput;


    public Node(String serverIP, int serverPort, int frequency) {
        receivedQueue = new LinkedBlockingQueue<>();
        sendingQueue = new LinkedBlockingQueue<>();
        new Client(serverIP, serverPort, frequency, receivedQueue, sendingQueue);
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
        //syn = (new Random().nextInt((int) System.currentTimeMillis())) % 128;
        syn = 1;
    }

    public void initialize() {
        mediumIsFree = false;
        PONGsToSend = new LinkedBlockingQueue<>();
        recentlyReceived = new ArrayList<>();
        ourNeighbours = new ArrayList<>();
        receivedACKs = new ArrayList<>();
        routingTable = new HashMap<>(3);
        routingMessagesToSend = new LinkedBlockingQueue<>();
        dataMessagesToSend = new LinkedBlockingQueue<>();
        ackToSend = new LinkedBlockingQueue<>();
        new receiveThread(receivedQueue).start();


        inputting = false;

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
     * Currently the sending thread is used only for starting the PING and PONG sending threads
     */
    private  class transmitThread extends Thread {
        private BlockingQueue<Message> sendingQueue;
        private Thread sendPINGs;
        private Thread sendPONGs;
        private Thread sendRoutingInfo;
        private Thread sendDataThread;


        public transmitThread(BlockingQueue<Message> sendingQueue){
            super();
            this.sendingQueue = sendingQueue;
            sendPINGs = new sendPINGsThread();
            sendPONGs = new sendPONGsThread();
            sendRoutingInfo = new sendRoutingInfoThread();
            sendDataThread = new sendDataThread();
        }

        @Override
        public void run() {
            sendPINGs.start();
            sendPONGs.start();
            sendRoutingInfo.start();
            sendDataThread.start();
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

        //time interval at which to try to send a PING
        private final long timeInterval = 10000;
        // ≈ 33% probability to send
        boolean send;
        //counts how many PINGs a node has sent, so overtime it can decrease the rate it's sending them at
        int counter;

        @Override
        public void run() {
            while (true) {
                send = new Random().nextInt(8) == 0; //how to make it different for every time it has to send, because now it's unfair

                if (mediumIsFree && PONGsToSend.isEmpty() && counter < 5) {     //we are still in discovery phase
                  //  System.out.println(getIp() + " is sending a PING.");
                    sendPING();
                    counter++;
                }
                while (send) {
                    if (mediumIsFree && PONGsToSend.isEmpty() && send) {            //we decrease the rate at which we are sending
                      //  System.out.println(getIp() + " is sending a PING.");
                        sendPING();
                        counter++;
                    }
                    send = new Random().nextInt(9) == 0;
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
                        //If we have received SYNs, send ACKs
                        if (mediumIsFree && PONGsToSend.size() > 0) {
                          //  System.out.println(getIp() + " is sending a PONG.");
                            putMessageInSendingQueue(PONGsToSend.take());
                        }
                        Thread.sleep(timeInterval);
                        send = (new Random().nextInt(9) == 0);
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

        boolean send;

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
                send =(new Random().nextInt(8) == 0);
                while (send) {
                    try {
                        putMessageInSendingQueue(routingMessagesToSend.take());
                        Thread.sleep(10000);
                        send = (new Random().nextInt(9) == 0);

                    } catch (InterruptedException e) {
                        System.out.println("Node failed to send routing info " + e);
                    }
                }
            }
        }
    }

    private class sendDataThread extends Thread {

        @Override
        public void run() {
            //Before it starts data, it waits 20 seconds
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                System.err.println("Failed to send data message.");
            }
            //inputting = true;                                         //attempt to provide time for input
            try {
                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
                String input = "";
                DataPacket dataPacket;
                long start;                                            //for later use of timeout
                long end = 0;

                //startInput = System.currentTimeMillis();              //attempt to provide time for input N2

                System.out.println("Input destination/message");
                input = inp.readLine();// read input
                String[] parsed = input.split("/");
                for (int i =0; i <parsed.length; i++) {
                    System.out.println("\n" + parsed[i]);
                }
                System.out.println(input);                              //for debugging

                byte[] payloadBytes = parsed[1].getBytes();           // get bytes from input
                dataPacket = new DataPacket(getIp(), Byte.parseByte(parsed[0]), syn, payloadBytes);

                //TEST
//                String string = "Hello, nice to meet you.";
//                byte[] payloadBytes = string.getBytes(StandardCharsets.UTF_8);
//                dataPacket = new DataPacket(getIp(),(int) routingTable.keySet().toArray()[0], syn, payloadBytes);

                dataMessagesToSend.put(dataPacket.convertToMessage());

                //after timeout if we haven't received an ack for the previous message retransmit
                if (System.currentTimeMillis() > end && !receivedACKs.contains((byte) syn)) {
                   // System.out.println("Retransmitting packet with SYN " + syn);
                    putMessageInSendingQueue(dataPacket.convertToMessage());
                } else if (receivedACKs.contains((byte) syn)) {
                    syn++;                                          //if all the ACKs are received increment the syn for the next message
                }

                while (true) {
                    if (mediumIsFree) {
                        if (ackToSend.size() > 0) {                 //first send the ACKs waiting to be sent
                            putMessageInSendingQueue(ackToSend.take());
                        } else {
                            start = System.currentTimeMillis();
                            end = start + 300000;

                            putMessageInSendingQueue(dataMessagesToSend.take());

                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
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
                                routingTable.put(m.getData().get(0),m.getData().get(0));
                                if (!ourNeighbours.contains(m.getData().get(0))) {
                                    ourNeighbours.add(m.getData().get(0));
                                }
                               // System.out.println("Receive DATA_SHORT");
//                                for (Byte b: routingTable.keySet()) {
//                                    System.out.println("\nDestination: " + b);
//                                    System.out.println("Next Hop: " + routingTable.get(b));
//                                }
                            }

                            //------------------- RECEIVING A PING -------------------//    //[sender of PING] + [01000000]
                            if (m.getData().get(1) == 64) {                                 //only if is message is SYN, send a response
                               // System.out.print(getIp() + " received a PING.from " + m.getData().get(0) + " .");
                                //printByteBuffer(m.getData(), m.getData().capacity());
                                PONGsToSend.put(m.respondToDiscoverySYN((byte) getIp()));   //send a response through sending thread
                            }
                            //------------------- RECEIVING A PONG -------------------//    //[the node that ACKed our SYN] + [00000000]
                            else if ((m.getData().get(1))  == 0) {                          //if message is ACK, just add to neighbour's map
                               // System.out.print(getIp() + " received a PONG from " + m.getData().get(0) + " .");
                                //routingTable.put(m.getData().get(0),m.getData().get(0));
                            }
                            //------------------- RECEIVING A DIRECTED PING -------------------//

                            //if the second byte is out IP, we are being checked
                            else if ((m.getData().get(1) - 64) == getIp()) {
                               // System.out.println(m.getData().get(0) + " wants to check if we are still in range.");
                                PONGsToSend.put(m.respondToDirectedPING());
                            }
                            //------------------- RECEIVING A DIRECTED PONG -------------------//
                            else if ((m.getData().get(1))  == (byte) (getIp() + 128)) {                          //if message is ACK, just add to neighbour's map
                                //System.out.println(getIp() + " received a PONG from " + m.getData().get(0) + " .");
                                //routingTable.put(m.getData().get(0),m.getData().get(0));
                            }
                            //------------------- RECEIVING AN ACK -------------------//
                            else if (m.getData().get(1) == (byte) 1) {
                                receivedACKs.add(m.getData().get(0));                       //store the ACKs in a list
                            }
                        }
                        case DATA -> {
                            //System.out.print("DATA: ");
                           // printByteBuffer(m.getData(), m.getData().capacity());
                            receivedDataQueue.put(m);
                            //------------------- RECEIVING A ROUTING MESSAGE -------------------//

                            //When we receive a DATA message we check the flag to see if corresponds to a routing message
                            if (m.getData().get(1) >> (byte) 4 == 4) {
                                //FIRST --- we add the sender to our routing table and to the recently received list
                                routingTable.put(m.getData().get(0),m.getData().get(0));
                                recentlyReceived.add(m.getData().get(0));

                                ArrayList<Byte> senderDestinations = m.findSenderDestinations();
                               // ArrayList<Byte> senderNextHops = m.findSenderNextHops();

                                //SECOND --- add only the destinations we currently don't have in range
                                //with next hop - the sender of the routing table
                                int i = 0;
                                for (Byte senderDest : senderDestinations) {
                                   // System.out.println("Destination of " + m.getData().get(0) + " is " + senderDest);
                                    if (senderDest!=getIp() && !routingTable.containsKey(senderDest)) {
                                        routingTable.put(senderDest, m.getData().get(0));
                                    }

//                                    if (routingTable.containsKey(senderDest)
//                                    && routingTable.get(senderDest) == m.getData().get(0)
//                                    && senderNextHops.get(i) == getIp()) {
//                                        routingTable.remove(senderDest);
//                                    }

                                }

                                //THIRD --- if the topology has changed we remove the old info, so it can update anew
                                for (Byte ourNeighbour : ourNeighbours) {
                                    //if that IP is not our neighbour anymore, remove it
                                    if (!hasRecentlyReceivedPONG(ourNeighbour)) {
                                        routingTable.put(ourNeighbour, m.getData().get(0));
                                        //routingTable.remove(ourNeighbour);
                                        //if we used that IP for a next hop as well, remove it
                                        if (routingTable.containsValue(ourNeighbour)) {
                                            routingTable.put(findKey(routingTable, ourNeighbour), m.getData().get(0));
                                           // routingTable.remove((findKey(routingTable, ourNeighbour)), ourNeighbour);
                                        }
                                    }
                                }

                                //controlling the list's size to not exceed 20
                                //removing the excessive  information in the front
                                while (recentlyReceived.size() > 10) {
                                    recentlyReceived.remove(0);
                                }

//                                for (int j = 1; j < recentlyReceived.size(); j++) {
//                                    System.out.println("Recently received PONG " + recentlyReceived.get(recentlyReceived.size() - j) + " with INDEX " + ( recentlyReceived.size() - j));
//                                }
//                                System.out.println("Receive DATA");
//
//                                for (Byte b: routingTable.keySet()) {
//                                    System.out.println("\nDestination: " + b);
//                                    System.out.println("Next Hop: " + routingTable.get(b));
//                                }
                            }
                            //with the updated routing table we create a new routing message, we put it in the queue
                            LinkStateRoutingPacket LSP1 = new LinkStateRoutingPacket(getIp(), routingTable);
                            routingMessagesToSend.put(LSP1.convertToMessage());

                            //------------------- RECEIVING A DATA MESSAGE -------------------//

                            if (m.getData().get(2) == (byte) 128) {     //Data message flag = 10000000
                                byte[] payload = new byte[28];
                                for (int i = 4; i < m.getData().capacity(); i++) {
                                    payload[i - 4] = m.getData().get(4);
//                                    if (m.getData().get(i)!=(byte)0) {
//                                        String string = String.valueOf(m.getData().get(i));
//                                        System.out.println(string);
//                                        message.append(string);
//                                    }
                                }
                                System.out.println("Received message");
                                String string = new String(payload);
                                System.out.println(string);
                                StringBuilder message = new StringBuilder("");

                                //Build the message


                                //we want to send an ACK - the same as the SYN we received
                               // System.out.println(message);
                                byte sendAck = m.getData().get(3);
                                ackToSend.put((new AckPacket(sendAck)).convertToMessage());
                            }

                        }
                        case HELLO -> {
                            System.out.println("HELLO");
                            mediumIsFree = true;
                        }
                        case FREE -> {
                            //System.out.println("FREE");
                            mediumIsFree = true;
                        }
                        case BUSY -> {
                           // System.out.println("BUSY");
                            mediumIsFree = false;
                        }
                        case SENDING -> mediumIsFree = false;
                       // case DONE_SENDING -> System.out.println("DONE_SENDING");
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
        while (i < 10) {
            if (i <= recentlyReceived.size() && recentlyReceived.get(recentlyReceived.size() - i).equals(IP)) {
                return true;
            } i++;
        }
        return false;
    }
}

