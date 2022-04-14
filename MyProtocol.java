import Network.Node;
import Packets.LinkStateRoutingPacket;
import client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

/**
* This is just some example code to show you how to interact 
* with the server using the provided 'Client' class and two queues.
* Feel free to modify this code in any way you like!
*/

public class MyProtocol{

    private static final String SERVER_IP = "netsys.ewi.utwente.nl";
    private static final int SERVER_PORT = 8954;
    private static final int frequency = 5301;

    public MyProtocol(){

    }

    public static void main(String args[]) throws IOException, InterruptedException {

        Node node =  new Node(SERVER_IP,SERVER_PORT,frequency);
        System.out.println(node.getIp());




    }

}

