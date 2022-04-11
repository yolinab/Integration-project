package Network;

import Packets.DiscoveryPacket;
import client.Client;
import client.Message;
import client.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {

    private int ip;

    private static String SERVER_IP = "netsys.ewi.utwente.nl"; //"127.0.0.1";
    private static int SERVER_PORT = 8954;
    private static int frequency = 5300;

    public BlockingQueue<Message> dataQueue;
    public BlockingQueue<Message> shortDataQueue;
    public BlockingQueue<Message> mediumState;
    public BlockingQueue<Message> nodeBuffer;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node() {
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();


        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use

        DiscoveryPacket discoveryPacket = new DiscoveryPacket(this.getIp());
        discoveryPacket.makeSYN();
        Message msg = new Message(MessageType.DATA_SHORT, discoveryPacket.getByteBuffer());


        new transmitThread(sendingQueue, msg).start();
        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!

//        try {
//            if (mediumState.peek().getType().equals(MessageType.FREE) || mediumState.peek().getType().equals(MessageType.HELLO)) {
//                sendDiscoveryMessage();
//            }
//        } catch (NullPointerException e) {
//            System.err.println(e);
//        }


        //sent discovery message as soon as object is initialised, so there are no collisions?
    }

    //we assume since this method gets invoked only once in the beginning,
    //there will be no collisions(hoping the propagation time for DATA_SHORT is fast enough)
    //asserTrue (time between node initialisations > propagation time of DATA_SHORT)
    public void sendDiscoveryMessage() {
        DiscoveryPacket discoveryPacket = new DiscoveryPacket(this.getIp());
        discoveryPacket.makeSYN();
        try {
            sendingQueue.put(new Message(MessageType.DATA_SHORT, discoveryPacket.getByteBuffer()));
        } catch (InterruptedException e) {
            System.err.println("Failed to send discovery message.");
        }
    }

    //For now only works with console input
    private  class transmitThread extends Thread {
        private BlockingQueue<Message> sendingQueue;
        private Message message;

        public transmitThread(BlockingQueue<Message> sendingQueue, Message message){
            super();
            this.sendingQueue = sendingQueue;
            this.message = message;
        }

        private void putMessageInQueue(Message message) {
            try {
                sendingQueue.put(message);
            } catch (InterruptedException e) {
                System.err.println("Failed to put message in sending queue." + e);
            }

        }

        public void run() {
//            while (true) {
//                try {
//                    sendingQueue.put(consoleInput());
//                } catch (InterruptedException e) {
//                    System.err.println("Failed to send message. " + e);
//                }
                putMessageInQueue(message);
//            }
        }

        private Message consoleInput() {
            try {
                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
                String input = "";
                while (true) {
                    input = inp.readLine(); // read input
                    byte[] inputBytes = input.getBytes(); // get bytes from input
                    ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
                    toSend.put(inputBytes, 0, inputBytes.length); // copy the input string into the byte buffer.
                    Message msg;
                    if ((input.length()) > 2) {
                        msg = new Message(MessageType.DATA, toSend);
                    } else {
                        msg = new Message(MessageType.DATA_SHORT, toSend);
                    }
                    return msg;
                }
            } catch (IOException e) {
                System.err.println("Failed to get input from the console. " + e);
            }
            //DANGER!!!//TODO fix this shit
            return null;
        }
    }

    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
            dataQueue = new LinkedBlockingQueue<>();
            shortDataQueue = new LinkedBlockingQueue<>();
            mediumState = new LinkedBlockingQueue<>();
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
//                System.out.print( (char) ( bytes.get(i) ) );
                System.out.println(String.format("%8s",Integer.toBinaryString(bytes.get(i))).replace(' ','0'));

            }
            System.out.println();
        }

        //print received messages
        public void run(){
            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){ // The channel is busy (A node is sending within our detection range)
                        System.out.println("BUSY");
                        mediumState.put(m);
                    } else if (m.getType() == MessageType.FREE){ // The channel is no longer busy (no nodes are sending within our detection range)
                        System.out.println("FREE");
                        mediumState.put(m);
                    } else if (m.getType() == MessageType.DATA){ // We received a data frame!

                        System.out.print("DATA: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                        dataQueue.put(m);

                    } else if (m.getType() == MessageType.DATA_SHORT){ // We received a short data frame!

                        System.out.print("DATA_SHORT: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                        shortDataQueue.put(m);

                    } else if (m.getType() == MessageType.DONE_SENDING){ // This node is done sending
                        System.out.println("DONE_SENDING");
                    } else if (m.getType() == MessageType.HELLO){ // Server / audio framework hello message. You don't have to handle this
                        System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING){ // This node is sending
                        System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END){ // Server / audio framework disconnect message. You don't have to handle this
                        System.out.println("END");
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }
            }
        }
        /**
         * Checks whether the last state of the medium is "FREE"
         * @return true if the channel is available to use
         */
        public boolean channelIsFree() {
            //if the last received message in the stack is "FREE", then we are able to send data
            return mediumState.peek().getType().equals(MessageType.FREE);
        }
    }

    public int getIp() {
        return ip;
    }

}
