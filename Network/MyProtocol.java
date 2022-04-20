package Network;

import Packets.DataPacket;
import client.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

/**
*
*/

public class MyProtocol{

    private static final String SERVER_IP = "netsys.ewi.utwente.nl";
    private static final int SERVER_PORT = 8954;
    private static final int frequency = 5301;

    Node node;
    private BlockingQueue<Message> dataMessagesToSend;
    private static HashMap<Integer,Message> chatInput; //TODO -static?

    public MyProtocol(){

        node =  new Node(SERVER_IP,SERVER_PORT,frequency);
        System.out.println(node.getIp());
        node.initialize();
    }

    public void start() {

        System.out.println("Waiting for discovery of nodes...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Ready to send a message to:");
        for (int i = 0; i < getNodeRoutingTable().size(); i++) {
            System.out.println("Node:" + getNodeRoutingTable().get(i));
        }
        chatInput = this.sendInput();

    }

    /**
     * Reads input from the console and returns a hashmap where the key is the
     * desired destination and the value is the messages for that destination.
     *
     * @return the destination and the message intended to reach that destination
     */
    private HashMap<Integer,Message> sendInput() {
        HashMap<Integer,Message> input = new HashMap<>();
        DataPacket dataPacket;
        try {
            BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please input chat message into the format: destination/message");
            String usrInp = "";
            usrInp = inp.readLine();
            String[] parsed = usrInp.split("/");

            try {
                int dest = Integer.parseInt(parsed[0]);
                byte[] payloadBytes = parsed[1].getBytes();
                dataPacket = new DataPacket(node.getIp(),dest,payloadBytes.length,1,payloadBytes);

                input.put(dest,dataPacket.convertToMessage());
                node.putMessageInSendingQueue(dataPacket.convertToMessage());
            } catch (Exception e) {
                System.out.println("Use correct input format.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

    /**
     * Reads the routing table of a node and returns a list containing
     * all the nodes currently reachable.
     *
     * @return ArrayList with all the currently reachable nodes
     */
    private ArrayList<Byte> getNodeRoutingTable() {
        HashMap<Byte,Byte> rt = node.getRoutingTable();
        return new ArrayList<>(rt.keySet());
    }

}

