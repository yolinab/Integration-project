package Packets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class tes {

    public static void main(String[] args) {

        ByteBuffer buffer = ByteBuffer.allocate(2);

        buffer.put((byte) 63);              //00111111 for source IP(63)
        buffer.put((byte) (64 + 55));       //01 110111 for 01 SYN flag(64) and a placeholder for neighbours ip(55)

        //+128 for ACK flag (10000000)
//        for (int i = 0; i < buffer.capacity(); i++) {
//            System.out.println(String.format("%8s",Integer.toBinaryString(buffer.get(i))).replace(' ', '0'));
//        }
//        byte decodedIP = buffer.get(0);
//        System.out.println("Decoded ip: " + decodedIP);

        byte flag = (byte) ((buffer.get(1)) >> 6);  //01000000 becomes 00000001

        byte neighbourIP = (byte) (buffer.get(1) - 64);  //actual number

        boolean synFlag = flag == 1;

        System.out.println("The SYN flag is set:" + synFlag);
        System.out.println(String.format("%8s",Integer.toBinaryString(flag)).replace(' ', '0'));
        System.out.println("Decoded IP: ");
        System.out.println(String.format("%8s",Integer.toBinaryString(neighbourIP)).replace(' ', '0'));


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
}
