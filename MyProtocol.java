import Network.Node;
import client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
* This is just some example code to show you how to interact 
* with the server using the provided 'Client' class and two queues.
* Feel free to modify this code in any way you like!
*/

public class MyProtocol{

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    public MyProtocol(String server_ip, int server_port, int frequency){

    }

    public static void main(String args[]) throws IOException, InterruptedException {

        Node node =  new Node();
        System.out.println(node.getIp());

    }

}

