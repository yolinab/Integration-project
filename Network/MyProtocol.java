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

    private boolean online;
    Node node;

    public MyProtocol(){

        node =  new Node(SERVER_IP,SERVER_PORT,frequency);
        online = true;
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
        new UpdateAvailableNodesThread().start();
        sendInput();

    }

    /**
     * Continuously reads from the console and puts the message in the sending queue of the node.
     *
     * Format: destination/message
     */
    private void sendInput() {
        DataPacket dataPacket;
        System.out.println("Please input chat message into the format: destination/message");
        while (online) {
            try {
                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
                String usrInp = "";
                usrInp = inp.readLine();
                if (usrInp.equals("exit")){online=false;}
                String[] parsed = usrInp.split("/");
                try {
                    int dest = Integer.parseInt(parsed[0]);
                    byte[] payloadBytes = parsed[1].getBytes();
                    dataPacket = new DataPacket(node.getIp(), dest, payloadBytes.length, payloadBytes);
                    node.putMessageInSendingQueue(dataPacket.convertToMessage());
                } catch (Exception e) {
                    System.out.println("Use correct input format.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads the routing table of a node and returns a list containing all the nodes currently reachable.
     *
     * @return ArrayList with all the currently reachable nodes
     */
    private ArrayList<Byte> getNodeRoutingTable() {
        HashMap<Byte,Byte> rt = node.getRoutingTable();
        return new ArrayList<>(rt.keySet());
    }

    /**
     * A class for printing the updated list of nodes that are currently online.
     */
    private class UpdateAvailableNodesThread extends Thread {

        public void run() {
            while (true) {
                try {
                    System.out.println("ONLINE NODES: ");
                    for (int i = 0; i < getNodeRoutingTable().size(); i++) {
                        System.out.println("Node:" + getNodeRoutingTable().get(i));
                    }
                    Thread.sleep(15000);
                    node.updateRoutingTable();
                }catch (InterruptedException e) {
                    System.err.println("Failed to send updated neighbours list.");
                    break;
                }
            }
        }
    }

}

