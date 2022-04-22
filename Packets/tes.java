package Packets;

import client.Message;
import client.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class tes {


    public static void main(String[] args) {

        //debugging printing of received routing table
//        HashMap<Byte, Byte> neighbours = new HashMap<>();
//        neighbours.put((byte)1,(byte)2);
//        neighbours.put((byte)2,(byte)2);
//        neighbours.put((byte)3,(byte)3);
//        LinkStateRoutingPacket packet = new LinkStateRoutingPacket(64,neighbours);
//
//        ByteBuffer buffer = packet.getByteBuffer();
//        for (int i = 0; i < buffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
//        }
//
//        ArrayList<Byte> nei = packet.convertToMessage().readReceivedRoutingTable();
//        for (int i = 0; i < nei.size(); i++) {
//            System.out.println(nei.get(i));
//        }
//
//        System.out.println(getIp() + " received " + buffer.get(0) + "s' routing table");
//        for (int i = 0; i < 5; i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
//        }

//        ArrayList<Byte> readNeigh = packet.convertToMessage().readReceivedRoutingTable();
//        for (int i = 0; i < readNeigh.size(); i++) {
//            System.out.println(readNeigh.get(i));
//        }

//        HashMap<Integer,String> data = getConsoleInput();
//
//        for (Integer key: data.keySet()) {
//            System.out.println("Key: " + key);
//            System.out.println("Chat: " + data.get(key));
//        }


//        DiscoveryPacket packet = new DiscoveryPacket(63);
//        System.out.println("Intial packet:");
//        for (int i = 0; i < packet.getByteBuffer().capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(packet.getByteBuffer().get(i))).replace(' ', '0'));
//        }
//
//        Message message = new Message(MessageType.DATA,packet.getByteBuffer());
//        message.respondToDiscoverySYN((byte)5);
//        ByteBuffer buffer = message.getData();
//        System.out.println("After response:");
//        for (int i = 0; i < buffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(buffer.get(i) & 0xFF)).replace(' ', '0'));
//        }

//        byte b1 = (byte) 255;

//        tes test = new tes();
//
//        ArrayList<Byte> inRNG = test.getNodesInRange();
//        for (int i = 0; i < inRNG.size(); i++) {
//            System.out.println(inRNG.get(i));
//        }

//        LinkStateRoutingPacket packet = new LinkStateRoutingPacket(63, neighbours);       // 00111111
//
//        ByteBuffer buffer = packet.getByteBuffer();
//
//        System.out.println("Bytes:");
//        for (int i = 0; i < buffer.capacity(); i++) {
//            System.out.println(String.format("%8s", Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
//        }
//
//    }
    }

//    public ArrayList<Byte> getNodesInRange() {
//        ArrayList<Byte> directNeighbours = new ArrayList<>();
//        for (Byte destination: neighbours.keySet()) {
//
//            if ((byte)destination == neighbours.get(destination)) {
//                directNeighbours.add(destination);
//            }
//        }
//        return directNeighbours;
//    }
}
//        ByteBuffer buffer = ByteBuffer.allocate(2);
//
//        buffer.put((byte) 63);              //00111111 for source IP(63)
//        buffer.put((byte) (64 + 55));       //01 110111 for 01 SYN flag(64) and a placeholder for neighbours ip(55)

//+128 for ACK flag (10000000)
//        for (int i = 0; i < buffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
//        }
//        byte decodedIP = buffer.get(0);
//        System.out.println("Decoded ip: " + decodedIP);

//        byte flag = (byte) ((buffer.get(1)) >> 6);  //01000000 becomes 00000001
//
//        byte neighbourIP = (byte) (buffer.get(1) - 64);  //actual number
//
//        boolean synFlag = flag == 1;
//
//        System.out.println("The SYN flag is set:" + synFlag);
//        System.out.println(String.format("%8s",Integer.toBinaryString(flag)).replace(' ', '0'));
//        System.out.println("Decoded IP: ");
//        System.out.println(String.format("%8s",Integer.toBinaryString(neighbourIP)).replace(' ', '0'));


