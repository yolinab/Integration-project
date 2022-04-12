package Network;

import Packets.DiscoveryPacket;
import client.Client;
import client.Message;
import client.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {

    private int ip;
    private boolean mediumIsFree;

    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    private static int SERVER_PORT = 8954;
    private static int frequency = 5301;

    public BlockingQueue<Message> receivedDataQueue;
    public BlockingQueue<Message> receivedShortDataQueue;
//    public BlockingQueue<Message> mediumState;

    public BlockingQueue<Message> ACKsToSend;

    public ArrayList<Byte> neighbours;

    public Message discoveryMessage;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node() {
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;//mod 64 so it fits is 6 bits

        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();
//        mediumState = new LinkedBlockingQueue<Message>();
        ACKsToSend = new LinkedBlockingQueue<Message>();
        mediumIsFree = false;

        neighbours = new ArrayList<>(3);

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use

        //has to be started before transmit thread, so mediumState is not null - has to contain at least MessageType.HELLO
        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!

        DiscoveryPacket discoveryPacket = new DiscoveryPacket(this.getIp());                        //creates discovery packet upon initialization
        discoveryPacket.makeSYN();

        discoveryMessage = discoveryPacket.convertToMessage();

        new transmitThread(sendingQueue).start();
    }

    //------------------------- Start of transmit thread -----------------------//
    private  class transmitThread extends Thread {
        private BlockingQueue<Message> sendingQueue;
        private Message message;

        public transmitThread(BlockingQueue<Message> sendingQueue){
            super();
            this.sendingQueue = sendingQueue;
        }

        /**
         * Method that is called only once, immediately after a node joins the network
         * to broadcasts its discovery message.
         */
        private void sendDiscoveryMessage() {
            putMessageInQueue(discoveryMessage);
        }

        /**
         * Placing messages in the sending queue is done only by using this method
         * to avoid mistakes and to centralize the operation
         * @param msgToPutInSendingQueue message to be sent
         */
        private void putMessageInQueue(Message msgToPutInSendingQueue) {
            try {
                sendingQueue.put(msgToPutInSendingQueue);
            } catch (InterruptedException e) {
                System.err.println("Failed to put message in sending queue." + e);
            }
        }

        public void run() {
            //if the last message in the medium state queue is HELLO => we are the first node to join the network?,
            //then we immediately start sending our discovery message
            if (ACKsToSend.isEmpty()) {
                System.out.println(getIp() + " is sending a SYN");
                sendDiscoveryMessage();
            }
            while (true) {
                //If we have received SYNs, send ACKs
                try {
                    if (mediumIsFree && ACKsToSend.size() > 0) {
                        System.out.println(getIp() + " is sending an ACK");
                        putMessageInQueue(ACKsToSend.take());
                    }
                    Thread.sleep(1000);         //if medium is busy wait 1 second
                } catch (InterruptedException e) {
                    System.out.println("Failed to send an ACK " + e);
                    break;
                }
            }
        }

//        private Message consoleInput() {
//            try {
//                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
//                String input = "";
//                while (true) {
//                    input = inp.readLine(); // read input
//                    byte[] inputBytes = input.getBytes(); // get bytes from input
//                    ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
//                    toSend.put(inputBytes, 0, inputBytes.length); // copy the input string into the byte buffer.
//                    Message msg;
//                    if ((input.length()) > 2) {
//                        msg = new Message(MessageType.DATA, toSend);
//                    } else {
//                        msg = new Message(MessageType.DATA_SHORT, toSend);
//                    }
//                    return msg;
//                }
//            } catch (IOException e) {
//                System.err.println("Failed to get input from the console. " + e);
//            }
//            //DANGER!!!//TODO fix this shit
//            return null;
//        }
    }
    //------------------------- End of transmit thread -----------------------//


    //------------------------- Start of receive thread -----------------------//
    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;
        private BlockingQueue<Message> pendingACKs;
//        private BlockingQueue<Message> lastReceivedMediumState;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
            receivedDataQueue = new LinkedBlockingQueue<>();
            receivedShortDataQueue = new LinkedBlockingQueue<>();
            pendingACKs = ACKsToSend;
//            lastReceivedMediumState = mediumState;
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print(  ( bytes.get(i) ) );
//                System.out.println(String.format("%8s",Integer.toBinaryString(bytes.get(i))).replace(' ','0'));
            }
            System.out.println();
        }

        //print received messages
        public void run() {
            while (true) {
                try {
                    Message m = receivedQueue.take();
                    MessageType type = m.getType();
                    switch (type) {
                        case HELLO -> {
                            System.out.println("HELLO");
                            mediumIsFree = true;
                        }
                        case DATA_SHORT -> {
                            //while (neighbours.size < 4)? - when do we know to stop receiving discovery messages
                            System.out.print("DATA_SHORT: ");
                            printByteBuffer(m.getData(), m.getData().capacity()); //Just print the data
                            receivedShortDataQueue.put(m);

                            if (m.getData().get(1) == 64) {         //only if is message is SYN, send a response

//                                sendingQueue.put(m.respondToSYN((byte) getIp()));   //send a response

                                ACKsToSend.put(m.respondToSYN((byte) getIp()));     //send a response through sending thread

                                neighbours.add(m.getData().get(0));                 //add source IP to neighbours list


                                System.out.print("ACK from " + getIp() + ":");
                                printByteBuffer(m.getData(), 2);
                                System.out.println("Pending ACKS size " + pendingACKs.size());
                                System.out.println("ACKs to send: " + ACKsToSend.size());

                            } else if ((m.getData().get(1)) >> 6 == 0) {   //if message is ACK, just add to neighbour's list
                                neighbours.add(m.getData().get(1));        //add the second byte to neighbour's list
                            }
                        }
                        case FREE -> {
                            System.out.println("FREE");
                            mediumIsFree = true;
                        }
                        case BUSY -> {
                            System.out.println("BUSY");
                            mediumIsFree = false;
                        }
                        case DATA -> {
                            System.out.print("DATA: ");
                            printByteBuffer(m.getData(), m.getData().capacity()); //Just print the data
                            receivedDataQueue.put(m);
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
    //------------------------- End of receive thread -----------------------//

    public int getIp() {
        return ip;
    }
}

