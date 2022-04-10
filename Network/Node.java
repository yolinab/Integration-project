package Network;

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

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public Node() {
        ip = (new Random().nextInt((int) System.currentTimeMillis())) % 64;
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use
        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!
        new sendingThread(sendingQueue);
        sendMessage();
    }


    public void sendDiscoveryMessage() {

    }

    public void sendMessage() {
        try{
            BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
            String input = "";
            while(true){
                input = inp.readLine(); // read input
                byte[] inputBytes = input.getBytes(); // get bytes from input
                ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
                toSend.put( inputBytes, 0, inputBytes.length ); // copy the input string into the byte buffer.
                Message msg;
                if( (input.length()) > 2 ){
                    msg = new Message(MessageType.DATA, toSend);
                } else {
                    msg = new Message(MessageType.DATA_SHORT, toSend);
                }
                sendingQueue.put(msg);
            }
        } catch (InterruptedException e){
            System.exit(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class sendingThread extends Thread {
        private BlockingQueue<Message> sendingQueue;

        public sendingThread(BlockingQueue<Message> sendingQueue) {
            super();
            this.sendingQueue = sendingQueue;
        }

        public void run() {
            try{
                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
                String input = "";
                while(true){
                    input = inp.readLine(); // read input
                    byte[] inputBytes = input.getBytes(); // get bytes from input
                    ByteBuffer toSend = ByteBuffer.allocate(inputBytes.length); // make a new byte buffer with the length of the input string
                    toSend.put( inputBytes, 0, inputBytes.length ); // copy the input string into the byte buffer.
                    Message msg;
                    if( (input.length()) > 2 ){
                        msg = new Message(MessageType.DATA, toSend);
                    } else {
                        msg = new Message(MessageType.DATA_SHORT, toSend);
                    }
                    sendingQueue.put(msg);
                }
            } catch (InterruptedException e){
                System.exit(2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;
        private BlockingQueue<Message> dataQueue;
        private BlockingQueue<Message> shortDataQueue;
        private BlockingQueue<Message> mediumState;


        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
            dataQueue = new LinkedBlockingQueue<>();
            shortDataQueue = new LinkedBlockingQueue<>();
            mediumState = new LinkedBlockingQueue<>();
        }

        public BlockingQueue<Message> getDataQueue() {
            return dataQueue;
        }

        public BlockingQueue<Message> getShortDataQueue() {
            return shortDataQueue;
        }


        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print( (char) ( bytes.get(i) ) );
            }
            System.out.println();
        }

        /**
         * Checks whether the last state of the medium is "FREE"
         * @return true if the channel is available to use
         */
        public boolean channelIsFree() {
            //if the last received message in the stack is "FREE", then we are able to send data
            return mediumState.peek().getType().equals(MessageType.FREE);
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
    }

    public int getIp() {
        return ip;
    }

}
