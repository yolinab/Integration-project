package Network;

import Packets.DiscoveryPacket;
import Packets.LinkStatePacket;
import client.Client;
import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
    private static final String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    private static final int SERVER_PORT = 8954;
    private static final int frequency = 5300;
    private final int ip;
    private boolean mediumIsFree;
    private Message discoveryMessage;
    private BlockingQueue<Message> PONGsToSend;

    //ROUTING TABLE -> key - destination | value - next hop
    private HashMap<Byte,Byte> neighbours;
//    private ArrayList<Byte> nodesInRange;
    private Message routingMessage;
    private LinkStatePacket LSP;

    public BlockingQueue<Message> receivedDataQueue;
    public BlockingQueue<Message> receivedShortDataQueue;
    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node() {
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;//mod 64 so it fits is 6 bits
        mediumIsFree = true;
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        PONGsToSend = new LinkedBlockingQueue<Message>();
        neighbours = new HashMap<>(3);
//        nodesInRange = new ArrayList<>();

//        LinkStatePacket routingPacket = new LinkStatePacket(ip, neighbours);
//        routingMessage = routingPacket.convertToMessage();

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use
        new receiveThread(receivedQueue).start(); ///has to be started before transmit thread, so mediumState is set accurately

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
    //synchronized - so only one thread can access at a time
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
    private synchronized void sendDiscoveryMessage() {
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

        public transmitThread(BlockingQueue<Message> sendingQueue){
            super();
            this.sendingQueue = sendingQueue;
            sendPINGs = new sendPINGsThread();
            sendPONGs = new sendPONGsThread();
        }

        @Override
        public void run() {
            sendPINGs.start();                        //starts the thread for sending PINGs, on 15 second intervals
            sendPONGs.start();
            while (true) {
                try {
                    Thread.sleep(1000);
                    //TODO: routing sequence
//                    //If we have received SYNs, send ACKs
//                    if (mediumIsFree && PONGsToSend.size() > 0) {
//                        System.out.println(getIp() + " is sending a PONG.");
//                        putMessageInSendingQueue(PONGsToSend.take());
//                    }
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

        private final long timeInterval = 15000;        //execute every 15 seconds

        @Override
        public void run() {
            while (true) {
                if (PONGsToSend.isEmpty() && mediumIsFree) {
                    System.out.println(getIp() + " is sending a PING.");
                    sendDiscoveryMessage();
                }
                try {
                    Thread.sleep(timeInterval); //does it make transmit thread sleep or is runnable a separate thread
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

        @Override
        public void run() {
            while (true) {
                try {
                    //If we have received SYNs, send ACKs
                    if (mediumIsFree && PONGsToSend.size() > 0) {
                        System.out.println(getIp() + " is sending a PONG.");
                        putMessageInSendingQueue(PONGsToSend.take());
                    }
                    Thread.sleep(500);
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
//                            System.out.print("DATA_SHORT: ");
//                            printByteBuffer(m.getData(), m.getData().capacity());
                            System.out.println();
                            receivedShortDataQueue.put(m);

                            //------------------- RECEIVING A PING -------------------//
                            if (m.getData().get(1) == 64) {                                  //only if is message is SYN, send a response
                                System.out.println(getIp() + " received a PING.");
                                printByteBuffer(m.getData(), m.getData().capacity());

                                PONGsToSend.put(m.respondToDiscoverySYN((byte) getIp()));     //send a response through sending thread
                                neighbours.put(m.getData().get(0),m.getData().get(0));

                                //------------------- RECEIVING A PONG -------------------//
                            } else if ((m.getData().get(1))  == 0) {                    //if message is ACK, just add to neighbour's map
                                System.out.println(getIp() + " received a PONG from " + m.getData().get(0));
                                neighbours.put(m.getData().get(0),m.getData().get(0));
                            }
                        }
                        case DATA -> {
                            System.out.print("DATA: ");
                            printByteBuffer(m.getData(), m.getData().capacity());
                            receivedDataQueue.put(m);
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
                        case SENDING -> System.out.println("SENDING");
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

    public HashMap<Byte,Byte> getNeighbours() {
        return neighbours;
    }

//    public ArrayList<Byte> getNodesInRange() {
//        return nodesInRange;
//    }
}

