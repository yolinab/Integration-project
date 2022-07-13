package Network;

import Packets.DataPacket;
import client.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class for the protocol used by the nodes in the network.
 */

public class MyProtocol{

    private static final String SERVER_IP = "netsys.ewi.utwente.nl";
    private static final int SERVER_PORT = 8954;
    private static final int frequency = 5304;


    private boolean online;
    Node node;

    public MyProtocol(){

        node =  new Node(SERVER_IP,SERVER_PORT,frequency);
        online = true;
        System.out.println(node.getIp());
        node.initialize();

    }

    public void start() throws InterruptedException {

        System.out.println("Waiting for discovery of nodes...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendInput();
    }

    /**
     * Continuously reads from the console and puts the message in the sending queue of the node.
     * Format: destination/message
     */
    private void sendInput() {
        DataPacket dataPacket;
        System.out.println("Menu:\nsend   -> input your message\nonline -> show the online nodes\nexit   -> exit the chat");
        while (online) {
            try {
                BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
                String usrInp = "";
                int sw = 1;
                int seq = 1;
                int length;
                ArrayList<Byte> payloadBytesList;
                usrInp = inp.readLine();

                if (usrInp.equals("exit")){online=false;}
                if (usrInp.equals("online")) {
                    System.out.println("ONLINE NODES: ");
                    for (int i = 0; i < getNodeRoutingTable().size(); i++) {
                        System.out.println("Node:" + getNodeRoutingTable().get(i));
                    }
                }
                if (usrInp.equals("send")) {System.out.println("Please input chat message into the format: destination/message");}
                String[] parsed = usrInp.split("/");
                try {
                    int dest = Integer.parseInt(parsed[0]);
                    byte[] payloadBytes = parsed[1].getBytes();

                    //message needs fragmentation
                    if (payloadBytes.length > 27) {
                        length = 27;
                        if (payloadBytes.length % 27 > 0) {
                            sw = payloadBytes.length / 27 + 1;
                        } else {
                            sw = payloadBytes.length / 27;
                        }
                        payloadBytesList = arrayToArrayList(payloadBytes);
                        for (int i = 0; i < 27 ; i++) {
                            byte[] fragmented = new byte[27];
                            for (int j = 0; j < length; j++) {
                                fragmented[j] = payloadBytes[j];
                                payloadBytesList.remove(0);
                            }
                            payloadBytes = arrayListToArray(payloadBytesList);
                            dataPacket = new DataPacket(node.getIp(), dest, seq, sw, fragmented);
                            node.putMessageInSendingQueue(dataPacket.convertToMessage());

                            //get the last message's length
                            if ((seq == sw - 1) && payloadBytes.length % 27 > 0) {
                                length = payloadBytes.length % 27;
                            }
                            if (seq == sw) {break;}
                            seq++;
                        }
                    } else {
                        //send a single message
                        dataPacket = new DataPacket(node.getIp(), dest, seq, sw, payloadBytes);
                        node.putMessageInSendingQueue(dataPacket.convertToMessage());
                    }
                } catch (Exception e) {
                    System.out.println("");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads the routing table of a node and returns a list containing all the nodes currently reachable.
     * @return ArrayList with all the currently reachable nodes
     */
    private ArrayList<Byte> getNodeRoutingTable() {
        ArrayList<Byte> rt = node.getRoutingTable();
        return rt;
    }

    /**
     * Converts a byte array to an array list of bytes
     * @param byteArray to convert
     * @return an array list with the array's elemrnts
     */
    public ArrayList<Byte> arrayToArrayList(byte[] byteArray) {
        ArrayList<Byte> result = new ArrayList<>();
        for (int i = 0; i < byteArray.length; i++) {
            result.add(byteArray[i]);
        }
        return result;
    }

    /**
     * Converts an arrayList of bytes to a byte array
     * @param byteArrayList to convert
     * @return an array with the arrayList's elements
     */
    public byte[] arrayListToArray(ArrayList<Byte> byteArrayList) {
        byte[] result = new byte[byteArrayList.size()];
        for (int i = 0; i < byteArrayList.size(); i++) {
            result[i] = byteArrayList.get(i);
        }
        return result;
    }
}

