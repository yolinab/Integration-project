package Packets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;

public class tes {

    public static void main(String[] args) {

        HashMap<Byte,Byte> neighbours = new HashMap<>();
        neighbours.put((byte)1,(byte)1);
        neighbours.put((byte)2,(byte)2);
        neighbours.put((byte)3,(byte)3);

        LinkStatePacket packet = new LinkStatePacket(63, neighbours);       // 00111111

        ByteBuffer buffer = packet.getByteBuffer();

        System.out.println("Bytes:");
        for (int i = 0; i<buffer.capacity(); i++) {
            System.out.println(String.format("%8s", Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
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
}