//        //Message
//        String msg = "hi";
//
//        //Binary message - 104 105
//        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
//
//        System.out.println("Bytes: ");
//        for (int i = 0; i < byteBuffer.capacity(); i++) {
//            System.out.println(byteBuffer.get(i));
//        }
//
//        //Decoding message
//        System.out.println("Message:");
//        for (int i = 0; i < byteBuffer.capacity(); i++) {
//            System.out.print((char)byteBuffer.get(i));
//        }

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

    //    For debugging
//    public static void main(String[] args) {
//
//        //--------
//        HashMap<Byte,Byte> neighbours = new HashMap<>();
//        neighbours.put((byte)1,(byte)1);                                //00000001
//        neighbours.put((byte)2,(byte)2);                                //00000010
//        neighbours.put((byte)3,(byte)3);                                //00000011
//
//        LinkStatePacket packet = new LinkStatePacket(63, neighbours);       //00111111
//
//        ByteBuffer bytes = packet.getByteBuffer();
//
//        System.out.println("Bytes:");
//        for (int i = 0; i<bytes.capacity(); i++) {
//            System.out.println(String.format("%8s", Integer.toBinaryString(bytes.get(i))).replace(' ', '0'));
//        }
//    }



/**
 * Fragmentation
 */
//    private HashMap<Integer, ArrayList<Message>> getConsoleInput() {
//        HashMap<Integer,ArrayList<Message>> input = new HashMap<>();
//        ArrayList<Message> messages = new ArrayList<>();
//        DataPacket dataPacket;
//        try {
//            BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
//            System.out.println("Please input chat message into the format: destination/message");
//            String[] parsed = inp.readLine().split("/");
//
//            int dest = Integer.parseInt(parsed[0]);
//            byte[] payloadBytes = parsed[1].getBytes();
//
//            if (payloadBytes.length > 28) {
//                int sw;
//                if (payloadBytes.length % 28 == 0){sw = payloadBytes.length / 28;}
//                else {sw = (payloadBytes.length / 28) + 1;}
//
//                for (int i = 1; i <= sw; i++) {
//
//                    //TODO - fragment payloadBytes
//                    dataPacket = new DataPacket(node.getIp(),dest,sw,i,payloadBytes);
//                    messages.add(dataPacket.convertToMessage());
//                }
//            } else {
//                dataPacket = new DataPacket(node.getIp(),dest,1,1,payloadBytes);
//                messages.add(dataPacket.convertToMessage());
//            }
//            input.put(dest,messages);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return input;
//    }
//
//
//    private ArrayList<byte[]> fragmentByteArray (byte[] byteArray) {
//        ArrayList<byte[]> listOfArrays = new ArrayList<>();
//        int sw;
//        int i = 0;
//        if (byteArray.length % 28 == 0){sw = byteArray.length / 28;}
//        else {sw = (byteArray.length / 28) + 1;}
//        byte[] current = new byte[];
//
//        while (i <= sw) {
//            for (int j = i*28; j < byteArray.length - sw * 28; j++) {
//                current[j] = byteArray[j];
//                //TODO - finish fragmentation
//            }
//        }
//    }

//------------------- RECEIVING ROUTING TABLE -------------------//
//FROM RECEIVING THREAD DATA
//                            if (m.getData().get(1) >> (byte) 4 == 4) {           //if we receive a SYN routing table we first read the routing table
//
//                                    ArrayList<Byte> senderNeighbours = m.readReceivedRoutingTable();
//        ArrayList<Byte> ourNeighbours = getNodesInRange();
//
//        //FIRST --- add only the destinations we currently don't have in range with next hop - the sender of the routing table
//        for (Byte senderNeighbour : senderNeighbours) {
//        neighbours.putIfAbsent(senderNeighbour, m.getData().get(0));
//        }
//        //SECOND - check if the nodes that are the same as in our routing table, we are still in direct contact with by checking if we have received a PONG from them recently
//        for (Byte ourNeighbour : ourNeighbours) {
//        for (Byte senderNeighbour : senderNeighbours) {
//
//        if (ourNeighbour.equals(senderNeighbour)) {
//
//        if (!hasRecentlyReceivedPONG(ourNeighbour)) {            //if we haven't received a PONG from that IP, update it
//        neighbours.put(ourNeighbour, m.getData().get(0));    //with next hop being the sender of the routing table
//        }
//        }
//        }
//        }
//        }
